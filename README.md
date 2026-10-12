# printer-sdk-samples

NETUM 打印机 SDK 的 MIT 许可应用示例。Android 与 iOS 均固定接入在线 SDK **0.2.0-preview.2**。
克隆本仓库即可构建，无需访问 SDK 私有开发仓库。

| 平台 | 最小接入 | 完整示例 |
| --- | --- | --- |
| Android | [Quick Start](android/quick-start/README.md) | [Full Demo](android/full-demo/README.md) |
| iOS | [Quick Start](ios/quick-start/README.md) | [Full Demo](ios/full-demo/README.md) |

Quick Start 展示最小的发现/选择、连接、显式提交、状态与释放流程。Full Demo 提供
标签/小票图片、配置、取消、Raw 命令与诊断界面。打印只在用户明确操作后发生。

## 构建

- Android：JDK 17、Gradle 8.13、Android SDK Platform 35。见 [Android 指南](android/README.md)。
- iOS：Xcode 15+（Swift 5.9+）、iOS 15+。见 [iOS 指南](ios/README.md)。
- Android SDK：`com.netumscan:printer-sdk-android:0.2.0-preview.2`，来自 Maven Central。
- iOS SDK：`https://github.com/netumscan/printer-sdk-ios`，Exact Version `0.2.0-preview.2`。

依赖的版本、SDK 许可地址和已发布制品 SHA-256 见 [sdk-lock.json](sdk-lock.json)。
仓库只包含应用示例源码；SDK 实现及二进制由对应的在线分发入口提供。

## 验证与反馈

运行 `python3 tools/check-samples.py` 检查导出完整性和依赖边界。平台构建验证命令见
各端指南。GitHub Actions 在 push、pull request 和手动触发时执行相同验证；
Android 检查两个应用的构建、lint 和 R8 制品，iOS 检查两个应用的构建及模拟器基础运行。
CI 不连接打印机、不发布 APK/IPA，也不使用任何签名密钥。

两端 Full Demo 的功能源码统一从开发仓库的主 Demo 导出，来源路径及逐文件 SHA-256
记录在 `samples-manifest.json` 的 `canonical_full_demos` 中。维护者的一键同步流程先
验证来源一致性及两端在线接入，再推送 `codex/samples-sync-*` 候选分支；双端 CI 通过后
才快进更新 `main`。现有标签与 Release 保持原样，SDK 依赖继续固定为 `0.2.0-preview.2`。
请在开发仓库修改 Demo 后同步；直接修改公开导出文件会被完整性检查拒绝。

版本发布包含四个示例的一致快照。请在
[Issues](https://github.com/netumscan/printer-sdk-samples/issues) 提交可脱敏复现的接入问题。

## 许可与预览边界

本仓库自有示例代码、资源和工程配置采用 [MIT](LICENSE)，详见 [许可边界](LICENSING.md)。
**SDK 保持闭源商用许可，MIT 不适用于下载的 SDK、包装层、C 头文件或第三方组件。**

本版是开发预览：原生 SDK 0.2.0、C ABI 2/r10，`hardware_verified=false`。
构建及模拟器通过不等于无线、固件、纸面输出或手机峰值内存完成验收。
数据发送完成不代表机械打印完成；连接中断后不会自动续打或重放。
M90 取消后重连的短斜痕问题仍待解决，ESC/POS 尚未完成硬件验收。
