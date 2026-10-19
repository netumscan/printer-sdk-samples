import Foundation
import Combine
import CoreGraphics
import PrinterSDK

struct DemoNextHint {
    let message: String
    let target: String?
}

/// Demo dependency only; discovery continues to belong to the platform SDK.
protocol DemoScanner: AnyObject {
    func scan(seconds: TimeInterval, service: UUID?,
              completion: @escaping @Sendable (Result<[DiscoveredPrinter], PrinterSDKError>) -> Void)
    func stop()
}
extension BLEScanner: DemoScanner {}

@MainActor
final class DemoModel: ObservableObject {
    @Published var devices: [DiscoveredPrinter] = []
    @Published var selected: UUID?
    @Published var scanning = false
    @Published var phase: PrinterController.Phase = .offline
    @Published var status = "尚未执行操作"
    @Published private(set) var deviceState: DeviceStateSnapshot?
    @Published private(set) var resourceState: ResourceSnapshot?
    @Published private(set) var deviceDiagnostic: ErrorDiagnostic?
    @Published private(set) var runtimeInfo: SDKInfo?
    @Published var recoveryNotice: String?
    @Published var logs: [String] = []
    @Published var preview: CGImage?
    @Published var previewing = false
    @Published var imageSummary = "内置校准图"
    @Published var localOperation = false
    @Published var logWarning: String?
    @Published var service = ""
    @Published var characteristic = ""
    @Published var payload = "512"
    @Published var m90Flow = true
    @Published var mode = 0
    @Published var media: MediaMode = .gap
    @Published var form = 0
    @Published var dpi = ""
    @Published var deviceWidth = ""
    @Published var width = ""
    @Published var xOffset = "0"
    @Published var originStrategy = DemoOriginStrategy.manual
    @Published var originAdjustment = "0"
    @Published var height = ""
    @Published var copies = "1"
    @Published var documentKind = DemoDocumentKind.label
    @Published var trailingBlank = "0"
    @Published var rawHex = ""
    @Published private(set) var fieldErrors: [String: String] = [:]
    @Published private(set) var validationTarget: String?
    @Published private(set) var validationRequestID: UUID?
    @Published private(set) var importRequestID: UUID?
    @Published private(set) var appliedMedia: MediaMode?
    @Published private(set) var sendingMedia: MediaMode?
    private struct ImportRequest {
        let id: UUID
        let configuration: LabelConfiguration
    }
    private var pendingImport: ImportRequest?
    private var mediaRequest: (mode: MediaMode, epoch: UInt64)?
    private var foregroundActive = true
    private var destroyed = false
    private let scanner: DemoScanner
    private let makeSession: ((UUID, BLEOptions) throws -> DemoPrinterSession)?
    private let consoleLogging: Bool
    private let journalRoot: URL?
    private let consoleWriter = DispatchQueue(label: "PrinterDemo.console")
    private let rendering: DispatchQueue
    private let preparation = DemoPreparationState()
    private let imageStore: DemoImageStore
    private let configurationStore: DemoConfigurationStore
    private var source: DemoImageSource?
    private var connectionSequence = 0
    private lazy var journal = DemoLogJournal(root: journalRoot, warning: { [weak self] message in
        DispatchQueue.main.async { [weak self] in
            guard let self, !self.destroyed else { return }; self.logWarning = message
        }
    })
    private var generation: UInt64 = 0
    private var heldController: PrinterController?
    private var controller: PrinterController {
        if let heldController { return heldController }
        let created = PrinterController(changed: { [weak self] phase, message in
            DispatchQueue.main.async { [weak self] in
                guard let self, !self.destroyed, self.heldController?.phase == phase else { return }
                self.phase = phase; self.status = message
                self.recoveryNotice = self.heldController?.recoveryNotice
                self.append(message)
                if phase == .offline {
                    self.appliedMedia = nil; self.sendingMedia = nil; self.mediaRequest = nil
                    if let request = self.pendingImport, self.foregroundActive {
                        self.importRequestID = request.id
                        self.status = "已断开；请选择 PNG/JPEG 图片"
                    }
                }
            }
        }, log: { [weak self] message in
            DispatchQueue.main.async { [weak self] in self?.append(message) }
        }, completed: { [weak self] kind, epoch, succeeded in
            DispatchQueue.main.async { [weak self] in
                guard let self, !self.destroyed else { return }
                if case .media(let mode) = kind, let mode,
                   self.mediaRequest?.mode == mode, self.mediaRequest?.epoch == epoch {
                    self.mediaRequest = nil
                    self.sendingMedia = nil
                    if succeeded, self.phase == .ready || self.phase == .printing { self.appliedMedia = mode }
                }
            }
        }, stateChanged: { [weak self] snapshot in
            DispatchQueue.main.async { [weak self] in
                guard let self, !self.destroyed, self.heldController?.deviceState == snapshot else { return }
                self.deviceState = snapshot
            }
        }, detailsChanged: { [weak self] resources, diagnostic in
            DispatchQueue.main.async { [weak self] in
                guard let self, !self.destroyed,
                      self.heldController?.resourceState == resources,
                      self.heldController?.diagnosticState == diagnostic else { return }
                self.resourceState = resources; self.deviceDiagnostic = diagnostic
            }
        })
        heldController = created
        return created
    }

