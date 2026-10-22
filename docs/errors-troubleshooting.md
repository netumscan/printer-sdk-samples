# 错误码与排障

[返回指南](README.md) · 适用版本 `0.3.0-preview.11`

Manager 的参数、状态、准备、准入和执行失败统一通过异步完成回调报告。
先检查 `ManagerResult.outcome` 和稳定 `code`，再读取辅助 `message`、完成证据与
`outputMayHaveOccurred`；成功获得结果不等于已证明机械打印完成。数据对象构造器的校验错误由应用处理。

| 错误码 | 含义 |
| ---: | --- |
| 0 | 请求成功；不是硬件完成证明。 |
| 1000 | INTERNAL_ERROR |
| 1001 | INVALID_ARGUMENT |
| 1002 | INVALID_STATE，例如未连接、正在清理或已关闭 Manager。 |
| 1003 | NOT_INITIALIZED |
| 1004 | UNSUPPORTED_OPERATION；未验证的硬件查询不发送猜测指令。 |
| 1005 | CANCELLED |
| 1006 | ABI_MISMATCH；检查整套库与包装版本。 |
| 1007 | BUFFER_TOO_SMALL |
| 1008 | RESOURCE_LIMIT |
| 2000 | TRANSPORT_NOT_OPEN |
| 2001 | TRANSPORT_TIMEOUT |
| 2002 | TRANSPORT_CONNECTION_LOST |
| 2003 | TRANSPORT_IO_ERROR |
| 2004 | TRANSPORT_NO_PROGRESS |
| 3000 | PROTOCOL_INVALID_RESPONSE |

消息用于可读诊断，程序分支以稳定错误码为主。移动 API 不要求读取设备最新诊断、native 原生码、
operation 快照或资源快照来决定任务结果，避免另一请求的状态覆盖本次判断。

两端 Raw 入口按以下顺序拒绝，多个条件同时失败时取最先命中的一项：

1. 空数据（Android 包括 null）：1001。
2. 数据超过 1MiB（1,048,576 字节）：1008；恰好等于上限允许继续检查。
3. Manager 未连接、正在清理或已永久关闭：1002。
4. Manager 已有 64 个未结束业务请求：1008。

公开Raw固定一份；例如已关闭后传空数据仍返回1001。入口拒绝异步交付一次主线程结果，completion=NONE、
outputMayHaveOccurred=false，不执行打印机 IO。前置检查不预留容量，最终入队仍原子复检；
native 接纳还可能因自己的预算或分配失败拒绝。此顺序不改变 C SDK 的独立校验规则。

1008 可能来自 Manager 未完成业务请求上限、单图、作业、Raw、native 源载荷预算或内存分配失败。
缩减输入或未完成工作；已终态请求自动归还接纳容量，不靠轮询或重复释放结果解决。
准入失败同样有本次请求的完成回调，不会产生应用需要释放的 operation。

状态值为可选业务结果，缺失表示没有有效硬件读回；状态位未知不能解释为正常。
`outputMayHaveOccurred=true` 表示可能已产生部分输出，取消不能撤回这些字节。
SDK 不自动重连或重放，后续操作由应用结合用户确认显式发起。
