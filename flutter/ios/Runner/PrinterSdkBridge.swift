import Foundation
import CoreFoundation
@preconcurrency import Flutter
import XLPrinterSDK

// Flutter invokes plugin methods on its platform main thread. This immutable box
// transfers the reply into MainActor storage; it is never invoked off that actor.
private final class FlutterReplyTransfer: @unchecked Sendable {
  let reply: FlutterResult
  init(_ reply: @escaping FlutterResult) { self.reply = reply }
}

/// One manager belongs to one Flutter engine. The SDK owns decoding and printer IO.
@MainActor
final class PrinterSdkBridge: NSObject, FlutterPlugin, PrinterDelegate {
  private let manager = PrinterManager()
  private var channel: FlutterMethodChannel?
  private var attached = true
  private var closingOwner: PrinterSdkBridge?
  private var closed = false
  private var closing = false
  private var closeResult: ManagerResult<Void>?
  private var closeReplies: [String: FlutterResult] = [:]
  private var requests: [String: RequestID] = [:]
  private var replies: [String: FlutterResult] = [:]
  private var connectionEpoch: UInt64 = 0
  private var connectingToken: String?
  private var connectedToken: String?
  private var connectedRequest: RequestID?

  override init() { super.init(); manager.delegate = self }

  nonisolated static func register(with registrar: FlutterPluginRegistrar) {
    MainActor.assumeIsolated {
      let instance = PrinterSdkBridge()
      let channel = FlutterMethodChannel(
        name: "com.netumscan/printer_sdk", binaryMessenger: registrar.messenger())
      instance.channel = channel
      registrar.addMethodCallDelegate(instance, channel: channel)
      // Flutter only calls detachFromEngine for a published plugin instance.
      registrar.publish(instance)
    }
  }

  nonisolated func handle(_ call: FlutterMethodCall, result reply: @escaping FlutterResult) {
    let transfer = FlutterReplyTransfer(reply)
    MainActor.assumeIsolated { handleOnMain(call, reply: transfer.reply) }
  }

  nonisolated func detachFromEngine(for registrar: FlutterPluginRegistrar) {
    MainActor.assumeIsolated {
      attached = false
      if !closed { closingOwner = self }
      connectionEpoch &+= 1
      connectingToken = nil
      connectedToken = nil
      connectedRequest = nil
      channel?.setMethodCallHandler(nil)
      channel = nil
      replies.removeAll()
      closeReplies.removeAll()
      requests.removeAll()
      if !closed && !closing { closing = true; manager.close() }
    }
  }

  private func handleOnMain(_ call: FlutterMethodCall, reply: @escaping FlutterResult) {
    guard attached else { return }
    let methods = ["sdkInfo", "scanBle", "bondedDevices", "connect", "setPaperType",
                   "printEncodedImage", "locateNextLabel", "disconnect", "close", "cancel"]
    guard methods.contains(call.method) else { reply(FlutterMethodNotImplemented); return }
    let args = call.arguments as? [String: Any] ?? [:]
    guard let token = args["requestId"] as? String, !token.isEmpty, token.utf8.count <= 128 else {
      reply(Self.failure(token: "", code: 1001, message: "requestId must be a non-empty string"))
      return
    }
    if closing || closed {
      switch call.method {
      case "cancel": reply(nil)
      case "close":
        if let closeResult { reply(Self.envelope(closeResult, token: token)) }
        else if replies[token] != nil || closeReplies[token] != nil {
          reply(Self.failure(token: token, code: 1002, message: "requestId is already active"))
        } else { closeReplies[token] = reply }
      default:
        reply(FlutterError(code: "sdk_closed", message: "PrinterManager is permanently closing or closed", details: nil))
      }
      return
    }
    if call.method == "cancel" {
      if let id = requests[token] { manager.cancel(id) }
      // Cancellation never replaces the original request's terminal result.
      reply(nil)
      return
    }
    guard replies[token] == nil else {
      reply(Self.failure(token: token, code: 1002, message: "requestId is already active"))
      return
    }
    replies[token] = reply
    switch call.method {
    case "sdkInfo":
      requests[token] = manager.sdkInfo()
    case "scanBle":
      requests[token] = manager.scanBLE()
    case "bondedDevices":
      reject(token, code: 1004, message: "iOS supports BLE discovery, not bonded-device enumeration")
    case "connect":
      guard args["transport"] as? String == "BLE" else {
        reject(token, code: 1004, message: "iOS supports BLE transport only"); return
      }
      guard let device = args["device"] as? [String: Any],
            let rawID = device["id"] as? String, let id = UUID(uuidString: rawID),
            Self.isNullableString(device["name"]), Self.isNullableString(args["model"]) else {
        reject(token, code: 1001, message: "connect requires a BLE UUID and an optional device name"); return
      }
      let model = args["model"] as? String
      guard model == nil || model == "LT_M90" else {
        reject(token, code: 1004, message: "Unsupported printer model"); return
      }
      guard connectingToken == nil, connectedToken == nil else {
        reject(token, code: 1002, message: "A printer connection is already active"); return
      }
      let target = DiscoveredPrinter(identifier: id, name: device["name"] as? String)
      connectionEpoch &+= 1
      connectingToken = token
      if model == "LT_M90" { requests[token] = manager.connectBle(device: target, model: .ltM90) }
      else { requests[token] = manager.connectBle(device: target) }
    case "setPaperType":
      let paper: PaperType
      switch args["paperType"] as? String {
      case "GAP_LABEL": paper = .gapLabel
      case "BLACK_MARK": paper = .blackMark
      case "CONTINUOUS": paper = .continuous
      default: reject(token, code: 1001, message: "Invalid paperType"); return
      }
      requests[token] = manager.setPaperType(paper)
    case "printEncodedImage":
      guard let bytes = args["imageBytes"] as? FlutterStandardTypedData, bytes.type == .uInt8,
            let width = Self.positiveNumber(args["widthMm"]),
            let height = Self.positiveNumber(args["heightMm"]) else {
        reject(token, code: 1001, message: "Provide encoded image bytes and positive widthMm/heightMm"); return
      }
      requests[token] = manager.printEncodedImage(bytes.data, widthMm: width, heightMm: height)
    case "locateNextLabel":
      requests[token] = manager.locateNextLabel()
    case "disconnect":
      connectionEpoch &+= 1
      connectingToken = nil
      connectedToken = nil
      connectedRequest = nil
      requests[token] = manager.disconnect()
    case "close":
      connectionEpoch &+= 1
      connectingToken = nil
      connectedToken = nil
      connectedRequest = nil
      closing = true
      requests[token] = manager.close()
    default: break
    }
  }

