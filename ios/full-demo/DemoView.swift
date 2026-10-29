import SwiftUI
import XLPrinterSDK
import UniformTypeIdentifiers

/// UI bindings and read-only rendering. The Model owns all requests and file workflows.
struct DemoView: View {
    private enum Page: String, Identifiable {
        case device, paper, more
        var id: String { rawValue }
        var title: String {
            switch self {
            case .device: return "连接与设备"
            case .paper: return "纸张设置"
            case .more: return "更多"
            }
        }
    }
    @ObservedObject var model: DemoModel
    @State private var pickerPresentationActive = false
    @State private var presentedRequestID: UUID?
    @State private var presentedPage: Page?
    private var state: DemoViewState { model.viewState }
    var body: some View {
        NavigationView {
            Form {
                deviceSummarySection
                imageSection
                paperSummarySection
                requestSummarySection
            }
            .safeAreaInset(edge: .bottom, spacing: 0) { printActions }
            .navigationTitle("NETUM 打印 Demo").navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("更多") { presentedPage = .more }
                        .disabled(model.imageLoading || model.previewLoading)
                        .accessibilityIdentifier("demo.more")
                }
            }
            .disabled(pickerPresentationActive)
        }.navigationViewStyle(.stack)
        .sheet(item: $presentedPage) { page in pageView(page) }
        .sheet(item: Binding(get: { model.pickerRequest }, set: { value in
            if value == nil, let id = presentedRequestID, model.pickerRequest?.id == id { model.cancelImageFlow() }
        }), onDismiss: { pickerPresentationActive = false; presentedRequestID = nil }) { request in
            DemoImagePicker(kind: request.kind, importer: model.makePickerImporter(request.id),
                onBegin: { model.beginPickerLoad(request.id) },
                onFile: { model.receiveImageFile($0, requestID: request.id) },
                onImported: { model.receiveImportedImage($0, requestID: request.id) })
        }
        .fullScreenCover(isPresented: Binding(get: { model.imagePreview != nil }, set: { presented in
            if !presented { model.invalidatePreview() }
        })) {
            if let preview = model.imagePreview { DemoFullPreview(preview: preview) { model.invalidatePreview() } }
        }
    }
    private func pageView(_ page: Page) -> some View {
        let export = model.logExport
        return NavigationView {
            Form {
                switch page {
                case .device:
                    connectionSection
                    realtimeStatusSection
                    profileSection
                    printerInfoSection
                case .paper:
                    paperSettingsSection
                case .more:
                    extensionSection
                    settingsSection
                    Section {
                        NavigationLink("原始请求结果") { Form { resultSection }.navigationTitle("原始请求结果") }
                        NavigationLink("运行日志") { Form { logSection }.navigationTitle("运行日志") }
                    }
                    Section("关于") {
                        Text(DemoResultText.identity(state.identity)).font(.footnote).textSelection(.enabled)
                            .accessibilityIdentifier("demo.sdkVersion")
                    }
                }
                requestSummarySection
                if page == .device, let result = model.configurationResult {
                    Section("最近型号设置结果") {
                        Text(DemoResultText.result(result)).textSelection(.enabled)
                    }
                }
                if page == .paper, let result = model.initializationResult {
                    Section("最近纸张设置结果") {
                        Text(DemoResultText.result(result)).textSelection(.enabled)
                    }
                }
            }
            .navigationTitle(page.title).navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) { Button("完成") { presentedPage = nil } }
            }
        }.navigationViewStyle(.stack)
        .safeAreaInset(edge: .bottom, spacing: 0) {
            if state.canCancel {
                VStack(spacing: 4) {
                    cancelButton
                    Text(printActionHint).font(.caption).foregroundStyle(.secondary)
                }.padding(.horizontal).padding(.vertical, 10).background(.regularMaterial)
            }
        }
        .fileExporter(isPresented: Binding(get: { model.logExport?.id == export?.id && export != nil }, set: { presented in
            if !presented, let id = export?.id { model.dismissLogExport(id) }
        }), document: export.map { DemoLogDocument(text: $0.text) }, contentType: .plainText,
            defaultFilename: "printer-demo-log", onCompletion: { result in
                if let id = export?.id { model.logExportCompleted(result, requestID: id) }
            })
    }
    private var deviceSummarySection: some View {
        Section("打印机") {
            Button { presentedPage = .device } label: {
                HStack(spacing: 12) {
                    Image(systemName: "printer").font(.title2)
                    VStack(alignment: .leading, spacing: 4) {
                        Text(model.connectionInfo.map { $0.device.name ?? "未命名打印机" } ?? "连接打印机")
                            .foregroundStyle(.primary)
                        Text(connectionSummary).font(.caption).foregroundStyle(.secondary)
                    }
                    Spacer()
                    Image(systemName: "chevron.right").font(.caption).foregroundStyle(.secondary)
                }.padding(.vertical, 4)
            }
            .disabled(model.imageLoading || model.previewLoading)
            .accessibilityIdentifier("demo.openDevice")
            HStack {
                statusIndicator("盖", value: model.realtimeStatus?.coverOpen,
                    warning: "开盖", normal: "关盖", identifier: "demo.coverStatus")
                Spacer()
                statusIndicator("纸", value: model.realtimeStatus?.paperOut,
                    warning: "缺纸", normal: "有纸", identifier: "demo.paperStatus")
            }
            recoveryNotice
        }
    }
    private var connectionSummary: String {
        switch model.phase {
        case .offline, .closed: return "未连接 · BLE"
        case .connecting: return "正在连接…"
        case .disconnecting: return "正在断开…"
        case .ready, .working: return "已连接 · BLE · \(model.connectionInfo?.model?.rawValue ?? "型号未知")"
        }
    }
    private var paperSummarySection: some View {
        Section("页面") {
            Button { presentedPage = .paper } label: {
                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("\(model.labelWidthMm) × \(model.labelHeightMm) mm").foregroundStyle(.primary)
                        Text("纸型草稿：\(paperTypeTitle)").font(.caption).foregroundStyle(.secondary)
                    }
                    Spacer()
                    Image(systemName: "chevron.right").font(.caption).foregroundStyle(.secondary)
                }
            }.disabled(model.imageLoading || model.previewLoading)
                .accessibilityIdentifier("demo.openPaper")
        }
    }
    private var requestSummarySection: some View {
        Section("当前操作与最近结果") {
            Text(state.status).accessibilityIdentifier("demo.status")
            if let error = state.inputError { Text(error.localizedDescription).foregroundStyle(.red) }
            if let result = state.result {
                if result.evidence.outcome == .success {
                    Text("\(DemoResultText.title(result))：\(DemoResultText.requestSummary(result))")
                        .font(.footnote).foregroundStyle(.secondary)
                } else {
                    Text(DemoResultText.result(result)).font(.footnote).textSelection(.enabled)
                }
            }
        }
    }
    private var printActions: some View {
        VStack(spacing: 8) {
            if state.canCancel {
                cancelButton
            } else {
                Button(action: model.printCurrentContent) {
                    Text(model.importedImage == nil ? "打印内置测试图" : "打印当前图片")
                        .frame(maxWidth: .infinity, minHeight: 44)
                }.buttonStyle(.borderedProminent).disabled(!model.canPrint)
                    .accessibilityIdentifier(model.importedImage == nil ? "demo.printTest" : "demo.printImage")
            }
            Text(printActionHint)
                .font(.caption).foregroundStyle(.secondary)
        }.padding(.horizontal).padding(.vertical, 10)
            .background(.regularMaterial)
            .overlay(alignment: .top) { Divider() }
    }
    private var cancelButton: some View {
        Button("取消当前操作", action: model.cancel)
            .frame(maxWidth: .infinity, minHeight: 44)
            .accessibilityIdentifier("demo.cancel")
    }
    private var printActionHint: String {
        if model.imageLoading { return "正在导入图片，可取消当前操作" }
        if model.previewLoading { return "正在生成预览，可取消当前操作" }
        if model.phase == .connecting { return "正在连接，可取消当前操作" }
        if state.canCancel { return "正在处理；取消不能撤回已发送的数据" }
        if state.inputError != nil { return "请检查上方提示后重试" }
        if model.canPrint { return "数据发送完成后，请核实实际纸面结果" }
        if model.phase == .disconnecting { return "正在断开，请稍候" }
        if model.phase == .ready { return "请在设备详情中采用支持的打印机型号" }
        return "连接支持的打印机后即可打印"
    }
    private var settingsSection: some View {
        Section("参数草稿") {
            HStack {
                Button("保存参数", action: model.saveSettings).accessibilityIdentifier("demo.saveSettings")
                Spacer()
                Button("加载参数", action: model.loadSettings).accessibilityIdentifier("demo.loadSettings")
                Spacer()
                Button("清除保存", action: model.clearSavedSettings).accessibilityIdentifier("demo.clearSettings")
            }.disabled(!model.canManageSettings)
            if model.settingsBusy { ProgressView("正在操作本机参数…") }
            Text(model.settingsStatus).font(.footnote).accessibilityIdentifier("demo.settingsStatus")
            Text("断开并空闲后操作；只保存型号选择、纸型与页面尺寸，不保存设备、Raw、图片或任务。加载不会连接、采用型号或打印。").font(.footnote)
        }
    }
    private var logSection: some View {
        Section("开发日志") {
            HStack {
                Button("导出日志", action: model.prepareLogExport).accessibilityIdentifier("demo.exportLogs")
                Spacer()
                Button("清除日志", action: model.clearLogs).accessibilityIdentifier("demo.clearLogs")
            }.disabled(model.logBusy || model.phase == .closed)
            if model.logBusy { ProgressView("正在准备日志…") }
            DemoLogRecordsView(records: model.logRecords)
            Text(model.logStatus).font(.footnote)
            Text("显示最近 160 条；文件最多 2 × 1 MiB，保留 7 天。日志记录操作结果，不包含设备身份、图片路径或 Raw 数据。").font(.footnote)
        }
    }
    private var connectionSection: some View {
        Section("BLE 发现与连接") {
            Button(state.scanStopping ? "正在停止…" : state.scanning ? "停止扫描" : "扫描 BLE（10 秒）", action: model.toggleScan)
                .disabled(!state.canScan).accessibilityIdentifier("demo.scan")
            Text(state.scanMessage).font(.footnote).accessibilityIdentifier("demo.scanStatus")
            VStack(alignment: .leading, spacing: 12) {
                ForEach(state.deviceRows) { row in
                    Button {
                        model.selectDevice(row.isSelected ? nil : row.id)
                    } label: {
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(row.title).foregroundStyle(.primary)
                                Text(row.detail).font(.caption).foregroundStyle(.secondary)
                            }
                            Spacer()
                            if row.isSelected { Image(systemName: "checkmark.circle.fill").accessibilityHidden(true) }
                        }.frame(maxWidth: .infinity, alignment: .leading).contentShape(Rectangle())
                    }.buttonStyle(.plain).disabled(!state.canSelectDevice)
                        .accessibilityIdentifier("demo.device.\(row.id.uuidString)")
                        .accessibilityValue(row.isSelected ? "已选择" : "未选择")
                }
            }
            if state.scanning {
                Text("候选会陆续出现。可先选择，结束或停止扫描后再手动连接。").font(.footnote)
            }
            HStack {
                Button("连接所选设备", action: model.connect).disabled(!state.canConnect).accessibilityIdentifier("demo.connect")
                Spacer()
                Button("断开", action: model.disconnect).disabled(!state.canDisconnect).accessibilityIdentifier("demo.disconnect")
            }
            Picker("型号选择", selection: Binding(get: { model.connectionSettings.mode }, set: { model.selectConnectionMode($0) })) {
                Text("按原始名称匹配").tag(DemoConnectionSettings.Mode.automatic)
                Text("明确选择 LT-M90").tag(DemoConnectionSettings.Mode.m90)
            }.disabled(!state.canEditConnection)
            Text("改名设备可明确选择已收录型号；未知型号不猜测通信参数。").font(.footnote)
        }
    }
    private var profileSection: some View {
        Section("设备型号") {
            Text(model.connectionInfo?.model?.rawValue ?? "尚未连接")
            Button("采用 LT-M90 型号", action: model.applyPrinterModel).disabled(!model.canWork)
                .accessibilityIdentifier("demo.configure")
            Text("连接结果已包含采用的型号；仅在需要更正时操作。").font(.footnote)
        }
    }
    private var imageSection: some View {
        Section("当前打印内容") {
            if let image = model.importedImage {
                HStack {
                    Image(systemName: "photo").font(.largeTitle).foregroundStyle(.secondary)
                    VStack(alignment: .leading, spacing: 4) {
                        Text(image.originalFilename).lineLimit(2)
                        Text("\(image.widthPx) × \(image.heightPx) px").font(.caption).foregroundStyle(.secondary)
                    }
                }.padding(.vertical, 8).accessibilityIdentifier("demo.currentContent")
                Button("生成并查看完整预览", action: model.preparePreview)
                    .disabled(!(model.canConnect || model.canWork)).accessibilityIdentifier("demo.preview")
                Button("使用内置测试图", action: model.clearImage)
                    .disabled(!(model.canConnect || model.canWork)).accessibilityIdentifier("demo.useSample")
            } else {
                HStack(spacing: 12) {
                    Image(systemName: "square.grid.3x3.fill").font(.largeTitle).foregroundStyle(.secondary)
                    VStack(alignment: .leading, spacing: 4) {
                        Text("内置棋盘测试图")
                        Text("无需导入图片，可直接体验打印").font(.caption).foregroundStyle(.secondary)
                    }
                }.padding(.vertical, 8).accessibilityIdentifier("demo.currentContent")
            }
            HStack {
                Button("从文件选择") { openPicker(.files) }.accessibilityIdentifier("demo.chooseFile")
                Spacer()
                Button("从照片选择") { openPicker(.photos) }.accessibilityIdentifier("demo.choosePhoto")
            }.disabled(!model.canImport)
            if model.imageLoading { ProgressView("正在导入图片…") }
            if model.previewLoading { ProgressView("正在准备完整画布预览…") }
            Text("支持 PNG / JPEG。导入成功后切换内容；取消或失败保留当前内容。预览可选。").font(.footnote).foregroundStyle(.secondary)
        }
    }
    private var printerInfoSection: some View {
        Section("设备信息（JXIG 连接快照）") {
            if let text = DemoResultText.printerInfo(model.printerInfo) {
                Text(text).font(.footnote).textSelection(.enabled)
                    .accessibilityIdentifier("demo.printerInfo")
            } else { Text("尚无本次连接的设备信息").foregroundStyle(.secondary) }
            Text("每次连接后自动读取一次；未读到信息仍可打印。信息为连接快照，部分字段含义待确认；未知字段为空，实时开盖、缺纸见状态区。").font(.footnote)
        }
    }
    private var realtimeStatusSection: some View {
        Section("打印机状态") {
            HStack(alignment: .firstTextBaseline) {
                Text("打印机实时状态").font(.headline)
                Spacer()
                Button(action: model.getStatus) {
                    Text("查询状态").frame(minHeight: 44).contentShape(Rectangle())
                }.buttonStyle(.bordered).disabled(!model.canWork)
                    .accessibilityIdentifier("demo.queryStatus")
            }
            statusIndicator("盖状态", value: model.realtimeStatus?.coverOpen,
                warning: "开盖", normal: "关盖", identifier: "demo.coverStatus")
            statusIndicator("纸张状态", value: model.realtimeStatus?.paperOut,
                warning: "缺纸", normal: "有纸", identifier: "demo.paperStatus")
            Text(statusConnectionHint).font(.caption).foregroundStyle(.secondary)
        }
    }
    @ViewBuilder private var recoveryNotice: some View {
        if let notice = model.statusRecoveryNotice {
            let emphasis = DemoStatusEmphasis(status: model.realtimeStatus)
            HStack(alignment: .top) {
                Label(notice.message, systemImage: emphasis.symbol)
                    .font(.subheadline).foregroundColor(statusColor(emphasis))
                    .accessibilityIdentifier("demo.statusRecovery")
                Spacer()
                Button { model.dismissStatusRecoveryNotice(notice.id) } label: {
                    Image(systemName: "xmark").frame(minWidth: 44, minHeight: 44)
                        .contentShape(Rectangle()).accessibilityLabel("关闭状态提示")
                }.buttonStyle(.plain).accessibilityIdentifier("demo.dismissStatusRecovery")
            }
        }
    }
    private func statusIndicator(_ title: String, value: Bool?, warning: String, normal: String,
                                 identifier: String) -> some View {
        let emphasis = DemoStatusEmphasis(value)
        return HStack(spacing: 5) {
            Image(systemName: emphasis.symbol).accessibilityHidden(true)
            Text("\(title)：\(value.map { $0 ? warning : normal } ?? "未知")")
                .accessibilityIdentifier(identifier)
        }.font(.subheadline.weight(.semibold)).foregroundColor(statusColor(emphasis))
    }
    private func statusColor(_ emphasis: DemoStatusEmphasis) -> Color {
        switch emphasis {
        case .warning: return .red
        case .normal: return .green
        case .unknown: return .secondary
        }
    }
    private var statusConnectionHint: String {
        switch model.phase {
        case .offline, .closed: return "未连接；盖纸状态未知"
        case .connecting: return "正在连接；等待有效状态"
        case .disconnecting: return "正在断开；已清除本次状态"
        case .ready, .working:
            if model.realtimeStatus?.coverOpen == nil || model.realtimeStatus?.paperOut == nil {
                return "已连接；等待有效盖纸状态，可在空闲时查询"
            }
            return "本次连接最近的上报／查询结果；状态不代表打印完成"
        }
    }
    private func openPicker(_ kind: DemoPickerKind) {
        guard !pickerPresentationActive, let id = model.requestImageImport(kind) else { return }
        presentedRequestID = id; pickerPresentationActive = true
    }
    private var paperTypeTitle: String {
        switch model.media {
        case .gapLabel: return "间隙标签纸"
        case .blackMark: return "黑标纸"
        case .continuous: return "连续纸"
        }
    }
    @ViewBuilder private var paperSettingsSection: some View {
        Section("本次页面尺寸") {
            Group {
                TextField("页面宽度（mm）", text: $model.labelWidthMm).keyboardType(.decimalPad)
                TextField("页面高度（mm）", text: $model.labelHeightMm).keyboardType(.decimalPad)
            }.disabled(!(model.canConnect || model.canWork))
            Text(model.layoutDescription).font(.footnote)
            Text("修改尺寸只更新本次布局，不发送打印机命令。完整比例居中、透明白底、不裁切、不放大原图。").font(.footnote)
        }
        Section("打印机纸张类型") {
            Picker("纸张类型", selection: $model.media) {
                Text("间隙标签纸").tag(PaperType.gapLabel)
                Text("黑标纸").tag(PaperType.blackMark)
                Text("连续纸").tag(PaperType.continuous)
            }.disabled(!model.canWork)
            Button("应用纸张类型", action: model.initializeMedia).disabled(!model.canWork)
            Text(model.mediaInitialized ? "纸张设置已发送；不代表设备确认" : "按实际装纸选择并显式应用；连接与打印不会自动设置纸张类型")
                .font(.footnote)
        }
        Section("标签定位") {
            Button("定位下一标签", action: model.feed).disabled(!model.canWork)
            Text("会走纸；独立执行，打印不会自动定位下一标签。").font(.footnote)
        }
    }
    private var extensionSection: some View {
        Section("诊断与控制") {
            DisclosureGroup("Raw／能力") {
                TextField("Raw HEX（ASCII 空白分隔）", text: $model.rawHex)
                    .textInputAutocapitalization(.never).autocorrectionDisabled()
                Button("发送 Raw（一次）", action: model.sendRaw).disabled(!model.canWork)
                Button("读取能力", action: model.capabilities).disabled(!model.canWork)
                Text("设备详情中的“查询状态”主动发送指令；不支持或失败保留原始错误。").font(.footnote)
                Text("Raw 可能改变纸张设置；必要时重新设置纸张类型。").font(.footnote)
            }
        }
    }
    private var resultSection: some View {
        Section("状态与结果") {
            Text(state.status).accessibilityIdentifier("demo.status")
            if let error = state.inputError { Text(error.localizedDescription).foregroundStyle(.red) }
            Text(DemoResultText.result(state.result)).textSelection(.enabled)
            if let result = model.configurationResult { Text(DemoResultText.result(result)).textSelection(.enabled) }
            if let result = model.initializationResult { Text(DemoResultText.result(result)).textSelection(.enabled) }
            if let result = state.connectionLoss { Text(DemoResultText.result(result)).textSelection(.enabled) }
            if let result = state.cleanupResult { Text(DemoResultText.result(result)).textSelection(.enabled) }
            Text(DemoResultText.identity(state.identity)).font(.footnote)
            Text("进入后台会取消图片工作并断开；返回后手动连接，不重放请求。").font(.footnote)
        }
    }
}

