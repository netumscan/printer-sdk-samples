// SPDX-License-Identifier: MIT
// Copyright (c) 2026 NETUM
import Combine
import Foundation
import CoreGraphics
import ImageIO
import XLPrinterSDK

@MainActor
final class QuickStartModel: ObservableObject, PrinterDelegate {
    enum Phase { case offline, connecting, ready, printing, disconnecting, closed }
    enum ResultAction { case connect, print }
    @Published var devices: [DiscoveredPrinter] = []
    @Published var selected: UUID?
    @Published private(set) var phase = Phase.offline
    @Published private(set) var scanning = false
    @Published private(set) var scanStopping = false
    @Published private(set) var status = "请选择 BLE 设备"
    @Published private(set) var connectionResult: ManagerResult<ConnectionInfo>?
    @Published private(set) var lastResult: ManagerResult<Void>?
    @Published private(set) var lastAction: ResultAction?
    @Published private(set) var cleanupResult: ManagerResult<Void>?
    @Published private(set) var closeResult: ManagerResult<Void>?
    @Published private(set) var disconnectionResult: ManagerResult<Void>?
    @Published private(set) var scanResult: ManagerResult<[DiscoveredPrinter]>?
    private let manager: PrinterManager
    @Published private var foreground = true
    private var connection: RequestID?
    private var connectingRequest: RequestID?
    private var active: RequestID?
    private var scanRequest: RequestID?
    private var cleanupMessages: [RequestID: String] = [:]
    var canSelectDevice: Bool { foreground && phase == .offline && active == nil && connectingRequest == nil }
    var canConnect: Bool { canSelectDevice && !scanning }
    var canScan: Bool { canConnect || (foreground && scanning && !scanStopping) }
    struct DeviceRow: Identifiable, Equatable {
        let id: UUID
        let title: String
        let signal: String
        let isSelected: Bool
    }
    var deviceRows: [DeviceRow] {
        devices.map { DeviceRow(id: $0.identifier, title: Self.deviceTitle($0),
            signal: $0.signalStrength.map { "\($0) dBm" } ?? "信号未知", isSelected: $0.identifier == selected) }
    }
    var scanMessage: String {
        if scanStopping { return "正在停止扫描；保留本轮已发现的 \(devices.count) 个候选" }
        if scanning { return "扫描中 · 已发现 \(devices.count) 个候选" }
        return devices.isEmpty ? "尚无候选设备，点击扫描开始" : "\(devices.count) 个候选设备；请选择后手动连接"
    }
    var canPrint: Bool { phase == .ready && active == nil && connectingRequest == nil }
    var busy: Bool { phase == .connecting || phase == .disconnecting }
    var connected: Bool { phase == .ready || phase == .printing }
    var printing: Bool { phase == .printing }
    var canCancel: Bool { phase == .connecting || phase == .printing }
    init(manager: PrinterManager = PrinterManager()) { self.manager = manager; manager.delegate = self }

