# 在线接入与权限

[返回指南](README.md) · 适用 SDK `0.2.0-preview.3`

## Android

运行环境为 Android 8.0 / API 26+，Java 8；AAR 包含 `arm64-v8a`、`armeabi-v7a`、
`x86_64`。构建工具版本以应用工程配置为准；公开示例使用 JDK 17、Gradle 8.13、
Android SDK Platform 35。SDK 提供经典蓝牙 SPP 与 BLE，协议显式选择 CPCL 或 ESC/POS。

在 `settings.gradle.kts` 配置仓库：

```kotlin
dependencyResolutionManagement {
    repositories { google(); mavenCentral() }
}
```

在应用模块的 `build.gradle.kts` 添加精确版本：

```kotlin
dependencies {
    implementation("com.netumscan:printer-sdk-android:0.2.0-preview.3")
}
```

公共 Java 包名为 `com.netumscan.printersdk`。AAR 自带 consumer R8 规则和
`libc++_shared.so`，不要从 AAR 只提取 Java 类。与其他原生 SDK 集成时，应核对共同使用的
C++ 运行库；不要通过盲目 `pickFirst` 掩盖不兼容库。Java/JNI/原生库必须来自同一发布包。

宿主负责声明并请求权限、配对、用户选择和蓝牙状态处理：

| 系统 | 操作与权限 |
| --- | --- |
| Android 12+ | 连接需 `BLUETOOTH_CONNECT`；发现需 `BLUETOOTH_SCAN`。SPP 连接会取消系统发现，因此也需要 `BLUETOOTH_SCAN` |
| Android 8–11 | 声明旧版 `BLUETOOTH` / `BLUETOOTH_ADMIN`；BLE 扫描还需运行时 `ACCESS_FINE_LOCATION`，并满足系统定位设置要求 |

`neverForLocation` 由应用按实际用途决定，SDK 不代为声明该承诺；使用它可能过滤部分 BLE
广播。SDK 不申请权限、不自动配对、不输入 PIN、不打开蓝牙。
`BluetoothDiscovery.bondedDevices()` 仅列出已配对候选，不验证 SPP 或协议支持；
`scanBle(...)` 为有限时长扫描，完成回调在主线程。扫描器停止后可以再次显式启动。

## iOS

运行环境为 iOS 15+、Swift 5.9+。包包含设备 arm64 和模拟器 arm64/x86_64；
公共模块为 `PrinterSDK`，C 模块为 `CPrinterSDK`。当前移动包装通过 BLE 连接，
不提供经典蓝牙 SPP。模拟器用于编译和替身验证；实际 BLE 通信需要真机。

在 Xcode Package Dependencies 添加
`https://github.com/netumscan/printer-sdk-ios`，选择 Exact Version `0.2.0-preview.3`，
将 `PrinterSDK` 产品加入 App target。SwiftPM 声明为：

```swift
.package(url: "https://github.com/netumscan/printer-sdk-ios", exact: "0.2.0-preview.3")
```

App 的 `Info.plist` 必须包含符合自身用途的 `NSBluetoothAlwaysUsageDescription`。
用户授权、蓝牙状态和前后台策略由宿主处理。保留 `BLEScanner`，显式调用
`scan(seconds:service:completion:)`；完成回调在主线程，正常窗口结束可能返回空列表。
使用扫描得到的 `UUID` 创建连接；它是不透明外设标识符，不能使用 Android MAC 地址替代。

## 两端共同的接入顺序

1. 确认设备协议和连接参数，依据实际纸张建立 profile。发现名称不是型号识别依据。
2. 由用户选择端点，在工作线程连接；连接成功只证明通信路径建立。
3. 将图像转换为紧密排列的 GRAY8，显式提交并持有 `PrintOperation`。
4. 读取快照至终态，保存诊断结果后释放 operation；结束会话时关闭打印设备。

两端 SDK 的连接、断开和设备关闭会阻塞。不要在 UI 线程、蓝牙回调队列中调用，
也不要在 UI 线程等待打印完成。完整方法对应关系见 [API 与生命周期](api-lifecycle.md)。

在线依赖失败时先核对精确版本、仓库地址及代理网络，勿回退到未知本地 AAR、源码包或混合
framework。已发布版本修复使用新版本，参见[迁移政策](migration.md)。
