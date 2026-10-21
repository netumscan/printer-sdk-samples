# Android Java API 参考

发布候选版本 **0.3.0-preview.10**；原生 SDK **0.3.0**；C ABI **3/r2**。

本页逐项覆盖审定的编译器公共 API 基线，共 **87** 个文档条目。
声明、JVM descriptor、常量、枚举和编译器关系均来自基线，说明按稳定文档 ID 维护。
机器可读的完整基线与说明见 [api-reference.json](api-reference.json)；运行
`python3 tools/check-api-reference.py` 可检查覆盖范围、审定 SHA-256 和本页字节一致性。

基线 SHA-256：`b42d80dff97b0dd790219ae23107f9946bca030b301aef865ef93694c093e980`

运行约束与错误处理见 [API 与生命周期](api-lifecycle.md) 和 [错误码与排障](errors-troubleshooting.md)。
本页是未发布的发布候选契约，不代表发布制品或公开历史证明。硬件验收仍暂缓，`hardware_verified=false`。

## 类型导航

- [ConnectionInfo](#api-bf113bf34fa7893fb410)
- [DiscoveredDevice](#api-b790baf5c321454d18b7)
- [ManagerResult](#api-98f169a3e4c7b889744f)
- [ManagerResult.Completion](#api-1721ae5f9f8202edb30d)
- [ManagerResult.Outcome](#api-2784cdf665d09aa3045c)
- [PaperType](#api-ba5c99041265566e5203)
- [PrinterCapabilities](#api-a36171733f6c01585161)
- [PrinterCapabilities.Support](#api-b817fe4ca0353d12c928)
- [PrinterManager](#api-bfc786be5b911ef74d19)
- [PrinterManager.Callback](#api-a8eff59edaee0576f508)
- [PrinterManager.DisconnectedListener](#api-9149b393dfb876ce6b8e)
- [PrinterModel](#api-59800c82d4445d8c7af4)
- [RequestId](#api-764c5a4147cfdf641cc3)
- [SdkInfo](#api-e68d6e49886d528a891c)
- [StatusResult](#api-346a10d907279a636fde)

Java 签名保留编译器记录的二进制嵌套名 `$`；源码导入和类型访问使用 `.`。
方法参数名不在 `javap` 基线中，参数顺序与类型以声明和 descriptor 为准。

<a id="api-bf113bf34fa7893fb410"></a>

## ConnectionInfo

连接成功或型号更新后采用的本地配置。

```java
public final class com.netumscan.printersdk.ConnectionInfo
```

只读的设备目标和可选型号；型号来自原发现名称推荐或显式选择，不代表打印机已认证其硬件身份。未采用型号时保持空值。

文档 ID：`android:type:com.netumscan.printersdk.ConnectionInfo`

<a id="api-422195db5e432566d0fb"></a>

### public final com.netumscan.printersdk.DiscoveredDevice device

本次连接的设备目标。

```java
public final com.netumscan.printersdk.DiscoveredDevice device;
```

保留连接时使用的地址/标识和可选名称；该快照不携带平台原生蓝牙句柄，也不是新的发现或认证结果。

文档 ID：`android:member:com.netumscan.printersdk.ConnectionInfo:ac9ad83729a5ccf7`

JVM descriptor：`Lcom/netumscan/printersdk/DiscoveredDevice;`

编译器注解：`@androidx.annotation.NonNull`。

<a id="api-8e9818ed0b503b4ed9af"></a>

### public final com.netumscan.printersdk.PrinterModel model

当前已采用的型号。

```java
public final com.netumscan.printersdk.PrinterModel model;
```

来自名称推荐或显式setPrinterModel/连接选择；没有已采用的收录型号时为空。它描述本地配置，不声称打印机回报了该型号。

文档 ID：`android:member:com.netumscan.printersdk.ConnectionInfo:61dfaf805ce5131f`

JVM descriptor：`Lcom/netumscan/printersdk/PrinterModel;`

编译器注解：`@androidx.annotation.Nullable`。

<a id="api-b790baf5c321454d18b7"></a>

## DiscoveredDevice

Android蓝牙发现或已配对列表中的设备值。

```java
public final class com.netumscan.printersdk.DiscoveredDevice
```

只包含地址、可选名称和可选信号强度；不暴露BluetoothDevice等原生句柄。发现或配对不证明支持所选SPP/BLE通信或打印协议。

文档 ID：`android:type:com.netumscan.printersdk.DiscoveredDevice`

<a id="api-4026985104c25aedb242"></a>

### public com.netumscan.printersdk.DiscoveredDevice(java.lang.String, java.lang.String)

用设备地址和可选原名创建连接目标值。

```java
public com.netumscan.printersdk.DiscoveredDevice(java.lang.String, java.lang.String);
```

address标注非null，name允许null；该构造不执行发现、连接或格式探测，rssi为空。必须提供真实目标，连接时再验证可用性。

文档 ID：`android:member:com.netumscan.printersdk.DiscoveredDevice:0d0c198c516a6d6e`

JVM descriptor：`(Ljava/lang/String;Ljava/lang/String;)V`

参数 0（从 0 起）注解：`@androidx.annotation.NonNull`。

参数 1（从 0 起）注解：`@androidx.annotation.Nullable`。

<a id="api-5d1582cb157eea406b6d"></a>

### public final java.lang.Integer rssi

可选的BLE接收信号强度。

```java
public final java.lang.Integer rssi;
```

以dBm表示，未知时为空；已配对枚举或App手动构造的目标通常没有此信息。信号强度不是连接或打印能力证明。

文档 ID：`android:member:com.netumscan.printersdk.DiscoveredDevice:4911150c511b5cd7`

JVM descriptor：`Ljava/lang/Integer;`

编译器注解：`@androidx.annotation.Nullable`。

<a id="api-862732bae26f04800853"></a>

### public final java.lang.String address

Android蓝牙设备地址。

```java
public final java.lang.String address;
```

由发现/已配对列表返回，或由App作为连接目标提供。它不是连接句柄；目标和通信支持在连接时验证。

文档 ID：`android:member:com.netumscan.printersdk.DiscoveredDevice:b1aeb5a5c56be66f`

JVM descriptor：`Ljava/lang/String;`

编译器注解：`@androidx.annotation.NonNull`。

<a id="api-9b7190a400caf05e5d3d"></a>

### public final java.lang.String name

可选的原设备发现名称。

```java
public final java.lang.String name;
```

未知名称保持空值；SDK可按原名推荐已收录型号。App展示名和用户改名不能作为硬件已识别的证据。

文档 ID：`android:member:com.netumscan.printersdk.DiscoveredDevice:77409202b35ac1a5`

JVM descriptor：`Ljava/lang/String;`

编译器注解：`@androidx.annotation.Nullable`。

<a id="api-98f169a3e4c7b889744f"></a>

## ManagerResult

单个请求的一次不可变终态结果。

```java
public final class com.netumscan.printersdk.ManagerResult<T>
```

同时报告请求身份、成功/失败/取消、稳定错误码、完成依据和可能已输出标记。成功不自动等于纸面打印完成；失败或取消也可能发生在部分输出之后。

文档 ID：`android:type:com.netumscan.printersdk.ManagerResult`

<a id="api-9decaa39a103f3afbb6b"></a>

### public final T value

成功查询或连接操作的可选返回值。

```java
public final T value;
```

连接、能力、状态、身份和发现请求成功时使用对应类型；打印等无返回值操作、失败和取消为空。

文档 ID：`android:member:com.netumscan.printersdk.ManagerResult:60258455367224f8`

JVM descriptor：`Ljava/lang/Object;`

编译器注解：`@androidx.annotation.Nullable`。

<a id="api-356bea50d55103f0d6ac"></a>

### public final boolean outputMayHaveOccurred

本次请求是否可能已经产生输出。

```java
public final boolean outputMayHaveOccurred;
```

失败或取消在部分发送后仍可能为true；调用方不得据此自动重放。false也不使传输完成升级为设备确认。

文档 ID：`android:member:com.netumscan.printersdk.ManagerResult:a134b771ae6ec59f`

JVM descriptor：`Z`

<a id="api-ea8d4b6c1a2baff62e9d"></a>

### public final com.netumscan.printersdk.ManagerResult$Completion completion

本次请求已达到的完成证据级别。

```java
public final com.netumscan.printersdk.ManagerResult$Completion completion;
```

无输出的本地操作可成功且仍为NONE/none；DATA_SENT/dataSent只证明数据传输完成，不能据此推断纸面完成。

文档 ID：`android:member:com.netumscan.printersdk.ManagerResult:97ce1429538d8a64`

JVM descriptor：`Lcom/netumscan/printersdk/ManagerResult$Completion;`

编译器注解：`@androidx.annotation.NonNull`。

<a id="api-b511db0e6dff799b48b8"></a>

### public final com.netumscan.printersdk.ManagerResult$Outcome outcome

该请求的成功、失败或取消分类。

```java
public final com.netumscan.printersdk.ManagerResult$Outcome outcome;
```

成功对应错误码0；失败和取消没有查询值。不能仅凭分类忽略已发送的数据或可能已输出标记。

文档 ID：`android:member:com.netumscan.printersdk.ManagerResult:c2dc63bbace485a2`

JVM descriptor：`Lcom/netumscan/printersdk/ManagerResult$Outcome;`

编译器注解：`@androidx.annotation.NonNull`。

<a id="api-e4a0bf43f650f01efaeb"></a>

### public final com.netumscan.printersdk.RequestId requestId

该终态对应的原请求身份。

```java
public final com.netumscan.printersdk.RequestId requestId;
```

与方法立即返回的RequestId对应；用于App关联请求，不含可供操作底层设备的原生句柄。

文档 ID：`android:member:com.netumscan.printersdk.ManagerResult:e82f6b58f9864f0c`

JVM descriptor：`Lcom/netumscan/printersdk/RequestId;`

编译器注解：`@androidx.annotation.NonNull`。

<a id="api-4fe8239624fcc5ce1537"></a>

### public final int code

供程序分支使用的稳定错误码。

```java
public final int code;
```

成功为0；典型失败包括1001非法参数、1002状态不允许、1004不支持、1005取消、1008资源限制、2001超时、2002连接丢失。具体请求可能返回其他稳定SDK错误码。

文档 ID：`android:member:com.netumscan.printersdk.ManagerResult:c87585a61ac30812`

JVM descriptor：`I`

<a id="api-94755314530c221bb6bc"></a>

### public final java.lang.String message

面向诊断的可读错误信息。

```java
public final java.lang.String message;
```

成功通常为空字符串；程序应根据code处理，不依赖文字匹配。它不替代完成级别与可能已输出标记。

文档 ID：`android:member:com.netumscan.printersdk.ManagerResult:62f72828fbf07916`

JVM descriptor：`Ljava/lang/String;`

编译器注解：`@androidx.annotation.NonNull`。

<a id="api-1721ae5f9f8202edb30d"></a>

## ManagerResult.Completion

请求完成所依据的证据级别。

```java
public final class com.netumscan.printersdk.ManagerResult$Completion extends java.lang.Enum<com.netumscan.printersdk.ManagerResult$Completion>
```

NONE表示无传输完成依据，DATA_SENT只确认本次数据已传送，DEVICE_CONFIRMED需要明确设备响应。不得把DATA_SENT解释为纸面完成。

文档 ID：`android:type:com.netumscan.printersdk.ManagerResult$Completion`

枚举声明顺序（`ordinal()` 从 0 起；不作为持久化协议值）：

| ordinal | 枚举项 |
| --- | --- |
| 0 | `NONE` |
| 1 | `DATA_SENT` |
| 2 | `DEVICE_CONFIRMED` |

<a id="api-6d97921c294a639254f4"></a>

### public static com.netumscan.printersdk.ManagerResult$Completion valueOf(java.lang.String)

按精确枚举名取得ManagerResult$Completion值。

```java
public static com.netumscan.printersdk.ManagerResult$Completion valueOf(java.lang.String);
```

Java编译器生成的方法，名称区分大小写；未知名称抛IllegalArgumentException，null抛NullPointerException，不执行设备IO。

文档 ID：`android:member:com.netumscan.printersdk.ManagerResult$Completion:b1c46fd2ef782e48`

JVM descriptor：`(Ljava/lang/String;)Lcom/netumscan/printersdk/ManagerResult$Completion;`

这是 Java 编译器为枚举生成的公共方法。

<a id="api-352c269dc1c32a562773"></a>

### public static com.netumscan.printersdk.ManagerResult$Completion[] values()

返回ManagerResult$Completion的全部声明值。

```java
public static com.netumscan.printersdk.ManagerResult$Completion[] values();
```

Java编译器生成的方法，返回声明顺序的新数组。枚举ordinal不是SDK稳定错误码或设备协议值。

文档 ID：`android:member:com.netumscan.printersdk.ManagerResult$Completion:bbc424685d75dbb8`

JVM descriptor：`()[Lcom/netumscan/printersdk/ManagerResult$Completion;`

这是 Java 编译器为枚举生成的公共方法。

<a id="api-290d9cf4014b21ab8076"></a>

### public static final com.netumscan.printersdk.ManagerResult$Completion DATA_SENT

本次数据传输完成。

```java
public static final com.netumscan.printersdk.ManagerResult$Completion DATA_SENT;
```

只确认数据已交给传输，不推断纸面打印完成、出纸完成或介质设置已被硬件确认。

文档 ID：`android:member:com.netumscan.printersdk.ManagerResult$Completion:25379bc32a34dbdc`

JVM descriptor：`Lcom/netumscan/printersdk/ManagerResult$Completion;`

<a id="api-544156fc54fe7a081b86"></a>

### public static final com.netumscan.printersdk.ManagerResult$Completion DEVICE_CONFIRMED

设备明确确认完成。

```java
public static final com.netumscan.printersdk.ManagerResult$Completion DEVICE_CONFIRMED;
```

只有受支持协议提供足够设备响应证据时才可使用；普通写入成功不得升级为此值。

文档 ID：`android:member:com.netumscan.printersdk.ManagerResult$Completion:0f0213ab646475bb`

JVM descriptor：`Lcom/netumscan/printersdk/ManagerResult$Completion;`

<a id="api-40186482712649e8f8f2"></a>

### public static final com.netumscan.printersdk.ManagerResult$Completion NONE

没有传输完成证据。

```java
public static final com.netumscan.printersdk.ManagerResult$Completion NONE;
```

无输出的本地操作可成功且仍使用此级别；不表示纸面已完成。

文档 ID：`android:member:com.netumscan.printersdk.ManagerResult$Completion:b2288a2b928b5668`

JVM descriptor：`Lcom/netumscan/printersdk/ManagerResult$Completion;`

<a id="api-2784cdf665d09aa3045c"></a>

## ManagerResult.Outcome

请求的唯一终态分类。

```java
public final class com.netumscan.printersdk.ManagerResult$Outcome extends java.lang.Enum<com.netumscan.printersdk.ManagerResult$Outcome>
```

SUCCESS、FAILURE、CANCELLED分别表示成功、失败和取消；调用方还应检查完成级别与outputMayHaveOccurred。

文档 ID：`android:type:com.netumscan.printersdk.ManagerResult$Outcome`

枚举声明顺序（`ordinal()` 从 0 起；不作为持久化协议值）：

| ordinal | 枚举项 |
| --- | --- |
| 0 | `SUCCESS` |
| 1 | `FAILURE` |
| 2 | `CANCELLED` |

<a id="api-63c73722c03dc5aa768a"></a>

### public static com.netumscan.printersdk.ManagerResult$Outcome valueOf(java.lang.String)

按精确枚举名取得ManagerResult$Outcome值。

```java
public static com.netumscan.printersdk.ManagerResult$Outcome valueOf(java.lang.String);
```

Java编译器生成的方法，名称区分大小写；未知名称抛IllegalArgumentException，null抛NullPointerException，不执行设备IO。

文档 ID：`android:member:com.netumscan.printersdk.ManagerResult$Outcome:e5d94c2fff5d9609`

JVM descriptor：`(Ljava/lang/String;)Lcom/netumscan/printersdk/ManagerResult$Outcome;`

这是 Java 编译器为枚举生成的公共方法。

<a id="api-37db37ba405e67a9a9e4"></a>

### public static com.netumscan.printersdk.ManagerResult$Outcome[] values()

返回ManagerResult$Outcome的全部声明值。

```java
public static com.netumscan.printersdk.ManagerResult$Outcome[] values();
```

Java编译器生成的方法，返回声明顺序的新数组。枚举ordinal不是SDK稳定错误码或设备协议值。

文档 ID：`android:member:com.netumscan.printersdk.ManagerResult$Outcome:48814b7d3efdb6ee`

JVM descriptor：`()[Lcom/netumscan/printersdk/ManagerResult$Outcome;`

这是 Java 编译器为枚举生成的公共方法。

<a id="api-d7e9bb44403ab6f37534"></a>

### public static final com.netumscan.printersdk.ManagerResult$Outcome CANCELLED

请求取消。

```java
public static final com.netumscan.printersdk.ManagerResult$Outcome CANCELLED;
```

通常对应1005；最终结果仍可能带有已输出风险，实际后台退出前保留资源。

文档 ID：`android:member:com.netumscan.printersdk.ManagerResult$Outcome:6dc2c8acb95c7598`

JVM descriptor：`Lcom/netumscan/printersdk/ManagerResult$Outcome;`

<a id="api-851e4da4067f58b66f35"></a>

### public static final com.netumscan.printersdk.ManagerResult$Outcome FAILURE

请求失败。

```java
public static final com.netumscan.printersdk.ManagerResult$Outcome FAILURE;
```

错误原因见code/message；部分发送后失败可能已产生输出，不自动重放。

文档 ID：`android:member:com.netumscan.printersdk.ManagerResult$Outcome:e3c5f749b2c365b7`

JVM descriptor：`Lcom/netumscan/printersdk/ManagerResult$Outcome;`

<a id="api-515dec3613d752c82ce0"></a>

### public static final com.netumscan.printersdk.ManagerResult$Outcome SUCCESS

请求成功。

```java
public static final com.netumscan.printersdk.ManagerResult$Outcome SUCCESS;
```

错误码为0；仍须按completion区分数据传输完成和明确设备确认。

文档 ID：`android:member:com.netumscan.printersdk.ManagerResult$Outcome:458dd1ddc909dc38`

JVM descriptor：`Lcom/netumscan/printersdk/ManagerResult$Outcome;`

<a id="api-ba5c99041265566e5203"></a>

## PaperType

纸张的定位特征。

```java
public final class com.netumscan.printersdk.PaperType extends java.lang.Enum<com.netumscan.printersdk.PaperType>
```

仅包含间隙标签、黑标和连续纸。通过setPaperType作为独立IO请求采用；页面宽高不隐式选择纸型，也不隐式校准或持久化。

文档 ID：`android:type:com.netumscan.printersdk.PaperType`

枚举声明顺序（`ordinal()` 从 0 起；不作为持久化协议值）：

| ordinal | 枚举项 |
| --- | --- |
| 0 | `GAP_LABEL` |
| 1 | `BLACK_MARK` |
| 2 | `CONTINUOUS` |

<a id="api-c612dbdd30b777d12d12"></a>

### public static com.netumscan.printersdk.PaperType valueOf(java.lang.String)

按精确枚举名取得PaperType值。

```java
public static com.netumscan.printersdk.PaperType valueOf(java.lang.String);
```

Java编译器生成的方法，名称区分大小写；未知名称抛IllegalArgumentException，null抛NullPointerException，不执行设备IO。

文档 ID：`android:member:com.netumscan.printersdk.PaperType:343bb371a909b85a`

JVM descriptor：`(Ljava/lang/String;)Lcom/netumscan/printersdk/PaperType;`

这是 Java 编译器为枚举生成的公共方法。

<a id="api-e0912c7bd31b248852ac"></a>

### public static com.netumscan.printersdk.PaperType[] values()

返回PaperType的全部声明值。

```java
public static com.netumscan.printersdk.PaperType[] values();
```

Java编译器生成的方法，返回声明顺序的新数组。枚举ordinal不是SDK稳定错误码或设备协议值。

文档 ID：`android:member:com.netumscan.printersdk.PaperType:d866d54f3650c105`

JVM descriptor：`()[Lcom/netumscan/printersdk/PaperType;`

这是 Java 编译器为枚举生成的公共方法。

<a id="api-35bbd2007584932a9b68"></a>

### public static final com.netumscan.printersdk.PaperType BLACK_MARK

黑标纸。

```java
public static final com.netumscan.printersdk.PaperType BLACK_MARK;
```

作为setPaperType参数时映射黑标定位，不隐式设置黑标尺寸/偏移或校准。

文档 ID：`android:member:com.netumscan.printersdk.PaperType:a22ce97e3eff5ee1`

JVM descriptor：`Lcom/netumscan/printersdk/PaperType;`

<a id="api-3516ce79bdf8eeb4687e"></a>

### public static final com.netumscan.printersdk.PaperType CONTINUOUS

连续纸。

```java
public static final com.netumscan.printersdk.PaperType CONTINUOUS;
```

作为setPaperType参数时选择连续定位；不隐式改变页面宽高或添加切纸。

文档 ID：`android:member:com.netumscan.printersdk.PaperType:4fde727fb38041dd`

JVM descriptor：`Lcom/netumscan/printersdk/PaperType;`

<a id="api-d83b4b0f141b83961b3c"></a>

### public static final com.netumscan.printersdk.PaperType GAP_LABEL

间隙标签纸。

```java
public static final com.netumscan.printersdk.PaperType GAP_LABEL;
```

作为setPaperType参数时映射间隙定位，不隐式设置标签长度、间隙长度或校准。

文档 ID：`android:member:com.netumscan.printersdk.PaperType:53503fb1c2e52d8f`

JVM descriptor：`Lcom/netumscan/printersdk/PaperType;`

<a id="api-a36171733f6c01585161"></a>

## PrinterCapabilities

当前SDK实现与设备依据合成的只读能力。

```java
public final class com.netumscan.printersdk.PrinterCapabilities
```

每项能力为已知支持、已知不支持或未知，未知不能视作支持；毫米范围为空表示缺少型号或几何依据。能力查询不是打印机实时状态查询。

文档 ID：`android:type:com.netumscan.printersdk.PrinterCapabilities`

<a id="api-bbf41ae5d2ee9b91f01c"></a>

### public final com.netumscan.printersdk.PrinterCapabilities$Support imagePrinting

图像打印的支持程度。

```java
public final com.netumscan.printersdk.PrinterCapabilities$Support imagePrinting;
```

综合SDK实现和设备支持依据，未知时保留UNKNOWN/unknown。支持不豁免页面、图像和连接状态校验。

文档 ID：`android:member:com.netumscan.printersdk.PrinterCapabilities:022e17806bb6418a`

JVM descriptor：`Lcom/netumscan/printersdk/PrinterCapabilities$Support;`

编译器注解：`@androidx.annotation.NonNull`。

<a id="api-91f7c6df74a2d3f017ce"></a>

### public final com.netumscan.printersdk.PrinterCapabilities$Support labelPositioning

独立定位到下一标签的支持程度。

```java
public final com.netumscan.printersdk.PrinterCapabilities$Support labelPositioning;
```

对应locateNextLabel；普通图像打印不会隐式执行定位。不支持或未知时不能承诺硬件会前进到正确标签。

文档 ID：`android:member:com.netumscan.printersdk.PrinterCapabilities:5ba903f71f818a45`

JVM descriptor：`Lcom/netumscan/printersdk/PrinterCapabilities$Support;`

编译器注解：`@androidx.annotation.NonNull`。

<a id="api-3a44de2eb74759a40055"></a>

### public final com.netumscan.printersdk.PrinterCapabilities$Support paperTypeSelection

独立纸型设置请求的支持程度。

```java
public final com.netumscan.printersdk.PrinterCapabilities$Support paperTypeSelection;
```

对应setPaperType；明确不支持或未知不能视为已成功设置，纸面宽高不替代纸型命令。

文档 ID：`android:member:com.netumscan.printersdk.PrinterCapabilities:5e0094ad48e9dc6a`

JVM descriptor：`Lcom/netumscan/printersdk/PrinterCapabilities$Support;`

编译器注解：`@androidx.annotation.NonNull`。

<a id="api-2c333aa4b5ef86241c3f"></a>

### public final com.netumscan.printersdk.PrinterCapabilities$Support rawCommands

原始命令发送的支持程度。

```java
public final com.netumscan.printersdk.PrinterCapabilities$Support rawCommands;
```

Raw仍进入与打印相同的设备FIFO。支持发送字节不证明这些字节适用于当前型号，也不证明纸面完成。

文档 ID：`android:member:com.netumscan.printersdk.PrinterCapabilities:5a189c7f22023530`

JVM descriptor：`Lcom/netumscan/printersdk/PrinterCapabilities$Support;`

编译器注解：`@androidx.annotation.NonNull`。

<a id="api-3d2e1a7d2610080e5ba7"></a>

### public final com.netumscan.printersdk.PrinterCapabilities$Support statusQuery

硬件状态请求的支持程度。

```java
public final com.netumscan.printersdk.PrinterCapabilities$Support statusQuery;
```

支持必须有协议和读取响应依据，具备读取函数不自动等于可查询状态。UNKNOWN/unknown仍表示未知。

文档 ID：`android:member:com.netumscan.printersdk.PrinterCapabilities:0f9aeb00f9942b9d`

JVM descriptor：`Lcom/netumscan/printersdk/PrinterCapabilities$Support;`

编译器注解：`@androidx.annotation.NonNull`。

<a id="api-2eb04c9797d9fd93829f"></a>

### public final java.lang.Double maxPageHeightMm

有依据的最大页面高度，单位mm。

```java
public final java.lang.Double maxPageHeightMm;
```

由当前型号精确点密度与原生几何上限换算；缺少型号或高度依据时为空，不用零值或特殊字符串表示未知。

文档 ID：`android:member:com.netumscan.printersdk.PrinterCapabilities:2421998b2f8dc5c2`

JVM descriptor：`Ljava/lang/Double;`

编译器注解：`@androidx.annotation.Nullable`。

<a id="api-14af060f8ae7a4432187"></a>

### public final java.lang.Double maxPrintWidthMm

当前型号允许的最大打印宽度，单位mm。

```java
public final java.lang.Double maxPrintWidthMm;
```

由已采用型号的最大点宽和精确dots/mm换算；未采用型号时为空。它不是从图片DPI、平台density或设备名称之外的探测推断。

文档 ID：`android:member:com.netumscan.printersdk.PrinterCapabilities:3547f334ece36faf`

JVM descriptor：`Ljava/lang/Double;`

编译器注解：`@androidx.annotation.Nullable`。

<a id="api-b817fe4ca0353d12c928"></a>

## PrinterCapabilities.Support

能力支持程度的三态值。

```java
public final class com.netumscan.printersdk.PrinterCapabilities$Support extends java.lang.Enum<com.netumscan.printersdk.PrinterCapabilities$Support>
```

UNKNOWN保留未知，UNSUPPORTED表示未实现或明确不支持，SUPPORTED表示具备相应设备依据；不能仅凭SDK能编码就认定硬件支持。

文档 ID：`android:type:com.netumscan.printersdk.PrinterCapabilities$Support`

枚举声明顺序（`ordinal()` 从 0 起；不作为持久化协议值）：

| ordinal | 枚举项 |
| --- | --- |
| 0 | `UNKNOWN` |
| 1 | `UNSUPPORTED` |
| 2 | `SUPPORTED` |

<a id="api-20cc35ae09fbef927bc0"></a>

### public static com.netumscan.printersdk.PrinterCapabilities$Support valueOf(java.lang.String)

按精确枚举名取得PrinterCapabilities$Support值。

```java
public static com.netumscan.printersdk.PrinterCapabilities$Support valueOf(java.lang.String);
```

Java编译器生成的方法，名称区分大小写；未知名称抛IllegalArgumentException，null抛NullPointerException，不执行设备IO。

文档 ID：`android:member:com.netumscan.printersdk.PrinterCapabilities$Support:0ac0f62a4ed9a173`

JVM descriptor：`(Ljava/lang/String;)Lcom/netumscan/printersdk/PrinterCapabilities$Support;`

这是 Java 编译器为枚举生成的公共方法。

<a id="api-c4f162695f2c5f42aa78"></a>

### public static com.netumscan.printersdk.PrinterCapabilities$Support[] values()

返回PrinterCapabilities$Support的全部声明值。

```java
public static com.netumscan.printersdk.PrinterCapabilities$Support[] values();
```

Java编译器生成的方法，返回声明顺序的新数组。枚举ordinal不是SDK稳定错误码或设备协议值。

文档 ID：`android:member:com.netumscan.printersdk.PrinterCapabilities$Support:74f2f80c82e22674`

JVM descriptor：`()[Lcom/netumscan/printersdk/PrinterCapabilities$Support;`

这是 Java 编译器为枚举生成的公共方法。

<a id="api-18fd48cd65ddc68dbc84"></a>

### public static final com.netumscan.printersdk.PrinterCapabilities$Support SUPPORTED

具备支持依据。

```java
public static final com.netumscan.printersdk.PrinterCapabilities$Support SUPPORTED;
```

仍受当前连接、参数和型号范围限制；此值不是纸面输出成功证明。

文档 ID：`android:member:com.netumscan.printersdk.PrinterCapabilities$Support:97d33bb3269e4d4c`

JVM descriptor：`Lcom/netumscan/printersdk/PrinterCapabilities$Support;`

<a id="api-d97b2b9977e44f69c203"></a>

### public static final com.netumscan.printersdk.PrinterCapabilities$Support UNKNOWN

设备支持程度未知。

```java
public static final com.netumscan.printersdk.PrinterCapabilities$Support UNKNOWN;
```

没有足够设备支持依据，不能把此值当作SUPPORTED或正常硬件状态。

文档 ID：`android:member:com.netumscan.printersdk.PrinterCapabilities$Support:72fb39483fa5216b`

JVM descriptor：`Lcom/netumscan/printersdk/PrinterCapabilities$Support;`

<a id="api-f6393850878ee94867c6"></a>

### public static final com.netumscan.printersdk.PrinterCapabilities$Support UNSUPPORTED

未实现或明确不支持。

```java
public static final com.netumscan.printersdk.PrinterCapabilities$Support UNSUPPORTED;
```

对应SDK实现缺失或明确设备不支持；调用仍以具体请求的稳定错误码为准。

文档 ID：`android:member:com.netumscan.printersdk.PrinterCapabilities$Support:f9c0d12be8649af6`

JVM descriptor：`Lcom/netumscan/printersdk/PrinterCapabilities$Support;`

<a id="api-bfc786be5b911ef74d19"></a>

## PrinterManager

移动端唯一公开运行时控制入口。

```java
public final class com.netumscan.printersdk.PrinterManager
```

每实例同时管理至多一台设备；请求立即返回身份，后台准备和原设备FIFO负责执行，最终回调在主线程异步交付。无自动重连、续打或重放。

文档 ID：`android:type:com.netumscan.printersdk.PrinterManager`

编译器注解：`@androidx.annotation.AnyThread`。

<a id="api-6761fb538c137c164625"></a>

### public com.netumscan.printersdk.PrinterManager(android.content.Context)

创建独立的打印Manager。

```java
public com.netumscan.printersdk.PrinterManager(android.content.Context);
```

context必须非null；仅保留application Context，不持有Activity。每实例拥有独立会话/请求生命周期，回调默认主线程异步交付；创建不打开蓝牙或执行打印IO。

文档 ID：`android:member:com.netumscan.printersdk.PrinterManager:989371a8a2f8fb1d`

JVM descriptor：`(Landroid/content/Context;)V`

参数 0（从 0 起）注解：`@androidx.annotation.NonNull`。

<a id="api-67aaa2c77fa3ef876418"></a>

### public com.netumscan.printersdk.RequestId bondedDevices(com.netumscan.printersdk.PrinterManager$Callback<java.util.List<com.netumscan.printersdk.DiscoveredDevice>>)

异步读取Android系统已配对蓝牙设备。

```java
public com.netumscan.printersdk.RequestId bondedDevices(com.netumscan.printersdk.PrinterManager$Callback<java.util.List<com.netumscan.printersdk.DiscoveredDevice>>);
```

列表不按经典蓝牙/BLE过滤，不证明SPP可用；App负责系统权限。该方法不是发现新设备，也不发起连接。立即返回不透明请求身份，结果在主线程异步回调且仅一次；没有自动重试、重连或重放。

文档 ID：`android:member:com.netumscan.printersdk.PrinterManager:250ef332e85fdb96`

JVM descriptor：`(Lcom/netumscan/printersdk/PrinterManager$Callback;)Lcom/netumscan/printersdk/RequestId;`

编译器注解：`@androidx.annotation.NonNull`。

参数 0（从 0 起）注解：`@androidx.annotation.NonNull`。

<a id="api-aa164db0d48e84572c33"></a>

### public com.netumscan.printersdk.RequestId capabilities(com.netumscan.printersdk.PrinterManager$Callback<com.netumscan.printersdk.PrinterCapabilities>)

异步读取本地能力快照。

```java
public com.netumscan.printersdk.RequestId capabilities(com.netumscan.printersdk.PrinterManager$Callback<com.netumscan.printersdk.PrinterCapabilities>);
```

需要有效连接；结果为三态能力和可空毫米范围，不执行硬件状态查询，也不把未知升级为支持。立即返回不透明请求身份，结果在主线程异步回调且仅一次；没有自动重试、重连或重放。

文档 ID：`android:member:com.netumscan.printersdk.PrinterManager:3b59a91b1fcc68e8`

JVM descriptor：`(Lcom/netumscan/printersdk/PrinterManager$Callback;)Lcom/netumscan/printersdk/RequestId;`

编译器注解：`@androidx.annotation.NonNull`。

参数 0（从 0 起）注解：`@androidx.annotation.NonNull`。

<a id="api-704c0cf7d94eeebbfb7f"></a>

### public com.netumscan.printersdk.RequestId close(com.netumscan.printersdk.PrinterManager$Callback<java.lang.Void>)

异步永久关闭该Manager。

```java
public com.netumscan.printersdk.RequestId close(com.netumscan.printersdk.PrinterManager$Callback<java.lang.Void>);
```

执行断开与资源清理并拒绝后续业务请求；重复close保持幂等。清理回调等待实际资源退出，不通过提前取消释放仍在工作的图像预算。立即返回不透明请求身份，结果在主线程异步回调且仅一次；没有自动重试、重连或重放。

文档 ID：`android:member:com.netumscan.printersdk.PrinterManager:9ffe7f34b33337a5`

JVM descriptor：`(Lcom/netumscan/printersdk/PrinterManager$Callback;)Lcom/netumscan/printersdk/RequestId;`

编译器注解：`@androidx.annotation.NonNull`。

参数 0（从 0 起）注解：`@androidx.annotation.NonNull`。

<a id="api-6ecc74c53b5e1d84725b"></a>

### public com.netumscan.printersdk.RequestId connectBle(com.netumscan.printersdk.DiscoveredDevice, com.netumscan.printersdk.PrinterManager$Callback<com.netumscan.printersdk.ConnectionInfo>, com.netumscan.printersdk.PrinterManager$DisconnectedListener)

连接BLE设备。

```java
public com.netumscan.printersdk.RequestId connectBle(com.netumscan.printersdk.DiscoveredDevice, com.netumscan.printersdk.PrinterManager$Callback<com.netumscan.printersdk.ConnectionInfo>, com.netumscan.printersdk.PrinterManager$DisconnectedListener);
```

设备值和回调不可为null，onDisconnected可为null；内部连接超时10秒，每Manager至多一台设备，已有会话/清理时拒绝。按原发现名推荐收录型号，未知名失败1004并要求显式选择，不能猜服务UUID。连接成功返回ConnectionInfo；之后意外丢失至多交付一次独立断连通知，显式清理不触发。立即返回不透明请求身份，结果在主线程异步回调且仅一次；没有自动重试、重连或重放。

文档 ID：`android:member:com.netumscan.printersdk.PrinterManager:57134b41c845fc1d`

JVM descriptor：`(Lcom/netumscan/printersdk/DiscoveredDevice;Lcom/netumscan/printersdk/PrinterManager$Callback;Lcom/netumscan/printersdk/PrinterManager$DisconnectedListener;)Lcom/netumscan/printersdk/RequestId;`

编译器注解：`@androidx.annotation.NonNull`。

参数 0（从 0 起）注解：`@androidx.annotation.NonNull`。

参数 1（从 0 起）注解：`@androidx.annotation.NonNull`。

参数 2（从 0 起）注解：`@androidx.annotation.Nullable`。

<a id="api-3eaf68595717142762f4"></a>

### public com.netumscan.printersdk.RequestId connectBle(com.netumscan.printersdk.DiscoveredDevice, com.netumscan.printersdk.PrinterModel, com.netumscan.printersdk.PrinterManager$Callback<com.netumscan.printersdk.ConnectionInfo>, com.netumscan.printersdk.PrinterManager$DisconnectedListener)

连接BLE设备并显式采用型号。

```java
public com.netumscan.printersdk.RequestId connectBle(com.netumscan.printersdk.DiscoveredDevice, com.netumscan.printersdk.PrinterModel, com.netumscan.printersdk.PrinterManager$Callback<com.netumscan.printersdk.ConnectionInfo>, com.netumscan.printersdk.PrinterManager$DisconnectedListener);
```

设备值和回调不可为null，onDisconnected可为null；内部连接超时10秒，每Manager至多一台设备，已有会话/清理时拒绝。显式型号优先，使用收录型号的通信及打印参数；选择不是硬件认证。连接成功返回ConnectionInfo；之后意外丢失至多交付一次独立断连通知，显式清理不触发。立即返回不透明请求身份，结果在主线程异步回调且仅一次；没有自动重试、重连或重放。

文档 ID：`android:member:com.netumscan.printersdk.PrinterManager:597230c146479782`

JVM descriptor：`(Lcom/netumscan/printersdk/DiscoveredDevice;Lcom/netumscan/printersdk/PrinterModel;Lcom/netumscan/printersdk/PrinterManager$Callback;Lcom/netumscan/printersdk/PrinterManager$DisconnectedListener;)Lcom/netumscan/printersdk/RequestId;`

编译器注解：`@androidx.annotation.NonNull`。

参数 0（从 0 起）注解：`@androidx.annotation.NonNull`。

参数 1（从 0 起）注解：`@androidx.annotation.NonNull`。

参数 2（从 0 起）注解：`@androidx.annotation.NonNull`。

参数 3（从 0 起）注解：`@androidx.annotation.Nullable`。

<a id="api-51e711fb43e27540569b"></a>

### public com.netumscan.printersdk.RequestId connectSpp(com.netumscan.printersdk.DiscoveredDevice, com.netumscan.printersdk.PrinterManager$Callback<com.netumscan.printersdk.ConnectionInfo>, com.netumscan.printersdk.PrinterManager$DisconnectedListener)

连接经典蓝牙SPP/RFCOMM设备。

```java
public com.netumscan.printersdk.RequestId connectSpp(com.netumscan.printersdk.DiscoveredDevice, com.netumscan.printersdk.PrinterManager$Callback<com.netumscan.printersdk.ConnectionInfo>, com.netumscan.printersdk.PrinterManager$DisconnectedListener);
```

设备值和回调不可为null，onDisconnected可为null；内部连接超时10秒，每Manager至多一台设备，已有会话/清理时拒绝。按原发现名推荐型号；未知名称仍可建立仅通信连接和Raw，结构化打印前需连接后setPrinterModel。连接成功返回ConnectionInfo；之后意外丢失至多交付一次独立断连通知，显式清理不触发。立即返回不透明请求身份，结果在主线程异步回调且仅一次；没有自动重试、重连或重放。

文档 ID：`android:member:com.netumscan.printersdk.PrinterManager:34be794cdc369ff9`

JVM descriptor：`(Lcom/netumscan/printersdk/DiscoveredDevice;Lcom/netumscan/printersdk/PrinterManager$Callback;Lcom/netumscan/printersdk/PrinterManager$DisconnectedListener;)Lcom/netumscan/printersdk/RequestId;`

编译器注解：`@androidx.annotation.NonNull`。

参数 0（从 0 起）注解：`@androidx.annotation.NonNull`。

参数 1（从 0 起）注解：`@androidx.annotation.NonNull`。

参数 2（从 0 起）注解：`@androidx.annotation.Nullable`。

<a id="api-c15139d998f3841e2250"></a>

### public com.netumscan.printersdk.RequestId connectSpp(com.netumscan.printersdk.DiscoveredDevice, com.netumscan.printersdk.PrinterModel, com.netumscan.printersdk.PrinterManager$Callback<com.netumscan.printersdk.ConnectionInfo>, com.netumscan.printersdk.PrinterManager$DisconnectedListener)

连接经典蓝牙SPP/RFCOMM设备并显式采用型号。

```java
public com.netumscan.printersdk.RequestId connectSpp(com.netumscan.printersdk.DiscoveredDevice, com.netumscan.printersdk.PrinterModel, com.netumscan.printersdk.PrinterManager$Callback<com.netumscan.printersdk.ConnectionInfo>, com.netumscan.printersdk.PrinterManager$DisconnectedListener);
```

设备值和回调不可为null，onDisconnected可为null；内部连接超时10秒，每Manager至多一台设备，已有会话/清理时拒绝。显式型号优先，使用收录型号的通信及打印参数；选择不是硬件认证。连接成功返回ConnectionInfo；之后意外丢失至多交付一次独立断连通知，显式清理不触发。立即返回不透明请求身份，结果在主线程异步回调且仅一次；没有自动重试、重连或重放。

文档 ID：`android:member:com.netumscan.printersdk.PrinterManager:8aab29eea10c2e4b`

JVM descriptor：`(Lcom/netumscan/printersdk/DiscoveredDevice;Lcom/netumscan/printersdk/PrinterModel;Lcom/netumscan/printersdk/PrinterManager$Callback;Lcom/netumscan/printersdk/PrinterManager$DisconnectedListener;)Lcom/netumscan/printersdk/RequestId;`

编译器注解：`@androidx.annotation.NonNull`。

参数 0（从 0 起）注解：`@androidx.annotation.NonNull`。

参数 1（从 0 起）注解：`@androidx.annotation.NonNull`。

参数 2（从 0 起）注解：`@androidx.annotation.NonNull`。

参数 3（从 0 起）注解：`@androidx.annotation.Nullable`。

<a id="api-eace0338b53324c15ab8"></a>

### public com.netumscan.printersdk.RequestId disconnect(com.netumscan.printersdk.PrinterManager$Callback<java.lang.Void>)

异步断开并清理当前设备会话。

```java
public com.netumscan.printersdk.RequestId disconnect(com.netumscan.printersdk.PrinterManager$Callback<java.lang.Void>);
```

取消未完成请求、停止发现并回收当前连接；终态与清理完成按既有顺序交付。可重复调用，完成后Manager可再次连接；不会触发本次连接的意外断连回调。立即返回不透明请求身份，结果在主线程异步回调且仅一次；没有自动重试、重连或重放。

文档 ID：`android:member:com.netumscan.printersdk.PrinterManager:9b3300e7a5bc65cd`

JVM descriptor：`(Lcom/netumscan/printersdk/PrinterManager$Callback;)Lcom/netumscan/printersdk/RequestId;`

编译器注解：`@androidx.annotation.NonNull`。

参数 0（从 0 起）注解：`@androidx.annotation.NonNull`。

<a id="api-a332a8f7ec9cf155f89c"></a>

### public com.netumscan.printersdk.RequestId locateNextLabel(com.netumscan.printersdk.PrinterManager$Callback<java.lang.Void>)

通过独立设备请求定位下一标签。

```java
public com.netumscan.printersdk.RequestId locateNextLabel(com.netumscan.printersdk.PrinterManager$Callback<java.lang.Void>);
```

需要有效连接、已采用型号和相应协议能力；与打印、Raw、状态查询共享FIFO。不支持报告1004。普通打印不自动调用；发送完成不保证实际纸张已定位。立即返回不透明请求身份，结果在主线程异步回调且仅一次；没有自动重试、重连或重放。

文档 ID：`android:member:com.netumscan.printersdk.PrinterManager:98726094484858bd`

JVM descriptor：`(Lcom/netumscan/printersdk/PrinterManager$Callback;)Lcom/netumscan/printersdk/RequestId;`

编译器注解：`@androidx.annotation.NonNull`。

参数 0（从 0 起）注解：`@androidx.annotation.NonNull`。

<a id="api-fa12d92bee1dce11b51d"></a>

### public com.netumscan.printersdk.RequestId printEncodedImage(byte[], double, double, com.netumscan.printersdk.PrinterManager$Callback<java.lang.Void>)

直接打印一张PNG/JPEG编码图片。

```java
public com.netumscan.printersdk.RequestId printEncodedImage(byte[], double, double, com.netumscan.printersdk.PrinterManager$Callback<java.lang.Void>);
```

输入为完整PNG/JPEG byte[]或Data，可直接承接Flutter Uint8List；不接收Base64或裸RGBA。返回前复制稳定编码快照，解码/EXIF校正在后台执行；编码最多32MiB，损坏/不支持报告1001、资源超限1008。widthMm与heightMm必须为正有限毫米值，并受已采用型号宽高和资源限制。固定一份、阈值128、不反色；等比缩小并居中补白，不放大不裁切。页面尺寸不取图片DPI/density；不附加FORM、切纸或下一标签定位。立即返回不透明请求身份，结果在主线程异步回调且仅一次；没有自动重试、重连或重放。

文档 ID：`android:member:com.netumscan.printersdk.PrinterManager:0bd33b626f9d136c`

JVM descriptor：`([BDDLcom/netumscan/printersdk/PrinterManager$Callback;)Lcom/netumscan/printersdk/RequestId;`

编译器注解：`@androidx.annotation.NonNull`。

参数 0（从 0 起）注解：`@androidx.annotation.NonNull`。

参数 3（从 0 起）注解：`@androidx.annotation.NonNull`。

<a id="api-5a0ffdb00f8ebfe33ce2"></a>

### public com.netumscan.printersdk.RequestId printImage(android.graphics.Bitmap, double, double, com.netumscan.printersdk.PrinterManager$Callback<java.lang.Void>)

直接打印平台图片。

```java
public com.netumscan.printersdk.RequestId printImage(android.graphics.Bitmap, double, double, com.netumscan.printersdk.PrinterManager$Callback<java.lang.Void>);
```

Android接收Bitmap，iOS接收具有CGImage且非动画的UIImage。返回前取得独立像素快照，调用方不得在快照期间并发修改/回收输入；返回后可自行管理原图。方向校正、白底灰度、缩放在有界后台进行，二值化/协议按条带处理。widthMm与heightMm必须为正有限毫米值，并受已采用型号宽高和资源限制。固定一份、阈值128、不反色；等比缩小并居中补白，不放大不裁切。页面尺寸不取图片DPI/density；不附加FORM、切纸或下一标签定位。 无效输入报告1001、资源超限1008。立即返回不透明请求身份，结果在主线程异步回调且仅一次；没有自动重试、重连或重放。

文档 ID：`android:member:com.netumscan.printersdk.PrinterManager:f1d9223ff8bd7b98`

JVM descriptor：`(Landroid/graphics/Bitmap;DDLcom/netumscan/printersdk/PrinterManager$Callback;)Lcom/netumscan/printersdk/RequestId;`

编译器注解：`@androidx.annotation.NonNull`。

参数 0（从 0 起）注解：`@androidx.annotation.NonNull`。

参数 3（从 0 起）注解：`@androidx.annotation.NonNull`。

<a id="api-5f4e02a67aa5447d9503"></a>

### public com.netumscan.printersdk.RequestId queryStatus(com.netumscan.printersdk.PrinterManager$Callback<com.netumscan.printersdk.StatusResult>)

查询当前型号支持的硬件状态。

```java
public com.netumscan.printersdk.RequestId queryStatus(com.netumscan.printersdk.PrinterManager$Callback<com.netumscan.printersdk.StatusResult>);
```

需要有效连接与已采用型号；在设备FIFO中完成请求/响应，超时经最终回调报告。不支持报告1004，失败不给假正常值；成功的四项状态仍分别可能未知。立即返回不透明请求身份，结果在主线程异步回调且仅一次；没有自动重试、重连或重放。

文档 ID：`android:member:com.netumscan.printersdk.PrinterManager:2a893ee6c1f2bf16`

JVM descriptor：`(Lcom/netumscan/printersdk/PrinterManager$Callback;)Lcom/netumscan/printersdk/RequestId;`

编译器注解：`@androidx.annotation.NonNull`。

参数 0（从 0 起）注解：`@androidx.annotation.NonNull`。

<a id="api-3d3b4989ecd68e4791da"></a>

### public com.netumscan.printersdk.RequestId scanBle(com.netumscan.printersdk.PrinterManager$Callback<java.util.List<com.netumscan.printersdk.DiscoveredDevice>>)

异步扫描BLE设备。

```java
public com.netumscan.printersdk.RequestId scanBle(com.netumscan.printersdk.PrinterManager$Callback<java.util.List<com.netumscan.printersdk.DiscoveredDevice>>);
```

默认扫描5秒，不要求先连接；同Manager同时仅一项扫描，返回去重的发现值。权限和系统蓝牙授权由App处理；扫描不连接，也不证明型号或打印能力。立即返回不透明请求身份，结果在主线程异步回调且仅一次；没有自动重试、重连或重放。

文档 ID：`android:member:com.netumscan.printersdk.PrinterManager:fb6b45d71e47e9ac`

JVM descriptor：`(Lcom/netumscan/printersdk/PrinterManager$Callback;)Lcom/netumscan/printersdk/RequestId;`

编译器注解：`@androidx.annotation.NonNull`。

参数 0（从 0 起）注解：`@androidx.annotation.NonNull`。

<a id="api-89db5e6d23981953297a"></a>

### public com.netumscan.printersdk.RequestId sdkInfo(com.netumscan.printersdk.PrinterManager$Callback<com.netumscan.printersdk.SdkInfo>)

异步读取实际原生SDK身份。

```java
public com.netumscan.printersdk.RequestId sdkInfo(com.netumscan.printersdk.PrinterManager$Callback<com.netumscan.printersdk.SdkInfo>);
```

无需设备连接；关闭或正在清理的Manager拒绝请求。结果来自已加载/链接库，未知构建标识保留空值。立即返回不透明请求身份，结果在主线程异步回调且仅一次；没有自动重试、重连或重放。

文档 ID：`android:member:com.netumscan.printersdk.PrinterManager:cabce4d23a0c13a1`

JVM descriptor：`(Lcom/netumscan/printersdk/PrinterManager$Callback;)Lcom/netumscan/printersdk/RequestId;`

编译器注解：`@androidx.annotation.NonNull`。

参数 0（从 0 起）注解：`@androidx.annotation.NonNull`。

<a id="api-25a625be30ca197a919e"></a>

### public com.netumscan.printersdk.RequestId sendRaw(byte[], com.netumscan.printersdk.PrinterManager$Callback<java.lang.Void>)

在原设备队列中发送一份原始命令。

```java
public com.netumscan.printersdk.RequestId sendRaw(byte[], com.netumscan.printersdk.PrinterManager$Callback<java.lang.Void>);
```

输入必须非空且不超过1MiB，返回前复制稳定快照；同设备与图像/控制请求串行执行，失效先前纸型证据。允许未采用打印型号的有效通信连接使用；App自行保证命令适用。立即返回不透明请求身份，结果在主线程异步回调且仅一次；没有自动重试、重连或重放。

文档 ID：`android:member:com.netumscan.printersdk.PrinterManager:8da386595589c2ee`

JVM descriptor：`([BLcom/netumscan/printersdk/PrinterManager$Callback;)Lcom/netumscan/printersdk/RequestId;`

编译器注解：`@androidx.annotation.NonNull`。

参数 0（从 0 起）注解：`@androidx.annotation.NonNull`。

参数 1（从 0 起）注解：`@androidx.annotation.NonNull`。

<a id="api-ef4966e26d0de35bb92c"></a>

### public com.netumscan.printersdk.RequestId setPaperType(com.netumscan.printersdk.PaperType, com.netumscan.printersdk.PrinterManager$Callback<java.lang.Void>)

通过独立设备请求设置纸型。

```java
public com.netumscan.printersdk.RequestId setPaperType(com.netumscan.printersdk.PaperType, com.netumscan.printersdk.PrinterManager$Callback<java.lang.Void>);
```

仅接受间隙标签、黑标、连续纸；不是本地变量赋值，不隐式设宽高、校准或持久化。需要有效连接和型号，原设备FIFO串行执行；未知/不支持报告1004。成功仅按completion报告实际证据。立即返回不透明请求身份，结果在主线程异步回调且仅一次；没有自动重试、重连或重放。

文档 ID：`android:member:com.netumscan.printersdk.PrinterManager:98df2c7a6b8863dc`

JVM descriptor：`(Lcom/netumscan/printersdk/PaperType;Lcom/netumscan/printersdk/PrinterManager$Callback;)Lcom/netumscan/printersdk/RequestId;`

编译器注解：`@androidx.annotation.NonNull`。

参数 0（从 0 起）注解：`@androidx.annotation.NonNull`。

参数 1（从 0 起）注解：`@androidx.annotation.NonNull`。

<a id="api-179f1f3428b6c5276b1e"></a>

### public com.netumscan.printersdk.RequestId setPrinterModel(com.netumscan.printersdk.PrinterModel, com.netumscan.printersdk.PrinterManager$Callback<com.netumscan.printersdk.ConnectionInfo>)

连接后空闲时采用已收录型号。

```java
public com.netumscan.printersdk.RequestId setPrinterModel(com.netumscan.printersdk.PrinterModel, com.netumscan.printersdk.PrinterManager$Callback<com.netumscan.printersdk.ConnectionInfo>);
```

只修改本地默认通信/打印配置，不发送纸型命令或自动重连。必须已连接且业务空闲；忙碌失败1002、失败保留旧配置，成功返回ConnectionInfo并失效先前纸型证据。立即返回不透明请求身份，结果在主线程异步回调且仅一次；没有自动重试、重连或重放。

文档 ID：`android:member:com.netumscan.printersdk.PrinterManager:466250fe96845361`

JVM descriptor：`(Lcom/netumscan/printersdk/PrinterModel;Lcom/netumscan/printersdk/PrinterManager$Callback;)Lcom/netumscan/printersdk/RequestId;`

编译器注解：`@androidx.annotation.NonNull`。

参数 0（从 0 起）注解：`@androidx.annotation.NonNull`。

参数 1（从 0 起）注解：`@androidx.annotation.NonNull`。

<a id="api-c495b55a4368ea9d6525"></a>

### public void cancel(com.netumscan.printersdk.RequestId)

向来源Manager的单个请求发送取消信号。

```java
public void cancel(com.netumscan.printersdk.RequestId);
```

不是普通FIFO命令。未知、其他Manager、已完成或清理请求身份无效果；准备和发送阶段协作取消，实际退出前保留占用资源。取消可能已经输出，最终回调仍只有一次，不自动重放。

文档 ID：`android:member:com.netumscan.printersdk.PrinterManager:80c2dece04d1d2f6`

JVM descriptor：`(Lcom/netumscan/printersdk/RequestId;)V`

参数 0（从 0 起）注解：`@androidx.annotation.Nullable`。

<a id="api-a8eff59edaee0576f508"></a>

## PrinterManager.Callback

请求终态的函数式回调。

```java
public interface com.netumscan.printersdk.PrinterManager$Callback<T>
```

一次请求只异步交付一次ManagerResult，在Android主线程执行；回调可能对应成功、失败或取消。耗时业务应由App自行移出主线程。

文档 ID：`android:type:com.netumscan.printersdk.PrinterManager$Callback`

编译器注解：`@java.lang.FunctionalInterface`。

<a id="api-fda923ad62298237ac93"></a>

### public abstract void onComplete(com.netumscan.printersdk.ManagerResult<T>)

在主线程异步接收该请求的唯一终态。

```java
public abstract void onComplete(com.netumscan.printersdk.ManagerResult<T>);
```

result非null；本回调可对应成功、失败或取消。不要在回调阻塞主线程，按requestId关联请求并检查completion/输出风险。

文档 ID：`android:member:com.netumscan.printersdk.PrinterManager$Callback:ad4a015b2cb5b1b7`

JVM descriptor：`(Lcom/netumscan/printersdk/ManagerResult;)V`

编译器注解：`@androidx.annotation.MainThread`。

参数 0（从 0 起）注解：`@androidx.annotation.NonNull`。

<a id="api-9149b393dfb876ce6b8e"></a>

## PrinterManager.DisconnectedListener

连接成功之后的意外断连回调。

```java
public interface com.netumscan.printersdk.PrinterManager$DisconnectedListener
```

每条成功连接至多交付一次，在主线程异步执行，携带该连接请求的身份。显式disconnect/close不作为意外断连事件。

文档 ID：`android:type:com.netumscan.printersdk.PrinterManager$DisconnectedListener`

编译器注解：`@java.lang.FunctionalInterface`。

<a id="api-4ba251e1239d4f7bbd7f"></a>

### public abstract void onDisconnected(com.netumscan.printersdk.ManagerResult<java.lang.Void>)

在主线程接收该连接的一次意外断连结果。

```java
public abstract void onDisconnected(com.netumscan.printersdk.ManagerResult<java.lang.Void>);
```

仅在连接成功之后使用，结果关联原连接RequestId，value为空。显式断开/关闭不触发；不自动重连或重放中断的任务。

文档 ID：`android:member:com.netumscan.printersdk.PrinterManager$DisconnectedListener:bec808a8d85c7ee3`

JVM descriptor：`(Lcom/netumscan/printersdk/ManagerResult;)V`

编译器注解：`@androidx.annotation.MainThread`。

参数 0（从 0 起）注解：`@androidx.annotation.NonNull`。

<a id="api-59800c82d4445d8c7af4"></a>

## PrinterModel

SDK已经收录的打印机型号。

```java
public final class com.netumscan.printersdk.PrinterModel extends java.lang.Enum<com.netumscan.printersdk.PrinterModel>
```

目前只包含LT-M90。选择型号是本地通信与打印配置，不是硬件探测；不得把未知设备猜测为此型号。

文档 ID：`android:type:com.netumscan.printersdk.PrinterModel`

枚举声明顺序（`ordinal()` 从 0 起；不作为持久化协议值）：

| ordinal | 枚举项 |
| --- | --- |
| 0 | `LT_M90` |

<a id="api-b441a6b708a724bcff0a"></a>

### public static com.netumscan.printersdk.PrinterModel valueOf(java.lang.String)

按精确枚举名取得PrinterModel值。

```java
public static com.netumscan.printersdk.PrinterModel valueOf(java.lang.String);
```

Java编译器生成的方法，名称区分大小写；未知名称抛IllegalArgumentException，null抛NullPointerException，不执行设备IO。

文档 ID：`android:member:com.netumscan.printersdk.PrinterModel:e3ea6b1241cbd0b8`

JVM descriptor：`(Ljava/lang/String;)Lcom/netumscan/printersdk/PrinterModel;`

这是 Java 编译器为枚举生成的公共方法。

<a id="api-e97bdab069ec578a45db"></a>

### public static com.netumscan.printersdk.PrinterModel[] values()

返回PrinterModel的全部声明值。

```java
public static com.netumscan.printersdk.PrinterModel[] values();
```

Java编译器生成的方法，返回声明顺序的新数组。枚举ordinal不是SDK稳定错误码或设备协议值。

文档 ID：`android:member:com.netumscan.printersdk.PrinterModel:3cda81dab8112b6c`

JVM descriptor：`()[Lcom/netumscan/printersdk/PrinterModel;`

这是 Java 编译器为枚举生成的公共方法。

<a id="api-a3756c9418682d6dcbb8"></a>

### public static final com.netumscan.printersdk.PrinterModel LT_M90

已收录的LT-M90型号。

```java
public static final com.netumscan.printersdk.PrinterModel LT_M90;
```

内部集中提供CPCL、BLE通信、精确8 dots/mm、864点最大打印宽度与固定居中规则；名称推荐或显式选择都不等于硬件认证。

文档 ID：`android:member:com.netumscan.printersdk.PrinterModel:a75e8983b69690dc`

JVM descriptor：`Lcom/netumscan/printersdk/PrinterModel;`

<a id="api-764c5a4147cfdf641cc3"></a>

## RequestId

只能由来源Manager识别的不透明请求身份。

```java
public final class com.netumscan.printersdk.RequestId
```

由请求方法返回，不由App构造，不暴露内部序号或原生操作句柄。可传给同一Manager.cancel；已结束或其他Manager的身份无效果。

文档 ID：`android:type:com.netumscan.printersdk.RequestId`

<a id="api-29b3eab038d8022694c1"></a>

### public java.lang.String toString()

生成便于诊断的请求身份文字。

```java
public java.lang.String toString();
```

不透明格式只用于日志，不作为持久业务键、序号解析协议或跨Manager取消依据。

文档 ID：`android:member:com.netumscan.printersdk.RequestId:a9e4d7b84d18cdab`

JVM descriptor：`()Ljava/lang/String;`

<a id="api-e68d6e49886d528a891c"></a>

## SdkInfo

实际加载的原生SDK身份。

```java
public final class com.netumscan.printersdk.SdkInfo
```

nativeVersion和distributionVersion来自已加载库；buildId未知时为空。它们不是Demo构建版本，也不证明真实硬件验收。

文档 ID：`android:type:com.netumscan.printersdk.SdkInfo`

<a id="api-8a11e88892aadb1fa51d"></a>

### public final java.lang.String buildId

可选的原生构建来源标识。

```java
public final java.lang.String buildId;
```

仅在构建嵌入已知来源标识时返回；未知为空，不用N/A等特殊字符串。用于诊断，不是安全认证凭证。

文档 ID：`android:member:com.netumscan.printersdk.SdkInfo:1637e37d8d13a768`

JVM descriptor：`Ljava/lang/String;`

编译器注解：`@androidx.annotation.Nullable`。

<a id="api-c3ffff62c449b0a3e8f2"></a>

### public final java.lang.String distributionVersion

实际原生库嵌入的分发版本。

```java
public final java.lang.String distributionVersion;
```

用于核对所加载制品身份；不从Demo配置覆盖，也不代表已通过真实硬件或公开发布验证。

文档 ID：`android:member:com.netumscan.printersdk.SdkInfo:17e16ecc61815642`

JVM descriptor：`Ljava/lang/String;`

编译器注解：`@androidx.annotation.NonNull`。

<a id="api-17210b8724d6da37e2a2"></a>

### public final java.lang.String nativeVersion

原生核心SDK版本。

```java
public final java.lang.String nativeVersion;
```

由实际加载/链接的原生库返回，例如0.3.0；它与移动分发预览版本分别表示，不从App依赖声明猜测。

文档 ID：`android:member:com.netumscan.printersdk.SdkInfo:9d70e248890bcc50`

JVM descriptor：`Ljava/lang/String;`

编译器注解：`@androidx.annotation.NonNull`。

<a id="api-346a10d907279a636fde"></a>

## StatusResult

设备响应中有证据的硬件条件。

```java
public final class com.netumscan.printersdk.StatusResult
```

仅报告缺纸、开盖、纸将尽和切刀错误四项可空布尔值。空值表示该项未知，不是正常；没有受支持响应时查询失败，不能由本地忙闲状态合成结果。

文档 ID：`android:type:com.netumscan.printersdk.StatusResult`

<a id="api-87dac916669f60ae6e3f"></a>

### public final java.lang.Boolean coverOpen

开盖条件。

```java
public final java.lang.Boolean coverOpen;
```

有设备响应依据时true表示条件存在、false表示该条件未激活；空值表示未知。不能把未知解释为正常，也不能由本地发送完成推断此条件。

文档 ID：`android:member:com.netumscan.printersdk.StatusResult:ca9fcd86a46f33ad`

JVM descriptor：`Ljava/lang/Boolean;`

编译器注解：`@androidx.annotation.Nullable`。

<a id="api-93e3806945e4b328be1b"></a>

### public final java.lang.Boolean cutterError

切刀错误条件。

```java
public final java.lang.Boolean cutterError;
```

有设备响应依据时true表示条件存在、false表示该条件未激活；空值表示未知。不能把未知解释为正常，也不能由本地发送完成推断此条件。

文档 ID：`android:member:com.netumscan.printersdk.StatusResult:70a56a2b4dbcc29e`

JVM descriptor：`Ljava/lang/Boolean;`

编译器注解：`@androidx.annotation.Nullable`。

<a id="api-0c8de58180b97cb0075e"></a>

### public final java.lang.Boolean paperNearEnd

纸将尽条件。

```java
public final java.lang.Boolean paperNearEnd;
```

有设备响应依据时true表示条件存在、false表示该条件未激活；空值表示未知。不能把未知解释为正常，也不能由本地发送完成推断此条件。

文档 ID：`android:member:com.netumscan.printersdk.StatusResult:8b3621908309ea18`

JVM descriptor：`Ljava/lang/Boolean;`

编译器注解：`@androidx.annotation.Nullable`。

<a id="api-98651b4fda3e1ee19a17"></a>

### public final java.lang.Boolean paperOut

缺纸条件。

```java
public final java.lang.Boolean paperOut;
```

有设备响应依据时true表示条件存在、false表示该条件未激活；空值表示未知。不能把未知解释为正常，也不能由本地发送完成推断此条件。

文档 ID：`android:member:com.netumscan.printersdk.StatusResult:279a067bf191421d`

JVM descriptor：`Ljava/lang/Boolean;`

编译器注解：`@androidx.annotation.Nullable`。
