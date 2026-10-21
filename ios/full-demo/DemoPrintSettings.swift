import Foundation
import PrinterSDK

/// App preview geometry for the supported LT-M90. SDK independently validates every print.
struct DemoPrintSettings: Sendable {
    let widthMm, heightMm: Double
    let widthDots, heightDots: UInt32
    init(widthMm: String, heightMm: String, model: PrinterModel = .ltM90) throws {
        guard let width = Double(widthMm), let height = Double(heightMm),
              width.isFinite, height.isFinite, width > 0, height > 0 else { throw DemoInputError(1001) }
        switch model {
        case .ltM90:
            let w = (width * 8).rounded(.toNearestOrAwayFromZero)
            let h = (height * 8).rounded(.toNearestOrAwayFromZero)
            guard w >= 1, w <= 864, h >= 1, h <= 65535 else { throw DemoInputError(1001) }
            widthDots = UInt32(w); heightDots = UInt32(h)
        }
        self.widthMm = width; self.heightMm = height
    }
}
