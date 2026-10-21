import Foundation
import CoreGraphics
import ImageIO
import PrinterSDK

/// App image work has its own cancellation lifetime, independent of a printer request.
final class DemoImageCancellation: @unchecked Sendable {
    private let lock = NSLock()
    private var cancelled = false
    var isCancelled: Bool { lock.lock(); defer { lock.unlock() }; return cancelled }
    func cancel() { lock.lock(); cancelled = true; lock.unlock() }
    func checkCancellation() throws { if isCancelled { throw DemoInputError(1005) } }
}

/// CGImage is immutable; this wrapper shares only the finished, privately owned bitmap.
struct DemoImagePreview: @unchecked Sendable {
    let image: CGImage
    var widthPx: Int { image.width }
    var heightPx: Int { image.height }
}

/// An accepted import owns just its private copy. Replacing the selection does not
/// remove a file still retained by an image worker or a submitted print snapshot.
final class DemoImportedImage: Sendable {
    let originalFilename: String
    let encodedByteCount: Int
    let widthPx: Int
    let heightPx: Int
    let cacheURL: URL
    let encodedData: Data
    private let cache: DemoImageCache

    fileprivate init(originalFilename: String, encodedByteCount: Int, widthPx: Int, heightPx: Int,
                     cacheURL: URL, encodedData: Data, cache: DemoImageCache) {
        self.originalFilename = originalFilename; self.encodedByteCount = encodedByteCount
        self.widthPx = widthPx; self.heightPx = heightPx; self.cacheURL = cacheURL
        self.encodedData = encodedData; self.cache = cache
    }

    deinit { cache.release(bytes: encodedByteCount, file: cacheURL) }

    /// Call on the image worker. The original provider URL is never reopened.
    func readEncodedData(cancellation: DemoImageCancellation = DemoImageCancellation()) throws -> Data {
        try Self.read(cacheURL, expectedBytes: encodedByteCount, cancellation: cancellation)
    }

    /// A source preview keeps the entire oriented image and composites transparency
    /// onto white. ImageIO downsamples before the app creates its RGBA bitmap.
    /// This bounds app buffers, not ImageIO's private allocations or process RSS.
    func preview(maxPixelSize: Int = 1024,
                 cancellation: DemoImageCancellation = DemoImageCancellation()) throws -> DemoImagePreview {
        try cancellation.checkCancellation()
        guard (1...DemoImageStore.maximumSourceEdge).contains(maxPixelSize) else { throw DemoInputError(1001) }
        let sourceEdge = max(widthPx, heightPx), requestedEdge = min(maxPixelSize, sourceEdge)
        // Round up both estimated edges before decoding: unusual aspect ratios and
        // ImageIO rounding must not admit an oversized app-owned thumbnail bitmap.
        let estimatedWidth = (widthPx * requestedEdge + sourceEdge - 1) / sourceEdge
        let estimatedHeight = (heightPx * requestedEdge + sourceEdge - 1) / sourceEdge
        guard estimatedWidth <= 8_000_000 / estimatedHeight else { throw DemoInputError(1008) }
        guard let source = CGImageSourceCreateWithURL(cacheURL as CFURL,
                [kCGImageSourceShouldCache: false] as CFDictionary),
              let thumbnail = CGImageSourceCreateThumbnailAtIndex(source, 0, [
                kCGImageSourceCreateThumbnailFromImageAlways: true,
                kCGImageSourceCreateThumbnailWithTransform: true,
                kCGImageSourceThumbnailMaxPixelSize: maxPixelSize,
                kCGImageSourceShouldCacheImmediately: true
              ] as CFDictionary),
              thumbnail.width > 0, thumbnail.height > 0,
              thumbnail.width <= maxPixelSize, thumbnail.height <= maxPixelSize,
              thumbnail.width <= 8_000_000 / thumbnail.height,
              CGImageSourceGetStatusAtIndex(source, 0) == .statusComplete else {
            throw DemoInputError(1001)
        }
        try cancellation.checkCancellation()
        guard let bitmap = CGContext(data: nil, width: thumbnail.width, height: thumbnail.height,
                bitsPerComponent: 8, bytesPerRow: thumbnail.width * 4,
                space: CGColorSpaceCreateDeviceRGB(),
                bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue | CGBitmapInfo.byteOrder32Big.rawValue) else {
            throw DemoInputError(1008)
        }
        let rectangle = CGRect(x: 0, y: 0, width: thumbnail.width, height: thumbnail.height)
        bitmap.setFillColor(red: 1, green: 1, blue: 1, alpha: 1)
        bitmap.fill(rectangle)
        bitmap.draw(thumbnail, in: rectangle)
        try cancellation.checkCancellation()
        guard let image = bitmap.makeImage() else { throw DemoInputError(1008) }
        return DemoImagePreview(image: image)
    }

