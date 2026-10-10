import Foundation

/// Append is nonblocking; all retained-file operations are ordered on this private queue.
final class DemoLogJournal {
    static let maximumFileBytes = 1024 * 1024
    static let maximumRecordBytes = 4 * 1024
    static let retentionSeconds: TimeInterval = 7 * 24 * 60 * 60
    let runID = UUID().uuidString
    private let queue = DispatchQueue(label: "PrinterDemo.journal")
    private let stateLock = NSLock()
    private let root: URL
    private let warning: (String) -> Void
    private let clock: () -> Date
    private let started = ProcessInfo.processInfo.systemUptime
    private var sequence: UInt64 = 0
    private var memory: [String] = []
    private var memoryBytes = 0
    private var failedRecords: Set<String> = []
    private var initialized = false
    private var warned = false
    private var lastPrune: Date?
    private var current: URL { root.appendingPathComponent("journal-0.log") }
    private var previous: URL { root.appendingPathComponent("journal-1.log") }

    init(root: URL? = nil, warning: @escaping (String) -> Void = { _ in }, clock: @escaping () -> Date = Date.init) {
        self.root = root ?? DemoPrivateFiles.supportDirectory().appendingPathComponent("logs", isDirectory: true)
        self.warning = warning; self.clock = clock
        queue.async { self.initialize() }
    }

    @discardableResult
    func append(_ message: String, connection: Int) -> String {
        stateLock.lock()
        sequence &+= 1
        let formatter = ISO8601DateFormatter(); formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        let prefix = "utc=\(formatter.string(from: clock())) elapsed_ms=\(Int((ProcessInfo.processInfo.systemUptime - started) * 1000)) run=\(runID) conn=\(connection) seq=\(String(format: "%012llu", sequence)) "
        let sanitized = Self.redact(message).replacingOccurrences(of: "\n", with: " ").replacingOccurrences(of: "\r", with: " ")
        let record = Self.bounded(prefix + sanitized, limit: Self.maximumRecordBytes - 1)
        // Queue enqueue stays under the same lock as sequence allocation.
        queue.async { self.retainAndWrite(record) }
        stateLock.unlock()
        return record
    }

    func snapshot(completion: @escaping (Result<String, Error>) -> Void) {
        stateLock.lock()
        queue.async {
            self.pruneIfNeeded(force: true)
            let disk = self.readRecords()
            var records = disk ?? []
            let retained = Set(records)
            records.append(contentsOf: self.memory.filter { !retained.contains($0) && (disk == nil || self.failedRecords.contains($0)) })
            // Wall clocks may move backwards. Disk order establishes run order;
            // record sequence establishes order within each run, including fallback gaps.
            var runOrder: [String: Int] = [:]
            for record in records {
                let run = Self.field("run=", in: record)
                if runOrder[run] == nil { runOrder[run] = runOrder.count }
            }
            records = self.filterAge(records).sorted {
                let firstRun = Self.field("run=", in: $0), secondRun = Self.field("run=", in: $1)
                if firstRun != secondRun { return runOrder[firstRun, default: 0] < runOrder[secondRun, default: 0] }
                return Self.field("seq=", in: $0) < Self.field("seq=", in: $1)
            }
            let note = "Printer Demo 日志窗口：UTC 毫秒 / 运行内经过时间；两个各 1 MiB 文件，最长 7 天，单条最多 4 KiB。轮转或期限清理会截断更早记录；磁盘故障时仅有本次运行的有界内存记录。\n"
            completion(.success(note + records.joined(separator: "\n") + (records.isEmpty ? "" : "\n")))
        }
        stateLock.unlock()
    }

    func clear(completion: @escaping (Result<Void, Error>) -> Void) {
        stateLock.lock()
        queue.async {
            do {
                for file in [self.previous, self.current] where FileManager.default.fileExists(atPath: file.path) {
                    try FileManager.default.removeItem(at: file)
                }
                self.memory.removeAll(); self.memoryBytes = 0; self.failedRecords.removeAll(); self.warned = false
                completion(.success(()))
            } catch {
                self.diskFailure()
                completion(.failure(DemoValidationError(message: "清除本机日志失败")))
            }
        }
        stateLock.unlock()
    }

    private func initialize() {
        guard !initialized else { return }
        initialized = true
        do { try DemoPrivateFiles.createDirectory(root); try prune() }
        catch { diskFailure() }
    }

