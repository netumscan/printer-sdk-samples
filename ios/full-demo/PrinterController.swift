import Foundation
import PrinterSDK

struct DemoTaskUpdate {
    let id: UInt64
    let state, completion: UInt32
    let current, total: UInt64
    let error: Int32
    var totalKnown = true
    var sentExact = true
    var message = ""
    var diagnostic: ErrorDiagnostic?
}

protocol DemoPrinterSession: AnyObject {
    func connect() async throws
    func submit(_ pixels: Data, config: LabelConfiguration) throws -> UInt64
    func mediaControl(_ mode: MediaMode?, profile: CpclProfile) throws -> UInt64
    func raw(_ bytes: Data, copies: UInt32) throws -> UInt64
    func observe(queue: DispatchQueue, update: @escaping (DemoTaskUpdate) -> Void,
                 state: @escaping (DeviceStateSnapshot?) -> Void) throws -> () -> Void
    func stateSnapshot() throws -> DeviceStateSnapshot?
    func resourceSnapshot() throws -> ResourceSnapshot?
    func diagnostic() throws -> ErrorDiagnostic?
    func cancel(_ id: UInt64) throws
    func close() async
}

extension DemoPrinterSession {
    func mediaControl(_ mode: MediaMode?, profile: CpclProfile) throws -> UInt64 { throw PrinterSDKError(1004) }
    func raw(_ bytes: Data, copies: UInt32) throws -> UInt64 { throw PrinterSDKError(1004) }
    func stateSnapshot() throws -> DeviceStateSnapshot? { nil }
    func resourceSnapshot() throws -> ResourceSnapshot? { nil }
    func diagnostic() throws -> ErrorDiagnostic? { nil }
}

final class SDKSession: DemoPrinterSession {
    private let manager: PrinterManager
    private let printer: MobilePrinter
    private let lock = NSLock()
    private var operations: [UInt64: PrintOperation] = [:]
    private var subscriptions: [UInt64: ObservationSubscription] = [:]
    private var stateSubscription: ObservationSubscription?
    private var eventQueue = DispatchQueue.main
    private var eventCallback: ((DemoTaskUpdate) -> Void)?
    private var closed = false
    convenience init(manager: PrinterManager, identifier: UUID, options: BLEOptions) throws {
        self.init(manager: manager, printer: try manager.printer(bleIdentifier: identifier, options: options))
    }
    init(manager: PrinterManager, printer: MobilePrinter) {
        self.manager = manager; self.printer = printer
    }
    private func retain(_ operation: PrintOperation) throws -> UInt64 {
        lock.lock(); defer { lock.unlock() }
        guard !closed else { operation.close(); throw PrinterSDKError(1002) }
        operations[operation.id] = operation
        subscriptions[operation.id] = try operation.onComplete(queue: eventQueue) { [weak self] result in
            self?.complete(result)
        }
        return operation.id
    }
    func connect() async throws { try await manager.connect(printer, timeoutMilliseconds: 10000) }
    func stateSnapshot() throws -> DeviceStateSnapshot? { try printer.stateSnapshot() }
    func resourceSnapshot() throws -> ResourceSnapshot? { try printer.resourceSnapshot() }
    func diagnostic() throws -> ErrorDiagnostic? { try printer.diagnostic() }
    func submit(_ pixels: Data, config: LabelConfiguration) throws -> UInt64 {
        let parameters = try DemoConnectionConfiguration(dpi: config.dpi, deviceWidth: config.deviceWidth, kind: config.kind)
        let operation: PrintOperation
        if config.kind == .label {
            operation = try printer.submitGray8(pixels, width: UInt32(config.width), height: UInt32(config.height),
                profile: parameters.profile(),
                copies: UInt32(config.copies), trailingBlankDots: UInt32(config.trailingBlankDots),
                canvasWidthDots: UInt32((config.width + 7) / 8 * 8), originXDots: UInt32(config.xOffsetDots), form: config.form)
        } else {
            operation = try printer.submitReceiptGray8(pixels, width: UInt32(config.width), height: UInt32(config.height),
                profile: parameters.receiptProfile(),
                copies: UInt32(config.copies), maxTotalLengthDots: config.receiptBudget,
                trailingBlankDots: UInt32(config.trailingBlankDots))
        }
        return try retain(operation)
    }
    func mediaControl(_ mode: MediaMode?, profile: CpclProfile) throws -> UInt64 {
        if let mode { return try retain(printer.setMediaMode(mode, profile: profile)) }
        return try retain(printer.feedToNextLabel(profile: profile))
    }
    func raw(_ bytes: Data, copies: UInt32) throws -> UInt64 { try retain(printer.submitRaw(bytes, copies: copies)) }
    func observe(queue: DispatchQueue, update: @escaping (DemoTaskUpdate) -> Void,
                 state: @escaping (DeviceStateSnapshot?) -> Void) throws -> () -> Void {
        lock.lock(); defer { lock.unlock() }
        guard !closed else { throw PrinterSDKError(1002) }
        eventQueue = queue; eventCallback = update
        stateSubscription = try printer.observeState(queue: queue) { state($0) }
        return { [weak self] in self?.stopObserving() }
    }
    private func stopObserving() {
        lock.lock(); eventCallback = nil
        let state = stateSubscription; stateSubscription = nil
        let tokens = subscriptions; subscriptions.removeAll(); lock.unlock()
        state?.cancel(); for token in tokens.values { token.cancel() }
    }
    private func complete(_ result: OperationResult) {
        let value = result.snapshot
        lock.lock()
        let operation = operations.removeValue(forKey: value.id)
        subscriptions.removeValue(forKey: value.id)
        let callback = closed ? nil : eventCallback; lock.unlock()
        operation?.close()
        callback?(DemoTaskUpdate(id: value.id, state: value.state, completion: value.completion,
            current: value.bytesSent, total: value.bytesTotal ?? 0, error: value.errorCode,
            totalKnown: value.bytesTotal != nil, sentExact: value.bytesSentExact,
            message: result.diagnostic.message, diagnostic: result.diagnostic))
    }
    func cancel(_ id: UInt64) throws {
        lock.lock(); defer { lock.unlock() }
        if let operation = operations[id] { try operation.cancel() }
    }
    private func beginClose() { lock.lock(); closed = true; lock.unlock() }
    private func takeOperations() -> [UInt64: PrintOperation] {
        lock.lock(); defer { lock.unlock() }
        let held = operations; operations.removeAll(); return held
    }
    func close() async {
        beginClose(); stopObserving()
        await manager.release(printer)
        let held = takeOperations()
        for operation in held.values { operation.close() }
    }
}

