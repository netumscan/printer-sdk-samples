# NETUM Printer SDK 接入指南

开发预览分发身份 **0.3.0-preview.20**，native **0.3.0**，C ABI **3/r3**。
Android 使用统一 `PrinterListener`，iOS 使用弱引用 `PrinterDelegate`。每个 `PrinterManager`
管理一个设备，操作返回请求 ID，实时设备发现、动作终态、状态和意外断连由统一接收者通知。
本版要求 Android Listener 的16个方法和 iOS Delegate 的15个方法全部显式实现；SDK 不再提供默认空实现。Java／Kotlin／Swift 漏写任一方法均不能编译。应用可以在自己的方法中明确忽略不使用的事件。相对 `.17` 需补齐实现并重新编译；旧逐调用回调仍不公开。

| 指南 | 内容 |
| --- | --- |
| [接入](integration.md) | 精确依赖与编译检查过的 Android／iOS App owner |
| [API 与生命周期](api-lifecycle.md) | 请求 ID、统一接收者、实时扫描、取消和清理 |
| [设备与图片](configuration.md) | 型号、纸型、PNG/JPEG、页面尺寸和可空状态 |
| [错误码](errors-troubleshooting.md) | 稳定错误码与原始结果 |
| [兼容性](compatibility.md) | 移动 API 迁移及不变的 C ABI 边界 |
| [迁移](migration.md) | 从旧逐调用回调改为 Listener／Delegate |
| [支持与安全](support-security.md) | 分发身份、诊断和验证范围 |
| [Android Java API](android-java-api.md) | 精确编译器声明与逐项说明 |
| [iOS Swift API](ios-swift-api.md) | 精确编译器声明与逐项说明 |

同一设备的打印、Raw、纸型、走纸和查询共用 native FIFO；App 不需要轮询队列。
`DATA_SENT` 只表示数据交付，不证明纸面完成。软件验证不表示硬件认证，`hardware_verified=false`。
冻结候选与真实在线制品分别核验；候选契约不表示已上传，旧版本历史与精确依赖锁保持独立。
接入时核对版本、制品摘要及实际运行库身份。随包离线声明与历史检查：

```sh
python3 tools/check-api-reference.py
python3 tools/check-api-history.py
```

检查器不签名、联网、安装 App 或执行打印机 IO，不能代替独立制品消费者验证。

状态查询完成回调返回 ManagerResult<StatusResult>；成功值来自本次事务，相同状态连续查询也各自返回，失败／取消无值。状态变化事件独立去重。

正常停止扫描使用 stopScan(scanId)，原扫描完成回调成功返回停止时最新合并列表（可为空），仍用原请求ID；重复、外部、已完成或非扫描ID无操作。cancel 保持取消语义，后台／关闭仍应取消。
ConnectionInfo.printerInfoReadStatus 区分未尝试、成功、失败；仅失败时 printerInfoReadCode、printerInfoReadMessage 有值，保留本次错误和原因。普通信息读取失败不等于连接失败，打印仍可用；查询隔离策略不变，不自动重试或重连。换型号保留本连接诊断，重连清空旧结果并重新读取。
