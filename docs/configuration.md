# 连接与打印配置

[返回指南](README.md) · 适用发布候选 `0.3.0-preview.2`

设备只拥有通信连接；协议配置在每次打印、介质设置或走纸时单独传入。TCP/SPP/BLE 是
传输类型，CPCL/ESC/POS 是协议，不能相互推断。Android SPP 使用经典蓝牙 RFCOMM；
BLE 需显式提供服务、写特征及流控配置。设备名或开发 preset 不构成硬件验证。

Profile 包含协议、DPI、宽度、页高及传输/条带策略。App 必须使用已确认的设备/固件配置，
每次提交不改变设备默认值。光栅条带大小与传输分块大小独立；Raw 的 SDK 默认分块为
1024 字节、写超时为 5000ms，属于 SDK 策略，不是设备能力。

能力快照分别保留实现范围、deviceSupport、evidence、known 和 geometry 的有效字段；
未知不等于支持、就绪或无限。当前纯通信设备没有已知打印几何。标签页高是单页/单份限制，
小票 maxTotalLength 是所有副本和结束留白的整单预算；两者不能替换使用。

当前源载荷接纳策略：每图 16MiB、每作业逻辑图片 32MiB、Raw 1MiB、活动与等待载荷
64MiB、最多 64 个未终结 operation、copies 为 1..100。resourceSnapshot 只作本地观察，
不预留空间；提交返回才是接纳权威。这些是源载荷预算，不是手机 RSS 上限。

当前 CPCL/ESC/POS 状态查询在 IO 前返回 UNSUPPORTED_OPERATION（1004），不产生 operation；
硬件显示未知。连接、SDK 工作和队列状态可以展示给客户，但不能据此推断机械打印完成。
