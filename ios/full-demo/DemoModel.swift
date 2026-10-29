import Combine
import Foundation
import XLPrinterSDK

enum DemoPickerKind: String, Sendable { case files, photos }
struct DemoPickerRequest: Identifiable, Sendable { let id: UUID; let kind: DemoPickerKind }
struct DemoLogExport: Identifiable, Sendable { let id: UUID; let text: String }

@MainActor
final class DemoModel: ObservableObject, PrinterDelegate {
    enum Phase { case offline, connecting, ready, working, disconnecting, closed }
    @Published private(set) var devices: [DiscoveredPrinter] = []
    @Published var selected: UUID?
    @Published var connectionSettings = DemoConnectionSettings()
    @Published var labelWidthMm = "50" { didSet { invalidatePreview() } }
    @Published var labelHeightMm = "50" { didSet { invalidatePreview() } }
    @Published var rawHex = ""
    @Published var media = PaperType.gapLabel { didSet { if media != oldValue { mediaInitialized = false } } }
    @Published private(set) var phase = Phase.offline
    @Published private(set) var scanning = false
    @Published private(set) var scanStopping = false
    @Published private(set) var status = "请选择设备；连接不需要先填写图纸尺寸"
    @Published private(set) var connectionInfo: ConnectionInfo?
    @Published private(set) var printerInfo: PrinterInfo?
    @Published private(set) var realtimeStatus: StatusResult?
    @Published private(set) var statusRecoveryNotice: DemoStatusNotice?
    @Published private(set) var mediaInitialized = false
    @Published private(set) var lastResult: DemoResult? { didSet { recordResult(lastResult) } }
    @Published private(set) var cleanupResult: DemoResult? { didSet { recordResult(cleanupResult) } }
    @Published private(set) var connectionLoss: DemoResult? { didSet { recordResult(connectionLoss) } }
    @Published private(set) var identityResult: DemoResult? { didSet { recordResult(identityResult) } }
    @Published private(set) var configurationResult: DemoResult? { didSet { recordResult(configurationResult) } }
    @Published private(set) var initializationResult: DemoResult? { didSet { recordResult(initializationResult) } }
    @Published private(set) var inputError: DemoInputError? {
        didSet { if let inputError { appendLog("event=input_rejected code=\(inputError.code)") } }
    }
    @Published private(set) var importedImage: DemoImportedImage?
    @Published private(set) var imagePreview: DemoImagePreview?
    @Published private(set) var pickerRequest: DemoPickerRequest?
    @Published private(set) var imageLoading = false
    @Published private(set) var previewLoading = false
    @Published private(set) var settingsBusy = false
    @Published private(set) var settingsStatus = "显式保存或加载参数草稿；不会恢复连接或打印任务"
    @Published private(set) var logRecords: [String] = []
    @Published private(set) var logBusy = false
    @Published private(set) var logStatus = "日志不记录设备身份、文件路径或 Raw 内容"
    @Published private(set) var logExport: DemoLogExport?
    var logExportText: String? { logExport?.text }
    private let configurationStore: DemoConfigurationStore?
    private let journal: DemoLogJournal?
    private var settingsRequest: UUID?
    private var logRequest: UUID?
    private var logClearRequest: UUID?
    private var presentedLogExport: UUID?
    @Published private var foreground = true
    private var runtimeVerificationStarted = false
    private var requestNumbers: [RequestID: UInt64] = [:]
    private var nextRequestNumber: UInt64 = 0
    private let manager: PrinterManager
    private let imageStore: DemoImageStore
    private let imageWorkQueue: DispatchQueue
    private let imageSlots = DispatchSemaphore(value: 2)
    private var imageCancellation: DemoImageCancellation?
    private var imageRequest: UUID?
    private var previewRequest: UUID?
    private var connection: RequestID?
    private var connectingRequest: RequestID?
    private var active: RequestID?
    private var scanRequest: RequestID?
    private var infoRequest: RequestID?
    private var requestedMedia: PaperType?
    private var showingStatus = false
    private var connectionFailed = false
    private var statusNoticeTask: Task<Void, Never>?
    private var nextStatusNoticeID: UInt64 = 0
    private let statusNoticeLifetimeNanoseconds: UInt64
    private var imageBusy: Bool { imageRequest != nil || previewLoading }
    var canSelectDevice: Bool { foreground && phase == .offline && active == nil && connectingRequest == nil && !imageBusy && !settingsBusy }
    var canConnect: Bool { canSelectDevice && !scanning }
    var canManageSettings: Bool { foreground && canConnect && configurationStore != nil }
    var canWork: Bool { phase == .ready && active == nil && connectingRequest == nil && !imageBusy }
    var canPrint: Bool { canWork && connectionInfo?.model != nil }
    var canImport: Bool { (canConnect || canWork) && !scanning }
    var layoutDescription: String {
        guard let settings = try? printSettings() else { return "请填写有效页面尺寸（mm）" }
        return "\(settings.widthDots) × \(settings.heightDots) dots；单份、完整比例居中、阈值 128"
    }
    var viewState: DemoViewState {
        let identity: SDKInfo?
        if case .identity(let result) = identityResult { identity = result.value } else { identity = nil }
        return DemoViewState(devices: devices, selected: selected, scanning: scanning, scanStopping: scanStopping,
            canScan: canConnect || (scanning && !scanStopping && foreground), canSelectDevice: canSelectDevice,
            canEditConnection: canConnect, canConnect: canConnect && selected != nil,
            canDisconnect: phase != .offline && phase != .disconnecting && phase != .closed,
            canWork: canWork, canCancel: phase == .connecting || (phase == .working && active != nil) || imageBusy,
            status: status, result: lastResult, cleanupResult: cleanupResult, connectionLoss: connectionLoss,
            identityResult: identityResult, inputError: inputError, identity: identity)
    }
    init(manager: PrinterManager = PrinterManager(), imageStore: DemoImageStore = DemoImageStore(),
         imageWorkQueue: DispatchQueue = DispatchQueue(label: "PrinterDemo.images", attributes: .concurrent),
         configurationStore: DemoConfigurationStore? = nil, journal: DemoLogJournal? = nil,
         statusNoticeLifetimeNanoseconds: UInt64 = 3_000_000_000) {
        self.manager = manager; self.imageStore = imageStore; self.imageWorkQueue = imageWorkQueue
        self.configurationStore = configurationStore; self.journal = journal
        self.statusNoticeLifetimeNanoseconds = statusNoticeLifetimeNanoseconds
        manager.delegate = self
        appendLog("event=demo_started data_sent_is_not_printed=true automatic_replay=false")
    }
    func loadInfo() {
        guard infoRequest == nil, identityResult == nil, phase != .closed, phase != .disconnecting else { return }
        infoRequest = manager.sdkInfo()
    }
    func selectConnectionMode(_ mode: DemoConnectionSettings.Mode) { guard canConnect else { return }; connectionSettings.selectMode(mode) }
    func selectDevice(_ identifier: UUID?) {
        guard canSelectDevice, identifier == nil || devices.contains(where: { $0.identifier == identifier }) else { return }
        selected = identifier
    }
    func toggleScan() { if scanning { stopScan() } else { scan() } }
    func scan() {
        guard canConnect else { return }
        appendLog("event=scan_requested")
        inputError = nil; devices = []; selected = nil; scanStopping = false; scanning = true; status = "扫描中…"
        scanRequest = manager.scanBLE()
    }
    func stopScan() {
        guard let scanRequest, !scanStopping else { return }
        scanStopping = true; status = "正在停止扫描…"
        manager.cancel(scanRequest)
    }
    func connect() {
        guard canConnect, let selected else { return }
        let device = devices.first { $0.identifier == selected } ?? DiscoveredPrinter(identifier: selected)
        appendLog("event=connect_requested")
        stopStatusListening(); startStatusListening(); inputError = nil; printerInfo = nil; phase = .connecting; status = "正在连接…"
        connectionFailed = false
        if let model = connectionSettings.model {
            connection = manager.connectBle(device: device, model: model)
        } else {
            connection = manager.connectBle(device: device)
        }
        connectingRequest = connection
    }
    func applyPrinterModel() {
        guard canWork else { return }
        inputError = nil; phase = .working; status = "正在采用已收录型号"
        active = manager.setPrinterModel(.ltM90)
    }
    func initializeMedia() {
        guard canWork, connectionInfo?.model != nil else { return }
        requestedMedia = media
        mediaInitialized = false; inputError = nil; phase = .working; status = "正在设置纸张类型"
        active = manager.setPaperType(media)
    }
    func printLabel() { submitPrint(imported: false) }
    func printImage() { submitPrint(imported: true) }
    func printCurrentContent() {
        if importedImage != nil { printImage() } else { printLabel() }
    }
    private func submitPrint(imported: Bool) {
        guard canPrint else { return }
        do {
            let settings = try printSettings()
            let bytes: Data
            if imported {
                guard let image = importedImage else { throw DemoInputError(1001) }
                bytes = image.encodedData
            } else {
                bytes = try DemoPrintSample.testPNG(width: min(384, settings.widthDots), height: min(64, settings.heightDots))
            }
            appendLog("event=print_requested imported=\(imported) width=\(settings.widthDots) height=\(settings.heightDots)")
            inputError = nil; phase = .working; status = "打印请求已登记；无需先生成预览"
            active = manager.printEncodedImage(bytes, widthMm: settings.widthMm, heightMm: settings.heightMm)
        } catch { rejectInput(error) }
    }
    func printSettings() throws -> DemoPrintSettings {
        try DemoPrintSettings(widthMm: labelWidthMm, heightMm: labelHeightMm, model: connectionInfo?.model ?? .ltM90)
    }
    func sendRaw() {
        guard canWork else { return }
        do {
            let bytes = try DemoPrintSample.rawBytes(rawHex)
            inputError = nil; mediaInitialized = false; phase = .working; status = "Raw 请求已登记；纸张设置可能改变"
            active = manager.sendRaw(bytes)
        } catch { rejectInput(error) }
    }
    func setMedia() { initializeMedia() }
    func feed() {
        guard canWork else { return }
        inputError = nil; phase = .working
        active = manager.locateNextLabel()
    }
    func getStatus() {
        guard canWork else { return }
        inputError = nil; phase = .working; status = "正在查询状态；盖纸状态由状态监听更新"
        active = manager.getStatus()
    }
    func capabilities() {
        guard canWork else { return }
        inputError = nil; phase = .working
        active = manager.capabilities()
    }
    @discardableResult func requestImageImport(_ kind: DemoPickerKind = .files) -> UUID? {
        guard canImport else { return nil }
        let id = UUID(); imageCancellation = DemoImageCancellation(); imageRequest = id
        pickerRequest = DemoPickerRequest(id: id, kind: kind); inputError = nil
        status = "请选择 PNG/JPEG；保留空闲连接，不发送命令"
        return id
    }
    func beginPickerLoad(_ id: UUID) {
        guard imageRequest == id else { return }
        pickerRequest = nil; imageLoading = true; status = "正在稳定导入图片…"
    }
    func makePickerImporter(_ id: UUID) -> @Sendable (URL) throws -> DemoImportedImage {
        let store = imageStore, token = imageRequest == id ? imageCancellation : nil, slots = imageSlots
        return { url in
            guard let token else { throw DemoInputError(1005) }
            try token.checkCancellation()
            guard slots.wait(timeout: .now()) == .success else { throw DemoInputError(1008) }
            defer { slots.signal() }
            return try store.createImport(from: url, cancellation: token)
        }
    }
    func receiveImageFile(_ result: Result<URL, Error>, requestID: UUID) {
        guard imageRequest == requestID else { return }
        switch result {
        case .failure(let error): receiveImportedImage(.failure((error as? DemoInputError) ?? DemoInputError(1005)), requestID: requestID)
        case .success(let url):
            beginPickerLoad(requestID)
            let importer = makePickerImporter(requestID)
            imageWorkQueue.async { [weak self] in
                let imported: Result<DemoImportedImage, DemoInputError>
                do { imported = .success(try importer(url)) }
                catch { imported = .failure((error as? DemoInputError) ?? DemoInputError(2003)) }
                Task { @MainActor [weak self] in self?.receiveImportedImage(imported, requestID: requestID) }
            }
        }
    }
    func receiveImportedImage(_ result: Result<DemoImportedImage, DemoInputError>, requestID: UUID) {
        guard imageRequest == requestID, phase != .closed else { return }
        pickerRequest = nil; imageRequest = nil; imageCancellation = nil; imageLoading = false
        switch result {
        case .success(let image): importedImage = image; invalidatePreview(); inputError = nil; status = "图片已导入；可直接打印或生成完整预览"
        case .failure(let error):
            if error.code != 1005 { inputError = error }
            status = "导入已结束；保留上一有效图片"
        }
    }
    func preparePreview() {
        guard (canConnect || canWork), let image = importedImage else { return }
        let settings: DemoPrintSettings
        do { settings = try printSettings() } catch { rejectInput(error); return }
        invalidatePreview()
        let id = UUID(), token = DemoImageCancellation(), slots = imageSlots
        imageCancellation = token; previewRequest = id; previewLoading = true; inputError = nil
        imageWorkQueue.async { [weak self] in
            let result: Result<DemoImagePreview, DemoInputError>
            do {
                try token.checkCancellation()
                guard slots.wait(timeout: .now()) == .success else { throw DemoInputError(1008) }
                defer { slots.signal() }
                result = .success(try DemoPreviewRenderer.render(image: image, widthDots: settings.widthDots,
                    heightDots: settings.heightDots, cancellation: token))
            } catch { result = .failure((error as? DemoInputError) ?? DemoInputError(2003)) }
            Task { @MainActor [weak self] in
                guard let self, previewRequest == id, phase != .closed else { return }
                previewRequest = nil; previewLoading = false; imageCancellation = nil
                switch result {
                case .success(let preview): imagePreview = preview; status = "完整图片预览已生成；无裁切，不代表纸面完成"
                case .failure(let error): if error.code != 1005 { inputError = error }; status = "预览已结束；导入来源仍可使用"
                }
            }
        }
    }
    func invalidatePreview() {
        guard previewRequest != nil || imagePreview != nil else { return }
        if previewRequest != nil { imageCancellation?.cancel(); imageCancellation = nil }
        previewRequest = nil; previewLoading = false; imagePreview = nil
    }
    func clearImage() {
        guard canConnect || canWork else { return }
        invalidatePreview(); importedImage = nil; status = "已切回内置测试图"
    }
    func cancelImageFlow() {
        imageCancellation?.cancel(); imageCancellation = nil
        imageRequest = nil; pickerRequest = nil; imageLoading = false; previewRequest = nil; previewLoading = false
    }
    func cancel() {
        appendLog("event=cancel_requested")
        if imageBusy { cancelImageFlow(); status = "已取消图片流程；保留上一有效图片"; return }
        guard let request = phase == .connecting ? connection : active else { return }
        manager.cancel(request); status = "已请求取消；等待真实终态，已发送数据不能撤回"
    }
    func foregroundEntry() { foreground = true; startStatusListening() }
    func background() {
        stopStatusListening()
        foreground = false; cancelFilePresentation()
        appendLog("event=background")
        cancelImageFlow(); invalidatePreview(); disconnect()
    }
    func disconnect() {
        stopStatusListening()
        cancelSettingsWork()
        cancelImageFlow()
        guard phase != .closed, phase != .disconnecting else { return }
        if scanning { scanStopping = true }
        appendLog("event=disconnect_requested")
        connection = nil; connectionInfo = nil; printerInfo = nil; mediaInitialized = false; phase = .disconnecting; status = "正在断开并清理…"
        connectionFailed = false
        manager.disconnect()
    }
    func close() {
        stopStatusListening()
        guard phase != .closed else { return }
        cancelFilePresentation(); logClearRequest = nil; logBusy = false
        appendLog("event=demo_closed"); journal?.close()
        cancelImageFlow(); invalidatePreview(); importedImage = nil; manager.close()
        connection = nil; connectionInfo = nil; printerInfo = nil; scanRequest = nil; mediaInitialized = false; phase = .closed; scanning = false; scanStopping = false
    }
    deinit { statusNoticeTask?.cancel(); manager.delegate = nil; imageCancellation?.cancel(); journal?.close(); manager.close() }
    private func finish(_ result: DemoResult) {
        guard active == result.evidence.requestID, phase != .closed else { return }
        active = nil
        if phase == .working { phase = connection == nil ? .offline : .ready }
        lastResult = result; status = DemoResultText.requestSummary(result)
    }
    private func rejectInput(_ error: Error) {
        inputError = error as? DemoInputError ?? DemoInputError(1000); status = "输入或前置条件校验失败；尚未登记 SDK 请求"
    }

