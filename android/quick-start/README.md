# Android Quick Start

SDK 权限和 API 生命周期见[客户指南](../../docs/README.md)。

最小 SPP / CPCL 接入：用户填写已配对设备地址和参数，显式授权、连接、提交一份 Gray8 标签，观察任务状态并关闭。后台工作线程执行阻塞 SDK 操作。

```sh
./android/gradlew -p android :quick-start:assembleDebug
```

命令从仓库根目录执行；Windows 使用 `android\gradlew.bat -p android` 加相同任务。Gradle 8.13 由项目 Wrapper 自动提供，无需全局安装。

构建要求、权限及运行步骤见 [Android 指南](../README.md)。`DATA_SENT` 只表示数据发送完成。示例参数需按设备文档确认，点击打印会实际出纸。`hardware_verified=false`。

- [MainActivity.java](src/main/java/com/netumscan/printersdk/samples/quickstart/MainActivity.java)：界面与显式权限流程。
- [QuickStartSession.java](src/main/java/com/netumscan/printersdk/samples/quickstart/QuickStartSession.java)：最小连接、打印、状态观察和资源释放。

示例采用根目录 MIT 许可；在线依赖 `com.netumscan:printer-sdk-android:0.3.0-preview.2` 保持其闭源商用许可。