    init(scanner: DemoScanner = BLEScanner(),
         makeSession: ((UUID, BLEOptions) throws -> DemoPrinterSession)? = nil,
         imageStore: DemoImageStore = DemoImageStore(),
         configurationStore: DemoConfigurationStore = DemoConfigurationStore(), journalRoot: URL? = nil,
         rendering: DispatchQueue = DispatchQueue(label: "PrinterDemo.preview"),
         consoleLogging: Bool = ProcessInfo.processInfo.arguments.contains("--psdk-console-log")) {
        self.scanner = scanner; self.makeSession = makeSession; self.imageStore = imageStore
        self.configurationStore = configurationStore; self.journalRoot = journalRoot
        self.rendering = rendering; self.consoleLogging = consoleLogging
        let store = imageStore
        rendering.async { store.cleanup() }
        runtimeInfo = try? PrinterSDK.info()
        append("compatibility_record=" + compatibilityRecord().json())
        append(runtimeDescription)
        if consoleLogging { append("诊断日志已开启；请在界面选择设备与打印参数") }
    }

    var editable: Bool { !destroyed && phase == .offline && !scanning && !previewing && !localOperation && pendingImport == nil }
    var idle: Bool { !destroyed && (phase == .offline || phase == .ready) && !scanning && !previewing && !localOperation && pendingImport == nil }
    var canSend: Bool { currentPreparedImage() != nil }
    var canImport: Bool { foregroundActive && idle }
    var importTitle: String { phase == .ready ? "断开并导入 PNG/JPEG" : "导入 PNG/JPEG" }
    var isCalibration: Bool { source == nil }
    var connectionStatus: String {
        DemoDeviceStateDisplay(snapshot: deviceState, phase: phase).connection
    }
    var deviceActivity: String { DemoDeviceStateDisplay(snapshot: deviceState, phase: phase).activity }
    var resourceDescription: String {
        guard let resourceState else { return "SDK 资源：未知" }
        return "SDK 资源：\(resourceState.outstandingOperations)/\(resourceState.maxPendingOperations) 项，" +
            "源载荷 \(resourceState.pendingPayloadBytes)/\(resourceState.maxPendingPayloadBytes) 字节（非进程内存）"
    }
    var diagnosticDescription: String {
        guard let value = deviceDiagnostic, value.source != .none else { return "最近诊断：无" }
        let native = value.nativeCode.map { "，原生码 \($0)" } ?? ""
        return "最近诊断：\(value.errorCode) · \(value.domain)\(native) · \(value.message)" +
            "（连接代次 \(value.connectionGeneration)）"
    }
    var runtimeDescription: String {
        guard let info = runtimeInfo else { return "运行库身份：未知" }
        return "运行库：\(info.nativeVersion) / \(info.distributionVersion.isEmpty ? "未知" : info.distributionVersion) / ABI \(info.abiVersion)/r\(info.abiRevision) / " +
            "构建 \(info.buildID ?? "未知")"
    }
    var imageState: String {
        if previewing { return "正在准备图片" }
        guard let config = try? configuration(), preparation.snapshot?.configuration == config else { return "等待准备或更新预览" }
        if phase == .ready { return canSend ? "已准备，可打印" : "等待准备或更新预览" }
        return "已准备，待连接"
    }
    var mediaStatus: String {
        if sendingMedia == media { return "正在发送" }
        return appliedMedia == media ? "命令已发送，生效需实物确认" : "未发送"
    }
    var nextHint: DemoNextHint {
        if destroyed { return DemoNextHint(message: "演示会话已关闭", target: nil) }
        if pendingImport != nil { return DemoNextHint(message: "正在断开或选择图片，请稍候", target: nil) }
        if scanning { return DemoNextHint(message: "正在扫描设备，点击定位停止按钮", target: "device") }
        if previewing { return DemoNextHint(message: "正在准备图片，请稍候", target: nil) }
        if localOperation { return DemoNextHint(message: "正在处理本机数据，请稍候", target: nil) }
        if phase == .connecting || phase == .stopping || phase == .printing {
            return DemoNextHint(message: status, target: nil)
        }
        do {
            if phase == .offline {
                guard selected != nil else { return DemoNextHint(message: "请选择设备，点击定位", target: "device") }
                _ = try bleOptions()
                return DemoNextHint(message: "通信参数已填写，点击定位连接按钮", target: "connect")
            }
            _ = try configuration()
            if currentPreparedImage() == nil { return DemoNextHint(message: "请准备或更新图片，点击定位预览按钮", target: "preview") }
            return DemoNextHint(message: "图片已准备，点击定位打印按钮", target: "print")
        } catch let error as DemoValidationError {
            return DemoNextHint(message: error.message + "，点击定位", target: error.field)
        } catch { return DemoNextHint(message: error.localizedDescription, target: nil) }
    }
    private func currentPreparedImage() -> DemoPreparedImage? {
        guard phase == .ready, idle, let config = try? configuration(),
              let prepared = preparation.snapshot, prepared.configuration == config else { return nil }
        return prepared
    }
    var labelFields: [String] {
        [dpi, deviceWidth, width, xOffset, String(originStrategy.rawValue), originAdjustment,
         height, copies, String(form), String(documentKind.rawValue), trailingBlank,
         service, characteristic, payload, String(m90Flow), String(mode), String(media.rawValue)]
    }
    func invalidatePreview() { preparation.invalidate(); preview = nil; previewing = false }
    func append(_ message: String) {
        guard !destroyed else { return }
        let record = journal.append(message, connection: connectionSequence)
        logs.append(record)
        if logs.count > 300 { logs.removeFirst(logs.count - 300) }
        if consoleLogging {
            let entry = Data("PRINTER_DEMO_LOG \(record)\n".utf8)
            consoleWriter.async { try? FileHandle.standardOutput.write(contentsOf: entry) }
        }
    }
    func error(_ error: Error) {
        guard !destroyed else { return }
        status = error.localizedDescription
        if let failure = error as? DemoValidationError, let field = failure.field {
            fieldErrors[field] = failure.message
            validationTarget = field; validationRequestID = UUID()
        }
        append("错误：\(status)")
    }
    private func number(_ input: String) -> Int { Int(input.trimmingCharacters(in: .whitespacesAndNewlines)) ?? 0 }
    private func requiredNumber(_ input: String, field: String, title: String) throws -> Int {
        guard let value = Int(input.trimmingCharacters(in: .whitespacesAndNewlines)) else {
            throw DemoValidationError(message: "请填写\(title)", field: field)
        }
        return value
    }
    func connectionConfiguration() throws -> DemoConnectionConfiguration {
        try DemoConnectionConfiguration(dpi: requiredNumber(dpi, field: "dpi", title: "DPI"),
            deviceWidth: requiredNumber(deviceWidth, field: "deviceWidth", title: "设备最大宽度"), kind: documentKind)
    }
    func configuration() throws -> LabelConfiguration {
        let connection = try connectionConfiguration()
        let width = try requiredNumber(self.width, field: "width", title: "图片宽度")
        let height = try requiredNumber(self.height, field: "height", title: "图片高度")
        let copies = try requiredNumber(self.copies, field: "copies", title: "份数")
        let blank = try requiredNumber(trailingBlank, field: "trailingBlank", title: "结束留白")
        let x = try resolvedOrigin()
        return try LabelConfiguration(dpi: connection.dpi, deviceWidth: connection.deviceWidth, width: width,
                               height: height, copies: copies, formChoice: form, xOffsetDots: x,
                               kind: documentKind, trailingBlankDots: blank,
                               originStrategy: originStrategy, originAdjustmentDots: number(originAdjustment))
    }
    private func bleOptions() throws -> BLEOptions {
        let cap = try requiredNumber(payload, field: "payload", title: "单包上限")
        guard (1...512).contains(cap) else { throw DemoValidationError(message: "单包上限须为 1–512 字节", field: "payload") }
        if m90Flow { return try M90Profile.bleOptions(maxWriteBytes: cap) }
        guard let service = UUID(uuidString: service.trimmingCharacters(in: .whitespacesAndNewlines)) else {
            throw DemoValidationError(message: "请填写完整 128 位服务 UUID", field: "service")
        }
        guard let write = UUID(uuidString: characteristic.trimmingCharacters(in: .whitespacesAndNewlines)) else {
            throw DemoValidationError(message: "请填写完整 128 位写特征 UUID", field: "characteristic")
        }
        guard mode == 1 || mode == 2 else { throw DemoValidationError(message: "请明确选择写入模式", field: "mode") }
        return try BLEOptions(service: service, writeCharacteristic: write, withResponse: mode == 1, maxWriteBytes: cap)
    }
    private func resolvedOrigin() throws -> Int {
        if documentKind == .receipt { return 0 }
        let manualX: Int
        let adjustment: Int
        if originStrategy == .centered {
            guard let value = Int(originAdjustment.trimmingCharacters(in: .whitespacesAndNewlines)) else {
                throw DemoValidationError(message: "请填写有符号的横向校准偏移", field: "originAdjustment")
            }
            manualX = 0; adjustment = value
        } else {
            guard let value = Int(xOffset.trimmingCharacters(in: .whitespacesAndNewlines)) else {
                throw DemoValidationError(message: "请填写横向起点", field: "xOffset")
            }
            manualX = value; adjustment = number(originAdjustment)
        }
        return try DemoHorizontalOrigin.resolve(deviceWidth: number(deviceWidth), canvasWidth: number(width),
            manualX: manualX, strategy: originStrategy, adjustment: adjustment)
    }
    var centeredOriginDescription: String {
        do { return "\(try resolvedOrigin()) dots" }
        catch { return error.localizedDescription }
    }
    func applyM90DevelopmentPreset() {
        guard editable else { return }
        dpi = String(M90DevelopmentPreset.dpi); deviceWidth = String(M90DevelopmentPreset.widthDots)
        documentKind = .label; m90Flow = true; payload = String(M90DevelopmentPreset.maxWriteBytes)
        service = ""; characteristic = ""; mode = 0
        originStrategy = .centered; originAdjustment = "0"
        selected = nil; devices = []
        invalidatePreview()
        status = "M90 开发预设 r\(M90DevelopmentPreset.revision) 已应用；请手动选择设备并连接"
        append("开发预设=\(M90DevelopmentPreset.id) / revision=\(M90DevelopmentPreset.revision) / CPCL / 203 DPI / 864 dots；未扫描、连接或发送")
    }
    func scan() {
        guard editable else { return }
        generation &+= 1; let version = generation
        scanning = true; status = "扫描 BLE 设备（10 秒）…"
        append("BLE 扫描开始；10 秒")
        scanner.scan(seconds: 10, service: nil) { [weak self] result in
            Task { @MainActor [weak self] in self?.completeScan(result, generation: version) }
        }
    }
    private func completeScan(_ result: Result<[DiscoveredPrinter], PrinterSDKError>, generation version: UInt64) {
        guard !destroyed, version == generation, scanning else { return }
        scanning = false
        switch result {
        case .success(let devices):
            self.devices = devices
            if let selected, !devices.contains(where: { $0.identifier == selected }) { self.selected = nil }
            append("BLE 扫描完成；候选数量=\(devices.count)")
            status = devices.isEmpty ? "未发现设备；请检查蓝牙、权限与距离" : "请选择设备；发现结果不代表型号已验证"
        case .failure(let error):
            if error.code == 1005 { status = "扫描已停止；未连接" }
            else { self.error(error) }
        }
    }
    func stopScan() {
        guard scanning else { return }
        generation &+= 1; scanner.stop(); scanning = false
        status = "扫描已停止；未连接"
        append("BLE 扫描已停止")
    }
    func connect() {
        guard editable, controller.phase == .offline else { return }
        do {
            guard let identifier = selected else {
                throw DemoValidationError(message: "请先扫描并选择 BLE 设备", field: "device")
            }
            let options = try bleOptions()
            fieldErrors = [:]
            connectionSequence += 1
            append("BLE / payloadCap=\(options.maxWriteBytes) / withResponse=\(options.withResponse) / creditFlow=\(options.creditFlowControl != nil)")
            if let factory = makeSession { controller.connect { try factory(identifier, options) } }
            else { controller.connect(identifier: identifier, options: options) }
        } catch { self.error(error) }
    }
    func applyMedia() {
        guard idle, phase == .ready, controller.phase == .ready, documentKind == .label else { return }
        do {
            let profile = try connectionConfiguration().profile()
            mediaRequest = (media, controller.connectionEpoch); sendingMedia = media
            controller.mediaControl(media, profile: profile)
        } catch { self.error(error) }
    }
    func locateLabel() {
        guard idle, phase == .ready, controller.phase == .ready, documentKind == .label else { return }
        do { controller.mediaControl(nil, profile: try connectionConfiguration().profile()) }
        catch { self.error(error) }
    }
    func makePreview() {
        guard idle else { return }
        do {
            let config = try configuration()
            let ticket = preparation.begin(), selectedSource = source
            let store = imageStore, state = preparation
            previewing = true
            rendering.async { [weak self] in
                do {
                    let pixels = try store.render(selectedSource, configuration: config,
                        cancelled: { !state.isCurrent(ticket) })
                    let snapshot = try DemoPreparedImage(configuration: config, pixels: pixels)
                    DispatchQueue.main.async { [weak self] in
                        guard let self, !self.destroyed, self.preparation.publish(snapshot, ticket: ticket) else { return }
                        self.preview = self.previewImage(snapshot); self.previewing = false
                        self.status = "预览已更新；发送将使用同一份 GRAY8"
                        self.append("预览：\(config.description)")
                    }
                } catch {
                    DispatchQueue.main.async { [weak self] in
                        guard let self, !self.destroyed, self.preparation.isCurrent(ticket) else { return }
                        self.previewing = false; self.error(error)
                    }
                }
            }
        } catch { self.error(error) }
    }
    private func previewImage(_ snapshot: DemoPreparedImage) -> CGImage? {
        guard let provider = CGDataProvider(data: snapshot.pixels as CFData) else { return nil }
        let config = snapshot.configuration
        return CGImage(width: config.width, height: config.height, bitsPerComponent: 8,
            bitsPerPixel: 8, bytesPerRow: config.width, space: CGColorSpaceCreateDeviceGray(),
            bitmapInfo: CGBitmapInfo(rawValue: 0), provider: provider, decode: nil,
            shouldInterpolate: false, intent: .defaultIntent)
    }
    func requestImport() {
        guard canImport, controller.phase == phase else { return }
        do {
            let config = try configuration()
            fieldErrors = [:]
            let request = ImportRequest(id: UUID(), configuration: config)
            pendingImport = request
            if phase == .ready {
                status = "正在断开以导入图片…"
                controller.disconnect(message: "已断开；请选择 PNG/JPEG 图片")
            } else { importRequestID = request.id }
        } catch { self.error(error) }
    }
    func importImage(_ result: Result<URL, Error>, requestID: UUID) {
        guard !destroyed, foregroundActive, let request = pendingImport, request.id == requestID,
              importRequestID == requestID, phase == .offline, controller.phase == .offline else { return }
        pendingImport = nil; importRequestID = nil
        guard case .success(let url) = result else {
            status = "已取消导入；保留上一次有效图片，未发送"; return
        }
        do {
            let config = try configuration()
            guard config == request.configuration else {
                throw DemoValidationError(message: "画布参数已变化；请重新导入", field: "width")
            }
            let ticket = preparation.begin()
            let store = imageStore, state = preparation, rendering = rendering
            previewing = true; status = "正在导入与准备图片…"
            rendering.async { [weak self] in
                var imported: DemoImageSource?
                do {
                    let next = try store.importImage(url, cancelled: { !state.isCurrent(ticket) })
                    imported = next
                    let pixels = try store.render(next, configuration: config,
                        cancelled: { !state.isCurrent(ticket) })
                    let snapshot = try DemoPreparedImage(configuration: config, pixels: pixels)
                    DispatchQueue.main.async { [weak self] in
                        guard let self, !self.destroyed, self.preparation.publish(snapshot, ticket: ticket) else {
                            rendering.async { store.discard(next) }; return
                        }
                        let previous = self.source; self.source = next
                        self.rendering.async { if let previous { store.discard(previous) } }
                        self.preview = self.previewImage(snapshot); self.previewing = false
                        self.imageSummary = "导入图片 · \(next.width)×\(next.height) · 等比居中白底"
                        self.status = "图片已准备；请手动连接和发送"
                        self.append("图片导入成功；纠正方向后=\(next.width)×\(next.height) / \(config.description)")
                    }
                } catch {
                    if let imported { store.discard(imported) }
                    DispatchQueue.main.async { [weak self] in
                        guard let self, !self.destroyed, self.preparation.isCurrent(ticket) else { return }
                        self.previewing = false
                        self.status = "导入失败；保留上一次有效图片。请检查格式、容量和尺寸"
                        self.append("图片导入失败；未替换有效图片，未提交任务")
                    }
                }
            }
        } catch { self.error(error) }
    }
    func useCalibration() {
        guard editable, !isCalibration else { return }
        let previous = source; source = nil; imageSummary = "内置校准图"
        let store = imageStore
        invalidatePreview()
        rendering.async { if let previous { store.discard(previous) } }
    }
    func printLabel() {
        guard let snapshot = currentPreparedImage() else { return }
        if snapshot.configuration.kind == .receipt { appliedMedia = nil }
        controller.printPrepared(snapshot)
    }
    func sendRaw() {
        do {
            guard phase == .ready, controller.phase == .ready, idle else { return }
            let selectedCopies = number(copies)
            guard (1...20).contains(selectedCopies) else { throw DemoValidationError(message: "每次测试限 1–20 份", field: "copies") }
            guard rawHex.utf16.count <= 65536 else { throw DemoValidationError(message: "Raw 输入不得超过 65536 个字符", field: "raw") }
            let tokens = rawHex.split(whereSeparator: { $0.isWhitespace })
            guard !tokens.isEmpty, tokens.count <= 4096 else { throw DemoValidationError(message: "请输入 1–4096 个空格分隔的十六进制字节", field: "raw") }
            let bytes = try tokens.map { token -> UInt8 in
                let ascii = Array(token.utf8)
                guard ascii.count == 2, ascii.allSatisfy({ (48...57).contains($0) || (65...70).contains($0) || (97...102).contains($0) }),
                      let value = UInt8(token, radix: 16) else {
                    throw DemoValidationError(message: "Raw 字节必须是两位十六进制，如 1B 40", field: "raw")
                }
                return value
            }
            appliedMedia = nil
            controller.sendRaw(Data(bytes), copies: UInt32(selectedCopies))
        } catch { self.error(error) }
    }
    func cancel() {
        guard !destroyed else { return }
        if previewing {
            _ = preparation.begin() // Invalidate only the ticket; retain the previous accepted image.
            previewing = false; status = "图片准备已取消；保留上一有效图片"
            append(status)
        } else { controller.cancel() }
    }
    private func cancelPendingImport() { pendingImport = nil; importRequestID = nil }
    func disconnect() {
        guard !destroyed else { return }
        cancelPendingImport(); mediaRequest = nil; sendingMedia = nil; appliedMedia = nil
        controller.disconnect()
    }
    private func savedConfiguration() throws -> DemoSavedConfiguration {
        let saved = DemoSavedConfiguration(configuration: try configuration(),
            service: m90Flow ? nil : service, characteristic: m90Flow ? nil : characteristic,
            payload: number(payload), m90Flow: m90Flow, writeMode: m90Flow ? nil : mode, media: Int(media.rawValue))
        _ = try saved.validated(); return saved
    }
    func saveConfiguration() {
        guard editable else { return }
        do {
            let saved = try savedConfiguration(); localOperation = true
            let store = configurationStore
            rendering.async { [weak self] in
                do {
                    try store.save(saved)
                    DispatchQueue.main.async { [weak self] in
                        guard let self, !self.destroyed else { return }
                        self.localOperation = false; self.status = "当前配置已保存；启动不会自动加载"; self.append(self.status)
                    }
                } catch { DispatchQueue.main.async { [weak self] in
                    guard let self, !self.destroyed else { return }
                    self.localOperation = false; self.localFailure("配置保存失败")
                } }
            }
        } catch { self.error(error) }
    }
    func loadConfiguration() {
        guard editable else { return }; localOperation = true
        let store = configurationStore
        rendering.async { [weak self] in
            do {
                let saved = try store.load(), config = try saved.validated()
                DispatchQueue.main.async { [weak self] in
                    guard let self, !self.destroyed else { return }
                    self.localOperation = false
                    guard self.phase == .offline else { return }
                    self.dpi = String(config.dpi); self.deviceWidth = String(config.deviceWidth)
                    self.width = String(config.width); self.height = String(config.height)
                    self.xOffset = String(config.xOffsetDots); self.copies = String(config.copies)
                    self.originStrategy = config.originStrategy
                    self.originAdjustment = String(config.originAdjustmentDots)
                    self.form = saved.formChoice ?? 0; self.documentKind = config.kind
                    self.trailingBlank = String(config.trailingBlankDots)
                    self.service = saved.service ?? ""; self.characteristic = saved.characteristic ?? ""
                    self.payload = String(saved.payload); self.m90Flow = saved.m90Flow
                    self.mode = saved.writeMode ?? 0; self.media = MediaMode(rawValue: UInt32(saved.media))!
                    self.selected = nil; self.devices = []
                    self.invalidatePreview(); self.status = "配置已加载；请重新预览并手动选择设备"
                    self.append(self.status)
                }
            } catch { DispatchQueue.main.async { [weak self] in
                guard let self, !self.destroyed else { return }
                self.localOperation = false; self.localFailure("没有有效的保存配置；未应用参数")
            } }
        }
    }
    func clearConfiguration() {
        guard editable else { return }; localOperation = true
        let store = configurationStore
        rendering.async { [weak self] in
            do {
                try store.clear()
                DispatchQueue.main.async { [weak self] in
                    guard let self, !self.destroyed else { return }
                    self.localOperation = false; self.status = "保存配置已清除"; self.append(self.status)
                }
            } catch { DispatchQueue.main.async { [weak self] in
                guard let self, !self.destroyed else { return }
                self.localOperation = false; self.localFailure("配置清除失败")
            } }
        }
    }
    private func localFailure(_ message: String) { status = message; append(message) }
    func exportLogs(_ completion: @escaping (String) -> Void) {
        guard idle else { return }; localOperation = true
        let version = generation
        let compatibility = compatibilityRecord()
        journal.snapshot { [weak self] result in
            DispatchQueue.main.async { [weak self] in
                guard let self, !self.destroyed else { return }
                self.localOperation = false
                guard version == self.generation else { return }
                switch result {
                case .success(let text): completion(compatibility.exporting(text))
                case .failure: self.localFailure("日志快照失败；保留内存记录")
                }
            }
        }
    }
    private func compatibilityRecord() -> DemoCompatibilityRecord {
        let os = ProcessInfo.processInfo.operatingSystemVersion
        return DemoCompatibilityRecord(osVersion: "\(os.majorVersion).\(os.minorVersion).\(os.patchVersion)", transport: "bluetooth_ble")
    }
    func clearLogs() {
        guard idle else { return }; localOperation = true
        journal.clear { [weak self] result in
            DispatchQueue.main.async { [weak self] in
                guard let self, !self.destroyed else { return }
                self.localOperation = false
                switch result {
                case .success: self.logs = []; self.logWarning = nil; self.status = "本机日志已清除"
                case .failure: self.localFailure("本机日志清除失败")
                }
            }
        }
    }
    func background() {
        guard !destroyed else { return }
        foregroundActive = false; cancelPendingImport()
        mediaRequest = nil; appliedMedia = nil; sendingMedia = nil
        generation &+= 1; scanner.stop(); scanning = false; invalidatePreview()
        if phase == .offline { status = "应用已进入后台；返回后请重新选择操作" }
        heldController?.disconnect(message: "应用已进入后台，会话已断开；请手动重连")
    }
    func foreground() { if !destroyed { foregroundActive = true } }
    /// Permanent owner teardown only, never ordinary navigation between Demo pages.
    func shutdown() {
        guard !destroyed else { return }
        destroyed = true; foregroundActive = false; cancelPendingImport()
        generation &+= 1; scanner.stop(); scanning = false
        preparation.invalidate(); preview = nil; previewing = false; localOperation = false
        mediaRequest = nil; appliedMedia = nil; sendingMedia = nil
        deviceState = nil; resourceState = nil; deviceDiagnostic = nil
        phase = .closed; status = "演示会话已关闭"
        heldController?.close(); heldController = nil
    }
    deinit {
        scanner.stop(); preparation.invalidate(); heldController?.close()
    }
}
