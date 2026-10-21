#!/usr/bin/env python3
# SPDX-License-Identifier: MIT
"""Build both Android consumers using separate exact SDK pins and run their gates.

Only explicitly selected Android emulators may be installed or cleared. The
historical Quick Start gate is retained. The current Full Demo uses its entire
instrumentation suite, including actual loaded JNI identity and memory IO.
"""
import argparse
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import uuid

sys.dont_write_bytecode = True
FULL = "com.netum.printersdk.demo"
QUICK = "com.netumscan.printersdk.samples.quickstart"
RUNNER = FULL + ".test/com.netumscan.printersdk.demo.DemoInstrumentation"
QUICK_RUNNER = QUICK + ".test/com.netumscan.printersdk.samples.runtime.QuickStartRuntimeGate"


def load(path, name):
    spec = importlib.util.spec_from_file_location(name, path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def write_json(path, value):
    path.write_text(json.dumps(value, indent=2, sort_keys=True) + "\n")


def command(arguments, log, timeout=1800):
    with log.open("w") as output:
        result = subprocess.run(list(map(str, arguments)), stdout=output, stderr=subprocess.STDOUT, timeout=timeout)
    if result.returncode:
        raise ValueError("Verification command failed; inspect " + log.name)
    return log.read_text()


def instrument_passed(output, marker):
    # adb itself often exits zero after an instrumentation failure.
    codes = re.findall(r"^INSTRUMENTATION_CODE: (-?[0-9]+)\s*$", output, re.M)
    if codes != ["-1"] or marker not in output or "INSTRUMENTATION_FAILED" in output:
        raise ValueError("Instrumentation did not complete successfully")


def validate_identity(output, lock):
    matches = re.findall(r"CHECK: runtime identity distribution=([^\s]+) native=([^\s]+) ABI=([0-9]+)/r([0-9]+) APK=([^\s]+)", output)
    expected = (lock["version"], lock["native_sdk_version"], "3", "2", lock["version"])
    if matches != [expected]:
        raise ValueError("Full Demo actual loaded JNI identity differs from its independent SDK pin")
    return {"distribution_version": expected[0], "native_version": expected[1], "abi_version": 3, "abi_revision": 2}


def inspect_app(android, module, lock, contract):
    # Preserve the historical DEX/native/license/bytecode checks, but bind each
    # invocation to that application's own lock rather than a global version.
    packaging = load(android / "scripts/verify-packaging.py", "packaging_" + module.replace("-", "_"))
    packaging.SDK_LOCK, packaging.VERSION = lock, lock["version"]
    packaging.AAR_SHA256 = lock["android"]["aar_sha256"]
    packaging.abi_identity, packaging.jni_methods = contract.abi_identity, contract.jni_methods
    packaging.jni_callbacks, packaging.packaging_checks = contract.jni_callbacks, contract.packaging_checks
    app_id = FULL if module == "full-demo" else QUICK
    report = packaging.inspect(android, module, app_id)
    report.update(version=lock["version"], coordinate=lock["android"]["coordinate"], c_abi=lock["c_abi"],
                  sdk_release_commit=lock["android"]["revision"])
    return report


def verify(android, output, serial, adb_path=None):
    if not re.fullmatch(r"emulator-[0-9]+", serial):
        raise ValueError("Select --emulator-serial emulator-N; physical devices are refused")
    root = android.parent
    sys.path.insert(0, str(root / "tools"))
    contract = load(root / "tools/android_demo_contract.py", "mixed_demo_contract")
    checker = load(root / "tools/check-samples.py", "mixed_source_checker")
    before = checker.check(root)
    source_manifest_sha256 = digest(root / "samples-manifest.json")
    output.mkdir(parents=True, exist_ok=True)
    if any((output / name).exists() for name in ("validation.json", "runtime-validation.json", "gradle.log", "adb.log")):
        raise ValueError("Verification evidence already exists; choose a fresh output directory")
    write_json(output / "source-check.json", before)
    locks = {module: contract.app_lock(root, module) for module in ("quick-start", "full-demo")}
    gradle = os.environ.get("GRADLE") or str(android / "gradlew")
    bootstrap = command([gradle, "--version"], output / "gradle-bootstrap.log", timeout=180)
    if not re.search(r"^Gradle 8\.13$", bootstrap, re.M):
        raise ValueError("Gradle 8.13 is required")
    write_json(output / "gradle-tool.json", {"selection": "explicit_override" if os.environ.get("GRADLE") else "pinned_wrapper",
        "gradle_version": "8.13", "wrapper_jar_sha256": "81a82aaea5abcc8ff68b3dfcb58b3c3c429378efd98e7433460610fecd7ae45f",
        "distribution_sha256": "20f1b1176237254a6fc204d8434196fa11a4cfb387567519c61556e8710aed78"})
    tasks = [":" + module + ":" + task for module in locks for task in (
        "assembleDebug", "assembleDebugAndroidTest", "assembleRelease", "lintRelease", "copyResolvedSdk")]
    command([gradle, "-p", android, *tasks, "--refresh-dependencies", "--no-build-cache", "--max-workers=2", "--console", "plain"],
            output / "gradle.log")
    applications = {module: inspect_app(android, module, lock, contract) for module, lock in locks.items()}
    packaging = {"schema_version": 2, "success": False, "hardware_verified": False,
        "validation_scope": "mixed_exact_sdk_pins", "source_manifest_sha256": source_manifest_sha256,
        "applications": applications, "emulator_runtime_verified": False,
        "runtime_compatibility_verified": False,
        "runtime_compatibility_scope": "separate_sdk_pins_and_complete_current_demo",
        "historical_quick_start_gate_retained": True, "historical_full_demo_replaced": True}
    write_json(output / "validation.json", packaging)
    runtime = load(android / "scripts/verify-runtime.py", "historical_runtime_helpers")
    sdk_home = Path(os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT", ""))
    adb_path = Path(adb_path) if adb_path else sdk_home / "platform-tools/adb"
    if not adb_path.is_file():
        raise ValueError("Android SDK platform-tools/adb is required")
    adb = runtime.Adb(adb_path, serial, output)
    runtime.validate_device(serial, adb.run("shell", "getprop", "ro.kernel.qemu"),
                            adb.run("shell", "getprop", "sys.boot_completed"))
    abi = adb.run("shell", "getprop", "ro.product.cpu.abi")
    if abi not in {"arm64-v8a", "armeabi-v7a", "x86_64"}:
        raise ValueError("Emulator ABI is not included in the SDK AARs")
    nonce = uuid.uuid4().hex
    apks = {"quick-start": android / "quick-start/build/outputs/apk/debug/quick-start-debug.apk",
        "full-demo": android / "full-demo/build/outputs/apk/debug/full-demo-debug.apk",
        "quick-start-test": android / "quick-start/build/outputs/apk/androidTest/debug/quick-start-debug-androidTest.apk",
        "full-demo-test": android / "full-demo/build/outputs/apk/androidTest/debug/full-demo-debug-androidTest.apk"}
    apk_hashes = {name: digest(path) for name, path in apks.items()}
    for name in locks:
        if applications[name]["debug_apk_sha256"] != apk_hashes[name]:
            raise ValueError("Runtime APK differs from packaging-verified APK")
    for apk in apks.values():
        adb.run("install", "-r", "-t", str(apk), timeout=180)
    for application in (QUICK, FULL):
        if adb.run("shell", "pm", "clear", application) != "Success":
            raise ValueError("Could not clear sample data on the selected emulator")
    adb.run("shell", "input", "keyevent", "KEYCODE_WAKEUP")
    adb.run("shell", "wm", "dismiss-keyguard")
    quick_output = adb.run("shell", "am", "instrument", "-w", "-r", "-e", "nonce", nonce, QUICK_RUNNER, timeout=180)
    instrument_passed(quick_output, "offline runtime passed; nonce=" + nonce)
    quick_raw = adb.run("exec-out", "run-as", QUICK, "cat", "files/quick-runtime-gate.json")
    expected_quick = {"schema_version": 1, "success": True, "nonce": nonce, "offline_launch": True,
        "printer_created": False, "operation_created": False, "bluetooth_runtime_permissions_ungranted": True, "hardware_verified": False}
    if json.loads(quick_raw) != expected_quick:
        raise ValueError("Historical Quick Start runtime evidence is stale or incomplete")
    (output / "quick-start-runtime.json").write_text(quick_raw + "\n")
    adb.run("shell", "am", "force-stop", QUICK)
    full_output = adb.run("shell", "am", "instrument", "-w", "-r", RUNNER, timeout=600)
    instrument_passed(full_output, "PASS: Full Demo image source/import/EXIF/limits/reading leases")
    identity = validate_identity(full_output, locks["full-demo"])
    # Every complete-suite run verifies no unsolicited connection/replay, real
    # Manager/JNI IO through memory channels, imports, recreation and cleanup.
    write_json(output / "instrumentation-result.json", {"schema_version": 1, "success": True,
        "nonce": nonce, "validation_scope": "complete_current_android_full_demo_instrumentation",
        "sdk_runtime_identity": identity, "stream": full_output, "hardware_verified": False})
    adb.run("shell", "am", "force-stop", FULL)
    if apk_hashes != {name: digest(path) for name, path in apks.items()}:
        raise ValueError("APK bytes changed during runtime verification")
    if digest(root / "samples-manifest.json") != source_manifest_sha256 or checker.check(root) != before:
        raise ValueError("Sample sources changed during verification")
    evidence = {name: digest(output / name) for name in ("instrumentation-result.json", "quick-start-runtime.json", "adb.log")}
    report = {"schema_version": 2, "success": True, "hardware_verified": False,
        "emulator_only": True, "physical_device_installed": False, "emulator_serial": serial,
        "process_abi": abi, "nonce": nonce, "source_manifest_sha256": source_manifest_sha256,
        "apks_sha256": apk_hashes, "evidence_sha256": evidence,
        "applications": {"quick-start": {"sdk_version": locks["quick-start"]["version"],
            "aar_sha256": locks["quick-start"]["android"]["aar_sha256"], "offline_runtime_verified": True,
            "gate": "retained_historical_quick_start"},
            "full-demo": {"sdk_version": locks["full-demo"]["version"],
                "aar_sha256": locks["full-demo"]["android"]["aar_sha256"], "sdk_runtime_identity": identity,
                "complete_instrumentation_verified": True, "gate": "current_DemoInstrumentation"}}}
    write_json(output / "runtime-validation.json", report)
    packaging.update(success=True, emulator_runtime_verified=True, runtime_compatibility_verified=True, runtime=report,
        runtime_validation_sha256=digest(output / "runtime-validation.json"))
    write_json(output / "validation.json", packaging)
    print("Verified separate online SDK pins, both R8/native packages, historical Quick Start and complete current Full Demo instrumentation.")
    return packaging


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--android-root", type=Path, required=True)
    parser.add_argument("--output", type=Path)
    parser.add_argument("--emulator-serial", required=True)
    parser.add_argument("--adb", type=Path)
    args = parser.parse_args()
    android = args.android_root.resolve()
    try:
        verify(android, (args.output or android / "build/verification").resolve(), args.emulator_serial, args.adb)
    except (ValueError, KeyError, OSError, subprocess.SubprocessError) as error:
        raise SystemExit(str(error)) from None


if __name__ == "__main__":
    main()
