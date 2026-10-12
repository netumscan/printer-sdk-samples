# Printer SDK 客户指南

适用分发版本 **0.2.0-preview.3**，原生核心 SDK **0.2.0**，C ABI **2/r10**。
Android 与 iOS 使用同一原生执行器，平台 API 名称和连接方式有所不同。

| 要解决的问题 | 文档 |
| --- | --- |
| 添加依赖、选择平台、申请权限 | [在线接入与权限](integration.md) |
| 连接、提交、取消、关闭及能力查询 | [API 与生命周期](api-lifecycle.md) |
| 查询 Android 所有公共类型、方法、字段和常量 | [Android Java API 参考](android-java-api.md) |
| 查询 iOS 所有公共类型、成员、默认参数及枚举值 | [iOS Swift API 参考](ios-swift-api.md) |
| 设置 DPI、可打印宽度、标签页高和小票预算 | [图像、纸张与协议配置](configuration.md) |
| 根据错误码定位失败、避免重复出纸 | [错误码与排障](errors-troubleshooting.md) |
| 判断已验证范围、解读兼容诊断记录 | [兼容性与诊断](compatibility.md) |
| 提交问题、脱敏日志、安全与隐私 | [反馈、安全与许可](support-security.md) |
| 从旧包名或旧预览版本升级 | [迁移与版本政策](migration.md) |

可运行工程位于 [printer-sdk-samples](https://github.com/netumscan/printer-sdk-samples)：
Android 与 iOS 均提供 Quick Start 和 Full Demo。Quick Start 用于理解最小调用流程；
Full Demo 增加图片处理、显式配置、取消、Raw 和本地诊断。
请使用与目标 SDK 版本匹配的示例版本，并核对其 `sdk-lock.json`。

完整参考对应已审定的 Java／Swift 编译 API 基线；每个公共 API 均有签名与行为说明。
在仓库根目录执行 `python3 tools/check-api-reference.py` 可检查覆盖、基线摘要及生成文档
一致性。检查器可离线运行；它检查文档与审定基线的对应关系，不代替编译、在线制品运行
或硬件验收。`docs/api-reference.json` 保存基线与说明，不包含 SDK 实现。

这是开发预览，**`hardware_verified=false`**。构建、签名校验和模拟器通过不能证明
某台打印机、固件、纸张或手机组合已完成验收。`DATA_SENT` 仅表示数据传输完成；
连接中断后 SDK 不自动续打、重放或重试。当前限制和确认方法见[兼容性](compatibility.md)。

SDK 适用各分发仓库的闭源商用 LICENSE；MIT 仅适用于示例仓库明确授权的示例材料。
详见[许可边界](support-security.md#许可边界)。
