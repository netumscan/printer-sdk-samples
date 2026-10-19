# API 与生命周期

[返回指南](README.md) · 适用发布候选 `0.3.0-preview.2`

Manager 管理设备与共享按需观察调度。创建通信设备不绑定打印 Profile、不执行 IO；
通过 Manager 的异步连接入口连接，再以每次提交的 Profile/不可变 PrintJob 打印。
直接创建的 MobilePrinter 也可订阅。阻塞的 connect/disconnect/close 不应直接在 UI 线程调用。

submit 校验并原子入队后返回；成功只代表已接纳。所有已接纳打印、Raw、介质设置及走纸
使用同一设备 FIFO。SDK 不自动重连、重放或续打；超时包括排队和执行，不在开始 IO 时重置。
资源或参数校验失败可直接抛错，此时没有 operation，也没有该任务的完成回调。

PrintOperation.onComplete 接收不可变终态 Result。每个有效注册恰好投递一次，终态后注册
也能收到；结果复制后在 SDK 锁外投递，默认主线程。Android 可指定 Executor，Swift 可指定
DispatchQueue。operation 的终态结果不依赖设备继续存活；读取成功不等于任务成功，须检查
snapshot.state/error 和 completion。DATA_SENT 仅表示传输完成。

Swift 不可变配置、快照和终态结果支持 Sendable；跨队列回调声明为 @Sendable。
默认主队列交付不代表 closure 静态属于 MainActor；UI 更新使用 Task { @MainActor in ... }。
App 不需要 @preconcurrency import，也不应给未同步的可变对象添加 unchecked Sendable 来压制诊断。

MobilePrinter.observeState 先交付当前连接/工作/队列快照，再合并变化；不是完整事件历史。
当前由平台 SDK 共享定时读取本地快照，C ABI 没有新增 App 回调入口或 native push。

| 动作 | 生命周期效果 |
| --- | --- |
| Subscription.close / ObservationSubscription.cancel | 停止该订阅，不取消打印。 |
| operation.cancel | 显式请求协作取消；最终结果仍通过有效完成订阅交付。 |
| operation.close | 关闭调用方读取、注册和取消权，不取消工作；已注册完成订阅仍由内部观察器保活并兑现。 |
| manager.release(printer) | 回收指定设备，中止其未完成工作，其他设备仍可使用。 |
| manager.close | 回收全部所属设备；这个 Manager 永久关闭。 |

内部观察器复制终态结果后释放 native 所有权，再安排回调。Manager 回收会保留已订阅任务
的终态交付，不等待主线程执行客户回调；close 完成不表示已安排回调全部执行完。
示例关闭独占会话时主动注销页面订阅，因此不再向已离开的 UI 显示结果。

Java image/job 构建成功后可依次关闭调用方 image 和 job，已接纳操作保留所需存储；Swift
由对象生命周期管理它们。operation.close 与取消分开；共享任务不应因页面离开而默认取消。
