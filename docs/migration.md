# 迁移与版本政策

[返回指南](README.md) · 适用 SDK `0.2.0-preview.3`

## 当前预览版

分发版本为 `0.2.0-preview.3`，原生核心保持 `0.2.0`，C ABI 保持 `2/r10`。
此次增加 Android `FeatureSupport` / `MobilePrinter.feature(int)`，旧 `implementsFeature(int)`
保持原有实现查询语义。Swift 保留现有 `FeatureSupport`。打印、许可和队列行为不因文档或诊断
更新而改变，`hardware_verified=false`。

从 `0.2.0-preview.2` 升级时更新 Maven 精确依赖或 SwiftPM exact 版本，使用整个匹配的
SDK 包，再重建应用。新增 Android 方法需要本版 Java/JNI；仅更新示例源码不能让旧 AAR 获得新 API。
应用可先保留旧布尔查询，或读取三字段区分“实现”“设备支持”和“证据”。

## Android 旧包名

从 `0.2.0-preview.2` 起，公共 Java 包名由 `com.netum.printersdk` 迁移为
`com.netumscan.printersdk`，不提供旧包兼容层。Maven 坐标保持
`com.netumscan:printer-sdk-android`。

```java
// 旧代码
import com.netum.printersdk.MobilePrinter;

// 更新为
import com.netumscan.printersdk.MobilePrinter;
```

同步修改所有 SDK import、反射类名及应用自定义 R8 规则；不要复制旧 JNI 库到新 AAR。
Demo 代码包名为 `com.netumscan.printersdk.demo`，其 `applicationId` 保留
`com.netum.printersdk.demo`，这两个概念不同。安装同 applicationId 的 Full Demo 会更新既有应用；
需要并行安装时可在自己的示例副本更改 applicationId。

## iOS 标识

公共 `import PrinterSDK`、产品 `PrinterSDK` 和 C 模块 `CPrinterSDK` 不变。
framework Bundle ID 从 `0.2.0-preview.2` 起为 `com.netumscan.printersdk.core`。
它不是 App 的 Bundle ID，无需把客户 App 标识改成 SDK 标识。
公开示例使用独立 App Bundle ID，签名团队由客户配置，不包含开发者证书或账号。

## 升级、回退与兼容性

开发预览仍需按发布说明逐版评估，不能把 `preview` 版本或相同 ABI 当成生产稳定性承诺。
维护者通过编译器生成的 Java / Swift API 基线检查防止意外接口回退，并编译独立消费者验证接入，
但尚未承诺预览版的固定维护期限或长期支持窗口。

应用应锁定精确版本并保留可重现的依赖解析记录；同时核对 SDK 公开 commit 与制品 SHA-256。
已发布标签、附件和封存记录不覆盖，制品修复另起新版本。不要用新版本覆盖旧 URL 的内容，
或拼接不同版本的包装层、C 头文件、JNI 和原生库。

回退时恢复完整的旧依赖与锁文件并重建，确认 ABI 和应用集成测试；SDK 回退不撤销已发往
打印机的命令或设备中可能残留的数据，不会自动恢复打印任务。本次没有业务数据迁移。

示例修复可通过示例仓库新提交发布，SDK 版本可以继续锁定；Full Demo 从同一维护来源导出，
来源哈希记录在公开 `samples-manifest.json`。直接改导出文件会破坏同源完整性检查，
应在维护来源修改后通过验证流程重新同步。
