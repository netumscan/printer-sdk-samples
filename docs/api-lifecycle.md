# API 与生命周期

[返回指南](README.md) · 适用 SDK `0.2.0-preview.4`

## 常用入口

| 操作 | Android | Swift |
| --- | --- | --- |
| CPCL 配置 | `CpclProfile` | `CpclProfile` |
| ESC/POS 配置 | `EscPosProfile` | `ReceiptProfile` |
| 创建打印设备 | `MobilePrinter.spp(...)` / `.ble(...)` | `MobilePrinter(bleIdentifier:options:profile:)` |
| 连接与断开 | `connect(timeoutMs)` / `disconnect()` | `connect(timeoutMilliseconds:)` / `disconnect()` |
| 本地连接状态 | `connectionState()` | `connectionState()` |
| 能力及证据 | `feature(featureId)` | `feature(_:)` |
| 标签图像 | `submitLabelGray8(...)` | `submitGray8(...)` |
| 小票图像 | `submitReceiptGray8(...)` | `submitReceiptGray8(...)` |
| 完整 Raw 流 | `sendRaw(bytes, copies, timeoutMs)` | `submitRaw(_:copies:timeoutMilliseconds:)` |
| 设置介质 | `setMedia(mode, timeoutMs)` | `setMediaMode(_:timeoutMilliseconds:)` |
| 定位下一标签 | `locateNextLabel(timeoutMs)` | `feedToNextLabel(timeoutMilliseconds:)` |
| 状态请求 | `queryStatus(timeoutMs)` | `queryStatus(timeoutMilliseconds:)` |
| 释放打印设备 | `close()` / `AutoCloseable` | `close()` |

表中提交和控制方法返回 `PrintOperation`；是否可执行仍取决于所选协议、已实现能力和参数。
当前标准后端未实现状态读取及切刀，调用不会凭空获得设备状态。未知能力不应当作已支持。

## 所有权、线程与关闭

应用持有 `MobilePrinter` 至会话结束，持有每个 `PrintOperation` 至所需结果读取完毕。
提交校验并入队后立即返回，不等待发送或物理打印；图像及 Raw 载荷复制至原生不可变作业。
同一打印设备的图像、Raw、介质和定位命令共用一个串行 IO 队列，无需建立绕过队列的写入通道。

`connect`、`disconnect`、设备 `close` 在工作线程执行。快照由应用在后台定时读取，UI 更新
派发回主线程。不要为显示进度紧密循环；取消或断开使用独立控制路径，不能排在正在阻塞的
连接或应用等待循环之后。SDK 不负责应用 UI 调度或持久任务历史。

| 调用 | 语义 |
| --- | --- |
| `operation.cancel()` | 请求协作取消，不能撤回已被传输接受的字节；请求后仍需读取终态 |
| `operation.close()` | 释放结果引用，**不会取消**已经入队的任务；释放后不能继续访问该 operation |
| `printer.disconnect()` | 停止会话中的工作并断开；实例保留，后续可显式重新连接 |
| `printer.close()` | 断开、等待原生释放并永久关闭实例；可重复调用，关闭后需新建实例 |

独立 operation 结果在打印设备关闭后仍可读取，直到 operation 自己被关闭。
提前关闭 operation 会失去该结果的读取入口；应先保存终态或明确不再需要结果。
取消、断开和失败后不自动重连、重放或恢复任务。重新发送必须由用户在核对纸面后明确决定。

Android 旧的 `submitGray8` / `submitRaw` 等任务 ID 便利接口仍保留，通过
`pollTaskEvents()` 读取并回收终态，最多保留 64 项未回收任务。新接入优先使用显式
`PrintOperation`，不要混用便利任务 ID 与 `Snapshot.id` 作为跨设备标识。

## 快照与完成

Android 读取 `operation.snapshot()`；Swift 使用 `try operation.snapshot()`。
Android 的 `terminal()` 与 Swift 的 `isTerminal` 判断终态。

| 状态值 | 含义 |
| --- | --- |
| `0` | `QUEUED`，已入队 |
| `1` | `PREPARING`，准备中 |
| `2` | `SENDING`，发送中 |
| `4` | `COMPLETED` |
| `5` | `FAILED` |
| `6` | `CANCELLED` |

终态成功的 `completion=1` 为 `DATA_SENT`，只证明字节传输。`completion=2`
为 `DEVICE_CONFIRMED`，需要实际支持的协议确认机制；当前移动端提交要求的是 `DATA_SENT`。
失败/取消时检查错误码及 `sideEffectsPossible`；该字段为 true 表示可能产生副作用，
不能据此断言完整出纸，也不能自动重发。

`bytesSent` 是传输计数，精确性由 Android `bytesSentExact()` / Swift `bytesSentExact`
表示。Android 仅在 `bytesTotalKnown()` 为 true 时使用 `bytesTotal`；Swift 对未知总量返回
`nil`。未知总量不是 0%，传输计数不是纸张页数或机械打印进度。
`connectionState()` 是本地缓存，`CONNECTED` 不证明无线仍存活或打印机已就绪。

## FeatureSupport

Android `MobilePrinter.feature(int)` 与 Swift `MobilePrinter.feature(_:)` 返回以下信息：

| 字段 | 数值与含义 |
| --- | --- |
| `implementation` | `0` 未实现；`1` 原生实现；`2` 由宿主渲染 |
| `deviceSupport` | `0` 未知；`1` 明确不支持；`2` 明确支持 |
| `evidence` | 位掩码：`1` 配置声明，`2` 设备报告，`4` 硬件验收；不得把声明升级为实测 |

Android `isImplemented()` 与 Swift `isImplemented` 仅检查实现非零。
Android `isSupported()` 与 Swift `isSupported` 还要求 `deviceSupport=2`。
因此 false 可能是**未知**，应同时检查原始字段。当前显式传输配置的设备支持通常为未知；
选择 M90 开发预设不会生成已认证的产品档案。

```java
import com.netumscan.printersdk.FeatureSupport;
import com.netumscan.printersdk.MobilePrinter;

FeatureSupport support = printer.feature(MobilePrinter.FEATURE_IMAGE);
// 三个字段一起记录；仅 isImplemented() 为 true 不证明硬件支持。
int implementation = support.implementation;
int deviceSupport = support.deviceSupport;
int evidence = support.evidence;
```

```swift
let support = try printer.feature(.image)
let implementation = support.implementation
let deviceSupport = support.deviceSupport
let evidence = support.evidence
```

Android 的 `implementsFeature(int)` 保持兼容，并仍只回答实现情况。Android 未知 feature ID 返回
`0/0/0`；关闭中的设备或已关闭实例返回错误 `1002`。完整错误解释见[错误码](errors-troubleshooting.md)。
