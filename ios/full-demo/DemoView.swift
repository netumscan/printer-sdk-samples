import SwiftUI
import UniformTypeIdentifiers
import UIKit
import PrinterSDK

struct LogDocument: FileDocument {
    static var readableContentTypes: [UTType] { [.plainText] }
    var text: String
    init(_ text: String) { self.text = text }
    init(configuration: ReadConfiguration) throws {
        text = String(decoding: configuration.file.regularFileContents ?? Data(), as: UTF8.self)
    }
    func fileWrapper(configuration: WriteConfiguration) throws -> FileWrapper {
        FileWrapper(regularFileWithContents: Data(text.utf8))
    }
}

/// Navigation stays inside one scene so viewing diagnostics never closes the session.
struct DemoView: View {
    private enum Page { case printing, logs, raw }
    private enum Help: String, Identifiable {
        case preset, media, form, origin, importImage
        var id: String { rawValue }
        var title: String {
            switch self {
            case .preset: return "M90 开发预设"
            case .media: return "介质设置与定位"
            case .form: return "FORM 策略"
            case .origin: return "横向定位"
            case .importImage: return "文件图片导入"
            }
        }
        var message: String {
            switch self {
            case .preset:
                return "开发预设 r1 填入 CPCL、203 DPI、864 dots、M90 BLE 令牌流控及横向自动居中。保留画布、份数、结束留白、FORM、介质与图片，不扫描、连接或发送。864 dots 来自已确认的 108 mm × 8 dots/mm；页高能力、M90 ESC/POS 和独立定位支持仍需硬件证据。"
            case .media:
                return "选择介质不会发送命令；点击发送后，仅确认命令传输完成，生效需检查实物。打印机重启后需重新应用。定位操作与介质设置分开，不执行传感器校准；M90 当前样机独立定位指令没有走纸，不能将发送成功视为走纸成功。连续纸建议关闭 FORM，请手动选择。"
            case .form:
                return "发送 FORM：在每份 CPCL 标签末尾、PRINT 前加入 FORM，交由固件按介质定位。不发送 FORM：省略该命令，实际进纸仍由固件处理。FORM 不决定画布尺寸，也不等于介质设置或传感器校准。修改选项不会发送命令；连续纸建议不发送 FORM。"
            case .origin:
                return "手动起点以 dots 向右偏移。自动居中：R = ceil(画布宽 / 8) × 8，X = floor((设备宽 − R) / 2) + 校准偏移；正数向右、负数向左。864 dots 设备配 800 dots 画布得到起点 32。越界拒绝，不裁切或钳制；预览展示内容，不代表纸张在打印头下的位置。"
            case .importImage:
                return "仅从系统文件选择器导入 PNG/JPEG，不申请相册或拍照权限。按纠正方向后的比例居中、透明白底，不裁切、不改变画布高度。原文件限 32 MiB、6400 万像素、单边 32768；输出限 800 万像素。图片仅本次运行保留。连接时先校验画布，再断开并选择；取消或失败保留上一有效图片，重连与打印仍需手动操作。"
            }
        }
    }

    @ObservedObject var model: DemoModel
    @Environment(\.sizeCategory) private var sizeCategory
    @State private var page = Page.printing
    @State private var advanced = false
    @State private var help: Help?
    @State private var exporting = false
    @State private var pickerRequest: UUID?
    @State private var document = LogDocument("")
    @State private var showDetails = false
    @State private var followLatest = true
    @State private var logViewport = DemoLogViewport()