    // The unified delegate is registered once. Foreground policy only controls
    // display; it never replaces the SDK receiver while requests are outstanding.
    private func startStatusListening() { showingStatus = true }
    private func stopStatusListening() {
        showingStatus = false; realtimeStatus = nil; clearStatusRecoveryNotice()
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
        lastResult = .discovery(result)
        guard phase == .offline else { return }
        if result.isSuccess {
            devices = result.value ?? []
            if !devices.contains(where: { $0.identifier == selected }) { selected = nil }
            status = "找到 \(devices.count) 个候选设备；请选择后连接"
        } else { status = "扫描已结束；保留本轮已发现候选，详见原始结果" }
    }
    func printerManager(_ manager: PrinterManager, didFinishConnecting result: ManagerResult<ConnectionInfo>) {
        guard connectingRequest == result.requestID else { return }
        connectingRequest = nil
        guard phase != .closed else { return }
        lastResult = .connection(.connect, result)
        guard connection == result.requestID, phase == .connecting else { return }
        if result.isSuccess {
            connectionInfo = result.value; printerInfo = result.value?.printerInfo; mediaInitialized = false; phase = .ready
            startStatusListening()
            status = "已连接；可设置纸张类型或直接打印，连接不发送纸张设置命令"
        } else {
            connection = nil; connectionInfo = nil; phase = .disconnecting; connectionFailed = true
            manager.disconnect()
        }
    }
    func printerManager(_ manager: PrinterManager, didLoseConnection result: ManagerResult<Void>) {
        guard connection == result.requestID else { return }
        stopStatusListening()
        connection = nil; connectionInfo = nil; printerInfo = nil; mediaInitialized = false; phase = .offline
        connectionLoss = .action(.disconnected, result); cancelImageFlow()
        status = "连接意外断开；请手动重新连接"
    }
    func printerManager(_ manager: PrinterManager, didFinishSettingPrinterModel result: ManagerResult<ConnectionInfo>) {
        guard active == result.requestID, phase != .closed else { return }
        active = nil; configurationResult = .connection(.configure, result)
        guard phase == .working, connection != nil else { return }
        phase = .ready
        if result.isSuccess, let info = result.value {
            connectionInfo = info; printerInfo = info.printerInfo; mediaInitialized = false; invalidatePreview()
            startStatusListening()
            status = "已采用 LT-M90；纸张设置需单独执行"
        } else { status = "型号未采用；保留原配置" }
    }
    func printerManager(_ manager: PrinterManager, didFinishSettingPaperType result: ManagerResult<Void>) {
        guard active == result.requestID, phase != .closed else { return }
        active = nil; initializationResult = .action(.media, result)
        let selectedMedia = requestedMedia; requestedMedia = nil
        guard phase == .working, connection != nil else { return }
        phase = .ready
        mediaInitialized = result.isSuccess && result.completion != .none && media == selectedMedia
        status = mediaInitialized ? "纸张类型设置已发送；不代表设备确认" : "纸张类型设置未成功；可手动重试"
    }
    func printerManager(_ manager: PrinterManager, didFinishPrinting result: ManagerResult<Void>) { finish(.action(.printLabel, result)) }
    func printerManager(_ manager: PrinterManager, didFinishSendingRaw result: ManagerResult<Void>) { finish(.action(.raw, result)) }
    func printerManager(_ manager: PrinterManager, didFinishPositioningLabel result: ManagerResult<Void>) { finish(.action(.feed, result)) }
    func printerManager(_ manager: PrinterManager, didFinishQueryingStatus result: ManagerResult<Void>) { finish(.action(.getStatus, result)) }
    func printerManager(_ manager: PrinterManager, didFinishReadingCapabilities result: ManagerResult<PrinterCapabilities>) { finish(.capabilities(result)) }
    func printerManager(_ manager: PrinterManager, didFinishReadingSDKInfo result: ManagerResult<SDKInfo>) {
        guard infoRequest == result.requestID, phase != .closed else { return }
        infoRequest = nil; identityResult = .identity(result)
        let arguments = ProcessInfo.processInfo.arguments
        if !runtimeVerificationStarted, let index = arguments.firstIndex(of: "--psdk-offline-verification"),
           arguments.indices.contains(index + 1), let nonce = UUID(uuidString: arguments[index + 1]), let journal {
            runtimeVerificationStarted = true
            let resource = Bundle.main.url(forResource: "printer-sdk-samples-build", withExtension: "json")
            Task { @MainActor in await DemoOfflineRuntimeVerification.run(nonce: nonce, journal: journal, resourceURL: resource) }
        }
    }
    func printerManager(_ manager: PrinterManager, didFinishDisconnecting result: ManagerResult<Void>) {
        guard phase == .disconnecting else { return }
        cleanupResult = .action(.disconnect, result); phase = .offline
        status = connectionFailed ? "连接未成功；已清理，可手动重试" : "已断开；不会自动重连或重放"
        connectionFailed = false
    }
    func printerManager(_ manager: PrinterManager, didUpdateStatus value: StatusResult) {
        guard showingStatus, foreground, connection != nil, phase == .ready || phase == .working else { return }
        let previous = realtimeStatus
        realtimeStatus = value
        if let message = DemoResultText.statusRecovery(previous: previous, current: value) {
            showStatusRecoveryNotice(message)
        } else if value.coverOpen == nil || value.paperOut == nil ||
                    (value.coverOpen == true && previous?.coverOpen != true) ||
                    (value.paperOut == true && previous?.paperOut != true) {
            clearStatusRecoveryNotice()
        }
    }
    private func showStatusRecoveryNotice(_ message: String) {
        statusNoticeTask?.cancel()
        nextStatusNoticeID &+= 1
        let id = nextStatusNoticeID, lifetime = statusNoticeLifetimeNanoseconds
        statusRecoveryNotice = DemoStatusNotice(id: id, message: message)
        statusNoticeTask = Task { [weak self] in
            do { try await Task.sleep(nanoseconds: lifetime) } catch { return }
            self?.dismissStatusRecoveryNotice(id)
        }
    }
    func dismissStatusRecoveryNotice(_ id: UInt64) {
        guard statusRecoveryNotice?.id == id else { return }
        clearStatusRecoveryNotice()
    }
    private func clearStatusRecoveryNotice() {
        statusNoticeTask?.cancel(); statusNoticeTask = nil; statusRecoveryNotice = nil
    }
}

