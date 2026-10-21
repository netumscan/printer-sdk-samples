#!/usr/bin/env python3
# SPDX-License-Identifier: MIT
"""Collect legacy reports unchanged or bounded, verified schema-2 Android evidence.

The immutable legacy collector still handles masking, budgets, safe filesystem
reads, command capture and iOS/schema-1 validation. A mixed report uses separate
SDK pins and complete current instrumentation instead of the retired Full Demo
journal; it must prove that scope before collection may report success.
"""
import argparse
import contextlib
import importlib.util
import io
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


LEGACY = load(Path(__file__).with_name("collect-legacy-evidence.py"), "legacy_evidence_collector")
MIXED_FILES = tuple(name for name in LEGACY.ANDROID_FILES if name != "full-demo-journal.log")


def read_json(path):
    result = LEGACY.read_source(path)
    if result.get("status") != "included" or result.get("truncated") is not False:
        raise ValueError("Incomplete mixed JSON evidence")
    value = json.loads(result["content"])
    if not isinstance(value, dict):
        raise ValueError("Mixed JSON evidence must be an object")
    return value, result["source_sha256"]


def validate_mixed(root, source, validation):
    contract = load(root / "tools/android_demo_contract.py", "collected_demo_contract")
    gate = load(root / "android/scripts/verify-mixed.py", "collected_mixed_gate")
    source_manifest, source_sha = read_json(root / "samples-manifest.json")
    current_quick = "android_quick_start" in source_manifest
    if (validation.get("schema_version") != 2 or validation.get("success") is not True
            or validation.get("hardware_verified") is not False or validation.get("emulator_runtime_verified") is not True
            or validation.get("runtime_compatibility_verified") is not True
            or validation.get("runtime_compatibility_scope") != gate.runtime_scope(current_quick)
            or validation.get("validation_scope") != "mixed_exact_sdk_pins"
            or validation.get("historical_quick_start_gate_retained") is not (not current_quick)
            or validation.get("historical_full_demo_replaced") is not True):
        raise ValueError("Mixed verification has not completed its documented scope")
    if current_quick and validation.get("current_quick_start_gate_verified") is not True:
        raise ValueError("Current Quick Start gate has not completed")
    runtime, runtime_sha = read_json(source / "runtime-validation.json")
    if (runtime.get("schema_version") != 2 or runtime.get("success") is not True
            or runtime.get("hardware_verified") is not False or runtime.get("emulator_only") is not True
            or runtime.get("physical_device_installed") is not False
            or not re.fullmatch(r"emulator-[0-9]+", runtime.get("emulator_serial", ""))
            or not re.fullmatch(r"[0-9a-f]{32}", runtime.get("nonce", ""))
            or validation.get("runtime_validation_sha256") != runtime_sha or validation.get("runtime") != runtime):
        raise ValueError("Mixed runtime report is incomplete or differs from its hash")
    if runtime.get("source_manifest_sha256") != source_sha or validation.get("source_manifest_sha256") != source_sha:
        raise ValueError("Mixed evidence is bound to another sample source snapshot")
    names = {"quick-start", "full-demo"}
    if set(validation.get("applications", {})) != names or set(runtime.get("applications", {})) != names:
        raise ValueError("Mixed evidence must cover both Android applications")
    apks = runtime.get("apks_sha256", {})
    if (set(apks) != names | {"quick-start-test", "full-demo-test"}
            or any(not re.fullmatch(r"[0-9a-f]{64}", value) for value in apks.values())):
        raise ValueError("Missing mixed runtime APK identity")
    locks = {}
    for name in names:
        lock_path = root / ("android/" + name + "/sdk-lock.json" if name == "full-demo" or current_quick else "sdk-lock.json")
        lock, _ = read_json(lock_path)
        if name == "full-demo" or current_quick:
            contract.validate_full_lock(lock)
        locks[name] = lock
        packaged, ran = validation["applications"][name], runtime["applications"][name]
        checks = packaged.get("checks", {})
        if (set(checks) != contract.packaging_checks(lock) or any(value is not True for value in checks.values())
                or packaged.get("aar_sha256") != lock["android"]["aar_sha256"]
                or packaged.get("version") != lock["version"] or packaged.get("coordinate") != lock["android"]["coordinate"]
                or packaged.get("sdk_release_commit") != lock["android"]["revision"] or packaged.get("c_abi") != lock["c_abi"]
                or packaged.get("debug_apk_sha256") != apks[name] or ran.get("sdk_version") != lock["version"]
                or ran.get("aar_sha256") != lock["android"]["aar_sha256"]):
            raise ValueError("Application packaging/runtime identity differs from its own exact SDK lock")
    expected_evidence = {"instrumentation-result.json", "quick-start-runtime.json", "adb.log"}
    hashes = runtime.get("evidence_sha256", {})
    if set(hashes) != expected_evidence:
        raise ValueError("Mixed runtime evidence inventory differs")
    for name in expected_evidence:
        result = LEGACY.read_source(source / name)
        if result.get("status") != "included" or result.get("source_sha256") != hashes[name]:
            raise ValueError("Mixed runtime evidence differs from its recorded hash")
    quick, _ = read_json(source / "quick-start-runtime.json")
    quick_identity = gate.validate_quick_report(quick, locks["quick-start"], runtime["nonce"], current_quick)
    quick_ran = runtime["applications"]["quick-start"]
    expected_gate = "current_QuickStartRuntimeGate" if current_quick else "retained_historical_quick_start"
    if quick_ran.get("gate") != expected_gate or quick_ran.get("offline_runtime_verified") is not True:
        raise ValueError("Quick Start gate has not passed for this source snapshot")
    if current_quick and (quick_ran.get("sdk_runtime_identity") != quick_identity or
            quick_ran.get("lifecycle_cleanup_verified") is not True):
        raise ValueError("Current Quick Start JNI identity or lifecycle cleanup differs")
    full, _ = read_json(source / "instrumentation-result.json")
    if (full.get("schema_version") != 1 or full.get("success") is not True or full.get("hardware_verified") is not False
            or full.get("nonce") != runtime["nonce"]
            or full.get("validation_scope") != "complete_current_android_full_demo_instrumentation"):
        raise ValueError("Current Full Demo instrumentation is incomplete or stale")
    stream = full.get("stream", "")
    gate.instrument_passed(stream, "PASS: Full Demo image source/import/EXIF/limits/reading leases")
    identity = gate.validate_identity(stream, locks["full-demo"])
    ran = runtime["applications"]["full-demo"]
    if (full.get("sdk_runtime_identity") != identity or ran.get("sdk_runtime_identity") != identity
            or ran.get("gate") != "current_DemoInstrumentation" or ran.get("complete_instrumentation_verified") is not True):
        raise ValueError("Full Demo loaded JNI identity or complete-suite evidence differs")


