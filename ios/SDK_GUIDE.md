# iOS XLPrinterSDK 接入

本目录的原生示例和 Flutter iOS 使用已发布的 `0.3.0-preview.16`，Swift 模块与两种包管理器的产品名均为 `XLPrinterSDK`。最低 iOS 15。精确版本、公开 commit 和原生 ZIP SHA-256 见根目录的 [iOS 平台锁](../ios-sdk-lock.json)。Android 与 Flutter Android 继续使用原 `.11`，根历史锁和文档不改写。

## Swift Package Manager

在 Xcode 添加 `https://github.com/netumscan/printer-sdk-ios`，选择 Exact Version `0.3.0-preview.16` 和 `XLPrinterSDK` 产品。Swift Package 清单等价为：

```swift
.package(url: "https://github.com/netumscan/printer-sdk-ios", exact: "0.3.0-preview.16")
.product(name: "XLPrinterSDK", package: "printer-sdk-ios")
```

## CocoaPods

```ruby
platform :ios, '15.0'
use_frameworks!

target 'YourApp' do
  pod 'XLPrinterSDK',
      :git => 'https://github.com/netumscan/printer-sdk-ios.git',
      :tag => 'v0.3.0-preview.16'
end
```

运行 `pod install` 后打开生成的 `.xcworkspace`。此 git/tag 接入不要求中央 Specs 源条目。Pod 安装阶段获取同版原生附件并验证 SHA-256；同一个 App target 选择一种 SDK 包管理器。

两种方式均使用：

```swift
import XLPrinterSDK
```

旧 `.11` 使用 `import PrinterSDK`；升级需要同步更改 import、SwiftPM 产品依赖并重新编译，没有旧模块别名。Swift 包装层与闭源 `CPrinterSDK` 原生框架须使用同一完整版本。当前 iOS native `0.3.0`、C ABI `3/r3`、28 个 C 导出；Android 保留 `.11`／ABI `3/r2`。

## 生命周期与状态

配置 `NSBluetoothAlwaysUsageDescription`。App 所有者持有 `PrinterManager()`，显式扫描、选择 BLE 设备和型号后连接。iOS 不提供经典蓝牙 SPP 或系统已配对设备列表。

`setStatusListener` 在主线程异步交付状态快照，`getStatus` 的请求回调只表示本次主动查询的最终结果。监听可跨显式断开／重连；关闭时解除并释放。未知或未支持状态不能当作正常。Raw、打印与状态查询继续共用设备 FIFO。`DATA_SENT` 仅表示数据交付，不代表机械打印完成；失败后不自动重连、重放或续打。

完整 Swift 调用与签名见 [iOS API 参考](ios-swift-api.md)，示例见 [Quick Start](quick-start/README.md)、[Full Demo](full-demo/README.md)和 [共享 App 所有者](../shared-app/ios/SharedPrinterApp.swift)。本目录 `api-reference.json` 保存同源冻结契约供 Swift 文档校验；其中 Android 契约部分不是 Android 新制品发布声明，Android 客户仍使用根目录 `.11` 文档与锁。

## 软件验证与许可

```sh
python3 tools/check-samples.py
bash ios/verify.sh --output build/ios
```

完整门禁校验精确公开 commit、原生 ZIP、三架构构建、隐私清单、18 项测试与模拟器离线生命周期。原生与 Flutter 两端 CI 继续保留；软件检查不增加无线、纸面、其他固件或手机峰值内存验收，`hardware_verified=false`。

示例为 MIT；下载的 SDK、Swift 包装层与原生附件保持其商用许可，见 [许可边界](../LICENSING.md)。
