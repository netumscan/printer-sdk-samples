# NETUM Printer SDK 接入指南

分发身份 **0.3.0-preview.2**，原生 SDK **0.3.0**，C ABI **3/r1**。
本版为开发预览；软件发布不代表硬件已认证。冻结声明和文档本身不作为在线发布证明，
集成时需核对精确版本的公开制品、commit、校验和及实际运行身份。

| 指南 | 内容 |
| --- | --- |
| [接入](integration.md) | 精确依赖与经过编译检查的 Android/iOS App 会话代码 |
| [API 与生命周期](api-lifecycle.md) | 队列、结果回调、状态订阅和资源释放 |
| [连接与打印配置](configuration.md) | 独立连接、每次打印 Profile、能力与未知状态 |
| [错误码与排障](errors-troubleshooting.md) | 接纳失败、任务终态和诊断 |
| [兼容性](compatibility.md) | ABI3 主版本边界和候选身份 |
| [迁移](migration.md) | 从 ABI2 迁移到独立连接及不可变作业 |
| [支持与安全](support-security.md) | 版本记录、诊断和验证边界 |
| [Android Java API](android-java-api.md) | 精确编译器声明与逐项说明 |
| [iOS Swift API](ios-swift-api.md) | 精确编译器声明与逐项说明 |

同一设备的打印、Raw、介质配置和走纸均经过同一个 FIFO；App 无需先查询是否忙碌。
`DATA_SENT` 只表示数据传输完成。当前硬件查询未验证，显示未知，`hardware_verified=false`。

`api-history.json` 将候选冻结契约与旧版本的真实发布历史分开，明确
`contract_status=candidate`、`published_contract_verified=false`。运行以下 source-free
检查器可验证文档、声明和旧证明，不需要 SDK 实现源码或 Git：

```sh
python3 tools/check-api-reference.py
python3 tools/check-api-history.py
```

这两个检查不会签名、联网、安装 App 或执行打印机 IO，也不能代替独立制品消费者验证。
