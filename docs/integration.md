# Android/iOS App 接入

[返回指南](README.md) · 适用发布候选 `0.3.0-preview.2` · native `0.3.0` · ABI `3/r1`

Android 最低 API 21、Java 8，在依赖仓库启用 `google()` 和 `mavenCentral()`，应用模块声明
`implementation("com.netumscan:printer-sdk-android:0.3.0-preview.2")`。iOS 最低 iOS 15、Swift 5.9，
使用 `.package(url: "https://github.com/netumscan/printer-sdk-ios", exact: "0.3.0-preview.2")` 并选择
`PrinterSDK` product；Info.plist 配置 `NSBluetoothAlwaysUsageDescription`。发布前使用本地
AAR/Swift Package 验证，精确公开坐标只有在对应发布完成后可用，不用其他版本制品替代。

App 负责授权、选择设备及必要的配对。BLE 的服务/特征/流控和打印 Profile 必须来自已确认
配置，不从设备名推断。连接只建立通信；每次打印另外传入协议、DPI、宽度和纸张参数。

以下完整代码直接取自 SDK 仓库中接受持续编译检查的 `LabelSession.java` 和
`AppPrinter.swift`，避免文档另维护一份示例。Android 与当前 Java SDK 一起按 Java 8 编译；
iOS 与新编译的 SDK module 一起按 iOS 15、Swift 5/6 类型检查。编译验证不表示真机打印通过。

## Android 会话

将下面代码保存为 App 源码 `LabelSession.java`，按项目添加 package。实例独占 Manager/设备，
演示一个在途任务；SDK 自身支持排队。构造和公开入口从主线程调用，图像准备/提交在后台。
构造时传入应用 Context、所选蓝牙地址、已确认 CpclProfile 和 Ui；BleOptions 为 null 选择
SPP，否则选择 BLE。`Ui.connection(null)` 表示连接调用成功，之后调用
`print(gray8, width, height, copies, timeoutMs)`；状态和终态由 Ui 方法接收。

