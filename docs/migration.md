# 从 ABI2 迁移

[返回指南](README.md) · 适用发布候选 `0.3.0-preview.2`

本次是 native `0.3.0`、ABI `3/r1` 的主版本迁移。先完整替换 SDK 组合并重新编译，
不要混用 ABI2 的公共头、库、平台包装或旧句柄。旧发布制品和历史证明继续保留。

1. 将带 Profile 的设备创建改为独立通信设备创建；连接成功后，在每次提交时传入 Profile。
2. 使用不可变 PrinterImage/PrintJob 和 submit；便捷 GRAY8 提交仍管理临时 image/job。
   C 图像变为 opaque，通过 image_create_copy 创建；job_create 一次复制 descriptor 元数据
   并保留图像存储，不再次复制像素。
3. 将 App 自行轮询替换为 PrintOperation.onComplete 和 MobilePrinter.observeState。
   平台内部共享按需观察本地快照；C 消费者仍使用 snapshot/result getter。
4. 分开完成订阅注销、operation.cancel 和 operation.close。页面只停止自己的通知，
   共享设备及 Manager 由应用级所有者回收。
5. 展示连接、SDK 工作和队列快照；未验证硬件继续显示未知，不将 DATA_SENT 显示为机械完成。

C Raw/介质/走纸/查询入口合并为固定 tag 的 typed execute，所有 IO 仍经过同一设备队列。
旧 builder、空产品目录及 ABI2 专用入口没有生产 alias。具体参数与平台声明见
[Java API](android-java-api.md) 和 [Swift API](ios-swift-api.md)，完整会话代码见[接入](integration.md)。
