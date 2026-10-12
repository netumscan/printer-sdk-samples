# Gradle Wrapper 8.13

`gradlew`、`gradlew.bat` 与 `gradle-wrapper.jar` 由官方 Gradle 8.13 的 `wrapper` 任务生成，保留原始内容和版权头。JAR 的 Apache-2.0 文本也原样保留在本目录的 [LICENSE](LICENSE)；这些第三方构建工具不适用项目示例的 MIT 授权。

上游依据：[Gradle Wrapper 文档](https://docs.gradle.org/8.13/userguide/gradle_wrapper.html)、[官方分发及 Wrapper JAR 校验和](https://gradle.org/release-checksums/)。

| 文件 | 固定 SHA-256 |
| --- | --- |
| Gradle 8.13 binary ZIP | `20f1b1176237254a6fc204d8434196fa11a4cfb387567519c61556e8710aed78` |
| `gradle-wrapper.jar` | `81a82aaea5abcc8ff68b3dfcb58b3c3c429378efd98e7433460610fecd7ae45f` |

`gradle-wrapper.properties` 仅使用官方 `https://services.gradle.org/distributions/gradle-8.13-bin.zip`，启用固定 ZIP checksum、URL 校验及 10 秒网络超时。默认使用标准 `GRADLE_USER_HOME/wrapper/dists` 缓存，不配置镜像或全局 Gradle 回退。

`python3 tools/check-samples.py` 从仓库根目录检查 JAR、脚本、许可证和完整配置，且仅允许这一份固定校验和的构建工具 JAR；SDK 和签名二进制仍被禁止。POSIX 下 `android/gradlew` 必须为 `0755`；Windows `.bat` 文件保持普通文件。根目录 `.gitattributes` 仅对这三个上游 Wrapper 文件禁止 Git 行尾转换，保证 Windows 与 POSIX 检出的脚本／JAR 字节一致，不影响 Demo 源码。