/// App orchestration only. All printer IO remains in the SDK's device executor.
enum DemoOperationKind { case image, raw, media(MediaMode?) }

/// Presentation keeps SDK work separate from unverified hardware state.
struct DemoDeviceStateDisplay {
    let connection, work, queue, hardware: String
    init(snapshot: DeviceStateSnapshot?, phase: PrinterController.Phase) {
        if let snapshot {
            switch snapshot.connectionState {
            case .disconnected: connection = "未连接 · BLE"
            case .connecting: connection = "正在连接 · BLE"
            case .connected: connection = "已连接 · BLE"
            case .disconnecting: connection = "正在断开 · BLE"
            }
            switch snapshot.workState {
            case .idle: work = "SDK：无待处理任务"
            case .queued: work = "SDK：等待执行"
            case .preparing: work = "SDK：准备中"
            case .sending: work = "SDK：发送中"
            }
            queue = "等待 \(snapshot.queuedCount) 项"
        } else {
            switch phase {
            case .offline, .closed: connection = "未连接 · BLE"
            case .connecting: connection = "正在连接 · BLE"
            case .stopping: connection = "正在断开 · BLE"
            case .ready, .printing: connection = "连接状态未知 · BLE"
            }
            work = "SDK：状态未知"; queue = "排队数未知"
        }
        hardware = "硬件：未知"
    }
    var activity: String { "\(work) · \(queue) · \(hardware)" }
}

final class PrinterController {
    enum Phase { case offline, connecting, ready, printing, stopping, closed }
    private let lock = NSRecursiveLock()
    private let worker = DispatchQueue(label: "PrinterDemo.worker")
    private let control = DispatchQueue(label: "PrinterDemo.control")
    private let manager: PrinterManager
    private let changed: (Phase, String) -> Void
    private let log: (String) -> Void
    private let completed: (DemoOperationKind, UInt64, Bool) -> Void
    private let stateChanged: (DeviceStateSnapshot?) -> Void
    private let detailsChanged: (ResourceSnapshot?, ErrorDiagnostic?) -> Void
    private let timeout: TimeInterval
    private var timeoutTask: DispatchWorkItem?
    private var session: DemoPrinterSession?
    private var currentPhase: Phase = .offline
    private var epoch: UInt64 = 0
    private var task: UInt64 = 0
    private var submissionStarted = false
    private var operationKind: DemoOperationKind?
    private var stopObserving: (() -> Void)?
    private var cancelledOutputNeedsReview = false
    private var pendingConnect: Task<Void, Never>?
    private var pendingConnectEpoch: UInt64?
    private var currentDeviceState: DeviceStateSnapshot?
    private var currentResources: ResourceSnapshot?
    private var currentDiagnostic: ErrorDiagnostic?