    var body: some View {
        NavigationView {
            ScrollViewReader { proxy in
                VStack(spacing: 0) {
                    pageHeader
                    connectionFeedback.dynamicTypeSize(...DynamicTypeSize.accessibility1)
                    switch page {
                    case .printing: printPage(proxy)
                    case .logs: logPage
                    case .raw: rawPage
                    }
                }
                .background(Color(UIColor.systemGroupedBackground))
                .safeAreaInset(edge: .bottom, spacing: 0) { actionBar(proxy) }
                .navigationBarHidden(true)
                .toolbar {
                    ToolbarItemGroup(placement: .keyboard) {
                        Spacer()
                        Button("完成", action: dismissKeyboard).frame(minHeight: 48)
                    }
                }
                .onChange(of: model.labelFields) { _ in model.invalidatePreview() }
                .onChange(of: model.validationRequestID) { _ in
                    if let target = model.validationTarget {
                        if page == .raw && ["copies", "raw"].contains(target) {
                            proxy.scrollTo(target == "copies" ? "rawCopies" : target, anchor: .center)
                        } else { locate(target, proxy: proxy) }
                    }
                }
                .onChange(of: model.importRequestID) { request in
                    pickerRequest = request
                }
                .background {
                    if let request = pickerRequest {
                        DemoFileImporter(requestID: request) { result in
                            guard pickerRequest == request else { return }
                            pickerRequest = nil
                            model.importImage(result, requestID: request)
                        }.id(request)
                    }
                }
                .fileExporter(isPresented: $exporting, document: document, contentType: .plainText,
                              defaultFilename: "printer-demo-log") { result in
                    switch result {
                    case .success: model.append("日志已导出")
                    case .failure: model.append("日志导出失败；请重新选择保存位置")
                    }
                }
                .alert(item: $help) { item in
                    Alert(title: Text(item.title), message: Text(item.message), dismissButton: .default(Text("知道了")))
                }
            }
        }.navigationViewStyle(.stack)
    }

    private var pageHeader: some View {
        HStack(spacing: 4) {
            if page != .printing {
                Button { dismissKeyboard(); page = .printing } label: {
                    Image(systemName: "chevron.left").frame(width: 48, height: 48).contentShape(Rectangle())
                }.buttonStyle(.plain).accessibilityLabel("返回打印").accessibilityIdentifier("demo.back")
            }
            Text(page == .logs ? "运行日志" : page == .raw ? "Raw 诊断" : "打印机联调")
                .font(.headline).lineLimit(2).accessibilityAddTraits(.isHeader)
                .accessibilityIdentifier("demo.pageTitle")
            Spacer(minLength: 0)
            if page != .logs {
                Button { dismissKeyboard(); page = .logs } label: {
                    Image(systemName: "doc.text").frame(width: 48, height: 48).contentShape(Rectangle())
                }.buttonStyle(.plain).accessibilityLabel("日志").accessibilityIdentifier("demo.logs")
            }
            Menu {
                Button("保存当前配置", action: model.saveConfiguration)
                    .disabled(!model.editable).accessibilityIdentifier("demo.saveConfiguration")
                Button("加载保存配置", action: model.loadConfiguration)
                    .disabled(!model.editable).accessibilityIdentifier("demo.loadConfiguration")
                Button("清除保存配置", action: model.clearConfiguration)
                    .disabled(!model.editable).accessibilityIdentifier("demo.clearConfiguration")
                Divider()
                Button("Raw 诊断") { dismissKeyboard(); page = .raw }
                    .accessibilityIdentifier("demo.openRaw")
            } label: {
                Image(systemName: "ellipsis").frame(width: 48, height: 48).contentShape(Rectangle())
            }.buttonStyle(.plain).accessibilityLabel("更多").accessibilityIdentifier("demo.more")
        }.padding(.horizontal, 16).padding(.vertical, 4)
            .dynamicTypeSize(...DynamicTypeSize.accessibility1)
            .background(Color(UIColor.secondarySystemGroupedBackground))
    }

    private var connectionFeedback: some View {
        VStack(alignment: .leading, spacing: 4) {
            Label(model.connectionStatus, systemImage: model.phase == .ready ? "checkmark.circle" : "printer")
                .font(.subheadline.weight(.semibold)).accessibilityIdentifier("demo.connectionStatus")
            Text(model.status).font(.footnote).accessibilityIdentifier("demo.status")
        }
        .frame(maxWidth: .infinity, alignment: .leading).padding(.horizontal, 16).padding(.vertical, 8)
        .background(Color(UIColor.secondarySystemGroupedBackground))
    }

