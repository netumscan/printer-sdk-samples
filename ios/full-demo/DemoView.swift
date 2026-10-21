import SwiftUI
import PrinterSDK
import UniformTypeIdentifiers

/// UI bindings and read-only rendering. The Model owns all requests and file workflows.
struct DemoView: View {
    @ObservedObject var model: DemoModel
    @State private var pickerPresentationActive = false
    @State private var presentedRequestID: UUID?
    private var state: DemoViewState { model.viewState }
    var body: some View {
        let export = model.logExport
        return NavigationView {
            Form {
                connectionSection
                profileSection
                imageSection
                printSection
                settingsSection
                extensionSection
                resultSection
                logSection
            }.navigationTitle("打印机联调").disabled(pickerPresentationActive)
        }.navigationViewStyle(.stack)
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
        .fileExporter(isPresented: Binding(get: { model.logExport?.id == export?.id && export != nil }, set: { presented in
            if !presented, let id = export?.id { model.dismissLogExport(id) }
        }), document: export.map { DemoLogDocument(text: $0.text) }, contentType: .plainText,
            defaultFilename: "printer-demo-log", onCompletion: { result in
                if let id = export?.id { model.logExportCompleted(result, requestID: id) }
            })
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
        Section("1. BLE 发现与连接") {
            Button(state.scanning ? "停止扫描" : "扫描 BLE（10 秒）", action: model.toggleScan)
                .disabled(!state.canScan).accessibilityIdentifier("demo.scan")
            Picker("设备", selection: Binding(get: { state.selected }, set: { model.selectDevice($0) })) {
                Text("请选择").tag(Optional<UUID>.none)
                ForEach(state.devices, id: \.identifier) { device in
                    Text(device.name ?? "未命名 BLE 设备").tag(Optional(device.identifier))
                }
            }.disabled(!state.canEditConnection).accessibilityIdentifier("demo.device")
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
        Section("2. 型号与纸张类型") {
            Text(model.connectionInfo?.model?.rawValue ?? "尚未连接")
            Button("采用 LT-M90 型号", action: model.applyPrinterModel).disabled(!model.canWork)
                .accessibilityIdentifier("demo.configure")
            Picker("纸张类型", selection: $model.media) {
                Text("间隙标签纸").tag(PaperType.gapLabel)
                Text("黑标纸").tag(PaperType.blackMark)
                Text("连续纸").tag(PaperType.continuous)
            }.disabled(!model.canWork)
            Button("设置纸张类型", action: model.initializeMedia).disabled(!model.canWork)
            Text(model.mediaInitialized ? "纸张设置已发送；不代表设备确认" : "连接与打印不会自动设置纸张类型")
                .font(.footnote)
        }
    }
    private var imageSection: some View {
        Section("3. PNG / JPEG 图像") {
            HStack {
                Button("从文件选择") { openPicker(.files) }.accessibilityIdentifier("demo.chooseFile")
                Spacer()
                Button("从照片选择") { openPicker(.photos) }.accessibilityIdentifier("demo.choosePhoto")
            }.disabled(!model.canImport)
            if model.imageLoading { ProgressView("正在复制为稳定缓存…") }
            if let image = model.importedImage {
                Text(image.originalFilename).lineLimit(2)
                Text("\(image.widthPx) × \(image.heightPx) px · \(image.encodedByteCount / 1024) KiB").font(.footnote)
                HStack {
                    Button("生成并查看完整预览", action: model.preparePreview).accessibilityIdentifier("demo.preview")
                    Spacer()
                    Button("清除图片", action: model.clearImage)
                }.disabled(!(model.canConnect || model.canWork))
            } else { Text("尚未导入图片；可使用内置棋盘测试图").foregroundStyle(.secondary) }
            if model.previewLoading { ProgressView("正在准备完整画布预览…") }
            Text("空闲连接选图不会重连或重复初始化。取消／失败保留上一图片；导入上限 32 MiB，打印按目标采样。图片与缓存仅本次运行使用。").font(.footnote)
        }
    }
    private func openPicker(_ kind: DemoPickerKind) {
        guard !pickerPresentationActive, let id = model.requestImageImport(kind) else { return }
        presentedRequestID = id; pickerPresentationActive = true
    }
    private var printSection: some View {
        Section("4. 本次打印布局") {
            Group {
                TextField("页面宽度（mm）", text: $model.labelWidthMm).keyboardType(.decimalPad)
                TextField("页面高度（mm）", text: $model.labelHeightMm).keyboardType(.decimalPad)
            }.disabled(!(model.canConnect || model.canWork))
            Text(model.layoutDescription).font(.footnote)
            Button("打印导入图片", action: model.printImage)
                .disabled(!model.canPrint || model.importedImage == nil).accessibilityIdentifier("demo.printImage")
            Button("打印内置测试图", action: model.printLabel).disabled(!model.canPrint).accessibilityIdentifier("demo.printTest")
            Button("取消当前操作", action: model.cancel).disabled(!state.canCancel).accessibilityIdentifier("demo.cancel")
            Text("完整比例居中、透明白底、不裁切、不放大原图。无需预览即可打印。修改布局不会发送 IO；DATA_SENT 只表示传输完成。").font(.footnote)
        }
    }
    private var extensionSection: some View {
        Section("诊断与控制") {
            DisclosureGroup("Raw／走纸／状态／能力") {
                TextField("Raw HEX（ASCII 空白分隔）", text: $model.rawHex)
                    .textInputAutocapitalization(.never).autocorrectionDisabled()
                Button("发送 Raw（一次）", action: model.sendRaw).disabled(!model.canWork)
                Button("定位下一标签", action: model.feed).disabled(!model.canWork)
                Button("查询状态", action: model.queryStatus).disabled(!model.canWork)
                Button("读取能力", action: model.capabilities).disabled(!model.canWork)
                Text("Raw 可能改变纸张设置；必要时重新设置纸张类型。未实现的状态查询原样返回不支持，不推断打印完成。").font(.footnote)
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
