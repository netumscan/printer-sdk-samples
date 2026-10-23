# 兼容性与分发身份

[返回指南](README.md) · 适用版本 `0.3.0-preview.16`

当前移动API相对 `.11` 增加设备信息与统一状态监听，并将 queryStatus 替换为 getStatus；调用方需要重新编译并按[迁移](migration.md)接入。应用成套使用同一版本的Java／Swift包装和native库。

native `0.3.0`／C ABI `3/r3`：生产使用单一printer_sdk.h、28个动态C导出、23个Android JNI导出和27个具体结构；header inline初始化器不在动态导出表中。r3保留r1/r2入口和自然对齐布局，利用原execute union的预留空间加入INFO描述，并增加psdk_operation_get_printer_info；需要新INFO功能时要求major==3、revision>=3。C ABI3与ABI2不兼容；不得混用不同身份的头、库、包装和句柄。

本次冻结当前r3，不增加r4，不调整现有ConnectionInfo。移动API、C ABI、SDK分发版本和硬件验证分别核对。

`0.3.0-preview.16` 的状态以随包契约为准。冻结候选不等于制品已上传，实际发布以精确制品及在线收据确认。公开历史的anchors／proofs／seals、旧候选、旧开发快照和旧SDK lock保持独立，不能为新API重写旧pins。旧在线Samples和消费者使用各自精确版本，回退选择完整旧版本及匹配依赖锁；不移动旧标签，不覆盖旧二进制，不自动重放失败打印。
