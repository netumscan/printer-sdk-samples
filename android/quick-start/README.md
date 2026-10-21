# Android Quick Start

SDK 权限和 API 生命周期见[客户指南](../../docs/README.md)。

最小 BLE 接入：显式授权、扫描、选择设备、连接并发送固定 Bitmap 测试图，观察完成结果，取消／断开／关闭。只传图片与页面毫米，通信与渲染策略由 SDK 内部管理。

```sh
./android/gradlew -p android :quick-start:assembleDebug
```

命令从仓库根目录执行；Windows 使用 `android\gradlew.bat -p android` 加相同任务。Gradle 8.13 由项目 Wrapper 自动提供，无需全局安装。

构建要求、权限及运行步骤见 [Android 指南](../README.md)。`DATA_SENT` 只表示数据发送完成。示例参数需按设备文档确认，点击打印会实际出纸。`hardware_verified=false`。

- [MainActivity.java](src/main/java/com/netumscan/printersdk/samples/quickstart/MainActivity.java)：界面与显式权限流程。
- [QuickStartModel.java](src/main/java/com/netumscan/printersdk/samples/quickstart/QuickStartModel.java)：Manager 所有权、固定图片、请求结果和界面状态。
本例由 `QuickStartModel` 持有 `PrinterManager`，直接提交 `Bitmap` 与毫米页面宽高；PNG/JPEG 数据使用 `printEncodedImage`。连接显式选择已确认的 `PrinterModel`，纸张定位使用独立的 `PaperType` 与 `locateNextLabel` 请求。

示例采用根目录 MIT 许可；在线依赖 `com.netumscan:printer-sdk-android:0.3.0-preview.10` 保持其闭源商用许可。