def collect(platform, source, command_output, output):
    source, command_output, output = map(LEGACY.plain_path, (source, command_output, output))
    mixed = False
    valid = False
    try:
        validation, _ = read_json(source / "validation.json")
        mixed = platform == "android" and validation.get("schema_version") == 2
        if mixed:
            validate_mixed(Path(__file__).resolve().parents[1], source, validation)
            valid = True
    except (ValueError, KeyError, OSError, TypeError):
        pass
    original_files = LEGACY.ANDROID_FILES
    try:
        if mixed:
            LEGACY.ANDROID_FILES = MIXED_FILES
        with contextlib.redirect_stdout(io.StringIO()):
            manifest = LEGACY.collect(platform, source, command_output, output)
    finally:
        LEGACY.ANDROID_FILES = original_files
    if mixed:
        manifest["validation_scope"] = validation.get("runtime_compatibility_scope")
        if not valid and manifest["verification_status"] == "passed":
            manifest["verification_status"] = "failed"
        manifest["mixed_report_integrity_verified"] = valid
        (output / "evidence-manifest.json").write_text(json.dumps(manifest, indent=2, sort_keys=True) + "\n")
    print(json.dumps({"collection_status": manifest["collection_status"],
                      "verification_status": manifest["verification_status"],
                      "files": len(manifest["files"]), "included_bytes": manifest["included_bytes"]}, sort_keys=True))
    return manifest


def main():
    # Command capture retains exactly the legacy safe runner implementation.
    if len(sys.argv) > 1 and sys.argv[1] == "run":
        return LEGACY.main(sys.argv[1:])
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("mode", choices=("collect",))
    parser.add_argument("--platform", required=True, choices=("android", "ios"))
    parser.add_argument("--source", required=True, type=Path)
    parser.add_argument("--command-output", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    try:
        collect(args.platform, args.source, args.command_output, args.output)
    except (ValueError, KeyError, OSError) as error:
        raise SystemExit(str(error)) from None
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
