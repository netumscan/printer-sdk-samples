# SPDX-License-Identifier: MIT
"""Reviewed sample verification boundaries, selected by the exact SDK lock."""
import json
from pathlib import Path
import re

MINIMAL_API_ABI = {"0.3.0-preview.10": (3, 2), "0.3.0-preview.11": (3, 2),
                   "0.3.0-preview.14": (3, 3), "0.3.0-preview.15": (3, 3)}
MINIMAL_API_VERSIONS = frozenset(MINIMAL_API_ABI)
UNIFIED_FLUTTER_VERSIONS = frozenset({"0.3.0-preview.11", "0.3.0-preview.14"})


def platform_lock(root, platform):
    """Keep the historical root lock; only the reviewed iOS overlay selects a sidecar."""
    root = Path(root)
    lock = json.loads((root / "sdk-lock.json").read_text())
    sidecar = root / "ios-sdk-lock.json"
    if platform == "ios" and sidecar.exists():
        if sidecar.is_symlink():
            raise ValueError("Symlinked iOS SDK lock")
        manifest = json.loads((root / "samples-manifest.json").read_text())
        if manifest.get("ios_overlay", {}).get("sdk_lock") != sidecar.name:
            raise ValueError("iOS SDK sidecar lacks reviewed overlay metadata")
        lock = json.loads(sidecar.read_text())
        validate_ios_overlay_lock(lock)
    return lock


def validate_ios_overlay_lock(lock):
    """This overlay is intentionally limited to the independently released .15 iOS SDK."""
    ios = lock.get("ios", {})
    if (set(lock) != {"version", "native_sdk_version", "c_abi", "hardware_verified", "ios"}
            or lock.get("version") != "0.3.0-preview.15" or lock.get("native_sdk_version") != "0.3.0"
            or lock.get("c_abi") != "3/r3" or lock.get("hardware_verified") is not False
            or ios.get("version") != lock["version"]
            or ios.get("repository") != "https://github.com/netumscan/printer-sdk-ios"
            or ios.get("license") != ios["repository"] + "/blob/v" + lock["version"] + "/LICENSE"
            or not isinstance(ios.get("revision"), str) or re.fullmatch(r"[0-9a-f]{40}", ios["revision"]) is None
            or not isinstance(ios.get("xcframework_sha256"), str)
            or re.fullmatch(r"[0-9a-f]{64}", ios["xcframework_sha256"]) is None):
        raise ValueError("Invalid independently published iOS .15 SDK lock")
    abi_identity(lock)
    return lock


def abi_identity(lock):
    value = lock.get("c_abi", "")
    match = re.fullmatch(r"([1-9][0-9]*)/r([1-9][0-9]*)", value) if isinstance(value, str) else None
    if match is None:
        raise ValueError("Unsupported samples C ABI identity")
    identity = tuple(map(int, match.groups()))
    expected = MINIMAL_API_ABI.get(lock.get("version"))
    if ((expected is not None and identity != expected) or
            (expected is None and identity not in {(2, 10), (3, 1), (3, 2)})):
        raise ValueError("Unsupported samples C ABI identity")
    return identity


def c_export_count(lock):
    identity = abi_identity(lock)
    return 28 if identity == (3, 3) else 27 if identity == (3, 2) else 25 if identity[0] == 3 else 49


def ios_test_count(lock):
    identity = abi_identity(lock)
    if lock.get("version") in MINIMAL_API_VERSIONS:
        return 18
    return 15 if identity == (3, 2) else 10 if identity[0] == 3 else 4


def jni_methods(lock):
    if abi_identity(lock)[0] == 2:
        return {"MobilePrinter": ("nCancelled", "nCreate", "nConnect", "nDisconnect", "nDestroy",
                                  "nConnectionState", "nFeature", "nSubmit", "nControl"),
                "PrintOperation": ("nSnapshot", "nCancel", "nRelease")}
    return {"MobilePrinter": ("nCancelled", "nCreate", "nConnect", "nDisconnect", "nDestroy",
                              "nStateSnapshot", "nCapabilities", "nDiagnostic", "nResources", "nSdkInfo",
                              "nObserverEpoch", "nObserverLost", "nObserverRelease", "nSubmit", "nExecute"),
            "PrintOperation": ("nSnapshot", "nResult", "nCancel", "nRelease"),
            "PrinterImage": ("nCreate", "nRelease"), "PrintJob": ("nCreate", "nRelease")}


def jni_callbacks(lock):
    result = {"open": "(JI)I", "write": "([BJI)I", "isOpen": "()Z", "close": "()V"}
    if abi_identity(lock)[0] == 3:
        result.update(read="([BJI)I", errorDiagnostic="()[J", attachObserver="(J)V", disposeObserver="()V")
    return result


def packaging_checks(lock):
    return {"online_aar_matches_pin", "java8_bytecode", "debug_native_payload_matches",
            "r8_jni_" + str(sum(map(len, jni_methods(lock).values()))) + "_methods",
            "r8_jni_" + str(len(jni_callbacks(lock))) + "_callbacks", "three_abi_payload_matches",
            "sdk_license_notices_preserved", "application_id", "min_sdk_21"}


