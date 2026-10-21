#!/usr/bin/env python3
# SPDX-License-Identifier: MIT
"""Validate the optional Flutter source consumer without relaxing SDK boundaries."""
import hashlib
import json
from pathlib import Path
import re
import struct
import zlib

PREFIX = "flutter/"
LOCAL_FLUTTER = "Flutter/ephemeral/Packages/FlutterGeneratedPluginSwiftPackage"
PROJECT = "flutter/ios/Runner.xcodeproj/project.pbxproj"
RESOLVED = "flutter/ios/Runner.xcodeproj/project.xcworkspace/xcshareddata/swiftpm/Package.resolved"
PNG = re.compile(r"flutter/(?:android/app/src/main/res/mipmap-[a-z]+/ic_launcher|ios/Runner/Assets\.xcassets/(?:AppIcon\.appiconset/Icon-App-[^/]+|LaunchImage\.imageset/LaunchImage[^/]*))\.png")
IGNORED = {".dart_tool", ".gradle", "build", "ephemeral", "Pods", ".symlinks", ".swiftpm", "__pycache__"}


def sha(value):
    return hashlib.sha256(value).hexdigest()


def validate_lock(lock):
    version = lock.get("version")
    if version != "0.3.0-preview.10" or lock.get("native_sdk_version") != "0.3.0" or lock.get("c_abi") != "3/r2" or lock.get("hardware_verified") is not False:
        raise ValueError("Invalid Flutter SDK lock")
    android, ios = lock.get("android", {}), lock.get("ios", {})
    if (android.get("coordinate") != "com.netumscan:printer-sdk-android:" + version
            or android.get("repository") != "https://repo.maven.apache.org/maven2"
            or ios.get("repository") != "https://github.com/netumscan/printer-sdk-ios"
            or ios.get("version") != version):
        raise ValueError("Flutter requires exact published Android and iOS SDK identities")
    for platform, digest in [(android, "aar_sha256"), (ios, "xcframework_sha256")]:
        if not re.fullmatch(r"[0-9a-f]{40}", platform.get("revision", "")) or not re.fullmatch(r"[0-9a-f]{64}", platform.get(digest, "")):
            raise ValueError("Invalid Flutter SDK artifact lock")


def validate_png(name, data):
    if not PNG.fullmatch(name) or not data.startswith(b"\x89PNG\r\n\x1a\n"):
        raise ValueError("Unexpected Flutter binary resource: " + name)
    offset, kinds = 8, []
    while offset < len(data):
        if offset + 12 > len(data):
            raise ValueError("Invalid PNG resource: " + name)
        size = struct.unpack(">I", data[offset:offset + 4])[0]
        chunk = data[offset + 4:offset + 8]
        end = offset + 12 + size
        if end > len(data) or zlib.crc32(data[offset + 4:end - 4]) & 0xffffffff != struct.unpack(">I", data[end - 4:end])[0]:
            raise ValueError("Invalid PNG resource: " + name)
        if chunk == b"tEXt" and data[offset + 8:end - 4] not in {
                b"Software\x00Adobe ImageReady", b"date:create\x002019-06-22T18:46:55+00:00", b"date:modify\x002019-06-22T18:46:55+00:00"}:
            raise ValueError("Unreviewed PNG metadata: " + name)
        if chunk not in {b"IHDR", b"IDAT", b"IEND", b"sRGB", b"gAMA", b"cHRM", b"pHYs", b"PLTE", b"tRNS", b"bKGD", b"tEXt"}:
            raise ValueError("Unreviewed PNG metadata: " + name)
        kinds.append(chunk)
        offset = end
    if not kinds or kinds[0] != b"IHDR" or kinds[-1] != b"IEND" or b"IDAT" not in kinds:
        raise ValueError("Invalid PNG resource: " + name)


def validated_flutter_project(text):
    # Only Flutter's standard generated integration package may remain local.
    blocks = re.findall(r'([A-F0-9]{24}) /\* XCLocalSwiftPackageReference [^\n]+\*/ = \{\s*isa = XCLocalSwiftPackageReference;\s*relativePath = ([^;]+);\s*\};', text)
    if len(blocks) != 1 or blocks[0][1] != LOCAL_FLUTTER:
        raise ValueError("Flutter permits only its standard generated local package")
    # Remove only the fully validated standard block/comments before the shared
    # scanner rejects every other local package reference.
    scrubbed = re.sub(r'[A-F0-9]{24} /\* XCLocalSwiftPackageReference [^\n]+\*/ = \{\s*isa = XCLocalSwiftPackageReference;\s*relativePath = ' + re.escape(LOCAL_FLUTTER) + r';\s*\};', "", text)
    scrubbed = re.sub(r'/\* XCLocalSwiftPackageReference "' + re.escape(LOCAL_FLUTTER) + r'" \*/', "", scrubbed)
    scrubbed = scrubbed.replace("/* Begin XCLocalSwiftPackageReference section */", "").replace("/* End XCLocalSwiftPackageReference section */", "")
    return scrubbed


