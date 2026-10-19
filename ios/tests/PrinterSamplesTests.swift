// SPDX-License-Identifier: MIT
// Copyright (c) 2026 NETUM
import CPrinterSDK
import Foundation
import PrinterSDK
@testable import PrinterQuickStart
import XCTest

private final class MemoryTransport {
    let lock = NSLock()
    var open = false
    var bytes = Data()
    let firstWrite = DispatchSemaphore(value: 0)
    let releaseWrite = DispatchSemaphore(value: 0)
    var started = false
}

private func memory(_ context: UnsafeMutableRawPointer?) -> MemoryTransport {
    Unmanaged<MemoryTransport>.fromOpaque(context!).takeUnretainedValue()
}

// The counter is shared by a detached preparation and the test's main actor.
private final class AdmissionCounter: @unchecked Sendable {
    private let lock = NSLock()
    private var value = 0
    func admit() -> Int { lock.lock(); defer { lock.unlock() }; value += 1; return value }
    var count: Int { lock.lock(); defer { lock.unlock() }; return value }
}

final class PrinterSamplesTests: XCTestCase {
    @MainActor
    func testCancellingSuspendedPreparationPreventsSubmission() async throws {
        let entered = DispatchSemaphore(value: 0), resume = DispatchSemaphore(value: 0)
        let admissions = AdmissionCounter()
        let preparation = Task.detached {
            try QuickStartModel.prepareAndSubmit({
                entered.signal()
                guard resume.wait(timeout: .now() + 5) == .success else { throw PrinterSDKError(1002) }
                return Data([0x00])
            }, submit: { _ in admissions.admit() })
        }
        defer { resume.signal(); preparation.cancel() }
        XCTAssertEqual(entered.wait(timeout: .now() + 3), .success)
        preparation.cancel() // User cancels while decoding/rendering is suspended.
        resume.signal()
        do { _ = try await preparation.value; XCTFail("Cancelled preparation must not submit") }
        catch is CancellationError {}
        XCTAssertEqual(admissions.count, 0, "Cancelled preparation must stop before the SDK admission boundary")
    }

    @MainActor
    func testDisconnectedSessionCanBeExplicitlyReleasedAndRecreated() async throws {
        let model = QuickStartModel(), identifier = UUID()
        let options = try M90Profile.bleOptions()
        let profile = try CpclProfile(dpi: 203, widthDots: 384, maxLabelHeightDots: 65535, form: false)
        let first = try model.installSession(identifier: identifier, options: options, profile: profile)
        // A real native disconnected snapshot is delivered through the SDK observer.
        // Simulate the prior UI-ready value without opening Bluetooth hardware.
        model.connected = true
        let deadline = Date(timeIntervalSinceNow: 3)
        while model.connected && Date() < deadline { try await Task.sleep(nanoseconds: 1_000_000) }
        XCTAssertFalse(model.connected)
        XCTAssertTrue(model.hasSession, "The UI must retain an explicit release action after link loss")
        model.disconnect()
        while model.busy && Date() < deadline { try await Task.sleep(nanoseconds: 1_000_000) }
        XCTAssertFalse(model.busy)
        XCTAssertFalse(model.hasSession)
        let replacement = try model.installSession(identifier: identifier, options: options, profile: profile)
        XCTAssertFalse(first === replacement, "An explicit retry creates a new owned device after release")
        XCTAssertTrue(model.hasSession)
        model.disconnect()
    }

    func testPublishedABIAndCatalog() throws {
        XCTAssertEqual(psdk_abi_version(), 3)
        XCTAssertEqual(psdk_abi_revision(), 1)
        let info = try PrinterSDK.info()
        XCTAssertEqual(info.nativeVersion, "0.3.0")
        XCTAssertEqual(info.distributionVersion, "0.3.0-preview.2")
        XCTAssertEqual(info.abiVersion, 3)
        XCTAssertEqual(info.abiRevision, 1)
    }

    func testPublicSwiftConfigurationValidation() throws {
        let options = try M90Profile.bleOptions()
        XCTAssertEqual(options.maxWriteBytes, 512)
        XCTAssertFalse(options.withResponse)
        XCTAssertNotNil(options.creditFlowControl)
        XCTAssertThrowsError(try CpclProfile(dpi: 203, widthDots: 7, maxLabelHeightDots: 65535, form: false))
        XCTAssertThrowsError(try M90Profile.bleOptions(maxWriteBytes: 0))
        XCTAssertThrowsError(try ReceiptProfile(dpi: 203, widthDots: 0))
    }

