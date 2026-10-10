import Foundation
import PrinterSDK

struct DemoTaskUpdate {
    let id: UInt64
    let state, completion: UInt32
    let current, total: UInt64
    let error: Int32
    var totalKnown = true
    var sentExact = true
}

protocol DemoPrinterSession: AnyObject {
    func connect() throws
    func submit(_ pixels: Data, config: LabelConfiguration) throws -> UInt64
    func mediaControl(_ mode: MediaMode?) throws -> UInt64
    func raw(_ bytes: Data, copies: UInt32) throws -> UInt64
    func poll() throws -> [DemoTaskUpdate]
    func cancel(_ id: UInt64) throws
    func close()
}

extension DemoPrinterSession {
    func mediaControl(_ mode: MediaMode?) throws -> UInt64 { throw PrinterSDKError(1004) }
    func raw(_ bytes: Data, copies: UInt32) throws -> UInt64 { throw PrinterSDKError(1004) }
}

final class SDKSession: DemoPrinterSession {
    private let printer: MobilePrinter
    private let connection: DemoConnectionConfiguration
    private let lock = NSLock()
    private var operations: [UInt64: PrintOperation] = [:]
    init(identifier: UUID, options: BLEOptions, config: DemoConnectionConfiguration) throws {
        connection = config
        if config.kind == .label {
            printer = try MobilePrinter(bleIdentifier: identifier, options: options, profile: config.profile())
        } else {
            printer = try MobilePrinter(bleIdentifier: identifier, options: options, profile: config.receiptProfile())
        }
    }
    private func retain(_ operation: PrintOperation) -> UInt64 {
        lock.lock(); defer { lock.unlock() }; operations[operation.id] = operation; return operation.id
    }
    func connect() throws { try printer.connect(timeoutMilliseconds: 10000) }
    func submit(_ pixels: Data, config: LabelConfiguration) throws -> UInt64 {
        guard connection.matches(config) else { throw PrinterSDKError(1001) }
        let operation: PrintOperation
        if config.kind == .label {
            operation = try printer.submitGray8(pixels, width: UInt32(config.width), height: UInt32(config.height),
                copies: UInt32(config.copies), trailingBlankDots: UInt32(config.trailingBlankDots),
                canvasWidthDots: UInt32((config.width + 7) / 8 * 8), originXDots: UInt32(config.xOffsetDots), form: config.form)
        } else {
            operation = try printer.submitReceiptGray8(pixels, width: UInt32(config.width), height: UInt32(config.height),
                copies: UInt32(config.copies), maxTotalLengthDots: config.receiptBudget,
                trailingBlankDots: UInt32(config.trailingBlankDots))
        }
        return retain(operation)
    }
    func mediaControl(_ mode: MediaMode?) throws -> UInt64 {
        if let mode { return retain(try printer.setMediaMode(mode)) }
        return retain(try printer.feedToNextLabel())
    }
    func raw(_ bytes: Data, copies: UInt32) throws -> UInt64 { retain(try printer.submitRaw(bytes, copies: copies)) }
    func poll() throws -> [DemoTaskUpdate] {
        lock.lock(); defer { lock.unlock() }
        var updates: [DemoTaskUpdate] = []
        for (id, operation) in operations {
            let value = try operation.snapshot()
            updates.append(DemoTaskUpdate(id: value.id, state: value.state, completion: value.completion,
                current: value.bytesSent, total: value.bytesTotal ?? 0, error: value.errorCode,
                totalKnown: value.bytesTotal != nil, sentExact: value.bytesSentExact))
            if value.isTerminal { operations.removeValue(forKey: id); operation.close() }
        }
        return updates
    }
    func cancel(_ id: UInt64) throws {
        lock.lock(); defer { lock.unlock() }
        if let operation = operations[id] { try operation.cancel() }
    }
    func close() {
        printer.close()
        lock.lock(); let held = operations; operations.removeAll(); lock.unlock()
        for operation in held.values { operation.close() }
    }
}

/// App orchestration only. All printer IO remains in the SDK's device executor.
enum DemoOperationKind { case image, raw, media(MediaMode?) }

final class PrinterController {
    enum Phase { case offline, connecting, ready, printing, stopping, closed }
    private let lock = NSRecursiveLock()
    private let worker = DispatchQueue(label: "PrinterDemo.worker")
    private let control = DispatchQueue(label: "PrinterDemo.control")
    private let changed: (Phase, String) -> Void
    private let log: (String) -> Void
    private let completed: (DemoOperationKind, UInt64, Bool) -> Void
    private let timeout: TimeInterval
    private var timer: DispatchSourceTimer?
    private var session: DemoPrinterSession?
    private var currentPhase: Phase = .offline
    private var epoch: UInt64 = 0
    private var task: UInt64 = 0
    private var operationKind: DemoOperationKind?
    private var deadline: TimeInterval = 0
    private var pollQueued = false
    private var cancelledOutputNeedsReview = false

