import Foundation
import CoreGraphics
import ImageIO
import PrinterSDK

/// A private copy exists only for the current run; the original file is never logged.
struct DemoImageSource: Sendable {
    fileprivate let url: URL
    let width: Int
    let height: Int
}

struct DemoPreparedImage: Sendable {
    let configuration: LabelConfiguration
    let pixels: Data

    init(configuration: LabelConfiguration, pixels: Data) throws {
        guard pixels.count == configuration.width * configuration.height else {
            throw DemoValidationError(message: "图片准备结果尺寸不一致")
        }
        self.configuration = configuration
        self.pixels = pixels
    }
}

/// The recursive lock protects cleanup and owned-file state for every public operation.
/// The Demo still schedules expensive operations on its serial preparation queue.
final class DemoImageStore: @unchecked Sendable {
    static let maximumFileBytes = 32 * 1024 * 1024
    static let maximumSourcePixels = 64_000_000
    static let maximumSourceEdge = 32768
    private let root: URL
    private let lock = NSRecursiveLock()
    private var cleaned = false
    private var owned: Set<URL> = []

    init(root: URL? = nil) {
        self.root = root ?? FileManager.default.temporaryDirectory.appendingPathComponent("printer-demo-import", isDirectory: true)
    }

    deinit {
        lock.lock(); defer { lock.unlock() }
        for url in owned { try? FileManager.default.removeItem(at: url) }
    }

    /// Schedule at app startup on the preparation queue, even before any image is selected.
    func cleanup() {
        lock.lock(); defer { lock.unlock() }
        cleanupOnce()
    }

    private func cleanupOnce() {
        guard !cleaned else { return }
        cleaned = true
        // Called on the preparation queue, never the UI thread.
        if let old = try? FileManager.default.contentsOfDirectory(at: root, includingPropertiesForKeys: nil) {
            for url in old { try? FileManager.default.removeItem(at: url) }
        }
    }

    func importImage(_ url: URL, cancelled: () -> Bool) throws -> DemoImageSource {
        lock.lock(); defer { lock.unlock() }
        cleanupOnce()
        let access = url.startAccessingSecurityScopedResource()
        defer { if access { url.stopAccessingSecurityScopedResource() } }
        let destination = root.appendingPathComponent(UUID().uuidString)
        do {
            try FileManager.default.createDirectory(at: root, withIntermediateDirectories: true)
            let input = try FileHandle(forReadingFrom: url)
            defer { try? input.close() }
            guard FileManager.default.createFile(atPath: destination.path, contents: nil) else {
                throw DemoValidationError(message: "无法创建图片临时副本")
            }
            let output = try FileHandle(forWritingTo: destination)
            defer { try? output.close() }
            var size = 0
            while true {
                if cancelled() { throw PrinterSDKError(1005) }
                guard let chunk = try input.read(upToCount: 64 * 1024), !chunk.isEmpty else { break }
                size += chunk.count
                guard size <= Self.maximumFileBytes else { throw DemoValidationError(message: "图片文件不得超过 32 MiB") }
                try output.write(contentsOf: chunk)
            }
            try output.synchronize()
            guard let image = CGImageSourceCreateWithURL(destination as CFURL, [kCGImageSourceShouldCache: false] as CFDictionary),
                  let type = CGImageSourceGetType(image), ["public.png", "public.jpeg"].contains(type as String),
                  CGImageSourceGetCount(image) == 1,
                  let properties = CGImageSourceCopyPropertiesAtIndex(image, 0, nil) as? [CFString: Any],
                  CGImageSourceGetStatusAtIndex(image, 0) == .statusComplete,
                  let width = properties[kCGImagePropertyPixelWidth] as? Int,
                  let height = properties[kCGImagePropertyPixelHeight] as? Int else {
                throw DemoValidationError(message: "仅支持完整的单帧 PNG/JPEG 图片")
            }
            guard width > 0, height > 0, width <= Self.maximumSourceEdge, height <= Self.maximumSourceEdge,
                  width <= Self.maximumSourcePixels / height else {
                throw DemoValidationError(message: "原图不得超过 6400 万像素或单边 32768 像素")
            }
            let orientation = properties[kCGImagePropertyOrientation] as? Int ?? 1
            guard (1...8).contains(orientation) else { throw DemoValidationError(message: "图片方向信息无效") }
            if cancelled() { throw PrinterSDKError(1005) }
            owned.insert(destination)
            return DemoImageSource(url: destination,
                width: orientation >= 5 ? height : width, height: orientation >= 5 ? width : height)
        } catch {
            try? FileManager.default.removeItem(at: destination)
            if let error = error as? DemoValidationError { throw error }
            if let error = error as? PrinterSDKError { throw error }
            // Native file errors include paths and names; expose a fixed safe diagnostic.
            throw DemoValidationError(message: "图片读取失败；请检查文件与访问权限")
        }
    }

