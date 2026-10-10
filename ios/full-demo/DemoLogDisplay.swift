import Foundation

/// Presentation only. The original record is retained for details, storage and export.
enum DemoLogDisplay {
    static let maximumVisibleRecords = 160
    struct Record: Equatable, Identifiable {
        let id: String
        let raw: String
        let summary: String
    }
    private static let prefix = try! NSRegularExpression(
        pattern: "^utc=(\\S+) elapsed_ms=[0-9]+ run=(\\S+) conn=[0-9]+ seq=([0-9]+) ([\\s\\S]*)$")

    static func records(_ rawRecords: [String], timeZone: TimeZone = .current) -> [Record] {
        let utc = ISO8601DateFormatter()
        utc.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        let local = DateFormatter()
        local.locale = Locale(identifier: "en_US_POSIX")
        local.calendar = Calendar(identifier: .gregorian)
        local.timeZone = timeZone
        local.dateFormat = "yyyy-MM-dd HH:mm:ss.SSS"
        return rawRecords.suffix(maximumVisibleRecords).map { raw in
            guard let fields = parsed(raw), let date = utc.date(from: fields.utc) else {
                return Record(id: recordID(raw), raw: raw, summary: raw)
            }
            return Record(id: recordID(raw), raw: raw, summary: local.string(from: date) + " " + fields.event)
        }
    }

    static func recordID(_ raw: String) -> String {
        guard let fields = parsed(raw), let sequence = UInt64(fields.sequence) else { return "raw:" + raw }
        return "run:" + fields.run + "|seq:" + String(sequence)
    }

    static func timeZoneDescription(_ timeZone: TimeZone = .current, at date: Date = Date()) -> String {
        let minutes = timeZone.secondsFromGMT(for: date) / 60
        let offset = String(format: "UTC%@%02d:%02d", minutes < 0 ? "−" : "+", abs(minutes) / 60, abs(minutes) % 60)
        return "当前时区：\(timeZone.identifier)（\(offset)）；诊断详情与导出保留 UTC。"
    }

    private static func parsed(_ raw: String) -> (utc: String, run: String, sequence: String, event: String)? {
        guard let match = prefix.firstMatch(in: raw, range: NSRange(raw.startIndex..., in: raw)) else { return nil }
        let text = raw as NSString
        return (text.substring(with: match.range(at: 1)), text.substring(with: match.range(at: 2)),
                text.substring(with: match.range(at: 3)), text.substring(with: match.range(at: 4)))
    }
}

/// Plain geometry keeps the scroll policy independently testable without UIKit.
enum DemoLogScroll {
    struct Frame {
        let recordID: String
        let top: Double
        let height: Double
    }
    struct Anchor: Equatable {
        let recordID: String
        let offset: Double
    }
    static func isAtBottom(offset: Double, contentHeight: Double, viewportHeight: Double,
                           tolerance: Double = 24) -> Bool {
        offset >= max(0, contentHeight - viewportHeight) - tolerance
    }
    static func anchor(offset: Double, frames: [Frame]) -> Anchor? {
        guard let frame = frames.first(where: { $0.top + $0.height > offset }) ?? frames.last else { return nil }
        return Anchor(recordID: frame.recordID, offset: offset - frame.top)
    }
    static func restoredOffset(_ anchor: Anchor?, frames: [Frame], maximumOffset: Double) -> Double {
        guard let anchor, let frame = frames.first(where: { $0.recordID == anchor.recordID }) else { return 0 }
        // A shorter summary may no longer contain the old line. Keep the record visible.
        let relative = max(-frame.top, min(anchor.offset, max(0, frame.height - 1)))
        return max(0, min(frame.top + relative, max(0, maximumOffset)))
    }
}

/// The page owns this value across UIKit view destruction and recreation.
final class DemoLogViewport {
    var anchor: DemoLogScroll.Anchor?
}
