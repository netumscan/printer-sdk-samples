import Foundation
import XLPrinterSDK

enum DemoJournalError: Error, Equatable, Sendable {
    case unavailable
    case closed

    var message: String {
        switch self {
        case .unavailable: return "本机日志存储不可用"
        case .closed: return "日志已关闭"
        }
    }
}

/// Diagnostic metadata only: callers must never pass print content or device identity.
/// The lock orders accepted work; all retained state and file I/O belong to `queue`.
final class DemoLogJournal: @unchecked Sendable {
    static let maximumFileBytes = 1024 * 1024
    static let maximumRecordBytes = 4096
    static let maximumVisibleRecords = 160
    static let retentionSeconds: TimeInterval = 7 * 24 * 60 * 60

    private struct Record: Sendable {
        let text: String
        let date: Date
        var failedToPersist = false
        var byteCount: Int { text.utf8.count + 1 }
    }

    private let queue = DispatchQueue(label: "PrinterDemo.log-journal", qos: .utility)
    private let lock = NSLock()
    private let root: URL
    private let clock: @Sendable () -> Date
    private let runID = UUID().uuidString.replacingOccurrences(of: "-", with: "")
    private var closed = false
    private var sequence: UInt64 = 0
    private var memory: [Record] = []
    private var memoryBytes = 0
    private var nextExpiry: Date?
    private var initialized = false
    private var diskUnavailable = false
    private var current: URL { root.appendingPathComponent("journal-0.log") }
    private var previous: URL { root.appendingPathComponent("journal-1.log") }

    init(root: URL? = nil, clock: @escaping @Sendable () -> Date = { Date() }) {
        let support = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first
            ?? FileManager.default.temporaryDirectory
        self.root = root ?? support.appendingPathComponent("PrinterDemo/logs", isDirectory: true)
        self.clock = clock
    }

    /// Returns the same bounded, sanitized line retained by the journal. Never waits for disk.
    @discardableResult
    func append(_ message: String) -> String { append(message, redacting: true) }

    private func append(_ message: String, redacting: Bool) -> String {
        lock.lock()
        defer { lock.unlock() }
        guard !closed else { return "" }
        sequence &+= 1
        let date = clock()
        let formatter = Self.formatter()
        let prefix = "utc=\(formatter.string(from: date)) run=\(runID) seq=\(sequence) "
        let text = Self.bounded(prefix + (redacting ? Self.redact(message) : message), limit: Self.maximumRecordBytes - 1)
        let record = Record(text: text, date: date)
        queue.async { self.retainAndWrite(record) }
        return text
    }

    /// Explicit release verification only: typed, allowlisted SDK/build metadata, never device or customer content.
    func recordRuntimeVerification(_ proof: DemoOfflineRuntimeProof, resourceURL: URL?) {
        queue.async {
            do {
                guard let resourceURL else { throw DemoJournalError.unavailable }
                let file = try FileHandle(forReadingFrom: resourceURL)
                defer { try? file.close() }
                let data = try file.read(upToCount: 64 * 1024 + 1) ?? Data()
                guard data.count <= 64 * 1024 else { throw DemoJournalError.unavailable }
                let record = try proof.encodedRecord(buildResource: data)
                // All strings have strict identity/nonce validation; generic path redaction would corrupt build IDs.
                _ = self.append("runtime_api_record=" + record, redacting: false)
            } catch { _ = self.append("event=runtime_api_record_rejected") }
        }
    }