    private func printPage(_ proxy: ScrollViewProxy) -> some View {
        GeometryReader { geometry in
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    deviceSection(proxy)
                    canvasSection(twoColumns: geometry.size.width - 64 >= 320 && sizeCategory <= .extraLarge)
                    imageSection
                }.padding(16)
            }.accessibilityIdentifier("demo.printPage")
        }
    }

    private func deviceSection(_ proxy: ScrollViewProxy) -> some View {
        group("设备设置") {
            Text("BLE · 扫描与选择不会连接或发送").font(.footnote).foregroundColor(.secondary)
            action(model.scanning ? "停止扫描" : "扫描 BLE 设备", id: "demo.scan") {
                if model.scanning { model.stopScan() } else { model.scan() }
            }.disabled(!model.editable && !model.scanning).id("device")
            fieldError("device")
            ForEach(model.devices, id: \.identifier) { device in
                Button { model.selected = device.identifier } label: {
                    HStack {
                        VStack(alignment: .leading, spacing: 3) {
                            Text(device.name ?? "未命名设备")
                            Text("\(device.identifier.uuidString.suffix(8)) · \(device.rssi == 127 ? "信号未知" : "\(device.rssi) dBm")")
                                .font(.caption).foregroundColor(.secondary)
                        }
                        Spacer()
                        if model.selected == device.identifier { Image(systemName: "checkmark.circle.fill") }
                    }.frame(minHeight: 48)
                }.disabled(!model.editable)
            }
            HStack {
                action("应用 M90 开发预设", id: "demo.m90Preset", perform: model.applyM90DevelopmentPreset)
                    .disabled(!model.editable)
                helpButton(.preset)
            }
            Picker("作业类型", selection: $model.documentKind) {
                Text("CPCL 标签").tag(DemoDocumentKind.label)
                Text("ESC/POS 小票").tag(DemoDocumentKind.receipt)
            }.frame(minHeight: 48).disabled(!model.editable)
                .accessibilityIdentifier("demo.documentKind").id("kind")
            numberField("DPI", text: $model.dpi, target: "dpi").disabled(!model.editable)
            numberField("设备最大宽度（dots）", text: $model.deviceWidth, target: "deviceWidth")
                .disabled(!model.editable)
            DisclosureGroup("高级连接参数", isExpanded: $advanced) {
                VStack(alignment: .leading, spacing: 12) {
                    Toggle("M90 V1.2 令牌流控", isOn: $model.m90Flow).frame(minHeight: 48)
                    if model.m90Flow {
                        Text("FF00 服务 · FF02 无响应写入 · FF01/FF03 通知；等待设备发放令牌。")
                            .font(.footnote).foregroundColor(.secondary)
                    } else {
                        textField("服务 UUID（完整 128 位）", text: $model.service, target: "service")
                        textField("写特征 UUID（完整 128 位）", text: $model.characteristic, target: "characteristic")
                        Picker("写入模式", selection: $model.mode) {
                            Text("待选择").tag(0); Text("有响应").tag(1); Text("无响应").tag(2)
                        }.frame(minHeight: 48).id("mode")
                        fieldError("mode")
                    }
                    numberField("单包上限（1–512 字节）", text: $model.payload, target: "payload")
                    Text("实际包长受设备与系统容量限制；iOS 不设置请求 MTU。")
                        .font(.footnote).foregroundColor(.secondary)
                }.padding(.top, 8).disabled(!model.editable)
            }.id("advanced")
            HStack {
                action("连接", id: "demo.connect", perform: model.connect).disabled(!model.editable)
                    .id("connect")
                action("断开连接", id: "demo.disconnect", perform: model.disconnect)
                    .disabled(model.phase == .offline || model.phase == .stopping)
            }
            if model.phase == .ready {
                action("前往画布设置", id: "demo.goCanvas") { locate("canvas", proxy: proxy) }
            }
        }.id("deviceSection")
    }

    private func canvasSection(twoColumns: Bool) -> some View {
        group("画布与纸张设置") {
            if twoColumns {
                HStack(alignment: .top, spacing: 12) {
                    numberField("宽（dots）", text: $model.width, target: "width")
                    numberField("高（dots）", text: $model.height, target: "height")
                }
                HStack(alignment: .top, spacing: 12) {
                    numberField("份数（1–20）", text: $model.copies, target: "copies")
                    if model.documentKind == .label { originInput }
                }
            } else {
                numberField("宽（dots）", text: $model.width, target: "width")
                numberField("高（dots）", text: $model.height, target: "height")
                numberField("份数（1–20）", text: $model.copies, target: "copies")
                if model.documentKind == .label { originInput }
            }
            if model.documentKind == .label {
                HStack {
                    Picker("横向定位", selection: $model.originStrategy) {
                        Text("手动起点").tag(DemoOriginStrategy.manual)
                        Text("自动居中").tag(DemoOriginStrategy.centered)
                    }.frame(minHeight: 48).accessibilityIdentifier("demo.originStrategy")
                    helpButton(.origin)
                }
                if model.originStrategy == .centered {
                    Text("计算起点：\(model.centeredOriginDescription)").font(.footnote)
                        .accessibilityIdentifier("demo.resolvedOrigin")
                }
            }
            numberField("每份结束留白（dots）", text: $model.trailingBlank, target: "trailingBlank")
            if model.documentKind == .label {
                Divider()
                HStack {
                    Text("介质").font(.subheadline.weight(.semibold))
                    Spacer(); helpButton(.media)
                }
                Picker("介质类型", selection: $model.media) {
                    Text("间隙纸").tag(MediaMode.gap)
                    Text("黑标纸").tag(MediaMode.blackMark)
                    Text("连续纸").tag(MediaMode.continuous)
                }.frame(minHeight: 48).accessibilityIdentifier("demo.media")
                Text("选择不发送；点击下方按钮才应用。").font(.footnote).foregroundColor(.secondary)
                Text(model.mediaStatus).font(.footnote).accessibilityIdentifier("demo.mediaStatus")
                action("发送介质设置", id: "demo.applyMedia", perform: model.applyMedia)
                    .disabled(model.phase != .ready || !model.idle)
                Divider()
                action("定位下一标签", id: "demo.locateLabel", perform: model.locateLabel)
                    .disabled(model.phase != .ready || !model.idle)
                Text("独立定位命令；是否走纸需确认设备支持。").font(.footnote).foregroundColor(.secondary)
                Divider()
                HStack {
                    Picker("FORM 策略", selection: $model.form) {
                        Text("待选择").tag(0); Text("发送 FORM").tag(1); Text("不发送 FORM").tag(2)
                    }.frame(minHeight: 48).accessibilityIdentifier("demo.form").id("form")
                    helpButton(.form)
                }
                fieldError("form")
                Text("FORM 是每份标签末尾的定位命令，需明确选择。").font(.footnote).foregroundColor(.secondary)
            } else {
                Text("小票预算包含全部份数的图片和结束留白；当前不支持切纸及状态读回。")
                    .font(.footnote).foregroundColor(.secondary)
            }
        }.disabled(!model.idle).id("canvas")
    }

    @ViewBuilder private var originInput: some View {
        if model.originStrategy == .centered {
            numberField("校准偏移（dots）", text: $model.originAdjustment, target: "originAdjustment", signed: true)
        } else {
            numberField("横向起点（dots）", text: $model.xOffset, target: "xOffset")
        }
    }

    private var imageSection: some View {
        group("图片与预览") {
            Text(model.imageSummary).accessibilityIdentifier("demo.imageSource")
            Text(model.imageState).font(.footnote).foregroundColor(.secondary)
                .accessibilityIdentifier("demo.imageState")
            HStack {
                action(model.importTitle, id: "demo.importImage", perform: model.requestImport)
                    .disabled(!model.canImport)
                helpButton(.importImage)
            }
            action(model.isCalibration ? "当前为内置校准图" : "切回校准图", id: "demo.calibration",
                   perform: model.useCalibration).disabled(!model.editable || model.isCalibration)
            if let image = model.preview {
                Image(image, scale: 1, label: Text("将要提交的 GRAY8 图片预览"))
                    .resizable().interpolation(.none).scaledToFit().frame(maxHeight: 260)
                    .accessibilityIdentifier("demo.imagePreview")
            }
            if let notice = model.recoveryNotice {
                Text(notice).font(.footnote).foregroundColor(.orange)
                    .accessibilityIdentifier("demo.recoveryNotice")
            }
        }.id("image")
    }

    private var logPage: some View {
        VStack(alignment: .leading, spacing: 8) {
            Toggle("显示诊断详情", isOn: $showDetails).frame(minHeight: 48)
                .dynamicTypeSize(...DynamicTypeSize.accessibility1)
                .accessibilityIdentifier("demo.logDetails")
            Text(DemoLogDisplay.timeZoneDescription()).font(.caption).foregroundColor(.secondary)
                .fixedSize(horizontal: false, vertical: true)
                .dynamicTypeSize(...DynamicTypeSize.accessibility1)
            DemoLogRecordsView(records: model.logs, showDetails: showDetails,
                               followLatest: $followLatest, viewport: logViewport)
            if !followLatest {
                action("到最新", id: "demo.logLatest") { followLatest = true }
                    .dynamicTypeSize(...DynamicTypeSize.accessibility1)
            }
            if let warning = model.logWarning {
                Text(warning).foregroundColor(.orange).font(.footnote)
                    .fixedSize(horizontal: false, vertical: true)
                    .dynamicTypeSize(...DynamicTypeSize.accessibility1)
            }
            HStack {
                action("导出日志", id: "demo.exportLogs") {
                    model.exportLogs { text in document = LogDocument(text); exporting = true }
                }.disabled(!model.idle)
                action("清除本机日志", id: "demo.clearLogs", perform: model.clearLogs).disabled(!model.idle)
            }.dynamicTypeSize(...DynamicTypeSize.accessibility1)
            Text("显示最近 160 条。本机两个 1 MiB 文件，最长 7 天；导出保留原始 UTC、会话字段及轮转说明。")
                .font(.caption).foregroundColor(.secondary)
                .fixedSize(horizontal: false, vertical: true)
                .dynamicTypeSize(...DynamicTypeSize.accessibility1)
        }.padding(16)
    }

    private var rawPage: some View {
        ScrollView {
            group("Raw 诊断") {
                Text("输入已确认的设备命令；1–4096 个空白分隔的两位十六进制字节。")
                    .font(.footnote).foregroundColor(.secondary)
                TextEditor(text: $model.rawHex).frame(minHeight: 150)
                    .font(.system(.body, design: .monospaced))
                    .textInputAutocapitalization(.characters).autocorrectionDisabled()
                    .accessibilityLabel("Raw 十六进制字节").accessibilityIdentifier("demo.rawHex")
                    .disabled(!model.idle).id("raw")
                fieldError("raw")
                numberField("份数（与打印页共用，1–20）", text: $model.copies, target: "rawCopies")
                    .disabled(!model.idle)
                action("按所选份数发送 Raw", id: "demo.sendRaw", perform: model.sendRaw)
                    .disabled(model.phase != .ready || !model.idle)
                Text("全部份数作为一个操作串行发送；失败或断开后不重放。Raw 不依赖画布或 FORM，输入不进入日志、保存配置或恢复草稿。")
                    .font(.footnote).foregroundColor(.secondary)
            }.padding(16)
        }.accessibilityIdentifier("demo.rawPage")
    }

    @ViewBuilder private func actionBar(_ proxy: ScrollViewProxy) -> some View {
        if page == .printing || model.phase == .printing || model.phase == .connecting || model.phase == .stopping || model.previewing {
            VStack(spacing: 8) {
                if page == .printing {
                    Button { locate(model.nextHint.target, proxy: proxy) } label: {
                        Text(model.nextHint.message).font(.footnote).frame(maxWidth: .infinity, minHeight: 48, alignment: .leading)
                    }.accessibilityIdentifier("demo.nextHint")
                    HStack {
                        action("生成预览", id: "demo.preview", perform: model.makePreview).disabled(!model.idle)
                            .id("preview")
                        action("打印", id: "demo.sendImage", perform: model.printLabel).disabled(!model.canSend || !model.idle)
                            .id("print")
                    }
                }
                if model.phase == .printing || model.previewing {
                    action("取消任务", id: "demo.cancel", perform: model.cancel)
                }
            }.padding(.horizontal, 16).padding(.vertical, 8)
                .dynamicTypeSize(...DynamicTypeSize.accessibility1)
                .background(Color(UIColor.secondarySystemGroupedBackground))
        }
    }

    private func locate(_ target: String?, proxy: ScrollViewProxy) {
        guard let target else { return }
        page = .printing
        if ["service", "characteristic", "mode", "payload", "advanced"].contains(target) { advanced = true }
        // Footer actions are already visible; only scroll fields inside the content viewport.
        guard target != "preview", target != "print" else { return }
        DispatchQueue.main.async {
            withAnimation { proxy.scrollTo(target, anchor: .center) }
        }
    }

    private func numberField(_ title: String, text: Binding<String>, target: String, signed: Bool = false) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title).font(.subheadline)
            TextField(title, text: text).keyboardType(signed ? .numbersAndPunctuation : .numberPad)
                .textFieldStyle(.roundedBorder).frame(minHeight: 48).accessibilityLabel(title)
                .accessibilityIdentifier("demo.\(target)")
            fieldError(target)
        }.frame(maxWidth: .infinity, alignment: .leading).id(target)
    }

    private func textField(_ title: String, text: Binding<String>, target: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title).font(.subheadline)
            TextField(title, text: text).textInputAutocapitalization(.never).autocorrectionDisabled()
                .textFieldStyle(.roundedBorder).frame(minHeight: 48).accessibilityLabel(title)
            fieldError(target)
        }.id(target)
    }

    @ViewBuilder private func fieldError(_ target: String) -> some View {
        if let message = model.fieldErrors[target == "rawCopies" ? "copies" : target] {
            Text(message).font(.caption).foregroundColor(.red).accessibilityIdentifier("demo.error.\(target)")
        }
    }

    private func group<Content: View>(_ title: String, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(title).font(.headline)
            content()
        }.padding(16).frame(maxWidth: .infinity, alignment: .leading)
            .background(Color(UIColor.secondarySystemGroupedBackground)).cornerRadius(12)
    }

    private func helpButton(_ item: Help) -> some View {
        Button { help = item } label: { Image(systemName: "questionmark.circle").frame(width: 48, height: 48) }
            .accessibilityLabel("\(item.title)说明").accessibilityIdentifier("demo.help.\(item.rawValue)")
    }

    private func action(_ title: String, id: String, perform: @escaping () -> Void) -> some View {
        Button(action: perform) {
            Text(title).multilineTextAlignment(.center).fixedSize(horizontal: false, vertical: true)
                .frame(maxWidth: .infinity, minHeight: 48)
        }
            .buttonStyle(.bordered).accessibilityIdentifier(id)
    }

    private func dismissKeyboard() {
        UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
    }
}

/// Each presentation owns its request ID, including callbacks arriving after dismissal.
private struct DemoFileImporter: View {
    let requestID: UUID
    let completion: (Result<URL, Error>) -> Void
    @State private var presented = false
    @State private var presentationSeen = false
    @State private var didFinish = false

    var body: some View {
        Color.clear.frame(width: 0, height: 0)
            .fileImporter(isPresented: $presented, allowedContentTypes: [.png, .jpeg]) { result in
                guard !didFinish else { return }
                didFinish = true
                completion(result)
            }
            .onChange(of: presented) { value in
                if value { presentationSeen = true }
                else if presentationSeen {
                    // iOS 15's importer does not call onCompletion when the user cancels.
                    // Let a selection completion finish first, then close a cancelled request.
                    DispatchQueue.main.async {
                        guard !didFinish else { return }
                        didFinish = true
                        completion(.failure(CocoaError(.userCancelled)))
                    }
                }
            }
            .onAppear { presented = true }
            .accessibilityHidden(true)
    }
}