def check(root, manifest, actual):
    node = manifest.get("flutter_demo")
    if not isinstance(node, dict) or node.get("sdk_lock") != PREFIX + "sdk-lock.json":
        raise ValueError("Invalid Flutter manifest node")
    lock = json.loads((root / node["sdk_lock"]).read_text())
    validate_lock(lock)
    if lock != json.loads((root / "sdk-lock.json").read_text()):
        raise ValueError("Flutter SDK lock differs from the public root SDK lock")
    canonical = node.get("canonical_sources")
    if not isinstance(canonical, dict) or set(canonical) != {"samples/flutter"}:
        raise ValueError("Missing canonical Flutter provenance")
    sources = {name: checksum for name, checksum in actual.items() if name.startswith(PREFIX)
               and name not in {PREFIX + "README.md", PREFIX + "LICENSE", node["sdk_lock"], RESOLVED}}
    if not sources or canonical["samples/flutter"] != {"target": PREFIX, "files_sha256": sources}:
        raise ValueError("Flutter provenance differs from exported sources")
    if node.get("source_fingerprint") != sha(json.dumps(canonical, sort_keys=True).encode()):
        raise ValueError("Flutter source fingerprint differs")
    settings = (root / PREFIX / "android/settings.gradle.kts").read_text()
    includes = re.findall(r'includeBuild\(([^\n]+)\)', settings)
    if includes != ['"$flutterSdkPath/packages/flutter_tools/gradle"'] or "dependencySubstitution" in settings:
        raise ValueError("Flutter Android may only include the standard Flutter tooling build")
    gradle = (root / PREFIX / "android/app/build.gradle.kts").read_text()
    coordinates = re.findall(r'implementation\("(com\.netumscan:printer-sdk-android:[^"]+)"\)', gradle)
    if (coordinates != [lock["android"]["coordinate"]] or "../../../VERSION" in gradle
            or len(re.findall(r'\b(?:implementation|api|compileOnly|runtimeOnly)\s*\(', gradle)) != 1):
        raise ValueError("Flutter Android must use its exact Maven SDK pin")
    for name in actual:
        if name.startswith(PREFIX) and name.endswith((".gradle", ".gradle.kts")):
            if re.search(r'\bflatDir\b|\bfileTree\s*\(|\bfiles\s*\(', (root / name).read_text()):
                raise ValueError("Flutter Android cannot use local SDK artifacts")
    pubspec = (root / PREFIX / "pubspec.yaml").read_text()
    if re.search(r'^\s*(?:path|dependency_overrides|workspace)\s*:', pubspec, re.M):
        raise ValueError("Flutter cannot declare local package dependencies")
    text = (root / PROJECT).read_text()
    validated_flutter_project(text)
    urls = re.findall(r'\brepositoryURL\s*=\s*"([^"\n]+)"\s*;', text)
    versions = re.findall(r'\bversion\s*=\s*"?([^";\s]+)"?\s*;', text)
    kinds = re.findall(r'\bkind\s*=\s*([^;\s]+)\s*;', text)
    if urls != [lock["ios"]["repository"]] or versions != [lock["version"]] or kinds != ["exactVersion"]:
        raise ValueError("Flutter iOS must use its exact remote SwiftPM SDK pin")
    pins = json.loads((root / RESOLVED).read_text()).get("pins")
    if pins != [{"identity": "printer-sdk-ios", "kind": "remoteSourceControl", "location": lock["ios"]["repository"],
                 "state": {"revision": lock["ios"]["revision"], "version": lock["version"]}}]:
        raise ValueError("Flutter resolved SDK identity differs from its lock")
    if (root / PREFIX / "LICENSE").read_bytes() != (root / "LICENSE").read_bytes():
        raise ValueError("Missing MIT Flutter sample license")
    return {"sdk_version": lock["version"], "files_verified": len(sources), "hardware_verified": False}
