# iOS Swift API 参考

发布候选版本 **0.3.0-preview.17**；原生 SDK **0.3.0**；C ABI **3/r3**。

本页逐项覆盖审定的编译器公共 API 基线，共 **141** 个文档条目。
声明、JVM descriptor、常量、枚举和编译器关系均来自基线，说明按稳定文档 ID 维护。
机器可读的完整基线与说明见 [api-reference.json](api-reference.json)；运行
`python3 tools/check-api-reference.py` 可检查覆盖范围、审定 SHA-256 和本页字节一致性。

基线 SHA-256：`7b1c77683ba68efba97058edb0aa38bc4996c17655670c678d3eb82b90164cff`

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
- [PrinterDelegate](#api-b18d6ae964c7a542be30)
- [PrinterInfo](#api-0e885a04f27772f30231)
- [PrinterManager](#api-1d5ad0ccf40ef0036743)
- [PrinterModel](#api-3f9717fd53ac6f2b1f4b)
- [RequestID](#api-98f79d03f589f3f1383b)
- [SDKInfo](#api-313c383cb33723929d18)
- [StatusResult](#api-498d0aa8573deb1c5df3)
- [Support](#api-eeb58ec4a51744fa2e58)

公共模块为 `XLPrinterSDK`，C 桥接模块为 `CPrinterSDK`。
本基线使用 iOS 15 arm64；符号附带的 Foundation 可用性不表示其他平台已完成分发或验收。
未单列 availability 的符号以 SDK 的最低 iOS 版本 15 为接入前提。

<a id="api-7027c38eacd1e136b879"></a>

## CompletionLevel

<a id="api-1fa8eb9f552510f1f0da"></a>

### CompletionLevel

请求完成所依据的证据级别。

```swift
enum CompletionLevel
```

none、dataSent、deviceConfirmed的原始值分别为0、1、2；发送完成不推断纸面打印完成，只有明确设备响应才可确认设备完成。

文档 ID：`apple:symbol:s:12XLPrinterSDK15CompletionLevelO`

符号种类：`swift.enum`；访问级别：`public`。

<a id="api-0b010773d86ed1739cb8"></a>

### CompletionLevel.!=(_:_:)

判断两个CompletionLevel值是否不同。

```swift
static func != (lhs: borrowing Self, rhs: borrowing Self) -> Bool
```

Swift Equatable协议提供的合成比较，按该值的相等语义取反，不执行打印机IO。哈希或相等不表示硬件身份已经认证。

文档 ID：`apple:symbol:s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:12XLPrinterSDK15CompletionLevelO`

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

<a id="api-ccc52243403977f5c112"></a>

### CompletionLevel.dataSent

本次数据传输完成。

```swift
case dataSent
```

只确认数据已交给传输，不推断纸面打印完成、出纸完成或介质设置已被硬件确认。

文档 ID：`apple:symbol:s:12XLPrinterSDK15CompletionLevelO8dataSentyA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-ce79c9afa2f725175b3c"></a>

### CompletionLevel.deviceConfirmed

设备明确确认完成。

```swift
case deviceConfirmed
```

只有受支持协议提供足够设备响应证据时才可使用；普通写入成功不得升级为此值。

文档 ID：`apple:symbol:s:12XLPrinterSDK15CompletionLevelO15deviceConfirmedyA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-4ad5d9d4c28756315316"></a>

### CompletionLevel.hash(into:)

向Swift Hasher写入该值的哈希内容。

```swift
func hash(into hasher: inout Hasher)
```

来自Hashable/RawRepresentable默认实现，供集合比较使用，不执行设备IO；不能作为稳定序列化或打印协议编码。

文档 ID：`apple:symbol:s:SYsSHRzSH8RawValueSYRpzrlE4hash4intoys6HasherVz_tF::SYNTHESIZED::s:12XLPrinterSDK15CompletionLevelO`

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

<a id="api-4dbc3f04861461cdbe5e"></a>

### CompletionLevel.hashValue

Swift合成的进程内哈希值。

```swift
var hashValue: Int { get }
```

用于Hashable集合，不保证跨运行或跨版本稳定，不作为打印协议值、设备认证或持久键。

文档 ID：`apple:symbol:s:SYsSHRzSH8RawValueSYRpzrlE04hashB0Sivp::SYNTHESIZED::s:12XLPrinterSDK15CompletionLevelO`

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

<a id="api-18cda5de7e8ce05c08a1"></a>

### CompletionLevel.init(rawValue:)

按精确原始值构造CompletionLevel。

```swift
init?(rawValue: UInt32)
```

原始值0、1、2分别构造none、dataSent、deviceConfirmed，未知值返回nil；不得据此伪造更高完成证据。

文档 ID：`apple:symbol:s:12XLPrinterSDK15CompletionLevelO8rawValueACSgs6UInt32V_tcfc`

符号种类：`swift.init`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-fc14bf6e974121e07b93"></a>

### CompletionLevel.makeFromRawAttachmentValue(_:)

CoreVideo提供的原始值附件转换扩展。

```swift
static func makeFromRawAttachmentValue(_ repr: CVAttachmentRawValue) -> Self?
```

这是当前Apple SDK对RawRepresentable类型合成的系统默认扩展，iOS27及以上可用；不是PrinterSDK打印、配置或设备控制入口。具体可用性以编译器声明为准。

文档 ID：`apple:symbol:s:SY9CoreVideoAA30CVAttachmentValueRepresentable03RawD0RpzrlE08makeFromf10AttachmentD0yxSgAA0cfD0VFZ::SYNTHESIZED::s:12XLPrinterSDK15CompletionLevelO`

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

<a id="api-2e74e30ba8fbea762840"></a>

### CompletionLevel.none

没有传输完成证据。

```swift
case none
```

无输出的本地操作可成功且仍使用此级别；不表示纸面已完成。

文档 ID：`apple:symbol:s:12XLPrinterSDK15CompletionLevelO4noneyA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-ccce04317afbbfc0a333"></a>

### CompletionLevel.rawAttachmentValueRepresentation

CoreVideo提供的原始值附件转换扩展。

```swift
var rawAttachmentValueRepresentation: CVAttachmentRawValue { get }
```

这是当前Apple SDK对RawRepresentable类型合成的系统默认扩展，iOS27及以上可用；不是PrinterSDK打印、配置或设备控制入口。具体可用性以编译器声明为准。

文档 ID：`apple:symbol:s:SY9CoreVideoAA30CVAttachmentValueRepresentable03RawD0RpzrlE013rawAttachmentD14RepresentationAA0cfD0Vvp::SYNTHESIZED::s:12XLPrinterSDK15CompletionLevelO`

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

<a id="api-9635cd060e781c11d13b"></a>

### ConnectionInfo

连接结果及本连接可选的设备报告。

```swift
struct ConnectionInfo
```

包含设备目标、已采用的可选型号和本次自动查询得到的可选printerInfo。型号来自本地配置，printerInfo来自设备回读，二者不能互相替代。信息未知或可选查询失败为空；重新连接重新查询，不继承旧设备信息。

文档 ID：`apple:symbol:s:12XLPrinterSDK14ConnectionInfoV`

符号种类：`swift.struct`；访问级别：`public`。

<a id="api-454aa7f7c38ee624be99"></a>

### ConnectionInfo.device

本次连接的设备目标。

```swift
var device: DiscoveredPrinter { get }
```

保留连接时使用的地址/标识和可选名称；该快照不携带平台原生蓝牙句柄，也不是新的发现或认证结果。

文档 ID：`apple:symbol:s:12XLPrinterSDK14ConnectionInfoV6deviceAA17DiscoveredPrinterVvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| DiscoveredPrinter | `s:12XLPrinterSDK17DiscoveredPrinterV` |

<a id="api-e5969d5ebd0faa1b604a"></a>

### ConnectionInfo.model

当前已采用的型号。

```swift
var model: PrinterModel? { get }
```

来自名称推荐或显式setPrinterModel/连接选择；没有已采用的收录型号时为空。它描述本地配置，不声称打印机回报了该型号。

文档 ID：`apple:symbol:s:12XLPrinterSDK14ConnectionInfoV5modelAA12PrinterModelOSgvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| PrinterModel | `s:12XLPrinterSDK12PrinterModelO` |

<a id="api-761a5ccb66662fb41755"></a>

### ConnectionInfo.printerInfo

本次连接自动读回的可选设备信息。

```swift
let printerInfo: PrinterInfo?
```

已采用且支持信息方言的型号在通信建立后，通过原设备FIFO自动查询一次，额外预算最多1000ms。回读超时、坏包或不支持时为空，保持可用连接与打印；真实通信故障和显式取消仍按原连接语义处理。不写入型号Profile，不沿用上次连接结果，修改型号不重复查询。

文档 ID：`apple:symbol:s:12XLPrinterSDK14ConnectionInfoV07printerD0AA07PrinterD0VSgvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| PrinterInfo | `s:12XLPrinterSDK11PrinterInfoV` |

<a id="api-e7eba043862754865338"></a>

## DiscoveredPrinter

<a id="api-bafd889e10de033cc84b"></a>

### DiscoveredPrinter

iOS蓝牙发现中的设备值。

```swift
struct DiscoveredPrinter
```

只包含CoreBluetooth标识、可选名称和信号强度，不暴露CBPeripheral。发现结果不表示已连接或已确认打印能力。

文档 ID：`apple:symbol:s:12XLPrinterSDK17DiscoveredPrinterV`

符号种类：`swift.struct`；访问级别：`public`。

<a id="api-a6d1087a65139a65ff49"></a>

### DiscoveredPrinter.identifier

iOS蓝牙外设的CoreBluetooth标识。

```swift
let identifier: UUID
```

可作为后续BLE连接目标；UUID不是MAC地址，构造或保存此值不会自动连接或证明设备可达。

文档 ID：`apple:symbol:s:12XLPrinterSDK17DiscoveredPrinterV10identifier10Foundation4UUIDVvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UUID | `s:10Foundation4UUIDV` |

<a id="api-309a6c76e566cf704316"></a>

### DiscoveredPrinter.init(identifier:name:)

使用BLE标识和可选名称创建发现值。

```swift
init(identifier: UUID, name: String? = nil)
```

identifier必须是真实CoreBluetooth外设UUID，name默认nil。创建不连接或扫描；没有测得信号强度，也不表示已认证型号。

文档 ID：`apple:symbol:s:12XLPrinterSDK17DiscoveredPrinterV10identifier4nameAC10Foundation4UUIDV_SSSgtcfc`

符号种类：`swift.init`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| String | `s:SS` |
| UUID | `s:10Foundation4UUIDV` |

<a id="api-e54ee8845cba31fb792c"></a>

### DiscoveredPrinter.name

可选的原设备发现名称。

```swift
let name: String?
```

未知名称保持空值；SDK可按原名推荐已收录型号。App展示名和用户改名不能作为硬件已识别的证据。

文档 ID：`apple:symbol:s:12XLPrinterSDK17DiscoveredPrinterV4nameSSSgvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| String | `s:SS` |

<a id="api-44ee501116f780d1a774"></a>

### DiscoveredPrinter.signalStrength

可选的BLE接收信号强度。

```swift
var signalStrength: Int? { get }
```

以dBm表示，底层未知哨兵127映射为nil。App手动构造和ConnectionInfo中的设备目标没有测得信号强度。

文档 ID：`apple:symbol:s:12XLPrinterSDK17DiscoveredPrinterV14signalStrengthSiSgvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Int | `s:Si` |

<a id="api-625d7c79302095211bcb"></a>

## ManagerOutcome

<a id="api-52ff4a2dac4c44df75ea"></a>

### ManagerOutcome

请求的唯一终态分类。

```swift
enum ManagerOutcome
```

success、failure、cancelled分别表示成功、失败和取消；调用方还应检查完成级别与outputMayHaveOccurred。

文档 ID：`apple:symbol:s:12XLPrinterSDK14ManagerOutcomeO`

符号种类：`swift.enum`；访问级别：`public`。

<a id="api-9568e83b651df7690f91"></a>

### ManagerOutcome.!=(_:_:)

判断两个ManagerOutcome值是否不同。

```swift
static func != (lhs: borrowing Self, rhs: borrowing Self) -> Bool
```

Swift Equatable协议提供的合成比较，按该值的相等语义取反，不执行打印机IO。哈希或相等不表示硬件身份已经认证。

文档 ID：`apple:symbol:s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:12XLPrinterSDK14ManagerOutcomeO`

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

<a id="api-128a39d124ef01d8f730"></a>

### ManagerOutcome.cancelled

请求取消。

```swift
case cancelled
```

通常对应1005；最终结果仍可能带有已输出风险，实际后台退出前保留资源。

文档 ID：`apple:symbol:s:12XLPrinterSDK14ManagerOutcomeO9cancelledyA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-0f663471444953d83baa"></a>

### ManagerOutcome.failure

请求失败。

```swift
case failure
```

错误原因见code/message；部分发送后失败可能已产生输出，不自动重放。

文档 ID：`apple:symbol:s:12XLPrinterSDK14ManagerOutcomeO7failureyA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-cf2b9ff8b85c12c098ca"></a>

### ManagerOutcome.success

请求成功。

```swift
case success
```

错误码为0；仍须按completion区分数据传输完成和明确设备确认。

文档 ID：`apple:symbol:s:12XLPrinterSDK14ManagerOutcomeO7successyA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-d5c711f55a02b7703b81"></a>

## ManagerResult

<a id="api-a8a9fb94fb8ed56d5bd4"></a>

### ManagerResult

单个请求的一次不可变终态结果。

```swift
struct ManagerResult<Value> where Value : Sendable
```

同时报告请求身份、成功/失败/取消、稳定错误码、完成依据和可能已输出标记。成功不自动等于纸面打印完成；失败或取消也可能发生在部分输出之后。

文档 ID：`apple:symbol:s:12XLPrinterSDK13ManagerResultV`

符号种类：`swift.struct`；访问级别：`public`。

泛型约束（保留基线原值）：

```json
{"constraints":[{"kind":"conformance","lhs":"Value","rhs":"Sendable","rhsPrecise":"s:s8SendableP"}],"parameters":[{"depth":0,"index":0,"name":"Value"}]}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Sendable | `s:s8SendableP` |

<a id="api-00a635b3248111081288"></a>

### ManagerResult.code

供程序分支使用的稳定错误码。

```swift
let code: Int32
```

成功为0；典型失败包括1001非法参数、1002状态不允许、1004不支持、1005取消、1008资源限制、2001超时、2002连接丢失。具体请求可能返回其他稳定SDK错误码。

文档 ID：`apple:symbol:s:12XLPrinterSDK13ManagerResultV4codes5Int32Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Int32 | `s:s5Int32V` |

<a id="api-176992ba4a58d6d4144d"></a>

### ManagerResult.completion

本次请求已达到的完成证据级别。

```swift
let completion: CompletionLevel
```

无输出的本地操作可成功且仍为NONE/none；DATA_SENT/dataSent只证明数据传输完成，不能据此推断纸面完成。

文档 ID：`apple:symbol:s:12XLPrinterSDK13ManagerResultV10completionAA15CompletionLevelOvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| CompletionLevel | `s:12XLPrinterSDK15CompletionLevelO` |

<a id="api-755b72b026af6eed0783"></a>

### ManagerResult.isSuccess

请求是否成功的便捷判断。

```swift
var isSuccess: Bool { get }
```

仅比较outcome是否为success，不判断纸面是否打印完成；仍须检查completion和outputMayHaveOccurred。

文档 ID：`apple:symbol:s:12XLPrinterSDK13ManagerResultV9isSuccessSbvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Bool | `s:Sb` |

<a id="api-3d1a76a61ad73e53d5e6"></a>

### ManagerResult.message

面向诊断的可读错误信息。

```swift
let message: String
```

成功通常为空字符串；程序应根据code处理，不依赖文字匹配。它不替代完成级别与可能已输出标记。

文档 ID：`apple:symbol:s:12XLPrinterSDK13ManagerResultV7messageSSvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| String | `s:SS` |

<a id="api-973b6fcf41bf816b3c22"></a>

### ManagerResult.outcome

该请求的成功、失败或取消分类。

```swift
let outcome: ManagerOutcome
```

成功对应错误码0；失败和取消没有查询值。不能仅凭分类忽略已发送的数据或可能已输出标记。

文档 ID：`apple:symbol:s:12XLPrinterSDK13ManagerResultV7outcomeAA0C7OutcomeOvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| ManagerOutcome | `s:12XLPrinterSDK14ManagerOutcomeO` |

<a id="api-a12ac7648f9ef8b0a1c1"></a>

### ManagerResult.outputMayHaveOccurred

本次请求是否可能已经产生输出。

```swift
let outputMayHaveOccurred: Bool
```

失败或取消在部分发送后仍可能为true；调用方不得据此自动重放。false也不使传输完成升级为设备确认。

文档 ID：`apple:symbol:s:12XLPrinterSDK13ManagerResultV21outputMayHaveOccurredSbvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Bool | `s:Sb` |

<a id="api-d78f21faa1b477cde583"></a>

### ManagerResult.requestID

该终态对应的原请求身份。

```swift
let requestID: RequestID
```

与方法立即返回的RequestID对应；用于App关联请求，不含可供操作底层设备的原生句柄。

文档 ID：`apple:symbol:s:12XLPrinterSDK13ManagerResultV9requestIDAA07RequestF0Vvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| RequestID | `s:12XLPrinterSDK9RequestIDV` |

<a id="api-138549431f873e2d4708"></a>

### ManagerResult.value

成功查询或连接操作的可选返回值。

```swift
let value: Value?
```

连接、能力、身份和发现请求成功时使用对应类型；getStatus、打印等Void操作以及失败／取消为空。状态值经setStatusListener通知，不在getStatus结果中重复返回。

文档 ID：`apple:symbol:s:12XLPrinterSDK13ManagerResultV5valuexSgvp`

符号种类：`swift.property`；访问级别：`public`。

<a id="api-d45d66e9043cfc850df0"></a>

## PaperType

<a id="api-939c260b98329f89cb4b"></a>

### PaperType

纸张的定位特征。

```swift
enum PaperType
```

仅包含间隙标签、黑标和连续纸。通过setPaperType作为独立IO请求采用；页面宽高不隐式选择纸型，也不隐式校准或持久化。

文档 ID：`apple:symbol:s:12XLPrinterSDK9PaperTypeO`

符号种类：`swift.enum`；访问级别：`public`。

<a id="api-56d022904dbb50525722"></a>

### PaperType.!=(_:_:)

判断两个PaperType值是否不同。

```swift
static func != (lhs: borrowing Self, rhs: borrowing Self) -> Bool
```

Swift Equatable协议提供的合成比较，按该值的相等语义取反，不执行打印机IO。哈希或相等不表示硬件身份已经认证。

文档 ID：`apple:symbol:s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:12XLPrinterSDK9PaperTypeO`

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

<a id="api-0cdfbc55dd39cbc671b1"></a>

### PaperType.blackMark

黑标纸。

```swift
case blackMark
```

作为setPaperType参数时映射黑标定位，不隐式设置黑标尺寸/偏移或校准。

文档 ID：`apple:symbol:s:12XLPrinterSDK9PaperTypeO9blackMarkyA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-25327b2dbd5c84bd5a3c"></a>

### PaperType.continuous

连续纸。

```swift
case continuous
```

作为setPaperType参数时选择连续定位；不隐式改变页面宽高或添加切纸。

文档 ID：`apple:symbol:s:12XLPrinterSDK9PaperTypeO10continuousyA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-e0807080e7d67726ead6"></a>

### PaperType.gapLabel

间隙标签纸。

```swift
case gapLabel
```

作为setPaperType参数时映射间隙定位，不隐式设置标签长度、间隙长度或校准。

文档 ID：`apple:symbol:s:12XLPrinterSDK9PaperTypeO8gapLabelyA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-51947abf7e628a2d3bc3"></a>

## PrinterCapabilities

<a id="api-02dfa74daff5b226d132"></a>

### PrinterCapabilities

当前SDK实现与设备依据合成的只读能力。

```swift
struct PrinterCapabilities
```

每项能力为已知支持、已知不支持或未知，未知不能视作支持；毫米范围为空表示缺少型号或几何依据。能力查询不是打印机实时状态查询。

文档 ID：`apple:symbol:s:12XLPrinterSDK19PrinterCapabilitiesV`

符号种类：`swift.struct`；访问级别：`public`。

<a id="api-ae2f460336c9d30f03b9"></a>

### PrinterCapabilities.imagePrinting

图像打印的支持程度。

```swift
var imagePrinting: Support { get }
```

综合SDK实现和设备支持依据，未知时保留UNKNOWN/unknown。支持不豁免页面、图像和连接状态校验。

文档 ID：`apple:symbol:s:12XLPrinterSDK19PrinterCapabilitiesV13imagePrintingAA7SupportOvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Support | `s:12XLPrinterSDK7SupportO` |

<a id="api-0c9435cc6da5e5b6c802"></a>

### PrinterCapabilities.labelPositioning

独立定位到下一标签的支持程度。

```swift
var labelPositioning: Support { get }
```

对应locateNextLabel；普通图像打印不会隐式执行定位。不支持或未知时不能承诺硬件会前进到正确标签。

文档 ID：`apple:symbol:s:12XLPrinterSDK19PrinterCapabilitiesV16labelPositioningAA7SupportOvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Support | `s:12XLPrinterSDK7SupportO` |

<a id="api-2987bd60c03d278a48ca"></a>

### PrinterCapabilities.maxPageHeightMm

有依据的最大页面高度，单位mm。

```swift
var maxPageHeightMm: Double? { get }
```

由当前型号精确点密度与原生几何上限换算；缺少型号或高度依据时为空，不用零值或特殊字符串表示未知。

文档 ID：`apple:symbol:s:12XLPrinterSDK19PrinterCapabilitiesV15maxPageHeightMmSdSgvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Double | `s:Sd` |

<a id="api-e516cb671c081a4e1328"></a>

### PrinterCapabilities.maxPrintWidthMm

当前型号允许的最大打印宽度，单位mm。

```swift
var maxPrintWidthMm: Double? { get }
```

由已采用型号的最大点宽和精确dots/mm换算；未采用型号时为空。它不是从图片DPI、平台density或设备名称之外的探测推断。

文档 ID：`apple:symbol:s:12XLPrinterSDK19PrinterCapabilitiesV15maxPrintWidthMmSdSgvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Double | `s:Sd` |

<a id="api-d57067e4c505a4ea404c"></a>

### PrinterCapabilities.paperTypeSelection

独立纸型设置请求的支持程度。

```swift
var paperTypeSelection: Support { get }
```

对应setPaperType；明确不支持或未知不能视为已成功设置，纸面宽高不替代纸型命令。

文档 ID：`apple:symbol:s:12XLPrinterSDK19PrinterCapabilitiesV18paperTypeSelectionAA7SupportOvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Support | `s:12XLPrinterSDK7SupportO` |

<a id="api-3b37ad9f0ef52895f33e"></a>

### PrinterCapabilities.rawCommands

原始命令发送的支持程度。

```swift
var rawCommands: Support { get }
```

Raw仍进入与打印相同的设备FIFO。支持发送字节不证明这些字节适用于当前型号，也不证明纸面完成。

文档 ID：`apple:symbol:s:12XLPrinterSDK19PrinterCapabilitiesV11rawCommandsAA7SupportOvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Support | `s:12XLPrinterSDK7SupportO` |

<a id="api-24e42b4b1e5a99c20cae"></a>

### PrinterCapabilities.statusQuery

主动getStatus查询的支持程度。

```swift
var statusQuery: Support { get }
```

描述已实现的主动设备事务，不把读取函数或被动缓存当查询。LT-M90适配后端选择GS a 00；执行仍受连接、型号和INFO软失败后的查询失效策略约束。与独立statusReporting不可互推，未知保持UNKNOWN／unknown。

文档 ID：`apple:symbol:s:12XLPrinterSDK19PrinterCapabilitiesV11statusQueryAA7SupportOvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Support | `s:12XLPrinterSDK7SupportO` |

<a id="api-86eeaf48e7272611f5fe"></a>

### PrinterCapabilities.statusReporting

设备主动状态上报的支持程度。

```swift
var statusReporting: Support { get }
```

独立于主动查询statusQuery。已适配后端在无有效被动样本时为UNKNOWN／unknown，有效err:上报后为SUPPORTED／supported；查询裸回复不提升此证据。Android LT-M90 BLE／SPP与iOS BLE有后端，未适配后端为UNSUPPORTED／unsupported；断开／换型号清除旧证据。2026-10-23同一台LT-M90的Android BLE／SPP、iOS BLE已完成正常盖纸变化和Demo刷新验收；范围限于当前设备与固件，其他固件及真机强制故障仍未验证。

文档 ID：`apple:symbol:s:12XLPrinterSDK19PrinterCapabilitiesV15statusReportingAA7SupportOvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Support | `s:12XLPrinterSDK7SupportO` |

<a id="api-b18d6ae964c7a542be30"></a>

## PrinterDelegate

<a id="api-67a271554a12f688efb5"></a>

### PrinterDelegate

统一移动端请求与事件接口。

```swift
@MainActor protocol PrinterDelegate : AnyObject
```

每个Manager同时控制一台设备，公开动作返回关联请求ID，统一接收接口在主线程锁外交付强类型结果。内部每请求终态一次；接收者失效不重放通知，所有设备IO仍经原执行器。

文档 ID：`apple:symbol:s:12XLPrinterSDK15PrinterDelegateP`

符号种类：`swift.protocol`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| MainActor | `s:ScM` |

<a id="api-6d4d5f0a6baf31562298"></a>

### PrinterDelegate.printerManager(_:didDiscover:scanID:)

通知本轮首次发现的唯一设备。

```swift
@MainActor func printerManager(_ manager: PrinterManager, didDiscover device: DiscoveredPrinter, scanID: RequestID)
```

携带所属扫描请求ID与SDK设备值；每轮地址或UUID首次出现通知一次，最终列表保留后续合并信息。旧扫描、取消和接收者失效后的进度不派发，不暴露平台句柄。

文档 ID：`apple:symbol:s:12XLPrinterSDK15PrinterDelegateP14printerManager_11didDiscover6scanIDyAA0cF0C_AA010DiscoveredC0VAA07RequestJ0VtF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| DiscoveredPrinter | `s:12XLPrinterSDK17DiscoveredPrinterV` |
| MainActor | `s:ScM` |
| PrinterManager | `s:12XLPrinterSDK14PrinterManagerC` |
| RequestID | `s:12XLPrinterSDK9RequestIDV` |

<a id="api-90c30e0ca53b1243cc75"></a>

### PrinterDelegate.printerManager(_:didDiscover:scanID:)

通知本轮首次发现的唯一设备。

```swift
@MainActor func printerManager(_ manager: PrinterManager, didDiscover device: DiscoveredPrinter, scanID: RequestID)
```

携带所属扫描请求ID与SDK设备值；每轮地址或UUID首次出现通知一次，最终列表保留后续合并信息。旧扫描、取消和接收者失效后的进度不派发，不暴露平台句柄。

文档 ID：`apple:symbol:s:12XLPrinterSDK15PrinterDelegatePAAE14printerManager_11didDiscover6scanIDyAA0cF0C_AA010DiscoveredC0VAA07RequestJ0VtF`

符号种类：`swift.method`；访问级别：`public`。

扩展来源（保留基线原值）：

```json
{"extendedModule":"XLPrinterSDK","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| DiscoveredPrinter | `s:12XLPrinterSDK17DiscoveredPrinterV` |
| MainActor | `s:ScM` |
| PrinterManager | `s:12XLPrinterSDK14PrinterManagerC` |
| RequestID | `s:12XLPrinterSDK9RequestIDV` |

<a id="api-c108b62ac8f2459eed5f"></a>

### PrinterDelegate.printerManager(_:didFinishClosing:)

永久关闭并完成异步资源清理。

```swift
@MainActor func printerManager(_ manager: PrinterManager, didFinishClosing result: ManagerResult<Void>)
```

返回请求ID；必要状态重置和关闭结果交付后释放统一接收者。重复清理共享原屏障，不在UI线程同步等待，不承诺关闭后的历史补交。

文档 ID：`apple:symbol:s:12XLPrinterSDK15PrinterDelegateP14printerManager_16didFinishClosingyAA0cF0C_AA0F6ResultVyytGtF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| MainActor | `s:ScM` |
| ManagerResult | `s:12XLPrinterSDK13ManagerResultV` |
| PrinterManager | `s:12XLPrinterSDK14PrinterManagerC` |
| Void | `s:s4Voida` |

<a id="api-ba8b5f47ea8c2cff5916"></a>

### PrinterDelegate.printerManager(_:didFinishClosing:)

永久关闭并完成异步资源清理。

```swift
@MainActor func printerManager(_ manager: PrinterManager, didFinishClosing result: ManagerResult<Void>)
```

返回请求ID；必要状态重置和关闭结果交付后释放统一接收者。重复清理共享原屏障，不在UI线程同步等待，不承诺关闭后的历史补交。

文档 ID：`apple:symbol:s:12XLPrinterSDK15PrinterDelegatePAAE14printerManager_16didFinishClosingyAA0cF0C_AA0F6ResultVyytGtF`

符号种类：`swift.method`；访问级别：`public`。

扩展来源（保留基线原值）：

```json
{"extendedModule":"XLPrinterSDK","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| MainActor | `s:ScM` |
| ManagerResult | `s:12XLPrinterSDK13ManagerResultV` |
| PrinterManager | `s:12XLPrinterSDK14PrinterManagerC` |
| Void | `s:s4Voida` |

<a id="api-8380b742b74c62e7b94e"></a>

### PrinterDelegate.printerManager(_:didFinishConnecting:)

完成显式设备连接。

```swift
@MainActor func printerManager(_ manager: PrinterManager, didFinishConnecting result: ManagerResult<ConnectionInfo>)
```

返回请求ID，成功值为ConnectionInfo。已知名称只推荐收录型号，可用显式型号重载；可选设备信息在原FIFO有界查询一次。意外断连通过独立事件通知，不自动停扫、重连或重放。

文档 ID：`apple:symbol:s:12XLPrinterSDK15PrinterDelegateP14printerManager_19didFinishConnectingyAA0cF0C_AA0F6ResultVyAA14ConnectionInfoVGtF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| ConnectionInfo | `s:12XLPrinterSDK14ConnectionInfoV` |
| MainActor | `s:ScM` |
| ManagerResult | `s:12XLPrinterSDK13ManagerResultV` |
| PrinterManager | `s:12XLPrinterSDK14PrinterManagerC` |

<a id="api-36d74dad6d179e75c58d"></a>

### PrinterDelegate.printerManager(_:didFinishConnecting:)

完成显式设备连接。

```swift
@MainActor func printerManager(_ manager: PrinterManager, didFinishConnecting result: ManagerResult<ConnectionInfo>)
```

返回请求ID，成功值为ConnectionInfo。已知名称只推荐收录型号，可用显式型号重载；可选设备信息在原FIFO有界查询一次。意外断连通过独立事件通知，不自动停扫、重连或重放。

文档 ID：`apple:symbol:s:12XLPrinterSDK15PrinterDelegatePAAE14printerManager_19didFinishConnectingyAA0cF0C_AA0F6ResultVyAA14ConnectionInfoVGtF`

符号种类：`swift.method`；访问级别：`public`。

扩展来源（保留基线原值）：

```json
{"extendedModule":"XLPrinterSDK","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| ConnectionInfo | `s:12XLPrinterSDK14ConnectionInfoV` |
| MainActor | `s:ScM` |
| ManagerResult | `s:12XLPrinterSDK13ManagerResultV` |
| PrinterManager | `s:12XLPrinterSDK14PrinterManagerC` |

<a id="api-c62db7088039655974b0"></a>

### PrinterDelegate.printerManager(_:didFinishDisconnecting:)

完成当前连接的异步清理。

```swift
@MainActor func printerManager(_ manager: PrinterManager, didFinishDisconnecting result: ManagerResult<Void>)
```

返回请求ID；请求处理、必要状态重置和清理屏障完成后交付Void结果。接收者保留，实例可显式重新连接，不自动恢复任务。

文档 ID：`apple:symbol:s:12XLPrinterSDK15PrinterDelegateP14printerManager_22didFinishDisconnectingyAA0cF0C_AA0F6ResultVyytGtF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| MainActor | `s:ScM` |
| ManagerResult | `s:12XLPrinterSDK13ManagerResultV` |
| PrinterManager | `s:12XLPrinterSDK14PrinterManagerC` |
| Void | `s:s4Voida` |

<a id="api-847e031b2bf72a317f9a"></a>

### PrinterDelegate.printerManager(_:didFinishDisconnecting:)

完成当前连接的异步清理。

```swift
@MainActor func printerManager(_ manager: PrinterManager, didFinishDisconnecting result: ManagerResult<Void>)
```

返回请求ID；请求处理、必要状态重置和清理屏障完成后交付Void结果。接收者保留，实例可显式重新连接，不自动恢复任务。

文档 ID：`apple:symbol:s:12XLPrinterSDK15PrinterDelegatePAAE14printerManager_22didFinishDisconnectingyAA0cF0C_AA0F6ResultVyytGtF`

符号种类：`swift.method`；访问级别：`public`。

扩展来源（保留基线原值）：

```json
{"extendedModule":"XLPrinterSDK","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| MainActor | `s:ScM` |
| ManagerResult | `s:12XLPrinterSDK13ManagerResultV` |
| PrinterManager | `s:12XLPrinterSDK14PrinterManagerC` |
| Void | `s:s4Voida` |

<a id="api-5658125508c37a33bbd7"></a>

### PrinterDelegate.printerManager(_:didFinishPositioningLabel:)

完成显式下一标签定位。

```swift
@MainActor func printerManager(_ manager: PrinterManager, didFinishPositioningLabel result: ManagerResult<Void>)
```

返回请求ID并按设备配置经原FIFO执行；可能走纸，不由普通图片打印隐式追加。不支持返回稳定错误码，终态为Void结果。

文档 ID：`apple:symbol:s:12XLPrinterSDK15PrinterDelegateP14printerManager_25didFinishPositioningLabelyAA0cF0C_AA0F6ResultVyytGtF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| MainActor | `s:ScM` |
| ManagerResult | `s:12XLPrinterSDK13ManagerResultV` |
| PrinterManager | `s:12XLPrinterSDK14PrinterManagerC` |
| Void | `s:s4Voida` |

<a id="api-867b0f6d547da0f2c41d"></a>

### PrinterDelegate.printerManager(_:didFinishPositioningLabel:)

完成显式下一标签定位。

```swift
@MainActor func printerManager(_ manager: PrinterManager, didFinishPositioningLabel result: ManagerResult<Void>)
```

返回请求ID并按设备配置经原FIFO执行；可能走纸，不由普通图片打印隐式追加。不支持返回稳定错误码，终态为Void结果。

文档 ID：`apple:symbol:s:12XLPrinterSDK15PrinterDelegatePAAE14printerManager_25didFinishPositioningLabelyAA0cF0C_AA0F6ResultVyytGtF`

符号种类：`swift.method`；访问级别：`public`。

扩展来源（保留基线原值）：

```json
{"extendedModule":"XLPrinterSDK","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| MainActor | `s:ScM` |
| ManagerResult | `s:12XLPrinterSDK13ManagerResultV` |
| PrinterManager | `s:12XLPrinterSDK14PrinterManagerC` |
| Void | `s:s4Voida` |

<a id="api-4e611ed16bbbbdde2e84"></a>

### PrinterDelegate.printerManager(_:didFinishPrinting:)

交付一次图像打印请求结果。

```swift
@MainActor func printerManager(_ manager: PrinterManager, didFinishPrinting result: ManagerResult<Void>)
```

输入为Bitmap/UIImage或完整PNG/JPEG字节，页面宽高为有限正数mm；返回请求ID，后台准备及原FIFO执行。两种入口共用打印终态；保留completion与outputMayHaveOccurred，DATA_SENT不代表纸面完成。

文档 ID：`apple:symbol:s:12XLPrinterSDK15PrinterDelegateP14printerManager_17didFinishPrintingyAA0cF0C_AA0F6ResultVyytGtF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| MainActor | `s:ScM` |
| ManagerResult | `s:12XLPrinterSDK13ManagerResultV` |
| PrinterManager | `s:12XLPrinterSDK14PrinterManagerC` |
| Void | `s:s4Voida` |

<a id="api-67a62222d26b19536f6e"></a>

### PrinterDelegate.printerManager(_:didFinishPrinting:)

交付一次图像打印请求结果。

```swift
@MainActor func printerManager(_ manager: PrinterManager, didFinishPrinting result: ManagerResult<Void>)
```

输入为Bitmap/UIImage或完整PNG/JPEG字节，页面宽高为有限正数mm；返回请求ID，后台准备及原FIFO执行。两种入口共用打印终态；保留completion与outputMayHaveOccurred，DATA_SENT不代表纸面完成。

文档 ID：`apple:symbol:s:12XLPrinterSDK15PrinterDelegatePAAE14printerManager_17didFinishPrintingyAA0cF0C_AA0F6ResultVyytGtF`

符号种类：`swift.method`；访问级别：`public`。

扩展来源（保留基线原值）：

```json
{"extendedModule":"XLPrinterSDK","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| MainActor | `s:ScM` |
| ManagerResult | `s:12XLPrinterSDK13ManagerResultV` |
| PrinterManager | `s:12XLPrinterSDK14PrinterManagerC` |
| Void | `s:s4Voida` |

<a id="api-bca22ed92165297b23c5"></a>

### PrinterDelegate.printerManager(_:didFinishQueryingStatus:)

完成一次主动状态查询。

```swift
@MainActor func printerManager(_ manager: PrinterManager, didFinishQueryingStatus result: ManagerResult<Void>)
```

通过原FIFO发命令并验证本次应答，结果为Void；状态值只从统一状态事件交付。同值可能不再通知状态，所以不能等待下一次状态事件来结束请求。

文档 ID：`apple:symbol:s:12XLPrinterSDK15PrinterDelegateP14printerManager_23didFinishQueryingStatusyAA0cF0C_AA0F6ResultVyytGtF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| MainActor | `s:ScM` |
| ManagerResult | `s:12XLPrinterSDK13ManagerResultV` |
| PrinterManager | `s:12XLPrinterSDK14PrinterManagerC` |
| Void | `s:s4Voida` |

<a id="api-4f7b382a47aa881c4e7e"></a>

### PrinterDelegate.printerManager(_:didFinishQueryingStatus:)

完成一次主动状态查询。

```swift
@MainActor func printerManager(_ manager: PrinterManager, didFinishQueryingStatus result: ManagerResult<Void>)
```

通过原FIFO发命令并验证本次应答，结果为Void；状态值只从统一状态事件交付。同值可能不再通知状态，所以不能等待下一次状态事件来结束请求。

文档 ID：`apple:symbol:s:12XLPrinterSDK15PrinterDelegatePAAE14printerManager_23didFinishQueryingStatusyAA0cF0C_AA0F6ResultVyytGtF`

符号种类：`swift.method`；访问级别：`public`。

扩展来源（保留基线原值）：

```json
{"extendedModule":"XLPrinterSDK","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| MainActor | `s:ScM` |
| ManagerResult | `s:12XLPrinterSDK13ManagerResultV` |
| PrinterManager | `s:12XLPrinterSDK14PrinterManagerC` |
| Void | `s:s4Voida` |

<a id="api-d118354595a4bc3e19a2"></a>

### PrinterDelegate.printerManager(_:didFinishReadingCapabilities:)

读取当前能力描述。

```swift
@MainActor func printerManager(_ manager: PrinterManager, didFinishReadingCapabilities result: ManagerResult<PrinterCapabilities>)
```

返回PrinterCapabilities，支持度使用未知/不支持/支持三态；未知范围为空。查询和上报分别描述，能力不等于硬件认证。

文档 ID：`apple:symbol:s:12XLPrinterSDK15PrinterDelegateP14printerManager_28didFinishReadingCapabilitiesyAA0cF0C_AA0F6ResultVyAA0cJ0VGtF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| MainActor | `s:ScM` |
| ManagerResult | `s:12XLPrinterSDK13ManagerResultV` |
| PrinterCapabilities | `s:12XLPrinterSDK19PrinterCapabilitiesV` |
| PrinterManager | `s:12XLPrinterSDK14PrinterManagerC` |

<a id="api-0467f66d9a6994562d66"></a>

### PrinterDelegate.printerManager(_:didFinishReadingCapabilities:)

读取当前能力描述。

```swift
@MainActor func printerManager(_ manager: PrinterManager, didFinishReadingCapabilities result: ManagerResult<PrinterCapabilities>)
```

返回PrinterCapabilities，支持度使用未知/不支持/支持三态；未知范围为空。查询和上报分别描述，能力不等于硬件认证。

文档 ID：`apple:symbol:s:12XLPrinterSDK15PrinterDelegatePAAE14printerManager_28didFinishReadingCapabilitiesyAA0cF0C_AA0F6ResultVyAA0cJ0VGtF`

符号种类：`swift.method`；访问级别：`public`。

扩展来源（保留基线原值）：

```json
{"extendedModule":"XLPrinterSDK","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| MainActor | `s:ScM` |
| ManagerResult | `s:12XLPrinterSDK13ManagerResultV` |
| PrinterCapabilities | `s:12XLPrinterSDK19PrinterCapabilitiesV` |
| PrinterManager | `s:12XLPrinterSDK14PrinterManagerC` |

<a id="api-a21cc1667004f9731594"></a>

### PrinterDelegate.printerManager(_:didFinishReadingSDKInfo:)

读取实际SDK身份信息。

```swift
@MainActor func printerManager(_ manager: PrinterManager, didFinishReadingSDKInfo result: ManagerResult<SDKInfo>)
```

返回SdkInfo或SDKInfo，包括原生版本、分发版本和可空构建标识；不是打印机固件版本，不执行打印机IO。

文档 ID：`apple:symbol:s:12XLPrinterSDK15PrinterDelegateP14printerManager_23didFinishReadingSDKInfoyAA0cF0C_AA0F6ResultVyAA0J0VGtF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| MainActor | `s:ScM` |
| ManagerResult | `s:12XLPrinterSDK13ManagerResultV` |
| PrinterManager | `s:12XLPrinterSDK14PrinterManagerC` |
| SDKInfo | `s:12XLPrinterSDK7SDKInfoV` |

<a id="api-80108dc4648d59bebfa6"></a>

### PrinterDelegate.printerManager(_:didFinishReadingSDKInfo:)

读取实际SDK身份信息。

```swift
@MainActor func printerManager(_ manager: PrinterManager, didFinishReadingSDKInfo result: ManagerResult<SDKInfo>)
```

返回SdkInfo或SDKInfo，包括原生版本、分发版本和可空构建标识；不是打印机固件版本，不执行打印机IO。

文档 ID：`apple:symbol:s:12XLPrinterSDK15PrinterDelegatePAAE14printerManager_23didFinishReadingSDKInfoyAA0cF0C_AA0F6ResultVyAA0J0VGtF`

符号种类：`swift.method`；访问级别：`public`。

扩展来源（保留基线原值）：

```json
{"extendedModule":"XLPrinterSDK","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| MainActor | `s:ScM` |
| ManagerResult | `s:12XLPrinterSDK13ManagerResultV` |
| PrinterManager | `s:12XLPrinterSDK14PrinterManagerC` |
| SDKInfo | `s:12XLPrinterSDK7SDKInfoV` |

<a id="api-1bb0d5dbabd4ba16b6ac"></a>

### PrinterDelegate.printerManager(_:didFinishScanning:)

完成本次有限窗口BLE扫描。

```swift
@MainActor func printerManager(_ manager: PrinterManager, didFinishScanning result: ManagerResult<[DiscoveredPrinter]>)
```

立即返回原有请求ID，终态经统一接收接口交付；成功值为最新合并候选列表。实时首见事件另行提供。失败或取消无部分成功列表，不自动连接设备。

文档 ID：`apple:symbol:s:12XLPrinterSDK15PrinterDelegateP14printerManager_17didFinishScanningyAA0cF0C_AA0F6ResultVySayAA010DiscoveredC0VGGtF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| DiscoveredPrinter | `s:12XLPrinterSDK17DiscoveredPrinterV` |
| MainActor | `s:ScM` |
| ManagerResult | `s:12XLPrinterSDK13ManagerResultV` |
| PrinterManager | `s:12XLPrinterSDK14PrinterManagerC` |

<a id="api-a63dc46dd784103fbc1f"></a>

### PrinterDelegate.printerManager(_:didFinishScanning:)

完成本次有限窗口BLE扫描。

```swift
@MainActor func printerManager(_ manager: PrinterManager, didFinishScanning result: ManagerResult<[DiscoveredPrinter]>)
```

立即返回原有请求ID，终态经统一接收接口交付；成功值为最新合并候选列表。实时首见事件另行提供。失败或取消无部分成功列表，不自动连接设备。

文档 ID：`apple:symbol:s:12XLPrinterSDK15PrinterDelegatePAAE14printerManager_17didFinishScanningyAA0cF0C_AA0F6ResultVySayAA010DiscoveredC0VGGtF`

符号种类：`swift.method`；访问级别：`public`。

扩展来源（保留基线原值）：

```json
{"extendedModule":"XLPrinterSDK","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| DiscoveredPrinter | `s:12XLPrinterSDK17DiscoveredPrinterV` |
| MainActor | `s:ScM` |
| ManagerResult | `s:12XLPrinterSDK13ManagerResultV` |
| PrinterManager | `s:12XLPrinterSDK14PrinterManagerC` |

<a id="api-9279a7b72a21cf1cfb39"></a>

### PrinterDelegate.printerManager(_:didFinishSendingRaw:)

完成一次原始命令发送。

```swift
@MainActor func printerManager(_ manager: PrinterManager, didFinishSendingRaw result: ManagerResult<Void>)
```

非空原始字节上限1MiB，固定发送一次，仍使用设备原FIFO；不解析命令语义、不绕过队列。保留部分输出和完成级别，不自动重试。

文档 ID：`apple:symbol:s:12XLPrinterSDK15PrinterDelegateP14printerManager_19didFinishSendingRawyAA0cF0C_AA0F6ResultVyytGtF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| MainActor | `s:ScM` |
| ManagerResult | `s:12XLPrinterSDK13ManagerResultV` |
| PrinterManager | `s:12XLPrinterSDK14PrinterManagerC` |
| Void | `s:s4Voida` |

<a id="api-a4c473d7e86aa55d55b8"></a>

### PrinterDelegate.printerManager(_:didFinishSendingRaw:)

完成一次原始命令发送。

```swift
@MainActor func printerManager(_ manager: PrinterManager, didFinishSendingRaw result: ManagerResult<Void>)
```

非空原始字节上限1MiB，固定发送一次，仍使用设备原FIFO；不解析命令语义、不绕过队列。保留部分输出和完成级别，不自动重试。

文档 ID：`apple:symbol:s:12XLPrinterSDK15PrinterDelegatePAAE14printerManager_19didFinishSendingRawyAA0cF0C_AA0F6ResultVyytGtF`

符号种类：`swift.method`；访问级别：`public`。

扩展来源（保留基线原值）：

```json
{"extendedModule":"XLPrinterSDK","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| MainActor | `s:ScM` |
| ManagerResult | `s:12XLPrinterSDK13ManagerResultV` |
| PrinterManager | `s:12XLPrinterSDK14PrinterManagerC` |
| Void | `s:s4Voida` |

<a id="api-692519253923679de436"></a>

### PrinterDelegate.printerManager(_:didFinishSettingPaperType:)

完成显式纸张定位类型设置。

```swift
@MainActor func printerManager(_ manager: PrinterManager, didFinishSettingPaperType result: ManagerResult<Void>)
```

这是独立设备FIFO请求，不由连接、页面尺寸或打印隐式触发；不自动校准或持久化。返回Void结果，保留错误码和完成证据。

文档 ID：`apple:symbol:s:12XLPrinterSDK15PrinterDelegateP14printerManager_25didFinishSettingPaperTypeyAA0cF0C_AA0F6ResultVyytGtF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| MainActor | `s:ScM` |
| ManagerResult | `s:12XLPrinterSDK13ManagerResultV` |
| PrinterManager | `s:12XLPrinterSDK14PrinterManagerC` |
| Void | `s:s4Voida` |

<a id="api-af73c31d92d419701f5f"></a>

### PrinterDelegate.printerManager(_:didFinishSettingPaperType:)

完成显式纸张定位类型设置。

```swift
@MainActor func printerManager(_ manager: PrinterManager, didFinishSettingPaperType result: ManagerResult<Void>)
```

这是独立设备FIFO请求，不由连接、页面尺寸或打印隐式触发；不自动校准或持久化。返回Void结果，保留错误码和完成证据。

文档 ID：`apple:symbol:s:12XLPrinterSDK15PrinterDelegatePAAE14printerManager_25didFinishSettingPaperTypeyAA0cF0C_AA0F6ResultVyytGtF`

符号种类：`swift.method`；访问级别：`public`。

扩展来源（保留基线原值）：

```json
{"extendedModule":"XLPrinterSDK","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| MainActor | `s:ScM` |
| ManagerResult | `s:12XLPrinterSDK13ManagerResultV` |
| PrinterManager | `s:12XLPrinterSDK14PrinterManagerC` |
| Void | `s:s4Voida` |

<a id="api-1de1a97646a7adbb6208"></a>

### PrinterDelegate.printerManager(_:didFinishSettingPrinterModel:)

更新连接采用的内置型号配置。

```swift
@MainActor func printerManager(_ manager: PrinterManager, didFinishSettingPrinterModel result: ManagerResult<ConnectionInfo>)
```

仅连接空闲时允许；成功返回更新的ConnectionInfo并失效介质证据，失败保留原配置。不重新执行设备信息查询。结果与连接完成使用不同方法。

文档 ID：`apple:symbol:s:12XLPrinterSDK15PrinterDelegateP14printerManager_016didFinishSettingC5ModelyAA0cF0C_AA0F6ResultVyAA14ConnectionInfoVGtF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| ConnectionInfo | `s:12XLPrinterSDK14ConnectionInfoV` |
| MainActor | `s:ScM` |
| ManagerResult | `s:12XLPrinterSDK13ManagerResultV` |
| PrinterManager | `s:12XLPrinterSDK14PrinterManagerC` |

<a id="api-90928209e2a0498aeaee"></a>

### PrinterDelegate.printerManager(_:didFinishSettingPrinterModel:)

更新连接采用的内置型号配置。

```swift
@MainActor func printerManager(_ manager: PrinterManager, didFinishSettingPrinterModel result: ManagerResult<ConnectionInfo>)
```

仅连接空闲时允许；成功返回更新的ConnectionInfo并失效介质证据，失败保留原配置。不重新执行设备信息查询。结果与连接完成使用不同方法。

文档 ID：`apple:symbol:s:12XLPrinterSDK15PrinterDelegatePAAE14printerManager_016didFinishSettingC5ModelyAA0cF0C_AA0F6ResultVyAA14ConnectionInfoVGtF`

符号种类：`swift.method`；访问级别：`public`。

扩展来源（保留基线原值）：

```json
{"extendedModule":"XLPrinterSDK","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| ConnectionInfo | `s:12XLPrinterSDK14ConnectionInfoV` |
| MainActor | `s:ScM` |
| ManagerResult | `s:12XLPrinterSDK13ManagerResultV` |
| PrinterManager | `s:12XLPrinterSDK14PrinterManagerC` |

<a id="api-9b1325e1a23548108165"></a>

### PrinterDelegate.printerManager(_:didLoseConnection:)

通知本连接的意外断开。

```swift
@MainActor func printerManager(_ manager: PrinterManager, didLoseConnection result: ManagerResult<Void>)
```

关联原成功连接的请求ID，每连接最多一次；主动断开和关闭走自己的完成方法。注册代次失效不把旧连接事件改投新接收者，不自动重连或重放。

文档 ID：`apple:symbol:s:12XLPrinterSDK15PrinterDelegateP14printerManager_17didLoseConnectionyAA0cF0C_AA0F6ResultVyytGtF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| MainActor | `s:ScM` |
| ManagerResult | `s:12XLPrinterSDK13ManagerResultV` |
| PrinterManager | `s:12XLPrinterSDK14PrinterManagerC` |
| Void | `s:s4Voida` |

<a id="api-ec245340afec058b68b9"></a>

### PrinterDelegate.printerManager(_:didLoseConnection:)

通知本连接的意外断开。

```swift
@MainActor func printerManager(_ manager: PrinterManager, didLoseConnection result: ManagerResult<Void>)
```

关联原成功连接的请求ID，每连接最多一次；主动断开和关闭走自己的完成方法。注册代次失效不把旧连接事件改投新接收者，不自动重连或重放。

文档 ID：`apple:symbol:s:12XLPrinterSDK15PrinterDelegatePAAE14printerManager_17didLoseConnectionyAA0cF0C_AA0F6ResultVyytGtF`

符号种类：`swift.method`；访问级别：`public`。

扩展来源（保留基线原值）：

```json
{"extendedModule":"XLPrinterSDK","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| MainActor | `s:ScM` |
| ManagerResult | `s:12XLPrinterSDK13ManagerResultV` |
| PrinterManager | `s:12XLPrinterSDK14PrinterManagerC` |
| Void | `s:s4Voida` |

<a id="api-b99ce56d12f3ce97a07a"></a>

### PrinterDelegate.printerManager(_:didUpdateStatus:)

通知当前设备状态快照。

```swift
@MainActor func printerManager(_ manager: PrinterManager, didUpdateStatus status: StatusResult)
```

注册时先异步交付当前快照；无证据时四项未知。连接成功通知执行后发布初始化样本，断开或换型号重置，重复状态可合并。不是某次getStatus的完成通知。

文档 ID：`apple:symbol:s:12XLPrinterSDK15PrinterDelegateP14printerManager_15didUpdateStatusyAA0cF0C_AA0I6ResultVtF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| MainActor | `s:ScM` |
| PrinterManager | `s:12XLPrinterSDK14PrinterManagerC` |
| StatusResult | `s:12XLPrinterSDK12StatusResultV` |

<a id="api-1704c6c880278ce5afff"></a>

### PrinterDelegate.printerManager(_:didUpdateStatus:)

通知当前设备状态快照。

```swift
@MainActor func printerManager(_ manager: PrinterManager, didUpdateStatus status: StatusResult)
```

注册时先异步交付当前快照；无证据时四项未知。连接成功通知执行后发布初始化样本，断开或换型号重置，重复状态可合并。不是某次getStatus的完成通知。

文档 ID：`apple:symbol:s:12XLPrinterSDK15PrinterDelegatePAAE14printerManager_15didUpdateStatusyAA0cF0C_AA0I6ResultVtF`

符号种类：`swift.method`；访问级别：`public`。

扩展来源（保留基线原值）：

```json
{"extendedModule":"XLPrinterSDK","typeKind":"swift.protocol"}
```

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| MainActor | `s:ScM` |
| PrinterManager | `s:12XLPrinterSDK14PrinterManagerC` |
| StatusResult | `s:12XLPrinterSDK12StatusResultV` |

<a id="api-0e885a04f27772f30231"></a>

## PrinterInfo

<a id="api-bb8a22ce0ab8cf1e96e6"></a>

### PrinterInfo

打印机信息查询的不可变设备观察值。

```swift
struct PrinterInfo
```

名称、电量、温度和能力标记来自JXIG候选回包，未知值为空。所有结果保留protocolVerified=false；不改变已采用型号，不将文档版本当作固件、UID当作序列号，也不推断实际打印完成。

文档 ID：`apple:symbol:s:12XLPrinterSDK11PrinterInfoV`

符号种类：`swift.struct`；访问级别：`public`。

<a id="api-bbe6c4e1201e92917ba0"></a>

### PrinterInfo.!=(_:_:)

比较两份打印机信息值是否不同。

```swift
static func != (lhs: borrowing Self, rhs: borrowing Self) -> Bool
```

Swift的Equatable协议合成比较；只比较值，不发送设备指令，不将字段变化解释为新设备或打印任务完成。

文档 ID：`apple:symbol:s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:12XLPrinterSDK11PrinterInfoV`

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

<a id="api-196022f1256d7507ac79"></a>

### PrinterInfo.batteryPercent

可选的电池电量百分比。

```swift
let batteryPercent: Int32?
```

仅接受文档的1至100；0或其他未知值为空。电源供电标记0xff通过externalPower单独表示，不转换为百分比。

文档 ID：`apple:symbol:s:12XLPrinterSDK11PrinterInfoV14batteryPercents5Int32VSgvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Int32 | `s:s5Int32V` |

<a id="api-1217d499cdf9617cfdc4"></a>

### PrinterInfo.densityLevel

可选的设备浓度等级读回。

```swift
let densityLevel: Int32?
```

保留文档的0至15，其他值为空；本轮仅查询，不提供浓度设置或持久化操作。

文档 ID：`apple:symbol:s:12XLPrinterSDK11PrinterInfoV12densityLevels5Int32VSgvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Int32 | `s:s5Int32V` |

<a id="api-052b24d01c092e37c527"></a>

### PrinterInfo.deviceName

打印机回报的设备名称。

```swift
let deviceName: String?
```

仅接收可打印ASCII文本；它不是已认证型号，也不会改变当前采用的打印型号或几何配置。

文档 ID：`apple:symbol:s:12XLPrinterSDK11PrinterInfoV10deviceNameSSSgvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| String | `s:SS` |

<a id="api-1781cf371e094da21168"></a>

### PrinterInfo.externalPower

可选的外部电源供电标记。

```swift
let externalPower: Bool?
```

0xff映射为真，已知电池电量映射为假；其他值为空，不以缺少电量数据推断电源状态。

文档 ID：`apple:symbol:s:12XLPrinterSDK11PrinterInfoV13externalPowerSbSgvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Bool | `s:Sb` |

<a id="api-a5a0bf33a2ed69b34fdf"></a>

### PrinterInfo.hasError

可选的粗粒度打印机异常标记。

```swift
let hasError: Bool?
```

0映射为假、1映射为真，其他值为空；不能区分缺纸、开盖等故障，也不能用于判定机械打印完成。

文档 ID：`apple:symbol:s:12XLPrinterSDK11PrinterInfoV8hasErrorSbSgvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Bool | `s:Sb` |

<a id="api-436815011780f305ca1d"></a>

### PrinterInfo.manufacturer

打印机回报的厂商名称。

```swift
let manufacturer: String?
```

仅接收字段中的可打印ASCII文本；编码未知或字段为空时为未知，不使用发现名称代填。

文档 ID：`apple:symbol:s:12XLPrinterSDK11PrinterInfoV12manufacturerSSSgvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| String | `s:SS` |

<a id="api-4b63c5fc677fcd901f34"></a>

### PrinterInfo.maxPrintWidthDots

打印机回报的一行最大点数。

```swift
let maxPrintWidthDots: UInt32?
```

非零值以点为单位保留，零值视为未知；信息查询不会自动修改已采用型号的打印宽度。

文档 ID：`apple:symbol:s:12XLPrinterSDK11PrinterInfoV17maxPrintWidthDotss6UInt32VSgvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-b468abd66cf8ad26a5cb"></a>

### PrinterInfo.paperTypeCode

可选的设备纸张类型原始代码。

```swift
let paperTypeCode: Int32?
```

候选值0为连续纸、1为标签纸、2为黑标纸、3为纹身纸；其他值为空。它不自动设置纸型或扩展公共PaperType枚举。

文档 ID：`apple:symbol:s:12XLPrinterSDK11PrinterInfoV13paperTypeCodes5Int32VSgvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Int32 | `s:s5Int32V` |

<a id="api-98ca718677268bb737ff"></a>

### PrinterInfo.printheadTemperatureCelsius

可选的打印头温度。

```swift
let printheadTemperatureCelsius: Int32?
```

按有符号字节解释为摄氏度，仅保留文档的-40至85范围；超出范围为未知，不代替过热故障查询。

文档 ID：`apple:symbol:s:12XLPrinterSDK11PrinterInfoV27printheadTemperatureCelsiuss5Int32VSgvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Int32 | `s:s5Int32V` |

<a id="api-ab3bb242ec21773e4e3c"></a>

### PrinterInfo.protocolVerified

候选报文格式是否通过硬件验证。

```swift
let protocolVerified: Bool
```

当前始终为假；通过候选长度与校验检查不等于实际LT-M90报文及字段含义已验证，也不等于打印完成。

文档 ID：`apple:symbol:s:12XLPrinterSDK11PrinterInfoV16protocolVerifiedSbvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Bool | `s:Sb` |

<a id="api-c9427f9f600810b601da"></a>

### PrinterInfo.reportedDpi

打印机回报的文档DPI类型。

```swift
let reportedDpi: UInt32?
```

候选映射为200、300或600；未知类型为空。它不替代LT-M90型号使用的精确8 dots/mm几何依据。

文档 ID：`apple:symbol:s:12XLPrinterSDK11PrinterInfoV11reportedDpis6UInt32VSgvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-5b8fcfad7ccb0bdd420f"></a>

### PrinterInfo.reportedVersion

文档中的原始版本数值。

```swift
let reportedVersion: UInt32?
```

按候选小端格式解释为0至65535的无符号值；文档没有确认其是固件版本，不据此推导固件身份。

文档 ID：`apple:symbol:s:12XLPrinterSDK11PrinterInfoV15reportedVersions6UInt32VSgvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| UInt32 | `s:s6UInt32V` |

<a id="api-a475b631991894e69a0e"></a>

### PrinterInfo.speedLevel

可选的设备速度等级读回。

```swift
let speedLevel: Int32?
```

候选保留0至4，其他值为空；文档对具体机型为三档或五档尚无确定解释，不从此值建立可写设置能力。

文档 ID：`apple:symbol:s:12XLPrinterSDK11PrinterInfoV10speedLevels5Int32VSgvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Int32 | `s:s5Int32V` |

<a id="api-46d1a26fe78e787ac665"></a>

### PrinterInfo.supportsCompression

可选的设备压缩支持标记。

```swift
let supportsCompression: Bool?
```

只将0和1映射为假和真；其他值为空。没有对应压缩算法协议，标记不启用压缩打印。

文档 ID：`apple:symbol:s:12XLPrinterSDK11PrinterInfoV19supportsCompressionSbSgvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Bool | `s:Sb` |

<a id="api-fa6d2129e65839130a49"></a>

### PrinterInfo.supportsNfc

可选的设备NFC能力标记。

```swift
let supportsNfc: Bool?
```

只将0和1映射为假和真；其他值为空。NFC剩余数量存在类型与长度冲突，本轮不公开数量或纸张尺寸。

文档 ID：`apple:symbol:s:12XLPrinterSDK11PrinterInfoV11supportsNfcSbSgvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Bool | `s:Sb` |

<a id="api-16a637d8e558fa390f9c"></a>

### PrinterInfo.supportsRtc

可选的设备RTC能力标记。

```swift
let supportsRtc: Bool?
```

只将0和1映射为假和真；其他值为空。标记不代表本轮提供设置时间指令，也不是功能已完成硬件验收的证明。

文档 ID：`apple:symbol:s:12XLPrinterSDK11PrinterInfoV11supportsRtcSbSgvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Bool | `s:Sb` |

<a id="api-7b9c29ffe582edb300c2"></a>

### PrinterInfo.uidHex

打印机回报的12字节UID。

```swift
let uidHex: String?
```

以完整24位小写十六进制保留，包括零字节；UID不被认定为产品序列号，未知时为空。

文档 ID：`apple:symbol:s:12XLPrinterSDK11PrinterInfoV6uidHexSSSgvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| String | `s:SS` |

<a id="api-1d5ad0ccf40ef0036743"></a>

## PrinterManager

<a id="api-e5d97f4ecabba15263ef"></a>

### PrinterManager

统一移动端请求与事件接口。

```swift
final class PrinterManager
```

每个Manager同时控制一台设备，公开动作返回关联请求ID，统一接收接口在主线程锁外交付强类型结果。内部每请求终态一次；接收者失效不重放通知，所有设备IO仍经原执行器。

文档 ID：`apple:symbol:s:12XLPrinterSDK14PrinterManagerC`

符号种类：`swift.class`；访问级别：`public`。

<a id="api-8a06bdb53dc49089016e"></a>

### PrinterManager.cancel(_:)

请求取消所属Manager的原操作。

```swift
func cancel(_ requestID: RequestID)
```

同步发送控制信号，不创建新请求。最终结果回到原动作的完成方法，已结束、外部或清理请求ID为空操作。不能撤回已发送字节，不将取消意图覆盖真实成功或部分输出证据。

文档 ID：`apple:symbol:s:12XLPrinterSDK14PrinterManagerC6cancelyyAA9RequestIDVF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| RequestID | `s:12XLPrinterSDK9RequestIDV` |

<a id="api-72d8611257c84406d72d"></a>

### PrinterManager.capabilities()

读取当前能力描述。

```swift
@discardableResult func capabilities() -> RequestID
```

返回PrinterCapabilities，支持度使用未知/不支持/支持三态；未知范围为空。查询和上报分别描述，能力不等于硬件认证。

文档 ID：`apple:symbol:s:12XLPrinterSDK14PrinterManagerC12capabilitiesAA9RequestIDVyF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| RequestID | `s:12XLPrinterSDK9RequestIDV` |

<a id="api-3318aaf7e78267ccb9fe"></a>

### PrinterManager.close()

永久关闭并完成异步资源清理。

```swift
@discardableResult func close() -> RequestID
```

返回请求ID；必要状态重置和关闭结果交付后释放统一接收者。重复清理共享原屏障，不在UI线程同步等待，不承诺关闭后的历史补交。

文档 ID：`apple:symbol:s:12XLPrinterSDK14PrinterManagerC5closeAA9RequestIDVyF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| RequestID | `s:12XLPrinterSDK9RequestIDV` |

<a id="api-478ef9d0f3652827e324"></a>

### PrinterManager.connectBle(device:)

完成显式设备连接。

```swift
@discardableResult func connectBle(device: DiscoveredPrinter) -> RequestID
```

返回请求ID，成功值为ConnectionInfo。已知名称只推荐收录型号，可用显式型号重载；可选设备信息在原FIFO有界查询一次。意外断连通过独立事件通知，不自动停扫、重连或重放。

文档 ID：`apple:symbol:s:12XLPrinterSDK14PrinterManagerC10connectBle6deviceAA9RequestIDVAA010DiscoveredC0V_tF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| DiscoveredPrinter | `s:12XLPrinterSDK17DiscoveredPrinterV` |
| RequestID | `s:12XLPrinterSDK9RequestIDV` |

<a id="api-a67fd023b6bec20168f7"></a>

### PrinterManager.connectBle(device:model:)

完成显式设备连接。

```swift
@discardableResult func connectBle(device: DiscoveredPrinter, model: PrinterModel) -> RequestID
```

返回请求ID，成功值为ConnectionInfo。已知名称只推荐收录型号，可用显式型号重载；可选设备信息在原FIFO有界查询一次。意外断连通过独立事件通知，不自动停扫、重连或重放。

文档 ID：`apple:symbol:s:12XLPrinterSDK14PrinterManagerC10connectBle6device5modelAA9RequestIDVAA010DiscoveredC0V_AA0C5ModelOtF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| DiscoveredPrinter | `s:12XLPrinterSDK17DiscoveredPrinterV` |
| PrinterModel | `s:12XLPrinterSDK12PrinterModelO` |
| RequestID | `s:12XLPrinterSDK9RequestIDV` |

<a id="api-194b80712c938304f753"></a>

### PrinterManager.delegate

注册或解除统一结果接收者。

```swift
var delegate: (any PrinterDelegate)? { get set }
```

Android持有当前Listener，Swift弱引用Delegate。注册代次与请求绑定；替换或解除抑制旧代次尚未进入的通知，不改投新对象。内部终态和资源回收继续执行，不隐式取消请求。

文档 ID：`apple:symbol:s:12XLPrinterSDK14PrinterManagerC8delegateAA0C8Delegate_pSgvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| PrinterDelegate | `s:12XLPrinterSDK15PrinterDelegateP` |

<a id="api-826b521ae121ade1a609"></a>

### PrinterManager.disconnect()

完成当前连接的异步清理。

```swift
@discardableResult func disconnect() -> RequestID
```

返回请求ID；请求处理、必要状态重置和清理屏障完成后交付Void结果。接收者保留，实例可显式重新连接，不自动恢复任务。

文档 ID：`apple:symbol:s:12XLPrinterSDK14PrinterManagerC10disconnectAA9RequestIDVyF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| RequestID | `s:12XLPrinterSDK9RequestIDV` |

<a id="api-91a0c9ce052d29464724"></a>

### PrinterManager.getStatus()

完成一次主动状态查询。

```swift
@discardableResult func getStatus() -> RequestID
```

通过原FIFO发命令并验证本次应答，结果为Void；状态值只从统一状态事件交付。同值可能不再通知状态，所以不能等待下一次状态事件来结束请求。

文档 ID：`apple:symbol:s:12XLPrinterSDK14PrinterManagerC9getStatusAA9RequestIDVyF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| RequestID | `s:12XLPrinterSDK9RequestIDV` |

<a id="api-cbe283d2cdcfb5b817b6"></a>

### PrinterManager.init()

统一移动端请求与事件接口。

```swift
init()
```

每个Manager同时控制一台设备，公开动作返回关联请求ID，统一接收接口在主线程锁外交付强类型结果。内部每请求终态一次；接收者失效不重放通知，所有设备IO仍经原执行器。

文档 ID：`apple:symbol:s:12XLPrinterSDK14PrinterManagerCACycfc`

符号种类：`swift.init`；访问级别：`public`。

<a id="api-1c0a7c2558d13ba2d122"></a>

### PrinterManager.locateNextLabel()

完成显式下一标签定位。

```swift
@discardableResult func locateNextLabel() -> RequestID
```

返回请求ID并按设备配置经原FIFO执行；可能走纸，不由普通图片打印隐式追加。不支持返回稳定错误码，终态为Void结果。

文档 ID：`apple:symbol:s:12XLPrinterSDK14PrinterManagerC15locateNextLabelAA9RequestIDVyF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| RequestID | `s:12XLPrinterSDK9RequestIDV` |

<a id="api-a8bb790e34b8db845526"></a>

### PrinterManager.printEncodedImage(_:widthMm:heightMm:)

交付一次图像打印请求结果。

```swift
@discardableResult func printEncodedImage(_ bytes: Data, widthMm: Double, heightMm: Double) -> RequestID
```

输入为Bitmap/UIImage或完整PNG/JPEG字节，页面宽高为有限正数mm；返回请求ID，后台准备及原FIFO执行。两种入口共用打印终态；保留completion与outputMayHaveOccurred，DATA_SENT不代表纸面完成。

文档 ID：`apple:symbol:s:12XLPrinterSDK14PrinterManagerC17printEncodedImage_7widthMm06heightI0AA9RequestIDV10Foundation4DataV_S2dtF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Data | `s:10Foundation4DataV` |
| Double | `s:Sd` |
| RequestID | `s:12XLPrinterSDK9RequestIDV` |

<a id="api-b87708b088d6004f5efb"></a>

### PrinterManager.printImage(_:widthMm:heightMm:)

交付一次图像打印请求结果。

```swift
@discardableResult func printImage(_ image: UIImage, widthMm: Double, heightMm: Double) -> RequestID
```

输入为Bitmap/UIImage或完整PNG/JPEG字节，页面宽高为有限正数mm；返回请求ID，后台准备及原FIFO执行。两种入口共用打印终态；保留completion与outputMayHaveOccurred，DATA_SENT不代表纸面完成。

文档 ID：`apple:symbol:s:12XLPrinterSDK14PrinterManagerC10printImage_7widthMm06heightH0AA9RequestIDVSo7UIImageC_S2dtF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Double | `s:Sd` |
| RequestID | `s:12XLPrinterSDK9RequestIDV` |
| UIImage | `c:objc(cs)UIImage` |

<a id="api-fc442aaa683638a369a9"></a>

### PrinterManager.scanBLE()

完成本次有限窗口BLE扫描。

```swift
@discardableResult func scanBLE() -> RequestID
```

立即返回原有请求ID，终态经统一接收接口交付；成功值为最新合并候选列表。实时首见事件另行提供。失败或取消无部分成功列表，不自动连接设备。

文档 ID：`apple:symbol:s:12XLPrinterSDK14PrinterManagerC7scanBLEAA9RequestIDVyF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| RequestID | `s:12XLPrinterSDK9RequestIDV` |

<a id="api-2bc898f317608cd5279a"></a>

### PrinterManager.sdkInfo()

读取实际SDK身份信息。

```swift
@discardableResult func sdkInfo() -> RequestID
```

返回SdkInfo或SDKInfo，包括原生版本、分发版本和可空构建标识；不是打印机固件版本，不执行打印机IO。

文档 ID：`apple:symbol:s:12XLPrinterSDK14PrinterManagerC7sdkInfoAA9RequestIDVyF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| RequestID | `s:12XLPrinterSDK9RequestIDV` |

<a id="api-7b4af02848c3ad4651d9"></a>

### PrinterManager.sendRaw(_:)

完成一次原始命令发送。

```swift
@discardableResult func sendRaw(_ bytes: Data) -> RequestID
```

非空原始字节上限1MiB，固定发送一次，仍使用设备原FIFO；不解析命令语义、不绕过队列。保留部分输出和完成级别，不自动重试。

文档 ID：`apple:symbol:s:12XLPrinterSDK14PrinterManagerC7sendRawyAA9RequestIDV10Foundation4DataVF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Data | `s:10Foundation4DataV` |
| RequestID | `s:12XLPrinterSDK9RequestIDV` |

<a id="api-57bcf6e7910d2030bc4d"></a>

### PrinterManager.setPaperType(_:)

完成显式纸张定位类型设置。

```swift
@discardableResult func setPaperType(_ type: PaperType) -> RequestID
```

这是独立设备FIFO请求，不由连接、页面尺寸或打印隐式触发；不自动校准或持久化。返回Void结果，保留错误码和完成证据。

文档 ID：`apple:symbol:s:12XLPrinterSDK14PrinterManagerC12setPaperTypeyAA9RequestIDVAA0fG0OF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| PaperType | `s:12XLPrinterSDK9PaperTypeO` |
| RequestID | `s:12XLPrinterSDK9RequestIDV` |

<a id="api-047f05c36cbd29ab7c3a"></a>

### PrinterManager.setPrinterModel(_:)

更新连接采用的内置型号配置。

```swift
@discardableResult func setPrinterModel(_ model: PrinterModel) -> RequestID
```

仅连接空闲时允许；成功返回更新的ConnectionInfo并失效介质证据，失败保留原配置。不重新执行设备信息查询。结果与连接完成使用不同方法。

文档 ID：`apple:symbol:s:12XLPrinterSDK14PrinterManagerC03setC5ModelyAA9RequestIDVAA0cF0OF`

符号种类：`swift.method`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| PrinterModel | `s:12XLPrinterSDK12PrinterModelO` |
| RequestID | `s:12XLPrinterSDK9RequestIDV` |

<a id="api-3f9717fd53ac6f2b1f4b"></a>

## PrinterModel

<a id="api-246c3e95dacece3d0a38"></a>

### PrinterModel

SDK已经收录的打印机型号。

```swift
enum PrinterModel
```

目前只包含LT-M90。选择型号是本地通信与打印配置，不是硬件探测；不得把未知设备猜测为此型号。

文档 ID：`apple:symbol:s:12XLPrinterSDK12PrinterModelO`

符号种类：`swift.enum`；访问级别：`public`。

<a id="api-d34084be34bff3cfb12a"></a>

### PrinterModel.!=(_:_:)

判断两个PrinterModel值是否不同。

```swift
static func != (lhs: borrowing Self, rhs: borrowing Self) -> Bool
```

Swift Equatable协议提供的合成比较，按该值的相等语义取反，不执行打印机IO。哈希或相等不表示硬件身份已经认证。

文档 ID：`apple:symbol:s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:12XLPrinterSDK12PrinterModelO`

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

<a id="api-23cdc480fcf76d33c635"></a>

### PrinterModel.hash(into:)

向Swift Hasher写入该值的哈希内容。

```swift
func hash(into hasher: inout Hasher)
```

来自Hashable/RawRepresentable默认实现，供集合比较使用，不执行设备IO；不能作为稳定序列化或打印协议编码。

文档 ID：`apple:symbol:s:SYsSHRzSH8RawValueSYRpzrlE4hash4intoys6HasherVz_tF::SYNTHESIZED::s:12XLPrinterSDK12PrinterModelO`

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

<a id="api-f850659b43982101128e"></a>

### PrinterModel.hashValue

Swift合成的进程内哈希值。

```swift
var hashValue: Int { get }
```

用于Hashable集合，不保证跨运行或跨版本稳定，不作为打印协议值、设备认证或持久键。

文档 ID：`apple:symbol:s:SYsSHRzSH8RawValueSYRpzrlE04hashB0Sivp::SYNTHESIZED::s:12XLPrinterSDK12PrinterModelO`

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

<a id="api-f74d7d272068b77c989a"></a>

### PrinterModel.init(rawValue:)

按精确原始值构造PrinterModel。

```swift
init?(rawValue: String)
```

目前仅字符串LT-M90可构造ltM90，其他字符串返回nil；本地构造不探测或认证设备。

文档 ID：`apple:symbol:s:12XLPrinterSDK12PrinterModelO8rawValueACSgSS_tcfc`

符号种类：`swift.init`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| String | `s:SS` |

<a id="api-082967ad0f05c5db6731"></a>

### PrinterModel.ltM90

已收录的LT-M90型号。

```swift
case ltM90
```

内部集中提供CPCL、BLE通信、精确8 dots/mm、864点最大打印宽度与固定居中规则；名称推荐或显式选择都不等于硬件认证。

文档 ID：`apple:symbol:s:12XLPrinterSDK12PrinterModelO5ltM90yA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-734595dc7ec727f038bf"></a>

### PrinterModel.makeFromRawAttachmentValue(_:)

CoreVideo提供的原始值附件转换扩展。

```swift
static func makeFromRawAttachmentValue(_ repr: CVAttachmentRawValue) -> Self?
```

这是当前Apple SDK对RawRepresentable类型合成的系统默认扩展，iOS27及以上可用；不是PrinterSDK打印、配置或设备控制入口。具体可用性以编译器声明为准。

文档 ID：`apple:symbol:s:SY9CoreVideoAA30CVAttachmentValueRepresentable03RawD0RpzrlE08makeFromf10AttachmentD0yxSgAA0cfD0VFZ::SYNTHESIZED::s:12XLPrinterSDK12PrinterModelO`

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

<a id="api-125fed21a62ab4dffc54"></a>

### PrinterModel.rawAttachmentValueRepresentation

CoreVideo提供的原始值附件转换扩展。

```swift
var rawAttachmentValueRepresentation: CVAttachmentRawValue { get }
```

这是当前Apple SDK对RawRepresentable类型合成的系统默认扩展，iOS27及以上可用；不是PrinterSDK打印、配置或设备控制入口。具体可用性以编译器声明为准。

文档 ID：`apple:symbol:s:SY9CoreVideoAA30CVAttachmentValueRepresentable03RawD0RpzrlE013rawAttachmentD14RepresentationAA0cfD0Vvp::SYNTHESIZED::s:12XLPrinterSDK12PrinterModelO`

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

<a id="api-af23c3cfa380259370a2"></a>

### RequestID

只能由来源Manager识别的不透明请求身份。

```swift
struct RequestID
```

由请求方法返回，支持Hashable/Sendable，不开放内部序号或构造器。可交给同一Manager.cancel；已结束或其他Manager的身份无效果。

文档 ID：`apple:symbol:s:12XLPrinterSDK9RequestIDV`

符号种类：`swift.struct`；访问级别：`public`。

<a id="api-b609695afb8219089ab0"></a>

### RequestID.!=(_:_:)

判断两个RequestID值是否不同。

```swift
static func != (lhs: borrowing Self, rhs: borrowing Self) -> Bool
```

Swift Equatable协议提供的合成比较，按该值的相等语义取反，不执行打印机IO。哈希或相等不表示硬件身份已经认证。

文档 ID：`apple:symbol:s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:12XLPrinterSDK9RequestIDV`

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

<a id="api-544521e93030eb02a1c6"></a>

### SDKInfo

实际链接的原生SDK身份。

```swift
struct SDKInfo
```

nativeVersion和distributionVersion来自已链接库；buildID未知时为nil。它们不是Demo构建版本，也不证明真实硬件验收。

文档 ID：`apple:symbol:s:12XLPrinterSDK7SDKInfoV`

符号种类：`swift.struct`；访问级别：`public`。

<a id="api-11a08b2a924c35ec57f5"></a>

### SDKInfo.!=(_:_:)

判断两个SDKInfo值是否不同。

```swift
static func != (lhs: borrowing Self, rhs: borrowing Self) -> Bool
```

Swift Equatable协议提供的合成比较，按该值的相等语义取反，不执行打印机IO。哈希或相等不表示硬件身份已经认证。

文档 ID：`apple:symbol:s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:12XLPrinterSDK7SDKInfoV`

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

<a id="api-00e1ed2e6a89edd1c773"></a>

### SDKInfo.buildID

可选的原生构建来源标识。

```swift
let buildID: String?
```

仅在构建嵌入已知来源标识时返回；未知为nil，不用N/A等特殊字符串。用于诊断，不是安全认证凭证。

文档 ID：`apple:symbol:s:12XLPrinterSDK7SDKInfoV7buildIDSSSgvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| String | `s:SS` |

<a id="api-ef338b21ed7f859a716f"></a>

### SDKInfo.distributionVersion

实际原生库嵌入的分发版本。

```swift
let distributionVersion: String
```

用于核对所加载制品身份；不从Demo配置覆盖，也不代表已通过真实硬件或公开发布验证。

文档 ID：`apple:symbol:s:12XLPrinterSDK7SDKInfoV19distributionVersionSSvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| String | `s:SS` |

<a id="api-cd28bfe709939f14a9ad"></a>

### SDKInfo.nativeVersion

原生核心SDK版本。

```swift
let nativeVersion: String
```

由实际加载/链接的原生库返回，例如0.3.0；它与移动分发预览版本分别表示，不从App依赖声明猜测。

文档 ID：`apple:symbol:s:12XLPrinterSDK7SDKInfoV13nativeVersionSSvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| String | `s:SS` |

<a id="api-498d0aa8573deb1c5df3"></a>

## StatusResult

<a id="api-5b42d0a7f69375202e11"></a>

### StatusResult

设备响应中有证据的硬件条件。

```swift
struct StatusResult
```

缺纸、开盖、纸将尽和切刀错误四项可空布尔值。监听注册或无有效样本时四项未知；LT-M90只使coverOpen、paperOut已知，另两项仍未知。查询回复与主动上报统一更新连接缓存，不能从本地发送完成推断机械就绪。

文档 ID：`apple:symbol:s:12XLPrinterSDK12StatusResultV`

符号种类：`swift.struct`；访问级别：`public`。

<a id="api-b954a90e3d2376f49f1c"></a>

### StatusResult.!=(_:_:)

判断两个StatusResult值是否不同。

```swift
static func != (lhs: borrowing Self, rhs: borrowing Self) -> Bool
```

Swift Equatable协议提供的合成比较，按该值的相等语义取反，不执行打印机IO。哈希或相等不表示硬件身份已经认证。

文档 ID：`apple:symbol:s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:12XLPrinterSDK12StatusResultV`

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

<a id="api-dc87fd5baa6cd5849434"></a>

### StatusResult.coverOpen

开盖条件。

```swift
var coverOpen: Bool? { get }
```

有设备响应依据时true表示条件存在、false表示该条件未激活；空值表示未知。不能把未知解释为正常，也不能由本地发送完成推断此条件。

文档 ID：`apple:symbol:s:12XLPrinterSDK12StatusResultV9coverOpenSbSgvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Bool | `s:Sb` |

<a id="api-9126e727af0439a15193"></a>

### StatusResult.cutterError

切刀错误条件。

```swift
var cutterError: Bool? { get }
```

有设备响应依据时true表示条件存在、false表示该条件未激活；空值表示未知。不能把未知解释为正常，也不能由本地发送完成推断此条件。

文档 ID：`apple:symbol:s:12XLPrinterSDK12StatusResultV11cutterErrorSbSgvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Bool | `s:Sb` |

<a id="api-97b3c00d3e36bbf7cb82"></a>

### StatusResult.paperNearEnd

纸将尽条件。

```swift
var paperNearEnd: Bool? { get }
```

有设备响应依据时true表示条件存在、false表示该条件未激活；空值表示未知。不能把未知解释为正常，也不能由本地发送完成推断此条件。

文档 ID：`apple:symbol:s:12XLPrinterSDK12StatusResultV12paperNearEndSbSgvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Bool | `s:Sb` |

<a id="api-d5c679a8d439effd8e8a"></a>

### StatusResult.paperOut

缺纸条件。

```swift
var paperOut: Bool? { get }
```

有设备响应依据时true表示条件存在、false表示该条件未激活；空值表示未知。不能把未知解释为正常，也不能由本地发送完成推断此条件。

文档 ID：`apple:symbol:s:12XLPrinterSDK12StatusResultV8paperOutSbSgvp`

符号种类：`swift.property`；访问级别：`public`。

声明引用的编译器标识：

| 名称 | precise identifier |
| --- | --- |
| Bool | `s:Sb` |

<a id="api-eeb58ec4a51744fa2e58"></a>

## Support

<a id="api-4c84c57bb7fc23d5d1df"></a>

### Support

能力支持程度的三态值。

```swift
enum Support
```

unknown保留未知，unsupported表示未实现或明确不支持，supported表示具备相应设备依据；不能仅凭SDK能编码就认定硬件支持。

文档 ID：`apple:symbol:s:12XLPrinterSDK7SupportO`

符号种类：`swift.enum`；访问级别：`public`。

<a id="api-b3373aed8d7ee6a3ce35"></a>

### Support.!=(_:_:)

判断两个Support值是否不同。

```swift
static func != (lhs: borrowing Self, rhs: borrowing Self) -> Bool
```

Swift Equatable协议提供的合成比较，按该值的相等语义取反，不执行打印机IO。哈希或相等不表示硬件身份已经认证。

文档 ID：`apple:symbol:s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:12XLPrinterSDK7SupportO`

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

<a id="api-55837aefef4270208ce3"></a>

### Support.supported

具备支持依据。

```swift
case supported
```

仍受当前连接、参数和型号范围限制；此值不是纸面输出成功证明。

文档 ID：`apple:symbol:s:12XLPrinterSDK7SupportO9supportedyA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-cfebba34313bb6bcbfeb"></a>

### Support.unknown

设备支持程度未知。

```swift
case unknown
```

没有足够设备支持依据，不能把此值当作SUPPORTED或正常硬件状态。

文档 ID：`apple:symbol:s:12XLPrinterSDK7SupportO7unknownyA2CmF`

符号种类：`swift.enum.case`；访问级别：`public`。

<a id="api-fd77f8ebec8e5576f782"></a>

### Support.unsupported

未实现或明确不支持。

```swift
case unsupported
```

对应SDK实现缺失或明确设备不支持；调用仍以具体请求的稳定错误码为准。

文档 ID：`apple:symbol:s:12XLPrinterSDK7SupportO11unsupportedyA2CmF`

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

完整保留 **173** 条继承、协议遵循、成员及合成关系；
source 和 target 使用精确编译器标识，外部 Swift/Foundation 类型也保持原值。

| kind | source | target | 其他编译器字段 |
| --- | --- | --- | --- |
| `conformsTo` | `s:12XLPrinterSDK11PrinterInfoV` | `s:SQ` | `{}` |
| `conformsTo` | `s:12XLPrinterSDK11PrinterInfoV` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:12XLPrinterSDK11PrinterInfoV` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:12XLPrinterSDK12PrinterModelO` | `s:SH` | `{}` |
| `conformsTo` | `s:12XLPrinterSDK12PrinterModelO` | `s:SQ` | `{}` |
| `conformsTo` | `s:12XLPrinterSDK12PrinterModelO` | `s:SY` | `{}` |
| `conformsTo` | `s:12XLPrinterSDK12PrinterModelO` | `s:s12CaseIterableP` | `{}` |
| `conformsTo` | `s:12XLPrinterSDK12PrinterModelO` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:12XLPrinterSDK12PrinterModelO` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:12XLPrinterSDK12StatusResultV` | `s:SQ` | `{}` |
| `conformsTo` | `s:12XLPrinterSDK12StatusResultV` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:12XLPrinterSDK12StatusResultV` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:12XLPrinterSDK13ManagerResultV` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:12XLPrinterSDK13ManagerResultV` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:12XLPrinterSDK14ConnectionInfoV` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:12XLPrinterSDK14ConnectionInfoV` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:12XLPrinterSDK14ManagerOutcomeO` | `s:SH` | `{}` |
| `conformsTo` | `s:12XLPrinterSDK14ManagerOutcomeO` | `s:SQ` | `{}` |
| `conformsTo` | `s:12XLPrinterSDK14ManagerOutcomeO` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:12XLPrinterSDK14ManagerOutcomeO` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:12XLPrinterSDK14PrinterManagerC` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:12XLPrinterSDK14PrinterManagerC` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:12XLPrinterSDK15CompletionLevelO` | `s:SH` | `{}` |
| `conformsTo` | `s:12XLPrinterSDK15CompletionLevelO` | `s:SQ` | `{}` |
| `conformsTo` | `s:12XLPrinterSDK15CompletionLevelO` | `s:SY` | `{}` |
| `conformsTo` | `s:12XLPrinterSDK15CompletionLevelO` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:12XLPrinterSDK15CompletionLevelO` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:12XLPrinterSDK17DiscoveredPrinterV` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:12XLPrinterSDK17DiscoveredPrinterV` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:12XLPrinterSDK19PrinterCapabilitiesV` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:12XLPrinterSDK19PrinterCapabilitiesV` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:12XLPrinterSDK7SDKInfoV` | `s:SQ` | `{}` |
| `conformsTo` | `s:12XLPrinterSDK7SDKInfoV` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:12XLPrinterSDK7SDKInfoV` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:12XLPrinterSDK7SupportO` | `s:SH` | `{}` |
| `conformsTo` | `s:12XLPrinterSDK7SupportO` | `s:SQ` | `{}` |
| `conformsTo` | `s:12XLPrinterSDK7SupportO` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:12XLPrinterSDK7SupportO` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:12XLPrinterSDK9PaperTypeO` | `s:SH` | `{}` |
| `conformsTo` | `s:12XLPrinterSDK9PaperTypeO` | `s:SQ` | `{}` |
| `conformsTo` | `s:12XLPrinterSDK9PaperTypeO` | `s:s12CaseIterableP` | `{}` |
| `conformsTo` | `s:12XLPrinterSDK9PaperTypeO` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:12XLPrinterSDK9PaperTypeO` | `s:s8SendableP` | `{}` |
| `conformsTo` | `s:12XLPrinterSDK9RequestIDV` | `s:SH` | `{}` |
| `conformsTo` | `s:12XLPrinterSDK9RequestIDV` | `s:SQ` | `{}` |
| `conformsTo` | `s:12XLPrinterSDK9RequestIDV` | `s:s16SendableMetatypeP` | `{}` |
| `conformsTo` | `s:12XLPrinterSDK9RequestIDV` | `s:s8SendableP` | `{}` |
| `defaultImplementationOf` | `s:12XLPrinterSDK15PrinterDelegatePAAE14printerManager_016didFinishSettingC5ModelyAA0cF0C_AA0F6ResultVyAA14ConnectionInfoVGtF` | `s:12XLPrinterSDK15PrinterDelegateP14printerManager_016didFinishSettingC5ModelyAA0cF0C_AA0F6ResultVyAA14ConnectionInfoVGtF` | `{}` |
| `defaultImplementationOf` | `s:12XLPrinterSDK15PrinterDelegatePAAE14printerManager_11didDiscover6scanIDyAA0cF0C_AA010DiscoveredC0VAA07RequestJ0VtF` | `s:12XLPrinterSDK15PrinterDelegateP14printerManager_11didDiscover6scanIDyAA0cF0C_AA010DiscoveredC0VAA07RequestJ0VtF` | `{}` |
| `defaultImplementationOf` | `s:12XLPrinterSDK15PrinterDelegatePAAE14printerManager_15didUpdateStatusyAA0cF0C_AA0I6ResultVtF` | `s:12XLPrinterSDK15PrinterDelegateP14printerManager_15didUpdateStatusyAA0cF0C_AA0I6ResultVtF` | `{}` |
| `defaultImplementationOf` | `s:12XLPrinterSDK15PrinterDelegatePAAE14printerManager_16didFinishClosingyAA0cF0C_AA0F6ResultVyytGtF` | `s:12XLPrinterSDK15PrinterDelegateP14printerManager_16didFinishClosingyAA0cF0C_AA0F6ResultVyytGtF` | `{}` |
| `defaultImplementationOf` | `s:12XLPrinterSDK15PrinterDelegatePAAE14printerManager_17didFinishPrintingyAA0cF0C_AA0F6ResultVyytGtF` | `s:12XLPrinterSDK15PrinterDelegateP14printerManager_17didFinishPrintingyAA0cF0C_AA0F6ResultVyytGtF` | `{}` |
| `defaultImplementationOf` | `s:12XLPrinterSDK15PrinterDelegatePAAE14printerManager_17didFinishScanningyAA0cF0C_AA0F6ResultVySayAA010DiscoveredC0VGGtF` | `s:12XLPrinterSDK15PrinterDelegateP14printerManager_17didFinishScanningyAA0cF0C_AA0F6ResultVySayAA010DiscoveredC0VGGtF` | `{}` |
| `defaultImplementationOf` | `s:12XLPrinterSDK15PrinterDelegatePAAE14printerManager_17didLoseConnectionyAA0cF0C_AA0F6ResultVyytGtF` | `s:12XLPrinterSDK15PrinterDelegateP14printerManager_17didLoseConnectionyAA0cF0C_AA0F6ResultVyytGtF` | `{}` |
| `defaultImplementationOf` | `s:12XLPrinterSDK15PrinterDelegatePAAE14printerManager_19didFinishConnectingyAA0cF0C_AA0F6ResultVyAA14ConnectionInfoVGtF` | `s:12XLPrinterSDK15PrinterDelegateP14printerManager_19didFinishConnectingyAA0cF0C_AA0F6ResultVyAA14ConnectionInfoVGtF` | `{}` |
| `defaultImplementationOf` | `s:12XLPrinterSDK15PrinterDelegatePAAE14printerManager_19didFinishSendingRawyAA0cF0C_AA0F6ResultVyytGtF` | `s:12XLPrinterSDK15PrinterDelegateP14printerManager_19didFinishSendingRawyAA0cF0C_AA0F6ResultVyytGtF` | `{}` |
| `defaultImplementationOf` | `s:12XLPrinterSDK15PrinterDelegatePAAE14printerManager_22didFinishDisconnectingyAA0cF0C_AA0F6ResultVyytGtF` | `s:12XLPrinterSDK15PrinterDelegateP14printerManager_22didFinishDisconnectingyAA0cF0C_AA0F6ResultVyytGtF` | `{}` |
| `defaultImplementationOf` | `s:12XLPrinterSDK15PrinterDelegatePAAE14printerManager_23didFinishQueryingStatusyAA0cF0C_AA0F6ResultVyytGtF` | `s:12XLPrinterSDK15PrinterDelegateP14printerManager_23didFinishQueryingStatusyAA0cF0C_AA0F6ResultVyytGtF` | `{}` |
| `defaultImplementationOf` | `s:12XLPrinterSDK15PrinterDelegatePAAE14printerManager_23didFinishReadingSDKInfoyAA0cF0C_AA0F6ResultVyAA0J0VGtF` | `s:12XLPrinterSDK15PrinterDelegateP14printerManager_23didFinishReadingSDKInfoyAA0cF0C_AA0F6ResultVyAA0J0VGtF` | `{}` |
| `defaultImplementationOf` | `s:12XLPrinterSDK15PrinterDelegatePAAE14printerManager_25didFinishPositioningLabelyAA0cF0C_AA0F6ResultVyytGtF` | `s:12XLPrinterSDK15PrinterDelegateP14printerManager_25didFinishPositioningLabelyAA0cF0C_AA0F6ResultVyytGtF` | `{}` |
| `defaultImplementationOf` | `s:12XLPrinterSDK15PrinterDelegatePAAE14printerManager_25didFinishSettingPaperTypeyAA0cF0C_AA0F6ResultVyytGtF` | `s:12XLPrinterSDK15PrinterDelegateP14printerManager_25didFinishSettingPaperTypeyAA0cF0C_AA0F6ResultVyytGtF` | `{}` |
| `defaultImplementationOf` | `s:12XLPrinterSDK15PrinterDelegatePAAE14printerManager_28didFinishReadingCapabilitiesyAA0cF0C_AA0F6ResultVyAA0cJ0VGtF` | `s:12XLPrinterSDK15PrinterDelegateP14printerManager_28didFinishReadingCapabilitiesyAA0cF0C_AA0F6ResultVyAA0cJ0VGtF` | `{}` |
| `memberOf` | `s:12XLPrinterSDK11PrinterInfoV10deviceNameSSSgvp` | `s:12XLPrinterSDK11PrinterInfoV` | `{}` |
| `memberOf` | `s:12XLPrinterSDK11PrinterInfoV10speedLevels5Int32VSgvp` | `s:12XLPrinterSDK11PrinterInfoV` | `{}` |
| `memberOf` | `s:12XLPrinterSDK11PrinterInfoV11reportedDpis6UInt32VSgvp` | `s:12XLPrinterSDK11PrinterInfoV` | `{}` |
| `memberOf` | `s:12XLPrinterSDK11PrinterInfoV11supportsNfcSbSgvp` | `s:12XLPrinterSDK11PrinterInfoV` | `{}` |
| `memberOf` | `s:12XLPrinterSDK11PrinterInfoV11supportsRtcSbSgvp` | `s:12XLPrinterSDK11PrinterInfoV` | `{}` |
| `memberOf` | `s:12XLPrinterSDK11PrinterInfoV12densityLevels5Int32VSgvp` | `s:12XLPrinterSDK11PrinterInfoV` | `{}` |
| `memberOf` | `s:12XLPrinterSDK11PrinterInfoV12manufacturerSSSgvp` | `s:12XLPrinterSDK11PrinterInfoV` | `{}` |
| `memberOf` | `s:12XLPrinterSDK11PrinterInfoV13externalPowerSbSgvp` | `s:12XLPrinterSDK11PrinterInfoV` | `{}` |
| `memberOf` | `s:12XLPrinterSDK11PrinterInfoV13paperTypeCodes5Int32VSgvp` | `s:12XLPrinterSDK11PrinterInfoV` | `{}` |
| `memberOf` | `s:12XLPrinterSDK11PrinterInfoV14batteryPercents5Int32VSgvp` | `s:12XLPrinterSDK11PrinterInfoV` | `{}` |
| `memberOf` | `s:12XLPrinterSDK11PrinterInfoV15reportedVersions6UInt32VSgvp` | `s:12XLPrinterSDK11PrinterInfoV` | `{}` |
| `memberOf` | `s:12XLPrinterSDK11PrinterInfoV16protocolVerifiedSbvp` | `s:12XLPrinterSDK11PrinterInfoV` | `{}` |
| `memberOf` | `s:12XLPrinterSDK11PrinterInfoV17maxPrintWidthDotss6UInt32VSgvp` | `s:12XLPrinterSDK11PrinterInfoV` | `{}` |
| `memberOf` | `s:12XLPrinterSDK11PrinterInfoV19supportsCompressionSbSgvp` | `s:12XLPrinterSDK11PrinterInfoV` | `{}` |
| `memberOf` | `s:12XLPrinterSDK11PrinterInfoV27printheadTemperatureCelsiuss5Int32VSgvp` | `s:12XLPrinterSDK11PrinterInfoV` | `{}` |
| `memberOf` | `s:12XLPrinterSDK11PrinterInfoV6uidHexSSSgvp` | `s:12XLPrinterSDK11PrinterInfoV` | `{}` |
| `memberOf` | `s:12XLPrinterSDK11PrinterInfoV8hasErrorSbSgvp` | `s:12XLPrinterSDK11PrinterInfoV` | `{}` |
| `memberOf` | `s:12XLPrinterSDK12PrinterModelO5ltM90yA2CmF` | `s:12XLPrinterSDK12PrinterModelO` | `{}` |
| `memberOf` | `s:12XLPrinterSDK12PrinterModelO8rawValueACSgSS_tcfc` | `s:12XLPrinterSDK12PrinterModelO` | `{"sourceOrigin":{"displayName":"RawRepresentable.init(rawValue:)","identifier":"s:SY8rawValuexSg03RawB0Qz_tcfc"}}` |
| `memberOf` | `s:12XLPrinterSDK12StatusResultV11cutterErrorSbSgvp` | `s:12XLPrinterSDK12StatusResultV` | `{}` |
| `memberOf` | `s:12XLPrinterSDK12StatusResultV12paperNearEndSbSgvp` | `s:12XLPrinterSDK12StatusResultV` | `{}` |
| `memberOf` | `s:12XLPrinterSDK12StatusResultV8paperOutSbSgvp` | `s:12XLPrinterSDK12StatusResultV` | `{}` |
| `memberOf` | `s:12XLPrinterSDK12StatusResultV9coverOpenSbSgvp` | `s:12XLPrinterSDK12StatusResultV` | `{}` |
| `memberOf` | `s:12XLPrinterSDK13ManagerResultV10completionAA15CompletionLevelOvp` | `s:12XLPrinterSDK13ManagerResultV` | `{}` |
| `memberOf` | `s:12XLPrinterSDK13ManagerResultV21outputMayHaveOccurredSbvp` | `s:12XLPrinterSDK13ManagerResultV` | `{}` |
| `memberOf` | `s:12XLPrinterSDK13ManagerResultV4codes5Int32Vvp` | `s:12XLPrinterSDK13ManagerResultV` | `{}` |
| `memberOf` | `s:12XLPrinterSDK13ManagerResultV5valuexSgvp` | `s:12XLPrinterSDK13ManagerResultV` | `{}` |
| `memberOf` | `s:12XLPrinterSDK13ManagerResultV7messageSSvp` | `s:12XLPrinterSDK13ManagerResultV` | `{}` |
| `memberOf` | `s:12XLPrinterSDK13ManagerResultV7outcomeAA0C7OutcomeOvp` | `s:12XLPrinterSDK13ManagerResultV` | `{}` |
| `memberOf` | `s:12XLPrinterSDK13ManagerResultV9isSuccessSbvp` | `s:12XLPrinterSDK13ManagerResultV` | `{}` |
| `memberOf` | `s:12XLPrinterSDK13ManagerResultV9requestIDAA07RequestF0Vvp` | `s:12XLPrinterSDK13ManagerResultV` | `{}` |
| `memberOf` | `s:12XLPrinterSDK14ConnectionInfoV07printerD0AA07PrinterD0VSgvp` | `s:12XLPrinterSDK14ConnectionInfoV` | `{}` |
| `memberOf` | `s:12XLPrinterSDK14ConnectionInfoV5modelAA12PrinterModelOSgvp` | `s:12XLPrinterSDK14ConnectionInfoV` | `{}` |
| `memberOf` | `s:12XLPrinterSDK14ConnectionInfoV6deviceAA17DiscoveredPrinterVvp` | `s:12XLPrinterSDK14ConnectionInfoV` | `{}` |
| `memberOf` | `s:12XLPrinterSDK14ManagerOutcomeO7failureyA2CmF` | `s:12XLPrinterSDK14ManagerOutcomeO` | `{}` |
| `memberOf` | `s:12XLPrinterSDK14ManagerOutcomeO7successyA2CmF` | `s:12XLPrinterSDK14ManagerOutcomeO` | `{}` |
| `memberOf` | `s:12XLPrinterSDK14ManagerOutcomeO9cancelledyA2CmF` | `s:12XLPrinterSDK14ManagerOutcomeO` | `{}` |
| `memberOf` | `s:12XLPrinterSDK14PrinterManagerC03setC5ModelyAA9RequestIDVAA0cF0OF` | `s:12XLPrinterSDK14PrinterManagerC` | `{}` |
| `memberOf` | `s:12XLPrinterSDK14PrinterManagerC10connectBle6device5modelAA9RequestIDVAA010DiscoveredC0V_AA0C5ModelOtF` | `s:12XLPrinterSDK14PrinterManagerC` | `{}` |
| `memberOf` | `s:12XLPrinterSDK14PrinterManagerC10connectBle6deviceAA9RequestIDVAA010DiscoveredC0V_tF` | `s:12XLPrinterSDK14PrinterManagerC` | `{}` |
| `memberOf` | `s:12XLPrinterSDK14PrinterManagerC10disconnectAA9RequestIDVyF` | `s:12XLPrinterSDK14PrinterManagerC` | `{}` |
| `memberOf` | `s:12XLPrinterSDK14PrinterManagerC10printImage_7widthMm06heightH0AA9RequestIDVSo7UIImageC_S2dtF` | `s:12XLPrinterSDK14PrinterManagerC` | `{}` |
| `memberOf` | `s:12XLPrinterSDK14PrinterManagerC12capabilitiesAA9RequestIDVyF` | `s:12XLPrinterSDK14PrinterManagerC` | `{}` |
| `memberOf` | `s:12XLPrinterSDK14PrinterManagerC12setPaperTypeyAA9RequestIDVAA0fG0OF` | `s:12XLPrinterSDK14PrinterManagerC` | `{}` |
| `memberOf` | `s:12XLPrinterSDK14PrinterManagerC15locateNextLabelAA9RequestIDVyF` | `s:12XLPrinterSDK14PrinterManagerC` | `{}` |
| `memberOf` | `s:12XLPrinterSDK14PrinterManagerC17printEncodedImage_7widthMm06heightI0AA9RequestIDV10Foundation4DataV_S2dtF` | `s:12XLPrinterSDK14PrinterManagerC` | `{}` |
| `memberOf` | `s:12XLPrinterSDK14PrinterManagerC5closeAA9RequestIDVyF` | `s:12XLPrinterSDK14PrinterManagerC` | `{}` |
| `memberOf` | `s:12XLPrinterSDK14PrinterManagerC6cancelyyAA9RequestIDVF` | `s:12XLPrinterSDK14PrinterManagerC` | `{}` |
| `memberOf` | `s:12XLPrinterSDK14PrinterManagerC7scanBLEAA9RequestIDVyF` | `s:12XLPrinterSDK14PrinterManagerC` | `{}` |
| `memberOf` | `s:12XLPrinterSDK14PrinterManagerC7sdkInfoAA9RequestIDVyF` | `s:12XLPrinterSDK14PrinterManagerC` | `{}` |
| `memberOf` | `s:12XLPrinterSDK14PrinterManagerC7sendRawyAA9RequestIDV10Foundation4DataVF` | `s:12XLPrinterSDK14PrinterManagerC` | `{}` |
| `memberOf` | `s:12XLPrinterSDK14PrinterManagerC8delegateAA0C8Delegate_pSgvp` | `s:12XLPrinterSDK14PrinterManagerC` | `{}` |
| `memberOf` | `s:12XLPrinterSDK14PrinterManagerC9getStatusAA9RequestIDVyF` | `s:12XLPrinterSDK14PrinterManagerC` | `{}` |
| `memberOf` | `s:12XLPrinterSDK14PrinterManagerCACycfc` | `s:12XLPrinterSDK14PrinterManagerC` | `{}` |
| `memberOf` | `s:12XLPrinterSDK15CompletionLevelO15deviceConfirmedyA2CmF` | `s:12XLPrinterSDK15CompletionLevelO` | `{}` |
| `memberOf` | `s:12XLPrinterSDK15CompletionLevelO4noneyA2CmF` | `s:12XLPrinterSDK15CompletionLevelO` | `{}` |
| `memberOf` | `s:12XLPrinterSDK15CompletionLevelO8dataSentyA2CmF` | `s:12XLPrinterSDK15CompletionLevelO` | `{}` |
| `memberOf` | `s:12XLPrinterSDK15CompletionLevelO8rawValueACSgs6UInt32V_tcfc` | `s:12XLPrinterSDK15CompletionLevelO` | `{"sourceOrigin":{"displayName":"RawRepresentable.init(rawValue:)","identifier":"s:SY8rawValuexSg03RawB0Qz_tcfc"}}` |
| `memberOf` | `s:12XLPrinterSDK17DiscoveredPrinterV10identifier10Foundation4UUIDVvp` | `s:12XLPrinterSDK17DiscoveredPrinterV` | `{}` |
| `memberOf` | `s:12XLPrinterSDK17DiscoveredPrinterV10identifier4nameAC10Foundation4UUIDV_SSSgtcfc` | `s:12XLPrinterSDK17DiscoveredPrinterV` | `{}` |
| `memberOf` | `s:12XLPrinterSDK17DiscoveredPrinterV14signalStrengthSiSgvp` | `s:12XLPrinterSDK17DiscoveredPrinterV` | `{}` |
| `memberOf` | `s:12XLPrinterSDK17DiscoveredPrinterV4nameSSSgvp` | `s:12XLPrinterSDK17DiscoveredPrinterV` | `{}` |
| `memberOf` | `s:12XLPrinterSDK19PrinterCapabilitiesV11rawCommandsAA7SupportOvp` | `s:12XLPrinterSDK19PrinterCapabilitiesV` | `{}` |
| `memberOf` | `s:12XLPrinterSDK19PrinterCapabilitiesV11statusQueryAA7SupportOvp` | `s:12XLPrinterSDK19PrinterCapabilitiesV` | `{}` |
| `memberOf` | `s:12XLPrinterSDK19PrinterCapabilitiesV13imagePrintingAA7SupportOvp` | `s:12XLPrinterSDK19PrinterCapabilitiesV` | `{}` |
| `memberOf` | `s:12XLPrinterSDK19PrinterCapabilitiesV15maxPageHeightMmSdSgvp` | `s:12XLPrinterSDK19PrinterCapabilitiesV` | `{}` |
| `memberOf` | `s:12XLPrinterSDK19PrinterCapabilitiesV15maxPrintWidthMmSdSgvp` | `s:12XLPrinterSDK19PrinterCapabilitiesV` | `{}` |
| `memberOf` | `s:12XLPrinterSDK19PrinterCapabilitiesV15statusReportingAA7SupportOvp` | `s:12XLPrinterSDK19PrinterCapabilitiesV` | `{}` |
| `memberOf` | `s:12XLPrinterSDK19PrinterCapabilitiesV16labelPositioningAA7SupportOvp` | `s:12XLPrinterSDK19PrinterCapabilitiesV` | `{}` |
| `memberOf` | `s:12XLPrinterSDK19PrinterCapabilitiesV18paperTypeSelectionAA7SupportOvp` | `s:12XLPrinterSDK19PrinterCapabilitiesV` | `{}` |
| `memberOf` | `s:12XLPrinterSDK7SDKInfoV13nativeVersionSSvp` | `s:12XLPrinterSDK7SDKInfoV` | `{}` |
| `memberOf` | `s:12XLPrinterSDK7SDKInfoV19distributionVersionSSvp` | `s:12XLPrinterSDK7SDKInfoV` | `{}` |
| `memberOf` | `s:12XLPrinterSDK7SDKInfoV7buildIDSSSgvp` | `s:12XLPrinterSDK7SDKInfoV` | `{}` |
| `memberOf` | `s:12XLPrinterSDK7SupportO11unsupportedyA2CmF` | `s:12XLPrinterSDK7SupportO` | `{}` |
| `memberOf` | `s:12XLPrinterSDK7SupportO7unknownyA2CmF` | `s:12XLPrinterSDK7SupportO` | `{}` |
| `memberOf` | `s:12XLPrinterSDK7SupportO9supportedyA2CmF` | `s:12XLPrinterSDK7SupportO` | `{}` |
| `memberOf` | `s:12XLPrinterSDK9PaperTypeO10continuousyA2CmF` | `s:12XLPrinterSDK9PaperTypeO` | `{}` |
| `memberOf` | `s:12XLPrinterSDK9PaperTypeO8gapLabelyA2CmF` | `s:12XLPrinterSDK9PaperTypeO` | `{}` |
| `memberOf` | `s:12XLPrinterSDK9PaperTypeO9blackMarkyA2CmF` | `s:12XLPrinterSDK9PaperTypeO` | `{}` |
| `memberOf` | `s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:12XLPrinterSDK11PrinterInfoV` | `s:12XLPrinterSDK11PrinterInfoV` | `{"sourceOrigin":{"displayName":"Equatable.!=(_:_:)","identifier":"s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ"}}` |
| `memberOf` | `s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:12XLPrinterSDK12PrinterModelO` | `s:12XLPrinterSDK12PrinterModelO` | `{"sourceOrigin":{"displayName":"Equatable.!=(_:_:)","identifier":"s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ"}}` |
| `memberOf` | `s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:12XLPrinterSDK12StatusResultV` | `s:12XLPrinterSDK12StatusResultV` | `{"sourceOrigin":{"displayName":"Equatable.!=(_:_:)","identifier":"s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ"}}` |
| `memberOf` | `s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:12XLPrinterSDK14ManagerOutcomeO` | `s:12XLPrinterSDK14ManagerOutcomeO` | `{"sourceOrigin":{"displayName":"Equatable.!=(_:_:)","identifier":"s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ"}}` |
| `memberOf` | `s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:12XLPrinterSDK15CompletionLevelO` | `s:12XLPrinterSDK15CompletionLevelO` | `{"sourceOrigin":{"displayName":"Equatable.!=(_:_:)","identifier":"s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ"}}` |
| `memberOf` | `s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:12XLPrinterSDK7SDKInfoV` | `s:12XLPrinterSDK7SDKInfoV` | `{"sourceOrigin":{"displayName":"Equatable.!=(_:_:)","identifier":"s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ"}}` |
| `memberOf` | `s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:12XLPrinterSDK7SupportO` | `s:12XLPrinterSDK7SupportO` | `{"sourceOrigin":{"displayName":"Equatable.!=(_:_:)","identifier":"s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ"}}` |
| `memberOf` | `s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:12XLPrinterSDK9PaperTypeO` | `s:12XLPrinterSDK9PaperTypeO` | `{"sourceOrigin":{"displayName":"Equatable.!=(_:_:)","identifier":"s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ"}}` |
| `memberOf` | `s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ::SYNTHESIZED::s:12XLPrinterSDK9RequestIDV` | `s:12XLPrinterSDK9RequestIDV` | `{"sourceOrigin":{"displayName":"Equatable.!=(_:_:)","identifier":"s:SQsRi_zRi0_zrlE2neoiySbx_xtFZ"}}` |
| `memberOf` | `s:SY9CoreVideoAA30CVAttachmentValueRepresentable03RawD0RpzrlE013rawAttachmentD14RepresentationAA0cfD0Vvp::SYNTHESIZED::s:12XLPrinterSDK12PrinterModelO` | `s:12XLPrinterSDK12PrinterModelO` | `{"sourceOrigin":{"displayName":"RawRepresentable.rawAttachmentValueRepresentation","identifier":"s:SY9CoreVideoAA30CVAttachmentValueRepresentable03RawD0RpzrlE013rawAttachmentD14RepresentationAA0cfD0Vvp"}}` |
| `memberOf` | `s:SY9CoreVideoAA30CVAttachmentValueRepresentable03RawD0RpzrlE013rawAttachmentD14RepresentationAA0cfD0Vvp::SYNTHESIZED::s:12XLPrinterSDK15CompletionLevelO` | `s:12XLPrinterSDK15CompletionLevelO` | `{"sourceOrigin":{"displayName":"RawRepresentable.rawAttachmentValueRepresentation","identifier":"s:SY9CoreVideoAA30CVAttachmentValueRepresentable03RawD0RpzrlE013rawAttachmentD14RepresentationAA0cfD0Vvp"}}` |
| `memberOf` | `s:SY9CoreVideoAA30CVAttachmentValueRepresentable03RawD0RpzrlE08makeFromf10AttachmentD0yxSgAA0cfD0VFZ::SYNTHESIZED::s:12XLPrinterSDK12PrinterModelO` | `s:12XLPrinterSDK12PrinterModelO` | `{"sourceOrigin":{"displayName":"RawRepresentable.makeFromRawAttachmentValue(_:)","identifier":"s:SY9CoreVideoAA30CVAttachmentValueRepresentable03RawD0RpzrlE08makeFromf10AttachmentD0yxSgAA0cfD0VFZ"}}` |
| `memberOf` | `s:SY9CoreVideoAA30CVAttachmentValueRepresentable03RawD0RpzrlE08makeFromf10AttachmentD0yxSgAA0cfD0VFZ::SYNTHESIZED::s:12XLPrinterSDK15CompletionLevelO` | `s:12XLPrinterSDK15CompletionLevelO` | `{"sourceOrigin":{"displayName":"RawRepresentable.makeFromRawAttachmentValue(_:)","identifier":"s:SY9CoreVideoAA30CVAttachmentValueRepresentable03RawD0RpzrlE08makeFromf10AttachmentD0yxSgAA0cfD0VFZ"}}` |
| `memberOf` | `s:SYsSHRzSH8RawValueSYRpzrlE04hashB0Sivp::SYNTHESIZED::s:12XLPrinterSDK12PrinterModelO` | `s:12XLPrinterSDK12PrinterModelO` | `{"sourceOrigin":{"displayName":"RawRepresentable.hashValue","identifier":"s:SYsSHRzSH8RawValueSYRpzrlE04hashB0Sivp"}}` |
| `memberOf` | `s:SYsSHRzSH8RawValueSYRpzrlE04hashB0Sivp::SYNTHESIZED::s:12XLPrinterSDK15CompletionLevelO` | `s:12XLPrinterSDK15CompletionLevelO` | `{"sourceOrigin":{"displayName":"RawRepresentable.hashValue","identifier":"s:SYsSHRzSH8RawValueSYRpzrlE04hashB0Sivp"}}` |
| `memberOf` | `s:SYsSHRzSH8RawValueSYRpzrlE4hash4intoys6HasherVz_tF::SYNTHESIZED::s:12XLPrinterSDK12PrinterModelO` | `s:12XLPrinterSDK12PrinterModelO` | `{"sourceOrigin":{"displayName":"RawRepresentable.hash(into:)","identifier":"s:SYsSHRzSH8RawValueSYRpzrlE4hash4intoys6HasherVz_tF"}}` |
| `memberOf` | `s:SYsSHRzSH8RawValueSYRpzrlE4hash4intoys6HasherVz_tF::SYNTHESIZED::s:12XLPrinterSDK15CompletionLevelO` | `s:12XLPrinterSDK15CompletionLevelO` | `{"sourceOrigin":{"displayName":"RawRepresentable.hash(into:)","identifier":"s:SYsSHRzSH8RawValueSYRpzrlE4hash4intoys6HasherVz_tF"}}` |
| `requirementOf` | `s:12XLPrinterSDK15PrinterDelegateP14printerManager_016didFinishSettingC5ModelyAA0cF0C_AA0F6ResultVyAA14ConnectionInfoVGtF` | `s:12XLPrinterSDK15PrinterDelegateP` | `{}` |
| `requirementOf` | `s:12XLPrinterSDK15PrinterDelegateP14printerManager_11didDiscover6scanIDyAA0cF0C_AA010DiscoveredC0VAA07RequestJ0VtF` | `s:12XLPrinterSDK15PrinterDelegateP` | `{}` |
| `requirementOf` | `s:12XLPrinterSDK15PrinterDelegateP14printerManager_15didUpdateStatusyAA0cF0C_AA0I6ResultVtF` | `s:12XLPrinterSDK15PrinterDelegateP` | `{}` |
| `requirementOf` | `s:12XLPrinterSDK15PrinterDelegateP14printerManager_16didFinishClosingyAA0cF0C_AA0F6ResultVyytGtF` | `s:12XLPrinterSDK15PrinterDelegateP` | `{}` |
| `requirementOf` | `s:12XLPrinterSDK15PrinterDelegateP14printerManager_17didFinishPrintingyAA0cF0C_AA0F6ResultVyytGtF` | `s:12XLPrinterSDK15PrinterDelegateP` | `{}` |
| `requirementOf` | `s:12XLPrinterSDK15PrinterDelegateP14printerManager_17didFinishScanningyAA0cF0C_AA0F6ResultVySayAA010DiscoveredC0VGGtF` | `s:12XLPrinterSDK15PrinterDelegateP` | `{}` |
| `requirementOf` | `s:12XLPrinterSDK15PrinterDelegateP14printerManager_17didLoseConnectionyAA0cF0C_AA0F6ResultVyytGtF` | `s:12XLPrinterSDK15PrinterDelegateP` | `{}` |
| `requirementOf` | `s:12XLPrinterSDK15PrinterDelegateP14printerManager_19didFinishConnectingyAA0cF0C_AA0F6ResultVyAA14ConnectionInfoVGtF` | `s:12XLPrinterSDK15PrinterDelegateP` | `{}` |
| `requirementOf` | `s:12XLPrinterSDK15PrinterDelegateP14printerManager_19didFinishSendingRawyAA0cF0C_AA0F6ResultVyytGtF` | `s:12XLPrinterSDK15PrinterDelegateP` | `{}` |
| `requirementOf` | `s:12XLPrinterSDK15PrinterDelegateP14printerManager_22didFinishDisconnectingyAA0cF0C_AA0F6ResultVyytGtF` | `s:12XLPrinterSDK15PrinterDelegateP` | `{}` |
| `requirementOf` | `s:12XLPrinterSDK15PrinterDelegateP14printerManager_23didFinishQueryingStatusyAA0cF0C_AA0F6ResultVyytGtF` | `s:12XLPrinterSDK15PrinterDelegateP` | `{}` |
| `requirementOf` | `s:12XLPrinterSDK15PrinterDelegateP14printerManager_23didFinishReadingSDKInfoyAA0cF0C_AA0F6ResultVyAA0J0VGtF` | `s:12XLPrinterSDK15PrinterDelegateP` | `{}` |
| `requirementOf` | `s:12XLPrinterSDK15PrinterDelegateP14printerManager_25didFinishPositioningLabelyAA0cF0C_AA0F6ResultVyytGtF` | `s:12XLPrinterSDK15PrinterDelegateP` | `{}` |
| `requirementOf` | `s:12XLPrinterSDK15PrinterDelegateP14printerManager_25didFinishSettingPaperTypeyAA0cF0C_AA0F6ResultVyytGtF` | `s:12XLPrinterSDK15PrinterDelegateP` | `{}` |
| `requirementOf` | `s:12XLPrinterSDK15PrinterDelegateP14printerManager_28didFinishReadingCapabilitiesyAA0cF0C_AA0F6ResultVyAA0cJ0VGtF` | `s:12XLPrinterSDK15PrinterDelegateP` | `{}` |
