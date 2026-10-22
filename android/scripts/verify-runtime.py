#!/usr/bin/env python3
# SPDX-License-Identifier: MIT
"""Run the exact online SDK on an explicitly selected emulator; physical devices are rejected."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import uuid
sys.dont_write_bytecode = True
sys.path.insert(0, str(Path(__file__).resolve().parents[2] / "tools"))
from sdk_contract import MINIMAL_API_VERSIONS, abi_identity, native_evidence

sys.dont_write_bytecode = True
FULL = "com.netum.printersdk.demo"
QUICK = "com.netumscan.printersdk.samples.quickstart"
QUICK_RUNNER = QUICK + ".test/com.netumscan.printersdk.samples.runtime.QuickStartRuntimeGate"
RUNNER = FULL + ".test/com.netumscan.printersdk.samples.runtime.RuntimeGate"
CHECKS = {"emulator_only", "fresh_app_data", "quick_start_offline_launch", "full_demo_offline_launch",
          "jni_loaded", "abi_handshake", "partial_writes", "raw_order", "fresh_compatibility_journal",
          "exact_sdk_identity", "bluetooth_permissions_ungranted"}
MINIMAL_CHECKS = (CHECKS - {"fresh_compatibility_journal"}) | {"manager_cleanup"}


def minimal_api(lock):
    return lock.get("version") in MINIMAL_API_VERSIONS


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def validate_device(serial, qemu, booted):
    if not re.fullmatch(r"emulator-[0-9]+", serial) or qemu != "1" or booted != "1":
        raise ValueError("An explicitly selected, fully booted Android emulator is required; physical devices are refused")


def validate_manager_runtime(value, nonce, identity, *, quick=False):
    """Local candidate evidence is separate from the published compatibility journal."""
    if (identity.get("sdk_abi_version"), identity.get("sdk_abi_revision")) != (3, 2):
        raise ValueError("Manager local runtime requires exact ABI 3/r2")
    if (value.get("schema_version") != 1 or value.get("success") is not True or value.get("nonce") != nonce
            or value.get("hardware_verified") is not False or value.get("manager_cleanup_verified") is not True
            or value.get("bluetooth_runtime_permissions_ungranted") is not True
            or value.get("validation_scope") != "current_manager_offline_lifecycle_smoke"):
        raise ValueError("Manager offline runtime scope, nonce or cleanup evidence differs")
    if quick:
        expected = {"distribution_version": identity["sdk_distribution_version"], "native_version": identity["sdk_core_version"],
                    "abi_version": 3, "abi_revision": 2}
        if (value.get("offline_launch") is not True or value.get("printer_created") is not False
                or value.get("operation_created") is not False or value.get("sdk") != expected):
            raise ValueError("Quick Start runtime identity or offline behavior differs")
    elif (value.get("full_demo_offline") is not True or value.get("native") != native_evidence(3, 2,
            identity["sdk_core_version"], identity["sdk_distribution_version"])
            or type(value.get("api_level")) is not int or value["api_level"] < 21
            or value.get("process_abi") not in {"arm64-v8a", "armeabi-v7a", "x86_64"}):
        raise ValueError("Full Demo Manager/JNI/partial-write/once runtime evidence differs")
    return value


def validate_runtime(value, nonce, identity, device_started_ms):
    native = value.get("native", {})
    if (value.get("schema_version") != 1 or value.get("success") is not True or value.get("nonce") != nonce
            or value.get("hardware_verified") is not False or value.get("full_demo_offline") is not True
            or value.get("bluetooth_runtime_permissions_ungranted") is not True
            or native != native_evidence(identity["sdk_abi_version"], identity["sdk_abi_revision"],
                                         identity["sdk_core_version"], identity["sdk_distribution_version"])
            or not isinstance(value.get("api_level"), int) or value["api_level"] < 21
            or value.get("process_abi") not in {"arm64-v8a", "armeabi-v7a", "x86_64"}):
        raise ValueError("Actual emulator/JNI/ABI/partial-write/Raw runtime evidence is incomplete")
    compatibility = value.get("compatibility_record", {})
    expected = {"distribution_version": identity["sdk_distribution_version"], "core_version": identity["sdk_core_version"],
                "abi_version": identity["sdk_abi_version"], "abi_revision": identity["sdk_abi_revision"],
                "release_commit": identity["sdk_release_commit"], "artifact_sha256": identity["sdk_artifact_sha256"],
                "identity_source": "distribution_manifest", "abi_source": "distribution_manifest"}
    if (compatibility.get("schema_version") != 1 or compatibility.get("hardware_verified") is not False
            or compatibility.get("sdk") != expected
            or compatibility.get("demo") != {"source_fingerprint": identity["demo_source_fingerprint"],
                                             "build_version": identity["demo_build_version"]}
            or compatibility.get("runtime") != {"platform": "android", "os_version": value["os_version"],
                                                "transport": "bluetooth_spp"}
            or compatibility.get("known_limitations") != ["hardware_acceptance_deferred", "data_sent_is_not_printed", "no_automatic_replay"]):
        raise ValueError("Actual Full Demo compatibility record differs from the pinned build identity")
    matches = []
    for line in value.get("journal", "").splitlines():
        marker = " message=compatibility_record="
        if marker not in line:
            continue
        prefix, message = line.split(marker, 1)
        stamp = re.fullmatch(r"utc=[0-9TZ:.\-]+ utc_ms=([0-9]+) elapsed_ms=([0-9]+) run=([0-9a-f]{16}) connection=0 seq=([0-9]+)", prefix)
        if not stamp or int(stamp[1]) < device_started_ms - 5000 or int(stamp[4]) != 1:
            raise ValueError("Stale or malformed Full Demo compatibility journal evidence")
        if json.loads(message) != compatibility:
            raise ValueError("Runtime compatibility record differs from the real journal")
        matches.append(line)
    if len(matches) != 1:
        raise ValueError("Expected exactly one fresh Full Demo launch compatibility journal record")
    return compatibility


def validate_report(report, lock):
    """Reject old packaging-only reports and mismatched runtime SDK identities."""
    abi_version, abi_revision = abi_identity(lock)
    minimal = minimal_api(lock)
    expected_evidence = {"instrumentation-result.json", "quick-start-runtime.json"}
    if not minimal:
        expected_evidence.add("full-demo-journal.log")
    if (report.get("schema_version") != (2 if minimal else 1) or report.get("success") is not True
            or report.get("emulator_only") is not True or report.get("hardware_verified") is not False
            or report.get("physical_device_installed") is not False
            or not re.fullmatch(r"emulator-[0-9]+", report.get("emulator_serial", ""))
            or not re.fullmatch(r"[0-9a-f]{32}", report.get("nonce", ""))
            or report.get("sdk_version") != lock["version"]
            or report.get("sdk_revision") != lock["android"]["revision"]
            or report.get("aar_sha256") != lock["android"]["aar_sha256"]
            or set(report.get("checks", {})) != (MINIMAL_CHECKS if minimal else CHECKS)
            or any(value is not True for value in report["checks"].values())
            or report.get("process_abi") not in {"arm64-v8a", "armeabi-v7a", "x86_64"}
            or not isinstance(report.get("api_level"), int) or report["api_level"] < 21
            or not isinstance(report.get("device_started_ms"), int) or report["device_started_ms"] <= 0
            or report.get("native") != native_evidence(abi_version, abi_revision,
                                                      lock["native_sdk_version"], lock["version"])
            or set(report.get("apks_sha256", {})) != {"quick-start", "full-demo", "runtime-test", "quick-start-test"}
            or set(report.get("evidence_sha256", {})) != expected_evidence
            or any(not re.fullmatch(r"[0-9a-f]{64}", value) for group in ("apks_sha256", "evidence_sha256")
                   for value in report[group].values())):
        raise ValueError("Android emulator runtime report is missing or incomplete")
    if minimal:
        identity = report.get("build_identity", {})
        if (report.get("validation_scope") != "current_manager_offline_lifecycle_smoke"
                or identity.get("sdk_distribution_version") != lock["version"]
                or identity.get("sdk_core_version") != lock["native_sdk_version"]
                or (identity.get("sdk_abi_version"), identity.get("sdk_abi_revision")) != (abi_version, abi_revision)
                or identity.get("sdk_release_commit") != lock["android"]["revision"]
                or identity.get("sdk_artifact_sha256") != lock["android"]["aar_sha256"]
                or not re.fullmatch(r"[0-9a-f]{64}", identity.get("demo_source_fingerprint", ""))
                or identity.get("demo_build_version") != lock["version"] + "+samples." + identity["demo_source_fingerprint"][:12]):
            raise ValueError("Minimal API runtime build identity differs from SDK lock")
        validate_manager_runtime(report.get("full_demo_runtime", {}), report["nonce"], identity)
        validate_manager_runtime(report.get("quick_start_runtime", {}), report["nonce"], identity, quick=True)
        if (report["full_demo_runtime"]["native"] != report["native"]
                or report["full_demo_runtime"]["api_level"] != report["api_level"]
                or report["full_demo_runtime"]["process_abi"] != report["process_abi"]):
            raise ValueError("Minimal API runtime summaries differ from actual gate output")
        return
    compatibility = report.get("compatibility_record", {})
    sdk = compatibility.get("sdk", {})
    demo = compatibility.get("demo", {})
    platform = compatibility.get("runtime", {})
    if (compatibility.get("schema_version") != 1 or compatibility.get("hardware_verified") is not False
            or not re.fullmatch(r"[0-9a-f]{64}", demo.get("source_fingerprint", ""))
            or demo.get("build_version") != lock["version"] + "+samples." + demo["source_fingerprint"][:12]
            or set(demo) != {"source_fingerprint", "build_version"}
            or platform.get("platform") != "android" or platform.get("transport") != "bluetooth_spp"
            or not isinstance(platform.get("os_version"), str)
            or not re.fullmatch(r"[0-9]+(?:\.[0-9]+){0,3}", platform["os_version"])
            or set(platform) != {"platform", "os_version", "transport"}
            or compatibility.get("known_limitations") != ["hardware_acceptance_deferred", "data_sent_is_not_printed", "no_automatic_replay"]
            or sdk != {"distribution_version": lock["version"], "core_version": lock["native_sdk_version"],
                       "abi_version": abi_version, "abi_revision": abi_revision, "release_commit": lock["android"]["revision"],
                       "artifact_sha256": lock["android"]["aar_sha256"], "identity_source": "distribution_manifest",
                       "abi_source": "distribution_manifest"}):
        raise ValueError("Android runtime compatibility identity differs from the SDK pin")


class Adb:
    def __init__(self, executable, serial, output):
        self.executable, self.serial, self.output = str(executable), serial, output

    def run(self, *arguments, timeout=90):
        result = subprocess.run([self.executable, "-s", self.serial, *arguments], capture_output=True,
                                text=True, timeout=timeout)
        with (self.output / "adb.log").open("a") as log:
            log.write("$ adb -s " + self.serial + " " + " ".join(arguments) + "\n")
            log.write(result.stdout + result.stderr + "\n")
        if result.returncode:
            raise ValueError("Emulator command failed; inspect adb.log")
        return result.stdout.strip()


def verify(android_root, output, serial, adb_path=None):
    if not serial or not re.fullmatch(r"emulator-[0-9]+", serial):
        raise ValueError("--emulator-serial emulator-N is required; physical devices are refused")
    sdk_home = Path(os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT", ""))
    adb_path = Path(adb_path) if adb_path else sdk_home / "platform-tools/adb"
    if not adb_path.is_file():
        raise ValueError("Android SDK platform-tools/adb is required")
    output.mkdir(parents=True, exist_ok=True)
    target = output / "runtime-validation.json"
    if target.exists():
        raise ValueError("Runtime evidence already exists; choose a fresh output directory")
    adb = Adb(adb_path, serial, output)
    validate_device(serial, adb.run("shell", "getprop", "ro.kernel.qemu"),
                    adb.run("shell", "getprop", "sys.boot_completed"))
    device_abi = adb.run("shell", "getprop", "ro.product.cpu.abi")
    if device_abi not in {"arm64-v8a", "armeabi-v7a", "x86_64"}:
        raise ValueError("Selected emulator ABI is not included in the published AAR")
    nonce = uuid.uuid4().hex
    lock = json.loads((android_root.parent / "sdk-lock.json").read_text())
    minimal = minimal_api(lock)
    identity = json.loads((android_root / "full-demo/src/main/assets/printer-sdk-samples-build.json").read_text())["identity"]
    if (identity["sdk_distribution_version"] != lock["version"]
            or identity["sdk_release_commit"] != lock["android"]["revision"]
            or identity["sdk_artifact_sha256"] != lock["android"]["aar_sha256"]):
        raise ValueError("Full Demo build identity differs from SDK lock")
    apks = {"quick-start": android_root / "quick-start/build/outputs/apk/debug/quick-start-debug.apk",
            "full-demo": android_root / "full-demo/build/outputs/apk/debug/full-demo-debug.apk",
            "runtime-test": android_root / "full-demo/build/outputs/apk/androidTest/debug/full-demo-debug-androidTest.apk",
            "quick-start-test": android_root / "quick-start/build/outputs/apk/androidTest/debug/quick-start-debug-androidTest.apk"}
    for path in apks.values():
        adb.run("install", "-r", "-t", str(path), timeout=180)
    for application in (QUICK, FULL):
        if adb.run("shell", "pm", "clear", application) != "Success":
            raise ValueError("Could not clear emulator sample data before runtime validation")
    absent = adb.run("shell", "run-as", FULL, "sh", "-c",
                     "'test ! -e files/runtime-gate.json && test ! -e no_backup/demo-journal/journal-current.log && echo fresh'")
    if absent != "fresh":
        raise ValueError("A stale runtime report or journal survived app-data clearing")
    device_started_ms = int(adb.run("shell", "date", "+%s")) * 1000
    quick_result = adb.run("shell", "am", "instrument", "-w", "-r", "-e", "nonce", nonce, QUICK_RUNNER, timeout=120)
    if "INSTRUMENTATION_CODE: -1" not in quick_result or "offline runtime passed; nonce=" + nonce not in quick_result:
        raise ValueError("Quick Start offline instrumentation failed; inspect adb.log")
    quick_raw = adb.run("exec-out", "run-as", QUICK, "cat", "files/quick-runtime-gate.json")
    quick_value = json.loads(quick_raw)
    if minimal:
        validate_manager_runtime(quick_value, nonce, identity, quick=True)
    elif quick_value != {"schema_version": 1, "success": True, "nonce": nonce, "offline_launch": True,
                       "printer_created": False, "operation_created": False,
                       "bluetooth_runtime_permissions_ungranted": True, "hardware_verified": False}:
        raise ValueError("Quick Start runtime evidence is stale or incomplete")
    quick_evidence = output / "quick-start-runtime.json"
    quick_evidence.write_text(quick_raw + "\n")
    adb.run("shell", "am", "force-stop", QUICK)
    result = adb.run("shell", "am", "instrument", "-w", "-r", "-e", "nonce", nonce, RUNNER, timeout=120)
    passed = "Manager offline smoke passed; nonce=" if minimal else "runtime gate passed; nonce="
    if "INSTRUMENTATION_CODE: -1" not in result or passed + nonce not in result:
        raise ValueError("Online SDK instrumentation failed; inspect adb.log")
    raw = adb.run("exec-out", "run-as", FULL, "cat", "files/runtime-gate.json")
    value = json.loads(raw)
    if minimal:
        validate_manager_runtime(value, nonce, identity)
    else:
        validate_runtime(value, nonce, identity, device_started_ms)
    if value["process_abi"] != device_abi:
        raise ValueError("Runtime process ABI differs from selected emulator ABI")
    payload = output / "instrumentation-result.json"
    payload.write_text(raw + "\n")
    evidence = [payload, quick_evidence]
    if not minimal:
        journal = output / "full-demo-journal.log"
        journal.write_text(value["journal"])
        evidence.append(journal)
    report = {"schema_version": 2 if minimal else 1, "success": True, "emulator_serial": serial,
              "emulator_only": True, "hardware_verified": False, "physical_device_installed": False,
              "nonce": nonce, "device_started_ms": device_started_ms, "api_level": value["api_level"],
              "process_abi": value["process_abi"], "sdk_version": lock["version"],
              "sdk_revision": lock["android"]["revision"], "aar_sha256": lock["android"]["aar_sha256"],
              "native": value["native"],
              "checks": {name: True for name in sorted(MINIMAL_CHECKS if minimal else CHECKS)},
              "apks_sha256": {name: digest(path) for name, path in apks.items()},
              "evidence_sha256": {path.name: digest(path) for path in evidence}}
    if minimal:
        report.update(validation_scope="current_manager_offline_lifecycle_smoke", build_identity=identity,
            full_demo_runtime=value, quick_start_runtime=quick_value)
    else:
        report["compatibility_record"] = value["compatibility_record"]
    validate_report(report, lock)
    target.write_text(json.dumps(report, indent=2, sort_keys=True) + "\n")
    packaging_path = output / "validation.json"
    packaging = json.loads(packaging_path.read_text())
    for name in ("quick-start", "full-demo"):
        if packaging["applications"][name]["debug_apk_sha256"] != report["apks_sha256"][name]:
            raise ValueError("Runtime APK differs from the packaging-verified application")
    packaging.update(emulator_runtime_verified=True, runtime_compatibility_verified=not minimal,
                     runtime=report, runtime_validation_sha256=digest(target))
    if minimal:
        packaging["runtime_api_verified"] = True
    packaging_path.write_text(json.dumps(packaging, indent=2, sort_keys=True) + "\n")
    adb.run("shell", "am", "force-stop", FULL)
    print("Online Android SDK emulator runtime passed: real JNI/ABI handshake, partial writes, Raw order, fresh offline diagnostics")
    return report


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--android-root", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--emulator-serial", required=True)
    parser.add_argument("--adb", type=Path)
    args = parser.parse_args()
    try:
        verify(args.android_root.resolve(), args.output.resolve(), args.emulator_serial, args.adb)
    except (ValueError, KeyError, OSError, subprocess.SubprocessError) as error:
        raise SystemExit(str(error)) from None


if __name__ == "__main__":
    main()
