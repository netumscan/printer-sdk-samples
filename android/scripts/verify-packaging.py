#!/usr/bin/env python3
# SPDX-License-Identifier: MIT
"""Check real online SDK bytes and final R8 APKs without private repository tools."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import struct
import subprocess
import zipfile
import sys
sys.dont_write_bytecode = True
sys.path.insert(0, str(Path(__file__).resolve().parents[2] / "tools"))
from sdk_contract import jni_callbacks, jni_constructors, jni_methods, packaging_checks

VERSION = "0.3.0-preview.17"
SDK_LOCK = json.loads((Path(__file__).resolve().parents[2] / "sdk-lock.json").read_text())
AAR_SHA256 = SDK_LOCK["android"]["aar_sha256"]
ABIS = ("arm64-v8a", "armeabi-v7a", "x86_64")
MODULES = {
    "quick-start": "com.netumscan.printersdk.samples.quickstart",
    "full-demo": "com.netum.printersdk.demo",
}


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def dex_methods(data):
    """Read defined methods and signatures from the final DEX, not just R8 mappings."""
    if not data.startswith(b"dex\n"):
        raise SystemExit("Invalid APK DEX file")

    def uint(offset):
        return struct.unpack_from("<I", data, offset)[0]

    def uleb(offset):
        value, shift = 0, 0
        while True:
            byte = data[offset]
            offset += 1
            value |= (byte & 127) << shift
            if byte < 128:
                return value, offset
            shift += 7
            if shift > 28:
                raise SystemExit("Invalid DEX integer")

    strings = []
    for index in range(uint(56)):
        offset = uint(uint(60) + index * 4)
        _, offset = uleb(offset)
        strings.append(data[offset:data.index(0, offset)].decode("utf-8", errors="replace"))
    types = [strings[uint(uint(68) + index * 4)] for index in range(uint(64))]
    prototypes = []
    for index in range(uint(72)):
        offset = uint(76) + index * 12
        return_type, parameters_offset = uint(offset + 4), uint(offset + 8)
        parameters = [] if not parameters_offset else [
            types[struct.unpack_from("<H", data, parameters_offset + 4 + parameter * 2)[0]]
            for parameter in range(uint(parameters_offset))]
        prototypes.append("(" + "".join(parameters) + ")" + types[return_type])
    identifiers = []
    for index in range(uint(88)):
        owner, prototype, name = struct.unpack_from("<HHI", data, uint(92) + index * 8)
        identifiers.append((types[owner], strings[name], prototypes[prototype]))
    result = {}
    for index in range(uint(96)):
        offset = uint(uint(100) + index * 32 + 24)
        if not offset:
            continue
        counts = []
        for _ in range(4):
            count, offset = uleb(offset)
            counts.append(count)
        for _ in range(counts[0] + counts[1]):
            _, offset = uleb(offset)
            _, offset = uleb(offset)
        for count in counts[2:]:
            method_index = 0
            for _ in range(count):
                difference, offset = uleb(offset)
                flags, offset = uleb(offset)
                _, offset = uleb(offset)
                method_index += difference
                owner, name, descriptor = identifiers[method_index]
                result[(owner, name)] = {"native": bool(flags & 0x100), "descriptor": descriptor}
    return result


def validate_debug_payload(application, library, identity=None):
    """The APK installed by the runtime gate must carry the exact pinned AAR native bytes."""
    abis = {name.split("/")[1] for name in application.namelist()
            if name.startswith("lib/") and name.endswith(".so")}
    if abis != set(ABIS):
        raise SystemExit("Debug APK has missing or unexpected CPU ABI")
    expected = {f"lib/{abi}/{name}" for abi in ABIS
                for name in ("libprinter_sdk_c.so", "libprinter_sdk_jni.so")}
    if {name for name in application.namelist() if name.startswith("lib/") and name.endswith(".so")} != expected:
        raise SystemExit("Debug APK contains unexpected native libraries")
    for abi in ABIS:
        for name in ("libprinter_sdk_c.so", "libprinter_sdk_jni.so"):
            if application.read(f"lib/{abi}/{name}") != library.read(f"jni/{abi}/{name}"):
                raise SystemExit("Debug APK native payload differs from the pinned AAR")
    for name in ("LICENSE", "LICENSE-NOTICE.md", "NOTICE.android-ndk"):
        path = "assets/printer-sdk/" + name
        if application.read(path) != library.read(path):
            raise SystemExit("Debug APK SDK license or notice differs from the pinned AAR")
    if identity is not None and application.read("assets/printer-sdk-samples-build.json") != identity:
        raise SystemExit("Debug APK diagnostic identity differs from the exported build identity")


def validate_jni_constructors(methods, lock, module):
    for owner, descriptor in jni_constructors(lock).items():
        if methods.get(("Lcom/netumscan/printersdk/" + owner + ";", "<init>"), {}).get("descriptor") != descriptor:
            raise SystemExit(f"{module}: R8 changed JNI constructor {owner}")


def inspect(root, module, application_id):
    build = root / module / "build"
    aar = build / "verification/resolved-sdk.aar"
    apk = build / "outputs/apk/release" / (module + "-release-unsigned.apk")
    debug_apk = build / "outputs/apk/debug" / (module + "-debug.apk")
    if digest(aar) != AAR_SHA256:
        raise SystemExit(f"{module}: online AAR differs from pinned SDK {VERSION}")
    with zipfile.ZipFile(debug_apk) as application, zipfile.ZipFile(aar) as library:
        identity = (root / module / "src/main/assets/printer-sdk-samples-build.json").read_bytes() if module == "full-demo" else None
        validate_debug_payload(application, library, identity)
    with zipfile.ZipFile(apk) as application, zipfile.ZipFile(aar) as library:
        if module == "full-demo":
            identity = "assets/printer-sdk-samples-build.json"
            if application.read(identity) != (root / module / "src/main" / identity).read_bytes():
                raise SystemExit("Diagnostic build identity was not embedded unchanged")
        abis = {name.split("/")[1] for name in application.namelist()
                if name.startswith("lib/") and name.endswith(".so")}
        if abis != set(ABIS):
            raise SystemExit(f"{module}: missing or unexpected CPU ABI")
        expected = {f"lib/{abi}/{name}" for abi in ABIS
                    for name in ("libprinter_sdk_c.so", "libprinter_sdk_jni.so")}
        if {name for name in application.namelist() if name.startswith("lib/") and name.endswith(".so")} != expected:
            raise SystemExit(f"{module}: unexpected native libraries")
        for abi in ABIS:
            for name in ("libprinter_sdk_c.so", "libprinter_sdk_jni.so"):
                if application.read(f"lib/{abi}/{name}") != library.read(f"jni/{abi}/{name}"):
                    raise SystemExit(f"{module}: final native payload differs from pinned AAR")
        for name in ("LICENSE", "LICENSE-NOTICE.md", "NOTICE.android-ndk"):
            path = "assets/printer-sdk/" + name
            if application.read(path) != library.read(path):
                raise SystemExit(f"{module}: SDK license or third-party notice was changed")
        methods = {}
        for name in application.namelist():
            if re.fullmatch(r"classes\d*\.dex", name):
                methods.update(dex_methods(application.read(name)))
        if any(owner.startswith("Lcom/netum/printersdk/") for owner, _ in methods):
            raise SystemExit(f"{module}: legacy SDK namespace found in DEX")
        for owner, names in jni_methods(SDK_LOCK).items():
            for name in names:
                if not methods.get((f"Lcom/netumscan/printersdk/{owner};", name), {}).get("native"):
                    raise SystemExit(f"{module}: R8 removed JNI method {owner}.{name}")
        for name, descriptor in jni_callbacks(SDK_LOCK).items():
            method = methods.get(("Lcom/netumscan/printersdk/MobilePrinter$ChannelBridge;", name))
            if not method or method["descriptor"] != descriptor:
                raise SystemExit(f"{module}: R8 changed JNI callback {name}")
        validate_jni_constructors(methods, SDK_LOCK, module)
    sdk_home = Path(os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT", ""))
    aapt = sdk_home / "build-tools/35.0.0/aapt2"
    if not aapt.is_file():
        raise SystemExit("Android SDK Build Tools 35.0.0 is required for manifest validation")
    manifest = subprocess.run([str(aapt), "dump", "badging", str(apk)], text=True,
                              capture_output=True, check=True).stdout
    if f"name='{application_id}'" not in manifest or "minSdkVersion:'21'" not in manifest:
        raise SystemExit(f"{module}: applicationId or minimum API changed")
    classes = sorted((build / "intermediates/javac/release/compileReleaseJavaWithJavac/classes").rglob("*.class"))
    if not classes or any(path.read_bytes()[:8] != b"\xca\xfe\xba\xbe\x00\x00\x004" for path in classes):
        raise SystemExit(f"{module}: application Java classes are not Java 8 bytecode")
    return {
        "application_id": application_id, "min_sdk": 21, "java_bytecode_release": 8,
        "java_class_count": len(classes), "aar_sha256": digest(aar),
        "debug_apk_sha256": digest(debug_apk), "r8_apk_sha256": digest(apk),
        "mapping_sha256": digest(build / "outputs/mapping/release/mapping.txt"),
        "checks": {name: True for name in sorted(packaging_checks(SDK_LOCK))},
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--android-root", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    if VERSION.endswith("-dev"):
        parser.error("Unpublished development contracts cannot be verified as online SDK samples")
    report = {"version": VERSION, "coordinate": "com.netumscan:printer-sdk-android:" + VERSION,
              "compatibility_identity_verified": True,
              "repository": "https://repo.maven.apache.org/maven2", "cpu_abis": list(ABIS),
              "hardware_verified": False,
              "applications": {name: inspect(args.android_root, name, app_id)
                               for name, app_id in MODULES.items()}}
    args.output.mkdir(parents=True, exist_ok=True)
    target = args.output / "validation.json"
    target.write_text(json.dumps(report, indent=2) + "\n")
    print(f"Verified both Android samples against online SDK {VERSION}: Java 8, R8/JNI, 3 CPU ABIs")
    print(f"Report: {target}")


if __name__ == "__main__":
    main()