    init(timeout: TimeInterval = 120, manager: PrinterManager = PrinterManager(), changed: @escaping (Phase, String) -> Void,
         log: @escaping (String) -> Void,
         completed: @escaping (DemoOperationKind, UInt64, Bool) -> Void = { _, _, _ in },
         stateChanged: @escaping (DeviceStateSnapshot?) -> Void = { _ in },
         detailsChanged: @escaping (ResourceSnapshot?, ErrorDiagnostic?) -> Void = { _, _ in }) {
        self.timeout = timeout; self.manager = manager
        self.changed = changed; self.log = log; self.completed = completed; self.stateChanged = stateChanged
        self.detailsChanged = detailsChanged

    }
    private func locked<T>(_ body: () throws -> T) rethrows -> T {
        lock.lock(); defer { lock.unlock() }; return try body()
    }
    var phase: Phase { locked { currentPhase } }
    var connectionEpoch: UInt64 { locked { epoch } }
    var deviceState: DeviceStateSnapshot? { locked { currentDeviceState } }
    var resourceState: ResourceSnapshot? { locked { currentResources } }
    var diagnosticState: ErrorDiagnostic? { locked { currentDiagnostic } }
    var recoveryNotice: String? {
        locked {
            cancelledOutputNeedsReview ?
                "曾有任务在已发送数据或发送量不确定时取消。重连不保证清除打印机残留状态；请按设备说明恢复并检查纸面。" : nil
        }
    }
    private func transition(_ phase: Phase, _ message: String) {
        currentPhase = phase
        if phase != .printing { timeoutTask?.cancel(); timeoutTask = nil }
        if phase == .connecting || phase == .stopping || phase == .offline || phase == .closed {
            currentDeviceState = nil; stateChanged(nil)
            currentResources = nil; detailsChanged(nil, currentDiagnostic)
        }
        changed(phase, message)
    }
    private func stale(_ version: UInt64) -> Bool { locked { epoch != version } }
    // Native admission can begin sending before submit returns the operation ID.
    private func beginSubmission(_ version: UInt64) -> Bool {
        locked {
            guard version == epoch, currentPhase == .printing else { return false }
            submissionStarted = true; return true
        }
    }

