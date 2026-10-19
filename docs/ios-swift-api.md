# iOS Swift API 参考

发布候选版本 **0.3.0-preview.2**；原生 SDK **0.3.0**；C ABI **3/r1**。

本页逐项覆盖审定的编译器公共 API 基线，共 **349** 个文档条目。
声明、JVM descriptor、常量、枚举和编译器关系均来自基线，说明按稳定文档 ID 维护。
机器可读的完整基线与说明见 [api-reference.json](api-reference.json)；运行
`python3 tools/check-api-reference.py` 可检查覆盖范围、审定 SHA-256 和本页字节一致性。

基线 SHA-256：`7e2a51b45f07800a08a180161ad6bfff71b19bc2fd46dc70d0d124c74a44416c`

运行约束与错误处理见 [API 与生命周期](api-lifecycle.md) 和 [错误码与排障](errors-troubleshooting.md)。
本页是未发布的发布候选契约，不代表发布制品或公开历史证明。硬件验收仍暂缓，`hardware_verified=false`。

## 类型导航

- [BLECreditFlowControl](#api-cf29222de3a9439ea9d9)
- [BLEOptions](#api-5407994e1f7a0684ecd6)
- [BLEScanner](#api-b3a1ce59859e4cf66609)
- [ConnectionState](#api-7b987515d7ad703321ec)
- [CpclProfile](#api-d94c01c25a5ac00d3af5)
- [CutMode](#api-ca87677d19e3423fb23c)
- [DeviceActiveOperation](#api-a4469ccf35ccf99bbc8f)
- [DeviceStateSnapshot](#api-8ca6a77eedc34c565166)
- [DeviceWorkState](#api-54f3a6b00a3988e4c18e)
- [DiagnosticSource](#api-2e47e1cecb9f6ee6289d)
- [DiscoveredPrinter](#api-e7eba043862754865338)
- [ErrorDiagnostic](#api-33ee6f34f5b5b231c733)
- [FeatureSupport](#api-63244971bb8665f0438c)
- [FinishScope](#api-8da32930540a97e6bfd9)
- [HardwareState](#api-712e0c363892c35925d7)
- [HardwareStatusResult](#api-806056f7885dc5612709)
- [ImageAlgorithm](#api-41361c67b37b815a38ae)
- [LabelJobOptions](#api-9cd007436b5a242686f9)
- [M90Profile](#api-c09da0bc38ed43e7aef2)
- [MediaMode](#api-e9f6609ce460a51cc89f)
- [MediaRequirement](#api-623f02309548c44d0ebb)
- [MediaVerificationPolicy](#api-afcd3a74fc08e909f85d)
- [MobilePrinter](#api-ac14c812144bc8d503ff)
- [ObservationSubscription](#api-b2adb08956dca46c7ae5)
- [OperationResult](#api-afec0bb5086d128fee42)
- [OperationSnapshot](#api-57bb815c8787c72b8ed3)
- [PixelFormat](#api-145004af19a837676c99)
- [PrintJob](#api-853bae839e6f8b53cd97)
- [PrintOperation](#api-fcb644f7b0eb6c8a6489)
- [PrinterCapabilities](#api-51947abf7e628a2d3bc3)
- [PrinterConstraint](#api-1f9f816f9d0db63d9e95)
- [PrinterFeature](#api-67c88d53ad46bc6679a5)
- [PrinterGeometry](#api-b511b313f54808d516b6)
- [PrinterImage](#api-ed1435ddffbdea0f0ae1)
- [PrinterManager](#api-1d5ad0ccf40ef0036743)
- [PrinterProtocol](#api-7c7a9dd0ee08114f96bd)
- [PrinterSDK](#api-1d012fc82f6300f83d19)
- [PrinterSDKError](#api-7eade5ddc7d13cb74d9a)
- [ReceiptFinishOptions](#api-7a656acf1cbdc7b0f7ad)
- [ReceiptJobOptions](#api-082376dfdfb0bb2ec7b4)
- [ReceiptProfile](#api-8a4489e38f48eaabb599)
- [ResourceSnapshot](#api-ef876e2ccd4fa79506c8)
- [SDKInfo](#api-313c383cb33723929d18)
- [ScalarConstraint](#api-5c2f7f6243349bf78f99)
- [StatusQueryConfiguration](#api-4b76054d7c5874efb333)

公共模块为 `PrinterSDK`，C 桥接模块为 `CPrinterSDK`。
本基线使用 iOS 15 arm64；符号附带的 Foundation 可用性不表示其他平台已完成分发或验收。
未单列 availability 的符号以 SDK 的最低 iOS 版本 15 为接入前提。

<a id="api-cf29222de3a9439ea9d9"></a>

## BLECreditFlowControl

<a id="api-6524be748f6e725d4fc5"></a>

### BLECreditFlowControl

同一服务中 Data Transmission V1.2 的通知特征配置。

```swift
struct BLECreditFlowControl
```

保存 `dataOut` 和 `dataFC` UUID；通过 `BLEOptions.creditFlowControl` 启用，写入特征由 `BLEOptions` 单独配置。只有与固件协议匹配时使用，不自动探测或认证设备。构造本身不订阅通知或进行 IO。

文档 ID：`apple:symbol:s:10PrinterSDK20BLECreditFlowControlV`

符号种类：`swift.struct`；访问级别：`public`。

<a id="api-8eb334e5f5223bd3125b"></a>

### BLECreditFlowControl.dataFC

提供 V1.2 接收 Credit 和包长信息的通知特征 UUID。

```swift
let dataFC: UUID
```

传输根据有效流控信息限制发送，没有可用 Credit 时等待，等待受取消和总截止时间约束。它只管理接收容量，不是打印完成确认。与 Data Out、写入特征的互异条件由 `BLEOptions` 校验。

文档 ID：`apple:symbol:s:10PrinterSDK20BLECreditFlowControlV6dataFC10Foundation4UUIDVvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UUID | `s:10Foundation4UUIDV` |

<a id="api-875e230d2b4289113cc0"></a>

### BLECreditFlowControl.dataOut

选定服务中的 Data Out 通知特征 UUID。

```swift
let dataOut: UUID
```

用于启用 V1.2 要求的通知路径。当前适配器不把此特征通知作为公开状态、读回或打印完成结果；不能以收到通知证明标签已打印。与写入、Data FC 特征的互异条件由 `BLEOptions` 校验。

文档 ID：`apple:symbol:s:10PrinterSDK20BLECreditFlowControlV7dataOut10Foundation4UUIDVvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UUID | `s:10Foundation4UUIDV` |

<a id="api-2d3b7593669608215aae"></a>

### BLECreditFlowControl.init(dataOut:dataFC:)

保存 Data Out 和 Data FC 通知特征 UUID。

```swift
init(dataOut: UUID, dataFC: UUID)
```

两个参数均必填、无默认值；本初始化不抛出异常。与写入特征互异及只允许无响应写入的条件在 `BLEOptions` 初始化时校验。连接时还必须确认特征存在且具有所需通知能力。

文档 ID：`apple:symbol:s:10PrinterSDK20BLECreditFlowControlV7dataOut0F2FCAC10Foundation4UUIDV_AHtcfc`

符号种类：`swift.init`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UUID | `s:10Foundation4UUIDV` |

<a id="api-5407994e1f7a0684ecd6"></a>

## BLEOptions

<a id="api-77d41a22c85fa7300778"></a>

### BLEOptions

不可变的 BLE 服务、写入方式和可选流控配置。

```swift
struct BLEOptions
```

应用显式配置与目标固件一致的服务和特征；这些值不自动识别型号，也不构成硬件验证。所有字段在初始化时保存，通过 `MobilePrinter` 使用。当前前台 BLE 传输不提供平台原生句柄，权限和前后台策略由应用负责。

文档 ID：`apple:symbol:s:10PrinterSDK10BLEOptionsV`

符号种类：`swift.struct`；访问级别：`public`。

<a id="api-5ed7463fa203f0207e35"></a>

### BLEOptions.creditFlowControl

可选的 Data Transmission V1.2 Credit 流控配置。

```swift
let creditFlowControl: BLECreditFlowControl?
```

默认 `nil` 表示未启用该流控；非空时使用 Data Out/Data FC 通知，并要求无响应写入及三个互异特征。Credit 表示接收容量，不是应用层逐包 ACK、纸张状态或打印完成确认。其等待受原操作超时及取消约束；不把通知载荷作为公开状态结果。

文档 ID：`apple:symbol:s:10PrinterSDK10BLEOptionsV17creditFlowControlAA09BLECrediteF0VSgvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| BLECreditFlowControl | `s:10PrinterSDK20BLECreditFlowControlV` |

<a id="api-52654e007a72061d6748"></a>

### BLEOptions.init(service:writeCharacteristic:withResponse:maxWriteBytes:creditFlowControl:)

校验并构造明确的 BLE 写入配置。

```swift
init(service: UUID, writeCharacteristic: UUID, withResponse: Bool, maxWriteBytes: Int, creditFlowControl: BLECreditFlowControl? = nil) throws
```

`service` 为包含各目标特征的服务 UUID，`writeCharacteristic` 为写入特征 UUID；`withResponse` 选择 GATT 有响应/无响应写入，无默认值。`maxWriteBytes` 为每次平台写入的应用上限，单位字节，必须在 1…512；实际包长还受系统和固件流控容量限制。`creditFlowControl` 默认 `nil`；非空时仅允许 `withResponse == false`，且写入、Data Out 和 Data FC 三个特征 UUID 必须互不相同。上述条件不满足抛 1001。初始化只校验配置，不发现服务、不连接、不确认固件支持；特征能力在连接时验证，失败可抛 1004。

文档 ID：`apple:symbol:s:10PrinterSDK10BLEOptionsV7service19writeCharacteristic12withResponse13maxWriteBytes17creditFlowControlAC10Foundation4UUIDV_AKSbSiAA09BLECreditmN0VSgtKcfc`

符号种类：`swift.init`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| BLECreditFlowControl | `s:10PrinterSDK20BLECreditFlowControlV` |
| Bool | `s:Sb` |
| Int | `s:Si` |
| UUID | `s:10Foundation4UUIDV` |

<a id="api-4aafe93560df3b56ca06"></a>

### BLEOptions.maxWriteBytes

每次 BLE 平台写入的应用载荷上限。

```swift
let maxWriteBytes: Int
```

单位字节，初始化接受 1…512。实际包长取应用上限、平台最大写入长度，以及启用 Credit 时固件公告包长等约束的最小值；此值不是协商 MTU，也不表示每次都会写满，不应从型号名称推断。

文档 ID：`apple:symbol:s:10PrinterSDK10BLEOptionsV13maxWriteBytesSivp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Int | `s:Si` |

<a id="api-a69cec0ede2b2be6566e"></a>

### BLEOptions.service

连接后查找目标写入与流控特征的服务 UUID。

```swift
let service: UUID
```

由初始化参数明确提供，无隐式型号默认值。配置正确性仍须由实际服务发现和目标固件确认；发现广播中未列出此服务不等于连接后不存在。

文档 ID：`apple:symbol:s:10PrinterSDK10BLEOptionsV7service10Foundation4UUIDVvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UUID | `s:10Foundation4UUIDV` |

<a id="api-ab9670d691ab1ad3b3c0"></a>

### BLEOptions.withResponse

选择是否使用 GATT 有响应写入。

```swift
let withResponse: Bool
```

true 请求 `.withResponse`，false 请求 `.withoutResponse`。使用 Credit 流控时必须为 false；构造器拒绝相冲突配置。有响应写入的完成回调只表示该层写入完成，不能推断打印机已解析命令或机械打印完成。

文档 ID：`apple:symbol:s:10PrinterSDK10BLEOptionsV12withResponseSbvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Bool | `s:Sb` |

<a id="api-0e1e809f584a36f95939"></a>

### BLEOptions.writeCharacteristic

用于发送打印载荷的 BLE 特征 UUID。

```swift
let writeCharacteristic: UUID
```

该特征位于配置的 `service` 中，须支持 `withResponse` 所选写入方式。启用 Credit 时须与 `dataOut`、`dataFC` 不同。它不是读取状态或确认机械打印完成的接口。

文档 ID：`apple:symbol:s:10PrinterSDK10BLEOptionsV19writeCharacteristic10Foundation4UUIDVvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UUID | `s:10Foundation4UUIDV` |

<a id="api-b3a1ce59859e4cf66609"></a>

## BLEScanner

<a id="api-7935f69ef4752ec50e2b"></a>

### BLEScanner

前台 BLE 设备发现器。

```swift
final class BLEScanner
```

扫描通过异步完成回调返回，不阻塞调用线程；一次仅允许一个扫描。应用持有扫描器直到完成，可用 `stop` 取消；完成后可再次扫描。应用负责用途说明、权限与前后台策略。发现不连接设备，不判断协议/型号，也不验证打印机硬件；没有原生外设句柄暴露。 Swift 跨隔离访问采用 Sendable 契约，内部同步与原生存储生命周期保护保持；可变 App 状态须自行同步，阻塞 IO 仍应在后台执行。

文档 ID：`apple:symbol:s:10PrinterSDK10BLEScannerC`

符号种类：`swift.class`；访问级别：`public`。

<a id="api-d0a645c918661744d908"></a>

### BLEScanner.init()

创建一个可复用的前台扫描器。

```swift
init()
```

无参数，不抛出异常；创建对象不立即扫描或连接设备。调用 `scan` 才启动一个有限扫描窗口。应用应保留此对象，结束页面或进入后台时显式 `stop`。

文档 ID：`apple:symbol:s:10PrinterSDK10BLEScannerCACycfc`

符号种类：`swift.init`；访问级别：`public`。

<a id="api-1ac780fde7fe9f8c6153"></a>

### BLEScanner.scan(seconds:service:completion:)

异步扫描一个有限窗口并在主队列交付一次结果。

```swift
func scan(seconds: TimeInterval = 10, service: UUID? = nil, completion: @escaping @Sendable (Result<[DiscoveredPrinter], PrinterSDKError>) -> Void)
```

`seconds` 默认 `10`，单位秒，必须有限且在 `(0, 60]` 内，窗口包含蓝牙初始化等待；无效值通过主队列回调 `.failure(PrinterSDKError(1001))`，方法不抛出。`service` 默认 `nil`，表示不按单一服务过滤；传 UUID 时要求广播匹配，可能排除没有在广播列出服务的设备。`completion` 无默认值；每次请求只调用一次，且始终派发到主队列，可在回调中再次启动扫描。已有活动扫描时新请求以 1002 失败，不中断原扫描。

正常窗口结束返回 `.success`，可以是空数组；按 UUID 合并后保留首次发现顺序，名称、服务与 RSSI 更新。停止以 1005 失败；蓝牙关闭、权限不足或扫描期间状态失效以 1002 失败，不支持蓝牙以 1004 失败；蓝牙初始化未能在窗口内就绪以 2001 失败。失败和取消不返回部分结果。SDK 不自动开启蓝牙、配对、重试、连接或选择设备。 回调声明为 `@Sendable`，捕获的跨线程值须满足 Sendable 或适当隔离；UI 更新显式切回 MainActor。默认回调队列仍为主队列。

文档 ID：`apple:symbol:s:10PrinterSDK10BLEScannerC4scan7seconds7service10completionySd_10Foundation4UUIDVSgys6ResultOySayAA010DiscoveredA0VGAA0A8SDKErrorVGYbctF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| DiscoveredPrinter | `s:10PrinterSDK010DiscoveredA0V` |
| PrinterSDKError | `s:10PrinterSDK0A8SDKErrorV` |
| Result | `s:s6ResultO` |
| TimeInterval | `c:@T@NSTimeInterval` |
| UUID | `s:10Foundation4UUIDV` |
| Void | `s:s4Voida` |

<a id="api-93fd3659dde18045d8a7"></a>

### BLEScanner.stop()

请求取消当前扫描并清理本轮扫描资源。

```swift
func stop()
```

方法异步安排停止，不等待回调，不抛出异常；活动扫描以 1005 在主队列完成，空闲时为空操作。每轮只交付一次结果，旧回调和截止任务不会污染新一轮。回调完成后扫描器可以再次使用；析构也会请求停止。停止扫描不关闭已经创建的 `MobilePrinter` 连接。

文档 ID：`apple:symbol:s:10PrinterSDK10BLEScannerC4stopyyF`

符号种类：`swift.method`；访问级别：`public`。

<a id="api-7b987515d7ad703321ec"></a>

## ConnectionState

<a id="api-08d8e39489c4e34135e1"></a>

### ConnectionState

SDK 本地观察到的连接生命周期状态。

```swift
enum ConnectionState
```

由 `MobilePrinter.connectionState` 返回，仅是缓存观察，不探测蓝牙链路或打印机机械状态。错误由调用/操作结果表示，不增加一个稳定的“失败”连接状态。

文档 ID：`apple:symbol:s:10PrinterSDK15ConnectionStateO`

符号种类：`swift.enum`；访问级别：`public`。

<a id="api-c07119716f05d83cc201"></a>

### ConnectionState.!=(_:_:)

比较两个 ConnectionState 值是否不相等。

```swift
static func != (lhs: borrowing Self, rhs: borrowing Self) -> Bool
```

Swift 标准库 `Equatable` 扩展为此类型提供的合成公开成员，不是独立 SDK 实现。参数 `lhs`、`rhs` 为两个值；枚举项不同时返回 true，否则 false。不抛出错误、不执行 IO，也不比较设备状态或诊断文案。

文档 ID：`apple:symbol:s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK15ConnectionStateO`

符号种类：`swift.func.op`；访问级别：`public`。

这是编译器为当前类型带入的协议扩展／合成公共成员。

扩展来源（保留基线原值）：

```json
{"extendedModule":"Swift","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Bool | `s:Sb` |

<a id="api-54c2af3ad14d7a325f43"></a>

### ConnectionState.connected

SDK 已记录通信路径建立成功。

```swift
case connected
```

不是即时存活探测或打印机就绪证明；链路断开在被 IO 察觉之前可能仍保持此值。任务是否成功和纸面是否完成必须分别判断。

文档 ID：`apple:symbol:s:10PrinterSDK15ConnectionStateO9connectedyA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-08c606124ce8c59a1805"></a>

### ConnectionState.connecting

SDK 正在建立通信路径。

```swift
case connecting
```

是连接生命周期的瞬时本地状态；连接完成或失败由 `connect` 的结果表示。不要在 UI 线程等待，不根据此状态重放任务。

文档 ID：`apple:symbol:s:10PrinterSDK15ConnectionStateO10connectingyA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-a782875819b5b839434c"></a>

### ConnectionState.disconnected

SDK 当前记录为未连接。

```swift
case disconnected
```

尚未连接、显式断开、关闭，或已被 IO 发现连接丢失后的本地状态。应用需显式连接，SDK 不会自动续打。

文档 ID：`apple:symbol:s:10PrinterSDK15ConnectionStateO12disconnectedyA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-d2d5b92fc8cfecf69b5d"></a>

### ConnectionState.disconnecting

SDK 正在中断 IO 并完成断开屏障。

```swift
case disconnecting
```

断开尚未完成，正在停止接纳并等待活动工作退出。仅该状态不能证明系统蓝牙资源已经释放；等 `disconnect` 或 `close` 返回。

文档 ID：`apple:symbol:s:10PrinterSDK15ConnectionStateO13disconnectingyA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-ae6bf1c1479e246c63fb"></a>

### ConnectionState.hash(into:)

把 ConnectionState 的相等性相关值混入 Hasher。

```swift
func hash(into hasher: inout Hasher)
```

Swift 标准库为原始值枚举提供的合成成员，不是 SDK 实现。`hasher` 为调用方按 `inout` 传入的 `Hasher`，方法更新其状态，没有返回值且不抛出错误。用于 `Hashable` 集合语义，哈希结果不是跨进程稳定序列化格式，不涉及设备 IO。

文档 ID：`apple:symbol:s:SYsSHRzSH8RawValueSYRpzrlE4hash4intoys6HasherVz_tF::SYNTHESIZED::s:10PrinterSDK15ConnectionStateO`

符号种类：`swift.method`；访问级别：`public`。

这是编译器为当前类型带入的协议扩展／合成公共成员。

泛型约束（保留基线原值）：

```json
{"constraints":[{"kind":"conformance","lhs":"Self","rhs":"Hashable","rhsPrecise":"s:SH"},{"kind":"conformance","lhs":"Self.RawValue","rhs":"Hashable","rhsPrecise":"s:SH"}]}
```

扩展来源（保留基线原值）：

```json
{"constraints":[{"kind":"conformance","lhs":"Self","rhs":"Hashable","rhsPrecise":"s:SH"},{"kind":"conformance","lhs":"Self.RawValue","rhs":"Hashable","rhsPrecise":"s:SH"}],"extendedModule":"Swift","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Hasher | `s:s6HasherV` |

<a id="api-1f2a4d1f92174b905f4f"></a>

### ConnectionState.hashValue

获取 ConnectionState 的进程内哈希值。

```swift
var hashValue: Int { get }
```

Swift 标准库为原始值枚举提供的合成 `Hashable` 成员，不是 SDK 稳定编号。用于集合/字典查找；不要持久化、传输、作为 API 原始值或在不同进程/版本间比较。需要契约数值时使用枚举原始值语义。

文档 ID：`apple:symbol:s:SYsSHRzSH8RawValueSYRpzrlE04hashB0Sivp::SYNTHESIZED::s:10PrinterSDK15ConnectionStateO`

符号种类：`swift.property`；访问级别：`public`。

这是编译器为当前类型带入的协议扩展／合成公共成员。

扩展来源（保留基线原值）：

```json
{"constraints":[{"kind":"conformance","lhs":"Self","rhs":"Hashable","rhsPrecise":"s:SH"},{"kind":"conformance","lhs":"Self.RawValue","rhs":"Hashable","rhsPrecise":"s:SH"}],"extendedModule":"Swift","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Int | `s:Si` |

<a id="api-de083f3b2068cb277091"></a>

### ConnectionState.init(rawValue:)

按已声明raw值构造枚举。

```swift
init?(rawValue: UInt32)
```

匹配本版枚举值时返回对应项，未知数值返回nil；不执行IO、不探测设备，也不自动接受未来枚举。精确raw值见本页编译器枚举表。

文档 ID：`apple:symbol:s:10PrinterSDK15ConnectionStateO8rawValueACSgs6UInt32V_tcfc`

符号种类：`swift.init`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-d94c01c25a5ac00d3af5"></a>

## CpclProfile

<a id="api-f5f076cac6d8b28c330d"></a>

### CpclProfile

不可变的 CPCL 标签几何和作业默认布局配置。

```swift
struct CpclProfile
```

明确描述应用为该设备选择的 DPI、可打印点宽及标签最大高度；不从型号、纸宽或名称推断能力。`form` 和 `xOffsetDots` 只是每次标签作业的默认值，不是设备设置，也不影响 Raw。初始化可拒绝无效参数，但不验证实际硬件。 可作为本次操作参数传入，不需要在连接时提供，也不会改变连接或其他操作。

文档 ID：`apple:symbol:s:10PrinterSDK11CpclProfileV`

符号种类：`swift.struct`；访问级别：`public`。

<a id="api-7b0add7dea80368c409e"></a>

### CpclProfile.dpi

应用明确配置的打印分辨率。

```swift
let dpi: UInt32
```

单位 dots per inch，由初始化参数保存且大于 0；用于标签横纵几何。未通过设备查询或纸面测量确认，选择正确值是应用和集成方的责任。

文档 ID：`apple:symbol:s:10PrinterSDK11CpclProfileV3dpis6UInt32Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-c36ad2dd66c8d6f10672"></a>

### CpclProfile.form

每个标签作业默认的结束定位策略。

```swift
let form: Bool
```

true 请求下一标签起点结束，false 请求内容结束；没有隐式默认值，初始化时必填。`submitGray8(form: nil)` 取此值，显式传 true/false 只覆盖当前作业。它不设置纸张类型、不做传感器校准，也不影响原始字节发送。

文档 ID：`apple:symbol:s:10PrinterSDK11CpclProfileV4formSbvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Bool | `s:Sb` |

<a id="api-b6168f9850c4606d844d"></a>

### CpclProfile.init(dpi:widthDots:maxLabelHeightDots:form:xOffsetDots:)

校验并保存 CPCL 标签配置及作业默认值。

```swift
init(dpi: UInt32, widthDots: UInt32, maxLabelHeightDots: UInt32, form: Bool, xOffsetDots: UInt32 = 0) throws
```

`dpi` 必须大于 0，单位 dots per inch；`widthDots` 必须大于 0、不超过 7992 且为 8 的整数倍；`maxLabelHeightDots` 必须在 1…65535；两者单位为点。`form` 必填：true 默认请求下一标签起点结束，false 默认在内容结束。`xOffsetDots` 默认 0，须小于 `widthDots`。任一构造条件无效抛 1001；设备初始化还要求 DPI 可表示为正的 Int32，超出时抛 1001；不能将配置构造成功解释为硬件支持。初始化不发送设置或校准命令。

文档 ID：`apple:symbol:s:10PrinterSDK11CpclProfileV3dpi9widthDots014maxLabelHeightG04form07xOffsetG0ACs6UInt32V_A2JSbAJtKcfc`

符号种类：`swift.init`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Bool | `s:Sb` |
| UInt32 | `s:s6UInt32V` |

<a id="api-5a360689d670c9a843a1"></a>

### CpclProfile.maxLabelHeightDots

配置的单张标签最大高度。

```swift
let maxLabelHeightDots: UInt32
```

单位为点，构造接受 1…65535；提交标签的图像高度加尾部留白不得超过此范围。该值由调用方显式指定，构造成功不代表固件已报告或硬件已验证这一长度能力。

文档 ID：`apple:symbol:s:10PrinterSDK11CpclProfileV18maxLabelHeightDotss6UInt32Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-6a75981ab8bbcc72c055"></a>

### CpclProfile.widthDots

配置的设备可打印最大横向点宽。

```swift
let widthDots: UInt32
```

单位为点；构造要求 1…7992 且为 8 的整数倍。它不等于介质毫米宽，也不自动给出居中装纸的原点。标签作业的横向原点、画布和补齐到整字节的图像范围必须共同落在此宽度内。

文档 ID：`apple:symbol:s:10PrinterSDK11CpclProfileV9widthDotss6UInt32Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-2f5362ff7cb84df701bf"></a>

### CpclProfile.xOffsetDots

每个标签作业默认的横向原点。

```swift
let xOffsetDots: UInt32
```

单位为点，构造参数默认 0，须小于配置打印宽度。`submitGray8(originXDots: nil)` 使用此值；每次提交独立保存原点，显式覆盖不会修改 profile。Raw 原样发送，不应用此偏移；SDK 不自动推断居中装纸位置。

文档 ID：`apple:symbol:s:10PrinterSDK11CpclProfileV11xOffsetDotss6UInt32Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-ca87677d19e3423fb23c"></a>

## CutMode

<a id="api-7d24b7168d872e491e4f"></a>

### CutMode

小票结束时请求的切纸方式。

```swift
enum CutMode
```

传给 `submitReceiptGray8(cut:)`；当前标准 ESC/POS 后端未实现切纸，非 `.none` 请求会在首次 IO 前拒绝，不会静默省略。枚举项是契约选项，不是设备具有切刀的证据。

文档 ID：`apple:symbol:s:10PrinterSDK7CutModeO`

符号种类：`swift.enum`；访问级别：`public`。

<a id="api-a3eaff6517b3380de212"></a>

### CutMode.!=(_:_:)

比较两个 CutMode 值是否不相等。

```swift
static func != (lhs: borrowing Self, rhs: borrowing Self) -> Bool
```

Swift 标准库 `Equatable` 扩展为此类型提供的合成公开成员，不是独立 SDK 实现。参数 `lhs`、`rhs` 为两个值；枚举项不同时返回 true，否则 false。不抛出错误、不执行 IO，也不比较设备状态或诊断文案。

文档 ID：`apple:symbol:s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK7CutModeO`

符号种类：`swift.func.op`；访问级别：`public`。

这是编译器为当前类型带入的协议扩展／合成公共成员。

扩展来源（保留基线原值）：

```json
{"extendedModule":"Swift","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Bool | `s:Sb` |

<a id="api-1a062cca2ef105422cca"></a>

### CutMode.full

请求全切。

```swift
case full
```

当前标准后端未实现，提交时在 IO 前以 1004 拒绝；不会降级为不切或半切。目标设备存在切刀的事实需独立证据。

文档 ID：`apple:symbol:s:10PrinterSDK7CutModeO4fullyA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-89a8df5865e8f99daaa7"></a>

### CutMode.hash(into:)

把 CutMode 的相等性相关值混入 Hasher。

```swift
func hash(into hasher: inout Hasher)
```

Swift 标准库为原始值枚举提供的合成成员，不是 SDK 实现。`hasher` 为调用方按 `inout` 传入的 `Hasher`，方法更新其状态，没有返回值且不抛出错误。用于 `Hashable` 集合语义，哈希结果不是跨进程稳定序列化格式，不涉及设备 IO。

文档 ID：`apple:symbol:s:SYsSHRzSH8RawValueSYRpzrlE4hash4intoys6HasherVz_tF::SYNTHESIZED::s:10PrinterSDK7CutModeO`

符号种类：`swift.method`；访问级别：`public`。

这是编译器为当前类型带入的协议扩展／合成公共成员。

泛型约束（保留基线原值）：

```json
{"constraints":[{"kind":"conformance","lhs":"Self","rhs":"Hashable","rhsPrecise":"s:SH"},{"kind":"conformance","lhs":"Self.RawValue","rhs":"Hashable","rhsPrecise":"s:SH"}]}
```

扩展来源（保留基线原值）：

```json
{"constraints":[{"kind":"conformance","lhs":"Self","rhs":"Hashable","rhsPrecise":"s:SH"},{"kind":"conformance","lhs":"Self.RawValue","rhs":"Hashable","rhsPrecise":"s:SH"}],"extendedModule":"Swift","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Hasher | `s:s6HasherV` |

<a id="api-08952d6d2d79cdfc7946"></a>

### CutMode.hashValue

获取 CutMode 的进程内哈希值。

```swift
var hashValue: Int { get }
```

Swift 标准库为原始值枚举提供的合成 `Hashable` 成员，不是 SDK 稳定编号。用于集合/字典查找；不要持久化、传输、作为 API 原始值或在不同进程/版本间比较。需要契约数值时使用枚举原始值语义。

文档 ID：`apple:symbol:s:SYsSHRzSH8RawValueSYRpzrlE04hashB0Sivp::SYNTHESIZED::s:10PrinterSDK7CutModeO`

符号种类：`swift.property`；访问级别：`public`。

这是编译器为当前类型带入的协议扩展／合成公共成员。

扩展来源（保留基线原值）：

```json
{"constraints":[{"kind":"conformance","lhs":"Self","rhs":"Hashable","rhsPrecise":"s:SH"},{"kind":"conformance","lhs":"Self.RawValue","rhs":"Hashable","rhsPrecise":"s:SH"}],"extendedModule":"Swift","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Int | `s:Si` |

<a id="api-b51c659cf65b513b98dd"></a>

### CutMode.init(rawValue:)

按已声明raw值构造枚举。

```swift
init?(rawValue: UInt32)
```

匹配本版枚举值时返回对应项，未知数值返回nil；不执行IO、不探测设备，也不自动接受未来枚举。精确raw值见本页编译器枚举表。

文档 ID：`apple:symbol:s:10PrinterSDK7CutModeO8rawValueACSgs6UInt32V_tcfc`

符号种类：`swift.init`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-88dbcfe1305b1c57a033"></a>

### CutMode.none

不请求切纸。

```swift
case none
```

为 `submitReceiptGray8` 的默认值；仍可根据 `trailingBlankDots` 与 `finishScope` 发送尾部留白。它不关闭设备，也不承诺自动撕纸位置。

文档 ID：`apple:symbol:s:10PrinterSDK7CutModeO4noneyA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-91534bd27acdb707ecb9"></a>

### CutMode.partial

请求半切。

```swift
case partial
```

当前标准后端未实现，提交时在 IO 前以 1004 拒绝；不会降级为全切或不切。枚举项不构成硬件支持。

文档 ID：`apple:symbol:s:10PrinterSDK7CutModeO7partialyA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-a4469ccf35ccf99bbc8f"></a>

## DeviceActiveOperation

<a id="api-d084cbcce8288d7cadad"></a>

### DeviceActiveOperation

不可变的活动操作信息。

```swift
struct DeviceActiveOperation
```

仅设备快照有活动业务操作时存在；包含原生 ID、kind、state，不保存操作句柄或控制权。

文档 ID：`apple:symbol:s:10PrinterSDK21DeviceActiveOperationV`

符号种类：`swift.struct`；访问级别：`public`。

<a id="api-47fdcd3632e1b417608a"></a>

### DeviceActiveOperation.!=(_:_:)

编译器提供的值不等比较。

```swift
static func != (lhs: borrowing Self, rhs: borrowing Self) -> Bool
```

比较两个同类型快照或状态的值；只反映已采样值差异，不刷新设备状态或执行打印机 IO。

文档 ID：`apple:symbol:s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK21DeviceActiveOperationV`

符号种类：`swift.func.op`；访问级别：`public`。

这是编译器为当前类型带入的协议扩展／合成公共成员。

扩展来源（保留基线原值）：

```json
{"extendedModule":"Swift","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Bool | `s:Sb` |

<a id="api-2673d315e9c65be94145"></a>

### DeviceActiveOperation.id

活动操作的原生 ID。

```swift
let id: UInt64
```

来自同一设备执行器，与 PrintOperation 快照的 id 对应；不能作为跨设备身份或兼容 task ID。

文档 ID：`apple:symbol:s:10PrinterSDK21DeviceActiveOperationV2ids6UInt64Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt64 | `s:s6UInt64V` |

<a id="api-8cfe6170c7cea9038bb3"></a>

### DeviceActiveOperation.kind

活动操作种类。

```swift
let kind: UInt32
```

沿用原生 PSDK_OP_* 值；0 表示没有公共种类的内部操作，不据此推断硬件打印状态。

文档 ID：`apple:symbol:s:10PrinterSDK21DeviceActiveOperationV4kinds6UInt32Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-76fc3921f5d35cdbddcb"></a>

### DeviceActiveOperation.state

活动操作状态。

```swift
let state: UInt32
```

沿用 operation 状态值，描述 SDK 执行阶段；发送完成不表示纸面打印完成。

文档 ID：`apple:symbol:s:10PrinterSDK21DeviceActiveOperationV5states6UInt32Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-8ca6a77eedc34c565166"></a>

## DeviceStateSnapshot

<a id="api-7a36f4147385d13e0a48"></a>

### DeviceStateSnapshot

不可变的设备状态快照。

```swift
struct DeviceStateSnapshot
```

读取不调用传输回调、不发送打印机 IO、不入队也不等待打印完成。快照只用于展示和诊断，不作为提交前置条件；不得与设备销毁并发访问。 activeOperation 与硬件采样时间采用可选值表示不存在或未知。

文档 ID：`apple:symbol:s:10PrinterSDK19DeviceStateSnapshotV`

符号种类：`swift.struct`；访问级别：`public`。

<a id="api-b955293ae582f638118c"></a>

### DeviceStateSnapshot.!=(_:_:)

编译器提供的值不等比较。

```swift
static func != (lhs: borrowing Self, rhs: borrowing Self) -> Bool
```

比较两个同类型快照或状态的值；只反映已采样值差异，不刷新设备状态或执行打印机 IO。

文档 ID：`apple:symbol:s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK19DeviceStateSnapshotV`

符号种类：`swift.func.op`；访问级别：`public`。

这是编译器为当前类型带入的协议扩展／合成公共成员。

扩展来源（保留基线原值）：

```json
{"extendedModule":"Swift","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Bool | `s:Sb` |

<a id="api-a854e34e4f2cb44537b7"></a>

### DeviceStateSnapshot.activeOperation

可选的活动操作信息。

```swift
let activeOperation: DeviceActiveOperation?
```

无活动业务操作时为 nil；非 nil 时包含原生 operation ID、kind 和 state，不是兼容 task ID。

文档 ID：`apple:symbol:s:10PrinterSDK19DeviceStateSnapshotV15activeOperationAA0c6ActiveG0VSgvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| DeviceActiveOperation | `s:10PrinterSDK21DeviceActiveOperationV` |

<a id="api-313849eb4c10c183ec35"></a>

### DeviceStateSnapshot.connectionGeneration

成功新连接的代数。

```swift
let connectionGeneration: UInt64
```

初始为 0，每次成功建立新的连接递增；失败连接、断开以及已连接时重复 connect 不递增。

文档 ID：`apple:symbol:s:10PrinterSDK19DeviceStateSnapshotV20connectionGenerations6UInt64Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt64 | `s:s6UInt64V` |

<a id="api-af8b665ab8b548d003c4"></a>

### DeviceStateSnapshot.connectionState

SDK 本地连接状态。

```swift
let connectionState: ConnectionState
```

使用既有四态值；CONNECTED 不证明实时链路存活、设备就绪或纸面完成。

文档 ID：`apple:symbol:s:10PrinterSDK19DeviceStateSnapshotV010connectionD0AA010ConnectionD0Ovp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| ConnectionState | `s:10PrinterSDK15ConnectionStateO` |

<a id="api-b72c3e7ed667a0834f4b"></a>

### DeviceStateSnapshot.hardwareActiveConditions

已知且成立的硬件条件位集合。

```swift
let hardwareActiveConditions: UInt32
```

当前固定为 0；仅在对应 known 条件位有效时才可解释，当前没有有效硬件采样。

文档 ID：`apple:symbol:s:10PrinterSDK19DeviceStateSnapshotV24hardwareActiveConditionss6UInt32Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-967c4402a94902378ec6"></a>

### DeviceStateSnapshot.hardwareKnownConditions

有有效硬件证据的条件位集合。

```swift
let hardwareKnownConditions: UInt32
```

当前固定为 0，表示没有已知条件；未知不能推断为没有缺纸、开盖或故障。

文档 ID：`apple:symbol:s:10PrinterSDK19DeviceStateSnapshotV23hardwareKnownConditionss6UInt32Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-58bd2a5619cf3ca33686"></a>

### DeviceStateSnapshot.hardwareSampledMonotonicMilliseconds

可选的硬件采样单调时钟毫秒。

```swift
let hardwareSampledMonotonicMilliseconds: UInt64?
```

当前固定为 nil，表示没有有效硬件采样；未来非 nil 时也不是墙上时钟或日历时间。

文档 ID：`apple:symbol:s:10PrinterSDK19DeviceStateSnapshotV36hardwareSampledMonotonicMillisecondss6UInt64VSgvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt64 | `s:s6UInt64V` |

<a id="api-d41db8d97ca57d6264b8"></a>

### DeviceStateSnapshot.hardwareState

打印机实际硬件状态。

```swift
let hardwareState: HardwareState
```

当前标准后端固定返回 UNKNOWN=0；不得把队列为空或已连接显示为硬件就绪。

文档 ID：`apple:symbol:s:10PrinterSDK19DeviceStateSnapshotV08hardwareD0AA08HardwareD0Ovp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| HardwareState | `s:10PrinterSDK13HardwareStateO` |

<a id="api-a96a5cb9643a94cb778c"></a>

### DeviceStateSnapshot.queuedCount

等待执行的业务操作数。

```swift
let queuedCount: UInt32
```

不包含活动操作或内部连接命令；取消请求尚未完成时可能仍计入，排队取消成功返回后已经移出；读取后数值可立即改变。

文档 ID：`apple:symbol:s:10PrinterSDK19DeviceStateSnapshotV11queuedCounts6UInt32Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-34afc3901a6552dfed2f"></a>

### DeviceStateSnapshot.workState

SDK 工作阶段。

```swift
let workState: DeviceWorkState
```

IDLE=0、QUEUED=1、PREPARING=2、SENDING=3；只反映 SDK 队列与执行器，不表示硬件正在打印。

文档 ID：`apple:symbol:s:10PrinterSDK19DeviceStateSnapshotV04workD0AA0c4WorkD0Ovp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| DeviceWorkState | `s:10PrinterSDK15DeviceWorkStateO` |

<a id="api-54f3a6b00a3988e4c18e"></a>

## DeviceWorkState

<a id="api-777c72318715e1e6fdf0"></a>

### DeviceWorkState

SDK 本地工作状态枚举。

```swift
enum DeviceWorkState
```

UInt32 raw 值固定为 idle=0、queued=1、preparing=2、sending=3；不能解释为硬件物理打印状态。

文档 ID：`apple:symbol:s:10PrinterSDK15DeviceWorkStateO`

符号种类：`swift.enum`；访问级别：`public`。

<a id="api-4f6f9484dc4dc82743e8"></a>

### DeviceWorkState.!=(_:_:)

编译器提供的值不等比较。

```swift
static func != (lhs: borrowing Self, rhs: borrowing Self) -> Bool
```

比较两个同类型快照或状态的值；只反映已采样值差异，不刷新设备状态或执行打印机 IO。

文档 ID：`apple:symbol:s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK15DeviceWorkStateO`

符号种类：`swift.func.op`；访问级别：`public`。

这是编译器为当前类型带入的协议扩展／合成公共成员。

扩展来源（保留基线原值）：

```json
{"extendedModule":"Swift","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Bool | `s:Sb` |

<a id="api-983c9989af02431e2b97"></a>

### DeviceWorkState.hash(into:)

编译器提供的枚举哈希计算。

```swift
func hash(into hasher: inout Hasher)
```

由 Hashable 协议合成，仅向当前进程 hasher 添加值，不触发状态查询或打印机 IO。

文档 ID：`apple:symbol:s:SYsSHRzSH8RawValueSYRpzrlE4hash4intoys6HasherVz_tF::SYNTHESIZED::s:10PrinterSDK15DeviceWorkStateO`

符号种类：`swift.method`；访问级别：`public`。

这是编译器为当前类型带入的协议扩展／合成公共成员。

泛型约束（保留基线原值）：

```json
{"constraints":[{"kind":"conformance","lhs":"Self","rhs":"Hashable","rhsPrecise":"s:SH"},{"kind":"conformance","lhs":"Self.RawValue","rhs":"Hashable","rhsPrecise":"s:SH"}]}
```

扩展来源（保留基线原值）：

```json
{"constraints":[{"kind":"conformance","lhs":"Self","rhs":"Hashable","rhsPrecise":"s:SH"},{"kind":"conformance","lhs":"Self.RawValue","rhs":"Hashable","rhsPrecise":"s:SH"}],"extendedModule":"Swift","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Hasher | `s:s6HasherV` |

<a id="api-e747e90291ad41f45f2c"></a>

### DeviceWorkState.hashValue

编译器提供的枚举哈希值。

```swift
var hashValue: Int { get }
```

仅用于当前进程集合操作；不稳定，不能代替 raw 值持久化或作为设备状态协议。

文档 ID：`apple:symbol:s:SYsSHRzSH8RawValueSYRpzrlE04hashB0Sivp::SYNTHESIZED::s:10PrinterSDK15DeviceWorkStateO`

符号种类：`swift.property`；访问级别：`public`。

这是编译器为当前类型带入的协议扩展／合成公共成员。

扩展来源（保留基线原值）：

```json
{"constraints":[{"kind":"conformance","lhs":"Self","rhs":"Hashable","rhsPrecise":"s:SH"},{"kind":"conformance","lhs":"Self.RawValue","rhs":"Hashable","rhsPrecise":"s:SH"}],"extendedModule":"Swift","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Int | `s:Si` |

<a id="api-acafcf7458171d599cac"></a>

### DeviceWorkState.idle

SDK IDLE 工作状态枚举项。

```swift
case idle
```

UInt32 raw 值为 0；没有活动或等待的 SDK 业务操作，不证明打印机已停止出纸。

文档 ID：`apple:symbol:s:10PrinterSDK15DeviceWorkStateO4idleyA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-47d71661356fba0da958"></a>

### DeviceWorkState.init(rawValue:)

按已声明raw值构造枚举。

```swift
init?(rawValue: UInt32)
```

匹配本版枚举值时返回对应项，未知数值返回nil；不执行IO、不探测设备，也不自动接受未来枚举。精确raw值见本页编译器枚举表。

文档 ID：`apple:symbol:s:10PrinterSDK15DeviceWorkStateO8rawValueACSgs6UInt32V_tcfc`

符号种类：`swift.init`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-2074c3f9c34c348a10cb"></a>

### DeviceWorkState.preparing

SDK PREPARING 工作状态枚举项。

```swift
case preparing
```

UInt32 raw 值为 2；正在准备图像或执行发送前处理，不表示实际出纸。

文档 ID：`apple:symbol:s:10PrinterSDK15DeviceWorkStateO9preparingyA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-46e2a1461cb7acddbad9"></a>

### DeviceWorkState.queued

SDK QUEUED 工作状态枚举项。

```swift
case queued
```

UInt32 raw 值为 1；有等待操作，不能作为提交前置条件。

文档 ID：`apple:symbol:s:10PrinterSDK15DeviceWorkStateO6queuedyA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-f9629408dc9e1e4b0649"></a>

### DeviceWorkState.sending

SDK SENDING 工作状态枚举项。

```swift
case sending
```

UInt32 raw 值为 3；正在发送或等待流控，实际硬件状态仍未知。

文档 ID：`apple:symbol:s:10PrinterSDK15DeviceWorkStateO7sendingyA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-2e47e1cecb9f6ee6289d"></a>

## DiagnosticSource

<a id="api-1a9599a0532af244d73f"></a>

### DiagnosticSource

诊断来源分类。

```swift
enum DiagnosticSource
```

不可变 UInt32 原始值枚举；分类不表示经过验证的硬件支持，未知原始值不能猜测映射。

文档 ID：`apple:symbol:s:10PrinterSDK16DiagnosticSourceO`

符号种类：`swift.enum`；访问级别：`public`。

<a id="api-e5af391fa098f9b026ab"></a>

### DiagnosticSource.!=(_:_:)

按值比较两个 DiagnosticSource 是否不同。

```swift
static func != (lhs: borrowing Self, rhs: borrowing Self) -> Bool
```

Swift Equatable 提供的比较，不执行 IO、不刷新状态。比较本次值或枚举，不能把相等的快照当作设备仍在线、硬件正常或纸面打印完成的证明。

文档 ID：`apple:symbol:s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK16DiagnosticSourceO`

符号种类：`swift.func.op`；访问级别：`public`。

这是编译器为当前类型带入的协议扩展／合成公共成员。

扩展来源（保留基线原值）：

```json
{"extendedModule":"Swift","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Bool | `s:Sb` |

<a id="api-66780a30555012cfc063"></a>

### DiagnosticSource.connect

连接建立失败，值 1。

```swift
case connect
```

固定公共枚举值；其含义仅限本查询参数、来源分类或协议选择，不能据此推断设备当前状态或纸面完成。

文档 ID：`apple:symbol:s:10PrinterSDK16DiagnosticSourceO7connectyA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-e8d7d11912100f81b43b"></a>

### DiagnosticSource.hash(into:)

把枚举值加入 Swift 哈希器。

```swift
func hash(into hasher: inout Hasher)
```

Swift Hashable 合成操作，不执行 IO；哈希结果不保证跨进程一致，持久化时使用已文档化的 rawValue。

文档 ID：`apple:symbol:s:SYsSHRzSH8RawValueSYRpzrlE4hash4intoys6HasherVz_tF::SYNTHESIZED::s:10PrinterSDK16DiagnosticSourceO`

符号种类：`swift.method`；访问级别：`public`。

这是编译器为当前类型带入的协议扩展／合成公共成员。

泛型约束（保留基线原值）：

```json
{"constraints":[{"kind":"conformance","lhs":"Self","rhs":"Hashable","rhsPrecise":"s:SH"},{"kind":"conformance","lhs":"Self.RawValue","rhs":"Hashable","rhsPrecise":"s:SH"}]}
```

扩展来源（保留基线原值）：

```json
{"constraints":[{"kind":"conformance","lhs":"Self","rhs":"Hashable","rhsPrecise":"s:SH"},{"kind":"conformance","lhs":"Self.RawValue","rhs":"Hashable","rhsPrecise":"s:SH"}],"extendedModule":"Swift","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Hasher | `s:s6HasherV` |

<a id="api-529b3dc9f92271ac8987"></a>

### DiagnosticSource.hashValue

枚举的进程内哈希值。

```swift
var hashValue: Int { get }
```

Swift Hashable 合成值，用于当前进程集合，不是稳定协议值、持久标识或设备 ID；协议值读取 rawValue。

文档 ID：`apple:symbol:s:SYsSHRzSH8RawValueSYRpzrlE04hashB0Sivp::SYNTHESIZED::s:10PrinterSDK16DiagnosticSourceO`

符号种类：`swift.property`；访问级别：`public`。

这是编译器为当前类型带入的协议扩展／合成公共成员。

扩展来源（保留基线原值）：

```json
{"constraints":[{"kind":"conformance","lhs":"Self","rhs":"Hashable","rhsPrecise":"s:SH"},{"kind":"conformance","lhs":"Self.RawValue","rhs":"Hashable","rhsPrecise":"s:SH"}],"extendedModule":"Swift","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Int | `s:Si` |

<a id="api-19d6f62bc12b14e42e4a"></a>

### DiagnosticSource.init(rawValue:)

按已声明raw值构造枚举。

```swift
init?(rawValue: UInt32)
```

匹配本版枚举值时返回对应项，未知数值返回nil；不执行IO、不探测设备，也不自动接受未来枚举。精确raw值见本页编译器枚举表。

文档 ID：`apple:symbol:s:10PrinterSDK16DiagnosticSourceO8rawValueACSgs6UInt32V_tcfc`

符号种类：`swift.init`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-773d22c09798e349e915"></a>

### DiagnosticSource.none

无来源，值 0。

```swift
case none
```

固定公共枚举值；其含义仅限本查询参数、来源分类或协议选择，不能据此推断设备当前状态或纸面完成。

文档 ID：`apple:symbol:s:10PrinterSDK16DiagnosticSourceO4noneyA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-1e2cc4f7310d0247a60e"></a>

### DiagnosticSource.operation

业务操作失败，值 2。

```swift
case operation
```

固定公共枚举值；其含义仅限本查询参数、来源分类或协议选择，不能据此推断设备当前状态或纸面完成。

文档 ID：`apple:symbol:s:10PrinterSDK16DiagnosticSourceO9operationyA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-00c034c828329094fa66"></a>

### DiagnosticSource.transport

平台主动断连，值 3。

```swift
case transport
```

固定公共枚举值；其含义仅限本查询参数、来源分类或协议选择，不能据此推断设备当前状态或纸面完成。

文档 ID：`apple:symbol:s:10PrinterSDK16DiagnosticSourceO9transportyA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-601bacfa9f80a1aec096"></a>

### DiagnosticSource.userDisconnect

调用方主动断开，值 4。

```swift
case userDisconnect
```

固定公共枚举值；其含义仅限本查询参数、来源分类或协议选择，不能据此推断设备当前状态或纸面完成。

文档 ID：`apple:symbol:s:10PrinterSDK16DiagnosticSourceO14userDisconnectyA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-e7eba043862754865338"></a>

## DiscoveredPrinter

<a id="api-f8569a880ca35c2fbdc3"></a>

### DiscoveredPrinter

一次 BLE 发现窗口中合并后的外设描述。

```swift
struct DiscoveredPrinter
```

由 `BLEScanner` 返回的不可变值；相同 `identifier` 的多次广播合并为一项，列表保留首次发现顺序。发现结果仅用于应用选择连接目标，不证明设备是打印机、支持某种协议或已完成硬件验收。没有公开初始化入口，也不包含 CoreBluetooth 原生句柄。

文档 ID：`apple:symbol:s:10PrinterSDK010DiscoveredA0V`

符号种类：`swift.struct`；访问级别：`public`。

<a id="api-0af7f41203b681bd0867"></a>

### DiscoveredPrinter.advertisedServices

扫描窗口中观察到的服务 UUID 字符串集合。

```swift
let advertisedServices: [String]
```

按首次出现顺序合并广播服务，忽略首尾空白和大小写造成的重复。它只表示广播信息，并不是连接后完整的 GATT 服务清单；空数组也不证明目标缺少所需服务。

文档 ID：`apple:symbol:s:10PrinterSDK010DiscoveredA0V18advertisedServicesSaySSGvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| String | `s:SS` |

<a id="api-33036fc5f14ef3b9bca2"></a>

### DiscoveredPrinter.identifier

CoreBluetooth 为外设提供的不透明标识符。

```swift
let identifier: UUID
```

传给 `MobilePrinter(bleIdentifier:options:)` 的选择结果。它是 iOS 外设 UUID，不是 Android 蓝牙 MAC 地址，也不是跨平台设备序列号；连接前目标须已被 CoreBluetooth 识别。应用应把设备身份诊断按隐私要求处理。

文档 ID：`apple:symbol:s:10PrinterSDK010DiscoveredA0V10identifier10Foundation4UUIDVvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UUID | `s:10Foundation4UUIDV` |

<a id="api-a00f5310f1770af3ec9e"></a>

### DiscoveredPrinter.name

广播或平台提供的可选外设名称。

```swift
let name: String?
```

名称会移除首尾空白；没有可用名称时为 `nil`。相同外设的后续有效名称更新该值，没有新名称时保留此前值。名称可以重复或变化，不用于判断协议、型号或硬件支持。

文档 ID：`apple:symbol:s:10PrinterSDK010DiscoveredA0V4nameSSSgvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| String | `s:SS` |

<a id="api-2a8c15042b6af9aba7d5"></a>

### DiscoveredPrinter.rssi

最近一次发现事件携带的接收信号强度。

```swift
let rssi: Int
```

单位为 dBm，直接保留平台最近读数；CoreBluetooth 以 `127` 表示 RSSI 不可用。不要把 `127` 当作有效信号强度，也不要由 RSSI 推断打印机就绪或连接稳定。

文档 ID：`apple:symbol:s:10PrinterSDK010DiscoveredA0V4rssiSivp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Int | `s:Si` |

<a id="api-33ee6f34f5b5b231c733"></a>

## ErrorDiagnostic

<a id="api-0a45bc3ca5dffe636003"></a>

### ErrorDiagnostic

不可变的有界错误诊断。

```swift
struct ErrorDiagnostic
```

设备保留一条最新记录，operation 的记录独立存活。sequence=0 表示无设备记录。只包含稳定错误码、关联代次和经筛选的诊断，不携带打印载荷、地址或平台自由文本；单调时间只可在当前进程内比较。

文档 ID：`apple:symbol:s:10PrinterSDK15ErrorDiagnosticV`

符号种类：`swift.struct`；访问级别：`public`。

<a id="api-c024781fce5fa93e4830"></a>

### ErrorDiagnostic.!=(_:_:)

按值比较两个 ErrorDiagnostic 是否不同。

```swift
static func != (lhs: borrowing Self, rhs: borrowing Self) -> Bool
```

Swift Equatable 提供的比较，不执行 IO、不刷新状态。比较本次值或枚举，不能把相等的快照当作设备仍在线、硬件正常或纸面打印完成的证明。

文档 ID：`apple:symbol:s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK15ErrorDiagnosticV`

符号种类：`swift.func.op`；访问级别：`public`。

这是编译器为当前类型带入的协议扩展／合成公共成员。

扩展来源（保留基线原值）：

```json
{"extendedModule":"Swift","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Bool | `s:Sb` |

<a id="api-56104f0cf92b09ed6fbc"></a>

### ErrorDiagnostic.connectionGeneration

产生记录时的连接成功代次。

```swift
let connectionGeneration: UInt64
```

较早代次记录在重连后仍保留；与当前设备代次比较，避免把旧错误显示为当前故障。

文档 ID：`apple:symbol:s:10PrinterSDK15ErrorDiagnosticV20connectionGenerations6UInt64Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt64 | `s:s6UInt64V` |

<a id="api-7579e7cc51a093664182"></a>

### ErrorDiagnostic.domain

经过筛选的错误域。

```swift
let domain: String
```

最多 31 个 UTF-8 字节；无域时为空字符串，不包含地址或任意平台错误文本。

文档 ID：`apple:symbol:s:10PrinterSDK15ErrorDiagnosticV6domainSSvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| String | `s:SS` |

<a id="api-4b7bdc7a52cfd52ce457"></a>

### ErrorDiagnostic.errorCode

稳定 SDK 错误码。

```swift
let errorCode: Int32
```

成功或主动断开记录可为 0；业务判断使用此字段，平台错误码仅供诊断。

文档 ID：`apple:symbol:s:10PrinterSDK15ErrorDiagnosticV9errorCodes5Int32Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Int32 | `s:s5Int32V` |

<a id="api-644377568a008990faf5"></a>

### ErrorDiagnostic.message

经过筛选的有界错误说明。

```swift
let message: String
```

最多 191 个 UTF-8 字节；无说明时为空字符串。稳定业务判断继续使用 errorCode。

文档 ID：`apple:symbol:s:10PrinterSDK15ErrorDiagnosticV7messageSSvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| String | `s:SS` |

<a id="api-030738a1ab4d8bd8882e"></a>

### ErrorDiagnostic.nativeCode

可选平台诊断码。

```swift
let nativeCode: Int64?
```

nil 表示未提供平台原生码；有值也仅用于诊断，应结合 domain 解释，不作为稳定业务分支。

文档 ID：`apple:symbol:s:10PrinterSDK15ErrorDiagnosticV10nativeCodes5Int64VSgvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Int64 | `s:s5Int64V` |

<a id="api-2991c963667c3d1d1b50"></a>

### ErrorDiagnostic.operationID

相关 operation 的标识。

```swift
let operationID: UInt64
```

0 表示记录没有关联业务操作；operation 结果可在设备关闭后独立读取。

文档 ID：`apple:symbol:s:10PrinterSDK15ErrorDiagnosticV11operationIDs6UInt64Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt64 | `s:s6UInt64V` |

<a id="api-fed6af8e1cddc46c2f4d"></a>

### ErrorDiagnostic.sampledMonotonicMilliseconds

诊断记录的进程单调时间，单位毫秒。

```swift
let sampledMonotonicMilliseconds: UInt64?
```

nil 表示没有采样时间；非 nil 为当前进程单调时间，单位毫秒，不是墙上时间，只可在同一进程计算相对时长。

文档 ID：`apple:symbol:s:10PrinterSDK15ErrorDiagnosticV28sampledMonotonicMillisecondss6UInt64VSgvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt64 | `s:s6UInt64V` |

<a id="api-a707ed48511b9b94fd46"></a>

### ErrorDiagnostic.sequence

设备诊断的递增记录序号。

```swift
let sequence: UInt64
```

0 表示尚无设备诊断；这是最新一条记录的身份，不是历史记录数或全局日志编号。

文档 ID：`apple:symbol:s:10PrinterSDK15ErrorDiagnosticV8sequences6UInt64Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt64 | `s:s6UInt64V` |

<a id="api-b549bb306001fcfbb3a2"></a>

### ErrorDiagnostic.source

诊断来源分类。

```swift
let source: DiagnosticSource
```

值为 NONE、CONNECT、OPERATION、TRANSPORT 或 USER_DISCONNECT，分类不表示纸面打印结果。

文档 ID：`apple:symbol:s:10PrinterSDK15ErrorDiagnosticV6sourceAA0D6SourceOvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| DiagnosticSource | `s:10PrinterSDK16DiagnosticSourceO` |

<a id="api-02182e3c7c479930aac9"></a>

### ErrorDiagnostic.transportEpoch

发生记录时的底层 open 尝试代次。

```swift
let transportEpoch: UInt64
```

用于过滤旧连接回调，不等同于成功连接 generation；旧 epoch 的断连信号不会影响新连接。

文档 ID：`apple:symbol:s:10PrinterSDK15ErrorDiagnosticV14transportEpochs6UInt64Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt64 | `s:s6UInt64V` |

<a id="api-63244971bb8665f0438c"></a>

## FeatureSupport

<a id="api-1e75e58e75779acbd3b1"></a>

### FeatureSupport

区分软件实现、设备支持和证据来源的功能描述。

```swift
struct FeatureSupport
```

由 `MobilePrinter.feature` 返回，无公开初始化入口。所有字段只读，读值不进行 IO；各字段是底层契约数值，避免把非零实现当作硬件认证。当前显式 BLE 配置可以已实现但设备支持未知。

文档 ID：`apple:symbol:s:10PrinterSDK14FeatureSupportV`

符号种类：`swift.struct`；访问级别：`public`。

<a id="api-c4a446b320b944bde7b4"></a>

### FeatureSupport.!=(_:_:)

编译器提供的值不等比较。

```swift
static func != (lhs: borrowing Self, rhs: borrowing Self) -> Bool
```

按该值类型的Equatable语义比较，返回布尔值，不执行设备IO。比较相同状态或结果不代表设备当前在线或机械打印完成。

文档 ID：`apple:symbol:s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK14FeatureSupportV`

符号种类：`swift.func.op`；访问级别：`public`。

这是编译器为当前类型带入的协议扩展／合成公共成员。

扩展来源（保留基线原值）：

```json
{"extendedModule":"Swift","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Bool | `s:Sb` |

<a id="api-583fbacd94ea963788cc"></a>

### FeatureSupport.deviceSupport

设备支持的三态数值。

```swift
let deviceSupport: UInt32
```

0 未知，1 明确不支持，2 明确支持。显式配置当前仍可能为 0，即使 SDK 已实现相关路径也不能提升为支持。`isSupported` 只有在 `implementation != 0` 且此值为 2 时为 true。

文档 ID：`apple:symbol:s:10PrinterSDK14FeatureSupportV06deviceD0s6UInt32Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-d96ba7fde3cdfcaa76c8"></a>

### FeatureSupport.evidence

支持结论的来源标志位。

```swift
let evidence: UInt32
```

1 为配置声明，2 为设备报告，4 为硬件验证；标志可按位组合。当前显式 BLE 配置的配置声明不等同设备读回或真机验证，应用不得由预设名、广播名或构造成功补造证据。

文档 ID：`apple:symbol:s:10PrinterSDK14FeatureSupportV8evidences6UInt32Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-1219985c6bef05a31c21"></a>

### FeatureSupport.implementation

SDK 对该功能采用的实现方式。

```swift
let implementation: UInt32
```

数值 0 表示未实现，1 表示协议原生实现，2 表示主机渲染。`isImplemented` 仅判断是否非零。它描述软件路径，不证明固件支持或硬件验收通过；未知未来数值应保留并谨慎处理。

文档 ID：`apple:symbol:s:10PrinterSDK14FeatureSupportV14implementations6UInt32Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-5e4c669bce7781a5a963"></a>

### FeatureSupport.isImplemented

SDK 是否提供非零的实现方式。

```swift
var isImplemented: Bool { get }
```

当前判断等同 `implementation != 0`，不读取设备、不发送 IO。true 只证明存在软件实现；设备支持未知、实际参数不兼容或权限/连接失败仍可能导致调用失败。

文档 ID：`apple:symbol:s:10PrinterSDK14FeatureSupportV13isImplementedSbvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Bool | `s:Sb` |

<a id="api-49ce3c417a36013b07c6"></a>

### FeatureSupport.isSupported

是否同时具备 SDK 实现和已知设备支持。

```swift
var isSupported: Bool { get }
```

当前判断等同 `isImplemented && deviceSupport == 2`，不会把未知支持视为 true。false 包含未实现、未知和明确不支持多种情形，需结合原始字段区分；true 也不能替代参数校验或逐次操作结果。

文档 ID：`apple:symbol:s:10PrinterSDK14FeatureSupportV11isSupportedSbvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Bool | `s:Sb` |

<a id="api-cec17b5e240f3a9421b6"></a>

### FeatureSupport.knownFields

功能knownFields位集合。

```swift
let knownFields: UInt32
```

VALUES_KNOWN=1、REQUIRED_FIELDS_KNOWN=2、SUPPORTED_FIELDS_KNOWN=4，先查此位再解释对应字段，零不等于空集合。

文档 ID：`apple:symbol:s:10PrinterSDK14FeatureSupportV11knownFieldss6UInt32Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-4988ef01e31d9294c5c9"></a>

### FeatureSupport.requiredFields

功能requiredFields位集合。

```swift
let requiredFields: UInt32
```

MEDIA场景的必需字段mask；仅knownFields的REQUIRED_FIELDS_KNOWN有效时可解释。当前CPCL介质设置只支持tracking。

文档 ID：`apple:symbol:s:10PrinterSDK14FeatureSupportV14requiredFieldss6UInt32Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-c11ea825ce32570601d9"></a>

### FeatureSupport.supportedFields

功能supportedFields位集合。

```swift
let supportedFields: UInt32
```

MEDIA场景可接受的字段mask；仅SUPPORTED_FIELDS_KNOWN有效时可解释。未知不能推断全部可用。

文档 ID：`apple:symbol:s:10PrinterSDK14FeatureSupportV15supportedFieldss6UInt32Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-331e4f49e6471771ee44"></a>

### FeatureSupport.supportedValues

功能supportedValues位集合。

```swift
let supportedValues: UInt32
```

功能允许的枚举值mask，仅VALUES_KNOWN有效时可解释；CUT按FULL/PARTIAL位，IMAGE按算法值，MEDIA按tracking值。实现与硬件支持仍分开。

文档 ID：`apple:symbol:s:10PrinterSDK14FeatureSupportV15supportedValuess6UInt32Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-8da32930540a97e6bfd9"></a>

## FinishScope

<a id="api-e4878aa730e1148172be"></a>

### FinishScope

小票结束留白和切纸请求的执行范围。

```swift
enum FinishScope
```

配合 `submitReceiptGray8(finishScope:)` 使用；范围影响整单长度预算与结束动作次数，不改变份数。不支持的策略组合在 IO 前明确拒绝，不能把多份拆为循环入队来替代一个原子队列条目。

文档 ID：`apple:symbol:s:10PrinterSDK11FinishScopeO`

符号种类：`swift.enum`；访问级别：`public`。

<a id="api-176630565f0c8734e6d9"></a>

### FinishScope.!=(_:_:)

比较两个 FinishScope 值是否不相等。

```swift
static func != (lhs: borrowing Self, rhs: borrowing Self) -> Bool
```

Swift 标准库 `Equatable` 扩展为此类型提供的合成公开成员，不是独立 SDK 实现。参数 `lhs`、`rhs` 为两个值；枚举项不同时返回 true，否则 false。不抛出错误、不执行 IO，也不比较设备状态或诊断文案。

文档 ID：`apple:symbol:s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK11FinishScopeO`

符号种类：`swift.func.op`；访问级别：`public`。

这是编译器为当前类型带入的协议扩展／合成公共成员。

扩展来源（保留基线原值）：

```json
{"extendedModule":"Swift","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Bool | `s:Sb` |

<a id="api-139fcb246390b5ee8234"></a>

### FinishScope.eachCopy

每份内容后执行收尾留白及切纸请求。

```swift
case eachCopy
```

为默认值；整单预算包含每一份的 `trailingBlankDots`。同一操作包含所有份数，取消或失败后不自动补打剩余份。切纸仍受独立能力限制。

文档 ID：`apple:symbol:s:10PrinterSDK11FinishScopeO8eachCopyyA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-289057185bc6f71d3ce4"></a>

### FinishScope.endOfJob

全部份数之后执行一次收尾动作。

```swift
case endOfJob
```

整单预算仅累加一次结束留白，但当前标准 ESC/POS 后端仅支持 `.eachCopy`，本选项即使只有一份也会在 IO 前抛 1004，不会静默改为逐份。

文档 ID：`apple:symbol:s:10PrinterSDK11FinishScopeO8endOfJobyA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-a23c935c1ac70f053865"></a>

### FinishScope.hash(into:)

把 FinishScope 的相等性相关值混入 Hasher。

```swift
func hash(into hasher: inout Hasher)
```

Swift 标准库为原始值枚举提供的合成成员，不是 SDK 实现。`hasher` 为调用方按 `inout` 传入的 `Hasher`，方法更新其状态，没有返回值且不抛出错误。用于 `Hashable` 集合语义，哈希结果不是跨进程稳定序列化格式，不涉及设备 IO。

文档 ID：`apple:symbol:s:SYsSHRzSH8RawValueSYRpzrlE4hash4intoys6HasherVz_tF::SYNTHESIZED::s:10PrinterSDK11FinishScopeO`

符号种类：`swift.method`；访问级别：`public`。

这是编译器为当前类型带入的协议扩展／合成公共成员。

泛型约束（保留基线原值）：

```json
{"constraints":[{"kind":"conformance","lhs":"Self","rhs":"Hashable","rhsPrecise":"s:SH"},{"kind":"conformance","lhs":"Self.RawValue","rhs":"Hashable","rhsPrecise":"s:SH"}]}
```

扩展来源（保留基线原值）：

```json
{"constraints":[{"kind":"conformance","lhs":"Self","rhs":"Hashable","rhsPrecise":"s:SH"},{"kind":"conformance","lhs":"Self.RawValue","rhs":"Hashable","rhsPrecise":"s:SH"}],"extendedModule":"Swift","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Hasher | `s:s6HasherV` |

<a id="api-e5ff4abeaf1932e95561"></a>

### FinishScope.hashValue

获取 FinishScope 的进程内哈希值。

```swift
var hashValue: Int { get }
```

Swift 标准库为原始值枚举提供的合成 `Hashable` 成员，不是 SDK 稳定编号。用于集合/字典查找；不要持久化、传输、作为 API 原始值或在不同进程/版本间比较。需要契约数值时使用枚举原始值语义。

文档 ID：`apple:symbol:s:SYsSHRzSH8RawValueSYRpzrlE04hashB0Sivp::SYNTHESIZED::s:10PrinterSDK11FinishScopeO`

符号种类：`swift.property`；访问级别：`public`。

这是编译器为当前类型带入的协议扩展／合成公共成员。

扩展来源（保留基线原值）：

```json
{"constraints":[{"kind":"conformance","lhs":"Self","rhs":"Hashable","rhsPrecise":"s:SH"},{"kind":"conformance","lhs":"Self.RawValue","rhs":"Hashable","rhsPrecise":"s:SH"}],"extendedModule":"Swift","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Int | `s:Si` |

<a id="api-747ce5e305b31ea61b90"></a>

### FinishScope.init(rawValue:)

按已声明raw值构造枚举。

```swift
init?(rawValue: UInt32)
```

匹配本版枚举值时返回对应项，未知数值返回nil；不执行IO、不探测设备，也不自动接受未来枚举。精确raw值见本页编译器枚举表。

文档 ID：`apple:symbol:s:10PrinterSDK11FinishScopeO8rawValueACSgs6UInt32V_tcfc`

符号种类：`swift.init`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-712e0c363892c35925d7"></a>

## HardwareState

<a id="api-531c2774072fab40e06d"></a>

### HardwareState

打印机硬件状态枚举。

```swift
enum HardwareState
```

当前仅 unknown=0，必须保持未知展示；后端未实现硬件查询，不推断就绪、缺纸或打印完成。

文档 ID：`apple:symbol:s:10PrinterSDK13HardwareStateO`

符号种类：`swift.enum`；访问级别：`public`。

<a id="api-397829ebb06b0ec63c8b"></a>

### HardwareState.!=(_:_:)

编译器提供的值不等比较。

```swift
static func != (lhs: borrowing Self, rhs: borrowing Self) -> Bool
```

比较两个同类型快照或状态的值；只反映已采样值差异，不刷新设备状态或执行打印机 IO。

文档 ID：`apple:symbol:s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK13HardwareStateO`

符号种类：`swift.func.op`；访问级别：`public`。

这是编译器为当前类型带入的协议扩展／合成公共成员。

扩展来源（保留基线原值）：

```json
{"extendedModule":"Swift","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Bool | `s:Sb` |

<a id="api-eb13be33f52d4c5fbe10"></a>

### HardwareState.hash(into:)

编译器提供的枚举哈希计算。

```swift
func hash(into hasher: inout Hasher)
```

由 Hashable 协议合成，仅向当前进程 hasher 添加值，不触发状态查询或打印机 IO。

文档 ID：`apple:symbol:s:SYsSHRzSH8RawValueSYRpzrlE4hash4intoys6HasherVz_tF::SYNTHESIZED::s:10PrinterSDK13HardwareStateO`

符号种类：`swift.method`；访问级别：`public`。

这是编译器为当前类型带入的协议扩展／合成公共成员。

泛型约束（保留基线原值）：

```json
{"constraints":[{"kind":"conformance","lhs":"Self","rhs":"Hashable","rhsPrecise":"s:SH"},{"kind":"conformance","lhs":"Self.RawValue","rhs":"Hashable","rhsPrecise":"s:SH"}]}
```

扩展来源（保留基线原值）：

```json
{"constraints":[{"kind":"conformance","lhs":"Self","rhs":"Hashable","rhsPrecise":"s:SH"},{"kind":"conformance","lhs":"Self.RawValue","rhs":"Hashable","rhsPrecise":"s:SH"}],"extendedModule":"Swift","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Hasher | `s:s6HasherV` |

<a id="api-e0bd33e6c35c7e0035f6"></a>

### HardwareState.hashValue

编译器提供的枚举哈希值。

```swift
var hashValue: Int { get }
```

仅用于当前进程集合操作；不稳定，不能代替 raw 值持久化或作为设备状态协议。

文档 ID：`apple:symbol:s:SYsSHRzSH8RawValueSYRpzrlE04hashB0Sivp::SYNTHESIZED::s:10PrinterSDK13HardwareStateO`

符号种类：`swift.property`；访问级别：`public`。

这是编译器为当前类型带入的协议扩展／合成公共成员。

扩展来源（保留基线原值）：

```json
{"constraints":[{"kind":"conformance","lhs":"Self","rhs":"Hashable","rhsPrecise":"s:SH"},{"kind":"conformance","lhs":"Self.RawValue","rhs":"Hashable","rhsPrecise":"s:SH"}],"extendedModule":"Swift","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Int | `s:Si` |

<a id="api-b2f6904678ca7764b9a5"></a>

### HardwareState.init(rawValue:)

按已声明raw值构造枚举。

```swift
init?(rawValue: UInt32)
```

匹配本版枚举值时返回对应项，未知数值返回nil；不执行IO、不探测设备，也不自动接受未来枚举。精确raw值见本页编译器枚举表。

文档 ID：`apple:symbol:s:10PrinterSDK13HardwareStateO8rawValueACSgs6UInt32V_tcfc`

符号种类：`swift.init`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-ed2755a34dd467c1fba5"></a>

### HardwareState.unknown

硬件状态未知。

```swift
case unknown
```

UInt32 raw 值为 0；当前标准后端返回此值，硬件条件位为 0、采样时间为 nil。

文档 ID：`apple:symbol:s:10PrinterSDK13HardwareStateO7unknownyA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-806056f7885dc5612709"></a>

## HardwareStatusResult

<a id="api-c3f1c4c9b6ee23b753ae"></a>

### HardwareStatusResult

成功硬件状态查询的不可变结果。

```swift
struct HardwareStatusResult
```

仅成功且 responseValid 的状态 operation 可读取；knownConditions 表示已知位，activeConditions 仅在已知位内有意义。未知位不能推断正常；连接代次和采样时间用于判断结果是否过期。当前标准后端不提供硬件结果。

文档 ID：`apple:symbol:s:10PrinterSDK20HardwareStatusResultV`

符号种类：`swift.struct`；访问级别：`public`。

<a id="api-70fc335f763ddf43477e"></a>

### HardwareStatusResult.!=(_:_:)

按值比较两个 HardwareStatusResult 是否不同。

```swift
static func != (lhs: borrowing Self, rhs: borrowing Self) -> Bool
```

Swift Equatable 提供的比较，不执行 IO、不刷新状态。比较本次值或枚举，不能把相等的快照当作设备仍在线、硬件正常或纸面打印完成的证明。

文档 ID：`apple:symbol:s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK20HardwareStatusResultV`

符号种类：`swift.func.op`；访问级别：`public`。

这是编译器为当前类型带入的协议扩展／合成公共成员。

扩展来源（保留基线原值）：

```json
{"extendedModule":"Swift","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Bool | `s:Sb` |

<a id="api-d28e164baa755463f3bf"></a>

### HardwareStatusResult.activeConditions

已知条件中当前激活的状态位。

```swift
let activeConditions: UInt32
```

只在 knownConditions 内有效；不能从零值推出所有硬件状态正常。

文档 ID：`apple:symbol:s:10PrinterSDK20HardwareStatusResultV16activeConditionss6UInt32Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-71c6cbd8bccf80123164"></a>

### HardwareStatusResult.connectionGeneration

结果所属的成功连接代次。

```swift
let connectionGeneration: UInt64
```

断开或重连后旧结果不能作为新连接的当前硬件状态。

文档 ID：`apple:symbol:s:10PrinterSDK20HardwareStatusResultV20connectionGenerations6UInt64Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt64 | `s:s6UInt64V` |

<a id="api-6c219c5d2216894585d9"></a>

### HardwareStatusResult.evidence

硬件状态结果的证据来源。

```swift
let evidence: UInt32
```

由原生结果复制；不将 SDK 排队或 DATA_SENT 当作硬件状态依据。

文档 ID：`apple:symbol:s:10PrinterSDK20HardwareStatusResultV8evidences6UInt32Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-6c29279c0c9656970591"></a>

### HardwareStatusResult.knownConditions

本次响应明确报告的状态位掩码。

```swift
let knownConditions: UInt32
```

只对置位条件解释 activeConditions；未置位条件保持未知。

文档 ID：`apple:symbol:s:10PrinterSDK20HardwareStatusResultV15knownConditionss6UInt32Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-c14e22e843149533a912"></a>

### HardwareStatusResult.sampledMonotonicMilliseconds

状态采样的进程单调时间，单位毫秒。

```swift
let sampledMonotonicMilliseconds: UInt64
```

用于同进程的相对陈旧度判断，不是墙上时间。

文档 ID：`apple:symbol:s:10PrinterSDK20HardwareStatusResultV28sampledMonotonicMillisecondss6UInt64Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt64 | `s:s6UInt64V` |

<a id="api-41361c67b37b815a38ae"></a>

## ImageAlgorithm

<a id="api-eb40e0bf1994e99d4591"></a>

### ImageAlgorithm

图像二值化或抖动算法。

```swift
enum ImageAlgorithm
```

threshold=0、floydSteinberg=1、bayer4x4=2、bayer8x8=3；算法属于图像处理，不是打印机协议或硬件能力。

文档 ID：`apple:symbol:s:10PrinterSDK14ImageAlgorithmO`

符号种类：`swift.enum`；访问级别：`public`。

<a id="api-8d19f4fbd2529fbb7278"></a>

### ImageAlgorithm.!=(_:_:)

编译器提供的值不等比较。

```swift
static func != (lhs: borrowing Self, rhs: borrowing Self) -> Bool
```

按该值类型的Equatable语义比较，返回布尔值，不执行设备IO。比较相同状态或结果不代表设备当前在线或机械打印完成。

文档 ID：`apple:symbol:s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK14ImageAlgorithmO`

符号种类：`swift.func.op`；访问级别：`public`。

这是编译器为当前类型带入的协议扩展／合成公共成员。

扩展来源（保留基线原值）：

```json
{"extendedModule":"Swift","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Bool | `s:Sb` |

<a id="api-d6ec14ad4f4093b37d35"></a>

### ImageAlgorithm.bayer4x4

选择bayer4x4图像算法。

```swift
case bayer4x4
```

4×4 Bayer有序抖动。处理在SDK图像流水线执行，不发送型号专属命令。

文档 ID：`apple:symbol:s:10PrinterSDK14ImageAlgorithmO8bayer4x4yA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-6945005b41984b7b4eeb"></a>

### ImageAlgorithm.bayer8x8

选择bayer8x8图像算法。

```swift
case bayer8x8
```

8×8 Bayer有序抖动。处理在SDK图像流水线执行，不发送型号专属命令。

文档 ID：`apple:symbol:s:10PrinterSDK14ImageAlgorithmO8bayer8x8yA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-be0ae4ffc2dd3677f1f6"></a>

### ImageAlgorithm.floydSteinberg

选择floydSteinberg图像算法。

```swift
case floydSteinberg
```

Floyd–Steinberg误差扩散。处理在SDK图像流水线执行，不发送型号专属命令。

文档 ID：`apple:symbol:s:10PrinterSDK14ImageAlgorithmO14floydSteinbergyA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-456c05d0e75fa3d9e20f"></a>

### ImageAlgorithm.hash(into:)

编译器提供的枚举散列。

```swift
func hash(into hasher: inout Hasher)
```

用于Swift集合与字典，按当前枚举raw值生成；散列不稳定，不能持久化为协议值、设备身份或跨进程标识，不执行IO。

文档 ID：`apple:symbol:s:SYsSHRzSH8RawValueSYRpzrlE4hash4intoys6HasherVz_tF::SYNTHESIZED::s:10PrinterSDK14ImageAlgorithmO`

符号种类：`swift.method`；访问级别：`public`。

这是编译器为当前类型带入的协议扩展／合成公共成员。

泛型约束（保留基线原值）：

```json
{"constraints":[{"kind":"conformance","lhs":"Self","rhs":"Hashable","rhsPrecise":"s:SH"},{"kind":"conformance","lhs":"Self.RawValue","rhs":"Hashable","rhsPrecise":"s:SH"}]}
```

扩展来源（保留基线原值）：

```json
{"constraints":[{"kind":"conformance","lhs":"Self","rhs":"Hashable","rhsPrecise":"s:SH"},{"kind":"conformance","lhs":"Self.RawValue","rhs":"Hashable","rhsPrecise":"s:SH"}],"extendedModule":"Swift","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Hasher | `s:s6HasherV` |

<a id="api-3363ed8986a4951eb034"></a>

### ImageAlgorithm.hashValue

编译器提供的枚举散列。

```swift
var hashValue: Int { get }
```

用于Swift集合与字典，按当前枚举raw值生成；散列不稳定，不能持久化为协议值、设备身份或跨进程标识，不执行IO。

文档 ID：`apple:symbol:s:SYsSHRzSH8RawValueSYRpzrlE04hashB0Sivp::SYNTHESIZED::s:10PrinterSDK14ImageAlgorithmO`

符号种类：`swift.property`；访问级别：`public`。

这是编译器为当前类型带入的协议扩展／合成公共成员。

扩展来源（保留基线原值）：

```json
{"constraints":[{"kind":"conformance","lhs":"Self","rhs":"Hashable","rhsPrecise":"s:SH"},{"kind":"conformance","lhs":"Self.RawValue","rhs":"Hashable","rhsPrecise":"s:SH"}],"extendedModule":"Swift","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Int | `s:Si` |

<a id="api-f728255c350011718d01"></a>

### ImageAlgorithm.init(rawValue:)

按已声明raw值构造枚举。

```swift
init?(rawValue: UInt32)
```

匹配本版枚举值时返回对应项，未知数值返回nil；不执行IO、不探测设备，也不自动接受未来枚举。精确raw值见本页编译器枚举表。

文档 ID：`apple:symbol:s:10PrinterSDK14ImageAlgorithmO8rawValueACSgs6UInt32V_tcfc`

符号种类：`swift.init`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-a85d93817ab96637e0b3"></a>

### ImageAlgorithm.threshold

选择threshold图像算法。

```swift
case threshold
```

阈值二值化，默认threshold=128。处理在SDK图像流水线执行，不发送型号专属命令。

文档 ID：`apple:symbol:s:10PrinterSDK14ImageAlgorithmO9thresholdyA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-9cd007436b5a242686f9"></a>

## LabelJobOptions

<a id="api-34d851c149bbdf909c5c"></a>

### LabelJobOptions

标签作业的不可变布局选项。

```swift
struct LabelJobOptions
```

只保存参数，build/submit进行完整校验；宽高和DPI大于零，copies为1..100，原点非负，不自动裁剪。页面高度是一份输出，form描述本次结束位置。

文档 ID：`apple:symbol:s:10PrinterSDK15LabelJobOptionsV`

符号种类：`swift.struct`；访问级别：`public`。

<a id="api-408f66dc4d26cc013ed6"></a>

### LabelJobOptions.copies

份数，默认1、范围1..100。

```swift
let copies: UInt32
```

初始化后不可变；属于作业参数而非连接默认配置，最终有效性由build/submit校验。

文档 ID：`apple:symbol:s:10PrinterSDK15LabelJobOptionsV6copiess6UInt32Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-0826e3485b67092d8ac7"></a>

### LabelJobOptions.dpi

标签横纵DPI。

```swift
let dpi: UInt32
```

初始化后不可变；属于作业参数而非连接默认配置，最终有效性由build/submit校验。

文档 ID：`apple:symbol:s:10PrinterSDK15LabelJobOptionsV3dpis6UInt32Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-a5681b14608ca076bac0"></a>

### LabelJobOptions.form

true请求下一标签起点结束；false在内容结束。

```swift
let form: Bool
```

初始化后不可变；属于作业参数而非连接默认配置，最终有效性由build/submit校验。

文档 ID：`apple:symbol:s:10PrinterSDK15LabelJobOptionsV4formSbvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Bool | `s:Sb` |

<a id="api-e6d70734b080512e93ec"></a>

### LabelJobOptions.heightDots

每份标签声明高度（点）。

```swift
let heightDots: UInt32
```

初始化后不可变；属于作业参数而非连接默认配置，最终有效性由build/submit校验。

文档 ID：`apple:symbol:s:10PrinterSDK15LabelJobOptionsV10heightDotss6UInt32Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-4b761d5230ae97b83116"></a>

### LabelJobOptions.init(widthDots:heightDots:dpi:originXDots:originYDots:copies:form:)

保存LabelJobOptions的不可变作业参数。

```swift
init(widthDots: UInt32, heightDots: UInt32, dpi: UInt32, originXDots: Int32 = 0, originYDots: Int32 = 0, copies: UInt32 = 1, form: Bool = false)
```

只保存参数，build/submit进行完整校验；宽高和DPI大于零，copies为1..100，原点非负，不自动裁剪。页面高度是一份输出，form描述本次结束位置。 此初始化不执行IO，也不提前证明参数或硬件支持；实际校验在Builder.build与MobilePrinter.submit进行。

文档 ID：`apple:symbol:s:10PrinterSDK15LabelJobOptionsV9widthDots06heightG03dpi11originXDots0J5YDots6copies4formACs6UInt32V_A2Ls5Int32VAnLSbtcfc`

符号种类：`swift.init`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Bool | `s:Sb` |
| Int32 | `s:s5Int32V` |
| UInt32 | `s:s6UInt32V` |

<a id="api-8f8763f81d12828bc071"></a>

### LabelJobOptions.originXDots

非负横向原点（点）。

```swift
let originXDots: Int32
```

初始化后不可变；属于作业参数而非连接默认配置，最终有效性由build/submit校验。

文档 ID：`apple:symbol:s:10PrinterSDK15LabelJobOptionsV11originXDotss5Int32Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Int32 | `s:s5Int32V` |

<a id="api-33545797ae4117bfce58"></a>

### LabelJobOptions.originYDots

非负纵向原点（点）。

```swift
let originYDots: Int32
```

初始化后不可变；属于作业参数而非连接默认配置，最终有效性由build/submit校验。

文档 ID：`apple:symbol:s:10PrinterSDK15LabelJobOptionsV11originYDotss5Int32Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Int32 | `s:s5Int32V` |

<a id="api-cca1f2750f44dc7b35f8"></a>

### LabelJobOptions.widthDots

标签画布宽度（点）。

```swift
let widthDots: UInt32
```

初始化后不可变；属于作业参数而非连接默认配置，最终有效性由build/submit校验。

文档 ID：`apple:symbol:s:10PrinterSDK15LabelJobOptionsV9widthDotss6UInt32Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-c09da0bc38ed43e7aef2"></a>

## M90Profile

<a id="api-36e6668b567033ab4cff"></a>

### M90Profile

提供依据 M90 固件 V1.2 文档的 BLE 配置工厂。

```swift
enum M90Profile
```

该枚举用作静态命名空间，没有可供创建实例的公开枚举项。当前只提供 BLE 服务/写入/Credit 特征预设，DPI、点宽、标签高度及 FORM 仍由应用明确提供。选择此预设不表示 M90 或任意硬件已通过本 SDK 验收。

文档 ID：`apple:symbol:s:10PrinterSDK10M90ProfileO`

符号种类：`swift.enum`；访问级别：`public`。

<a id="api-5f602b8f4d618a37f4f0"></a>

### M90Profile.bleOptions(maxWriteBytes:)

生成 M90 文档 V1.2 的无响应写入和 Credit 配置。

```swift
static func bleOptions(maxWriteBytes: Int = 512) throws -> BLEOptions
```

`maxWriteBytes` 默认 `512`，单位字节，接受 1…512，否则抛 1001。返回服务 FF00、写入 FF02、Data Out FF01、Data FC FF03 对应的标准 Bluetooth Base UUID，并启用无响应写入及 Credit；完整 UUID 形式为 `0000xxxx-0000-1000-8000-00805F9B34FB`。实际发送包长仍由系统写入容量、固件公告与应用上限共同限制。工厂不进行扫描、连接或 IO，不预置固定初始 Credit，不确认机械打印能力。

文档 ID：`apple:symbol:s:10PrinterSDK10M90ProfileO10bleOptions13maxWriteBytesAA10BLEOptionsVSi_tKFZ`

符号种类：`swift.type.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| BLEOptions | `s:10PrinterSDK10BLEOptionsV` |
| Int | `s:Si` |

<a id="api-e9f6609ce460a51cc89f"></a>

## MediaMode

<a id="api-16bbe8e3b5e45eac13b0"></a>

### MediaMode

与打印语言独立的介质跟踪模式。

```swift
enum MediaMode
```

传给 `setMediaMode`；当前只有 CPCL 标签设备提供对应编码，不能因为枚举项存在就推断其他后端或固件支持。设置发送不确认已应用，不设置标签图像尺寸或执行传感器校准。

文档 ID：`apple:symbol:s:10PrinterSDK9MediaModeO`

符号种类：`swift.enum`；访问级别：`public`。

<a id="api-2b58768b3d9fd4906ea0"></a>

### MediaMode.!=(_:_:)

比较两个 MediaMode 值是否不相等。

```swift
static func != (lhs: borrowing Self, rhs: borrowing Self) -> Bool
```

Swift 标准库 `Equatable` 扩展为此类型提供的合成公开成员，不是独立 SDK 实现。参数 `lhs`、`rhs` 为两个值；枚举项不同时返回 true，否则 false。不抛出错误、不执行 IO，也不比较设备状态或诊断文案。

文档 ID：`apple:symbol:s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK9MediaModeO`

符号种类：`swift.func.op`；访问级别：`public`。

这是编译器为当前类型带入的协议扩展／合成公共成员。

扩展来源（保留基线原值）：

```json
{"extendedModule":"Swift","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Bool | `s:Sb` |

<a id="api-453392472f7fd260ebae"></a>

### MediaMode.blackMark

请求按黑标跟踪介质。

```swift
case blackMark
```

用于实际具备相应传感与固件支持的黑标纸；不设置标记长度/偏移，不执行校准或读回。发送成功不能证明检测到黑标。

文档 ID：`apple:symbol:s:10PrinterSDK9MediaModeO9blackMarkyA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-a395d1df3275fc067674"></a>

### MediaMode.continuous

请求连续纸介质模式。

```swift
case continuous
```

不依赖间隙/黑标定位的模式设置请求；它与标签作业的 `form` 结束策略、画布尺寸是独立参数，不自动改写已有作业或推断纸长。

文档 ID：`apple:symbol:s:10PrinterSDK9MediaModeO10continuousyA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-65190e7c3660ac4b065e"></a>

### MediaMode.gap

请求按间隙跟踪介质。

```swift
case gap
```

用于具有相应传感与固件支持的标签介质；不设置间隙毫米尺寸，不执行校准或验证装纸。是否实际应用需设备/硬件证据。

文档 ID：`apple:symbol:s:10PrinterSDK9MediaModeO3gapyA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-aee116a2c72751661c7a"></a>

### MediaMode.hash(into:)

把 MediaMode 的相等性相关值混入 Hasher。

```swift
func hash(into hasher: inout Hasher)
```

Swift 标准库为原始值枚举提供的合成成员，不是 SDK 实现。`hasher` 为调用方按 `inout` 传入的 `Hasher`，方法更新其状态，没有返回值且不抛出错误。用于 `Hashable` 集合语义，哈希结果不是跨进程稳定序列化格式，不涉及设备 IO。

文档 ID：`apple:symbol:s:SYsSHRzSH8RawValueSYRpzrlE4hash4intoys6HasherVz_tF::SYNTHESIZED::s:10PrinterSDK9MediaModeO`

符号种类：`swift.method`；访问级别：`public`。

这是编译器为当前类型带入的协议扩展／合成公共成员。

泛型约束（保留基线原值）：

```json
{"constraints":[{"kind":"conformance","lhs":"Self","rhs":"Hashable","rhsPrecise":"s:SH"},{"kind":"conformance","lhs":"Self.RawValue","rhs":"Hashable","rhsPrecise":"s:SH"}]}
```

扩展来源（保留基线原值）：

```json
{"constraints":[{"kind":"conformance","lhs":"Self","rhs":"Hashable","rhsPrecise":"s:SH"},{"kind":"conformance","lhs":"Self.RawValue","rhs":"Hashable","rhsPrecise":"s:SH"}],"extendedModule":"Swift","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Hasher | `s:s6HasherV` |

<a id="api-af5ef87066699f13cab7"></a>

### MediaMode.hashValue

获取 MediaMode 的进程内哈希值。

```swift
var hashValue: Int { get }
```

Swift 标准库为原始值枚举提供的合成 `Hashable` 成员，不是 SDK 稳定编号。用于集合/字典查找；不要持久化、传输、作为 API 原始值或在不同进程/版本间比较。需要契约数值时使用枚举原始值语义。

文档 ID：`apple:symbol:s:SYsSHRzSH8RawValueSYRpzrlE04hashB0Sivp::SYNTHESIZED::s:10PrinterSDK9MediaModeO`

符号种类：`swift.property`；访问级别：`public`。

这是编译器为当前类型带入的协议扩展／合成公共成员。

扩展来源（保留基线原值）：

```json
{"constraints":[{"kind":"conformance","lhs":"Self","rhs":"Hashable","rhsPrecise":"s:SH"},{"kind":"conformance","lhs":"Self.RawValue","rhs":"Hashable","rhsPrecise":"s:SH"}],"extendedModule":"Swift","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Int | `s:Si` |

<a id="api-ca7c6cc262ae54bb1700"></a>

### MediaMode.init(rawValue:)

按已声明raw值构造枚举。

```swift
init?(rawValue: UInt32)
```

匹配本版枚举值时返回对应项，未知数值返回nil；不执行IO、不探测设备，也不自动接受未来枚举。精确raw值见本页编译器枚举表。

文档 ID：`apple:symbol:s:10PrinterSDK9MediaModeO8rawValueACSgs6UInt32V_tcfc`

符号种类：`swift.init`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-623f02309548c44d0ebb"></a>

## MediaRequirement

<a id="api-6ab82cb7819671ca0578"></a>

### MediaRequirement

显式介质前置要求。

```swift
struct MediaRequirement
```

设为nil表示没有前置要求；提供值时tracking必须存在，其他nil字段未指定。只允许已有发送证据或明确读回；当前仅CPCL tracking+allowLastSent可提交，其他字段/读回要求1004拒绝且零IO。

文档 ID：`apple:symbol:s:10PrinterSDK16MediaRequirementV`

符号种类：`swift.struct`；访问级别：`public`。

<a id="api-bc320e05f07d87edf3d4"></a>

### MediaRequirement.gapLengthMicrometers

可选间隙长度（微米），仅gap模式。

```swift
let gapLengthMicrometers: UInt32?
```

nil表示未指定，不能当作设备报告；存在的宽度/长度须为正并与tracking匹配。当前后端只支持tracking及allowLastSent，其他要求明确拒绝。

文档 ID：`apple:symbol:s:10PrinterSDK16MediaRequirementV20gapLengthMicrometerss6UInt32VSgvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-c1b0df34326b9632e0a7"></a>

### MediaRequirement.init(tracking:widthMicrometers:labelLengthMicrometers:gapLengthMicrometers:markLengthMicrometers:markOffsetMicrometers:verification:)

保存MediaRequirement的不可变作业参数。

```swift
init(tracking: MediaMode? = nil, widthMicrometers: UInt32? = nil, labelLengthMicrometers: UInt32? = nil, gapLengthMicrometers: UInt32? = nil, markLengthMicrometers: UInt32? = nil, markOffsetMicrometers: Int32? = nil, verification: MediaVerificationPolicy = .allowLastSent)
```

设为nil表示没有前置要求；提供值时tracking必须存在，其他nil字段未指定。只允许已有发送证据或明确读回；当前仅CPCL tracking+allowLastSent可提交，其他字段/读回要求1004拒绝且零IO。 此初始化不执行IO，也不提前证明参数或硬件支持；实际校验在Builder.build与MobilePrinter.submit进行。

文档 ID：`apple:symbol:s:10PrinterSDK16MediaRequirementV8tracking16widthMicrometers011labelLengthG003gapiG004markiG00k6OffsetG012verificationAcA0C4ModeOSg_s6UInt32VSgA3Ps5Int32VSgAA0C18VerificationPolicyOtcfc`

符号种类：`swift.init`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Int32 | `s:s5Int32V` |
| MediaMode | `s:10PrinterSDK9MediaModeO` |
| MediaVerificationPolicy | `s:10PrinterSDK23MediaVerificationPolicyO` |
| UInt32 | `s:s6UInt32V` |

<a id="api-57f722d1d95fdba39e10"></a>

### MediaRequirement.labelLengthMicrometers

可选标签长度（微米）。

```swift
let labelLengthMicrometers: UInt32?
```

nil表示未指定，不能当作设备报告；存在的宽度/长度须为正并与tracking匹配。当前后端只支持tracking及allowLastSent，其他要求明确拒绝。

文档 ID：`apple:symbol:s:10PrinterSDK16MediaRequirementV22labelLengthMicrometerss6UInt32VSgvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-864e525223aa04aa5fba"></a>

### MediaRequirement.markLengthMicrometers

可选黑标长度（微米），仅blackMark模式。

```swift
let markLengthMicrometers: UInt32?
```

nil表示未指定，不能当作设备报告；存在的宽度/长度须为正并与tracking匹配。当前后端只支持tracking及allowLastSent，其他要求明确拒绝。

文档 ID：`apple:symbol:s:10PrinterSDK16MediaRequirementV21markLengthMicrometerss6UInt32VSgvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-9b19f3b49b288586aaed"></a>

### MediaRequirement.markOffsetMicrometers

可选黑标偏移（微米），仅blackMark模式。

```swift
let markOffsetMicrometers: Int32?
```

nil表示未指定，不能当作设备报告；存在的宽度/长度须为正并与tracking匹配。当前后端只支持tracking及allowLastSent，其他要求明确拒绝。

文档 ID：`apple:symbol:s:10PrinterSDK16MediaRequirementV21markOffsetMicrometerss5Int32VSgvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Int32 | `s:s5Int32V` |

<a id="api-25bd0ab9dda5cd9b9143"></a>

### MediaRequirement.tracking

必需的介质跟踪模式。

```swift
let tracking: MediaMode?
```

nil表示未指定，不能当作设备报告；存在的宽度/长度须为正并与tracking匹配。当前后端只支持tracking及allowLastSent，其他要求明确拒绝。

文档 ID：`apple:symbol:s:10PrinterSDK16MediaRequirementV8trackingAA0C4ModeOSgvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| MediaMode | `s:10PrinterSDK9MediaModeO` |

<a id="api-2b13f60eeae1024f115a"></a>

### MediaRequirement.verification

介质证据策略，默认allowLastSent。

```swift
let verification: MediaVerificationPolicy
```

allowLastSent只使用当前协议最近成功发送的介质记录，不是读回；requireReadback当前不支持，提交前1004拒绝。

文档 ID：`apple:symbol:s:10PrinterSDK16MediaRequirementV12verificationAA0C18VerificationPolicyOvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| MediaVerificationPolicy | `s:10PrinterSDK23MediaVerificationPolicyO` |

<a id="api-f14b9a4722aaa2d3a61f"></a>

### MediaRequirement.widthMicrometers

可选介质宽度（微米）。

```swift
let widthMicrometers: UInt32?
```

nil表示未指定，不能当作设备报告；存在的宽度/长度须为正并与tracking匹配。当前后端只支持tracking及allowLastSent，其他要求明确拒绝。

文档 ID：`apple:symbol:s:10PrinterSDK16MediaRequirementV16widthMicrometerss6UInt32VSgvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-afcd3a74fc08e909f85d"></a>

## MediaVerificationPolicy

<a id="api-e07fc2708245563b7f0b"></a>

### MediaVerificationPolicy

介质前置条件的证据选择。

```swift
enum MediaVerificationPolicy
```

allowLastSent=1允许SDK已成功发送的记录；requireReadback=2要求受支持的设备读回。策略不触发自动设置或猜测状态。

文档 ID：`apple:symbol:s:10PrinterSDK23MediaVerificationPolicyO`

符号种类：`swift.enum`；访问级别：`public`。

<a id="api-1cd83842b55b9ab5d029"></a>

### MediaVerificationPolicy.!=(_:_:)

编译器提供的值不等比较。

```swift
static func != (lhs: borrowing Self, rhs: borrowing Self) -> Bool
```

按该值类型的Equatable语义比较，返回布尔值，不执行设备IO。比较相同状态或结果不代表设备当前在线或机械打印完成。

文档 ID：`apple:symbol:s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK23MediaVerificationPolicyO`

符号种类：`swift.func.op`；访问级别：`public`。

这是编译器为当前类型带入的协议扩展／合成公共成员。

扩展来源（保留基线原值）：

```json
{"extendedModule":"Swift","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Bool | `s:Sb` |

<a id="api-aeab64eaf6f4a90e1bbb"></a>

### MediaVerificationPolicy.allowLastSent

允许匹配最近发送的介质记录。

```swift
case allowLastSent
```

只描述本次协议对应的SDK缓存证据，不证明固件实际配置，不能跨协议借用记录。

文档 ID：`apple:symbol:s:10PrinterSDK23MediaVerificationPolicyO13allowLastSentyA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-49891c4cb429ecf760e9"></a>

### MediaVerificationPolicy.hash(into:)

编译器提供的枚举散列。

```swift
func hash(into hasher: inout Hasher)
```

用于Swift集合与字典，按当前枚举raw值生成；散列不稳定，不能持久化为协议值、设备身份或跨进程标识，不执行IO。

文档 ID：`apple:symbol:s:SYsSHRzSH8RawValueSYRpzrlE4hash4intoys6HasherVz_tF::SYNTHESIZED::s:10PrinterSDK23MediaVerificationPolicyO`

符号种类：`swift.method`；访问级别：`public`。

这是编译器为当前类型带入的协议扩展／合成公共成员。

泛型约束（保留基线原值）：

```json
{"constraints":[{"kind":"conformance","lhs":"Self","rhs":"Hashable","rhsPrecise":"s:SH"},{"kind":"conformance","lhs":"Self.RawValue","rhs":"Hashable","rhsPrecise":"s:SH"}]}
```

扩展来源（保留基线原值）：

```json
{"constraints":[{"kind":"conformance","lhs":"Self","rhs":"Hashable","rhsPrecise":"s:SH"},{"kind":"conformance","lhs":"Self.RawValue","rhs":"Hashable","rhsPrecise":"s:SH"}],"extendedModule":"Swift","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Hasher | `s:s6HasherV` |

<a id="api-ad19059c6d7670b6ce57"></a>

### MediaVerificationPolicy.hashValue

编译器提供的枚举散列。

```swift
var hashValue: Int { get }
```

用于Swift集合与字典，按当前枚举raw值生成；散列不稳定，不能持久化为协议值、设备身份或跨进程标识，不执行IO。

文档 ID：`apple:symbol:s:SYsSHRzSH8RawValueSYRpzrlE04hashB0Sivp::SYNTHESIZED::s:10PrinterSDK23MediaVerificationPolicyO`

符号种类：`swift.property`；访问级别：`public`。

这是编译器为当前类型带入的协议扩展／合成公共成员。

扩展来源（保留基线原值）：

```json
{"constraints":[{"kind":"conformance","lhs":"Self","rhs":"Hashable","rhsPrecise":"s:SH"},{"kind":"conformance","lhs":"Self.RawValue","rhs":"Hashable","rhsPrecise":"s:SH"}],"extendedModule":"Swift","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Int | `s:Si` |

<a id="api-1a1a37ae0f2ef3ee9b57"></a>

### MediaVerificationPolicy.init(rawValue:)

按已声明raw值构造枚举。

```swift
init?(rawValue: UInt32)
```

匹配本版枚举值时返回对应项，未知数值返回nil；不执行IO、不探测设备，也不自动接受未来枚举。精确raw值见本页编译器枚举表。

文档 ID：`apple:symbol:s:10PrinterSDK23MediaVerificationPolicyO8rawValueACSgs6UInt32V_tcfc`

符号种类：`swift.init`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-7bacf537b3274c2275ea"></a>

### MediaVerificationPolicy.requireReadback

要求设备读回介质证据。

```swift
case requireReadback
```

当前标准后端未验证介质读回，提交返回1004且零IO；不能自动降级为最近发送记录。

文档 ID：`apple:symbol:s:10PrinterSDK23MediaVerificationPolicyO15requireReadbackyA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-ac14c812144bc8d503ff"></a>

## MobilePrinter

<a id="api-48451d5fee6a3c851247"></a>

### MobilePrinter

逐操作配置的打印机通信设备。

```swift
final class MobilePrinter
```

创建只需要BLE通信选项，不绑定打印Profile。打印、Raw、介质和定位共享设备唯一FIFO；结构化操作逐次提供配置。connect/disconnect/close会阻塞，应在后台执行；Manager提供异步生命周期。提交只表示接纳，成功最多为DATA_SENT。所有权由创建者或Manager管理；不自动重连、重放或续打。ABI3必须与同版原生库成套使用。 Swift 跨隔离访问采用 Sendable 契约，内部同步与原生存储生命周期保护保持；可变 App 状态须自行同步，阻塞 IO 仍应在后台执行。

文档 ID：`apple:symbol:s:10PrinterSDK06MobileA0C`

符号种类：`swift.class`；访问级别：`public`。

<a id="api-c6465a4526c29369267b"></a>

### MobilePrinter.capabilities()

读取聚合的本地能力与约束。

```swift
func capabilities() throws -> PrinterCapabilities
```

返回八项feature、四项constraint及geometry的不可变值，不执行IO、不探测或预留容量。软件实现、设备支持与证据独立；纯通信设备几何未知，不能从最近打印参数填充。设备关闭后抛1002。

文档 ID：`apple:symbol:s:10PrinterSDK06MobileA0C12capabilitiesAA0A12CapabilitiesVyKF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| PrinterCapabilities | `s:10PrinterSDK0A12CapabilitiesV` |

<a id="api-efb8cdaa74d4d828c4a7"></a>

### MobilePrinter.close()

阻塞并永久关闭此设备对象。

```swift
func close()
```

在应用工作线程调用。阻止后续设备访问，断开并中止未完成 IO，等待正在使用设备的调用退出后释放资源；并发重复关闭等待同一次关闭完成，重复调用安全。此方法不抛出错误，不能用于查询断开是否成功。关闭后的对象不能重新连接，需创建新对象；后续设备调用抛 1002。已有 `PrintOperation` 独立保存快照，仍可读终态并关闭。设备析构也执行关闭，因此应主动在合适的工作线程关闭以免最后一次引用释放阻塞 UI。已经发送的数据不会被撤回。

文档 ID：`apple:symbol:s:10PrinterSDK06MobileA0C5closeyyF`

符号种类：`swift.method`；访问级别：`public`。

<a id="api-48899b1c41f77aafe9a2"></a>

### MobilePrinter.connect(timeoutMilliseconds:)

阻塞建立选定外设的 BLE 通信路径。

```swift
func connect(timeoutMilliseconds: UInt32 = 5000) throws
```

`timeoutMilliseconds` 单位为毫秒，默认 `5000`，必须大于 0；零值抛 1001。必须在应用工作线程执行，不得占用 UI 或 CoreBluetooth 回调队列。已经连接时有效调用可直接成功，不重新打开传输；冲突的连接状态转换或已关闭设备抛 1002。蓝牙/权限/目标识别/特征不符合要求、超时或连接失败以 `PrinterSDKError` 返回（例如 1002、1004、2001、2002、2003）。成功只证明已建立可用通信路径，不证明纸张、机械状态或打印完成；不会重放此前中断的操作。

文档 ID：`apple:symbol:s:10PrinterSDK06MobileA0C7connect19timeoutMillisecondsys6UInt32V_tKF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-9cd746227b15bcf2cc8d"></a>

### MobilePrinter.connectionState()

读取 SDK 本地缓存的连接状态。

```swift
func connectionState() throws -> ConnectionState
```

返回 `ConnectionState`，不进行蓝牙探测、不发送打印机 IO，也不进入打印队列；可观察正在连接或断开的状态。`.connected` 不证明此刻链路仍存活或打印机就绪，未被 IO 察觉的断线可能暂时仍显示已连接。设备已关闭时抛 1002；无法识别的底层状态抛 1000，其他读取错误作为 `PrinterSDKError` 返回。

文档 ID：`apple:symbol:s:10PrinterSDK06MobileA0C15connectionStateAA010ConnectionE0OyKF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| ConnectionState | `s:10PrinterSDK15ConnectionStateO` |

<a id="api-12982da98fa0af6781d0"></a>

### MobilePrinter.constraint(_:)

读取指定参数的标量约束。

```swift
func constraint(_ constraint: PrinterConstraint) throws -> ScalarConstraint
```

parameterId 使用 ScalarConstraint 标识；本地查询无 IO，不保证硬件支持。known=false 表示未知。设备关闭后抛 1002，其他原生错误按 PrinterSDKError 返回。

文档 ID：`apple:symbol:s:10PrinterSDK06MobileA0C10constraintyAA16ScalarConstraintVAA0aF0OKF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| PrinterConstraint | `s:10PrinterSDK0A10ConstraintO` |
| ScalarConstraint | `s:10PrinterSDK16ScalarConstraintV` |

<a id="api-3c22bd2bb2a05a1bf9de"></a>

### MobilePrinter.diagnostic()

读取设备最新一条本地诊断。

```swift
func diagnostic() throws -> ErrorDiagnostic
```

不执行 IO，不清除记录；sequence=0 表示无记录。重连保留较早代次记录，展示前比较 generation。设备关闭后抛 1002；不替代本次 connect 的异常或 operation 的独立诊断。

文档 ID：`apple:symbol:s:10PrinterSDK06MobileA0C10diagnosticAA15ErrorDiagnosticVyKF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| ErrorDiagnostic | `s:10PrinterSDK15ErrorDiagnosticV` |

<a id="api-291abd150d7fdc40bbb7"></a>

### MobilePrinter.disconnect()

阻塞断开通信并使未完成工作结束。

```swift
func disconnect() throws
```

在应用工作线程调用。停止接纳工作，取消并中断进行中的 IO，等待相应工作退出；未完成操作会取得终态。重复断开是安全的，设备对象仍可通过后续显式 `connect` 使用。设备已经 `close` 后抛 1002，底层断开错误以 `PrinterSDKError` 表示。保留的操作对象仍能读取结果；断开不能撤回已发送字节，不自动重连或重放。

文档 ID：`apple:symbol:s:10PrinterSDK06MobileA0C10disconnectyyKF`

符号种类：`swift.method`；访问级别：`public`。

<a id="api-b47b4b75be0a4bba31ab"></a>

### MobilePrinter.feature(_:)

读取所选配置下一个功能的实现与支持证据。

```swift
func feature(_ feature: PrinterFeature) throws -> FeatureSupport
```

`feature` 选择 `PrinterFeature`；返回不可变的 `FeatureSupport`。读取使用本地能力数据，不发送打印机 IO，不验证硬件或改变队列。区分 SDK 已实现、设备支持已知与证据来源：当前显式 BLE 配置的设备支持仍可未知，`isSupported == false` 不能一律理解为确定不支持。已关闭设备抛 1002，其他错误抛 `PrinterSDKError`。当前标准后端未实现状态查询和切纸。 无 Profile 的通信连接提供两种协议的 SDK 实现信息，deviceSupport 保持 UNKNOWN、evidence=0，不能用实现存在推断设备支持。

文档 ID：`apple:symbol:s:10PrinterSDK06MobileA0C7featureyAA14FeatureSupportVAA0aE0OKF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| FeatureSupport | `s:10PrinterSDK14FeatureSupportV` |
| PrinterFeature | `s:10PrinterSDK0A7FeatureO` |

<a id="api-2ca3a7fe0ea7c8180c83"></a>

### MobilePrinter.feedToNextLabel(profile:timeoutMilliseconds:)

使用本次 CPCL 配置排入下一标签定位操作。

```swift
func feedToNextLabel(profile: CpclProfile, timeoutMilliseconds: UInt32 = 120000) throws -> PrintOperation
```

本次配置在接纳时按值复制，只属于这个队列条目，不绑定到连接、不记住最后一次配置，不改变连接的通信选项。可以在同一连接中连续排队不同协议或几何配置的操作，仍与 Raw 共用同一个 FIFO，不切换连接或自动重放。 `profile` 指定本次 CPCL 参数；`timeoutMilliseconds` 默认 120000，须大于 0，单位毫秒，覆盖排队和执行。调用者须先装入并配置合适的间隙或黑标介质；此操作不设置介质、不校准传感器、不读取纸张位置。返回需持有和关闭的 PrintOperation；零超时或无效配置为 1001，其他错误按 PrinterSDKError 报告，异步结果通过快照观察。成功 DATA_SENT 只证明定位命令已传输，不能证明物理定位完成。

文档 ID：`apple:symbol:s:10PrinterSDK06MobileA0C15feedToNextLabel7profile19timeoutMillisecondsAA14PrintOperationCAA11CpclProfileV_s6UInt32VtKF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| CpclProfile | `s:10PrinterSDK11CpclProfileV` |
| PrintOperation | `s:10PrinterSDK14PrintOperationC` |
| UInt32 | `s:s6UInt32V` |

<a id="api-185a1b4f89b6fc167870"></a>

### MobilePrinter.init(bleIdentifier:options:)

创建不绑定打印配置的BLE通信设备。

```swift
convenience init(bleIdentifier: UUID, options: BLEOptions) throws
```

bleIdentifier是已选择且CoreBluetooth识别的外设UUID，options指定服务、写特征和流控。只创建本地通信设备，不扫描、连接、申请权限或发送IO。后续结构化操作逐次提供Profile，Raw无需Profile。要求完整ABI3/r1原生库，不匹配1006；创建失败按稳定错误码报告。调用方或Manager负责close，不能由创建推断硬件协议或几何。

文档 ID：`apple:symbol:s:10PrinterSDK06MobileA0C13bleIdentifier7optionsAC10Foundation4UUIDV_AA10BLEOptionsVtKcfc`

符号种类：`swift.init`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| BLEOptions | `s:10PrinterSDK10BLEOptionsV` |
| UUID | `s:10Foundation4UUIDV` |

<a id="api-919dd545c263f56055dc"></a>

### MobilePrinter.observeState(queue:_:)

订阅合并的本地连接、工作和队列快照。

```swift
func observeState(queue: DispatchQueue = .main, _ callback: @escaping @Sendable (DeviceStateSnapshot) -> Void) throws -> ObservationSubscription
```

默认在主队列异步投递，可指定DispatchQueue。共享按需观察器读取本地快照，先交付当前值，再合并相同值；不保证短暂中间状态的完整历史，也不做硬件查询。须保留返回的状态Subscription；释放该token或显式cancel/close会停止监听，设备close也停止监听。不会取消打印，客户代码在SDK锁外执行。关闭设备后注册抛1002。 回调声明为 `@Sendable`，捕获的跨线程值须满足 Sendable 或适当隔离；UI 更新显式切回 MainActor。默认回调队列仍为主队列。

文档 ID：`apple:symbol:s:10PrinterSDK06MobileA0C12observeState5queue_AA23ObservationSubscriptionCSo012OS_dispatch_F0C_yAA06DeviceE8SnapshotVYbctKF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| DeviceStateSnapshot | `s:10PrinterSDK19DeviceStateSnapshotV` |
| DispatchQueue | `c:objc(cs)OS_dispatch_queue` |
| ObservationSubscription | `s:10PrinterSDK23ObservationSubscriptionC` |
| Void | `s:s4Voida` |

<a id="api-2fc22b7e1d246d6d5cab"></a>

### MobilePrinter.queryStatus(configuration:timeoutMilliseconds:)

按本次协议和响应超时发起状态查询。

```swift
func queryStatus(configuration: StatusQueryConfiguration, timeoutMilliseconds: UInt32 = 5000) throws -> PrintOperation
```

configuration 指定本次协议与请求／响应预算，timeoutMilliseconds 默认 5000 毫秒，须大于 0，包含排队和执行。配置接纳时复制，不改变默认打印配置。当前标准后端同步抛 1004 UNSUPPORTED_OPERATION，零打印机 IO，硬件维持未知；未来受支持查询仍共用设备 FIFO，调用方持有并关闭 operation。

文档 ID：`apple:symbol:s:10PrinterSDK06MobileA0C11queryStatus13configuration19timeoutMillisecondsAA14PrintOperationCAA0E18QueryConfigurationV_s6UInt32VtKF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| PrintOperation | `s:10PrinterSDK14PrintOperationC` |
| StatusQueryConfiguration | `s:10PrinterSDK24StatusQueryConfigurationV` |
| UInt32 | `s:s6UInt32V` |

<a id="api-fd3f05de96b59bf48f9a"></a>

### MobilePrinter.resourceSnapshot()

读取设备源载荷接纳限制和当前占用。

```swift
func resourceSnapshot() throws -> ResourceSnapshot
```

本地一致快照，不执行 IO，不预留空间；submit 决定实际接纳。设备关闭后抛 1002，原生资源或 ABI 错误按 PrinterSDKError 返回。

文档 ID：`apple:symbol:s:10PrinterSDK06MobileA0C16resourceSnapshotAA08ResourceE0VyKF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| ResourceSnapshot | `s:10PrinterSDK16ResourceSnapshotV` |

<a id="api-fb9517d11980d6aa0c3a"></a>

### MobilePrinter.setMediaMode(_:profile:timeoutMilliseconds:)

使用本次 CPCL 配置排入介质跟踪设置操作。

```swift
func setMediaMode(_ mode: MediaMode, profile: CpclProfile, timeoutMilliseconds: UInt32 = 120000) throws -> PrintOperation
```

本次配置在接纳时按值复制，只属于这个队列条目，不绑定到连接、不记住最后一次配置，不改变连接的通信选项。可以在同一连接中连续排队不同协议或几何配置的操作，仍与 Raw 共用同一个 FIFO，不切换连接或自动重放。 `mode` 为间隙、黑标或连续纸；`profile` 指定本次 CPCL 参数。`timeoutMilliseconds` 默认 120000，须大于 0，单位毫秒，覆盖排队和执行；零超时或无效配置以 1001 拒绝。操作不设置纸宽、纸长、间隙长度，不校准传感器或持久保存设置。返回需调用者持有并关闭的 PrintOperation，代表接纳；错误按 PrinterSDKError 报告，执行错误读取快照。成功 DATA_SENT 只证明设置字节已发送，不证明硬件已经应用或介质准备就绪。

文档 ID：`apple:symbol:s:10PrinterSDK06MobileA0C12setMediaMode_7profile19timeoutMillisecondsAA14PrintOperationCAA0eF0O_AA11CpclProfileVs6UInt32VtKF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| CpclProfile | `s:10PrinterSDK11CpclProfileV` |
| MediaMode | `s:10PrinterSDK9MediaModeO` |
| PrintOperation | `s:10PrinterSDK14PrintOperationC` |
| UInt32 | `s:s6UInt32V` |

<a id="api-0aebe7cd2c7ace7b6fa4"></a>

### MobilePrinter.stateSnapshot()

同步读取本地一致的设备状态。

```swift
func stateSnapshot() throws -> DeviceStateSnapshot
```

读取不调用传输回调、不发送打印机 IO、不入队也不等待打印完成。快照只用于展示和诊断，不作为提交前置条件；不得与设备销毁并发访问。 未连接也可读取；设备关闭后抛出 PrinterSDKError code 1002。当前硬件状态未知，不触发硬件刷新。

文档 ID：`apple:symbol:s:10PrinterSDK06MobileA0C13stateSnapshotAA011DeviceStateE0VyKF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| DeviceStateSnapshot | `s:10PrinterSDK19DeviceStateSnapshotV` |

<a id="api-770b811c49f042364b2d"></a>

### MobilePrinter.submit(_:profile:timeoutMilliseconds:)

提交不可变作业及本次打印配置。

```swift
func submit(_ job: PrintJob, profile: CpclProfile, timeoutMilliseconds: UInt32 = 120000) throws -> PrintOperation
```

标签作业配CpclProfile，小票配ReceiptProfile；配置仅属于该队列条目，不绑定连接。timeout默认120000ms且大于零，覆盖排队、准备和发送。成功仅表示接纳，共享原设备FIFO；返回PrintOperation供result/onComplete/cancel使用。输入job可在返回后释放，错误或不支持在IO前拒绝，不自动重放。

文档 ID：`apple:symbol:s:10PrinterSDK06MobileA0C6submit_7profile19timeoutMillisecondsAA14PrintOperationCAA0H3JobC_AA11CpclProfileVs6UInt32VtKF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| CpclProfile | `s:10PrinterSDK11CpclProfileV` |
| PrintJob | `s:10PrinterSDK8PrintJobC` |
| PrintOperation | `s:10PrinterSDK14PrintOperationC` |
| UInt32 | `s:s6UInt32V` |

<a id="api-3d863085cc14d9b7c534"></a>

### MobilePrinter.submit(_:profile:timeoutMilliseconds:)

提交不可变作业及本次打印配置。

```swift
func submit(_ job: PrintJob, profile: ReceiptProfile, timeoutMilliseconds: UInt32 = 120000) throws -> PrintOperation
```

标签作业配CpclProfile，小票配ReceiptProfile；配置仅属于该队列条目，不绑定连接。timeout默认120000ms且大于零，覆盖排队、准备和发送。成功仅表示接纳，共享原设备FIFO；返回PrintOperation供result/onComplete/cancel使用。输入job可在返回后释放，错误或不支持在IO前拒绝，不自动重放。

文档 ID：`apple:symbol:s:10PrinterSDK06MobileA0C6submit_7profile19timeoutMillisecondsAA14PrintOperationCAA0H3JobC_AA14ReceiptProfileVs6UInt32VtKF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| PrintJob | `s:10PrinterSDK8PrintJobC` |
| PrintOperation | `s:10PrinterSDK14PrintOperationC` |
| ReceiptProfile | `s:10PrinterSDK14ReceiptProfileV` |
| UInt32 | `s:s6UInt32V` |

<a id="api-5bd1501b8ec8d282d467"></a>

### MobilePrinter.submitGray8(_:width:height:profile:copies:trailingBlankDots:canvasWidthDots:originXDots:form:timeoutMilliseconds:)

使用本次 CPCL 配置提交完整 Gray8 标签。

```swift
func submitGray8(_ pixels: Data, width: UInt32, height: UInt32, profile: CpclProfile, copies: UInt32 = 1, trailingBlankDots: UInt32 = 0, canvasWidthDots: UInt32? = nil, originXDots: UInt32? = nil, form: Bool? = nil, timeoutMilliseconds: UInt32 = 120000) throws -> PrintOperation
```

本次配置在接纳时按值复制，只属于这个队列条目，不绑定到连接、不记住最后一次配置，不改变连接的通信选项。可以在同一连接中连续排队不同协议或几何配置的操作，仍与 Raw 共用同一个 FIFO，不切换连接或自动重放。 `pixels` 为逐行紧密排列、每像素 1 字节的灰度数据（0 黑、255 白），长度必须恰为 `width * height`；宽高均以点为单位且大于 0。当前此入口以阈值 128 二值化，不自动缩放或裁剪。`copies` 默认 1，范围 1…100；`trailingBlankDots` 默认 0，增加每张标签声明高度。`originXDots` 默认 `nil`，取 `CpclProfile.xOffsetDots`；须小于配置打印宽度。`canvasWidthDots` 默认 `nil`，取配置打印宽度减本次横向原点；图像补齐到整字节的横向范围及总高度必须落在标签和设备边界内。`form` 默认 `nil`，取 profile 默认值；true 请求下一标签起点结束，false 请求内容结束，仅保存在本次作业中。`timeoutMilliseconds` 默认 `120000`，必须大于 0，是入队后包含排队、准备和发送的共享总预算，单位毫秒，不按份数重新计时。

本次 `profile` 选择 CPCL 协议，只属于本次操作，不绑定连接。参数或几何无效抛 1001；单幅源图超过 16 MiB、设备未终态源载荷合计超过 64 MiB、每设备未终态操作超过 64 个或资源分配失败可抛 1008；这些是 SDK 接纳限制，不是硬件内存承诺。输入在提交过程中复制，返回后调用方可释放原 `Data`。成功返回一个需调用方持有和关闭的 `PrintOperation`，只表示已接纳；运行期失败、超时、取消通过其快照观察。打印机 IO 在设备队列执行，成功最多报告 `DATA_SENT`，不确认机械打印完成。为避免在 UI 上承担图像校验和复制成本，应在应用工作线程提交。

文档 ID：`apple:symbol:s:10PrinterSDK06MobileA0C11submitGray8_5width6height7profile6copies17trailingBlankDots011canvasWidthL011originXDots4form19timeoutMillisecondsAA14PrintOperationC10Foundation4DataV_s6UInt32VAtA11CpclProfileVA3TSgAWSbSgATtKF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Bool | `s:Sb` |
| CpclProfile | `s:10PrinterSDK11CpclProfileV` |
| Data | `s:10Foundation4DataV` |
| PrintOperation | `s:10PrinterSDK14PrintOperationC` |
| UInt32 | `s:s6UInt32V` |

<a id="api-27bebb908c0a75091108"></a>

### MobilePrinter.submitRaw(_:copies:timeoutMilliseconds:)

在设备原有串行队列中原样发送一个 Raw 载荷。

```swift
func submitRaw(_ bytes: Data, copies: UInt32 = 1, timeoutMilliseconds: UInt32 = 120000) throws -> PrintOperation
```

`bytes` 必须非空，最多 1 MiB；超出载荷限制抛 1008。`copies` 默认 1，范围 1…100；各份在同一个队列条目内发送，其他操作不会插入份与份之间。`timeoutMilliseconds` 默认 `120000`，必须大于 0，单位毫秒；全部份数共享包含排队的总超时与取消状态。空载荷、份数或超时无效抛 1001，未连接或已关闭时抛状态/传输错误。源载荷复制一次，份数不扩增接纳预算；设备未终态源载荷合计上限 64 MiB，每设备最多 64 个未终态操作，超限抛 1008。

载荷不经过协议封装，不应用标签原点、FORM、图像处理或介质推断；调用方承担命令与目标设备匹配的责任。返回 `PrintOperation` 只表示接纳，后续通过快照查看结果。取消、断线或超时停止后续字节，已发送数据不可撤回，不自动重放剩余份数。成功仍仅 `DATA_SENT`，操作对象由调用者持有并显式关闭。

文档 ID：`apple:symbol:s:10PrinterSDK06MobileA0C9submitRaw_6copies19timeoutMillisecondsAA14PrintOperationC10Foundation4DataV_s6UInt32VAMtKF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Data | `s:10Foundation4DataV` |
| PrintOperation | `s:10PrinterSDK14PrintOperationC` |
| UInt32 | `s:s6UInt32V` |

<a id="api-a9afaaea8c1291ea6962"></a>

### MobilePrinter.submitReceiptGray8(_:width:height:profile:copies:maxTotalLengthDots:trailingBlankDots:cut:finishScope:timeoutMilliseconds:)

使用本次 ESC/POS 配置提交完整 Gray8 小票。

```swift
func submitReceiptGray8(_ pixels: Data, width: UInt32, height: UInt32, profile: ReceiptProfile, copies: UInt32 = 1, maxTotalLengthDots: UInt32, trailingBlankDots: UInt32 = 0, cut: CutMode = .none, finishScope: FinishScope = .eachCopy, timeoutMilliseconds: UInt32 = 120000) throws -> PrintOperation
```

本次配置在接纳时按值复制，只属于这个队列条目，不绑定到连接、不记住最后一次配置，不改变连接的通信选项。可以在同一连接中连续排队不同协议或几何配置的操作，仍与 Raw 共用同一个 FIFO，不切换连接或自动重放。 `pixels` 为紧密排列 Gray8（0 黑、255 白），长度恰为正数 `width * height`，宽高单位为点；当前使用阈值 128，不自动缩放或裁剪。点宽不得超过配置版心。`copies` 默认 1、范围 1…100；`maxTotalLengthDots` 为调用方必填的正数整单长度预算，包含全部份数及收尾留白，不代表设备单页长度能力。`trailingBlankDots` 默认 0；`cut` 默认 `.none`；`finishScope` 默认 `.eachCopy`；当前标准 ESC/POS 后端仅支持此值，`.endOfJob` 即使只有一份也会在 IO 前以 1004 拒绝。整单长度为图像高度乘份数，加按 scope 执行的尾部留白。当前标准 ESC/POS 后端要求不切纸，切纸策略不能静默丢弃；不支持的结束策略在首次 IO 前以 1004 拒绝。`timeoutMilliseconds` 默认 `120000`，必须大于 0，单位毫秒，涵盖排队、准备和所有份数的发送。

本次 `profile` 选择 ESC/POS 协议，只属于本次操作，不绑定连接；参数、长度预算或几何无效抛 1001。单幅源图最多 16 MiB，设备未终态源载荷合计最多 64 MiB，每设备最多 64 个未终态操作；资源或接纳超限可抛 1008。提交复制像素，返回后原 `Data` 可释放。成功返回的 `PrintOperation` 表示入队；后续错误、取消和终态由快照观察，调用者负责持有和关闭。不在调用线程等待 IO 完成，但图像校验和复制应放在应用工作线程。成功只证明数据发送，不能断言已出纸或已切纸。

文档 ID：`apple:symbol:s:10PrinterSDK06MobileA0C18submitReceiptGray8_5width6height7profile6copies18maxTotalLengthDots013trailingBlankN03cut11finishScope19timeoutMillisecondsAA14PrintOperationC10Foundation4DataV_s6UInt32VAtA0E7ProfileVA3tA7CutModeOAA06FinishS0OATtKF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| CutMode | `s:10PrinterSDK7CutModeO` |
| Data | `s:10Foundation4DataV` |
| FinishScope | `s:10PrinterSDK11FinishScopeO` |
| PrintOperation | `s:10PrinterSDK14PrintOperationC` |
| ReceiptProfile | `s:10PrinterSDK14ReceiptProfileV` |
| UInt32 | `s:s6UInt32V` |

<a id="api-b2adb08956dca46c7ae5"></a>

## ObservationSubscription

<a id="api-5a52c794ca552950bd7d"></a>

### ObservationSubscription

可显式注销的通知所有权。

```swift
final class ObservationSubscription
```

cancel与close等价且幂等，只停止通知，不调用operation.cancel或关闭设备。已开始回调不可撤回；Swift状态订阅需要持有token，完成订阅不因丢弃token自动注销。 Swift 跨隔离访问采用 Sendable 契约，内部同步与原生存储生命周期保护保持；可变 App 状态须自行同步，阻塞 IO 仍应在后台执行。

文档 ID：`apple:symbol:s:10PrinterSDK23ObservationSubscriptionC`

符号种类：`swift.class`；访问级别：`public`。

<a id="api-f7d2d4e41ed5ac75acfa"></a>

### ObservationSubscription.cancel()

停止本订阅的后续通知。

```swift
func cancel()
```

幂等，未开始的排队回调会被有效性检查抑制，已经开始的回调不可撤回。不取消打印或共享连接；停止打印需显式PrintOperation.cancel。

文档 ID：`apple:symbol:s:10PrinterSDK23ObservationSubscriptionC6cancelyyF`

符号种类：`swift.method`；访问级别：`public`。

<a id="api-025aac6a3651723a311d"></a>

### ObservationSubscription.close()

停止本订阅的后续通知。

```swift
func close()
```

幂等，未开始的排队回调会被有效性检查抑制，已经开始的回调不可撤回。不取消打印或共享连接；停止打印需显式PrintOperation.cancel。

文档 ID：`apple:symbol:s:10PrinterSDK23ObservationSubscriptionC5closeyyF`

符号种类：`swift.method`；访问级别：`public`。

<a id="api-afec0bb5086d128fee42"></a>

## OperationResult

<a id="api-66c1b85669cdbd1b3afa"></a>

### OperationResult

一次终态的不可变完整结果。

```swift
struct OperationResult
```

包含最终snapshot、诊断和可选硬件status；缓存不保留作业像素，独立于设备和native句柄存活。成功DATA_SENT仍不确认机械完成。

文档 ID：`apple:symbol:s:10PrinterSDK15OperationResultV`

符号种类：`swift.struct`；访问级别：`public`。

<a id="api-8e580bcd0dc2bfda8d39"></a>

### OperationResult.!=(_:_:)

编译器提供的值不等比较。

```swift
static func != (lhs: borrowing Self, rhs: borrowing Self) -> Bool
```

按该值类型的Equatable语义比较，返回布尔值，不执行设备IO。比较相同状态或结果不代表设备当前在线或机械打印完成。

文档 ID：`apple:symbol:s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK15OperationResultV`

符号种类：`swift.func.op`；访问级别：`public`。

这是编译器为当前类型带入的协议扩展／合成公共成员。

扩展来源（保留基线原值）：

```json
{"extendedModule":"Swift","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Bool | `s:Sb` |

<a id="api-b286ead8fdb6c5b220e3"></a>

### OperationResult.diagnostic

该操作的有界终态诊断。

```swift
let diagnostic: ErrorDiagnostic
```

稳定错误码为业务依据，nativeCode/domain仅排障；成功记录code=0。与设备最新诊断分离，可独立保存。

文档 ID：`apple:symbol:s:10PrinterSDK15OperationResultV10diagnosticAA15ErrorDiagnosticVvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| ErrorDiagnostic | `s:10PrinterSDK15ErrorDiagnosticV` |

<a id="api-3add52f47062106479c9"></a>

### OperationResult.snapshot

最终操作快照。

```swift
let snapshot: OperationSnapshot
```

state必为COMPLETED/FAILED/CANCELLED；稳定errorCode、计数证据、副作用及completion均来自同一终态，不是状态轮询猜测。

文档 ID：`apple:symbol:s:10PrinterSDK15OperationResultV8snapshotAA0C8SnapshotVvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| OperationSnapshot | `s:10PrinterSDK17OperationSnapshotV` |

<a id="api-0f5810e7fef4946d3f6e"></a>

### OperationResult.status

仅有有效响应时存在的硬件结果。

```swift
let status: HardwareStatusResult?
```

nil表示没有有效硬件状态响应；knownConditions决定哪些位可解释，未出现的位不自动表示正常。当前未验证协议保持nil。

文档 ID：`apple:symbol:s:10PrinterSDK15OperationResultV6statusAA014HardwareStatusD0VSgvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| HardwareStatusResult | `s:10PrinterSDK20HardwareStatusResultV` |

<a id="api-57bb815c8787c72b8ed3"></a>

## OperationSnapshot

<a id="api-d85fa2f33bb0f7379bb8"></a>

### OperationSnapshot

操作在某一时刻的不可变结果和进度值。

```swift
struct OperationSnapshot
```

由 `PrintOperation.snapshot` 返回，无公开初始化入口。值不持有设备资源，设备或操作关闭后已取得值仍可读；新进展需要再次获取快照。数值状态、完成级别、字节精确性和副作用必须组合解释，不以已发送数或传输完成推断机械打印完成。

文档 ID：`apple:symbol:s:10PrinterSDK17OperationSnapshotV`

符号种类：`swift.struct`；访问级别：`public`。

<a id="api-95b233c39f4d764f9d65"></a>

### OperationSnapshot.!=(_:_:)

编译器提供的值不等比较。

```swift
static func != (lhs: borrowing Self, rhs: borrowing Self) -> Bool
```

按该值类型的Equatable语义比较，返回布尔值，不执行设备IO。比较相同状态或结果不代表设备当前在线或机械打印完成。

文档 ID：`apple:symbol:s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK17OperationSnapshotV`

符号种类：`swift.func.op`；访问级别：`public`。

这是编译器为当前类型带入的协议扩展／合成公共成员。

扩展来源（保留基线原值）：

```json
{"extendedModule":"Swift","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Bool | `s:Sb` |

<a id="api-7cd2699839d9615581aa"></a>

### OperationSnapshot.bytesSent

截至快照已观察到的传输接受字节数。

```swift
let bytesSent: UInt64
```

单位字节，包含操作实际发送的协议控制和打印数据，不能按源像素数直接换算进度。`bytesSentExact == false` 时该数只是已知下界，可能有已被平台接受但尚未确证的字节；0 不总是无副作用证明。该统计不表示机械输出长度。

文档 ID：`apple:symbol:s:10PrinterSDK17OperationSnapshotV9bytesSents6UInt64Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt64 | `s:s6UInt64V` |

<a id="api-58649d01668b55c18eeb"></a>

### OperationSnapshot.bytesSentExact

已发送字节统计是否有精确计数证据。

```swift
let bytesSentExact: Bool
```

true 表示本快照的 `bytesSent` 是精确统计；false 表示保守下界。发送中的平台回调可能尚未返回，CoreBluetooth 失败也未必能报告全部接受量，因此不能从数值大小自行补充精确性。即使精确，也只衡量传输接受，不确认纸面。

文档 ID：`apple:symbol:s:10PrinterSDK17OperationSnapshotV14bytesSentExactSbvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Bool | `s:Sb` |

<a id="api-c72438bbe74273844c80"></a>

### OperationSnapshot.bytesTotal

已知时的总发送字节数。

```swift
let bytesTotal: UInt64?
```

单位字节；未知时为 `nil`，不要替换成确定的 0。当前流式处理一般在具有精确证据的成功终态才知道总数；运行中、失败或证据不足时可为 nil。只有已知且合适时才计算比例，不能用进度百分比断言机械打印完成。

文档 ID：`apple:symbol:s:10PrinterSDK17OperationSnapshotV10bytesTotals6UInt64VSgvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt64 | `s:s6UInt64V` |

<a id="api-9fdb78619f93f77f3fbd"></a>

### OperationSnapshot.completion

成功操作达到的完成证据级别。

```swift
let completion: UInt32
```

数值 0 无完成证据，1 `DATA_SENT`，2 `DEVICE_CONFIRMED`。当前标准 CPCL/ESC/POS 发送成功仅为 1；运行中、失败或取消不带完成级别。1 只表示所需字节已交付传输路径，不能推断纸面、走纸或设置应用结果；保留值 2 不表示当前后端已实现设备确认。

文档 ID：`apple:symbol:s:10PrinterSDK17OperationSnapshotV10completions6UInt32Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-5c1420f167d1273e0a2e"></a>

### OperationSnapshot.errorCode

操作运行期结果的稳定错误码。

```swift
let errorCode: Int32
```

0 表示该快照没有记录错误，并不能单独证明已经成功完成。失败或取消时查看对应 SDK 数值，例如取消 1005、超时 2001、连接丢失 2002；应结合 `state` 和 `isTerminal`。设备关闭后仍可通过持有的操作读到终态错误，不自动重试或重放。

文档 ID：`apple:symbol:s:10PrinterSDK17OperationSnapshotV9errorCodes5Int32Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Int32 | `s:s5Int32V` |

<a id="api-4f35dec662d0fcc4e1ee"></a>

### OperationSnapshot.id

该快照所属的操作标识符。

```swift
let id: UInt64
```

与对应 `PrintOperation.id` 一致，是设备内的 `UInt64` 标识。值随快照保存，读取不访问设备；不可用作跨设备、跨运行会话的全局唯一身份。

文档 ID：`apple:symbol:s:10PrinterSDK17OperationSnapshotV2ids6UInt64Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt64 | `s:s6UInt64V` |

<a id="api-5895611bc88f582dc7e9"></a>

### OperationSnapshot.isTerminal

该快照是否属于终态。

```swift
var isTerminal: Bool { get }
```

当前计算为 `state >= 4`；当前终态为成功完成 4、失败 5、取消 6。true 不表示成功，必须再检查状态和错误码。值只是这次快照的观察，调用本属性不阻塞、不取消、不释放操作对象。

文档 ID：`apple:symbol:s:10PrinterSDK17OperationSnapshotV10isTerminalSbvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Bool | `s:Sb` |

<a id="api-2d62800506ccab82a1fb"></a>

### OperationSnapshot.kind

操作类型的底层数值。

```swift
let kind: UInt32
```

当前数值：1 打印，2 状态查询，3 介质设置，4 Raw，5 下一标签定位。当前标准后端拒绝状态查询，所以保留该编号不表示已有状态查询实现。未知未来值应保留并作通用展示，不猜测行为。

文档 ID：`apple:symbol:s:10PrinterSDK17OperationSnapshotV4kinds6UInt32Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-2ffc75161da35775effb"></a>

### OperationSnapshot.mediaBasis

作业介质条件判断所使用的证据数值。

```swift
let mediaBasis: UInt32
```

0 未使用介质依据，1 同一连接中最近完整发送的设置，2 设备读回。最近发送只证明传输成功，不能提升为已应用或读回；当前 Swift 简化提交入口不暴露介质条件参数，通常为 0，当前标准后端也不提供设备读回。该字段不证明纸张已安装或定位正确。

文档 ID：`apple:symbol:s:10PrinterSDK17OperationSnapshotV10mediaBasiss6UInt32Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-09583e05ff22ba20e09a"></a>

### OperationSnapshot.sideEffectsPossible

是否可能已对打印机产生副作用。

```swift
let sideEffectsPossible: Bool
```

保守标记：写入尝试或交付不明确后可能为 true，不要求 `bytesSent > 0`。结合错误、取消和字节证据决定人工恢复；true 时不可盲目重试或重放。false 也不是连接存活、设备就绪或未来重试必然安全的证明。

文档 ID：`apple:symbol:s:10PrinterSDK17OperationSnapshotV19sideEffectsPossibleSbvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Bool | `s:Sb` |

<a id="api-0970649d86ed8992279c"></a>

### OperationSnapshot.state

操作执行状态的底层数值。

```swift
let state: UInt32
```

当前数值：0 排队，1 准备，2 发送，4 成功完成，5 失败，6 取消；当前没有值 3 的公开状态。以 `isTerminal` 判断是否结束，再查看 `completion`、`errorCode` 和副作用字段。终态成功不等于设备已机械打印完成。

文档 ID：`apple:symbol:s:10PrinterSDK17OperationSnapshotV5states6UInt32Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-145004af19a837676c99"></a>

## PixelFormat

<a id="api-ef118e5e76594eff95ed"></a>

### PixelFormat

紧密排列的源像素格式。

```swift
enum PixelFormat
```

GRAY8=1、RGB24=2、RGBA32=3；行内和行间均无padding，Data长度必须恰等于width*height*每像素字节数。平台图像须先转换为这些字节格式。

文档 ID：`apple:symbol:s:10PrinterSDK11PixelFormatO`

符号种类：`swift.enum`；访问级别：`public`。

<a id="api-4e4eaf4a6a6274b5ee13"></a>

### PixelFormat.!=(_:_:)

编译器提供的值不等比较。

```swift
static func != (lhs: borrowing Self, rhs: borrowing Self) -> Bool
```

按该值类型的Equatable语义比较，返回布尔值，不执行设备IO。比较相同状态或结果不代表设备当前在线或机械打印完成。

文档 ID：`apple:symbol:s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK11PixelFormatO`

符号种类：`swift.func.op`；访问级别：`public`。

这是编译器为当前类型带入的协议扩展／合成公共成员。

扩展来源（保留基线原值）：

```json
{"extendedModule":"Swift","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Bool | `s:Sb` |

<a id="api-201fc189ec6a92750c71"></a>

### PixelFormat.gray8

选择gray8源像素。

```swift
case gray8
```

每像素1字节灰度，0黑、255白，rawValue=1。不执行解码或打印IO。

文档 ID：`apple:symbol:s:10PrinterSDK11PixelFormatO5gray8yA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-736be9763b56a01a1d07"></a>

### PixelFormat.hash(into:)

编译器提供的枚举散列。

```swift
func hash(into hasher: inout Hasher)
```

用于Swift集合与字典，按当前枚举raw值生成；散列不稳定，不能持久化为协议值、设备身份或跨进程标识，不执行IO。

文档 ID：`apple:symbol:s:SYsSHRzSH8RawValueSYRpzrlE4hash4intoys6HasherVz_tF::SYNTHESIZED::s:10PrinterSDK11PixelFormatO`

符号种类：`swift.method`；访问级别：`public`。

这是编译器为当前类型带入的协议扩展／合成公共成员。

泛型约束（保留基线原值）：

```json
{"constraints":[{"kind":"conformance","lhs":"Self","rhs":"Hashable","rhsPrecise":"s:SH"},{"kind":"conformance","lhs":"Self.RawValue","rhs":"Hashable","rhsPrecise":"s:SH"}]}
```

扩展来源（保留基线原值）：

```json
{"constraints":[{"kind":"conformance","lhs":"Self","rhs":"Hashable","rhsPrecise":"s:SH"},{"kind":"conformance","lhs":"Self.RawValue","rhs":"Hashable","rhsPrecise":"s:SH"}],"extendedModule":"Swift","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Hasher | `s:s6HasherV` |

<a id="api-f1432bd35a1bd5bbfb7e"></a>

### PixelFormat.hashValue

编译器提供的枚举散列。

```swift
var hashValue: Int { get }
```

用于Swift集合与字典，按当前枚举raw值生成；散列不稳定，不能持久化为协议值、设备身份或跨进程标识，不执行IO。

文档 ID：`apple:symbol:s:SYsSHRzSH8RawValueSYRpzrlE04hashB0Sivp::SYNTHESIZED::s:10PrinterSDK11PixelFormatO`

符号种类：`swift.property`；访问级别：`public`。

这是编译器为当前类型带入的协议扩展／合成公共成员。

扩展来源（保留基线原值）：

```json
{"constraints":[{"kind":"conformance","lhs":"Self","rhs":"Hashable","rhsPrecise":"s:SH"},{"kind":"conformance","lhs":"Self.RawValue","rhs":"Hashable","rhsPrecise":"s:SH"}],"extendedModule":"Swift","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Int | `s:Si` |

<a id="api-6e6ab1e42778179f800f"></a>

### PixelFormat.init(rawValue:)

按已声明raw值构造枚举。

```swift
init?(rawValue: UInt32)
```

匹配本版枚举值时返回对应项，未知数值返回nil；不执行IO、不探测设备，也不自动接受未来枚举。精确raw值见本页编译器枚举表。

文档 ID：`apple:symbol:s:10PrinterSDK11PixelFormatO8rawValueACSgs6UInt32V_tcfc`

符号种类：`swift.init`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-ed0fa655c10b949327d6"></a>

### PixelFormat.rgb24

选择rgb24源像素。

```swift
case rgb24
```

每像素3字节，顺序R/G/B，rawValue=2。不执行解码或打印IO。

文档 ID：`apple:symbol:s:10PrinterSDK11PixelFormatO5rgb24yA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-3d42babddc39cfe2b52b"></a>

### PixelFormat.rgba32

选择rgba32源像素。

```swift
case rgba32
```

每像素4字节，顺序R/G/B/A，rawValue=3；Alpha按图像流水线规则合成。不执行解码或打印IO。

文档 ID：`apple:symbol:s:10PrinterSDK11PixelFormatO6rgba32yA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-853bae839e6f8b53cd97"></a>

## PrintJob

<a id="api-1ed5d1eb45a8ca964c75"></a>

### PrintJob

保留共享图像存储的不可变作业。

```swift
final class PrintJob
```

由Builder.build创建元数据快照；之后修改builder不影响已构建job，submit保留全部所需存储。ARC析构释放公共job句柄，不取消已入队打印；每作业逻辑图像上限32MiB。 Swift 跨隔离访问采用 Sendable 契约，内部同步与原生存储生命周期保护保持；可变 App 状态须自行同步，阻塞 IO 仍应在后台执行。

文档 ID：`apple:symbol:s:10PrinterSDK8PrintJobC`

符号种类：`swift.class`；访问级别：`public`。

<a id="api-af29c8d159c71ce634fe"></a>

### PrintJob.Builder

平台层的类型化作业构建器。

```swift
final class Builder
```

选择标签或小票选项后收集图像/空白/finish/media，build一次创建不可变native descriptor。构建器保留引用图像；build不复制像素，不执行打印机IO。后续变更只影响下一次build。 Swift 跨隔离访问采用 Sendable 契约，内部同步与原生存储生命周期保护保持；可变 App 状态须自行同步，阻塞 IO 仍应在后台执行。

文档 ID：`apple:symbol:s:10PrinterSDK8PrintJobC7BuilderC`

符号种类：`swift.class`；访问级别：`public`。

<a id="api-ae88c1f5f6e151e7410c"></a>

### PrintJob.Builder.addImage(_:x:y:)

向标签追加图像引用和坐标。

```swift
func addImage(_ image: PrinterImage, x: Int32 = 0, y: Int32 = 0) throws
```

仅标签可用，否则1004；不复制像素。坐标非负、图像须在画布内，当前CPCL只支持x=0和纵向顺序不重叠，build/submit检查边界。

文档 ID：`apple:symbol:s:10PrinterSDK8PrintJobC7BuilderC8addImage_1x1yyAA0aG0C_s5Int32VALtKF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Int32 | `s:s5Int32V` |
| PrinterImage | `s:10PrinterSDK0A5ImageC` |

<a id="api-52fccbe72de9a1c8e86e"></a>

### PrintJob.Builder.appendBlank(dots:)

顺序追加小票空白。

```swift
func appendBlank(dots: UInt32) throws
```

仅小票可用，否则1004；dots必须为正，计入整单长度，build检查。

文档 ID：`apple:symbol:s:10PrinterSDK8PrintJobC7BuilderC11appendBlank4dotsys6UInt32V_tKF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-d84d9e1ad3aaa63eaedd"></a>

### PrintJob.Builder.appendImage(_:)

顺序追加小票图像引用。

```swift
func appendImage(_ image: PrinterImage) throws
```

仅小票可用，否则1004；坐标固定零，native按顺序排版，不复制像素。

文档 ID：`apple:symbol:s:10PrinterSDK8PrintJobC7BuilderC11appendImageyyAA0aG0CKF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| PrinterImage | `s:10PrinterSDK0A5ImageC` |

<a id="api-b45cb97a6d27d54ec716"></a>

### PrintJob.Builder.build()

生成不可变作业快照。

```swift
func build() throws -> PrintJob
```

校验typed选项和最多1024项，至少一幅图；保留image存储无像素复制，每次引用计入32MiB逻辑预算。失败抛1001/1004/1008，不产生打印工作；原builder仍可修改，已建job不变。

文档 ID：`apple:symbol:s:10PrinterSDK8PrintJobC7BuilderC5buildACyKF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| PrintJob | `s:10PrinterSDK8PrintJobC` |

<a id="api-404dcf6454492301bd4b"></a>

### PrintJob.Builder.init(label:)

创建标签构建器。

```swift
init(label: LabelJobOptions)
```

保存LabelJobOptions，不连接或执行IO；图片位置通过addImage给出。

文档 ID：`apple:symbol:s:10PrinterSDK8PrintJobC7BuilderC5labelAeA05LabelD7OptionsV_tcfc`

符号种类：`swift.init`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| LabelJobOptions | `s:10PrinterSDK15LabelJobOptionsV` |

<a id="api-4df0a06f03062729ef9d"></a>

### PrintJob.Builder.init(receipt:)

创建小票构建器。

```swift
init(receipt: ReceiptJobOptions)
```

保存ReceiptJobOptions，不执行IO；通过appendImage/appendBlank顺序追加。

文档 ID：`apple:symbol:s:10PrinterSDK8PrintJobC7BuilderC7receiptAeA07ReceiptD7OptionsV_tcfc`

符号种类：`swift.init`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| ReceiptJobOptions | `s:10PrinterSDK17ReceiptJobOptionsV` |

<a id="api-0d60a4469d8988c43268"></a>

### PrintJob.Builder.setMediaRequirement(_:)

设置或清除介质前置条件。

```swift
func setMediaRequirement(_ value: MediaRequirement?)
```

nil清除要求，不发送介质设置。build校验字段，submit校验支持与已有证据。

文档 ID：`apple:symbol:s:10PrinterSDK8PrintJobC7BuilderC19setMediaRequirementyyAA0gH0VSgF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| MediaRequirement | `s:10PrinterSDK16MediaRequirementV` |

<a id="api-05953ecc687babfb40b0"></a>

### PrintJob.Builder.setReceiptFinish(_:)

设置本次小票结束选项。

```swift
func setReceiptFinish(_ value: ReceiptFinishOptions) throws
```

仅小票可用，否则1004；当前不支持的cut/scope在提交时明确拒绝，不自动发送切纸。

文档 ID：`apple:symbol:s:10PrinterSDK8PrintJobC7BuilderC16setReceiptFinishyyAA0gH7OptionsVKF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| ReceiptFinishOptions | `s:10PrinterSDK20ReceiptFinishOptionsV` |

<a id="api-fcb644f7b0eb6c8a6489"></a>

## PrintOperation

<a id="api-97ddc1978712bd1f39f9"></a>

### PrintOperation

独立持有结果与终态订阅的操作。

```swift
final class PrintOperation
```

由成功提交返回，不要求设备继续存活。snapshot观察过程，result读取终态；close撤销调用方访问和新增注册权，不取消工作，既有onComplete订阅仍被观察器保活并兑现。取消工作须显式cancel，停止通知须注销对应Subscription。 Swift 跨隔离访问采用 Sendable 契约，内部同步与原生存储生命周期保护保持；可变 App 状态须自行同步，阻塞 IO 仍应在后台执行。

文档 ID：`apple:symbol:s:10PrinterSDK14PrintOperationC`

符号种类：`swift.class`；访问级别：`public`。

<a id="api-92ab438cb6d8e8afb8b9"></a>

### PrintOperation.cancel()

显式请求协作取消此操作。

```swift
func cancel() throws
```

不关闭共享设备，不能撤回已发送字节。结果仍由result/onComplete观察，完成与取消竞争时可能已经正常完成。调用方close后抛1002，终态后重复cancel为空操作。

文档 ID：`apple:symbol:s:10PrinterSDK14PrintOperationC6cancelyyKF`

符号种类：`swift.method`；访问级别：`public`。

<a id="api-c62d17e2700dd934e80a"></a>

### PrintOperation.close()

关闭调用方操作访问权。

```swift
func close()
```

幂等；不取消打印，也不撤回之前注册的终态通知。之后getter/cancel/新增onComplete抛1002；内部观察器保留必要native所有权至终态复制，随后释放。显式Subscription注销只停止对应通知。

文档 ID：`apple:symbol:s:10PrinterSDK14PrintOperationC5closeyyF`

符号种类：`swift.method`；访问级别：`public`。

<a id="api-4e6fd0d3748e85734262"></a>

### PrintOperation.diagnostic()

读取终态结构化诊断。

```swift
func diagnostic() throws -> ErrorDiagnostic
```

复用result().diagnostic，包含稳定错误码、操作/连接代次和可选原生代码，仅用于诊断。未终结或close后抛1002；结果独立于设备存活。

文档 ID：`apple:symbol:s:10PrinterSDK14PrintOperationC10diagnosticAA15ErrorDiagnosticVyKF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| ErrorDiagnostic | `s:10PrinterSDK15ErrorDiagnosticV` |

<a id="api-7ac60332768717e5aa7e"></a>

### PrintOperation.id

接纳该操作时分配的设备内操作标识符。

```swift
let id: UInt64
```

不可变 `UInt64`，与 `OperationSnapshot.id` 一致，在所属设备中标识操作；不要当作跨设备或跨进程唯一 ID。读取此保存值不访问设备，即使操作对象已关闭仍可读取，但关闭后不能再取得新快照或请求取消。

文档 ID：`apple:symbol:s:10PrinterSDK14PrintOperationC2ids6UInt64Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt64 | `s:s6UInt64V` |

<a id="api-11fe73b3d6c5be86dd14"></a>

### PrintOperation.message()

读取终态诊断消息。

```swift
func message() throws -> String
```

复用result()的有界诊断文本；未终结或close后抛1002。业务以稳定errorCode判断，消息可能为空，不包含像素或平台自由文本。

文档 ID：`apple:symbol:s:10PrinterSDK14PrintOperationC7messageSSyKF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| String | `s:SS` |

<a id="api-9d9ad06a23d74f3c64b0"></a>

### PrintOperation.onComplete(queue:_:)

注册一次异步终态结果投递。

```swift
@discardableResult func onComplete(queue: DispatchQueue = .main, _ callback: @escaping @Sendable (OperationResult) -> Void) throws -> ObservationSubscription
```

默认主队列，可显式指定DispatchQueue；终态后注册仍异步投递。注册与完成竞态不会遗漏或重复；有效订阅最多一次执行，客户代码在锁外且不在原生IO worker执行。close不撤回既有通知，Subscription.cancel/close停止尚未开始的投递，已开始回调不可撤回。无需持有token来维持完成通知。关闭后新增注册抛1002。 回调声明为 `@Sendable`，捕获的跨线程值须满足 Sendable 或适当隔离；UI 更新显式切回 MainActor。默认回调队列仍为主队列。

文档 ID：`apple:symbol:s:10PrinterSDK14PrintOperationC10onComplete5queue_AA23ObservationSubscriptionCSo012OS_dispatch_G0C_yAA0D6ResultVYbctKF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| DispatchQueue | `c:objc(cs)OS_dispatch_queue` |
| ObservationSubscription | `s:10PrinterSDK23ObservationSubscriptionC` |
| OperationResult | `s:10PrinterSDK15OperationResultV` |
| Void | `s:s4Voida` |

<a id="api-1ad69c0dbc84ea62f076"></a>

### PrintOperation.result()

读取并缓存完整不可变终态。

```swift
func result() throws -> OperationResult
```

尚未终结或调用方close后抛1002。成功返回最终snapshot、诊断和可选status；复制完成后释放内部native结果句柄，缓存不保留图像载荷。设备销毁后仍可读，不等待打印机IO。

文档 ID：`apple:symbol:s:10PrinterSDK14PrintOperationC6resultAA0D6ResultVyKF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| OperationResult | `s:10PrinterSDK15OperationResultV` |

<a id="api-12fe1504d628308a8272"></a>

### PrintOperation.snapshot()

读取操作的过程或缓存终态快照。

```swift
func snapshot() throws -> OperationSnapshot
```

不执行打印机IO，不要求设备存活。bytesTotal和精确性按证据位解释，DATA_SENT不是物理打印完成；close后抛1002。

文档 ID：`apple:symbol:s:10PrinterSDK14PrintOperationC8snapshotAA0D8SnapshotVyKF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| OperationSnapshot | `s:10PrinterSDK17OperationSnapshotV` |

<a id="api-ffaa4650c0920a01ae16"></a>

### PrintOperation.statusResult()

读取有效硬件状态响应。

```swift
func statusResult() throws -> HardwareStatusResult
```

仅当终态result.status非nil时返回，否则抛1002；不能由零mask推断硬件正常。当前标准CPCL/ESC/POS查询在IO前1004拒绝，所以不会产生成功状态operation。

文档 ID：`apple:symbol:s:10PrinterSDK14PrintOperationC12statusResultAA014HardwareStatusF0VyKF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| HardwareStatusResult | `s:10PrinterSDK20HardwareStatusResultV` |

<a id="api-51947abf7e628a2d3bc3"></a>

## PrinterCapabilities

<a id="api-04cc2e358bdb700591f5"></a>

### PrinterCapabilities

聚合且相互独立的能力观察值。

```swift
struct PrinterCapabilities
```

由MobilePrinter.capabilities()返回，包含features、constraints和geometry；不存在整体“已验证”布尔值，必须按各成员known/deviceSupport/evidence判断。此对象不包含平台原生句柄。

文档 ID：`apple:symbol:s:10PrinterSDK0A12CapabilitiesV`

符号种类：`swift.struct`；访问级别：`public`。

<a id="api-a21bf93f74a28bda67ff"></a>

### PrinterCapabilities.constraints

按参数枚举索引的四项范围约束。

```swift
let constraints: [PrinterConstraint : ScalarConstraint]
```

copies和源载荷预算属于SDK接纳规则；介质间隙没有硬件证据时known为false。观察不预留额度，提交结果才是接纳权威。

文档 ID：`apple:symbol:s:10PrinterSDK0A12CapabilitiesV11constraintsSDyAA0A10ConstraintOAA06ScalarE0VGvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| PrinterConstraint | `s:10PrinterSDK0A10ConstraintO` |
| ScalarConstraint | `s:10PrinterSDK16ScalarConstraintV` |

<a id="api-354f2eeb9045ebcb43d0"></a>

### PrinterCapabilities.features

按功能枚举索引的八项能力。

```swift
let features: [PrinterFeature : FeatureSupport]
```

每项分别保存SDK实现、设备支持、证据和已知位掩码。当前状态与切纸未实现，其他实现可用不代表硬件支持；没有型号证据时deviceSupport保持unknown。

文档 ID：`apple:symbol:s:10PrinterSDK0A12CapabilitiesV8featuresSDyAA0A7FeatureOAA0E7SupportVGvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| FeatureSupport | `s:10PrinterSDK14FeatureSupportV` |
| PrinterFeature | `s:10PrinterSDK0A7FeatureO` |

<a id="api-af5f287d707c793db940"></a>

### PrinterCapabilities.geometry

有存在位依据的可选打印几何。

```swift
let geometry: PrinterGeometry
```

各未知数值转换为nil，不使用零或最近一次Profile冒充已知能力。当前通信设备不绑定打印配置，所有几何字段未知。

文档 ID：`apple:symbol:s:10PrinterSDK0A12CapabilitiesV8geometryAA0A8GeometryVvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| PrinterGeometry | `s:10PrinterSDK0A8GeometryV` |

<a id="api-1f9f816f9d0db63d9e95"></a>

## PrinterConstraint

<a id="api-ea37902c12ef0d4b96c4"></a>

### PrinterConstraint

标量约束的查询参数。

```swift
enum PrinterConstraint
```

不可变 UInt32 原始值枚举；分类不表示经过验证的硬件支持，未知原始值不能猜测映射。

文档 ID：`apple:symbol:s:10PrinterSDK0A10ConstraintO`

符号种类：`swift.enum`；访问级别：`public`。

<a id="api-ddb66ae3be01f1dd5dd7"></a>

### PrinterConstraint.!=(_:_:)

按值比较两个 PrinterConstraint 是否不同。

```swift
static func != (lhs: borrowing Self, rhs: borrowing Self) -> Bool
```

Swift Equatable 提供的比较，不执行 IO、不刷新状态。比较本次值或枚举，不能把相等的快照当作设备仍在线、硬件正常或纸面打印完成的证明。

文档 ID：`apple:symbol:s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK0A10ConstraintO`

符号种类：`swift.func.op`；访问级别：`public`。

这是编译器为当前类型带入的协议扩展／合成公共成员。

扩展来源（保留基线原值）：

```json
{"extendedModule":"Swift","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Bool | `s:Sb` |

<a id="api-3d4fa4e842fdf2855bac"></a>

### PrinterConstraint.copies

份数约束，值 1，单位份。

```swift
case copies
```

固定公共枚举值；其含义仅限本查询参数、来源分类或协议选择，不能据此推断设备当前状态或纸面完成。

文档 ID：`apple:symbol:s:10PrinterSDK0A10ConstraintO6copiesyA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-7f300210109826fe0e28"></a>

### PrinterConstraint.hash(into:)

把枚举值加入 Swift 哈希器。

```swift
func hash(into hasher: inout Hasher)
```

Swift Hashable 合成操作，不执行 IO；哈希结果不保证跨进程一致，持久化时使用已文档化的 rawValue。

文档 ID：`apple:symbol:s:SYsSHRzSH8RawValueSYRpzrlE4hash4intoys6HasherVz_tF::SYNTHESIZED::s:10PrinterSDK0A10ConstraintO`

符号种类：`swift.method`；访问级别：`public`。

这是编译器为当前类型带入的协议扩展／合成公共成员。

泛型约束（保留基线原值）：

```json
{"constraints":[{"kind":"conformance","lhs":"Self","rhs":"Hashable","rhsPrecise":"s:SH"},{"kind":"conformance","lhs":"Self.RawValue","rhs":"Hashable","rhsPrecise":"s:SH"}]}
```

扩展来源（保留基线原值）：

```json
{"constraints":[{"kind":"conformance","lhs":"Self","rhs":"Hashable","rhsPrecise":"s:SH"},{"kind":"conformance","lhs":"Self.RawValue","rhs":"Hashable","rhsPrecise":"s:SH"}],"extendedModule":"Swift","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Hasher | `s:s6HasherV` |

<a id="api-6ccd3f3e850120d8bbd6"></a>

### PrinterConstraint.hashValue

枚举的进程内哈希值。

```swift
var hashValue: Int { get }
```

Swift Hashable 合成值，用于当前进程集合，不是稳定协议值、持久标识或设备 ID；协议值读取 rawValue。

文档 ID：`apple:symbol:s:SYsSHRzSH8RawValueSYRpzrlE04hashB0Sivp::SYNTHESIZED::s:10PrinterSDK0A10ConstraintO`

符号种类：`swift.property`；访问级别：`public`。

这是编译器为当前类型带入的协议扩展／合成公共成员。

扩展来源（保留基线原值）：

```json
{"constraints":[{"kind":"conformance","lhs":"Self","rhs":"Hashable","rhsPrecise":"s:SH"},{"kind":"conformance","lhs":"Self.RawValue","rhs":"Hashable","rhsPrecise":"s:SH"}],"extendedModule":"Swift","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Int | `s:Si` |

<a id="api-30fba607314bd3559116"></a>

### PrinterConstraint.init(rawValue:)

按已声明raw值构造枚举。

```swift
init?(rawValue: UInt32)
```

匹配本版枚举值时返回对应项，未知数值返回nil；不执行IO、不探测设备，也不自动接受未来枚举。精确raw值见本页编译器枚举表。

文档 ID：`apple:symbol:s:10PrinterSDK0A10ConstraintO8rawValueACSgs6UInt32V_tcfc`

符号种类：`swift.init`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-0647aa2da183a5553240"></a>

### PrinterConstraint.jobImageBytes

单作业图像源载荷，值 3，单位字节。

```swift
case jobImageBytes
```

固定公共枚举值；其含义仅限本查询参数、来源分类或协议选择，不能据此推断设备当前状态或纸面完成。

文档 ID：`apple:symbol:s:10PrinterSDK0A10ConstraintO13jobImageBytesyA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-bbc3b5bb6f5a53fe0ddf"></a>

### PrinterConstraint.mediaGapMicrometers

介质间隙约束，值 2，单位微米。

```swift
case mediaGapMicrometers
```

固定公共枚举值；其含义仅限本查询参数、来源分类或协议选择，不能据此推断设备当前状态或纸面完成。

文档 ID：`apple:symbol:s:10PrinterSDK0A10ConstraintO19mediaGapMicrometersyA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-d95f145203c6ec6479ae"></a>

### PrinterConstraint.pendingPayloadBytes

设备未终态源载荷，值 4，单位字节。

```swift
case pendingPayloadBytes
```

固定公共枚举值；其含义仅限本查询参数、来源分类或协议选择，不能据此推断设备当前状态或纸面完成。

文档 ID：`apple:symbol:s:10PrinterSDK0A10ConstraintO19pendingPayloadBytesyA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-67c88d53ad46bc6679a5"></a>

## PrinterFeature

<a id="api-e03f0b77f68138fbd004"></a>

### PrinterFeature

可供能力查询选择的功能标识。

```swift
enum PrinterFeature
```

传给 `MobilePrinter.feature`，选择要读取的软件实现、设备支持及证据。枚举项存在不表示功能已实现或硬件支持；以返回的 `FeatureSupport` 为准。

文档 ID：`apple:symbol:s:10PrinterSDK0A7FeatureO`

符号种类：`swift.enum`；访问级别：`public`。

<a id="api-2a266904387af010fa70"></a>

### PrinterFeature.!=(_:_:)

比较两个 PrinterFeature 值是否不相等。

```swift
static func != (lhs: borrowing Self, rhs: borrowing Self) -> Bool
```

Swift 标准库 `Equatable` 扩展为此类型提供的合成公开成员，不是独立 SDK 实现。参数 `lhs`、`rhs` 为两个值；枚举项不同时返回 true，否则 false。不抛出错误、不执行 IO，也不比较设备状态或诊断文案。

文档 ID：`apple:symbol:s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK0A7FeatureO`

符号种类：`swift.func.op`；访问级别：`public`。

这是编译器为当前类型带入的协议扩展／合成公共成员。

扩展来源（保留基线原值）：

```json
{"extendedModule":"Swift","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Bool | `s:Sb` |

<a id="api-ed8320a141e822e9838f"></a>

### PrinterFeature.cut

查询切纸功能。

```swift
case cut
```

当前标准后端未实现切纸。此项与小票打印功能分开；不要以 ESC/POS 协议名或小票配置推断机器存在切刀。

文档 ID：`apple:symbol:s:10PrinterSDK0A7FeatureO3cutyA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-0f03e7d7e9c7eaacfc34"></a>

### PrinterFeature.feedToNextLabel

查询下一标签起点定位功能。

```swift
case feedToNextLabel
```

当前 CPCL 标签设备提供定位命令，小票设备不提供。调用方负责合适介质配置，能力和发送成功均不证明纸张已实际定位。

文档 ID：`apple:symbol:s:10PrinterSDK0A7FeatureO15feedToNextLabelyA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-a5485c5b38516b40782d"></a>

### PrinterFeature.hash(into:)

把 PrinterFeature 的相等性相关值混入 Hasher。

```swift
func hash(into hasher: inout Hasher)
```

Swift 标准库为原始值枚举提供的合成成员，不是 SDK 实现。`hasher` 为调用方按 `inout` 传入的 `Hasher`，方法更新其状态，没有返回值且不抛出错误。用于 `Hashable` 集合语义，哈希结果不是跨进程稳定序列化格式，不涉及设备 IO。

文档 ID：`apple:symbol:s:SYsSHRzSH8RawValueSYRpzrlE4hash4intoys6HasherVz_tF::SYNTHESIZED::s:10PrinterSDK0A7FeatureO`

符号种类：`swift.method`；访问级别：`public`。

这是编译器为当前类型带入的协议扩展／合成公共成员。

泛型约束（保留基线原值）：

```json
{"constraints":[{"kind":"conformance","lhs":"Self","rhs":"Hashable","rhsPrecise":"s:SH"},{"kind":"conformance","lhs":"Self.RawValue","rhs":"Hashable","rhsPrecise":"s:SH"}]}
```

扩展来源（保留基线原值）：

```json
{"constraints":[{"kind":"conformance","lhs":"Self","rhs":"Hashable","rhsPrecise":"s:SH"},{"kind":"conformance","lhs":"Self.RawValue","rhs":"Hashable","rhsPrecise":"s:SH"}],"extendedModule":"Swift","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Hasher | `s:s6HasherV` |

<a id="api-3106913662603703972d"></a>

### PrinterFeature.hashValue

获取 PrinterFeature 的进程内哈希值。

```swift
var hashValue: Int { get }
```

Swift 标准库为原始值枚举提供的合成 `Hashable` 成员，不是 SDK 稳定编号。用于集合/字典查找；不要持久化、传输、作为 API 原始值或在不同进程/版本间比较。需要契约数值时使用枚举原始值语义。

文档 ID：`apple:symbol:s:SYsSHRzSH8RawValueSYRpzrlE04hashB0Sivp::SYNTHESIZED::s:10PrinterSDK0A7FeatureO`

符号种类：`swift.property`；访问级别：`public`。

这是编译器为当前类型带入的协议扩展／合成公共成员。

扩展来源（保留基线原值）：

```json
{"constraints":[{"kind":"conformance","lhs":"Self","rhs":"Hashable","rhsPrecise":"s:SH"},{"kind":"conformance","lhs":"Self.RawValue","rhs":"Hashable","rhsPrecise":"s:SH"}],"extendedModule":"Swift","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Int | `s:Si` |

<a id="api-543fb5685df7584ecfc2"></a>

### PrinterFeature.image

查询主机图像处理与发送功能。

```swift
case image
```

描述图像路径的软件实现；当前使用主机渲染。可用算法和源载荷资源限制仍独立，不能据此推断目标设备所有像素尺寸都可接纳。

文档 ID：`apple:symbol:s:10PrinterSDK0A7FeatureO5imageyA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-fc7bddb2fbc60c0085ae"></a>

### PrinterFeature.init(rawValue:)

按已声明raw值构造枚举。

```swift
init?(rawValue: UInt32)
```

匹配本版枚举值时返回对应项，未知数值返回nil；不执行IO、不探测设备，也不自动接受未来枚举。精确raw值见本页编译器枚举表。

文档 ID：`apple:symbol:s:10PrinterSDK0A7FeatureO8rawValueACSgs6UInt32V_tcfc`

符号种类：`swift.init`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-a7c224117407dc06c54c"></a>

### PrinterFeature.label

查询固定标签打印功能。

```swift
case label
```

判断当前配置是否提供标签作业路径；当前标签设备具备软件实现，小票设备不提供。实现支持与硬件支持证据独立，须读取 `FeatureSupport`。

文档 ID：`apple:symbol:s:10PrinterSDK0A7FeatureO5labelyA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-da8aea00014fb7c0bc4d"></a>

### PrinterFeature.media

查询介质模式设置功能。

```swift
case media
```

当前 CPCL 标签设备提供介质跟踪设置，小票设备拒绝。设置发送成功仍不是设备读回或硬件确认，且不改变每个图像作业的几何。

文档 ID：`apple:symbol:s:10PrinterSDK0A7FeatureO5mediayA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-a8e4a139bc0c500db441"></a>

### PrinterFeature.raw

查询原始字节发送功能。

```swift
case raw
```

原样发送仍通过设备同一串行队列；功能实现不校验载荷的协议语义，也不保证任意 Raw 命令在设备上安全或受支持。

文档 ID：`apple:symbol:s:10PrinterSDK0A7FeatureO3rawyA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-7b4cb6ec046e34c9fe5b"></a>

### PrinterFeature.receipt

查询小票打印功能。

```swift
case receipt
```

判断当前配置是否提供小票作业路径；当前小票设备具备软件实现，标签设备不提供。设备页长与切刀仍是独立能力，不能由小票功能推断。

文档 ID：`apple:symbol:s:10PrinterSDK0A7FeatureO7receiptyA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-d82541532c85edfb8283"></a>

### PrinterFeature.status

查询打印机状态读取功能。

```swift
case status
```

当前标准 CPCL/ESC/POS 后端未实现，`queryStatus` 明确抛 1004；该能力项不代表可读取缺纸、开盖或就绪状态。

文档 ID：`apple:symbol:s:10PrinterSDK0A7FeatureO6statusyA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-b511b313f54808d516b6"></a>

## PrinterGeometry

<a id="api-850e4163c3320f338599"></a>

### PrinterGeometry

以nil表示未知的打印几何。

```swift
struct PrinterGeometry
```

从本地能力观察返回，不发送IO；值只有在相应存在位有效时才非nil。单页上限与小票跨副本的累计长度预算分开。

文档 ID：`apple:symbol:s:10PrinterSDK0A8GeometryV`

符号种类：`swift.struct`；访问级别：`public`。

<a id="api-12a1be9bdde743deeae9"></a>

### PrinterGeometry.!=(_:_:)

编译器提供的值不等比较。

```swift
static func != (lhs: borrowing Self, rhs: borrowing Self) -> Bool
```

按该值类型的Equatable语义比较，返回布尔值，不执行设备IO。比较相同状态或结果不代表设备当前在线或机械打印完成。

文档 ID：`apple:symbol:s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK0A8GeometryV`

符号种类：`swift.func.op`；访问级别：`public`。

这是编译器为当前类型带入的协议扩展／合成公共成员。

扩展来源（保留基线原值）：

```json
{"extendedModule":"Swift","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Bool | `s:Sb` |

<a id="api-0c1b4d883c4741237970"></a>

### PrinterGeometry.dpiX

横向DPI的可选值。

```swift
let dpiX: UInt32?
```

nil表示未知，不能解释为零、无限或不支持。当前纯通信设备没有硬件几何证据，因此返回nil。

文档 ID：`apple:symbol:s:10PrinterSDK0A8GeometryV4dpiXs6UInt32VSgvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-b08f8570db8cc04b75ff"></a>

### PrinterGeometry.dpiY

纵向DPI的可选值。

```swift
let dpiY: UInt32?
```

nil表示未知，不能解释为零、无限或不支持。当前纯通信设备没有硬件几何证据，因此返回nil。

文档 ID：`apple:symbol:s:10PrinterSDK0A8GeometryV4dpiYs6UInt32VSgvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-78b2f9a426e1788f3d27"></a>

### PrinterGeometry.evidence

打印几何证据位。

```swift
let evidence: UInt32
```

PROFILE_DECLARED=1、DEVICE_REPORTED=2、HARDWARE_VERIFIED=4；各位描述来源，零表示没有相应证据。不能由软件构建通过提升为硬件验证。

文档 ID：`apple:symbol:s:10PrinterSDK0A8GeometryV8evidences6UInt32Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-f95ae36feed3348d378e"></a>

### PrinterGeometry.maxPageLengthDots

一页或一份输出的最大长度（点）的可选值。

```swift
let maxPageLengthDots: UInt32?
```

nil表示未知，不能解释为零、无限或不支持。此值不乘copies，不是ReceiptJobOptions.maxTotalLengthDots整单预算。

文档 ID：`apple:symbol:s:10PrinterSDK0A8GeometryV17maxPageLengthDotss6UInt32VSgvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-c1efa55a22dad33d90d2"></a>

### PrinterGeometry.minCutBlankDots

切纸前最小留白（点）的可选值。

```swift
let minCutBlankDots: UInt32?
```

nil表示未知，不能解释为零、无限或不支持。当前纯通信设备没有硬件几何证据，因此返回nil。

文档 ID：`apple:symbol:s:10PrinterSDK0A8GeometryV15minCutBlankDotss6UInt32VSgvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-d294829e2a935b9b07ff"></a>

### PrinterGeometry.printableWidthDots

可打印宽度（点）的可选值。

```swift
let printableWidthDots: UInt32?
```

nil表示未知，不能解释为零、无限或不支持。当前纯通信设备没有硬件几何证据，因此返回nil。

文档 ID：`apple:symbol:s:10PrinterSDK0A8GeometryV18printableWidthDotss6UInt32VSgvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-ed1435ddffbdea0f0ae1"></a>

## PrinterImage

<a id="api-1d9823b5f5c64962004c"></a>

### PrinterImage

一次复制、可被多个作业共享的不可变图像。

```swift
final class PrinterImage
```

初始化复制紧密排列像素和图像处理选项，成功后可释放源Data。PrintJob只保留其只读native存储，不再次复制像素；ARC析构释放调用方image句柄，已构建作业不受影响。每图源像素上限16MiB。 Swift 跨隔离访问采用 Sendable 契约，内部同步与原生存储生命周期保护保持；可变 App 状态须自行同步，阻塞 IO 仍应在后台执行。

文档 ID：`apple:symbol:s:10PrinterSDK0A5ImageC`

符号种类：`swift.class`；访问级别：`public`。

<a id="api-ba529303b83ad3cc64a6"></a>

### PrinterImage.height

源图像高度。

```swift
let height: UInt32
```

单位为像素，初始化后不可变且大于零；不能由源高度推断纸张物理高度或打印完成。

文档 ID：`apple:symbol:s:10PrinterSDK0A5ImageC6heights6UInt32Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-ad0b8c63e749b4a5e4e5"></a>

### PrinterImage.init(pixels:width:height:format:algorithm:threshold:invert:fitWidth:targetWidthDots:)

校验并一次复制源像素与图像选项。

```swift
init(pixels: Data, width: UInt32, height: UInt32, format: PixelFormat = .gray8, algorithm: ImageAlgorithm = .threshold, threshold: UInt32 = 128, invert: Bool = false, fitWidth: Bool = false, targetWidthDots: UInt32 = 0) throws
```

Data必须紧密排列且长度恰为width*height*bpp，宽高大于零、每图最多16MiB；threshold为0..255。默认gray8/threshold128、不反色、不拟合；fitWidth=false要求targetWidthDots=0，开启后由目标宽度和作业可用区计算。成功后源Data可释放，jobs共享只读native存储；校验失败1001/1004、资源超限1008，不做IO。

文档 ID：`apple:symbol:s:10PrinterSDK0A5ImageC6pixels5width6height6format9algorithm9threshold6invert8fitWidth06targetL4DotsAC10Foundation4DataV_s6UInt32VAqA11PixelFormatOAA0C9AlgorithmOAQS2bAQtKcfc`

符号种类：`swift.init`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Bool | `s:Sb` |
| Data | `s:10Foundation4DataV` |
| ImageAlgorithm | `s:10PrinterSDK14ImageAlgorithmO` |
| PixelFormat | `s:10PrinterSDK11PixelFormatO` |
| UInt32 | `s:s6UInt32V` |

<a id="api-81975c6aaf2b82351d38"></a>

### PrinterImage.width

源图像宽度。

```swift
let width: UInt32
```

单位为像素，初始化后不可变且大于零；拟合后的打印宽度由作业可用区域和图像选项决定。

文档 ID：`apple:symbol:s:10PrinterSDK0A5ImageC5widths6UInt32Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-1d5ad0ccf40ef0036743"></a>

## PrinterManager

<a id="api-03e4d1448ba1e8315656"></a>

### PrinterManager

拥有并复用 BLE 设备的可选管理器。

```swift
final class PrinterManager
```

通过 shared 获取共享实例，或直接初始化注册表独立的普通实例。同一管理器按外设 UUID 复用 MobilePrinter，连接选项必须一致；不同管理器不共享设备，应用负责避免重复占用同一硬件。管理器拥有设备，返回对象供调用者借用打印接口；管理器所有者负责 release/close，借用方不直接 close 设备。工厂只创建未连接设备，不需要打印 Profile；打印参数仍逐次提供，打印与 Raw 继续使用原设备的唯一 FIFO。异步生命周期调用在后台运行阻塞 IO，关闭控制使用独立执行路径；Task 取消不隐式关闭共享设备，需要中断时显式 release/close。close 后实例永久停止接纳，包括 shared。显式 await close() 可等待回收；析构兜底仅调度异步清理，不阻塞析构调用方。管理器不新增协议状态、自动发现、权限界面、重连或重放。 Swift 跨隔离访问采用 Sendable 契约，内部同步与原生存储生命周期保护保持；可变 App 状态须自行同步，阻塞 IO 仍应在后台执行。

文档 ID：`apple:symbol:s:10PrinterSDK0A7ManagerC`

符号种类：`swift.class`；访问级别：`public`。

<a id="api-5fb188f0b92a416ded70"></a>

### PrinterManager.close()

永久停止接纳并异步回收全部设备。

```swift
func close() async
```

停止新设备和连接，为各设备独立调度关闭并等待回收；重复调用共享屏障。回收后drain/copy已注册operation的终态并安排投递，await完成不等待主队列或客户回调执行，不丢取消/失败结果。状态订阅结束，已有完成订阅不依赖设备。shared同样永久关闭；Task取消不撤回清理，也不隐式重放打印。

文档 ID：`apple:symbol:s:10PrinterSDK0A7ManagerC5closeyyYaF`

符号种类：`swift.method`；访问级别：`public`。

<a id="api-aa0caee2bede3cd43707"></a>

### PrinterManager.connect(_:timeoutMilliseconds:)

异步显式连接本管理器拥有的设备。

```swift
func connect(_ printer: MobilePrinter, timeoutMilliseconds: UInt32 = 5000) async throws
```

printer 必须是当前由本管理器拥有的 MobilePrinter；timeoutMilliseconds 默认 5000，须大于 0，单位毫秒，为这次连接的超时。零超时或外部/已经移除的对象抛 PrinterSDKError 1001；管理器关闭、设备正在回收或同设备已有未完成 connect 为 1002。后台执行原设备阻塞连接，await 成功返回 Void，只证明通信连接成功，不确认打印机就绪或纸面能力。已连接设备的再次有效调用沿用原幂等行为；失败后保留条目，可在此前请求结束后显式重试，不自动重连或重放。Task 取消不隐式关闭设备，需要中断时显式 release/close；被回收或关闭打断的连接以 1002 结束，其他传输错误沿用 PrinterSDKError。

文档 ID：`apple:symbol:s:10PrinterSDK0A7ManagerC7connect_19timeoutMillisecondsyAA06MobileA0C_s6UInt32VtYaKF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| MobilePrinter | `s:10PrinterSDK06MobileA0C` |
| UInt32 | `s:s6UInt32V` |

<a id="api-d35ade3a5258fcd64664"></a>

### PrinterManager.init()

创建具有独立设备注册表的普通管理器。

```swift
init()
```

无参数，无打印 Profile。初始化不创建外设连接、不执行打印机 IO，也不扫描或申请权限。实例不与 shared 或其他普通实例复用设备，应用负责避免同一硬件被多个管理器重复占用。管理器所有者持有实例，并显式 await close() 等待设备回收完成；析构仅提供异步关闭兜底，不应当作已经等待回收的证据。

文档 ID：`apple:symbol:s:10PrinterSDK0A7ManagerCACycfc`

符号种类：`swift.init`；访问级别：`public`。

<a id="api-d928e3335ac92eefa708"></a>

### PrinterManager.printer(bleIdentifier:options:)

创建或复用本管理器拥有的 BLE 通信设备。

```swift
func printer(bleIdentifier: UUID, options: BLEOptions) throws -> MobilePrinter
```

bleIdentifier 为已选择的外设 UUID，options 为显式 BLEOptions。相同 UUID 且 service、writeCharacteristic、withResponse、maxWriteBytes 及可选信用流控 dataOut/dataFC 全部相等时返回同一个 MobilePrinter；只要求字段相等，不要求相同配置对象。相同端点选项冲突在 IO 前抛 PrinterSDKError 1001，改变选项须先 await release 原设备。管理器关闭或端点正在回收为 1002，其他 ABI 或资源错误沿用 MobilePrinter。新设备尚未连接；已有设备保留原连接状态。工厂不扫描、不连接、不发送数据、不申请权限，也不要求打印 Profile。返回值由管理器拥有，调用者借用打印接口，并由管理器所有者管理 release/close。

文档 ID：`apple:symbol:s:10PrinterSDK0A7ManagerC7printer13bleIdentifier7optionsAA06MobileA0C10Foundation4UUIDV_AA10BLEOptionsVtKF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| BLEOptions | `s:10PrinterSDK10BLEOptionsV` |
| MobilePrinter | `s:10PrinterSDK06MobileA0C` |
| UUID | `s:10Foundation4UUIDV` |

<a id="api-c972c1939568b364be28"></a>

### PrinterManager.release(_:)

异步回收这个确切的设备对象。

```swift
func release(_ printer: MobilePrinter) async
```

按对象身份匹配，未知/其他管理器/已移除对象为空操作；回收期间保留端点占位，中断连接及发送并等待设备close，随后允许新对象。已注册operation终态仍兑现，状态订阅结束；完成不等待客户callback。不会隐式关闭独立operation调用方访问权或其他管理器设备。

文档 ID：`apple:symbol:s:10PrinterSDK0A7ManagerC7releaseyyAA06MobileA0CYaF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| MobilePrinter | `s:10PrinterSDK06MobileA0C` |

<a id="api-79c127a8fe0a0adb5aea"></a>

### PrinterManager.shared

取得进程共享的可选管理器。

```swift
static let shared: PrinterManager
```

静态只读属性，返回同一 PrinterManager；首次获取只创建本地管理状态，不扫描、连接或发送打印机字节。由应用指定的管理器所有者协调共享设备的释放与关闭，借用方不直接关闭它们。await shared.close() 完成后该对象永久停止接纳，再次读取 shared 仍返回已关闭实例，不自动重建；需要新生命周期时显式 PrinterManager()。

文档 ID：`apple:symbol:s:10PrinterSDK0A7ManagerC6sharedACvpZ`

符号种类：`swift.type.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| PrinterManager | `s:10PrinterSDK0A7ManagerC` |

<a id="api-7c7a9dd0ee08114f96bd"></a>

## PrinterProtocol

<a id="api-ee78c2a7b2ae3e430847"></a>

### PrinterProtocol

状态查询使用的协议标识。

```swift
enum PrinterProtocol
```

不可变 UInt32 原始值枚举；分类不表示经过验证的硬件支持，未知原始值不能猜测映射。

文档 ID：`apple:symbol:s:10PrinterSDK0A8ProtocolO`

符号种类：`swift.enum`；访问级别：`public`。

<a id="api-e21ec72a9af0e560a42a"></a>

### PrinterProtocol.!=(_:_:)

按值比较两个 PrinterProtocol 是否不同。

```swift
static func != (lhs: borrowing Self, rhs: borrowing Self) -> Bool
```

Swift Equatable 提供的比较，不执行 IO、不刷新状态。比较本次值或枚举，不能把相等的快照当作设备仍在线、硬件正常或纸面打印完成的证明。

文档 ID：`apple:symbol:s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK0A8ProtocolO`

符号种类：`swift.func.op`；访问级别：`public`。

这是编译器为当前类型带入的协议扩展／合成公共成员。

扩展来源（保留基线原值）：

```json
{"extendedModule":"Swift","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Bool | `s:Sb` |

<a id="api-34ca04cf50ed336494f8"></a>

### PrinterProtocol.cpcl

CPCL 协议，值 1。

```swift
case cpcl
```

固定公共枚举值；其含义仅限本查询参数、来源分类或协议选择，不能据此推断设备当前状态或纸面完成。

文档 ID：`apple:symbol:s:10PrinterSDK0A8ProtocolO4cpclyA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-d293be9eeff309e50a88"></a>

### PrinterProtocol.escpos

ESC/POS 协议，值 2。

```swift
case escpos
```

固定公共枚举值；其含义仅限本查询参数、来源分类或协议选择，不能据此推断设备当前状态或纸面完成。

文档 ID：`apple:symbol:s:10PrinterSDK0A8ProtocolO6escposyA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-6b9ae2c161eae60889a4"></a>

### PrinterProtocol.hash(into:)

把枚举值加入 Swift 哈希器。

```swift
func hash(into hasher: inout Hasher)
```

Swift Hashable 合成操作，不执行 IO；哈希结果不保证跨进程一致，持久化时使用已文档化的 rawValue。

文档 ID：`apple:symbol:s:SYsSHRzSH8RawValueSYRpzrlE4hash4intoys6HasherVz_tF::SYNTHESIZED::s:10PrinterSDK0A8ProtocolO`

符号种类：`swift.method`；访问级别：`public`。

这是编译器为当前类型带入的协议扩展／合成公共成员。

泛型约束（保留基线原值）：

```json
{"constraints":[{"kind":"conformance","lhs":"Self","rhs":"Hashable","rhsPrecise":"s:SH"},{"kind":"conformance","lhs":"Self.RawValue","rhs":"Hashable","rhsPrecise":"s:SH"}]}
```

扩展来源（保留基线原值）：

```json
{"constraints":[{"kind":"conformance","lhs":"Self","rhs":"Hashable","rhsPrecise":"s:SH"},{"kind":"conformance","lhs":"Self.RawValue","rhs":"Hashable","rhsPrecise":"s:SH"}],"extendedModule":"Swift","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Hasher | `s:s6HasherV` |

<a id="api-31eab7428337d2d3cd1f"></a>

### PrinterProtocol.hashValue

枚举的进程内哈希值。

```swift
var hashValue: Int { get }
```

Swift Hashable 合成值，用于当前进程集合，不是稳定协议值、持久标识或设备 ID；协议值读取 rawValue。

文档 ID：`apple:symbol:s:SYsSHRzSH8RawValueSYRpzrlE04hashB0Sivp::SYNTHESIZED::s:10PrinterSDK0A8ProtocolO`

符号种类：`swift.property`；访问级别：`public`。

这是编译器为当前类型带入的协议扩展／合成公共成员。

扩展来源（保留基线原值）：

```json
{"constraints":[{"kind":"conformance","lhs":"Self","rhs":"Hashable","rhsPrecise":"s:SH"},{"kind":"conformance","lhs":"Self.RawValue","rhs":"Hashable","rhsPrecise":"s:SH"}],"extendedModule":"Swift","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Int | `s:Si` |

<a id="api-79c7a249bf4dd30b8b12"></a>

### PrinterProtocol.init(rawValue:)

按已声明raw值构造枚举。

```swift
init?(rawValue: UInt32)
```

匹配本版枚举值时返回对应项，未知数值返回nil；不执行IO、不探测设备，也不自动接受未来枚举。精确raw值见本页编译器枚举表。

文档 ID：`apple:symbol:s:10PrinterSDK0A8ProtocolO8rawValueACSgs6UInt32V_tcfc`

符号种类：`swift.init`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-1d012fc82f6300f83d19"></a>

## PrinterSDK

<a id="api-69cd0edad9ceb306a136"></a>

### PrinterSDK

SDK 运行库信息入口。

```swift
enum PrinterSDK
```

只提供本地运行库身份查询，不持有全局设备，不创建第二个执行器或全局打印队列。

文档 ID：`apple:symbol:s:10PrinterSDKAAO`

符号种类：`swift.enum`；访问级别：`public`。

<a id="api-85ad18e01796aece6428"></a>

### PrinterSDK.info()

读取实际加载原生运行库身份。

```swift
static func info() throws -> SDKInfo
```

静态本地查询，无需设备或蓝牙连接，不进行 IO。返回 SDKInfo，ABI、原生版本、分发版本及可选构建 ID 均来自已加载原生库；资源或 ABI 错误按 PrinterSDKError 返回。

文档 ID：`apple:symbol:s:10PrinterSDKAAO4infoAA7SDKInfoVyKFZ`

符号种类：`swift.type.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| SDKInfo | `s:10PrinterSDK7SDKInfoV` |

<a id="api-7eade5ddc7d13cb74d9a"></a>

## PrinterSDKError

<a id="api-c92cbc522af8f71283af"></a>

### PrinterSDKError

以稳定数值错误码表示 SDK 调用失败的值。

```swift
struct PrinterSDKError
```

遵循 `LocalizedError` 与 `Equatable`。应用按 `code` 分支处理，面向人的英文描述用于诊断，不作为稳定解析协议。构造该值不执行 IO，也不会改变设备状态；不可变值可保存为诊断结果。`Equatable` 比较错误码，Foundation/Swift 扩展成员在参考中单列，不表示 SDK 额外执行了恢复或查询。

文档 ID：`apple:symbol:s:10PrinterSDK0A8SDKErrorV`

符号种类：`swift.struct`；访问级别：`public`。

<a id="api-bbdf32fa79df5bb148b0"></a>

### PrinterSDKError.!=(_:_:)

比较两个 PrinterSDKError 值是否不相等。

```swift
static func != (lhs: borrowing Self, rhs: borrowing Self) -> Bool
```

Swift 标准库 `Equatable` 扩展为此类型提供的合成公开成员，不是独立 SDK 实现。参数 `lhs`、`rhs` 为两个值；所保存错误码不同时返回 true，否则 false。不抛出错误、不执行 IO，也不比较设备状态或诊断文案。

文档 ID：`apple:symbol:s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK0A8SDKErrorV`

符号种类：`swift.func.op`；访问级别：`public`。

这是编译器为当前类型带入的协议扩展／合成公共成员。

扩展来源（保留基线原值）：

```json
{"extendedModule":"Swift","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Bool | `s:Sb` |

<a id="api-0ca4770bef6f2a99fc82"></a>

### PrinterSDKError.==(_:_:)

按稳定错误码比较 SDK 异常。

```swift
static func == (lhs: PrinterSDKError, rhs: PrinterSDKError) -> Bool
```

保留既有 PrinterSDKError 仅按 code 相等的语义；附加诊断内容不同不改变错误分类比较，不执行 IO。

文档 ID：`apple:symbol:s:10PrinterSDK0A8SDKErrorV2eeoiySbAC_ACtFZ`

符号种类：`swift.func.op`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Bool | `s:Sb` |
| PrinterSDKError | `s:10PrinterSDK0A8SDKErrorV` |

<a id="api-646e532f5b246d5fb619"></a>

### PrinterSDKError.code

可供程序判断的稳定 SDK 错误码。

```swift
let code: Int32
```

主要值：1000 内部错误，1001 参数无效，1002 状态无效，1003 SDK 未初始化，1004 不支持，1005 已取消，1006 ABI 不匹配，1007 输出缓冲区不足，1008 资源限制；2000 传输未打开，2001 超时，2002 连接丢失，2003 IO 错误，2004 无传输进展，3000 协议响应无效。取消或连接丢失不证明零字节交付；不得据错误码自动重放可能产生副作用的打印。

文档 ID：`apple:symbol:s:10PrinterSDK0A8SDKErrorV4codes5Int32Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Int32 | `s:s5Int32V` |

<a id="api-ec199e4764e14065952b"></a>

### PrinterSDKError.diagnostic

可选的原生有界诊断。

```swift
let diagnostic: ErrorDiagnostic?
```

旧 init(_ code:) 初始化为 nil；新重载可以附加本次连接失败等记录。业务分类继续使用 code，比较保留仅按 code 相等的语义。

文档 ID：`apple:symbol:s:10PrinterSDK0A8SDKErrorV10diagnosticAA15ErrorDiagnosticVSgvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| ErrorDiagnostic | `s:10PrinterSDK15ErrorDiagnosticV` |

<a id="api-a1633f493b15c6c59882"></a>

### PrinterSDKError.errorDescription

包含错误码的可读诊断消息。

```swift
var errorDescription: String? { get }
```

返回当前实现的英文说明加括号内数值；未知码也有说明。虽然类型为 `String?`，当前实现总会生成字符串。文案不承诺稳定或自动本地化，业务分支应使用 `code`，需要本地化时由应用映射。

文档 ID：`apple:symbol:s:10PrinterSDK0A8SDKErrorV16errorDescriptionSSSgvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| String | `s:SS` |

<a id="api-16ca14e3ec29f4e04bd0"></a>

### PrinterSDKError.failureReason

Foundation 提供的可选失败原因。

```swift
var failureReason: String? { get }
```

`LocalizedError` 的 Foundation 默认扩展成员，不是 SDK 额外查询的结果；当前 SDK 没有覆盖，默认 nil。主要错误诊断使用 `code` 和 `errorDescription`，业务不能依赖此字段一定存在。

文档 ID：`apple:symbol:s:10Foundation14LocalizedErrorPAAE13failureReasonSSSgvp::SYNTHESIZED::s:10PrinterSDK0A8SDKErrorV`

符号种类：`swift.property`；访问级别：`public`。

这是编译器为当前类型带入的协议扩展／合成公共成员。

编译器 availability（保留基线原值）：

```json
[{"domain":"iOS","introduced":{"major":8,"minor":0}},{"domain":"macOS","introduced":{"major":10,"minor":10}},{"domain":"tvOS","introduced":{"major":9,"minor":0}},{"domain":"watchOS","introduced":{"major":2,"minor":0}}]
```

扩展来源（保留基线原值）：

```json
{"extendedModule":"Foundation","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| String | `s:SS` |

<a id="api-dd0dd15e4546e0d69b49"></a>

### PrinterSDKError.helpAnchor

Foundation 提供的可选错误帮助锚点。

```swift
var helpAnchor: String? { get }
```

`LocalizedError` 的 Foundation 默认扩展成员，不是 SDK 的独立实现；当前 SDK 没有提供专用帮助锚点，使用默认 nil。不要把它作为稳定文档 URL 或错误码替代。

文档 ID：`apple:symbol:s:10Foundation14LocalizedErrorPAAE10helpAnchorSSSgvp::SYNTHESIZED::s:10PrinterSDK0A8SDKErrorV`

符号种类：`swift.property`；访问级别：`public`。

这是编译器为当前类型带入的协议扩展／合成公共成员。

编译器 availability（保留基线原值）：

```json
[{"domain":"iOS","introduced":{"major":8,"minor":0}},{"domain":"macOS","introduced":{"major":10,"minor":10}},{"domain":"tvOS","introduced":{"major":9,"minor":0}},{"domain":"watchOS","introduced":{"major":2,"minor":0}}]
```

扩展来源（保留基线原值）：

```json
{"extendedModule":"Foundation","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| String | `s:SS` |

<a id="api-14712022ed2f2e9e4922"></a>

### PrinterSDKError.init(_:)

用一个 SDK 数值错误码构造错误值。

```swift
init(_ code: Int32)
```

`code` 为 `Int32`，原样保存；不校验是否已定义，也不抛出异常。未知数值仍可保留，描述显示未知 SDK 错误并包含该数值，便于未来版本和原生错误诊断。构造本身不尝试恢复连接或重试打印。

文档 ID：`apple:symbol:s:10PrinterSDK0A8SDKErrorVyACs5Int32Vcfc`

符号种类：`swift.init`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Int32 | `s:s5Int32V` |

<a id="api-313bc03ed43a1cebd226"></a>

### PrinterSDKError.init(_:diagnostic:)

创建附带可选诊断的 SDK 异常。

```swift
init(_ code: Int32, diagnostic: ErrorDiagnostic?)
```

code 是稳定 SDK 错误码，diagnostic 为可选的有界原生说明；不执行 IO。旧 init(_ code:) 签名保留，此重载必须显式提供 diagnostic，未提供诊断时传 nil。

文档 ID：`apple:symbol:s:10PrinterSDK0A8SDKErrorV_10diagnosticACs5Int32V_AA15ErrorDiagnosticVSgtcfc`

符号种类：`swift.init`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| ErrorDiagnostic | `s:10PrinterSDK15ErrorDiagnosticV` |
| Int32 | `s:s5Int32V` |

<a id="api-a56515ad7a768803dd57"></a>

### PrinterSDKError.localizedDescription

Foundation 为 Error 提供的可读描述入口。

```swift
var localizedDescription: String { get }
```

来自 Foundation 的 `Error` 扩展，由系统桥接 `LocalizedError` 描述；不是 SDK 的另一套错误码或独立恢复逻辑。当前错误说明可经 `errorDescription` 获得，业务分支仍使用稳定 `code`。不要解析系统描述文本或假定它已按应用语言翻译。

文档 ID：`apple:symbol:s:s5ErrorP10FoundationE20localizedDescriptionSSvp::SYNTHESIZED::s:10PrinterSDK0A8SDKErrorV`

符号种类：`swift.property`；访问级别：`public`。

这是编译器为当前类型带入的协议扩展／合成公共成员。

编译器 availability（保留基线原值）：

```json
[{"domain":"iOS","introduced":{"major":8,"minor":0}},{"domain":"macOS","introduced":{"major":10,"minor":10}},{"domain":"tvOS","introduced":{"major":9,"minor":0}},{"domain":"watchOS","introduced":{"major":2,"minor":0}}]
```

扩展来源（保留基线原值）：

```json
{"extendedModule":"Swift","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| String | `s:SS` |

<a id="api-1072a2373d94dcac09c2"></a>

### PrinterSDKError.recoverySuggestion

Foundation 提供的可选恢复建议。

```swift
var recoverySuggestion: String? { get }
```

`LocalizedError` 的 Foundation 默认扩展成员，当前 SDK 没有覆盖，默认 nil；不表示 SDK 会恢复或重试打印。应用应按错误码、连接状态与副作用证据决定下一步，交付不明确时不得自动重放。

文档 ID：`apple:symbol:s:10Foundation14LocalizedErrorPAAE18recoverySuggestionSSSgvp::SYNTHESIZED::s:10PrinterSDK0A8SDKErrorV`

符号种类：`swift.property`；访问级别：`public`。

这是编译器为当前类型带入的协议扩展／合成公共成员。

编译器 availability（保留基线原值）：

```json
[{"domain":"iOS","introduced":{"major":8,"minor":0}},{"domain":"macOS","introduced":{"major":10,"minor":10}},{"domain":"tvOS","introduced":{"major":9,"minor":0}},{"domain":"watchOS","introduced":{"major":2,"minor":0}}]
```

扩展来源（保留基线原值）：

```json
{"extendedModule":"Foundation","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| String | `s:SS` |

<a id="api-7a656acf1cbdc7b0f7ad"></a>

## ReceiptFinishOptions

<a id="api-a4749e8b9f0e1acedc93"></a>

### ReceiptFinishOptions

小票结束留白、切纸与作用范围。

```swift
struct ReceiptFinishOptions
```

默认零留白、不切纸、eachCopy。只保存选项，build/submit判断有效性；当前切纸和endOfJob范围尚不支持，不能静默忽略。

文档 ID：`apple:symbol:s:10PrinterSDK20ReceiptFinishOptionsV`

符号种类：`swift.struct`；访问级别：`public`。

<a id="api-146d593bdf6bbf7c6947"></a>

### ReceiptFinishOptions.cut

切纸方式，默认none；full/partial当前提交返回不支持。

```swift
let cut: CutMode
```

初始化后不可变；属于作业参数而非连接默认配置，最终有效性由build/submit校验。

文档 ID：`apple:symbol:s:10PrinterSDK20ReceiptFinishOptionsV3cutAA7CutModeOvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| CutMode | `s:10PrinterSDK7CutModeO` |

<a id="api-e8935bf12ba754426517"></a>

### ReceiptFinishOptions.init(trailingBlankDots:cut:scope:)

保存ReceiptFinishOptions的不可变作业参数。

```swift
init(trailingBlankDots: UInt32 = 0, cut: CutMode = .none, scope: FinishScope = .eachCopy)
```

默认零留白、不切纸、eachCopy。只保存选项，build/submit判断有效性；当前切纸和endOfJob范围尚不支持，不能静默忽略。 此初始化不执行IO，也不提前证明参数或硬件支持；实际校验在Builder.build与MobilePrinter.submit进行。

文档 ID：`apple:symbol:s:10PrinterSDK20ReceiptFinishOptionsV17trailingBlankDots3cut5scopeACs6UInt32V_AA7CutModeOAA0D5ScopeOtcfc`

符号种类：`swift.init`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| CutMode | `s:10PrinterSDK7CutModeO` |
| FinishScope | `s:10PrinterSDK11FinishScopeO` |
| UInt32 | `s:s6UInt32V` |

<a id="api-e299c0399f12e84fa692"></a>

### ReceiptFinishOptions.scope

结束作用范围，默认eachCopy；endOfJob当前后端不支持。

```swift
let scope: FinishScope
```

初始化后不可变；属于作业参数而非连接默认配置，最终有效性由build/submit校验。

文档 ID：`apple:symbol:s:10PrinterSDK20ReceiptFinishOptionsV5scopeAA0D5ScopeOvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| FinishScope | `s:10PrinterSDK11FinishScopeO` |

<a id="api-d04d8f4bdcdb75182b55"></a>

### ReceiptFinishOptions.trailingBlankDots

结束留白（点），默认0，计入整单预算。

```swift
let trailingBlankDots: UInt32
```

初始化后不可变；属于作业参数而非连接默认配置，最终有效性由build/submit校验。

文档 ID：`apple:symbol:s:10PrinterSDK20ReceiptFinishOptionsV17trailingBlankDotss6UInt32Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-082376dfdfb0bb2ec7b4"></a>

## ReceiptJobOptions

<a id="api-3fbbb8e14aedd2ed7caf"></a>

### ReceiptJobOptions

小票作业的不可变累计长度预算。

```swift
struct ReceiptJobOptions
```

只保存参数，build/submit校验；width/DPI/整单预算大于零，copies为1..100。maxTotalLengthDots包含全部副本和结束留白，不是设备单页上限；当前后端不支持非零边距。

文档 ID：`apple:symbol:s:10PrinterSDK17ReceiptJobOptionsV`

符号种类：`swift.struct`；访问级别：`public`。

<a id="api-8d43204aab1591945c81"></a>

### ReceiptJobOptions.copies

份数，默认1、范围1..100。

```swift
let copies: UInt32
```

初始化后不可变；属于作业参数而非连接默认配置，最终有效性由build/submit校验。

文档 ID：`apple:symbol:s:10PrinterSDK17ReceiptJobOptionsV6copiess6UInt32Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-233a665f6be45d09017b"></a>

### ReceiptJobOptions.dpi

小票横纵DPI。

```swift
let dpi: UInt32
```

初始化后不可变；属于作业参数而非连接默认配置，最终有效性由build/submit校验。

文档 ID：`apple:symbol:s:10PrinterSDK17ReceiptJobOptionsV3dpis6UInt32Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-799061c2c571da06d642"></a>

### ReceiptJobOptions.init(widthDots:maxTotalLengthDots:dpi:copies:marginLeftDots:marginRightDots:)

保存ReceiptJobOptions的不可变作业参数。

```swift
init(widthDots: UInt32, maxTotalLengthDots: UInt32, dpi: UInt32, copies: UInt32 = 1, marginLeftDots: UInt32 = 0, marginRightDots: UInt32 = 0)
```

只保存参数，build/submit校验；width/DPI/整单预算大于零，copies为1..100。maxTotalLengthDots包含全部副本和结束留白，不是设备单页上限；当前后端不支持非零边距。 此初始化不执行IO，也不提前证明参数或硬件支持；实际校验在Builder.build与MobilePrinter.submit进行。

文档 ID：`apple:symbol:s:10PrinterSDK17ReceiptJobOptionsV9widthDots014maxTotalLengthG03dpi6copies010marginLeftG00m5RightG0ACs6UInt32V_A5Ktcfc`

符号种类：`swift.init`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-4a91c1efdeef2b1c3bfd"></a>

### ReceiptJobOptions.marginLeftDots

左边距（点），当前后端要求0。

```swift
let marginLeftDots: UInt32
```

初始化后不可变；属于作业参数而非连接默认配置，最终有效性由build/submit校验。

文档 ID：`apple:symbol:s:10PrinterSDK17ReceiptJobOptionsV14marginLeftDotss6UInt32Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-351109631333ae6a55bd"></a>

### ReceiptJobOptions.marginRightDots

右边距（点），当前后端要求0。

```swift
let marginRightDots: UInt32
```

初始化后不可变；属于作业参数而非连接默认配置，最终有效性由build/submit校验。

文档 ID：`apple:symbol:s:10PrinterSDK17ReceiptJobOptionsV15marginRightDotss6UInt32Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-4aaa73a85e092d7d72d7"></a>

### ReceiptJobOptions.maxTotalLengthDots

全部副本与结束留白的累计长度预算（点）。

```swift
let maxTotalLengthDots: UInt32
```

初始化后不可变；属于作业参数而非连接默认配置，最终有效性由build/submit校验。

文档 ID：`apple:symbol:s:10PrinterSDK17ReceiptJobOptionsV18maxTotalLengthDotss6UInt32Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-385b6fe9d8c77250cadb"></a>

### ReceiptJobOptions.widthDots

小票打印宽度（点）。

```swift
let widthDots: UInt32
```

初始化后不可变；属于作业参数而非连接默认配置，最终有效性由build/submit校验。

文档 ID：`apple:symbol:s:10PrinterSDK17ReceiptJobOptionsV9widthDotss6UInt32Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-8a4489e38f48eaabb599"></a>

## ReceiptProfile

<a id="api-5a5eb09c3d6917fdc589"></a>

### ReceiptProfile

不可变的 ESC/POS 小票分辨率和版心点宽配置。

```swift
struct ReceiptProfile
```

由应用明确提供，硬件最大页长和切刀能力仍未知；不以小票毫米宽度推断打印点宽。整单长度预算在 `submitReceiptGray8(maxTotalLengthDots:)` 中独立提供。构造不进行连接、IO 或硬件认证。 可作为本次操作参数传入，不需要在连接时提供，也不会改变连接或其他操作。

文档 ID：`apple:symbol:s:10PrinterSDK14ReceiptProfileV`

符号种类：`swift.struct`；访问级别：`public`。

<a id="api-83facab82caf055f4700"></a>

### ReceiptProfile.dpi

小票内容使用的明确分辨率。

```swift
let dpi: UInt32
```

单位 dots per inch，构造接受 1…255。该值来自调用方而非设备读回，应用须按实际打印头参数配置。

文档 ID：`apple:symbol:s:10PrinterSDK14ReceiptProfileV3dpis6UInt32Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-02f3341679870e3a5030"></a>

### ReceiptProfile.init(dpi:widthDots:)

校验并保存显式小票配置。

```swift
init(dpi: UInt32, widthDots: UInt32) throws
```

`dpi` 为 1…255，单位 dots per inch；`widthDots` 大于 0、不超过 7992 且为 8 的整数倍，单位点。无默认参数；违反条件抛 1001。只保存配置，不自动检测纸宽、页长、协议或切刀；设备初始化和实际提交仍有独立校验。

文档 ID：`apple:symbol:s:10PrinterSDK14ReceiptProfileV3dpi9widthDotsACs6UInt32V_AGtKcfc`

符号种类：`swift.init`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-03511eb7a7b8b538d8f8"></a>

### ReceiptProfile.widthDots

小票作业的可打印版心宽度。

```swift
let widthDots: UInt32
```

单位为点；构造要求 1…7992 且为 8 的整数倍。当前图像以此作为横向边界，不静默缩放或裁剪超宽输入。它不代表介质毫米宽度、每份最大长度或切刀能力。

文档 ID：`apple:symbol:s:10PrinterSDK14ReceiptProfileV9widthDotss6UInt32Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-ef876e2ccd4fa79506c8"></a>

## ResourceSnapshot

<a id="api-c7eec63c89e2fbc86ca7"></a>

### ResourceSnapshot

设备源载荷接纳限制及占用快照。

```swift
struct ResourceSnapshot
```

仅为本地观察，不执行 IO，不预留容量；submit 的原子接纳结果为准。数据不是 App 总内存、编码缓冲区大小或打印机硬件容量。终态 operation 保留结果不会继续占用源载荷接纳预算。

文档 ID：`apple:symbol:s:10PrinterSDK16ResourceSnapshotV`

符号种类：`swift.struct`；访问级别：`public`。

<a id="api-700f799593359550c431"></a>

### ResourceSnapshot.!=(_:_:)

按值比较两个 ResourceSnapshot 是否不同。

```swift
static func != (lhs: borrowing Self, rhs: borrowing Self) -> Bool
```

Swift Equatable 提供的比较，不执行 IO、不刷新状态。比较本次值或枚举，不能把相等的快照当作设备仍在线、硬件正常或纸面打印完成的证明。

文档 ID：`apple:symbol:s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK16ResourceSnapshotV`

符号种类：`swift.func.op`；访问级别：`public`。

这是编译器为当前类型带入的协议扩展／合成公共成员。

扩展来源（保留基线原值）：

```json
{"extendedModule":"Swift","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Bool | `s:Sb` |

<a id="api-5e313de43ea969df329f"></a>

### ResourceSnapshot.maxCopies

SDK 单作业份数上限。

```swift
let maxCopies: UInt32
```

接纳限制，与打印机机械容量或已打印份数无关。

文档 ID：`apple:symbol:s:10PrinterSDK16ResourceSnapshotV9maxCopiess6UInt32Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-bbd896dccb6910f4a329"></a>

### ResourceSnapshot.maxImageBytes

单幅图像源像素载荷上限。

```swift
let maxImageBytes: UInt64
```

单位字节，提交时复制像素并检查；不等同于图像文件压缩长度。

文档 ID：`apple:symbol:s:10PrinterSDK16ResourceSnapshotV13maxImageBytess6UInt64Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt64 | `s:s6UInt64V` |

<a id="api-2dd1865e23d131e32c69"></a>

### ResourceSnapshot.maxJobImageBytes

单个作业的图像源载荷总上限。

```swift
let maxJobImageBytes: UInt64
```

单位字节，多个图像相加，不是最终编码输出大小。

文档 ID：`apple:symbol:s:10PrinterSDK16ResourceSnapshotV16maxJobImageBytess6UInt64Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt64 | `s:s6UInt64V` |

<a id="api-eef6b18bac08cfd25bf4"></a>

### ResourceSnapshot.maxPendingOperations

设备允许的未终态业务操作上限。

```swift
let maxPendingOperations: UInt32
```

包括活动和等待操作；用于展示，提交仍是原子接纳权威。

文档 ID：`apple:symbol:s:10PrinterSDK16ResourceSnapshotV20maxPendingOperationss6UInt32Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-8db625384f435445b854"></a>

### ResourceSnapshot.maxPendingPayloadBytes

设备未终态源载荷合计上限。

```swift
let maxPendingPayloadBytes: UInt64
```

单位字节，与手机进程总内存、实时空闲内存及打印机容量不同。

文档 ID：`apple:symbol:s:10PrinterSDK16ResourceSnapshotV22maxPendingPayloadBytess6UInt64Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt64 | `s:s6UInt64V` |

<a id="api-d245dd07dd8302330165"></a>

### ResourceSnapshot.maxRawBytes

单个 Raw 操作的源字节上限。

```swift
let maxRawBytes: UInt64
```

单位字节，提交时检查，不表示协议编码后或打印机内部缓存大小。

文档 ID：`apple:symbol:s:10PrinterSDK16ResourceSnapshotV11maxRawBytess6UInt64Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt64 | `s:s6UInt64V` |

<a id="api-0661d9cd0f6fe06c55fd"></a>

### ResourceSnapshot.outstandingOperations

当前未终态业务操作数。

```swift
let outstandingOperations: UInt32
```

包括活动与等待操作；不包括已经终态但尚持有结果的 operation。

文档 ID：`apple:symbol:s:10PrinterSDK16ResourceSnapshotV21outstandingOperationss6UInt32Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-acd5a51fe46219464136"></a>

### ResourceSnapshot.pendingPayloadBytes

当前未终态操作保留的源载荷字节数。

```swift
let pendingPayloadBytes: UInt64
```

包括等待和活动操作，终态后释放预算；观察不会锁定剩余容量。

文档 ID：`apple:symbol:s:10PrinterSDK16ResourceSnapshotV19pendingPayloadBytess6UInt64Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt64 | `s:s6UInt64V` |

<a id="api-313c383cb33723929d18"></a>

## SDKInfo

<a id="api-b963dd90e312ec5418c9"></a>

### SDKInfo

实际加载原生运行库的身份。

```swift
struct SDKInfo
```

由原生 C ABI 返回版本和构建身份，与 Demo BuildConfig 独立。生产 CMake 将原生版本、分发 VERSION 和源输入 SHA-256 编入库；缺失构建身份明确未知。该信息不证明签名、发布或硬件兼容。

文档 ID：`apple:symbol:s:10PrinterSDK7SDKInfoV`

符号种类：`swift.struct`；访问级别：`public`。

<a id="api-3640fc8aa15d95ee1c79"></a>

### SDKInfo.!=(_:_:)

按值比较两个 SDKInfo 是否不同。

```swift
static func != (lhs: borrowing Self, rhs: borrowing Self) -> Bool
```

Swift Equatable 提供的比较，不执行 IO、不刷新状态。比较本次值或枚举，不能把相等的快照当作设备仍在线、硬件正常或纸面打印完成的证明。

文档 ID：`apple:symbol:s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK7SDKInfoV`

符号种类：`swift.func.op`；访问级别：`public`。

这是编译器为当前类型带入的协议扩展／合成公共成员。

扩展来源（保留基线原值）：

```json
{"extendedModule":"Swift","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Bool | `s:Sb` |

<a id="api-9babe8011e2c916ddfaa"></a>

### SDKInfo.abiRevision

实际加载库的ABI修订号。

```swift
let abiRevision: UInt32
```

当前包装要求ABI3且修订至少1；版本来自实际加载库，不是Demo资源。

文档 ID：`apple:symbol:s:10PrinterSDK7SDKInfoV11abiRevisions6UInt32Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-d86873128ef22ecdf11c"></a>

### SDKInfo.abiVersion

实际加载库的ABI主版本。

```swift
let abiVersion: UInt32
```

当前为3，须与本包装一致；ABI2不能混用，必须成套更换头、库和平台包装。

文档 ID：`apple:symbol:s:10PrinterSDK7SDKInfoV10abiVersions6UInt32Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-00f428edf3dc4b9c681e"></a>

### SDKInfo.buildID

可选原生源构建 SHA-256 标识。

```swift
let buildID: String?
```

未知时为 nil。生产 CMake 对受审定原生源和构建文件白名单计算，不包含 Git dirty、主机路径、环境或私有配置；不等同于完整二进制哈希。

文档 ID：`apple:symbol:s:10PrinterSDK7SDKInfoV7buildIDSSSgvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| String | `s:SS` |

<a id="api-9fa418f7057af6a2344f"></a>

### SDKInfo.distributionVersion

构建库时嵌入的分发版本。

```swift
let distributionVersion: String
```

生产 CMake 来源于 VERSION；未嵌入时为空字符串，不猜测或继承 Demo 版本。

文档 ID：`apple:symbol:s:10PrinterSDK7SDKInfoV19distributionVersionSSvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| String | `s:SS` |

<a id="api-7a66a92b85f58e9fa84d"></a>

### SDKInfo.nativeVersion

实际加载原生SDK的语义版本。

```swift
let nativeVersion: String
```

生产CMake来源于PROJECT_VERSION，当前0.3.0；SwiftPM源码构建使用同版只读fallback。此值不表示已发布或硬件验收。

文档 ID：`apple:symbol:s:10PrinterSDK7SDKInfoV13nativeVersionSSvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| String | `s:SS` |

<a id="api-5c2f7f6243349bf78f99"></a>

## ScalarConstraint

<a id="api-4aa08db4f6b0bb842bd5"></a>

### ScalarConstraint

不可变标量约束及依据。

```swift
struct ScalarConstraint
```

parameterId 指定约束，known=false 表示未知，不能解释为无限制。源载荷约束来自 SDK 接纳策略；协议几何或硬件支持仍须使用相应配置和能力证据。

文档 ID：`apple:symbol:s:10PrinterSDK16ScalarConstraintV`

符号种类：`swift.struct`；访问级别：`public`。

<a id="api-f47a4d2402a65d0762e8"></a>

### ScalarConstraint.!=(_:_:)

按值比较两个 ScalarConstraint 是否不同。

```swift
static func != (lhs: borrowing Self, rhs: borrowing Self) -> Bool
```

Swift Equatable 提供的比较，不执行 IO、不刷新状态。比较本次值或枚举，不能把相等的快照当作设备仍在线、硬件正常或纸面打印完成的证明。

文档 ID：`apple:symbol:s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK16ScalarConstraintV`

符号种类：`swift.func.op`；访问级别：`public`。

这是编译器为当前类型带入的协议扩展／合成公共成员。

扩展来源（保留基线原值）：

```json
{"extendedModule":"Swift","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Bool | `s:Sb` |

<a id="api-350f4bf05e3c6634e8fd"></a>

### ScalarConstraint.evidence

范围的证据来源。

```swift
let evidence: UInt32
```

区分 SDK 接纳策略、协议定义及其他证据，不能把 SDK 限制解释为已验证硬件能力。

文档 ID：`apple:symbol:s:10PrinterSDK16ScalarConstraintV8evidences6UInt32Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-cbf34d5c1705b3a3f4b2"></a>

### ScalarConstraint.isKnown

此约束是否有可靠已知范围。

```swift
let isKnown: Bool
```

false 时 minimum、maximum、step 不能解释为无限制或硬件保证。

文档 ID：`apple:symbol:s:10PrinterSDK16ScalarConstraintV7isKnownSbvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Bool | `s:Sb` |

<a id="api-7fd038757eba6b305b57"></a>

### ScalarConstraint.maximum

已知约束的闭区间最大值。

```swift
let maximum: Int32
```

单位由 parameterId 决定；仅 known=true 时有意义。

文档 ID：`apple:symbol:s:10PrinterSDK16ScalarConstraintV7maximums5Int32Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Int32 | `s:s5Int32V` |

<a id="api-0d3fcd23d2d40d84f9ae"></a>

### ScalarConstraint.minimum

已知约束的闭区间最小值。

```swift
let minimum: Int32
```

单位由 parameterId 决定；仅 known=true 时有意义。

文档 ID：`apple:symbol:s:10PrinterSDK16ScalarConstraintV7minimums5Int32Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Int32 | `s:s5Int32V` |

<a id="api-ea4f31b903b4fdfb80db"></a>

### ScalarConstraint.parameter

本快照的约束参数标识。

```swift
let parameter: PrinterConstraint
```

使用 COPIES、MEDIA_GAP_UM、JOB_IMAGE_BYTES 或 PENDING_PAYLOAD_BYTES 查询；未知标识返回已知位为 false 的约束。

文档 ID：`apple:symbol:s:10PrinterSDK16ScalarConstraintV9parameterAA0aD0Ovp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| PrinterConstraint | `s:10PrinterSDK0A10ConstraintO` |

<a id="api-d26a0e2876b56d2d5331"></a>

### ScalarConstraint.step

已知约束的取值步长。

```swift
let step: UInt32
```

在 minimum 起点按此步长取值；仅 known=true 时有意义。

文档 ID：`apple:symbol:s:10PrinterSDK16ScalarConstraintV4steps6UInt32Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-4b76054d7c5874efb333"></a>

## StatusQueryConfiguration

<a id="api-ae4e3d4063252a2520f5"></a>

### StatusQueryConfiguration

逐次硬件状态查询配置。

```swift
struct StatusQueryConfiguration
```

与打印几何、纸宽和 DPI 独立，接纳时复制，不改变连接默认值。CPCL/ESC_POS 当前未验证状态指令，因此查询同步返回 1004 UNSUPPORTED_OPERATION 且零打印机 IO；硬件仍未知。

文档 ID：`apple:symbol:s:10PrinterSDK24StatusQueryConfigurationV`

符号种类：`swift.struct`；访问级别：`public`。

<a id="api-dab1f65c670e0e9b0329"></a>

### StatusQueryConfiguration.!=(_:_:)

按值比较两个 StatusQueryConfiguration 是否不同。

```swift
static func != (lhs: borrowing Self, rhs: borrowing Self) -> Bool
```

Swift Equatable 提供的比较，不执行 IO、不刷新状态。比较本次值或枚举，不能把相等的快照当作设备仍在线、硬件正常或纸面打印完成的证明。

文档 ID：`apple:symbol:s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK24StatusQueryConfigurationV`

符号种类：`swift.func.op`；访问级别：`public`。

这是编译器为当前类型带入的协议扩展／合成公共成员。

扩展来源（保留基线原值）：

```json
{"extendedModule":"Swift","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Bool | `s:Sb` |

<a id="api-74892691e3057eed07a0"></a>

### StatusQueryConfiguration.init(printerProtocol:queryTimeoutMilliseconds:)

创建本次状态查询配置。

```swift
init(printerProtocol: PrinterProtocol, queryTimeoutMilliseconds: UInt32 = 5000) throws
```

printerProtocol 显式选择 .cpcl 或 .escpos；queryTimeoutMilliseconds 默认 5000 毫秒，须大于 0，零值抛 PrinterSDKError code 1001。构造不执行 IO，不要求打印几何；选择协议不证明目标固件支持状态命令。

文档 ID：`apple:symbol:s:10PrinterSDK24StatusQueryConfigurationV15printerProtocol24queryTimeoutMillisecondsAcA0aG0O_s6UInt32VtKcfc`

符号种类：`swift.init`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| PrinterProtocol | `s:10PrinterSDK0A8ProtocolO` |
| UInt32 | `s:s6UInt32V` |

<a id="api-db6c8f80b95aadcc9848"></a>

### StatusQueryConfiguration.printerProtocol

本次状态查询选择的协议。

```swift
let printerProtocol: PrinterProtocol
```

只接受 CPCL=1 或 ESC_POS=2；选择协议不证明目标固件支持状态读回。

文档 ID：`apple:symbol:s:10PrinterSDK24StatusQueryConfigurationV15printerProtocolAA0aG0Ovp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| PrinterProtocol | `s:10PrinterSDK0A8ProtocolO` |

<a id="api-34db23365fe06623acb1"></a>

### StatusQueryConfiguration.queryTimeoutMilliseconds

本次请求／响应事务的超时预算。

```swift
let queryTimeoutMilliseconds: UInt32
```

必须大于 0，单位毫秒；与 queryStatus 的排队及操作总预算分别设置，不能据此绕过总预算。

文档 ID：`apple:symbol:s:10PrinterSDK24StatusQueryConfigurationV24queryTimeoutMillisecondss6UInt32Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

## 枚举声明顺序与 raw 值

顺序来自编译器 AST；raw 值保留符号、类型和值，不用展示顺序代替 raw 值。

### ConnectionState

| 声明顺序（从 0 起） | case | raw 值 | 编译器原值 |
| --- | --- | --- | --- |
| 0 | `disconnected` | `0` | `{"kind":"integer","negative":false,"value":"0"}` |
| 1 | `connecting` | `1` | `{"kind":"integer","negative":false,"value":"1"}` |
| 2 | `connected` | `2` | `{"kind":"integer","negative":false,"value":"2"}` |
| 3 | `disconnecting` | `3` | `{"kind":"integer","negative":false,"value":"3"}` |

### CutMode

| 声明顺序（从 0 起） | case | raw 值 | 编译器原值 |
| --- | --- | --- | --- |
| 0 | `none` | `0` | `{"kind":"integer","negative":false,"value":"0"}` |
| 1 | `full` | `1` | `{"kind":"integer","negative":false,"value":"1"}` |
| 2 | `partial` | `2` | `{"kind":"integer","negative":false,"value":"2"}` |

### DeviceWorkState

| 声明顺序（从 0 起） | case | raw 值 | 编译器原值 |
| --- | --- | --- | --- |
| 0 | `idle` | `0` | `{"kind":"integer","negative":false,"value":"0"}` |
| 1 | `queued` | `1` | `{"kind":"integer","negative":false,"value":"1"}` |
| 2 | `preparing` | `2` | `{"kind":"integer","negative":false,"value":"2"}` |
| 3 | `sending` | `3` | `{"kind":"integer","negative":false,"value":"3"}` |

### DiagnosticSource

| 声明顺序（从 0 起） | case | raw 值 | 编译器原值 |
| --- | --- | --- | --- |
| 0 | `none` | `0` | `{"kind":"integer","negative":false,"value":"0"}` |
| 1 | `connect` | `1` | `{"kind":"integer","negative":false,"value":"1"}` |
| 2 | `operation` | `2` | `{"kind":"integer","negative":false,"value":"2"}` |
| 3 | `transport` | `3` | `{"kind":"integer","negative":false,"value":"3"}` |
| 4 | `userDisconnect` | `4` | `{"kind":"integer","negative":false,"value":"4"}` |

### FinishScope

| 声明顺序（从 0 起） | case | raw 值 | 编译器原值 |
| --- | --- | --- | --- |
| 0 | `eachCopy` | `0` | `{"kind":"integer","negative":false,"value":"0"}` |
| 1 | `endOfJob` | `1` | `{"kind":"integer","negative":false,"value":"1"}` |

### HardwareState

| 声明顺序（从 0 起） | case | raw 值 | 编译器原值 |
| --- | --- | --- | --- |
| 0 | `unknown` | `0` | `{"kind":"integer","negative":false,"value":"0"}` |

### ImageAlgorithm

| 声明顺序（从 0 起） | case | raw 值 | 编译器原值 |
| --- | --- | --- | --- |
| 0 | `threshold` | `0` | `{"kind":"integer","negative":false,"value":"0"}` |
| 1 | `floydSteinberg` | `1` | `{"kind":"integer","negative":false,"value":"1"}` |
| 2 | `bayer4x4` | `2` | `{"kind":"integer","negative":false,"value":"2"}` |
| 3 | `bayer8x8` | `3` | `{"kind":"integer","negative":false,"value":"3"}` |

### MediaMode

| 声明顺序（从 0 起） | case | raw 值 | 编译器原值 |
| --- | --- | --- | --- |
| 0 | `gap` | `1` | `{"kind":"integer","negative":false,"value":"1"}` |
| 1 | `blackMark` | `2` | `{"kind":"integer","negative":false,"value":"2"}` |
| 2 | `continuous` | `3` | `{"kind":"integer","negative":false,"value":"3"}` |

### MediaVerificationPolicy

| 声明顺序（从 0 起） | case | raw 值 | 编译器原值 |
| --- | --- | --- | --- |
| 0 | `allowLastSent` | `1` | `{"kind":"integer","negative":false,"value":"1"}` |
| 1 | `requireReadback` | `2` | `{"kind":"integer","negative":false,"value":"2"}` |

### PixelFormat

| 声明顺序（从 0 起） | case | raw 值 | 编译器原值 |
| --- | --- | --- | --- |
| 0 | `gray8` | `1` | `{"kind":"integer","negative":false,"value":"1"}` |
| 1 | `rgb24` | `2` | `{"kind":"integer","negative":false,"value":"2"}` |
| 2 | `rgba32` | `3` | `{"kind":"integer","negative":false,"value":"3"}` |

### PrinterConstraint

| 声明顺序（从 0 起） | case | raw 值 | 编译器原值 |
| --- | --- | --- | --- |
| 0 | `copies` | `1` | `{"kind":"integer","negative":false,"value":"1"}` |
| 1 | `mediaGapMicrometers` | `2` | `{"kind":"integer","negative":false,"value":"2"}` |
| 2 | `jobImageBytes` | `3` | `{"kind":"integer","negative":false,"value":"3"}` |
| 3 | `pendingPayloadBytes` | `4` | `{"kind":"integer","negative":false,"value":"4"}` |

### PrinterFeature

| 声明顺序（从 0 起） | case | raw 值 | 编译器原值 |
| --- | --- | --- | --- |
| 0 | `label` | `1` | `{"kind":"integer","negative":false,"value":"1"}` |
| 1 | `receipt` | `2` | `{"kind":"integer","negative":false,"value":"2"}` |
| 2 | `status` | `3` | `{"kind":"integer","negative":false,"value":"3"}` |
| 3 | `media` | `4` | `{"kind":"integer","negative":false,"value":"4"}` |
| 4 | `cut` | `5` | `{"kind":"integer","negative":false,"value":"5"}` |
| 5 | `raw` | `6` | `{"kind":"integer","negative":false,"value":"6"}` |
| 6 | `image` | `7` | `{"kind":"integer","negative":false,"value":"7"}` |
| 7 | `feedToNextLabel` | `8` | `{"kind":"integer","negative":false,"value":"8"}` |

### PrinterProtocol

| 声明顺序（从 0 起） | case | raw 值 | 编译器原值 |
| --- | --- | --- | --- |
| 0 | `cpcl` | `1` | `{"kind":"integer","negative":false,"value":"1"}` |
| 1 | `escpos` | `2` | `{"kind":"integer","negative":false,"value":"2"}` |

## 编译器公共关系

完整保留 **447** 条继承、协议遵循、成员及合成关系；
source 和 target 使用精确编译器标识，外部 Swift/Foundation 类型也保持原值。

| kind | source | target | 其他编译器字段 |
| --- | --- | --- | --- |
| `conformsTo` | `s:10PrinterSDK010DiscoveredA0V` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:10PrinterSDK010DiscoveredA0V` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:10PrinterSDK06MobileA0C` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:10PrinterSDK06MobileA0C` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:10PrinterSDK0A10ConstraintO` | `s:SH` | `{}` |
| `conformsTo` | `s:10PrinterSDK0A10ConstraintO` | `s:SQ` | `{}` |
| `conformsTo` | `s:10PrinterSDK0A10ConstraintO` | `s:SY` | `{}` |
| `conformsTo` | `s:10PrinterSDK0A10ConstraintO` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:10PrinterSDK0A10ConstraintO` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:10PrinterSDK0A12CapabilitiesV` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:10PrinterSDK0A12CapabilitiesV` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:10PrinterSDK0A5ImageC` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:10PrinterSDK0A5ImageC` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:10PrinterSDK0A7FeatureO` | `s:SH` | `{}` |
| `conformsTo` | `s:10PrinterSDK0A7FeatureO` | `s:SQ` | `{}` |
| `conformsTo` | `s:10PrinterSDK0A7FeatureO` | `s:SY` | `{}` |
| `conformsTo` | `s:10PrinterSDK0A7FeatureO` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:10PrinterSDK0A7FeatureO` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:10PrinterSDK0A7ManagerC` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:10PrinterSDK0A7ManagerC` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:10PrinterSDK0A8GeometryV` | `s:SQ` | `{}` |
| `conformsTo` | `s:10PrinterSDK0A8GeometryV` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:10PrinterSDK0A8GeometryV` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:10PrinterSDK0A8ProtocolO` | `s:SH` | `{}` |
| `conformsTo` | `s:10PrinterSDK0A8ProtocolO` | `s:SQ` | `{}` |
| `conformsTo` | `s:10PrinterSDK0A8ProtocolO` | `s:SY` | `{}` |
| `conformsTo` | `s:10PrinterSDK0A8ProtocolO` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:10PrinterSDK0A8ProtocolO` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:10PrinterSDK0A8SDKErrorV` | `s:10Foundation14LocalizedErrorP` | `{}` |
| `conformsTo` | `s:10PrinterSDK0A8SDKErrorV` | `s:SQ` | `{}` |
| `conformsTo` | `s:10PrinterSDK0A8SDKErrorV` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:10PrinterSDK0A8SDKErrorV` | `s:s5ErrorP` | `{}` |
| `conformsTo` | `s:10PrinterSDK0A8SDKErrorV` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:10PrinterSDK10BLEOptionsV` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:10PrinterSDK10BLEOptionsV` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:10PrinterSDK10BLEScannerC` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:10PrinterSDK10BLEScannerC` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:10PrinterSDK11CpclProfileV` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:10PrinterSDK11CpclProfileV` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:10PrinterSDK11FinishScopeO` | `s:SH` | `{}` |
| `conformsTo` | `s:10PrinterSDK11FinishScopeO` | `s:SQ` | `{}` |
| `conformsTo` | `s:10PrinterSDK11FinishScopeO` | `s:SY` | `{}` |
| `conformsTo` | `s:10PrinterSDK11FinishScopeO` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:10PrinterSDK11FinishScopeO` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:10PrinterSDK11PixelFormatO` | `s:SH` | `{}` |
| `conformsTo` | `s:10PrinterSDK11PixelFormatO` | `s:SQ` | `{}` |
| `conformsTo` | `s:10PrinterSDK11PixelFormatO` | `s:SY` | `{}` |
| `conformsTo` | `s:10PrinterSDK11PixelFormatO` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:10PrinterSDK11PixelFormatO` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:10PrinterSDK13HardwareStateO` | `s:SH` | `{}` |
| `conformsTo` | `s:10PrinterSDK13HardwareStateO` | `s:SQ` | `{}` |
| `conformsTo` | `s:10PrinterSDK13HardwareStateO` | `s:SY` | `{}` |
| `conformsTo` | `s:10PrinterSDK13HardwareStateO` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:10PrinterSDK13HardwareStateO` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:10PrinterSDK14FeatureSupportV` | `s:SQ` | `{}` |
| `conformsTo` | `s:10PrinterSDK14FeatureSupportV` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:10PrinterSDK14FeatureSupportV` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:10PrinterSDK14ImageAlgorithmO` | `s:SH` | `{}` |
| `conformsTo` | `s:10PrinterSDK14ImageAlgorithmO` | `s:SQ` | `{}` |
| `conformsTo` | `s:10PrinterSDK14ImageAlgorithmO` | `s:SY` | `{}` |
| `conformsTo` | `s:10PrinterSDK14ImageAlgorithmO` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:10PrinterSDK14ImageAlgorithmO` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:10PrinterSDK14PrintOperationC` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:10PrinterSDK14PrintOperationC` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:10PrinterSDK14ReceiptProfileV` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:10PrinterSDK14ReceiptProfileV` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:10PrinterSDK15ConnectionStateO` | `s:SH` | `{}` |
| `conformsTo` | `s:10PrinterSDK15ConnectionStateO` | `s:SQ` | `{}` |
| `conformsTo` | `s:10PrinterSDK15ConnectionStateO` | `s:SY` | `{}` |
| `conformsTo` | `s:10PrinterSDK15ConnectionStateO` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:10PrinterSDK15ConnectionStateO` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:10PrinterSDK15DeviceWorkStateO` | `s:SH` | `{}` |
| `conformsTo` | `s:10PrinterSDK15DeviceWorkStateO` | `s:SQ` | `{}` |
| `conformsTo` | `s:10PrinterSDK15DeviceWorkStateO` | `s:SY` | `{}` |
| `conformsTo` | `s:10PrinterSDK15DeviceWorkStateO` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:10PrinterSDK15DeviceWorkStateO` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:10PrinterSDK15ErrorDiagnosticV` | `s:SQ` | `{}` |
| `conformsTo` | `s:10PrinterSDK15ErrorDiagnosticV` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:10PrinterSDK15ErrorDiagnosticV` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:10PrinterSDK15LabelJobOptionsV` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:10PrinterSDK15LabelJobOptionsV` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:10PrinterSDK15OperationResultV` | `s:SQ` | `{}` |
| `conformsTo` | `s:10PrinterSDK15OperationResultV` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:10PrinterSDK15OperationResultV` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:10PrinterSDK16DiagnosticSourceO` | `s:SH` | `{}` |
| `conformsTo` | `s:10PrinterSDK16DiagnosticSourceO` | `s:SQ` | `{}` |
| `conformsTo` | `s:10PrinterSDK16DiagnosticSourceO` | `s:SY` | `{}` |
| `conformsTo` | `s:10PrinterSDK16DiagnosticSourceO` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:10PrinterSDK16DiagnosticSourceO` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:10PrinterSDK16MediaRequirementV` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:10PrinterSDK16MediaRequirementV` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:10PrinterSDK16ResourceSnapshotV` | `s:SQ` | `{}` |
| `conformsTo` | `s:10PrinterSDK16ResourceSnapshotV` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:10PrinterSDK16ResourceSnapshotV` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:10PrinterSDK16ScalarConstraintV` | `s:SQ` | `{}` |
| `conformsTo` | `s:10PrinterSDK16ScalarConstraintV` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:10PrinterSDK16ScalarConstraintV` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:10PrinterSDK17OperationSnapshotV` | `s:SQ` | `{}` |
| `conformsTo` | `s:10PrinterSDK17OperationSnapshotV` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:10PrinterSDK17OperationSnapshotV` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:10PrinterSDK17ReceiptJobOptionsV` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:10PrinterSDK17ReceiptJobOptionsV` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:10PrinterSDK19DeviceStateSnapshotV` | `s:SQ` | `{}` |
| `conformsTo` | `s:10PrinterSDK19DeviceStateSnapshotV` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:10PrinterSDK19DeviceStateSnapshotV` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:10PrinterSDK20BLECreditFlowControlV` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:10PrinterSDK20BLECreditFlowControlV` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:10PrinterSDK20HardwareStatusResultV` | `s:SQ` | `{}` |
| `conformsTo` | `s:10PrinterSDK20HardwareStatusResultV` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:10PrinterSDK20HardwareStatusResultV` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:10PrinterSDK20ReceiptFinishOptionsV` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:10PrinterSDK20ReceiptFinishOptionsV` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:10PrinterSDK21DeviceActiveOperationV` | `s:SQ` | `{}` |
| `conformsTo` | `s:10PrinterSDK21DeviceActiveOperationV` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:10PrinterSDK21DeviceActiveOperationV` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:10PrinterSDK23MediaVerificationPolicyO` | `s:SH` | `{}` |
| `conformsTo` | `s:10PrinterSDK23MediaVerificationPolicyO` | `s:SQ` | `{}` |
| `conformsTo` | `s:10PrinterSDK23MediaVerificationPolicyO` | `s:SY` | `{}` |
| `conformsTo` | `s:10PrinterSDK23MediaVerificationPolicyO` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:10PrinterSDK23MediaVerificationPolicyO` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:10PrinterSDK23ObservationSubscriptionC` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:10PrinterSDK23ObservationSubscriptionC` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:10PrinterSDK24StatusQueryConfigurationV` | `s:SQ` | `{}` |
| `conformsTo` | `s:10PrinterSDK24StatusQueryConfigurationV` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:10PrinterSDK24StatusQueryConfigurationV` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:10PrinterSDK7CutModeO` | `s:SH` | `{}` |
| `conformsTo` | `s:10PrinterSDK7CutModeO` | `s:SQ` | `{}` |
| `conformsTo` | `s:10PrinterSDK7CutModeO` | `s:SY` | `{}` |
| `conformsTo` | `s:10PrinterSDK7CutModeO` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:10PrinterSDK7CutModeO` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:10PrinterSDK7SDKInfoV` | `s:SQ` | `{}` |
| `conformsTo` | `s:10PrinterSDK7SDKInfoV` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:10PrinterSDK7SDKInfoV` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:10PrinterSDK8PrintJobC` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:10PrinterSDK8PrintJobC` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:10PrinterSDK8PrintJobC7BuilderC` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:10PrinterSDK8PrintJobC7BuilderC` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:10PrinterSDK9MediaModeO` | `s:SH` | `{}` |
| `conformsTo` | `s:10PrinterSDK9MediaModeO` | `s:SQ` | `{}` |
| `conformsTo` | `s:10PrinterSDK9MediaModeO` | `s:SY` | `{}` |
| `conformsTo` | `s:10PrinterSDK9MediaModeO` | `s:s12CaseIterableP` | `{}` |
| `conformsTo` | `s:10PrinterSDK9MediaModeO` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:10PrinterSDK9MediaModeO` | `s:s8SendableP` | `{}` |
| `memberOf` | `s:10Foundation14LocalizedErrorPAAE10helpAnchorSSSgvp::SYNTHESIZED::s:10PrinterSDK0A8SDKErrorV` | `s:10PrinterSDK0A8SDKErrorV` | `{"sourceOrigin":{"displayName":"LocalizedError.helpAnchor","identifier":"s:10Foundation14LocalizedErrorP10helpAnchorSSSgvp"}}` |
| `memberOf` | `s:10Foundation14LocalizedErrorPAAE13failureReasonSSSgvp::SYNTHESIZED::s:10PrinterSDK0A8SDKErrorV` | `s:10PrinterSDK0A8SDKErrorV` | `{"sourceOrigin":{"displayName":"LocalizedError.failureReason","identifier":"s:10Foundation14LocalizedErrorP13failureReasonSSSgvp"}}` |
| `memberOf` | `s:10Foundation14LocalizedErrorPAAE18recoverySuggestionSSSgvp::SYNTHESIZED::s:10PrinterSDK0A8SDKErrorV` | `s:10PrinterSDK0A8SDKErrorV` | `{"sourceOrigin":{"displayName":"LocalizedError.recoverySuggestion","identifier":"s:10Foundation14LocalizedErrorP18recoverySuggestionSSSgvp"}}` |
| `memberOf` | `s:10PrinterSDK010DiscoveredA0V10identifier10Foundation4UUIDVvp` | `s:10PrinterSDK010DiscoveredA0V` | `{}` |
| `memberOf` | `s:10PrinterSDK010DiscoveredA0V18advertisedServicesSaySSGvp` | `s:10PrinterSDK010DiscoveredA0V` | `{}` |
| `memberOf` | `s:10PrinterSDK010DiscoveredA0V4nameSSSgvp` | `s:10PrinterSDK010DiscoveredA0V` | `{}` |
| `memberOf` | `s:10PrinterSDK010DiscoveredA0V4rssiSivp` | `s:10PrinterSDK010DiscoveredA0V` | `{}` |
| `memberOf` | `s:10PrinterSDK06MobileA0C10constraintyAA16ScalarConstraintVAA0aF0OKF` | `s:10PrinterSDK06MobileA0C` | `{}` |
| `memberOf` | `s:10PrinterSDK06MobileA0C10diagnosticAA15ErrorDiagnosticVyKF` | `s:10PrinterSDK06MobileA0C` | `{}` |
| `memberOf` | `s:10PrinterSDK06MobileA0C10disconnectyyKF` | `s:10PrinterSDK06MobileA0C` | `{}` |
| `memberOf` | `s:10PrinterSDK06MobileA0C11queryStatus13configuration19timeoutMillisecondsAA14PrintOperationCAA0E18QueryConfigurationV_s6UInt32VtKF` | `s:10PrinterSDK06MobileA0C` | `{}` |
| `memberOf` | `s:10PrinterSDK06MobileA0C11submitGray8_5width6height7profile6copies17trailingBlankDots011canvasWidthL011originXDots4form19timeoutMillisecondsAA14PrintOperationC10Foundation4DataV_s6UInt32VAtA11CpclProfileVA3TSgAWSbSgATtKF` | `s:10PrinterSDK06MobileA0C` | `{}` |
| `memberOf` | `s:10PrinterSDK06MobileA0C12capabilitiesAA0A12CapabilitiesVyKF` | `s:10PrinterSDK06MobileA0C` | `{}` |
| `memberOf` | `s:10PrinterSDK06MobileA0C12observeState5queue_AA23ObservationSubscriptionCSo012OS_dispatch_F0C_yAA06DeviceE8SnapshotVYbctKF` | `s:10PrinterSDK06MobileA0C` | `{}` |
| `memberOf` | `s:10PrinterSDK06MobileA0C12setMediaMode_7profile19timeoutMillisecondsAA14PrintOperationCAA0eF0O_AA11CpclProfileVs6UInt32VtKF` | `s:10PrinterSDK06MobileA0C` | `{}` |
| `memberOf` | `s:10PrinterSDK06MobileA0C13bleIdentifier7optionsAC10Foundation4UUIDV_AA10BLEOptionsVtKcfc` | `s:10PrinterSDK06MobileA0C` | `{}` |
| `memberOf` | `s:10PrinterSDK06MobileA0C13stateSnapshotAA011DeviceStateE0VyKF` | `s:10PrinterSDK06MobileA0C` | `{}` |
| `memberOf` | `s:10PrinterSDK06MobileA0C15connectionStateAA010ConnectionE0OyKF` | `s:10PrinterSDK06MobileA0C` | `{}` |
| `memberOf` | `s:10PrinterSDK06MobileA0C15feedToNextLabel7profile19timeoutMillisecondsAA14PrintOperationCAA11CpclProfileV_s6UInt32VtKF` | `s:10PrinterSDK06MobileA0C` | `{}` |
| `memberOf` | `s:10PrinterSDK06MobileA0C16resourceSnapshotAA08ResourceE0VyKF` | `s:10PrinterSDK06MobileA0C` | `{}` |
| `memberOf` | `s:10PrinterSDK06MobileA0C18submitReceiptGray8_5width6height7profile6copies18maxTotalLengthDots013trailingBlankN03cut11finishScope19timeoutMillisecondsAA14PrintOperationC10Foundation4DataV_s6UInt32VAtA0E7ProfileVA3tA7CutModeOAA06FinishS0OATtKF` | `s:10PrinterSDK06MobileA0C` | `{}` |
| `memberOf` | `s:10PrinterSDK06MobileA0C5closeyyF` | `s:10PrinterSDK06MobileA0C` | `{}` |
| `memberOf` | `s:10PrinterSDK06MobileA0C6submit_7profile19timeoutMillisecondsAA14PrintOperationCAA0H3JobC_AA11CpclProfileVs6UInt32VtKF` | `s:10PrinterSDK06MobileA0C` | `{}` |
| `memberOf` | `s:10PrinterSDK06MobileA0C6submit_7profile19timeoutMillisecondsAA14PrintOperationCAA0H3JobC_AA14ReceiptProfileVs6UInt32VtKF` | `s:10PrinterSDK06MobileA0C` | `{}` |
| `memberOf` | `s:10PrinterSDK06MobileA0C7connect19timeoutMillisecondsys6UInt32V_tKF` | `s:10PrinterSDK06MobileA0C` | `{}` |
| `memberOf` | `s:10PrinterSDK06MobileA0C7featureyAA14FeatureSupportVAA0aE0OKF` | `s:10PrinterSDK06MobileA0C` | `{}` |
| `memberOf` | `s:10PrinterSDK06MobileA0C9submitRaw_6copies19timeoutMillisecondsAA14PrintOperationC10Foundation4DataV_s6UInt32VAMtKF` | `s:10PrinterSDK06MobileA0C` | `{}` |
| `memberOf` | `s:10PrinterSDK0A10ConstraintO13jobImageBytesyA2CmF` | `s:10PrinterSDK0A10ConstraintO` | `{}` |
| `memberOf` | `s:10PrinterSDK0A10ConstraintO19mediaGapMicrometersyA2CmF` | `s:10PrinterSDK0A10ConstraintO` | `{}` |
| `memberOf` | `s:10PrinterSDK0A10ConstraintO19pendingPayloadBytesyA2CmF` | `s:10PrinterSDK0A10ConstraintO` | `{}` |
| `memberOf` | `s:10PrinterSDK0A10ConstraintO6copiesyA2CmF` | `s:10PrinterSDK0A10ConstraintO` | `{}` |
| `memberOf` | `s:10PrinterSDK0A10ConstraintO8rawValueACSgs6UInt32V_tcfc` | `s:10PrinterSDK0A10ConstraintO` | `{"sourceOrigin":{"displayName":"RawRepresentable.init(rawValue:)","identifier":"s:SY8rawValuexSg03RawB0Qz_tcfc"}}` |
| `memberOf` | `s:10PrinterSDK0A12CapabilitiesV11constraintsSDyAA0A10ConstraintOAA06ScalarE0VGvp` | `s:10PrinterSDK0A12CapabilitiesV` | `{}` |
| `memberOf` | `s:10PrinterSDK0A12CapabilitiesV8featuresSDyAA0A7FeatureOAA0E7SupportVGvp` | `s:10PrinterSDK0A12CapabilitiesV` | `{}` |
| `memberOf` | `s:10PrinterSDK0A12CapabilitiesV8geometryAA0A8GeometryVvp` | `s:10PrinterSDK0A12CapabilitiesV` | `{}` |
| `memberOf` | `s:10PrinterSDK0A5ImageC5widths6UInt32Vvp` | `s:10PrinterSDK0A5ImageC` | `{}` |
| `memberOf` | `s:10PrinterSDK0A5ImageC6heights6UInt32Vvp` | `s:10PrinterSDK0A5ImageC` | `{}` |
| `memberOf` | `s:10PrinterSDK0A5ImageC6pixels5width6height6format9algorithm9threshold6invert8fitWidth06targetL4DotsAC10Foundation4DataV_s6UInt32VAqA11PixelFormatOAA0C9AlgorithmOAQS2bAQtKcfc` | `s:10PrinterSDK0A5ImageC` | `{}` |
| `memberOf` | `s:10PrinterSDK0A7FeatureO15feedToNextLabelyA2CmF` | `s:10PrinterSDK0A7FeatureO` | `{}` |
| `memberOf` | `s:10PrinterSDK0A7FeatureO3cutyA2CmF` | `s:10PrinterSDK0A7FeatureO` | `{}` |
| `memberOf` | `s:10PrinterSDK0A7FeatureO3rawyA2CmF` | `s:10PrinterSDK0A7FeatureO` | `{}` |
| `memberOf` | `s:10PrinterSDK0A7FeatureO5imageyA2CmF` | `s:10PrinterSDK0A7FeatureO` | `{}` |
| `memberOf` | `s:10PrinterSDK0A7FeatureO5labelyA2CmF` | `s:10PrinterSDK0A7FeatureO` | `{}` |
| `memberOf` | `s:10PrinterSDK0A7FeatureO5mediayA2CmF` | `s:10PrinterSDK0A7FeatureO` | `{}` |
| `memberOf` | `s:10PrinterSDK0A7FeatureO6statusyA2CmF` | `s:10PrinterSDK0A7FeatureO` | `{}` |
| `memberOf` | `s:10PrinterSDK0A7FeatureO7receiptyA2CmF` | `s:10PrinterSDK0A7FeatureO` | `{}` |
| `memberOf` | `s:10PrinterSDK0A7FeatureO8rawValueACSgs6UInt32V_tcfc` | `s:10PrinterSDK0A7FeatureO` | `{"sourceOrigin":{"displayName":"RawRepresentable.init(rawValue:)","identifier":"s:SY8rawValuexSg03RawB0Qz_tcfc"}}` |
| `memberOf` | `s:10PrinterSDK0A7ManagerC5closeyyYaF` | `s:10PrinterSDK0A7ManagerC` | `{}` |
| `memberOf` | `s:10PrinterSDK0A7ManagerC6sharedACvpZ` | `s:10PrinterSDK0A7ManagerC` | `{}` |
| `memberOf` | `s:10PrinterSDK0A7ManagerC7connect_19timeoutMillisecondsyAA06MobileA0C_s6UInt32VtYaKF` | `s:10PrinterSDK0A7ManagerC` | `{}` |
| `memberOf` | `s:10PrinterSDK0A7ManagerC7printer13bleIdentifier7optionsAA06MobileA0C10Foundation4UUIDV_AA10BLEOptionsVtKF` | `s:10PrinterSDK0A7ManagerC` | `{}` |
| `memberOf` | `s:10PrinterSDK0A7ManagerC7releaseyyAA06MobileA0CYaF` | `s:10PrinterSDK0A7ManagerC` | `{}` |
| `memberOf` | `s:10PrinterSDK0A7ManagerCACycfc` | `s:10PrinterSDK0A7ManagerC` | `{}` |
| `memberOf` | `s:10PrinterSDK0A8GeometryV15minCutBlankDotss6UInt32VSgvp` | `s:10PrinterSDK0A8GeometryV` | `{}` |
| `memberOf` | `s:10PrinterSDK0A8GeometryV17maxPageLengthDotss6UInt32VSgvp` | `s:10PrinterSDK0A8GeometryV` | `{}` |
| `memberOf` | `s:10PrinterSDK0A8GeometryV18printableWidthDotss6UInt32VSgvp` | `s:10PrinterSDK0A8GeometryV` | `{}` |
| `memberOf` | `s:10PrinterSDK0A8GeometryV4dpiXs6UInt32VSgvp` | `s:10PrinterSDK0A8GeometryV` | `{}` |
| `memberOf` | `s:10PrinterSDK0A8GeometryV4dpiYs6UInt32VSgvp` | `s:10PrinterSDK0A8GeometryV` | `{}` |
| `memberOf` | `s:10PrinterSDK0A8GeometryV8evidences6UInt32Vvp` | `s:10PrinterSDK0A8GeometryV` | `{}` |
| `memberOf` | `s:10PrinterSDK0A8ProtocolO4cpclyA2CmF` | `s:10PrinterSDK0A8ProtocolO` | `{}` |
| `memberOf` | `s:10PrinterSDK0A8ProtocolO6escposyA2CmF` | `s:10PrinterSDK0A8ProtocolO` | `{}` |
| `memberOf` | `s:10PrinterSDK0A8ProtocolO8rawValueACSgs6UInt32V_tcfc` | `s:10PrinterSDK0A8ProtocolO` | `{"sourceOrigin":{"displayName":"RawRepresentable.init(rawValue:)","identifier":"s:SY8rawValuexSg03RawB0Qz_tcfc"}}` |
| `memberOf` | `s:10PrinterSDK0A8SDKErrorV10diagnosticAA15ErrorDiagnosticVSgvp` | `s:10PrinterSDK0A8SDKErrorV` | `{}` |
| `memberOf` | `s:10PrinterSDK0A8SDKErrorV16errorDescriptionSSSgvp` | `s:10PrinterSDK0A8SDKErrorV` | `{"sourceOrigin":{"displayName":"LocalizedError.errorDescription","identifier":"s:10Foundation14LocalizedErrorP16errorDescriptionSSSgvp"}}` |
| `memberOf` | `s:10PrinterSDK0A8SDKErrorV2eeoiySbAC_ACtFZ` | `s:10PrinterSDK0A8SDKErrorV` | `{"sourceOrigin":{"displayName":"Equatable.==(_:_:)","identifier":"s:SQ2eeoiySbx_xtFZ"}}` |
| `memberOf` | `s:10PrinterSDK0A8SDKErrorV4codes5Int32Vvp` | `s:10PrinterSDK0A8SDKErrorV` | `{}` |
| `memberOf` | `s:10PrinterSDK0A8SDKErrorV_10diagnosticACs5Int32V_AA15ErrorDiagnosticVSgtcfc` | `s:10PrinterSDK0A8SDKErrorV` | `{}` |
| `memberOf` | `s:10PrinterSDK0A8SDKErrorVyACs5Int32Vcfc` | `s:10PrinterSDK0A8SDKErrorV` | `{}` |
| `memberOf` | `s:10PrinterSDK10BLEOptionsV12withResponseSbvp` | `s:10PrinterSDK10BLEOptionsV` | `{}` |
| `memberOf` | `s:10PrinterSDK10BLEOptionsV13maxWriteBytesSivp` | `s:10PrinterSDK10BLEOptionsV` | `{}` |
| `memberOf` | `s:10PrinterSDK10BLEOptionsV17creditFlowControlAA09BLECrediteF0VSgvp` | `s:10PrinterSDK10BLEOptionsV` | `{}` |
| `memberOf` | `s:10PrinterSDK10BLEOptionsV19writeCharacteristic10Foundation4UUIDVvp` | `s:10PrinterSDK10BLEOptionsV` | `{}` |
| `memberOf` | `s:10PrinterSDK10BLEOptionsV7service10Foundation4UUIDVvp` | `s:10PrinterSDK10BLEOptionsV` | `{}` |
| `memberOf` | `s:10PrinterSDK10BLEOptionsV7service19writeCharacteristic12withResponse13maxWriteBytes17creditFlowControlAC10Foundation4UUIDV_AKSbSiAA09BLECreditmN0VSgtKcfc` | `s:10PrinterSDK10BLEOptionsV` | `{}` |
| `memberOf` | `s:10PrinterSDK10BLEScannerC4scan7seconds7service10completionySd_10Foundation4UUIDVSgys6ResultOySayAA010DiscoveredA0VGAA0A8SDKErrorVGYbctF` | `s:10PrinterSDK10BLEScannerC` | `{}` |
| `memberOf` | `s:10PrinterSDK10BLEScannerC4stopyyF` | `s:10PrinterSDK10BLEScannerC` | `{}` |
| `memberOf` | `s:10PrinterSDK10BLEScannerCACycfc` | `s:10PrinterSDK10BLEScannerC` | `{}` |
| `memberOf` | `s:10PrinterSDK10M90ProfileO10bleOptions13maxWriteBytesAA10BLEOptionsVSi_tKFZ` | `s:10PrinterSDK10M90ProfileO` | `{}` |
| `memberOf` | `s:10PrinterSDK11CpclProfileV11xOffsetDotss6UInt32Vvp` | `s:10PrinterSDK11CpclProfileV` | `{}` |
| `memberOf` | `s:10PrinterSDK11CpclProfileV18maxLabelHeightDotss6UInt32Vvp` | `s:10PrinterSDK11CpclProfileV` | `{}` |
| `memberOf` | `s:10PrinterSDK11CpclProfileV3dpi9widthDots014maxLabelHeightG04form07xOffsetG0ACs6UInt32V_A2JSbAJtKcfc` | `s:10PrinterSDK11CpclProfileV` | `{}` |
| `memberOf` | `s:10PrinterSDK11CpclProfileV3dpis6UInt32Vvp` | `s:10PrinterSDK11CpclProfileV` | `{}` |
| `memberOf` | `s:10PrinterSDK11CpclProfileV4formSbvp` | `s:10PrinterSDK11CpclProfileV` | `{}` |
| `memberOf` | `s:10PrinterSDK11CpclProfileV9widthDotss6UInt32Vvp` | `s:10PrinterSDK11CpclProfileV` | `{}` |
| `memberOf` | `s:10PrinterSDK11FinishScopeO8eachCopyyA2CmF` | `s:10PrinterSDK11FinishScopeO` | `{}` |
| `memberOf` | `s:10PrinterSDK11FinishScopeO8endOfJobyA2CmF` | `s:10PrinterSDK11FinishScopeO` | `{}` |
| `memberOf` | `s:10PrinterSDK11FinishScopeO8rawValueACSgs6UInt32V_tcfc` | `s:10PrinterSDK11FinishScopeO` | `{"sourceOrigin":{"displayName":"RawRepresentable.init(rawValue:)","identifier":"s:SY8rawValuexSg03RawB0Qz_tcfc"}}` |
| `memberOf` | `s:10PrinterSDK11PixelFormatO5gray8yA2CmF` | `s:10PrinterSDK11PixelFormatO` | `{}` |
| `memberOf` | `s:10PrinterSDK11PixelFormatO5rgb24yA2CmF` | `s:10PrinterSDK11PixelFormatO` | `{}` |
| `memberOf` | `s:10PrinterSDK11PixelFormatO6rgba32yA2CmF` | `s:10PrinterSDK11PixelFormatO` | `{}` |
| `memberOf` | `s:10PrinterSDK11PixelFormatO8rawValueACSgs6UInt32V_tcfc` | `s:10PrinterSDK11PixelFormatO` | `{"sourceOrigin":{"displayName":"RawRepresentable.init(rawValue:)","identifier":"s:SY8rawValuexSg03RawB0Qz_tcfc"}}` |
| `memberOf` | `s:10PrinterSDK13HardwareStateO7unknownyA2CmF` | `s:10PrinterSDK13HardwareStateO` | `{}` |
| `memberOf` | `s:10PrinterSDK13HardwareStateO8rawValueACSgs6UInt32V_tcfc` | `s:10PrinterSDK13HardwareStateO` | `{"sourceOrigin":{"displayName":"RawRepresentable.init(rawValue:)","identifier":"s:SY8rawValuexSg03RawB0Qz_tcfc"}}` |
| `memberOf` | `s:10PrinterSDK14FeatureSupportV06deviceD0s6UInt32Vvp` | `s:10PrinterSDK14FeatureSupportV` | `{}` |
| `memberOf` | `s:10PrinterSDK14FeatureSupportV11isSupportedSbvp` | `s:10PrinterSDK14FeatureSupportV` | `{}` |
| `memberOf` | `s:10PrinterSDK14FeatureSupportV11knownFieldss6UInt32Vvp` | `s:10PrinterSDK14FeatureSupportV` | `{}` |
| `memberOf` | `s:10PrinterSDK14FeatureSupportV13isImplementedSbvp` | `s:10PrinterSDK14FeatureSupportV` | `{}` |
| `memberOf` | `s:10PrinterSDK14FeatureSupportV14implementations6UInt32Vvp` | `s:10PrinterSDK14FeatureSupportV` | `{}` |
| `memberOf` | `s:10PrinterSDK14FeatureSupportV14requiredFieldss6UInt32Vvp` | `s:10PrinterSDK14FeatureSupportV` | `{}` |
| `memberOf` | `s:10PrinterSDK14FeatureSupportV15supportedFieldss6UInt32Vvp` | `s:10PrinterSDK14FeatureSupportV` | `{}` |
| `memberOf` | `s:10PrinterSDK14FeatureSupportV15supportedValuess6UInt32Vvp` | `s:10PrinterSDK14FeatureSupportV` | `{}` |
| `memberOf` | `s:10PrinterSDK14FeatureSupportV8evidences6UInt32Vvp` | `s:10PrinterSDK14FeatureSupportV` | `{}` |
| `memberOf` | `s:10PrinterSDK14ImageAlgorithmO14floydSteinbergyA2CmF` | `s:10PrinterSDK14ImageAlgorithmO` | `{}` |
| `memberOf` | `s:10PrinterSDK14ImageAlgorithmO8bayer4x4yA2CmF` | `s:10PrinterSDK14ImageAlgorithmO` | `{}` |
| `memberOf` | `s:10PrinterSDK14ImageAlgorithmO8bayer8x8yA2CmF` | `s:10PrinterSDK14ImageAlgorithmO` | `{}` |
| `memberOf` | `s:10PrinterSDK14ImageAlgorithmO8rawValueACSgs6UInt32V_tcfc` | `s:10PrinterSDK14ImageAlgorithmO` | `{"sourceOrigin":{"displayName":"RawRepresentable.init(rawValue:)","identifier":"s:SY8rawValuexSg03RawB0Qz_tcfc"}}` |
| `memberOf` | `s:10PrinterSDK14ImageAlgorithmO9thresholdyA2CmF` | `s:10PrinterSDK14ImageAlgorithmO` | `{}` |
| `memberOf` | `s:10PrinterSDK14PrintOperationC10diagnosticAA15ErrorDiagnosticVyKF` | `s:10PrinterSDK14PrintOperationC` | `{}` |
| `memberOf` | `s:10PrinterSDK14PrintOperationC10onComplete5queue_AA23ObservationSubscriptionCSo012OS_dispatch_G0C_yAA0D6ResultVYbctKF` | `s:10PrinterSDK14PrintOperationC` | `{}` |
| `memberOf` | `s:10PrinterSDK14PrintOperationC12statusResultAA014HardwareStatusF0VyKF` | `s:10PrinterSDK14PrintOperationC` | `{}` |
| `memberOf` | `s:10PrinterSDK14PrintOperationC2ids6UInt64Vvp` | `s:10PrinterSDK14PrintOperationC` | `{}` |
| `memberOf` | `s:10PrinterSDK14PrintOperationC5closeyyF` | `s:10PrinterSDK14PrintOperationC` | `{}` |
| `memberOf` | `s:10PrinterSDK14PrintOperationC6cancelyyKF` | `s:10PrinterSDK14PrintOperationC` | `{}` |
| `memberOf` | `s:10PrinterSDK14PrintOperationC6resultAA0D6ResultVyKF` | `s:10PrinterSDK14PrintOperationC` | `{}` |
| `memberOf` | `s:10PrinterSDK14PrintOperationC7messageSSyKF` | `s:10PrinterSDK14PrintOperationC` | `{}` |
| `memberOf` | `s:10PrinterSDK14PrintOperationC8snapshotAA0D8SnapshotVyKF` | `s:10PrinterSDK14PrintOperationC` | `{}` |
| `memberOf` | `s:10PrinterSDK14ReceiptProfileV3dpi9widthDotsACs6UInt32V_AGtKcfc` | `s:10PrinterSDK14ReceiptProfileV` | `{}` |
| `memberOf` | `s:10PrinterSDK14ReceiptProfileV3dpis6UInt32Vvp` | `s:10PrinterSDK14ReceiptProfileV` | `{}` |
| `memberOf` | `s:10PrinterSDK14ReceiptProfileV9widthDotss6UInt32Vvp` | `s:10PrinterSDK14ReceiptProfileV` | `{}` |
| `memberOf` | `s:10PrinterSDK15ConnectionStateO10connectingyA2CmF` | `s:10PrinterSDK15ConnectionStateO` | `{}` |
| `memberOf` | `s:10PrinterSDK15ConnectionStateO12disconnectedyA2CmF` | `s:10PrinterSDK15ConnectionStateO` | `{}` |
| `memberOf` | `s:10PrinterSDK15ConnectionStateO13disconnectingyA2CmF` | `s:10PrinterSDK15ConnectionStateO` | `{}` |
| `memberOf` | `s:10PrinterSDK15ConnectionStateO8rawValueACSgs6UInt32V_tcfc` | `s:10PrinterSDK15ConnectionStateO` | `{"sourceOrigin":{"displayName":"RawRepresentable.init(rawValue:)","identifier":"s:SY8rawValuexSg03RawB0Qz_tcfc"}}` |
| `memberOf` | `s:10PrinterSDK15ConnectionStateO9connectedyA2CmF` | `s:10PrinterSDK15ConnectionStateO` | `{}` |
| `memberOf` | `s:10PrinterSDK15DeviceWorkStateO4idleyA2CmF` | `s:10PrinterSDK15DeviceWorkStateO` | `{}` |
| `memberOf` | `s:10PrinterSDK15DeviceWorkStateO6queuedyA2CmF` | `s:10PrinterSDK15DeviceWorkStateO` | `{}` |
| `memberOf` | `s:10PrinterSDK15DeviceWorkStateO7sendingyA2CmF` | `s:10PrinterSDK15DeviceWorkStateO` | `{}` |
| `memberOf` | `s:10PrinterSDK15DeviceWorkStateO8rawValueACSgs6UInt32V_tcfc` | `s:10PrinterSDK15DeviceWorkStateO` | `{"sourceOrigin":{"displayName":"RawRepresentable.init(rawValue:)","identifier":"s:SY8rawValuexSg03RawB0Qz_tcfc"}}` |
| `memberOf` | `s:10PrinterSDK15DeviceWorkStateO9preparingyA2CmF` | `s:10PrinterSDK15DeviceWorkStateO` | `{}` |
| `memberOf` | `s:10PrinterSDK15ErrorDiagnosticV10nativeCodes5Int64VSgvp` | `s:10PrinterSDK15ErrorDiagnosticV` | `{}` |
| `memberOf` | `s:10PrinterSDK15ErrorDiagnosticV11operationIDs6UInt64Vvp` | `s:10PrinterSDK15ErrorDiagnosticV` | `{}` |
| `memberOf` | `s:10PrinterSDK15ErrorDiagnosticV14transportEpochs6UInt64Vvp` | `s:10PrinterSDK15ErrorDiagnosticV` | `{}` |
| `memberOf` | `s:10PrinterSDK15ErrorDiagnosticV20connectionGenerations6UInt64Vvp` | `s:10PrinterSDK15ErrorDiagnosticV` | `{}` |
| `memberOf` | `s:10PrinterSDK15ErrorDiagnosticV28sampledMonotonicMillisecondss6UInt64VSgvp` | `s:10PrinterSDK15ErrorDiagnosticV` | `{}` |
| `memberOf` | `s:10PrinterSDK15ErrorDiagnosticV6domainSSvp` | `s:10PrinterSDK15ErrorDiagnosticV` | `{}` |
| `memberOf` | `s:10PrinterSDK15ErrorDiagnosticV6sourceAA0D6SourceOvp` | `s:10PrinterSDK15ErrorDiagnosticV` | `{}` |
| `memberOf` | `s:10PrinterSDK15ErrorDiagnosticV7messageSSvp` | `s:10PrinterSDK15ErrorDiagnosticV` | `{}` |
| `memberOf` | `s:10PrinterSDK15ErrorDiagnosticV8sequences6UInt64Vvp` | `s:10PrinterSDK15ErrorDiagnosticV` | `{}` |
| `memberOf` | `s:10PrinterSDK15ErrorDiagnosticV9errorCodes5Int32Vvp` | `s:10PrinterSDK15ErrorDiagnosticV` | `{}` |
| `memberOf` | `s:10PrinterSDK15LabelJobOptionsV10heightDotss6UInt32Vvp` | `s:10PrinterSDK15LabelJobOptionsV` | `{}` |
| `memberOf` | `s:10PrinterSDK15LabelJobOptionsV11originXDotss5Int32Vvp` | `s:10PrinterSDK15LabelJobOptionsV` | `{}` |
| `memberOf` | `s:10PrinterSDK15LabelJobOptionsV11originYDotss5Int32Vvp` | `s:10PrinterSDK15LabelJobOptionsV` | `{}` |
| `memberOf` | `s:10PrinterSDK15LabelJobOptionsV3dpis6UInt32Vvp` | `s:10PrinterSDK15LabelJobOptionsV` | `{}` |
| `memberOf` | `s:10PrinterSDK15LabelJobOptionsV4formSbvp` | `s:10PrinterSDK15LabelJobOptionsV` | `{}` |
| `memberOf` | `s:10PrinterSDK15LabelJobOptionsV6copiess6UInt32Vvp` | `s:10PrinterSDK15LabelJobOptionsV` | `{}` |
| `memberOf` | `s:10PrinterSDK15LabelJobOptionsV9widthDots06heightG03dpi11originXDots0J5YDots6copies4formACs6UInt32V_A2Ls5Int32VAnLSbtcfc` | `s:10PrinterSDK15LabelJobOptionsV` | `{}` |
| `memberOf` | `s:10PrinterSDK15LabelJobOptionsV9widthDotss6UInt32Vvp` | `s:10PrinterSDK15LabelJobOptionsV` | `{}` |
| `memberOf` | `s:10PrinterSDK15OperationResultV10diagnosticAA15ErrorDiagnosticVvp` | `s:10PrinterSDK15OperationResultV` | `{}` |
| `memberOf` | `s:10PrinterSDK15OperationResultV6statusAA014HardwareStatusD0VSgvp` | `s:10PrinterSDK15OperationResultV` | `{}` |
| `memberOf` | `s:10PrinterSDK15OperationResultV8snapshotAA0C8SnapshotVvp` | `s:10PrinterSDK15OperationResultV` | `{}` |
| `memberOf` | `s:10PrinterSDK16DiagnosticSourceO14userDisconnectyA2CmF` | `s:10PrinterSDK16DiagnosticSourceO` | `{}` |
| `memberOf` | `s:10PrinterSDK16DiagnosticSourceO4noneyA2CmF` | `s:10PrinterSDK16DiagnosticSourceO` | `{}` |
| `memberOf` | `s:10PrinterSDK16DiagnosticSourceO7connectyA2CmF` | `s:10PrinterSDK16DiagnosticSourceO` | `{}` |
| `memberOf` | `s:10PrinterSDK16DiagnosticSourceO8rawValueACSgs6UInt32V_tcfc` | `s:10PrinterSDK16DiagnosticSourceO` | `{"sourceOrigin":{"displayName":"RawRepresentable.init(rawValue:)","identifier":"s:SY8rawValuexSg03RawB0Qz_tcfc"}}` |
| `memberOf` | `s:10PrinterSDK16DiagnosticSourceO9operationyA2CmF` | `s:10PrinterSDK16DiagnosticSourceO` | `{}` |
| `memberOf` | `s:10PrinterSDK16DiagnosticSourceO9transportyA2CmF` | `s:10PrinterSDK16DiagnosticSourceO` | `{}` |
| `memberOf` | `s:10PrinterSDK16MediaRequirementV12verificationAA0C18VerificationPolicyOvp` | `s:10PrinterSDK16MediaRequirementV` | `{}` |
| `memberOf` | `s:10PrinterSDK16MediaRequirementV16widthMicrometerss6UInt32VSgvp` | `s:10PrinterSDK16MediaRequirementV` | `{}` |
| `memberOf` | `s:10PrinterSDK16MediaRequirementV20gapLengthMicrometerss6UInt32VSgvp` | `s:10PrinterSDK16MediaRequirementV` | `{}` |
| `memberOf` | `s:10PrinterSDK16MediaRequirementV21markLengthMicrometerss6UInt32VSgvp` | `s:10PrinterSDK16MediaRequirementV` | `{}` |
| `memberOf` | `s:10PrinterSDK16MediaRequirementV21markOffsetMicrometerss5Int32VSgvp` | `s:10PrinterSDK16MediaRequirementV` | `{}` |
| `memberOf` | `s:10PrinterSDK16MediaRequirementV22labelLengthMicrometerss6UInt32VSgvp` | `s:10PrinterSDK16MediaRequirementV` | `{}` |
| `memberOf` | `s:10PrinterSDK16MediaRequirementV8tracking16widthMicrometers011labelLengthG003gapiG004markiG00k6OffsetG012verificationAcA0C4ModeOSg_s6UInt32VSgA3Ps5Int32VSgAA0C18VerificationPolicyOtcfc` | `s:10PrinterSDK16MediaRequirementV` | `{}` |
| `memberOf` | `s:10PrinterSDK16MediaRequirementV8trackingAA0C4ModeOSgvp` | `s:10PrinterSDK16MediaRequirementV` | `{}` |
| `memberOf` | `s:10PrinterSDK16ResourceSnapshotV11maxRawBytess6UInt64Vvp` | `s:10PrinterSDK16ResourceSnapshotV` | `{}` |
| `memberOf` | `s:10PrinterSDK16ResourceSnapshotV13maxImageBytess6UInt64Vvp` | `s:10PrinterSDK16ResourceSnapshotV` | `{}` |
| `memberOf` | `s:10PrinterSDK16ResourceSnapshotV16maxJobImageBytess6UInt64Vvp` | `s:10PrinterSDK16ResourceSnapshotV` | `{}` |
| `memberOf` | `s:10PrinterSDK16ResourceSnapshotV19pendingPayloadBytess6UInt64Vvp` | `s:10PrinterSDK16ResourceSnapshotV` | `{}` |
| `memberOf` | `s:10PrinterSDK16ResourceSnapshotV20maxPendingOperationss6UInt32Vvp` | `s:10PrinterSDK16ResourceSnapshotV` | `{}` |
| `memberOf` | `s:10PrinterSDK16ResourceSnapshotV21outstandingOperationss6UInt32Vvp` | `s:10PrinterSDK16ResourceSnapshotV` | `{}` |
| `memberOf` | `s:10PrinterSDK16ResourceSnapshotV22maxPendingPayloadBytess6UInt64Vvp` | `s:10PrinterSDK16ResourceSnapshotV` | `{}` |
| `memberOf` | `s:10PrinterSDK16ResourceSnapshotV9maxCopiess6UInt32Vvp` | `s:10PrinterSDK16ResourceSnapshotV` | `{}` |
| `memberOf` | `s:10PrinterSDK16ScalarConstraintV4steps6UInt32Vvp` | `s:10PrinterSDK16ScalarConstraintV` | `{}` |
| `memberOf` | `s:10PrinterSDK16ScalarConstraintV7isKnownSbvp` | `s:10PrinterSDK16ScalarConstraintV` | `{}` |
| `memberOf` | `s:10PrinterSDK16ScalarConstraintV7maximums5Int32Vvp` | `s:10PrinterSDK16ScalarConstraintV` | `{}` |
| `memberOf` | `s:10PrinterSDK16ScalarConstraintV7minimums5Int32Vvp` | `s:10PrinterSDK16ScalarConstraintV` | `{}` |
| `memberOf` | `s:10PrinterSDK16ScalarConstraintV8evidences6UInt32Vvp` | `s:10PrinterSDK16ScalarConstraintV` | `{}` |
| `memberOf` | `s:10PrinterSDK16ScalarConstraintV9parameterAA0aD0Ovp` | `s:10PrinterSDK16ScalarConstraintV` | `{}` |
| `memberOf` | `s:10PrinterSDK17OperationSnapshotV10bytesTotals6UInt64VSgvp` | `s:10PrinterSDK17OperationSnapshotV` | `{}` |
| `memberOf` | `s:10PrinterSDK17OperationSnapshotV10completions6UInt32Vvp` | `s:10PrinterSDK17OperationSnapshotV` | `{}` |
| `memberOf` | `s:10PrinterSDK17OperationSnapshotV10isTerminalSbvp` | `s:10PrinterSDK17OperationSnapshotV` | `{}` |
| `memberOf` | `s:10PrinterSDK17OperationSnapshotV10mediaBasiss6UInt32Vvp` | `s:10PrinterSDK17OperationSnapshotV` | `{}` |
| `memberOf` | `s:10PrinterSDK17OperationSnapshotV14bytesSentExactSbvp` | `s:10PrinterSDK17OperationSnapshotV` | `{}` |
| `memberOf` | `s:10PrinterSDK17OperationSnapshotV19sideEffectsPossibleSbvp` | `s:10PrinterSDK17OperationSnapshotV` | `{}` |
| `memberOf` | `s:10PrinterSDK17OperationSnapshotV2ids6UInt64Vvp` | `s:10PrinterSDK17OperationSnapshotV` | `{}` |
| `memberOf` | `s:10PrinterSDK17OperationSnapshotV4kinds6UInt32Vvp` | `s:10PrinterSDK17OperationSnapshotV` | `{}` |
| `memberOf` | `s:10PrinterSDK17OperationSnapshotV5states6UInt32Vvp` | `s:10PrinterSDK17OperationSnapshotV` | `{}` |
| `memberOf` | `s:10PrinterSDK17OperationSnapshotV9bytesSents6UInt64Vvp` | `s:10PrinterSDK17OperationSnapshotV` | `{}` |
| `memberOf` | `s:10PrinterSDK17OperationSnapshotV9errorCodes5Int32Vvp` | `s:10PrinterSDK17OperationSnapshotV` | `{}` |
| `memberOf` | `s:10PrinterSDK17ReceiptJobOptionsV14marginLeftDotss6UInt32Vvp` | `s:10PrinterSDK17ReceiptJobOptionsV` | `{}` |
| `memberOf` | `s:10PrinterSDK17ReceiptJobOptionsV15marginRightDotss6UInt32Vvp` | `s:10PrinterSDK17ReceiptJobOptionsV` | `{}` |
| `memberOf` | `s:10PrinterSDK17ReceiptJobOptionsV18maxTotalLengthDotss6UInt32Vvp` | `s:10PrinterSDK17ReceiptJobOptionsV` | `{}` |
| `memberOf` | `s:10PrinterSDK17ReceiptJobOptionsV3dpis6UInt32Vvp` | `s:10PrinterSDK17ReceiptJobOptionsV` | `{}` |
| `memberOf` | `s:10PrinterSDK17ReceiptJobOptionsV6copiess6UInt32Vvp` | `s:10PrinterSDK17ReceiptJobOptionsV` | `{}` |
| `memberOf` | `s:10PrinterSDK17ReceiptJobOptionsV9widthDots014maxTotalLengthG03dpi6copies010marginLeftG00m5RightG0ACs6UInt32V_A5Ktcfc` | `s:10PrinterSDK17ReceiptJobOptionsV` | `{}` |
| `memberOf` | `s:10PrinterSDK17ReceiptJobOptionsV9widthDotss6UInt32Vvp` | `s:10PrinterSDK17ReceiptJobOptionsV` | `{}` |
| `memberOf` | `s:10PrinterSDK19DeviceStateSnapshotV010connectionD0AA010ConnectionD0Ovp` | `s:10PrinterSDK19DeviceStateSnapshotV` | `{}` |
| `memberOf` | `s:10PrinterSDK19DeviceStateSnapshotV04workD0AA0c4WorkD0Ovp` | `s:10PrinterSDK19DeviceStateSnapshotV` | `{}` |
| `memberOf` | `s:10PrinterSDK19DeviceStateSnapshotV08hardwareD0AA08HardwareD0Ovp` | `s:10PrinterSDK19DeviceStateSnapshotV` | `{}` |
| `memberOf` | `s:10PrinterSDK19DeviceStateSnapshotV11queuedCounts6UInt32Vvp` | `s:10PrinterSDK19DeviceStateSnapshotV` | `{}` |
| `memberOf` | `s:10PrinterSDK19DeviceStateSnapshotV15activeOperationAA0c6ActiveG0VSgvp` | `s:10PrinterSDK19DeviceStateSnapshotV` | `{}` |
| `memberOf` | `s:10PrinterSDK19DeviceStateSnapshotV20connectionGenerations6UInt64Vvp` | `s:10PrinterSDK19DeviceStateSnapshotV` | `{}` |
| `memberOf` | `s:10PrinterSDK19DeviceStateSnapshotV23hardwareKnownConditionss6UInt32Vvp` | `s:10PrinterSDK19DeviceStateSnapshotV` | `{}` |
| `memberOf` | `s:10PrinterSDK19DeviceStateSnapshotV24hardwareActiveConditionss6UInt32Vvp` | `s:10PrinterSDK19DeviceStateSnapshotV` | `{}` |
| `memberOf` | `s:10PrinterSDK19DeviceStateSnapshotV36hardwareSampledMonotonicMillisecondss6UInt64VSgvp` | `s:10PrinterSDK19DeviceStateSnapshotV` | `{}` |
| `memberOf` | `s:10PrinterSDK20BLECreditFlowControlV6dataFC10Foundation4UUIDVvp` | `s:10PrinterSDK20BLECreditFlowControlV` | `{}` |
| `memberOf` | `s:10PrinterSDK20BLECreditFlowControlV7dataOut0F2FCAC10Foundation4UUIDV_AHtcfc` | `s:10PrinterSDK20BLECreditFlowControlV` | `{}` |
| `memberOf` | `s:10PrinterSDK20BLECreditFlowControlV7dataOut10Foundation4UUIDVvp` | `s:10PrinterSDK20BLECreditFlowControlV` | `{}` |
| `memberOf` | `s:10PrinterSDK20HardwareStatusResultV15knownConditionss6UInt32Vvp` | `s:10PrinterSDK20HardwareStatusResultV` | `{}` |
| `memberOf` | `s:10PrinterSDK20HardwareStatusResultV16activeConditionss6UInt32Vvp` | `s:10PrinterSDK20HardwareStatusResultV` | `{}` |
| `memberOf` | `s:10PrinterSDK20HardwareStatusResultV20connectionGenerations6UInt64Vvp` | `s:10PrinterSDK20HardwareStatusResultV` | `{}` |
| `memberOf` | `s:10PrinterSDK20HardwareStatusResultV28sampledMonotonicMillisecondss6UInt64Vvp` | `s:10PrinterSDK20HardwareStatusResultV` | `{}` |
| `memberOf` | `s:10PrinterSDK20HardwareStatusResultV8evidences6UInt32Vvp` | `s:10PrinterSDK20HardwareStatusResultV` | `{}` |
| `memberOf` | `s:10PrinterSDK20ReceiptFinishOptionsV17trailingBlankDots3cut5scopeACs6UInt32V_AA7CutModeOAA0D5ScopeOtcfc` | `s:10PrinterSDK20ReceiptFinishOptionsV` | `{}` |
| `memberOf` | `s:10PrinterSDK20ReceiptFinishOptionsV17trailingBlankDotss6UInt32Vvp` | `s:10PrinterSDK20ReceiptFinishOptionsV` | `{}` |
| `memberOf` | `s:10PrinterSDK20ReceiptFinishOptionsV3cutAA7CutModeOvp` | `s:10PrinterSDK20ReceiptFinishOptionsV` | `{}` |
| `memberOf` | `s:10PrinterSDK20ReceiptFinishOptionsV5scopeAA0D5ScopeOvp` | `s:10PrinterSDK20ReceiptFinishOptionsV` | `{}` |
| `memberOf` | `s:10PrinterSDK21DeviceActiveOperationV2ids6UInt64Vvp` | `s:10PrinterSDK21DeviceActiveOperationV` | `{}` |
| `memberOf` | `s:10PrinterSDK21DeviceActiveOperationV4kinds6UInt32Vvp` | `s:10PrinterSDK21DeviceActiveOperationV` | `{}` |
| `memberOf` | `s:10PrinterSDK21DeviceActiveOperationV5states6UInt32Vvp` | `s:10PrinterSDK21DeviceActiveOperationV` | `{}` |
| `memberOf` | `s:10PrinterSDK23MediaVerificationPolicyO13allowLastSentyA2CmF` | `s:10PrinterSDK23MediaVerificationPolicyO` | `{}` |
| `memberOf` | `s:10PrinterSDK23MediaVerificationPolicyO15requireReadbackyA2CmF` | `s:10PrinterSDK23MediaVerificationPolicyO` | `{}` |
| `memberOf` | `s:10PrinterSDK23MediaVerificationPolicyO8rawValueACSgs6UInt32V_tcfc` | `s:10PrinterSDK23MediaVerificationPolicyO` | `{"sourceOrigin":{"displayName":"RawRepresentable.init(rawValue:)","identifier":"s:SY8rawValuexSg03RawB0Qz_tcfc"}}` |
| `memberOf` | `s:10PrinterSDK23ObservationSubscriptionC5closeyyF` | `s:10PrinterSDK23ObservationSubscriptionC` | `{}` |
| `memberOf` | `s:10PrinterSDK23ObservationSubscriptionC6cancelyyF` | `s:10PrinterSDK23ObservationSubscriptionC` | `{}` |
| `memberOf` | `s:10PrinterSDK24StatusQueryConfigurationV15printerProtocol24queryTimeoutMillisecondsAcA0aG0O_s6UInt32VtKcfc` | `s:10PrinterSDK24StatusQueryConfigurationV` | `{}` |
| `memberOf` | `s:10PrinterSDK24StatusQueryConfigurationV15printerProtocolAA0aG0Ovp` | `s:10PrinterSDK24StatusQueryConfigurationV` | `{}` |
| `memberOf` | `s:10PrinterSDK24StatusQueryConfigurationV24queryTimeoutMillisecondss6UInt32Vvp` | `s:10PrinterSDK24StatusQueryConfigurationV` | `{}` |
| `memberOf` | `s:10PrinterSDK7CutModeO4fullyA2CmF` | `s:10PrinterSDK7CutModeO` | `{}` |
| `memberOf` | `s:10PrinterSDK7CutModeO4noneyA2CmF` | `s:10PrinterSDK7CutModeO` | `{}` |
| `memberOf` | `s:10PrinterSDK7CutModeO7partialyA2CmF` | `s:10PrinterSDK7CutModeO` | `{}` |
| `memberOf` | `s:10PrinterSDK7CutModeO8rawValueACSgs6UInt32V_tcfc` | `s:10PrinterSDK7CutModeO` | `{"sourceOrigin":{"displayName":"RawRepresentable.init(rawValue:)","identifier":"s:SY8rawValuexSg03RawB0Qz_tcfc"}}` |
| `memberOf` | `s:10PrinterSDK7SDKInfoV10abiVersions6UInt32Vvp` | `s:10PrinterSDK7SDKInfoV` | `{}` |
| `memberOf` | `s:10PrinterSDK7SDKInfoV11abiRevisions6UInt32Vvp` | `s:10PrinterSDK7SDKInfoV` | `{}` |
| `memberOf` | `s:10PrinterSDK7SDKInfoV13nativeVersionSSvp` | `s:10PrinterSDK7SDKInfoV` | `{}` |
| `memberOf` | `s:10PrinterSDK7SDKInfoV19distributionVersionSSvp` | `s:10PrinterSDK7SDKInfoV` | `{}` |
| `memberOf` | `s:10PrinterSDK7SDKInfoV7buildIDSSSgvp` | `s:10PrinterSDK7SDKInfoV` | `{}` |
| `memberOf` | `s:10PrinterSDK8PrintJobC7BuilderC` | `s:10PrinterSDK8PrintJobC` | `{}` |
| `memberOf` | `s:10PrinterSDK8PrintJobC7BuilderC11appendBlank4dotsys6UInt32V_tKF` | `s:10PrinterSDK8PrintJobC7BuilderC` | `{}` |
| `memberOf` | `s:10PrinterSDK8PrintJobC7BuilderC11appendImageyyAA0aG0CKF` | `s:10PrinterSDK8PrintJobC7BuilderC` | `{}` |
| `memberOf` | `s:10PrinterSDK8PrintJobC7BuilderC16setReceiptFinishyyAA0gH7OptionsVKF` | `s:10PrinterSDK8PrintJobC7BuilderC` | `{}` |
| `memberOf` | `s:10PrinterSDK8PrintJobC7BuilderC19setMediaRequirementyyAA0gH0VSgF` | `s:10PrinterSDK8PrintJobC7BuilderC` | `{}` |
| `memberOf` | `s:10PrinterSDK8PrintJobC7BuilderC5buildACyKF` | `s:10PrinterSDK8PrintJobC7BuilderC` | `{}` |
| `memberOf` | `s:10PrinterSDK8PrintJobC7BuilderC5labelAeA05LabelD7OptionsV_tcfc` | `s:10PrinterSDK8PrintJobC7BuilderC` | `{}` |
| `memberOf` | `s:10PrinterSDK8PrintJobC7BuilderC7receiptAeA07ReceiptD7OptionsV_tcfc` | `s:10PrinterSDK8PrintJobC7BuilderC` | `{}` |
| `memberOf` | `s:10PrinterSDK8PrintJobC7BuilderC8addImage_1x1yyAA0aG0C_s5Int32VALtKF` | `s:10PrinterSDK8PrintJobC7BuilderC` | `{}` |
| `memberOf` | `s:10PrinterSDK9MediaModeO10continuousyA2CmF` | `s:10PrinterSDK9MediaModeO` | `{}` |
| `memberOf` | `s:10PrinterSDK9MediaModeO3gapyA2CmF` | `s:10PrinterSDK9MediaModeO` | `{}` |
| `memberOf` | `s:10PrinterSDK9MediaModeO8rawValueACSgs6UInt32V_tcfc` | `s:10PrinterSDK9MediaModeO` | `{"sourceOrigin":{"displayName":"RawRepresentable.init(rawValue:)","identifier":"s:SY8rawValuexSg03RawB0Qz_tcfc"}}` |
| `memberOf` | `s:10PrinterSDK9MediaModeO9blackMarkyA2CmF` | `s:10PrinterSDK9MediaModeO` | `{}` |
| `memberOf` | `s:10PrinterSDKAAO4infoAA7SDKInfoVyKFZ` | `s:10PrinterSDKAAO` | `{}` |
| `memberOf` | `s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK0A10ConstraintO` | `s:10PrinterSDK0A10ConstraintO` | `{"sourceOrigin":{"displayName":"Equatable.!=(_:_:)","identifier":"s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ"}}` |
| `memberOf` | `s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK0A7FeatureO` | `s:10PrinterSDK0A7FeatureO` | `{"sourceOrigin":{"displayName":"Equatable.!=(_:_:)","identifier":"s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ"}}` |
| `memberOf` | `s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK0A8GeometryV` | `s:10PrinterSDK0A8GeometryV` | `{"sourceOrigin":{"displayName":"Equatable.!=(_:_:)","identifier":"s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ"}}` |
| `memberOf` | `s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK0A8ProtocolO` | `s:10PrinterSDK0A8ProtocolO` | `{"sourceOrigin":{"displayName":"Equatable.!=(_:_:)","identifier":"s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ"}}` |
| `memberOf` | `s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK0A8SDKErrorV` | `s:10PrinterSDK0A8SDKErrorV` | `{"sourceOrigin":{"displayName":"Equatable.!=(_:_:)","identifier":"s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ"}}` |
| `memberOf` | `s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK11FinishScopeO` | `s:10PrinterSDK11FinishScopeO` | `{"sourceOrigin":{"displayName":"Equatable.!=(_:_:)","identifier":"s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ"}}` |
| `memberOf` | `s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK11PixelFormatO` | `s:10PrinterSDK11PixelFormatO` | `{"sourceOrigin":{"displayName":"Equatable.!=(_:_:)","identifier":"s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ"}}` |
| `memberOf` | `s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK13HardwareStateO` | `s:10PrinterSDK13HardwareStateO` | `{"sourceOrigin":{"displayName":"Equatable.!=(_:_:)","identifier":"s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ"}}` |
| `memberOf` | `s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK14FeatureSupportV` | `s:10PrinterSDK14FeatureSupportV` | `{"sourceOrigin":{"displayName":"Equatable.!=(_:_:)","identifier":"s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ"}}` |
| `memberOf` | `s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK14ImageAlgorithmO` | `s:10PrinterSDK14ImageAlgorithmO` | `{"sourceOrigin":{"displayName":"Equatable.!=(_:_:)","identifier":"s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ"}}` |
| `memberOf` | `s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK15ConnectionStateO` | `s:10PrinterSDK15ConnectionStateO` | `{"sourceOrigin":{"displayName":"Equatable.!=(_:_:)","identifier":"s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ"}}` |
| `memberOf` | `s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK15DeviceWorkStateO` | `s:10PrinterSDK15DeviceWorkStateO` | `{"sourceOrigin":{"displayName":"Equatable.!=(_:_:)","identifier":"s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ"}}` |
| `memberOf` | `s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK15ErrorDiagnosticV` | `s:10PrinterSDK15ErrorDiagnosticV` | `{"sourceOrigin":{"displayName":"Equatable.!=(_:_:)","identifier":"s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ"}}` |
| `memberOf` | `s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK15OperationResultV` | `s:10PrinterSDK15OperationResultV` | `{"sourceOrigin":{"displayName":"Equatable.!=(_:_:)","identifier":"s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ"}}` |
| `memberOf` | `s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK16DiagnosticSourceO` | `s:10PrinterSDK16DiagnosticSourceO` | `{"sourceOrigin":{"displayName":"Equatable.!=(_:_:)","identifier":"s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ"}}` |
| `memberOf` | `s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK16ResourceSnapshotV` | `s:10PrinterSDK16ResourceSnapshotV` | `{"sourceOrigin":{"displayName":"Equatable.!=(_:_:)","identifier":"s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ"}}` |
| `memberOf` | `s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK16ScalarConstraintV` | `s:10PrinterSDK16ScalarConstraintV` | `{"sourceOrigin":{"displayName":"Equatable.!=(_:_:)","identifier":"s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ"}}` |
| `memberOf` | `s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK17OperationSnapshotV` | `s:10PrinterSDK17OperationSnapshotV` | `{"sourceOrigin":{"displayName":"Equatable.!=(_:_:)","identifier":"s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ"}}` |
| `memberOf` | `s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK19DeviceStateSnapshotV` | `s:10PrinterSDK19DeviceStateSnapshotV` | `{"sourceOrigin":{"displayName":"Equatable.!=(_:_:)","identifier":"s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ"}}` |
| `memberOf` | `s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK20HardwareStatusResultV` | `s:10PrinterSDK20HardwareStatusResultV` | `{"sourceOrigin":{"displayName":"Equatable.!=(_:_:)","identifier":"s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ"}}` |
| `memberOf` | `s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK21DeviceActiveOperationV` | `s:10PrinterSDK21DeviceActiveOperationV` | `{"sourceOrigin":{"displayName":"Equatable.!=(_:_:)","identifier":"s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ"}}` |
| `memberOf` | `s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK23MediaVerificationPolicyO` | `s:10PrinterSDK23MediaVerificationPolicyO` | `{"sourceOrigin":{"displayName":"Equatable.!=(_:_:)","identifier":"s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ"}}` |
| `memberOf` | `s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK24StatusQueryConfigurationV` | `s:10PrinterSDK24StatusQueryConfigurationV` | `{"sourceOrigin":{"displayName":"Equatable.!=(_:_:)","identifier":"s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ"}}` |
| `memberOf` | `s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK7CutModeO` | `s:10PrinterSDK7CutModeO` | `{"sourceOrigin":{"displayName":"Equatable.!=(_:_:)","identifier":"s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ"}}` |
| `memberOf` | `s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK7SDKInfoV` | `s:10PrinterSDK7SDKInfoV` | `{"sourceOrigin":{"displayName":"Equatable.!=(_:_:)","identifier":"s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ"}}` |
| `memberOf` | `s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK9MediaModeO` | `s:10PrinterSDK9MediaModeO` | `{"sourceOrigin":{"displayName":"Equatable.!=(_:_:)","identifier":"s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ"}}` |
| `memberOf` | `s:SYsSHRzSH8RawValueSYRpzrlE04hashB0Sivp::SYNTHESIZED::s:10PrinterSDK0A10ConstraintO` | `s:10PrinterSDK0A10ConstraintO` | `{"sourceOrigin":{"displayName":"RawRepresentable.hashValue","identifier":"s:SYsSHRzSH8RawValueSYRpzrlE04hashB0Sivp"}}` |
| `memberOf` | `s:SYsSHRzSH8RawValueSYRpzrlE04hashB0Sivp::SYNTHESIZED::s:10PrinterSDK0A7FeatureO` | `s:10PrinterSDK0A7FeatureO` | `{"sourceOrigin":{"displayName":"RawRepresentable.hashValue","identifier":"s:SYsSHRzSH8RawValueSYRpzrlE04hashB0Sivp"}}` |
| `memberOf` | `s:SYsSHRzSH8RawValueSYRpzrlE04hashB0Sivp::SYNTHESIZED::s:10PrinterSDK0A8ProtocolO` | `s:10PrinterSDK0A8ProtocolO` | `{"sourceOrigin":{"displayName":"RawRepresentable.hashValue","identifier":"s:SYsSHRzSH8RawValueSYRpzrlE04hashB0Sivp"}}` |
| `memberOf` | `s:SYsSHRzSH8RawValueSYRpzrlE04hashB0Sivp::SYNTHESIZED::s:10PrinterSDK11FinishScopeO` | `s:10PrinterSDK11FinishScopeO` | `{"sourceOrigin":{"displayName":"RawRepresentable.hashValue","identifier":"s:SYsSHRzSH8RawValueSYRpzrlE04hashB0Sivp"}}` |
| `memberOf` | `s:SYsSHRzSH8RawValueSYRpzrlE04hashB0Sivp::SYNTHESIZED::s:10PrinterSDK11PixelFormatO` | `s:10PrinterSDK11PixelFormatO` | `{"sourceOrigin":{"displayName":"RawRepresentable.hashValue","identifier":"s:SYsSHRzSH8RawValueSYRpzrlE04hashB0Sivp"}}` |
| `memberOf` | `s:SYsSHRzSH8RawValueSYRpzrlE04hashB0Sivp::SYNTHESIZED::s:10PrinterSDK13HardwareStateO` | `s:10PrinterSDK13HardwareStateO` | `{"sourceOrigin":{"displayName":"RawRepresentable.hashValue","identifier":"s:SYsSHRzSH8RawValueSYRpzrlE04hashB0Sivp"}}` |
| `memberOf` | `s:SYsSHRzSH8RawValueSYRpzrlE04hashB0Sivp::SYNTHESIZED::s:10PrinterSDK14ImageAlgorithmO` | `s:10PrinterSDK14ImageAlgorithmO` | `{"sourceOrigin":{"displayName":"RawRepresentable.hashValue","identifier":"s:SYsSHRzSH8RawValueSYRpzrlE04hashB0Sivp"}}` |
| `memberOf` | `s:SYsSHRzSH8RawValueSYRpzrlE04hashB0Sivp::SYNTHESIZED::s:10PrinterSDK15ConnectionStateO` | `s:10PrinterSDK15ConnectionStateO` | `{"sourceOrigin":{"displayName":"RawRepresentable.hashValue","identifier":"s:SYsSHRzSH8RawValueSYRpzrlE04hashB0Sivp"}}` |
| `memberOf` | `s:SYsSHRzSH8RawValueSYRpzrlE04hashB0Sivp::SYNTHESIZED::s:10PrinterSDK15DeviceWorkStateO` | `s:10PrinterSDK15DeviceWorkStateO` | `{"sourceOrigin":{"displayName":"RawRepresentable.hashValue","identifier":"s:SYsSHRzSH8RawValueSYRpzrlE04hashB0Sivp"}}` |
| `memberOf` | `s:SYsSHRzSH8RawValueSYRpzrlE04hashB0Sivp::SYNTHESIZED::s:10PrinterSDK16DiagnosticSourceO` | `s:10PrinterSDK16DiagnosticSourceO` | `{"sourceOrigin":{"displayName":"RawRepresentable.hashValue","identifier":"s:SYsSHRzSH8RawValueSYRpzrlE04hashB0Sivp"}}` |
| `memberOf` | `s:SYsSHRzSH8RawValueSYRpzrlE04hashB0Sivp::SYNTHESIZED::s:10PrinterSDK23MediaVerificationPolicyO` | `s:10PrinterSDK23MediaVerificationPolicyO` | `{"sourceOrigin":{"displayName":"RawRepresentable.hashValue","identifier":"s:SYsSHRzSH8RawValueSYRpzrlE04hashB0Sivp"}}` |
| `memberOf` | `s:SYsSHRzSH8RawValueSYRpzrlE04hashB0Sivp::SYNTHESIZED::s:10PrinterSDK7CutModeO` | `s:10PrinterSDK7CutModeO` | `{"sourceOrigin":{"displayName":"RawRepresentable.hashValue","identifier":"s:SYsSHRzSH8RawValueSYRpzrlE04hashB0Sivp"}}` |
| `memberOf` | `s:SYsSHRzSH8RawValueSYRpzrlE04hashB0Sivp::SYNTHESIZED::s:10PrinterSDK9MediaModeO` | `s:10PrinterSDK9MediaModeO` | `{"sourceOrigin":{"displayName":"RawRepresentable.hashValue","identifier":"s:SYsSHRzSH8RawValueSYRpzrlE04hashB0Sivp"}}` |
| `memberOf` | `s:SYsSHRzSH8RawValueSYRpzrlE4hash4intoys6HasherVz_tF::SYNTHESIZED::s:10PrinterSDK0A10ConstraintO` | `s:10PrinterSDK0A10ConstraintO` | `{"sourceOrigin":{"displayName":"RawRepresentable.hash(into:)","identifier":"s:SYsSHRzSH8RawValueSYRpzrlE4hash4intoys6HasherVz_tF"}}` |
| `memberOf` | `s:SYsSHRzSH8RawValueSYRpzrlE4hash4intoys6HasherVz_tF::SYNTHESIZED::s:10PrinterSDK0A7FeatureO` | `s:10PrinterSDK0A7FeatureO` | `{"sourceOrigin":{"displayName":"RawRepresentable.hash(into:)","identifier":"s:SYsSHRzSH8RawValueSYRpzrlE4hash4intoys6HasherVz_tF"}}` |
| `memberOf` | `s:SYsSHRzSH8RawValueSYRpzrlE4hash4intoys6HasherVz_tF::SYNTHESIZED::s:10PrinterSDK0A8ProtocolO` | `s:10PrinterSDK0A8ProtocolO` | `{"sourceOrigin":{"displayName":"RawRepresentable.hash(into:)","identifier":"s:SYsSHRzSH8RawValueSYRpzrlE4hash4intoys6HasherVz_tF"}}` |
| `memberOf` | `s:SYsSHRzSH8RawValueSYRpzrlE4hash4intoys6HasherVz_tF::SYNTHESIZED::s:10PrinterSDK11FinishScopeO` | `s:10PrinterSDK11FinishScopeO` | `{"sourceOrigin":{"displayName":"RawRepresentable.hash(into:)","identifier":"s:SYsSHRzSH8RawValueSYRpzrlE4hash4intoys6HasherVz_tF"}}` |
| `memberOf` | `s:SYsSHRzSH8RawValueSYRpzrlE4hash4intoys6HasherVz_tF::SYNTHESIZED::s:10PrinterSDK11PixelFormatO` | `s:10PrinterSDK11PixelFormatO` | `{"sourceOrigin":{"displayName":"RawRepresentable.hash(into:)","identifier":"s:SYsSHRzSH8RawValueSYRpzrlE4hash4intoys6HasherVz_tF"}}` |
| `memberOf` | `s:SYsSHRzSH8RawValueSYRpzrlE4hash4intoys6HasherVz_tF::SYNTHESIZED::s:10PrinterSDK13HardwareStateO` | `s:10PrinterSDK13HardwareStateO` | `{"sourceOrigin":{"displayName":"RawRepresentable.hash(into:)","identifier":"s:SYsSHRzSH8RawValueSYRpzrlE4hash4intoys6HasherVz_tF"}}` |
| `memberOf` | `s:SYsSHRzSH8RawValueSYRpzrlE4hash4intoys6HasherVz_tF::SYNTHESIZED::s:10PrinterSDK14ImageAlgorithmO` | `s:10PrinterSDK14ImageAlgorithmO` | `{"sourceOrigin":{"displayName":"RawRepresentable.hash(into:)","identifier":"s:SYsSHRzSH8RawValueSYRpzrlE4hash4intoys6HasherVz_tF"}}` |
| `memberOf` | `s:SYsSHRzSH8RawValueSYRpzrlE4hash4intoys6HasherVz_tF::SYNTHESIZED::s:10PrinterSDK15ConnectionStateO` | `s:10PrinterSDK15ConnectionStateO` | `{"sourceOrigin":{"displayName":"RawRepresentable.hash(into:)","identifier":"s:SYsSHRzSH8RawValueSYRpzrlE4hash4intoys6HasherVz_tF"}}` |
| `memberOf` | `s:SYsSHRzSH8RawValueSYRpzrlE4hash4intoys6HasherVz_tF::SYNTHESIZED::s:10PrinterSDK15DeviceWorkStateO` | `s:10PrinterSDK15DeviceWorkStateO` | `{"sourceOrigin":{"displayName":"RawRepresentable.hash(into:)","identifier":"s:SYsSHRzSH8RawValueSYRpzrlE4hash4intoys6HasherVz_tF"}}` |
| `memberOf` | `s:SYsSHRzSH8RawValueSYRpzrlE4hash4intoys6HasherVz_tF::SYNTHESIZED::s:10PrinterSDK16DiagnosticSourceO` | `s:10PrinterSDK16DiagnosticSourceO` | `{"sourceOrigin":{"displayName":"RawRepresentable.hash(into:)","identifier":"s:SYsSHRzSH8RawValueSYRpzrlE4hash4intoys6HasherVz_tF"}}` |
| `memberOf` | `s:SYsSHRzSH8RawValueSYRpzrlE4hash4intoys6HasherVz_tF::SYNTHESIZED::s:10PrinterSDK23MediaVerificationPolicyO` | `s:10PrinterSDK23MediaVerificationPolicyO` | `{"sourceOrigin":{"displayName":"RawRepresentable.hash(into:)","identifier":"s:SYsSHRzSH8RawValueSYRpzrlE4hash4intoys6HasherVz_tF"}}` |
| `memberOf` | `s:SYsSHRzSH8RawValueSYRpzrlE4hash4intoys6HasherVz_tF::SYNTHESIZED::s:10PrinterSDK7CutModeO` | `s:10PrinterSDK7CutModeO` | `{"sourceOrigin":{"displayName":"RawRepresentable.hash(into:)","identifier":"s:SYsSHRzSH8RawValueSYRpzrlE4hash4intoys6HasherVz_tF"}}` |
| `memberOf` | `s:SYsSHRzSH8RawValueSYRpzrlE4hash4intoys6HasherVz_tF::SYNTHESIZED::s:10PrinterSDK9MediaModeO` | `s:10PrinterSDK9MediaModeO` | `{"sourceOrigin":{"displayName":"RawRepresentable.hash(into:)","identifier":"s:SYsSHRzSH8RawValueSYRpzrlE4hash4intoys6HasherVz_tF"}}` |
| `memberOf` | `s:s5ErrorP10FoundationE20localizedDescriptionSSvp::SYNTHESIZED::s:10PrinterSDK0A8SDKErrorV` | `s:10PrinterSDK0A8SDKErrorV` | `{"sourceOrigin":{"displayName":"Error.localizedDescription","identifier":"s:s5ErrorP10FoundationE20localizedDescriptionSSvp"}}` |
