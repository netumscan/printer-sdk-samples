import Foundation
import CoreGraphics
import PrinterSDK

/// A bounded layout preview. Printing still starts from the immutable source;
/// this app bitmap is never submitted to the SDK as a printing prerequisite.
enum DemoPreviewRenderer {
    static let maximumCanvasPixels = 8_000_000

    static func render(image: DemoImportedImage, widthDots: UInt32, heightDots: UInt32,
                       blankDots: UInt32 = 0, cancellation: DemoImageCancellation) throws -> DemoImagePreview {
        try cancellation.checkCancellation()
        guard widthDots > 0, heightDots > 0 else { throw DemoInputError(1001) }
        let totalHeight = UInt64(heightDots) + UInt64(blankDots)
        guard totalHeight <= UInt64(maximumCanvasPixels),
              UInt64(widthDots) <= UInt64(maximumCanvasPixels) / totalHeight else {
            throw DemoInputError(1008)
        }
        let width = Int(widthDots), contentHeight = Int(heightDots), height = Int(totalHeight)
        let fit = contain(sourceWidth: image.widthPx, sourceHeight: image.heightPx,
            width: width, height: contentHeight)
        // Use the same ceil-rounded source-axis requirements as the SDK decoder.
        let sampleEdge = thumbnailEdge(sourceWidth: image.widthPx, sourceHeight: image.heightPx,
            width: width, height: contentHeight)
        let preview = try image.preview(maxPixelSize: sampleEdge, cancellation: cancellation)
        try cancellation.checkCancellation()
        guard let bitmap = CGContext(data: nil, width: width, height: height, bitsPerComponent: 8,
                bytesPerRow: width * 4, space: CGColorSpaceCreateDeviceRGB(),
                bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue | CGBitmapInfo.byteOrder32Big.rawValue) else {
            throw DemoInputError(1008)
        }
        bitmap.setFillColor(red: 1, green: 1, blue: 1, alpha: 1)
        bitmap.fill(CGRect(x: 0, y: 0, width: width, height: height))
        guard let data = bitmap.data?.assumingMemoryBound(to: UInt8.self) else { throw DemoInputError(1008) }
        guard preview.image.bitsPerPixel == 32, let source = preview.image.dataProvider?.data,
              let sourceBytes = CFDataGetBytePtr(source) else { throw DemoInputError(1001) }
        let rowBytes = preview.image.bytesPerRow
        // Match the SDK's integer nearest-neighbor phase, rather than CGContext's
        // pixel-center mapping, so fine lines do not move between preview and print.
        for y in 0..<fit.height {
            try cancellation.checkCancellation()
            let sy = y * preview.heightPx / fit.height
            for x in 0..<fit.width {
                let sx = x * preview.widthPx / fit.width
                let input = sy * rowBytes + sx * 4
                let output = ((y + fit.y) * width + x + fit.x) * 4
                for channel in 0..<4 { data[output + channel] = sourceBytes[input + channel] }
            }
        }
        withExtendedLifetime(source) {}
        try monochrome(data, width: width, height: contentHeight, cancellation: cancellation)
        try cancellation.checkCancellation()
        guard let output = bitmap.makeImage() else { throw DemoInputError(1008) }
        return DemoImagePreview(image: output)
    }

    private static func thumbnailEdge(sourceWidth: Int, sourceHeight: Int, width: Int, height: Int) -> Int {
        var numerator = min(width, sourceWidth), denominator = sourceWidth
        if height * denominator < numerator * sourceHeight {
            numerator = min(height, sourceHeight); denominator = sourceHeight
        }
        let requiredWidth = max(1, (sourceWidth * numerator + denominator - 1) / denominator)
        let requiredHeight = max(1, (sourceHeight * numerator + denominator - 1) / denominator)
        let edge = max(sourceWidth, sourceHeight)
        var requested = 1
        if requiredWidth > 1 { requested = max(requested, (requiredWidth * edge + sourceWidth - 1) / sourceWidth) }
        if requiredHeight > 1 { requested = max(requested, (requiredHeight * edge + sourceHeight - 1) / sourceHeight) }
        return min(edge, requested)
    }

    // Same fixed threshold and integer luma as
    // imaging/raster/raster_pipeline.cpp. All source alpha is already white-composited.
    // Appended paper-feed rows stay white, including when image pixels are inverted.
    private static func monochrome(_ rgba: UnsafeMutablePointer<UInt8>, width: Int, height: Int,
                                   cancellation: DemoImageCancellation) throws {
        for y in 0..<height {
            try cancellation.checkCancellation()
            for x in 0..<width {
                let offset = (y * width + x) * 4
                let gray = (299 * Int(rgba[offset]) + 587 * Int(rgba[offset + 1]) + 114 * Int(rgba[offset + 2]) + 500) / 1000
                let value: UInt8 = gray < 128 ? 0 : 255
                rgba[offset] = value; rgba[offset + 1] = value; rgba[offset + 2] = value; rgba[offset + 3] = 255
            }
        }
    }

    private static func contain(sourceWidth: Int, sourceHeight: Int, width: Int, height: Int)
        -> (width: Int, height: Int, x: Int, y: Int) {
        var w = sourceWidth, h = sourceHeight
        if width < sourceWidth && width * sourceHeight <= height * sourceWidth {
            w = width; h = max(1, sourceHeight * w / sourceWidth)
        } else if height < sourceHeight {
            h = height; w = max(1, sourceWidth * h / sourceHeight)
        }
        return (w, h, (width - w) / 2, (height - h) / 2)
    }
}
