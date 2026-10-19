import Foundation
import PrinterSDK

// Keep this owner at App scope. Pages borrow it; only this owner releases hardware.
@MainActor
final class SharedPrinterApp {
    enum Phase: Sendable { case disconnected, connecting, connected, disconnecting, closed }
    enum BackgroundPolicy { case keepConnection, disconnect }
    struct Gray8Label: Sendable {
        let pixels: Data
        let width: UInt32
        let height: UInt32
    }
    struct CompletedPrint: Sendable {
        let requestID: UUID
        let result: OperationResult
    }
    struct ViewState: Sendable {
        let phase: Phase
        let device: DeviceStateSnapshot?
        let submitting: Bool
        let hasOperation: Bool
        let completed: CompletedPrint?
        let error: PrinterSDKError?
    }

    // Explicit cancellation removes this page's notifications, never App-owned work.
    @MainActor
    final class PageSubscription {
        private weak var owner: SharedPrinterApp?
        private let id: UUID
        fileprivate init(owner: SharedPrinterApp, id: UUID) { self.owner = owner; self.id = id }
        func cancel() { owner?.pages.removeValue(forKey: id); owner = nil }
    }

    private let manager: PrinterManager
    private let deliveryQueue: DispatchQueue
    private var device: MobilePrinter?
    private var operation: PrintOperation?
    private var stateToken: ObservationSubscription?
    private var completionToken: ObservationSubscription?
    private var preparation: Task<PrintOperation, Error>?
    private var submission: Task<Void, Never>?
    private var pages: [UUID: @MainActor (ViewState) -> Void] = [:]
    private var endpoint: (UUID, BLEOptions)?
    private var revision: UInt64 = 0
    private var phase = Phase.disconnected
    private var currentState: DeviceStateSnapshot?
    private var completed: CompletedPrint?
    private var lastError: PrinterSDKError?
    private var requestID: UUID?
    private var foreground = true, cancelPending = false, closed = false

    init(manager: PrinterManager = PrinterManager(), deliveryQueue: DispatchQueue = .main) {
        self.manager = manager; self.deliveryQueue = deliveryQueue
    }

    var viewState: ViewState {
        ViewState(phase: phase, device: currentState, submitting: submission != nil,
                  hasOperation: operation != nil, completed: completed, error: lastError)
    }
    func observePage(_ render: @escaping @MainActor (ViewState) -> Void) -> PageSubscription {
        let id = UUID()
        if !closed { pages[id] = render }
        render(viewState)
        return PageSubscription(owner: self, id: id)
    }
    private func publish() {
        let value = viewState
        // Snapshot iteration permits a page to unsubscribe inside its own callback.
        for (id, render) in Array(pages) where pages[id] != nil { render(value) }
    }

    func connect(identifier: UUID, options: BLEOptions) async throws {
        guard !closed, foreground, device == nil, phase == .disconnected else { throw PrinterSDKError(1002) }
        endpoint = (identifier, options)
        revision &+= 1; let version = revision
        let printer = try manager.printer(bleIdentifier: identifier, options: options)
        device = printer; phase = .connecting; lastError = nil; publish()
        do {
            stateToken = try printer.observeState(queue: deliveryQueue) { [weak self] value in
                Task { @MainActor [weak self] in self?.receiveState(value, printer: printer, revision: version) }
            }
            try await manager.connect(printer)
            guard revision == version, device === printer, !closed else { throw CancellationError() }
            let value = try printer.stateSnapshot() // Local observation, no printer IO.
            guard value.connectionState == .connected else { throw PrinterSDKError(2002) }
            currentState = value; phase = .connected; publish()
        } catch {
            if revision == version {
                lastError = sdkError(error)
                await disconnect()
            }
            throw error
        }
    }
    // Retry is a user/App decision. It never replays a failed or cancelled print.
    func retryConnection() async throws {
        guard !closed, foreground, phase != .connecting, phase != .disconnecting,
              let (identifier, options) = endpoint else { throw PrinterSDKError(1002) }
        await disconnect()
        try await connect(identifier: identifier, options: options)
    }
    private func receiveState(_ value: DeviceStateSnapshot, printer: MobilePrinter, revision version: UInt64) {
        guard revision == version, device === printer, !closed,
              value.connectionGeneration >= (currentState?.connectionGeneration ?? 0) else { return }
        currentState = value
        if phase == .connected && value.connectionState != .connected { phase = .disconnected }
        publish()
    }

