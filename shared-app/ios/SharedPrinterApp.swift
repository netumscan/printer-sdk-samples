import Foundation
import XLPrinterSDK

// The application owns this object. Pages subscribe to App state, never SDK internals.
@MainActor
final class SharedPrinterApp: PrinterDelegate {
    enum LifecycleError: Error { case closed, noSavedDevice }
    enum Phase: Sendable { case disconnected, connecting, connected, disconnecting, closed }
    enum BackgroundPolicy { case keepConnection, disconnect }
    struct ViewState: Sendable {
        let phase: Phase
        let pendingPrintCount: Int
        let completed: ManagerResult<Void>?
        let errorCode: Int32?
    }
    @MainActor
    final class PageSubscription {
        private weak var owner: SharedPrinterApp?
        private let id: UUID
        fileprivate init(owner: SharedPrinterApp, id: UUID) { self.owner = owner; self.id = id }
        func cancel() { owner?.pages.removeValue(forKey: id); owner = nil }
    }
    private let manager: PrinterManager
    private var phase = Phase.disconnected
    private var pending: Set<RequestID> = []
    private var completed: ManagerResult<Void>?
    private var errorCode: Int32?
    private var pages: [UUID: @MainActor (ViewState) -> Void] = [:]
    private var endpoint: DiscoveredPrinter?
    private var endpointModel: PrinterModel?
    private var foreground = true
    private var connections: [RequestID: @MainActor @Sendable (ManagerResult<ConnectionInfo>) -> Void] = [:]
    private var cleanup: [RequestID: @MainActor @Sendable (ManagerResult<Void>) -> Void] = [:]
    private var closeRequest: RequestID?
    private var closeResult: ManagerResult<Void>?
    private var closeWaiters: [@MainActor @Sendable (ManagerResult<Void>) -> Void] = []
    private var closingOwner: SharedPrinterApp?
    init(manager: PrinterManager = PrinterManager()) { self.manager = manager; manager.delegate = self }
    private func requireOpen() throws {
        guard phase != .closed else { throw LifecycleError.closed }
    }
    var viewState: ViewState {
        ViewState(phase: phase, pendingPrintCount: pending.count, completed: completed, errorCode: errorCode)
    }
    func observePage(_ render: @escaping @MainActor (ViewState) -> Void) -> PageSubscription {
        let id = UUID()
        if phase != .closed { pages[id] = render }
        render(viewState)
        return PageSubscription(owner: self, id: id)
    }
    private func publish() {
        let value = viewState
        for (id, render) in Array(pages) where pages[id] != nil { render(value) }
    }
    @discardableResult
    func connect(_ device: DiscoveredPrinter, model: PrinterModel? = nil,
                 completion: @escaping @MainActor @Sendable (ManagerResult<ConnectionInfo>) -> Void = { _ in }) throws -> RequestID {
        try requireOpen()
        let id = model.map { manager.connectBle(device: device, model: $0) } ?? manager.connectBle(device: device)
        connections[id] = completion
        endpoint = device
        endpointModel = model
        if phase == .disconnected { phase = .connecting; errorCode = nil; publish() }
        return id
    }
    func printerManager(_ manager: PrinterManager, didFinishConnecting result: ManagerResult<ConnectionInfo>) {
        if phase != .closed {
            if result.isSuccess { phase = .connected }
            else if phase == .connecting { phase = .disconnected }
            errorCode = result.isSuccess ? nil : result.code
            publish()
        }
        connections.removeValue(forKey: result.requestID)?(result)
    }
    func printerManager(_ manager: PrinterManager, didLoseConnection result: ManagerResult<Void>) {
        guard phase != .closed else { return }
        phase = .disconnected; errorCode = result.code; publish()
    }
    // Explicit retry reconnects only; it never resubmits a previous print.
    func retryConnection(completion: @escaping @MainActor @Sendable (Result<ManagerResult<ConnectionInfo>, LifecycleError>) -> Void) throws {
        try requireOpen()
        guard let endpoint else { throw LifecycleError.noSavedDevice }
        let model = endpointModel
        try disconnect { [self] _ in
            do { try connect(endpoint, model: model) { completion(.success($0)) } }
            catch { completion(.failure(.closed)) }
        }
    }
    @discardableResult
    func printLabel(pngJpeg: Data, widthMm: Double, heightMm: Double) throws -> RequestID {
        try requireOpen()
        let id = manager.printEncodedImage(pngJpeg, widthMm: widthMm, heightMm: heightMm)
        pending.insert(id); publish(); return id
    }
    func printerManager(_ manager: PrinterManager, didFinishPrinting result: ManagerResult<Void>) {
        pending.remove(result.requestID); completed = result
        errorCode = result.isSuccess ? nil : result.code
        publish() // A retired print updates history, not connection readiness.
    }
    func cancelPrint() { for id in pending { manager.cancel(id) } }
    func cancel(_ id: RequestID) { manager.cancel(id) }
    func disconnect(_ completion: @escaping @MainActor @Sendable (ManagerResult<Void>) -> Void = { _ in }) throws {
        try requireOpen()
        cleanup[manager.disconnect()] = completion
        phase = .disconnecting; publish()
    }
    func printerManager(_ manager: PrinterManager, didFinishDisconnecting result: ManagerResult<Void>) {
        if phase != .closed { phase = .disconnected; publish() }
        cleanup.removeValue(forKey: result.requestID)?(result)
    }
    func applicationActive(_ active: Bool, background policy: BackgroundPolicy,
                           completion: @escaping @MainActor @Sendable () -> Void = {}) throws {
        try requireOpen()
        foreground = active
        if !active && policy == .disconnect { try disconnect { _ in completion() } }
        else { completion() }
    }
    func close(_ completion: @escaping @MainActor @Sendable (ManagerResult<Void>) -> Void = { _ in }) {
        if let closeResult { DispatchQueue.main.async { completion(closeResult) }; return }
        phase = .closed
        closeWaiters.append(completion)
        if closeRequest == nil { closingOwner = self; closeRequest = manager.close() }
    }
    func printerManager(_ manager: PrinterManager, didFinishClosing result: ManagerResult<Void>) {
        guard closeRequest == result.requestID else { return }
        closeResult = result; closeRequest = nil
        publish(); pages.removeAll()
        let waiters = closeWaiters; closeWaiters.removeAll()
        for completion in waiters { completion(result) }
        closingOwner = nil
    }
}

@MainActor
final class SharedPrinterPage {
    private let app: SharedPrinterApp
    private var notifications: SharedPrinterApp.PageSubscription?
    var render: (SharedPrinterApp.ViewState) -> Void = { _ in }
    init(app: SharedPrinterApp) { self.app = app }
    func appear() { notifications?.cancel(); notifications = app.observePage { [weak self] in self?.render($0) } }
    func disappear() { notifications?.cancel(); notifications = nil }
}
