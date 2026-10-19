# SPDX-License-Identifier: MIT
"""Reviewed sample verification boundaries, selected by the exact SDK lock."""
import re


def abi_identity(lock):
    value = lock.get("c_abi", "")
    match = re.fullmatch(r"([1-9][0-9]*)/r([1-9][0-9]*)", value) if isinstance(value, str) else None
    if match is None or tuple(map(int, match.groups())) not in {(2, 10), (3, 1)}:
        raise ValueError("Unsupported samples C ABI identity")
    return tuple(map(int, match.groups()))


def c_export_count(lock):
    return 25 if abi_identity(lock)[0] == 3 else 49


def ios_test_count(lock):
    return 6 if abi_identity(lock)[0] == 3 else 4


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
    return result