// Demo-only persistence and diagnostics. These helpers never control the printer.
extension DemoModel {
    func savedSettingsSnapshot() throws -> DemoSavedSettings {
        var value = DemoSavedSettings()
        value.connectionMode = connectionSettings.mode.rawValue
        value.labelWidthMm = labelWidthMm; value.labelHeightMm = labelHeightMm
        switch media { case .gapLabel: value.paperType = "gapLabel"; case .blackMark: value.paperType = "blackMark"; case .continuous: value.paperType = "continuous" }
        return try value.validated()
    }

    func saveSettings() {
        guard canManageSettings, let store = configurationStore else { return }
        do {
            let value = try savedSettingsSnapshot(), request = UUID()
            settingsRequest = request; settingsBusy = true; settingsStatus = "正在保存参数草稿…"
            store.save(value) { [weak self] result in
                Task { @MainActor [weak self] in
                    guard let self, settingsRequest == request, phase != .closed else { return }
                    settingsRequest = nil; settingsBusy = false
                    switch result {
                    case .success: settingsStatus = "参数已保存；不会保存设备、图片或运行中的任务"; appendLog("event=settings_saved")
                    case .failure(let error): settingsStatus = error.message; appendLog("event=settings_save_failed")
                    }
                }
            }
        } catch {
            settingsStatus = (error as? DemoConfigurationError)?.message ?? DemoConfigurationError.invalidFields.message
            appendLog("event=settings_rejected")
        }
    }

