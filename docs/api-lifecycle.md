# API 与生命周期

[返回指南](README.md) · 适用版本 `0.3.0-preview.17`

App 的稳定 owner 先创建 `PrinterManager` 并注册接收者：Android `setListener(this)`，
iOS `manager.delegate = self`。Android owner 实现 `PrinterListener`，iOS owner 实现
类约束、`@MainActor` 的 `PrinterDelegate`。公共操作不再逐调用传闭包或 Callback，立即返回
`RequestId`／`RequestID`，异步在主线程／MainActor 通知完整 `ManagerResult`。

| 事件 | Android Listener | iOS Delegate 方法标签 |
| --- | --- | --- |
| 实时设备发现 | `onDeviceDiscovered(scanId, device)` | `printerManager(_:didDiscover:scanID:)` |
| 扫描终态 | `onScanCompleted` | `printerManager(_:didFinishScanning:)` |
| 连接终态 | `onConnectionCompleted` | `printerManager(_:didFinishConnecting:)` |
| 图片打印终态 | `onPrintCompleted` | `printerManager(_:didFinishPrinting:)` |
| 主动断开终态 | `onDisconnectCompleted` | `printerManager(_:didFinishDisconnecting:)` |
| 永久关闭终态 | `onCloseCompleted` | `printerManager(_:didFinishClosing:)` |
| 状态更新 | `onStatusChanged` | `printerManager(_:didUpdateStatus:)` |
| 意外断连 | `onDisconnected` | `printerManager(_:didLoseConnection:)` |

其他动作也有对应的类型化终态，详见完整 Java／Swift API。所有结果保留请求 ID、outcome、code、message、
completion、`outputMayHaveOccurred` 和可选 value；参数、状态、准备、接纳和执行失败仍按原请求报告。
业务按 ID 匹配并移除待处理请求；意外断连携带所属连接请求 ID，主动 disconnect 有自己的请求 ID。
不要同步等待主线程通知。

## 扫描与展示

`scanBle()`／`scanBLE()` 返回本轮请求 ID。实时通知携带同一 ID，每个唯一设备首见通知一次；
App 立即将候选加入可见列表。终态成功列表包含最终合并的名称、信号等信息，App 按地址／UUID 刷新并保留有效选择。
取消或失败不会把已显示的部分候选变成成功列表。停止扫描调用 `cancel(scanId)`，仍等待一次真实终态。
App 过滤旧轮次和自身已停止显示的事件，SDK 也隔离取消、关闭和旧接收者的迟到通知。
设备发现不自动连接；连接也不会自动停止扫描，App 应明确安排停止和连接顺序。
Android `bondedDevices()` 返回系统已配对列表；iOS 不提供经典蓝牙列表或 SPP。

## 接收者与清理

Android Manager 强持有当前 Listener；iOS Manager 弱持有 Delegate，因此 App 必须保持 owner 存活。
通知对仍有效接收者至多交付一次。替换、解除或弱 Delegate 释放使旧注册尚未交付的通知失效，不改投新对象，
不补交历史，也不取消任务；已进入的通知可以结束。解除使用 `setListener(null)`／`delegate = nil`。
要等待关闭结果，保留 owner 和接收者直到自己的关闭终态，之后 Manager 释放接收者。

`cancel(id)` 表达取消意图；终态不代表非合作解码读者已退出，实际退出前仍占资源。
disconnect／close 先结束受影响请求并清理设备和扫描，再通知清理结果；disconnect 后可显式重连，
close 幂等且永久，之后业务请求仍按公共错误规则拒绝。所有设备 IO 共用 FIFO，Raw 不旁路。
SDK 不自动重连、续打、重放或因 UI 重建重新提交请求。

页面观察稳定 owner 的状态；共享 owner 的页面离开只解绑 UI，不拆 SDK 接收者来等待清理。
后台策略由 App 明确执行，返回前台不自动扫描、重连或补印。
状态按四个可选字段去重；重复查询仍各有独立请求终态。查询写出后超时、取消或坏回复保守断开，
隔离没有请求编号的迟到回复。自动 INFO 最多额外等待 1 秒；软失败保留可用连接和打印，
但本连接后续 wire 查询不可用，需用户显式重连。