    fileprivate static func read(_ url: URL, expectedBytes: Int,
                                 cancellation: DemoImageCancellation) throws -> Data {
        try cancellation.checkCancellation()
        do {
            let input = try FileHandle(forReadingFrom: url)
            defer { try? input.close() }
            var data = Data(capacity: expectedBytes)
            while true {
                try cancellation.checkCancellation()
                guard let chunk = try input.read(upToCount: 64 * 1024), !chunk.isEmpty else { break }
                guard chunk.count <= expectedBytes - data.count else { throw DemoInputError(1001) }
                data.append(chunk)
            }
            try cancellation.checkCancellation()
            guard data.count == expectedBytes else { throw DemoInputError(1001) }
            return data
        } catch let error as DemoInputError { throw error }
        catch { throw DemoInputError(1001) } // Do not expose provider paths or native file errors.
    }
}

/// A store has a private directory and a 128 MiB budget shared with its retained
/// imports. A new store never removes another store's active or abandoned files.
final class DemoImageStore: Sendable {
    static let maximumFileBytes = 32 * 1024 * 1024
    static let maximumCacheBytes = 128 * 1024 * 1024
    static let maximumSourcePixels = 64_000_000
    static let maximumSourceEdge = 32768
    private let cache: DemoImageCache

    init(cacheDirectory: URL? = nil, maximumCacheBytes: Int = DemoImageStore.maximumCacheBytes) {
        let root = cacheDirectory ?? FileManager.default.temporaryDirectory
            .appendingPathComponent("full-demo-images", isDirectory: true)
        cache = DemoImageCache(directory: root.appendingPathComponent(UUID().uuidString, isDirectory: true),
            maximumBytes: max(0, min(Self.maximumCacheBytes, maximumCacheBytes)))
    }

    /// Synchronous by design: schedule on the image worker. For a photo provider's
    /// temporary URL, finish this copy inside its loadFileRepresentation callback.
    /// Security-scoped access is held only while the app copies the selected file.
    func createImport(from url: URL,
                      cancellation: DemoImageCancellation = DemoImageCancellation()) throws -> DemoImportedImage {
        try cancellation.checkCancellation()
        guard url.isFileURL else { throw DemoInputError(1001) }
        return try cache.acceptImport { available in
            try cancellation.checkCancellation()
            let destination = cache.directory.appendingPathComponent(UUID().uuidString + ".image")
            do {
                try FileManager.default.createDirectory(at: cache.directory, withIntermediateDirectories: true,
                    attributes: [.posixPermissions: 0o700])
                try Data().write(to: destination, options: .withoutOverwriting)
                let bytes = try Self.copySelection(url, to: destination, maximumBytes: min(Self.maximumFileBytes, available),
                    cancellation: cancellation)
                let dimensions = try Self.dimensions(destination)
                try cancellation.checkCancellation()
                let data = try DemoImportedImage.read(destination, expectedBytes: bytes, cancellation: cancellation)
                try cancellation.checkCancellation()
                return DemoImportedImage(originalFilename: url.lastPathComponent, encodedByteCount: bytes,
                    widthPx: dimensions.0, heightPx: dimensions.1, cacheURL: destination,
                    encodedData: data, cache: cache)
            } catch {
                try? FileManager.default.removeItem(at: destination)
                if let error = error as? DemoInputError { throw error }
                throw DemoInputError(1001)
            }
        }
    }

