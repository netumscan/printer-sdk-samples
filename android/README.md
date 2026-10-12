# Android 示例

SDK 权限、生命周期、FeatureSupport、错误码和诊断字段见[客户指南](../docs/README.md)。

两个独立 App 固定从 Maven Central 获取 `com.netumscan:printer-sdk-android:0.2.0-preview.2`，不依赖本地 SDK 源码或 AAR。

| 示例 | 用途 | applicationId |
| --- | --- | --- |
| `quick-start` | 最小 SPP / CPCL 连接、Gray8 标签提交、任务状态和关闭 | `com.netumscan.printersdk.samples.quickstart` |
| `full-demo` | 配对设备 / BLE 发现、连接、图片导入与预览、打印、取消、Raw 和诊断 | `com.netum.printersdk.demo` |

## 构建

需要 JDK 17 或更新版本、Gradle 8.13、Android SDK Platform 35 和 Build Tools 35.0.0。最低运行版本 Android 8.0 / API 26；APK 包含 `arm64-v8a`、`armeabi-v7a`、`x86_64`。

```sh
export ANDROID_HOME="<Android SDK 安装目录>"
gradle -p android :quick-start:assembleDebug :full-demo:assembleDebug
```

在 Android Studio 中打开本目录时，选择 Gradle 8.13 安装目录。完整 CI 和 R8 / JNI / 三架构验证：

```sh
bash android/verify.sh --output /tmp/printer-sdk-samples-android-validation
```

验证脚本使用 Gradle 8.13，可通过 `GRADLE` 指定可执行文件。脚本运行两端 App 的 debug / release 构建和 lint，核对实际解析的在线 SDK、三个 CPU ABI、最终 R8 DEX 中的 JNI 方法与回调，以及 APK 中的 SDK 许可资源；不会连接设备或执行打印。

## Quick Start

1. 在 Android 系统蓝牙设置中配对支持 SPP / CPCL 的打印机。
2. 构建并按需安装 `quick-start` debug APK。
3. 填写蓝牙地址、DPI 和设备最大可打印宽度。默认 `203 / 576 dots` 只是示例值，必须按设备文档确认。
4. 点击“授权并连接”，授予 Android 12+ 的蓝牙连接权限。
5. 检查纸张后点击“打印测试条纹”。此操作会实际打印一份标签。
6. `DATA_SENT` 只表示数据已发送，实际打印结果需要观察打印机。结束后点击“关闭连接”。

入口在 [MainActivity.java](quick-start/src/main/java/com/netumscan/printersdk/samples/quickstart/MainActivity.java)，最小 SDK 调用集中在 [QuickStartSession.java](quick-start/src/main/java/com/netumscan/printersdk/samples/quickstart/QuickStartSession.java)。连接、提交、轮询和关闭都在应用工作线程执行；SDK 内部串行设备 IO，示例不会自动重连或重放。

Quick Start 只展示 SPP / CPCL。其他协议和 BLE 的显式配置、扫描权限、任务取消、Raw、图片处理和诊断见 Full Demo。Full Demo 保留已有安装身份，安装它可能更新已安装的同 applicationId 应用；需要独立安装时，可自行修改其 applicationId。

## 许可和验证边界

本仓库的示例源码与构建配置采用根目录 MIT 许可。通过 Maven Central 下载的 Printer SDK 仍为闭源商用软件，适用上游 [SDK 许可](https://github.com/netumscan/printer-sdk-android/blob/main/LICENSE)，MIT 不授予 SDK 实现的权利。SDK AAR 自带许可及 Android NDK 第三方声明，打包验证会确认它们保留在 APK 中。

本轮 `hardware_verified=false`。在线解析、编译、R8 和制品检查通过不代表任何打印机、纸张或协议组合已完成硬件验收。
