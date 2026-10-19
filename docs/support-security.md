# 支持与安全

[返回指南](README.md) · 适用发布候选 `0.3.0-preview.2`

报告问题时记录以下身份及可复现步骤：

```text
SDK distribution version: 0.3.0-preview.2
Native SDK: 0.3.0
C ABI: 3/r1
Contract status: candidate (not published)
Artifact SHA-256: supplied by the local artifact owner
Platform and OS version: supplied by the App
Stable SDK error code: supplied by the failing call or operation result
```

可以附带经过清理的 SDK diagnostic；domain/native code 仅用于诊断。不要将客户内容、
完整像素、连接地址或凭据写入公共问题报告。SDK 包装不通过公共 API 暴露平台原生句柄。

当前候选的软件门禁涵盖 C 声明/导出/布局、Java/Swift 编译器 API、队列/取消/部分写入
和平台示例编译。独立本地消费者与精确制品摘要在发布准备阶段另行验证。
本轮没有新增真机蓝牙、纸面、固件状态协议或手机峰值 RSS 测量；hardware_verified=false。
旧内存报告仅属于对应旧版本，不能用来宣称 ABI3 已通过新内存验收。

本地候选不包含上传、签名或发布授权；公开历史检查器也不会执行这些动作。
SDK 的商业许可和第三方声明随分发保留，App 示例用途以对应源码许可说明为准。