    func loadSettings() {
        guard canManageSettings, let store = configurationStore else { return }
        let request = UUID()
        settingsRequest = request; settingsBusy = true; settingsStatus = "正在读取参数草稿…"
        store.load { [weak self] result in
            Task { @MainActor [weak self] in
                guard let self, settingsRequest == request, phase != .closed else { return }
                settingsRequest = nil; settingsBusy = false
                guard foreground, canManageSettings else { return }
                switch result {
                case .success(let value):
                    applySavedDraft(value)
                    settingsStatus = "已加载草稿；请重新选择设备并连接，设备配置仍以连接结果为准"
                    appendLog("event=settings_loaded draft_only=true")
                case .failure(let error): settingsStatus = error.message; appendLog("event=settings_load_failed")
                }
            }
        }
    }

    func clearSavedSettings() {
        guard canManageSettings, let store = configurationStore else { return }
        let request = UUID()
        settingsRequest = request; settingsBusy = true; settingsStatus = "正在清除已保存参数…"
        store.clear { [weak self] result in
            Task { @MainActor [weak self] in
                guard let self, settingsRequest == request, phase != .closed else { return }
                settingsRequest = nil; settingsBusy = false
                switch result {
                case .success: settingsStatus = "已清除保存文件；当前表单保持不变"; appendLog("event=settings_cleared")
                case .failure(let error): settingsStatus = error.message; appendLog("event=settings_clear_failed")
                }
            }
        }
    }