    init(timeout: TimeInterval = 120, changed: @escaping (Phase, String) -> Void,
         log: @escaping (String) -> Void,
         completed: @escaping (DemoOperationKind, UInt64, Bool) -> Void = { _, _, _ in }) {
        self.timeout = timeout; self.changed = changed; self.log = log; self.completed = completed
        let timer = DispatchSource.makeTimerSource(queue: control)
        timer.schedule(deadline: .now() + .milliseconds(100), repeating: .milliseconds(100))
        timer.setEventHandler { [weak self] in self?.poll() }
        self.timer = timer; timer.resume()
    }
    private func locked<T>(_ body: () throws -> T) rethrows -> T {
        lock.lock(); defer { lock.unlock() }; return try body()
    }
    var phase: Phase { locked { currentPhase } }
    var connectionEpoch: UInt64 { locked { epoch } }
    var recoveryNotice: String? {
        locked {
            cancelledOutputNeedsReview ?
                "曾有任务在已发送数据或发送量不确定时取消。重连不保证清除打印机残留状态；请按设备说明恢复并检查纸面。" : nil
        }
    }
    private func transition(_ phase: Phase, _ message: String) {
        currentPhase = phase; changed(phase, message)
    }
    private func stale(_ version: UInt64) -> Bool { locked { epoch != version } }

    func connect(_ factory: @escaping () throws -> DemoPrinterSession) {
        locked {
            guard currentPhase == .offline else { return }
            epoch &+= 1; let version = epoch
            transition(.connecting, "正在连接…")
            worker.async {
                do {
                    let candidate = try factory()
                    let abandoned = self.locked { () -> Bool in
                        if version != self.epoch { return true }
                        self.session = candidate; return false
                    }
                    if abandoned { candidate.close(); return }
                    try candidate.connect()
                    self.locked {
                        if version == self.epoch { self.transition(.ready, "已连接；请设置画布和介质，准备预览后发送") }
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
            task = 0; deadline = ProcessInfo.processInfo.systemUptime + timeout
            transition(.printing, "正在提交图片…")
            worker.async {
                do {
                    let pixels = try render { self.stale(version) }
                    if self.stale(version) { return }
                    let id = try selected.submit(pixels, config: config)
                    self.locked {
                        guard version == self.epoch else { return }
                        self.task = id
                        self.log("task=\(id) copies=\(config.copies)")
                        self.transition(.printing, "任务 \(id) 已入队，正在发送…")
                    }
                } catch { self.fail(version, error) }
            }
        }
    }
    func mediaControl(_ mode: MediaMode?) {
        locked {
            guard currentPhase == .ready, let selected = session else { return }
            let version = epoch
            operationKind = .media(mode)
            task = 0; deadline = ProcessInfo.processInfo.systemUptime + timeout
            transition(.printing, mode == nil ? "正在发送标签定位指令…" : "正在发送介质设置…")
            worker.async {
                do {
                    if self.stale(version) { return }
                    let id = try selected.mediaControl(mode)
                    self.locked {
                        guard version == self.epoch else { return }
                        self.task = id
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
            task = 0; deadline = ProcessInfo.processInfo.systemUptime + timeout
            transition(.printing, "正在提交 Raw 多份操作…")
            worker.async {
                do {
                    if self.stale(version) { return }
                    let id = try selected.raw(bytes, copies: copies)
                    self.locked {
                        guard version == self.epoch else { return }
                        self.task = id; self.log("operation=\(id) Raw copies=\(copies)")
                    }
                } catch { self.fail(version, error) }
            }
        }
    }
    func cancel() {
        locked {
            guard currentPhase == .printing else { return }
            guard task != 0, let selected = session else {
                disconnect(message: "准备已取消；不会自动重印"); return
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
            let selected = session; session = nil; task = 0; operationKind = nil
            transition(.stopping, "正在断开…")
            control.async {
                selected?.close()
                self.locked { if version == self.epoch { self.transition(.offline, message) } }
            }
        }
    }
    private func poll() {
        // Poll on worker so event draining cannot race submit's task-ID publication.
        let shouldDrain = locked { () -> Bool in
            if currentPhase == .printing && ProcessInfo.processInfo.systemUptime >= deadline {
                log("任务超过演示应用 \(Int(timeout)) 秒截止时间；断开且不重放")
                disconnect(message: "任务超时；请检查出纸后手动重连")
                return false
            }
            guard !pollQueued, currentPhase == .ready || currentPhase == .printing else { return false }
            pollQueued = true; return true
        }
        if shouldDrain {
            worker.async { [weak self] in
                guard let self else { return }
                self.drain(); self.locked { self.pollQueued = false }
            }
        }
    }
    private func drain() {
        let snapshot = locked { () -> (DemoPrinterSession, UInt64)? in
            guard let selected = session, currentPhase == .ready || currentPhase == .printing else { return nil }
            return (selected, epoch)
        }
        guard let (selected, version) = snapshot else { return }
        do {
            for event in try selected.poll() {
                locked {
                    guard version == epoch else { return }
                    log("task=\(event.id) state=\(event.state) completion=\(event.completion) " +
                        "bytes=\(event.sentExact ? "" : "≥")\(event.current)/\(event.totalKnown ? String(event.total) : "unknown") error=\(event.error)")
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
                            "任务失败：\(PrinterSDKError(event.error).localizedDescription)"
                        log(message); disconnect(message: message)
                    }
                }
            }
        } catch { fail(version, error) }
    }
    private func fail(_ version: UInt64, _ error: Error) {
        locked {
            guard version == epoch else { return }
            let message = "操作失败：\(error.localizedDescription)"
            log(message); disconnect(message: message)
        }
    }
    func close() {
        locked {
            guard currentPhase != .closed else { return }
            epoch &+= 1
            let selected = session; session = nil
            timer?.cancel(); timer = nil
            transition(.closed, "会话已关闭")
            control.async { selected?.close() }
        }
    }
    deinit {
        timer?.cancel()
        let selected = session
        control.async { selected?.close() }
    }
}
