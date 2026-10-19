# Android Java API 参考

发布候选版本 **0.3.0-preview.2**；原生 SDK **0.3.0**；C ABI **3/r1**。

本页逐项覆盖审定的编译器公共 API 基线，共 **309** 个文档条目。
声明、JVM descriptor、常量、枚举和编译器关系均来自基线，说明按稳定文档 ID 维护。
机器可读的完整基线与说明见 [api-reference.json](api-reference.json)；运行
`python3 tools/check-api-reference.py` 可检查覆盖范围、审定 SHA-256 和本页字节一致性。

基线 SHA-256：`07ff86b2327a0fc2ba155928f4847d8047eb1b61826bcdb6b7e223e563442fe6`

运行约束与错误处理见 [API 与生命周期](api-lifecycle.md) 和 [错误码与排障](errors-troubleshooting.md)。
本页是未发布的发布候选契约，不代表发布制品或公开历史证明。硬件验收仍暂缓，`hardware_verified=false`。

## 类型导航

- [BleOptions](#api-168ab2e3c48dfc7424a0)
- [BleOptions.CreditFlowControl](#api-7b31b50111b4bf370803)
- [BluetoothDiscovery](#api-8e189f6bdf21bdc6ff63)
- [BluetoothDiscovery.Device](#api-478f1d653a33d3fd7141)
- [BluetoothDiscovery.Listener](#api-d904c51e8af901bda535)
- [CpclProfile](#api-d96de71f64e0601eb399)
- [DeviceStateSnapshot](#api-70062abe97718165e073)
- [Diagnostic](#api-641eb88b4509fb2e59c6)
- [EscPosProfile](#api-961de0862e0cb78fdffe)
- [FeatureSupport](#api-1713e3bca4852cbf74f4)
- [M90Profile](#api-594e899b3823cf2251e4)
- [MediaMode](#api-4b74ae3f0e52d0db0f65)
- [MediaRequirement](#api-2f5be4c6d23e94746056)
- [MediaSettings](#api-e5824d9e8f16babb6dab)
- [MobilePrinter](#api-58282a32df748c4ef2b2)
- [MobilePrinter.StateListener](#api-436fb81e3c66cbf43885)
- [PrintJob](#api-4d9f9ddfaae0d09415c5)
- [PrintJob.Builder](#api-651fa2175d90c196f861)
- [PrintOperation](#api-2a1340ef023ed5fd60c2)
- [PrintOperation.CompletionListener](#api-def001084f33882939be)
- [PrintOperation.Result](#api-c0f443447380816989d4)
- [PrintOperation.Snapshot](#api-28695f8f756b652c83c0)
- [PrintOperation.StatusResult](#api-e6306611fa6971c0f618)
- [PrinterCapabilities](#api-a36171733f6c01585161)
- [PrinterException](#api-21f2c4252d1bd090ee0b)
- [PrinterImage](#api-5a783d70e33472c7d48b)
- [PrinterManager](#api-bfc786be5b911ef74d19)
- [PrinterManager.LifecycleListener](#api-16b0fff816f0a5953ef5)
- [ResourceSnapshot](#api-bc4525656d3d87c6f484)
- [ScalarConstraint](#api-b49ec93e3a988bef134a)
- [SdkInfo](#api-e68d6e49886d528a891c)
- [StatusQueryConfig](#api-31d51fc34c8d04a03b44)
- [Subscription](#api-0f650e9e7ff17c052425)

Java 签名保留编译器记录的二进制嵌套名 `$`；源码导入和类型访问使用 `.`。
方法参数名不在 `javap` 基线中，参数顺序与类型以声明和 descriptor 为准。

<a id="api-168ab2e3c48dfc7424a0"></a>

## BleOptions

不可变的 BLE 服务、写入及可选信用流控配置。

```java
public final class com.netumscan.printersdk.BleOptions
```

应用须显式提供服务与特征 UUID、MTU 请求值及写入上限；没有自动识别型号或猜测 UUID 的行为。构造只校验配置，不连接设备。`withResponse` 选择 GATT 写入方式，不能作为打印机执行确认；实际分包长度受协商 MTU、配置上限和可选信用额度共同限制。字段均为 `final`，可在多个设备创建请求间复用。

文档 ID：`android:type:com.netumscan.printersdk.BleOptions`

<a id="api-06c89b8949bb2d2f4f47"></a>

### public com.netumscan.printersdk.BleOptions(java.util.UUID, java.util.UUID, boolean, int, int)

创建不启用信用流控的 BLE 配置。

```java
public com.netumscan.printersdk.BleOptions(java.util.UUID, java.util.UUID, boolean, int, int);
```

参数 `service`、`writeCharacteristic` 为非 null 的服务和写特征 UUID；`withResponse` 为 true 时使用带响应写入，false 时使用无响应写入；`requestedMtu` 的范围为 23..517 字节，`maxWriteBytes` 的范围为 1..512 字节。等价于六参数构造器传入 `creditFlowControl=null`。配置非法抛出 `IllegalArgumentException`。同步返回新配置，不执行蓝牙 IO；没有隐式 MTU 或分包默认值。

文档 ID：`android:member:com.netumscan.printersdk.BleOptions:c88f3fcaa589a6a9`

JVM descriptor：`(Ljava/util/UUID;Ljava/util/UUID;ZII)V`

<a id="api-62616d2dd2d65d913d01"></a>

### public com.netumscan.printersdk.BleOptions(java.util.UUID, java.util.UUID, boolean, int, int, com.netumscan.printersdk.BleOptions$CreditFlowControl)

创建带可选信用流控的 BLE 配置。

```java
public com.netumscan.printersdk.BleOptions(java.util.UUID, java.util.UUID, boolean, int, int, com.netumscan.printersdk.BleOptions$CreditFlowControl);
```

参数 `service`、`writeCharacteristic` 不可为 null；`requestedMtu` 为 23..517 字节，`maxWriteBytes` 为 1..512 字节。`creditFlowControl` 可为 null；非 null 时 `withResponse` 必须为 false，且写特征必须不同于 `dataOut` 和 `dataFC`。该对象已保证两种通知特征互不相同。违反条件抛出 `IllegalArgumentException`。同步创建配置，不发起连接或订阅；GATT 属性和通知能力在 `connect()` 时验证。

文档 ID：`android:member:com.netumscan.printersdk.BleOptions:7cd4a0c376a401a5`

JVM descriptor：`(Ljava/util/UUID;Ljava/util/UUID;ZIILcom/netumscan/printersdk/BleOptions$CreditFlowControl;)V`

<a id="api-cb046178c975767ab6e0"></a>

### public final boolean withResponse

是否使用带响应的 GATT 特征写入。

```java
public final boolean withResponse;
```

由构造参数显式指定，true 对应 `WRITE_TYPE_DEFAULT`，false 对应 `WRITE_TYPE_NO_RESPONSE`；启用信用流控时必须为 false。GATT 写回调或响应不表示纸面打印完成。没有可变 setter。

文档 ID：`android:member:com.netumscan.printersdk.BleOptions:9349d0eb41799510`

JVM descriptor：`Z`

<a id="api-7dd3ed5c4225dba40584"></a>

### public final com.netumscan.printersdk.BleOptions$CreditFlowControl creditFlowControl

可选的 Data Transmission V1.2 信用流控配置。

```java
public final com.netumscan.printersdk.BleOptions$CreditFlowControl creditFlowControl;
```

值为 null 表示不启用该流控；五参数构造器固定采用 null。非 null 时连接会订阅两种通知特征，并等待初始信用信息后才就绪；不能用它代替通用状态查询或打印完成确认。

文档 ID：`android:member:com.netumscan.printersdk.BleOptions:5020202ee255a725`

JVM descriptor：`Lcom/netumscan/printersdk/BleOptions$CreditFlowControl;`

<a id="api-3e70e0378b25fbbd5251"></a>

### public final int maxWriteBytes

单次 BLE 写入的数据长度上限，单位为字节。

```java
public final int maxWriteBytes;
```

合法范围为 1..512，由构造参数显式指定。实际单包长度不超过此值与协商 MTU 减 3 的较小值；启用信用流控时还受信用额度约束。这是传输分包上限，不是光栅条带高度或完整作业大小限制。

文档 ID：`android:member:com.netumscan.printersdk.BleOptions:f96a0e16f66d9c02`

JVM descriptor：`I`

<a id="api-329424eb072add7be305"></a>

### public final int requestedMtu

连接时请求的 ATT MTU，单位为字节。

```java
public final int requestedMtu;
```

合法范围为 23..517。23 表示沿用初始 MTU；大于 23 时请求 MTU 协商。请求值不保证等于协商结果，实际写包还受 `maxWriteBytes` 限制；没有自动将 MTU 请求值当成可用载荷大小的行为。

文档 ID：`android:member:com.netumscan.printersdk.BleOptions:11c9b2aff8de43b4`

JVM descriptor：`I`

<a id="api-3a483f5c69ca680c6989"></a>

### public final java.util.UUID service

目标 BLE GATT 服务 UUID。

```java
public final java.util.UUID service;
```

构造时必须非 null。连接后在该服务中查找写特征和可选通知特征；未知服务不会自动回退到其他服务。UUID 本身不证明设备型号或协议兼容性。

文档 ID：`android:member:com.netumscan.printersdk.BleOptions:d1aa937cc2ea3c07`

JVM descriptor：`Ljava/util/UUID;`

<a id="api-4790d77a0327dac499e3"></a>

### public final java.util.UUID writeCharacteristic

目标服务内用于发送打印数据的特征 UUID。

```java
public final java.util.UUID writeCharacteristic;
```

构造时必须非 null；连接时验证其具备所选的带响应或无响应写属性。启用信用流控时不得与两种通知特征 UUID 相同。特征不支持时连接失败，通常报告 `PrinterException` code 1004。

文档 ID：`android:member:com.netumscan.printersdk.BleOptions:0d32a05b9d9ca5ae`

JVM descriptor：`Ljava/util/UUID;`

<a id="api-7b31b50111b4bf370803"></a>

## BleOptions.CreditFlowControl

Data Transmission V1.2 的两种通知特征配置。

```java
public final class com.netumscan.printersdk.BleOptions$CreditFlowControl
```

不可变的 `BleOptions` 嵌套值类型。`dataOut` 与 `dataFC` 均位于配置选定的服务；SDK 使用通知建立流控会话，其中信用通知决定可发送额度。只适用于明确支持该固件协议的设备，配置本身不升级硬件证据等级，也不公开平台 GATT 句柄。

文档 ID：`android:type:com.netumscan.printersdk.BleOptions$CreditFlowControl`

<a id="api-6657cb40e72e0c97a9e3"></a>

### public com.netumscan.printersdk.BleOptions$CreditFlowControl(java.util.UUID, java.util.UUID)

声明互不相同的数据通知与信用通知 UUID。

```java
public com.netumscan.printersdk.BleOptions$CreditFlowControl(java.util.UUID, java.util.UUID);
```

参数 `dataOut`、`dataFC` 均不可为 null，且两者不得相等，否则同步抛出 `IllegalArgumentException`。返回新配置，不进行订阅。作为 `BleOptions.creditFlowControl` 使用时，还要求写特征不同于这两个 UUID，并使用无响应写入。

文档 ID：`android:member:com.netumscan.printersdk.BleOptions$CreditFlowControl:33abf9a6aa732f1e`

JVM descriptor：`(Ljava/util/UUID;Ljava/util/UUID;)V`

<a id="api-ab18289d478138156f5d"></a>

### public final java.util.UUID dataFC

Data Transmission V1.2 的信用流控通知特征 UUID。

```java
public final java.util.UUID dataFC;
```

非 null，且不同于 `dataOut`。连接时订阅此特征，解析其信用信息并限制后续发送；等待信用也占用操作超时预算。通知不表示纸面打印完成；流控错误不会自动重放已发送数据。

文档 ID：`android:member:com.netumscan.printersdk.BleOptions$CreditFlowControl:1c434c73eb8d8d08`

JVM descriptor：`Ljava/util/UUID;`

<a id="api-ba50413f8eac6399d66f"></a>

### public final java.util.UUID dataOut

Data Transmission V1.2 的数据通知特征 UUID。

```java
public final java.util.UUID dataOut;
```

非 null，且不同于 `dataFC`。连接时须能在目标服务中找到该特征、NOTIFY 属性与 CCCD；SDK 订阅该特征以建立此固件要求的通知通道。当前公共 Java API 不提供数据通知回调，不能据此读取通用打印机状态。

文档 ID：`android:member:com.netumscan.printersdk.BleOptions$CreditFlowControl:e8f4989c9668b7e5`

JVM descriptor：`Ljava/util/UUID;`

<a id="api-8e189f6bdf21bdc6ff63"></a>

## BluetoothDiscovery

提供前台 BLE 扫描及已配对设备快照。

```java
public final class com.netumscan.printersdk.BluetoothDiscovery implements java.lang.AutoCloseable
```

实现 `AutoCloseable`，但 `close()` 仅停止当前扫描，实例仍可复用。扫描请求状态和完成回调在 Android 主线程处理；SDK 不申请权限、不管理应用前后台策略，也不自动连接、识别型号或判定候选是打印机。调用方须先获得当前 Android 版本所需的蓝牙/扫描权限，并在前台生命周期结束时停止扫描。

文档 ID：`android:type:com.netumscan.printersdk.BluetoothDiscovery`

<a id="api-6228db1e24c493671b58"></a>

### public com.netumscan.printersdk.BluetoothDiscovery(android.content.Context)

从 Android 系统服务创建发现实例。

```java
public com.netumscan.printersdk.BluetoothDiscovery(android.content.Context);
```

参数 `context` 必须为有效非 null 的 Android `Context`，用于取得 `BluetoothManager`。没有可用蓝牙适配器时抛出 `PrinterException` code 1004；不主动打开蓝牙或申请权限。同步创建，不启动扫描，不保留发现结果的全局缓存。

文档 ID：`android:member:com.netumscan.printersdk.BluetoothDiscovery:b7eaac504d92422e`

JVM descriptor：`(Landroid/content/Context;)V`

<a id="api-b98af1bb8198b4897cef"></a>

### public java.util.List<com.netumscan.printersdk.BluetoothDiscovery$Device> bondedDevices()

获取已配对蓝牙设备的排序、只读快照。

```java
public java.util.List<com.netumscan.printersdk.BluetoothDiscovery$Device> bondedDevices();
```

无参数。同步读取系统已配对集合，按 `address` 排序，返回非 null 的不可修改 `List<Device>`，可为空。每项 `name` 未知时为 null，`rssi` 为 null，`advertisedServices` 为空表。不会发起发现或连接；缺少权限/非法状态映射为 `PrinterException` code 1002，不支持为 1004，其他平台运行时错误为 2003。调用方自行决定展示或筛选，配对不证明是打印机。

文档 ID：`android:member:com.netumscan.printersdk.BluetoothDiscovery:4ea301e96fb6a008`

JVM descriptor：`()Ljava/util/List;`

<a id="api-23fc625c0cd98de3ba0c"></a>

### public void close()

按 AutoCloseable 约定停止当前扫描。

```java
public void close();
```

无参数、无返回值，等价于 `stop()`，停止动作在主线程异步执行。活动请求会收到取消回调 1005；可重复调用，调用后仍可再次 `scanBle()`。它没有永久关闭或销毁此发现实例的语义。

文档 ID：`android:member:com.netumscan.printersdk.BluetoothDiscovery:71f6e6626cb069d2`

JVM descriptor：`()V`

<a id="api-831b43f43e4edaeb38e9"></a>

### public void scanBle(java.util.UUID, int, com.netumscan.printersdk.BluetoothDiscovery$Listener)

在有限时间窗内异步收集 BLE 广播候选。

```java
public void scanBle(java.util.UUID, int, com.netumscan.printersdk.BluetoothDiscovery$Listener);
```

参数 `service` 可为 null，表示不按服务过滤；非 null 时只使用该服务 UUID 的系统扫描过滤器。`durationMs` 为 1..60000 毫秒，`completion` 不可为 null；非法参数在调用线程同步抛出 `IllegalArgumentException`。方法将请求投递到主线程后返回；完成在主线程恰好回调一次。完整时间窗成功可返回空表，设备按地址去重，名称及服务信息随广播合并。只允许一个活动扫描；重叠请求回调 1002。权限/蓝牙状态错误通常为 1002，不支持为 1004，扫描平台失败为 2003，停止为 1005；失败/取消一律返回空表。回调不自动连接设备；调用方自行处理权限和前台生命周期。

文档 ID：`android:member:com.netumscan.printersdk.BluetoothDiscovery:35126e77bf34ca0c`

JVM descriptor：`(Ljava/util/UUID;ILcom/netumscan/printersdk/BluetoothDiscovery$Listener;)V`

<a id="api-f6491a72a1fdc09ef173"></a>

### public void stop()

异步取消当前扫描，实例保持可复用。

```java
public void stop();
```

无参数、无返回值，将停止动作投递到主线程。活动请求以空列表和 errorCode 1005 完成一次；没有活动请求时不做任何事。重复调用安全，不影响已经完成的请求，也不撤销候选选择或断开其他打印连接。

文档 ID：`android:member:com.netumscan.printersdk.BluetoothDiscovery:81294639c41765b7`

JVM descriptor：`()V`

<a id="api-478f1d653a33d3fd7141"></a>

## BluetoothDiscovery.Device

发现结果中的不可变蓝牙候选快照。

```java
public final class com.netumscan.printersdk.BluetoothDiscovery$Device
```

由 `bondedDevices()` 或扫描完成列表提供，应用不能通过公共构造器自行创建。字段描述发现证据，未知名称和 RSSI 使用 null；不能把地址、名称或广播服务当作产品身份、固件版本或打印协议确认。扫描候选不会因返回该对象而被连接。

文档 ID：`android:type:com.netumscan.printersdk.BluetoothDiscovery$Device`

<a id="api-656b0c15888e307b3394"></a>

### public final java.lang.Integer rssi

最近一次 BLE 扫描结果的可选 RSSI，单位为 dBm。

```java
public final java.lang.Integer rssi;
```

BLE 结果使用 Android `ScanResult.getRssi()` 的整数值；已配对列表未测量 RSSI，使用 null。RSSI 仅供展示接收信号信息，不保证距离或连接质量，也不参与型号判定。

文档 ID：`android:member:com.netumscan.printersdk.BluetoothDiscovery$Device:4911150c511b5cd7`

JVM descriptor：`Ljava/lang/Integer;`

<a id="api-b6ecd6b2b62ddf3f08a6"></a>

### public final java.lang.String address

系统返回的非空蓝牙设备地址。

```java
public final java.lang.String address;
```

作为当前结果内去重及后续设备工厂连接的地址使用；SDK 会跳过空白地址。地址属于设备标识，日志或对外诊断应按客户隐私要求脱敏。它不证明型号，也不保证设备当前在线。

文档 ID：`android:member:com.netumscan.printersdk.BluetoothDiscovery$Device:b1aeb5a5c56be66f`

JVM descriptor：`Ljava/lang/String;`

<a id="api-8bc6fc42495c6f159424"></a>

### public final java.lang.String name

经过首尾空白规范化的可选设备名称。

```java
public final java.lang.String name;
```

未知、空串或全空白名称为 null。BLE 扫描优先使用广播名称，其次系统名称，再保留该请求中此前已知的名称；已配对快照使用系统名称。用于显示，不应用于自动确认协议或硬件支持。

文档 ID：`android:member:com.netumscan.printersdk.BluetoothDiscovery$Device:77409202b35ac1a5`

JVM descriptor：`Ljava/lang/String;`

<a id="api-3080a85593447762e470"></a>

### public final java.util.List<java.lang.String> advertisedServices

该扫描窗口已观察到的服务 UUID 字符串只读列表。

```java
public final java.util.List<java.lang.String> advertisedServices;
```

非 null 且不可修改；BLE 结果合并同一地址历次广播中的服务 UUID 并去重。广播未携带服务或结果来自已配对快照时为空表。空表表示未观察到，不表示设备不支持服务；该列表不是连接后完整的 GATT 服务发现结果。

文档 ID：`android:member:com.netumscan.printersdk.BluetoothDiscovery$Device:278808084302b914`

JVM descriptor：`Ljava/util/List;`

<a id="api-d904c51e8af901bda535"></a>

## BluetoothDiscovery.Listener

接收一次 BLE 扫描请求的最终结果。

```java
public interface com.netumscan.printersdk.BluetoothDiscovery$Listener
```

由调用方实现并传给 `scanBle()`。回调固定运行于 Android 主线程，应快速返回，耗时处理交给应用工作线程。一次合法请求完成一次；不得将候选列表当作已连接打印机列表。

文档 ID：`android:type:com.netumscan.printersdk.BluetoothDiscovery$Listener`

<a id="api-089f7b3795f9916458cf"></a>

### public abstract void completed(java.util.List<com.netumscan.printersdk.BluetoothDiscovery$Device>, int)

在主线程接收扫描时间窗、失败或取消的完成通知。

```java
public abstract void completed(java.util.List<com.netumscan.printersdk.BluetoothDiscovery$Device>, int);
```

参数 `devices` 为非 null 的只读结果表；`errorCode=0` 表示扫描时间窗正常结束，空表也属于成功。失败或取消时列表为空：1002 表示权限/蓝牙状态/重叠扫描问题，1004 表示不支持，2003 表示平台扫描 IO 失败，1005 表示调用方停止。方法无返回值；回调在主线程恰好执行一次，SDK 不在此回调内自动连接。

文档 ID：`android:member:com.netumscan.printersdk.BluetoothDiscovery$Listener:88ba7237b2eeba9a`

JVM descriptor：`(Ljava/util/List;I)V`

<a id="api-d96de71f64e0601eb399"></a>

## CpclProfile

声明 CPCL 打印的显式光栅几何及兼容便捷入口默认值。

```java
public final class com.netumscan.printersdk.CpclProfile
```

不可变配置，可在每次 CPCL 操作提交时提供；旧 CPCL 设备工厂也可绑定它。点数为打印机 dots，DPI 与纸张毫米数需由调用方结合实际设备换算，SDK 不猜测型号。`widthDots` 是设备最大可打印宽度，不是输入图片固定宽度。`xOffsetDots` 和 `form` 仅为 `submitGray8()` 便捷入口的默认值；显式 `submitLabelGray8()` 使用自己的原点和结束策略，Raw 字节不受这些值改写。配置不是硬件验收证明。

文档 ID：`android:type:com.netumscan.printersdk.CpclProfile`

<a id="api-d9243ca54e436527f6fc"></a>

### public com.netumscan.printersdk.CpclProfile(int, int, int, boolean)

创建水平原点为零的 CPCL 配置。

```java
public com.netumscan.printersdk.CpclProfile(int, int, int, boolean);
```

参数 `dpi` 为正整数；`widthDots` 为 8..7992 dots，必须为 8 的倍数；`maxLabelHeightDots` 为 1..65535 dots；`form` 指定兼容 `submitGray8()` 的下一标签开始结束策略。等价于五参数构造器传入 `xOffsetDots=0`。非法几何同步抛出 `IllegalArgumentException`；不执行 IO，不自动推断纸宽。

文档 ID：`android:member:com.netumscan.printersdk.CpclProfile:7002d48a5c80a84b`

JVM descriptor：`(IIIZ)V`

<a id="api-acf193df3ed9c8f9d898"></a>

### public com.netumscan.printersdk.CpclProfile(int, int, int, boolean, int)

创建带水平光栅原点的 CPCL 配置。

```java
public com.netumscan.printersdk.CpclProfile(int, int, int, boolean, int);
```

参数 `dpi` 为正整数；`widthDots` 为 8..7992 dots 且为 8 的倍数；`maxLabelHeightDots` 为 1..65535 dots；`xOffsetDots` 为 0..`widthDots-1`；`form` 指定兼容图像提交的结束策略。非法参数同步抛出 `IllegalArgumentException`。偏移不会扩大可打印宽度或裁剪图片：提交时偏移加按整字节补齐的图宽仍须落在设备宽度内。显式标签提交可另设原点/结束策略；Raw 命令不受此配置改写。

文档 ID：`android:member:com.netumscan.printersdk.CpclProfile:8f3cfa075f5b2a1c`

JVM descriptor：`(IIIZI)V`

<a id="api-b21b6b4649d00efca4d5"></a>

### public final boolean form

兼容图像入口默认采用的标签结束策略。

```java
public final boolean form;
```

true 使 `submitGray8()` 使用下一标签开始策略（CPCL FORM）；false 使用内容结束策略。它不选择 GAP/BLACK_MARK/CONTINUOUS、不校准传感器，也不验证纸张实际位置。`submitLabelGray8()` 的 `form` 参数独立覆盖该默认值，Raw 不受影响。

文档 ID：`android:member:com.netumscan.printersdk.CpclProfile:6df5f7cd5045db8d`

JVM descriptor：`Z`

<a id="api-dc0ed4d1751e21a8bf5b"></a>

### public final int dpi

显式声明的打印密度，单位为 dots per inch。

```java
public final int dpi;
```

由构造器要求为正整数，没有型号默认值。Java 包装将其用于标签作业的 X/Y DPI 和几何校验；它不自动改变打印机物理分辨率，必须与实际设备匹配。

文档 ID：`android:member:com.netumscan.printersdk.CpclProfile:a54ea9eab4fa22b3`

JVM descriptor：`I`

<a id="api-62ac8ccfc717fd318b32"></a>

### public final int maxLabelHeightDots

一张标签的最大页高，单位为 dots。

```java
public final int maxLabelHeightDots;
```

范围为 1..65535，由调用方按设备约束声明。显式标签提交的 `originY + height + trailingBlankDots` 须不超过它；此值不乘 copies，也不是整单累计长度或手机内存承诺。

文档 ID：`android:member:com.netumscan.printersdk.CpclProfile:442cf4821daf3658`

JVM descriptor：`I`

<a id="api-9fc785d8c552c1b98cd0"></a>

### public final int widthDots

设备可打印的最大光栅宽度，单位为 dots。

```java
public final int widthDots;
```

范围为 8..7992，必须为 8 的倍数。输入图片可窄于该值；SDK 不自动缩放，实际图宽按 8 点补齐后再加水平原点不得超过此上限。纸张毫米宽度或蓝牙包长均不能直接代入。

文档 ID：`android:member:com.netumscan.printersdk.CpclProfile:d1f158b9ba2a99a4`

JVM descriptor：`I`

<a id="api-5180a79db48655575a6e"></a>

### public final int xOffsetDots

兼容图像入口默认的非负水平原点，单位为 dots。

```java
public final int xOffsetDots;
```

四参数构造器默认值为 0；显式值必须小于 `widthDots`。仅 `submitGray8()` 继承此值，`submitLabelGray8()` 使用自己的 `originX`。原点不改变输入图尺寸或 Raw 字节，超界不自动裁剪。

文档 ID：`android:member:com.netumscan.printersdk.CpclProfile:af181a7757d57091`

JVM descriptor：`I`

<a id="api-70062abe97718165e073"></a>

## DeviceStateSnapshot

不可变的设备状态快照。

```java
public final class com.netumscan.printersdk.DeviceStateSnapshot
```

读取不调用传输回调、不发送打印机 IO、不入队也不等待打印完成。快照只用于展示和诊断，不作为提交前置条件；不得与设备销毁并发访问。 返回连接、工作、队列及硬件未知字段，所有属性只读。

文档 ID：`android:type:com.netumscan.printersdk.DeviceStateSnapshot`

<a id="api-093259d04a0618b9e6eb"></a>

### public final boolean hasActiveOperation

是否存在活动业务操作。

```java
public final boolean hasActiveOperation;
```

false 时活动 ID、kind、state 都为 0；true 时结合活动字段展示，不能预留队列容量。

文档 ID：`android:member:com.netumscan.printersdk.DeviceStateSnapshot:0a6f21925198141e`

JVM descriptor：`Z`

<a id="api-2d8fc67889bbd6b85e9b"></a>

### public final int activeOperationKind

活动操作种类。

```java
public final int activeOperationKind;
```

仅 hasActiveOperation=true 时有效，沿用原生 PSDK_OP_* 值；0 表示没有公共种类的内部操作。

文档 ID：`android:member:com.netumscan.printersdk.DeviceStateSnapshot:c2a6d6d58a617150`

JVM descriptor：`I`

<a id="api-871527adf3f98383c4ce"></a>

### public final int activeOperationState

活动操作当前状态。

```java
public final int activeOperationState;
```

仅 hasActiveOperation=true 时有效，沿用 operation 状态；它与整体 workState 一起由本地一致快照产生。

文档 ID：`android:member:com.netumscan.printersdk.DeviceStateSnapshot:774f3fc4e81c2819`

JVM descriptor：`I`

<a id="api-a270f3d3de305785829c"></a>

### public final int connectionState

SDK 本地连接状态。

```java
public final int connectionState;
```

使用既有四态值；CONNECTED 不证明实时链路存活、设备就绪或纸面完成。

文档 ID：`android:member:com.netumscan.printersdk.DeviceStateSnapshot:359f83ce24857a78`

JVM descriptor：`I`

<a id="api-bdddcee20e62729480ab"></a>

### public final int hardwareActiveConditions

已知且成立的硬件条件位集合。

```java
public final int hardwareActiveConditions;
```

当前固定为 0；仅在对应 known 条件位有效时才可解释，当前没有有效硬件采样。

文档 ID：`android:member:com.netumscan.printersdk.DeviceStateSnapshot:f606839c4f2a21a8`

JVM descriptor：`I`

<a id="api-5586a222637ddd0a247d"></a>

### public final int hardwareKnownConditions

有有效硬件证据的条件位集合。

```java
public final int hardwareKnownConditions;
```

当前固定为 0，表示没有已知条件；未知不能推断为没有缺纸、开盖或故障。

文档 ID：`android:member:com.netumscan.printersdk.DeviceStateSnapshot:6e30a1cb3f8533c2`

JVM descriptor：`I`

<a id="api-58bdadca8001d58e4f80"></a>

### public final int hardwareState

打印机实际硬件状态。

```java
public final int hardwareState;
```

当前标准后端固定返回 UNKNOWN=0；不得把队列为空或已连接显示为硬件就绪。

文档 ID：`android:member:com.netumscan.printersdk.DeviceStateSnapshot:75e59c32fe1cef44`

JVM descriptor：`I`

<a id="api-7e00a294311def12bd5d"></a>

### public final int workState

SDK 工作阶段。

```java
public final int workState;
```

IDLE=0、QUEUED=1、PREPARING=2、SENDING=3；只反映 SDK 队列与执行器，不表示硬件正在打印。

文档 ID：`android:member:com.netumscan.printersdk.DeviceStateSnapshot:dd2a51a54c692e5b`

JVM descriptor：`I`

<a id="api-b98d19540f53690debe7"></a>

### public final long activeOperationId

活动操作的原生 ID。

```java
public final long activeOperationId;
```

仅 hasActiveOperation=true 时有效，与 PrintOperation.Snapshot.id 对应；不是兼容 task ID。

文档 ID：`android:member:com.netumscan.printersdk.DeviceStateSnapshot:ee21659459fd1672`

JVM descriptor：`J`

<a id="api-2ca658f4c29dc7e3cfa9"></a>

### public final long connectionGeneration

成功新连接的代数。

```java
public final long connectionGeneration;
```

初始为 0，每次成功建立新的连接递增；失败连接、断开以及已连接时重复 connect 不递增。

文档 ID：`android:member:com.netumscan.printersdk.DeviceStateSnapshot:70bf86ec3b40d69c`

JVM descriptor：`J`

<a id="api-f2f23820020186c61a41"></a>

### public final long hardwareSampledMonotonicMs

有效硬件样本的单调时钟毫秒。

```java
public final long hardwareSampledMonotonicMs;
```

当前固定为 0，表示没有有效采样；该值不属于墙上时钟，不可作为日历时间展示。

文档 ID：`android:member:com.netumscan.printersdk.DeviceStateSnapshot:bb763bfad5286cec`

JVM descriptor：`J`

<a id="api-4558fe069a9cdc5d275e"></a>

### public final long queuedCount

等待执行的业务操作数。

```java
public final long queuedCount;
```

不包含活动操作或内部连接命令；取消请求尚未完成时可能仍计入，排队取消成功返回后已经移出；读取后数值可立即改变。

文档 ID：`android:member:com.netumscan.printersdk.DeviceStateSnapshot:2896ab845bcec0b5`

JVM descriptor：`J`

<a id="api-c0ff46f5c23a3ea4a003"></a>

### public static final int HARDWARE_UNKNOWN = 0

硬件状态未知。

```java
public static final int HARDWARE_UNKNOWN = 0;
```

稳定常量值为 0；当前标准后端返回此值，硬件条件位及采样时间为 0。

文档 ID：`android:member:com.netumscan.printersdk.DeviceStateSnapshot:273ee7b5ad5fa249`

JVM descriptor：`I`

<a id="api-d624ac735be6b457b8a6"></a>

### public static final int WORK_IDLE = 0

SDK IDLE 工作状态常量。

```java
public static final int WORK_IDLE = 0;
```

稳定值为 0；没有活动或等待的 SDK 业务操作，不证明打印机已停止出纸。

文档 ID：`android:member:com.netumscan.printersdk.DeviceStateSnapshot:85fc115059a518f9`

JVM descriptor：`I`

<a id="api-f599830014d3ea4ac351"></a>

### public static final int WORK_PREPARING = 2

SDK PREPARING 工作状态常量。

```java
public static final int WORK_PREPARING = 2;
```

稳定值为 2；正在准备图像或执行发送前处理，不表示实际出纸。

文档 ID：`android:member:com.netumscan.printersdk.DeviceStateSnapshot:5c6be8b50598d977`

JVM descriptor：`I`

<a id="api-026c3f7e7d32fcc080a3"></a>

### public static final int WORK_QUEUED = 1

SDK QUEUED 工作状态常量。

```java
public static final int WORK_QUEUED = 1;
```

稳定值为 1；有等待操作，不能作为提交前置条件。

文档 ID：`android:member:com.netumscan.printersdk.DeviceStateSnapshot:6aa74d77b823484b`

JVM descriptor：`I`

<a id="api-afb8fe641338296f00c6"></a>

### public static final int WORK_SENDING = 3

SDK SENDING 工作状态常量。

```java
public static final int WORK_SENDING = 3;
```

稳定值为 3；正在发送或等待流控，实际硬件状态仍未知。

文档 ID：`android:member:com.netumscan.printersdk.DeviceStateSnapshot:3694c1ec6f33c5c0`

JVM descriptor：`I`

<a id="api-641eb88b4509fb2e59c6"></a>

## Diagnostic

不可变的有界错误诊断。

```java
public final class com.netumscan.printersdk.Diagnostic
```

设备保留一条最新记录，operation 的记录独立存活。sequence=0 表示无设备记录。只包含稳定错误码、关联代次和经筛选的诊断，不携带打印载荷、地址或平台自由文本；单调时间只可在当前进程内比较。

文档 ID：`android:type:com.netumscan.printersdk.Diagnostic`

<a id="api-633467c7950e7571d4c3"></a>

### public final boolean hasNativeCode

是否附带平台原生错误码。

```java
public final boolean hasNativeCode;
```

false 时 nativeCode 没有含义，不应展示为真实平台错误。

文档 ID：`android:member:com.netumscan.printersdk.Diagnostic:8502c33fbd8b4316`

JVM descriptor：`Z`

<a id="api-4db844f9b2f8cfff67c5"></a>

### public final int errorCode

稳定 SDK 错误码。

```java
public final int errorCode;
```

成功或主动断开记录可为 0；业务判断使用此字段，平台错误码仅供诊断。

文档 ID：`android:member:com.netumscan.printersdk.Diagnostic:17e7f653fdcbb7b7`

JVM descriptor：`I`

<a id="api-b5e6e9e40bd81cd713e0"></a>

### public final int source

诊断来源分类。

```java
public final int source;
```

值为 NONE、CONNECT、OPERATION、TRANSPORT 或 USER_DISCONNECT，分类不表示纸面打印结果。

文档 ID：`android:member:com.netumscan.printersdk.Diagnostic:17fdbef0d353da1f`

JVM descriptor：`I`

<a id="api-2bfdda08c1a42c70138d"></a>

### public final java.lang.String domain

经过筛选的错误域。

```java
public final java.lang.String domain;
```

最多 31 个 UTF-8 字节；无域时为空字符串，不包含地址或任意平台错误文本。

文档 ID：`android:member:com.netumscan.printersdk.Diagnostic:238d4ff22f7ac70e`

JVM descriptor：`Ljava/lang/String;`

<a id="api-a0ea464b286c6dd88f30"></a>

### public final java.lang.String message

经过筛选的有界错误说明。

```java
public final java.lang.String message;
```

最多 191 个 UTF-8 字节；无说明时为空字符串。稳定业务判断继续使用 errorCode。

文档 ID：`android:member:com.netumscan.printersdk.Diagnostic:62f72828fbf07916`

JVM descriptor：`Ljava/lang/String;`

<a id="api-7d8f888f6bf616e2ee94"></a>

### public final long connectionGeneration

产生记录时的连接成功代次。

```java
public final long connectionGeneration;
```

较早代次记录在重连后仍保留；与当前设备代次比较，避免把旧错误显示为当前故障。

文档 ID：`android:member:com.netumscan.printersdk.Diagnostic:70bf86ec3b40d69c`

JVM descriptor：`J`

<a id="api-fc71bd72d4324fdc2aed"></a>

### public final long nativeCode

可选平台诊断码。

```java
public final long nativeCode;
```

仅 hasNativeCode=true 时有意义，不能作为稳定业务分支依据。

文档 ID：`android:member:com.netumscan.printersdk.Diagnostic:04297378d9274201`

JVM descriptor：`J`

<a id="api-077a20f867e0b8b7aa3e"></a>

### public final long operationId

相关 operation 的标识。

```java
public final long operationId;
```

0 表示记录没有关联业务操作；operation 结果可在设备关闭后独立读取。

文档 ID：`android:member:com.netumscan.printersdk.Diagnostic:b50e2bdd599a6434`

JVM descriptor：`J`

<a id="api-f46bab73e14b1d4f1b99"></a>

### public final long sampledMonotonicMs

诊断记录的进程单调时间，单位毫秒。

```java
public final long sampledMonotonicMs;
```

不是墙上时间；只能在同一进程中计算相对时长。

文档 ID：`android:member:com.netumscan.printersdk.Diagnostic:0b0a9a42fa4de096`

JVM descriptor：`J`

<a id="api-b7dabd288e1bbe3e9a1c"></a>

### public final long sequence

设备诊断的递增记录序号。

```java
public final long sequence;
```

0 表示尚无设备诊断；这是最新一条记录的身份，不是历史记录数或全局日志编号。

文档 ID：`android:member:com.netumscan.printersdk.Diagnostic:928e65fe2f7f9efc`

JVM descriptor：`J`

<a id="api-6d3e1bf61cf2b2e58682"></a>

### public final long transportEpoch

发生记录时的底层 open 尝试代次。

```java
public final long transportEpoch;
```

用于过滤旧连接回调，不等同于成功连接 generation；旧 epoch 的断连信号不会影响新连接。

文档 ID：`android:member:com.netumscan.printersdk.Diagnostic:63aec364483990c2`

JVM descriptor：`J`

<a id="api-5b8ad8ebb960973d2d12"></a>

### public static final int CONNECT = 1

连接建立失败来源。

```java
public static final int CONNECT = 1;
```

固定公共分类值；不替代 errorCode、连接代次及有效性判断。

文档 ID：`android:member:com.netumscan.printersdk.Diagnostic:1c74cba7d4561488`

JVM descriptor：`I`

<a id="api-c1f9d8d60933b3c96a3c"></a>

### public static final int NONE = 0

无诊断来源。

```java
public static final int NONE = 0;
```

固定公共分类值；不替代 errorCode、连接代次及有效性判断。

文档 ID：`android:member:com.netumscan.printersdk.Diagnostic:5f26a92b9709b12b`

JVM descriptor：`I`

<a id="api-9f5442a323c8e44a2cc6"></a>

### public static final int OPERATION = 2

业务 operation 失败来源。

```java
public static final int OPERATION = 2;
```

固定公共分类值；不替代 errorCode、连接代次及有效性判断。

文档 ID：`android:member:com.netumscan.printersdk.Diagnostic:17f6a4f8f7207290`

JVM descriptor：`I`

<a id="api-c2deebeeaceab6fc6090"></a>

### public static final int TRANSPORT = 3

平台主动断连来源。

```java
public static final int TRANSPORT = 3;
```

固定公共分类值；不替代 errorCode、连接代次及有效性判断。

文档 ID：`android:member:com.netumscan.printersdk.Diagnostic:81d9a25d1f7b40b7`

JVM descriptor：`I`

<a id="api-8bb9d6128f735eb2d7a7"></a>

### public static final int USER_DISCONNECT = 4

调用方主动断开来源。

```java
public static final int USER_DISCONNECT = 4;
```

固定公共分类值；不替代 errorCode、连接代次及有效性判断。

文档 ID：`android:member:com.netumscan.printersdk.Diagnostic:6eda2493274f0510`

JVM descriptor：`I`

<a id="api-961de0862e0cb78fdffe"></a>

## EscPosProfile

声明 ESC/POS 小票光栅的显式 DPI 和可打印宽度。

```java
public final class com.netumscan.printersdk.EscPosProfile
```

不可变配置，用于 ESC/POS 设备工厂。必须与实际设备的密度和可打印点宽匹配，不从纸宽或名称推断型号。该类型不包含切刀或状态支持声明；当前标准后端不实现状态查询及切纸。整张小票须由调用方渲染为 Gray8 图片，再用 `submitReceiptGray8()` 提交。

文档 ID：`android:type:com.netumscan.printersdk.EscPosProfile`

<a id="api-a389a27bb00caa90a626"></a>

### public com.netumscan.printersdk.EscPosProfile(int, int)

创建 ESC/POS 光栅几何配置。

```java
public com.netumscan.printersdk.EscPosProfile(int, int);
```

参数 `dpi` 为 1..255 dots per inch，`widthDots` 为 8..7992 dots 且为 8 的倍数。超出范围同步抛出 `IllegalArgumentException`。两值都必须显式提供，没有型号默认值；构造不连接或设置硬件。

文档 ID：`android:member:com.netumscan.printersdk.EscPosProfile:13dfdc2812e6b642`

JVM descriptor：`(II)V`

<a id="api-b663d0e9cf4b797b927b"></a>

### public final int dpi

ESC/POS 作业声明的打印密度，单位为 dots per inch。

```java
public final int dpi;
```

范围为 1..255，由构造参数指定；用于与设备配置一致的 X/Y DPI。它不测量打印头、不自动改变硬件分辨率，配置值须经调用方核实。

文档 ID：`android:member:com.netumscan.printersdk.EscPosProfile:a54ea9eab4fa22b3`

JVM descriptor：`I`

<a id="api-73fb6b16df56ec320fd7"></a>

### public final int widthDots

ESC/POS 设备可打印的最大光栅宽度，单位为 dots。

```java
public final int widthDots;
```

范围为 8..7992，必须为 8 的倍数。整张小票图片可窄于该值；按 8 点补齐后的图宽必须在此范围内，不自动缩放或裁剪，也不等同于纸张毫米宽度。

文档 ID：`android:member:com.netumscan.printersdk.EscPosProfile:d1f158b9ba2a99a4`

JVM descriptor：`I`

<a id="api-1713e3bca4852cbf74f4"></a>

## FeatureSupport

分别记录实现可用性、设备支持与证据的不可变快照。

```java
public final class com.netumscan.printersdk.FeatureSupport
```

通过 `MobilePrinter.feature()` 获取，不提供公共构造器。`implementation` 描述 SDK 路径是否实现，`deviceSupport` 描述设备支持是否已知，`evidence` 是证据位掩码；三个维度不能相互替代。当前显式 CPCL/ESC-POS 配置的已知功能可能仅有 PROFILE_DECLARED 且设备支持为 UNKNOWN，因此 `isImplemented()` 可为 true、`isSupported()` 仍为 false。未知功能 ID 返回未实现、未知设备支持和 evidence=0。

文档 ID：`android:type:com.netumscan.printersdk.FeatureSupport`

<a id="api-5adf34cff79f8a28b5fc"></a>

### public boolean isImplemented()

判断 SDK 是否存在该功能的实现路径。

```java
public boolean isImplemented();
```

无参数，返回 `implementation != NOT_IMPLEMENTED`。NATIVE 与 HOST_RENDERED 都返回 true；不读取设备、不执行 IO。该结果不检查 `deviceSupport` 或证据，不是当前硬件可用或打印已验收的保证。

文档 ID：`android:member:com.netumscan.printersdk.FeatureSupport:9f723c20b86e9048`

JVM descriptor：`()Z`

<a id="api-9648a867f14b13663a1a"></a>

### public boolean isSupported()

判断实现可用且设备支持已明确为支持。

```java
public boolean isSupported();
```

无参数，仅在 `isImplemented()` 为 true 且 `deviceSupport == SUPPORTED` 时返回 true。UNKNOWN 与 UNSUPPORTED 都返回 false，不能用 false 反推设备必定不支持。此判断不要求 HARDWARE_VERIFIED 位，仍应单独检查证据。

文档 ID：`android:member:com.netumscan.printersdk.FeatureSupport:751796123f4e2c80`

JVM descriptor：`()Z`

<a id="api-b69d65a56f8413e395f5"></a>

### public final int deviceSupport

该功能在设备上的支持知识状态。

```java
public final int deviceSupport;
```

当前定义为 UNKNOWN=0、UNSUPPORTED=1、SUPPORTED=2。UNKNOWN 表示没有足够证据，不等于 UNSUPPORTED；显式打印参数和协议编码存在也不能推导为 SUPPORTED。字段只读，配合 `implementation` 与 `evidence` 使用。

文档 ID：`android:member:com.netumscan.printersdk.FeatureSupport:6dfcca999e954a11`

JVM descriptor：`I`

<a id="api-0034bebc335217764927"></a>

### public final int evidence

支持声明的证据位掩码。

```java
public final int evidence;
```

PROFILE_DECLARED=1、DEVICE_REPORTED=2、HARDWARE_VERIFIED=4 可按位组合，0 表示没有对应证据。应使用按位与测试目标位；配置声明或设备自报均不能替代硬件验证。读取快照不会触发新的查询或验收。

文档 ID：`android:member:com.netumscan.printersdk.FeatureSupport:958eb37813714ec1`

JVM descriptor：`I`

<a id="api-99fa3cc41f8df96a5868"></a>

### public final int implementation

该功能的 SDK 实现类别整数。

```java
public final int implementation;
```

当前定义为 NOT_IMPLEMENTED=0、NATIVE=1、HOST_RENDERED=2。它描述实现路径而非设备能力；使用 `isImplemented()` 可保留对将来非零实现值的保守兼容。字段只读，读取不执行 IO。

文档 ID：`android:member:com.netumscan.printersdk.FeatureSupport:9ad48e1c07766934`

JVM descriptor：`I`

<a id="api-5ede70a7e57b8e7202c9"></a>

### public final int knownFields

功能knownFields位集合。

```java
public final int knownFields;
```

VALUES_KNOWN=1、REQUIRED_FIELDS_KNOWN=2、SUPPORTED_FIELDS_KNOWN=4，先检查此位再解释对应集合，未知不等于空。

文档 ID：`android:member:com.netumscan.printersdk.FeatureSupport:700caffac055e8b2`

JVM descriptor：`I`

<a id="api-c5a9c98790c8414e72cf"></a>

### public final int requiredFields

功能requiredFields位集合。

```java
public final int requiredFields;
```

介质设置必需字段mask，仅REQUIRED_FIELDS_KNOWN有效时解释；当前CPCL仅tracking。

文档 ID：`android:member:com.netumscan.printersdk.FeatureSupport:7da7b5176ab9cfb2`

JVM descriptor：`I`

<a id="api-09357b4d94c68c8db5dc"></a>

### public final int supportedFields

功能supportedFields位集合。

```java
public final int supportedFields;
```

介质允许字段mask，仅SUPPORTED_FIELDS_KNOWN有效时解释；未知不能推断全部可用。

文档 ID：`android:member:com.netumscan.printersdk.FeatureSupport:6a1a916a859f4e9f`

JVM descriptor：`I`

<a id="api-0eeee30ce1ebd05e100f"></a>

### public final int supportedValues

功能supportedValues位集合。

```java
public final int supportedValues;
```

功能允许的枚举值mask，仅VALUES_KNOWN有效时解释；CUT按FULL/PARTIAL，IMAGE按算法，MEDIA按tracking值。

文档 ID：`android:member:com.netumscan.printersdk.FeatureSupport:8e1bfa54fc237043`

JVM descriptor：`I`

<a id="api-2711890ae200c6e98573"></a>

### public static final int DEVICE_REPORTED = 2

DEVICE_REPORTED常量，值2。

```java
public static final int DEVICE_REPORTED = 2;
```

表示DEVICE_REPORTED对应的已知/存在字段mask；先检查known或present位再解释相关字段。 不执行IO，不表示硬件已验证支持。

文档 ID：`android:member:com.netumscan.printersdk.FeatureSupport:7384e00424860bf1`

JVM descriptor：`I`

<a id="api-29c5a143635d71a155f4"></a>

### public static final int HARDWARE_VERIFIED = 4

HARDWARE_VERIFIED常量，值4。

```java
public static final int HARDWARE_VERIFIED = 4;
```

表示HARDWARE_VERIFIED对应的已知/存在字段mask；先检查known或present位再解释相关字段。 不执行IO，不表示硬件已验证支持。

文档 ID：`android:member:com.netumscan.printersdk.FeatureSupport:a41dc2f2bd9542ff`

JVM descriptor：`I`

<a id="api-333b4ee110fff9669c75"></a>

### public static final int HOST_RENDERED = 2

HOST_RENDERED常量，值2。

```java
public static final int HOST_RENDERED = 2;
```

表示HOST_RENDERED对应的已知/存在字段mask；先检查known或present位再解释相关字段。 不执行IO，不表示硬件已验证支持。

文档 ID：`android:member:com.netumscan.printersdk.FeatureSupport:6e6ae85ee79f4469`

JVM descriptor：`I`

<a id="api-36b7b3a7bdf8d0253599"></a>

### public static final int NATIVE = 1

NATIVE常量，值1。

```java
public static final int NATIVE = 1;
```

表示NATIVE对应的已知/存在字段mask；先检查known或present位再解释相关字段。 不执行IO，不表示硬件已验证支持。

文档 ID：`android:member:com.netumscan.printersdk.FeatureSupport:9c09323ac8e6906f`

JVM descriptor：`I`

<a id="api-8f674851f75479a82ebf"></a>

### public static final int NOT_IMPLEMENTED = 0

NOT_IMPLEMENTED常量，值0。

```java
public static final int NOT_IMPLEMENTED = 0;
```

表示NOT_IMPLEMENTED对应的已知/存在字段mask；先检查known或present位再解释相关字段。 不执行IO，不表示硬件已验证支持。

文档 ID：`android:member:com.netumscan.printersdk.FeatureSupport:258ec4eedffc58f1`

JVM descriptor：`I`

<a id="api-7cb192a56c0db341863a"></a>

### public static final int PROFILE_DECLARED = 1

PROFILE_DECLARED常量，值1。

```java
public static final int PROFILE_DECLARED = 1;
```

表示PROFILE_DECLARED对应的已知/存在字段mask；先检查known或present位再解释相关字段。 不执行IO，不表示硬件已验证支持。

文档 ID：`android:member:com.netumscan.printersdk.FeatureSupport:296a3a40ae4f87ce`

JVM descriptor：`I`

<a id="api-1a137e5ddc2239a97711"></a>

### public static final int REQUIRED_FIELDS_KNOWN = 2

REQUIRED_FIELDS_KNOWN常量，值2。

```java
public static final int REQUIRED_FIELDS_KNOWN = 2;
```

表示REQUIRED_FIELDS_KNOWN对应的已知/存在字段mask；先检查known或present位再解释相关字段。 不执行IO，不表示硬件已验证支持。

文档 ID：`android:member:com.netumscan.printersdk.FeatureSupport:67c7e976e8b80178`

JVM descriptor：`I`

<a id="api-d39842a85f93d06ffa3a"></a>

### public static final int SUPPORTED = 2

SUPPORTED常量，值2。

```java
public static final int SUPPORTED = 2;
```

表示SUPPORTED对应的已知/存在字段mask；先检查known或present位再解释相关字段。 不执行IO，不表示硬件已验证支持。

文档 ID：`android:member:com.netumscan.printersdk.FeatureSupport:2660f6d171c11793`

JVM descriptor：`I`

<a id="api-96b5ed030acd084cf86a"></a>

### public static final int SUPPORTED_FIELDS_KNOWN = 4

SUPPORTED_FIELDS_KNOWN常量，值4。

```java
public static final int SUPPORTED_FIELDS_KNOWN = 4;
```

表示SUPPORTED_FIELDS_KNOWN对应的已知/存在字段mask；先检查known或present位再解释相关字段。 不执行IO，不表示硬件已验证支持。

文档 ID：`android:member:com.netumscan.printersdk.FeatureSupport:8b212cb35baa50a9`

JVM descriptor：`I`

<a id="api-b402a3b6e7c5f8404721"></a>

### public static final int UNKNOWN = 0

UNKNOWN常量，值0。

```java
public static final int UNKNOWN = 0;
```

表示UNKNOWN对应的已知/存在字段mask；先检查known或present位再解释相关字段。 不执行IO，不表示硬件已验证支持。

文档 ID：`android:member:com.netumscan.printersdk.FeatureSupport:12fee7f020c6e236`

JVM descriptor：`I`

<a id="api-d1ae2781a5eb8768e313"></a>

### public static final int UNSUPPORTED = 1

UNSUPPORTED常量，值1。

```java
public static final int UNSUPPORTED = 1;
```

表示UNSUPPORTED对应的已知/存在字段mask；先检查known或present位再解释相关字段。 不执行IO，不表示硬件已验证支持。

文档 ID：`android:member:com.netumscan.printersdk.FeatureSupport:cc394616ff0eb36c`

JVM descriptor：`I`

<a id="api-5f63e368a44431300b29"></a>

### public static final int VALUES_KNOWN = 1

VALUES_KNOWN常量，值1。

```java
public static final int VALUES_KNOWN = 1;
```

表示VALUES_KNOWN对应的已知/存在字段mask；先检查known或present位再解释相关字段。 不执行IO，不表示硬件已验证支持。

文档 ID：`android:member:com.netumscan.printersdk.FeatureSupport:2a8cd771661a8b86`

JVM descriptor：`I`

<a id="api-594e899b3823cf2251e4"></a>

## M90Profile

提供 M90 固件 Data Transmission V1.2 的 BLE 连接预设。

```java
public final class com.netumscan.printersdk.M90Profile
```

工具类型，无公共构造器。预设只组合固定的服务、数据写入和信用通知 UUID，调用方仍须提供 MTU 与包长上限并选择打印协议/几何配置。选择此预设不自动发现或认证型号，也不表示无线或纸面行为已经完成硬件验收。

文档 ID：`android:type:com.netumscan.printersdk.M90Profile`

<a id="api-4a211dd1fcaf5ed95e6f"></a>

### public static com.netumscan.printersdk.BleOptions bleOptions(int, int)

创建 M90 V1.2 无响应写入与信用流控配置。

```java
public static com.netumscan.printersdk.BleOptions bleOptions(int, int);
```

参数 `requestedMtu` 为 23..517 字节，`maxWriteBytes` 为 1..512 字节；非法值由 `BleOptions` 抛出 `IllegalArgumentException`。返回非 null `BleOptions`：服务 `0000ff00-0000-1000-8000-00805f9b34fb`、写特征 FF02、数据通知 FF01、信用通知 FF03（后三者使用相同蓝牙基础 UUID），`withResponse=false`。同步创建，不连接设备；预设仅依据固件文档，不证明硬件支持。

文档 ID：`android:member:com.netumscan.printersdk.M90Profile:fadbe287c9213756`

JVM descriptor：`(II)Lcom/netumscan/printersdk/BleOptions;`

<a id="api-4b74ae3f0e52d0db0f65"></a>

## MediaMode

与打印语言独立的介质跟踪方式枚举。

```java
public final class com.netumscan.printersdk.MediaMode extends java.lang.Enum<com.netumscan.printersdk.MediaMode>
```

用于 `setMedia()` 或兼容 `setMediaMode()`，当前 CPCL 后端实现跟踪模式命令，ESC/POS 后端拒绝。选择枚举不会声明纸宽、纸长、间隙长度或传感器校准结果；调用方必须装入适当介质，并核实固件兼容性。枚举的 Java ordinal 不是公共协议编码，不应自行用于拼接命令。

文档 ID：`android:type:com.netumscan.printersdk.MediaMode`

枚举声明顺序（`ordinal()` 从 0 起；不作为持久化协议值）：

| ordinal | 枚举项 |
| --- | --- |
| 0 | `GAP` |
| 1 | `BLACK_MARK` |
| 2 | `CONTINUOUS` |

<a id="api-f964ddb8df3fb623eb9a"></a>

### public static com.netumscan.printersdk.MediaMode valueOf(java.lang.String)

按精确枚举名称查找介质模式。

```java
public static com.netumscan.printersdk.MediaMode valueOf(java.lang.String);
```

参数 `name` 必须为非 null 且大小写精确的 `GAP`、`BLACK_MARK` 或 `CONTINUOUS`；返回对应枚举常量。Java 自动生成此方法：null 抛出 `NullPointerException`，未匹配名称抛出 `IllegalArgumentException`。不按显示名称、数字或 ordinal 解析，不执行 IO。

文档 ID：`android:member:com.netumscan.printersdk.MediaMode:25dac0f8c6207093`

JVM descriptor：`(Ljava/lang/String;)Lcom/netumscan/printersdk/MediaMode;`

这是 Java 编译器为枚举生成的公共方法。

<a id="api-5e48053988753ec90a1e"></a>

### public static com.netumscan.printersdk.MediaMode[] values()

返回按声明顺序排列的介质枚举数组。

```java
public static com.netumscan.printersdk.MediaMode[] values();
```

无参数，返回新数组 `[GAP, BLACK_MARK, CONTINUOUS]`。这是 Java 枚举自动生成的辅助方法；修改返回数组不改变枚举常量集合。顺序仅用于枚举展示，不能推断设备支持或协议数值。

文档 ID：`android:member:com.netumscan.printersdk.MediaMode:e6138ae9aa44ee4e`

JVM descriptor：`()[Lcom/netumscan/printersdk/MediaMode;`

这是 Java 编译器为枚举生成的公共方法。

<a id="api-011c7bd7bc5fb2bb6e29"></a>

### public static final com.netumscan.printersdk.MediaMode BLACK_MARK

黑标介质的跟踪模式。

```java
public static final com.netumscan.printersdk.MediaMode BLACK_MARK;
```

用于已装入黑标介质的 CPCL 跟踪设置；不测量标记长度、偏移或纸张位置，不执行传感器校准。枚举存在不表示所有设备固件支持该模式。

文档 ID：`android:member:com.netumscan.printersdk.MediaMode:f19064fdd38fb10e`

JVM descriptor：`Lcom/netumscan/printersdk/MediaMode;`

<a id="api-13c48e699eb17d12cb27"></a>

### public static final com.netumscan.printersdk.MediaMode CONTINUOUS

连续纸的介质跟踪模式。

```java
public static final com.netumscan.printersdk.MediaMode CONTINUOUS;
```

用于 CPCL 连续介质跟踪设置；不会自动将标签作业转换为 ESC/POS 小票，也不控制切刀。应用须提供匹配的纸张和打印参数。

文档 ID：`android:member:com.netumscan.printersdk.MediaMode:cf069c583bcf0729`

JVM descriptor：`Lcom/netumscan/printersdk/MediaMode;`

<a id="api-904ffa7e8f2a61c5f45d"></a>

### public static final com.netumscan.printersdk.MediaMode GAP

间隙标签的介质跟踪模式。

```java
public static final com.netumscan.printersdk.MediaMode GAP;
```

用于已装入间隙标签的 CPCL 跟踪设置；不自动测量间隙、设置标签尺寸或执行校准。调用 `setMedia()` 仍可能因设备/协议不支持而失败，成功只证明命令数据已发送。

文档 ID：`android:member:com.netumscan.printersdk.MediaMode:4b03b38b1cbe2f6a`

JVM descriptor：`Lcom/netumscan/printersdk/MediaMode;`

<a id="api-2f5be4c6d23e94746056"></a>

## MediaRequirement

作业提交的显式介质前置条件。

```java
public final class com.netumscan.printersdk.MediaRequirement
```

包含非null MediaSettings和证据策略，不自动设置介质。当前仅CPCL tracking+ALLOW_LAST_SENT可用；REQUIRE_READBACK及其他字段在提交前1004拒绝，不自动降级。

文档 ID：`android:type:com.netumscan.printersdk.MediaRequirement`

<a id="api-934cea35f180203cee46"></a>

### public com.netumscan.printersdk.MediaRequirement(com.netumscan.printersdk.MediaSettings, int)

保存介质前置条件及证据策略。

```java
public com.netumscan.printersdk.MediaRequirement(com.netumscan.printersdk.MediaSettings, int);
```

settings不可null；policy只接受ALLOW_LAST_SENT=1或REQUIRE_READBACK=2，否则IllegalArgumentException。不执行IO；当前仅CPCL tracking+LAST_SENT可提交，读回在IO前1004拒绝。

文档 ID：`android:member:com.netumscan.printersdk.MediaRequirement:9b01646a36c59049`

JVM descriptor：`(Lcom/netumscan/printersdk/MediaSettings;I)V`

<a id="api-075e118dcce2937e38f1"></a>

### public final com.netumscan.printersdk.MediaSettings settings

介质前置条件的settings。

```java
public final com.netumscan.printersdk.MediaSettings settings;
```

settings为非null不可变MediaSettings；verificationPolicy为ALLOW_LAST_SENT=1或REQUIRE_READBACK=2。仅描述要求，不执行设置；当前读回策略不支持。

文档 ID：`android:member:com.netumscan.printersdk.MediaRequirement:c1088016778c7033`

JVM descriptor：`Lcom/netumscan/printersdk/MediaSettings;`

<a id="api-6b3c71590e4b08e71543"></a>

### public final int verificationPolicy

介质前置条件的verificationPolicy。

```java
public final int verificationPolicy;
```

settings为非null不可变MediaSettings；verificationPolicy为ALLOW_LAST_SENT=1或REQUIRE_READBACK=2。仅描述要求，不执行设置；当前读回策略不支持。

文档 ID：`android:member:com.netumscan.printersdk.MediaRequirement:8513b835b607017f`

JVM descriptor：`I`

<a id="api-763fa86084104696fcd6"></a>

### public static final int ALLOW_LAST_SENT = 1

ALLOW_LAST_SENT常量，值1。

```java
public static final int ALLOW_LAST_SENT = 1;
```

允许当前协议最近成功发送的介质缓存，非设备读回。 不执行IO，不表示硬件已验证支持。

文档 ID：`android:member:com.netumscan.printersdk.MediaRequirement:5f044e082d8420a3`

JVM descriptor：`I`

<a id="api-947792eed5478e949e92"></a>

### public static final int REQUIRE_READBACK = 2

REQUIRE_READBACK常量，值2。

```java
public static final int REQUIRE_READBACK = 2;
```

要求设备读回，当前IO前1004拒绝。 不执行IO，不表示硬件已验证支持。

文档 ID：`android:member:com.netumscan.printersdk.MediaRequirement:f4ae445776867292`

JVM descriptor：`I`

<a id="api-e5824d9e8f16babb6dab"></a>

## MediaSettings

以微米表示的不可变介质参数。

```java
public final class com.netumscan.printersdk.MediaSettings
```

null字段表示未指定，不能当作硬件默认；presentFields按非null字段生成。使用时tracking必须存在，现有标准后端只支持CPCL tracking，其他字段在IO前拒绝。

文档 ID：`android:type:com.netumscan.printersdk.MediaSettings`

<a id="api-06350cf378ee9c0d4586"></a>

### public com.netumscan.printersdk.MediaSettings(com.netumscan.printersdk.MediaMode, java.lang.Long, java.lang.Long, java.lang.Long, java.lang.Long, java.lang.Integer, int)

保存显式微米介质参数。

```java
public com.netumscan.printersdk.MediaSettings(com.netumscan.printersdk.MediaMode, java.lang.Long, java.lang.Long, java.lang.Long, java.lang.Long, java.lang.Integer, int);
```

tracking及尺寸字段可null，存在位据此生成；非null长度须在uint32范围（负数/超过0xffffffff构造IllegalArgumentException），实际操作进一步要求正数和tracking匹配。持久化policy只接受0或1；标准后端只支持tracking，其他字段IO前1004拒绝。

文档 ID：`android:member:com.netumscan.printersdk.MediaSettings:e4681a628fcf986d`

JVM descriptor：`(Lcom/netumscan/printersdk/MediaMode;Ljava/lang/Long;Ljava/lang/Long;Ljava/lang/Long;Ljava/lang/Long;Ljava/lang/Integer;I)V`

<a id="api-ff2768b6972dcbc90dc5"></a>

### public final com.netumscan.printersdk.MediaMode tracking

生成仅含tracking的易失介质设置。

```java
public final com.netumscan.printersdk.MediaMode tracking;
```

mode不可null，其余字段未指定、policy=VOLATILE_ONLY。不执行IO；通过MobilePrinter.setMedia提交。

文档 ID：`android:member:com.netumscan.printersdk.MediaSettings:29ec59d75927919b`

JVM descriptor：`Lcom/netumscan/printersdk/MediaMode;`

<a id="api-6ce20c57ddf2984df3c5"></a>

### public final int persistencePolicy

介质persistencePolicy字段。

```java
public final int persistencePolicy;
```

VOLATILE_ONLY=0默认仅易失设置，ALLOW_PERSISTENT=1表示允许持久化，不要求后端必须持久化。 null表示未指定，不是硬件报告；标准后端支持性在提交前校验。

文档 ID：`android:member:com.netumscan.printersdk.MediaSettings:3203af0199d0da68`

JVM descriptor：`I`

<a id="api-91c5a169dc65d41b9147"></a>

### public final int presentFields

介质presentFields字段。

```java
public final int presentFields;
```

TRACKING=1/GAP_LENGTH=2/LABEL_LENGTH=4/WIDTH=8/MARK_LENGTH=16/MARK_OFFSET=32，按非null字段生成。 null表示未指定，不是硬件报告；标准后端支持性在提交前校验。

文档 ID：`android:member:com.netumscan.printersdk.MediaSettings:794dc497376c264f`

JVM descriptor：`I`

<a id="api-5c070755010f3fda64e6"></a>

### public final java.lang.Integer markOffsetMicrometers

介质markOffsetMicrometers字段。

```java
public final java.lang.Integer markOffsetMicrometers;
```

可选有符号黑标偏移（微米），仅BLACK_MARK模式。 null表示未指定，不是硬件报告；标准后端支持性在提交前校验。

文档 ID：`android:member:com.netumscan.printersdk.MediaSettings:4a5c58d30f50366d`

JVM descriptor：`Ljava/lang/Integer;`

<a id="api-dada2dd1d134d006d048"></a>

### public final java.lang.Long gapLengthMicrometers

介质gapLengthMicrometers字段。

```java
public final java.lang.Long gapLengthMicrometers;
```

可选间隙（微米），仅GAP模式。 null表示未指定，不是硬件报告；标准后端支持性在提交前校验。

文档 ID：`android:member:com.netumscan.printersdk.MediaSettings:49aefeb794b34aaf`

JVM descriptor：`Ljava/lang/Long;`

<a id="api-63d5c4618a10caef79aa"></a>

### public final java.lang.Long labelLengthMicrometers

介质labelLengthMicrometers字段。

```java
public final java.lang.Long labelLengthMicrometers;
```

可选标签长度（微米）。 null表示未指定，不是硬件报告；标准后端支持性在提交前校验。

文档 ID：`android:member:com.netumscan.printersdk.MediaSettings:1ab71564f3d5b5a9`

JVM descriptor：`Ljava/lang/Long;`

<a id="api-b3363ec26b6c849ac06e"></a>

### public final java.lang.Long markLengthMicrometers

介质markLengthMicrometers字段。

```java
public final java.lang.Long markLengthMicrometers;
```

可选黑标长度（微米），仅BLACK_MARK模式。 null表示未指定，不是硬件报告；标准后端支持性在提交前校验。

文档 ID：`android:member:com.netumscan.printersdk.MediaSettings:469334b25042ab9e`

JVM descriptor：`Ljava/lang/Long;`

<a id="api-837ac1cf2b702376736a"></a>

### public final java.lang.Long widthMicrometers

介质widthMicrometers字段。

```java
public final java.lang.Long widthMicrometers;
```

可选宽度（微米）。 null表示未指定，不是硬件报告；标准后端支持性在提交前校验。

文档 ID：`android:member:com.netumscan.printersdk.MediaSettings:d98d979bb5fb0351`

JVM descriptor：`Ljava/lang/Long;`

<a id="api-eb04add88b1f2a07c336"></a>

### public static com.netumscan.printersdk.MediaSettings tracking(com.netumscan.printersdk.MediaMode)

生成仅含tracking的易失介质设置。

```java
public static com.netumscan.printersdk.MediaSettings tracking(com.netumscan.printersdk.MediaMode);
```

mode不可null，其余字段未指定、policy=VOLATILE_ONLY。不执行IO；通过MobilePrinter.setMedia提交。

文档 ID：`android:member:com.netumscan.printersdk.MediaSettings:ec3e07d79765b01f`

JVM descriptor：`(Lcom/netumscan/printersdk/MediaMode;)Lcom/netumscan/printersdk/MediaSettings;`

<a id="api-99b4670819c188d481e0"></a>

### public static final int ALLOW_PERSISTENT = 1

ALLOW_PERSISTENT常量，值1。

```java
public static final int ALLOW_PERSISTENT = 1;
```

允许后端持久化，不要求一定持久化。 不执行IO，不表示硬件已验证支持。

文档 ID：`android:member:com.netumscan.printersdk.MediaSettings:70ffa1d5adb462ca`

JVM descriptor：`I`

<a id="api-cdd22e96de0b4001e166"></a>

### public static final int GAP_LENGTH = 2

GAP_LENGTH常量，值2。

```java
public static final int GAP_LENGTH = 2;
```

表示GAP_LENGTH对应的已知/存在字段mask；先检查known或present位再解释相关字段。 不执行IO，不表示硬件已验证支持。

文档 ID：`android:member:com.netumscan.printersdk.MediaSettings:baa2bf119fedcc5e`

JVM descriptor：`I`

<a id="api-e28057198450a2970dad"></a>

### public static final int LABEL_LENGTH = 4

LABEL_LENGTH常量，值4。

```java
public static final int LABEL_LENGTH = 4;
```

表示LABEL_LENGTH对应的已知/存在字段mask；先检查known或present位再解释相关字段。 不执行IO，不表示硬件已验证支持。

文档 ID：`android:member:com.netumscan.printersdk.MediaSettings:7fc0684ca801c37f`

JVM descriptor：`I`

<a id="api-e40c8ce4469837329fee"></a>

### public static final int MARK_LENGTH = 16

MARK_LENGTH常量，值16。

```java
public static final int MARK_LENGTH = 16;
```

表示MARK_LENGTH对应的已知/存在字段mask；先检查known或present位再解释相关字段。 不执行IO，不表示硬件已验证支持。

文档 ID：`android:member:com.netumscan.printersdk.MediaSettings:166a764d81b676c2`

JVM descriptor：`I`

<a id="api-91248227efd9d134953e"></a>

### public static final int MARK_OFFSET = 32

MARK_OFFSET常量，值32。

```java
public static final int MARK_OFFSET = 32;
```

表示MARK_OFFSET对应的已知/存在字段mask；先检查known或present位再解释相关字段。 不执行IO，不表示硬件已验证支持。

文档 ID：`android:member:com.netumscan.printersdk.MediaSettings:542e43778f1fbf84`

JVM descriptor：`I`

<a id="api-298798dd8bd76c5f8357"></a>

### public static final int TRACKING = 1

TRACKING常量，值1。

```java
public static final int TRACKING = 1;
```

表示TRACKING对应的已知/存在字段mask；先检查known或present位再解释相关字段。 不执行IO，不表示硬件已验证支持。

文档 ID：`android:member:com.netumscan.printersdk.MediaSettings:83415fc166517f4b`

JVM descriptor：`I`

<a id="api-cf997c833d8a6ef4cbd9"></a>

### public static final int VOLATILE_ONLY = 0

VOLATILE_ONLY常量，值0。

```java
public static final int VOLATILE_ONLY = 0;
```

仅允许易失设置。 不执行IO，不表示硬件已验证支持。

文档 ID：`android:member:com.netumscan.printersdk.MediaSettings:9275e9849a183175`

JVM descriptor：`I`

<a id="api-716625f2b58108b19de8"></a>

### public static final int WIDTH = 8

WIDTH常量，值8。

```java
public static final int WIDTH = 8;
```

表示WIDTH对应的已知/存在字段mask；先检查known或present位再解释相关字段。 不执行IO，不表示硬件已验证支持。

文档 ID：`android:member:com.netumscan.printersdk.MediaSettings:26874840ab7f4a0d`

JVM descriptor：`I`

<a id="api-58282a32df748c4ef2b2"></a>

## MobilePrinter

逐操作配置的打印机通信设备。

```java
public final class com.netumscan.printersdk.MobilePrinter implements java.lang.AutoCloseable
```

创建只依赖SPP/BLE通信选项，不绑定打印Profile。打印、Raw、介质、定位共用设备唯一FIFO；submit只表示接纳，结果用PrintOperation.result/onComplete观察。阻塞connect/disconnect/close应放后台，Manager提供异步生命周期。无自动重连、重放或续打；ABI3包装须与同版JNI/原生库成套使用。

文档 ID：`android:type:com.netumscan.printersdk.MobilePrinter`

<a id="api-65f08b06135e3355adbb"></a>

### public boolean implementsFeature(int)

仅判断该后端是否实现指定功能。

```java
public boolean implementsFeature(int);
```

参数 `featureId` 使用 FEATURE_* 常量；同步返回 `feature(featureId).isImplemented()`。未知 ID 为 false；不访问硬件，不代表 deviceSupport=SUPPORTED 或存在硬件证据。关闭后的设备抛出 `PrinterException` code 1002；需要呈现设备支持与来源时使用 `feature()` 的三个维度。

文档 ID：`android:member:com.netumscan.printersdk.MobilePrinter:63f3dbfbdb382ee4`

JVM descriptor：`(I)Z`

<a id="api-aa9acfd6ffdda75c5a05"></a>

### public com.netumscan.printersdk.DeviceStateSnapshot stateSnapshot()

同步读取本地一致的设备状态。

```java
public com.netumscan.printersdk.DeviceStateSnapshot stateSnapshot();
```

读取不调用传输回调、不发送打印机 IO、不入队也不等待打印完成。快照只用于展示和诊断，不作为提交前置条件；不得与设备销毁并发访问。 未连接也可读取；设备关闭后抛出 PrinterException code 1002。当前硬件状态固定为未知，未新增硬件查询支持。

文档 ID：`android:member:com.netumscan.printersdk.MobilePrinter:dba7a75445146818`

JVM descriptor：`()Lcom/netumscan/printersdk/DeviceStateSnapshot;`

<a id="api-6277ef711bf11743883a"></a>

### public com.netumscan.printersdk.Diagnostic diagnostic()

读取设备最新一条本地诊断。

```java
public com.netumscan.printersdk.Diagnostic diagnostic();
```

不执行 IO，不清除记录；sequence=0 表示无记录。重连保留较早代次记录，展示前比较 generation。设备关闭后抛 1002；不替代本次 connect 的异常或 operation 的独立诊断。

文档 ID：`android:member:com.netumscan.printersdk.MobilePrinter:48f3c2c79634512b`

JVM descriptor：`()Lcom/netumscan/printersdk/Diagnostic;`

<a id="api-0fa9bb4b61b2a3f589d7"></a>

### public com.netumscan.printersdk.FeatureSupport feature(int)

读取指定功能的本地能力与证据快照。

```java
public com.netumscan.printersdk.FeatureSupport feature(int);
```

参数 `featureId` 使用 FEATURE_LABEL/RECEIPT/STATUS/MEDIA/CUT/RAW/IMAGE/FEED_TO_NEXT_LABEL。同步返回非 null `FeatureSupport`，不发打印机 IO、不入设备队列。未知 ID 返回 implementation=NOT_IMPLEMENTED、deviceSupport=UNKNOWN、evidence=0。已知显式配置通常只有 PROFILE_DECLARED 且设备支持未知；当前图像是 HOST_RENDERED，状态和切纸未实现，CPCL 支持标签/介质/定位实现，ESC/POS 支持小票实现，Raw 两者均有实现。对象关闭等非法状态抛出 `PrinterException` code 1002，不能用实现存在替代硬件验证。 无 Profile 的通信连接描述 SDK 可提供的两种协议实现，标签、小票、介质和定位实现可用；其 deviceSupport 保持 UNKNOWN、evidence=0，不推断打印机支持哪一种协议。

文档 ID：`android:member:com.netumscan.printersdk.MobilePrinter:02554ca7e27ea317`

JVM descriptor：`(I)Lcom/netumscan/printersdk/FeatureSupport;`

<a id="api-3117347855d6e2c56c96"></a>

### public com.netumscan.printersdk.PrintOperation locateNextLabel(com.netumscan.printersdk.CpclProfile, int)

使用本次 CPCL 配置排入下一标签定位操作。

```java
public com.netumscan.printersdk.PrintOperation locateNextLabel(com.netumscan.printersdk.CpclProfile, int);
```

`profile` 为非 null 的 `CpclProfile`，提供此次命令的协议和参数；null 同步抛出 `NullPointerException`。`timeoutMs` 为正整数毫秒，包含排队和执行预算，非法值为 1001。本次配置在接纳时按值复制，只属于这个队列条目，不绑定到连接、不记住最后一次配置，不改变连接的通信选项。可以在同一连接中连续排队不同协议或几何配置的操作，仍与 Raw 共用同一个 FIFO，不切换连接或自动重放。 调用方须先装入并配置适当的间隙或黑标介质；不会配置介质、校准传感器或读回纸张位置。成功返回需持有并关闭的 `PrintOperation`，表示校验和入队；未连接为 2000，设备关闭为 1002，接纳上限为 1008。成功 DATA_SENT 只证明数据传输。

文档 ID：`android:member:com.netumscan.printersdk.MobilePrinter:11874e9102e06cae`

JVM descriptor：`(Lcom/netumscan/printersdk/CpclProfile;I)Lcom/netumscan/printersdk/PrintOperation;`

<a id="api-4ab3e12c40c86d1fdc7d"></a>

### public com.netumscan.printersdk.PrintOperation queryStatus(com.netumscan.printersdk.StatusQueryConfig, int)

按本次协议和响应超时发起状态查询。

```java
public com.netumscan.printersdk.PrintOperation queryStatus(com.netumscan.printersdk.StatusQueryConfig, int);
```

config 不可为 null，timeoutMs 必须大于 0，单位毫秒并包含排队和执行；配置接纳时复制，不要求打印几何。当前标准后端同步抛 1004 UNSUPPORTED_OPERATION，零打印机 IO，硬件维持未知。未来受支持的查询仍共用设备 FIFO，调用方持有并关闭 operation。

文档 ID：`android:member:com.netumscan.printersdk.MobilePrinter:408d64b0ce69b18a`

JVM descriptor：`(Lcom/netumscan/printersdk/StatusQueryConfig;I)Lcom/netumscan/printersdk/PrintOperation;`

<a id="api-94334ec2b14d399b2952"></a>

### public com.netumscan.printersdk.PrintOperation sendRaw(byte[], int, int)

在同一设备队列中发送原始字节及显式份数。

```java
public com.netumscan.printersdk.PrintOperation sendRaw(byte[], int, int);
```

参数 `bytes` 为非 null、非空字节数组，单份至多 1 MiB；`copies` 为 1..100。SDK 在一次调用中复制单份载荷，一个队列条目内逐份发送，不构造 copies 倍拼接数组，不解析、补齐或更改协议字节。调用方须自行保证命令适用于目标固件和当前介质，Raw 不继承标签原点/form。取消、超时或断开会停止后续发送，已发送份数/部分字节无法撤回；不要自动重试不确定的输出。同步完成校验和输入快照复制、接纳后返回非 null `PrintOperation`，不等候操作终态；不要在调用仍进行时并发修改输入数组。`timeoutMs` 为正整数毫秒，入队后的总预算覆盖排队/准备/发送；传输单次写入也受其剩余时间限制。操作与本设备其他打印/Raw/控制 IO 串行。无效参数通常抛出 `PrinterException` code 1001，错误后端/未实现功能为 1004，未连接为 2000，接纳数或源载荷超限为 1008，关闭状态为 1002；已接纳后的异步错误读 `snapshot().error`。结果使用完须 `close()`；成功 DATA_SENT 只证明数据发送。

文档 ID：`android:member:com.netumscan.printersdk.MobilePrinter:0edaf051d84b86c8`

JVM descriptor：`([BII)Lcom/netumscan/printersdk/PrintOperation;`

<a id="api-9f9567a66d5d8914ce1a"></a>

### public com.netumscan.printersdk.PrintOperation setMedia(com.netumscan.printersdk.MediaMode, com.netumscan.printersdk.CpclProfile, int)

使用本次 CPCL 配置排入介质跟踪设置操作。

```java
public com.netumscan.printersdk.PrintOperation setMedia(com.netumscan.printersdk.MediaMode, com.netumscan.printersdk.CpclProfile, int);
```

`mode` 使用 GAP、BLACK_MARK 或 CONTINUOUS；`profile` 为本次 CPCL 参数，两者均不可为 null，否则抛出 `NullPointerException`。`timeoutMs` 为正整数毫秒，覆盖排队和执行，非法值为 1001。本次配置在接纳时按值复制，只属于这个队列条目，不绑定到连接、不记住最后一次配置，不改变连接的通信选项。可以在同一连接中连续排队不同协议或几何配置的操作，仍与 Raw 共用同一个 FIFO，不切换连接或自动重放。 不设置纸宽、纸长或间隙长度，不校准传感器，不自动永久保存设置或读取实际介质。同步校验和接纳后返回非 null `PrintOperation`；未连接为 2000，设备关闭为 1002，接纳上限为 1008，异步错误读取 snapshot。成功 DATA_SENT 只证明设置字节已发送，调用者负责确认硬件适配；结果须关闭。

文档 ID：`android:member:com.netumscan.printersdk.MobilePrinter:cef49c24fd8479cd`

JVM descriptor：`(Lcom/netumscan/printersdk/MediaMode;Lcom/netumscan/printersdk/CpclProfile;I)Lcom/netumscan/printersdk/PrintOperation;`

<a id="api-ba747978d4eebf24f5f5"></a>

### public com.netumscan.printersdk.PrintOperation setMedia(com.netumscan.printersdk.MediaSettings, com.netumscan.printersdk.CpclProfile, int)

按本次CPCL配置设置显式介质字段。

```java
public com.netumscan.printersdk.PrintOperation setMedia(com.netumscan.printersdk.MediaSettings, com.netumscan.printersdk.CpclProfile, int);
```

参数settings/profile/timeoutMs；同一设备FIFO，返回operation只表示接纳。当前只支持tracking字段，其他字段/协议在IO前1004拒绝，不能静默丢弃。timeout为正且涵盖排队，设置完成不代表读回验证。

文档 ID：`android:member:com.netumscan.printersdk.MobilePrinter:2dacd65a25c194f0`

JVM descriptor：`(Lcom/netumscan/printersdk/MediaSettings;Lcom/netumscan/printersdk/CpclProfile;I)Lcom/netumscan/printersdk/PrintOperation;`

<a id="api-ad7474aaa15d46e4990d"></a>

### public com.netumscan.printersdk.PrintOperation submit(com.netumscan.printersdk.PrintJob, com.netumscan.printersdk.CpclProfile, int)

提交不可变作业及本次打印配置。

```java
public com.netumscan.printersdk.PrintOperation submit(com.netumscan.printersdk.PrintJob, com.netumscan.printersdk.CpclProfile, int);
```

job、profile不可null，timeoutMs为正，覆盖排队准备发送。标签配CpclProfile、小票配EscPosProfile；配置只属于本队列条目，共享设备FIFO。成功返回operation只表示接纳，job可关闭；错误/不支持/接纳超限1001/1004/1008，不自动重放。

文档 ID：`android:member:com.netumscan.printersdk.MobilePrinter:a1c5de7d0a102558`

JVM descriptor：`(Lcom/netumscan/printersdk/PrintJob;Lcom/netumscan/printersdk/CpclProfile;I)Lcom/netumscan/printersdk/PrintOperation;`

<a id="api-3ae1ddd2652147d9aafb"></a>

### public com.netumscan.printersdk.PrintOperation submit(com.netumscan.printersdk.PrintJob, com.netumscan.printersdk.EscPosProfile, int)

提交不可变作业及本次打印配置。

```java
public com.netumscan.printersdk.PrintOperation submit(com.netumscan.printersdk.PrintJob, com.netumscan.printersdk.EscPosProfile, int);
```

job、profile不可null，timeoutMs为正，覆盖排队准备发送。标签配CpclProfile、小票配EscPosProfile；配置只属于本队列条目，共享设备FIFO。成功返回operation只表示接纳，job可关闭；错误/不支持/接纳超限1001/1004/1008，不自动重放。

文档 ID：`android:member:com.netumscan.printersdk.MobilePrinter:20e5b6f953c9c633`

JVM descriptor：`(Lcom/netumscan/printersdk/PrintJob;Lcom/netumscan/printersdk/EscPosProfile;I)Lcom/netumscan/printersdk/PrintOperation;`

<a id="api-c523f3bf70cf2bcaff9b"></a>

### public com.netumscan.printersdk.PrintOperation submitLabelGray8(byte[], int, int, com.netumscan.printersdk.CpclProfile, int, int, int, int, boolean, int)

使用本次 CPCL 配置提交一张完整 Gray8 标签。

```java
public com.netumscan.printersdk.PrintOperation submitLabelGray8(byte[], int, int, com.netumscan.printersdk.CpclProfile, int, int, int, int, boolean, int);
```

`profile` 为非 null 的本次打印配置，null 同步抛出 `NullPointerException`。本次配置在接纳时按值复制，只属于这个队列条目，不绑定到连接、不记住最后一次配置，不改变连接的通信选项。可以在同一连接中连续排队不同协议或几何配置的操作，仍与 Raw 共用同一个 FIFO，不切换连接或自动重放。 参数 `pixels` 不可为 null，必须为紧密排列、逐行存储的 `width*height` 字节：每像素灰度 0 黑、255 白，不含行 padding。`width`、`height` 为正整数 dots；单图源数据至多 16 MiB。`copies` 为 1..100；`trailingBlankDots` 为非负逐份结束留白；`originX`、`originY` 为非负 dots；`form=true` 使用下一标签开始策略，false 使用内容结束策略。`originX + ceil(width/8)*8` 不得超过 profile.widthDots，`originY + height + trailingBlankDots` 不得超过 profile.maxLabelHeightDots。没有自动缩放、负坐标或裁剪；采用阈值 128 的灰度转单色处理。本次 profile 明确选择 CPCL 协议，显式原点/form 不继承 profile 的兼容默认值。同步完成校验和输入快照复制、接纳后返回非 null `PrintOperation`，不等候操作终态；不要在调用仍进行时并发修改输入数组。`timeoutMs` 为正整数毫秒，入队后的总预算覆盖排队/准备/发送；传输单次写入也受其剩余时间限制。操作与本设备其他打印/Raw/控制 IO 串行。无效参数通常抛出 `PrinterException` code 1001，错误后端/未实现功能为 1004，未连接为 2000，接纳数或源载荷超限为 1008，关闭状态为 1002；已接纳后的异步错误读 `snapshot().error`。结果使用完须 `close()`；成功 DATA_SENT 只证明数据发送。

文档 ID：`android:member:com.netumscan.printersdk.MobilePrinter:417a8dba17562ad1`

JVM descriptor：`([BIILcom/netumscan/printersdk/CpclProfile;IIIIZI)Lcom/netumscan/printersdk/PrintOperation;`

<a id="api-6115e4465af1bd3fcbef"></a>

### public com.netumscan.printersdk.PrintOperation submitReceiptGray8(byte[], int, int, com.netumscan.printersdk.EscPosProfile, int, int, long, int, int)

使用本次 ESC/POS 配置提交一张完整 Gray8 小票。

```java
public com.netumscan.printersdk.PrintOperation submitReceiptGray8(byte[], int, int, com.netumscan.printersdk.EscPosProfile, int, int, long, int, int);
```

`profile` 为非 null 的本次打印配置，null 同步抛出 `NullPointerException`。本次配置在接纳时按值复制，只属于这个队列条目，不绑定到连接、不记住最后一次配置，不改变连接的通信选项。可以在同一连接中连续排队不同协议或几何配置的操作，仍与 Raw 共用同一个 FIFO，不切换连接或自动重放。 参数 `pixels` 不可为 null，必须为 `width*height` 字节的紧密 Gray8 行数据（0 黑、255 白，无行 padding），单图至多 16 MiB；`width`、`height` 为正整数 dots，按 8 点补齐的宽度不得超过 profile.widthDots，不自动缩放。`copies` 为 1..100，`trailingBlankDots` 为每份的非负结束留白。`maxTotalLength` 为 1..4294967295 dots 的调用方整单预算，须覆盖 `(height + trailingBlankDots) * copies`；它不替代设备单份长度限制。`cutMode` 使用 CUT_NONE/FULL/PARTIAL，当前后端只支持 CUT_NONE，另外两种明确抛出 1004，不静默忽略。本次 profile 明确选择 ESC/POS 协议；灰度采用阈值 128 转单色。同步完成校验和输入快照复制、接纳后返回非 null `PrintOperation`，不等候操作终态；不要在调用仍进行时并发修改输入数组。`timeoutMs` 为正整数毫秒，入队后的总预算覆盖排队/准备/发送；传输单次写入也受其剩余时间限制。操作与本设备其他打印/Raw/控制 IO 串行。无效参数通常抛出 `PrinterException` code 1001，错误后端/未实现功能为 1004，未连接为 2000，接纳数或源载荷超限为 1008，关闭状态为 1002；已接纳后的异步错误读 `snapshot().error`。结果使用完须 `close()`；成功 DATA_SENT 只证明数据发送。

文档 ID：`android:member:com.netumscan.printersdk.MobilePrinter:5a6f93a357312ca8`

JVM descriptor：`([BIILcom/netumscan/printersdk/EscPosProfile;IIJII)Lcom/netumscan/printersdk/PrintOperation;`

<a id="api-753106b34610ac1e3dcb"></a>

### public com.netumscan.printersdk.PrinterCapabilities capabilities()

读取聚合本地能力与约束。

```java
public com.netumscan.printersdk.PrinterCapabilities capabilities();
```

无IO、无探测、无接纳预留，返回PrinterCapabilities；设备close后1002。hardware support/known/evidence分开，纯通信设备几何未知。

文档 ID：`android:member:com.netumscan.printersdk.MobilePrinter:772db24b34ff95b0`

JVM descriptor：`()Lcom/netumscan/printersdk/PrinterCapabilities;`

<a id="api-b0353c1fddd5370e37b3"></a>

### public com.netumscan.printersdk.ResourceSnapshot resourceSnapshot()

读取设备源载荷接纳限制和当前占用。

```java
public com.netumscan.printersdk.ResourceSnapshot resourceSnapshot();
```

本地一致快照，不执行 IO，不预留空间；submit 决定实际接纳。设备关闭后抛 1002，原生资源或 ABI 错误按 PrinterException 返回。

文档 ID：`android:member:com.netumscan.printersdk.MobilePrinter:b90ef5bc8e4476e9`

JVM descriptor：`()Lcom/netumscan/printersdk/ResourceSnapshot;`

<a id="api-6cc2840ca6523738aeda"></a>

### public com.netumscan.printersdk.ScalarConstraint constraint(int)

读取指定参数的标量约束。

```java
public com.netumscan.printersdk.ScalarConstraint constraint(int);
```

parameterId 使用 ScalarConstraint 标识；本地查询无 IO，不保证硬件支持。known=false 表示未知。设备关闭后抛 1002，其他原生错误按 PrinterException 返回。

文档 ID：`android:member:com.netumscan.printersdk.MobilePrinter:a258c0e7ea03dc6f`

JVM descriptor：`(I)Lcom/netumscan/printersdk/ScalarConstraint;`

<a id="api-79d9e40d8bdcfde116d4"></a>

### public com.netumscan.printersdk.Subscription observeState(com.netumscan.printersdk.MobilePrinter$StateListener)

订阅合并的本地设备状态。

```java
public com.netumscan.printersdk.Subscription observeState(com.netumscan.printersdk.MobilePrinter$StateListener);
```

listener与显式executor不可null。默认主线程或指定Executor，先投递当前快照再合并相同值，不保证中间事件历史。Subscription.close或设备close结束监听，不取消打印；共享按需观察器只调用本地getters。拒绝custom Executor时记录日志并向主线程降级一次，callback异常隔离。

文档 ID：`android:member:com.netumscan.printersdk.MobilePrinter:14b86ad273d436f4`

JVM descriptor：`(Lcom/netumscan/printersdk/MobilePrinter$StateListener;)Lcom/netumscan/printersdk/Subscription;`

<a id="api-014b71b7a4f62f0f1131"></a>

### public com.netumscan.printersdk.Subscription observeState(java.util.concurrent.Executor, com.netumscan.printersdk.MobilePrinter$StateListener)

订阅合并的本地设备状态。

```java
public com.netumscan.printersdk.Subscription observeState(java.util.concurrent.Executor, com.netumscan.printersdk.MobilePrinter$StateListener);
```

listener与显式executor不可null。默认主线程或指定Executor，先投递当前快照再合并相同值，不保证中间事件历史。Subscription.close或设备close结束监听，不取消打印；共享按需观察器只调用本地getters。拒绝custom Executor时记录日志并向主线程降级一次，callback异常隔离。

文档 ID：`android:member:com.netumscan.printersdk.MobilePrinter:6d5d6894abc8709c`

JVM descriptor：`(Ljava/util/concurrent/Executor;Lcom/netumscan/printersdk/MobilePrinter$StateListener;)Lcom/netumscan/printersdk/Subscription;`

<a id="api-88210c16c0ec402f3612"></a>

### public int connectionState()

读取 SDK 本地观察到的连接状态，不探测打印机。

```java
public int connectionState();
```

无参数，返回 DISCONNECTED=0、CONNECTING=1、CONNECTED=2 或 DISCONNECTING=3。同步读取本地状态，不发蓝牙 IO、不入打印队列，可在连接转换或打印时读取；对象关闭/正在关闭等非法生命周期抛出 `PrinterException` code 1002。CONNECTED 仅表示当前 SDK 观察状态，不保证设备就绪、纸张正常或链路此刻存活；未被 IO 察觉的断线可能暂时仍显示 CONNECTED。

文档 ID：`android:member:com.netumscan.printersdk.MobilePrinter:2414ee607f59e5f0`

JVM descriptor：`()I`

<a id="api-16b02c26c8039a6fbe92"></a>

### public static com.netumscan.printersdk.MobilePrinter ble(android.content.Context, java.lang.String, com.netumscan.printersdk.BleOptions)

创建不绑定打印 Profile 的 BLE 通信设备。

```java
public static com.netumscan.printersdk.MobilePrinter ble(android.content.Context, java.lang.String, com.netumscan.printersdk.BleOptions);
```

`context` 为有效非 null Android Context，`address` 为有效蓝牙设备地址，`ble` 为非 null 的 `BleOptions`，指定服务、写特征、写方式、MTU 请求、分包和可选信用流控。返回尚未连接的新 `MobilePrinter`，构造不扫描、不申请权限、不协商 MTU、不执行打印机 IO；随后在应用工作线程调用 `connect(timeoutMs)`，最终调用 `close()`。连接不需要协议、DPI 或打印点宽，也不会自动检测这些信息；结构化打印、介质设置和定位须使用带本次 profile 的重载，所有结构化操作必须提供本次profile；Raw 无需 profile。无蓝牙适配器为 `PrinterException` 1004，原生失败按 SDK 错误码报告，ABI 不匹配为 1006。新入口要求匹配的 ABI 3/r1 SDK 整包；非法/null 平台参数并不保证统一映射到 SDK 错误码，调用者须先校验。

文档 ID：`android:member:com.netumscan.printersdk.MobilePrinter:a7faeba495fa913a`

JVM descriptor：`(Landroid/content/Context;Ljava/lang/String;Lcom/netumscan/printersdk/BleOptions;)Lcom/netumscan/printersdk/MobilePrinter;`

<a id="api-be8a80f277cb783e4a0f"></a>

### public static com.netumscan.printersdk.MobilePrinter spp(android.content.Context, java.lang.String)

创建不绑定打印 Profile 的经典蓝牙通信设备。

```java
public static com.netumscan.printersdk.MobilePrinter spp(android.content.Context, java.lang.String);
```

`context` 为有效非 null Android Context，`address` 为有效蓝牙设备地址；使用标准 SPP/RFCOMM 服务 UUID `00001101-0000-1000-8000-00805f9b34fb`。返回尚未连接的新 `MobilePrinter`，构造不发送打印数据，不提供自动配对、发现或权限申请。随后在应用工作线程调用 `connect(timeoutMs)`，最终调用 `close()`；连接无需协议、DPI 或打印点宽。结构化打印、介质设置及定位使用带本次 profile 的重载，所有结构化操作必须提供本次profile；Raw 无需 profile。无蓝牙适配器为 `PrinterException` 1004，原生失败按 SDK 错误码报告，ABI 不匹配为 1006；须使用匹配 ABI 3/r1 的 SDK 整包。非法/null 平台参数并不保证统一映射 SDK 错误码，调用者须先校验。

文档 ID：`android:member:com.netumscan.printersdk.MobilePrinter:e4c91f96c709bae3`

JVM descriptor：`(Landroid/content/Context;Ljava/lang/String;)Lcom/netumscan/printersdk/MobilePrinter;`

<a id="api-855d672961456beee435"></a>

### public static com.netumscan.printersdk.SdkInfo sdkInfo()

读取实际加载原生运行库身份。

```java
public static com.netumscan.printersdk.SdkInfo sdkInfo();
```

静态本地查询，无需设备或蓝牙连接，不进行 IO。返回 SdkInfo，ABI、原生版本、分发版本及可选构建 ID 均来自已加载原生库；资源或 ABI 错误按 PrinterException 返回。

文档 ID：`android:member:com.netumscan.printersdk.MobilePrinter:1fb03319fdb2941c`

JVM descriptor：`()Lcom/netumscan/printersdk/SdkInfo;`

<a id="api-bdaf7186295da0476fee"></a>

### public static final int CONNECTED = 2

连接状态值 2：SDK 当前观察到会话已连接。

```java
public static final int CONNECTED = 2;
```

由 `connectionState()` 返回。该值不探测实时存活，不确认介质/设备状态或打印完成；尚未被 IO 察觉的断线可暂时保留此值。

文档 ID：`android:member:com.netumscan.printersdk.MobilePrinter:f822937144b14a78`

JVM descriptor：`I`

<a id="api-4e59ef48a31e38d0eebb"></a>

### public static final int CONNECTING = 1

连接状态值 1：连接过程正在进行。

```java
public static final int CONNECTING = 1;
```

由 `connectionState()` 返回，仅表示 SDK 正在建立会话，不能提交并假定打印机已经就绪。应等待工作线程的 `connect()` 成功或失败。

文档 ID：`android:member:com.netumscan.printersdk.MobilePrinter:f82bf1b4f9c9691f`

JVM descriptor：`I`

<a id="api-c0b65906a9d740bb8e3e"></a>

### public static final int CUT_FULL = 1

切纸模式值 1：请求全切。

```java
public static final int CUT_FULL = 1;
```

供小票 `cutMode` 表达请求，但当前标准后端未实现，调用明确抛出 1004，不自动降级为不切纸。常量存在不证明设备有切刀。

文档 ID：`android:member:com.netumscan.printersdk.MobilePrinter:64956941107fd9c3`

JVM descriptor：`I`

<a id="api-cd14b5395caec9d5635f"></a>

### public static final int CUT_NONE = 0

切纸模式值 0：不请求切纸。

```java
public static final int CUT_NONE = 0;
```

供 `submitReceiptGray8()` 的 `cutMode` 使用，是当前 ESC/POS 后端唯一支持的切纸模式。结束留白由 trailingBlankDots 独立控制。

文档 ID：`android:member:com.netumscan.printersdk.MobilePrinter:a31548e7e8ce3a2e`

JVM descriptor：`I`

<a id="api-f1e0aa48444d66ae8dcc"></a>

### public static final int CUT_PARTIAL = 2

切纸模式值 2：请求半切。

```java
public static final int CUT_PARTIAL = 2;
```

供小票 `cutMode` 表达请求，但当前标准后端未实现，调用明确抛出 1004，不静默忽略。硬件是否具备半切需单独证据。

文档 ID：`android:member:com.netumscan.printersdk.MobilePrinter:b6051533a14a0c38`

JVM descriptor：`I`

<a id="api-9bf56ec14e15d9850d79"></a>

### public static final int DISCONNECTED = 0

连接状态值 0：SDK 已处于断开状态。

```java
public static final int DISCONNECTED = 0;
```

由 `connectionState()` 返回；设备创建后尚未连接或断开完成均可处于此状态。提交要求连接的工作通常失败为 2000；可对未关闭实例显式 `connect()`。

文档 ID：`android:member:com.netumscan.printersdk.MobilePrinter:34e35f024a3b5787`

JVM descriptor：`I`

<a id="api-bb0aae2f9aa799783292"></a>

### public static final int DISCONNECTING = 3

连接状态值 3：断开控制过程尚未完成。

```java
public static final int DISCONNECTING = 3;
```

由 `connectionState()` 返回；正在中止工作并释放会话。不要把该状态作为继续发送的许可，应等待阻塞 `disconnect()`/`close()` 完成。

文档 ID：`android:member:com.netumscan.printersdk.MobilePrinter:f75cf21f8bbc467c`

JVM descriptor：`I`

<a id="api-42bcc08688e48a07e223"></a>

### public static final int FEATURE_CUT = 5

功能 ID 5：小票切纸。

```java
public static final int FEATURE_CUT = 5;
```

传给 feature()/implementsFeature()；当前标准后端未实现，全切和半切请求明确拒绝。协议或型号名称不能代替 feature 的实现与硬件证据。

文档 ID：`android:member:com.netumscan.printersdk.MobilePrinter:7ac9bf17bd0dbac2`

JVM descriptor：`I`

<a id="api-aecd50874f30dffeb050"></a>

### public static final int FEATURE_FEED_TO_NEXT_LABEL = 8

功能 ID 8：独立定位到下一标签开始。

```java
public static final int FEATURE_FEED_TO_NEXT_LABEL = 8;
```

传给 feature()/implementsFeature()；当前 CPCL 有原生命令实现，ESC/POS 没有。不设置介质、不做校准，成功仍只 DATA_SENT。

文档 ID：`android:member:com.netumscan.printersdk.MobilePrinter:bb60d15581a3e604`

JVM descriptor：`I`

<a id="api-21d85042ab649a0605f2"></a>

### public static final int FEATURE_IMAGE = 7

功能 ID 7：主机侧图像处理与光栅打印。

```java
public static final int FEATURE_IMAGE = 7;
```

传给 feature()/implementsFeature()；当前实现类别为 HOST_RENDERED。公共 Java 提交入口接受紧密 Gray8 整图，不意味着公开了文本、条码或所有 C 图像格式。

文档 ID：`android:member:com.netumscan.printersdk.MobilePrinter:bbf3ec21c86c2b1d`

JVM descriptor：`I`

<a id="api-2afd7c4407f09a5cb37b"></a>

### public static final int FEATURE_LABEL = 1

功能 ID 1：标签作业编码与提交。

```java
public static final int FEATURE_LABEL = 1;
```

传给 feature()/implementsFeature()；当前 CPCL 有原生实现，ESC/POS 没有。实现类别不保证目标型号或介质已验证。

文档 ID：`android:member:com.netumscan.printersdk.MobilePrinter:4c5619690e84e1bf`

JVM descriptor：`I`

<a id="api-33add0eb9163c6cd66f8"></a>

### public static final int FEATURE_MEDIA = 4

功能 ID 4：介质设置。

```java
public static final int FEATURE_MEDIA = 4;
```

传给 feature()/implementsFeature()；当前 CPCL 实现跟踪模式设置，ESC/POS 未实现。不等于完整纸宽/纸长/间隙参数或传感器校准能力。

文档 ID：`android:member:com.netumscan.printersdk.MobilePrinter:bdac68bb07a3ff93`

JVM descriptor：`I`

<a id="api-203033ba1ebc26da61c4"></a>

### public static final int FEATURE_RAW = 6

功能 ID 6：同队列发送原始命令。

```java
public static final int FEATURE_RAW = 6;
```

传给 feature()/implementsFeature()；当前两种后端均有原生发送路径。SDK 不校验 Raw 字节是否适合固件，必须由调用方保证命令安全及协议正确性。

文档 ID：`android:member:com.netumscan.printersdk.MobilePrinter:7bb504424a281ed6`

JVM descriptor：`I`

<a id="api-4e8cc7777832a02084a6"></a>

### public static final int FEATURE_RECEIPT = 2

功能 ID 2：小票作业编码与提交。

```java
public static final int FEATURE_RECEIPT = 2;
```

传给 feature()/implementsFeature()；当前 ESC/POS 有原生实现，CPCL 没有。整张小票内容由应用渲染为 Gray8，不包含切刀支持承诺。

文档 ID：`android:member:com.netumscan.printersdk.MobilePrinter:34743d368e7191d9`

JVM descriptor：`I`

<a id="api-cafb7376a05a6005fa90"></a>

### public static final int FEATURE_STATUS = 3

功能 ID 3：打印机状态查询。

```java
public static final int FEATURE_STATUS = 3;
```

传给 feature()/implementsFeature()；当前 CPCL 与 ESC/POS 标准后端均未实现，queryStatus() 明确拒绝为 1004。与本地 connectionState() 不同。

文档 ID：`android:member:com.netumscan.printersdk.MobilePrinter:57a2e549cda799a3`

JVM descriptor：`I`

<a id="api-e307b0083d3557a43bd2"></a>

### public synchronized void close()

同步断开并释放设备及其兼容 task 结果。

```java
public synchronized void close();
```

无参数、无返回值，实现 `AutoCloseable`，可重复调用。会停止活动/排队工作、断开传输并释放设备，可能阻塞，须在应用工作线程执行。完成后除再次 `close()` 外不应调用本设备；其他设备方法通常抛出 `PrinterException` code 1002。此前由调用方持有的独立 `PrintOperation` 可继续读取终态并单独关闭；兼容 task 入口保管的结果在此释放，不能再轮询。关闭不撤回已经发送的数据。

文档 ID：`android:member:com.netumscan.printersdk.MobilePrinter:2c2502cc89f847f7`

JVM descriptor：`()V`

<a id="api-e266e6306e56a1ddffff"></a>

### public void connect(int)

同步建立蓝牙会话并等待连接结果。

```java
public void connect(int);
```

参数 `timeoutMs` 必须为正整数，单位毫秒，否则同步抛出 `IllegalArgumentException`。方法阻塞至连接成功或失败，无返回值，须在应用工作线程调用；成功后可提交操作。已连接时成功返回，连接/断开转换期间可能抛出 `PrinterException` code 1002；权限/蓝牙状态错误通常为 1002，不支持的特征为 1004，超时为 2001，断线为 2002，其他 IO 失败为 2003。连接失败不会自动重试或重放旧数据。关闭后的实例不可重连，须新建设备。

文档 ID：`android:member:com.netumscan.printersdk.MobilePrinter:2331cc3ab7b99fc0`

JVM descriptor：`(I)V`

<a id="api-b4ecf7674244c1ed1580"></a>

### public void disconnect()

同步发出控制信号，停止设备工作并断开会话。

```java
public void disconnect();
```

无参数、无返回值，可能等待活动 IO 停止，须在应用工作线程调用。断开是控制信号，会中止活动及排队工作，不等候普通 FIFO 打印任务逐个成功；已发送内容无法撤回。已断开时可重复调用；设备对象仍可通过显式 `connect()` 建立新会话，旧任务不会重放。独立 `PrintOperation` 仍可读取各自终态；对象已关闭等非法生命周期抛出 `PrinterException` code 1002。

文档 ID：`android:member:com.netumscan.printersdk.MobilePrinter:bd89511427af4f83`

JVM descriptor：`()V`

<a id="api-436fb81e3c66cbf43885"></a>

## MobilePrinter.StateListener

接收合并本地设备快照的监听器。

```java
public interface com.netumscan.printersdk.MobilePrinter$StateListener
```

先投递当前快照，之后合并相同值，不保证短暂中间状态的完整历史。默认主线程或指定Executor，回调锁外执行，不触发硬件IO。

文档 ID：`android:type:com.netumscan.printersdk.MobilePrinter$StateListener`

<a id="api-38804c7cce1df8fe69c2"></a>

### public abstract void changed(com.netumscan.printersdk.DeviceStateSnapshot)

接收一份合并的本地设备快照。

```java
public abstract void changed(com.netumscan.printersdk.DeviceStateSnapshot);
```

snapshot非null。默认主线程或指定Executor锁外执行；CONNECTED/IDLE/空队列不证明硬件就绪，硬件未知保持UNKNOWN。

文档 ID：`android:member:com.netumscan.printersdk.MobilePrinter$StateListener:d3dc14b087892a4e`

JVM descriptor：`(Lcom/netumscan/printersdk/DeviceStateSnapshot;)V`

<a id="api-4d9f9ddfaae0d09415c5"></a>

## PrintJob

保留共享图像存储的不可变作业。

```java
public final class com.netumscan.printersdk.PrintJob implements java.lang.AutoCloseable
```

由Builder.build一次创建元数据快照，后续修改builder不改变已建job。submit保留所有所需存储，返回后可close job；close不取消打印。每作业逻辑图片预算32MiB，同一图片多次引用分别计入。

文档 ID：`android:type:com.netumscan.printersdk.PrintJob`

<a id="api-02a1552cf29814da4051"></a>

### public static com.netumscan.printersdk.PrintJob$Builder label(int, int, int)

创建标签作业构建器。

```java
public static com.netumscan.printersdk.PrintJob$Builder label(int, int, int);
```

参数依次widthDots、heightDots、dpi；只保存配置，不创建连接或执行IO。copies默认1、origin0、nextLabel=false，完整边界在build/submit校验。

文档 ID：`android:member:com.netumscan.printersdk.PrintJob:6cd656b116b27b9c`

JVM descriptor：`(III)Lcom/netumscan/printersdk/PrintJob$Builder;`

<a id="api-9eb57788de8dc10e2abc"></a>

### public static com.netumscan.printersdk.PrintJob$Builder receipt(int, long, int)

创建小票作业构建器。

```java
public static com.netumscan.printersdk.PrintJob$Builder receipt(int, long, int);
```

参数依次widthDots、maxTotalLengthDots、dpi；整单长度包含所有副本和尾部留白，使用uint32正数范围。只保存选项，不执行IO，后续顺序appendImage/appendBlank。

文档 ID：`android:member:com.netumscan.printersdk.PrintJob:cd5d89d2a582c347`

JVM descriptor：`(IJI)Lcom/netumscan/printersdk/PrintJob$Builder;`

<a id="api-58e8d5b9f5521b1001ff"></a>

### public void close()

释放调用方不可变作业句柄。

```java
public void close();
```

幂等；已接纳operation保留其所需存储，close不取消打印。之后不能再次用此job提交，返回1002。

文档 ID：`android:member:com.netumscan.printersdk.PrintJob:71f6e6626cb069d2`

JVM descriptor：`()V`

<a id="api-651fa2175d90c196f861"></a>

## PrintJob.Builder

平台层的类型化作业构建器。

```java
public final class com.netumscan.printersdk.PrintJob$Builder
```

label/receipt工厂选择作业种类，配置只影响本作业。builder在调用方线程使用，不承诺并发修改安全；引用的PrinterImage须保持open至build返回。build不复制像素、不执行IO，最多1024项且至少一幅图片。

文档 ID：`android:type:com.netumscan.printersdk.PrintJob$Builder`

<a id="api-f3884d198686fc36fac0"></a>

### public com.netumscan.printersdk.PrintJob build()

一次创建不可变作业元数据快照。

```java
public com.netumscan.printersdk.PrintJob build();
```

引用图片须open至返回；元数据复制、图像存储retain，不再次复制像素。最多1024项且至少一幅图，每作业逻辑图片32MiB，重复引用分别计数。无效1001/不支持1004/资源1008，不产生IO；后续修改builder不改变已建job。

文档 ID：`android:member:com.netumscan.printersdk.PrintJob$Builder:9c4af4b59c0ea701`

JVM descriptor：`()Lcom/netumscan/printersdk/PrintJob;`

<a id="api-67d4708ea087169784da"></a>

### public com.netumscan.printersdk.PrintJob$Builder addImage(com.netumscan.printersdk.PrinterImage, int, int)

追加图像引用和位置。

```java
public com.netumscan.printersdk.PrintJob$Builder addImage(com.netumscan.printersdk.PrinterImage, int, int);
```

image不可null且保持open至build；x/y为点。标签需非负且在画布内，当前CPCL只接受x=0、纵向不重叠。小票只接受x=y=0并顺序追加；不复制像素，不执行IO。

文档 ID：`android:member:com.netumscan.printersdk.PrintJob$Builder:e7c55b5c90cf40fc`

JVM descriptor：`(Lcom/netumscan/printersdk/PrinterImage;II)Lcom/netumscan/printersdk/PrintJob$Builder;`

<a id="api-3f4b1342ee18e385e700"></a>

### public com.netumscan.printersdk.PrintJob$Builder appendBlank(int)

追加小票空白点数。

```java
public com.netumscan.printersdk.PrintJob$Builder appendBlank(int);
```

仅小票，否则IllegalStateException；dots须为正并计入整单长度，build时完整检查，不执行IO。

文档 ID：`android:member:com.netumscan.printersdk.PrintJob$Builder:ab27f10fa07b944e`

JVM descriptor：`(I)Lcom/netumscan/printersdk/PrintJob$Builder;`

<a id="api-0e2b89713111ec32307e"></a>

### public com.netumscan.printersdk.PrintJob$Builder appendImage(com.netumscan.printersdk.PrinterImage)

顺序追加小票图像引用。

```java
public com.netumscan.printersdk.PrintJob$Builder appendImage(com.netumscan.printersdk.PrinterImage);
```

仅小票，否则IllegalStateException；等价以零坐标addImage，image非null且open至build。

文档 ID：`android:member:com.netumscan.printersdk.PrintJob$Builder:8535c97ad735317e`

JVM descriptor：`(Lcom/netumscan/printersdk/PrinterImage;)Lcom/netumscan/printersdk/PrintJob$Builder;`

<a id="api-0cc600c68265b517f4b0"></a>

### public com.netumscan.printersdk.PrintJob$Builder copies(int)

设置本作业份数。

```java
public com.netumscan.printersdk.PrintJob$Builder copies(int);
```

value最终须1..100，默认1；返回同一builder，完整有效性在build校验，不扩增图片物理存储或Raw预算。

文档 ID：`android:member:com.netumscan.printersdk.PrintJob$Builder:7af593b4248bac8a`

JVM descriptor：`(I)Lcom/netumscan/printersdk/PrintJob$Builder;`

<a id="api-c922fa8f413a65f19c3b"></a>

### public com.netumscan.printersdk.PrintJob$Builder mediaRequirement(com.netumscan.printersdk.MediaRequirement)

设置本作业介质前置要求。

```java
public com.netumscan.printersdk.PrintJob$Builder mediaRequirement(com.netumscan.printersdk.MediaRequirement);
```

requirement不可null；不自动发送设置。build校验字段，submit根据当前协议缓存及证据策略检查；READBACK目前1004拒绝。

文档 ID：`android:member:com.netumscan.printersdk.PrintJob$Builder:dcd188965e8a3ae0`

JVM descriptor：`(Lcom/netumscan/printersdk/MediaRequirement;)Lcom/netumscan/printersdk/PrintJob$Builder;`

<a id="api-3da530155f8539733c77"></a>

### public com.netumscan.printersdk.PrintJob$Builder nextLabel(boolean)

设置标签结束是否定位下一起点。

```java
public com.netumscan.printersdk.PrintJob$Builder nextLabel(boolean);
```

true选择NEXT_LABEL_START，false选择CONTENT_END，默认false；仅标签，否则IllegalStateException。只属于本作业，不改变连接或主动校准介质。

文档 ID：`android:member:com.netumscan.printersdk.PrintJob$Builder:6beb2408391d9c39`

JVM descriptor：`(Z)Lcom/netumscan/printersdk/PrintJob$Builder;`

<a id="api-8fdfba7a7570c1dd3f5f"></a>

### public com.netumscan.printersdk.PrintJob$Builder origin(int, int)

设置标签的非负原点。

```java
public com.netumscan.printersdk.PrintJob$Builder origin(int, int);
```

x/y单位点，仅标签；误用于小票立即IllegalStateException。范围与画布在build/submit校验，不自动裁剪。

文档 ID：`android:member:com.netumscan.printersdk.PrintJob$Builder:87fc30a4a97b852e`

JVM descriptor：`(II)Lcom/netumscan/printersdk/PrintJob$Builder;`

<a id="api-e7eb03673962fa1a081f"></a>

### public com.netumscan.printersdk.PrintJob$Builder receiptFinish(int, int)

设置小票结束留白、切纸及作用范围。

```java
public com.netumscan.printersdk.PrintJob$Builder receiptFinish(int, int);
```

参数trailingBlankDots、cutMode、可选finishScope；双参数默认EACH_COPY。仅小票，否则IllegalStateException；当前标准后端只支持不切纸和EACH_COPY，其他值在提交前明确拒绝。

文档 ID：`android:member:com.netumscan.printersdk.PrintJob$Builder:aed007bc4670e2f5`

JVM descriptor：`(II)Lcom/netumscan/printersdk/PrintJob$Builder;`

<a id="api-15137d52281e9aa47704"></a>

### public com.netumscan.printersdk.PrintJob$Builder receiptFinish(int, int, int)

设置小票结束留白、切纸及作用范围。

```java
public com.netumscan.printersdk.PrintJob$Builder receiptFinish(int, int, int);
```

参数trailingBlankDots、cutMode、可选finishScope；双参数默认EACH_COPY。仅小票，否则IllegalStateException；当前标准后端只支持不切纸和EACH_COPY，其他值在提交前明确拒绝。

文档 ID：`android:member:com.netumscan.printersdk.PrintJob$Builder:1d14bc1ca93c16ef`

JVM descriptor：`(III)Lcom/netumscan/printersdk/PrintJob$Builder;`

<a id="api-67f71941d4a06dcf201a"></a>

### public com.netumscan.printersdk.PrintJob$Builder receiptMargins(int, int)

设置小票左右边距。

```java
public com.netumscan.printersdk.PrintJob$Builder receiptMargins(int, int);
```

leftDots/rightDots单位点，仅小票，否则IllegalStateException；当前ESC/POS后端非零边距在提交前1004拒绝，不静默忽略。

文档 ID：`android:member:com.netumscan.printersdk.PrintJob$Builder:dd98ae8493d76ec7`

JVM descriptor：`(II)Lcom/netumscan/printersdk/PrintJob$Builder;`

<a id="api-ca4e5b856e5cfc2192e3"></a>

### public static final int EACH_COPY = 0

EACH_COPY常量，值0。

```java
public static final int EACH_COPY = 0;
```

每份执行结束留白。 不执行IO，不表示硬件已验证支持。

文档 ID：`android:member:com.netumscan.printersdk.PrintJob$Builder:447559c0d9447997`

JVM descriptor：`I`

<a id="api-aa5a3867e8589cd6210d"></a>

### public static final int END_OF_JOB = 1

END_OF_JOB常量，值1。

```java
public static final int END_OF_JOB = 1;
```

仅整单末尾；当前标准后端不支持。 不执行IO，不表示硬件已验证支持。

文档 ID：`android:member:com.netumscan.printersdk.PrintJob$Builder:0714561c652f45aa`

JVM descriptor：`I`

<a id="api-2a1340ef023ed5fd60c2"></a>

## PrintOperation

独立持有终态结果与订阅的操作。

```java
public final class com.netumscan.printersdk.PrintOperation implements java.lang.AutoCloseable
```

设备销毁后仍能读取结果。close撤销调用方getter/cancel/新增注册权，不取消工作，既有终态订阅仍兑现；注销Subscription只停止对应通知。结果缓存不保留作业像素，不能把DATA_SENT解释为机械完成。

文档 ID：`android:type:com.netumscan.printersdk.PrintOperation`

<a id="api-62712491e8677d5fdeb8"></a>

### public com.netumscan.printersdk.Subscription onComplete(com.netumscan.printersdk.PrintOperation$CompletionListener)

注册一次异步终态结果回调。

```java
public com.netumscan.printersdk.Subscription onComplete(com.netumscan.printersdk.PrintOperation$CompletionListener);
```

listener与显式executor不可null。默认主线程，或指定Executor；已经终结时仍安排一次投递，注册与完成竞态不漏不重复。close不撤回已有通知，Subscription.close抑制未开始的投递。SDK锁外执行，拒绝custom Executor记录日志并向主线程降级一次，客户异常被隔离；close后新增注册1002。

文档 ID：`android:member:com.netumscan.printersdk.PrintOperation:3986e0d8bf2fcaa6`

JVM descriptor：`(Lcom/netumscan/printersdk/PrintOperation$CompletionListener;)Lcom/netumscan/printersdk/Subscription;`

<a id="api-7c0d9a01f2d3032d84bb"></a>

### public com.netumscan.printersdk.Subscription onComplete(java.util.concurrent.Executor, com.netumscan.printersdk.PrintOperation$CompletionListener)

注册一次异步终态结果回调。

```java
public com.netumscan.printersdk.Subscription onComplete(java.util.concurrent.Executor, com.netumscan.printersdk.PrintOperation$CompletionListener);
```

listener与显式executor不可null。默认主线程，或指定Executor；已经终结时仍安排一次投递，注册与完成竞态不漏不重复。close不撤回已有通知，Subscription.close抑制未开始的投递。SDK锁外执行，拒绝custom Executor记录日志并向主线程降级一次，客户异常被隔离；close后新增注册1002。

文档 ID：`android:member:com.netumscan.printersdk.PrintOperation:aa4636797c27b361`

JVM descriptor：`(Ljava/util/concurrent/Executor;Lcom/netumscan/printersdk/PrintOperation$CompletionListener;)Lcom/netumscan/printersdk/Subscription;`

<a id="api-123bb6beee4d939c501a"></a>

### public synchronized com.netumscan.printersdk.Diagnostic diagnostic()

读取终态结构化诊断。

```java
public synchronized com.netumscan.printersdk.Diagnostic diagnostic();
```

复用result().diagnostic，设备关闭后仍可保存；未终结或调用方close后1002，原生码仅用于排障。

文档 ID：`android:member:com.netumscan.printersdk.PrintOperation:8a5a3f98a3a0b3b7`

JVM descriptor：`()Lcom/netumscan/printersdk/Diagnostic;`

<a id="api-3f56d6df1055e3faf387"></a>

### public synchronized com.netumscan.printersdk.PrintOperation$Result result()

读取并缓存完整终态结果。

```java
public synchronized com.netumscan.printersdk.PrintOperation$Result result();
```

非终态或close后抛PrinterException(1002)。终态复制后可释放native结果存储，缓存独立于设备，包含snapshot/diagnostic/可选status，不等待打印机IO。

文档 ID：`android:member:com.netumscan.printersdk.PrintOperation:affc50da4c100e0a`

JVM descriptor：`()Lcom/netumscan/printersdk/PrintOperation$Result;`

<a id="api-0a9b35e0113bf90ba495"></a>

### public synchronized com.netumscan.printersdk.PrintOperation$Snapshot snapshot()

读取过程或缓存终态快照。

```java
public synchronized com.netumscan.printersdk.PrintOperation$Snapshot snapshot();
```

本地读取，无IO且不要求设备存活；精确计数和总量须检查证据位，DATA_SENT不是机械完成。调用方close后抛1002。

文档 ID：`android:member:com.netumscan.printersdk.PrintOperation:b2032eaf77200681`

JVM descriptor：`()Lcom/netumscan/printersdk/PrintOperation$Snapshot;`

<a id="api-f30a5b82e72a708377d9"></a>

### public synchronized com.netumscan.printersdk.PrintOperation$StatusResult statusResult()

读取有效硬件状态结果。

```java
public synchronized com.netumscan.printersdk.PrintOperation$StatusResult statusResult();
```

result().status为null时1002；未终结或close也1002，不能由零mask推断正常。当前标准查询IO前1004拒绝，不返回成功状态operation。

文档 ID：`android:member:com.netumscan.printersdk.PrintOperation:b921dc45bca937dd`

JVM descriptor：`()Lcom/netumscan/printersdk/PrintOperation$StatusResult;`

<a id="api-5ef6d74af622312dd333"></a>

### public synchronized java.lang.String message()

读取终态诊断消息。

```java
public synchronized java.lang.String message();
```

复用result().diagnostic.message，未终结或close后1002。消息有界、可能为空，业务只依赖稳定错误码。

文档 ID：`android:member:com.netumscan.printersdk.PrintOperation:3f10a6887f7a9287`

JVM descriptor：`()Ljava/lang/String;`

<a id="api-e62a83d4b95eb6b8d59b"></a>

### public synchronized void cancel()

显式请求此操作协作取消。

```java
public synchronized void cancel();
```

不关闭共享设备，不能撤回已发送字节；终态后幂等，取消与完成竞争时可已成功。最终结果仍由result/onComplete观察；close后抛1002。

文档 ID：`android:member:com.netumscan.printersdk.PrintOperation:a1228dc331f5087e`

JVM descriptor：`()V`

<a id="api-3deb04efc9c14ab102d1"></a>

### public void close()

关闭调用方的操作访问权。

```java
public void close();
```

幂等，不取消已入队工作，已注册终态订阅仍由observer保活并兑现；之后getter/cancel/新增注册抛1002。只有对应Subscription.close停止通知，终态缓存不保留像素。

文档 ID：`android:member:com.netumscan.printersdk.PrintOperation:71f6e6626cb069d2`

JVM descriptor：`()V`

<a id="api-def001084f33882939be"></a>

## PrintOperation.CompletionListener

接收一次操作终态的监听器。

```java
public interface com.netumscan.printersdk.PrintOperation$CompletionListener
```

由onComplete注册，默认主线程或指定Executor异步调用；锁外执行且不在原生IO worker，客户异常被隔离记录。取消监听不取消打印。

文档 ID：`android:type:com.netumscan.printersdk.PrintOperation$CompletionListener`

<a id="api-583427626328918b15dd"></a>

### public abstract void completed(com.netumscan.printersdk.PrintOperation$Result)

接收这个订阅的一次不可变终态。

```java
public abstract void completed(com.netumscan.printersdk.PrintOperation$Result);
```

result非null；callback开始前检查订阅有效性，已开始不可撤回。可在回调中重入SDK，客户代码不在SDK锁内；成功DATA_SENT不等于物理完成。

文档 ID：`android:member:com.netumscan.printersdk.PrintOperation$CompletionListener:f89d5a782e133a4e`

JVM descriptor：`(Lcom/netumscan/printersdk/PrintOperation$Result;)V`

<a id="api-c0f443447380816989d4"></a>

## PrintOperation.Result

操作的不可变完整终态副本。

```java
public final class com.netumscan.printersdk.PrintOperation$Result
```

包含同一终态snapshot、结构化诊断和可选status，不含native句柄或图像载荷；独立于设备存活，可用于一次性完成回调。

文档 ID：`android:type:com.netumscan.printersdk.PrintOperation$Result`

<a id="api-cd82242cfdb19321a8d7"></a>

### public final com.netumscan.printersdk.Diagnostic diagnostic

完整终态的diagnostic。

```java
public final com.netumscan.printersdk.Diagnostic diagnostic;
```

有界稳定错误与可选native证据；成功code=0，原生码仅诊断。 与设备存活无关，不包含图像或平台句柄。

文档 ID：`android:member:com.netumscan.printersdk.PrintOperation$Result:89955c2737b28a99`

JVM descriptor：`Lcom/netumscan/printersdk/Diagnostic;`

<a id="api-dde04668b123da5c6c29"></a>

### public final com.netumscan.printersdk.PrintOperation$Snapshot snapshot

完整终态的snapshot。

```java
public final com.netumscan.printersdk.PrintOperation$Snapshot snapshot;
```

最终state/error/completion/字节及副作用证据来自同一操作终态。 与设备存活无关，不包含图像或平台句柄。

文档 ID：`android:member:com.netumscan.printersdk.PrintOperation$Result:847afd8733bbd82e`

JVM descriptor：`Lcom/netumscan/printersdk/PrintOperation$Snapshot;`

<a id="api-d92c6fddc9418fe4e4a1"></a>

### public final com.netumscan.printersdk.PrintOperation$StatusResult status

完整终态的status。

```java
public final com.netumscan.printersdk.PrintOperation$StatusResult status;
```

仅snapshot.responseValid时非null，否则没有有效硬件响应；knownConditions决定哪些位可解释。 与设备存活无关，不包含图像或平台句柄。

文档 ID：`android:member:com.netumscan.printersdk.PrintOperation$Result:7b53f4b174880aa4`

JVM descriptor：`Lcom/netumscan/printersdk/PrintOperation$StatusResult;`

<a id="api-28695f8f756b652c83c0"></a>

## PrintOperation.Snapshot

一个操作在读取时刻的不可变结果快照。

```java
public final class com.netumscan.printersdk.PrintOperation$Snapshot
```

由 PrintOperation.snapshot() 返回，无公共构造器。所有字段为 final，但不保证调用方观察到每次中间状态；需要新进度时重新读取。状态、完成级别、错误与副作用须共同判断；字节计数依赖标志，未知总数不得解释为零。当前标准后端不提供状态响应或 DEVICE_CONFIRMED 纸面证明。

文档 ID：`android:type:com.netumscan.printersdk.PrintOperation$Snapshot`

<a id="api-ab5d7e3762f966c8206b"></a>

### public boolean bytesSentExact()

判断 bytesSent 是否为精确发送计数。

```java
public boolean bytesSentExact();
```

无参数，返回 `(byteCountFlags & 1) != 0`。false 时 bytesSent 仅能作为保守下界，活动写入或平台错误可能接受了未能精确报告的字节；不得据差额补发。精确传输计数也不证明实际打印完成。

文档 ID：`android:member:com.netumscan.printersdk.PrintOperation$Snapshot:588d307e45c509b3`

JVM descriptor：`()Z`

<a id="api-bbb10ead1924f09f96bd"></a>

### public boolean bytesTotalKnown()

判断 bytesTotal 是否具有确定的总量语义。

```java
public boolean bytesTotalKnown();
```

无参数，返回 `(byteCountFlags & 2) != 0`。false 时总量未知，bytesTotal=0 不能解释为零字节作业；true 时才可展示确定总数。当前流式打印通常到成功终态且发送量精确后才得到已知总量。

文档 ID：`android:member:com.netumscan.printersdk.PrintOperation$Snapshot:57b2090675f9a61f`

JVM descriptor：`()Z`

<a id="api-2fcd525774ba067953d5"></a>

### public boolean terminal()

判断该快照是否已进入终态。

```java
public boolean terminal();
```

无参数，返回 `state >= 4`；当前终态为 COMPLETED=4、FAILED=5、CANCELLED=6。true 不等于成功，必须同时检查 state/error/completion；未终态为 QUEUED=0、PREPARING=1、SENDING=2。仅检查快照，不读取新状态或等待终止。

文档 ID：`android:member:com.netumscan.printersdk.PrintOperation$Snapshot:f3da21f08233867e`

JVM descriptor：`()Z`

<a id="api-c384643903b58e491a99"></a>

### public final boolean responseValid

操作是否具有可用的设备响应结果。

```java
public final boolean responseValid;
```

当前标准后端状态查询未实现，现有打印/Raw/介质/定位操作通常为 false。false 不影响 DATA_SENT 的发送语义；不得把 true 或字段存在自行解释为所有纸张状态已知，公共 Java 当前不提供状态结果载荷读取接口。

文档 ID：`android:member:com.netumscan.printersdk.PrintOperation$Snapshot:55f5e2e0e93ecf48`

JVM descriptor：`Z`

<a id="api-b859f072ff4d4ae6a93a"></a>

### public final boolean sideEffectsPossible

该操作是否存在已产生外部输出的风险。

```java
public final boolean sideEffectsPossible;
```

true 表示可能已有写入或其他外部副作用，失败/取消也可能已有部分纸面输出。该标志不是打印完成确认，不应据此自动重放；需要结合状态、错误、字节计数和业务验收决定下一步。

文档 ID：`android:member:com.netumscan.printersdk.PrintOperation$Snapshot:09b6d93c4ce76763`

JVM descriptor：`Z`

<a id="api-a0e0b61cf248901cc736"></a>

### public final int byteCountFlags

发送计数的精确性及总量已知位掩码。

```java
public final int byteCountFlags;
```

位 1 为 BYTES_SENT_EXACT，位 2 为 BYTES_TOTAL_KNOWN，可组合。优先通过 bytesSentExact()/bytesTotalKnown() 读取；无标志时保守处理，不能把计数直接当纸面进度或确定零。

文档 ID：`android:member:com.netumscan.printersdk.PrintOperation$Snapshot:aa7a5ce72b4732d1`

JVM descriptor：`I`

<a id="api-638041ea518efd4b7543"></a>

### public final int completion

操作已达到的完成确认级别。

```java
public final int completion;
```

0 为 NONE，1 为 DATA_SENT，2 为 DEVICE_CONFIRMED。当前标准后端成功仅为 DATA_SENT；不能因为 state=COMPLETED 或此值为 1 推断纸面完成，值 2 的定义不表示当前支持设备确认。

文档 ID：`android:member:com.netumscan.printersdk.PrintOperation$Snapshot:8241840aff5f60d2`

JVM descriptor：`I`

<a id="api-7f2542cc57c5d75c071c"></a>

### public final int error

操作状态对应的稳定错误码。

```java
public final int error;
```

成功为 0；失败/取消可由 PrinterException.description(error) 获取诊断短语。1005 表示取消，2001 超时，2002 连接丢失等。以 state 区分未结束与终态，error=0 的运行中快照不代表成功。

文档 ID：`android:member:com.netumscan.printersdk.PrintOperation$Snapshot:0f938395cfdf79ab`

JVM descriptor：`I`

<a id="api-6adb36cc55b3e4280be0"></a>

### public final int kind

操作类别整数。

```java
public final int kind;
```

当前值 PRINT=1、STATUS=2、MEDIA=3、RAW=4、FEED_TO_NEXT_LABEL=5。类别描述已接纳的操作种类，不自动表示该功能有设备支持证据；当前 STATUS 请求在接纳前被拒绝。

文档 ID：`android:member:com.netumscan.printersdk.PrintOperation$Snapshot:ce09769247087c6a`

JVM descriptor：`I`

<a id="api-bd23423dc3a2aacfa455"></a>

### public final int mediaBasis

操作介质要求判定采用的依据整数。

```java
public final int mediaBasis;
```

0 为 NONE，1 为 LAST_SENT，2 为 READBACK。LAST_SENT 只代表依据此前成功发送的设置，不证明设备实际介质；当前公共 Java 整图入口不附加介质匹配要求，通常为 NONE，不能据此宣称已检测纸张。

文档 ID：`android:member:com.netumscan.printersdk.PrintOperation$Snapshot:557bf522a9e3c685`

JVM descriptor：`I`

<a id="api-24b0461ee72ffa91b495"></a>

### public final int state

读取时的操作执行状态整数。

```java
public final int state;
```

当前值 QUEUED=0、PREPARING=1、SENDING=2、COMPLETED=4、FAILED=5、CANCELLED=6。4/5/6 为终态，使用 terminal() 判断；读取可能跳过中间状态，COMPLETED 仅达到当前传输完成要求。

文档 ID：`android:member:com.netumscan.printersdk.PrintOperation$Snapshot:47d05db295c29a6c`

JVM descriptor：`I`

<a id="api-8f7ba4964ae80a257982"></a>

### public final long bytesSent

已经观察到的发送字节数或保守下界。

```java
public final long bytesSent;
```

单位 bytes，包含该操作逐份输出的传输计数。仅 bytesSentExact() 为 true 时是精确值；否则可能有平台已接受但未准确报告的字节，不可依据此值自动补发或估算已打印份数。

文档 ID：`android:member:com.netumscan.printersdk.PrintOperation$Snapshot:100d9a5074a00a14`

JVM descriptor：`J`

<a id="api-fe8b71ea187ab7347af5"></a>

### public final long bytesTotal

总发送字节数的候选字段，单位为 bytes。

```java
public final long bytesTotal;
```

必须先检查 bytesTotalKnown()。未设置已知标志时即使字段为 0 也只是未知；流式编码不预先保证确定总字节数。当前成功且精确计数的终态将总量设置为 bytesSent。

文档 ID：`android:member:com.netumscan.printersdk.PrintOperation$Snapshot:b3926476a06ff84e`

JVM descriptor：`J`

<a id="api-5d7551b4b3ab0fd62e2d"></a>

### public final long id

SDK 为该操作生成的原生操作 ID。

```java
public final long id;
```

来自操作接纳结果，供关联当前 operation 的诊断和快照；与 MobilePrinter 便捷 task ID 的命名空间不同。不要把它传给 MobilePrinter.cancel(long)，独立操作应使用所属 PrintOperation.cancel()。

文档 ID：`android:member:com.netumscan.printersdk.PrintOperation$Snapshot:26be605f3fdc9e9c`

JVM descriptor：`J`

<a id="api-e6306611fa6971c0f618"></a>

## PrintOperation.StatusResult

成功硬件状态查询的不可变结果。

```java
public final class com.netumscan.printersdk.PrintOperation$StatusResult
```

仅成功且 responseValid 的状态 operation 可读取；knownConditions 表示已知位，activeConditions 仅在已知位内有意义。未知位不能推断正常；连接代次和采样时间用于判断结果是否过期。当前标准后端不提供硬件结果。

文档 ID：`android:type:com.netumscan.printersdk.PrintOperation$StatusResult`

<a id="api-8c55b681a0d22f70a0c2"></a>

### public final int activeConditions

已知条件中当前激活的状态位。

```java
public final int activeConditions;
```

只在 knownConditions 内有效；不能从零值推出所有硬件状态正常。

文档 ID：`android:member:com.netumscan.printersdk.PrintOperation$StatusResult:7399537314a382a4`

JVM descriptor：`I`

<a id="api-0faf80983e415732e5cb"></a>

### public final int evidence

硬件状态结果的证据来源。

```java
public final int evidence;
```

由原生结果复制；不将 SDK 排队或 DATA_SENT 当作硬件状态依据。

文档 ID：`android:member:com.netumscan.printersdk.PrintOperation$StatusResult:958eb37813714ec1`

JVM descriptor：`I`

<a id="api-530e7866362f60023c57"></a>

### public final int knownConditions

本次响应明确报告的状态位掩码。

```java
public final int knownConditions;
```

只对置位条件解释 activeConditions；未置位条件保持未知。

文档 ID：`android:member:com.netumscan.printersdk.PrintOperation$StatusResult:ca1edcaa356737e1`

JVM descriptor：`I`

<a id="api-7224805d9a6c76ce3fa3"></a>

### public final long connectionGeneration

结果所属的成功连接代次。

```java
public final long connectionGeneration;
```

断开或重连后旧结果不能作为新连接的当前硬件状态。

文档 ID：`android:member:com.netumscan.printersdk.PrintOperation$StatusResult:70bf86ec3b40d69c`

JVM descriptor：`J`

<a id="api-d3976cea18902c7928c8"></a>

### public final long sampledMonotonicMs

状态采样的进程单调时间，单位毫秒。

```java
public final long sampledMonotonicMs;
```

用于同进程的相对陈旧度判断，不是墙上时间。

文档 ID：`android:member:com.netumscan.printersdk.PrintOperation$StatusResult:0b0a9a42fa4de096`

JVM descriptor：`J`

<a id="api-76246b3529d521ba6c7e"></a>

### public static final int COVER_OPEN = 2

开盖状态位。

```java
public static final int COVER_OPEN = 2;
```

固定公共状态位；仅当 knownConditions 对应位已知时才解释 activeConditions。

文档 ID：`android:member:com.netumscan.printersdk.PrintOperation$StatusResult:63e53ba840019830`

JVM descriptor：`I`

<a id="api-bcc92a67663e528208a2"></a>

### public static final int CUTTER_ERROR = 8

切刀故障状态位。

```java
public static final int CUTTER_ERROR = 8;
```

固定公共状态位；仅当 knownConditions 对应位已知时才解释 activeConditions。

文档 ID：`android:member:com.netumscan.printersdk.PrintOperation$StatusResult:637b2910ef015ada`

JVM descriptor：`I`

<a id="api-5c06e14ecda12c1b523e"></a>

### public static final int PAPER_NEAR_END = 4

纸将尽状态位。

```java
public static final int PAPER_NEAR_END = 4;
```

固定公共状态位；仅当 knownConditions 对应位已知时才解释 activeConditions。

文档 ID：`android:member:com.netumscan.printersdk.PrintOperation$StatusResult:bf16c2c3cb59609d`

JVM descriptor：`I`

<a id="api-7c91218e79b4e7d0c73a"></a>

### public static final int PAPER_OUT = 1

缺纸状态位。

```java
public static final int PAPER_OUT = 1;
```

固定公共状态位；仅当 knownConditions 对应位已知时才解释 activeConditions。

文档 ID：`android:member:com.netumscan.printersdk.PrintOperation$StatusResult:3f80da847c593bc9`

JVM descriptor：`I`

<a id="api-a36171733f6c01585161"></a>

## PrinterCapabilities

聚合本地能力、约束与几何观察。

```java
public final class com.netumscan.printersdk.PrinterCapabilities
```

八项feature、四项constraint和geometry分别保留known/deviceSupport/evidence。不执行IO、不预留接纳额度；纯连接设备几何未知，不能由最近Profile或零数值推断硬件支持。

文档 ID：`android:type:com.netumscan.printersdk.PrinterCapabilities`

<a id="api-99761482a4773c008c52"></a>

### public com.netumscan.printersdk.FeatureSupport feature(int)

按ID取一项功能能力。

```java
public com.netumscan.printersdk.FeatureSupport feature(int);
```

ID1..8对应MobilePrinter功能常量；其他ID返回unknown/未实现值，不执行IO。实现、deviceSupport与evidence独立，检查knownFields后才能解释集合mask。

文档 ID：`android:member:com.netumscan.printersdk.PrinterCapabilities:02554ca7e27ea317`

JVM descriptor：`(I)Lcom/netumscan/printersdk/FeatureSupport;`

<a id="api-e4cdb110786ced1f5c22"></a>

### public com.netumscan.printersdk.ScalarConstraint constraint(int)

按ID取一项范围约束。

```java
public com.netumscan.printersdk.ScalarConstraint constraint(int);
```

ID1..4对应PrinterConstraint参数；其他ID返回known=false，不执行IO。copies及源载荷额度是SDK策略，未知介质范围不是无限。

文档 ID：`android:member:com.netumscan.printersdk.PrinterCapabilities:a258c0e7ea03dc6f`

JVM descriptor：`(I)Lcom/netumscan/printersdk/ScalarConstraint;`

<a id="api-1144e63ac4d0fc5c6a33"></a>

### public final int dpiX

打印几何dpiX观察值。

```java
public final int dpiX;
```

横向DPI，须geometryPresentFields的DPI位1有效。 当前纯通信设备无几何证据，必须检查存在位，不能从数值零推断正常或支持。

文档 ID：`android:member:com.netumscan.printersdk.PrinterCapabilities:5253b3056074e745`

JVM descriptor：`I`

<a id="api-442b043e5d7a36e6f759"></a>

### public final int dpiY

打印几何dpiY观察值。

```java
public final int dpiY;
```

纵向DPI，须DPI位1有效。 当前纯通信设备无几何证据，必须检查存在位，不能从数值零推断正常或支持。

文档 ID：`android:member:com.netumscan.printersdk.PrinterCapabilities:414f3289a65cc726`

JVM descriptor：`I`

<a id="api-b2acb3e6aacd017bcd27"></a>

### public final int geometryEvidence

打印几何geometryEvidence观察值。

```java
public final int geometryEvidence;
```

PROFILE_DECLARED=1、DEVICE_REPORTED=2、HARDWARE_VERIFIED=4；零表示无证据。 当前纯通信设备无几何证据，必须检查存在位，不能从数值零推断正常或支持。

文档 ID：`android:member:com.netumscan.printersdk.PrinterCapabilities:913c74adc3bf7c03`

JVM descriptor：`I`

<a id="api-6ef929d87e42388914f9"></a>

### public final int geometryPresentFields

打印几何geometryPresentFields观察值。

```java
public final int geometryPresentFields;
```

DPI=1、WIDTH=2、LENGTH=4、CUT_BLANK=8；未知字段数值零不能解释为已知零或无限。 当前纯通信设备无几何证据，必须检查存在位，不能从数值零推断正常或支持。

文档 ID：`android:member:com.netumscan.printersdk.PrinterCapabilities:cf4ac59529e54952`

JVM descriptor：`I`

<a id="api-cf25b3fad6e01659f4c4"></a>

### public final int maxPageLengthDots

打印几何maxPageLengthDots观察值。

```java
public final int maxPageLengthDots;
```

一页/一份最大长度（点），须LENGTH位4有效，不乘copies，不是小票整单预算。 当前纯通信设备无几何证据，必须检查存在位，不能从数值零推断正常或支持。

文档 ID：`android:member:com.netumscan.printersdk.PrinterCapabilities:d4fa201acc001d96`

JVM descriptor：`I`

<a id="api-0bc443d11dcb982b726a"></a>

### public final int minCutBlankDots

打印几何minCutBlankDots观察值。

```java
public final int minCutBlankDots;
```

切纸前最小留白（点），须CUT_BLANK位8有效。 当前纯通信设备无几何证据，必须检查存在位，不能从数值零推断正常或支持。

文档 ID：`android:member:com.netumscan.printersdk.PrinterCapabilities:fd704893c25f2b1c`

JVM descriptor：`I`

<a id="api-5cd56ea0b31eb4942545"></a>

### public final int printableWidthDots

打印几何printableWidthDots观察值。

```java
public final int printableWidthDots;
```

可打印宽度（点），须WIDTH位2有效。 当前纯通信设备无几何证据，必须检查存在位，不能从数值零推断正常或支持。

文档 ID：`android:member:com.netumscan.printersdk.PrinterCapabilities:338e757da58637fb`

JVM descriptor：`I`

<a id="api-21f2c4252d1bd090ee0b"></a>

## PrinterException

携带稳定 SDK 错误码的运行时异常。

```java
public final class com.netumscan.printersdk.PrinterException extends java.lang.RuntimeException
```

继承 RuntimeException，无需 Java checked-exception 声明。同步创建/接纳/生命周期错误通过该异常报告；已接纳操作的异步失败通常通过 Snapshot.error/Result.snapshot.error 报告，不会自动在提交线程抛出。业务分支使用 code，description/message 仅供诊断。蓝牙权限及平台状态问题可能归并为 1002；已发送内容不可因异常自动重试。

文档 ID：`android:type:com.netumscan.printersdk.PrinterException`

<a id="api-c108013031016285f34c"></a>

### public com.netumscan.printersdk.PrinterException(com.netumscan.printersdk.Diagnostic)

由原生诊断创建 SDK 异常。

```java
public com.netumscan.printersdk.PrinterException(com.netumscan.printersdk.Diagnostic);
```

diagnostic 必须非 null；code 取其 errorCode，异常消息使用稳定描述和经筛选的有界说明，不附加任意平台文本。旧 int 构造器仍保留。

文档 ID：`android:member:com.netumscan.printersdk.PrinterException:0ad0377b200f0092`

JVM descriptor：`(Lcom/netumscan/printersdk/Diagnostic;)V`

<a id="api-1e6a1358f647d8248abe"></a>

### public com.netumscan.printersdk.PrinterException(int)

用数值错误码创建诊断异常。

```java
public com.netumscan.printersdk.PrinterException(int);
```

参数 `code` 为 int 错误码，不验证其是否已知；保存到只读 code 字段。异常 message 为 `description(code) + " (" + code + ")"`。同步创建，不执行 IO、取消或恢复；手动构造异常不会改变设备状态。

文档 ID：`android:member:com.netumscan.printersdk.PrinterException:81d76c07932c70b6`

JVM descriptor：`(I)V`

<a id="api-f67e21e163fcf1beeb28"></a>

### public final int code

此异常对应的稳定数值错误码。

```java
public final int code;
```

SDK 采用 1000..1008 的通用错误、2000..2004 的传输错误及 3000 协议响应错误；未知值须保留原数值，避免只靠消息文字分支。code 1006 为 ABI 不匹配，需更换完整匹配 SDK；1008 为资源接纳限制，不表示设备不支持功能。

文档 ID：`android:member:com.netumscan.printersdk.PrinterException:c87585a61ac30812`

JVM descriptor：`I`

<a id="api-5231a4649d520a09741f"></a>

### public final transient com.netumscan.printersdk.Diagnostic diagnostic

可选的原生有界诊断。

```java
public final transient com.netumscan.printersdk.Diagnostic diagnostic;
```

旧 code 构造器为 null；诊断构造器保留关联记录。字段为 transient，异常序列化不会复制诊断上下文。

文档 ID：`android:member:com.netumscan.printersdk.PrinterException:8fed9880dcf7865f`

JVM descriptor：`Lcom/netumscan/printersdk/Diagnostic;`

<a id="api-9845f15fdceaec00b85e"></a>

### public static java.lang.String description(int)

返回数值错误码的英文诊断短语。

```java
public static java.lang.String description(int);
```

参数 `code` 为任意 int，返回非 null String，不执行 IO。当前映射：1000 Internal error；1001 Invalid argument；1002 Invalid state（检查蓝牙、权限和生命周期）；1003 SDK not initialized；1004 Unsupported operation or Bluetooth characteristic；1005 Cancelled（已发送数据不可撤回）；1006 SDK ABI mismatch（替换完整 SDK）；1007 Output buffer too small；1008 SDK resource limit（释放/轮询结果）；2000 Transport not open；2001 Transport timeout；2002 Connection lost（显式重连、无自动重放）；2003 Transport IO error；2004 Transport made no progress；3000 Invalid protocol response。其他值返回 `Unknown printer SDK error`。短语只用于诊断，不是稳定的业务判断键；成功 code=0 不应通过此方法推导成功文本。 1008 提示缩减输入或未终态工作并检查诊断；已终态操作自动退出接纳预算，轮询或释放其结果不能修复输入超限，错误码也可能表示分配失败。

文档 ID：`android:member:com.netumscan.printersdk.PrinterException:ba359d8df979350d`

JVM descriptor：`(I)Ljava/lang/String;`

<a id="api-5a783d70e33472c7d48b"></a>

## PrinterImage

一次复制并可共享的不可变源图像。

```java
public final class com.netumscan.printersdk.PrinterImage implements java.lang.AutoCloseable
```

create校验并复制紧密排列像素和图像选项，每图最多16MiB。成功后源byte[]可释放；job.build只retain native存储，无像素二次复制。应显式close公共image句柄；已构建job不受影响。

文档 ID：`android:type:com.netumscan.printersdk.PrinterImage`

<a id="api-941dd3d9e4aee422944f"></a>

### public final int height

源图像height（像素）。

```java
public final int height;
```

初始化后不可变且为正；拟合后的打印尺寸由图像选项和作业区域决定，不能推断纸面完成。

文档 ID：`android:member:com.netumscan.printersdk.PrinterImage:1e8b3775e799a9fa`

JVM descriptor：`I`

<a id="api-b330b147d9f6ec224e51"></a>

### public final int width

源图像width（像素）。

```java
public final int width;
```

初始化后不可变且为正；拟合后的打印尺寸由图像选项和作业区域决定，不能推断纸面完成。

文档 ID：`android:member:com.netumscan.printersdk.PrinterImage:5727cdfd5221243c`

JVM descriptor：`I`

<a id="api-a20fc3b280f0a73ffefd"></a>

### public static com.netumscan.printersdk.PrinterImage create(byte[], int, int, int, int, int, boolean, int)

校验并复制像素与图像选项。

```java
public static com.netumscan.printersdk.PrinterImage create(byte[], int, int, int, int, int, boolean, int);
```

参数依次为pixels、width、height、pixelFormat、algorithm、threshold、invert、targetWidthDots。宽高为正，数据长度恰为w*h*bpp，每图最多16MiB，threshold0..255。targetWidth=0保留源尺寸，正值开启fit-width；格式/算法必须是已声明常量。失败1001/1004/1008，无IO；成功后源数组可释放。

文档 ID：`android:member:com.netumscan.printersdk.PrinterImage:2c3a23b3363c24cf`

JVM descriptor：`([BIIIIIZI)Lcom/netumscan/printersdk/PrinterImage;`

<a id="api-3435d02c1e6370e38723"></a>

### public static com.netumscan.printersdk.PrinterImage gray8(byte[], int, int)

用阈值128复制一幅Gray8图像。

```java
public static com.netumscan.printersdk.PrinterImage gray8(byte[], int, int);
```

pixels非null且紧密排列，长度恰为正数width*height；每图16MiB上限。等价GRAY8/THRESHOLD/128、不反色、targetWidth=0，不执行打印机IO；成功后源byte[]可释放。

文档 ID：`android:member:com.netumscan.printersdk.PrinterImage:3e3bc6c74b96c988`

JVM descriptor：`([BII)Lcom/netumscan/printersdk/PrinterImage;`

<a id="api-8bfea55b36ced987736f"></a>

### public static final int BAYER_4X4 = 2

BAYER_4X4常量，值2。

```java
public static final int BAYER_4X4 = 2;
```

4×4有序抖动。 不执行IO，不表示硬件已验证支持。

文档 ID：`android:member:com.netumscan.printersdk.PrinterImage:0dcb27f1cd32e35c`

JVM descriptor：`I`

<a id="api-2018f450c669a66b08d8"></a>

### public static final int BAYER_8X8 = 3

BAYER_8X8常量，值3。

```java
public static final int BAYER_8X8 = 3;
```

8×8有序抖动。 不执行IO，不表示硬件已验证支持。

文档 ID：`android:member:com.netumscan.printersdk.PrinterImage:122522976173ccef`

JVM descriptor：`I`

<a id="api-e7a3566affba90e5c7a3"></a>

### public static final int FLOYD_STEINBERG = 1

FLOYD_STEINBERG常量，值1。

```java
public static final int FLOYD_STEINBERG = 1;
```

误差扩散。 不执行IO，不表示硬件已验证支持。

文档 ID：`android:member:com.netumscan.printersdk.PrinterImage:17319debb6248132`

JVM descriptor：`I`

<a id="api-e4b3f62c3e7e34be20be"></a>

### public static final int GRAY8 = 1

GRAY8常量，值1。

```java
public static final int GRAY8 = 1;
```

紧密排列1字节灰度。 不执行IO，不表示硬件已验证支持。

文档 ID：`android:member:com.netumscan.printersdk.PrinterImage:4909d4185765b94a`

JVM descriptor：`I`

<a id="api-7e001b3f49a1dabee614"></a>

### public static final int RGB24 = 2

RGB24常量，值2。

```java
public static final int RGB24 = 2;
```

紧密排列3字节RGB。 不执行IO，不表示硬件已验证支持。

文档 ID：`android:member:com.netumscan.printersdk.PrinterImage:72c70a2f4730535e`

JVM descriptor：`I`

<a id="api-cc3909b61b5da5146687"></a>

### public static final int RGBA32 = 3

RGBA32常量，值3。

```java
public static final int RGBA32 = 3;
```

紧密排列4字节RGBA。 不执行IO，不表示硬件已验证支持。

文档 ID：`android:member:com.netumscan.printersdk.PrinterImage:5ff7bf192f2c5b09`

JVM descriptor：`I`

<a id="api-b5b1a6fd3f1c4339ab75"></a>

### public static final int THRESHOLD = 0

THRESHOLD常量，值0。

```java
public static final int THRESHOLD = 0;
```

阈值二值化。 不执行IO，不表示硬件已验证支持。

文档 ID：`android:member:com.netumscan.printersdk.PrinterImage:885594684c18a57d`

JVM descriptor：`I`

<a id="api-85aa3c2df091e3c7d209"></a>

### public void close()

释放调用方图像句柄。

```java
public void close();
```

幂等，并与build中的图像借用同步。已构建job已retain存储，图像close不影响它们；仍未build的builder再使用此关闭图像会1002失败。

文档 ID：`android:member:com.netumscan.printersdk.PrinterImage:71f6e6626cb069d2`

JVM descriptor：`()V`

<a id="api-bfc786be5b911ef74d19"></a>

## PrinterManager

拥有并复用蓝牙设备的可选管理器，支持共享实例和普通实例。

```java
public final class com.netumscan.printersdk.PrinterManager
```

可使用 getInstance() 获取进程共享实例，也可 new PrinterManager() 创建注册表独立的普通实例。同一管理器按传输类型和规范化蓝牙地址复用设备，BLE 还要求全部连接选项一致；不同管理器不共享设备，应用负责避免重复占用同一硬件。返回的 MobilePrinter 由管理器拥有，调用者借用其打印接口并自行持有 PrintOperation，不直接关闭借用设备；管理器所有者显式 release 或 close。设备工厂不进行打印机 IO，不要求打印 Profile，打印协议和参数仍逐次提交。connect/release/close 把阻塞生命周期工作调度到后台，关闭控制不排在阻塞连接之后；Future 不接受 cancel，get/带超时 get 会阻塞，不应在 UI 线程等待。管理器不新增打印队列、协议状态、权限申请、自动发现、重连或重放。close 后实例永久停止接纳，共享实例也不会自动重建。本类型不实现 AutoCloseable，close 返回可等待的回收 Future。

文档 ID：`android:type:com.netumscan.printersdk.PrinterManager`

<a id="api-ec328bdbe21bce40497b"></a>

### public com.netumscan.printersdk.MobilePrinter ble(android.content.Context, java.lang.String, com.netumscan.printersdk.BleOptions)

取得同一管理器内可复用的 BLE 通信设备。

```java
public com.netumscan.printersdk.MobilePrinter ble(android.content.Context, java.lang.String, com.netumscan.printersdk.BleOptions);
```

context、address 和 options 不可为 null；context 必须具有有效 application context，地址按 SPP 工厂相同规则校验和规范化。相同 BLE 端点仅在 service、writeCharacteristic、withResponse、requestedMtu、maxWriteBytes 及可选信用流控 dataOut/dataFC 全部一致时复用同一 MobilePrinter；不同 BleOptions 对象只要字段相等也可复用。相同端点选项冲突在 IO 前同步抛 1001，改变选项须先完成 release。管理器关闭或端点正在回收为 1002，其他创建错误沿用 MobilePrinter。新设备尚未连接；工厂无打印机 IO，不申请权限、不扫描、不协商 MTU，不要求打印 Profile。管理器仅保留 application context。设备由管理器拥有，借用方使用现有打印 API，由管理器所有者负责 release/close。

文档 ID：`android:member:com.netumscan.printersdk.PrinterManager:058fd7f261ce9c3a`

JVM descriptor：`(Landroid/content/Context;Ljava/lang/String;Lcom/netumscan/printersdk/BleOptions;)Lcom/netumscan/printersdk/MobilePrinter;`

<a id="api-3b2971368ec64f8f40cf"></a>

### public com.netumscan.printersdk.MobilePrinter spp(android.content.Context, java.lang.String)

取得同一管理器内可复用的 SPP 通信设备。

```java
public com.netumscan.printersdk.MobilePrinter spp(android.content.Context, java.lang.String);
```

context 必须非 null 且具有有效 application context；address 为六段冒号分隔的蓝牙 MAC 地址。地址先去除两端空白并按 Locale.ROOT 转为大写，同一规范化地址和 SPP 传输类型复用同一个 MobilePrinter；SPP 与 BLE 注册表键不同。新设备尚未连接，已有设备按原连接状态返回。工厂不执行打印机 IO、不申请权限、不配对或扫描，也无需打印 Profile。参数无效同步抛 PrinterException 1001，管理器已关闭或端点正在回收为 1002；无适配器、ABI 或资源错误沿用 MobilePrinter 的创建错误。返回值由管理器拥有，调用者借用；使用 connect 异步连接，并由管理器所有者 release/close，不直接关闭设备。

文档 ID：`android:member:com.netumscan.printersdk.PrinterManager:b7f533f714ca5d5e`

JVM descriptor：`(Landroid/content/Context;Ljava/lang/String;)Lcom/netumscan/printersdk/MobilePrinter;`

<a id="api-5fe0718e836fd0a4f976"></a>

### public com.netumscan.printersdk.PrinterManager()

创建一个具有独立设备注册表的管理器。

```java
public com.netumscan.printersdk.PrinterManager();
```

无参数，构造不创建蓝牙设备、不打开连接、不执行打印机 IO，也不要求 Profile。新实例不与 getInstance() 或其他普通实例复用设备；对同一硬件创建多个管理器的协调由应用负责。管理器所有者负责最终调用 close() 并在工作线程等待其 Future，以确认全部设备清理完成。Android 设备工厂仅保存 application context，不保留调用方 Activity。

文档 ID：`android:member:com.netumscan.printersdk.PrinterManager:54c074bc78a7bb32`

JVM descriptor：`()V`

<a id="api-005a504ad1356a55b57f"></a>

### public com.netumscan.printersdk.Subscription close(com.netumscan.printersdk.PrinterManager$LifecycleListener)

以回调观察Manager的close完成。

```java
public com.netumscan.printersdk.Subscription close(com.netumscan.printersdk.PrinterManager$LifecycleListener);
```

参数验证先于生命周期动作；listener/显式executor不可null。等价对应Future入口，默认主线程或指定Executor异步回调，failure=null成功。已完成后注册仍一次投递；Subscription.close只停止通知，不中断连接或撤销关闭。关闭会drain/copy已注册operation终态，但不等待客户callback执行。拒绝Executor记录并主线程降级一次，异常隔离。

文档 ID：`android:member:com.netumscan.printersdk.PrinterManager:9b8164f81798fd26`

JVM descriptor：`(Lcom/netumscan/printersdk/PrinterManager$LifecycleListener;)Lcom/netumscan/printersdk/Subscription;`

<a id="api-2d824d7b0ae162cb1294"></a>

### public com.netumscan.printersdk.Subscription close(java.util.concurrent.Executor, com.netumscan.printersdk.PrinterManager$LifecycleListener)

以回调观察Manager的close完成。

```java
public com.netumscan.printersdk.Subscription close(java.util.concurrent.Executor, com.netumscan.printersdk.PrinterManager$LifecycleListener);
```

参数验证先于生命周期动作；listener/显式executor不可null。等价对应Future入口，默认主线程或指定Executor异步回调，failure=null成功。已完成后注册仍一次投递；Subscription.close只停止通知，不中断连接或撤销关闭。关闭会drain/copy已注册operation终态，但不等待客户callback执行。拒绝Executor记录并主线程降级一次，异常隔离。

文档 ID：`android:member:com.netumscan.printersdk.PrinterManager:dc0da8db2c7d1052`

JVM descriptor：`(Ljava/util/concurrent/Executor;Lcom/netumscan/printersdk/PrinterManager$LifecycleListener;)Lcom/netumscan/printersdk/Subscription;`

<a id="api-2d202ef9caaa7a15123a"></a>

### public com.netumscan.printersdk.Subscription connect(com.netumscan.printersdk.MobilePrinter, int, com.netumscan.printersdk.PrinterManager$LifecycleListener)

以回调观察Manager的connect完成。

```java
public com.netumscan.printersdk.Subscription connect(com.netumscan.printersdk.MobilePrinter, int, com.netumscan.printersdk.PrinterManager$LifecycleListener);
```

参数验证先于生命周期动作；listener/显式executor不可null。等价对应Future入口，默认主线程或指定Executor异步回调，failure=null成功。已完成后注册仍一次投递；Subscription.close只停止通知，不中断连接或撤销关闭。关闭会drain/copy已注册operation终态，但不等待客户callback执行。拒绝Executor记录并主线程降级一次，异常隔离。

文档 ID：`android:member:com.netumscan.printersdk.PrinterManager:e0a75afb3d625c87`

JVM descriptor：`(Lcom/netumscan/printersdk/MobilePrinter;ILcom/netumscan/printersdk/PrinterManager$LifecycleListener;)Lcom/netumscan/printersdk/Subscription;`

<a id="api-970609ecd09096e60dd7"></a>

### public com.netumscan.printersdk.Subscription connect(com.netumscan.printersdk.MobilePrinter, int, java.util.concurrent.Executor, com.netumscan.printersdk.PrinterManager$LifecycleListener)

以回调观察Manager的connect完成。

```java
public com.netumscan.printersdk.Subscription connect(com.netumscan.printersdk.MobilePrinter, int, java.util.concurrent.Executor, com.netumscan.printersdk.PrinterManager$LifecycleListener);
```

参数验证先于生命周期动作；listener/显式executor不可null。等价对应Future入口，默认主线程或指定Executor异步回调，failure=null成功。已完成后注册仍一次投递；Subscription.close只停止通知，不中断连接或撤销关闭。关闭会drain/copy已注册operation终态，但不等待客户callback执行。拒绝Executor记录并主线程降级一次，异常隔离。

文档 ID：`android:member:com.netumscan.printersdk.PrinterManager:14e7db73a17af2f2`

JVM descriptor：`(Lcom/netumscan/printersdk/MobilePrinter;ILjava/util/concurrent/Executor;Lcom/netumscan/printersdk/PrinterManager$LifecycleListener;)Lcom/netumscan/printersdk/Subscription;`

<a id="api-92b80757d4c2e78aecbd"></a>

### public com.netumscan.printersdk.Subscription release(com.netumscan.printersdk.MobilePrinter, com.netumscan.printersdk.PrinterManager$LifecycleListener)

以回调观察Manager的release完成。

```java
public com.netumscan.printersdk.Subscription release(com.netumscan.printersdk.MobilePrinter, com.netumscan.printersdk.PrinterManager$LifecycleListener);
```

参数验证先于生命周期动作；listener/显式executor不可null。等价对应Future入口，默认主线程或指定Executor异步回调，failure=null成功。已完成后注册仍一次投递；Subscription.close只停止通知，不中断连接或撤销关闭。关闭会drain/copy已注册operation终态，但不等待客户callback执行。拒绝Executor记录并主线程降级一次，异常隔离。

文档 ID：`android:member:com.netumscan.printersdk.PrinterManager:439521cb24ed4d5d`

JVM descriptor：`(Lcom/netumscan/printersdk/MobilePrinter;Lcom/netumscan/printersdk/PrinterManager$LifecycleListener;)Lcom/netumscan/printersdk/Subscription;`

<a id="api-55724a05a78f25935400"></a>

### public com.netumscan.printersdk.Subscription release(com.netumscan.printersdk.MobilePrinter, java.util.concurrent.Executor, com.netumscan.printersdk.PrinterManager$LifecycleListener)

以回调观察Manager的release完成。

```java
public com.netumscan.printersdk.Subscription release(com.netumscan.printersdk.MobilePrinter, java.util.concurrent.Executor, com.netumscan.printersdk.PrinterManager$LifecycleListener);
```

参数验证先于生命周期动作；listener/显式executor不可null。等价对应Future入口，默认主线程或指定Executor异步回调，failure=null成功。已完成后注册仍一次投递；Subscription.close只停止通知，不中断连接或撤销关闭。关闭会drain/copy已注册operation终态，但不等待客户callback执行。拒绝Executor记录并主线程降级一次，异常隔离。

文档 ID：`android:member:com.netumscan.printersdk.PrinterManager:a450dc4023416840`

JVM descriptor：`(Lcom/netumscan/printersdk/MobilePrinter;Ljava/util/concurrent/Executor;Lcom/netumscan/printersdk/PrinterManager$LifecycleListener;)Lcom/netumscan/printersdk/Subscription;`

<a id="api-67dc6709f0583f946f7c"></a>

### public java.util.concurrent.Future<java.lang.Void> close()

永久停止接纳，并异步回收此管理器的全部设备。

```java
public java.util.concurrent.Future<java.lang.Void> close();
```

无参数，开始处理即关闭接纳入口，并独立调度每个拥有设备的关闭；一个端点的阻塞 IO 不应延迟其他端点的中断。立即返回 Future<Void>，重复调用返回同一个回收 Future；成功 get() 返回 null 表示全部设备清理完成，清理失败通过 ExecutionException 报告。close 不影响其他普通管理器或外部设备，也不隐式释放调用者持有的独立 PrintOperation 结果。完成后本实例永久关闭，工厂和 connect 以 1002 拒绝新工作；getInstance() 的共享实例同样不会重建，可显式构造普通实例开始新生命周期。Future.cancel(...) 返回 false；不要在 UI 线程阻塞等待 get()。

文档 ID：`android:member:com.netumscan.printersdk.PrinterManager:537df28a283a6a10`

JVM descriptor：`()Ljava/util/concurrent/Future;`

<a id="api-ac7d80f4f1c946591247"></a>

### public java.util.concurrent.Future<java.lang.Void> connect(com.netumscan.printersdk.MobilePrinter, int)

在后台显式连接本管理器拥有的设备。

```java
public java.util.concurrent.Future<java.lang.Void> connect(com.netumscan.printersdk.MobilePrinter, int);
```

printer 必须是本管理器当前拥有的非 null MobilePrinter；timeoutMs 为正整数毫秒，控制这次连接的超时。null、非正超时或外部/已移除设备同步抛 PrinterException 1001；管理器关闭、设备正在回收或同设备已有未完成 connect 为 1002。接纳后立即返回非 null Future<Void>，后台执行阻塞连接，成功的 get() 返回 null；运行错误由 get() 抛 ExecutionException，其 cause 保留连接或 SDK 错误。已连接设备的再次有效连接沿用 MobilePrinter 的幂等成功行为。连接失败保留注册表条目，应用可在此前请求结束后显式重试；不自动重连或重放。Future.cancel(...) 始终返回 false；需要中断时显式 release/close。被回收或关闭打断的连接以 1002 结束。get() 和带超时 get() 可能阻塞或抛 InterruptedException/TimeoutException，不在 UI 线程等待；等待超时或线程中断不会隐式关闭设备。

文档 ID：`android:member:com.netumscan.printersdk.PrinterManager:25538fd80aa466e2`

JVM descriptor：`(Lcom/netumscan/printersdk/MobilePrinter;I)Ljava/util/concurrent/Future;`

<a id="api-7936f6026f075bff46d7"></a>

### public java.util.concurrent.Future<java.lang.Void> release(com.netumscan.printersdk.MobilePrinter)

异步关闭并移除这个确切的管理器所有设备。

```java
public java.util.concurrent.Future<java.lang.Void> release(com.netumscan.printersdk.MobilePrinter);
```

printer 按对象身份匹配，而不是地址。null、外部管理器设备、未知或已经移除的旧对象返回已完成的空操作 Future，不关闭外部设备或同端点后来创建的替代设备。仍在回收的对象重复 release 返回同一 Future；端点直到关闭完成一直保留占位，此期间工厂以 1002 拒绝重建。回收在独立控制执行路径调用设备 close，可中断阻塞连接/发送，不等待它们在连接执行器前方完成；成功 get() 返回 null 表示关闭和移除完成，之后工厂可为该端点创建新对象。若清理失败，Future.get() 通过 ExecutionException 报错并保留端点占位，避免替换一个所有权不确定的设备。独立 PrintOperation 结果仍由调用者持有。Future 不接受 cancel；不在 UI 线程阻塞等待。

文档 ID：`android:member:com.netumscan.printersdk.PrinterManager:0811d1ab77fdf9af`

JVM descriptor：`(Lcom/netumscan/printersdk/MobilePrinter;)Ljava/util/concurrent/Future;`

<a id="api-38b564c3c2367365e8ef"></a>

### public static com.netumscan.printersdk.PrinterManager getInstance()

取得进程共享的可选管理器。

```java
public static com.netumscan.printersdk.PrinterManager getInstance();
```

无参数，同步返回同一个非 null PrinterManager；首次获取只创建本地管理状态，不打开蓝牙或执行打印机 IO。共享实例的设备生命周期由应用指定的管理器所有者协调，借用方不直接关闭共享设备。调用 close() 后共享实例永久关闭；再次 getInstance() 仍返回该已关闭对象，不静默重建。需要新生命周期时显式 new PrinterManager()。

文档 ID：`android:member:com.netumscan.printersdk.PrinterManager:f6832d37e339b9a8`

JVM descriptor：`()Lcom/netumscan/printersdk/PrinterManager;`

<a id="api-16b0fff816f0a5953ef5"></a>

## PrinterManager.LifecycleListener

接收连接或回收完成的监听器。

```java
public interface com.netumscan.printersdk.PrinterManager$LifecycleListener
```

completed的failure为null表示成功，否则为对应异常。默认主线程或指定Executor异步投递，注销Subscription只停止通知，不撤销连接/关闭；回调异常被隔离记录。

文档 ID：`android:type:com.netumscan.printersdk.PrinterManager$LifecycleListener`

<a id="api-bd5b0dcc5184565493f9"></a>

### public abstract void completed(java.lang.Throwable)

接收一次连接或回收结果。

```java
public abstract void completed(java.lang.Throwable);
```

failure为null表示成功，否则为对应Throwable；已完成后注册仍异步通知。客户异常被隔离，Subscription.close只抑制通知，不撤销清理。

文档 ID：`android:member:com.netumscan.printersdk.PrinterManager$LifecycleListener:39947a2f11e46a00`

JVM descriptor：`(Ljava/lang/Throwable;)V`

<a id="api-bc4525656d3d87c6f484"></a>

## ResourceSnapshot

设备源载荷接纳限制及占用快照。

```java
public final class com.netumscan.printersdk.ResourceSnapshot
```

仅为本地观察，不执行 IO，不预留容量；submit 的原子接纳结果为准。数据不是 App 总内存、编码缓冲区大小或打印机硬件容量。终态 operation 保留结果不会继续占用源载荷接纳预算。

文档 ID：`android:type:com.netumscan.printersdk.ResourceSnapshot`

<a id="api-0c92bf14563b8e50d9d2"></a>

### public final long maxCopies

SDK 单作业份数上限。

```java
public final long maxCopies;
```

接纳限制，与打印机机械容量或已打印份数无关。

文档 ID：`android:member:com.netumscan.printersdk.ResourceSnapshot:a3f888df0730ec00`

JVM descriptor：`J`

<a id="api-26af1144eb38e0b34bed"></a>

### public final long maxImageBytes

单幅图像源像素载荷上限。

```java
public final long maxImageBytes;
```

单位字节，提交时复制像素并检查；不等同于图像文件压缩长度。

文档 ID：`android:member:com.netumscan.printersdk.ResourceSnapshot:aff496219ca95113`

JVM descriptor：`J`

<a id="api-131f99fa57b8f2331029"></a>

### public final long maxJobImageBytes

单个作业的图像源载荷总上限。

```java
public final long maxJobImageBytes;
```

单位字节，多个图像相加，不是最终编码输出大小。

文档 ID：`android:member:com.netumscan.printersdk.ResourceSnapshot:a76e5e884697087b`

JVM descriptor：`J`

<a id="api-4cf9218dff756b3c1e5a"></a>

### public final long maxPendingOperations

设备允许的未终态业务操作上限。

```java
public final long maxPendingOperations;
```

包括活动和等待操作；用于展示，提交仍是原子接纳权威。

文档 ID：`android:member:com.netumscan.printersdk.ResourceSnapshot:cce72d0143610139`

JVM descriptor：`J`

<a id="api-4072fd2d6646bd567458"></a>

### public final long maxPendingPayloadBytes

设备未终态源载荷合计上限。

```java
public final long maxPendingPayloadBytes;
```

单位字节，与手机进程总内存、实时空闲内存及打印机容量不同。

文档 ID：`android:member:com.netumscan.printersdk.ResourceSnapshot:f4ec4bd2cc69f60d`

JVM descriptor：`J`

<a id="api-442bc1577a6ee42e0849"></a>

### public final long maxRawBytes

单个 Raw 操作的源字节上限。

```java
public final long maxRawBytes;
```

单位字节，提交时检查，不表示协议编码后或打印机内部缓存大小。

文档 ID：`android:member:com.netumscan.printersdk.ResourceSnapshot:3be1a4e66086017e`

JVM descriptor：`J`

<a id="api-2df9bb17a4979eb0b013"></a>

### public final long outstandingOperations

当前未终态业务操作数。

```java
public final long outstandingOperations;
```

包括活动与等待操作；不包括已经终态但尚持有结果的 operation。

文档 ID：`android:member:com.netumscan.printersdk.ResourceSnapshot:c2b79699e3c0841d`

JVM descriptor：`J`

<a id="api-19c7ab5ca03c1e6a32d5"></a>

### public final long pendingPayloadBytes

当前未终态操作保留的源载荷字节数。

```java
public final long pendingPayloadBytes;
```

包括等待和活动操作，终态后释放预算；观察不会锁定剩余容量。

文档 ID：`android:member:com.netumscan.printersdk.ResourceSnapshot:8e1f6620357ac8d8`

JVM descriptor：`J`

<a id="api-b49ec93e3a988bef134a"></a>

## ScalarConstraint

不可变标量约束及依据。

```java
public final class com.netumscan.printersdk.ScalarConstraint
```

parameterId 指定约束，known=false 表示未知，不能解释为无限制。源载荷约束来自 SDK 接纳策略；协议几何或硬件支持仍须使用相应配置和能力证据。

文档 ID：`android:type:com.netumscan.printersdk.ScalarConstraint`

<a id="api-8477e1284dd57f499175"></a>

### public final boolean known

此约束是否有可靠已知范围。

```java
public final boolean known;
```

false 时 minimum、maximum、step 不能解释为无限制或硬件保证。

文档 ID：`android:member:com.netumscan.printersdk.ScalarConstraint:a2fcb139f5d87956`

JVM descriptor：`Z`

<a id="api-d43001a41eba5b3320b7"></a>

### public final int evidence

范围的证据来源。

```java
public final int evidence;
```

区分 SDK 接纳策略、协议定义及其他证据，不能把 SDK 限制解释为已验证硬件能力。

文档 ID：`android:member:com.netumscan.printersdk.ScalarConstraint:958eb37813714ec1`

JVM descriptor：`I`

<a id="api-0e47382cd2419c821178"></a>

### public final int maximum

已知约束的闭区间最大值。

```java
public final int maximum;
```

单位由 parameterId 决定；仅 known=true 时有意义。

文档 ID：`android:member:com.netumscan.printersdk.ScalarConstraint:324164fa97d559e7`

JVM descriptor：`I`

<a id="api-5efa04f35bb99b9c6002"></a>

### public final int minimum

已知约束的闭区间最小值。

```java
public final int minimum;
```

单位由 parameterId 决定；仅 known=true 时有意义。

文档 ID：`android:member:com.netumscan.printersdk.ScalarConstraint:4c11d30ea9a16e9f`

JVM descriptor：`I`

<a id="api-118090c567ac5cb1249a"></a>

### public final int parameterId

本快照的约束参数标识。

```java
public final int parameterId;
```

使用 COPIES、MEDIA_GAP_UM、JOB_IMAGE_BYTES 或 PENDING_PAYLOAD_BYTES 查询；未知标识返回已知位为 false 的约束。

文档 ID：`android:member:com.netumscan.printersdk.ScalarConstraint:55be3d55c85c3eb8`

JVM descriptor：`I`

<a id="api-fefbccf3a2f0c364c608"></a>

### public final long step

已知约束的取值步长。

```java
public final long step;
```

在 minimum 起点按此步长取值；仅 known=true 时有意义。

文档 ID：`android:member:com.netumscan.printersdk.ScalarConstraint:5aa9cefccae2739d`

JVM descriptor：`J`

<a id="api-7b40261535e852085c70"></a>

### public static final int COPIES = 1

份数约束标识，单位份。

```java
public static final int COPIES = 1;
```

固定公共查询参数标识；范围通过 MobilePrinter.constraint 读取，未知时不能推断无限制。

文档 ID：`android:member:com.netumscan.printersdk.ScalarConstraint:1cb3e86c38667f1f`

JVM descriptor：`I`

<a id="api-e3f831803f36216af72a"></a>

### public static final int JOB_IMAGE_BYTES = 3

单作业图像源载荷约束标识，单位字节。

```java
public static final int JOB_IMAGE_BYTES = 3;
```

固定公共查询参数标识；范围通过 MobilePrinter.constraint 读取，未知时不能推断无限制。

文档 ID：`android:member:com.netumscan.printersdk.ScalarConstraint:633c5aa13832b2cb`

JVM descriptor：`I`

<a id="api-f817ae755a9cdbe8ec44"></a>

### public static final int MEDIA_GAP_UM = 2

介质间隙约束标识，单位微米。

```java
public static final int MEDIA_GAP_UM = 2;
```

固定公共查询参数标识；范围通过 MobilePrinter.constraint 读取，未知时不能推断无限制。

文档 ID：`android:member:com.netumscan.printersdk.ScalarConstraint:050af49c580b838f`

JVM descriptor：`I`

<a id="api-6c3c1651b1e3c2abcccf"></a>

### public static final int PENDING_PAYLOAD_BYTES = 4

设备未终态源载荷约束标识，单位字节。

```java
public static final int PENDING_PAYLOAD_BYTES = 4;
```

固定公共查询参数标识；范围通过 MobilePrinter.constraint 读取，未知时不能推断无限制。

文档 ID：`android:member:com.netumscan.printersdk.ScalarConstraint:da0e3bdd16ea3c2c`

JVM descriptor：`I`

<a id="api-e68d6e49886d528a891c"></a>

## SdkInfo

实际加载原生运行库的身份。

```java
public final class com.netumscan.printersdk.SdkInfo
```

由原生 C ABI 返回版本和构建身份，与 Demo BuildConfig 独立。生产 CMake 将原生版本、分发 VERSION 和源输入 SHA-256 编入库；缺失构建身份明确未知。该信息不证明签名、发布或硬件兼容。

文档 ID：`android:type:com.netumscan.printersdk.SdkInfo`

<a id="api-2fe33d151571157e902c"></a>

### public final int abiRevision

实际加载库的abiRevision。

```java
public final int abiRevision;
```

当前ABI3最低修订1，须与同版包装一起使用。

文档 ID：`android:member:com.netumscan.printersdk.SdkInfo:3c9a67871faec9fc`

JVM descriptor：`I`

<a id="api-fa7deb09d69a004e2a1f"></a>

### public final int abiVersion

实际加载库的abiVersion。

```java
public final int abiVersion;
```

当前ABI主版本3；ABI2不能混用，必须成套更新包装与库。

文档 ID：`android:member:com.netumscan.printersdk.SdkInfo:da69401829dacd32`

JVM descriptor：`I`

<a id="api-8a11e88892aadb1fa51d"></a>

### public final java.lang.String buildId

可选原生源构建 SHA-256 标识。

```java
public final java.lang.String buildId;
```

未知时为 null。生产 CMake 对受审定原生源和构建文件白名单计算，不包含 Git dirty、主机路径、环境或私有配置；不等同于完整二进制哈希。

文档 ID：`android:member:com.netumscan.printersdk.SdkInfo:1637e37d8d13a768`

JVM descriptor：`Ljava/lang/String;`

<a id="api-c3ffff62c449b0a3e8f2"></a>

### public final java.lang.String distributionVersion

构建库时嵌入的分发版本。

```java
public final java.lang.String distributionVersion;
```

生产 CMake 来源于 VERSION；未嵌入时为空字符串，不猜测或继承 Demo 版本。

文档 ID：`android:member:com.netumscan.printersdk.SdkInfo:17e16ecc61815642`

JVM descriptor：`Ljava/lang/String;`

<a id="api-17210b8724d6da37e2a2"></a>

### public final java.lang.String nativeVersion

实际加载库的nativeVersion。

```java
public final java.lang.String nativeVersion;
```

当前原生SDK语义版本0.3.0，生产CMake来源PROJECT_VERSION；不表示已发布。

文档 ID：`android:member:com.netumscan.printersdk.SdkInfo:9d70e248890bcc50`

JVM descriptor：`Ljava/lang/String;`

<a id="api-31d51fc34c8d04a03b44"></a>

## StatusQueryConfig

逐次硬件状态查询配置。

```java
public final class com.netumscan.printersdk.StatusQueryConfig
```

与打印几何、纸宽和 DPI 独立，接纳时复制，不改变连接默认值。CPCL/ESC_POS 当前未验证状态指令，因此查询同步返回 1004 UNSUPPORTED_OPERATION 且零打印机 IO；硬件仍未知。

文档 ID：`android:type:com.netumscan.printersdk.StatusQueryConfig`

<a id="api-972333d5cecdc3dfd0e8"></a>

### public com.netumscan.printersdk.StatusQueryConfig(int, int)

创建本次状态查询配置。

```java
public com.netumscan.printersdk.StatusQueryConfig(int, int);
```

protocol 只能是 CPCL 或 ESC_POS，queryTimeoutMs 必须为正整数毫秒，非法值同步抛 IllegalArgumentException。构造不执行 IO，不设置打印几何，也不证明固件支持状态查询。

文档 ID：`android:member:com.netumscan.printersdk.StatusQueryConfig:bfc5664f8357a682`

JVM descriptor：`(II)V`

<a id="api-7d926b09b7dd58da8403"></a>

### public final int protocol

本次状态查询选择的协议。

```java
public final int protocol;
```

只接受 CPCL=1 或 ESC_POS=2；选择协议不证明目标固件支持状态读回。

文档 ID：`android:member:com.netumscan.printersdk.StatusQueryConfig:b02cb9ffeefa8216`

JVM descriptor：`I`

<a id="api-61205a74675bd4d9f2d1"></a>

### public final int queryTimeoutMs

本次请求／响应事务的超时预算。

```java
public final int queryTimeoutMs;
```

必须大于 0，单位毫秒；与 queryStatus 的排队及操作总预算分别设置，不能据此绕过总预算。

文档 ID：`android:member:com.netumscan.printersdk.StatusQueryConfig:ee80c97565d158f3`

JVM descriptor：`I`

<a id="api-f34b5e25eec42fda666e"></a>

### public static final int CPCL = 1

CPCL 协议标识。

```java
public static final int CPCL = 1;
```

固定公共常量；仅标识协议，不保证目标固件已验证。

文档 ID：`android:member:com.netumscan.printersdk.StatusQueryConfig:bc25745e438ddbc7`

JVM descriptor：`I`

<a id="api-885a4db7b2a8d2745ec9"></a>

### public static final int ESC_POS = 2

ESC/POS 协议标识。

```java
public static final int ESC_POS = 2;
```

固定公共常量；仅标识协议，不保证目标固件已验证。

文档 ID：`android:member:com.netumscan.printersdk.StatusQueryConfig:282da3ae891951ec`

JVM descriptor：`I`

<a id="api-0f650e9e7ff17c052425"></a>

## Subscription

可显式注销的通知所有权。

```java
public final class com.netumscan.printersdk.Subscription implements java.lang.AutoCloseable
```

close幂等，只丢弃本订阅客户引用并抑制未开始回调；已开始回调可完成。不会取消打印、关闭设备或撤销Manager生命周期工作。

文档 ID：`android:type:com.netumscan.printersdk.Subscription`

<a id="api-59bef25903e0eb9f91ea"></a>

### public void close()

停止本订阅的后续通知。

```java
public void close();
```

幂等，抑制尚未开始的已排队callback，已执行callback可以结束。不调用operation.cancel或关闭共享设备，也不撤销Manager已开始的生命周期工作。

文档 ID：`android:member:com.netumscan.printersdk.Subscription:71f6e6626cb069d2`

JVM descriptor：`()V`
