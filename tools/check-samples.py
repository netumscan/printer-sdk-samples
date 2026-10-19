#!/usr/bin/env python3
# SPDX-License-Identifier: MIT
"""Check the source-only sample snapshot and its exact online SDK dependencies."""
import hashlib
import json
import os
from pathlib import Path
import re
import stat
import sys
import types
sys.dont_write_bytecode = True
from sdk_contract import abi_identity

WRAPPER_JAR = "android/gradle/wrapper/gradle-wrapper.jar"
WRAPPER_SHA256 = "81a82aaea5abcc8ff68b3dfcb58b3c3c429378efd98e7433460610fecd7ae45f"
WRAPPER_INPUTS = {
    ".gitattributes": "01e94d407cac570df3b4a66f2d76e291f19aa22ec4a73cb9a41be03a83f02bfe",
    "android/gradlew": "734b3879d3501dce471cf0522d3bcbafe76873d9fc5129345b67fb43bd15e933",
    "android/gradlew.bat": "57931b17dd228e5c24dac90e815d0bf82477e831a4618dfab4136f5446b42a9f",
    WRAPPER_JAR: WRAPPER_SHA256,
    "android/gradle/wrapper/LICENSE": "0d542e0c8804e39aa7f37eb00da5a762149dc682d7829451287e11b938e94594",
}
WRAPPER_PROPERTIES = {
    "distributionBase": "GRADLE_USER_HOME", "distributionPath": "wrapper/dists",
    "distributionSha256Sum": "20f1b1176237254a6fc204d8434196fa11a4cfb387567519c61556e8710aed78",
    "distributionUrl": r"https\://services.gradle.org/distributions/gradle-8.13-bin.zip",
    "networkTimeout": "10000", "validateDistributionUrl": "true",
    "zipStoreBase": "GRADLE_USER_HOME", "zipStorePath": "wrapper/dists",
}
IGNORED = {".git", ".gradle", "build", "DerivedData", "SourcePackages", "xcuserdata"}
IOS_DEPENDENCY_DOCS = ("ios/README.md", "ios/quick-start/README.md", "ios/full-demo/README.md")


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


def check_wrapper(root, actual, manifest):
    if manifest.get("executable_files") != ["android/gradlew"]:
        raise ValueError("Invalid executable-file allowlist; only the official gradlew is executable")
    for name, expected in WRAPPER_INPUTS.items():
        if actual.get(name) != expected:
            raise ValueError("Missing or changed official Gradle 8.13 Wrapper input: " + name)
    properties = root / "android/gradle/wrapper/gradle-wrapper.properties"
    values = {}
    for line in properties.read_text().splitlines():
        if not line.strip() or line.lstrip().startswith(("#", "!")):
            continue
        key, separator, value = line.partition("=")
        if not separator or key in values:
            raise ValueError("Malformed or duplicate Gradle Wrapper property")
        values[key] = value
    if values != WRAPPER_PROPERTIES:
        raise ValueError("Gradle Wrapper properties must pin the official HTTPS 8.13 distribution, checksum and timeout")
    if os.name == "posix":
        if stat.S_IMODE((root / "android/gradlew").stat().st_mode) != 0o755:
            raise ValueError("android/gradlew must have executable mode 0755")
        if stat.S_IMODE((root / "android/gradlew.bat").stat().st_mode) != 0o644:
            raise ValueError("android/gradlew.bat must have ordinary mode 0644")


