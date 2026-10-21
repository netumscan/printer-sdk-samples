#!/usr/bin/env python3
# SPDX-License-Identifier: MIT
"""Verify a Flutter APK against the exact public Maven AAR in flutter/sdk-lock.json.

Run from any directory: python3 tools/check-flutter-android.py --apk <app-debug.apk>
An optional --aar <cached.aar> avoids downloading; it must match the same SHA256.
Only temporary files are written. Flutter's own native libraries are permitted.
"""
import argparse
import hashlib
import json
from pathlib import Path
import re
import sys
import tempfile
import urllib.request
import zipfile

sys.dont_write_bytecode = True
MAX_AAR_BYTES = 64 * 1024 * 1024
MAX_APK_BYTES = 512 * 1024 * 1024
MAX_ENTRY_BYTES = 64 * 1024 * 1024
CHUNK_BYTES = 1024 * 1024
SDK_LIBRARIES = {"libprinter_sdk_c.so", "libprinter_sdk_jni.so"}
NOTICES = ("LICENSE", "LICENSE-NOTICE.md", "NOTICE.android-ndk")
MAVEN_REPOSITORIES = {"https://repo.maven.apache.org/maven2", "https://repo1.maven.org/maven2"}


def digest(path):
    result = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(CHUNK_BYTES), b""):
            result.update(chunk)
    return result.hexdigest()


def load_lock(path):
    if path.stat().st_size > 64 * 1024:
        raise ValueError("SDK lock exceeds the size limit")
    lock = json.loads(path.read_text(encoding="utf-8"))
    android = lock.get("android", {})
    coordinate = android.get("coordinate", "")
    match = re.fullmatch(r"com\.netumscan:printer-sdk-android:([0-9][A-Za-z0-9_.-]*)", coordinate)
    if match is None or match[1] != lock.get("version"):
        raise ValueError("SDK lock must pin an exact public printer-sdk-android version")
    repository = android.get("repository", "").rstrip("/")
    if repository not in MAVEN_REPOSITORIES:
        raise ValueError("SDK lock repository must be an official HTTPS Maven Central endpoint")
    checksum = android.get("aar_sha256", "")
    if not isinstance(checksum, str) or re.fullmatch(r"[0-9a-f]{64}", checksum) is None:
        raise ValueError("SDK lock has an invalid AAR SHA256")
    version = match[1]
    url = repository + f"/com/netumscan/printer-sdk-android/{version}/printer-sdk-android-{version}.aar"
    return {"version": version, "coordinate": coordinate, "aar_sha256": checksum, "url": url}


def download_aar(url, destination):
    request = urllib.request.Request(url, headers={"User-Agent": "netum-printer-samples-verifier/1"})
    with urllib.request.urlopen(request, timeout=30) as response:
        if response.geturl() != url:
            raise ValueError("Unexpected redirect while downloading the pinned Maven AAR")
        length = response.headers.get("Content-Length")
        if length is not None and (not length.isdecimal() or int(length) > MAX_AAR_BYTES):
            raise ValueError("Pinned AAR download exceeds the size limit")
        size = 0
        with destination.open("wb") as stream:
            while True:
                chunk = response.read(CHUNK_BYTES)
                if not chunk:
                    break
                size += len(chunk)
                if size > MAX_AAR_BYTES:
                    raise ValueError("Pinned AAR download exceeds the size limit")
                stream.write(chunk)
        if size == 0 or (length is not None and size != int(length)):
            raise ValueError("Pinned AAR download was empty or incomplete")


def entry_identity(archive, name):
    entry = archive.getinfo(name)
    if entry.file_size > MAX_ENTRY_BYTES:
        raise ValueError("ZIP entry exceeds the size limit: " + name)
    checksum = hashlib.sha256()
    size = 0
    with archive.open(entry) as stream:
        while True:
            chunk = stream.read(CHUNK_BYTES)
            if not chunk:
                break
            size += len(chunk)
            if size > MAX_ENTRY_BYTES:
                raise ValueError("ZIP entry exceeds the size limit: " + name)
            checksum.update(chunk)
    return {"sha256": checksum.hexdigest(), "bytes": size}


