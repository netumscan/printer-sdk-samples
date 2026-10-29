# 迁移到统一 Listener／Delegate

[返回指南](README.md) · 适用版本 `0.3.0-preview.17`

本版公共移动 API 有不兼容变化，旧逐调用 callback／completion、独立状态监听和连接断连闭包已移除。
Android／iOS 调用方必须迁移并重新编译；不提供旧入口兼容。旧 SDK 标签、制品、开发快照与精确依赖锁保持原身份。

1. 由稳定业务 owner 创建一个 `PrinterManager`。Android owner 实现 `PrinterListener` 并调用
   `setListener(this)`；iOS owner 实现 `@MainActor PrinterDelegate` 并设置 `delegate = self`。
2. 从 scan、connect、print、Raw、纸型、定位、查询、disconnect 和 close 调用中移除逐调用回调参数，
   保存返回的 `RequestId`／`RequestID`，在对应动作的类型化通知中匹配和处理原始 `ManagerResult`。
3. 将旧 `setStatusListener` 的状态处理迁入 Android `onStatusChanged`／iOS `didUpdateStatus`；
   `getStatus()` 的结果分别经 `onStatusQueryCompleted`／`didFinishQueryingStatus` 交付 Void，状态值仍来自状态通知。
4. 将旧连接断连闭包迁入 `onDisconnected`／`didLoseConnection`，按所属连接请求 ID 处理，
   主动 disconnect 的结果与意外断连分开保留。
5. scan 立即返回本轮 ID，实时 `onDeviceDiscovered`／`didDiscover` 将首见候选加入可见列表；
   扫描成功终态再刷新最终合并名称和 RSSI。停止使用 `cancel(scanId)`，按 ID 隔离旧轮和迟到通知；选择不自动连接。
6. 不要先解除接收者再等待关闭。Android 强持有当前 Listener，iOS 弱持有 Delegate；
   App 需持有 owner 直至 `onCloseCompleted`／`didFinishClosing`。替换或解除后不补交历史通知。

从更早版本升级时，图片直接传 Bitmap／UIImage 或完整 PNG/JPEG byte[]／Data 和页面毫米宽高；
删除公开 Profile、ImageSource、loader、渲染选项及调用方通信参数。设备、图片和参数对象不暴露平台句柄。
按实际装纸显式 `setPaperType`，定位另调 `locateNextLabel`；连接和打印不隐式设纸型或定位。

native 仍为 `0.3.0`，C ABI `3/r3`、28 个 C／23 个 JNI 导出和 `ConnectionInfo` 保持。
移动 API 迁移不要求新增 C ABI revision；包装、运行库和头仍须成套使用匹配版本。
SDK 不自动重连、续打或重放，取消不能撤回已发送数据，`DATA_SENT` 不证明纸面完成。

完整声明见 [Java API](android-java-api.md)、[Swift API](ios-swift-api.md)，示例见[接入](integration.md)。