private struct DemoFullPreview: View {
    let preview: DemoImagePreview
    let close: () -> Void
    @State private var zoom = 1.0
    var body: some View {
        NavigationView {
            VStack {
                Text("\(preview.widthPx) × \(preview.heightPx) dots · 完整画布预览").font(.footnote)
                GeometryReader { geometry in
                    let fit = min(1, min(geometry.size.width / CGFloat(preview.widthPx),
                                         geometry.size.height / CGFloat(preview.heightPx)))
                    ScrollView([.horizontal, .vertical]) {
                        Image(decorative: preview.image, scale: 1).resizable().interpolation(.none)
                            .frame(width: CGFloat(preview.widthPx) * fit * zoom,
                                   height: CGFloat(preview.heightPx) * fit * zoom)
                            .accessibilityLabel("完整内容画布，结束留白单独计入打印")
                    }
                }
                Slider(value: $zoom, in: 1...6).padding().accessibilityLabel("预览缩放")
                Text("预览不代表纸面效果或机械打印完成").font(.footnote)
            }.navigationTitle("打印预览").navigationBarTitleDisplayMode(.inline)
                .toolbar { ToolbarItem(placement: .confirmationAction) { Button("关闭", action: close) } }
        }.navigationViewStyle(.stack)
    }
}