def check_ios_dependencies(root, lock):
    version = lock["version"]
    ios = lock.get("ios", {})
    if (ios.get("version") != version or ios.get("repository") not in {
            "https://github.com/netumscan/printer-sdk-ios", "https://github.com/netumscan/printer-sdk-ios.git"}
            or not isinstance(ios.get("revision"), str) or not re.fullmatch(r"[0-9a-f]{40}", ios["revision"])):
        raise ValueError("SDK lock has an invalid current iOS dependency identity")
    repository = ios["repository"].removesuffix(".git")
    project = (root / "ios/PrinterSamples.xcodeproj/project.pbxproj").read_text()
    urls = re.findall(r'\brepositoryURL\s*=\s*"([^"\n]+)"\s*;', project)
    versions = re.findall(r'\bversion\s*=\s*"?([^";\s]+)"?\s*;', project)
    kinds = re.findall(r'\bkind\s*=\s*([^;\s]+)\s*;', project)
    if ([url.removesuffix(".git") for url in urls] != [repository]
            or versions != [version] or kinds != ["exactVersion"]):
        raise ValueError("iOS samples must use the exact public SwiftPM version")
    resolved = root / "ios/PrinterSamples.xcodeproj/project.xcworkspace/xcshareddata/swiftpm/Package.resolved"
    pins = json.loads(resolved.read_text()).get("pins")
    if (not isinstance(pins, list) or len(pins) != 1 or not isinstance(pins[0], dict)
            or pins[0].get("identity") != "printer-sdk-ios" or pins[0].get("kind") != "remoteSourceControl"
            or pins[0].get("location", "").removesuffix(".git") != repository
            or pins[0].get("state") != {"version": version, "revision": ios["revision"]}):
        raise ValueError("SwiftPM resolved SDK identity differs from sdk-lock.json")
    expected = {"Repository": repository + ".git", "Exact version": version, "Release commit": ios["revision"]}
    for name in IOS_DEPENDENCY_DOCS:
        text = (root / name).read_text()
        sections = re.findall(r"^## Current SDK dependency\s*\n(.*?)(?=^## |\Z)", text, re.M | re.S)
        if len(sections) != 1:
            raise ValueError("Missing or duplicate current iOS SDK dependency declaration in " + name)
        # Only this current declaration is compared. Older identities may appear
        # in separately headed migration or release-history sections.
        rows = re.findall(r"^\|\s*([^|]+?)\s*\|\s*`([^`\n]+)`\s*\|\s*$", sections[0], re.M)
        if len(rows) != len(expected) or dict(rows) != expected:
            raise ValueError("Current iOS SDK dependency differs from sdk-lock.json in " + name)


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
    check_wrapper(root, actual, manifest)
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
    abi_version, abi_revision = abi_identity(lock)
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
                        "sdk_abi_version": abi_version, "sdk_abi_revision": abi_revision, "sdk_release_commit": sdk.get("revision"),
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
    check_ios_dependencies(root, lock)
    project = (root / "ios/PrinterSamples.xcodeproj/project.pbxproj").read_text()
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
    reference_tool = root / "tools/check-api-reference.py"
    if not reference_tool.is_file():
        raise ValueError("Missing complete Java/Swift API reference checker")
    reference = types.ModuleType("samples_api_reference_gate")
    reference.__file__ = str(reference_tool)
    exec(compile(reference_tool.read_bytes(), str(reference_tool), "exec"), reference.__dict__)
    reference_report = reference.check(root)
    history_tool = root / "tools/check-api-history.py"
    if history_tool.is_symlink() or not history_tool.is_file():
        raise ValueError("Missing published API history checker")
    history = types.ModuleType("samples_api_history_gate")
    history.__file__ = str(history_tool)
    exec(compile(history_tool.read_bytes(), str(history_tool), "exec"), history.__dict__)
    history_report = history.check(root)
    # Only these exact, fully verified outputs may contain reviewed compiler
    # identifiers (for example c:@T@NSTimeInterval), never arbitrary templates.
    reference_files = {"docs/api-reference.json", "docs/android-java-api.md", "docs/ios-swift-api.md",
                       "docs/api-history.json"}
    prohibited = re.compile(
        r"/Users/[A-Za-z0-9_.-]+/|/home/[A-Za-z0-9_.-]+/|XCLocalSwiftPackageReference|mavenLocal\s*\(|"
        r"implementation\s*\(\s*project\s*\(|"
        r"-----BEGIN (?:RSA |EC |OPENSSH |PGP )?PRIVATE KEY|"
        r"DEVELOPMENT_TEAM\s*=\s*[A-Z0-9]+|PROVISIONING_PROFILE(?:_SPECIFIER)?\s*=\s*[^;\s]+")
    for name in actual:
        path = root / name
        if name == WRAPPER_JAR:
            # Exact path, upstream checksum and associated license were checked above.
            continue
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
        if prohibited.search(text) or (name not in reference_files and re.search(r"@[A-Z][A-Z0-9_]*@", text)):
            raise ValueError("Local dependency, signing value or unresolved template: " + name)
        if path.suffix == ".md":
            for link in re.findall(r"(?<!!)\[[^\]\n]+\]\(([^)\n]+)\)", text):
                if "://" in link or link.startswith(("#", "mailto:")):
                    continue
                target = link.split("#", 1)[0]
                linked = (path.parent / target).resolve()
                if target and (not linked.is_relative_to(root.resolve()) or not linked.exists()):
                    raise ValueError("Broken sample documentation link in " + name + ": " + link)
    return {"verified": True, "sdk_version": version, "files_verified": len(actual), "hardware_verified": False,
            "api_reference": reference_report, "api_history": history_report}


if __name__ == "__main__":
    try:
        print(json.dumps(check(Path(__file__).resolve().parents[1]), sort_keys=True))
    except (ValueError, KeyError, OSError, json.JSONDecodeError) as error:
        sys.exit(str(error))