    @MainActor
    func testQuickStartRejectsInvalidConfigurationBeforeConnection() {
        let model = QuickStartModel()
        XCTAssertFalse(model.connected)
        XCTAssertFalse(model.scanning)
        model.selected = UUID()
        model.widthDots = "7"
        model.connect()
        XCTAssertFalse(model.busy)
        XCTAssertFalse(model.connected)
        XCTAssertTrue(model.status.contains("1001"))
        XCTAssertTrue(QuickStartModel.terminalMessage(state: 4, completion: 1, errorCode: 0, bytesSent: 10)
            .hasPrefix("DATA_SENT"))
        XCTAssertFalse(QuickStartModel.terminalMessage(state: 5, completion: 1, errorCode: 0, bytesSent: 10)
            .hasPrefix("DATA_SENT"))
        XCTAssertFalse(QuickStartModel.terminalMessage(state: 4, completion: 0, errorCode: 0, bytesSent: 10)
            .hasPrefix("DATA_SENT"))
        XCTAssertFalse(QuickStartModel.terminalMessage(state: 4, completion: 1, errorCode: 2002, bytesSent: 10)
            .hasPrefix("DATA_SENT"))
    }

    func testPartialWritesAndRawQueueOrderWithoutHardware() throws {
        let fake = MemoryTransport()
        var callbacks = psdk_transport_callbacks()
        XCTAssertEqual(psdk_transport_callbacks_init(&callbacks, MemoryLayout.size(ofValue: callbacks)), PSDK_OK)
        callbacks.context = Unmanaged.passRetained(fake).toOpaque()
        callbacks.transport_type = UInt32(PSDK_BLE)
        callbacks.write_count_flags = UInt32(PSDK_WRITE_ERROR_COUNT_EXACT)
        callbacks.open = { context, _ in
            let fake = memory(context); fake.lock.lock(); defer { fake.lock.unlock() }
            fake.open = true; return PSDK_OK
        }
        callbacks.close = { context in
            let fake = memory(context); fake.lock.lock(); defer { fake.lock.unlock() }; fake.open = false
        }
        callbacks.is_open = { context in
            let fake = memory(context); fake.lock.lock(); defer { fake.lock.unlock() }; return fake.open ? 1 : 0
        }
        callbacks.write = { context, data, count, _, written in
            let fake = memory(context)
            fake.lock.lock(); let first = !fake.started; fake.started = true; fake.lock.unlock()
            if first {
                fake.firstWrite.signal()
                guard fake.releaseWrite.wait(timeout: .now() + 5) == .success else { return PSDK_TRANSPORT_TIMEOUT }
            }
            fake.lock.lock(); defer { fake.lock.unlock() }
            let accepted = min(count, 3)
            fake.bytes.append(data!, count: accepted); written!.pointee = accepted; return PSDK_OK
        }
        callbacks.dispose = { context in Unmanaged<MemoryTransport>.fromOpaque(context!).release() }
        var device: OpaquePointer?
        let created = psdk_device_create(&callbacks, &device)
        if created != PSDK_OK { Unmanaged<MemoryTransport>.fromOpaque(callbacks.context!).release() }
        XCTAssertEqual(created, PSDK_OK)
        guard created == PSDK_OK else { return }
        defer { psdk_device_destroy(device) }
        XCTAssertEqual(psdk_device_connect(device, 1000, nil), PSDK_OK)
        var options = psdk_operation_options()
        XCTAssertEqual(psdk_operation_options_init(&options, MemoryLayout.size(ofValue: options)), PSDK_OK)
        options.timeout_ms = 1000
        options.required_completion = UInt32(PSDK_DATA_SENT)
        let first: [UInt8] = [0x1b, 0x40, 0x00, 0xff, 0x0a, 0x0d, 0x01]
        let second: [UInt8] = [0x02, 0x03, 0x04, 0x05]
        var operationA: OpaquePointer?, operationB: OpaquePointer?, cancelled: OpaquePointer?
        func send(_ data: [UInt8], copies: UInt32, operation: inout OpaquePointer?) -> Int32 {
            data.withUnsafeBufferPointer {
                var request = psdk_execute_request()
                _ = psdk_execute_request_init(&request, MemoryLayout.size(ofValue: request))
                request.kind = UInt32(PSDK_OP_RAW)
                request.data.raw.data = $0.baseAddress; request.data.raw.data_size = $0.count
                request.data.raw.copies = copies
                return psdk_device_execute(device, &request, &options, &operation)
            }
        }
        XCTAssertEqual(send(first, copies: 2, operation: &operationA), PSDK_OK)
        defer { fake.releaseWrite.signal() }
        XCTAssertEqual(fake.firstWrite.wait(timeout: .now() + 3), .success)
        XCTAssertEqual(send(second, copies: 1, operation: &operationB), PSDK_OK)
        XCTAssertEqual(send([0x99], copies: 1, operation: &cancelled), PSDK_OK)
        XCTAssertEqual(psdk_operation_cancel(cancelled), PSDK_OK)
        fake.releaseWrite.signal()
        defer { psdk_operation_release(operationA); psdk_operation_release(operationB); psdk_operation_release(cancelled) }
        for (operation, bytes) in [(operationA, UInt64(14)), (operationB, UInt64(4))] {
            var snapshot = psdk_operation_snapshot()
            XCTAssertEqual(psdk_operation_snapshot_init(&snapshot, MemoryLayout.size(ofValue: snapshot)), PSDK_OK)
            let deadline = Date(timeIntervalSinceNow: 3)
            repeat {
                XCTAssertEqual(psdk_operation_get_snapshot(operation, &snapshot), PSDK_OK)
                if snapshot.state >= 4 { break }
                Thread.sleep(forTimeInterval: 0.001)
            } while Date() < deadline
            XCTAssertEqual(snapshot.state, 4)
            XCTAssertEqual(snapshot.error_code, 0)
            XCTAssertEqual(snapshot.completion, 1)
            XCTAssertEqual(snapshot.bytes_sent, bytes)
        }
        fake.lock.lock(); let transferred = fake.bytes; fake.lock.unlock()
        XCTAssertEqual(transferred, Data(first + first + second))
        var cancelledSnapshot = psdk_operation_snapshot()
        XCTAssertEqual(psdk_operation_snapshot_init(&cancelledSnapshot, MemoryLayout.size(ofValue: cancelledSnapshot)), PSDK_OK)
        let deadline = Date(timeIntervalSinceNow: 3)
        repeat {
            XCTAssertEqual(psdk_operation_get_snapshot(cancelled, &cancelledSnapshot), PSDK_OK)
            if cancelledSnapshot.state >= 4 { break }
            Thread.sleep(forTimeInterval: 0.001)
        } while Date() < deadline
        XCTAssertEqual(cancelledSnapshot.error_code, PSDK_CANCELLED)
        XCTAssertEqual(cancelledSnapshot.bytes_sent, 0)
        var resources = psdk_resource_snapshot()
        XCTAssertEqual(psdk_resource_snapshot_init(&resources, MemoryLayout.size(ofValue: resources)), PSDK_OK)
        XCTAssertEqual(psdk_device_get_resources(device, &resources), PSDK_OK)
        XCTAssertEqual(resources.outstanding_operations, 0, "Terminal results do not occupy the queue")
        XCTAssertEqual(resources.pending_payload_bytes, 0)
        var state = psdk_device_state_snapshot()
        XCTAssertEqual(psdk_device_state_snapshot_init(&state, MemoryLayout.size(ofValue: state)), PSDK_OK)
        XCTAssertEqual(psdk_device_get_state(device, &state), PSDK_OK)
        XCTAssertEqual(state.hardware_state, UInt32(PSDK_HARDWARE_UNKNOWN))
        XCTAssertEqual(state.queued_count, 0)
        // Cancel is idempotent for a terminal operation; close/release remains a separate action.
        XCTAssertEqual(psdk_operation_cancel(operationA), PSDK_OK)
        XCTAssertEqual(psdk_device_disconnect(device), PSDK_OK)
        XCTAssertEqual(psdk_device_disconnect(device), PSDK_OK)
    }
}