def native_evidence(abi_version, abi_revision, native_version=None, distribution_version=None):
    # These are assertions from the actual native channel test, never inferred
    # from the diagnostic build resource alone.
    result = {"jni_loaded": True, "abi_handshake_passed": True, "abi_expected_version": abi_version,
              "abi_minimum_revision": abi_revision, "partial_write_calls": 3, "write_calls": 5,
              "bytes_sent": 14, "raw_order_verified": True, "transport": "in_memory_fake"}
    if abi_version == 3:
        result.update(native_version=native_version, distribution_version=distribution_version,
                      completion_callback_verified=True)
        if abi_revision >= 2:
            result.update(main_thread_callback_verified=True, exactly_once_callback_verified=True)
    return result


def validate_ios_runtime_api(value, lock):
    """Recheck reviewed minimal API identities; never relabel legacy ABI evidence."""
    if lock.get("version") not in MINIMAL_API_VERSIONS:
        raise ValueError("Minimal iOS runtime evidence requires a reviewed exact version")
    abi_version, abi_revision = abi_identity(lock)
    if (not isinstance(value, dict) or set(value) != {"schema_version", "scope", "nonce", "sdk", "distribution",
            "demo", "offline_lifecycle", "hardware_verified", "transport", "build_resource", "journal_sha256"}
            or type(value.get("schema_version")) is not int or value["schema_version"] != 1
            or value.get("scope") != "sdk_identity_and_offline_lifecycle"
            or value.get("hardware_verified") is not False or value.get("transport") != "none"):
        raise ValueError("Incomplete iOS minimal runtime scope")
    for field, pattern in (("nonce", r"[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}"),
                           ("journal_sha256", r"[0-9a-f]{64}")):
        if not isinstance(value.get(field), str) or re.fullmatch(pattern, value[field]) is None:
            raise ValueError("Invalid iOS runtime " + field)
    resource = value.get("build_resource")
    if (not isinstance(resource, dict) or set(resource) != {"schema_version", "identity"}
            or type(resource.get("schema_version")) is not int or resource["schema_version"] != 1
            or not isinstance(resource.get("identity"), dict)):
        raise ValueError("Invalid iOS build resource")
    identity = resource["identity"]
    expected = {"sdk_distribution_version": lock["version"], "sdk_core_version": lock["native_sdk_version"],
                "sdk_abi_version": abi_version, "sdk_abi_revision": abi_revision,
                "sdk_release_commit": lock["ios"]["revision"],
                "sdk_artifact_sha256": lock["ios"]["xcframework_sha256"]}
    if ({key: identity.get(key) for key in expected} != expected
            or any(type(identity.get(key)) is not int for key in ("sdk_abi_version", "sdk_abi_revision"))):
        raise ValueError("iOS build resource differs from the exact SDK lock")
    fingerprint = identity.get("demo_source_fingerprint")
    if (not isinstance(fingerprint, str) or re.fullmatch(r"[0-9a-f]{64}", fingerprint) is None
            or identity.get("demo_build_version") != lock["version"] + "+samples." + fingerprint[:12]):
        raise ValueError("Invalid iOS Demo source identity")
    sdk = value.get("sdk")
    if (not isinstance(sdk, dict) or set(sdk) != {"native_version", "distribution_version", "build_id"}
            or sdk.get("native_version") != lock["native_sdk_version"]
            or sdk.get("distribution_version") != lock["version"]
            or (sdk.get("build_id") is not None and (not isinstance(sdk["build_id"], str)
                or re.fullmatch(r"[0-9a-f]{64}", sdk["build_id"]) is None))):
        raise ValueError("Actual SDKInfo differs from the exact runtime identity")
    if value.get("distribution") != {"distribution_version": lock["version"],
            "core_version": lock["native_sdk_version"], "release_commit": lock["ios"]["revision"],
            "artifact_sha256": lock["ios"]["xcframework_sha256"]}:
        raise ValueError("iOS runtime distribution metadata differs from the SDK lock")
    if value.get("demo") != {"source_fingerprint": fingerprint, "build_version": identity["demo_build_version"]}:
        raise ValueError("Running iOS Demo differs from its generated build resource")
    lifecycle = value.get("offline_lifecycle")
    expected_lifecycle = {"operations": ["sdkInfo", "disconnect", "disconnect", "sdkInfo", "close", "close", "sdkInfo"],
        "codes": [0, 0, 0, 0, 0, 0, 1002], "callbacks_on_main_thread": True, "callbacks_asynchronous": True,
        "exactly_once": True, "completion_none": True, "output_may_have_occurred": False}
    if (lifecycle != expected_lifecycle or not isinstance(lifecycle, dict)
            or any(type(code) is not int for code in lifecycle.get("codes", []))
            or any(type(lifecycle.get(name)) is not bool for name in ("callbacks_on_main_thread",
                "callbacks_asynchronous", "exactly_once", "completion_none", "output_may_have_occurred"))):
        raise ValueError("iOS offline lifecycle did not execute the exact safe request sequence")
    return value
