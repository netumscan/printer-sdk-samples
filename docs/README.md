# NETUM Printer SDK 接入指南

分发身份 **0.3.0-preview.10**，原生 SDK **0.3.0**，C ABI **3/r2**。
当前为开发预览，移动 API 使用独立的 `PrinterManager` 管理一个设备；输入、配置和结果均为数据对象。
开发版本不作为在线发布证明，接入时核对精确版本的契约、制品摘要及实际运行身份。
软件验证不表示硬件已认证，`hardware_verified=false`。

| 指南 | 内容 |
| --- | --- |
| [接入](integration.md) | 精确依赖与经过编译检查的 Android/iOS App 会话代码 |
| [API 与生命周期](api-lifecycle.md) | Manager 请求、回调、取消和清理屏障 |
| [连接与打印配置](configuration.md) | 内置型号、纸型、页面尺寸、PNG/JPEG与能力未知状态 |
| [错误码与排障](errors-troubleshooting.md) | 稳定错误码与统一完成结果 |
| [兼容性](compatibility.md) | 移动 API 迁移和 C ABI 边界 |
| [迁移](migration.md) | 将旧移动控制入口收敛到 Manager |
| [支持与安全](support-security.md) | 版本记录、诊断和验证边界 |
| [Android Java API](android-java-api.md) | 精确编译器声明与逐项说明 |
| [iOS Swift API](ios-swift-api.md) | 精确编译器声明与逐项说明 |

同一设备的打印、Raw、介质配置、走纸和查询均经过同一个 native FIFO。
应用无需轮询队列或先查询是否忙碌。`DATA_SENT` 只表示数据发送完成；未验证的硬件状态显示未知。

发布时以随包契约区分 `development`、`candidate` 与真实已发布制品。
`contract_status=candidate` 和 `published_contract_verified=false` 只说明冻结候选与历史证明已核对，
不表示候选已上传。开发身份不能直接作为发布候选，旧在线版本和公开历史继续保留。
随包文档及历史检查器可离线验证声明和旧证明：

```sh
python3 tools/check-api-reference.py
python3 tools/check-api-history.py
```

检查器不会签名、联网、安装 App 或执行打印机 IO，也不能代替独立制品消费者验证。
