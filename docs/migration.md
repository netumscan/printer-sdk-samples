# 迁移到最小移动API

[返回指南](README.md) · 适用版本 `0.3.0-preview.11`

从 `.10` 升级本版无需调整方法和参数，更新精确依赖后重新构建。UIImage 使用原生不可变图片持有语义，全部属性与像素读取在后台；自定义 provider 的底层数据须保持不变，Data 仍在返回前深复制。native 0.3.0／C ABI3/r2、旧制品、标签和冻结公共契约保持。

以下步骤用于迁移更早的旧移动 API：

1. 由业务所有者创建PrinterManager。使用DiscoveredDevice／DiscoveredPrinter连接，必要时显式指定已收录PrinterModel。删除ConnectionOptions、BleOptions及调用方自定通信参数。
2. 删除公开Profile、ImageSource、PixelImage、ImageLoader、ImageLoadContext、ImageRender、PrintOptions、PreparedPrint和preparation闭包。Android直接传Bitmap或PNG/JPEG byte[]；iOS直接传UIImage或PNG/JPEG Data，另传页面widthMm／heightMm。
3. 删除外部DPI、点密度、预算、原点、份数、阈值、抖动、反色、超时及FORM选项。SDK固定一份、等比缩小居中补白、不放大裁剪。需要新的产品行为时另行明确接口，不通过内部类型绕过。
4. 按实际耗材调用setPaperType：GAP_LABEL／BLACK_MARK／CONTINUOUS。定位下一张用locateNextLabel，设置和页面尺寸不互相替代。
5. 保留RequestId／RequestID和一次主线程回调。结果使用稳定错误码、completion与outputMayHaveOccurred；不再读取公开位掩码、队列、epoch、原生句柄或ABI字段。
6. Flutter统一传完整PNG/JPEG Uint8List，原生插件只转交，保留每个请求唯一回复与detach抑制迟到回调。旧rawRgba数据需要明确编码PNG后再传。

Bitmap、byte[]／Data 在复制期间保持稳定，返回后 SDK 持有独立副本；UIImage 由 SDK 强持有不可变对象，调用方可释放其引用，自定义 provider 底层数据须保持不变；取消后仍在退出的读者由SDK管理。App保留自己的文件授权、缓存、日志、UI绑定与后台策略。正常换机等待disconnect，独占所有者结束时close；不自动重连或重放。

完整公开参数见[Java API](android-java-api.md)与[Swift API](ios-swift-api.md)，示例见[接入](integration.md)。
