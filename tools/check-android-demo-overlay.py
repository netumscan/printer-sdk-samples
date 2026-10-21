#!/usr/bin/env python3
# SPDX-License-Identifier: MIT
"""Preserve historical consumers and verify independently pinned Android overlays."""
import hashlib
import importlib.util
import json
from pathlib import Path
import re
import sys
sys.dont_write_bytecode = True


def load(path, name):
    spec = importlib.util.spec_from_file_location(name, path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def sha(value):
    return hashlib.sha256(value).hexdigest()


def check(root):
    root = root.resolve()
    tools = root / "tools"
    sys.path.insert(0, str(tools))
    contract = load(tools / "android_demo_contract.py", "checked_demo_contract")
    legacy_gate = load(tools / "check-legacy-samples.py", "checked_legacy_gate")
    manifest = json.loads((root / "samples-manifest.json").read_text())
    if manifest.get("schema") != 2 or manifest.get("hardware_verified") is not False:
        raise ValueError("Invalid Android overlay manifest")
    actual = legacy_gate.inventory(root)
    if actual != manifest["files_sha256"]:
        raise ValueError("Sample source inventory differs from overlay manifest")
    for name, digest in actual.items():
        path = Path(name)
        if path.is_absolute() or ".." in path.parts or path.as_posix() != name or not re.fullmatch(r"[0-9a-f]{64}", digest):
            raise ValueError("Invalid overlay inventory path or hash")
    legacy = manifest["legacy_manifest"]
    if legacy.get("schema") != 1 or legacy.get("hardware_verified") is not False:
        raise ValueError("Invalid retained legacy manifest")
    quick = manifest.get("android_quick_start")
    quick_current = "android_quick_start" in manifest
    if quick_current and not isinstance(quick, dict):
        raise ValueError("Invalid current Quick Start manifest")
    current_prefixes = (contract.FULL, contract.QUICK) if quick_current else (contract.FULL,)
    retained = {name: digest for name, digest in legacy["files_sha256"].items()
                if not name.startswith(current_prefixes) and name not in contract.COMMON}
    if any(actual.get(name) != digest for name, digest in retained.items()):
        raise ValueError("Historical Quick Start, iOS, API references or root SDK lock changed")
    extras = set(actual) - set(retained)
    if any(not name.startswith(current_prefixes) and name not in contract.COMMON for name in extras):
        raise ValueError("Android overlay modifies an unauthorized public path")
    if actual.get("tools/check-legacy-samples.py") != legacy["files_sha256"]["tools/check-samples.py"]:
        raise ValueError("Historical source checker was changed")
    if actual.get("tools/collect-legacy-evidence.py") != legacy["files_sha256"]["tools/collect-evidence.py"]:
        raise ValueError("Historical diagnostic collector was changed")
    readme = (root / "README.md").read_bytes()
    if readme.count(contract.README_MARKER.encode()) != 1 or sha(contract.legacy_readme(readme)) != legacy["files_sha256"]["README.md"]:
        raise ValueError("Historical README was changed outside its preserved section")
    legacy_gate.check_wrapper(root, actual, manifest)
    root_lock = json.loads((root / "sdk-lock.json").read_text())
    legacy_gate.check_ios_dependencies(root, root_lock)
    # Old iOS diagnostic identity retains its original, cross-platform basis.
    ios_identity = "ios/full-demo/printer-sdk-samples-build.json"
    if actual.get(ios_identity) != legacy["generated_build_identity"][ios_identity]:
        raise ValueError("Historical iOS diagnostic identity changed")
    full_lock = contract.app_lock(root, "full-demo")
    if full_lock["version"] == root_lock["version"]:
        raise ValueError("The current Full Demo must not reuse the historical SDK pin")
    full = manifest["android_full_demo"]
    if full.get("sdk_lock") != contract.FULL + "sdk-lock.json":
        raise ValueError("Full Demo lock path differs")
    canonical = full["canonical_sources"]
    expected_sources = {"platform/android/demo/src/main": contract.FULL + "src/main/",
                        "platform/android/demo/src/androidTest": contract.FULL + "src/androidTest/"}
    generated = contract.FULL + "src/main/assets/printer-sdk-samples-build.json"
    if set(canonical) != set(expected_sources):
        raise ValueError("Missing Full Demo main or instrumentation provenance")
    for source, target in expected_sources.items():
        source_files = {name: digest for name, digest in actual.items() if name.startswith(target) and name != generated}
        if not source_files or canonical[source] != {"target": target, "files_sha256": source_files}:
            raise ValueError("Current Full Demo provenance differs")
    fingerprint = sha(json.dumps(canonical, sort_keys=True).encode())
    if full.get("source_fingerprint") != fingerprint:
        raise ValueError("Full Demo source fingerprint differs")
    identity = json.loads((root / generated).read_text())
    expected_identity = {"sdk_distribution_version": full_lock["version"], "sdk_core_version": full_lock["native_sdk_version"],
        "sdk_abi_version": 3, "sdk_abi_revision": 2, "sdk_release_commit": full_lock["android"]["revision"],
        "sdk_artifact_sha256": full_lock["android"]["aar_sha256"], "demo_source_fingerprint": fingerprint,
        "demo_build_version": full_lock["version"] + "+samples." + fingerprint[:12]}
    if identity != {"schema_version": 1, "identity": expected_identity}:
        raise ValueError("Current Full Demo diagnostic identity differs from its exact SDK pin")
    build = (root / "android/full-demo/build.gradle.kts").read_text()
    if ('implementation("' + full_lock["android"]["coordinate"] + '")' not in build
            or 'versionName = "' + full_lock["version"] + '"' not in build
            or 'testInstrumentationRunner = "com.netumscan.printersdk.demo.DemoInstrumentation"' not in build):
        raise ValueError("Full Demo must use its independent exact Maven pin and complete instrumentation")
    quick_lock = None
    if quick_current:
        if quick.get("sdk_lock") != contract.QUICK + "sdk-lock.json":
            raise ValueError("Quick Start lock path differs")
        quick_lock = contract.app_lock(root, "quick-start")
        if quick_lock["version"] == root_lock["version"]:
            raise ValueError("The current Quick Start must not reuse the historical SDK pin")
        origin = "samples/android/quick-start/src/main"
        target = contract.QUICK + "src/main/"
        source_files = {name: digest for name, digest in actual.items() if name.startswith(target)}
        canonical = {origin: {"target": target, "files_sha256": source_files}}
        if not source_files or quick.get("canonical_sources") != canonical:
            raise ValueError("Current Quick Start provenance differs")
        if quick.get("source_fingerprint") != sha(json.dumps(canonical, sort_keys=True).encode()):
            raise ValueError("Quick Start source fingerprint differs")
        build = (root / contract.QUICK / "build.gradle.kts").read_text()
        if ('implementation("' + quick_lock["android"]["coordinate"] + '")' not in build
                or 'versionName = "' + quick_lock["version"] + '"' not in build
                or 'testInstrumentationRunner = "com.netumscan.printersdk.samples.runtime.QuickStartRuntimeGate"' not in build):
            raise ValueError("Quick Start must use its independent exact Maven pin and current instrumentation")
    for tool in ("tools/check-api-reference.py", "tools/check-api-history.py"):
        load(root / tool, "retained_" + Path(tool).stem.replace("-", "_")).check(root)
    prohibited = re.compile(r"/Users/[A-Za-z0-9_.-]+/|/home/[A-Za-z0-9_.-]+/|mavenLocal\s*\(|"
        r"implementation\s*\(\s*project\s*\(|-----BEGIN (?:RSA |EC |OPENSSH |PGP )?PRIVATE KEY|"
        r"DEVELOPMENT_TEAM\s*=\s*[A-Z0-9]+|PROVISIONING_PROFILE(?:_SPECIFIER)?\s*=\s*[^;\s]+")
    for name in extras:
        path = root / name
        if path.suffix.lower() in {".aar", ".jar", ".so", ".a", ".apk", ".aab", ".p12", ".jks", ".keystore"}:
            raise ValueError("Binary or signing material entered Android overlay")
        value = path.read_text()
        if name not in {"tools/check-android-demo-overlay.py", "tools/check-legacy-samples.py"} and (prohibited.search(value) or re.search(r"@[A-Z][A-Z0-9_]*@", value)):
            raise ValueError("Private value or unresolved template in Android overlay: " + name)
        if path.suffix == ".md":
            for link in re.findall(r"(?<!!)\[[^\]\n]+\]\(([^)\n]+)\)", value):
                if "://" in link or link.startswith(("#", "mailto:")):
                    continue
                target = link.split("#", 1)[0]
                linked = (path.parent / target).resolve()
                if target and (not linked.is_relative_to(root) or not linked.exists()):
                    raise ValueError("Broken current Android documentation link")
    report = {"verified": True, "schema": 2, "files_verified": len(actual), "hardware_verified": False,
            "android_full_demo_sdk_version": full_lock["version"], "historical_sdk_version": root_lock["version"],
            "historical_files_preserved": len(retained), "legacy_full_demo_replaced": True}
    if quick_lock is not None:
        report.update(android_quick_start_sdk_version=quick_lock["version"], legacy_quick_start_replaced=True)
    return report
