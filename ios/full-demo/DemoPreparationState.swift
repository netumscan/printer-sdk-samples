import Foundation

/// Preparation tickets are independent of discovery and printer IO.
// All mutable state is protected by lock, including cross-queue cancellation checks.
final class DemoPreparationState: @unchecked Sendable {
    private let lock = NSLock()
    private var generation: UInt64 = 0
    private var prepared: DemoPreparedImage?

    var snapshot: DemoPreparedImage? {
        lock.lock(); defer { lock.unlock() }; return prepared
    }
    func snapshot(matching configuration: LabelConfiguration,
                  connection: DemoConnectionConfiguration) -> DemoPreparedImage? {
        lock.lock(); defer { lock.unlock() }
        guard connection.matches(configuration), prepared?.configuration == configuration else { return nil }
        return prepared
    }
    func begin() -> UInt64 {
        lock.lock(); defer { lock.unlock() }
        generation &+= 1; return generation
    }
    func invalidate() {
        lock.lock(); defer { lock.unlock() }
        generation &+= 1; prepared = nil
    }
    func isCurrent(_ ticket: UInt64) -> Bool {
        lock.lock(); defer { lock.unlock() }; return generation == ticket
    }
    @discardableResult
    func publish(_ image: DemoPreparedImage, ticket: UInt64) -> Bool {
        lock.lock(); defer { lock.unlock() }
        guard ticket == generation else { return false }
        prepared = image; return true
    }
}
