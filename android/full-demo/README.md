# Android Full Demo

完整诊断应用提供 SPP / BLE 连接、已配对设备和 BLE 发现、图片导入与画布预览、CPCL 标签 / ESC/POS 小票、取消、Raw 命令及日志导出。示例不会自动重连或重放。

```sh
gradle -p android :full-demo:assembleDebug
```

操作前根据设备文档确认协议、纸张、DPI、宽度及 BLE 服务和特征值。按需授予连接和扫描权限；Android 11 及更早系统扫描还需要位置权限。打印只在明确点击发送后发生；取消不能撤回已发送数据。构建要求和完整验证见 [Android 指南](../README.md)。

`applicationId` 保留 `com.netum.printersdk.demo`，代码包名为 `com.netumscan.printersdk.demo`。相同 applicationId 的安装会更新已有应用；需要并行安装时可自行修改 applicationId。

示例采用根目录 MIT 许可；在线依赖 `com.netumscan:printer-sdk-android:0.2.0-preview.2` 保持其闭源商用许可。SDK 许可及第三方声明随 APK 保留。

本版 `hardware_verified=false`。构建和 R8 验证不代表实物验收；数据发送完成不代表打印完成。M90 取消后重连短斜痕问题仍待解决，ESC/POS 尚未完成硬件验收。
