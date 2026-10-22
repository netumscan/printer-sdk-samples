# Android Full Demo

统一诊断字段、兼容性边界和反馈方式见[客户指南](../../docs/README.md)。

完整诊断应用提供 SPP / BLE 连接、已配对设备和 BLE 发现、图片导入与画布预览、内置型号／纸型和页面毫米、取消、Raw 命令及日志导出。示例不会自动重连或重放。

```sh
./android/gradlew -p android :full-demo:assembleDebug
```

命令从仓库根目录执行；Windows 使用 `android\gradlew.bat -p android` 加相同任务。Gradle 8.13 由项目 Wrapper 自动提供，无需全局安装。

操作前确认实际内置型号和纸型；SDK 管理协议、点密度、BLE 服务与传输策略。PNG/JPEG 直接通过 printEncodedImage 提交，不需要构造图源、渲染或打印 Options。Android 12+ 按需授予附近设备权限；Android 6–11 BLE 扫描还需运行时位置权限，Android 5.x 使用安装时授权的旧版蓝牙权限。打印只在明确点击发送后发生；取消不能撤回已发送数据。构建要求和完整验证见 [Android 指南](../README.md)。

`applicationId` 保留 `com.netum.printersdk.demo`，代码包名为 `com.netumscan.printersdk.demo`。相同 applicationId 的安装会更新已有应用；需要并行安装时可自行修改 applicationId。

示例采用根目录 MIT 许可；在线依赖 `com.netumscan:printer-sdk-android:0.3.0-preview.11` 保持其闭源商用许可。SDK 许可及第三方声明随 APK 保留。

本版 `hardware_verified=false`。构建和 R8 验证不代表实物验收；数据发送完成不代表打印完成。M90 取消后重连短斜痕问题仍待解决，ESC/POS 尚未完成硬件验收。
