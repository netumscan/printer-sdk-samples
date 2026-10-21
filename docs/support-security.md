# 支持与安全

[返回指南](README.md) · 适用版本 `0.3.0-preview.10`

报告问题时记录以下身份及可复现步骤：

```text
SDK distribution version: 0.3.0-preview.10
Native SDK: 0.3.0
C ABI: 3/r2
Contract status: read from the exact distribution manifest
Artifact SHA-256: supplied by the local artifact owner
Platform and OS version: supplied by the App
Stable SDK error code: supplied by ManagerResult
Completion and possible output: supplied by ManagerResult
```

通过 `manager.sdkInfo` 的完成回调读取运行库身份。不要将客户内容、完整像素、连接地址、
凭据或未经清理的日志写入公共问题报告。移动公开 API 不暴露平台原生句柄。

当前软件门禁检查 C 声明/导出/布局、Java/Swift 公共 Manager API、取消、部分写入、设备 FIFO、
清理屏障与平台示例。独立本地消费者及精确制品摘要另行验证；旧在线版本的报告不作为当前开发版本验收。
本轮没有新增真机蓝牙、纸面效果、固件状态协议或手机峰值 RSS 测量，`hardware_verified=false`。

本地构建与文档检查不包含上传、签名或发布授权；开发契约必须另经候选审查才能发布。
SDK 商业许可和第三方声明随分发保留，App 示例以对应源码许可说明为准。