    func scan() {
        guard canConnect else { return }
        devices = []; selected = nil; scanResult = nil; scanStopping = false; scanning = true; status = "扫描中…"
        scanRequest = manager.scanBLE()
    }
    func selectDevice(_ identifier: UUID?) {
        guard canSelectDevice, identifier == nil || devices.contains(where: { $0.identifier == identifier }) else { return }
        selected = identifier
    }
    func printerManager(_ manager: PrinterManager, didDiscover device: DiscoveredPrinter, scanID: RequestID) {
        guard scanRequest == scanID, scanning, !scanStopping, foreground, phase == .offline else { return }
        if let index = devices.firstIndex(where: { $0.identifier == device.identifier }) { devices[index] = device }
        else { devices.append(device) }
    }
    func printerManager(_ manager: PrinterManager, didFinishScanning result: ManagerResult<[DiscoveredPrinter]>) {
        guard scanRequest == result.requestID else { return }
        scanRequest = nil; scanning = false; scanStopping = false
        guard phase != .closed else { return }
        scanResult = result
        guard phase == .offline else { return }
        if result.isSuccess {
            devices = result.value ?? []
            if !devices.contains(where: { $0.identifier == self.selected }) { selected = nil }
        }
        status = result.isSuccess ? "扫描完成，请手动选择设备" : result.message
    }
    func stopScan() {
        guard let scanRequest, !scanStopping else { return }
        scanStopping = true; status = "正在停止扫描…"
        manager.cancel(scanRequest)
    }
    func foregroundEntry() { foreground = true }
    func background() { foreground = false; disconnect() }
    func connect() {
        guard canConnect, let device = devices.first(where: { $0.identifier == selected }) else { return }
        phase = .connecting; status = "正在连接…"
        connection = manager.connectBle(device: device)
        connectingRequest = connection
    }
    func printerManager(_ manager: PrinterManager, didFinishConnecting result: ManagerResult<ConnectionInfo>) {
        guard connectingRequest == result.requestID else { return }
        connectingRequest = nil
        guard phase != .closed else { return }
        connectionResult = result; lastAction = .connect
        guard connection == result.requestID, phase == .connecting else { return }
        if result.isSuccess { phase = .ready; status = "已连接；请手动打印" }
        else {
            let failure = "\(result.message)（\(result.code)）"
            connection = nil; phase = .disconnecting; status = failure
            cleanupMessages[manager.disconnect()] = failure
        }
    }
    func printerManager(_ manager: PrinterManager, didLoseConnection result: ManagerResult<Void>) {
        guard connection == result.requestID else { return }
        connection = nil; phase = .offline; disconnectionResult = result
        status = "连接意外断开（\(result.code)）；请手动重连"
    }
    func printSample() {
        guard canPrint else { return }
        do {
            let bytes = try Self.testEncodedImage()
            phase = .printing; status = "请求已登记；等待最终回调"
            active = manager.printEncodedImage(bytes, widthMm: 48, heightMm: 8)
        } catch { status = error.localizedDescription }
    }
    func printerManager(_ manager: PrinterManager, didFinishPrinting result: ManagerResult<Void>) {
        guard active == result.requestID, phase != .closed else { return }
        active = nil; lastResult = result; lastAction = .print
        if phase == .printing { phase = connection == nil ? .offline : .ready }
        status = "请求已结束；请检查完成级别和实际纸面"
    }
    func cancel() {
        if let request = phase == .connecting ? connection : active {
            manager.cancel(request); status = "已请求取消；等待终态，已发送数据不能撤回"
        }
    }
    func disconnect() {
        guard phase != .closed, phase != .disconnecting else { return }
        if scanning { scanStopping = true }
        connection = nil; phase = .disconnecting; status = "正在断开并清理…"
        cleanupMessages[manager.disconnect()] = "已断开；不会自动重连或重放"
    }
    func printerManager(_ manager: PrinterManager, didFinishDisconnecting result: ManagerResult<Void>) {
        guard let message = cleanupMessages.removeValue(forKey: result.requestID), phase == .disconnecting else { return }
        cleanupResult = result; phase = .offline; status = message
    }
    func close() {
        guard phase != .closed else { return }
        manager.close()
        connection = nil; scanRequest = nil; scanning = false; scanStopping = false; phase = .closed
    }
    func printerManager(_ manager: PrinterManager, didFinishClosing result: ManagerResult<Void>) { closeResult = result }
    deinit { manager.close() }

    var resultDescription: String {
        if lastAction == .connect, let result = connectionResult { return "连接\n" + Self.describe(result) }
        if let result = lastResult { return "打印\n" + Self.describe(result) }
        return "尚无连接/打印结果"
    }
    static func describe<Value>(_ result: ManagerResult<Value>) -> String {
        let level: String
        switch result.completion { case .none: level = "NONE"; case .dataSent: level = "DATA_SENT"; case .deviceConfirmed: level = "DEVICE_CONFIRMED" }
        return "\(result.outcome) · code=\(result.code) · \(level)\n\(result.message)\n" +
            (result.outputMayHaveOccurred ? "可能已有输出；取消不能撤回" : "未报告可能输出")
    }
    nonisolated static func deviceTitle(_ device: DiscoveredPrinter) -> String {
        "\(device.name ?? "未命名 BLE 设备") · \(device.identifier.uuidString.prefix(8))"
    }
    nonisolated static func testEncodedImage() throws -> Data {
        let pixels = testImage(width: 384, height: 64)
        guard let provider = CGDataProvider(data: pixels as CFData),
              let image = CGImage(width: 384, height: 64, bitsPerComponent: 8, bitsPerPixel: 8,
                bytesPerRow: 384, space: CGColorSpaceCreateDeviceGray(), bitmapInfo: [], provider: provider,
                decode: nil, shouldInterpolate: false, intent: .defaultIntent) else { throw CocoaError(.coderInvalidValue) }
        let output = NSMutableData()
        guard let destination = CGImageDestinationCreateWithData(output, "public.png" as CFString, 1, nil) else { throw CocoaError(.coderInvalidValue) }
        CGImageDestinationAddImage(destination, image, nil)
        guard CGImageDestinationFinalize(destination) else { throw CocoaError(.coderInvalidValue) }
        return output as Data
    }
    nonisolated static func testImage(width: Int, height: Int) -> Data {
        var pixels = Data(repeating: 255, count: width * height)
        for y in 0..<height { for x in 0..<width where (x / 16 + y / 16) % 2 == 0 { pixels[y * width + x] = 0 } }
        return pixels
    }
}
