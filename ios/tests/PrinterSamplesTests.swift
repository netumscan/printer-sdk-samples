// SPDX-License-Identifier: MIT
// Copyright (c) 2026 NETUM
import CPrinterSDK
import Foundation
import CoreBluetooth
@testable import PrinterSDK
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

final class PrinterSamplesTests: XCTestCase {
    @MainActor
    func testModelCloseIsPermanentAndImmediatelyStopsManagerAdmission() async {
        let manager = PrinterManager(), model = QuickStartModel(manager: manager)
        model.close(); model.close(); model.selected = UUID(); model.connect()
        let result: ManagerResult<SDKInfo> = await withCheckedContinuation { continuation in
            manager.sdkInfo { continuation.resume(returning: $0) }
        }
        XCTAssertEqual(model.phase, .closed); XCTAssertEqual(result.code, 1002)
        XCTAssertFalse(model.connected); XCTAssertFalse(model.printing)
    }
    @MainActor
    func testModelDisconnectWaitsForManagerCallback() async {
        let model = QuickStartModel()
        model.disconnect(); XCTAssertEqual(model.phase, .disconnecting)
        for _ in 0..<300 where model.phase == .disconnecting { try? await Task.sleep(nanoseconds: 1_000_000) }
        XCTAssertEqual(model.phase, .offline); XCTAssertFalse(model.connected)
        model.close()
    }
    @MainActor
    func testOfflinePrintButtonDoesNotCreateARequest() {
        let model = QuickStartModel()
        model.printSample(); model.cancel()
        XCTAssertNil(model.lastResult); XCTAssertEqual(model.phase, .offline)
        XCTAssertEqual(model.resultDescription, "尚无连接/打印结果"); model.close()
    }
    func testDevelopmentRuntimeIdentity() async {
        let manager = PrinterManager()
        let result: ManagerResult<SDKInfo> = await withCheckedContinuation { continuation in
            manager.sdkInfo { continuation.resume(returning: $0) }
        }
        XCTAssertEqual(result.value?.abiVersion, 3); XCTAssertEqual(result.value?.abiRevision, 2)
        XCTAssertEqual(result.value?.nativeVersion, "0.3.0")
        await withCheckedContinuation { continuation in manager.close { _ in continuation.resume() } }
    }
    func testPublicModelAndPaperTypeAreMinimalBusinessValues() {
        XCTAssertEqual(PrinterModel.allCases, [.ltM90])
        XCTAssertEqual(PrinterModel.ltM90.rawValue, "LT-M90")
        XCTAssertEqual(PaperType.allCases.count, 3)
        let target = DiscoveredPrinter(identifier: UUID(), name: "saved printer")
        XCTAssertEqual(target.name, "saved printer"); XCTAssertNil(target.signalStrength)
    }
    @MainActor
    func testConnectRequiresAnExplicitSelectionFromDiscoveredCandidates() {
        let model = QuickStartModel(); defer { model.close() }
        let candidate = DiscoveredPrinter(identifier: UUID(), name: "LT-M90_A1b2", advertisedServices: [], rssi: -50)
        model.devices = [candidate]; model.connect()
        XCTAssertEqual(model.phase, .offline); XCTAssertNil(model.connectionResult)
        model.selected = UUID(); model.connect()
        XCTAssertEqual(model.phase, .offline); XCTAssertNil(model.connectionResult)
    }
    func testFixedImageIsGray8Checkerboard() {
        let pixels = QuickStartModel.testImage(width: 384, height: 64)
        XCTAssertEqual(pixels.count, 384 * 64)
        XCTAssertEqual(Set(pixels), [0, 255])
        XCTAssertEqual(pixels[0], 0); XCTAssertEqual(pixels[16], 255)
        XCTAssertEqual(pixels[16 * 384], 255); XCTAssertEqual(pixels[16 * 384 + 16], 0)
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

// Test-only channel injection exercises the real Swift Manager/native executor.
// No application adapter or SDK public testing API is introduced.
private final class QuickStartMemoryChannel: BluetoothByteChannel, @unchecked Sendable {
    private let lock = NSLock()
    private var opened = false, interrupted = false, holdOpen = false
    private var holdAfter: Int?
    private var bytes = Data(), opens = 0, openTimeout: TimeInterval = 0
    private var observer: ChannelConnectionObserver?
    let opening = DispatchSemaphore(value: 0), blockedWrite = DispatchSemaphore(value: 0)
    var isOpen: Bool { lock.lock(); defer { lock.unlock() }; return opened }
    var sent: Data { lock.lock(); defer { lock.unlock() }; return bytes }
    var openCount: Int { lock.lock(); defer { lock.unlock() }; return opens }
    var connectionTimeout: TimeInterval { lock.lock(); defer { lock.unlock() }; return openTimeout }
    func configure(holdOpen: Bool = false, holdWriteAfter: Int? = nil) {
        lock.lock(); self.holdOpen = holdOpen; holdAfter = holdWriteAfter.map { bytes.count + $0 }; lock.unlock()
    }
    func open(_ control: IOControl) throws {
        lock.lock(); interrupted = false; opens += 1; let held = holdOpen; openTimeout = control.timeout; lock.unlock()
        opening.signal()
        let deadline = ProcessInfo.processInfo.systemUptime + control.timeout
        while held {
            lock.lock(); let stopped = interrupted; lock.unlock()
            if stopped || control.cancelled() { throw PrinterSDKError(1005) }
            if ProcessInfo.processInfo.systemUptime >= deadline { throw PrinterSDKError(2001) }
            Thread.sleep(forTimeInterval: 0.001)
        }
        lock.lock(); opened = true; lock.unlock()
    }
    func close() { lock.lock(); opened = false; interrupted = true; lock.unlock() }
    func write(_ data: Data, _ control: IOControl) throws -> Int {
        lock.lock(); let held = holdAfter.map { bytes.count >= $0 } ?? false; lock.unlock()
        if held { blockedWrite.signal() }
        let deadline = ProcessInfo.processInfo.systemUptime + control.timeout
        while held {
            if !isOpen || control.cancelled() { throw PrinterSDKError(1005) }
            if ProcessInfo.processInfo.systemUptime >= deadline { throw PrinterSDKError(2001) }
            Thread.sleep(forTimeInterval: 0.001)
        }
        lock.lock(); defer { lock.unlock() }
        guard opened else { throw PrinterSDKError(2002) }
        let count = min(3, data.count); bytes.append(data.prefix(count)); return count
    }
    func setConnectionObserver(_ observer: ChannelConnectionObserver?) {
        lock.lock(); self.observer = observer; lock.unlock()
    }
    func lose() {
        lock.lock(); let observer = observer; opened = false; interrupted = true; lock.unlock()
        if let observer {
            observer.lost(observer.epoch(), ErrorDiagnostic(sequence: 0, connectionGeneration: 0,
                transportEpoch: 0, operationID: 0, sampledMonotonicMilliseconds: nil,
                errorCode: 2002, source: .transport, nativeCode: nil, domain: "test", message: "memory connection lost"))
        }
    }
}

private final class QuickStartScanFixture: @unchecked Sendable {
    private final class Central: BLEScanCentral, @unchecked Sendable {
        var stateChanged: (@Sendable (BLEScanCentral, CBManagerState) -> Void)?
        var discovered: (@Sendable (BLEScanCentral, DiscoveredPrinter) -> Void)?
        func start(service: UUID?) {}
        func stop() {}
        func detach() { stateChanged = nil; discovered = nil }
    }
    private final class Deadline: BLEScanDeadline, Sendable { func cancel() {} }
    private let queue = DispatchQueue(label: "PrinterQuickStartTests.scan")
    private var centrals: [Central] = []
    private var deadlines: [@Sendable () -> Void] = []
    var count: Int { queue.sync { centrals.count } }
    func scanner() -> BLEScanner {
        BLEScanner(queue: queue, makeCentral: { _ in
            let central = Central(); self.centrals.append(central); return central
        }, scheduleDeadline: { _, _, action in self.deadlines.append(action); return Deadline() })
    }
    func complete(with device: DiscoveredPrinter) {
        queue.sync {
            let central = centrals.last!
            central.stateChanged?(central, .poweredOn); central.discovered?(central, device); deadlines.last?()
        }
    }
    // Simulate an already queued platform deadline after cancellation/detach.
    func expire(_ index: Int) { queue.sync { deadlines[index]() } }
}

extension PrinterSamplesTests {
    @MainActor
    private func consumer(_ channel: QuickStartMemoryChannel) -> (QuickStartModel, PrinterManager) {
        let manager = PrinterManager(makePrinter: { _, options in
            XCTAssertEqual(options.service, UUID(uuidString: "0000FF00-0000-1000-8000-00805F9B34FB"))
            XCTAssertEqual(options.maxWriteBytes, 512); XCTAssertFalse(options.withResponse)
            XCTAssertNotNil(options.creditFlowControl)
            return try MobilePrinter(channel: channel)
        })
        let model = QuickStartModel(manager: manager)
        let device = DiscoveredPrinter(identifier: UUID(), name: "LT-M90_A1B2", advertisedServices: [], rssi: -50)
        model.devices = [device]; model.selected = device.identifier
        return (model, manager)
    }
    @MainActor
    private func until(_ condition: () -> Bool, file: StaticString = #filePath, line: UInt = #line) async throws {
        for _ in 0..<3000 {
            if condition() { return }
            try await Task.sleep(nanoseconds: 1_000_000)
        }
        XCTFail("Expected Quick Start consumer callback", file: file, line: line); throw PrinterSDKError(2001)
    }
    @MainActor
    func testConsumerUnknownNameReportsUnsupportedWithoutOpeningTransport() async throws {
        let channel = QuickStartMemoryChannel(), (model, _) = consumer(channel); defer { model.close() }
        for name: String? in ["Unknown_A1B2", "LT-M90_A1B2 · 12345678", "LT-M90_A1B", "LT-M90_A1B2X", nil] {
            let unknown = DiscoveredPrinter(identifier: UUID(), name: name)
            model.devices = [unknown]; model.selected = unknown.identifier; model.connect()
            try await until { model.canConnect }
            XCTAssertEqual(model.phase, .offline); XCTAssertEqual(model.connectionResult?.code, 1004)
            XCTAssertNil(model.lastResult); XCTAssertEqual(channel.openCount, 0); XCTAssertTrue(channel.sent.isEmpty)
        }
    }
    @MainActor
    func testConsumerFailedConnectKeepsOriginalFailureAndWaitsForDisconnectBeforeRetry() async throws {
        let manager = PrinterManager(makePrinter: { _, _ in throw PrinterSDKError(2002) })
        let model = QuickStartModel(manager: manager); defer { model.close() }
        let candidate = DiscoveredPrinter(identifier: UUID(), name: "LT-M90_A1B2", advertisedServices: [], rssi: -50)
        model.devices = [candidate]; model.selected = candidate.identifier
        model.connect(); XCTAssertEqual(model.phase, .connecting); XCTAssertFalse(model.canConnect)
        try await until { model.canConnect }
        XCTAssertEqual(model.connectionResult?.outcome, .failure); XCTAssertEqual(model.connectionResult?.code, 2002)
        XCTAssertEqual(model.cleanupResult?.code, 0); XCTAssertNil(model.lastResult)
        let firstID = model.connectionResult?.requestID
        model.connect(); XCTAssertFalse(model.canConnect)
        try await until { model.canConnect && model.connectionResult?.requestID != firstID }
        XCTAssertEqual(model.connectionResult?.code, 2002); XCTAssertEqual(model.cleanupResult?.code, 0)
    }
    @MainActor
    func testConsumerAdoptsOriginalScannedNameAndPrintsFixedSingleCopyWithoutForm() async throws {
        let channel = QuickStartMemoryChannel(), (model, _) = consumer(channel); defer { model.close() }
        model.connect(); try await until { model.canPrint }
        let info = try XCTUnwrap(model.connectionResult?.value)
        XCTAssertEqual(info.deviceName, "LT-M90_A1B2")
        XCTAssertEqual(info.configurationSource, .nameRecommendation)
        XCTAssertEqual(info.printProfile?.dpi, 203); XCTAssertEqual(info.printProfile?.maxPrintWidthDots, 864)
        XCTAssertEqual(info.printProfile?.labelAlignment, .centered)
        // Later display/selection changes cannot replace the adopted connection profile.
        model.devices = []; model.selected = nil; model.printSample()
        try await until { model.lastAction == .print }
        XCTAssertEqual(model.lastResult?.code, 0); XCTAssertEqual(model.lastResult?.completion, .dataSent)
        let header = Data("! 0 200 200 64 1\r\nPAGE-WIDTH 864\r\nCG 48 64 240 0 ".utf8)
        let footer = Data("\r\nPRINT\r\n".utf8)
        XCTAssertTrue(channel.sent.starts(with: header)); XCTAssertTrue(channel.sent.suffix(footer.count) == footer)
        XCTAssertNil(channel.sent.range(of: Data("FORM".utf8)))
        XCTAssertEqual(channel.sent.count, header.count + 48 * 64 + footer.count)
        XCTAssertEqual(channel.openCount, 1)
    }
    @MainActor
    func testConsumerConnectPrintKeepsTypedConnectionAndOriginalDataSentResults() async throws {
        let channel = QuickStartMemoryChannel(), (model, _) = consumer(channel); defer { model.close() }
        model.connect(); try await until { model.canPrint }
        XCTAssertEqual(model.lastAction, .connect); XCTAssertEqual(model.connectionResult?.code, 0)
        let connectionID = model.connectionResult?.requestID
        XCTAssertNil(model.lastResult)
        XCTAssertGreaterThan(channel.connectionTimeout, 9); XCTAssertLessThanOrEqual(channel.connectionTimeout, 10)
        model.printSample(); try await until { model.canPrint }
        XCTAssertEqual(model.lastAction, .print); XCTAssertEqual(model.lastResult?.outcome, .success)
        XCTAssertEqual(model.lastResult?.completion, .dataSent); XCTAssertEqual(model.lastResult?.outputMayHaveOccurred, true)
        XCTAssertEqual(model.connectionResult?.requestID, connectionID)
        XCTAssertEqual(channel.openCount, 1)
    }
    @MainActor
    func testConsumerSendingCancelKeepsOutputEvidenceAndNeverReplays() async throws {
        let channel = QuickStartMemoryChannel(), (model, _) = consumer(channel); defer { model.close() }
        model.connect(); try await until { model.canPrint }
        channel.configure(holdWriteAfter: 3); model.printSample()
        try await until { channel.blockedWrite.wait(timeout: .now()) == .success }
        model.cancel(); try await until { model.lastAction == .print }
        XCTAssertEqual(model.lastResult?.outcome, .cancelled); XCTAssertEqual(model.lastResult?.code, 1005)
        XCTAssertEqual(model.lastResult?.completion, CompletionLevel.none)
        XCTAssertEqual(model.lastResult?.outputMayHaveOccurred, true); XCTAssertEqual(channel.sent.count, 3)
        let terminalID = model.lastResult?.requestID
        model.disconnect(); try await until { model.canConnect }
        XCTAssertEqual(model.lastResult?.requestID, terminalID); XCTAssertEqual(model.cleanupResult?.code, 0)
        channel.configure(); model.connect(); try await until { model.canPrint }
        XCTAssertEqual(channel.sent.count, 3); XCTAssertEqual(channel.openCount, 2)
    }
    @MainActor
    func testConsumerDisconnectCannotHidePriorTerminalBeforeManualReconnect() async throws {
        let channel = QuickStartMemoryChannel(), (model, _) = consumer(channel); defer { model.close() }
        for round in 0..<8 {
            model.connect(); try await until { model.canPrint }
            let before = channel.sent.count
            channel.configure(holdWriteAfter: 3); model.printSample()
            try await until { channel.blockedWrite.wait(timeout: .now()) == .success }
            model.disconnect(); model.connect()
            try await until {
                let terminal = model.lastAction == .print && (model.lastResult?.code == 1005 || model.lastResult?.code == 2002)
                if model.canConnect { XCTAssertTrue(terminal, "Consume the real terminal before allowing another connection") }
                else if model.phase == .offline && !terminal { model.connect(); XCTAssertEqual(model.phase, .offline) }
                return model.canConnect
            }
            XCTAssertEqual(model.cleanupResult?.code, 0); XCTAssertEqual(model.lastResult?.outputMayHaveOccurred, true)
            XCTAssertNotEqual(model.cleanupResult?.requestID, model.lastResult?.requestID)
            XCTAssertEqual(channel.sent.count, before + 3); XCTAssertEqual(channel.openCount, round + 1)
            channel.configure()
        }
    }
    @MainActor
    func testConsumerConnectCancellationWaitsForCleanupThenAllowsManualRetry() async throws {
        let channel = QuickStartMemoryChannel(), (model, _) = consumer(channel); defer { model.close() }
        channel.configure(holdOpen: true); model.connect()
        try await until { channel.opening.wait(timeout: .now()) == .success }
        model.cancel(); model.connect(); try await until { model.canConnect }
        XCTAssertEqual(model.lastAction, .connect); XCTAssertEqual(model.connectionResult?.outcome, .cancelled)
        XCTAssertEqual(model.connectionResult?.code, 1005); XCTAssertEqual(model.cleanupResult?.code, 0)
        XCTAssertEqual(channel.openCount, 1); XCTAssertTrue(channel.sent.isEmpty)
        channel.configure()
        let other = DiscoveredPrinter(identifier: UUID(), name: "LT-M90_C3D4", advertisedServices: [], rssi: -50)
        model.devices = [other]; model.selected = other.identifier; model.connect(); try await until { model.canPrint }
        XCTAssertEqual(channel.openCount, 2); XCTAssertTrue(channel.sent.isEmpty)
    }
    @MainActor
    func testConsumerLossRetainsOriginalPrintTerminalAndSessionBoundDiagnostic() async throws {
        let channel = QuickStartMemoryChannel(), (model, _) = consumer(channel); defer { model.close() }
        model.connect(); try await until { model.canPrint }; let connectionID = model.connectionResult?.requestID
        model.printSample(); try await until { model.canPrint }; let printID = model.lastResult?.requestID
        let written = channel.sent
        channel.lose(); try await until { model.phase == .offline }
        XCTAssertEqual(model.disconnectionResult?.requestID, connectionID); XCTAssertEqual(model.disconnectionResult?.code, 2002)
        XCTAssertEqual(model.disconnectionResult?.outcome, .failure)
        XCTAssertEqual(model.lastResult?.requestID, printID); XCTAssertEqual(model.lastResult?.completion, .dataSent)
        model.connect(); try await until { model.canPrint }
        XCTAssertEqual(channel.openCount, 2); XCTAssertEqual(channel.sent, written)
    }
    @MainActor
    func testConsumerScanCleanupKeepsTypedCancellationCandidatesAndIgnoresLateDeadline() async throws {
        let channel = QuickStartMemoryChannel(), scan = QuickStartScanFixture()
        let manager = PrinterManager(makePrinter: { _, _ in try MobilePrinter(channel: channel) }, makeScanner: { scan.scanner() })
        let model = QuickStartModel(manager: manager); defer { model.close() }
        let first = DiscoveredPrinter(identifier: UUID(uuidString: "11111111-0000-0000-0000-000000000000")!,
            name: "LT-M90_A1B2", advertisedServices: ["FF00"], rssi: -50)
        let other = DiscoveredPrinter(identifier: UUID(uuidString: "22222222-0000-0000-0000-000000000000")!,
            name: "LT-M90_A1B2", advertisedServices: [], rssi: 127)
        XCTAssertNotEqual(QuickStartModel.deviceTitle(first), QuickStartModel.deviceTitle(other))
        model.scan(); try await until { scan.count == 1 }; scan.complete(with: first)
        try await until { !model.scanning }; model.selected = first.identifier
        XCTAssertEqual(model.scanResult?.value?.first?.identifier, first.identifier)
        model.scan(); try await until { scan.count == 2 }; model.disconnect()
        try await until { model.canConnect }
        XCTAssertEqual(model.scanResult?.outcome, .cancelled); XCTAssertEqual(model.scanResult?.code, 1005)
        XCTAssertEqual(model.cleanupResult?.code, 0); XCTAssertNil(model.lastResult)
        XCTAssertEqual(model.devices.map(\.identifier), [first.identifier]); XCTAssertEqual(model.selected, first.identifier)
        let cancelledID = model.scanResult?.requestID
        scan.expire(0); scan.expire(1)
        for _ in 0..<5 { await Task.yield() }
        XCTAssertEqual(model.scanResult?.requestID, cancelledID); XCTAssertEqual(scan.count, 2)
        XCTAssertEqual(channel.openCount, 0)
    }
    @MainActor
    func testConsumerCloseDoesNotReactivateFromAnOutstandingPrintCallback() async throws {
        let channel = QuickStartMemoryChannel(), (model, manager) = consumer(channel)
        model.connect(); try await until { model.canPrint }; let priorID = model.connectionResult?.requestID
        channel.configure(holdWriteAfter: 3); model.printSample()
        try await until { channel.blockedWrite.wait(timeout: .now()) == .success }
        model.close()
        await withCheckedContinuation { continuation in manager.close { _ in continuation.resume() } }
        for _ in 0..<5 { await Task.yield() }
        model.connect(); model.printSample()
        XCTAssertEqual(model.phase, .closed); XCTAssertFalse(model.canConnect); XCTAssertFalse(model.canPrint)
        XCTAssertEqual(model.connectionResult?.requestID, priorID); XCTAssertNil(model.lastResult); XCTAssertFalse(channel.isOpen)
        XCTAssertEqual(channel.openCount, 1); XCTAssertEqual(channel.sent.count, 3)
    }
}
