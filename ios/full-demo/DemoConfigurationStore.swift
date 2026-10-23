import Foundation
import XLPrinterSDK

/// A form draft only: never persist device identity, Raw data, image locations, or runtime state.
struct DemoSavedSettings: Codable, Sendable, Equatable {
    var schemaVersion = 2
    var connectionMode = "automatic"
    var labelWidthMm = "50"
    var labelHeightMm = "50"
    var paperType = "gapLabel"

    @discardableResult func validated() throws -> DemoSavedSettings {
        guard schemaVersion == 2 else { throw DemoConfigurationError.unsupportedSchema }
        guard DemoConnectionSettings.Mode(rawValue: connectionMode) != nil,
              ["gapLabel", "blackMark", "continuous"].contains(paperType) else { throw DemoConfigurationError.invalidFields }
        do { _ = try DemoPrintSettings(widthMm: labelWidthMm, heightMm: labelHeightMm) }
        catch { throw DemoConfigurationError.invalidFields }
        return self
    }
}

/// Old form files are read without rewriting them. Only semantically identical defaults migrate.
private struct DemoLegacySavedSettings: Decodable {
    let schemaVersion: Int
    let connectionMode, service, writeCharacteristic: String
    let withResponse: Bool
    let maxWriteBytes: String
    let printProtocol: UInt32
    let dpi, widthDots, dotsPerMm, labelAlignment, labelWidthMm, labelHeightMm: String
    let copies, calibrationDots, originXDots, originYDots, trailingBlankDots: String
    let form: Bool
    let algorithm: UInt32
    let threshold: String
    let invert: Bool
    let media: UInt32

    func migrated() throws -> DemoSavedSettings {
        guard schemaVersion == 1 else { throw DemoConfigurationError.unsupportedSchema }
        guard ["automatic", "m90"].contains(connectionMode),
              UUID(uuidString: service) == UUID(uuidString: "0000FF00-0000-1000-8000-00805F9B34FB"),
              UUID(uuidString: writeCharacteristic) == UUID(uuidString: "0000FF02-0000-1000-8000-00805F9B34FB"),
              !withResponse, Int(maxWriteBytes) == 512, printProtocol == 1,
              UInt32(dpi) == 203, UInt32(widthDots) == 864, Double(dotsPerMm) == 8,
              labelAlignment == "centered", UInt32(copies) == 1,
              Int32(calibrationDots) == 0, Int32(originXDots) == 0, Int32(originYDots) == 0,
              UInt32(trailingBlankDots) == 0, !form, algorithm == 0, UInt32(threshold) == 128, !invert,
              (1...3).contains(media) else { throw DemoConfigurationError.legacyParameters }
        var result = DemoSavedSettings()
        result.connectionMode = connectionMode; result.labelWidthMm = labelWidthMm; result.labelHeightMm = labelHeightMm
        result.paperType = media == 1 ? "gapLabel" : media == 2 ? "blackMark" : "continuous"
        return try result.validated()
    }
}

extension DemoSavedSettings {
    static func decode(_ data: Data) throws -> DemoSavedSettings {
        let object = try JSONSerialization.jsonObject(with: data) as? [String: Any]
        guard let version = object?["schemaVersion"] as? Int else { throw DemoConfigurationError.invalidFormat }
        switch version {
        case 1: return try JSONDecoder().decode(DemoLegacySavedSettings.self, from: data).migrated()
        case 2: return try JSONDecoder().decode(DemoSavedSettings.self, from: data).validated()
        default: throw DemoConfigurationError.unsupportedSchema
        }
    }
}

enum DemoConfigurationError: Error, LocalizedError, Equatable, Sendable {
    case missing, invalidFormat, unsupportedSchema, invalidFields, legacyParameters, oversized, storage

    var message: String {
        switch self {
        case .missing: return "没有已保存的参数；当前表单保持不变"
        case .invalidFormat: return "保存参数文件损坏；当前表单保持不变"
        case .unsupportedSchema: return "保存参数版本不受支持；当前表单保持不变"
        case .legacyParameters: return "旧草稿含已移除的参数，无法保持打印语义；原文件和当前表单保持不变"
        case .invalidFields: return "参数无效；请检查型号选择、纸张类型与页面尺寸"
        case .oversized: return "保存参数文件超过 64 KiB；当前表单保持不变"
        case .storage: return "参数存储操作失败；请检查本机存储是否可用"
        }
    }
    var errorDescription: String? { message }
}

/// Explicit operations run in submission order off the UI thread; initialization never reads a file.
final class DemoConfigurationStore: Sendable {
    static let maximumBytes = 64 * 1024
    private let root: URL
    private let queue = DispatchQueue(label: "PrinterDemo.settings")
    private var file: URL { root.appendingPathComponent("settings-v1.json") }

    init(root: URL? = nil) {
        let support = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first
            ?? FileManager.default.temporaryDirectory
        self.root = root ?? support.appendingPathComponent("PrinterDemo", isDirectory: true)
    }

    func save(_ value: DemoSavedSettings,
              completion: @escaping @Sendable (Result<DemoSavedSettings, DemoConfigurationError>) -> Void) {
        queue.async {
            do {
                let validated = try value.validated()
                let encoder = JSONEncoder(); encoder.outputFormatting = [.sortedKeys]
                let data = try encoder.encode(validated)
                guard data.count <= Self.maximumBytes else { throw DemoConfigurationError.oversized }
                try self.prepareDirectory()
                var options: Data.WritingOptions = [.atomic]
                #if os(iOS)
                options.insert(.completeFileProtectionUntilFirstUserAuthentication)
                #endif
                try data.write(to: self.file, options: options)
                completion(.success(validated))
            } catch { completion(.failure(Self.error(error))) }
        }
    }

    func load(completion: @escaping @Sendable (Result<DemoSavedSettings, DemoConfigurationError>) -> Void) {
        queue.async {
            do {
                guard FileManager.default.fileExists(atPath: self.file.path) else { throw DemoConfigurationError.missing }
                let input = try FileHandle(forReadingFrom: self.file); defer { try? input.close() }
                let data = try input.read(upToCount: Self.maximumBytes + 1) ?? Data()
                guard data.count <= Self.maximumBytes else { throw DemoConfigurationError.oversized }
                let value: DemoSavedSettings
                do { value = try DemoSavedSettings.decode(data) }
                catch let error as DemoConfigurationError { throw error }
                catch { throw DemoConfigurationError.invalidFormat }
                completion(.success(try value.validated()))
            } catch { completion(.failure(Self.error(error))) }
        }
    }

    func clear(completion: @escaping @Sendable (Result<Void, DemoConfigurationError>) -> Void) {
        queue.async {
            do {
                if FileManager.default.fileExists(atPath: self.file.path) { try FileManager.default.removeItem(at: self.file) }
                completion(.success(()))
            } catch { completion(.failure(Self.error(error))) }
        }
    }

    private func prepareDirectory() throws {
        var attributes: [FileAttributeKey: Any] = [.posixPermissions: 0o700]
        #if os(iOS)
        attributes[.protectionKey] = FileProtectionType.completeUntilFirstUserAuthentication
        #endif
        try FileManager.default.createDirectory(at: root, withIntermediateDirectories: true, attributes: attributes)
        var location = root
        var values = URLResourceValues(); values.isExcludedFromBackup = true
        // Excluding the private directory also excludes its atomically replaced descendants.
        try location.setResourceValues(values)
    }

    private static func error(_ error: Error) -> DemoConfigurationError {
        (error as? DemoConfigurationError) ?? .storage
    }
}