```java
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.netumscan.printersdk.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Main-thread owner: one in-flight task, an exclusive manager/device, caller-supplied profile.
 * null BleOptions selects SPP. Bluetooth permissions and endpoint selection belong to the App. */
public final class LabelSession implements AutoCloseable {
    public interface Ui {
        void connection(Throwable failure);
        void state(DeviceStateSnapshot state);
        void completed(PrintOperation.Result result);
        void failed(Throwable failure);
    }
    private final PrinterManager manager = new PrinterManager();
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final AtomicBoolean stopped = new AtomicBoolean();
    private final MobilePrinter printer;
    private final CpclProfile profile;
    private volatile Ui ui; // May also be cleared if the main looper stops accepting work.
    private Subscription connection, state, completion; // Remaining mutable state is main-thread-only.
    private PrintOperation active;
    private boolean ready, submitting, cancelRequested;
    private long generation;

    public LabelSession(Context context, String address, BleOptions ble,
                        CpclProfile profile, int connectTimeoutMs, Ui ui) {
        requireMain(); this.profile = profile; this.ui = ui;
        try {
            if (profile == null || ui == null) throw new IllegalArgumentException("Profile and Ui are required");
            printer = ble == null ? manager.spp(context, address) : manager.ble(context, address, ble);
            state = printer.observeState(this::onState);
            connection = manager.connect(printer, connectTimeoutMs, this::onConnected);
        } catch (RuntimeException | Error failure) {
            try { close(); } catch (RuntimeException | Error cleanup) { failure.addSuppressed(cleanup); }
            throw failure;
        }
    }
    private void onState(DeviceStateSnapshot value) {
        if (stopped.get() || value.connectionGeneration < generation) return;
        generation = value.connectionGeneration;
        if (value.connectionState != MobilePrinter.CONNECTED) ready = false;
        Ui target = ui; if (target != null) target.state(value);
    }
    private void onConnected(Throwable failure) {
        if (stopped.get()) return;
        ready = false;
        DeviceStateSnapshot current = null;
        if (failure == null) {
            try { current = printer.stateSnapshot(); } // Local observation; no printer IO.
            catch (RuntimeException unavailable) { failure = unavailable; }
        }
        if (current != null) {
            generation = current.connectionGeneration;
            ready = current.connectionState == MobilePrinter.CONNECTED;
            if (!ready) failure = new PrinterException(2002); // The connection was lost before callback delivery.
            Ui target = ui; if (target != null) target.state(current);
        }
        Ui target = ui; if (target != null) target.connection(failure);
    }
    /** Transfer gray8 as read-only: do not modify it after this call. */
    public void print(byte[] gray8, int width, int height, int copies, int timeoutMs) {
        requireMain();
        if (stopped.get() || !ready || submitting || active != null)
            throw new IllegalStateException("Session not connected or already has work");
        submitting = true; cancelRequested = false;
        try {
            worker.execute(() -> {
                if (stopped.get()) return;
                try {
                    PrintOperation next = printer.submitLabelGray8(gray8, width, height, profile,
                        copies, 0, profile.xOffsetDots, 0, profile.form, timeoutMs);
                    boolean posted = false;
                    try { if (!stopped.get()) posted = main.post(() -> publish(next)); }
                    finally { if (!posted) { next.close(); stopBackground(); } }
                } catch (RuntimeException failure) {
                    if (!main.post(() -> { submitting = false; report(failure); })) stopBackground();
                }
            });
        } catch (RuntimeException failure) { submitting = false; report(failure); }
    }
    private void publish(PrintOperation next) {
        submitting = false;
        if (stopped.get()) { next.close(); return; }
        active = next;
        try {
            completion = next.onComplete(result -> {
                if (active != next) return;
                releaseActive(); Ui target = ui; if (target != null) target.completed(result);
            });
            if (cancelRequested) next.cancel();
        } catch (RuntimeException failure) {
            try { next.cancel(); } catch (RuntimeException error) { failure.addSuppressed(error); }
            releaseActive(); report(failure);
        }
    }
    public void cancel() {
        requireMain(); if (stopped.get()) return;
        cancelRequested = true; // Also applied if submit has started but has not published its operation.
        if (active != null) active.cancel(); // A cancellation request cannot withdraw accepted bytes.
    }
    private void report(Throwable failure) { Ui target = ui; if (target != null) target.failed(failure); }
    private void releaseActive() {
        if (completion != null) { completion.close(); completion = null; }
        if (active != null) { active.close(); active = null; }
    }
    private void stopBackground() {
        stopped.set(true); ui = null; manager.close(); worker.shutdown();
    }
    @Override public void close() {
        requireMain(); if (!stopped.compareAndSet(false, true)) return;
        ready = false; ui = null;
        if (connection != null) { connection.close(); connection = null; }
        if (state != null) { state.close(); state = null; }
        releaseActive(); // Unsubscribe/release; manager shutdown cancels remaining device work.
        manager.close(failure -> { if (failure != null) Log.e("PrinterApp", "Printer cleanup failed"); });
        worker.shutdown(); // No get/await/join on the main thread; late submit closes its own operation.
    }
    private static void requireMain() {
        if (Looper.myLooper() != Looper.getMainLooper()) throw new IllegalStateException("Call on the main thread");
    }
}
```

像素须是紧密排列的 GRAY8 原始数据，每像素一字节，长度等于 width*height；PNG/JPEG
须先解码。将数组作为只读输入交给会话，不在后台复制期间修改。取消请求不能撤回已发送
字节。页面离开时调用 close，它注销页面通知并异步关闭独占 Manager，不在主线程等待 Future。

## iOS 会话

将下面代码保存为 App 源码 `AppPrinter.swift`，由 ViewModel 长期持有。MainActor 上设置
showState/showResult，再 await connect 和 printGray8。await printGray8 只等待准备/接纳，
终态由 showResult 交付。新版 SDK 和示例均接受 Swift 5/6 严格并发检查，直接
`import PrinterSDK`；跨队列回调为 `@Sendable`，UI 更新显式切回 MainActor。

