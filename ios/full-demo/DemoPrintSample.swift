import Foundation
import CoreGraphics
import ImageIO

/// Demo-owned content. SDK clients pass an image, without constructing SDK image or job wrappers.
enum DemoPrintSample {
    static func testPNG(width: UInt32 = 384, height: UInt32 = 64) throws -> Data {
        guard width > 0, height > 0, UInt64(width) * UInt64(height) <= 8_000_000 else { throw DemoInputError(1008) }
        var pixels = Data(repeating: 255, count: Int(width) * Int(height))
        for y in 0..<Int(height) {
            for x in 0..<Int(width) where (x / 16 + y / 16) % 2 == 0 { pixels[y * Int(width) + x] = 0 }
        }
        guard let provider = CGDataProvider(data: pixels as CFData),
              let image = CGImage(width: Int(width), height: Int(height), bitsPerComponent: 8, bitsPerPixel: 8,
                bytesPerRow: Int(width), space: CGColorSpaceCreateDeviceGray(), bitmapInfo: [], provider: provider,
                decode: nil, shouldInterpolate: false, intent: .defaultIntent) else { throw DemoInputError(1008) }
        let output = NSMutableData()
        guard let destination = CGImageDestinationCreateWithData(output, "public.png" as CFString, 1, nil) else { throw DemoInputError(1008) }
        CGImageDestinationAddImage(destination, image, nil)
        guard CGImageDestinationFinalize(destination) else { throw DemoInputError(1008) }
        return output as Data
    }
    static func rawBytes(_ text: String) throws -> Data {
        guard text.utf8.count <= 12288 else { throw DemoInputError(1008) }
        let tokens = text.utf8.split { $0 == 0x20 || (0x09...0x0D).contains($0) }
        guard !tokens.isEmpty else { throw DemoInputError(1001) }
        guard tokens.count <= 4096 else { throw DemoInputError(1008) }
        return Data(try tokens.map { token in
            guard token.count == 2, token.allSatisfy({ (0x30...0x39).contains($0) || (0x41...0x46).contains($0) || (0x61...0x66).contains($0) }),
                  let byte = UInt8(String(decoding: token, as: UTF8.self), radix: 16) else { throw DemoInputError(1001) }
            return byte
        })
    }
}
