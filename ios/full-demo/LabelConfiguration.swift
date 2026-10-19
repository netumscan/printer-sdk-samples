import Foundation
import PrinterSDK

struct DemoValidationError: LocalizedError {
    let message: String
    let field: String?
    init(message: String, field: String? = nil) { self.message = message; self.field = field }
    var errorDescription: String? { message }
}
enum DemoDocumentKind: Int, Equatable, Sendable { case label = 0, receipt = 1 }

/// Development input only; this is not an ID in the SDK product catalog.
enum M90DevelopmentPreset {
    static let id = "m90-development"
    static let revision = 1
    static let dpi = 203
    static let widthDots = 864
    // Android requests this MTU; iOS uses the system's negotiated write capacity.
    static let requestedMtu = 517
    static let maxWriteBytes = 512

    static func connectionConfiguration() throws -> DemoConnectionConfiguration {
        try DemoConnectionConfiguration(dpi: dpi, deviceWidth: widthDots, kind: .label)
    }
    static func bleOptions() throws -> BLEOptions {
        try M90Profile.bleOptions(maxWriteBytes: maxWriteBytes)
    }
}

enum DemoOriginStrategy: Int, Codable, Equatable, Sendable { case manual = 0, centered = 1 }

/// Layout assumes centered paper. Calibration is explicit and never clamped.
enum DemoHorizontalOrigin {
    static func resolve(deviceWidth: Int, canvasWidth: Int, manualX: Int,
                        strategy: DemoOriginStrategy, adjustment: Int) throws -> Int {
        guard deviceWidth > 0, canvasWidth > 0, canvasWidth <= deviceWidth,
              canvasWidth <= Int.max - 7 else {
            throw DemoValidationError(message: "请填写有效的设备宽度和图片宽度", field: deviceWidth <= 0 ? "deviceWidth" : "width")
        }
        let rasterWidth = (canvasWidth + 7) / 8 * 8
        guard rasterWidth <= deviceWidth else {
            throw DemoValidationError(message: "图片宽度按整字节补齐后不能超过设备最大宽度", field: "width")
        }
        let x: Int
        if strategy == .centered {
            let result = ((deviceWidth - rasterWidth) / 2).addingReportingOverflow(adjustment)
            guard !result.overflow else { throw DemoValidationError(message: "横向校准偏移超出范围", field: "originAdjustment") }
            x = result.partialValue
        } else { x = manualX }
        guard x >= 0, x <= deviceWidth - rasterWidth else {
            throw DemoValidationError(message: "横向起点加图片宽度（按整字节补齐）不能超过设备最大宽度",
                                      field: strategy == .centered ? "originAdjustment" : "xOffset")
        }
        return x
    }
}

/// Explicit printer parameters validated for each print or control submission.
struct DemoConnectionConfiguration: Equatable, Sendable {
    // Demo validation ceiling, not a detected hardware capability.
    static let maximumPageHeight = 65535
    let dpi, deviceWidth: Int
    let kind: DemoDocumentKind

    init(dpi: Int, deviceWidth: Int, kind: DemoDocumentKind) throws {
        guard (1...(kind == .receipt ? 255 : 1200)).contains(dpi) else {
            throw DemoValidationError(message: kind == .receipt ? "小票 DPI 须为 1–255" : "DPI 须为 1–1200", field: "dpi")
        }
        guard (8...7992).contains(deviceWidth), deviceWidth % 8 == 0 else {
            throw DemoValidationError(message: "设备最大宽度须为 8–7992 dots，且为 8 的倍数", field: "deviceWidth")
        }
        self.dpi = dpi; self.deviceWidth = deviceWidth; self.kind = kind
    }
    func matches(_ configuration: LabelConfiguration) -> Bool {
        dpi == configuration.dpi && deviceWidth == configuration.deviceWidth && kind == configuration.kind
    }
    func profile() throws -> CpclProfile {
        try CpclProfile(dpi: UInt32(dpi), widthDots: UInt32(deviceWidth),
                        maxLabelHeightDots: UInt32(Self.maximumPageHeight), form: false)
    }
    func receiptProfile() throws -> ReceiptProfile {
        try ReceiptProfile(dpi: UInt32(dpi), widthDots: UInt32(deviceWidth))
    }
    var description: String {
        "\(kind == .label ? "CPCL" : "ESC/POS") / \(dpi) DPI / 设备最大宽度=\(deviceWidth) dots"
    }
}

/// Diagnostic limits belong to this app, not the printer's capabilities.
struct LabelConfiguration: Equatable, Sendable {
    let dpi, deviceWidth, width, height, copies, xOffsetDots: Int
    let form: Bool
    let kind: DemoDocumentKind
    let trailingBlankDots: Int
    let originStrategy: DemoOriginStrategy
    let originAdjustmentDots: Int

