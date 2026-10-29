# 兼容性与分发身份

[返回指南](README.md) · 适用版本 `0.3.0-preview.17`

相对 `.16`，本版将移动控制接口改为统一 Listener／Delegate；旧逐调用回调、状态监听和断连闭包不再公开。
调用方必须按[迁移](migration.md)修改并重新编译。操作仍保留原始请求 ID、完整 ManagerResult、稳定错误码和完成证据。
实时设备发现是独立进度通知，最终扫描结果仍保留最终合并设备列表。

native `0.3.0`／C ABI `3/r3` 保持：单一 `printer_sdk.h`、28 个动态 C 导出、23 个 Android JNI 导出和27个具体结构；
header inline 初始化器不计入动态导出。本次不增加 r4，不改变现有 `ConnectionInfo`、C 布局或 JNI ABI。
C ABI3 与 ABI2 不兼容；App 成套使用同一版本的 Java／Swift 包装和 native 库。

本版 iOS 产品、Swift 模块和 CocoaPods Pod 均为 `XLPrinterSDK`；从 `.14` 或更早升级还需将
`PrinterSDK` 的 import 和产品依赖改为 `XLPrinterSDK`。已使用 `.16` 模块的调用方无需再次更名。

冻结候选不等于制品已上传；实际发布状态、精确公开 revision、制品 SHA-256 和独立消费者结果分别核对。
旧在线制品、历史 anchors／proofs／seals、旧候选、开发快照与 SDK lock 不为新接口重写。
回退选择完整旧版本及匹配依赖锁，不移动标签或覆盖二进制，不自动重放失败打印。
