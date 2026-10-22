# Android/iOS App接入

[返回指南](README.md) · 适用版本 `0.3.0-preview.11` · native `0.3.0` · ABI `3/r2`

Android最低API21、Java8；iOS最低iOS15。实际发布后使用同版精确Maven／SwiftPM依赖，核对分发身份与制品摘要；候选或开发契约不表示线上已发布。App负责权限、设备选择和配对，iOS配置NSBluetoothAlwaysUsageDescription。

两端直接接收设备值，已知名称可推荐内置型号；改名的已支持设备可显式指定PrinterModel。未知型号不猜通信参数。连接、选型号和实际设纸型分开；图片打印只提供图片和页面宽高，不自动设置纸张。

以下会话源码与当前SDK进行Java8和iOS15 Swift5/6严格编译检查。owner持有Manager和请求标识，页面只观察结果。编译通过不表示真实无线或纸面验收。

## Android

构造 LabelSession 时提供应用 Context、DiscoveredDevice、是否 BLE 和 Ui；可选型号构造重载用于已确认型号的改名设备或无名保存地址。打印支持 Bitmap 或完整 PNG/JPEG byte[]，widthMm／heightMm 为本页尺寸。固定一份、不隐式 FORM；定位调用独立方法。

```java
import android.content.Context;
import android.graphics.Bitmap;
import com.netumscan.printersdk.*;

/** Application notification target; the SDK owns image processing and all printer IO. */
public final class LabelSession implements AutoCloseable {
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
        this.manager = manager; this.ui = ui;
        PrinterManager.Callback<ConnectionInfo> connected = result -> {
            Ui target = this.ui; if (target != null) target.connection(result);
        };
        PrinterManager.DisconnectedListener disconnected = result -> {
            Ui target = this.ui; if (target != null) target.disconnected(result);
        };
        if (model != null) {
            if (lowEnergy) manager.connectBle(device, model, connected, disconnected);
            else manager.connectSpp(device, model, connected, disconnected);
        } else if (lowEnergy) manager.connectBle(device, connected, disconnected);
        else manager.connectSpp(device, connected, disconnected);
    }
    public void setPrinterModel(PrinterModel model) {
        manager.setPrinterModel(model, result -> {
            Ui target = ui; if (target != null) target.configuration(result);
        });
    }
    public void setPaperType(PaperType type) {
        manager.setPaperType(type, result -> {
            Ui target = ui; if (target != null) target.media(result);
        });
    }
    public void printImage(Bitmap image, double widthMm, double heightMm) {
        active = manager.printImage(image, widthMm, heightMm, this::completed);
    }
    public void printEncodedImage(byte[] pngJpeg, double widthMm, double heightMm) {
        active = manager.printEncodedImage(pngJpeg, widthMm, heightMm, this::completed);
    }
    private void completed(ManagerResult<Void> result) {
        Ui target = ui; if (target != null) target.completed(result);
    }
    public void locateNextLabel() { active = manager.locateNextLabel(this::completed); }
    public void cancel() { manager.cancel(active); }
    @Override public void close() { ui = null; manager.close(result -> { }); }
}
```

## iOS

在 MainActor 由 ViewModel 持有 AppPrinter。connect 提供 DiscoveredPrinter，可选 model 用于改名设备或只有 UUID 的目标；不传时仍按名称推荐。printEncodedImage 提供完整 PNG/JPEG Data 与页面毫米。回调直接更新 MainActor 状态。SDK 同时提供 UIImage 入口。

```swift
import Foundation
import PrinterSDK

// An App/ViewModel owner. The Manager owns snapshots, processing, printer IO and cleanup.
@MainActor
final class AppPrinter {
    private let manager: PrinterManager
    private var requests: Set<RequestID> = []
    var showConnection: (ManagerResult<ConnectionInfo>) -> Void = { _ in }
    var showResult: (ManagerResult<Void>) -> Void = { _ in }
    var disconnected: (ManagerResult<Void>) -> Void = { _ in }
    init(manager: PrinterManager = PrinterManager()) { self.manager = manager }
    @discardableResult
    func connect(_ device: DiscoveredPrinter, model: PrinterModel? = nil) -> RequestID {
        let completed: @MainActor @Sendable (ManagerResult<ConnectionInfo>) -> Void = { [weak self] in self?.showConnection($0) }
        let disconnected: @MainActor @Sendable (ManagerResult<Void>) -> Void = { [weak self] in self?.disconnected($0) }
        if let model {
            return manager.connectBle(device: device, model: model, completion: completed, onDisconnected: disconnected)
        }
        return manager.connectBle(device: device, completion: completed, onDisconnected: disconnected)
    }
    @discardableResult
    func printEncodedImage(_ pngJpeg: Data, widthMm: Double, heightMm: Double) -> RequestID {
        let id = manager.printEncodedImage(pngJpeg, widthMm: widthMm, heightMm: heightMm) { [weak self] result in
            guard let self else { return }
            requests.remove(result.requestID)
            showResult(result) // dataSent confirms transport delivery, not physical paper output.
        }
        requests.insert(id); return id
    }
    func cancel(_ id: RequestID) { manager.cancel(id) }
    func cancelPrint() { for id in requests { manager.cancel(id) } }
    func disconnect(_ completion: @escaping @MainActor @Sendable (ManagerResult<Void>) -> Void = { _ in }) {
        manager.disconnect(completion: completion)
    }
    func close(_ completion: @escaping @MainActor @Sendable (ManagerResult<Void>) -> Void = { _ in }) {
        manager.close(completion: completion)
    }
}
```

Flutter通过MethodChannel传Uint8List，Android接byte[]，iOS接FlutterStandardTypedData后取data，直接调用printEncodedImage。不要在插件解码再重新编码，也不传rawRgba。ui.Image.toByteData需要明确format: ImageByteFormat.png，并保留ByteData的offset／length范围。

取消、失败和断开可能已有部分输出，读取outputMayHaveOccurred；DATA_SENT不等于机械完成。disconnect完成后才能正常换机，close永久停止独占会话。共享所有者的页面解绑不关闭设备；恢复前台不重连或补印。

UIImage 入口支持可取得 CGImage 的非动画图片；CI-only、矢量／系统符号等无 CGImage 来源及动画图片返回 1001。检查和像素读取在后台执行。源像素 bytesPerRow×height 上限 16 MiB，接纳时按此上限保守预留输入额度；编码 Data 上限 32 MiB，大图优先使用 Data 按目标采样。SDK 强持有不可变 UIImage，返回后可释放或重新赋值调用方引用；自定义 CGDataProvider 的底层数据须在 provider 生命周期内保持不变。Bitmap、byte[]／Data 仍在返回前复制，复制期间不得并发修改，返回后可修改或释放原数据。iOS 只有保存 UUID、没有有效名称且未显式选择型号时，连接返回 1004。