    private func applySavedDraft(_ value: DemoSavedSettings) {
        // The store validates the whole snapshot before any field is changed.
        connectionSettings = DemoConnectionSettings(mode: DemoConnectionSettings.Mode(rawValue: value.connectionMode)!)
        labelWidthMm = value.labelWidthMm; labelHeightMm = value.labelHeightMm
        switch value.paperType { case "blackMark": media = .blackMark; case "continuous": media = .continuous; default: media = .gapLabel }
        selected = nil; devices = []; connectionInfo = nil; mediaInitialized = false
        invalidatePreview()
    }

    private func cancelSettingsWork() {
        if settingsRequest != nil { settingsStatus = "参数操作已离开；加载结果不会应用到当前页面" }
        settingsRequest = nil; settingsBusy = false
    }

    private func cancelFilePresentation() {
        cancelSettingsWork(); logRequest = nil; logBusy = logClearRequest != nil
        logExport = nil; presentedLogExport = nil
    }

    func prepareLogExport() {
        guard foreground, phase != .closed, !logBusy, logExport == nil, let journal else { return }
        let request = UUID(); logRequest = request; logBusy = true; logStatus = "正在准备脱敏日志…"
        journal.snapshot { [weak self] result in
            Task { @MainActor [weak self] in
                guard let self, logRequest == request, phase != .closed else { return }
                logRequest = nil; logBusy = false
                switch result {
                case .success(let text):
                    presentedLogExport = request; logExport = DemoLogExport(id: request, text: text)
                    logStatus = "日志已准备；请选择系统文件保存位置"
                case .failure(let error): logStatus = error.message
                }
            }
        }
    }

