# Flutter Quick Start

NETUM Printer SDK `0.3.0-preview.10` 的 Android／iOS 快速示例。Android 精确依赖 Maven Central，iOS 精确依赖公开 Swift Package；克隆本示例仓库即可构建，无需 SDK 私有源码。

## 运行

需要 Flutter `3.41.9`（Dart `3.11.5`）或兼容版本。示例最低 Android 7.0／API24、iOS15；SDK 本身的 Android API21 支持不改变 Flutter 的最低系统要求。Android 使用 JDK17、SDK Platform36；iOS 使用 Xcode16+。依赖首次解析需要网络，已完整缓存后可使用 `flutter pub get --offline`。

```sh
cd flutter
flutter pub get --enforce-lockfile
flutter devices
flutter run -d <设备ID>
```

iOS 真机运行时在 Xcode 中为 Runner 选择自己的签名团队；仓库不含个人签名设置。Android 构建工具由 Flutter 工程的 Gradle 配置决定，无需下载 SDK 实现源码或运行 SDK 的 CMake 构建。

1. 打开打印机和手机蓝牙。Android 可选 BLE 或 SPP；SPP 先在系统设置配对。iOS 仅支持 BLE。
2. 扫描或读取已配对设备，明确点选目标后连接。按提示授予蓝牙权限；Android11及以下 BLE 扫描还需要位置权限与系统位置服务。
3. 默认按设备名称推荐内置型号；改名设备须确认实物后显式选择 `LT-M90`，未知型号不会默认按 M90 处理。
4. 按实际装纸选择类型，点击“设置纸型”并等待结果。填写页面宽高（mm），点击“打印测试标签”。“定位下一张”是独立操作，会移动纸张。
5. 可取消当前请求或断开。`DATA_SENT` 仅表示传输交付，不表示纸面打印完成；取消或失败也可能已有部分输出，查看 `outputMayHaveOccurred` 和请求日志。

界面显示内置测试图的来源预览；图片导入可由业务 App 自行添加。系统权限弹窗导致的 `inactive` 不作为后台；真正进入后台会取消并断开，返回前台不自动连接或补印。清理结果与打印终态分开保存。owner 结束或 engine detach 时关闭 Manager。

## 接入自己的页面

`lib/printer_bridge.dart` 封装通道，`lib/demo_controller.dart` 管理页面状态。每个 engine 的原生适配层持有独立 `PrinterManager`，SDK RequestId 和平台句柄不出原生层。普通方法在 SDK 最终回调时回复一次；取消仅确认控制信号，原请求仍需等待终态。

```dart
final bridge = PrinterBridge(); // 由 App owner 创建并持有
// 先通过 scanBle / bondedDevices 选择设备，再 connect。
final request = bridge.start('printEncodedImage', {
  'imageBytes': pngOrJpegBytes, // 包含完整 PNG/JPEG 文件的 Uint8List
  'widthMm': 50.0,
  'heightMm': 50.0,
});
// 用户取消时：await bridge.cancel(request.id);
final result = await request.result;
// 保留 code、completion、outputMayHaveOccurred，处理 result.isSuccess。
// owner 最终结束时：await bridge.release();
```

从 Flutter `ui.Image` 导出时明确指定 `ui.ImageByteFormat.png`，按 `ByteData.offsetInBytes/lengthInBytes` 取有效字节。默认 `rawRgba` 不能传入此入口。Android 收到 `byte[]`、iOS 收到 `FlutterStandardTypedData.data` 后直接调用 `printEncodedImage`；适配层不重复解码或二值化，也不创建第二条打印机发送队列。

结果保留应用 token `requestId`、`outcome`、整数 `code`、`message`、`completion`、`outputMayHaveOccurred`、`value`。Dart 的 `code=-1` 专用于适配层或通道错误，消息保留平台错误标识，不冒充 SDK 错误码。

## 精确 SDK 依赖与许可

[独立锁](sdk-lock.json) 固定 Maven 坐标、AAR SHA-256、SwiftPM exact version／revision、XCFramework SHA-256，并与根 [sdk-lock.json](../sdk-lock.json) 一致。示例代码与资源采用 [MIT](LICENSE)；下载的 SDK 保持其原许可，见 [许可边界](../LICENSING.md)。

本目录的工程和桥接源码由开发仓库规范 Flutter demo 导出，文件摘要见根 `samples-manifest.json` 的 `flutter_demo`。不要把缓存、已下载 SDK、APK、framework、个人路径或签名文件提交进示例仓库。

## 验证

```sh
flutter analyze --no-pub
flutter test --no-pub
flutter build apk --debug --no-pub
flutter build ios --simulator --debug --no-codesign --no-pub
```

从仓库根目录检查 APK 对精确发行包的消费：

```sh
python3 tools/check-flutter-android.py --apk flutter/build/app/outputs/flutter-apk/app-debug.apk
```

本机 Flutter3.41.9 的 generic Simulator 双架构 prepare 脚本需要固定架构；Apple Silicon 上可从仓库根目录使用：

```sh
xcodebuild -project flutter/ios/Runner.xcodeproj -scheme Runner \
  -configuration Debug -sdk iphonesimulator \
  -destination 'generic/platform=iOS Simulator' -derivedDataPath build/flutter-ios \
  ARCHS=arm64 ONLY_ACTIVE_ARCH=YES CODE_SIGNING_ALLOWED=NO build
```

18 项 Flutter 测试覆盖参数、typed bytes、取消终态、后台竞态、无自动重连／重播、非法尺寸和窄屏布局；5 项 iOS RunnerTests 覆盖原生桥接边界及真实 engine detach。CI 入口见 [验证说明](../VERIFICATION.md)。软件检查不替代实体蓝牙、纸面、固件或手机峰值内存验收，`hardware_verified=false`。

更多说明见 [SDK 接入](../docs/integration.md)、[API 与生命周期](../docs/api-lifecycle.md)、[设备与纸张](../docs/configuration.md)。
