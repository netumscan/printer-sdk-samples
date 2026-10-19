// SPDX-License-Identifier: MIT
// Copyright (c) 2026 NETUM
import Combine
import Foundation
import PrinterSDK

/// MainActor owns UI/lifecycle. Manager performs connection/close IO off the UI thread.
@MainActor
final class QuickStartModel: ObservableObject {
    @Published var devices: [DiscoveredPrinter] = []
    @Published var selected: UUID?
    @Published var scanning = false
    @Published var busy = false
    @Published var connected = false
    @Published private(set) var hasSession = false
    @Published var printing = false
    @Published var dpi = "203"
    @Published var widthDots = "864"
    @Published var status = "尚未连接 · SDK 0.3.0-preview.2 · ABI 3/r1"
    @Published var deviceStatus = "连接=未连接；工作=空闲；等待队列=0；硬件=未知"
    private let scanner = BLEScanner()
    private let manager = PrinterManager()
    private var printer: MobilePrinter?
    private var profile: CpclProfile?
    private var operation: PrintOperation?
    private var stateToken: ObservationSubscription?
    private var completionToken: ObservationSubscription?
    private var epoch: UInt64 = 0
    private var scanEpoch: UInt64 = 0
    private var connectionGeneration: UInt64 = 0
    private var cancelPending = false
    private var preparation: Task<PrintOperation, Error>?

    func scan() {
        guard !connected, !busy, !scanning else { return }
        scanEpoch &+= 1; let version = scanEpoch
        scanning = true; devices = []; selected = nil; status = "扫描中…"
        scanner.scan(seconds: 10) { [weak self] result in
            Task { @MainActor [weak self] in
                guard let self, version == self.scanEpoch else { return }
                self.scanning = false
                switch result {
                case .success(let devices):
                    self.devices = devices
                    self.status = "扫描完成：\(devices.count) 个候选设备，请手动选择"
                case .failure(let error): self.status = error.localizedDescription
                }
            }
        }
    }
    func stopScan() { scanEpoch &+= 1; scanner.stop(); scanning = false }

    func connect() {
        guard !connected, !busy, !scanning, printer == nil, let identifier = selected else { return }
        let selectedProfile: CpclProfile
        let options: BLEOptions
        do {
            guard let dpi = UInt32(dpi), let width = UInt32(widthDots) else { throw PrinterSDKError(1001) }
            selectedProfile = try CpclProfile(dpi: dpi, widthDots: width, maxLabelHeightDots: 65535, form: false)
            options = try M90Profile.bleOptions()
        } catch { status = error.localizedDescription; return }
        epoch &+= 1; let version = epoch
        connectionGeneration = 0; busy = true; status = "正在连接…"
        do {
            let created = try installSession(identifier: identifier, options: options, profile: selectedProfile)
            Task { [weak self, manager] in
                do {
                    try await manager.connect(created)
                    guard let self, self.epoch == version, self.printer === created else { return }
                    let state = try created.stateSnapshot()
                    guard state.connectionState == .connected else { throw PrinterSDKError(2002) }
                    self.connectionGeneration = state.connectionGeneration
                    self.deviceStatus = Self.stateMessage(state)
                    self.busy = false; self.connected = true; self.status = "已连接，请手动提交任务"
                } catch {
                    guard let self, self.epoch == version else { return }
                    self.disconnect(); self.status = error.localizedDescription
                }
            }
        } catch { disconnect(); status = error.localizedDescription }
    }

    /// Allocate and subscribe locally. Connection IO is a separate explicit manager action.
    @discardableResult
    func installSession(identifier: UUID, options: BLEOptions, profile: CpclProfile) throws -> MobilePrinter {
        guard printer == nil else { throw PrinterSDKError(1002) }
        let created = try manager.printer(bleIdentifier: identifier, options: options)
        printer = created; self.profile = profile; hasSession = true
        let version = epoch
        stateToken = try created.observeState { [weak self] state in
            Task { @MainActor [weak self] in
                guard let self, self.epoch == version,
                      state.connectionGeneration >= self.connectionGeneration else { return }
                self.connectionGeneration = state.connectionGeneration
                if state.connectionState != .connected { self.connected = false }
                self.deviceStatus = Self.stateMessage(state)
            }
        }
        return created
    }

