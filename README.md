# printer-sdk-samples

NETUM 打印机 SDK 的 MIT 许可应用示例。Android、iOS 与 Flutter 均固定接入在线 SDK **0.3.0-preview.16**。克隆本仓库即可构建，无需访问 SDK 私有开发仓库。

| 平台 | 最小接入 | 完整示例 |
| --- | --- | --- |
| Flutter（Android／iOS） | [Quick Start](flutter/README.md) | 扫描、连接、测试标签、纸型、取消与断开 |
| Android | [Quick Start](android/quick-start/README.md) | [Full Demo](android/full-demo/README.md) |
| iOS | [Quick Start](ios/quick-start/README.md) | [Full Demo](ios/full-demo/README.md) |

Quick Start 展示设备发现、显式选择、连接、图片打印与清理。Flutter 使用标准通道传完整 PNG/JPEG `Uint8List`，不向 Dart 暴露原生句柄。Full Demo 提供图片导入与预览、页面尺寸、型号与纸型、Raw、参数保存／加载和日志。

[客户指南](docs/README.md) 说明精确依赖、权限、最小 API、生命周期、配置、错误码和迁移；[共享 App 示例](shared-app/README.md) 展示应用所有权。打印只在用户明确操作后发生。

## 快速开始

Flutter 需要 Flutter3.41.9／Dart3.11.5 或兼容版本，Android 最低 API24、iOS最低15：

```sh
cd flutter
flutter pub get --enforce-lockfile
flutter run
```

原生 Android 需要 JDK17、Android SDK35／Build Tools35.0.0，固定 Gradle8.13 Wrapper 已随仓库提供：

```sh
./android/gradlew -p android :quick-start:assembleDebug :full-demo:assembleDebug
```

Windows 使用 `android\gradlew.bat -p android` 加相同任务。原生 iOS 使用 Xcode16+／Swift6+，打开 `ios/PrinterSamples.xcodeproj`。具体准备见 [Flutter](flutter/README.md)、[Android](android/README.md)、[iOS](ios/README.md)。Flutter Android 工程使用 SDK36，由自身 Gradle 配置管理工具链。

## 当前 SDK 与行为

- Android：`com.netumscan:printer-sdk-android:0.3.0-preview.16`，来自 Maven Central。
- iOS：公开 `https://github.com/netumscan/printer-sdk-ios`，Exact Version `0.3.0-preview.16`。
- 身份与制品摘要见根 [sdk-lock.json](sdk-lock.json)；Flutter 的 [独立锁](flutter/sdk-lock.json) 与根锁一致。
- 传图片与页面毫米宽高；SDK 管理型号通信、点密度和流式编码。设置纸型与定位均为独立请求，打印不隐式 FORM 或切纸。
- Android 支持 BLE／SPP，iOS 支持 BLE；未知型号不猜参数。`DATA_SENT` 仅表示数据交付，取消或失败后可能已有部分输出，不自动重连、续打或重放。

## 验证与维护

```sh
python3 tools/check-samples.py
```

该检查校验完整清单、来源摘要、精确在线依赖、文档链接与源码边界。原生 Samples CI 保留两端完整构建及运行检查；Flutter CI 增加静态分析、18项测试、精确 SDK APK 验证和 iOS Simulator／5项 RunnerTests。失败定位与证据说明见 [VERIFICATION.md](VERIFICATION.md)。CI 不连接打印机、不发布 APK/IPA、不使用生产签名。

规范应用源码在 SDK 开发仓库维护。`samples-manifest.json` 的 `canonical_full_demos` 及 `flutter_demo` 记录来源摘要；维护流程先导出并验证候选，再推送 `codex/samples-sync-*` 分支，CI 通过后普通快进 `main`。现有标签与 Release 保持原样。

请在 [Issues](https://github.com/netumscan/printer-sdk-samples/issues) 提交可脱敏复现的接入问题。

## 许可与预览边界

自有示例代码、资源和工程配置采用 [MIT](LICENSE)，详见 [许可边界](LICENSING.md)；Gradle Wrapper 保留 [Apache-2.0 许可](android/gradle/wrapper/LICENSE)。**SDK 保持闭源商用许可，MIT 不适用于下载的 SDK、包装层、C 头文件或第三方组件。**

当前 native SDK `0.3.0`、C ABI `3/r3`，`hardware_verified=false`。构建及模拟器通过不表示真实无线、固件、纸面输出或手机峰值内存完成验收。M90 取消后重连的短斜痕仍待解决。ESC/POS 尚无硬件验收。
