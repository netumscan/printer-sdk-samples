// SPDX-License-Identifier: MIT
// Copyright (c) 2026 NETUM
import Foundation
import Combine
// The SDK predates Swift 6 Sendable annotations; its documented lifetime locks
// and our worker/control queues govern access in this Swift 5 sample.
@preconcurrency import PrinterSDK

/// UI state belongs to the main actor; all blocking SDK work uses a worker queue.
@MainActor
final class QuickStartModel: ObservableObject {
    @Published var devices: [DiscoveredPrinter] = []
    @Published var selected: UUID?
    @Published var scanning = false
    @Published var busy = false
    @Published var connected = false
    @Published var printing = false
    @Published var dpi = "203"
    @Published var widthDots = "864"
    @Published var status = "尚未连接 · SDK 0.2.0-preview.2"

    private let scanner = BLEScanner()
    private let worker = DispatchQueue(label: "PrinterQuickStart.worker")
    private let control = DispatchQueue(label: "PrinterQuickStart.control")
    private var printer: MobilePrinter?
    private var operation: PrintOperation?
    private var generation: UInt64 = 0
    private var scanGeneration: UInt64 = 0
    private var configuredWidth: UInt32 = 0

    func scan() {
        guard !connected, !busy, !scanning else { return }
        scanGeneration &+= 1
        let version = scanGeneration
        scanning = true; devices = []; selected = nil; status = "扫描中…"
        scanner.scan(seconds: 10) { [weak self] result in
            guard let self, version == self.scanGeneration else { return }
            self.scanning = false
            switch result {
            case .success(let devices):
                self.devices = devices
                self.status = "扫描完成：\(devices.count) 个候选设备，请手动选择"
            case .failure(let error): self.status = error.localizedDescription
            }
        }
    }

    func stopScan() {
        scanGeneration &+= 1; scanner.stop(); scanning = false
        status = "扫描已停止"
    }

    func connect() {
        guard !connected, !busy, !scanning, let identifier = selected else { return }
        let profile: CpclProfile
        let options: BLEOptions
        do {
            guard let dpi = UInt32(dpi), let width = UInt32(widthDots) else {
                throw PrinterSDKError(1001)
            }
            profile = try CpclProfile(dpi: dpi, widthDots: width, maxLabelHeightDots: 65535, form: false)
            options = try M90Profile.bleOptions()
        } catch { status = error.localizedDescription; return }
        generation &+= 1
        let version = generation
        busy = true; status = "正在连接…"
        worker.async { [weak self] in
            var candidate: MobilePrinter?
            do {
                let created = try MobilePrinter(bleIdentifier: identifier, options: options, profile: profile)
                candidate = created
                try created.connect(timeoutMilliseconds: 5000)
                DispatchQueue.main.async { [weak self] in
                    guard let self, self.generation == version else {
                        // Destruction can block and must stay off the application thread.
                        DispatchQueue.global().async { created.close() }; return
                    }
                    self.printer = created; self.configuredWidth = profile.widthDots
                    self.busy = false; self.connected = true; self.status = "已连接，请手动提交任务"
                }
            } catch {
                candidate?.close()
                DispatchQueue.main.async { [weak self] in
                    guard let self, self.generation == version else { return }
                    self.busy = false; self.status = error.localizedDescription
                }
            }
        }
    }

    func printSample() {
        guard connected, !busy, !printing, let printer else { return }
        let version = generation, width = min(configuredWidth, 384)
        printing = true; status = "正在提交测试图…"
        worker.async { [weak self] in
            do {
                // One immutable image and one job; SDK owns chunking and device IO ordering.
                let pixels = Self.testImage(width: Int(width), height: 64)
                let submitted = try printer.submitGray8(pixels, width: width, height: 64,
                    canvasWidthDots: width, timeoutMilliseconds: 120000)
                DispatchQueue.main.async { [weak self] in
                    guard let self, self.generation == version else {
                        try? submitted.cancel(); submitted.close(); return
                    }
                    self.operation = submitted; self.poll(submitted, version: version)
                }
            } catch {
                DispatchQueue.main.async { [weak self] in
                    guard let self, self.generation == version else { return }
                    self.printing = false; self.status = error.localizedDescription
                }
            }
        }
    }