    func printSample() {
        guard connected, !busy, !printing, let printer, let profile else { return }
        let version = epoch
        printing = true; cancelPending = false; status = "正在准备测试图…"
        let preparation = Task.detached {
            let submitted = try Self.prepareAndSubmit({
                let width = min(profile.widthDots, 384), height: UInt32 = 64
                let pixels = Self.testImage(width: Int(width), height: Int(height))
                try Task.checkCancellation()
                let image = try PrinterImage(pixels: pixels, width: width, height: height)
                let builder = PrintJob.Builder(label: LabelJobOptions(widthDots: width, heightDots: height, dpi: profile.dpi))
                try builder.addImage(image)
                return try builder.build()
            }, submit: { job in try printer.submit(job, profile: profile, timeoutMilliseconds: 30000) })
            if Task.isCancelled { try? submitted.cancel() }
            return submitted
        }
        self.preparation = preparation
        Task { [weak self] in
            do {
                let submitted = try await preparation.value
                guard let self, self.epoch == version, self.printer === printer else {
                    try? submitted.cancel(); submitted.close(); return
                }
                self.preparation = nil
                self.operation = submitted
                do {
                    self.completionToken = try submitted.onComplete { [weak self] result in
                        Task { @MainActor [weak self] in
                            guard let self, self.epoch == version, self.operation?.id == result.snapshot.id else { return }
                            self.completionToken?.cancel(); self.completionToken = nil
                            self.operation?.close(); self.operation = nil; self.printing = false
                            self.status = Self.terminalMessage(state: result.snapshot.state, completion: result.snapshot.completion,
                                errorCode: result.snapshot.errorCode, bytesSent: result.snapshot.bytesSent)
                        }
                    }
                    if self.cancelPending { try submitted.cancel() }
                    self.status = "任务已入队；状态和完成结果由回调更新"
                } catch {
                    try? submitted.cancel(); submitted.close(); self.operation = nil
                    self.completionToken?.cancel(); self.completionToken = nil
                    throw error
                }
            } catch {
                guard let self, self.epoch == version else { return }
                self.preparation = nil
                self.printing = false; self.status = error.localizedDescription
            }
        }
    }
    func cancel() {
        guard printing else { return }
        cancelPending = true
        preparation?.cancel()
        do { try operation?.cancel(); status = "已请求取消；已发送的数据无法撤回" }
        catch { status = error.localizedDescription }
    }
    func refreshConnectionState() {
        guard let printer else { return }
        do { deviceStatus = Self.stateMessage(try printer.stateSnapshot()) }
        catch { status = error.localizedDescription }
    }
    func disconnect() {
        preparation?.cancel(); preparation = nil
        stopScan(); epoch &+= 1; let version = epoch, previous = printer
        printer = nil; profile = nil; hasSession = false; connected = false; printing = false; cancelPending = false
        stateToken?.cancel(); stateToken = nil
        completionToken?.cancel(); completionToken = nil
        try? operation?.cancel(); operation?.close(); operation = nil
        busy = previous != nil; status = "正在释放会话…"
        deviceStatus = "连接=未连接；工作=未知；等待队列=未知；硬件=未知"
        Task { [weak self, manager] in
            if let previous { await manager.release(previous) }
            guard let self, self.epoch == version else { return }
            self.busy = false; self.status = "已断开；不会自动重连或重放"
        }
    }
    func background() { disconnect() }
    nonisolated static func prepareAndSubmit<Job: Sendable, Output: Sendable>(
        _ prepare: @Sendable () throws -> Job, submit: @Sendable (Job) throws -> Output
    ) throws -> Output {
        try Task.checkCancellation()
        let job = try prepare()
        try Task.checkCancellation()
        return try submit(job)
    }
    nonisolated static func testImage(width: Int, height: Int) -> Data {
        var pixels = Data(repeating: 255, count: width * height)
        for y in 0..<height { for x in 0..<width where (x / 16 + y / 16) % 2 == 0 { pixels[y * width + x] = 0 } }
        return pixels
    }
    nonisolated static func terminalMessage(state: UInt32, completion: UInt32, errorCode: Int32, bytesSent: UInt64) -> String {
        if errorCode != 0 { return PrinterSDKError(errorCode).localizedDescription }
        if state == 4 && completion == 1 { return "DATA_SENT：\(bytesSent) bytes；请检查实际出纸" }
        return "任务已终止：state=\(state)，completion=\(completion)，已发送 \(bytesSent) bytes"
    }
    private static func stateMessage(_ value: DeviceStateSnapshot) -> String {
        "连接=\(value.connectionState)；工作=\(value.workState)；等待队列=\(value.queuedCount)；硬件=未知"
    }
    deinit {
        preparation?.cancel()
        scanner.stop(); stateToken?.cancel(); completionToken?.cancel()
        let previous = operation, manager = manager
        Task { try? previous?.cancel(); previous?.close(); await manager.close() }
    }
}
