# 兼容性与分发身份

[返回指南](README.md) · 适用版本 `0.3.0-preview.10`

当前 Manager 移动 API 与旧移动 API 不兼容，应用必须重新编译并迁移；不能继续公开使用旧的设备、
operation、image/job、订阅或 scanner 控制对象。应用使用同一版本的 Java/Swift 包装和 native 库。

本轮移动 API 收敛没有改变 native `0.3.0` / C ABI `3/r2`：生产仍为一个 `printer_sdk.h`、
27 个动态 C 导出、23 个 Android JNI 导出及 25 个具体结构。C SDK 的独立消费者继续使用已冻结的
句柄与 snapshot/result 契约；移动端隐藏内部类型不等于删除 C 符号。
自然对齐结构不得 packed，header inline 初始化器不在动态导出表中。

C ABI3 与 ABI2 不兼容；ABI3/r1 的既有结构布局和 25 个入口仍由 r2 保留，r2 额外提供两个
携带调用诊断的入口。需要 r2 行为的 native 消费者检查 major==3、revision>=2。
移动 API、C ABI、SDK 分发版本和硬件验证是独立概念，不能只凭其中一个推断兼容。

`0.3.0-preview.10` 的状态以随包契约为准。开发身份不是发布候选；冻结候选也不等于已上传制品。
公开历史的 anchors/proofs/seals、旧候选、旧开发快照和 SDK lock 保持独立，不能为新 API 重写旧 pins。
旧在线 Samples 和 consumer 模板按原版本冻结，不会自动升级到新 Manager。

`manager.sdkInfo` 异步提供 native/distribution 版本及可选 buildID；ABI身份由分发契约记录，移动结果不公开ABI字段。
buildID 未知时明确显示未知；它不能替代 SHA-256 或独立消费者验证，软件测试不提升 hardware_verified。
