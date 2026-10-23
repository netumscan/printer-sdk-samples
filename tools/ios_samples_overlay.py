# SPDX-License-Identifier: MIT
"""Reviewed independent iOS overlays retain immutable .11 Android provenance."""
import hashlib
import json
from pathlib import Path
import types
from sdk_contract import platform_lock, validate_ios_overlay_lock

BASE_VERSION = "0.3.0-preview.11"
# Historical .15 helpers retain their identity; exports select ios_lock["version"].
IOS_VERSION = "0.3.0-preview.15"
BASE_MANIFEST = "ios-overlay-base-manifest.json"
BASE_MANIFEST_SHA256 = "53452a3e654dbfd4237ab9e9a9985cbd1eb638b39a3f58729789d8e818c65352"
LOCK = "ios-sdk-lock.json"
CHANGED_FILES = {"README.md", "VERIFICATION.md", "flutter/README.md", "shared-app/README.md", LOCK, BASE_MANIFEST,
                 "samples-manifest.json", "tools/check-samples.py", "tools/check-flutter-demo.py",
                 "tools/sdk_contract.py", "tools/ios_samples_overlay.py", "tools/check-ios-api-reference.py"}


def sha(value):
    return hashlib.sha256(value).hexdigest()


def may_change(name):
    return name in CHANGED_FILES or name.startswith(("ios/", "flutter/ios/", "shared-app/ios/"))


def ios_reference_document(value, version=None):
    """Relocate only two shared-guide links; compiler signatures and prose remain exact."""
    value = value.replace(b"](api-lifecycle.md)", "](SDK_GUIDE.md#生命周期与状态)".encode()).replace(
        b"](errors-troubleshooting.md)", "](SDK_GUIDE.md#软件验证与许可)".encode())
    if version == "0.3.0-preview.16":
        # The default reference checker targets the historical root .11 bundle.
        # .16 names the aggregate gate that also checks this scoped iOS bundle.
        value = value.replace(b"python3 tools/check-api-reference.py", b"python3 tools/check-samples.py")
    return value


def baseline(root):
    path = Path(root) / BASE_MANIFEST
    if path.is_symlink() or sha(path.read_bytes()) != BASE_MANIFEST_SHA256:
        raise ValueError("iOS overlay baseline differs from the immutable reviewed .11 snapshot")
    value = json.loads(path.read_text())
    if value.get("schema") != 1 or value.get("sdk_version") != BASE_VERSION:
        raise ValueError("Invalid iOS overlay baseline")
    return value


def validate_overlay(root, manifest, actual):
    node = manifest.get("ios_overlay")
    if (not isinstance(node, dict) or set(node) != {"schema", "baseline_version", "baseline_manifest",
            "baseline_manifest_sha256", "sdk_lock", "sdk_lock_sha256", "swift_module"}
            or node.get("schema") != 1 or node.get("baseline_version") != BASE_VERSION
            or node.get("baseline_manifest") != BASE_MANIFEST
            or node.get("baseline_manifest_sha256") != BASE_MANIFEST_SHA256
            or node.get("sdk_lock") != LOCK or node.get("swift_module") != "XLPrinterSDK"
            or node.get("sdk_lock_sha256") != actual.get(LOCK)):
        raise ValueError("Invalid iOS overlay metadata")
    old = baseline(root)
    retained = {name: checksum for name, checksum in old["files_sha256"].items() if not may_change(name)}
    if any(actual.get(name) != checksum for name, checksum in retained.items()):
        raise ValueError("iOS overlay changed a retained Android, Dart, historical lock or document file")
    if any(name not in old["files_sha256"] and not may_change(name) for name in actual):
        raise ValueError("iOS overlay added a file outside its reviewed scope")
    lock = validate_ios_overlay_lock(platform_lock(root, "ios"))
    if json.loads((root / "sdk-lock.json").read_text())["version"] != BASE_VERSION:
        raise ValueError("iOS overlay must preserve the root .11 SDK lock")
    for name in actual:
        if name.startswith(("ios/", "flutter/ios/", "shared-app/ios/")) and name.endswith(".swift"):
            text = (root / name).read_text()
            if "import PrinterSDK" in text:
                raise ValueError("iOS overlay consumer retains the old Swift module: " + name)
    for name in ("ios/PrinterSamples.xcodeproj/project.pbxproj", "flutter/ios/Runner.xcodeproj/project.pbxproj"):
        text = (root / name).read_text()
        if "productName = XLPrinterSDK;" not in text or "productName = PrinterSDK;" in text:
            raise ValueError("iOS overlay project must select the XLPrinterSDK product")
    tool = root / "tools/check-ios-api-reference.py"
    gate = types.ModuleType("ios_overlay_api_reference")
    gate.__file__ = str(tool)
    exec(compile(tool.read_bytes(), str(tool), "exec"), gate.__dict__)
    bundle = gate.loads((root / "ios/api-reference.json").read_text())
    if bundle.get("sdk_version") != lock["version"]:
        raise ValueError("iOS API reference differs from its independent SDK lock")
    gate.validate_bundle(bundle)
    if ios_reference_document(gate.render_documents(bundle)["docs/ios-swift-api.md"], lock["version"]) != (root / "ios/ios-swift-api.md").read_bytes():
        raise ValueError("iOS API reference differs from the reviewed compiler contract")
    fingerprint = sha(json.dumps(old["canonical_full_demos"], sort_keys=True).encode())
    return {"android_source_fingerprint": fingerprint}
