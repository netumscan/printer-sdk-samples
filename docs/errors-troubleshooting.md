# 错误码与排障

[返回指南](README.md) · 适用发布候选 `0.3.0-preview.2`

参数/能力/资源校验可以在接纳前失败；此时处理同步错误或异常。已有 operation 则观察
COMPLETED（4）、FAILED（5）、CANCELLED（6）终态，读取 snapshot/diagnostic/status。
`get_result==0` 或 result getter 返回只表示成功读回终态，仍须检查任务状态和错误码。

| 错误码 | 含义 |
| ---: | --- |
| 0 | 接口调用成功；不是硬件完成证明。 |
| 1000 | INTERNAL_ERROR |
| 1001 | INVALID_ARGUMENT |
| 1002 | INVALID_STATE，包括提前读取结果或访问已关闭包装对象。 |
| 1003 | NOT_INITIALIZED |
| 1004 | UNSUPPORTED_OPERATION；当前未验证的硬件查询在 IO 前返回此码。 |
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

diagnostic 提供有界消息、domain、可选原生码及关联信息；原生码仅用于诊断，不作为跨平台
业务判断依据。device.diagnostic 是最新记录，不是历史日志；连接异常和 operation 结果
应优先使用各自携带的诊断，避免被另一次调用覆盖。

1008 可能来自单图、作业、Raw、份数、未终态工作数量或源载荷预算，也可能来自内存分配失败。
先检查输入、资源快照和本次诊断，缩减输入或未完成工作；结果读取不预留接纳容量。
已终态任务自动退出接纳预算，不需要轮询或释放完成结果才能继续提交。不要根据该错误
自动重试或重放打印。接纳失败没有 operation，也不会收到该任务的完成回调。

status 为可选结果；缺失表示没有有效硬件读回。掩码为零也不能解释为正常。连接中断后
SDK 不会自动重放未完成任务，已发送字节可能产生副作用；取消不能撤回这些字节。