    func connect(identifier: UUID, options: BLEOptions) {
        let manager = manager
        connect { try SDKSession(manager: manager, identifier: identifier, options: options) }
    }
    private func prepareSession(_ factory: @escaping () throws -> DemoPrinterSession) async throws -> DemoPrinterSession {
        try await withCheckedThrowingContinuation { continuation in
            worker.async {
                do { continuation.resume(returning: try factory()) }
                catch { continuation.resume(throwing: error) }
            }
        }
    }
    func connect(_ factory: @escaping () throws -> DemoPrinterSession) {
        locked {
            guard currentPhase == .offline else { return }
            epoch &+= 1; let version = epoch
            transition(.connecting, "正在连接…")
            pendingConnectEpoch = version
            pendingConnect = Task { [self] in
                defer {
                    self.locked {
                        if self.pendingConnectEpoch == version {
                            self.pendingConnect = nil; self.pendingConnectEpoch = nil
                        }
                    }
                }
                do {
                    let candidate = try await self.prepareSession(factory)
                    let abandoned = self.locked { () -> Bool in
                        if version != self.epoch { return true }
                        self.session = candidate; return false
                    }
                    if abandoned { await candidate.close(); return }
                    let stop = try candidate.observe(queue: self.worker, update: { [weak self, weak candidate] event in
                        guard let self, let candidate else { return }; self.receive(event, selected: candidate, version: version)
                    }, state: { [weak self, weak candidate] _ in
                        guard let self, let candidate else { return }; self.refreshState(candidate, version: version)
                    })
                    self.locked { if version == self.epoch { self.stopObserving = stop } else { stop() } }
                    try await candidate.connect()
                    self.refreshState(candidate, version: version)
                    self.locked {
                        if version == self.epoch {
                            self.transition(.ready, "已连接；请设置画布和介质，准备预览后发送")
                            self.disconnectIdleLostConnection()
                        }
                    }
                } catch { self.fail(version, error) }
            }
        }
    }
    func printPrepared(_ image: DemoPreparedImage) {
        printLabel(image.configuration) { _ in image.pixels }
    }
    func printLabel(_ config: LabelConfiguration,
                    render: @escaping (@escaping () -> Bool) throws -> Data) {
        locked {
            guard currentPhase == .ready, let selected = session else { return }
            let version = epoch
            operationKind = .image
            task = 0; submissionStarted = false; scheduleDeadline(version)
            transition(.printing, "正在提交图片…")
            worker.async {
                do {
                    let pixels = try render { self.stale(version) }
                    guard self.beginSubmission(version) else { return }
                    let id = try selected.submit(pixels, config: config)
                    self.locked {
                        guard version == self.epoch else { return }
                        self.task = id; self.submissionStarted = false
                        self.log("task=\(id) copies=\(config.copies)")
                        self.transition(.printing, "任务 \(id) 已入队，正在发送…")
                    }
                } catch { self.fail(version, error) }
            }
        }
    }
    func mediaControl(_ mode: MediaMode?, profile: CpclProfile) {
        locked {
            guard currentPhase == .ready, let selected = session else { return }
            let version = epoch
            operationKind = .media(mode)
            task = 0; submissionStarted = false; scheduleDeadline(version)
            transition(.printing, mode == nil ? "正在发送标签定位指令…" : "正在发送介质设置…")
            worker.async {
                do {
                    guard self.beginSubmission(version) else { return }
                    let id = try selected.mediaControl(mode, profile: profile)
                    self.locked {
                        guard version == self.epoch else { return }
                        self.task = id; self.submissionStarted = false
                        self.log("task=\(id) CPCL control=\(mode?.rawValue ?? 0)；需实物确认生效")
                    }
                } catch { self.fail(version, error) }
            }
        }
    }
    func sendRaw(_ bytes: Data, copies: UInt32) {
        locked {
            guard currentPhase == .ready, let selected = session else { return }
            let version = epoch
            operationKind = .raw
            task = 0; submissionStarted = false; scheduleDeadline(version)
            transition(.printing, "正在提交 Raw 多份操作…")
            worker.async {
                do {
                    guard self.beginSubmission(version) else { return }
                    let id = try selected.raw(bytes, copies: copies)
                    self.locked {
                        guard version == self.epoch else { return }
                        self.task = id; self.submissionStarted = false
                        self.log("operation=\(id) Raw copies=\(copies)")
                    }
                } catch { self.fail(version, error) }
            }
        }
    }
    func cancel() {
        locked {
            guard currentPhase == .printing else { return }
            guard task != 0, let selected = session else {
                if submissionStarted {
                    cancelledOutputNeedsReview = true
                    if let notice = recoveryNotice { log(notice) }
                    disconnect(message: "提交已取消；部分数据可能已发送，请检查纸面")
                } else { disconnect(message: "准备已取消；不会自动重印") }
                return
            }
            let version = epoch, id = task
            log("取消任务 \(id)；已发送的数据无法撤回")
            control.async {
                if self.stale(version) { return }
                do { try selected.cancel(id) } catch { self.fail(version, error) }
            }
        }
    }
    func disconnect(message: String = "已断开；不会自动重印") {
        locked {
            guard currentPhase != .closed, currentPhase != .offline, currentPhase != .stopping else { return }
            epoch &+= 1; let version = epoch
            let selected = session; session = nil; stopObserving?(); stopObserving = nil; task = 0; submissionStarted = false; operationKind = nil
            let connecting = pendingConnect
            transition(.stopping, "正在断开…")
            Task {
                await selected?.close()
                await connecting?.value
                self.locked { if version == self.epoch { self.transition(.offline, message) } }
            }
        }
    }
    private func scheduleDeadline(_ version: UInt64) {
        timeoutTask?.cancel()
        let pending = DispatchWorkItem { [weak self] in
            guard let self else { return }
            self.locked {
                guard version == self.epoch, self.currentPhase == .printing else { return }
                self.log("任务超过演示应用 \(Int(self.timeout)) 秒截止时间；断开且不重放")
                self.disconnect(message: "任务超时；请检查出纸后手动重连")
            }
        }
        timeoutTask = pending; control.asyncAfter(deadline: .now() + timeout, execute: pending)
    }
    private func receive(_ event: DemoTaskUpdate, selected: DemoPrinterSession, version: UInt64) {
        refreshState(selected, version: version)
        locked {
            guard version == epoch, session === selected, currentPhase != .connecting else { return }
            log("task=\(event.id) state=\(event.state) completion=\(event.completion) " +
                "bytes=\(event.sentExact ? "" : "≥")\(event.current)/\(event.totalKnown ? String(event.total) : "unknown") error=\(event.error)")
            if !event.message.isEmpty { log("task=\(event.id) message=\(event.message)") }
            guard event.id == task else { return }
            if event.state < 4 {
                transition(.printing, "任务 \(event.id)：已发送\(event.sentExact ? "" : "至少 ")\(event.current) 字节" +
                    (event.totalKnown ? " / \(event.total)" : "；总字节数未知"))
            } else if event.state == 4 && event.completion == 1 {
                let kind = operationKind; operationKind = nil
                task = 0; transition(.ready, "DATA_SENT · 数据已发送，设置与出纸结果需实物确认")
                if let kind { completed(kind, version, true) }
            } else {
                let kind = operationKind; operationKind = nil
                if let kind { completed(kind, version, false) }
                if event.state == 6 && (event.current > 0 || !event.sentExact) {
                    cancelledOutputNeedsReview = true
                    if let notice = recoveryNotice { log(notice) }
                }
                let message = event.state == 6 ? "任务已取消；已发送数据无法撤回" :
                    "任务失败：\(PrinterSDKError(event.error, diagnostic: event.diagnostic).localizedDescription)"
                log(message); disconnect(message: message)
            }

            if version == epoch { disconnectIdleLostConnection() }
        }
    }
    private func disconnectIdleLostConnection() {
        // Drain an active operation's terminal metadata before releasing its session:
        // a cancelled send can disconnect the link while its output warning is still pending.
        guard currentPhase == .ready, currentDeviceState?.connectionState == .disconnected else { return }
        let code = currentDiagnostic?.errorCode ?? 2002
        let message = "连接已断开：\(PrinterSDKError(code == 0 ? 2002 : code, diagnostic: currentDiagnostic).localizedDescription)"
        log(message); disconnect(message: message)
    }
    private func refreshState(_ selected: DemoPrinterSession, version: UInt64) {
        let resources = try? selected.resourceSnapshot()
        let diagnostic = try? selected.diagnostic()
        locked {
            guard version == epoch, session === selected else { return }
            if currentResources != resources || currentDiagnostic != diagnostic {
                currentResources = resources; currentDiagnostic = diagnostic
                detailsChanged(resources, diagnostic)
            }
        }
        do {
            let value = try selected.stateSnapshot()
            locked {
                guard version == epoch, session === selected else { return }
                if currentDeviceState != value {
                    currentDeviceState = value; stateChanged(value)
                }
                disconnectIdleLostConnection()
            }
        } catch {
            locked {
                guard version == epoch, session === selected, currentDeviceState != nil else { return }
                currentDeviceState = nil; stateChanged(nil)
            }
        }
    }
    private func fail(_ version: UInt64, _ error: Error) {
        locked {
            guard version == epoch else { return }
            if let diagnostic = (error as? PrinterSDKError)?.diagnostic {
                currentDiagnostic = diagnostic; detailsChanged(currentResources, diagnostic)
            }
            let message = "操作失败：\(error.localizedDescription)"
            log(message); disconnect(message: message)
        }
    }
    func close() {
        locked {
            guard currentPhase != .closed else { return }
            epoch &+= 1
            let selected = session; session = nil; stopObserving?(); stopObserving = nil
            let connecting = pendingConnect, manager = manager
            timeoutTask?.cancel(); timeoutTask = nil
            transition(.closed, "会话已关闭")
            Task {
                async let released: Void = selected?.close() ?? ()
                await manager.close()
                await released
                await connecting?.value
            }
        }
    }
    deinit {
        timeoutTask?.cancel(); stopObserving?()
        let selected = session, manager = manager, connecting = pendingConnect
        Task {
            async let released: Void = selected?.close() ?? ()
            await manager.close()
            await released
            await connecting?.value
        }
    }
}
