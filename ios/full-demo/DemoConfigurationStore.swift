import Foundation

/// Only non-identifying, explicitly saved parameters belong in this schema.
struct DemoSavedConfiguration: Codable, Sendable {
    let schemaVersion: Int
    let dpi, deviceWidth, width, height, copies, xOffsetDots: Int
    let formChoice: Int?
    let documentKind: Int
    let trailingBlankDots: Int
    let originStrategy: Int?
    let originAdjustmentDots: Int?
    let service: String?
    let characteristic: String?
    let payload: Int
    let m90Flow: Bool
    let writeMode: Int?
    let media: Int

    init(configuration: LabelConfiguration, service: String?, characteristic: String?, payload: Int,
         m90Flow: Bool, writeMode: Int?, media: Int, schemaVersion: Int = 2) {
        self.schemaVersion = schemaVersion
        dpi = configuration.dpi; deviceWidth = configuration.deviceWidth
        width = configuration.width; height = configuration.height; copies = configuration.copies
        xOffsetDots = configuration.xOffsetDots; trailingBlankDots = configuration.trailingBlankDots
        originStrategy = schemaVersion >= 2 ? configuration.originStrategy.rawValue : nil
        originAdjustmentDots = schemaVersion >= 2 ? configuration.originAdjustmentDots : nil
        documentKind = configuration.kind.rawValue
        formChoice = configuration.kind == .label ? (configuration.form ? 1 : 2) : nil
        self.service = service; self.characteristic = characteristic; self.payload = payload
        self.m90Flow = m90Flow; self.writeMode = writeMode; self.media = media
    }

    func validated() throws -> LabelConfiguration {
        guard schemaVersion == 1 || schemaVersion == 2 else { throw DemoValidationError(message: "保存配置版本不受支持") }
        let strategy: DemoOriginStrategy
        let adjustment: Int
        if schemaVersion == 1 {
            strategy = .manual; adjustment = 0
        } else {
            guard let rawStrategy = originStrategy, let decodedStrategy = DemoOriginStrategy(rawValue: rawStrategy),
                  let decodedAdjustment = originAdjustmentDots else {
                throw DemoValidationError(message: "保存配置缺少有效的横向定位策略或校准偏移")
            }
            strategy = decodedStrategy; adjustment = decodedAdjustment
        }
        guard let kind = DemoDocumentKind(rawValue: documentKind), (1...3).contains(media),
              (1...512).contains(payload) else { throw DemoValidationError(message: "保存配置中的模式或传输参数无效") }
        if schemaVersion == 2 && kind == .receipt {
            guard strategy == .manual, adjustment == 0 else {
                throw DemoValidationError(message: "保存的小票配置横向策略须为手动，校准偏移须为 0")
            }
        }
        if !m90Flow {
            guard let service, UUID(uuidString: service) != nil,
                  let characteristic, UUID(uuidString: characteristic) != nil,
                  writeMode == 1 || writeMode == 2 else {
                throw DemoValidationError(message: "保存配置缺少明确的 BLE 服务、写特征或写入模式")
            }
        }
        let configuration = try LabelConfiguration(dpi: dpi, deviceWidth: deviceWidth, width: width, height: height,
            copies: copies, formChoice: formChoice ?? 0, xOffsetDots: xOffsetDots,
            kind: kind, trailingBlankDots: trailingBlankDots,
            originStrategy: strategy, originAdjustmentDots: adjustment)
        guard schemaVersion != 2 || configuration.xOffsetDots == xOffsetDots else {
            throw DemoValidationError(message: "保存配置的横向起点与定位策略计算结果不一致")
        }
        return configuration
    }
}

/// No automatic load. The caller serializes these explicit operations off the UI thread.
final class DemoConfigurationStore: Sendable {
    private let root: URL
    private var file: URL { root.appendingPathComponent("saved-configuration-v1.json") }

    init(root: URL? = nil) { self.root = root ?? DemoPrivateFiles.supportDirectory().appendingPathComponent("configuration", isDirectory: true) }

    func save(_ value: DemoSavedConfiguration) throws {
        _ = try value.validated()
        do {
            try DemoPrivateFiles.createDirectory(root)
            let encoder = JSONEncoder(); encoder.outputFormatting = [.sortedKeys]
            try encoder.encode(value).write(to: file, options: .atomic)
        } catch { throw DemoValidationError(message: "保存配置失败；请检查本机存储空间") }
    }

    func load() throws -> DemoSavedConfiguration {
        do {
            let handle = try FileHandle(forReadingFrom: file); defer { try? handle.close() }
            let data = try handle.read(upToCount: 16 * 1024 + 1) ?? Data()
            guard data.count <= 16 * 1024 else { throw DemoValidationError(message: "保存配置文件超限") }
            let value = try JSONDecoder().decode(DemoSavedConfiguration.self, from: data)
            _ = try value.validated()
            return value
        } catch let error as DemoValidationError { throw error }
        catch { throw DemoValidationError(message: "没有可加载的有效保存配置") }
    }

    func clear() throws {
        do {
            if FileManager.default.fileExists(atPath: file.path) { try FileManager.default.removeItem(at: file) }
        } catch { throw DemoValidationError(message: "清除保存配置失败") }
    }
}

enum DemoPrivateFiles {
    static func supportDirectory() -> URL {
        let base = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first
            ?? FileManager.default.temporaryDirectory
        return base.appendingPathComponent("NETUMPrinterDemo", isDirectory: true)
    }

    static func createDirectory(_ url: URL) throws {
        try FileManager.default.createDirectory(at: url, withIntermediateDirectories: true)
        var location = url
        var values = URLResourceValues(); values.isExcludedFromBackup = true
        try location.setResourceValues(values)
    }
}
