import XLPrinterSDK

/// Presentation only. Formatting never changes request state or infers printer completion.
enum DemoResultText {
    static func requestSummary(_ value: DemoResult) -> String {
        switch value.evidence.outcome {
        case .failure: return "请求失败；详见原始结果"
        case .cancelled: return "请求已取消；详见原始结果"
        case .success: break
        }
        switch value {
        case .action(.getStatus, _): return "状态查询回复已校验；盖纸状态由状态监听更新"
        case .capabilities: return "能力已读取；未知项保持未知"
        default:
            switch value.evidence.completion {
            case .dataSent:
                switch value {
                case .action(.printLabel, _), .action(.printReceipt, _):
                    return "打印数据已发送；DATA_SENT 只表示传输完成，请核实实际纸面结果"
                default: return "数据已发送；DATA_SENT 只表示传输完成，请核对设备实际执行结果"
                }
            case .deviceConfirmed: return "设备已确认请求；详见原始结果"
            case .none: return "请求成功；详见原始结果"
            }
        }
    }
    static func title(_ value: DemoResult) -> String {
        switch value {
        case .action(let action, _), .connection(let action, _):
            switch action {
            case .connect: return "连接"
            case .configure: return "采用型号"
            case .printLabel: return "CPCL 打印"
            case .printReceipt: return "ESC/POS 打印"
            case .raw: return "Raw"
            case .media: return "纸张类型"
            case .feed: return "走纸"
            case .getStatus: return "查询状态"
            case .disconnected: return "意外断连"
            case .disconnect: return "断开清理"
            }
        case .status: return "状态"
        case .capabilities: return "能力"
        case .identity: return "SDK 身份"
        case .discovery: return "BLE 发现"
        }
    }
    static func result(_ value: DemoResult?) -> String {
        guard let value else { return "尚无任务结果" }
        let detail: String?
        switch value {
        case .action, .connection: detail = nil
        case .status(let result): detail = status(result.value)
        case .capabilities(let result): detail = capabilities(result.value)
        case .identity(let result): detail = result.value.map(identity)
        case .discovery(let result): detail = result.value.map { "候选设备=\($0.count)" }
        }
        return title(value) + "\n" + terminal(value.evidence) + (detail.map { "\n" + $0 } ?? "")
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
    static func statusRecovery(previous: StatusResult?, current: StatusResult) -> String? {
        var changes: [String] = []
        if previous?.coverOpen == true, current.coverOpen == false { changes.append("上盖已关闭") }
        if previous?.paperOut == true, current.paperOut == false { changes.append("纸张已恢复") }
        guard !changes.isEmpty else { return nil }
        if current.coverOpen == true { changes.append("上盖仍打开") }
        if current.paperOut == true { changes.append("仍缺纸") }
        if current.coverOpen == nil { changes.append("上盖状态未知") }
        if current.paperOut == nil { changes.append("纸张状态未知") }
        return changes.joined(separator: "；")
    }
    static func sdkVersion(_ value: SDKInfo?) -> String {
        value.map { "SDK \($0.distributionVersion)" } ?? "SDK 版本未知"
    }
    static func printerInfo(_ value: PrinterInfo?) -> String? {
        guard let value else { return nil }
        func number<T>(_ value: T?) -> String { value.map { String(describing: $0) } ?? "未知" }
        return "厂商=\(value.manufacturer ?? "未知")，设备名称=\(value.deviceName ?? "未知")\n" +
            "UID=\(value.uidHex ?? "未知")（非产品序列号）\n" +
            "报告版本=\(number(value.reportedVersion))（含义待确认），宽度=\(number(value.maxPrintWidthDots)) dots，DPI=\(number(value.reportedDpi))\n" +
            "电量=\(number(value.batteryPercent))%，外部供电=\(known(value.externalPower))，打印头温度=\(number(value.printheadTemperatureCelsius)) °C\n" +
            "粗略异常=\(known(value.hasError))，纸型代码=\(number(value.paperTypeCode))，速度级别=\(number(value.speedLevel))，浓度级别=\(number(value.densityLevel))\n" +
            "RTC=\(known(value.supportsRtc))，NFC=\(known(value.supportsNfc))，压缩=\(known(value.supportsCompression))\n" +
            (value.protocolVerified ? "协议已验证" : "JXIG 连接信息快照；布局通用性与部分字段含义待协议方确认")
    }
    private static func known(_ value: Bool?) -> String { value.map { $0 ? "是" : "否" } ?? "未知" }
    static func capabilities(_ value: PrinterCapabilities?) -> String? {
        value.map { "图像=\(support($0.imagePrinting))，纸型=\(support($0.paperTypeSelection))，定位=\(support($0.labelPositioning))，状态查询=\(support($0.statusQuery))，状态上报=\(support($0.statusReporting))，Raw=\(support($0.rawCommands))" }
    }
    private static func support(_ value: Support) -> String {
        switch value { case .unknown: return "未知"; case .unsupported: return "不支持"; case .supported: return "支持" }
    }
    static func identity(_ value: SDKInfo?) -> String {
        guard let value else { return "SDK 身份未知；等待 sdkInfo 回调" }
        return "SDK \(value.distributionVersion) · native \(value.nativeVersion)\nbuild=\(value.buildID ?? "未知")"
    }
}
