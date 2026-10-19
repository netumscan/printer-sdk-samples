# 兼容性与候选身份

[返回指南](README.md) · 适用发布候选 `0.3.0-preview.2`

本版 native `0.3.0`、C ABI `3/r1` 与 ABI2 不兼容。必须成套更新公共头、原生库、JNI/Swift
包装并重新编译，不能混用句柄、只替换某个文件，或依赖旧符号别名/兼容 shim。
原生消费者先检查 major==3、revision>=1，再调用结构体/句柄入口。

生产只有一个 printer_sdk.h、25 个动态 C 导出、25 个具体结构及 header inline 初始化器。
初始化器不在动态导出表中；自然对齐结构不得 packed。输入初始化不提供业务默认值，
聚合结构需分别初始化选中嵌套输入；输出仅初始化最外层，由 SDK 填写嵌套输出。

`0.3.0-preview.2` 是独立冻结的发布候选，`contract_status=candidate`、
`published_contract_verified=false`。声明校验通过不表示 AAR/XCFramework 已上传，
不应将候选摘要写成真实公开发布证明。后续精确制品版本、commit 和 SHA-256 分别核验。

随包 api-history.json 保留原 ABI2 的不可变发布证明，并单独检查新候选声明；不修改旧
anchors/proofs/seals、不将本候选加入 published_releases。API 参考采用新 Java/Swift
编译器基线，旧声明不会被静默沿用。

sdkInfo 提供 native/distribution 版本及可选 buildID。buildID 未知时明确标记未知；
它不能替代制品 SHA-256 或独立消费者验证。软件测试不提升 hardware_verified。
