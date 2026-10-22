# API与生命周期

[返回指南](README.md) · 适用版本 `0.3.0-preview.11`

Android使用PrinterManager(Context)，iOS使用无参构造；每实例拥有一个设备连接。所有异步操作立即返回RequestId／RequestID，主线程交付一次ManagerResult。参数、状态、准备、接纳或执行失败均走同一结果；不要同步等待主线程回调。

调用方保存请求标识并按终态移除。结果保留outcome、code、message、completion、outputMayHaveOccurred和可选value。DATA_SENT只表示数据交付；未知状态保持未知。意外断连单独通知所属连接，主动disconnect使用自己的完成回调。

SDK 拥有独立输入副本或强持有不可变 UIImage，解码和准备在有界后台执行。所有设备IO共用同一native FIFO，Raw不旁路。取消是控制信号，取消终态不表示准备读者已退出；实际退出前资源仍计费，迟到结果不输出。超时由内部策略管理，不在开始发送时重新计时。

| 动作 | 效果 |
| --- | --- |
| cancel(requestId) | 请求取消本实例未完成工作，最终结果以回调为准。 |
| setPrinterModel(model, callback) | 仅连接后空闲允许，无IO；成功失效旧介质证据。 |
| disconnect(callback) | 清理当前连接，完成后可显式重连。 |
| close(callback) | 幂等永久关闭，之后业务请求无效。 |

断开和关闭先安排受影响请求终态并清理设备与扫描资源，再交付清理完成；不等待非合作解码读者返回，但保留其输入和额度直到实际退出。旧连接事件不影响新连接。SDK不自动重连、续打或重放。

Manager由App所有者持有，页面仅观察状态；重建不重提请求。共享Manager的页面离开只解绑自己的UI，独占所有者结束时异步close。选图与导入期间由App禁止换机或修改配置；后台策略明确执行，回前台不自动补印。Swift完成和断连闭包为@MainActor @Sendable，可直接更新MainActor所有者。