def verify(apk, aar, lock, aar_source):
    if not 0 < aar.stat().st_size <= MAX_AAR_BYTES:
        raise ValueError("Pinned AAR is empty or exceeds the size limit")
    if not 0 < apk.stat().st_size <= MAX_APK_BYTES:
        raise ValueError("APK is empty or exceeds the size limit")
    if digest(aar) != lock["aar_sha256"]:
        raise ValueError("AAR SHA256 differs from the exact SDK lock")
    with zipfile.ZipFile(apk) as application, zipfile.ZipFile(aar) as library:
        for archive in (application, library):
            if len(archive.namelist()) != len(set(archive.namelist())):
                raise ValueError("Duplicate ZIP entries are not allowed")
        sdk_entries = [name for name in application.namelist()
                       if name.startswith("lib/") and Path(name).name.startswith("libprinter_sdk_")
                       and name.endswith(".so")]
        if not sdk_entries:
            raise ValueError("APK contains no printer SDK native libraries")
        by_abi = {}
        for name in sdk_entries:
            parts = name.split("/")
            if len(parts) != 3 or parts[2] not in SDK_LIBRARIES:
                raise ValueError("APK contains an unexpected SDK native library: " + name)
            by_abi.setdefault(parts[1], set()).add(parts[2])
        natives = {}
        for abi, names in sorted(by_abi.items()):
            if names != SDK_LIBRARIES:
                raise ValueError("APK is missing an SDK native library for ABI " + abi)
            for name in sorted(names):
                apk_name, aar_name = f"lib/{abi}/{name}", f"jni/{abi}/{name}"
                if aar_name not in library.namelist():
                    raise ValueError("APK SDK ABI is absent from the pinned AAR: " + abi)
                actual = entry_identity(application, apk_name)
                if actual != entry_identity(library, aar_name):
                    raise ValueError("APK SDK native bytes differ from the pinned AAR: " + apk_name)
                natives[apk_name] = actual
        notices = {}
        for name in NOTICES:
            path = "assets/printer-sdk/" + name
            actual = entry_identity(application, path)
            if actual != entry_identity(library, path):
                raise ValueError("APK SDK license or notice differs from the pinned AAR: " + path)
            notices[path] = actual
    return {"schema_version": 1, "ok": True, "scope": "flutter_android_artifact_integrity",
            "hardware_verified": False, "sdk_version": lock["version"],
            "coordinate": lock["coordinate"], "aar_source": aar_source,
            "aar_sha256": lock["aar_sha256"], "apk_name": apk.name, "apk_sha256": digest(apk),
            "abis": sorted(by_abi), "native_libraries": natives, "sdk_license_notices": notices}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apk", type=Path, required=True)
    parser.add_argument("--aar", type=Path, help="Optional unmodified pinned Maven AAR from a local cache")
    parser.add_argument("--lock", type=Path,
                        default=Path(__file__).resolve().parents[1] / "flutter/sdk-lock.json")
    args = parser.parse_args()
    try:
        lock = load_lock(args.lock)
        if args.aar:
            report = verify(args.apk, args.aar, lock, "provided_pinned_aar")
        else:
            with tempfile.TemporaryDirectory(prefix="printer-flutter-aar-") as temporary:
                aar = Path(temporary) / "pinned-sdk.aar"
                download_aar(lock["url"], aar)
                report = verify(args.apk, aar, lock, lock["url"])
        print(json.dumps(report, indent=2, sort_keys=True))
        return 0
    except (OSError, ValueError, KeyError, zipfile.BadZipFile) as error:
        print(json.dumps({"ok": False, "error": str(error)}, sort_keys=True), file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