    /// The callback runs on the journal queue. Disk failure falls back to bounded session memory.
    func snapshot(completion: @escaping @Sendable (Result<String, DemoJournalError>) -> Void) {
        lock.lock()
        defer { lock.unlock() }
        guard !closed else { queue.async { completion(.failure(.closed)) }; return }
        queue.async {
            let now = self.clock()
            self.expireMemory(at: now)
            var records: [String]
            do {
                try self.initialize(at: now)
                try self.pruneFiles(at: now)
                records = try self.read(self.previous, at: now) + self.read(self.current, at: now)
                let persisted = Set(records)
                records += self.memory.filter { $0.failedToPersist && !persisted.contains($0.text) }.map(\.text)
            } catch {
                self.diskUnavailable = true
                records = self.memory.map(\.text)
            }
            let note = self.diskUnavailable
                ? "日志存储不可用；以下仅含保留文件及本次运行的有界内存记录。\n"
                : "日志窗口：两个各 1 MiB 文件，保留 7 天，单条最多 4096 字节；轮转会移除较早记录。\n"
            records = Self.ordered(records)
            completion(.success(note + records.joined(separator: "\n") + (records.isEmpty ? "" : "\n")))
        }
    }

    /// Clearing session memory succeeds even when file removal fails; failure is explicit to the UI.
    func clear(completion: @escaping @Sendable (Result<Void, DemoJournalError>) -> Void) {
        lock.lock()
        defer { lock.unlock() }
        guard !closed else { queue.async { completion(.failure(.closed)) }; return }
        queue.async {
            self.memory.removeAll()
            self.memoryBytes = 0
            self.nextExpiry = nil
            do {
                for file in [self.previous, self.current] where FileManager.default.fileExists(atPath: file.path) {
                    try FileManager.default.removeItem(at: file)
                }
                self.diskUnavailable = false
                completion(.success(()))
            } catch {
                self.diskUnavailable = true
                completion(.failure(.unavailable))
            }
        }
    }

    /// Already accepted work finishes in order. No open file handle survives an operation.
    func close() {
        lock.lock()
        defer { lock.unlock() }
        guard !closed else { return }
        closed = true
        queue.async { self.memory.removeAll(); self.memoryBytes = 0 }
    }

    private func retainAndWrite(_ record: Record) {
        let now = clock()
        expireMemory(at: now)
        memory.append(record)
        memoryBytes += record.byteCount
        while memoryBytes > 2 * Self.maximumFileBytes, let oldest = memory.first {
            memoryBytes -= oldest.byteCount
            memory.removeFirst()
        }
        do {
            try initialize(at: now)
            if let expiry = nextExpiry, expiry <= now { try pruneFiles(at: now) }
            let bytes = Data((record.text + "\n").utf8)
            try createDirectory()
            let size = try fileSize(current)
            guard size <= Self.maximumFileBytes else { throw DemoJournalError.unavailable }
            if size + bytes.count > Self.maximumFileBytes {
                if FileManager.default.fileExists(atPath: previous.path) { try FileManager.default.removeItem(at: previous) }
                if FileManager.default.fileExists(atPath: current.path) { try FileManager.default.moveItem(at: current, to: previous) }
            }
            if !FileManager.default.fileExists(atPath: current.path) {
                guard FileManager.default.createFile(atPath: current.path, contents: nil,
                    attributes: [.posixPermissions: 0o600]) else { throw DemoJournalError.unavailable }
            }
            let handle = try FileHandle(forWritingTo: current)
            defer { try? handle.close() }
            try handle.seekToEnd()
            try handle.write(contentsOf: bytes)
            let expiry = record.date.addingTimeInterval(Self.retentionSeconds)
            nextExpiry = nextExpiry.map { min($0, expiry) } ?? expiry
        } catch {
            diskUnavailable = true
            if !memory.isEmpty { memory[memory.count - 1].failedToPersist = true }
        }
    }

    private func initialize(at now: Date) throws {
        guard !initialized else { return }
        try createDirectory()
        try pruneFiles(at: now)
        initialized = true
    }

    private func createDirectory() throws {
        try FileManager.default.createDirectory(at: root, withIntermediateDirectories: true,
            attributes: [.posixPermissions: 0o700])
        var directory = root
        var values = URLResourceValues()
        values.isExcludedFromBackup = true
        try directory.setResourceValues(values)
    }

    private func fileSize(_ file: URL) throws -> Int {
        guard FileManager.default.fileExists(atPath: file.path) else { return 0 }
        let values = try file.resourceValues(forKeys: [.isRegularFileKey, .isSymbolicLinkKey, .fileSizeKey])
        guard values.isRegularFile == true, values.isSymbolicLink != true,
              let size = values.fileSize else { throw DemoJournalError.unavailable }
        return size
    }

