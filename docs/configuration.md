# 设备、图片与纸张

[返回指南](README.md) · 适用版本 `0.3.0-preview.11`

每个Manager管理一个连接。扫描或保存的设备目标只包含地址／标识符和可选名称；不暴露平台句柄。已知名称可以推荐内置PrinterModel，改名但已支持的设备可显式指定型号。未知型号不得默认按M90处理；UUID、协议、点密度、超时和传输分块由SDK内部管理。

连接成功后读取ConnectionInfo.device和可选model。连接后空闲时setPrinterModel可以更正型号；成功只更新本地配置并失效旧介质证据，失败保留原值，断开清除。名称不认证硬件。

## 当前移动端支持矩阵

| 能力 | Android | iOS |
| --- | --- | --- |
| 最低编译／部署目标 | API21、Java8 | iOS15、Swift5/6 |
| BLE发现／连接 | scanBle、connectBle | scanBLE、connectBle |
| 系统已配对设备列表 | bondedDevices | 不提供 |
| 经典蓝牙 | connectSpp，SPP/RFCOMM | 不提供 |
| 图片 | Bitmap、PNG/JPEG byte[] | UIImage、PNG/JPEG Data |
| 纸型／定位 | 按已采用型号能力执行 | 同Android |
| 硬件状态查询／结构化切纸 | 当前不支持，查询回调1004 | 同Android |
| TCP／USB／串口移动入口 | 不提供 | 不提供 |
| 权限、配对、文件选择 | App/plugin负责 | App/plugin负责 |
| 自动重连／续打／重放 | 不提供 | 不提供 |

此矩阵描述软件实现，hardware_verified=false。1004不覆盖参数错误或未连接状态。最低部署目标不表示最低系统运行时已验收，设备在已配对列表不证明支持SPP。

## 图片与页面尺寸

printImage接收Bitmap／UIImage；printEncodedImage接收完整PNG/JPEG byte[]／Data。Flutter的Uint8List通过标准codec映射为Android byte[]和iOS FlutterStandardTypedData.data，插件直接转交；不使用Base64或裸RGBA。Flutter ui.Image需明确导出PNG。

两入口均只传widthMm、heightMm和完成回调。毫米按已采用型号的精确点密度换算，屏幕density、UIImage.scale和文件DPI不决定尺寸。SDK修正方向、透明白底、等比缩小居中补白，不放大、不裁剪；固定一份、阈值128、不反色、不隐式FORM或切纸。文本和条码先由App渲染为图片。

Bitmap、byte[]／Data 在返回前取得独立副本；返回后可更改原 buffer 或回收 Bitmap，复制期间不得并发修改输入。UIImage 则由 SDK 强持有不可变对象，返回后可释放调用方引用，自定义 provider 底层数据须保持不变。解码与准备在有界后台执行，取消后的实际读者退出前继续计入资源。逻辑载荷预算不等于进程RSS上限；资源不足应缩减输入或在途工作，不连续重试规避。

## 纸张与结果

PaperType为GAP_LABEL／BLACK_MARK／CONTINUOUS，Swift为gapLabel／blackMark／continuous，即间隙标签纸、黑标纸、连续纸。setPaperType独立发送设置，不自动校准或持久化；locateNextLabel独立定位，页面尺寸不替代纸型。

capabilities返回三态Support和可选毫米范围，UNKNOWN保留未知。queryStatus的缺纸、开盖、纸将尽和切刀错误均为可选布尔；缺失不等于正常。DATA_SENT仅证明发送完成，不代表设置已读回或机械打印完成。

UIImage 入口支持可取得 CGImage 的非动画图片；CI-only、矢量／系统符号等无 CGImage 来源及动画图片返回 1001。检查和像素读取在后台执行。源像素 bytesPerRow×height 上限 16 MiB，接纳时按此上限保守预留输入额度；编码 Data 上限 32 MiB，大图优先使用 Data 按目标采样。SDK 强持有不可变 UIImage，返回后可释放或重新赋值调用方引用；自定义 CGDataProvider 的底层数据须在 provider 生命周期内保持不变。Bitmap、byte[]／Data 仍在返回前复制，复制期间不得并发修改，返回后可修改或释放原数据。iOS 只有保存 UUID、没有有效名称且未显式选择型号时，连接返回 1004。
