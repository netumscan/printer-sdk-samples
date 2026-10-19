# 自有 APP 的共享打印机所有权

本目录代码固定接入 SDK **0.3.0-preview.2**（ABI 3/r1），可直接加入自有 APP；SDK 依赖及权限使用本仓库的 Android/iOS 工程配置。

- [Android owner](android/SharedPrinterApp.java)：由 `Application` 持有；页面 `bind()` 后观察连接、设备与打印结果，页面销毁时关闭 `PageBinding`。连接需要显式 `connect()`，`PrintRequest.cancel()` 覆盖准备及发送阶段。一个页面最多保留一个请求，多个页面仍使用 SDK 的同一设备 FIFO。
- [iOS owner](ios/SharedPrinterApp.swift)：由 `App` 持有的 `@MainActor` 对象；页面通过 `SharedPrinterPage` 订阅，离开页面仅解绑通知。`connect`、`submit`、`cancel` 与 `close` 由 owner 负责。示例限制一个在途请求；SDK 本身提供设备 FIFO。

页面解绑不会取消已接纳任务，也不会关闭共享设备。真正退出业务时，由 APP owner 执行显式断开及异步释放。终态结果由 owner 复制并保留最近一条；完整业务历史应写入 APP 自己的存储。关闭或更换 owner 后不承诺继续通知旧页面。

Swift 示例以 Swift 6、完整严格并发检查编译，不需要 `@preconcurrency import PrinterSDK`。回调为 `@Sendable`，UI 更新由主 actor 负责；耗时准备和阻塞 SDK 生命周期操作离开 UI 线程。Android 示例兼容 Java 8，公开控制调用在主线程执行，准备与释放交给后台队列。

资源不足 `1008` 时应缩小输入、减少未完成工作，并查看资源快照和诊断；已完成操作的容量会自动归还，不能靠轮询或重复释放历史结果解决。仅在用户明确操作后连接、打印或重试；不自动重连、续打或重放。

设备连接、SDK 工作及队列状态可以展示给用户，未验证的硬件状态显示未知。`DATA_SENT` 表示数据发送完成，需要用户查看实际出纸。

示例代码采用 [MIT](../LICENSE)，SDK 本身的许可保持不变。它们作为可复用应用源码导出，不新增第五个可安装 App；两端 Quick Start 与 Full Demo 仍是完整构建入口。