    init(dpi: Int, deviceWidth: Int, width: Int, height: Int, copies: Int, formChoice: Int, xOffsetDots: Int = 0,
         kind: DemoDocumentKind = .label, trailingBlankDots: Int = 0,
         originStrategy: DemoOriginStrategy = .manual, originAdjustmentDots: Int = 0) throws {
        guard (1...1200).contains(dpi) else { throw DemoValidationError(message: "DPI 须为 1–1200", field: "dpi") }
        guard (8...7992).contains(deviceWidth), deviceWidth % 8 == 0 else {
            throw DemoValidationError(message: "设备最大宽度须为 8–7992 dots，且为 8 的倍数", field: "deviceWidth")
        }
        guard width >= 128, width <= deviceWidth else {
            throw DemoValidationError(message: "标签宽度至少 128 dots，且不能超过设备最大宽度", field: "width")
        }
        self.originStrategy = kind == .label ? originStrategy : .manual
        self.originAdjustmentDots = kind == .label ? originAdjustmentDots : 0
        self.xOffsetDots = try DemoHorizontalOrigin.resolve(deviceWidth: deviceWidth, canvasWidth: width,
            manualX: xOffsetDots, strategy: self.originStrategy, adjustment: self.originAdjustmentDots)
        guard (128...65535).contains(height), width * height <= 8_000_000 else {
            throw DemoValidationError(message: "高度须为 128–65535 dots，总像素不得超过 800 万", field: "height")
        }
        guard (1...20).contains(copies) else { throw DemoValidationError(message: "每次测试限 1–20 份", field: "copies") }
        guard kind == .receipt || formChoice == 1 || formChoice == 2 else {
            throw DemoValidationError(message: "请按介质要求明确选择 FORM 策略", field: "form")
        }
        guard (0...65535).contains(trailingBlankDots) else { throw DemoValidationError(message: "结束留白须为 0–65535 dots", field: "trailingBlank") }
        if kind == .receipt {
            guard dpi <= 255, self.xOffsetDots == 0 else {
                throw DemoValidationError(message: "当前小票后端 DPI 不超过 255，横向起点须为 0", field: dpi > 255 ? "dpi" : "xOffset")
            }
        }
        if kind == .label && height + trailingBlankDots > 65535 {
            throw DemoValidationError(message: "标签高度与留白合计不得超过 65535 dots", field: "trailingBlank")
        }
        self.kind = kind; self.trailingBlankDots = trailingBlankDots
        self.dpi = dpi; self.deviceWidth = deviceWidth; self.width = width
        self.height = height; self.copies = copies; self.form = formChoice == 1
    }
    func receiptProfile() throws -> ReceiptProfile { try ReceiptProfile(dpi: UInt32(dpi), widthDots: UInt32(deviceWidth)) }
    var receiptBudget: UInt32 { UInt32((height + trailingBlankDots) * copies) }

    func profile() throws -> CpclProfile {
        // A per-session safety cap, not a claim about the model's maximum page height.
        try CpclProfile(dpi: UInt32(dpi), widthDots: UInt32(deviceWidth),
                        maxLabelHeightDots: UInt32(height + trailingBlankDots), form: form, xOffsetDots: UInt32(xOffsetDots))
    }
    var description: String {
        "\(dpi) DPI / 设备最大宽度=\(deviceWidth) dots / \(kind == .label ? "标签" : "小票图片")=\(width)×\(height) dots / 横向起点=\(xOffsetDots) dots / 横向策略=\(originStrategy == .centered ? "自动居中" : "手动") / 校准偏移=\(originAdjustmentDots) dots / FORM=\(form) / 留白=\(trailingBlankDots) dots"
    }
}

enum LabelRaster {
    /// Border, diagonals, and a 10 mm ruler; tightly packed GRAY8 used by preview and printing.
    static func render(_ config: LabelConfiguration, cancelled: () -> Bool) throws -> Data {
        let width = config.width, height = config.height
        var pixels = Data(repeating: 255, count: width * height)
        let tick = max(1, Int((Double(config.dpi) * 10 / 25.4).rounded()))
        try pixels.withUnsafeMutableBytes { (buffer: UnsafeMutableRawBufferPointer) in
            for y in 0..<height {
                if cancelled() { throw PrinterSDKError(1005) }
                for x in 0..<width {
                    let border = (x >= 2 && x <= width - 3 && (y == 2 || y == height - 3)) ||
                        (y >= 2 && y <= height - 3 && (x == 2 || x == width - 3))
                    let diagonal = y >= 16 && y < height - 32 &&
                        (x == 16 + (y - 16) * (width - 33) / max(1, height - 49) ||
                         x == width - 17 - (y - 16) * (width - 33) / max(1, height - 49))
                    let ruler = x >= 12 && x < width - 12 &&
                        (y == height - 16 || (abs(y - (height - 16)) <= 5 && (x - 12) % tick == 0))
                    if border || diagonal || ruler { buffer[y * width + x] = 0 }
                }
            }
        }
        return pixels
    }
}