    func discard(_ source: DemoImageSource) {
        lock.lock(); defer { lock.unlock() }
        owned.remove(source.url); try? FileManager.default.removeItem(at: source.url)
    }

    static func layout(sourceWidth: Int, sourceHeight: Int, canvasWidth: Int, canvasHeight: Int) -> (width: Int, height: Int, x: Int, y: Int) {
        let width: Int, height: Int
        if Int64(sourceWidth) * Int64(canvasHeight) >= Int64(sourceHeight) * Int64(canvasWidth) {
            width = canvasWidth
            height = max(1, Int(Int64(sourceHeight) * Int64(canvasWidth) / Int64(sourceWidth)))
        } else {
            height = canvasHeight
            width = max(1, Int(Int64(sourceWidth) * Int64(canvasHeight) / Int64(sourceHeight)))
        }
        return (width, height, (canvasWidth - width) / 2, (canvasHeight - height) / 2)
    }

    func render(_ source: DemoImageSource?, configuration: LabelConfiguration, cancelled: () -> Bool) throws -> Data {
        lock.lock(); defer { lock.unlock() }
        cleanupOnce()
        guard let source else { return try LabelRaster.render(configuration, cancelled: cancelled) }
        if cancelled() { throw PrinterSDKError(1005) }
        let rectangle = Self.layout(sourceWidth: source.width, sourceHeight: source.height,
            canvasWidth: configuration.width, canvasHeight: configuration.height)
        guard let decoder = CGImageSourceCreateWithURL(source.url as CFURL, [kCGImageSourceShouldCache: false] as CFDictionary),
              let type = CGImageSourceGetType(decoder) else {
            throw DemoValidationError(message: "图片解码失败")
        }
        let image: CGImage, bitmapWidth: Int, bitmapHeight: Int, orientation: Int
        let pngStripHeight: Int?
        if type as String == "public.png" {
            // ImageIO's PNG thumbnail applies a filter, even to 1-pixel edges.
            guard let properties = CGImageSourceCopyPropertiesAtIndex(decoder, 0, nil) as? [CFString: Any],
                  let width = properties[kCGImagePropertyPixelWidth] as? Int,
                  let height = properties[kCGImagePropertyPixelHeight] as? Int,
                  width > 0, height > 0, width <= Self.maximumSourceEdge, height <= Self.maximumSourceEdge,
                  width <= Self.maximumSourcePixels / height else {
                throw DemoValidationError(message: "PNG 图片解码失败或原图尺寸超限")
            }
            orientation = properties[kCGImagePropertyOrientation] as? Int ?? 1
            guard (1...8).contains(orientation),
                  (orientation >= 5 ? height : width) == source.width,
                  (orientation >= 5 ? width : height) == source.height else {
                throw DemoValidationError(message: "PNG 图片方向或尺寸无效")
            }
            let rawWidth = orientation >= 5 ? rectangle.height : rectangle.width
            let rawHeight = orientation >= 5 ? rectangle.width : rectangle.height
            let scaleX = width / rawWidth, scaleY = height / rawHeight
            // Binary integer scales keep strip boundaries and sampling phase exact.
            // Other scales retain the original whole-image path.
            let aligned = scaleX > 0 && scaleY > 0 && width % rawWidth == 0 && height % rawHeight == 0 &&
                scaleX & (scaleX - 1) == 0 && scaleY & (scaleY - 1) == 0
            let rows = max(scaleY, 32 / max(1, scaleY) * scaleY)
            pngStripHeight = aligned && width <= 1_048_576 / rows ? rows : nil
            // Cache only for strips: otherwise each draw would decode the PNG again.
            // One source-sized native decode remains; this is not an RSS bound.
            guard let original = CGImageSourceCreateImageAtIndex(decoder, 0,
                    [kCGImageSourceShouldCache: pngStripHeight != nil] as CFDictionary),
                  original.width == width, original.height == height else {
                throw DemoValidationError(message: "PNG 图片解码失败或尺寸无效")
            }
            image = original; bitmapWidth = rectangle.width; bitmapHeight = rectangle.height
        } else {
            guard type as String == "public.jpeg",
                  let thumbnail = CGImageSourceCreateThumbnailAtIndex(decoder, 0, [
                    kCGImageSourceCreateThumbnailFromImageAlways: true,
                    kCGImageSourceCreateThumbnailWithTransform: true,
                    kCGImageSourceThumbnailMaxPixelSize: max(rectangle.width, rectangle.height),
                    kCGImageSourceShouldCacheImmediately: true
                  ] as CFDictionary), thumbnail.width > 0, thumbnail.height > 0,
                  thumbnail.width <= 8_000_000 / thumbnail.height else {
                throw DemoValidationError(message: "JPEG 图片解码失败或下采样结果超限")
            }
            image = thumbnail; bitmapWidth = thumbnail.width; bitmapHeight = thumbnail.height; orientation = 1
            pngStripHeight = nil
        }
        guard CGImageSourceGetStatusAtIndex(decoder, 0) == .statusComplete else {
            throw DemoValidationError(message: "图片数据不完整")
        }
        if cancelled() { throw PrinterSDKError(1005) }
        var rgba = Data(count: bitmapWidth * bitmapHeight * 4)
        let rendered = try rgba.withUnsafeMutableBytes { (buffer: UnsafeMutableRawBufferPointer) -> Bool in
            guard let context = CGContext(data: buffer.baseAddress, width: bitmapWidth, height: bitmapHeight,
                bitsPerComponent: 8, bytesPerRow: bitmapWidth * 4, space: CGColorSpace(name: CGColorSpace.sRGB)!,
                bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue | CGBitmapInfo.byteOrder32Big.rawValue) else { return false }
            context.interpolationQuality = .none
            context.setBlendMode(.copy)
            let orientedWidth = orientation >= 5 ? image.height : image.width
            let orientedHeight = orientation >= 5 ? image.width : image.height
            context.scaleBy(x: CGFloat(bitmapWidth) / CGFloat(orientedWidth), y: CGFloat(bitmapHeight) / CGFloat(orientedHeight))
            context.concatenate(Self.orientationTransform(orientation, width: CGFloat(image.width), height: CGFloat(image.height)))
            if let pngStripHeight {
                context.setShouldAntialias(false)
                // Keep the original transform and nearest-neighbor sampling phase.
                // Small source strips avoid a second full-source conversion buffer.
                for top in stride(from: 0, to: image.height, by: pngStripHeight) {
                    if cancelled() { throw PrinterSDKError(1005) }
                    let rows = min(pngStripHeight, image.height - top)
                    guard let strip = image.cropping(to: CGRect(x: 0, y: top, width: image.width, height: rows)) else {
                        throw DemoValidationError(message: "PNG 图片转换失败")
                    }
                    context.draw(strip, in: CGRect(x: 0, y: image.height - top - rows, width: image.width, height: rows))
                }
            } else {
                context.draw(image, in: CGRect(x: 0, y: 0, width: image.width, height: image.height))
            }
            return true
        }
        guard rendered else { throw DemoValidationError(message: "无法分配图片转换缓冲区") }
        var gray = Data(repeating: 255, count: configuration.width * configuration.height)
        try rgba.withUnsafeBytes { (input: UnsafeRawBufferPointer) in
            try gray.withUnsafeMutableBytes { (output: UnsafeMutableRawBufferPointer) in
                for y in 0..<rectangle.height {
                    if cancelled() { throw PrinterSDKError(1005) }
                    let sourceY = y * bitmapHeight / rectangle.height
                    for x in 0..<rectangle.width {
                        let sourceX = x * bitmapWidth / rectangle.width
                        let offset = (sourceY * bitmapWidth + sourceX) * 4
                        let alpha = Int(input[offset + 3])
                        // The core composites each RGB channel before its integer luma.
                        // A premultiplied byte already contains round(channel * alpha / 255),
                        // so adding the white contribution avoids lossy unpremultiplication.
                        let white = 255 - alpha
                        let red = min(255, Int(input[offset]) + white)
                        let green = min(255, Int(input[offset + 1]) + white)
                        let blue = min(255, Int(input[offset + 2]) + white)
                        let luminance = (299 * red + 587 * green + 114 * blue + 500) / 1000
                        output[(y + rectangle.y) * configuration.width + x + rectangle.x] = UInt8(luminance)
                    }
                }
            }
        }
        return gray
    }

    private static func orientationTransform(_ orientation: Int, width: CGFloat, height: CGFloat) -> CGAffineTransform {
        switch orientation {
        case 2: return CGAffineTransform(a: -1, b: 0, c: 0, d: 1, tx: width, ty: 0)
        case 3: return CGAffineTransform(a: -1, b: 0, c: 0, d: -1, tx: width, ty: height)
        case 4: return CGAffineTransform(a: 1, b: 0, c: 0, d: -1, tx: 0, ty: height)
        // EXIF uses top-left coordinates; CoreGraphics' image coordinates use bottom-left.
        case 5: return CGAffineTransform(a: 0, b: -1, c: -1, d: 0, tx: height, ty: width)
        case 6: return CGAffineTransform(a: 0, b: -1, c: 1, d: 0, tx: 0, ty: width)
        case 7: return CGAffineTransform(a: 0, b: 1, c: 1, d: 0, tx: 0, ty: 0)
        case 8: return CGAffineTransform(a: 0, b: 1, c: -1, d: 0, tx: height, ty: 0)
        default: return .identity
        }
    }
}