    private static func copySelection(_ source: URL, to destination: URL, maximumBytes: Int,
                                      cancellation: DemoImageCancellation) throws -> Int {
        let access = source.startAccessingSecurityScopedResource()
        defer { if access { source.stopAccessingSecurityScopedResource() } }
        let values = try source.resourceValues(forKeys: [.isRegularFileKey, .fileSizeKey])
        guard values.isRegularFile == true else { throw DemoInputError(1001) }
        if let size = values.fileSize, size > maximumBytes { throw DemoInputError(1008) }
        return try copy(source, to: destination, maximumBytes: maximumBytes, cancellation: cancellation)
    }

    private static func copy(_ source: URL, to destination: URL, maximumBytes: Int,
                             cancellation: DemoImageCancellation) throws -> Int {
        let input = try FileHandle(forReadingFrom: source)
        defer { try? input.close() }
        let output = try FileHandle(forWritingTo: destination)
        defer { try? output.close() }
        var bytes = 0
        while true {
            try cancellation.checkCancellation()
            guard let chunk = try input.read(upToCount: 64 * 1024), !chunk.isEmpty else { break }
            guard chunk.count <= maximumBytes - bytes else { throw DemoInputError(1008) }
            try output.write(contentsOf: chunk)
            bytes += chunk.count
        }
        guard bytes > 0 else { throw DemoInputError(1001) }
        try cancellation.checkCancellation()
        try output.close()
        return bytes
    }

    private static func dimensions(_ url: URL) throws -> (Int, Int) {
        guard let source = CGImageSourceCreateWithURL(url as CFURL, [kCGImageSourceShouldCache: false] as CFDictionary),
              let type = CGImageSourceGetType(source), ["public.png", "public.jpeg"].contains(type as String),
              CGImageSourceGetCount(source) == 1,
              let properties = CGImageSourceCopyPropertiesAtIndex(source, 0, nil) as? [CFString: Any],
              CGImageSourceGetStatusAtIndex(source, 0) == .statusComplete,
              let width = properties[kCGImagePropertyPixelWidth] as? Int,
              let height = properties[kCGImagePropertyPixelHeight] as? Int,
              width > 0, height > 0 else { throw DemoInputError(1001) }
        guard width <= maximumSourceEdge, height <= maximumSourceEdge,
              width <= maximumSourcePixels / height else { throw DemoInputError(1008) }
        var orientation = properties[kCGImagePropertyOrientation] as? Int ?? 1
        if orientation == 0 { orientation = 1 }
        guard (1...8).contains(orientation) else { throw DemoInputError(1001) }
        return orientation >= 5 ? (height, width) : (width, height)
    }
}

/// The lock covers admission and the entire copy, so concurrent imports cannot
/// exceed the quota. Imported snapshots keep this ledger and directory alive.
fileprivate final class DemoImageCache: @unchecked Sendable {
    let directory: URL
    private let lock = NSLock()
    private let maximumBytes: Int
    private var bytes = 0
    init(directory: URL, maximumBytes: Int) { self.directory = directory; self.maximumBytes = maximumBytes }

    func acceptImport(_ operation: (Int) throws -> DemoImportedImage) throws -> DemoImportedImage {
        lock.lock(); defer { lock.unlock() }
        let available = maximumBytes - bytes
        guard available > 0 else { throw DemoInputError(1008) }
        let imported = try operation(available)
        bytes += imported.encodedByteCount
        return imported
    }

    func release(bytes count: Int, file: URL) {
        lock.lock(); defer { lock.unlock() }
        do {
            if FileManager.default.fileExists(atPath: file.path) { try FileManager.default.removeItem(at: file) }
            bytes -= count
        } catch {
            // A copy that could not be deleted remains charged to this store.
        }
    }

    deinit {
        // Remove only our now-empty directory. Never recursively delete unknown files.
        if let contents = try? FileManager.default.contentsOfDirectory(at: directory, includingPropertiesForKeys: nil),
           contents.isEmpty { try? FileManager.default.removeItem(at: directory) }
    }
}