    private func retainAndWrite(_ record: String) {
        initialize()
        memory.append(record); memoryBytes += record.utf8.count + 1
        while memoryBytes > 2 * Self.maximumFileBytes, let first = memory.first {
            memoryBytes -= first.utf8.count + 1; memory.removeFirst(); failedRecords.remove(first)
        }
        pruneIfNeeded(force: false)
        do {
            try DemoPrivateFiles.createDirectory(root)
            let bytes = Data((record + "\n").utf8)
            let attributes = try? FileManager.default.attributesOfItem(atPath: current.path)
            let size = (attributes?[.size] as? NSNumber)?.intValue ?? 0
            if size + bytes.count > Self.maximumFileBytes {
                if FileManager.default.fileExists(atPath: previous.path) { try FileManager.default.removeItem(at: previous) }
                if FileManager.default.fileExists(atPath: current.path) { try FileManager.default.moveItem(at: current, to: previous) }
            }
            if !FileManager.default.fileExists(atPath: current.path) {
                guard FileManager.default.createFile(atPath: current.path, contents: nil) else { throw DemoValidationError(message: "日志存储不可用") }
            }
            let handle = try FileHandle(forWritingTo: current); defer { try? handle.close() }
            try handle.seekToEnd(); try handle.write(contentsOf: bytes)
            // Each queued write is closed before snapshot is dequeued.
        } catch { failedRecords.insert(record); diskFailure() }
    }

    private func diskFailure() {
        guard !warned else { return }
        warned = true
        warning("本机日志写入失败；打印不受影响，本次运行保留有界内存日志")
    }

    private func pruneIfNeeded(force: Bool) {
        if force || lastPrune == nil || clock().timeIntervalSince(lastPrune!) >= 3600 {
            do { try prune() } catch { diskFailure() }
        }
    }

    private func prune() throws {
        for file in [previous, current] where FileManager.default.fileExists(atPath: file.path) {
            let attributes = try FileManager.default.attributesOfItem(atPath: file.path)
            guard let size = attributes[.size] as? NSNumber, size.intValue <= Self.maximumFileBytes else {
                try FileManager.default.removeItem(at: file)
                continue
            }
            let all = try String(contentsOf: file, encoding: .utf8).split(separator: "\n").map(String.init)
            let live = filterAge(all).filter { $0.utf8.count < Self.maximumRecordBytes }
            if live != all { try Data((live.joined(separator: "\n") + (live.isEmpty ? "" : "\n")).utf8).write(to: file, options: .atomic) }
        }
        memory = filterAge(memory); memoryBytes = memory.reduce(0) { $0 + $1.utf8.count + 1 }
        failedRecords.formIntersection(memory)
        lastPrune = clock()
    }

    private func readRecords() -> [String]? {
        var result: [String] = []
        for file in [previous, current] {
            guard FileManager.default.fileExists(atPath: file.path) else { continue }
            guard let attributes = try? FileManager.default.attributesOfItem(atPath: file.path),
                  let size = attributes[.size] as? NSNumber, size.intValue <= Self.maximumFileBytes,
                  let data = try? Data(contentsOf: file), let value = String(data: data, encoding: .utf8) else { return nil }
            result.append(contentsOf: value.split(separator: "\n").map(String.init))
        }
        return result
    }

    private func filterAge(_ values: [String]) -> [String] {
        let cutoff = clock().addingTimeInterval(-Self.retentionSeconds)
        let formatter = ISO8601DateFormatter(); formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return values.filter { line in
            guard line.hasPrefix("utc="), let first = line.split(separator: " ").first,
                  let date = formatter.date(from: String(first.dropFirst(4))) else { return false }
            return date >= cutoff
        }
    }

    private static func field(_ prefix: String, in value: String) -> String {
        String(value.split(separator: " ").first { $0.hasPrefix(prefix) } ?? "")
    }

    static func redact(_ message: String) -> String {
        var value = message
        for pattern in [
            "(?i)\\b(?:[0-9a-f]{2}:){5}[0-9a-f]{2}\\b",
            "(?i)\\b[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\b",
            "(?i)\\b(?:imageName|imagePath|imageContent|exif|rawPayload|rawBytes|rawInput|deviceId|deviceIdentifier|address|peripheralId|peripheralUUID|mac)\\s*=\\s*[^;\\n]*",
            "(?:file://|/Users/|/private/|/var/|/tmp/)[^\\s;]+"
        ] {
            value = value.replacingOccurrences(of: pattern, with: "[redacted]", options: .regularExpression)
        }
        return value
    }

    private static func bounded(_ value: String, limit: Int) -> String {
        guard value.utf8.count > limit else { return value }
        let suffix = " [record truncated]"
        var bytes = Array(value.utf8.prefix(limit - suffix.utf8.count))
        while String(bytes: bytes, encoding: .utf8) == nil { bytes.removeLast() }
        return String(bytes: bytes, encoding: .utf8)! + suffix
    }
}
