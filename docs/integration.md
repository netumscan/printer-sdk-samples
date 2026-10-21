# Android/iOS App接入

[返回指南](README.md) · 适用版本 `0.3.0-preview.10` · native `0.3.0` · ABI `3/r2`

Android最低API21、Java8；iOS最低iOS15。实际发布后使用同版精确Maven／SwiftPM依赖，核对分发身份与制品摘要；候选或开发契约不表示线上已发布。App负责权限、设备选择和配对，iOS配置NSBluetoothAlwaysUsageDescription。

两端直接接收设备值，已知名称可推荐内置型号；改名的已支持设备可显式指定PrinterModel。未知型号不猜通信参数。连接、选型号和实际设纸型分开；图片打印只提供图片和页面宽高，不自动设置纸张。

以下会话源码与当前SDK进行Java8和iOS15 Swift5/6严格编译检查。owner持有Manager和请求标识，页面只观察结果。编译通过不表示真实无线或纸面验收。

## Android

构造LabelSession时提供应用Context、DiscoveredDevice、是否BLE和Ui。打印支持Bitmap或完整PNG/JPEG byte[]，widthMm／heightMm为本页尺寸。固定一份、不隐式FORM；定位调用独立方法。

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
        manager = new PrinterManager(context); this.ui = ui;
        PrinterManager.Callback<ConnectionInfo> connected = result -> {
            Ui target = this.ui; if (target != null) target.connection(result);
        };
        PrinterManager.DisconnectedListener disconnected = result -> {
            Ui target = this.ui; if (target != null) target.disconnected(result);
        };
        if (lowEnergy) manager.connectBle(device, connected, disconnected);
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

在MainActor由ViewModel持有AppPrinter。connect提供DiscoveredPrinter；printEncodedImage提供完整PNG/JPEG Data与页面毫米。回调直接更新MainActor状态。SDK同时提供UIImage入口。

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
    func connect(_ device: DiscoveredPrinter) -> RequestID {
        manager.connectBle(device: device, completion: { [weak self] in self?.showConnection($0) },
            onDisconnected: { [weak self] in self?.disconnected($0) })
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

UIImage入口支持可取得CGImage的非动画图片；CI-only、矢量／系统符号等无CGImage来源及动画图片返回1001。App可先渲染为CGImage-backed UIImage，或提供PNG/JPEG Data。UIImage源像素快照按bytesPerRow×height限制为16MiB，编码Data上限32MiB；大图优先使用Data，由SDK后台按目标采样。快照复制期间不得并发修改输入，返回后可修改或释放原数据。iOS只有保存UUID、没有有效名称且未显式选择型号时，连接返回1004。