    private func read(_ file: URL, at now: Date) throws -> [String] {
        guard FileManager.default.fileExists(atPath: file.path) else { return [] }
        guard try fileSize(file) <= Self.maximumFileBytes else { throw DemoJournalError.unavailable }
        let formatter = Self.formatter()
        return try String(contentsOf: file, encoding: .utf8).split(separator: "\n").compactMap { line in
            let value = String(line)
            guard value.utf8.count < Self.maximumRecordBytes,
                  let date = Self.date(value, formatter: formatter),
                  now.timeIntervalSince(date) < Self.retentionSeconds else { return nil }
            return value
        }
    }

    private func pruneFiles(at now: Date) throws {
        nextExpiry = nil
        let formatter = Self.formatter()
        for file in [previous, current] where FileManager.default.fileExists(atPath: file.path) {
            let records = try read(file, at: now)
            let bytes = Data((records.joined(separator: "\n") + (records.isEmpty ? "" : "\n")).utf8)
            // Rewriting is infrequent: initialization, export, or the next record's expiry.
            try bytes.write(to: file, options: .atomic)
            try FileManager.default.setAttributes([.posixPermissions: 0o600], ofItemAtPath: file.path)
            for record in records {
                if let date = Self.date(record, formatter: formatter) {
                    let expiry = date.addingTimeInterval(Self.retentionSeconds)
                    nextExpiry = nextExpiry.map { min($0, expiry) } ?? expiry
                }
            }
        }
    }

    private func expireMemory(at now: Date) {
        memory.removeAll { now.timeIntervalSince($0.date) >= Self.retentionSeconds }
        memoryBytes = memory.reduce(0) { $0 + $1.byteCount }
    }

    private static func formatter() -> ISO8601DateFormatter {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return formatter
    }

    private static func date(_ record: String, formatter: ISO8601DateFormatter) -> Date? {
        guard record.hasPrefix("utc="), let end = record.firstIndex(of: " ") else { return nil }
        return formatter.date(from: String(record[record.index(record.startIndex, offsetBy: 4)..<end]))
    }

    static func redact(_ message: String) -> String {
        // Bound work as well as output. Newlines cannot inject forged journal records.
        let separators = CharacterSet.controlCharacters.union(.newlines)
        var value = String(String.UnicodeScalarView(bounded(message, limit: 16 * 1024).unicodeScalars.map {
            separators.contains($0) ? UnicodeScalar(32)! : $0
        }))
        for pattern in [
            "(?i)\\b(?:raw(?:Payload|Bytes|Input|Command|Text)?|printContent|printPayload|image(?:Name|Path|Content)|device(?:Name|Id|Identifier|Serial|Address)|peripheral(?:Id|UUID)|address|host|mac|serial(?:Number|_number)?|privateKey|certificate)\\s*[:=]\\s*[^;]*",
            "(?i)\\braw[ _-]+(?:payload|bytes|input|command|text)\\b[^;]*",
            "(?i)\\b(?:[0-9a-f]{2}[:-]){5}[0-9a-f]{2}\\b",
            "(?i)\\b[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\b",
            "(?i)\\b[a-z][a-z0-9+.-]*://[^\\s;]+",
            "\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}(?::[0-9]+)?\\b",
            "(?i)(?<![a-z0-9])(?:[0-9a-f]{0,4}:){2,7}[0-9a-f]{0,4}(?:%[a-z0-9._-]+)?(?![a-z0-9])",
            "(?i)\\b(?:[a-z0-9-]+\\.)+[a-z]{2,}(?::[0-9]+)?\\b",
            "(?:[A-Za-z]:[\\\\/]|~[/\\\\]|/)[^\\s;]+"
        ] {
            value = value.replacingOccurrences(of: pattern, with: "[redacted]", options: .regularExpression)
        }
        return value
    }

