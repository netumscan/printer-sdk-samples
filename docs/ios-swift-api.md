# iOS Swift API 参考

发布候选版本 **0.3.0-preview.10**；原生 SDK **0.3.0**；C ABI **3/r2**。

本页逐项覆盖审定的编译器公共 API 基线，共 **88** 个文档条目。
声明、JVM descriptor、常量、枚举和编译器关系均来自基线，说明按稳定文档 ID 维护。
机器可读的完整基线与说明见 [api-reference.json](api-reference.json)；运行
`python3 tools/check-api-reference.py` 可检查覆盖范围、审定 SHA-256 和本页字节一致性。

基线 SHA-256：`8aaed8263549d0d9ea0abc7c14918fdf38492531e97e975143c2d1c583533377`

运行约束与错误处理见 [API 与生命周期](api-lifecycle.md) 和 [错误码与排障](errors-troubleshooting.md)。
本页是未发布的发布候选契约，不代表发布制品或公开历史证明。硬件验收仍暂缓，`hardware_verified=false`。

## 类型导航

- [CompletionLevel](#api-7027c38eacd1e136b879)
- [ConnectionInfo](#api-00a5b075847f8811a848)
- [DiscoveredPrinter](#api-e7eba043862754865338)
- [ManagerOutcome](#api-625d7c79302095211bcb)
- [ManagerResult](#api-d5c711f55a02b7703b81)
- [PaperType](#api-d45d66e9043cfc850df0)
- [PrinterCapabilities](#api-51947abf7e628a2d3bc3)
- [PrinterManager](#api-1d5ad0ccf40ef0036743)
- [PrinterModel](#api-3f9717fd53ac6f2b1f4b)
- [RequestID](#api-98f79d03f589f3f1383b)
- [SDKInfo](#api-313c383cb33723929d18)
- [StatusResult](#api-498d0aa8573deb1c5df3)
- [Support](#api-eeb58ec4a51744fa2e58)

公共模块为 `PrinterSDK`，C 桥接模块为 `CPrinterSDK`。
本基线使用 iOS 15 arm64；符号附带的 Foundation 可用性不表示其他平台已完成分发或验收。
未单列 availability 的符号以 SDK 的最低 iOS 版本 15 为接入前提。

<a id="api-7027c38eacd1e136b879"></a>

## CompletionLevel

<a id="api-b43551f1f08033ae6071"></a>

### CompletionLevel

请求完成所依据的证据级别。

```swift
enum CompletionLevel
```

none、dataSent、deviceConfirmed的原始值分别为0、1、2；发送完成不推断纸面打印完成，只有明确设备响应才可确认设备完成。

文档 ID：`apple:symbol:s:10PrinterSDK15CompletionLevelO`

符号种类：`swift.enum`；访问级别：`public`。

<a id="api-17a429bc3fc72efaac8f"></a>

### CompletionLevel.!=(_:_:)

判断两个CompletionLevel值是否不同。

```swift
static func != (lhs: borrowing Self, rhs: borrowing Self) -> Bool
```

Swift Equatable协议提供的合成比较，按该值的相等语义取反，不执行打印机IO。哈希或相等不表示硬件身份已经认证。

文档 ID：`apple:symbol:s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK15CompletionLevelO`

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

<a id="api-405f6927c286f89ab363"></a>

### CompletionLevel.dataSent

本次数据传输完成。

```swift
case dataSent
```

只确认数据已交给传输，不推断纸面打印完成、出纸完成或介质设置已被硬件确认。

文档 ID：`apple:symbol:s:10PrinterSDK15CompletionLevelO8dataSentyA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-6b3a08b5989ebfa2d674"></a>

### CompletionLevel.deviceConfirmed

设备明确确认完成。

```swift
case deviceConfirmed
```

只有受支持协议提供足够设备响应证据时才可使用；普通写入成功不得升级为此值。

文档 ID：`apple:symbol:s:10PrinterSDK15CompletionLevelO15deviceConfirmedyA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-a9f99ca50aa642e6ad0c"></a>

### CompletionLevel.hash(into:)

向Swift Hasher写入该值的哈希内容。

```swift
func hash(into hasher: inout Hasher)
```

来自Hashable/RawRepresentable默认实现，供集合比较使用，不执行设备IO；不能作为稳定序列化或打印协议编码。

文档 ID：`apple:symbol:s:SYsSHRzSH8RawValueSYRpzrlE4hash4intoys6HasherVz_tF::SYNTHESIZED::s:10PrinterSDK15CompletionLevelO`

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

<a id="api-ef6d11e5397b2497f5d4"></a>

### CompletionLevel.hashValue

Swift合成的进程内哈希值。

```swift
var hashValue: Int { get }
```

用于Hashable集合，不保证跨运行或跨版本稳定，不作为打印协议值、设备认证或持久键。

文档 ID：`apple:symbol:s:SYsSHRzSH8RawValueSYRpzrlE04hashB0Sivp::SYNTHESIZED::s:10PrinterSDK15CompletionLevelO`

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

<a id="api-0329693e289810ecd80e"></a>

### CompletionLevel.init(rawValue:)

按精确原始值构造CompletionLevel。

```swift
init?(rawValue: UInt32)
```

原始值0、1、2分别构造none、dataSent、deviceConfirmed，未知值返回nil；不得据此伪造更高完成证据。

文档 ID：`apple:symbol:s:10PrinterSDK15CompletionLevelO8rawValueACSgs6UInt32V_tcfc`

符号种类：`swift.init`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-a62c684a4d7e718a5975"></a>

### CompletionLevel.makeFromRawAttachmentValue(_:)

CoreVideo提供的原始值附件转换扩展。

```swift
static func makeFromRawAttachmentValue(_ repr: CVAttachmentRawValue) -> Self?
```

这是当前Apple SDK对RawRepresentable类型合成的系统默认扩展，iOS27及以上可用；不是PrinterSDK打印、配置或设备控制入口。具体可用性以编译器声明为准。

文档 ID：`apple:symbol:s:SY9CoreVideoAA30CVAttachmentValueRepresentable03RawD0RpzrlE08makeFromf10AttachmentD0yxSgAA0cfD0VFZ::SYNTHESIZED::s:10PrinterSDK15CompletionLevelO`

符号种类：`swift.type.method`；访问级别：`public`。

这是编译器为当前类型带入的协议扩展／合成公共成员。

编译器 availability（保留基线原值）：

```json
[{"domain":"iOS","introduced":{"major":27,"minor":0}},{"domain":"macOS","introduced":{"major":27,"minor":0}},{"domain":"tvOS","introduced":{"major":27,"minor":0}},{"domain":"visionOS","introduced":{"major":27,"minor":0}},{"domain":"watchOS","introduced":{"major":27,"minor":0}}]
```

泛型约束（保留基线原值）：

```json
{"constraints":[{"kind":"conformance","lhs":"Self.RawValue","rhs":"CVAttachmentValueRepresentable","rhsPrecise":"s:9CoreVideo30CVAttachmentValueRepresentableP"}]}
```

扩展来源（保留基线原值）：

```json
{"constraints":[{"kind":"conformance","lhs":"Self.RawValue","rhs":"CVAttachmentValueRepresentable","rhsPrecise":"s:9CoreVideo30CVAttachmentValueRepresentableP"}],"extendedModule":"Swift","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| CVAttachmentRawValue | `s:9CoreVideo20CVAttachmentRawValueV` |

<a id="api-823f156da66c4aac3a9b"></a>

### CompletionLevel.none

没有传输完成证据。

```swift
case none
```

无输出的本地操作可成功且仍使用此级别；不表示纸面已完成。

文档 ID：`apple:symbol:s:10PrinterSDK15CompletionLevelO4noneyA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-f67788ebb1165c42739b"></a>

### CompletionLevel.rawAttachmentValueRepresentation

CoreVideo提供的原始值附件转换扩展。

```swift
var rawAttachmentValueRepresentation: CVAttachmentRawValue { get }
```

这是当前Apple SDK对RawRepresentable类型合成的系统默认扩展，iOS27及以上可用；不是PrinterSDK打印、配置或设备控制入口。具体可用性以编译器声明为准。

文档 ID：`apple:symbol:s:SY9CoreVideoAA30CVAttachmentValueRepresentable03RawD0RpzrlE013rawAttachmentD14RepresentationAA0cfD0Vvp::SYNTHESIZED::s:10PrinterSDK15CompletionLevelO`

符号种类：`swift.property`；访问级别：`public`。

这是编译器为当前类型带入的协议扩展／合成公共成员。

编译器 availability（保留基线原值）：

```json
[{"domain":"iOS","introduced":{"major":27,"minor":0}},{"domain":"macOS","introduced":{"major":27,"minor":0}},{"domain":"tvOS","introduced":{"major":27,"minor":0}},{"domain":"visionOS","introduced":{"major":27,"minor":0}},{"domain":"watchOS","introduced":{"major":27,"minor":0}}]
```

扩展来源（保留基线原值）：

```json
{"constraints":[{"kind":"conformance","lhs":"Self.RawValue","rhs":"CVAttachmentValueRepresentable","rhsPrecise":"s:9CoreVideo30CVAttachmentValueRepresentableP"}],"extendedModule":"Swift","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| CVAttachmentRawValue | `s:9CoreVideo20CVAttachmentRawValueV` |

<a id="api-00a5b075847f8811a848"></a>

## ConnectionInfo

<a id="api-42386e10b92938691f0a"></a>

### ConnectionInfo

连接成功或型号更新后采用的本地配置。

```swift
struct ConnectionInfo
```

只读的设备目标和可选型号；型号来自原发现名称推荐或显式选择，不代表打印机已认证其硬件身份。未采用型号时保持空值。

文档 ID：`apple:symbol:s:10PrinterSDK14ConnectionInfoV`

符号种类：`swift.struct`；访问级别：`public`。

<a id="api-532d663b1f40a2546db8"></a>

### ConnectionInfo.device

本次连接的设备目标。

```swift
var device: DiscoveredPrinter { get }
```

保留连接时使用的地址/标识和可选名称；该快照不携带平台原生蓝牙句柄，也不是新的发现或认证结果。

文档 ID：`apple:symbol:s:10PrinterSDK14ConnectionInfoV6deviceAA010DiscoveredA0Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| DiscoveredPrinter | `s:10PrinterSDK010DiscoveredA0V` |

<a id="api-61ae78bed1963d012e5b"></a>

### ConnectionInfo.model

当前已采用的型号。

```swift
var model: PrinterModel? { get }
```

来自名称推荐或显式setPrinterModel/连接选择；没有已采用的收录型号时为空。它描述本地配置，不声称打印机回报了该型号。

文档 ID：`apple:symbol:s:10PrinterSDK14ConnectionInfoV5modelAA0A5ModelOSgvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| PrinterModel | `s:10PrinterSDK0A5ModelO` |

<a id="api-e7eba043862754865338"></a>

## DiscoveredPrinter

<a id="api-f8569a880ca35c2fbdc3"></a>

### DiscoveredPrinter

iOS蓝牙发现中的设备值。

```swift
struct DiscoveredPrinter
```

只包含CoreBluetooth标识、可选名称和信号强度，不暴露CBPeripheral。发现结果不表示已连接或已确认打印能力。

文档 ID：`apple:symbol:s:10PrinterSDK010DiscoveredA0V`

符号种类：`swift.struct`；访问级别：`public`。

<a id="api-33036fc5f14ef3b9bca2"></a>

### DiscoveredPrinter.identifier

iOS蓝牙外设的CoreBluetooth标识。

```swift
let identifier: UUID
```

可作为后续BLE连接目标；UUID不是MAC地址，构造或保存此值不会自动连接或证明设备可达。

文档 ID：`apple:symbol:s:10PrinterSDK010DiscoveredA0V10identifier10Foundation4UUIDVvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UUID | `s:10Foundation4UUIDV` |

<a id="api-3c9b9c4f02bcc36293a0"></a>

### DiscoveredPrinter.init(identifier:name:)

使用BLE标识和可选名称创建发现值。

```swift
init(identifier: UUID, name: String? = nil)
```

identifier必须是真实CoreBluetooth外设UUID，name默认nil。创建不连接或扫描；没有测得信号强度，也不表示已认证型号。

文档 ID：`apple:symbol:s:10PrinterSDK010DiscoveredA0V10identifier4nameAC10Foundation4UUIDV_SSSgtcfc`

符号种类：`swift.init`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| String | `s:SS` |
| UUID | `s:10Foundation4UUIDV` |

<a id="api-a00f5310f1770af3ec9e"></a>

### DiscoveredPrinter.name

可选的原设备发现名称。

```swift
let name: String?
```

未知名称保持空值；SDK可按原名推荐已收录型号。App展示名和用户改名不能作为硬件已识别的证据。

文档 ID：`apple:symbol:s:10PrinterSDK010DiscoveredA0V4nameSSSgvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| String | `s:SS` |

<a id="api-3dd94cd3224fe40bb77f"></a>

### DiscoveredPrinter.signalStrength

可选的BLE接收信号强度。

```swift
var signalStrength: Int? { get }
```

以dBm表示，底层未知哨兵127映射为nil。App手动构造和ConnectionInfo中的设备目标没有测得信号强度。

文档 ID：`apple:symbol:s:10PrinterSDK010DiscoveredA0V14signalStrengthSiSgvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Int | `s:Si` |

<a id="api-625d7c79302095211bcb"></a>

## ManagerOutcome

<a id="api-3c6f85967ca0141b6cfb"></a>

### ManagerOutcome

请求的唯一终态分类。

```swift
enum ManagerOutcome
```

success、failure、cancelled分别表示成功、失败和取消；调用方还应检查完成级别与outputMayHaveOccurred。

文档 ID：`apple:symbol:s:10PrinterSDK14ManagerOutcomeO`

符号种类：`swift.enum`；访问级别：`public`。

<a id="api-e03191c4ce4d20764463"></a>

### ManagerOutcome.!=(_:_:)

判断两个ManagerOutcome值是否不同。

```swift
static func != (lhs: borrowing Self, rhs: borrowing Self) -> Bool
```

Swift Equatable协议提供的合成比较，按该值的相等语义取反，不执行打印机IO。哈希或相等不表示硬件身份已经认证。

文档 ID：`apple:symbol:s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK14ManagerOutcomeO`

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

<a id="api-e0724738dd1226b09583"></a>

### ManagerOutcome.cancelled

请求取消。

```swift
case cancelled
```

通常对应1005；最终结果仍可能带有已输出风险，实际后台退出前保留资源。

文档 ID：`apple:symbol:s:10PrinterSDK14ManagerOutcomeO9cancelledyA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-acb0001b23a99a7238a0"></a>

### ManagerOutcome.failure

请求失败。

```swift
case failure
```

错误原因见code/message；部分发送后失败可能已产生输出，不自动重放。

文档 ID：`apple:symbol:s:10PrinterSDK14ManagerOutcomeO7failureyA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-863a858c6e9dc3938409"></a>

### ManagerOutcome.success

请求成功。

```swift
case success
```

错误码为0；仍须按completion区分数据传输完成和明确设备确认。

文档 ID：`apple:symbol:s:10PrinterSDK14ManagerOutcomeO7successyA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-d5c711f55a02b7703b81"></a>

## ManagerResult

<a id="api-d56d334dd7a53c7814e9"></a>

### ManagerResult

单个请求的一次不可变终态结果。

```swift
struct ManagerResult<Value> where Value : Sendable
```

同时报告请求身份、成功/失败/取消、稳定错误码、完成依据和可能已输出标记。成功不自动等于纸面打印完成；失败或取消也可能发生在部分输出之后。

文档 ID：`apple:symbol:s:10PrinterSDK13ManagerResultV`

符号种类：`swift.struct`；访问级别：`public`。

泛型约束（保留基线原值）：

```json
{"constraints":[{"kind":"conformance","lhs":"Value","rhs":"Sendable","rhsPrecise":"s:s8SendableP"}],"parameters":[{"depth":0,"index":0,"name":"Value"}]}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Sendable | `s:s8SendableP` |

<a id="api-cb62e9deec0766a167ff"></a>

### ManagerResult.code

供程序分支使用的稳定错误码。

```swift
let code: Int32
```

成功为0；典型失败包括1001非法参数、1002状态不允许、1004不支持、1005取消、1008资源限制、2001超时、2002连接丢失。具体请求可能返回其他稳定SDK错误码。

文档 ID：`apple:symbol:s:10PrinterSDK13ManagerResultV4codes5Int32Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Int32 | `s:s5Int32V` |

<a id="api-82c678c43ee69cf97faf"></a>

### ManagerResult.completion

本次请求已达到的完成证据级别。

```swift
let completion: CompletionLevel
```

无输出的本地操作可成功且仍为NONE/none；DATA_SENT/dataSent只证明数据传输完成，不能据此推断纸面完成。

文档 ID：`apple:symbol:s:10PrinterSDK13ManagerResultV10completionAA15CompletionLevelOvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| CompletionLevel | `s:10PrinterSDK15CompletionLevelO` |

<a id="api-f78e30aca271cd8f90c4"></a>

### ManagerResult.isSuccess

请求是否成功的便捷判断。

```swift
var isSuccess: Bool { get }
```

仅比较outcome是否为success，不判断纸面是否打印完成；仍须检查completion和outputMayHaveOccurred。

文档 ID：`apple:symbol:s:10PrinterSDK13ManagerResultV9isSuccessSbvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Bool | `s:Sb` |

<a id="api-b6ff4bc88b3b1789c28e"></a>

### ManagerResult.message

面向诊断的可读错误信息。

```swift
let message: String
```

成功通常为空字符串；程序应根据code处理，不依赖文字匹配。它不替代完成级别与可能已输出标记。

文档 ID：`apple:symbol:s:10PrinterSDK13ManagerResultV7messageSSvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| String | `s:SS` |

<a id="api-7e6703d4eaf50b7c6138"></a>

### ManagerResult.outcome

该请求的成功、失败或取消分类。

```swift
let outcome: ManagerOutcome
```

成功对应错误码0；失败和取消没有查询值。不能仅凭分类忽略已发送的数据或可能已输出标记。

文档 ID：`apple:symbol:s:10PrinterSDK13ManagerResultV7outcomeAA0C7OutcomeOvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| ManagerOutcome | `s:10PrinterSDK14ManagerOutcomeO` |

<a id="api-8884170bc72ef1c2ea76"></a>

### ManagerResult.outputMayHaveOccurred

本次请求是否可能已经产生输出。

```swift
let outputMayHaveOccurred: Bool
```

失败或取消在部分发送后仍可能为true；调用方不得据此自动重放。false也不使传输完成升级为设备确认。

文档 ID：`apple:symbol:s:10PrinterSDK13ManagerResultV21outputMayHaveOccurredSbvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Bool | `s:Sb` |

<a id="api-c9a074ae453b91eb03fe"></a>

### ManagerResult.requestID

该终态对应的原请求身份。

```swift
let requestID: RequestID
```

与方法立即返回的RequestID对应；用于App关联请求，不含可供操作底层设备的原生句柄。

文档 ID：`apple:symbol:s:10PrinterSDK13ManagerResultV9requestIDAA07RequestF0Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| RequestID | `s:10PrinterSDK9RequestIDV` |

<a id="api-3593583350ae830705d0"></a>

### ManagerResult.value

成功查询或连接操作的可选返回值。

```swift
let value: Value?
```

连接、能力、状态、身份和发现请求成功时使用对应类型；打印等无返回值操作、失败和取消为空。

文档 ID：`apple:symbol:s:10PrinterSDK13ManagerResultV5valuexSgvp`

符号种类：`swift.property`；访问级别：`public`。

<a id="api-d45d66e9043cfc850df0"></a>

## PaperType

<a id="api-24c1b177a510265b689d"></a>

### PaperType

纸张的定位特征。

```swift
enum PaperType
```

仅包含间隙标签、黑标和连续纸。通过setPaperType作为独立IO请求采用；页面宽高不隐式选择纸型，也不隐式校准或持久化。

文档 ID：`apple:symbol:s:10PrinterSDK9PaperTypeO`

符号种类：`swift.enum`；访问级别：`public`。

<a id="api-ebaec18099dfc5f4b19a"></a>

### PaperType.!=(_:_:)

判断两个PaperType值是否不同。

```swift
static func != (lhs: borrowing Self, rhs: borrowing Self) -> Bool
```

Swift Equatable协议提供的合成比较，按该值的相等语义取反，不执行打印机IO。哈希或相等不表示硬件身份已经认证。

文档 ID：`apple:symbol:s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK9PaperTypeO`

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

<a id="api-2dd54db59e77741fb1b0"></a>

### PaperType.blackMark

黑标纸。

```swift
case blackMark
```

作为setPaperType参数时映射黑标定位，不隐式设置黑标尺寸/偏移或校准。

文档 ID：`apple:symbol:s:10PrinterSDK9PaperTypeO9blackMarkyA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-a1695cb3f3d8fa61f8a0"></a>

### PaperType.continuous

连续纸。

```swift
case continuous
```

作为setPaperType参数时选择连续定位；不隐式改变页面宽高或添加切纸。

文档 ID：`apple:symbol:s:10PrinterSDK9PaperTypeO10continuousyA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-b24ee93ad711aab74177"></a>

### PaperType.gapLabel

间隙标签纸。

```swift
case gapLabel
```

作为setPaperType参数时映射间隙定位，不隐式设置标签长度、间隙长度或校准。

文档 ID：`apple:symbol:s:10PrinterSDK9PaperTypeO8gapLabelyA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-51947abf7e628a2d3bc3"></a>

## PrinterCapabilities

<a id="api-04cc2e358bdb700591f5"></a>

### PrinterCapabilities

当前SDK实现与设备依据合成的只读能力。

```swift
struct PrinterCapabilities
```

每项能力为已知支持、已知不支持或未知，未知不能视作支持；毫米范围为空表示缺少型号或几何依据。能力查询不是打印机实时状态查询。

文档 ID：`apple:symbol:s:10PrinterSDK0A12CapabilitiesV`

符号种类：`swift.struct`；访问级别：`public`。

<a id="api-d3ac8e54ad06c2f6c0a2"></a>

### PrinterCapabilities.imagePrinting

图像打印的支持程度。

```swift
var imagePrinting: Support { get }
```

综合SDK实现和设备支持依据，未知时保留UNKNOWN/unknown。支持不豁免页面、图像和连接状态校验。

文档 ID：`apple:symbol:s:10PrinterSDK0A12CapabilitiesV13imagePrintingAA7SupportOvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Support | `s:10PrinterSDK7SupportO` |

<a id="api-6db91f6e0d8619a6d6f1"></a>

### PrinterCapabilities.labelPositioning

独立定位到下一标签的支持程度。

```swift
var labelPositioning: Support { get }
```

对应locateNextLabel；普通图像打印不会隐式执行定位。不支持或未知时不能承诺硬件会前进到正确标签。

文档 ID：`apple:symbol:s:10PrinterSDK0A12CapabilitiesV16labelPositioningAA7SupportOvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Support | `s:10PrinterSDK7SupportO` |

<a id="api-056293dbd67032630983"></a>

### PrinterCapabilities.maxPageHeightMm

有依据的最大页面高度，单位mm。

```swift
var maxPageHeightMm: Double? { get }
```

由当前型号精确点密度与原生几何上限换算；缺少型号或高度依据时为空，不用零值或特殊字符串表示未知。

文档 ID：`apple:symbol:s:10PrinterSDK0A12CapabilitiesV15maxPageHeightMmSdSgvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Double | `s:Sd` |

<a id="api-2adc76032ce7b2020773"></a>

### PrinterCapabilities.maxPrintWidthMm

当前型号允许的最大打印宽度，单位mm。

```swift
var maxPrintWidthMm: Double? { get }
```

由已采用型号的最大点宽和精确dots/mm换算；未采用型号时为空。它不是从图片DPI、平台density或设备名称之外的探测推断。

文档 ID：`apple:symbol:s:10PrinterSDK0A12CapabilitiesV15maxPrintWidthMmSdSgvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Double | `s:Sd` |

<a id="api-2a7390c5f6f83d38b6ac"></a>

### PrinterCapabilities.paperTypeSelection

独立纸型设置请求的支持程度。

```swift
var paperTypeSelection: Support { get }
```

对应setPaperType；明确不支持或未知不能视为已成功设置，纸面宽高不替代纸型命令。

文档 ID：`apple:symbol:s:10PrinterSDK0A12CapabilitiesV18paperTypeSelectionAA7SupportOvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Support | `s:10PrinterSDK7SupportO` |

<a id="api-795bfe303a330e27ee25"></a>

### PrinterCapabilities.rawCommands

原始命令发送的支持程度。

```swift
var rawCommands: Support { get }
```

Raw仍进入与打印相同的设备FIFO。支持发送字节不证明这些字节适用于当前型号，也不证明纸面完成。

文档 ID：`apple:symbol:s:10PrinterSDK0A12CapabilitiesV11rawCommandsAA7SupportOvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Support | `s:10PrinterSDK7SupportO` |

<a id="api-5416c03a2d6a97b76bb7"></a>

### PrinterCapabilities.statusQuery

硬件状态请求的支持程度。

```swift
var statusQuery: Support { get }
```

支持必须有协议和读取响应依据，具备读取函数不自动等于可查询状态。UNKNOWN/unknown仍表示未知。

文档 ID：`apple:symbol:s:10PrinterSDK0A12CapabilitiesV11statusQueryAA7SupportOvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Support | `s:10PrinterSDK7SupportO` |

<a id="api-1d5ad0ccf40ef0036743"></a>

## PrinterManager

<a id="api-03e4d1448ba1e8315656"></a>

### PrinterManager

移动端唯一公开运行时控制入口。

```swift
final class PrinterManager
```

每实例同时管理至多一台设备；请求立即返回身份，后台准备和原设备FIFO负责执行，最终回调在主线程异步交付。无自动重连、续打或重放。

文档 ID：`apple:symbol:s:10PrinterSDK0A7ManagerC`

符号种类：`swift.class`；访问级别：`public`。

<a id="api-8e997bbe3a79436294af"></a>

### PrinterManager.cancel(_:)

向来源Manager的单个请求发送取消信号。

```swift
func cancel(_ requestID: RequestID)
```

不是普通FIFO命令。未知、其他Manager、已完成或清理请求身份无效果；准备和发送阶段协作取消，实际退出前保留占用资源。取消可能已经输出，最终回调仍只有一次，不自动重放。

文档 ID：`apple:symbol:s:10PrinterSDK0A7ManagerC6cancelyyAA9RequestIDVF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| RequestID | `s:10PrinterSDK9RequestIDV` |

<a id="api-de266a4346e17581587d"></a>

### PrinterManager.capabilities(completion:)

异步读取本地能力快照。

```swift
@discardableResult func capabilities(completion: @escaping @MainActor @Sendable (ManagerResult<PrinterCapabilities>) -> Void) -> RequestID
```

需要有效连接；结果为三态能力和可空毫米范围，不执行硬件状态查询，也不把未知升级为支持。立即返回不透明请求身份，结果在主线程异步回调且仅一次；没有自动重试、重连或重放。

文档 ID：`apple:symbol:s:10PrinterSDK0A7ManagerC12capabilities10completionAA9RequestIDVyAA0C6ResultVyAA0A12CapabilitiesVGYbScMYcc_tF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| MainActor | `s:ScM` |
| ManagerResult | `s:10PrinterSDK13ManagerResultV` |
| PrinterCapabilities | `s:10PrinterSDK0A12CapabilitiesV` |
| RequestID | `s:10PrinterSDK9RequestIDV` |
| Void | `s:s4Voida` |

<a id="api-e260c729de54e181e4e5"></a>

### PrinterManager.close(completion:)

异步永久关闭该Manager。

```swift
@discardableResult func close(completion: @escaping @MainActor @Sendable (ManagerResult<Void>) -> Void) -> RequestID
```

执行断开与资源清理并拒绝后续业务请求；重复close保持幂等。清理回调等待实际资源退出，不通过提前取消释放仍在工作的图像预算。立即返回不透明请求身份，结果在主线程异步回调且仅一次；没有自动重试、重连或重放。

文档 ID：`apple:symbol:s:10PrinterSDK0A7ManagerC5close10completionAA9RequestIDVyAA0C6ResultVyytGYbScMYcc_tF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| MainActor | `s:ScM` |
| ManagerResult | `s:10PrinterSDK13ManagerResultV` |
| RequestID | `s:10PrinterSDK9RequestIDV` |
| Void | `s:s4Voida` |

<a id="api-9178b9dcd9ee871b7d4f"></a>

### PrinterManager.connectBle(device:completion:onDisconnected:)

连接BLE设备。

```swift
@discardableResult func connectBle(device: DiscoveredPrinter, completion: @escaping @MainActor @Sendable (ManagerResult<ConnectionInfo>) -> Void, onDisconnected: @escaping @MainActor @Sendable (ManagerResult<Void>) -> Void = { _ in }) -> RequestID
```

内部连接超时10秒，每Manager至多一台设备。按原设备名推荐收录型号，未知名失败1004并要求显式选择，不猜UUID/协议。成功返回ConnectionInfo；之后意外丢失至多交付一次onDisconnected，默认回调为空闭包，显式清理不触发。立即返回不透明请求身份，结果在主线程异步回调且仅一次；没有自动重试、重连或重放。

文档 ID：`apple:symbol:s:10PrinterSDK0A7ManagerC10connectBle6device10completion14onDisconnectedAA9RequestIDVAA010DiscoveredA0V_yAA0C6ResultVyAA14ConnectionInfoVGYbScMYccyAMyytGYbScMYcctF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| ConnectionInfo | `s:10PrinterSDK14ConnectionInfoV` |
| DiscoveredPrinter | `s:10PrinterSDK010DiscoveredA0V` |
| MainActor | `s:ScM` |
| ManagerResult | `s:10PrinterSDK13ManagerResultV` |
| RequestID | `s:10PrinterSDK9RequestIDV` |
| Void | `s:s4Voida` |

<a id="api-d1070459054a29450269"></a>

### PrinterManager.connectBle(device:model:completion:onDisconnected:)

连接BLE设备并显式采用型号。

```swift
@discardableResult func connectBle(device: DiscoveredPrinter, model: PrinterModel, completion: @escaping @MainActor @Sendable (ManagerResult<ConnectionInfo>) -> Void, onDisconnected: @escaping @MainActor @Sendable (ManagerResult<Void>) -> Void = { _ in }) -> RequestID
```

内部连接超时10秒，每Manager至多一台设备。显式选择收录型号提供通信/打印配置，覆盖名称推荐；选择不是硬件认证。成功返回ConnectionInfo；之后意外丢失至多交付一次onDisconnected，默认回调为空闭包，显式清理不触发。立即返回不透明请求身份，结果在主线程异步回调且仅一次；没有自动重试、重连或重放。

文档 ID：`apple:symbol:s:10PrinterSDK0A7ManagerC10connectBle6device5model10completion14onDisconnectedAA9RequestIDVAA010DiscoveredA0V_AA0A5ModelOyAA0C6ResultVyAA14ConnectionInfoVGYbScMYccyAPyytGYbScMYcctF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| ConnectionInfo | `s:10PrinterSDK14ConnectionInfoV` |
| DiscoveredPrinter | `s:10PrinterSDK010DiscoveredA0V` |
| MainActor | `s:ScM` |
| ManagerResult | `s:10PrinterSDK13ManagerResultV` |
| PrinterModel | `s:10PrinterSDK0A5ModelO` |
| RequestID | `s:10PrinterSDK9RequestIDV` |
| Void | `s:s4Voida` |

<a id="api-87b5c726b74156715212"></a>

### PrinterManager.disconnect(completion:)

异步断开并清理当前设备会话。

```swift
@discardableResult func disconnect(completion: @escaping @MainActor @Sendable (ManagerResult<Void>) -> Void) -> RequestID
```

取消未完成请求、停止发现并回收当前连接；终态与清理完成按既有顺序交付。可重复调用，完成后Manager可再次连接；不会触发本次连接的意外断连回调。立即返回不透明请求身份，结果在主线程异步回调且仅一次；没有自动重试、重连或重放。

文档 ID：`apple:symbol:s:10PrinterSDK0A7ManagerC10disconnect10completionAA9RequestIDVyAA0C6ResultVyytGYbScMYcc_tF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| MainActor | `s:ScM` |
| ManagerResult | `s:10PrinterSDK13ManagerResultV` |
| RequestID | `s:10PrinterSDK9RequestIDV` |
| Void | `s:s4Voida` |

<a id="api-d35ade3a5258fcd64664"></a>

### PrinterManager.init()

创建独立的iOS打印Manager。

```swift
init()
```

创建只分配本地管理状态，不连接、不扫描或执行打印IO。每实例同时管理至多一台BLE设备，所有公开完成回调均为MainActor异步交付。

文档 ID：`apple:symbol:s:10PrinterSDK0A7ManagerCACycfc`

符号种类：`swift.init`；访问级别：`public`。

<a id="api-f2d2b9d05b17c275fc50"></a>

### PrinterManager.locateNextLabel(completion:)

通过独立设备请求定位下一标签。

```swift
@discardableResult func locateNextLabel(completion: @escaping @MainActor @Sendable (ManagerResult<Void>) -> Void) -> RequestID
```

需要有效连接、已采用型号和相应协议能力；与打印、Raw、状态查询共享FIFO。不支持报告1004。普通打印不自动调用；发送完成不保证实际纸张已定位。立即返回不透明请求身份，结果在主线程异步回调且仅一次；没有自动重试、重连或重放。

文档 ID：`apple:symbol:s:10PrinterSDK0A7ManagerC15locateNextLabel10completionAA9RequestIDVyAA0C6ResultVyytGYbScMYcc_tF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| MainActor | `s:ScM` |
| ManagerResult | `s:10PrinterSDK13ManagerResultV` |
| RequestID | `s:10PrinterSDK9RequestIDV` |
| Void | `s:s4Voida` |

<a id="api-4e86c61c89baf50fafee"></a>

### PrinterManager.printEncodedImage(_:widthMm:heightMm:completion:)

直接打印一张PNG/JPEG编码图片。

```swift
@discardableResult func printEncodedImage(_ bytes: Data, widthMm: Double, heightMm: Double, completion: @escaping @MainActor @Sendable (ManagerResult<Void>) -> Void) -> RequestID
```

输入为完整PNG/JPEG byte[]或Data，可直接承接Flutter Uint8List；不接收Base64或裸RGBA。返回前复制稳定编码快照，解码/EXIF校正在后台执行；编码最多32MiB，损坏/不支持报告1001、资源超限1008。widthMm与heightMm必须为正有限毫米值，并受已采用型号宽高和资源限制。固定一份、阈值128、不反色；等比缩小并居中补白，不放大不裁切。页面尺寸不取图片DPI/density；不附加FORM、切纸或下一标签定位。立即返回不透明请求身份，结果在主线程异步回调且仅一次；没有自动重试、重连或重放。

文档 ID：`apple:symbol:s:10PrinterSDK0A7ManagerC17printEncodedImage_7widthMm06heightH010completionAA9RequestIDV10Foundation4DataV_S2dyAA0C6ResultVyytGYbScMYcctF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Data | `s:10Foundation4DataV` |
| Double | `s:Sd` |
| MainActor | `s:ScM` |
| ManagerResult | `s:10PrinterSDK13ManagerResultV` |
| RequestID | `s:10PrinterSDK9RequestIDV` |
| Void | `s:s4Voida` |

<a id="api-c355324ea2abbf675305"></a>

### PrinterManager.printImage(_:widthMm:heightMm:completion:)

直接打印平台图片。

```swift
@discardableResult func printImage(_ image: UIImage, widthMm: Double, heightMm: Double, completion: @escaping @MainActor @Sendable (ManagerResult<Void>) -> Void) -> RequestID
```

Android接收Bitmap，iOS接收具有CGImage且非动画的UIImage。返回前取得独立像素快照，调用方不得在快照期间并发修改/回收输入；返回后可自行管理原图。方向校正、白底灰度、缩放在有界后台进行，二值化/协议按条带处理。widthMm与heightMm必须为正有限毫米值，并受已采用型号宽高和资源限制。固定一份、阈值128、不反色；等比缩小并居中补白，不放大不裁切。页面尺寸不取图片DPI/density；不附加FORM、切纸或下一标签定位。 无效输入报告1001、资源超限1008。立即返回不透明请求身份，结果在主线程异步回调且仅一次；没有自动重试、重连或重放。

文档 ID：`apple:symbol:s:10PrinterSDK0A7ManagerC10printImage_7widthMm06heightG010completionAA9RequestIDVSo7UIImageC_S2dyAA0C6ResultVyytGYbScMYcctF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Double | `s:Sd` |
| MainActor | `s:ScM` |
| ManagerResult | `s:10PrinterSDK13ManagerResultV` |
| RequestID | `s:10PrinterSDK9RequestIDV` |
| UIImage | `c:objc(cs)UIImage` |
| Void | `s:s4Voida` |

<a id="api-a7bc7210d55174a7ca0e"></a>

### PrinterManager.queryStatus(completion:)

查询当前型号支持的硬件状态。

```swift
@discardableResult func queryStatus(completion: @escaping @MainActor @Sendable (ManagerResult<StatusResult>) -> Void) -> RequestID
```

需要有效连接与已采用型号；在设备FIFO中完成请求/响应，超时经最终回调报告。不支持报告1004，失败不给假正常值；成功的四项状态仍分别可能未知。立即返回不透明请求身份，结果在主线程异步回调且仅一次；没有自动重试、重连或重放。

文档 ID：`apple:symbol:s:10PrinterSDK0A7ManagerC11queryStatus10completionAA9RequestIDVyAA0C6ResultVyAA0eI0VGYbScMYcc_tF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| MainActor | `s:ScM` |
| ManagerResult | `s:10PrinterSDK13ManagerResultV` |
| RequestID | `s:10PrinterSDK9RequestIDV` |
| StatusResult | `s:10PrinterSDK12StatusResultV` |
| Void | `s:s4Voida` |

<a id="api-34c7311406a132d0572a"></a>

### PrinterManager.scanBLE(completion:)

异步扫描BLE设备。

```swift
@discardableResult func scanBLE(completion: @escaping @MainActor @Sendable (ManagerResult<[DiscoveredPrinter]>) -> Void) -> RequestID
```

默认扫描10秒，不限定服务；同Manager同时仅一项扫描，返回去重发现值。App提供系统蓝牙权限说明；扫描不连接或认证型号，cancel/清理会停止扫描。立即返回不透明请求身份，结果在主线程异步回调且仅一次；没有自动重试、重连或重放。

文档 ID：`apple:symbol:s:10PrinterSDK0A7ManagerC7scanBLE10completionAA9RequestIDVyAA0C6ResultVySayAA010DiscoveredA0VGGYbScMYcc_tF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| DiscoveredPrinter | `s:10PrinterSDK010DiscoveredA0V` |
| MainActor | `s:ScM` |
| ManagerResult | `s:10PrinterSDK13ManagerResultV` |
| RequestID | `s:10PrinterSDK9RequestIDV` |
| Void | `s:s4Voida` |

<a id="api-b2b88a9b9daf0fc19e3c"></a>

### PrinterManager.sdkInfo(completion:)

异步读取实际原生SDK身份。

```swift
@discardableResult func sdkInfo(completion: @escaping @MainActor @Sendable (ManagerResult<SDKInfo>) -> Void) -> RequestID
```

无需设备连接；关闭或正在清理的Manager拒绝请求。结果来自已加载/链接库，未知构建标识保留空值。立即返回不透明请求身份，结果在主线程异步回调且仅一次；没有自动重试、重连或重放。

文档 ID：`apple:symbol:s:10PrinterSDK0A7ManagerC7sdkInfo10completionAA9RequestIDVyAA0C6ResultVyAA7SDKInfoVGYbScMYcc_tF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| MainActor | `s:ScM` |
| ManagerResult | `s:10PrinterSDK13ManagerResultV` |
| RequestID | `s:10PrinterSDK9RequestIDV` |
| SDKInfo | `s:10PrinterSDK7SDKInfoV` |
| Void | `s:s4Voida` |

<a id="api-648522cc349a6d6b4388"></a>

### PrinterManager.sendRaw(_:completion:)

在原设备队列中发送一份原始命令。

```swift
@discardableResult func sendRaw(_ bytes: Data, completion: @escaping @MainActor @Sendable (ManagerResult<Void>) -> Void) -> RequestID
```

输入必须非空且不超过1MiB，返回前复制稳定快照；同设备与图像/控制请求串行执行，失效先前纸型证据。允许未采用打印型号的有效通信连接使用；App自行保证命令适用。立即返回不透明请求身份，结果在主线程异步回调且仅一次；没有自动重试、重连或重放。

文档 ID：`apple:symbol:s:10PrinterSDK0A7ManagerC7sendRaw_10completionAA9RequestIDV10Foundation4DataV_yAA0C6ResultVyytGYbScMYcctF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Data | `s:10Foundation4DataV` |
| MainActor | `s:ScM` |
| ManagerResult | `s:10PrinterSDK13ManagerResultV` |
| RequestID | `s:10PrinterSDK9RequestIDV` |
| Void | `s:s4Voida` |

<a id="api-ec1a332ef2cc38599c80"></a>

### PrinterManager.setPaperType(_:completion:)

通过独立设备请求设置纸型。

```swift
@discardableResult func setPaperType(_ type: PaperType, completion: @escaping @MainActor @Sendable (ManagerResult<Void>) -> Void) -> RequestID
```

仅接受间隙标签、黑标、连续纸；不是本地变量赋值，不隐式设宽高、校准或持久化。需要有效连接和型号，原设备FIFO串行执行；未知/不支持报告1004。成功仅按completion报告实际证据。立即返回不透明请求身份，结果在主线程异步回调且仅一次；没有自动重试、重连或重放。

文档 ID：`apple:symbol:s:10PrinterSDK0A7ManagerC12setPaperType_10completionAA9RequestIDVAA0eF0O_yAA0C6ResultVyytGYbScMYcctF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| MainActor | `s:ScM` |
| ManagerResult | `s:10PrinterSDK13ManagerResultV` |
| PaperType | `s:10PrinterSDK9PaperTypeO` |
| RequestID | `s:10PrinterSDK9RequestIDV` |
| Void | `s:s4Voida` |

<a id="api-869a693fa4d6c4700b2d"></a>

### PrinterManager.setPrinterModel(_:completion:)

连接后空闲时采用已收录型号。

```swift
@discardableResult func setPrinterModel(_ model: PrinterModel, completion: @escaping @MainActor @Sendable (ManagerResult<ConnectionInfo>) -> Void) -> RequestID
```

只修改本地默认通信/打印配置，不发送纸型命令或自动重连。必须已连接且业务空闲；忙碌失败1002、失败保留旧配置，成功返回ConnectionInfo并失效先前纸型证据。立即返回不透明请求身份，结果在主线程异步回调且仅一次；没有自动重试、重连或重放。

文档 ID：`apple:symbol:s:10PrinterSDK0A7ManagerC03setA5Model_10completionAA9RequestIDVAA0aE0O_yAA0C6ResultVyAA14ConnectionInfoVGYbScMYcctF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| ConnectionInfo | `s:10PrinterSDK14ConnectionInfoV` |
| MainActor | `s:ScM` |
| ManagerResult | `s:10PrinterSDK13ManagerResultV` |
| PrinterModel | `s:10PrinterSDK0A5ModelO` |
| RequestID | `s:10PrinterSDK9RequestIDV` |
| Void | `s:s4Voida` |

<a id="api-3f9717fd53ac6f2b1f4b"></a>

## PrinterModel

<a id="api-5a057d634dd702c10f54"></a>

### PrinterModel

SDK已经收录的打印机型号。

```swift
enum PrinterModel
```

目前只包含LT-M90。选择型号是本地通信与打印配置，不是硬件探测；不得把未知设备猜测为此型号。

文档 ID：`apple:symbol:s:10PrinterSDK0A5ModelO`

符号种类：`swift.enum`；访问级别：`public`。

<a id="api-038377d897b5e0fddb52"></a>

### PrinterModel.!=(_:_:)

判断两个PrinterModel值是否不同。

```swift
static func != (lhs: borrowing Self, rhs: borrowing Self) -> Bool
```

Swift Equatable协议提供的合成比较，按该值的相等语义取反，不执行打印机IO。哈希或相等不表示硬件身份已经认证。

文档 ID：`apple:symbol:s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK0A5ModelO`

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

<a id="api-72bd5d02c376337597ce"></a>

### PrinterModel.hash(into:)

向Swift Hasher写入该值的哈希内容。

```swift
func hash(into hasher: inout Hasher)
```

来自Hashable/RawRepresentable默认实现，供集合比较使用，不执行设备IO；不能作为稳定序列化或打印协议编码。

文档 ID：`apple:symbol:s:SYsSHRzSH8RawValueSYRpzrlE4hash4intoys6HasherVz_tF::SYNTHESIZED::s:10PrinterSDK0A5ModelO`

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

<a id="api-1bdc9d04a37a9a39f848"></a>

### PrinterModel.hashValue

Swift合成的进程内哈希值。

```swift
var hashValue: Int { get }
```

用于Hashable集合，不保证跨运行或跨版本稳定，不作为打印协议值、设备认证或持久键。

文档 ID：`apple:symbol:s:SYsSHRzSH8RawValueSYRpzrlE04hashB0Sivp::SYNTHESIZED::s:10PrinterSDK0A5ModelO`

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

<a id="api-814a4bef12d39641cfe4"></a>

### PrinterModel.init(rawValue:)

按精确原始值构造PrinterModel。

```swift
init?(rawValue: String)
```

目前仅字符串LT-M90可构造ltM90，其他字符串返回nil；本地构造不探测或认证设备。

文档 ID：`apple:symbol:s:10PrinterSDK0A5ModelO8rawValueACSgSS_tcfc`

符号种类：`swift.init`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| String | `s:SS` |

<a id="api-ceb6c45858237c5effb0"></a>

### PrinterModel.ltM90

已收录的LT-M90型号。

```swift
case ltM90
```

内部集中提供CPCL、BLE通信、精确8 dots/mm、864点最大打印宽度与固定居中规则；名称推荐或显式选择都不等于硬件认证。

文档 ID：`apple:symbol:s:10PrinterSDK0A5ModelO5ltM90yA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-344f16542184e2560502"></a>

### PrinterModel.makeFromRawAttachmentValue(_:)

CoreVideo提供的原始值附件转换扩展。

```swift
static func makeFromRawAttachmentValue(_ repr: CVAttachmentRawValue) -> Self?
```

这是当前Apple SDK对RawRepresentable类型合成的系统默认扩展，iOS27及以上可用；不是PrinterSDK打印、配置或设备控制入口。具体可用性以编译器声明为准。

文档 ID：`apple:symbol:s:SY9CoreVideoAA30CVAttachmentValueRepresentable03RawD0RpzrlE08makeFromf10AttachmentD0yxSgAA0cfD0VFZ::SYNTHESIZED::s:10PrinterSDK0A5ModelO`

符号种类：`swift.type.method`；访问级别：`public`。

这是编译器为当前类型带入的协议扩展／合成公共成员。

编译器 availability（保留基线原值）：

```json
[{"domain":"iOS","introduced":{"major":27,"minor":0}},{"domain":"macOS","introduced":{"major":27,"minor":0}},{"domain":"tvOS","introduced":{"major":27,"minor":0}},{"domain":"visionOS","introduced":{"major":27,"minor":0}},{"domain":"watchOS","introduced":{"major":27,"minor":0}}]
```

泛型约束（保留基线原值）：

```json
{"constraints":[{"kind":"conformance","lhs":"Self.RawValue","rhs":"CVAttachmentValueRepresentable","rhsPrecise":"s:9CoreVideo30CVAttachmentValueRepresentableP"}]}
```

扩展来源（保留基线原值）：

```json
{"constraints":[{"kind":"conformance","lhs":"Self.RawValue","rhs":"CVAttachmentValueRepresentable","rhsPrecise":"s:9CoreVideo30CVAttachmentValueRepresentableP"}],"extendedModule":"Swift","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| CVAttachmentRawValue | `s:9CoreVideo20CVAttachmentRawValueV` |

<a id="api-438aae2178950b513615"></a>

### PrinterModel.rawAttachmentValueRepresentation

CoreVideo提供的原始值附件转换扩展。

```swift
var rawAttachmentValueRepresentation: CVAttachmentRawValue { get }
```

这是当前Apple SDK对RawRepresentable类型合成的系统默认扩展，iOS27及以上可用；不是PrinterSDK打印、配置或设备控制入口。具体可用性以编译器声明为准。

文档 ID：`apple:symbol:s:SY9CoreVideoAA30CVAttachmentValueRepresentable03RawD0RpzrlE013rawAttachmentD14RepresentationAA0cfD0Vvp::SYNTHESIZED::s:10PrinterSDK0A5ModelO`

符号种类：`swift.property`；访问级别：`public`。

这是编译器为当前类型带入的协议扩展／合成公共成员。

编译器 availability（保留基线原值）：

```json
[{"domain":"iOS","introduced":{"major":27,"minor":0}},{"domain":"macOS","introduced":{"major":27,"minor":0}},{"domain":"tvOS","introduced":{"major":27,"minor":0}},{"domain":"visionOS","introduced":{"major":27,"minor":0}},{"domain":"watchOS","introduced":{"major":27,"minor":0}}]
```

扩展来源（保留基线原值）：

```json
{"constraints":[{"kind":"conformance","lhs":"Self.RawValue","rhs":"CVAttachmentValueRepresentable","rhsPrecise":"s:9CoreVideo30CVAttachmentValueRepresentableP"}],"extendedModule":"Swift","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| CVAttachmentRawValue | `s:9CoreVideo20CVAttachmentRawValueV` |

<a id="api-98f79d03f589f3f1383b"></a>

## RequestID

<a id="api-1f4163eebc83ddaebea0"></a>

### RequestID

只能由来源Manager识别的不透明请求身份。

```swift
struct RequestID
```

由请求方法返回，支持Hashable/Sendable，不开放内部序号或构造器。可交给同一Manager.cancel；已结束或其他Manager的身份无效果。

文档 ID：`apple:symbol:s:10PrinterSDK9RequestIDV`

符号种类：`swift.struct`；访问级别：`public`。

<a id="api-27a4a4298443bc685dd7"></a>

### RequestID.!=(_:_:)

判断两个RequestID值是否不同。

```swift
static func != (lhs: borrowing Self, rhs: borrowing Self) -> Bool
```

Swift Equatable协议提供的合成比较，按该值的相等语义取反，不执行打印机IO。哈希或相等不表示硬件身份已经认证。

文档 ID：`apple:symbol:s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK9RequestIDV`

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

<a id="api-313c383cb33723929d18"></a>

## SDKInfo

<a id="api-b963dd90e312ec5418c9"></a>

### SDKInfo

实际链接的原生SDK身份。

```swift
struct SDKInfo
```

nativeVersion和distributionVersion来自已链接库；buildID未知时为nil。它们不是Demo构建版本，也不证明真实硬件验收。

文档 ID：`apple:symbol:s:10PrinterSDK7SDKInfoV`

符号种类：`swift.struct`；访问级别：`public`。

<a id="api-3640fc8aa15d95ee1c79"></a>

### SDKInfo.!=(_:_:)

判断两个SDKInfo值是否不同。

```swift
static func != (lhs: borrowing Self, rhs: borrowing Self) -> Bool
```

Swift Equatable协议提供的合成比较，按该值的相等语义取反，不执行打印机IO。哈希或相等不表示硬件身份已经认证。

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

<a id="api-00f428edf3dc4b9c681e"></a>

### SDKInfo.buildID

可选的原生构建来源标识。

```swift
let buildID: String?
```

仅在构建嵌入已知来源标识时返回；未知为nil，不用N/A等特殊字符串。用于诊断，不是安全认证凭证。

文档 ID：`apple:symbol:s:10PrinterSDK7SDKInfoV7buildIDSSSgvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| String | `s:SS` |

<a id="api-9fa418f7057af6a2344f"></a>

### SDKInfo.distributionVersion

实际原生库嵌入的分发版本。

```swift
let distributionVersion: String
```

用于核对所加载制品身份；不从Demo配置覆盖，也不代表已通过真实硬件或公开发布验证。

文档 ID：`apple:symbol:s:10PrinterSDK7SDKInfoV19distributionVersionSSvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| String | `s:SS` |

<a id="api-7a66a92b85f58e9fa84d"></a>

### SDKInfo.nativeVersion

原生核心SDK版本。

```swift
let nativeVersion: String
```

由实际加载/链接的原生库返回，例如0.3.0；它与移动分发预览版本分别表示，不从App依赖声明猜测。

文档 ID：`apple:symbol:s:10PrinterSDK7SDKInfoV13nativeVersionSSvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| String | `s:SS` |

<a id="api-498d0aa8573deb1c5df3"></a>

## StatusResult

<a id="api-29137902e8f73010baa0"></a>

### StatusResult

设备响应中有证据的硬件条件。

```swift
struct StatusResult
```

仅报告缺纸、开盖、纸将尽和切刀错误四项可空布尔值。空值表示该项未知，不是正常；没有受支持响应时查询失败，不能由本地忙闲状态合成结果。

文档 ID：`apple:symbol:s:10PrinterSDK12StatusResultV`

符号种类：`swift.struct`；访问级别：`public`。

<a id="api-0ade4684fd8aff05bb46"></a>

### StatusResult.!=(_:_:)

判断两个StatusResult值是否不同。

```swift
static func != (lhs: borrowing Self, rhs: borrowing Self) -> Bool
```

Swift Equatable协议提供的合成比较，按该值的相等语义取反，不执行打印机IO。哈希或相等不表示硬件身份已经认证。

文档 ID：`apple:symbol:s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK12StatusResultV`

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

<a id="api-c53d1a22ed2696b0f73c"></a>

### StatusResult.coverOpen

开盖条件。

```swift
var coverOpen: Bool? { get }
```

有设备响应依据时true表示条件存在、false表示该条件未激活；空值表示未知。不能把未知解释为正常，也不能由本地发送完成推断此条件。

文档 ID：`apple:symbol:s:10PrinterSDK12StatusResultV9coverOpenSbSgvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Bool | `s:Sb` |

<a id="api-8af71d8fa89206341073"></a>

### StatusResult.cutterError

切刀错误条件。

```swift
var cutterError: Bool? { get }
```

有设备响应依据时true表示条件存在、false表示该条件未激活；空值表示未知。不能把未知解释为正常，也不能由本地发送完成推断此条件。

文档 ID：`apple:symbol:s:10PrinterSDK12StatusResultV11cutterErrorSbSgvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Bool | `s:Sb` |

<a id="api-2c1afb6518d9ced48df1"></a>

### StatusResult.paperNearEnd

纸将尽条件。

```swift
var paperNearEnd: Bool? { get }
```

有设备响应依据时true表示条件存在、false表示该条件未激活；空值表示未知。不能把未知解释为正常，也不能由本地发送完成推断此条件。

文档 ID：`apple:symbol:s:10PrinterSDK12StatusResultV12paperNearEndSbSgvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Bool | `s:Sb` |

<a id="api-983f3c62c4bc76d95400"></a>

### StatusResult.paperOut

缺纸条件。

```swift
var paperOut: Bool? { get }
```

有设备响应依据时true表示条件存在、false表示该条件未激活；空值表示未知。不能把未知解释为正常，也不能由本地发送完成推断此条件。

文档 ID：`apple:symbol:s:10PrinterSDK12StatusResultV8paperOutSbSgvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Bool | `s:Sb` |

<a id="api-eeb58ec4a51744fa2e58"></a>

## Support

<a id="api-571630f20c8cac161e08"></a>

### Support

能力支持程度的三态值。

```swift
enum Support
```

unknown保留未知，unsupported表示未实现或明确不支持，supported表示具备相应设备依据；不能仅凭SDK能编码就认定硬件支持。

文档 ID：`apple:symbol:s:10PrinterSDK7SupportO`

符号种类：`swift.enum`；访问级别：`public`。

<a id="api-b3120c2690210d0de857"></a>

### Support.!=(_:_:)

判断两个Support值是否不同。

```swift
static func != (lhs: borrowing Self, rhs: borrowing Self) -> Bool
```

Swift Equatable协议提供的合成比较，按该值的相等语义取反，不执行打印机IO。哈希或相等不表示硬件身份已经认证。

文档 ID：`apple:symbol:s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK7SupportO`

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

<a id="api-2fc157f662b65e20b807"></a>

### Support.supported

具备支持依据。

```swift
case supported
```

仍受当前连接、参数和型号范围限制；此值不是纸面输出成功证明。

文档 ID：`apple:symbol:s:10PrinterSDK7SupportO9supportedyA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-e97252e0072369f0ba0f"></a>

### Support.unknown

设备支持程度未知。

```swift
case unknown
```

没有足够设备支持依据，不能把此值当作SUPPORTED或正常硬件状态。

文档 ID：`apple:symbol:s:10PrinterSDK7SupportO7unknownyA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-6ec5bf1f73ac23e89cb9"></a>

### Support.unsupported

未实现或明确不支持。

```swift
case unsupported
```

对应SDK实现缺失或明确设备不支持；调用仍以具体请求的稳定错误码为准。

文档 ID：`apple:symbol:s:10PrinterSDK7SupportO11unsupportedyA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

## 枚举声明顺序与 raw 值

顺序来自编译器 AST；raw 值保留符号、类型和值，不用展示顺序代替 raw 值。

### CompletionLevel

| 声明顺序（从 0 起） | case | raw 值 | 编译器原值 |
| --- | --- | --- | --- |
| 0 | `none` | `0` | `{"kind":"integer","negative":false,"value":"0"}` |
| 1 | `dataSent` | `1` | `{"kind":"integer","negative":false,"value":"1"}` |
| 2 | `deviceConfirmed` | `2` | `{"kind":"integer","negative":false,"value":"2"}` |

### ManagerOutcome

| 声明顺序（从 0 起） | case | raw 值 | 编译器原值 |
| --- | --- | --- | --- |
| 0 | `success` | `无 raw 值` | `null` |
| 1 | `failure` | `无 raw 值` | `null` |
| 2 | `cancelled` | `无 raw 值` | `null` |

### PaperType

| 声明顺序（从 0 起） | case | raw 值 | 编译器原值 |
| --- | --- | --- | --- |
| 0 | `gapLabel` | `无 raw 值` | `null` |
| 1 | `blackMark` | `无 raw 值` | `null` |
| 2 | `continuous` | `无 raw 值` | `null` |

### PrinterModel

| 声明顺序（从 0 起） | case | raw 值 | 编译器原值 |
| --- | --- | --- | --- |
| 0 | `ltM90` | `LT-M90` | `{"kind":"string","negative":false,"value":"LT-M90"}` |

### Support

| 声明顺序（从 0 起） | case | raw 值 | 编译器原值 |
| --- | --- | --- | --- |
| 0 | `unknown` | `无 raw 值` | `null` |
| 1 | `unsupported` | `无 raw 值` | `null` |
| 2 | `supported` | `无 raw 值` | `null` |

## 编译器公共关系

完整保留 **119** 条继承、协议遵循、成员及合成关系；
source 和 target 使用精确编译器标识，外部 Swift/Foundation 类型也保持原值。

| kind | source | target | 其他编译器字段 |
| --- | --- | --- | --- |
| `conformsTo` | `s:10PrinterSDK010DiscoveredA0V` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:10PrinterSDK010DiscoveredA0V` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:10PrinterSDK0A12CapabilitiesV` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:10PrinterSDK0A12CapabilitiesV` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:10PrinterSDK0A5ModelO` | `s:SH` | `{}` |
| `conformsTo` | `s:10PrinterSDK0A5ModelO` | `s:SQ` | `{}` |
| `conformsTo` | `s:10PrinterSDK0A5ModelO` | `s:SY` | `{}` |
| `conformsTo` | `s:10PrinterSDK0A5ModelO` | `s:s12CaseIterableP` | `{}` |
| `conformsTo` | `s:10PrinterSDK0A5ModelO` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:10PrinterSDK0A5ModelO` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:10PrinterSDK0A7ManagerC` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:10PrinterSDK0A7ManagerC` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:10PrinterSDK12StatusResultV` | `s:SQ` | `{}` |
| `conformsTo` | `s:10PrinterSDK12StatusResultV` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:10PrinterSDK12StatusResultV` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:10PrinterSDK13ManagerResultV` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:10PrinterSDK13ManagerResultV` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:10PrinterSDK14ConnectionInfoV` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:10PrinterSDK14ConnectionInfoV` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:10PrinterSDK14ManagerOutcomeO` | `s:SH` | `{}` |
| `conformsTo` | `s:10PrinterSDK14ManagerOutcomeO` | `s:SQ` | `{}` |
| `conformsTo` | `s:10PrinterSDK14ManagerOutcomeO` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:10PrinterSDK14ManagerOutcomeO` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:10PrinterSDK15CompletionLevelO` | `s:SH` | `{}` |
| `conformsTo` | `s:10PrinterSDK15CompletionLevelO` | `s:SQ` | `{}` |
| `conformsTo` | `s:10PrinterSDK15CompletionLevelO` | `s:SY` | `{}` |
| `conformsTo` | `s:10PrinterSDK15CompletionLevelO` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:10PrinterSDK15CompletionLevelO` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:10PrinterSDK7SDKInfoV` | `s:SQ` | `{}` |
| `conformsTo` | `s:10PrinterSDK7SDKInfoV` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:10PrinterSDK7SDKInfoV` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:10PrinterSDK7SupportO` | `s:SH` | `{}` |
| `conformsTo` | `s:10PrinterSDK7SupportO` | `s:SQ` | `{}` |
| `conformsTo` | `s:10PrinterSDK7SupportO` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:10PrinterSDK7SupportO` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:10PrinterSDK9PaperTypeO` | `s:SH` | `{}` |
| `conformsTo` | `s:10PrinterSDK9PaperTypeO` | `s:SQ` | `{}` |
| `conformsTo` | `s:10PrinterSDK9PaperTypeO` | `s:s12CaseIterableP` | `{}` |
| `conformsTo` | `s:10PrinterSDK9PaperTypeO` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:10PrinterSDK9PaperTypeO` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:10PrinterSDK9RequestIDV` | `s:SH` | `{}` |
| `conformsTo` | `s:10PrinterSDK9RequestIDV` | `s:SQ` | `{}` |
| `conformsTo` | `s:10PrinterSDK9RequestIDV` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:10PrinterSDK9RequestIDV` | `s:s8SendableP` | `{}` |
| `memberOf` | `s:10PrinterSDK010DiscoveredA0V10identifier10Foundation4UUIDVvp` | `s:10PrinterSDK010DiscoveredA0V` | `{}` |
| `memberOf` | `s:10PrinterSDK010DiscoveredA0V10identifier4nameAC10Foundation4UUIDV_SSSgtcfc` | `s:10PrinterSDK010DiscoveredA0V` | `{}` |
| `memberOf` | `s:10PrinterSDK010DiscoveredA0V14signalStrengthSiSgvp` | `s:10PrinterSDK010DiscoveredA0V` | `{}` |
| `memberOf` | `s:10PrinterSDK010DiscoveredA0V4nameSSSgvp` | `s:10PrinterSDK010DiscoveredA0V` | `{}` |
| `memberOf` | `s:10PrinterSDK0A12CapabilitiesV11rawCommandsAA7SupportOvp` | `s:10PrinterSDK0A12CapabilitiesV` | `{}` |
| `memberOf` | `s:10PrinterSDK0A12CapabilitiesV11statusQueryAA7SupportOvp` | `s:10PrinterSDK0A12CapabilitiesV` | `{}` |
| `memberOf` | `s:10PrinterSDK0A12CapabilitiesV13imagePrintingAA7SupportOvp` | `s:10PrinterSDK0A12CapabilitiesV` | `{}` |
| `memberOf` | `s:10PrinterSDK0A12CapabilitiesV15maxPageHeightMmSdSgvp` | `s:10PrinterSDK0A12CapabilitiesV` | `{}` |
| `memberOf` | `s:10PrinterSDK0A12CapabilitiesV15maxPrintWidthMmSdSgvp` | `s:10PrinterSDK0A12CapabilitiesV` | `{}` |
| `memberOf` | `s:10PrinterSDK0A12CapabilitiesV16labelPositioningAA7SupportOvp` | `s:10PrinterSDK0A12CapabilitiesV` | `{}` |
| `memberOf` | `s:10PrinterSDK0A12CapabilitiesV18paperTypeSelectionAA7SupportOvp` | `s:10PrinterSDK0A12CapabilitiesV` | `{}` |
| `memberOf` | `s:10PrinterSDK0A5ModelO5ltM90yA2CmF` | `s:10PrinterSDK0A5ModelO` | `{}` |
| `memberOf` | `s:10PrinterSDK0A5ModelO8rawValueACSgSS_tcfc` | `s:10PrinterSDK0A5ModelO` | `{"sourceOrigin":{"displayName":"RawRepresentable.init(rawValue:)","identifier":"s:SY8rawValuexSg03RawB0Qz_tcfc"}}` |
| `memberOf` | `s:10PrinterSDK0A7ManagerC03setA5Model_10completionAA9RequestIDVAA0aE0O_yAA0C6ResultVyAA14ConnectionInfoVGYbScMYcctF` | `s:10PrinterSDK0A7ManagerC` | `{}` |
| `memberOf` | `s:10PrinterSDK0A7ManagerC10connectBle6device10completion14onDisconnectedAA9RequestIDVAA010DiscoveredA0V_yAA0C6ResultVyAA14ConnectionInfoVGYbScMYccyAMyytGYbScMYcctF` | `s:10PrinterSDK0A7ManagerC` | `{}` |
| `memberOf` | `s:10PrinterSDK0A7ManagerC10connectBle6device5model10completion14onDisconnectedAA9RequestIDVAA010DiscoveredA0V_AA0A5ModelOyAA0C6ResultVyAA14ConnectionInfoVGYbScMYccyAPyytGYbScMYcctF` | `s:10PrinterSDK0A7ManagerC` | `{}` |
| `memberOf` | `s:10PrinterSDK0A7ManagerC10disconnect10completionAA9RequestIDVyAA0C6ResultVyytGYbScMYcc_tF` | `s:10PrinterSDK0A7ManagerC` | `{}` |
| `memberOf` | `s:10PrinterSDK0A7ManagerC10printImage_7widthMm06heightG010completionAA9RequestIDVSo7UIImageC_S2dyAA0C6ResultVyytGYbScMYcctF` | `s:10PrinterSDK0A7ManagerC` | `{}` |
| `memberOf` | `s:10PrinterSDK0A7ManagerC11queryStatus10completionAA9RequestIDVyAA0C6ResultVyAA0eI0VGYbScMYcc_tF` | `s:10PrinterSDK0A7ManagerC` | `{}` |
| `memberOf` | `s:10PrinterSDK0A7ManagerC12capabilities10completionAA9RequestIDVyAA0C6ResultVyAA0A12CapabilitiesVGYbScMYcc_tF` | `s:10PrinterSDK0A7ManagerC` | `{}` |
| `memberOf` | `s:10PrinterSDK0A7ManagerC12setPaperType_10completionAA9RequestIDVAA0eF0O_yAA0C6ResultVyytGYbScMYcctF` | `s:10PrinterSDK0A7ManagerC` | `{}` |
| `memberOf` | `s:10PrinterSDK0A7ManagerC15locateNextLabel10completionAA9RequestIDVyAA0C6ResultVyytGYbScMYcc_tF` | `s:10PrinterSDK0A7ManagerC` | `{}` |
| `memberOf` | `s:10PrinterSDK0A7ManagerC17printEncodedImage_7widthMm06heightH010completionAA9RequestIDV10Foundation4DataV_S2dyAA0C6ResultVyytGYbScMYcctF` | `s:10PrinterSDK0A7ManagerC` | `{}` |
| `memberOf` | `s:10PrinterSDK0A7ManagerC5close10completionAA9RequestIDVyAA0C6ResultVyytGYbScMYcc_tF` | `s:10PrinterSDK0A7ManagerC` | `{}` |
| `memberOf` | `s:10PrinterSDK0A7ManagerC6cancelyyAA9RequestIDVF` | `s:10PrinterSDK0A7ManagerC` | `{}` |
| `memberOf` | `s:10PrinterSDK0A7ManagerC7scanBLE10completionAA9RequestIDVyAA0C6ResultVySayAA010DiscoveredA0VGGYbScMYcc_tF` | `s:10PrinterSDK0A7ManagerC` | `{}` |
| `memberOf` | `s:10PrinterSDK0A7ManagerC7sdkInfo10completionAA9RequestIDVyAA0C6ResultVyAA7SDKInfoVGYbScMYcc_tF` | `s:10PrinterSDK0A7ManagerC` | `{}` |
| `memberOf` | `s:10PrinterSDK0A7ManagerC7sendRaw_10completionAA9RequestIDV10Foundation4DataV_yAA0C6ResultVyytGYbScMYcctF` | `s:10PrinterSDK0A7ManagerC` | `{}` |
| `memberOf` | `s:10PrinterSDK0A7ManagerCACycfc` | `s:10PrinterSDK0A7ManagerC` | `{}` |
| `memberOf` | `s:10PrinterSDK12StatusResultV11cutterErrorSbSgvp` | `s:10PrinterSDK12StatusResultV` | `{}` |
| `memberOf` | `s:10PrinterSDK12StatusResultV12paperNearEndSbSgvp` | `s:10PrinterSDK12StatusResultV` | `{}` |
| `memberOf` | `s:10PrinterSDK12StatusResultV8paperOutSbSgvp` | `s:10PrinterSDK12StatusResultV` | `{}` |
| `memberOf` | `s:10PrinterSDK12StatusResultV9coverOpenSbSgvp` | `s:10PrinterSDK12StatusResultV` | `{}` |
| `memberOf` | `s:10PrinterSDK13ManagerResultV10completionAA15CompletionLevelOvp` | `s:10PrinterSDK13ManagerResultV` | `{}` |
| `memberOf` | `s:10PrinterSDK13ManagerResultV21outputMayHaveOccurredSbvp` | `s:10PrinterSDK13ManagerResultV` | `{}` |
| `memberOf` | `s:10PrinterSDK13ManagerResultV4codes5Int32Vvp` | `s:10PrinterSDK13ManagerResultV` | `{}` |
| `memberOf` | `s:10PrinterSDK13ManagerResultV5valuexSgvp` | `s:10PrinterSDK13ManagerResultV` | `{}` |
| `memberOf` | `s:10PrinterSDK13ManagerResultV7messageSSvp` | `s:10PrinterSDK13ManagerResultV` | `{}` |
| `memberOf` | `s:10PrinterSDK13ManagerResultV7outcomeAA0C7OutcomeOvp` | `s:10PrinterSDK13ManagerResultV` | `{}` |
| `memberOf` | `s:10PrinterSDK13ManagerResultV9isSuccessSbvp` | `s:10PrinterSDK13ManagerResultV` | `{}` |
| `memberOf` | `s:10PrinterSDK13ManagerResultV9requestIDAA07RequestF0Vvp` | `s:10PrinterSDK13ManagerResultV` | `{}` |
| `memberOf` | `s:10PrinterSDK14ConnectionInfoV5modelAA0A5ModelOSgvp` | `s:10PrinterSDK14ConnectionInfoV` | `{}` |
| `memberOf` | `s:10PrinterSDK14ConnectionInfoV6deviceAA010DiscoveredA0Vvp` | `s:10PrinterSDK14ConnectionInfoV` | `{}` |
| `memberOf` | `s:10PrinterSDK14ManagerOutcomeO7failureyA2CmF` | `s:10PrinterSDK14ManagerOutcomeO` | `{}` |
| `memberOf` | `s:10PrinterSDK14ManagerOutcomeO7successyA2CmF` | `s:10PrinterSDK14ManagerOutcomeO` | `{}` |
| `memberOf` | `s:10PrinterSDK14ManagerOutcomeO9cancelledyA2CmF` | `s:10PrinterSDK14ManagerOutcomeO` | `{}` |
| `memberOf` | `s:10PrinterSDK15CompletionLevelO15deviceConfirmedyA2CmF` | `s:10PrinterSDK15CompletionLevelO` | `{}` |
| `memberOf` | `s:10PrinterSDK15CompletionLevelO4noneyA2CmF` | `s:10PrinterSDK15CompletionLevelO` | `{}` |
| `memberOf` | `s:10PrinterSDK15CompletionLevelO8dataSentyA2CmF` | `s:10PrinterSDK15CompletionLevelO` | `{}` |
| `memberOf` | `s:10PrinterSDK15CompletionLevelO8rawValueACSgs6UInt32V_tcfc` | `s:10PrinterSDK15CompletionLevelO` | `{"sourceOrigin":{"displayName":"RawRepresentable.init(rawValue:)","identifier":"s:SY8rawValuexSg03RawB0Qz_tcfc"}}` |
| `memberOf` | `s:10PrinterSDK7SDKInfoV13nativeVersionSSvp` | `s:10PrinterSDK7SDKInfoV` | `{}` |
| `memberOf` | `s:10PrinterSDK7SDKInfoV19distributionVersionSSvp` | `s:10PrinterSDK7SDKInfoV` | `{}` |
| `memberOf` | `s:10PrinterSDK7SDKInfoV7buildIDSSSgvp` | `s:10PrinterSDK7SDKInfoV` | `{}` |
| `memberOf` | `s:10PrinterSDK7SupportO11unsupportedyA2CmF` | `s:10PrinterSDK7SupportO` | `{}` |
| `memberOf` | `s:10PrinterSDK7SupportO7unknownyA2CmF` | `s:10PrinterSDK7SupportO` | `{}` |
| `memberOf` | `s:10PrinterSDK7SupportO9supportedyA2CmF` | `s:10PrinterSDK7SupportO` | `{}` |
| `memberOf` | `s:10PrinterSDK9PaperTypeO10continuousyA2CmF` | `s:10PrinterSDK9PaperTypeO` | `{}` |
| `memberOf` | `s:10PrinterSDK9PaperTypeO8gapLabelyA2CmF` | `s:10PrinterSDK9PaperTypeO` | `{}` |
| `memberOf` | `s:10PrinterSDK9PaperTypeO9blackMarkyA2CmF` | `s:10PrinterSDK9PaperTypeO` | `{}` |
| `memberOf` | `s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK0A5ModelO` | `s:10PrinterSDK0A5ModelO` | `{"sourceOrigin":{"displayName":"Equatable.!=(_:_:)","identifier":"s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ"}}` |
| `memberOf` | `s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK12StatusResultV` | `s:10PrinterSDK12StatusResultV` | `{"sourceOrigin":{"displayName":"Equatable.!=(_:_:)","identifier":"s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ"}}` |
| `memberOf` | `s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK14ManagerOutcomeO` | `s:10PrinterSDK14ManagerOutcomeO` | `{"sourceOrigin":{"displayName":"Equatable.!=(_:_:)","identifier":"s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ"}}` |
| `memberOf` | `s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK15CompletionLevelO` | `s:10PrinterSDK15CompletionLevelO` | `{"sourceOrigin":{"displayName":"Equatable.!=(_:_:)","identifier":"s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ"}}` |
| `memberOf` | `s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK7SDKInfoV` | `s:10PrinterSDK7SDKInfoV` | `{"sourceOrigin":{"displayName":"Equatable.!=(_:_:)","identifier":"s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ"}}` |
| `memberOf` | `s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK7SupportO` | `s:10PrinterSDK7SupportO` | `{"sourceOrigin":{"displayName":"Equatable.!=(_:_:)","identifier":"s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ"}}` |
| `memberOf` | `s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK9PaperTypeO` | `s:10PrinterSDK9PaperTypeO` | `{"sourceOrigin":{"displayName":"Equatable.!=(_:_:)","identifier":"s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ"}}` |
| `memberOf` | `s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:10PrinterSDK9RequestIDV` | `s:10PrinterSDK9RequestIDV` | `{"sourceOrigin":{"displayName":"Equatable.!=(_:_:)","identifier":"s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ"}}` |
| `memberOf` | `s:SY9CoreVideoAA30CVAttachmentValueRepresentable03RawD0RpzrlE013rawAttachmentD14RepresentationAA0cfD0Vvp::SYNTHESIZED::s:10PrinterSDK0A5ModelO` | `s:10PrinterSDK0A5ModelO` | `{"sourceOrigin":{"displayName":"RawRepresentable.rawAttachmentValueRepresentation","identifier":"s:SY9CoreVideoAA30CVAttachmentValueRepresentable03RawD0RpzrlE013rawAttachmentD14RepresentationAA0cfD0Vvp"}}` |
| `memberOf` | `s:SY9CoreVideoAA30CVAttachmentValueRepresentable03RawD0RpzrlE013rawAttachmentD14RepresentationAA0cfD0Vvp::SYNTHESIZED::s:10PrinterSDK15CompletionLevelO` | `s:10PrinterSDK15CompletionLevelO` | `{"sourceOrigin":{"displayName":"RawRepresentable.rawAttachmentValueRepresentation","identifier":"s:SY9CoreVideoAA30CVAttachmentValueRepresentable03RawD0RpzrlE013rawAttachmentD14RepresentationAA0cfD0Vvp"}}` |
| `memberOf` | `s:SY9CoreVideoAA30CVAttachmentValueRepresentable03RawD0RpzrlE08makeFromf10AttachmentD0yxSgAA0cfD0VFZ::SYNTHESIZED::s:10PrinterSDK0A5ModelO` | `s:10PrinterSDK0A5ModelO` | `{"sourceOrigin":{"displayName":"RawRepresentable.makeFromRawAttachmentValue(_:)","identifier":"s:SY9CoreVideoAA30CVAttachmentValueRepresentable03RawD0RpzrlE08makeFromf10AttachmentD0yxSgAA0cfD0VFZ"}}` |
| `memberOf` | `s:SY9CoreVideoAA30CVAttachmentValueRepresentable03RawD0RpzrlE08makeFromf10AttachmentD0yxSgAA0cfD0VFZ::SYNTHESIZED::s:10PrinterSDK15CompletionLevelO` | `s:10PrinterSDK15CompletionLevelO` | `{"sourceOrigin":{"displayName":"RawRepresentable.makeFromRawAttachmentValue(_:)","identifier":"s:SY9CoreVideoAA30CVAttachmentValueRepresentable03RawD0RpzrlE08makeFromf10AttachmentD0yxSgAA0cfD0VFZ"}}` |
| `memberOf` | `s:SYsSHRzSH8RawValueSYRpzrlE04hashB0Sivp::SYNTHESIZED::s:10PrinterSDK0A5ModelO` | `s:10PrinterSDK0A5ModelO` | `{"sourceOrigin":{"displayName":"RawRepresentable.hashValue","identifier":"s:SYsSHRzSH8RawValueSYRpzrlE04hashB0Sivp"}}` |
| `memberOf` | `s:SYsSHRzSH8RawValueSYRpzrlE04hashB0Sivp::SYNTHESIZED::s:10PrinterSDK15CompletionLevelO` | `s:10PrinterSDK15CompletionLevelO` | `{"sourceOrigin":{"displayName":"RawRepresentable.hashValue","identifier":"s:SYsSHRzSH8RawValueSYRpzrlE04hashB0Sivp"}}` |
| `memberOf` | `s:SYsSHRzSH8RawValueSYRpzrlE4hash4intoys6HasherVz_tF::SYNTHESIZED::s:10PrinterSDK0A5ModelO` | `s:10PrinterSDK0A5ModelO` | `{"sourceOrigin":{"displayName":"RawRepresentable.hash(into:)","identifier":"s:SYsSHRzSH8RawValueSYRpzrlE4hash4intoys6HasherVz_tF"}}` |
| `memberOf` | `s:SYsSHRzSH8RawValueSYRpzrlE4hash4intoys6HasherVz_tF::SYNTHESIZED::s:10PrinterSDK15CompletionLevelO` | `s:10PrinterSDK15CompletionLevelO` | `{"sourceOrigin":{"displayName":"RawRepresentable.hash(into:)","identifier":"s:SYsSHRzSH8RawValueSYRpzrlE4hash4intoys6HasherVz_tF"}}` |
