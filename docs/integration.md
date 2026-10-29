# Android／iOS App 接入

[返回指南](README.md) · 适用版本 `0.3.0-preview.17` · native `0.3.0` · ABI `3/r3`

Android 最低 API21、Java8；iOS 最低 iOS15，支持 Swift5／6。实际发布后使用精确同版 Maven／SwiftPM 依赖，
核对制品摘要和实际身份；候选契约不表示线上已发布。App 负责权限、设备选择、系统配对和后台策略，
iOS 配置 `NSBluetoothAlwaysUsageDescription`。

稳定 owner 在提交请求前注册统一 Listener／Delegate，操作只返回请求 ID；按动作接收完整结果并关联 ID。
扫描时将实时发现直接展示，成功终态刷新最终合并信息；停止后再显式连接，发现和选择不会自动连接或打印。
设备原名可推荐内置型号，已确认型号的改名设备可显式指定；未知型号不猜通信参数。

以下会话源码与当前 SDK 进行 Java8、iOS15 Swift5／6 严格编译检查。页面只观察 owner 的结果，
编译通过不证明真实无线或纸面验收。

## Android

`LabelSession` 实现 `PrinterListener`，在连接前调用 `manager.setListener(this)`。
构造时提供 Context、候选设备、是否 BLE 与 Ui；显式型号重载仅用于已确认型号的设备。
图片直接传 Bitmap 或完整 PNG/JPEG byte[] 和页面毫米宽高，不传逐调用 Callback。

```java
import android.content.Context;
import android.graphics.Bitmap;
import com.netumscan.printersdk.*;

/** Application notification target; the SDK owns image processing and all printer IO. */
public final class LabelSession implements AutoCloseable, PrinterListener {
    public interface Ui {
        void connection(ManagerResult<ConnectionInfo> result);
        void configuration(ManagerResult<ConnectionInfo> result);
        void media(ManagerResult<Void> result);
        void disconnected(ManagerResult<Void> result);
        void completed(ManagerResult<Void> result);
    }
    private final PrinterManager manager;
    private volatile Ui ui;
    private RequestId active;
    public LabelSession(Context context, DiscoveredDevice device, boolean lowEnergy, Ui ui) {
        this(context, device, lowEnergy, null, ui);
    }
    /** Explicit model selection also supports renamed and saved address-only devices. */
    public LabelSession(Context context, DiscoveredDevice device, boolean lowEnergy, PrinterModel model, Ui ui) {
        this(new PrinterManager(context), device, lowEnergy, model, ui);
    }
    LabelSession(PrinterManager manager, DiscoveredDevice device, boolean lowEnergy, PrinterModel model, Ui ui) {
        this.manager = manager; this.ui = ui; manager.setListener(this);
        if (model != null) {
            if (lowEnergy) manager.connectBle(device, model); else manager.connectSpp(device, model);
        } else if (lowEnergy) manager.connectBle(device); else manager.connectSpp(device);
    }
    @Override public void onConnectionCompleted(ManagerResult<ConnectionInfo> result) {
        Ui target = ui; if (target != null) target.connection(result);
    }
    @Override public void onModelConfigurationCompleted(ManagerResult<ConnectionInfo> result) {
        Ui target = ui; if (target != null) target.configuration(result);
    }
    @Override public void onPaperTypeCompleted(ManagerResult<Void> result) {
        Ui target = ui; if (target != null) target.media(result);
    }
    @Override public void onDisconnected(ManagerResult<Void> result) {
        Ui target = ui; if (target != null) target.disconnected(result);
    }
    @Override public void onPrintCompleted(ManagerResult<Void> result) { completed(result); }
    @Override public void onLabelPositioningCompleted(ManagerResult<Void> result) { completed(result); }
    public void setPrinterModel(PrinterModel model) { manager.setPrinterModel(model); }
    public void setPaperType(PaperType type) { manager.setPaperType(type); }
    public void printImage(Bitmap image, double widthMm, double heightMm) {
        active = manager.printImage(image, widthMm, heightMm);
    }
    public void printEncodedImage(byte[] pngJpeg, double widthMm, double heightMm) {
        active = manager.printEncodedImage(pngJpeg, widthMm, heightMm);
    }
    private void completed(ManagerResult<Void> result) {
        Ui target = ui; if (target != null) target.completed(result);
    }
    public void locateNextLabel() { active = manager.locateNextLabel(); }
    public void cancel() { manager.cancel(active); }
    @Override public void close() { ui = null; manager.close(); }
}
```

## iOS

由 MainActor owner 持有 `AppPrinter`。它实现 `PrinterDelegate` 并设置 `manager.delegate = self`；
SDK 弱持有 Delegate，App 必须保持 owner 存活到关闭清理终态。
connect 提供 `DiscoveredPrinter` 和可选型号；printEncodedImage 提供完整 PNG/JPEG Data 和页面毫米。
示例 App owner 可向自己的 UI 提供闭包，SDK 公共请求入口本身不再接收 completion 或断连闭包。