    private static func ordered(_ records: [String]) -> [String] {
        func field(_ prefix: String, in text: String) -> String {
            text.split(separator: " ").first(where: { $0.hasPrefix(prefix) }).map { String($0.dropFirst(prefix.count)) } ?? ""
        }
        var runs: [String: Int] = [:]
        for record in records {
            let run = field("run=", in: record)
            if runs[run] == nil { runs[run] = runs.count }
        }
        return records.sorted {
            let firstRun = field("run=", in: $0), secondRun = field("run=", in: $1)
            if firstRun != secondRun { return runs[firstRun, default: 0] < runs[secondRun, default: 0] }
            return (UInt64(field("seq=", in: $0)) ?? 0) < (UInt64(field("seq=", in: $1)) ?? 0)
        }
    }

    private static func bounded(_ value: String, limit: Int) -> String {
        guard value.utf8.count > limit else { return value }
        let suffix = " [truncated]"
        var bytes = Array(value.utf8.prefix(limit - suffix.utf8.count))
        while String(bytes: bytes, encoding: .utf8) == nil { bytes.removeLast() }
        return String(decoding: bytes, as: UTF8.self) + suffix
    }
}


/// Data from actual public SDK calls. This is separate from legacy compatibility/ABI evidence.
struct DemoOfflineRuntimeProof: Sendable {
    struct Step: Sendable {
        let name: String
        let code: Int32
        let mainThread, asynchronous, uniqueRequest, completionNone, outputMayHaveOccurred: Bool
    }
    let nonce: UUID
    let sdk: SDKInfo
    let steps: [Step]

    func encodedRecord(buildResource: Data) throws -> String {
        guard let resource = try JSONSerialization.jsonObject(with: buildResource) as? [String: Any],
              resource["schema_version"] as? Int == 1, let identity = resource["identity"] as? [String: Any],
              let distribution = identity["sdk_distribution_version"] as? String,
              let core = identity["sdk_core_version"] as? String,
              let revision = identity["sdk_release_commit"] as? String,
              let artifact = identity["sdk_artifact_sha256"] as? String,
              let fingerprint = identity["demo_source_fingerprint"] as? String,
              let demoBuild = identity["demo_build_version"] as? String,
              distribution == sdk.distributionVersion, core == sdk.nativeVersion,
              Self.matches(distribution, "^[0-9]+\\.[0-9]+\\.[0-9]+(?:-[A-Za-z0-9.-]+)?$"),
              Self.matches(core, "^[0-9]+\\.[0-9]+\\.[0-9]+(?:-[A-Za-z0-9.-]+)?$"),
              Self.matches(revision, "^[0-9a-f]{40}$"), Self.matches(artifact, "^[0-9a-f]{64}$"),
              Self.matches(fingerprint, "^[0-9a-f]{64}$"),
              demoBuild == distribution + "+samples." + fingerprint.prefix(12) else { throw DemoJournalError.unavailable }
        if let build = sdk.buildID { guard Self.matches(build, "^[0-9a-f]{64}$") else { throw DemoJournalError.unavailable } }
        let value: [String: Any] = [
            "schema_version": 1, "scope": "sdk_identity_and_offline_lifecycle", "nonce": nonce.uuidString.lowercased(),
            "sdk": ["native_version": sdk.nativeVersion, "distribution_version": sdk.distributionVersion,
                    "build_id": sdk.buildID as Any? ?? NSNull()],
            "distribution": ["distribution_version": distribution, "core_version": core,
                             "release_commit": revision, "artifact_sha256": artifact],
            "demo": ["source_fingerprint": fingerprint, "build_version": demoBuild],
            "offline_lifecycle": ["operations": steps.map(\.name), "codes": steps.map(\.code),
                "callbacks_on_main_thread": steps.allSatisfy(\.mainThread),
                "callbacks_asynchronous": steps.allSatisfy(\.asynchronous),
                "exactly_once": steps.allSatisfy(\.uniqueRequest),
                "completion_none": steps.allSatisfy(\.completionNone),
                "output_may_have_occurred": steps.contains(where: \.outputMayHaveOccurred)],
            "hardware_verified": false, "transport": "none"
        ]
        let data = try JSONSerialization.data(withJSONObject: value, options: [.sortedKeys, .withoutEscapingSlashes])
        guard let string = String(data: data, encoding: .utf8), string.utf8.count < 3500 else { throw DemoJournalError.unavailable }
        return string
    }
    private static func matches(_ value: String, _ pattern: String) -> Bool { value.range(of: pattern, options: .regularExpression) != nil }
}

