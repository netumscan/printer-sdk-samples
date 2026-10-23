import Foundation
import XLPrinterSDK

/// Unknown values never inherit the appearance of a normal hardware condition.
enum DemoStatusEmphasis: Equatable {
    case unknown, normal, warning
    init(_ fault: Bool?) {
        switch fault {
        case true?: self = .warning
        case false?: self = .normal
        case nil: self = .unknown
        }
    }
    init(status: StatusResult?) {
        if status?.coverOpen == true || status?.paperOut == true { self = .warning }
        else if status?.coverOpen == false && status?.paperOut == false { self = .normal }
        else { self = .unknown }
    }
    var symbol: String {
        switch self {
        case .unknown: return "questionmark.circle"
        case .normal: return "checkmark.circle.fill"
        case .warning: return "exclamationmark.triangle.fill"
        }
    }
}

/// A connection-scoped UI event, separate from the last SDK request result.
struct DemoStatusNotice: Identifiable, Equatable, Sendable {
    let id: UInt64
    let message: String
}

/// Each case retains the complete SDK result, including its typed optional value.
enum DemoResult: Sendable {
    enum Action: Sendable { case connect, configure, printLabel, printReceipt, raw, media, feed, getStatus, disconnected, disconnect }
    case action(Action, ManagerResult<Void>)
    case connection(Action, ManagerResult<ConnectionInfo>)
    case status(ManagerResult<StatusResult>)
    case capabilities(ManagerResult<PrinterCapabilities>)
    case identity(ManagerResult<SDKInfo>)
    case discovery(ManagerResult<[DiscoveredPrinter]>)

    var evidence: DemoResultEvidence {
        switch self {
        case .action(_, let result): return DemoResultEvidence(result)
        case .connection(_, let result): return DemoResultEvidence(result)
        case .status(let result): return DemoResultEvidence(result)
        case .capabilities(let result): return DemoResultEvidence(result)
        case .identity(let result): return DemoResultEvidence(result)
        case .discovery(let result): return DemoResultEvidence(result)
        }
    }
}

/// Common fields are copied for rendering; the original result remains in DemoResult.
struct DemoResultEvidence: Sendable {
    let requestID: RequestID
    let outcome: ManagerOutcome
    let code: Int32
    let message: String
    let completion: CompletionLevel
    let outputMayHaveOccurred: Bool
    init<Value>(_ result: ManagerResult<Value>) {
        requestID = result.requestID; outcome = result.outcome; code = result.code
        message = result.message; completion = result.completion
        outputMayHaveOccurred = result.outputMayHaveOccurred
    }
}

/// A read has no effects: repeated SwiftUI rendering never consumes a result or candidates.
struct DemoViewState {
    let devices: [DiscoveredPrinter]
    let selected: UUID?
    let scanning: Bool
    let canScan, canEditConnection, canConnect, canDisconnect, canWork, canCancel: Bool
    let status: String
    let result, cleanupResult, connectionLoss, identityResult: DemoResult?
    let inputError: DemoInputError?
    let identity: SDKInfo?
}