    nonisolated static func testImage(width: Int, height: Int) -> Data {
        var pixels = Data(repeating: 255, count: width * height)
        for y in 0..<height {
            for x in 0..<width where x < 2 || y < 2 || x >= width - 2 || y >= height - 2 || (x / 16 + y / 16) % 2 == 0 {
                pixels[y * width + x] = 0
            }
        }
        return pixels
    }

    private func poll(_ submitted: PrintOperation, version: UInt64) {
        worker.asyncAfter(deadline: .now() + 0.2) { [weak self] in
            do {
                let snapshot = try submitted.snapshot()
                if snapshot.isTerminal { submitted.close() }
                DispatchQueue.main.async { [weak self] in
                    guard let self, self.generation == version, self.operation?.id == submitted.id else { return }
                    if snapshot.isTerminal {
                        self.operation = nil; self.printing = false
                        self.status = Self.terminalMessage(state: snapshot.state, completion: snapshot.completion,
                            errorCode: snapshot.errorCode, bytesSent: snapshot.bytesSent)
                    } else {
                        self.status = "任务 \(snapshot.id)：state=\(snapshot.state)，已发送 \(snapshot.bytesSent) bytes"
                        self.poll(submitted, version: version)
                    }
                }
            } catch {
                submitted.close()
                DispatchQueue.main.async { [weak self] in
                    guard let self, self.generation == version else { return }
                    self.operation = nil; self.printing = false; self.status = error.localizedDescription
                }
            }
        }
    }

    nonisolated static func terminalMessage(state: UInt32, completion: UInt32, errorCode: Int32, bytesSent: UInt64) -> String {
        if errorCode != 0 { return PrinterSDKError(errorCode).localizedDescription }
        // ABI 2/r10: COMPLETED=4 and DATA_SENT=1 are independent observations.
        if state == 4 && completion == 1 { return "DATA_SENT：\(bytesSent) bytes；请检查实际出纸" }
        return "任务已终止：state=\(state)，completion=\(completion)，已发送 \(bytesSent) bytes；请检查设备"
    }

    func cancel() {
        guard printing else { return }
        guard let operation else {
            // A submission may still be being prepared on the worker.
            disconnect(); status = "正在取消准备并释放会话；已发送的数据无法撤回"; return
        }
        let version = generation
        status = "已请求取消；已发送的数据无法撤回"
        control.async { [weak self] in
            do { try operation.cancel() }
            catch {
                DispatchQueue.main.async { [weak self] in
                    guard let self, self.generation == version, self.operation?.id == operation.id else { return }
                    self.status = error.localizedDescription
                }
            }
        }
    }

    func refreshConnectionState() {
        guard let printer, !busy else { return }
        let version = generation
        worker.async { [weak self] in
            let message: String
            do { message = "本地连接状态：\(try printer.connectionState())；不代表实时链路或打印机状态" }
            catch { message = error.localizedDescription }
            DispatchQueue.main.async { [weak self] in
                guard let self, self.generation == version else { return }
                self.status = message
            }
        }
    }

    func disconnect() {
        stopScan(); generation &+= 1
        let version = generation, previous = printer, submitted = operation
        printer = nil; operation = nil; connected = false; printing = false; busy = true
        status = "正在释放会话…"
        // Cancellation is independent of the queue that may currently be connecting/submitting.
        control.async { try? submitted?.cancel() }
        worker.async { [weak self] in
            previous?.close(); submitted?.close()
            DispatchQueue.main.async { [weak self] in
                guard let self, self.generation == version else { return }
                self.busy = false; self.status = "已断开；不会自动重连或重放"
            }
        }
    }

    func background() { disconnect() }

    deinit {
        scanner.stop()
        let previous = printer, submitted = operation
        worker.async { try? submitted?.cancel(); previous?.close(); submitted?.close() }
    }
}