@MainActor
enum DemoOfflineRuntimeVerification {
    private enum Reply {
        case identity(ManagerResult<SDKInfo>)
        case lifecycle(ManagerResult<Void>)
    }
    private final class Delivery {
        let name: String
        let finish: (Reply) -> Void
        var returned = false
        var requestID: RequestID?
        init(_ name: String, finish: @escaping (Reply) -> Void) { self.name = name; self.finish = finish }
    }
    private final class Recorder: PrinterDelegate {
        var requests: Set<RequestID> = []
        var steps: [DemoOfflineRuntimeProof.Step] = []
        var pending: [RequestID: Delivery] = [:]
        func record<Value>(_ value: ManagerResult<Value>, reply: Reply) {
            guard let delivery = pending.removeValue(forKey: value.requestID) else { return }
            let unique = requests.insert(value.requestID).inserted && delivery.requestID == value.requestID
            steps.append(.init(name: delivery.name, code: value.code, mainThread: Thread.isMainThread,
                asynchronous: delivery.returned, uniqueRequest: unique,
                completionNone: value.completion == .none, outputMayHaveOccurred: value.outputMayHaveOccurred))
            delivery.finish(reply)
        }
        func printerManager(_ manager: PrinterManager, didFinishReadingSDKInfo result: ManagerResult<SDKInfo>) {
            record(result, reply: .identity(result))
        }
        func printerManager(_ manager: PrinterManager, didFinishDisconnecting result: ManagerResult<Void>) {
            record(result, reply: .lifecycle(result))
        }
        func printerManager(_ manager: PrinterManager, didFinishClosing result: ManagerResult<Void>) {
            record(result, reply: .lifecycle(result))
        }
    }
    private static func register(_ name: String, recorder: Recorder, operation: () -> RequestID,
                                 finish: @escaping (Reply) -> Void) {
        let delivery = Delivery(name, finish: finish)
        let id = operation()
        delivery.requestID = id; recorder.pending[id] = delivery; delivery.returned = true
    }
    private static func call(_ name: String, recorder: Recorder, _ operation: () -> RequestID) async -> Reply {
        await withCheckedContinuation { next in
            register(name, recorder: recorder, operation: operation) { next.resume(returning: $0) }
        }
    }
    static func run(nonce: UUID, journal: DemoLogJournal, resourceURL: URL?) async {
        // A second Manager exists only for this explicit offline acceptance invocation. No connect/print/raw call occurs.
        let manager = PrinterManager(), recorder = Recorder()
        manager.delegate = recorder
        let info = await call("sdkInfo", recorder: recorder) { manager.sdkInfo() }
        _ = await call("disconnect", recorder: recorder) { manager.disconnect() }
        _ = await call("disconnect", recorder: recorder) { manager.disconnect() }
        _ = await call("sdkInfo", recorder: recorder) { manager.sdkInfo() }
        // Register both cleanups before either result: close releases the delegate
        // after every result owed to this close barrier has been delivered.
        await withCheckedContinuation { (next: CheckedContinuation<Void, Never>) in
            var remaining = 2
            for _ in 0..<2 {
                register("close", recorder: recorder, operation: { manager.close() }) { _ in
                    remaining -= 1
                    if remaining == 0 { next.resume() }
                }
            }
        }
        guard case .identity(let result) = info, let sdk = result.value else {
            journal.append("event=runtime_api_identity_missing"); return
        }
        journal.recordRuntimeVerification(.init(nonce: nonce, sdk: sdk, steps: recorder.steps), resourceURL: resourceURL)
    }
}
