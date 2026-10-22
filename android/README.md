# Android 示例

SDK 权限、生命周期、capabilities、错误码和诊断字段见[客户指南](../docs/README.md)。

两个独立 App 固定从 Maven Central 获取 `com.netumscan:printer-sdk-android:0.3.0-preview.11`，不依赖本地 SDK 源码或 AAR。

| 示例 | 用途 | applicationId |
| --- | --- | --- |
| `quick-start` | 最小 BLE 发现／连接、PNG 图片和页面毫米、请求结果和关闭 | `com.netumscan.printersdk.samples.quickstart` |
| `full-demo` | 配对设备 / BLE 发现、连接、图片导入与预览、打印、取消、Raw 和诊断 | `com.netum.printersdk.demo` |

## 构建

需要 JDK 17 或更新版本、Android SDK Platform 35 和 Build Tools 35.0.0。仓库自带固定 Gradle 8.13 的标准 Wrapper，无需另行安装 Gradle。最低运行版本 Android 5.0 / API 21；APK 包含 `arm64-v8a`、`armeabi-v7a`、`x86_64`。

```sh
export ANDROID_HOME="<Android SDK 安装目录>"
./android/gradlew -p android :quick-start:assembleDebug :full-demo:assembleDebug
```

上述命令从仓库根目录执行；Windows 使用 `android\gradlew.bat -p android` 加相同任务。在 Android Studio 中打开本目录时，选择使用项目 Gradle Wrapper。

首次运行需要访问 Gradle 官方 HTTPS 分发入口；下载的 8.13 binary ZIP 必须符合固定 SHA-256，连接／读取超时为 10 秒。Wrapper JAR、POSIX／Windows 脚本、许可和分发配置由 `tools/check-samples.py` 按可信上游内容检查。已有 `GRADLE_USER_HOME/wrapper/dists` 缓存会按标准 Wrapper 机制复用；离线构建还要求 Android 工具链与 Maven／插件依赖已缓存，可以使用：

```sh
./android/gradlew -p android --offline :quick-start:assembleDebug :full-demo:assembleDebug
```

缓存缺失时请先联网完成一次构建；`--offline` 不能替代首次 Wrapper 分发下载。Wrapper 的来源、校验和与第三方许可见 [Wrapper 记录](gradle/wrapper/README.md)。完整 CI、R8 / JNI / 三架构及实际模拟器运行验证，需要先启动一个包含线上 AAR 对应 ABI 的 Android 模拟器（API 21+），然后显式选择其 serial：

```sh
bash android/verify.sh --emulator-serial emulator-5554 --output /tmp/printer-sdk-samples-android-validation
```

验证脚本默认执行项目 Wrapper；可通过 `GRADLE` 显式指定其他 Gradle 8.13 可执行文件。`gradle-tool.json` 会区分默认 Wrapper 和显式覆盖，`gradle-bootstrap.log` 保留启动或下载失败原因。脚本构建两个 App 的 debug / release / 测试 APK 并执行 lint，核对实际解析的在线 SDK、三个 CPU ABI、最终 R8 DEX 中的 JNI 方法与回调，以及 APK 中的 SDK 许可资源。

运行门禁只接受 `emulator-N` 且 `ro.kernel.qemu=1` 的已启动模拟器，拒绝物理设备。它安装这两个样例和测试 APK，并清除这两个样例在所选模拟器中的数据，以保证诊断记录来自本次运行；请使用专用测试模拟器。每次验证使用新的输出目录。

两个测试 runner 直接使用 Android `Instrumentation`，无需 AndroidX。Quick Start 启动后检查没有打印机实例或任务；Full Demo 保持离线、不扫描和不授予蓝牙权限。测试 APK 仅为门禁使用精确 SDK 版本的内部 `BluetoothChannel` / `IoControl` 和既有私有构造方法；客户集成仍使用公共 API。测试 APK 将内存通道注入该构造方法，实际加载 AAR 的 JNI、通过原生 `nCreate` 的 ABI 3 / 最低修订 2 握手，验证最多 3 字节的部分写入和两个 Raw 任务的 14 字节顺序。ABI 握手要求与诊断中的分发清单 ABI 来源会分别记录，避免把清单值当成运行时 getter 的结果。

`runtime-validation.json`（schema 2）和合并的 `validation.json` 记录本次 nonce、实际进程 ABI、API 级别、APK 哈希和测试结果；`instrumentation-result.json` 与 `quick-start-runtime.json` 保留两份真实 runner 结果。当前版本验证普通公开 API、离线启动和 Manager 释放，使用 `runtime_api_verified=true`、`runtime_compatibility_verified=false`，不声称旧版兼容日志场景通过。固定 SDK commit、AAR checksum 与 Demo 来源指纹另由构建身份记录核对。CI 在 API 35 / x86_64 模拟器执行门禁，并归档软件验证结果。全部流程不连接打印机、不执行真实打印，`hardware_verified=false`。下载双端脱敏日志与软件报告、定位失败步骤见 [CI 验证证据](../VERIFICATION.md)。

## Quick Start

1. 构建并按需安装 `quick-start` debug APK。
2. 扫描 BLE，按系统提示授权，明确选择自己的打印机。
3. 确认实物为 LT-M90 后手动连接；本例显式选择 `PrinterModel.LT_M90`。
4. 检查纸张后点击打印固定 Bitmap 条纹，页面为 48 × 8 mm。此操作会实际打印一份标签。
5. `DATA_SENT` 只表示数据已发送，实际打印结果需要观察打印机。结束后手动断开，页面销毁时关闭独占 Manager。

入口在 [MainActivity.java](quick-start/src/main/java/com/netumscan/printersdk/samples/quickstart/MainActivity.java)，请求与界面状态见 [QuickStartModel.java](quick-start/src/main/java/com/netumscan/printersdk/samples/quickstart/QuickStartModel.java)。图片使用 `Bitmap` 或 PNG/JPEG 编码数据，页面只传毫米宽高；SDK 在返回前捕获独立输入，在后台准备并串行发送，示例不会自动重连或重放。

Full Demo 另有配对设备 SPP、纸张类型、标签定位、Raw、图片导入预览和诊断。连接和打印不会隐式设置纸张或定位标签。Full Demo 保留已有安装身份，安装它可能更新已安装的同 applicationId 应用；需要独立安装时，可自行修改其 applicationId。

Full Demo 的 BLE 扫描在 Android 6–11 还需运行时位置权限和相应系统设置；Android 5.x 不请求运行时权限，Android 12+ 使用附近设备权限。完整策略见[接入与权限](../docs/integration.md)。

## 许可和验证边界

本仓库自有示例源码与构建配置采用根目录 MIT 许可；随仓库提供的 Gradle Wrapper 保留其 [Apache-2.0 许可](gradle/wrapper/LICENSE)。通过 Maven Central 下载的 Printer SDK 仍为闭源商用软件，适用上游 [SDK 许可](https://github.com/netumscan/printer-sdk-android/blob/main/LICENSE)，MIT 不授予 SDK 实现的权利。SDK AAR 自带许可及 Android NDK 第三方声明，打包验证会确认它们保留在 APK 中。

本轮 `hardware_verified=false`。在线解析、编译、R8 和制品检查通过不代表任何打印机、纸张或协议组合已完成硬件验收。
