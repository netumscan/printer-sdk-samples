# 兼容性与诊断

[返回指南](README.md) · 适用 SDK `0.2.0-preview.3`

## 软件支持范围与证据

| 项目 | 当前范围 | 不能由此推出 |
| --- | --- | --- |
| Android 运行 | API 26+，Java 8，arm64-v8a / armeabi-v7a / x86_64 | 每款手机及 Android 版本都完成真机验收 |
| iOS 运行 | iOS 15+，Swift 5.9+，设备 arm64 / 模拟器 arm64、x86_64 | iOS 15 真机、蓝牙及全部最低内存机型完成验收 |
| 连接 | Android SPP / BLE；iOS BLE；前台使用 | USB、串口、iOS SPP 或后台连续打印已受支持 |
| 图像协议 | CPCL 标签、ESC/POS 小票，显式 profile | 任何同名打印机、固件或切刀都受支持 |
| 在线验证 | 包完整性、精确依赖、编译链接、Android R8/JNI、多架构及模拟器替身路径 | 纸面输出、无线故障、寿命或机械完成得到认证 |

正式产品 profile 目录目前为空，开发预设不属于认证型号目录。
同为 ABI 2/r10 只表示 ABI 契约范围，**不能替代精确分发版本、commit 和制品 SHA-256**。
支持性结论应同时记录所用软件、固件、手机、传输和纸张组合。

## 已知限制

所有本版发布记录保留 **`hardware_verified=false`**，本轮未开展新的手机安装或打印机验收。

- M90 取消任务后重新连接，首张可能出现短斜痕；已发送字节不能撤回，问题尚未闭环。
- M90 独立定位、FORM / 安全页高、圆角和纸面边缘，以及无线/缺纸/开盖等完整故障矩阵仍待验收。
- ESC/POS 尚未完成实际打印机硬件验收。标准后端状态读回和切刀未实现。
- 超大 PNG、极端宽高比、SwiftUI / GPU / Files 等完整 App 峰值及最低支持机型仍需覆盖。
  已有局部内存测量不是对任意输入的安全承诺。
- SDK 不自动续打、重放、恢复任务；不提供物理打印完成保证。前台策略不等于支持后台连续任务。

## 统一兼容诊断记录

Full Demo 在本机日志/导出中提供版本化兼容诊断记录，`schema_version=1`：

| 字段 | 含义 |
| --- | --- |
| `sdk.distribution_version` | 在线分发版本；本指南为 `0.2.0-preview.3` |
| `sdk.core_version` | 已验证分发资源声明的原生 SDK 版本，本版为 `0.2.0`；资源缺失时为 `null` |
| `sdk.abi_version` / `sdk.abi_revision` | 本版为 `2` / `10`；Swift 从运行时读取，Android 从已验证分发资源读取，资源缺失时为 `null` |
| `sdk.release_commit` | SDK 公开分发仓库 commit；不是私有开发仓库的基准 commit |
| `sdk.artifact_sha256` | Android AAR / iOS XCFramework ZIP 的 SHA-256 |
| `sdk.identity_source` / `sdk.abi_source` | 身份来源为 `distribution_manifest` 或 `null`；ABI 来源区分分发资源、`runtime` 或未知 |
| `demo.source_fingerprint` | 同源 Demo 的来源指纹，便于确定导出源码快照 |
| `demo.build_version` | 当前 Demo 构建身份 |
| `runtime.platform` / `runtime.os_version` / `runtime.transport` | `android` / `ios`、系统版本及显式选择的 `bluetooth_spp` / `bluetooth_ble`；未选择传输为 `null` |
| `known_limitations` | 本次构建已知限制的稳定代码列表；解释以本节和对应发布说明为准 |
| `hardware_verified` | 本版为 `false`，不可由编译、模拟器或连接成功自动提升 |

每次日志导出的第一行为 `compatibility_record=<单行 JSON>`，随后保留原有有界日志。
公开 Full Demo 从生成的分发身份资源读取锁定版本、公开 revision 和真实制品摘要；
本地开发构建缺失的分发身份使用 `null`，不推断或伪造摘要。Android 本地无资源时
SDK 身份、核心版本与 ABI 均未知；Swift 仍可读取原生实际 ABI，不据此猜测分发身份。
该记录不发送打印机查询，不自动采集固件或未知设备信息，也不包含蓝牙地址、打印内容和 Raw 载荷。

当前 `known_limitations` 包含 `hardware_acceptance_deferred`（硬件验收暂缓）、
`data_sent_is_not_printed`（传输完成不等于物理打印完成）和 `no_automatic_replay`（不自动重放）。
这份机器字段列表不替代上面的具体型号及故障限制。

日志留在应用本地，用户显式导出后才产生分享文件。排查时一并提供 operation 状态、
错误码、计数字段和复现顺序；固件和纸张信息由用户在脱敏后人工补充，不把缺失值写成 `N/A`。

## API 基线的用途

Java 与 Swift 公共 API 基线及历史消费者编译检查用于识别接口删除、签名变化和兼容性回退。
它们不验证特定硬件、BLE 无线环境或纸面质量。升级时仍应固定整包版本、核对制品身份，
运行自己的集成测试，并按实际使用场景开展硬件验收。
