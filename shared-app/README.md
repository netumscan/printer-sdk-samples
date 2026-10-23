# 自有 APP 的共享打印机所有权

本目录 Android 代码使用 SDK **0.3.0-preview.11**，iOS 代码使用 **0.3.0-preview.16** 的 Manager 回调接口。当前开发示例通过本地 SDK 构建；已发布示例及依赖按各自版本冻结，不能把新代码配旧版制品。

- [Android owner](android/SharedPrinterApp.java)：由 `Application` 持有；页面使用 `bindPage()`，离开时关闭应用自己的 `PageBinding`。打印返回纯 `RequestId`，通过 owner 的 `cancel(id)` 取消；`lastResult()` 保留最近结果。
- [iOS owner](ios/SharedPrinterApp.swift)：由 `App` 持有的 `@MainActor` 对象；页面绑定应用通知，离开时仅解绑。owner 负责连接、逐请求打印、取消及关闭，保存请求身份与终态结果。

页面解绑不会取消已接纳任务，也不会关闭共享设备。真正退出业务时，由 APP owner 执行显式断开及异步释放。终态结果由 owner 复制并保留最近一条；完整业务历史应写入 APP 自己的存储。关闭或更换 owner 后不承诺继续通知旧页面。

Swift 示例通过 Swift 5/6 完整严格并发编译，不需要 `@preconcurrency import XLPrinterSDK`。SDK 回调为 `@Sendable` 并异步交付到主线程，UI 更新显式进入主 actor。Android 示例兼容 Java 8。两端图像准备在 Manager 后台方法中进行；App 不持有原生设备、operation、image/job 或 SDK 订阅。

同一 Manager 保留最多 64 个未结束业务请求并按调用顺序接纳，全部 IO 仍走原设备 FIFO。资源不足 `1008` 时缩小输入或减少未完成工作，读取本次结果的错误消息；终态自动归还 SDK 容量，不需要轮询或释放结果句柄。只在用户明确操作后连接、打印或重试，不自动重连、续打或重放。

界面只展示应用持有的连接/请求阶段和回调结果；SDK 内部工作、队列及资源状态不对外暴露，硬件状态继续未知。失败/取消保留 `outputMayHaveOccurred`；`DATA_SENT` 表示数据发送完成，需要用户查看实际出纸。

示例代码采用 [MIT](../LICENSE)，SDK 本身的许可保持不变。它们作为可复用应用源码导出，不新增第五个可安装 App；两端 Quick Start 与 Full Demo 仍是完整构建入口。

Android retains SDK .11; the iOS owner uses SDK .16 and `import XLPrinterSDK`. See the [iOS guide](../ios/SDK_GUIDE.md).