  private func token<T>(for result: ManagerResult<T>) -> String? {
    requests.first { $0.value == result.requestID }?.key
  }

  private func deliver<T>(_ result: ManagerResult<T>, value: Any = NSNull()) {
    guard let token = token(for: result) else { return }
    requests.removeValue(forKey: token)
    guard let reply = replies.removeValue(forKey: token), attached else { return }
    reply(Self.envelope(result, token: token, value: value))
  }

  func printerManager(_ manager: PrinterManager, didFinishReadingSDKInfo result: ManagerResult<SDKInfo>) {
    deliver(result, value: result.value.map { ["distributionVersion": $0.distributionVersion] } ?? NSNull())
  }
  func printerManager(_ manager: PrinterManager, didFinishScanning result: ManagerResult<[DiscoveredPrinter]>) {
    deliver(result, value: result.value.map { $0.map(Self.deviceValue) } ?? NSNull())
  }
  func printerManager(_ manager: PrinterManager, didFinishConnecting result: ManagerResult<ConnectionInfo>) {
    if let token = token(for: result), connectingToken == token {
      connectingToken = nil
      if result.isSuccess { connectedToken = token; connectedRequest = result.requestID }
    }
    deliver(result, value: result.value.map { info in
      ["device": Self.deviceValue(info.device), "model": info.model == .ltM90 ? "LT_M90" as Any : NSNull()]
    } ?? NSNull())
  }
  func printerManager(_ manager: PrinterManager, didLoseConnection result: ManagerResult<Void>) {
    guard attached, connectedRequest == result.requestID, let token = connectedToken else { return }
    connectedToken = nil; connectedRequest = nil
    channel?.invokeMethod("disconnected", arguments: Self.envelope(result, token: token))
  }
  func printerManager(_ manager: PrinterManager, didFinishSettingPaperType result: ManagerResult<Void>) { deliver(result) }
  func printerManager(_ manager: PrinterManager, didFinishPrinting result: ManagerResult<Void>) { deliver(result) }
  func printerManager(_ manager: PrinterManager, didFinishPositioningLabel result: ManagerResult<Void>) { deliver(result) }
  func printerManager(_ manager: PrinterManager, didFinishDisconnecting result: ManagerResult<Void>) { deliver(result) }
  func printerManager(_ manager: PrinterManager, didFinishClosing result: ManagerResult<Void>) {
    closed = true; closing = false; closeResult = result
    deliver(result)
    let waiting = closeReplies; closeReplies.removeAll()
    if attached { for (token, reply) in waiting { reply(Self.envelope(result, token: token)) } }
    closingOwner = nil
  }

  private func reject(_ token: String, code: Int32, message: String) {
    guard let reply = replies.removeValue(forKey: token), attached else { return }
    reply(Self.failure(token: token, code: code, message: message))
  }

  private static func envelope<T>(_ result: ManagerResult<T>, token: String, value: Any = NSNull()) -> [String: Any] {
    let outcome: String
    switch result.outcome {
    case .success: outcome = "SUCCESS"
    case .failure: outcome = "FAILURE"
    case .cancelled: outcome = "CANCELLED"
    }
    let completion: String
    switch result.completion {
    case .none: completion = "NONE"
    case .dataSent: completion = "DATA_SENT"
    case .deviceConfirmed: completion = "DEVICE_CONFIRMED"
    }
    return ["requestId": token, "outcome": outcome, "code": result.code, "message": result.message,
            "completion": completion, "outputMayHaveOccurred": result.outputMayHaveOccurred, "value": value]
  }

  private static func failure(token: String, code: Int32, message: String) -> [String: Any] {
    ["requestId": token, "outcome": "FAILURE", "code": code, "message": message,
     "completion": "NONE", "outputMayHaveOccurred": false, "value": NSNull()]
  }

  private static func deviceValue(_ device: DiscoveredPrinter) -> [String: Any] {
    ["id": device.identifier.uuidString, "name": device.name as Any? ?? NSNull(),
     "rssi": device.signalStrength as Any? ?? NSNull()]
  }

  private static func isNullableString(_ value: Any?) -> Bool {
    value == nil || value is NSNull || value is String
  }

  private static func positiveNumber(_ value: Any?) -> Double? {
    guard let number = value as? NSNumber, CFGetTypeID(number) != CFBooleanGetTypeID() else { return nil }
    let value = number.doubleValue
    return value.isFinite && value > 0 ? value : nil
  }
}
