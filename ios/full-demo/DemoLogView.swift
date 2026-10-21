import SwiftUI
import UniformTypeIdentifiers

/// The visible tail is bounded independently of the retained journal files.
struct DemoLogRecordsView: View {
    let records: [String]

    var body: some View {
        ScrollView([.horizontal, .vertical]) {
            Text(records.suffix(DemoLogJournal.maximumVisibleRecords).joined(separator: "\n"))
                .font(.system(.caption, design: .monospaced))
                .textSelection(.enabled)
                .frame(maxWidth: .infinity, alignment: .topLeading)
                .padding(8)
        }
        .frame(height: 220)
        .background(Color.secondary.opacity(0.08))
        .accessibilityLabel("运行日志记录")
        .accessibilityIdentifier("demo.logRecords")
    }
}

struct DemoLogDocument: FileDocument {
    static var readableContentTypes: [UTType] { [.plainText] }
    let text: String

    init(text: String) { self.text = text }

    init(configuration: ReadConfiguration) throws {
        text = String(decoding: configuration.file.regularFileContents ?? Data(), as: UTF8.self)
    }

    func fileWrapper(configuration: WriteConfiguration) throws -> FileWrapper {
        FileWrapper(regularFileWithContents: Data(text.utf8))
    }
}
