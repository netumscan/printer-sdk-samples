import Foundation
import PrinterSDK

/// App selection only. SDK owns BLE transport and printing parameters.
struct DemoConnectionSettings {
    enum Mode: String, CaseIterable { case automatic, m90 }
    var mode: Mode = .automatic
    init(mode: Mode = .automatic) { self.mode = mode }
    mutating func selectMode(_ mode: Mode) { self.mode = mode }
    var model: PrinterModel? { mode == .m90 ? .ltM90 : nil }
}

struct DemoInputError: LocalizedError, Equatable, Sendable {
    let code: Int32
    init(_ code: Int32) { self.code = code }
    var errorDescription: String? {
        switch code {
        case 1005: return "操作已取消"
        case 1008: return "图片或输入超过演示程序资源上限"
        case 1002: return "请先连接已支持的型号"
        default: return "输入无效，请检查图片和页面尺寸（code=\(code)）"
        }
    }
}
