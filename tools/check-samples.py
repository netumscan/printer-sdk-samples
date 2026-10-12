#!/usr/bin/env python3
# SPDX-License-Identifier: MIT
"""Check the source-only sample snapshot and its exact online SDK dependencies."""
import hashlib
import json
from pathlib import Path
import re
import sys

IGNORED = {".git", ".gradle", "build", "DerivedData", "SourcePackages", "xcuserdata"}


def inventory(root):
    result = {}
    for path in sorted(root.rglob("*")):
        relative = path.relative_to(root)
        if any(part in IGNORED for part in relative.parts) or path.name == ".DS_Store":
            continue
        if path.is_symlink():
            raise ValueError("Symlink is not a sample input: " + relative.as_posix())
        if path.is_file() and relative.as_posix() != "samples-manifest.json":
            result[relative.as_posix()] = hashlib.sha256(path.read_bytes()).hexdigest()
    return result


def check(root):
    manifest = json.loads((root / "samples-manifest.json").read_text())
    if manifest.get("schema") != 1 or manifest.get("hardware_verified") is not False:
        raise ValueError("Invalid samples manifest")
    expected = manifest["files_sha256"]
    for name, checksum in expected.items():
        path = Path(name)
        if path.is_absolute() or ".." in path.parts or not re.fullmatch(r"[0-9a-f]{64}", checksum):
            raise ValueError("Invalid manifest path or checksum")
    actual = inventory(root)
    if actual != expected:
        changes = [p for p in sorted(set(actual) | set(expected)) if actual.get(p) != expected.get(p)]
        raise ValueError("Sample snapshot differs: " + ", ".join(changes))
    canonical = manifest.get("canonical_full_demos", {})
    generated = manifest.get("generated_build_identity", {})
    identities = {"android/full-demo/src/main/assets/printer-sdk-samples-build.json",
                  "ios/full-demo/printer-sdk-samples-build.json"}
    if set(generated) != identities or any(actual.get(name) != checksum for name, checksum in generated.items()):
        raise ValueError("Missing or changed generated diagnostic identity")
    origins = {"platform/android/demo/src/main": "android/full-demo/src/main/",
               "platform/ios/Demo": "ios/full-demo/"}
    if set(canonical) != set(origins):
        raise ValueError("Missing canonical Full Demo source inventory")
    for origin, target in origins.items():
        files = {name: value for name, value in actual.items()
                 if name.startswith(target) and name not in generated and (origin.endswith("src/main") or name.endswith(".swift"))}
        if not files or canonical[origin] != {"target": target, "files_sha256": files}:
            raise ValueError("Full Demo provenance differs from exported sources: " + origin)
    lock = json.loads((root / "sdk-lock.json").read_text())
    version = manifest["sdk_version"]
    if lock["version"] != version or lock["hardware_verified"] is not False:
        raise ValueError("SDK lock differs from sample version")
    fingerprint = hashlib.sha256(json.dumps(canonical, sort_keys=True).encode()).hexdigest()
    for platform, name in [("android", "android/full-demo/src/main/assets/printer-sdk-samples-build.json"),
                           ("ios", "ios/full-demo/printer-sdk-samples-build.json")]:
        resource = json.loads((root / name).read_text())
        if resource.get("schema_version") != 1 or set(resource) != {"schema_version", "identity"}:
            raise ValueError("Invalid diagnostic build identity resource")
        identity = resource["identity"]
        sdk = lock.get(platform, {})
        if identity != {"sdk_distribution_version": version, "sdk_core_version": lock.get("native_sdk_version"),
                        "sdk_abi_version": 2, "sdk_abi_revision": 10, "sdk_release_commit": sdk.get("revision"),
                        "sdk_artifact_sha256": sdk.get("aar_sha256" if platform == "android" else "xcframework_sha256"),
                        "demo_source_fingerprint": fingerprint,
                        "demo_build_version": version + "+samples." + fingerprint[:12]}:
            raise ValueError("Diagnostic identity differs from SDK pin or canonical source fingerprint")
    if "Permission is hereby granted, free of charge" not in (root / "LICENSE").read_text():
        raise ValueError("Missing MIT sample license")
    for sample in ("quick-start", "full-demo"):
        text = (root / "android" / sample / "build.gradle.kts").read_text()
        if 'implementation("com.netumscan:printer-sdk-android:' + version + '")' not in text:
            raise ValueError("Android sample must use the exact public Maven coordinate")
    project = (root / "ios/PrinterSamples.xcodeproj/project.pbxproj").read_text()
    if ("https://github.com/netumscan/printer-sdk-ios" not in project or
            "kind = exactVersion" not in project or version not in project):
        raise ValueError("iOS samples must use the exact public SwiftPM version")
    if "isa = PBXProject;" in project:
        refs = {}
        for identifier, path in re.findall(r'([0-9A-F]{24}) = \{ isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = ("(?:[^"\\]|\\.)*"|[^;]+);', project):
            refs[identifier] = json.loads(path) if path.startswith('"') else path
        builds = dict(re.findall(r'([0-9A-F]{24}) = \{ isa = PBXBuildFile; fileRef = ([0-9A-F]{24});', project))
        for folder, phase in [("quick-start", "000000000000000000000026"),
                              ("full-demo", "000000000000000000000030"),
                              ("tests", "00000000000000000000003A")]:
            match = re.search(phase + r' = \{ isa = PBXSourcesBuildPhase; buildActionMask = \d+; files = \(([^)]*)\);', project)
            ids = re.findall(r"[0-9A-F]{24}", match[1]) if match else []
            compiled = ["ios/" + folder + "/" + refs.get(builds.get(item), "MISSING") for item in ids]
            expected_swift = {name for name in actual if name.startswith("ios/" + folder + "/") and name.endswith(".swift")}
            if len(compiled) != len(set(compiled)) or set(compiled) != expected_swift:
                raise ValueError("Xcode source membership differs from exported Swift files: " + folder)
    prohibited = re.compile(
        r"/Users/[A-Za-z0-9_.-]+/|/home/[A-Za-z0-9_.-]+/|XCLocalSwiftPackageReference|mavenLocal\s*\(|"
        r"implementation\s*\(\s*project\s*\(|@[A-Z][A-Z0-9_]*@|"
        r"-----BEGIN (?:RSA |EC |OPENSSH |PGP )?PRIVATE KEY|"
        r"DEVELOPMENT_TEAM\s*=\s*[A-Z0-9]+|PROVISIONING_PROFILE(?:_SPECIFIER)?\s*=\s*[^;\s]+")
    for name in actual:
        path = root / name
        if path.suffix.lower() in {".aar", ".jar", ".so", ".a", ".ipa", ".apk", ".aab", ".p12", ".jks", ".keystore", ".mobileprovision"}:
            raise ValueError("Binary/signing material is not a sample source: " + name)
        if "xcuserdata" in path.parts or "xcframework" in name or name.endswith("local.properties"):
            raise ValueError("Private/generated input found: " + name)
        # This checker contains the rejection patterns itself.
        if name == "tools/check-samples.py":
            continue
        try:
            text = path.read_text()
        except UnicodeDecodeError:
            raise ValueError("Unexpected binary sample source: " + name) from None
        if prohibited.search(text):
            raise ValueError("Local dependency, signing value or unresolved template: " + name)
        if path.suffix == ".md":
            for link in re.findall(r"(?<!!)\[[^\]\n]+\]\(([^)\n]+)\)", text):
                if "://" in link or link.startswith(("#", "mailto:")):
                    continue
                target = link.split("#", 1)[0]
                linked = (path.parent / target).resolve()
                if target and (not linked.is_relative_to(root.resolve()) or not linked.exists()):
                    raise ValueError("Broken sample documentation link in " + name + ": " + link)
    return {"verified": True, "sdk_version": version, "files_verified": len(actual), "hardware_verified": False}


if __name__ == "__main__":
    try:
        print(json.dumps(check(Path(__file__).resolve().parents[1]), sort_keys=True))
    except (ValueError, KeyError, OSError, json.JSONDecodeError) as error:
        sys.exit(str(error))