    // This example permits one App-owned print at a time. The SDK itself has a FIFO.
    // Decode/render in prepare; long preparation should check Task cancellation itself.
    func printLabel(profile: CpclProfile, prepare: @escaping @Sendable () throws -> Gray8Label) throws {
        guard !closed, foreground, phase == .connected, submission == nil, operation == nil,
              let printer = device else { throw PrinterSDKError(1002) }
        let id = UUID(), version = revision
        requestID = id; cancelPending = false; lastError = nil
        let work = Task.detached(priority: .userInitiated) {
            try Task.checkCancellation()
            let input = try prepare()
            try Task.checkCancellation() // Cancel before admission produces no printer operation.
            return try printer.submitGray8(input.pixels, width: input.width, height: input.height, profile: profile)
        }
        preparation = work
        submission = Task { @MainActor [self] in
            do {
                let next = try await work.value
                operation = next
                completionToken = try next.onComplete(queue: deliveryQueue) { [weak self] result in
                    Task { @MainActor [weak self] in self?.finish(result, request: id) }
                }
                // Cancellation can race the final check and native admission. Cancel the
                // returned operation too; already accepted printer bytes are irreversible.
                if cancelPending || closed || revision != version || device !== printer { try? next.cancel() }
            } catch {
                if let operation { try? operation.cancel(); operation.close(); self.operation = nil }
                completionToken?.cancel(); completionToken = nil
                lastError = sdkError(error); requestID = nil
            }
            preparation = nil; submission = nil; cancelPending = false; publish()
        }
        publish()
    }
    func cancelPrint() {
        cancelPending = true
        preparation?.cancel()
        try? operation?.cancel()
    }
    private func finish(_ result: OperationResult, request id: UUID) {
        guard requestID == id else { return }
        completed = CompletedPrint(requestID: id, result: result)
        completionToken?.cancel(); completionToken = nil
        operation?.close(); operation = nil; requestID = nil
        // A terminal from a retired connection updates history, never connection readiness.
        publish()
    }
    private func sdkError(_ error: Error) -> PrinterSDKError {
        if let value = error as? PrinterSDKError { return value }
        return PrinterSDKError(error is CancellationError ? 1005 : 1000)
    }

    func disconnect() async {
        revision &+= 1; let version = revision
        let printer = device
        phase = closed ? .closed : .disconnecting
        currentState = nil; stateToken?.cancel(); stateToken = nil
        cancelPrint(); publish()
        if let printer { await manager.release(printer) }
        if device === printer { device = nil }
        if revision == version { phase = closed ? .closed : .disconnected; publish() }
        // Keep the App's terminal subscription: a disconnected task still owes a result.
    }
    func applicationActive(_ active: Bool, background policy: BackgroundPolicy) async {
        foreground = active
        if !active && policy == .disconnect { await disconnect() }
        // Returning to foreground never silently reconnects or replays work.
    }
    func close() async {
        closed = true
        let pending = submission
        await disconnect(); await manager.close(); await pending?.value
        // Native destruction has made accepted operations terminal. Copy before release,
        // even if their main-queue callback has not run yet.
        if let operation, let id = requestID, let result = try? operation.result() { finish(result, request: id) }
        completionToken?.cancel(); completionToken = nil
        operation?.close(); operation = nil; requestID = nil
        phase = .closed; publish(); pages.removeAll()
    }
}

// A page keeps only its own notification token. The App retains SharedPrinterApp.
@MainActor
final class SharedPrinterPage {
    private let app: SharedPrinterApp
    private var notifications: SharedPrinterApp.PageSubscription?
    var render: (SharedPrinterApp.ViewState) -> Void = { _ in }
    init(app: SharedPrinterApp) { self.app = app }
    func appear() {
        notifications?.cancel()
        notifications = app.observePage { [weak self] value in self?.render(value) }
    }
    func disappear() { notifications?.cancel(); notifications = nil }
    // Printing/cancellation use app.printLabel/app.cancelPrint. Do not close on disappear.
}
