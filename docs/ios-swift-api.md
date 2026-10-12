# iOS Swift API 参考

适用分发版本 **0.2.0-preview.3**；原生 SDK **0.2.0**；C ABI **2/r10**。

本页逐项覆盖审定的编译器公共 API 基线，共 **124** 个文档条目。
声明、JVM descriptor、常量、枚举和编译器关系均来自基线，说明按稳定文档 ID 维护。
机器可读的完整基线与说明见 [api-reference.json](api-reference.json)；运行
`python3 tools/check-api-reference.py` 可检查覆盖范围、审定 SHA-256 和本页字节一致性。

基线 SHA-256：`62ccd5c542c02285429d8942b7eb69a68555180ddb3b9d335780e30009f2fd65`

运行约束与错误处理见 [API 与生命周期](api-lifecycle.md) 和 [错误码与排障](errors-troubleshooting.md)。
本页覆盖发布的公共接口；编译器合成接口也明确标注。硬件验收仍暂缓，`hardware_verified=false`。

## 类型导航

- [BLECreditFlowControl](#api-cf29222de3a9439ea9d9)
- [BLEOptions](#api-5407994e1f7a0684ecd6)
- [BLEScanner](#api-b3a1ce59859e4cf66609)
- [ConnectionState](#api-7b987515d7ad703321ec)
- [CpclProfile](#api-d94c01c25a5ac00d3af5)
- [CutMode](#api-ca87677d19e3423fb23c)
- [DiscoveredPrinter](#api-e7eba043862754865338)
- [FeatureSupport](#api-63244971bb8665f0438c)
- [FinishScope](#api-8da32930540a97e6bfd9)
- [M90Profile](#api-c09da0bc38ed43e7aef2)
- [MediaMode](#api-e9f6609ce460a51cc89f)
- [MobilePrinter](#api-ac14c812144bc8d503ff)
- [OperationSnapshot](#api-57bb815c8787c72b8ed3)
- [PrintOperation](#api-fcb644f7b0eb6c8a6489)
- [PrinterFeature](#api-67c88d53ad46bc6679a5)
- [PrinterSDKError](#api-7eade5ddc7d13cb74d9a)
- [ReceiptProfile](#api-8a4489e38f48eaabb599)

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

扫描通过异步完成回调返回，不阻塞调用线程；一次仅允许一个扫描。应用持有扫描器直到完成，可用 `stop` 取消；完成后可再次扫描。应用负责用途说明、权限与前后台策略。发现不连接设备，不判断协议/型号，也不验证打印机硬件；没有原生外设句柄暴露。

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

<a id="api-a9c50a674be4ea741a5f"></a>

### BLEScanner.scan(seconds:service:completion:)

异步扫描一个有限窗口并在主队列交付一次结果。

```swift
func scan(seconds: TimeInterval = 10, service: UUID? = nil, completion: @escaping (Result<[DiscoveredPrinter], PrinterSDKError>) -> Void)
```

`seconds` 默认 `10`，单位秒，必须有限且在 `(0, 60]` 内，窗口包含蓝牙初始化等待；无效值通过主队列回调 `.failure(PrinterSDKError(1001))`，方法不抛出。`service` 默认 `nil`，表示不按单一服务过滤；传 UUID 时要求广播匹配，可能排除没有在广播列出服务的设备。`completion` 无默认值；每次请求只调用一次，且始终派发到主队列，可在回调中再次启动扫描。已有活动扫描时新请求以 1002 失败，不中断原扫描。

正常窗口结束返回 `.success`，可以是空数组；按 UUID 合并后保留首次发现顺序，名称、服务与 RSSI 更新。停止以 1005 失败；蓝牙关闭、权限不足或扫描期间状态失效以 1002 失败，不支持蓝牙以 1004 失败；蓝牙初始化未能在窗口内就绪以 2001 失败。失败和取消不返回部分结果。SDK 不自动开启蓝牙、配对、重试、连接或选择设备。

文档 ID：`apple:symbol:s:10PrinterSDK10BLEScannerC4scan7seconds7service10completionySd_10Foundation4UUIDVSgys6ResultOySayAA010DiscoveredA0VGAA0A8SDKErrorVGctF`

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

从底层数值安全解析 ConnectionState 枚举。

```swift
init?(rawValue: UInt32)
```

Swift 编译器为 UInt32 原始值枚举提供的可失败初始化，不是独立 SDK IO 实现。`rawValue` 匹配本版已声明项时返回对应值，未知数值返回 `nil`；不抛出异常，不执行查询、连接或设置。调用方应显式处理 nil，避免强制解包未来或无效值。

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

明确描述应用为该设备选择的 DPI、可打印点宽及标签最大高度；不从型号、纸宽或名称推断能力。`form` 和 `xOffsetDots` 只是每次标签作业的默认值，不是设备设置，也不影响 Raw。初始化可拒绝无效参数，但不验证实际硬件。

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

从底层数值安全解析 CutMode 枚举。

```swift
init?(rawValue: UInt32)
```

Swift 编译器为 UInt32 原始值枚举提供的可失败初始化，不是独立 SDK IO 实现。`rawValue` 匹配本版已声明项时返回对应值，未知数值返回 `nil`；不抛出异常，不执行查询、连接或设置。调用方应显式处理 nil，避免强制解包未来或无效值。

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

传给 `MobilePrinter(bleIdentifier:options:profile:)` 的选择结果。它是 iOS 外设 UUID，不是 Android 蓝牙 MAC 地址，也不是跨平台设备序列号；连接前目标须已被 CoreBluetooth 识别。应用应把设备身份诊断按隐私要求处理。

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

从底层数值安全解析 FinishScope 枚举。

```swift
init?(rawValue: UInt32)
```

Swift 编译器为 UInt32 原始值枚举提供的可失败初始化，不是独立 SDK IO 实现。`rawValue` 匹配本版已声明项时返回对应值，未知数值返回 `nil`；不抛出异常，不执行查询、连接或设置。调用方应显式处理 nil，避免强制解包未来或无效值。

文档 ID：`apple:symbol:s:10PrinterSDK11FinishScopeO8rawValueACSgs6UInt32V_tcfc`

符号种类：`swift.init`；访问级别：`public`。

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

从底层数值安全解析 MediaMode 枚举。

```swift
init?(rawValue: UInt32)
```

Swift 编译器为 UInt32 原始值枚举提供的可失败初始化，不是独立 SDK IO 实现。`rawValue` 匹配本版已声明项时返回对应值，未知数值返回 `nil`；不抛出异常，不执行查询、连接或设置。调用方应显式处理 nil，避免强制解包未来或无效值。

文档 ID：`apple:symbol:s:10PrinterSDK9MediaModeO8rawValueACSgs6UInt32V_tcfc`

符号种类：`swift.init`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-ac14c812144bc8d503ff"></a>

## MobilePrinter

<a id="api-48451d5fee6a3c851247"></a>

### MobilePrinter

使用一个 BLE 通信路径的前台打印设备对象。

```swift
final class MobilePrinter
```

创建时选择 CPCL 标签或 ESC/POS 小票配置；创建本身不连接外设。应用负责蓝牙权限、Info.plist 中的 `NSBluetoothAlwaysUsageDescription`、设备选择和前后台策略。同设备的图像、Raw、介质及定位操作共享一个串行 IO 队列。`connect`、`disconnect`、`close` 会阻塞，必须在应用工作线程调用；不要阻塞 UI 或 CoreBluetooth 回调队列。提交成功表示校验和入队，不等待打印机 IO 完成。应用持有返回的每个 `PrintOperation`，轮询到终态后显式关闭；SDK 不保存无限的已完成操作历史；每设备最多接纳 64 个未终态操作、未终态源载荷合计最多 64 MiB，超限以 1008 拒绝。本类型未声明 `Sendable`，应用需安排自己的并发隔离和生命周期。断线、取消或超时后不会自动重放或续打。

文档 ID：`apple:symbol:s:10PrinterSDK06MobileA0C`

符号种类：`swift.class`；访问级别：`public`。

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

`feature` 选择 `PrinterFeature`；返回不可变的 `FeatureSupport`。读取使用本地能力数据，不发送打印机 IO，不验证硬件或改变队列。区分 SDK 已实现、设备支持已知与证据来源：当前显式 BLE 配置的设备支持仍可未知，`isSupported == false` 不能一律理解为确定不支持。已关闭设备抛 1002，其他错误抛 `PrinterSDKError`。当前标准后端未实现状态查询和切纸。

文档 ID：`apple:symbol:s:10PrinterSDK06MobileA0C7featureyAA14FeatureSupportVAA0aE0OKF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| FeatureSupport | `s:10PrinterSDK14FeatureSupportV` |
| PrinterFeature | `s:10PrinterSDK0A7FeatureO` |

<a id="api-0907a3a9655c11f06753"></a>

### MobilePrinter.feedToNextLabel(timeoutMilliseconds:)

排入一个请求定位到下一标签起点的操作。

```swift
func feedToNextLabel(timeoutMilliseconds: UInt32 = 120000) throws -> PrintOperation
```

`timeoutMilliseconds` 默认 `120000`，须大于 0，单位毫秒，包含排队与执行。调用方须先装入并配置合适的间隙或黑标介质；此入口不设置介质、不执行传感器校准或读回。当前仅 CPCL 标签设备支持，小票设备抛 1004，零超时抛 1001，其他错误按 SDK 错误码处理。返回的 `PrintOperation` 与打印和 Raw 共用队列、取消及总预算规则。成功只报告 `DATA_SENT`，不能断言纸张已实际定位。

文档 ID：`apple:symbol:s:10PrinterSDK06MobileA0C15feedToNextLabel19timeoutMillisecondsAA14PrintOperationCs6UInt32V_tKF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| PrintOperation | `s:10PrinterSDK14PrintOperationC` |
| UInt32 | `s:s6UInt32V` |

<a id="api-28f3cb890ec9a5b9ea23"></a>

### MobilePrinter.init(bleIdentifier:options:profile:)

创建使用 CPCL 标签配置的 BLE 打印设备。

```swift
convenience init(bleIdentifier: UUID, options: BLEOptions, profile: CpclProfile) throws
```

`bleIdentifier` 为已选择并被 CoreBluetooth 识别的外设 UUID；`options` 明确服务、写特征及流控，`profile` 明确 DPI、打印宽度、标签最大高度和作业默认布局。初始化不连接、不扫描、不发送打印机字节。SDK 组合不完整或 ABI 不匹配抛 `PrinterSDKError`（1006），超出 Int32 可表示范围的 DPI 等无效配置抛 1001，不受支持的配置或资源失败也可能抛 SDK 错误；必须整包接入匹配的 SDK。成功后由调用者持有设备并在工作线程调用 `connect`，最终调用 `close`。选择配置不等于硬件认证。

文档 ID：`apple:symbol:s:10PrinterSDK06MobileA0C13bleIdentifier7options7profileAC10Foundation4UUIDV_AA10BLEOptionsVAA11CpclProfileVtKcfc`

符号种类：`swift.init`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| BLEOptions | `s:10PrinterSDK10BLEOptionsV` |
| CpclProfile | `s:10PrinterSDK11CpclProfileV` |
| UUID | `s:10Foundation4UUIDV` |

<a id="api-9e57c64776a1e2f3c20e"></a>

### MobilePrinter.init(bleIdentifier:options:profile:)

创建使用 ESC/POS 小票配置的 BLE 打印设备。

```swift
convenience init(bleIdentifier: UUID, options: BLEOptions, profile: ReceiptProfile) throws
```

`bleIdentifier` 为已识别的外设 UUID；`options` 指定 BLE 服务、写特征和流控，`profile` 显式指定 DPI 和打印点宽。初始化不扫描、不连接、不发送数据，也不根据纸宽猜测点宽、页长或切刀能力。ABI 不匹配抛 1006，配置或资源错误抛相应 `PrinterSDKError`。小票设备使用 `submitReceiptGray8`；标签、介质设置及下一标签定位入口对该设备不受支持。成功后调用者负责显式连接和关闭。

文档 ID：`apple:symbol:s:10PrinterSDK06MobileA0C13bleIdentifier7options7profileAC10Foundation4UUIDV_AA10BLEOptionsVAA14ReceiptProfileVtKcfc`

符号种类：`swift.init`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| BLEOptions | `s:10PrinterSDK10BLEOptionsV` |
| ReceiptProfile | `s:10PrinterSDK14ReceiptProfileV` |
| UUID | `s:10Foundation4UUIDV` |

<a id="api-95746c9ed1845422bc46"></a>

### MobilePrinter.queryStatus(timeoutMilliseconds:)

请求一个状态查询操作；当前标准后端会明确拒绝。

```swift
func queryStatus(timeoutMilliseconds: UInt32 = 5000) throws -> PrintOperation
```

`timeoutMilliseconds` 默认 `5000`，单位毫秒，必须大于 0，否则抛 1001。该公开入口保留统一操作契约，但当前 CPCL/ESC/POS 标准后端尚未实现状态查询：有效调用在打印机 IO 前抛 `PrinterSDKError(1004)`，不返回假就绪结果，也不返回可用操作。设备已关闭时抛 1002。能力应先查 `.status`；不要以连接状态、GATT 写入回调或 Credit 替代打印机状态。

文档 ID：`apple:symbol:s:10PrinterSDK06MobileA0C11queryStatus19timeoutMillisecondsAA14PrintOperationCs6UInt32V_tKF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| PrintOperation | `s:10PrinterSDK14PrintOperationC` |
| UInt32 | `s:s6UInt32V` |

<a id="api-a1bad58fc0a05122012a"></a>

### MobilePrinter.setMediaMode(_:timeoutMilliseconds:)

把介质跟踪模式设置作为独立操作排入设备队列。

```swift
func setMediaMode(_ mode: MediaMode, timeoutMilliseconds: UInt32 = 120000) throws -> PrintOperation
```

`mode` 为间隙、黑标或连续纸；`timeoutMilliseconds` 默认 `120000`，须大于 0，单位毫秒，覆盖排队和执行。当前仅 CPCL 标签配置支持此入口，小票设备抛 1004，零超时抛 1001；设备关闭或未连接时抛相应 SDK 错误。返回 `PrintOperation` 表示接纳，不修改已经构建的标签作业，也不在正在发送的操作中插入设置。成功的 `DATA_SENT` 仅证明设置字节已经传输，不证明设备已应用、持久保存、读回或完成校准；调用者负责装纸和确认实际兼容性。

文档 ID：`apple:symbol:s:10PrinterSDK06MobileA0C12setMediaMode_19timeoutMillisecondsAA14PrintOperationCAA0eF0O_s6UInt32VtKF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| MediaMode | `s:10PrinterSDK9MediaModeO` |
| PrintOperation | `s:10PrinterSDK14PrintOperationC` |
| UInt32 | `s:s6UInt32V` |

<a id="api-2a47cea19522bf8e00f4"></a>

### MobilePrinter.submitGray8(_:width:height:copies:trailingBlankDots:canvasWidthDots:originXDots:form:timeoutMilliseconds:)

复制完整 Gray8 图像并提交一个 CPCL 标签作业。

```swift
func submitGray8(_ pixels: Data, width: UInt32, height: UInt32, copies: UInt32 = 1, trailingBlankDots: UInt32 = 0, canvasWidthDots: UInt32? = nil, originXDots: UInt32? = nil, form: Bool? = nil, timeoutMilliseconds: UInt32 = 120000) throws -> PrintOperation
```

`pixels` 为逐行紧密排列、每像素 1 字节的灰度数据（0 黑、255 白），长度必须恰为 `width * height`；宽高均以点为单位且大于 0。当前此入口以阈值 128 二值化，不自动缩放或裁剪。`copies` 默认 1，范围 1…100；`trailingBlankDots` 默认 0，增加每张标签声明高度。`originXDots` 默认 `nil`，取 `CpclProfile.xOffsetDots`；须小于配置打印宽度。`canvasWidthDots` 默认 `nil`，取配置打印宽度减本次横向原点；图像补齐到整字节的横向范围及总高度必须落在标签和设备边界内。`form` 默认 `nil`，取 profile 默认值；true 请求下一标签起点结束，false 请求内容结束，仅保存在本次作业中。`timeoutMilliseconds` 默认 `120000`，必须大于 0，是入队后包含排队、准备和发送的共享总预算，单位毫秒，不按份数重新计时。

只适用于标签设备，误用于小票设备抛 1004。参数或几何无效抛 1001；单幅源图超过 16 MiB、设备未终态源载荷合计超过 64 MiB、每设备未终态操作超过 64 个或资源分配失败可抛 1008；这些是 SDK 接纳限制，不是硬件内存承诺。输入在提交过程中复制，返回后调用方可释放原 `Data`。成功返回一个需调用方持有和关闭的 `PrintOperation`，只表示已接纳；运行期失败、超时、取消通过其快照观察。打印机 IO 在设备队列执行，成功最多报告 `DATA_SENT`，不确认机械打印完成。为避免在 UI 上承担图像校验和复制成本，应在应用工作线程提交。

文档 ID：`apple:symbol:s:10PrinterSDK06MobileA0C11submitGray8_5width6height6copies17trailingBlankDots011canvasWidthK011originXDots4form19timeoutMillisecondsAA14PrintOperationC10Foundation4DataV_s6UInt32VA4SSgATSbSgAStKF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Bool | `s:Sb` |
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

<a id="api-41c74549ac58ce2b7249"></a>

### MobilePrinter.submitReceiptGray8(_:width:height:copies:maxTotalLengthDots:trailingBlankDots:cut:finishScope:timeoutMilliseconds:)

复制完整 Gray8 图像并提交一个 ESC/POS 小票作业。

```swift
func submitReceiptGray8(_ pixels: Data, width: UInt32, height: UInt32, copies: UInt32 = 1, maxTotalLengthDots: UInt32, trailingBlankDots: UInt32 = 0, cut: CutMode = .none, finishScope: FinishScope = .eachCopy, timeoutMilliseconds: UInt32 = 120000) throws -> PrintOperation
```

`pixels` 为紧密排列 Gray8（0 黑、255 白），长度恰为正数 `width * height`，宽高单位为点；当前使用阈值 128，不自动缩放或裁剪。点宽不得超过配置版心。`copies` 默认 1、范围 1…100；`maxTotalLengthDots` 为调用方必填的正数整单长度预算，包含全部份数及收尾留白，不代表设备单页长度能力。`trailingBlankDots` 默认 0；`cut` 默认 `.none`；`finishScope` 默认 `.eachCopy`；当前标准 ESC/POS 后端仅支持此值，`.endOfJob` 即使只有一份也会在 IO 前以 1004 拒绝。整单长度为图像高度乘份数，加按 scope 执行的尾部留白。当前标准 ESC/POS 后端要求不切纸，切纸策略不能静默丢弃；不支持的结束策略在首次 IO 前以 1004 拒绝。`timeoutMilliseconds` 默认 `120000`，必须大于 0，单位毫秒，涵盖排队、准备和所有份数的发送。

只适用于小票配置，标签设备调用抛 1004；参数、长度预算或几何无效抛 1001。单幅源图最多 16 MiB，设备未终态源载荷合计最多 64 MiB，每设备最多 64 个未终态操作；资源或接纳超限可抛 1008。提交复制像素，返回后原 `Data` 可释放。成功返回的 `PrintOperation` 表示入队；后续错误、取消和终态由快照观察，调用者负责持有和关闭。不在调用线程等待 IO 完成，但图像校验和复制应放在应用工作线程。成功只证明数据发送，不能断言已出纸或已切纸。

文档 ID：`apple:symbol:s:10PrinterSDK06MobileA0C18submitReceiptGray8_5width6height6copies18maxTotalLengthDots013trailingBlankM03cut11finishScope19timeoutMillisecondsAA14PrintOperationC10Foundation4DataV_s6UInt32VA4sA7CutModeOAA06FinishR0OAStKF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| CutMode | `s:10PrinterSDK7CutModeO` |
| Data | `s:10Foundation4DataV` |
| FinishScope | `s:10PrinterSDK11FinishScopeO` |
| PrintOperation | `s:10PrinterSDK14PrintOperationC` |
| UInt32 | `s:s6UInt32V` |

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

<a id="api-fcb644f7b0eb6c8a6489"></a>

## PrintOperation

<a id="api-97ddc1978712bd1f39f9"></a>

### PrintOperation

由调用方持有、可独立于设备读取结果的操作对象。

```swift
final class PrintOperation
```

仅由设备提交入口返回，没有公开初始化入口。应用保留对象直到不再需要结果；SDK 不建立已完成操作历史。`snapshot` 和 `cancel` 不要求设备仍存活，内部访问与 `close` 串行协调。`close` 仅释放本对象持有的操作资源，不取消打印；取消须显式调用 `cancel` 并观察终态。本类型未声明 `Sendable`，应用仍需安排并发隔离。

文档 ID：`apple:symbol:s:10PrinterSDK14PrintOperationC`

符号种类：`swift.class`；访问级别：`public`。

<a id="api-92ab438cb6d8e8afb8b9"></a>

### PrintOperation.cancel()

向该操作发送协作式取消请求。

```swift
func cancel() throws
```

请求返回不代表已经终止，应继续读取 `snapshot` 直到 `isTerminal`。排队操作可在零 IO 下取消；活动操作在检查点停止后续数据，已接受的字节不可撤回。已完成操作的迟到取消不改写结果；设备已销毁时不会再有活动工作，取消是安全的空操作。操作对象已关闭时抛 1002，其他错误抛 SDK 错误。尝试写入后失败或取消可能使连接失效，需应用显式重连且不重放旧操作。

文档 ID：`apple:symbol:s:10PrinterSDK14PrintOperationC6cancelyyKF`

符号种类：`swift.method`；访问级别：`public`。

<a id="api-c62d17e2700dd934e80a"></a>

### PrintOperation.close()

释放本对象持有的操作资源，保持已接纳打印的执行语义。

```swift
func close()
```

不抛出错误，重复调用安全，析构也执行此操作；关闭本对象不会请求取消打印。若希望停止工作，先 `cancel` 并观察终态，再关闭。关闭后 `snapshot`、`cancel` 抛 1002；此前返回的 `OperationSnapshot` 仍是可读值。设备的 `close` 与此方法职责不同：关闭设备会终止未完成工作。

文档 ID：`apple:symbol:s:10PrinterSDK14PrintOperationC5closeyyF`

符号种类：`swift.method`；访问级别：`public`。

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

<a id="api-12fe1504d628308a8272"></a>

### PrintOperation.snapshot()

取得当前操作的独立不可变数值快照。

```swift
func snapshot() throws -> OperationSnapshot
```

立即返回 `OperationSnapshot`，不等待打印完成，也不发送 IO；应用可在工作线程轮询并把 UI 更新派发回主线程。设备断开/关闭后，只要此操作未 `close` 仍能读取结果；先前已经取得的值也继续有效。此操作已关闭时抛 1002，其他获取错误抛 `PrinterSDKError`。快照不提供未知总字节的假零值，发送进度或 `DATA_SENT` 不能代表机械打印完成。

文档 ID：`apple:symbol:s:10PrinterSDK14PrintOperationC8snapshotAA0D8SnapshotVyKF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| OperationSnapshot | `s:10PrinterSDK17OperationSnapshotV` |

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

从底层数值安全解析 PrinterFeature 枚举。

```swift
init?(rawValue: UInt32)
```

Swift 编译器为 UInt32 原始值枚举提供的可失败初始化，不是独立 SDK IO 实现。`rawValue` 匹配本版已声明项时返回对应值，未知数值返回 `nil`；不抛出异常，不执行查询、连接或设置。调用方应显式处理 nil，避免强制解包未来或无效值。

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

<a id="api-8a4489e38f48eaabb599"></a>

## ReceiptProfile

<a id="api-5a5eb09c3d6917fdc589"></a>

### ReceiptProfile

不可变的 ESC/POS 小票分辨率和版心点宽配置。

```swift
struct ReceiptProfile
```

由应用明确提供，硬件最大页长和切刀能力仍未知；不以小票毫米宽度推断打印点宽。整单长度预算在 `submitReceiptGray8(maxTotalLengthDots:)` 中独立提供。构造不进行连接、IO 或硬件认证。

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

### FinishScope

| 声明顺序（从 0 起） | case | raw 值 | 编译器原值 |
| --- | --- | --- | --- |
| 0 | `eachCopy` | `0` | `{"kind":"integer","negative":false,"value":"0"}` |
| 1 | `endOfJob` | `1` | `{"kind":"integer","negative":false,"value":"1"}` |

### MediaMode

| 声明顺序（从 0 起） | case | raw 值 | 编译器原值 |
| --- | --- | --- | --- |
| 0 | `gap` | `1` | `{"kind":"integer","negative":false,"value":"1"}` |
| 1 | `blackMark` | `2` | `{"kind":"integer","negative":false,"value":"2"}` |
| 2 | `continuous` | `3` | `{"kind":"integer","negative":false,"value":"3"}` |

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

## 编译器公共关系

完整保留 **128** 条继承、协议遵循、成员及合成关系；
source 和 target 使用精确编译器标识，外部 Swift/Foundation 类型也保持原值。

| kind | source | target | 其他编译器字段 |
| --- | --- | --- | --- |
| `conformsTo` | `s:10PrinterSDK0A7FeatureO` | `s:SH` | `{}` |
| `conformsTo` | `s:10PrinterSDK0A7FeatureO` | `s:SQ` | `{}` |
| `conformsTo` | `s:10PrinterSDK0A7FeatureO` | `s:SY` | `{}` |
| `conformsTo` | `s:10PrinterSDK0A8SDKErrorV` | `s:10Foundation14LocalizedErrorP` | `{}` |
| `conformsTo` | `s:10PrinterSDK0A8SDKErrorV` | `s:SQ` | `{}` |
| `conformsTo` | `s:10PrinterSDK0A8SDKErrorV` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:10PrinterSDK0A8SDKErrorV` | `s:s5ErrorP` | `{}` |
| `conformsTo` | `s:10PrinterSDK0A8SDKErrorV` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:10PrinterSDK11FinishScopeO` | `s:SH` | `{}` |
| `conformsTo` | `s:10PrinterSDK11FinishScopeO` | `s:SQ` | `{}` |
| `conformsTo` | `s:10PrinterSDK11FinishScopeO` | `s:SY` | `{}` |
| `conformsTo` | `s:10PrinterSDK15ConnectionStateO` | `s:SH` | `{}` |
| `conformsTo` | `s:10PrinterSDK15ConnectionStateO` | `s:SQ` | `{}` |
| `conformsTo` | `s:10PrinterSDK15ConnectionStateO` | `s:SY` | `{}` |
| `conformsTo` | `s:10PrinterSDK7CutModeO` | `s:SH` | `{}` |
| `conformsTo` | `s:10PrinterSDK7CutModeO` | `s:SQ` | `{}` |
| `conformsTo` | `s:10PrinterSDK7CutModeO` | `s:SY` | `{}` |
| `conformsTo` | `s:10PrinterSDK9MediaModeO` | `s:SH` | `{}` |
| `conformsTo` | `s:10PrinterSDK9MediaModeO` | `s:SQ` | `{}` |
| `conformsTo` | `s:10PrinterSDK9MediaModeO` | `s:SY` | `{}` |
| `conformsTo` | `s:10PrinterSDK9MediaModeO` | `s:s12CaseIterableP` | `{}` |
| `memberOf` | `s:10Foundation14LocalizedErrorPAAE10helpAnchorSSSgvp::SYNTHESIZED::s:10PrinterSDK0A8SDKErrorV` | `s:10PrinterSDK0A8SDKErrorV` | `{"sourceOrigin":{"displayName":"LocalizedError.helpAnchor","identifier":"s:10Foundation14LocalizedErrorP10helpAnchorSSSgvp"}}` |
| `memberOf` | `s:10Foundation14LocalizedErrorPAAE13failureReasonSSSgvp::SYNTHESIZED::s:10PrinterSDK0A8SDKErrorV` | `s:10PrinterSDK0A8SDKErrorV` | `{"sourceOrigin":{"displayName":"LocalizedError.failureReason","identifier":"s:10Foundation14LocalizedErrorP13failureReasonSSSgvp"}}` |
| `memberOf` | `s:10Foundation14LocalizedErrorPAAE18recoverySuggestionSSSgvp::SYNTHESIZED::s:10PrinterSDK0A8SDKErrorV` | `s:10PrinterSDK0A8SDKErrorV` | `{"sourceOrigin":{"displayName":"LocalizedError.recoverySuggestion","identifier":"s:10Foundation14LocalizedErrorP18recoverySuggestionSSSgvp"}}` |
| `memberOf` | `s:10PrinterSDK010DiscoveredA0V10identifier10Foundation4UUIDVvp` | `s:10PrinterSDK010DiscoveredA0V` | `{}` |
| `memberOf` | `s:10PrinterSDK010DiscoveredA0V18advertisedServicesSaySSGvp` | `s:10PrinterSDK010DiscoveredA0V` | `{}` |
| `memberOf` | `s:10PrinterSDK010DiscoveredA0V4nameSSSgvp` | `s:10PrinterSDK010DiscoveredA0V` | `{}` |
| `memberOf` | `s:10PrinterSDK010DiscoveredA0V4rssiSivp` | `s:10PrinterSDK010DiscoveredA0V` | `{}` |
| `memberOf` | `s:10PrinterSDK06MobileA0C10disconnectyyKF` | `s:10PrinterSDK06MobileA0C` | `{}` |
| `memberOf` | `s:10PrinterSDK06MobileA0C11queryStatus19timeoutMillisecondsAA14PrintOperationCs6UInt32V_tKF` | `s:10PrinterSDK06MobileA0C` | `{}` |
| `memberOf` | `s:10PrinterSDK06MobileA0C11submitGray8_5width6height6copies17trailingBlankDots011canvasWidthK011originXDots4form19timeoutMillisecondsAA14PrintOperationC10Foundation4DataV_s6UInt32VA4SSgATSbSgAStKF` | `s:10PrinterSDK06MobileA0C` | `{}` |
| `memberOf` | `s:10PrinterSDK06MobileA0C12setMediaMode_19timeoutMillisecondsAA14PrintOperationCAA0eF0O_s6UInt32VtKF` | `s:10PrinterSDK06MobileA0C` | `{}` |
| `memberOf` | `s:10PrinterSDK06MobileA0C13bleIdentifier7options7profileAC10Foundation4UUIDV_AA10BLEOptionsVAA11CpclProfileVtKcfc` | `s:10PrinterSDK06MobileA0C` | `{}` |
| `memberOf` | `s:10PrinterSDK06MobileA0C13bleIdentifier7options7profileAC10Foundation4UUIDV_AA10BLEOptionsVAA14ReceiptProfileVtKcfc` | `s:10PrinterSDK06MobileA0C` | `{}` |
| `memberOf` | `s:10PrinterSDK06MobileA0C15connectionStateAA010ConnectionE0OyKF` | `s:10PrinterSDK06MobileA0C` | `{}` |
| `memberOf` | `s:10PrinterSDK06MobileA0C15feedToNextLabel19timeoutMillisecondsAA14PrintOperationCs6UInt32V_tKF` | `s:10PrinterSDK06MobileA0C` | `{}` |
| `memberOf` | `s:10PrinterSDK06MobileA0C18submitReceiptGray8_5width6height6copies18maxTotalLengthDots013trailingBlankM03cut11finishScope19timeoutMillisecondsAA14PrintOperationC10Foundation4DataV_s6UInt32VA4sA7CutModeOAA06FinishR0OAStKF` | `s:10PrinterSDK06MobileA0C` | `{}` |
| `memberOf` | `s:10PrinterSDK06MobileA0C5closeyyF` | `s:10PrinterSDK06MobileA0C` | `{}` |
| `memberOf` | `s:10PrinterSDK06MobileA0C7connect19timeoutMillisecondsys6UInt32V_tKF` | `s:10PrinterSDK06MobileA0C` | `{}` |
| `memberOf` | `s:10PrinterSDK06MobileA0C7featureyAA14FeatureSupportVAA0aE0OKF` | `s:10PrinterSDK06MobileA0C` | `{}` |
| `memberOf` | `s:10PrinterSDK06MobileA0C9submitRaw_6copies19timeoutMillisecondsAA14PrintOperationC10Foundation4DataV_s6UInt32VAMtKF` | `s:10PrinterSDK06MobileA0C` | `{}` |
| `memberOf` | `s:10PrinterSDK0A7FeatureO15feedToNextLabelyA2CmF` | `s:10PrinterSDK0A7FeatureO` | `{}` |
| `memberOf` | `s:10PrinterSDK0A7FeatureO3cutyA2CmF` | `s:10PrinterSDK0A7FeatureO` | `{}` |
| `memberOf` | `s:10PrinterSDK0A7FeatureO3rawyA2CmF` | `s:10PrinterSDK0A7FeatureO` | `{}` |
| `memberOf` | `s:10PrinterSDK0A7FeatureO5imageyA2CmF` | `s:10PrinterSDK0A7FeatureO` | `{}` |
| `memberOf` | `s:10PrinterSDK0A7FeatureO5labelyA2CmF` | `s:10PrinterSDK0A7FeatureO` | `{}` |
| `memberOf` | `s:10PrinterSDK0A7FeatureO5mediayA2CmF` | `s:10PrinterSDK0A7FeatureO` | `{}` |
| `memberOf` | `s:10PrinterSDK0A7FeatureO6statusyA2CmF` | `s:10PrinterSDK0A7FeatureO` | `{}` |
| `memberOf` | `s:10PrinterSDK0A7FeatureO7receiptyA2CmF` | `s:10PrinterSDK0A7FeatureO` | `{}` |
| `memberOf` | `s:10PrinterSDK0A7FeatureO8rawValueACSgs6UInt32V_tcfc` | `s:10PrinterSDK0A7FeatureO` | `{"sourceOrigin":{"displayName":"RawRepresentable.init(rawValue:)","identifier":"s:SY8rawValuexSg03RawB0Qz_tcfc"}}` |
| `memberOf` | `s:10PrinterSDK0A8SDKErrorV16errorDescriptionSSSgvp` | `s:10PrinterSDK0A8SDKErrorV` | `{"sourceOrigin":{"displayName":"LocalizedError.errorDescription","identifier":"s:10Foundation14LocalizedErrorP16errorDescriptionSSSgvp"}}` |
| `memberOf` | `s:10PrinterSDK0A8SDKErrorV4codes5Int32Vvp` | `s:10PrinterSDK0A8SDKErrorV` | `{}` |
| `memberOf` | `s:10PrinterSDK0A8SDKErrorVyACs5Int32Vcfc` | `s:10PrinterSDK0A8SDKErrorV` | `{}` |
| `memberOf` | `s:10PrinterSDK10BLEOptionsV12withResponseSbvp` | `s:10PrinterSDK10BLEOptionsV` | `{}` |
| `memberOf` | `s:10PrinterSDK10BLEOptionsV13maxWriteBytesSivp` | `s:10PrinterSDK10BLEOptionsV` | `{}` |
| `memberOf` | `s:10PrinterSDK10BLEOptionsV17creditFlowControlAA09BLECrediteF0VSgvp` | `s:10PrinterSDK10BLEOptionsV` | `{}` |
| `memberOf` | `s:10PrinterSDK10BLEOptionsV19writeCharacteristic10Foundation4UUIDVvp` | `s:10PrinterSDK10BLEOptionsV` | `{}` |
| `memberOf` | `s:10PrinterSDK10BLEOptionsV7service10Foundation4UUIDVvp` | `s:10PrinterSDK10BLEOptionsV` | `{}` |
| `memberOf` | `s:10PrinterSDK10BLEOptionsV7service19writeCharacteristic12withResponse13maxWriteBytes17creditFlowControlAC10Foundation4UUIDV_AKSbSiAA09BLECreditmN0VSgtKcfc` | `s:10PrinterSDK10BLEOptionsV` | `{}` |
| `memberOf` | `s:10PrinterSDK10BLEScannerC4scan7seconds7service10completionySd_10Foundation4UUIDVSgys6ResultOySayAA010DiscoveredA0VGAA0A8SDKErrorVGctF` | `s:10PrinterSDK10BLEScannerC` | `{}` |
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
| `memberOf` | `s:10PrinterSDK14FeatureSupportV06deviceD0s6UInt32Vvp` | `s:10PrinterSDK14FeatureSupportV` | `{}` |
| `memberOf` | `s:10PrinterSDK14FeatureSupportV11isSupportedSbvp` | `s:10PrinterSDK14FeatureSupportV` | `{}` |
| `memberOf` | `s:10PrinterSDK14FeatureSupportV13isImplementedSbvp` | `s:10PrinterSDK14FeatureSupportV` | `{}` |
| `memberOf` | `s:10PrinterSDK14FeatureSupportV14implementations6UInt32Vvp` | `s:10PrinterSDK14FeatureSupportV` | `{}` |
| `memberOf` | `s:10PrinterSDK14FeatureSupportV8evidences6UInt32Vvp` | `s:10PrinterSDK14FeatureSupportV` | `{}` |
| `memberOf` | `s:10PrinterSDK14PrintOperationC2ids6UInt64Vvp` | `s:10PrinterSDK14PrintOperationC` | `{}` |
| `memberOf` | `s:10PrinterSDK14PrintOperationC5closeyyF` | `s:10PrinterSDK14PrintOperationC` | `{}` |
| `memberOf` | `s:10PrinterSDK14PrintOperationC6cancelyyKF` | `s:10PrinterSDK14PrintOperationC` | `{}` |
| `memberOf` | `s:10PrinterSDK14PrintOperationC8snapshotAA0D8SnapshotVyKF` | `s:10PrinterSDK14PrintOperationC` | `{}` |
| `memberOf` | `s:10PrinterSDK14ReceiptProfileV3dpi9widthDotsACs6UInt32V_AGtKcfc` | `s:10PrinterSDK14ReceiptProfileV` | `{}` |
| `memberOf` | `s:10PrinterSDK14ReceiptProfileV3dpis6UInt32Vvp` | `s:10PrinterSDK14ReceiptProfileV` | `{}` |
| `memberOf` | `s:10PrinterSDK14ReceiptProfileV9widthDotss6UInt32Vvp` | `s:10PrinterSDK14ReceiptProfileV` | `{}` |
| `memberOf` | `s:10PrinterSDK15ConnectionStateO10connectingyA2CmF` | `s:10PrinterSDK15ConnectionStateO` | `{}` |
| `memberOf` | `s:10PrinterSDK15ConnectionStateO12disconnectedyA2CmF` | `s:10PrinterSDK15ConnectionStateO` | `{}` |
| `memberOf` | `s:10PrinterSDK15ConnectionStateO13disconnectingyA2CmF` | `s:10PrinterSDK15ConnectionStateO` | `{}` |
| `memberOf` | `s:10PrinterSDK15ConnectionStateO8rawValueACSgs6UInt32V_tcfc` | `s:10PrinterSDK15ConnectionStateO` | `{"sourceOrigin":{"displayName":"RawRepresentable.init(rawValue:)","identifier":"s:SY8rawValuexSg03RawB0Qz_tcfc"}}` |
| `memberOf` | `s:10PrinterSDK15ConnectionStateO9connectedyA2CmF` | `s:10PrinterSDK15ConnectionStateO` | `{}` |
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
| `memberOf` | `s:10PrinterSDK20BLECreditFlowControlV6dataFC10Foundation4UUIDVvp` | `s:10PrinterSDK20BLECreditFlowControlV` | `{}` |
| `memberOf` | `s:10PrinterSDK20BLECreditFlowControlV7dataOut0F2FCAC10Foundation4UUIDV_AHtcfc` | `s:10PrinterSDK20BLECreditFlowControlV` | `{}` |
| `memberOf` | `s:10PrinterSDK20BLECreditFlowControlV7dataOut10Foundation4UUIDVvp` | `s:10PrinterSDK20BLECreditFlowControlV` | `{}` |
| `memberOf` | `s:10PrinterSDK7CutModeO4fullyA2CmF` | `s:10PrinterSDK7CutModeO` | `{}` |
| `memberOf` | `s:10PrinterSDK7CutModeO4noneyA2CmF` | `s:10PrinterSDK7CutModeO` | `{}` |
| `memberOf` | `s:10PrinterSDK7CutModeO7partialyA2CmF` | `s:10PrinterSDK7CutModeO` | `{}` |
| `memberOf` | `s:10PrinterSDK7CutModeO8rawValueACSgs6UInt32V_tcfc` | `s:10PrinterSDK7CutModeO` | `{"sourceOrigin":{"displayName":"RawRepresentable.init(rawValue:)","identifier":"s:SY8rawValuexSg03RawB0Qz_tcfc"}}` |
| `memberOf` | `s:10PrinterSDK9MediaModeO10continuousyA2CmF` | `s:10PrinterSDK9MediaModeO` | `{}` |
| `memberOf` | `s:10PrinterSDK9MediaModeO3gapyA2CmF` | `s:10PrinterSDK9MediaModeO` | `{}` |
| `memberOf` | `s:10PrinterSDK9MediaModeO8rawValueACSgs6UInt32V_tcfc` | `s:10PrinterSDK9MediaModeO` | `{"sourceOrigin":{"displayName":"RawRepresentable.init(rawValue:)","identifier":"s:SY8rawValuexSg03RawB0Qz_tcfc"}}` |
| `memberOf` | `s:10PrinterSDK9MediaModeO9blackMarkyA2CmF` | `s:10PrinterSDK9MediaModeO` | `{}` |
| `memberOf` | `s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK0A7FeatureO` | `s:10PrinterSDK0A7FeatureO` | `{"sourceOrigin":{"displayName":"Equatable.!=(_:_:)","identifier":"s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ"}}` |
| `memberOf` | `s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK0A8SDKErrorV` | `s:10PrinterSDK0A8SDKErrorV` | `{"sourceOrigin":{"displayName":"Equatable.!=(_:_:)","identifier":"s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ"}}` |
| `memberOf` | `s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK11FinishScopeO` | `s:10PrinterSDK11FinishScopeO` | `{"sourceOrigin":{"displayName":"Equatable.!=(_:_:)","identifier":"s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ"}}` |
| `memberOf` | `s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK15ConnectionStateO` | `s:10PrinterSDK15ConnectionStateO` | `{"sourceOrigin":{"displayName":"Equatable.!=(_:_:)","identifier":"s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ"}}` |
| `memberOf` | `s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK7CutModeO` | `s:10PrinterSDK7CutModeO` | `{"sourceOrigin":{"displayName":"Equatable.!=(_:_:)","identifier":"s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ"}}` |
| `memberOf` | `s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK9MediaModeO` | `s:10PrinterSDK9MediaModeO` | `{"sourceOrigin":{"displayName":"Equatable.!=(_:_:)","identifier":"s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ"}}` |
| `memberOf` | `s:SYsSHRzSH8RawValueSYRpzrlE04hashB0Sivp::SYNTHESIZED::s:10PrinterSDK0A7FeatureO` | `s:10PrinterSDK0A7FeatureO` | `{"sourceOrigin":{"displayName":"RawRepresentable.hashValue","identifier":"s:SYsSHRzSH8RawValueSYRpzrlE04hashB0Sivp"}}` |
| `memberOf` | `s:SYsSHRzSH8RawValueSYRpzrlE04hashB0Sivp::SYNTHESIZED::s:10PrinterSDK11FinishScopeO` | `s:10PrinterSDK11FinishScopeO` | `{"sourceOrigin":{"displayName":"RawRepresentable.hashValue","identifier":"s:SYsSHRzSH8RawValueSYRpzrlE04hashB0Sivp"}}` |
| `memberOf` | `s:SYsSHRzSH8RawValueSYRpzrlE04hashB0Sivp::SYNTHESIZED::s:10PrinterSDK15ConnectionStateO` | `s:10PrinterSDK15ConnectionStateO` | `{"sourceOrigin":{"displayName":"RawRepresentable.hashValue","identifier":"s:SYsSHRzSH8RawValueSYRpzrlE04hashB0Sivp"}}` |
| `memberOf` | `s:SYsSHRzSH8RawValueSYRpzrlE04hashB0Sivp::SYNTHESIZED::s:10PrinterSDK7CutModeO` | `s:10PrinterSDK7CutModeO` | `{"sourceOrigin":{"displayName":"RawRepresentable.hashValue","identifier":"s:SYsSHRzSH8RawValueSYRpzrlE04hashB0Sivp"}}` |
| `memberOf` | `s:SYsSHRzSH8RawValueSYRpzrlE04hashB0Sivp::SYNTHESIZED::s:10PrinterSDK9MediaModeO` | `s:10PrinterSDK9MediaModeO` | `{"sourceOrigin":{"displayName":"RawRepresentable.hashValue","identifier":"s:SYsSHRzSH8RawValueSYRpzrlE04hashB0Sivp"}}` |
| `memberOf` | `s:SYsSHRzSH8RawValueSYRpzrlE4hash4intoys6HasherVz_tF::SYNTHESIZED::s:10PrinterSDK0A7FeatureO` | `s:10PrinterSDK0A7FeatureO` | `{"sourceOrigin":{"displayName":"RawRepresentable.hash(into:)","identifier":"s:SYsSHRzSH8RawValueSYRpzrlE4hash4intoys6HasherVz_tF"}}` |
| `memberOf` | `s:SYsSHRzSH8RawValueSYRpzrlE4hash4intoys6HasherVz_tF::SYNTHESIZED::s:10PrinterSDK11FinishScopeO` | `s:10PrinterSDK11FinishScopeO` | `{"sourceOrigin":{"displayName":"RawRepresentable.hash(into:)","identifier":"s:SYsSHRzSH8RawValueSYRpzrlE4hash4intoys6HasherVz_tF"}}` |
| `memberOf` | `s:SYsSHRzSH8RawValueSYRpzrlE4hash4intoys6HasherVz_tF::SYNTHESIZED::s:10PrinterSDK15ConnectionStateO` | `s:10PrinterSDK15ConnectionStateO` | `{"sourceOrigin":{"displayName":"RawRepresentable.hash(into:)","identifier":"s:SYsSHRzSH8RawValueSYRpzrlE4hash4intoys6HasherVz_tF"}}` |
| `memberOf` | `s:SYsSHRzSH8RawValueSYRpzrlE4hash4intoys6HasherVz_tF::SYNTHESIZED::s:10PrinterSDK7CutModeO` | `s:10PrinterSDK7CutModeO` | `{"sourceOrigin":{"displayName":"RawRepresentable.hash(into:)","identifier":"s:SYsSHRzSH8RawValueSYRpzrlE4hash4intoys6HasherVz_tF"}}` |
| `memberOf` | `s:SYsSHRzSH8RawValueSYRpzrlE4hash4intoys6HasherVz_tF::SYNTHESIZED::s:10PrinterSDK9MediaModeO` | `s:10PrinterSDK9MediaModeO` | `{"sourceOrigin":{"displayName":"RawRepresentable.hash(into:)","identifier":"s:SYsSHRzSH8RawValueSYRpzrlE4hash4intoys6HasherVz_tF"}}` |
| `memberOf` | `s:s5ErrorP10FoundationE20localizedDescriptionSSvp::SYNTHESIZED::s:10PrinterSDK0A8SDKErrorV` | `s:10PrinterSDK0A8SDKErrorV` | `{"sourceOrigin":{"displayName":"Error.localizedDescription","identifier":"s:s5ErrorP10FoundationE20localizedDescriptionSSvp"}}` |