```swift
import Foundation
import XLPrinterSDK

// An App/ViewModel owner. The Manager owns snapshots, processing, printer IO and cleanup.
@MainActor
final class AppPrinter: PrinterDelegate {
    enum LifecycleError: Error { case closed }
    private let manager: PrinterManager
    private var requests: Set<RequestID> = []
    private var cleanup: [RequestID: @MainActor @Sendable (ManagerResult<Void>) -> Void] = [:]
    private var closeRequest: RequestID?
    private var closeResult: ManagerResult<Void>?
    private var closeWaiters: [@MainActor @Sendable (ManagerResult<Void>) -> Void] = []
    private var closingOwner: AppPrinter?
    var showConnection: (ManagerResult<ConnectionInfo>) -> Void = { _ in }
    var showResult: (ManagerResult<Void>) -> Void = { _ in }
    var disconnected: (ManagerResult<Void>) -> Void = { _ in }
    init(manager: PrinterManager = PrinterManager()) { self.manager = manager; manager.delegate = self }
    private func requireOpen() throws {
        guard closeRequest == nil, closeResult == nil else { throw LifecycleError.closed }
    }
    @discardableResult
    func connect(_ device: DiscoveredPrinter, model: PrinterModel? = nil) throws -> RequestID {
        try requireOpen()
        if let model { return manager.connectBle(device: device, model: model) }
        return manager.connectBle(device: device)
    }
    func printerManager(_ manager: PrinterManager, didFinishConnecting result: ManagerResult<ConnectionInfo>) {
        showConnection(result)
    }
    func printerManager(_ manager: PrinterManager, didLoseConnection result: ManagerResult<Void>) {
        disconnected(result)
    }
    @discardableResult
    func printEncodedImage(_ pngJpeg: Data, widthMm: Double, heightMm: Double) throws -> RequestID {
        try requireOpen()
        let id = manager.printEncodedImage(pngJpeg, widthMm: widthMm, heightMm: heightMm)
        requests.insert(id); return id
    }
    func printerManager(_ manager: PrinterManager, didFinishPrinting result: ManagerResult<Void>) {
        requests.remove(result.requestID)
        showResult(result) // dataSent confirms transport delivery, not physical paper output.
    }
    func cancel(_ id: RequestID) { manager.cancel(id) }
    func cancelPrint() { for id in requests { manager.cancel(id) } }
    func disconnect(_ completion: @escaping @MainActor @Sendable (ManagerResult<Void>) -> Void = { _ in }) throws {
        try requireOpen()
        cleanup[manager.disconnect()] = completion
    }
    func close(_ completion: @escaping @MainActor @Sendable (ManagerResult<Void>) -> Void = { _ in }) {
        if let closeResult { DispatchQueue.main.async { completion(closeResult) }; return }
        closeWaiters.append(completion)
        if closeRequest == nil { closingOwner = self; closeRequest = manager.close() }
    }
    func printerManager(_ manager: PrinterManager, didFinishDisconnecting result: ManagerResult<Void>) {
        cleanup.removeValue(forKey: result.requestID)?(result)
    }
    func printerManager(_ manager: PrinterManager, didFinishClosing result: ManagerResult<Void>) {
        guard closeRequest == result.requestID else { return }
        closeResult = result; closeRequest = nil
        let waiters = closeWaiters; closeWaiters.removeAll()
        for completion in waiters { completion(result) }
        closingOwner = nil
    }
}
```

Flutter 的标准 MethodChannel 将完整 PNG/JPEG `Uint8List` 映射到 Android `byte[]`／iOS
`FlutterStandardTypedData.data`。原生适配器保存每个请求 ID，将对应类型化通知转为一次插件回复；
detach 或关闭期间隔离迟到结果。无需 Base64、裸 RGBA 或插件重复解码；`ui.Image` 明确导出 PNG。

失败和取消可能已有部分输出，读取 `outputMayHaveOccurred`；`DATA_SENT` 不等于机械完成。
换机等待 disconnect，独占 owner 结束时 close，保持接收者直至关闭终态。返回前台不自动重连或补印。
图片规格与输入副本寿命见[设备与图片](configuration.md)。

SwiftPM 产品和 Swift 模块为 `XLPrinterSDK`。CocoaPods 接入使用同版公开仓库标签：

```ruby
pod 'XLPrinterSDK', :git => 'https://github.com/netumscan/printer-sdk-ios.git', :tag => 'v0.3.0-preview.17'
```

候选契约本身不证明线上发布；远程标签必须实际发布后才能使用。本地 `:path` 接入须先将校验后的原生附件解压到包的 `Binaries/`，详见包根目录 README。
