# SPDX-License-Identifier: MIT
"""Exact Android Full Demo pin; historical applications keep the root lock."""
import json
from pathlib import Path
import re

README_MARKER = "\n<!-- current-android-full-demo -->\n"
LEGACY_README_START = "\n<!-- legacy-readme-start -->\n"
LEGACY_README_END = "\n<!-- legacy-readme-end -->\n"
FULL = "android/full-demo/"
COMMON = {"README.md", "samples-manifest.json", "tools/check-samples.py",
          "tools/check-legacy-samples.py", "tools/check-android-demo-overlay.py",
          "tools/android_demo_contract.py", "tools/collect-evidence.py", "tools/collect-legacy-evidence.py",
          "android/verify.sh", "android/scripts/verify-mixed.py"}


def legacy_readme(value):
    """Extract exact legacy bytes from the bounded section or the old tail note."""
    marker, start, end = (item.encode() for item in (README_MARKER, LEGACY_README_START, LEGACY_README_END))
    if start in value or end in value:
        if value.count(start) != 1 or value.count(end) != 1 or value.count(marker) != 1:
            raise ValueError("Invalid bounded historical README section")
        prefix, remainder = value.split(start, 1)
        if end in prefix or marker not in prefix:
            raise ValueError("Invalid historical README marker order")
        historical, suffix = remainder.split(end, 1)
        if marker in historical or marker in suffix:
            raise ValueError("Ambiguous historical README section")
        return historical
    if value.count(marker) > 1:
        raise ValueError("Ambiguous current Android README note")
    return value.split(marker, 1)[0]


def render_readme(historical, full_lock, legacy_lock):
    version, old_version = full_lock["version"], legacy_lock["version"]
    current = ("# printer-sdk-samples\n" + README_MARKER +
        "\n当前 Android Full Demo 使用 SDK **" + version + "**；Android Quick Start 与两个 iOS 示例继续使用 **" + old_version + "**。\n\n"
        "| 应用 | SDK 版本 | C ABI | 精确依赖锁 | 入口 |\n"
        "| --- | --- | --- | --- | --- |\n"
        "| Android Full Demo | `" + version + "` | `" + full_lock["c_abi"] + "` | [独立锁](android/full-demo/sdk-lock.json) | [当前完整 Demo](android/full-demo/README.md) |\n"
        "| Android Quick Start | `" + old_version + "` | `" + legacy_lock["c_abi"] + "` | [历史锁](sdk-lock.json) | [Quick Start](android/quick-start/README.md) |\n"
        "| iOS Quick Start | `" + old_version + "` | `" + legacy_lock["c_abi"] + "` | [历史锁](sdk-lock.json) | [Quick Start](ios/quick-start/README.md) |\n"
        "| iOS Full Demo | `" + old_version + "` | `" + legacy_lock["c_abi"] + "` | [历史锁](sdk-lock.json) | [Full Demo](ios/full-demo/README.md) |\n\n"
        "Android Full Demo 的 Maven 坐标为 `" + full_lock["android"]["coordinate"] + "`。根目录 `sdk-lock.json` 只描述其余三个历史示例，"
        "不代表当前 Android Full Demo。\n\n"
        "当前 Android 的 Manager、Profile、图像与生命周期调用请从 [Full Demo 说明](android/full-demo/README.md) 开始。"
        "[客户指南](docs/README.md)、[共享 APP 示例](shared-app/README.md)及下方历史说明对应旧 SDK API；"
        "当前 Android 接口以 [Android SDK 发布文档](https://github.com/netumscan/printer-sdk-android)为准。\n\n"
        "Android 构建、lint、逐应用 AAR 校验与运行门禁见 [Full Demo 验证说明](android/full-demo/README.md)。"
        "来源记录位于 `samples-manifest.json`：当前 Android 为 `android_full_demo`，历史快照保留在 `legacy_manifest`。\n\n"
        "<details>\n<summary>历史 " + old_version + " 说明（不适用于当前 Android Full Demo）</summary>\n")
    return current.encode() + LEGACY_README_START.encode() + historical + LEGACY_README_END.encode() + b"\n</details>\n"


def validate_full_lock(lock):
    version = lock.get("version", "")
    sdk = lock.get("android", {})
    if (lock.get("schema", 1) != 1 or not re.fullmatch(r"[0-9]+\.[0-9]+\.[0-9]+(?:-[A-Za-z0-9]+(?:\.[A-Za-z0-9]+)*)?", version)
            or version.endswith("-dev") or lock.get("native_sdk_version") != "0.3.0"
            or lock.get("c_abi") != "3/r2" or lock.get("hardware_verified") is not False
            or sdk.get("coordinate") != "com.netumscan:printer-sdk-android:" + version
            or sdk.get("repository") != "https://repo.maven.apache.org/maven2"
            or not re.fullmatch(r"[0-9a-f]{64}", sdk.get("aar_sha256", ""))
            or not re.fullmatch(r"[0-9a-f]{40}", sdk.get("revision", ""))
            or sdk.get("license") != "https://github.com/netumscan/printer-sdk-android/blob/v" + version + "/LICENSE"):
        raise ValueError("Invalid independent Android Full Demo SDK lock")
    allowed = {"schema", "version", "native_sdk_version", "c_abi", "hardware_verified", "android"}
    if set(lock) - allowed or set(sdk) != {"coordinate", "repository", "aar_sha256", "revision", "license"}:
        raise ValueError("Unexpected independent SDK lock fields")
    return lock


def app_lock(root, module):
    if module == "full-demo":
        return validate_full_lock(json.loads((root / FULL / "sdk-lock.json").read_text()))
    if module == "quick-start":
        return json.loads((root / "sdk-lock.json").read_text())
    raise ValueError("Unknown Android sample module")


def abi_identity(lock):
    match = re.fullmatch(r"([1-9][0-9]*)/r([1-9][0-9]*)", lock.get("c_abi", ""))
    if match is None or tuple(map(int, match.groups())) not in {(2, 10), (3, 1), (3, 2)}:
        raise ValueError("Unsupported exact samples C ABI identity")
    return tuple(map(int, match.groups()))


def jni_methods(lock):
    if abi_identity(lock)[0] != 3:
        raise ValueError("Mixed Android samples require ABI3 historical and current pins")
    return {"MobilePrinter": ("nCancelled", "nCreate", "nConnect", "nDisconnect", "nDestroy",
        "nStateSnapshot", "nCapabilities", "nDiagnostic", "nResources", "nSdkInfo", "nObserverEpoch",
        "nObserverLost", "nObserverRelease", "nSubmit", "nExecute"),
        "PrintOperation": ("nSnapshot", "nResult", "nCancel", "nRelease"),
        "PrinterImage": ("nCreate", "nRelease"), "PrintJob": ("nCreate", "nRelease")}


def jni_callbacks(lock):
    abi_identity(lock)
    return {"open": "(JI)I", "write": "([BJI)I", "isOpen": "()Z", "close": "()V", "read": "([BJI)I",
            "errorDiagnostic": "()[J", "attachObserver": "(J)V", "disposeObserver": "()V"}


def packaging_checks(lock):
    jni_methods(lock)
    return {"online_aar_matches_pin", "java8_bytecode", "debug_native_payload_matches", "r8_jni_23_methods",
            "r8_jni_8_callbacks", "three_abi_payload_matches", "sdk_license_notices_preserved", "application_id", "min_sdk_21"}
