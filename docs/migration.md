# 迁移到必须完整实现的 Listener／Delegate

[返回指南](README.md) · 适用版本 `0.3.0-preview.20`

从 `.17` 升级时，为当前 owner 补齐全部回调：Android16个、iOS15个。Android 移除 Java default 方法体，Swift 移除 protocol extension 默认方法；漏写任一方法将编译失败。没有 SDK 公共空适配器、默认基类或 optional 要求。应用不使用的事件可以显式写出方法并在其中忽略，但仍需保留与实际操作对应的结果处理和关闭清理。

查询完成回调的泛型由 Void 改为 StatusResult；正常停止改用 `stopScan`，连接结果新增自动信息读取诊断。请求 ID 与接收者生命周期保持，移动调用方需要重新编译。旧 SDK 标签、制品、开发快照与精确依赖锁保持原身份。下方还列出从更早逐调用回调版本迁移的步骤。

1. 由稳定业务 owner 创建一个 `PrinterManager`。Android owner 实现 `PrinterListener` 并调用
   `setListener(this)`；iOS owner 实现 `@MainActor PrinterDelegate` 并设置 `delegate = self`。
2. 从 scan、connect、print、Raw、纸型、定位、查询、disconnect 和 close 调用中移除逐调用回调参数，
   保存返回的 `RequestId`／`RequestID`，在对应动作的类型化通知中匹配和处理原始 `ManagerResult`。
3. 将旧 `setStatusListener` 的状态处理迁入 Android `onStatusChanged`／iOS `didUpdateStatus`；
   `getStatus()` 的结果分别经 `onStatusQueryCompleted`／`didFinishQueryingStatus` 交付 ManagerResult<StatusResult>，value 为本次原生事务已校验状态，相同状态查询也各自返回结果；状态变化通知独立保留。
4. 将旧连接断连闭包迁入 `onDisconnected`／`didLoseConnection`，按所属连接请求 ID 处理，
   主动 disconnect 的结果与意外断连分开保留。
5. scan 立即返回本轮 ID，实时 `onDeviceDiscovered`／`didDiscover` 将首见候选加入可见列表；
   扫描成功终态再刷新最终合并名称和 RSSI。正常停止使用 `stopScan(scanId)`，按 ID 隔离旧轮和迟到通知；选择不自动连接。
6. 不要先解除接收者再等待关闭。Android 强持有当前 Listener，iOS 弱持有 Delegate；
   App 需持有 owner 直至 `onCloseCompleted`／`didFinishClosing`。替换或解除后不补交历史通知。

从更早版本升级时，图片直接传 Bitmap／UIImage 或完整 PNG/JPEG byte[]／Data 和页面毫米宽高；
删除公开 Profile、ImageSource、loader、渲染选项及调用方通信参数。设备、图片和参数对象不暴露平台句柄。
按实际装纸显式 `setPaperType`，定位另调 `locateNextLabel`；连接和打印不隐式设纸型或定位。

native 仍为 `0.3.0`，C ABI `3/r3`、28 个 C／23 个 JNI 导出保持；Java／Swift `ConnectionInfo` 新增只读诊断字段。
移动 API 迁移不要求新增 C ABI revision；包装、运行库和头仍须成套使用匹配版本。
SDK 不自动重连、续打或重放，取消不能撤回已发送数据，`DATA_SENT` 不证明纸面完成。

完整声明见 [Java API](android-java-api.md)、[Swift API](ios-swift-api.md)，示例见[接入](integration.md)。

正常停止扫描使用 stopScan(scanId)，原扫描完成回调成功返回停止时最新合并列表（可为空），仍用原请求ID；重复、外部、已完成或非扫描ID无操作。cancel 保持取消语义，后台／关闭仍应取消。
ConnectionInfo.printerInfoReadStatus 区分未尝试、成功、失败；仅失败时 printerInfoReadCode、printerInfoReadMessage 有值，保留本次错误和原因。普通信息读取失败不等于连接失败，打印仍可用；查询隔离策略不变，不自动重试或重连。换型号保留本连接诊断，重连清空旧结果并重新读取。
