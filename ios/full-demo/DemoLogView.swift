import SwiftUI
import UIKit

/// A bounded log-content viewport. Page controls and journal operations stay with the caller.
struct DemoLogRecordsView: UIViewRepresentable {
    let records: [String]
    let showDetails: Bool
    let viewport: DemoLogViewport
    @Binding var followLatest: Bool

    init(records: [String], showDetails: Bool, followLatest: Binding<Bool>, viewport: DemoLogViewport) {
        self.records = records; self.showDetails = showDetails; self._followLatest = followLatest
        self.viewport = viewport
    }
    func makeCoordinator() -> Coordinator { Coordinator(followLatest: $followLatest, viewport: viewport) }
    func makeUIView(context: Context) -> UIView {
        // SwiftUI owns the representable root's accessibility environment. Keep
        // the native scrollable text as a child so that its label and value survive.
        let container = UIView()
        let view = LogTextView()
        view.delegate = context.coordinator
        view.viewportChanged = { [weak coordinator = context.coordinator] view in coordinator?.viewportChanged(view) }
        view.isEditable = false
        view.isSelectable = true
        view.alwaysBounceVertical = true
        view.adjustsFontForContentSizeCategory = true
        view.backgroundColor = .secondarySystemBackground
        view.contentInsetAdjustmentBehavior = .never
        view.textContainerInset = UIEdgeInsets(top: 12, left: 12, bottom: 12, right: 12)
        view.textContainer.lineFragmentPadding = 0
        view.isAccessibilityElement = true
        view.accessibilityLabel = "运行日志记录"
        view.accessibilityIdentifier = "demo.logRecords"
        container.isAccessibilityElement = false
        container.addSubview(view)
        view.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            view.leadingAnchor.constraint(equalTo: container.leadingAnchor),
            view.trailingAnchor.constraint(equalTo: container.trailingAnchor),
            view.topAnchor.constraint(equalTo: container.topAnchor),
            view.bottomAnchor.constraint(equalTo: container.bottomAnchor)
        ])
        container.accessibilityElements = [view]
        return container
    }
    func updateUIView(_ container: UIView, context: Context) {
        guard let view = container.subviews.first as? UITextView else { return }
        // Reading the environment also refreshes attributed text after Dynamic Type changes.
        _ = context.environment.sizeCategory
        context.coordinator.update(view, records: DemoLogDisplay.records(records),
                                   details: showDetails, followLatest: $followLatest)
    }

    private final class LogTextView: UITextView {
        var viewportChanged: ((UITextView) -> Void)?
        private var previousSize = CGSize.zero
        override func layoutSubviews() {
            let changed = bounds.size != previousSize
            previousSize = bounds.size
            super.layoutSubviews()
            if changed { viewportChanged?(self) }
        }
    }

    final class Coordinator: NSObject, UITextViewDelegate {
        private var followLatest: Binding<Bool>
        private var displayed: [DemoLogDisplay.Record] = []
        private var ranges: [NSRange] = []
        private var details = false
        private var fontSize: CGFloat = 0
        private var updating = false
        private let viewport: DemoLogViewport

        init(followLatest: Binding<Bool>, viewport: DemoLogViewport) {
            self.followLatest = followLatest; self.viewport = viewport
        }

        fileprivate func update(_ view: UITextView, records: [DemoLogDisplay.Record], details: Bool,
                                followLatest: Binding<Bool>) {
            self.followLatest = followLatest
            let anchor = DemoLogScroll.anchor(offset: Double(view.contentOffset.y), frames: frames(view)) ?? viewport.anchor
            let font = UIFontMetrics(forTextStyle: .caption1).scaledFont(
                for: UIFont.monospacedSystemFont(ofSize: 12, weight: .regular))
            updating = true
            defer { updating = false }
            if records != displayed || details != self.details || font.pointSize != fontSize {
                displayed = records; self.details = details; fontSize = font.pointSize
                let text = NSMutableAttributedString(string: "")
                let attributes: [NSAttributedString.Key: Any] = [.font: font, .foregroundColor: UIColor.label]
                ranges = []
                for (index, record) in records.enumerated() {
                    if index > 0 { text.append(NSAttributedString(string: "\n\n", attributes: attributes)) }
                    let value = details ? record.raw : record.summary
                    ranges.append(NSRange(location: text.length, length: (value as NSString).length))
                    text.append(NSAttributedString(string: value, attributes: attributes))
                }
                view.attributedText = text
                view.font = font
                view.accessibilityValue = text.string
                view.layoutManager.ensureLayout(for: view.textContainer)
                view.layoutIfNeeded()
            }
            // A recreated bridge may first receive zero bounds. Keep the page's anchor until
            // UIKit has a real viewport rather than deriving a replacement from zero-width text.
            guard view.bounds.width > 0, view.bounds.height > 0 else { return }
            let maximum = max(0, view.contentSize.height - view.bounds.height)
            let offset = followLatest.wrappedValue ? Double(maximum) :
                DemoLogScroll.restoredOffset(anchor, frames: frames(view), maximumOffset: Double(maximum))
            view.setContentOffset(CGPoint(x: 0, y: CGFloat(offset)), animated: false)
            viewport.anchor = DemoLogScroll.anchor(offset: Double(view.contentOffset.y), frames: frames(view))
        }

        fileprivate func viewportChanged(_ view: UITextView) {
            guard !updating, view.bounds.width > 0, view.bounds.height > 0 else { return }
            updating = true
            defer { updating = false }
            let maximum = max(0, view.contentSize.height - view.bounds.height)
            let offset = followLatest.wrappedValue ? Double(maximum) :
                DemoLogScroll.restoredOffset(viewport.anchor, frames: frames(view), maximumOffset: Double(maximum))
            view.setContentOffset(CGPoint(x: 0, y: CGFloat(offset)), animated: false)
            viewport.anchor = DemoLogScroll.anchor(offset: Double(view.contentOffset.y), frames: frames(view))
        }

        func scrollViewDidScroll(_ scrollView: UIScrollView) {
            guard !updating, scrollView.isTracking || scrollView.isDragging || scrollView.isDecelerating else { return }
            let atBottom = DemoLogScroll.isAtBottom(offset: Double(scrollView.contentOffset.y),
                contentHeight: Double(scrollView.contentSize.height), viewportHeight: Double(scrollView.bounds.height))
            if let view = scrollView as? UITextView {
                viewport.anchor = DemoLogScroll.anchor(offset: Double(view.contentOffset.y), frames: frames(view))
            }
            if followLatest.wrappedValue != atBottom { followLatest.wrappedValue = atBottom }
        }

        private func frames(_ view: UITextView) -> [DemoLogScroll.Frame] {
            view.layoutManager.ensureLayout(for: view.textContainer)
            return zip(displayed, ranges).map { record, range in
                let glyphs = view.layoutManager.glyphRange(forCharacterRange: range, actualCharacterRange: nil)
                let bounds = view.layoutManager.boundingRect(forGlyphRange: glyphs, in: view.textContainer)
                return DemoLogScroll.Frame(recordID: record.id,
                    top: Double(bounds.minY + view.textContainerInset.top),
                    height: Double(max(bounds.height, view.font?.lineHeight ?? 1)))
            }
        }
    }
}