```swift
import Foundation
import PrinterSDK

// App-owned Manager/device, one in-flight print. Retain this owner until close().
@MainActor
final class AppPrinter {
    private let manager: PrinterManager
    private let preparation = DispatchQueue(label: "App.printer.prepare")
    private var device: MobilePrinter?
    private var operation: PrintOperation?
    private var stateToken: ObservationSubscription?
    private var completionToken: ObservationSubscription?
    private var epoch: UInt64 = 0
    private var connectionGeneration: UInt64 = 0
    private var connected = false, submitting = false, cancelPending = false, closed = false
    var showState: (DeviceStateSnapshot) -> Void = { _ in }
    var showResult: (OperationResult) -> Void = { _ in }
    init(manager: PrinterManager = PrinterManager()) { self.manager = manager }
    func connect(identifier: UUID, options: BLEOptions) async throws {
        guard !closed, device == nil else { throw PrinterSDKError(1002) }
        epoch &+= 1; let version = epoch
        connectionGeneration = 0; connected = false
        let printer = try manager.printer(bleIdentifier: identifier, options: options)
        device = printer
        do {
            stateToken = try printer.observeState { [weak self] value in
                Task { @MainActor [weak self] in
                    guard let self, self.epoch == version,
                          value.connectionGeneration >= self.connectionGeneration else { return }
                    self.connectionGeneration = value.connectionGeneration
                    if value.connectionState != .connected { self.connected = false }
                    self.showState(value)
                }
            }
            try await manager.connect(printer)
            guard epoch == version, device === printer else { throw CancellationError() }
            let value = try printer.stateSnapshot() // Local snapshot, no printer IO.
            guard value.connectionState == .connected else { throw PrinterSDKError(2002) }
            connectionGeneration = value.connectionGeneration; connected = true
            showState(value)
        } catch {
            if epoch == version { await disconnect() }
            throw error
        }
    }
    func printGray8(_ pixels: Data, width: UInt32, height: UInt32, profile: CpclProfile) async throws {
        guard connected, !submitting, operation == nil, let printer = device else { throw PrinterSDKError(1002) }
        let version = epoch; submitting = true; cancelPending = false
        defer { if epoch == version { submitting = false } }
        let op = try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<PrintOperation, Error>) in
            preparation.async {
                // Decode/render images here too, then submit tightly packed GRAY8.
                do { continuation.resume(returning: try printer.submitGray8(pixels, width: width, height: height, profile: profile)) }
                catch { continuation.resume(throwing: error) }
            }
        }
        guard epoch == version, device === printer else {
            try? op.cancel(); op.close(); throw CancellationError()
        }
        operation = op
        do {
            completionToken = try op.onComplete { [weak self] result in
                Task { @MainActor [weak self] in
                    guard let self, self.epoch == version, self.operation?.id == result.snapshot.id else { return }
                    self.completionToken?.cancel(); self.completionToken = nil
                    self.operation?.close(); self.operation = nil
                    self.showResult(result)
                }
            }
            if cancelPending { try op.cancel() }
        } catch {
            completionToken?.cancel(); completionToken = nil
            try? op.cancel(); op.close(); operation = nil
            throw error
        }
    }

    func cancelPrint() {
        if let operation { try? operation.cancel() }
        else if submitting { cancelPending = true }
    }

    func disconnect() async {
        epoch &+= 1
        let printer = device; connected = false
        submitting = false; cancelPending = false
        stateToken?.cancel(); stateToken = nil
        completionToken?.cancel(); completionToken = nil // Notification only.
        try? operation?.cancel(); operation?.close(); operation = nil
        if let printer {
            await manager.release(printer)
            if device === printer { device = nil }
        }
    }
    func close() async {
        closed = true; await disconnect(); await manager.close() // Permanently closed.
    }
}
```

像素同样是 GRAY8 原始 Data。点击取消调用 cancelPrint；断开某设备 await disconnect，
结束独占会话 await close。共享设备应由应用级所有者管理，页面退出仅注销自己的订阅。
Swift Task 取消不会自动关闭共享设备。

应用级共享所有者和页面订阅示例随同版
[Samples](https://github.com/netumscan/printer-sdk-samples) 提供。页面退出只注销通知，不默认
取消已接纳工作或关闭共享设备；进入后台的断开策略、重试和取消均由应用显式发起。
