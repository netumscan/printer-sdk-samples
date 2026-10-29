@preconcurrency import Flutter
import XCTest
@testable import Runner

final class RunnerTests: XCTestCase {
  @MainActor
  func testInvalidImageDimensionsHaveNoOutput() {
    let bridge = PrinterSdkBridge()
    var result: [String: Any]?
    bridge.handle(FlutterMethodCall(methodName: "printEncodedImage", arguments: [
      "requestId": "invalid-image", "imageBytes": FlutterStandardTypedData(bytes: Data([1, 2, 3])),
      "widthMm": 0.0, "heightMm": 30.0,
    ])) { result = $0 as? [String: Any] }
    XCTAssertEqual(result?["requestId"] as? String, "invalid-image")
    XCTAssertEqual(result?["outcome"] as? String, "FAILURE")
    XCTAssertEqual(result?["code"] as? Int32, 1001)
    XCTAssertEqual(result?["completion"] as? String, "NONE")
    XCTAssertEqual(result?["outputMayHaveOccurred"] as? Bool, false)
  }

  @MainActor
  func testBondedDevicesAreUnsupportedOnIOS() {
    let bridge = PrinterSdkBridge()
    var result: [String: Any]?
    bridge.handle(FlutterMethodCall(methodName: "bondedDevices", arguments: ["requestId": "bonded"])) {
      result = $0 as? [String: Any]
    }
    XCTAssertEqual(result?["outcome"] as? String, "FAILURE")
    XCTAssertEqual(result?["code"] as? Int32, 1004)
  }

  @MainActor
  func testSDKInfoCompletesAsynchronouslyAndRejectsDuplicateActiveToken() {
    let bridge = PrinterSdkBridge()
    let completed = expectation(description: "SDK info terminal reply")
    var callbacks = 0
    let call = FlutterMethodCall(methodName: "sdkInfo", arguments: ["requestId": "info"])
    bridge.handle(call) { response in
      callbacks += 1
      let result = response as? [String: Any]
      XCTAssertEqual(result?["requestId"] as? String, "info")
      XCTAssertEqual(result?["outcome"] as? String, "SUCCESS")
      let value = result?["value"] as? [String: Any]
      XCTAssertFalse((value?["distributionVersion"] as? String ?? "").isEmpty)
      completed.fulfill()
    }
    XCTAssertEqual(callbacks, 0)
    bridge.handle(call) { response in
      let result = response as? [String: Any]
      XCTAssertEqual(result?["code"] as? Int32, 1002)
    }
    wait(for: [completed], timeout: 5)
    XCTAssertEqual(callbacks, 1)
  }

  @MainActor
  func testCancelAcknowledgesWithoutASecondTerminalEnvelope() {
    let bridge = PrinterSdkBridge()
    var acknowledged = false
    bridge.handle(FlutterMethodCall(methodName: "cancel", arguments: ["requestId": "already-finished"])) {
      XCTAssertNil($0)
      acknowledged = true
    }
    XCTAssertTrue(acknowledged)
  }

  @MainActor
  func testPermanentCloseRejectsNewRequestsAndCompletesRepeatedCloseLocally() {
    let bridge = PrinterSdkBridge()
    let firstClose = expectation(description: "Original close terminal")
    let repeatedClose = expectation(description: "Repeated close shares the cleanup barrier")
    bridge.handle(FlutterMethodCall(methodName: "close", arguments: ["requestId": "close-first"])) { response in
      XCTAssertEqual((response as? [String: Any])?["code"] as? Int32, 0)
      firstClose.fulfill()
    }
    var rejectedWhileClosing = false
    bridge.handle(FlutterMethodCall(methodName: "sdkInfo", arguments: ["requestId": "info-during-close"])) {
      XCTAssertEqual(($0 as? FlutterError)?.code, "sdk_closed")
      rejectedWhileClosing = true
    }
    XCTAssertTrue(rejectedWhileClosing)
    bridge.handle(FlutterMethodCall(methodName: "close", arguments: ["requestId": "close-again"])) { response in
      XCTAssertEqual((response as? [String: Any])?["requestId"] as? String, "close-again")
      repeatedClose.fulfill()
    }
    wait(for: [firstClose, repeatedClose], timeout: 5)
    var rejectedAfterClose = false
    bridge.handle(FlutterMethodCall(methodName: "connect", arguments: ["requestId": "connect-after-close"])) {
      XCTAssertEqual(($0 as? FlutterError)?.code, "sdk_closed")
      rejectedAfterClose = true
    }
    XCTAssertTrue(rejectedAfterClose)
    var localClose = false
    bridge.handle(FlutterMethodCall(methodName: "close", arguments: ["requestId": "close-local"])) { response in
      XCTAssertEqual((response as? [String: Any])?["requestId"] as? String, "close-local")
      localClose = true
    }
    XCTAssertTrue(localClose)
    var cancelled = false
    bridge.handle(FlutterMethodCall(methodName: "cancel", arguments: ["requestId": "old-request"])) {
      XCTAssertNil($0); cancelled = true
    }
    XCTAssertTrue(cancelled)
  }

  @MainActor
  func testDetachDropsPendingReply() throws {
    let engine = FlutterEngine(name: "bridge-detach-test", project: nil, allowHeadlessExecution: true)
    defer { engine.destroyContext() }
    XCTAssertTrue(engine.run(withEntrypoint: nil))
    let registrar = try XCTUnwrap(engine.registrar(forPlugin: "BridgeDetachTest"))
    PrinterSdkBridge.register(with: registrar)
    let bridge = try XCTUnwrap(engine.valuePublished(byPlugin: "BridgeDetachTest") as? PrinterSdkBridge)
    let late = expectation(description: "Detached engine receives no late result")
    late.isInverted = true
    bridge.handle(FlutterMethodCall(methodName: "sdkInfo", arguments: ["requestId": "detached"])) { _ in
      late.fulfill()
    }
    bridge.detachFromEngine(for: registrar)
    wait(for: [late], timeout: 0.25)
  }
}
