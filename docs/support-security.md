# 反馈、安全与许可

[返回指南](README.md) · 适用 SDK `0.2.0-preview.4`

## 提交接入问题

可在 [printer-sdk-samples Issues](https://github.com/netumscan/printer-sdk-samples/issues)
提交已脱敏的 SDK 接入或 Demo 问题。这里是公开讨论区，请先检查已有问题及
[已知限制](compatibility.md#已知限制)。当前未承诺固定响应时限或商业支持 SLA。

建议包含以下信息：

```text
SDK distribution version: 0.2.0-preview.4
Platform / OS / CPU architecture:
SDK public release commit / artifact SHA-256:
Demo source fingerprint / build version:
Printer model / firmware (manually supplied, redact identifiers):
Transport / protocol / paper / DPI / printable width / page budget:
Steps to reproduce (including cancel / disconnect / reconnect order):
Expected result / observed result:
Operation terminal state / error code / completion / sideEffectsPossible:
bytesSent / bytesSentExact / bytesTotalKnown or optional total:
Reproduces in Quick Start / Full Demo / minimal application:
hardware_verified: false
```

可附统一兼容诊断记录及少量必要日志。没有可用值时写明“未知”，不要猜测设备能力或固件。
不要公开客户图片、订单、标签正文、完整 Raw 命令、设备地址、序列号、账户信息、密钥、
证书、签名配置或未脱敏全量日志。用人工生成的测试图替代真实客户内容。

## 安全问题

这些仓库当前没有文档化的专用安全邮箱或已确认启用的 GitHub 私密漏洞报告入口，
因此不要把漏洞细节、利用代码或敏感日志放入公开 Issue。
请先使用你与 NETUM 已有的私下联系渠道确认接收方式；若没有，可在公开 Issue 仅请求
建立私下安全沟通渠道，不披露漏洞细节。本指南不虚构安全通报渠道或响应承诺。

## 隐私与本地数据

SDK 不包含分析、广告或遥测上传；打印内容经用户选择的打印机传输路径发送。
宿主应用仍需负责自身业务数据、权限说明、隐私披露和分享行为。不要把 SDK 的无遥测行为
当成对宿主应用或打印机固件的隐私保证。

Full Demo 的图片处理、本地配置和有界日志由用户操作触发。日志导出需显式选择，
Raw 输入及其载荷不写入日志、不保存到配置、不自动恢复或发送。导出的文件由用户自行保管，
分享前应再次脱敏；清除本机日志不会撤销已经导出或分享的副本。

iOS 包提供 `PrivacyInfo.xcprivacy`，对超时计时使用的系统启动时间 API 声明理由 `35F9.1`。
App 应保留随包的隐私资源，并根据自身其他 API、权限和数据用途完成发布披露。
不要为排障要求用户提供 GPG 口令、开发证书、签名私钥或 Apple / Maven / GitHub 凭据。

## 许可边界

SDK 为闭源商用软件，适用
[Android SDK LICENSE](https://github.com/netumscan/printer-sdk-android/blob/main/LICENSE) 或
[iOS SDK LICENSE](https://github.com/netumscan/printer-sdk-ios/blob/main/LICENSE)。
商业应用免费集成及随应用分发的范围、设备使用限制和其他义务以对应版本 LICENSE 为准。

[printer-sdk-samples](https://github.com/netumscan/printer-sdk-samples) 明确授权的示例源码、资源和
工程配置采用 MIT。**MIT 不适用于 SDK 包装层、C 头文件、原生实现、二进制或第三方组件。**
不要从示例仓库的 MIT 推导出 SDK 源码权利。重新分发时保留 SDK 许可、隐私资源和第三方声明。
