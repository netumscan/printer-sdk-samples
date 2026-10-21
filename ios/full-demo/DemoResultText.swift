import PrinterSDK

/// Presentation only. Formatting never changes request state or infers printer completion.
enum DemoResultText {
    static func result(_ value: DemoResult?) -> String {
        guard let value else { return "尚无任务结果" }
        let title: String, detail: String?
        switch value {
        case .action(let action, _), .connection(let action, _):
            switch action {
            case .connect: title = "连接"
            case .configure: title = "采用型号"
            case .printLabel: title = "CPCL 打印"
            case .printReceipt: title = "ESC/POS 打印"
            case .raw: title = "Raw"
            case .media: title = "纸张类型"
            case .feed: title = "走纸"
            case .disconnected: title = "意外断连"
            case .disconnect: title = "断开清理"
            }
            detail = nil
        case .status(let result): title = "状态"; detail = status(result.value)
        case .capabilities(let result): title = "能力"; detail = capabilities(result.value)
        case .identity(let result): title = "SDK 身份"; detail = result.value.map(identity)
        case .discovery(let result): title = "BLE 发现"; detail = result.value.map { "候选设备=\($0.count)" }
        }
        return title + "\n" + terminal(value.evidence) + (detail.map { "\n" + $0 } ?? "")
    }
    static func terminal<Value>(_ result: ManagerResult<Value>) -> String { terminal(DemoResultEvidence(result)) }
    private static func terminal(_ result: DemoResultEvidence) -> String {
        let level: String
        switch result.completion {
        case .none: level = "NONE"
        case .dataSent: level = "DATA_SENT"
        case .deviceConfirmed: level = "DEVICE_CONFIRMED"
        }
        return "\(result.outcome) · code=\(result.code) · \(level)\n\(result.message)\n" +
            (result.outputMayHaveOccurred ? "可能已有输出；取消或断开不能撤回" : "未报告可能输出")
    }
    static func status(_ value: StatusResult?) -> String? {
        value.map { "缺纸=\(known($0.paperOut))，开盖=\(known($0.coverOpen))，将尽=\(known($0.paperNearEnd))，切刀错误=\(known($0.cutterError))" }
    }
    private static func known(_ value: Bool?) -> String { value.map { $0 ? "是" : "否" } ?? "未知" }
    static func capabilities(_ value: PrinterCapabilities?) -> String? {
        value.map { "图像=\(support($0.imagePrinting))，纸型=\(support($0.paperTypeSelection))，定位=\(support($0.labelPositioning))，状态=\(support($0.statusQuery))，Raw=\(support($0.rawCommands))" }
    }
    private static func support(_ value: Support) -> String {
        switch value { case .unknown: return "未知"; case .unsupported: return "不支持"; case .supported: return "支持" }
    }
    static func identity(_ value: SDKInfo?) -> String {
        guard let value else { return "SDK 身份未知；等待 sdkInfo 回调" }
        return "SDK \(value.distributionVersion) · native \(value.nativeVersion)\nbuild=\(value.buildID ?? "未知")"
    }
}
