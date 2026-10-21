import Foundation
import PrinterSDK

// The application owns this object. Pages subscribe to App state, never SDK internals.
@MainActor
final class SharedPrinterApp {
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
    private var foreground = true
    init(manager: PrinterManager = PrinterManager()) { self.manager = manager }
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
    func connect(_ device: DiscoveredPrinter,
                 completion: @escaping @MainActor @Sendable (ManagerResult<ConnectionInfo>) -> Void = { _ in }) -> RequestID {
        endpoint = device
        if phase == .disconnected { phase = .connecting; errorCode = nil; publish() }
        return manager.connectBle(device: device, completion: { [weak self] result in
            if let self, phase != .closed {
                if result.isSuccess { phase = .connected }
                else if phase == .connecting { phase = .disconnected }
                errorCode = result.isSuccess ? nil : result.code
                publish()
            }
            completion(result)
        }, onDisconnected: { [weak self] result in
            guard let self, phase != .closed else { return }
            phase = .disconnected; errorCode = result.code; publish()
        })
    }
    // Explicit retry reconnects only; it never resubmits a previous print.
    func retryConnection(completion: @escaping @MainActor @Sendable (ManagerResult<ConnectionInfo>) -> Void) {
        guard let endpoint else { return }
        disconnect { [weak self] _ in self?.connect(endpoint, completion: completion) }
    }
    @discardableResult
    func printLabel(pngJpeg: Data, widthMm: Double, heightMm: Double) -> RequestID {
        let id = manager.printEncodedImage(pngJpeg, widthMm: widthMm, heightMm: heightMm) { [weak self] result in
            guard let self else { return }
            pending.remove(result.requestID); completed = result
            errorCode = result.isSuccess ? nil : result.code
            publish() // A retired print updates history, not connection readiness.
        }
        pending.insert(id); publish(); return id
    }
    func cancelPrint() { for id in pending { manager.cancel(id) } }
    func cancel(_ id: RequestID) { manager.cancel(id) }
    func disconnect(_ completion: @escaping @MainActor @Sendable (ManagerResult<Void>) -> Void = { _ in }) {
        if phase != .closed { phase = .disconnecting; publish() }
        manager.disconnect { [weak self] result in
            if let self, phase != .closed { phase = .disconnected; publish() }
            completion(result)
        }
    }
    func applicationActive(_ active: Bool, background policy: BackgroundPolicy,
                           completion: @escaping @MainActor @Sendable () -> Void = {}) {
        foreground = active
        if !active && policy == .disconnect { disconnect { _ in completion() } }
        else { completion() }
    }
    func close(_ completion: @escaping @MainActor @Sendable (ManagerResult<Void>) -> Void = { _ in }) {
        phase = .closed
        manager.close { [weak self] result in
            self?.publish(); self?.pages.removeAll(); completion(result)
        }
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
