# Android Java API 参考

发布候选版本 **0.3.0-preview.17**；原生 SDK **0.3.0**；C ABI **3/r3**。

本页逐项覆盖审定的编译器公共 API 基线，共 **121** 个文档条目。
声明、JVM descriptor、常量、枚举和编译器关系均来自基线，说明按稳定文档 ID 维护。
机器可读的完整基线与说明见 [api-reference.json](api-reference.json)；运行
`python3 tools/check-api-reference.py` 可检查覆盖范围、审定 SHA-256 和本页字节一致性。

基线 SHA-256：`024cbe0df93a115d9ca83fd55e450dcbee9bb4da3505a95e66922b2d18e8f894`

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
- [PrinterInfo](#api-385a52379133ee5da261)
- [PrinterListener](#api-e9366ddeffde2a248201)
- [PrinterManager](#api-bfc786be5b911ef74d19)
- [PrinterModel](#api-59800c82d4445d8c7af4)
- [RequestId](#api-764c5a4147cfdf641cc3)
- [SdkInfo](#api-e68d6e49886d528a891c)
- [StatusResult](#api-346a10d907279a636fde)

Java 签名保留编译器记录的二进制嵌套名 `$`；源码导入和类型访问使用 `.`。
方法参数名不在 `javap` 基线中，参数顺序与类型以声明和 descriptor 为准。

<a id="api-bf113bf34fa7893fb410"></a>

## ConnectionInfo

连接结果及本连接可选的设备报告。

```java
public final class com.netumscan.printersdk.ConnectionInfo
```

包含设备目标、已采用的可选型号和本次自动查询得到的可选printerInfo。型号来自本地配置，printerInfo来自设备回读，二者不能互相替代。信息未知或可选查询失败为空；重新连接重新查询，不继承旧设备信息。

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

<a id="api-509fbea1a475aacbc372"></a>

### public final com.netumscan.printersdk.PrinterInfo printerInfo

本次连接自动读回的可选设备信息。

```java
public final com.netumscan.printersdk.PrinterInfo printerInfo;
```

已采用且支持信息方言的型号在通信建立后，通过原设备FIFO自动查询一次，额外预算最多1000ms。回读超时、坏包或不支持时为空，保持可用连接与打印；真实通信故障和显式取消仍按原连接语义处理。不写入型号Profile，不沿用上次连接结果，修改型号不重复查询。

文档 ID：`android:member:com.netumscan.printersdk.ConnectionInfo:a0fd2d9c593e5f95`

JVM descriptor：`Lcom/netumscan/printersdk/PrinterInfo;`

编译器注解：`@androidx.annotation.Nullable`。

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

连接、能力、身份和发现请求成功时使用对应类型；getStatus、打印等Void操作以及失败／取消为空。状态值经setStatusListener通知，不在getStatus结果中重复返回。

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

主动getStatus查询的支持程度。

```java
public final com.netumscan.printersdk.PrinterCapabilities$Support statusQuery;
```

描述已实现的主动设备事务，不把读取函数或被动缓存当查询。LT-M90适配后端选择GS a 00；执行仍受连接、型号和INFO软失败后的查询失效策略约束。与独立statusReporting不可互推，未知保持UNKNOWN／unknown。

文档 ID：`android:member:com.netumscan.printersdk.PrinterCapabilities:0f9aeb00f9942b9d`

JVM descriptor：`Lcom/netumscan/printersdk/PrinterCapabilities$Support;`

编译器注解：`@androidx.annotation.NonNull`。

<a id="api-ecdc134266dd674dd6d9"></a>

### public final com.netumscan.printersdk.PrinterCapabilities$Support statusReporting

设备主动状态上报的支持程度。

```java
public final com.netumscan.printersdk.PrinterCapabilities$Support statusReporting;
```

独立于主动查询statusQuery。已适配后端在无有效被动样本时为UNKNOWN／unknown，有效err:上报后为SUPPORTED／supported；查询裸回复不提升此证据。Android LT-M90 BLE／SPP与iOS BLE有后端，未适配后端为UNSUPPORTED／unsupported；断开／换型号清除旧证据。2026-10-23同一台LT-M90的Android BLE／SPP、iOS BLE已完成正常盖纸变化和Demo刷新验收；范围限于当前设备与固件，其他固件及真机强制故障仍未验证。

文档 ID：`android:member:com.netumscan.printersdk.PrinterCapabilities:dd16bbe10be803bb`

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

<a id="api-385a52379133ee5da261"></a>

## PrinterInfo

打印机信息查询的不可变设备观察值。

```java
public final class com.netumscan.printersdk.PrinterInfo
```

名称、电量、温度和能力标记来自JXIG候选回包，未知值为空。所有结果保留protocolVerified=false；不改变已采用型号，不将文档版本当作固件、UID当作序列号，也不推断实际打印完成。

文档 ID：`android:type:com.netumscan.printersdk.PrinterInfo`

<a id="api-9072c080fe6af46c32b6"></a>

### public final boolean protocolVerified

候选报文格式是否通过硬件验证。

```java
public final boolean protocolVerified;
```

当前始终为假；通过候选长度与校验检查不等于实际LT-M90报文及字段含义已验证，也不等于打印完成。

文档 ID：`android:member:com.netumscan.printersdk.PrinterInfo:9c00e28814c75a4f`

JVM descriptor：`Z`

<a id="api-ab66b58cb3eb1506fd8b"></a>

### public final java.lang.Boolean externalPower

可选的外部电源供电标记。

```java
public final java.lang.Boolean externalPower;
```

0xff映射为真，已知电池电量映射为假；其他值为空，不以缺少电量数据推断电源状态。

文档 ID：`android:member:com.netumscan.printersdk.PrinterInfo:7f0038b2836d53b3`

JVM descriptor：`Ljava/lang/Boolean;`

编译器注解：`@androidx.annotation.Nullable`。

<a id="api-270252ee42d2f3beed0c"></a>

### public final java.lang.Boolean hasError

可选的粗粒度打印机异常标记。

```java
public final java.lang.Boolean hasError;
```

0映射为假、1映射为真，其他值为空；不能区分缺纸、开盖等故障，也不能用于判定机械打印完成。

文档 ID：`android:member:com.netumscan.printersdk.PrinterInfo:de7f86089003b4fd`

JVM descriptor：`Ljava/lang/Boolean;`

编译器注解：`@androidx.annotation.Nullable`。

<a id="api-2f179641ecfa4a3d7525"></a>

### public final java.lang.Boolean supportsCompression

可选的设备压缩支持标记。

```java
public final java.lang.Boolean supportsCompression;
```

只将0和1映射为假和真；其他值为空。没有对应压缩算法协议，标记不启用压缩打印。

文档 ID：`android:member:com.netumscan.printersdk.PrinterInfo:40c1c3199038f3e8`

JVM descriptor：`Ljava/lang/Boolean;`

编译器注解：`@androidx.annotation.Nullable`。

<a id="api-b87a117288c0f33c039f"></a>

### public final java.lang.Boolean supportsNfc

可选的设备NFC能力标记。

```java
public final java.lang.Boolean supportsNfc;
```

只将0和1映射为假和真；其他值为空。NFC剩余数量存在类型与长度冲突，本轮不公开数量或纸张尺寸。

文档 ID：`android:member:com.netumscan.printersdk.PrinterInfo:410408a740c72262`

JVM descriptor：`Ljava/lang/Boolean;`

编译器注解：`@androidx.annotation.Nullable`。

<a id="api-b688aa66753a43af56af"></a>

### public final java.lang.Boolean supportsRtc

可选的设备RTC能力标记。

```java
public final java.lang.Boolean supportsRtc;
```

只将0和1映射为假和真；其他值为空。标记不代表本轮提供设置时间指令，也不是功能已完成硬件验收的证明。

文档 ID：`android:member:com.netumscan.printersdk.PrinterInfo:ecefac9007a0ac43`

JVM descriptor：`Ljava/lang/Boolean;`

编译器注解：`@androidx.annotation.Nullable`。

<a id="api-6d1376ea4ac17be87317"></a>

### public final java.lang.Integer batteryPercent

可选的电池电量百分比。

```java
public final java.lang.Integer batteryPercent;
```

仅接受文档的1至100；0或其他未知值为空。电源供电标记0xff通过externalPower单独表示，不转换为百分比。

文档 ID：`android:member:com.netumscan.printersdk.PrinterInfo:c34aa53eab60caf4`

JVM descriptor：`Ljava/lang/Integer;`

编译器注解：`@androidx.annotation.Nullable`。

<a id="api-71fb81c1413920bc4568"></a>

### public final java.lang.Integer densityLevel

可选的设备浓度等级读回。

```java
public final java.lang.Integer densityLevel;
```

保留文档的0至15，其他值为空；本轮仅查询，不提供浓度设置或持久化操作。

文档 ID：`android:member:com.netumscan.printersdk.PrinterInfo:4e080bcb7850ae3a`

JVM descriptor：`Ljava/lang/Integer;`

编译器注解：`@androidx.annotation.Nullable`。

<a id="api-9373e695a7066170e81d"></a>

### public final java.lang.Integer maxPrintWidthDots

打印机回报的一行最大点数。

```java
public final java.lang.Integer maxPrintWidthDots;
```

非零值以点为单位保留，零值视为未知；信息查询不会自动修改已采用型号的打印宽度。

文档 ID：`android:member:com.netumscan.printersdk.PrinterInfo:5380602019fc78e2`

JVM descriptor：`Ljava/lang/Integer;`

编译器注解：`@androidx.annotation.Nullable`。

<a id="api-c4886fde13f81910786e"></a>

### public final java.lang.Integer paperTypeCode

可选的设备纸张类型原始代码。

```java
public final java.lang.Integer paperTypeCode;
```

候选值0为连续纸、1为标签纸、2为黑标纸、3为纹身纸；其他值为空。它不自动设置纸型或扩展公共PaperType枚举。

文档 ID：`android:member:com.netumscan.printersdk.PrinterInfo:75aae68e85b514d3`

JVM descriptor：`Ljava/lang/Integer;`

编译器注解：`@androidx.annotation.Nullable`。

<a id="api-b0910aa7160074c86827"></a>

### public final java.lang.Integer printheadTemperatureCelsius

可选的打印头温度。

```java
public final java.lang.Integer printheadTemperatureCelsius;
```

按有符号字节解释为摄氏度，仅保留文档的-40至85范围；超出范围为未知，不代替过热故障查询。

文档 ID：`android:member:com.netumscan.printersdk.PrinterInfo:86b87a057f8ee019`

JVM descriptor：`Ljava/lang/Integer;`

编译器注解：`@androidx.annotation.Nullable`。

<a id="api-7ffad078c820d32899d5"></a>

### public final java.lang.Integer reportedDpi

打印机回报的文档DPI类型。

```java
public final java.lang.Integer reportedDpi;
```

候选映射为200、300或600；未知类型为空。它不替代LT-M90型号使用的精确8 dots/mm几何依据。

文档 ID：`android:member:com.netumscan.printersdk.PrinterInfo:92e16bec10b22c02`

JVM descriptor：`Ljava/lang/Integer;`

编译器注解：`@androidx.annotation.Nullable`。

<a id="api-b7826473dd50c547abcf"></a>

### public final java.lang.Integer speedLevel

可选的设备速度等级读回。

```java
public final java.lang.Integer speedLevel;
```

候选保留0至4，其他值为空；文档对具体机型为三档或五档尚无确定解释，不从此值建立可写设置能力。

文档 ID：`android:member:com.netumscan.printersdk.PrinterInfo:612b505ea6bcf749`

JVM descriptor：`Ljava/lang/Integer;`

编译器注解：`@androidx.annotation.Nullable`。

<a id="api-540206932f4eb2500bec"></a>

### public final java.lang.Long reportedVersion

文档中的原始版本数值。

```java
public final java.lang.Long reportedVersion;
```

按候选小端格式解释为0至65535的无符号值；文档没有确认其是固件版本，不据此推导固件身份。

文档 ID：`android:member:com.netumscan.printersdk.PrinterInfo:d76e0229bb81a1ea`

JVM descriptor：`Ljava/lang/Long;`

编译器注解：`@androidx.annotation.Nullable`。

<a id="api-7ccbd73039e837058dbe"></a>

### public final java.lang.String deviceName

打印机回报的设备名称。

```java
public final java.lang.String deviceName;
```

仅接收可打印ASCII文本；它不是已认证型号，也不会改变当前采用的打印型号或几何配置。

文档 ID：`android:member:com.netumscan.printersdk.PrinterInfo:f4705af91bfff61e`

JVM descriptor：`Ljava/lang/String;`

编译器注解：`@androidx.annotation.Nullable`。

<a id="api-b79772214186faa143e5"></a>

### public final java.lang.String manufacturer

打印机回报的厂商名称。

```java
public final java.lang.String manufacturer;
```

仅接收字段中的可打印ASCII文本；编码未知或字段为空时为未知，不使用发现名称代填。

文档 ID：`android:member:com.netumscan.printersdk.PrinterInfo:10193fcc2f1cedb0`

JVM descriptor：`Ljava/lang/String;`

编译器注解：`@androidx.annotation.Nullable`。

<a id="api-faa183000b12e2aaec5c"></a>

### public final java.lang.String uidHex

打印机回报的12字节UID。

```java
public final java.lang.String uidHex;
```

以完整24位小写十六进制保留，包括零字节；UID不被认定为产品序列号，未知时为空。

文档 ID：`android:member:com.netumscan.printersdk.PrinterInfo:48ea453d32b0eb29`

JVM descriptor：`Ljava/lang/String;`

编译器注解：`@androidx.annotation.Nullable`。

<a id="api-e9366ddeffde2a248201"></a>

## PrinterListener

统一移动端请求与事件接口。

```java
public interface com.netumscan.printersdk.PrinterListener
```

每个Manager同时控制一台设备，公开动作返回关联请求ID，统一接收接口在主线程锁外交付强类型结果。内部每请求终态一次；接收者失效不重放通知，所有设备IO仍经原执行器。

文档 ID：`android:type:com.netumscan.printersdk.PrinterListener`

编译器注解：`@androidx.annotation.MainThread`。

<a id="api-8b9e5bfee9dad90e48a6"></a>

### public default void onBondedDevicesCompleted(com.netumscan.printersdk.ManagerResult<java.util.List<com.netumscan.printersdk.DiscoveredDevice>>)

读取Android已配对设备快照。

```java
public default void onBondedDevicesCompleted(com.netumscan.printersdk.ManagerResult<java.util.List<com.netumscan.printersdk.DiscoveredDevice>>);
```

返回请求ID，经统一接收接口交付只读设备列表；不开始扫描、不自动配对或连接，未知名称与RSSI保持空值。

文档 ID：`android:member:com.netumscan.printersdk.PrinterListener:3dfb8bc6327ef01e`

JVM descriptor：`(Lcom/netumscan/printersdk/ManagerResult;)V`

参数 0（从 0 起）注解：`@androidx.annotation.NonNull`。

<a id="api-3b2ea71efc12dd727fea"></a>

### public default void onCapabilitiesCompleted(com.netumscan.printersdk.ManagerResult<com.netumscan.printersdk.PrinterCapabilities>)

读取当前能力描述。

```java
public default void onCapabilitiesCompleted(com.netumscan.printersdk.ManagerResult<com.netumscan.printersdk.PrinterCapabilities>);
```

返回PrinterCapabilities，支持度使用未知/不支持/支持三态；未知范围为空。查询和上报分别描述，能力不等于硬件认证。

文档 ID：`android:member:com.netumscan.printersdk.PrinterListener:9cc1394b422dc976`

JVM descriptor：`(Lcom/netumscan/printersdk/ManagerResult;)V`

参数 0（从 0 起）注解：`@androidx.annotation.NonNull`。

<a id="api-7214f044be684929430b"></a>

### public default void onCloseCompleted(com.netumscan.printersdk.ManagerResult<java.lang.Void>)

永久关闭并完成异步资源清理。

```java
public default void onCloseCompleted(com.netumscan.printersdk.ManagerResult<java.lang.Void>);
```

返回请求ID；必要状态重置和关闭结果交付后释放统一接收者。重复清理共享原屏障，不在UI线程同步等待，不承诺关闭后的历史补交。

文档 ID：`android:member:com.netumscan.printersdk.PrinterListener:7fbc9e7060a727de`

JVM descriptor：`(Lcom/netumscan/printersdk/ManagerResult;)V`

参数 0（从 0 起）注解：`@androidx.annotation.NonNull`。

<a id="api-50657d65597827419f28"></a>

### public default void onConnectionCompleted(com.netumscan.printersdk.ManagerResult<com.netumscan.printersdk.ConnectionInfo>)

完成显式设备连接。

```java
public default void onConnectionCompleted(com.netumscan.printersdk.ManagerResult<com.netumscan.printersdk.ConnectionInfo>);
```

返回请求ID，成功值为ConnectionInfo。已知名称只推荐收录型号，可用显式型号重载；可选设备信息在原FIFO有界查询一次。意外断连通过独立事件通知，不自动停扫、重连或重放。

文档 ID：`android:member:com.netumscan.printersdk.PrinterListener:2860ec639f88ddb2`

JVM descriptor：`(Lcom/netumscan/printersdk/ManagerResult;)V`

参数 0（从 0 起）注解：`@androidx.annotation.NonNull`。

<a id="api-8776079cb630287ac3af"></a>

### public default void onDeviceDiscovered(com.netumscan.printersdk.RequestId, com.netumscan.printersdk.DiscoveredDevice)

通知本轮首次发现的唯一设备。

```java
public default void onDeviceDiscovered(com.netumscan.printersdk.RequestId, com.netumscan.printersdk.DiscoveredDevice);
```

携带所属扫描请求ID与SDK设备值；每轮地址或UUID首次出现通知一次，最终列表保留后续合并信息。旧扫描、取消和接收者失效后的进度不派发，不暴露平台句柄。

文档 ID：`android:member:com.netumscan.printersdk.PrinterListener:ac19a6b84771ef8b`

JVM descriptor：`(Lcom/netumscan/printersdk/RequestId;Lcom/netumscan/printersdk/DiscoveredDevice;)V`

参数 0（从 0 起）注解：`@androidx.annotation.NonNull`。

参数 1（从 0 起）注解：`@androidx.annotation.NonNull`。

<a id="api-1b917ea06626364196dc"></a>

### public default void onDisconnectCompleted(com.netumscan.printersdk.ManagerResult<java.lang.Void>)

完成当前连接的异步清理。

```java
public default void onDisconnectCompleted(com.netumscan.printersdk.ManagerResult<java.lang.Void>);
```

返回请求ID；请求处理、必要状态重置和清理屏障完成后交付Void结果。接收者保留，实例可显式重新连接，不自动恢复任务。

文档 ID：`android:member:com.netumscan.printersdk.PrinterListener:800a51894513758a`

JVM descriptor：`(Lcom/netumscan/printersdk/ManagerResult;)V`

参数 0（从 0 起）注解：`@androidx.annotation.NonNull`。

<a id="api-7ae6b440497908244ad6"></a>

### public default void onDisconnected(com.netumscan.printersdk.ManagerResult<java.lang.Void>)

通知本连接的意外断开。

```java
public default void onDisconnected(com.netumscan.printersdk.ManagerResult<java.lang.Void>);
```

关联原成功连接的请求ID，每连接最多一次；主动断开和关闭走自己的完成方法。注册代次失效不把旧连接事件改投新接收者，不自动重连或重放。

文档 ID：`android:member:com.netumscan.printersdk.PrinterListener:73727feb4049b908`

JVM descriptor：`(Lcom/netumscan/printersdk/ManagerResult;)V`

参数 0（从 0 起）注解：`@androidx.annotation.NonNull`。

<a id="api-55a6a47f695c6fe9999b"></a>

### public default void onLabelPositioningCompleted(com.netumscan.printersdk.ManagerResult<java.lang.Void>)

完成显式下一标签定位。

```java
public default void onLabelPositioningCompleted(com.netumscan.printersdk.ManagerResult<java.lang.Void>);
```

返回请求ID并按设备配置经原FIFO执行；可能走纸，不由普通图片打印隐式追加。不支持返回稳定错误码，终态为Void结果。

文档 ID：`android:member:com.netumscan.printersdk.PrinterListener:3d3360e78045c1b2`

JVM descriptor：`(Lcom/netumscan/printersdk/ManagerResult;)V`

参数 0（从 0 起）注解：`@androidx.annotation.NonNull`。

<a id="api-21c2e5f2aa467150f17c"></a>

### public default void onModelConfigurationCompleted(com.netumscan.printersdk.ManagerResult<com.netumscan.printersdk.ConnectionInfo>)

更新连接采用的内置型号配置。

```java
public default void onModelConfigurationCompleted(com.netumscan.printersdk.ManagerResult<com.netumscan.printersdk.ConnectionInfo>);
```

仅连接空闲时允许；成功返回更新的ConnectionInfo并失效介质证据，失败保留原配置。不重新执行设备信息查询。结果与连接完成使用不同方法。

文档 ID：`android:member:com.netumscan.printersdk.PrinterListener:c6564ae9304c1ce9`

JVM descriptor：`(Lcom/netumscan/printersdk/ManagerResult;)V`

参数 0（从 0 起）注解：`@androidx.annotation.NonNull`。

<a id="api-5eeacdc07b31162e32a8"></a>

### public default void onPaperTypeCompleted(com.netumscan.printersdk.ManagerResult<java.lang.Void>)

完成显式纸张定位类型设置。

```java
public default void onPaperTypeCompleted(com.netumscan.printersdk.ManagerResult<java.lang.Void>);
```

这是独立设备FIFO请求，不由连接、页面尺寸或打印隐式触发；不自动校准或持久化。返回Void结果，保留错误码和完成证据。

文档 ID：`android:member:com.netumscan.printersdk.PrinterListener:b1356b078894877e`

JVM descriptor：`(Lcom/netumscan/printersdk/ManagerResult;)V`

参数 0（从 0 起）注解：`@androidx.annotation.NonNull`。

<a id="api-c74256087d36fcdee761"></a>

### public default void onPrintCompleted(com.netumscan.printersdk.ManagerResult<java.lang.Void>)

交付一次图像打印请求结果。

```java
public default void onPrintCompleted(com.netumscan.printersdk.ManagerResult<java.lang.Void>);
```

输入为Bitmap/UIImage或完整PNG/JPEG字节，页面宽高为有限正数mm；返回请求ID，后台准备及原FIFO执行。两种入口共用打印终态；保留completion与outputMayHaveOccurred，DATA_SENT不代表纸面完成。

文档 ID：`android:member:com.netumscan.printersdk.PrinterListener:5077631e283e7021`

JVM descriptor：`(Lcom/netumscan/printersdk/ManagerResult;)V`

参数 0（从 0 起）注解：`@androidx.annotation.NonNull`。

<a id="api-fa7238c354fc9ecc71c1"></a>

### public default void onRawCompleted(com.netumscan.printersdk.ManagerResult<java.lang.Void>)

完成一次原始命令发送。

```java
public default void onRawCompleted(com.netumscan.printersdk.ManagerResult<java.lang.Void>);
```

非空原始字节上限1MiB，固定发送一次，仍使用设备原FIFO；不解析命令语义、不绕过队列。保留部分输出和完成级别，不自动重试。

文档 ID：`android:member:com.netumscan.printersdk.PrinterListener:df9055ac05eba2c1`

JVM descriptor：`(Lcom/netumscan/printersdk/ManagerResult;)V`

参数 0（从 0 起）注解：`@androidx.annotation.NonNull`。

<a id="api-f5c057f9a8714c46b63a"></a>

### public default void onScanCompleted(com.netumscan.printersdk.ManagerResult<java.util.List<com.netumscan.printersdk.DiscoveredDevice>>)

完成本次有限窗口BLE扫描。

```java
public default void onScanCompleted(com.netumscan.printersdk.ManagerResult<java.util.List<com.netumscan.printersdk.DiscoveredDevice>>);
```

立即返回原有请求ID，终态经统一接收接口交付；成功值为最新合并候选列表。实时首见事件另行提供。失败或取消无部分成功列表，不自动连接设备。

文档 ID：`android:member:com.netumscan.printersdk.PrinterListener:4508167a6a571217`

JVM descriptor：`(Lcom/netumscan/printersdk/ManagerResult;)V`

参数 0（从 0 起）注解：`@androidx.annotation.NonNull`。

<a id="api-a7876a701c84a4f72cb2"></a>

### public default void onSdkInfoCompleted(com.netumscan.printersdk.ManagerResult<com.netumscan.printersdk.SdkInfo>)

读取实际SDK身份信息。

```java
public default void onSdkInfoCompleted(com.netumscan.printersdk.ManagerResult<com.netumscan.printersdk.SdkInfo>);
```

返回SdkInfo或SDKInfo，包括原生版本、分发版本和可空构建标识；不是打印机固件版本，不执行打印机IO。

文档 ID：`android:member:com.netumscan.printersdk.PrinterListener:412af23e46c3e448`

JVM descriptor：`(Lcom/netumscan/printersdk/ManagerResult;)V`

参数 0（从 0 起）注解：`@androidx.annotation.NonNull`。

<a id="api-50acbaf07c36b145b3d5"></a>

### public default void onStatusChanged(com.netumscan.printersdk.StatusResult)

通知当前设备状态快照。

```java
public default void onStatusChanged(com.netumscan.printersdk.StatusResult);
```

注册时先异步交付当前快照；无证据时四项未知。连接成功通知执行后发布初始化样本，断开或换型号重置，重复状态可合并。不是某次getStatus的完成通知。

文档 ID：`android:member:com.netumscan.printersdk.PrinterListener:22d36b55f77acbef`

JVM descriptor：`(Lcom/netumscan/printersdk/StatusResult;)V`

参数 0（从 0 起）注解：`@androidx.annotation.NonNull`。

<a id="api-db6bf7d11fc7c6ed1d8a"></a>

### public default void onStatusQueryCompleted(com.netumscan.printersdk.ManagerResult<java.lang.Void>)

完成一次主动状态查询。

```java
public default void onStatusQueryCompleted(com.netumscan.printersdk.ManagerResult<java.lang.Void>);
```

通过原FIFO发命令并验证本次应答，结果为Void；状态值只从统一状态事件交付。同值可能不再通知状态，所以不能等待下一次状态事件来结束请求。

文档 ID：`android:member:com.netumscan.printersdk.PrinterListener:1faffe67b1b2e64a`

JVM descriptor：`(Lcom/netumscan/printersdk/ManagerResult;)V`

参数 0（从 0 起）注解：`@androidx.annotation.NonNull`。

<a id="api-bfc786be5b911ef74d19"></a>

## PrinterManager

统一移动端请求与事件接口。

```java
public final class com.netumscan.printersdk.PrinterManager
```

每个Manager同时控制一台设备，公开动作返回关联请求ID，统一接收接口在主线程锁外交付强类型结果。内部每请求终态一次；接收者失效不重放通知，所有设备IO仍经原执行器。

文档 ID：`android:type:com.netumscan.printersdk.PrinterManager`

编译器注解：`@androidx.annotation.AnyThread`。

<a id="api-6761fb538c137c164625"></a>

### public com.netumscan.printersdk.PrinterManager(android.content.Context)

统一移动端请求与事件接口。

```java
public com.netumscan.printersdk.PrinterManager(android.content.Context);
```

每个Manager同时控制一台设备，公开动作返回关联请求ID，统一接收接口在主线程锁外交付强类型结果。内部每请求终态一次；接收者失效不重放通知，所有设备IO仍经原执行器。

文档 ID：`android:member:com.netumscan.printersdk.PrinterManager:989371a8a2f8fb1d`

JVM descriptor：`(Landroid/content/Context;)V`

参数 0（从 0 起）注解：`@androidx.annotation.NonNull`。

<a id="api-af561d518b091897258a"></a>

### public com.netumscan.printersdk.RequestId bondedDevices()

读取Android已配对设备快照。

```java
public com.netumscan.printersdk.RequestId bondedDevices();
```

返回请求ID，经统一接收接口交付只读设备列表；不开始扫描、不自动配对或连接，未知名称与RSSI保持空值。

文档 ID：`android:member:com.netumscan.printersdk.PrinterManager:554e7c8a27adf960`

JVM descriptor：`()Lcom/netumscan/printersdk/RequestId;`

编译器注解：`@androidx.annotation.NonNull`。

<a id="api-4c72d96932b88f6f1928"></a>

### public com.netumscan.printersdk.RequestId capabilities()

读取当前能力描述。

```java
public com.netumscan.printersdk.RequestId capabilities();
```

返回PrinterCapabilities，支持度使用未知/不支持/支持三态；未知范围为空。查询和上报分别描述，能力不等于硬件认证。

文档 ID：`android:member:com.netumscan.printersdk.PrinterManager:63604e0a05eb2e81`

JVM descriptor：`()Lcom/netumscan/printersdk/RequestId;`

编译器注解：`@androidx.annotation.NonNull`。

<a id="api-facf0ada1666cbf444ca"></a>

### public com.netumscan.printersdk.RequestId close()

永久关闭并完成异步资源清理。

```java
public com.netumscan.printersdk.RequestId close();
```

返回请求ID；必要状态重置和关闭结果交付后释放统一接收者。重复清理共享原屏障，不在UI线程同步等待，不承诺关闭后的历史补交。

文档 ID：`android:member:com.netumscan.printersdk.PrinterManager:75e50c08e0154a2e`

JVM descriptor：`()Lcom/netumscan/printersdk/RequestId;`

编译器注解：`@androidx.annotation.NonNull`。

<a id="api-6405110c810c9a5d257a"></a>

### public com.netumscan.printersdk.RequestId connectBle(com.netumscan.printersdk.DiscoveredDevice)

完成显式设备连接。

```java
public com.netumscan.printersdk.RequestId connectBle(com.netumscan.printersdk.DiscoveredDevice);
```

返回请求ID，成功值为ConnectionInfo。已知名称只推荐收录型号，可用显式型号重载；可选设备信息在原FIFO有界查询一次。意外断连通过独立事件通知，不自动停扫、重连或重放。

文档 ID：`android:member:com.netumscan.printersdk.PrinterManager:75457d51f237c649`

JVM descriptor：`(Lcom/netumscan/printersdk/DiscoveredDevice;)Lcom/netumscan/printersdk/RequestId;`

编译器注解：`@androidx.annotation.NonNull`。

参数 0（从 0 起）注解：`@androidx.annotation.NonNull`。

<a id="api-6c94dd4d7d5140877f91"></a>

### public com.netumscan.printersdk.RequestId connectBle(com.netumscan.printersdk.DiscoveredDevice, com.netumscan.printersdk.PrinterModel)

完成显式设备连接。

```java
public com.netumscan.printersdk.RequestId connectBle(com.netumscan.printersdk.DiscoveredDevice, com.netumscan.printersdk.PrinterModel);
```

返回请求ID，成功值为ConnectionInfo。已知名称只推荐收录型号，可用显式型号重载；可选设备信息在原FIFO有界查询一次。意外断连通过独立事件通知，不自动停扫、重连或重放。

文档 ID：`android:member:com.netumscan.printersdk.PrinterManager:4bd848b86e83357d`

JVM descriptor：`(Lcom/netumscan/printersdk/DiscoveredDevice;Lcom/netumscan/printersdk/PrinterModel;)Lcom/netumscan/printersdk/RequestId;`

编译器注解：`@androidx.annotation.NonNull`。

参数 0（从 0 起）注解：`@androidx.annotation.NonNull`。

参数 1（从 0 起）注解：`@androidx.annotation.NonNull`。

<a id="api-b0a63e8dace394e52636"></a>

### public com.netumscan.printersdk.RequestId connectSpp(com.netumscan.printersdk.DiscoveredDevice)

完成显式设备连接。

```java
public com.netumscan.printersdk.RequestId connectSpp(com.netumscan.printersdk.DiscoveredDevice);
```

返回请求ID，成功值为ConnectionInfo。已知名称只推荐收录型号，可用显式型号重载；可选设备信息在原FIFO有界查询一次。意外断连通过独立事件通知，不自动停扫、重连或重放。

文档 ID：`android:member:com.netumscan.printersdk.PrinterManager:9076607bcd5dca1c`

JVM descriptor：`(Lcom/netumscan/printersdk/DiscoveredDevice;)Lcom/netumscan/printersdk/RequestId;`

编译器注解：`@androidx.annotation.NonNull`。

参数 0（从 0 起）注解：`@androidx.annotation.NonNull`。

<a id="api-d288c5659e795771ce32"></a>

### public com.netumscan.printersdk.RequestId connectSpp(com.netumscan.printersdk.DiscoveredDevice, com.netumscan.printersdk.PrinterModel)

完成显式设备连接。

```java
public com.netumscan.printersdk.RequestId connectSpp(com.netumscan.printersdk.DiscoveredDevice, com.netumscan.printersdk.PrinterModel);
```

返回请求ID，成功值为ConnectionInfo。已知名称只推荐收录型号，可用显式型号重载；可选设备信息在原FIFO有界查询一次。意外断连通过独立事件通知，不自动停扫、重连或重放。

文档 ID：`android:member:com.netumscan.printersdk.PrinterManager:c8ef0d3826695d60`

JVM descriptor：`(Lcom/netumscan/printersdk/DiscoveredDevice;Lcom/netumscan/printersdk/PrinterModel;)Lcom/netumscan/printersdk/RequestId;`

编译器注解：`@androidx.annotation.NonNull`。

参数 0（从 0 起）注解：`@androidx.annotation.NonNull`。

参数 1（从 0 起）注解：`@androidx.annotation.NonNull`。

<a id="api-8d4cf22d8a72876d6214"></a>

### public com.netumscan.printersdk.RequestId disconnect()

完成当前连接的异步清理。

```java
public com.netumscan.printersdk.RequestId disconnect();
```

返回请求ID；请求处理、必要状态重置和清理屏障完成后交付Void结果。接收者保留，实例可显式重新连接，不自动恢复任务。

文档 ID：`android:member:com.netumscan.printersdk.PrinterManager:820ecb8168540d34`

JVM descriptor：`()Lcom/netumscan/printersdk/RequestId;`

编译器注解：`@androidx.annotation.NonNull`。

<a id="api-23467e881a5b3abb0cf1"></a>

### public com.netumscan.printersdk.RequestId getStatus()

完成一次主动状态查询。

```java
public com.netumscan.printersdk.RequestId getStatus();
```

通过原FIFO发命令并验证本次应答，结果为Void；状态值只从统一状态事件交付。同值可能不再通知状态，所以不能等待下一次状态事件来结束请求。

文档 ID：`android:member:com.netumscan.printersdk.PrinterManager:c2941347c8570279`

JVM descriptor：`()Lcom/netumscan/printersdk/RequestId;`

编译器注解：`@androidx.annotation.NonNull`。

<a id="api-bead3027f8a52d10b611"></a>

### public com.netumscan.printersdk.RequestId locateNextLabel()

完成显式下一标签定位。

```java
public com.netumscan.printersdk.RequestId locateNextLabel();
```

返回请求ID并按设备配置经原FIFO执行；可能走纸，不由普通图片打印隐式追加。不支持返回稳定错误码，终态为Void结果。

文档 ID：`android:member:com.netumscan.printersdk.PrinterManager:e43b443f0dcc263b`

JVM descriptor：`()Lcom/netumscan/printersdk/RequestId;`

编译器注解：`@androidx.annotation.NonNull`。

<a id="api-b314ef3203eaeb2a560c"></a>

### public com.netumscan.printersdk.RequestId printEncodedImage(byte[], double, double)

交付一次图像打印请求结果。

```java
public com.netumscan.printersdk.RequestId printEncodedImage(byte[], double, double);
```

输入为Bitmap/UIImage或完整PNG/JPEG字节，页面宽高为有限正数mm；返回请求ID，后台准备及原FIFO执行。两种入口共用打印终态；保留completion与outputMayHaveOccurred，DATA_SENT不代表纸面完成。

文档 ID：`android:member:com.netumscan.printersdk.PrinterManager:d576e80ce829acda`

JVM descriptor：`([BDD)Lcom/netumscan/printersdk/RequestId;`

编译器注解：`@androidx.annotation.NonNull`。

参数 0（从 0 起）注解：`@androidx.annotation.NonNull`。

<a id="api-9a3247ebc3f79faf4517"></a>

### public com.netumscan.printersdk.RequestId printImage(android.graphics.Bitmap, double, double)

交付一次图像打印请求结果。

```java
public com.netumscan.printersdk.RequestId printImage(android.graphics.Bitmap, double, double);
```

输入为Bitmap/UIImage或完整PNG/JPEG字节，页面宽高为有限正数mm；返回请求ID，后台准备及原FIFO执行。两种入口共用打印终态；保留completion与outputMayHaveOccurred，DATA_SENT不代表纸面完成。

文档 ID：`android:member:com.netumscan.printersdk.PrinterManager:77b14296d9416d05`

JVM descriptor：`(Landroid/graphics/Bitmap;DD)Lcom/netumscan/printersdk/RequestId;`

编译器注解：`@androidx.annotation.NonNull`。

参数 0（从 0 起）注解：`@androidx.annotation.NonNull`。

<a id="api-bc0b326f12673b6f0641"></a>

### public com.netumscan.printersdk.RequestId scanBle()

完成本次有限窗口BLE扫描。

```java
public com.netumscan.printersdk.RequestId scanBle();
```

立即返回原有请求ID，终态经统一接收接口交付；成功值为最新合并候选列表。实时首见事件另行提供。失败或取消无部分成功列表，不自动连接设备。

文档 ID：`android:member:com.netumscan.printersdk.PrinterManager:0f3c74488ab451b7`

JVM descriptor：`()Lcom/netumscan/printersdk/RequestId;`

编译器注解：`@androidx.annotation.NonNull`。

<a id="api-cb8098f700f57169716a"></a>

### public com.netumscan.printersdk.RequestId sdkInfo()

读取实际SDK身份信息。

```java
public com.netumscan.printersdk.RequestId sdkInfo();
```

返回SdkInfo或SDKInfo，包括原生版本、分发版本和可空构建标识；不是打印机固件版本，不执行打印机IO。

文档 ID：`android:member:com.netumscan.printersdk.PrinterManager:9536a151d9dd7343`

JVM descriptor：`()Lcom/netumscan/printersdk/RequestId;`

编译器注解：`@androidx.annotation.NonNull`。

<a id="api-72ce0a7e8a2b71ec323a"></a>

### public com.netumscan.printersdk.RequestId sendRaw(byte[])

完成一次原始命令发送。

```java
public com.netumscan.printersdk.RequestId sendRaw(byte[]);
```

非空原始字节上限1MiB，固定发送一次，仍使用设备原FIFO；不解析命令语义、不绕过队列。保留部分输出和完成级别，不自动重试。

文档 ID：`android:member:com.netumscan.printersdk.PrinterManager:84e362dbccd437a9`

JVM descriptor：`([B)Lcom/netumscan/printersdk/RequestId;`

编译器注解：`@androidx.annotation.NonNull`。

参数 0（从 0 起）注解：`@androidx.annotation.NonNull`。

<a id="api-081fd6248b57139e5bcc"></a>

### public com.netumscan.printersdk.RequestId setPaperType(com.netumscan.printersdk.PaperType)

完成显式纸张定位类型设置。

```java
public com.netumscan.printersdk.RequestId setPaperType(com.netumscan.printersdk.PaperType);
```

这是独立设备FIFO请求，不由连接、页面尺寸或打印隐式触发；不自动校准或持久化。返回Void结果，保留错误码和完成证据。

文档 ID：`android:member:com.netumscan.printersdk.PrinterManager:a059006f19f6c0f0`

JVM descriptor：`(Lcom/netumscan/printersdk/PaperType;)Lcom/netumscan/printersdk/RequestId;`

编译器注解：`@androidx.annotation.NonNull`。

参数 0（从 0 起）注解：`@androidx.annotation.NonNull`。

<a id="api-9e004c262deec5f1974c"></a>

### public com.netumscan.printersdk.RequestId setPrinterModel(com.netumscan.printersdk.PrinterModel)

更新连接采用的内置型号配置。

```java
public com.netumscan.printersdk.RequestId setPrinterModel(com.netumscan.printersdk.PrinterModel);
```

仅连接空闲时允许；成功返回更新的ConnectionInfo并失效介质证据，失败保留原配置。不重新执行设备信息查询。结果与连接完成使用不同方法。

文档 ID：`android:member:com.netumscan.printersdk.PrinterManager:c3fea7810240d838`

JVM descriptor：`(Lcom/netumscan/printersdk/PrinterModel;)Lcom/netumscan/printersdk/RequestId;`

编译器注解：`@androidx.annotation.NonNull`。

参数 0（从 0 起）注解：`@androidx.annotation.NonNull`。

<a id="api-c495b55a4368ea9d6525"></a>

### public void cancel(com.netumscan.printersdk.RequestId)

请求取消所属Manager的原操作。

```java
public void cancel(com.netumscan.printersdk.RequestId);
```

同步发送控制信号，不创建新请求。最终结果回到原动作的完成方法，已结束、外部或清理请求ID为空操作。不能撤回已发送字节，不将取消意图覆盖真实成功或部分输出证据。

文档 ID：`android:member:com.netumscan.printersdk.PrinterManager:80c2dece04d1d2f6`

JVM descriptor：`(Lcom/netumscan/printersdk/RequestId;)V`

参数 0（从 0 起）注解：`@androidx.annotation.Nullable`。

<a id="api-e83e957deff0d1c1cf63"></a>

### public void setListener(com.netumscan.printersdk.PrinterListener)

注册或解除统一结果接收者。

```java
public void setListener(com.netumscan.printersdk.PrinterListener);
```

Android持有当前Listener，Swift弱引用Delegate。注册代次与请求绑定；替换或解除抑制旧代次尚未进入的通知，不改投新对象。内部终态和资源回收继续执行，不隐式取消请求。

文档 ID：`android:member:com.netumscan.printersdk.PrinterManager:7f0132476bbec266`

JVM descriptor：`(Lcom/netumscan/printersdk/PrinterListener;)V`

参数 0（从 0 起）注解：`@androidx.annotation.Nullable`。

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

缺纸、开盖、纸将尽和切刀错误四项可空布尔值。监听注册或无有效样本时四项未知；LT-M90只使coverOpen、paperOut已知，另两项仍未知。查询回复与主动上报统一更新连接缓存，不能从本地发送完成推断机械就绪。

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