    func dismissLogExport(_ id: UUID) {
        guard presentedLogExport == id else { return }
        logExport = nil
    }
    func logExportCompleted(_ result: Result<URL, Error>, requestID: UUID) {
        guard foreground, phase != .closed, presentedLogExport == requestID else { return }
        presentedLogExport = nil; logExport = nil
        switch result {
        case .success: logStatus = "日志已导出"
        case .failure: logStatus = "导出已取消或失败；原日志保持不变"
        }
    }

    func clearLogs() {
        guard foreground, phase != .closed, !logBusy, let journal else { return }
        let request = UUID(), priorRecords = Set(logRecords)
        logClearRequest = request; logBusy = true
        journal.clear { [weak self] result in
            Task { @MainActor [weak self] in
                guard let self, logClearRequest == request, phase != .closed else { return }
                logClearRequest = nil; logBusy = false
                switch result {
                case .success:
                    // Preserve events appended after the clear was enqueued.
                    logRecords.removeAll { priorRecords.contains($0) }; logStatus = "已清除日志"
                case .failure(let error): logStatus = error.message
                }
            }
        }
    }

    private func appendLog(_ message: String) {
        let record = journal?.append(message) ?? message
        guard !record.isEmpty else { return }
        logRecords.append(record)
        if logRecords.count > DemoLogJournal.maximumVisibleRecords {
            logRecords.removeFirst(logRecords.count - DemoLogJournal.maximumVisibleRecords)
        }
    }

    private func recordResult(_ result: DemoResult?) {
        guard let result else { return }
        let evidence = result.evidence
        let operation: String
        switch result {
        case .action(let action, _), .connection(let action, _): operation = String(describing: action)
        case .status: operation = "status"
        case .capabilities: operation = "capabilities"
        case .identity: operation = "sdk_info"
        case .discovery: operation = "scan"
        }
        if requestNumbers[evidence.requestID] == nil {
            if requestNumbers.count >= 256 { requestNumbers.removeAll(keepingCapacity: true) }
            nextRequestNumber &+= 1; requestNumbers[evidence.requestID] = nextRequestNumber
        }
        // Never stringify SDK result values/messages: they can contain device or file details.
        appendLog("operation=\(operation) request=\(requestNumbers[evidence.requestID]!) outcome=\(evidence.outcome) code=\(evidence.code) completion=\(evidence.completion) outputMayHaveOccurred=\(evidence.outputMayHaveOccurred)")
        if case .identity(let result) = result, let value = result.value {
            appendLog("sdk=\(value.distributionVersion) native=\(value.nativeVersion)")
        }
    }
}
