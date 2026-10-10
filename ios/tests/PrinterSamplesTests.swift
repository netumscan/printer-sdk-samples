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
}

private func memory(_ context: UnsafeMutableRawPointer?) -> MemoryTransport {
    Unmanaged<MemoryTransport>.fromOpaque(context!).takeUnretainedValue()
}

final class PrinterSamplesTests: XCTestCase {
    func testPublishedABIAndCatalog() throws {
        XCTAssertEqual(psdk_abi_version(), 2)
        XCTAssertEqual(psdk_abi_revision(), 10)
        var count: UInt32 = UInt32.max
        XCTAssertEqual(psdk_profile_count(&count), PSDK_OK)
        XCTAssertEqual(count, 0, "Preview configuration does not certify a hardware catalog")
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
        var profile = psdk_transport_profile()
        XCTAssertEqual(psdk_transport_profile_init(&profile, MemoryLayout.size(ofValue: profile)), PSDK_OK)
        profile.protocol = UInt32(PSDK_PROTOCOL_CPCL)
        profile.dpi = 203; profile.width_dots = 384; profile.max_label_height_dots = 65535
        profile.cpcl_resolution = 200; profile.stripe_height = 128; profile.chunk_size = 1024
        profile.write_timeout_ms = 1000; profile.query_timeout_ms = 1000
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
            let fake = memory(context); fake.lock.lock(); defer { fake.lock.unlock() }
            let accepted = min(count, 3)
            fake.bytes.append(data!, count: accepted); written!.pointee = accepted; return PSDK_OK
        }
        callbacks.dispose = { context in Unmanaged<MemoryTransport>.fromOpaque(context!).release() }
        var device: OpaquePointer?
        let created = psdk_device_create_transport(&profile, &callbacks, &device)
        if created != PSDK_OK { Unmanaged<MemoryTransport>.fromOpaque(callbacks.context!).release() }
        XCTAssertEqual(created, PSDK_OK)
        guard created == PSDK_OK else { return }
        defer { psdk_device_destroy(device) }
        XCTAssertEqual(psdk_device_connect(device, 1000), PSDK_OK)
        var options = psdk_operation_options()
        XCTAssertEqual(psdk_operation_options_init(&options, MemoryLayout.size(ofValue: options)), PSDK_OK)
        options.timeout_ms = 1000
        let first: [UInt8] = [0x1b, 0x40, 0x00, 0xff, 0x0a, 0x0d, 0x01]
        let second: [UInt8] = [0x02, 0x03, 0x04, 0x05]
        var operationA: OpaquePointer?, operationB: OpaquePointer?
        XCTAssertEqual(first.withUnsafeBufferPointer {
            psdk_device_send_raw(device, $0.baseAddress, $0.count, 2, &options, &operationA)
        }, PSDK_OK)
        XCTAssertEqual(second.withUnsafeBufferPointer {
            psdk_device_send_raw(device, $0.baseAddress, $0.count, 1, &options, &operationB)
        }, PSDK_OK)
        defer { psdk_operation_release(operationA); psdk_operation_release(operationB) }
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
        XCTAssertEqual(psdk_device_disconnect(device), PSDK_OK)
    }
}
