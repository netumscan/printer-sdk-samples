# 兼容性与分发身份

[返回指南](README.md) · 适用版本 `0.3.0-preview.20`

相对已发布 `.17`，本版移除 Listener／Delegate 的默认空实现，所有回调都必须由调用者显式实现；漏写任一方法会导致 Java／Kotlin／Swift 编译失败。不新增公共空适配器或基类。查询完成回调改为 `ManagerResult<StatusResult>`，新增 `stopScan` 和连接信息读取诊断；请求 ID 与接收者生命周期保持，旧逐调用回调、独立状态监听和断连闭包仍不公开。
调用方必须按[迁移](migration.md)修改并重新编译。操作仍保留原始请求 ID、完整 ManagerResult、稳定错误码和完成证据。
实时设备发现是独立进度通知，最终扫描结果仍保留最终合并设备列表。

native `0.3.0`／C ABI `3/r3` 保持：单一 `printer_sdk.h`、28 个动态 C 导出、23 个 Android JNI 导出和27个具体结构；
header inline 初始化器不计入动态导出。本次不增加 r4，不改变 C 布局或 JNI ABI；Java／Swift `ConnectionInfo` 新增只读诊断字段。
C ABI3 与 ABI2 不兼容；App 成套使用同一版本的 Java／Swift 包装和 native 库。

本版 iOS 产品、Swift 模块和 CocoaPods Pod 均为 `XLPrinterSDK`；从 `.14` 或更早升级还需将
`PrinterSDK` 的 import 和产品依赖改为 `XLPrinterSDK`。已使用 `.16` 模块的调用方无需再次更名。

冻结候选不等于制品已上传；实际发布状态、精确公开 revision、制品 SHA-256 和独立消费者结果分别核对。
旧在线制品、历史 anchors／proofs／seals、旧候选、开发快照与 SDK lock 不为新接口重写。
回退选择完整旧版本及匹配依赖锁，不移动标签或覆盖二进制，不自动重放失败打印。

状态查询完成回调返回 ManagerResult<StatusResult>；成功值来自本次事务，相同状态连续查询也各自返回，失败／取消无值。状态变化事件独立去重。

正常停止扫描使用 stopScan(scanId)，原扫描完成回调成功返回停止时最新合并列表（可为空），仍用原请求ID；重复、外部、已完成或非扫描ID无操作。cancel 保持取消语义，后台／关闭仍应取消。
ConnectionInfo.printerInfoReadStatus 区分未尝试、成功、失败；仅失败时 printerInfoReadCode、printerInfoReadMessage 有值，保留本次错误和原因。普通信息读取失败不等于连接失败，打印仍可用；查询隔离策略不变，不自动重试或重连。换型号保留本连接诊断，重连清空旧结果并重新读取。
