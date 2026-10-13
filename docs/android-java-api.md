# Android Java API 参考

适用分发版本 **0.2.0-preview.4**；原生 SDK **0.2.0**；C ABI **2/r10**。

本页逐项覆盖审定的编译器公共 API 基线，共 **131** 个文档条目。
声明、JVM descriptor、常量、枚举和编译器关系均来自基线，说明按稳定文档 ID 维护。
机器可读的完整基线与说明见 [api-reference.json](api-reference.json)；运行
`python3 tools/check-api-reference.py` 可检查覆盖范围、审定 SHA-256 和本页字节一致性。

基线 SHA-256：`8ad69087e4a965f7df4aa56f84b33dc6cfcf340ed3e8395f57155dfe449390a2`

运行约束与错误处理见 [API 与生命周期](api-lifecycle.md) 和 [错误码与排障](errors-troubleshooting.md)。
本页覆盖发布的公共接口；编译器合成接口也明确标注。硬件验收仍暂缓，`hardware_verified=false`。

## 类型导航

- [BleOptions](#api-168ab2e3c48dfc7424a0)
- [BleOptions.CreditFlowControl](#api-7b31b50111b4bf370803)
- [BluetoothDiscovery](#api-8e189f6bdf21bdc6ff63)
- [BluetoothDiscovery.Device](#api-478f1d653a33d3fd7141)
- [BluetoothDiscovery.Listener](#api-d904c51e8af901bda535)
- [CpclProfile](#api-d96de71f64e0601eb399)
- [EscPosProfile](#api-961de0862e0cb78fdffe)
- [FeatureSupport](#api-1713e3bca4852cbf74f4)
- [M90Profile](#api-594e899b3823cf2251e4)
- [MediaMode](#api-4b74ae3f0e52d0db0f65)
- [MobilePrinter](#api-58282a32df748c4ef2b2)
- [MobilePrinter.TaskEvent](#api-321c7ca1e53db64c4b8f)
- [PrintOperation](#api-2a1340ef023ed5fd60c2)
- [PrintOperation.Snapshot](#api-28695f8f756b652c83c0)
- [PrinterException](#api-21f2c4252d1bd090ee0b)

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

不可变配置，用于 CPCL 设备工厂。点数为打印机 dots，DPI 与纸张毫米数需由调用方结合实际设备换算，SDK 不猜测型号。`widthDots` 是设备最大可打印宽度，不是输入图片固定宽度。`xOffsetDots` 和 `form` 仅为 `submitGray8()` 便捷入口的默认值；显式 `submitLabelGray8()` 使用自己的原点和结束策略，Raw 字节不受这些值改写。配置不是硬件验收证明。

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

<a id="api-2711890ae200c6e98573"></a>

### public static final int DEVICE_REPORTED = 2

证据位 2：支持信息由设备报告。

```java
public static final int DEVICE_REPORTED = 2;
```

用于 `evidence` 的位掩码。该位表达设备自报来源；不保证报告已被物理打印验证，也不能因为发现了广播或蓝牙名称而自行设置。

文档 ID：`android:member:com.netumscan.printersdk.FeatureSupport:7384e00424860bf1`

JVM descriptor：`I`

<a id="api-29c5a143635d71a155f4"></a>

### public static final int HARDWARE_VERIFIED = 4

证据位 4：存在硬件验证证据。

```java
public static final int HARDWARE_VERIFIED = 4;
```

用于 `evidence` 的位掩码，应通过按位与检查。当前预览版软件接入验证不授予该位，`hardware_verified=false` 保持；配置、模拟器成功或数据发送完成都不等同于硬件验收。

文档 ID：`android:member:com.netumscan.printersdk.FeatureSupport:a41dc2f2bd9542ff`

JVM descriptor：`I`

<a id="api-333b4ee110fff9669c75"></a>

### public static final int HOST_RENDERED = 2

实现类别值 2：功能使用主机侧渲染或处理路径。

```java
public static final int HOST_RENDERED = 2;
```

属于 `implementation` 的取值，当前图像能力使用该类别。它不意味着设备支持所有业务元素；应用仍需提供受支持的整图格式和尺寸。

文档 ID：`android:member:com.netumscan.printersdk.FeatureSupport:6e6ae85ee79f4469`

JVM descriptor：`I`

<a id="api-36b7b3a7bdf8d0253599"></a>

### public static final int NATIVE = 1

实现类别值 1：SDK 具有原生协议实现路径。

```java
public static final int NATIVE = 1;
```

属于 `implementation` 的取值；与设备平台原生句柄无关。此值不说明 `deviceSupport` 已知，更不说明硬件已经验证。

文档 ID：`android:member:com.netumscan.printersdk.FeatureSupport:9c09323ac8e6906f`

JVM descriptor：`I`

<a id="api-8f674851f75479a82ebf"></a>

### public static final int NOT_IMPLEMENTED = 0

实现类别值 0：SDK 尚无该功能实现。

```java
public static final int NOT_IMPLEMENTED = 0;
```

属于 `implementation` 的取值；不表示硬件永远不支持，调用当前未实现操作通常收到 1004。与同为 0 的设备 UNKNOWN 属于不同维度。

文档 ID：`android:member:com.netumscan.printersdk.FeatureSupport:258ec4eedffc58f1`

JVM descriptor：`I`

<a id="api-7cb192a56c0db341863a"></a>

### public static final int PROFILE_DECLARED = 1

证据位 1：支持信息来自配置档案声明。

```java
public static final int PROFILE_DECLARED = 1;
```

用于 `evidence` 的位掩码，不是实现类别。显式配置声明未经过设备探测或纸面验证，不能提升为 DEVICE_REPORTED 或 HARDWARE_VERIFIED。

文档 ID：`android:member:com.netumscan.printersdk.FeatureSupport:296a3a40ae4f87ce`

JVM descriptor：`I`

<a id="api-d39842a85f93d06ffa3a"></a>

### public static final int SUPPORTED = 2

设备支持值 2：设备支持已知为支持。

```java
public static final int SUPPORTED = 2;
```

属于 `deviceSupport` 的取值。只有同时存在 SDK 实现时 `isSupported()` 才为 true；仍须另看 evidence 是否包含硬件验证。

文档 ID：`android:member:com.netumscan.printersdk.FeatureSupport:2660f6d171c11793`

JVM descriptor：`I`

<a id="api-b402a3b6e7c5f8404721"></a>

### public static final int UNKNOWN = 0

设备支持值 0：设备支持情况未知。

```java
public static final int UNKNOWN = 0;
```

属于 `deviceSupport` 的取值。未知不能当作支持，也不能当作明确不支持；应保持未知并结合实现与证据展示。

文档 ID：`android:member:com.netumscan.printersdk.FeatureSupport:12fee7f020c6e236`

JVM descriptor：`I`

<a id="api-d1ae2781a5eb8768e313"></a>

### public static final int UNSUPPORTED = 1

设备支持值 1：已知设备不支持该功能。

```java
public static final int UNSUPPORTED = 1;
```

属于 `deviceSupport` 的取值，与 SDK 是否实现分别表达。`isSupported()` 对该值返回 false；不能仅凭 NOT_IMPLEMENTED 推断此值。

文档 ID：`android:member:com.netumscan.printersdk.FeatureSupport:cc394616ff0eb36c`

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

<a id="api-58282a32df748c4ef2b2"></a>

## MobilePrinter

持有一个蓝牙打印设备及其串行 IO 队列。

```java
public final class com.netumscan.printersdk.MobilePrinter implements java.lang.AutoCloseable
```

使用 `spp()` 或 `ble()` 创建，随后在应用工作线程调用阻塞的 `connect()`；`disconnect()`、`close()` 同样可能阻塞，不应在 UI 线程调用。图像、Raw、介质设置和下一标签定位共享同一设备 FIFO，不允许 Raw 绕过队列。提交同步完成输入校验、快照复制与接纳后返回 `PrintOperation`，不等待打印完成；大图复制也建议放在应用工作线程。每设备最多接纳 64 个未终态操作，未终态源载荷累计上限 64 MiB，单图 16 MiB、单次 Raw 1 MiB；这些是 SDK 接纳限制，不是手机峰值内存承诺。操作超时从入队后覆盖排队、准备与发送；取消/断开不能撤回已发送数据，发生传输中断后须显式重连且不会自动重放。`PrintOperation` 结果可独立于设备保留，兼容 task 便捷方法的结果则由本实例保管并轮询释放。设备关闭后除重复 `close()` 外不再使用其方法。成功仅 DATA_SENT，不代表纸面完成；当前状态查询和切纸尚未实现。

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

<a id="api-0fa9bb4b61b2a3f589d7"></a>

### public com.netumscan.printersdk.FeatureSupport feature(int)

读取指定功能的本地能力与证据快照。

```java
public com.netumscan.printersdk.FeatureSupport feature(int);
```

参数 `featureId` 使用 FEATURE_LABEL/RECEIPT/STATUS/MEDIA/CUT/RAW/IMAGE/FEED_TO_NEXT_LABEL。同步返回非 null `FeatureSupport`，不发打印机 IO、不入设备队列。未知 ID 返回 implementation=NOT_IMPLEMENTED、deviceSupport=UNKNOWN、evidence=0。已知显式配置通常只有 PROFILE_DECLARED 且设备支持未知；当前图像是 HOST_RENDERED，状态和切纸未实现，CPCL 支持标签/介质/定位实现，ESC/POS 支持小票实现，Raw 两者均有实现。对象关闭等非法状态抛出 `PrinterException` code 1002，不能用实现存在替代硬件验证。

文档 ID：`android:member:com.netumscan.printersdk.MobilePrinter:02554ca7e27ea317`

JVM descriptor：`(I)Lcom/netumscan/printersdk/FeatureSupport;`

<a id="api-7249dc158d86c89166a4"></a>

### public com.netumscan.printersdk.MobilePrinter$TaskEvent[] pollTaskEvents()

轮询兼容 task 快照并一次性消费终态结果。

```java
public com.netumscan.printersdk.MobilePrinter$TaskEvent[] pollTaskEvents();
```

无参数，同步返回非 null `TaskEvent[]`，没有保管 task 时为空数组；不触发蓝牙 IO，也不回调主线程。每个尚保管的 task 都生成当前快照，因此非终态可重复出现；终态在本次成功返回时释放结果和槽位，之后不再返回。数组按内部槽位遍历，不保证 task ID 或完成时间排序。仅包含兼容 task，不包含调用方持有的独立 PrintOperation。关闭设备后不可轮询（1002）；读取结果失败时可抛出 `PrinterException`，应用应在正常生命周期持续轮询避免 64 槽位占满。

文档 ID：`android:member:com.netumscan.printersdk.MobilePrinter:739b4215ea175425`

JVM descriptor：`()[Lcom/netumscan/printersdk/MobilePrinter$TaskEvent;`

<a id="api-efbbbfb8b2a1cb0e947e"></a>

### public com.netumscan.printersdk.PrintOperation locateNextLabel(int)

将定位到下一标签开始的独立命令入队。

```java
public com.netumscan.printersdk.PrintOperation locateNextLabel(int);
```

参数 `timeoutMs` 为正整数毫秒，包含排队与执行预算；非法值抛出 `PrinterException` code 1001。仅当前 CPCL 后端实现，ESC/POS 为 1004。调用方须先装入并配置适当的间隙/黑标介质；此操作不改变介质设置、不做传感器校准或纸张位置回读。同步接纳后返回非 null `PrintOperation`，同设备 FIFO 执行；成功仅 DATA_SENT，不能声称物理定位已经验证。未连接为 2000，接纳上限为 1008，关闭为 1002；结果及异步错误通过 snapshot 读取并须关闭。

文档 ID：`android:member:com.netumscan.printersdk.MobilePrinter:be9487a5c7874ba1`

JVM descriptor：`(I)Lcom/netumscan/printersdk/PrintOperation;`

<a id="api-6412b6dd41f160b0c7bc"></a>

### public com.netumscan.printersdk.PrintOperation queryStatus(int)

请求状态操作；当前标准后端明确拒绝。

```java
public com.netumscan.printersdk.PrintOperation queryStatus(int);
```

参数 `timeoutMs` 为正整数毫秒。当前 CPCL 与 ESC/POS 标准后端均未实现设备状态查询：有效请求同步抛出 `PrinterException` code 1004，不返回占位 `PrintOperation`、不发送 IO。非法超时为 1001，关闭状态为 1002。不要以 `connectionState()==CONNECTED`、发现结果或 DATA_SENT 代替纸尽/开盖/实际打印完成查询。保留此方法供未来受明确能力约束的实现使用。

文档 ID：`android:member:com.netumscan.printersdk.MobilePrinter:3dd7f34f0f422c70`

JVM descriptor：`(I)Lcom/netumscan/printersdk/PrintOperation;`

<a id="api-94334ec2b14d399b2952"></a>

### public com.netumscan.printersdk.PrintOperation sendRaw(byte[], int, int)

在同一设备队列中发送原始字节及显式份数。

```java
public com.netumscan.printersdk.PrintOperation sendRaw(byte[], int, int);
```

参数 `bytes` 为非 null、非空字节数组，单份至多 1 MiB；`copies` 为 1..100。SDK 在一次调用中复制单份载荷，一个队列条目内逐份发送，不构造 copies 倍拼接数组，不解析、补齐或更改协议字节。调用方须自行保证命令适用于目标固件和当前介质，Raw 不继承标签原点/form。取消、超时或断开会停止后续发送，已发送份数/部分字节无法撤回；不要自动重试不确定的输出。同步完成校验和输入快照复制、接纳后返回非 null `PrintOperation`，不等候操作终态；不要在调用仍进行时并发修改输入数组。`timeoutMs` 为正整数毫秒，入队后的总预算覆盖排队/准备/发送；传输单次写入也受其剩余时间限制。操作与本设备其他打印/Raw/控制 IO 串行。无效参数通常抛出 `PrinterException` code 1001，错误后端/未实现功能为 1004，未连接为 2000，接纳数或源载荷超限为 1008，关闭状态为 1002；已接纳后的异步错误读 `snapshot().error`。结果使用完须 `close()`；成功 DATA_SENT 只证明数据发送。

文档 ID：`android:member:com.netumscan.printersdk.MobilePrinter:0edaf051d84b86c8`

JVM descriptor：`([BII)Lcom/netumscan/printersdk/PrintOperation;`

<a id="api-141740b36c2c664495af"></a>

### public com.netumscan.printersdk.PrintOperation setMedia(com.netumscan.printersdk.MediaMode, int)

将介质跟踪方式设置命令排入设备队列。

```java
public com.netumscan.printersdk.PrintOperation setMedia(com.netumscan.printersdk.MediaMode, int);
```

参数 `mode` 不可为 null，否则同步抛出 `IllegalArgumentException`；使用 GAP、BLACK_MARK 或 CONTINUOUS。`timeoutMs` 为正整数毫秒；非法超时抛出 `PrinterException` code 1001。仅当前 CPCL 后端实现，ESC/POS 抛出 1004。同步校验/接纳后返回非 null `PrintOperation`，命令与打印及 Raw 共享 FIFO；不设置纸宽/纸长/间隙长度，不校准传感器，不自动保存到硬件永久存储或读取实际介质。成功仅 DATA_SENT，不证明纸张已按所选方式就绪。未连接为 2000，接纳上限为 1008，关闭为 1002；异步错误读 snapshot，结果须关闭。

文档 ID：`android:member:com.netumscan.printersdk.MobilePrinter:d13606768ca40154`

JVM descriptor：`(Lcom/netumscan/printersdk/MediaMode;I)Lcom/netumscan/printersdk/PrintOperation;`

<a id="api-b12383533eebbc9c6b6f"></a>

### public com.netumscan.printersdk.PrintOperation submitLabelGray8(byte[], int, int, int, int, int, int, boolean, int)

将调用方渲染的整张 Gray8 标签作为一个操作入队。

```java
public com.netumscan.printersdk.PrintOperation submitLabelGray8(byte[], int, int, int, int, int, int, boolean, int);
```

参数 `pixels` 不可为 null，必须为紧密排列、逐行存储的 `width*height` 字节：每像素灰度 0 黑、255 白，不含行 padding。`width`、`height` 为正整数 dots；单图源数据至多 16 MiB。`copies` 为 1..100；`trailingBlankDots` 为非负逐份结束留白；`originX`、`originY` 为非负 dots；`form=true` 使用下一标签开始策略，false 使用内容结束策略。`originX + ceil(width/8)*8` 不得超过 profile.widthDots，`originY + height + trailingBlankDots` 不得超过 profile.maxLabelHeightDots。没有自动缩放、负坐标或裁剪；采用阈值 128 的灰度转单色处理。仅 CPCL 后端支持，显式原点/form 不继承 profile 的兼容默认值。同步完成校验和输入快照复制、接纳后返回非 null `PrintOperation`，不等候操作终态；不要在调用仍进行时并发修改输入数组。`timeoutMs` 为正整数毫秒，入队后的总预算覆盖排队/准备/发送；传输单次写入也受其剩余时间限制。操作与本设备其他打印/Raw/控制 IO 串行。无效参数通常抛出 `PrinterException` code 1001，错误后端/未实现功能为 1004，未连接为 2000，接纳数或源载荷超限为 1008，关闭状态为 1002；已接纳后的异步错误读 `snapshot().error`。结果使用完须 `close()`；成功 DATA_SENT 只证明数据发送。

文档 ID：`android:member:com.netumscan.printersdk.MobilePrinter:d22f3ef7b82b6cea`

JVM descriptor：`([BIIIIIIZI)Lcom/netumscan/printersdk/PrintOperation;`

<a id="api-93c19bdf72c83a55be3e"></a>

### public com.netumscan.printersdk.PrintOperation submitReceiptGray8(byte[], int, int, int, int, long, int, int)

将整张调用方渲染的 Gray8 小票作为一个操作入队。

```java
public com.netumscan.printersdk.PrintOperation submitReceiptGray8(byte[], int, int, int, int, long, int, int);
```

参数 `pixels` 不可为 null，必须为 `width*height` 字节的紧密 Gray8 行数据（0 黑、255 白，无行 padding），单图至多 16 MiB；`width`、`height` 为正整数 dots，按 8 点补齐的宽度不得超过 profile.widthDots，不自动缩放。`copies` 为 1..100，`trailingBlankDots` 为每份的非负结束留白。`maxTotalLength` 为 1..4294967295 dots 的调用方整单预算，须覆盖 `(height + trailingBlankDots) * copies`；它不替代设备单份长度限制。`cutMode` 使用 CUT_NONE/FULL/PARTIAL，当前后端只支持 CUT_NONE，另外两种明确抛出 1004，不静默忽略。仅 ESC/POS 后端支持；灰度采用阈值 128 转单色。同步完成校验和输入快照复制、接纳后返回非 null `PrintOperation`，不等候操作终态；不要在调用仍进行时并发修改输入数组。`timeoutMs` 为正整数毫秒，入队后的总预算覆盖排队/准备/发送；传输单次写入也受其剩余时间限制。操作与本设备其他打印/Raw/控制 IO 串行。无效参数通常抛出 `PrinterException` code 1001，错误后端/未实现功能为 1004，未连接为 2000，接纳数或源载荷超限为 1008，关闭状态为 1002；已接纳后的异步错误读 `snapshot().error`。结果使用完须 `close()`；成功 DATA_SENT 只证明数据发送。

文档 ID：`android:member:com.netumscan.printersdk.MobilePrinter:46dd55dd945ff7eb`

JVM descriptor：`([BIIIIJII)Lcom/netumscan/printersdk/PrintOperation;`

<a id="api-88210c16c0ec402f3612"></a>

### public int connectionState()

读取 SDK 本地观察到的连接状态，不探测打印机。

```java
public int connectionState();
```

无参数，返回 DISCONNECTED=0、CONNECTING=1、CONNECTED=2 或 DISCONNECTING=3。同步读取本地状态，不发蓝牙 IO、不入打印队列，可在连接转换或打印时读取；对象关闭/正在关闭等非法生命周期抛出 `PrinterException` code 1002。CONNECTED 仅表示当前 SDK 观察状态，不保证设备就绪、纸张正常或链路此刻存活；未被 IO 察觉的断线可能暂时仍显示 CONNECTED。

文档 ID：`android:member:com.netumscan.printersdk.MobilePrinter:2414ee607f59e5f0`

JVM descriptor：`()I`

<a id="api-e6f3d0e2ba0469aa45ad"></a>

### public long feedToNextLabel()

以固定超时提交兼容下一标签定位 task。

```java
public long feedToNextLabel();
```

无参数，操作语义与 `locateNextLabel()` 相同，仅当前 CPCL 后端实现。须先装好并配置介质；不设置介质、不校准、不证明纸张实际位置。同步接纳后返回 long task ID；不支持、未连接等失败抛出 `PrinterException`。这些便捷 task 方法使用固定 120000 毫秒总超时，返回本设备生成的兼容 task ID，不是原生 operation ID；不要与 `PrintOperation.Snapshot.id` 混用。结果由设备保管，最多 64 个未被消费的 task（含已终态但未轮询者），满时抛出 `PrinterException` code 1008。应用须定期 `pollTaskEvents()` 回收终态，或使用独立 `PrintOperation` 方法控制结果生命周期。关闭设备会释放这些兼容结果。

文档 ID：`android:member:com.netumscan.printersdk.MobilePrinter:9a74c66f1528d999`

JVM descriptor：`()J`

<a id="api-1d4e4e9618a813f94cef"></a>

### public long setMediaMode(com.netumscan.printersdk.MediaMode)

以固定超时提交兼容介质跟踪 task。

```java
public long setMediaMode(com.netumscan.printersdk.MediaMode);
```

参数 `mode` 不可为 null（否则 `IllegalArgumentException`），使用 MediaMode 的三种枚举值。行为与 `setMedia()` 相同，仅当前 CPCL 后端实现，ESC/POS 抛出 `PrinterException` 1004；不校准、读取或验证介质。同步接纳后返回 long task ID。这些便捷 task 方法使用固定 120000 毫秒总超时，返回本设备生成的兼容 task ID，不是原生 operation ID；不要与 `PrintOperation.Snapshot.id` 混用。结果由设备保管，最多 64 个未被消费的 task（含已终态但未轮询者），满时抛出 `PrinterException` code 1008。应用须定期 `pollTaskEvents()` 回收终态，或使用独立 `PrintOperation` 方法控制结果生命周期。关闭设备会释放这些兼容结果。

文档 ID：`android:member:com.netumscan.printersdk.MobilePrinter:c2e92eaef16726cd`

JVM descriptor：`(Lcom/netumscan/printersdk/MediaMode;)J`

<a id="api-9bcf1fb964330fb83bec"></a>

### public long submitGray8(byte[], int, int, int, int)

以 profile 默认布局提交兼容标签 task。

```java
public long submitGray8(byte[], int, int, int, int);
```

参数 `pixels`、`width`、`height`、`copies`、`trailingBlankDots` 的格式及范围与 `submitLabelGray8()` 相同：紧密 Gray8（0 黑/255 白）、正尺寸、1..100 份、非负留白、单图至多 16 MiB。该入口仅提交 CPCL 标签，使用 `CpclProfile.xOffsetDots`、originY=0 及 profile.form；不是 ESC/POS 小票入口。同步复制/接纳后返回 long task ID，失败抛出 `PrinterException`，不等待纸面完成。这些便捷 task 方法使用固定 120000 毫秒总超时，返回本设备生成的兼容 task ID，不是原生 operation ID；不要与 `PrintOperation.Snapshot.id` 混用。结果由设备保管，最多 64 个未被消费的 task（含已终态但未轮询者），满时抛出 `PrinterException` code 1008。应用须定期 `pollTaskEvents()` 回收终态，或使用独立 `PrintOperation` 方法控制结果生命周期。关闭设备会释放这些兼容结果。

文档 ID：`android:member:com.netumscan.printersdk.MobilePrinter:a49b5f377af60d01`

JVM descriptor：`([BIIII)J`

<a id="api-c8ad141cdaf8e1d165d4"></a>

### public long submitRaw(byte[], int)

将 Raw 发送作为兼容 task 入队。

```java
public long submitRaw(byte[], int);
```

参数 `bytes` 为非 null、非空、至多 1 MiB 的单份原始数据，`copies` 为 1..100；其他 Raw 语义与 `sendRaw()` 相同，共享设备 FIFO，数据不被 SDK 改写。同步复制/接纳后返回 long task ID，参数/状态/资源错误抛出 `PrinterException`。这些便捷 task 方法使用固定 120000 毫秒总超时，返回本设备生成的兼容 task ID，不是原生 operation ID；不要与 `PrintOperation.Snapshot.id` 混用。结果由设备保管，最多 64 个未被消费的 task（含已终态但未轮询者），满时抛出 `PrinterException` code 1008。应用须定期 `pollTaskEvents()` 回收终态，或使用独立 `PrintOperation` 方法控制结果生命周期。关闭设备会释放这些兼容结果。

文档 ID：`android:member:com.netumscan.printersdk.MobilePrinter:5ad462ee249b739a`

JVM descriptor：`([BI)J`

<a id="api-4d862917ee7e8b261977"></a>

### public static com.netumscan.printersdk.MobilePrinter ble(android.content.Context, java.lang.String, com.netumscan.printersdk.BleOptions, com.netumscan.printersdk.CpclProfile)

创建使用 BLE GATT 传输的 CPCL 设备。

```java
public static com.netumscan.printersdk.MobilePrinter ble(android.content.Context, java.lang.String, com.netumscan.printersdk.BleOptions, com.netumscan.printersdk.CpclProfile);
```

参数 `context` 为有效非 null Android `Context`，`address` 为有效的蓝牙设备地址，`profile` 为非 null 且与真实设备匹配的显式几何配置。返回尚未连接的新 `MobilePrinter`，必须随后 `connect(timeoutMs)` 并在不用时 `close()`；工厂不发送打印数据，也不申请权限或自动确认型号。无蓝牙适配器时抛出 `PrinterException` code 1004，原生创建或 ABI 不匹配等失败也以 `PrinterException` 报告；ABI 不匹配为 1006，须替换整套 SDK。调用方不得依赖 null 或非法地址被统一映射为 SDK 错误码，应先校验参数。 参数 `ble` 为非 null `BleOptions`，明确服务、写特征、写方式、MTU 请求及分包上限。连接时验证实际 GATT 属性与可选信用流控，构造不协商 MTU。`CpclProfile` 不自动设置介质；使用显式标签提交或其兼容图像入口。

文档 ID：`android:member:com.netumscan.printersdk.MobilePrinter:b0ad77ed01a60793`

JVM descriptor：`(Landroid/content/Context;Ljava/lang/String;Lcom/netumscan/printersdk/BleOptions;Lcom/netumscan/printersdk/CpclProfile;)Lcom/netumscan/printersdk/MobilePrinter;`

<a id="api-943f0b5623d204e4d154"></a>

### public static com.netumscan.printersdk.MobilePrinter ble(android.content.Context, java.lang.String, com.netumscan.printersdk.BleOptions, com.netumscan.printersdk.EscPosProfile)

创建使用 BLE GATT 传输的 ESC/POS 设备。

```java
public static com.netumscan.printersdk.MobilePrinter ble(android.content.Context, java.lang.String, com.netumscan.printersdk.BleOptions, com.netumscan.printersdk.EscPosProfile);
```

参数 `context` 为有效非 null Android `Context`，`address` 为有效的蓝牙设备地址，`profile` 为非 null 且与真实设备匹配的显式几何配置。返回尚未连接的新 `MobilePrinter`，必须随后 `connect(timeoutMs)` 并在不用时 `close()`；工厂不发送打印数据，也不申请权限或自动确认型号。无蓝牙适配器时抛出 `PrinterException` code 1004，原生创建或 ABI 不匹配等失败也以 `PrinterException` 报告；ABI 不匹配为 1006，须替换整套 SDK。调用方不得依赖 null 或非法地址被统一映射为 SDK 错误码，应先校验参数。 参数 `ble` 为非 null `BleOptions`，明确服务、写特征、写方式、MTU 请求及分包上限；连接时验证实际 GATT 属性。该重载使用 ESC/POS 整图小票编码，应调用 `submitReceiptGray8()`；BLE 传输不改变切纸/状态尚未实现的限制。

文档 ID：`android:member:com.netumscan.printersdk.MobilePrinter:13954699662e0e49`

JVM descriptor：`(Landroid/content/Context;Ljava/lang/String;Lcom/netumscan/printersdk/BleOptions;Lcom/netumscan/printersdk/EscPosProfile;)Lcom/netumscan/printersdk/MobilePrinter;`

<a id="api-d4b123f0630c2ef57271"></a>

### public static com.netumscan.printersdk.MobilePrinter spp(android.content.Context, java.lang.String, com.netumscan.printersdk.CpclProfile)

创建使用经典蓝牙 SPP/RFCOMM 的 CPCL 设备。

```java
public static com.netumscan.printersdk.MobilePrinter spp(android.content.Context, java.lang.String, com.netumscan.printersdk.CpclProfile);
```

参数 `context` 为有效非 null Android `Context`，`address` 为有效的蓝牙设备地址，`profile` 为非 null 且与真实设备匹配的显式几何配置。返回尚未连接的新 `MobilePrinter`，必须随后 `connect(timeoutMs)` 并在不用时 `close()`；工厂不发送打印数据，也不申请权限或自动确认型号。无蓝牙适配器时抛出 `PrinterException` code 1004，原生创建或 ABI 不匹配等失败也以 `PrinterException` 报告；ABI 不匹配为 1006，须替换整套 SDK。调用方不得依赖 null 或非法地址被统一映射为 SDK 错误码，应先校验参数。 该重载使用标准 SPP 服务 UUID `00001101-0000-1000-8000-00805f9b34fb` 和 CPCL 编码；`CpclProfile.form/xOffsetDots` 作为 `submitGray8()` 的默认值，显式标签方法另行指定。不提供自动配对或设备发现。

文档 ID：`android:member:com.netumscan.printersdk.MobilePrinter:9cf9e836502f2c0d`

JVM descriptor：`(Landroid/content/Context;Ljava/lang/String;Lcom/netumscan/printersdk/CpclProfile;)Lcom/netumscan/printersdk/MobilePrinter;`

<a id="api-fc8d77ffde68d2521a67"></a>

### public static com.netumscan.printersdk.MobilePrinter spp(android.content.Context, java.lang.String, com.netumscan.printersdk.EscPosProfile)

创建使用经典蓝牙 SPP/RFCOMM 的 ESC/POS 设备。

```java
public static com.netumscan.printersdk.MobilePrinter spp(android.content.Context, java.lang.String, com.netumscan.printersdk.EscPosProfile);
```

参数 `context` 为有效非 null Android `Context`，`address` 为有效的蓝牙设备地址，`profile` 为非 null 且与真实设备匹配的显式几何配置。返回尚未连接的新 `MobilePrinter`，必须随后 `connect(timeoutMs)` 并在不用时 `close()`；工厂不发送打印数据，也不申请权限或自动确认型号。无蓝牙适配器时抛出 `PrinterException` code 1004，原生创建或 ABI 不匹配等失败也以 `PrinterException` 报告；ABI 不匹配为 1006，须替换整套 SDK。调用方不得依赖 null 或非法地址被统一映射为 SDK 错误码，应先校验参数。 该重载使用标准 SPP 服务 UUID `00001101-0000-1000-8000-00805f9b34fb` 和 ESC/POS 光栅小票编码；应使用 `submitReceiptGray8()`。不会因协议名称而启用切纸或状态查询；标签、介质跟踪及下一标签定位不适用于该后端。

文档 ID：`android:member:com.netumscan.printersdk.MobilePrinter:144fd0707f6f704b`

JVM descriptor：`(Landroid/content/Context;Ljava/lang/String;Lcom/netumscan/printersdk/EscPosProfile;)Lcom/netumscan/printersdk/MobilePrinter;`

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

<a id="api-de594263fd5fad35aae7"></a>

### public void cancel(long)

按兼容 task ID 请求取消当前设备保管的操作。

```java
public void cancel(long);
```

参数 `taskId` 只能使用本设备便捷 submit/set/feed 方法返回的 ID；不能传独立 operation 的 snapshot.id。无返回值，找到保管中的 task 时发出取消控制信号；未知或已轮询释放的 ID 不做任何事。取消不等待活动操作终态，也不能撤回已发送内容；请继续轮询确认 state/error/副作用。对象关闭时抛出 `PrinterException` 1002，已找到 operation 的取消失败可抛出相应 SDK 错误。

文档 ID：`android:member:com.netumscan.printersdk.MobilePrinter:ad0f905da65bdd75`

JVM descriptor：`(J)V`

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

<a id="api-321c7ca1e53db64c4b8f"></a>

## MobilePrinter.TaskEvent

兼容 task 轮询返回的不可变进度快照。

```java
public final class com.netumscan.printersdk.MobilePrinter$TaskEvent
```

由 `pollTaskEvents()` 创建，没有公共构造器。它是当前观察，不是一次状态转换通知：非终态可在多次轮询重复出现，终态只消费一次。`current/total` 是传输字节数，必须结合 byteCountFlags 判断精确性及总量是否已知，不能作为纸面打印比例。独立 PrintOperation 结果不进入此列表。

文档 ID：`android:type:com.netumscan.printersdk.MobilePrinter$TaskEvent`

<a id="api-d1621a34101f717d9bc6"></a>

### public final boolean sideEffectsPossible

是否可能已经对打印机产生外部副作用。

```java
public final boolean sideEffectsPossible;
```

true 表示已有写入尝试/输出等副作用风险，即使最终失败或取消也不能假定没有打印。false 不等同于状态成功；遇到 true 的不确定结果应由应用人工或业务规则决定后续操作，不自动重放。

文档 ID：`android:member:com.netumscan.printersdk.MobilePrinter$TaskEvent:09b6d93c4ce76763`

JVM descriptor：`Z`

<a id="api-0e0c4edc977e8e824fae"></a>

### public final int byteCountFlags

发送字节计数的有效性位掩码。

```java
public final int byteCountFlags;
```

位 1（BYTES_SENT_EXACT）表示 current 精确，位 2（BYTES_TOTAL_KNOWN）表示 total 已知，可组合。值为 0 时 current 最多是下界、total 未知；不能直接用 current/total 计算完成百分比。

文档 ID：`android:member:com.netumscan.printersdk.MobilePrinter$TaskEvent:aa7a5ce72b4732d1`

JVM descriptor：`I`

<a id="api-0b7a46e96612d35b9692"></a>

### public final int completion

操作已经达到的完成确认级别。

```java
public final int completion;
```

0 为 NONE，1 为 DATA_SENT，2 为 DEVICE_CONFIRMED。当前标准后端成功只报告 DATA_SENT，失败/取消通常 NONE；DATA_SENT 不证明纸面已完成，枚举值 2 的存在不表示当前已实现设备确认。

文档 ID：`android:member:com.netumscan.printersdk.MobilePrinter$TaskEvent:8241840aff5f60d2`

JVM descriptor：`I`

<a id="api-e676a4d3fb25d5506909"></a>

### public final int error

操作的稳定数值错误码。

```java
public final int error;
```

成功为 0；失败/取消参考 PrinterException.description() 的码表，取消通常 1005，超时 2001，断线 2002。非终态不能仅凭 error=0 认定成功，应先判断 state。消息供诊断，业务逻辑以数值与状态为准。

文档 ID：`android:member:com.netumscan.printersdk.MobilePrinter$TaskEvent:0f938395cfdf79ab`

JVM descriptor：`I`

<a id="api-5ebf09ed52e7e6a009ff"></a>

### public final int state

兼容任务当前操作状态整数。

```java
public final int state;
```

当前值为 QUEUED=0、PREPARING=1、SENDING=2、COMPLETED=4、FAILED=5、CANCELLED=6；4/5/6 为终态。完成仅表示传输操作结束，应另看 completion/error/sideEffectsPossible，不能推断物理打印完成。

文档 ID：`android:member:com.netumscan.printersdk.MobilePrinter$TaskEvent:47d05db295c29a6c`

JVM descriptor：`I`

<a id="api-c6dcccf3bb718921a82b"></a>

### public final long current

已观察到的发送字节数或保守下界。

```java
public final long current;
```

单位 bytes，来自操作 bytesSent；byteCountFlags 的位 1 表示该值精确，否则不得当作完整接受量。正在发送或出错时可能有平台已接受但尚未计入的字节，不能据此自动补发。

文档 ID：`android:member:com.netumscan.printersdk.MobilePrinter$TaskEvent:ad52f47997e5edf5`

JVM descriptor：`J`

<a id="api-f7a698f7e692ed9c2e30"></a>

### public final long id

便捷 task 方法返回的本设备任务 ID。

```java
public final long id;
```

用于将轮询结果关联到 submitGray8/submitRaw/setMediaMode/feedToNextLabel，并传给本设备 `cancel(long)`。它由 Java 便捷层分配，不是 PrintOperation.Snapshot.id，也不能跨设备使用。

文档 ID：`android:member:com.netumscan.printersdk.MobilePrinter$TaskEvent:26be605f3fdc9e9c`

JVM descriptor：`J`

<a id="api-70a90069b64a0497b310"></a>

### public final long total

已知时的总发送字节数，单位为 bytes。

```java
public final long total;
```

只有 `(byteCountFlags & 2) != 0` 才能使用。标志未设置时即使值为 0，也表示总量未知而非零字节工作；流式编码通常不能提前给出确定总量。

文档 ID：`android:member:com.netumscan.printersdk.MobilePrinter$TaskEvent:133a69f5ab94fe8b`

JVM descriptor：`J`

<a id="api-2a1340ef023ed5fd60c2"></a>

## PrintOperation

独立拥有一个已接纳操作的结果及取消能力。

```java
public final class com.netumscan.printersdk.PrintOperation implements java.lang.AutoCloseable
```

由 MobilePrinter 的显式提交/控制方法返回，实现 AutoCloseable，无公共构造器。持有数值快照和终态，不依赖设备对象仍然存活；设备关闭后仍可 snapshot()。snapshot/cancel/close 在该 Java 实例上同步互斥；它们不安排主线程回调。close() 只释放调用方的结果引用，不取消已经排队或发送的工作；要取消须先 cancel() 并观察终态。使用完应显式关闭，关闭后 snapshot/cancel 抛出 1002。

文档 ID：`android:type:com.netumscan.printersdk.PrintOperation`

<a id="api-0a9b35e0113bf90ba495"></a>

### public synchronized com.netumscan.printersdk.PrintOperation$Snapshot snapshot()

读取当前操作的不可变数值快照。

```java
public synchronized com.netumscan.printersdk.PrintOperation$Snapshot snapshot();
```

无参数，同步返回非 null `Snapshot`，不发起打印机 IO、不等待操作完成；可在设备已关闭后继续读取该结果的终态。结果仍在运行时可能重复读取到同一状态，也可能跳过中间状态。操作已关闭时抛出 `PrinterException` code 1002，其他快照读取失败以对应 code 抛出。方法在本实例上 synchronized，返回的快照不再随操作变化；新进度须再次读取。

文档 ID：`android:member:com.netumscan.printersdk.PrintOperation:b2032eaf77200681`

JVM descriptor：`()Lcom/netumscan/printersdk/PrintOperation$Snapshot;`

<a id="api-e62a83d4b95eb6b8d59b"></a>

### public synchronized void cancel()

请求取消当前操作，不释放结果。

```java
public synchronized void cancel();
```

无参数、无返回值，在本实例上 synchronized。对排队操作可直接形成取消终态；对活动准备/发送是协作取消请求，返回不表示活动操作已经终止，请继续 snapshot() 判断。终态操作或设备已销毁时取消可成功无事发生。已发送字节无法撤回，失败/取消后的副作用须检查；关闭后的结果抛出 `PrinterException` 1002。取消不自动重放、重连或关闭此结果。

文档 ID：`android:member:com.netumscan.printersdk.PrintOperation:a1228dc331f5087e`

JVM descriptor：`()V`

<a id="api-9d0a8e7ddf081740a212"></a>

### public synchronized void close()

释放该结果的调用方引用，不取消设备工作。

```java
public synchronized void close();
```

无参数、无返回值，在本实例上 synchronized，可重复调用。它不会等待操作终态，也不自动调用 cancel()；排队或活动工作可继续执行。关闭后 snapshot()/cancel() 抛出 `PrinterException` 1002；设备关闭和此结果关闭相互独立。若业务希望停止工作，应先请求取消并按需要读取终态，再关闭结果。

文档 ID：`android:member:com.netumscan.printersdk.PrintOperation:2c2502cc89f847f7`

JVM descriptor：`()V`

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

<a id="api-21f2c4252d1bd090ee0b"></a>

## PrinterException

携带稳定 SDK 错误码的运行时异常。

```java
public final class com.netumscan.printersdk.PrinterException extends java.lang.RuntimeException
```

继承 RuntimeException，无需 Java checked-exception 声明。同步创建/接纳/生命周期错误通过该异常报告；已接纳操作的异步失败通常通过 Snapshot.error/TaskEvent.error 报告，不会自动在提交线程抛出。业务分支使用 code，description/message 仅供诊断。蓝牙权限及平台状态问题可能归并为 1002；已发送内容不可因异常自动重试。

文档 ID：`android:type:com.netumscan.printersdk.PrinterException`

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

<a id="api-9845f15fdceaec00b85e"></a>

### public static java.lang.String description(int)

返回数值错误码的英文诊断短语。

```java
public static java.lang.String description(int);
```

参数 `code` 为任意 int，返回非 null String，不执行 IO。当前映射：1000 Internal error；1001 Invalid argument；1002 Invalid state（检查蓝牙、权限和生命周期）；1003 SDK not initialized；1004 Unsupported operation or Bluetooth characteristic；1005 Cancelled（已发送数据不可撤回）；1006 SDK ABI mismatch（替换完整 SDK）；1007 Output buffer too small；1008 SDK resource limit（释放/轮询结果）；2000 Transport not open；2001 Transport timeout；2002 Connection lost（显式重连、无自动重放）；2003 Transport IO error；2004 Transport made no progress；3000 Invalid protocol response。其他值返回 `Unknown printer SDK error`。短语只用于诊断，不是稳定的业务判断键；成功 code=0 不应通过此方法推导成功文本。

文档 ID：`android:member:com.netumscan.printersdk.PrinterException:ba359d8df979350d`

JVM descriptor：`(I)Ljava/lang/String;`
