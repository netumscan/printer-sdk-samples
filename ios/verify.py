#!/usr/bin/env python3
# SPDX-License-Identifier: MIT
"""Build both samples using the exact public SDK and run hardware-free simulator tests."""
import argparse
import hashlib
import json
import plistlib
import platform
import re
import subprocess
import time
import zipfile
from pathlib import Path

IOS = Path(__file__).resolve().parent
VERSION = "0.2.0-preview.2"
REVISION = "b629ccc1aeddf0370a3845cfb3f47152df0e9a5d"
CHECKSUM = "c0e7afc018ef533ab5868867446897d77b6f6b0f6531c8639b1d3699287fdeab"
REPOSITORY = "https://github.com/netumscan/printer-sdk-ios"
ARTIFACT_URL = REPOSITORY + "/releases/download/v" + VERSION + "/PrinterSDK.xcframework.zip"
SCHEMES = {"PrinterQuickStart": "quickstart", "PrinterFullDemo": "fulldemo"}


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def inventory(directory):
    values = {}
    for path in sorted(directory.rglob("*")):
        if path.is_symlink():
            raise SystemExit("Unexpected symlink: " + str(path))
        if path.is_file():
            values[str(path.relative_to(directory))] = digest(path)
    return values


def validate_pin(path):
    value = json.loads(path.read_text())
    pins = value.get("pins", [])
    if len(pins) != 1:
        raise SystemExit("Expected exactly one pinned remote SDK")
    pin = pins[0]
    if (pin.get("identity") != "printer-sdk-ios" or pin.get("kind") != "remoteSourceControl"
            or pin.get("location", "").removesuffix(".git") != REPOSITORY
            or pin.get("state") != {"revision": REVISION, "version": VERSION}):
        raise SystemExit("Resolved SDK version or commit differs from sdk-lock.json")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, default=IOS.parent / "build/ios")
    parser.add_argument("--simulator-udid", help="An available iOS simulator; never a physical device")
    parser.add_argument("--skip-runtime", action="store_true", help="Build only; omit simulator tests and launches")
    args = parser.parse_args()
    output = args.output.resolve()
    if output.exists():
        raise SystemExit("Verification output must be a new directory: " + str(output))
    if output == IOS or IOS in output.parents:
        raise SystemExit("Keep verification outputs outside the iOS source directory")
    output.mkdir(parents=True)
    logs = output / "logs"
    logs.mkdir()
    sequence = 0

    def run(command, timeout=600):
        nonlocal sequence
        sequence += 1
        command = list(map(str, command))
        print("+ " + " ".join(command), flush=True)
        log = logs / f"{sequence:02d}.log"
        with log.open("w") as stream:
            result = subprocess.run(command, stdout=stream, stderr=subprocess.STDOUT, timeout=timeout)
        if result.returncode:
            print(log.read_text()[-10000:])
            result.check_returncode()
        return log.read_text()

    project = IOS / "PrinterSamples.xcodeproj"
    resolved = project / "project.xcworkspace/xcshareddata/swiftpm/Package.resolved"
    validate_pin(resolved)
    pinned_bytes = resolved.read_bytes()
    common = ["xcodebuild", "-project", project, "-disablePackageRepositoryCache",
              "-packageCachePath", output / "package-cache", "-clonedSourcePackagesDirPath", output / "source-packages",
              "-onlyUsePackageVersionsFromResolvedFile"]
    run(common + ["-scheme", "PrinterQuickStart", "-resolvePackageDependencies"])
    validate_pin(resolved)
    if resolved.read_bytes() != pinned_bytes:
        raise SystemExit("SwiftPM rewrote the committed resolution; update it explicitly before publishing")
    state = json.loads((output / "source-packages/workspace-state.json").read_text())["object"]
    artifacts = state.get("artifacts", [])
    if len(artifacts) != 1:
        raise SystemExit("Expected exactly one downloaded XCFramework")
    artifact = artifacts[0]
    if (artifact.get("targetName") != "CPrinterSDK"
            or artifact.get("packageRef", {}).get("identity") != "printer-sdk-ios"
            or artifact.get("source") != {"type": "remote", "url": ARTIFACT_URL, "checksum": CHECKSUM}):
        raise SystemExit("SwiftPM did not use the locked public binary URL and checksum")
    binary = Path(artifact["path"]).resolve()
    if not binary.is_relative_to((output / "source-packages/artifacts").resolve()):
        raise SystemExit("Resolved binary did not come from the fresh online artifact directory")
    archive = output / "PrinterSDK.xcframework.zip"
    run(["curl", "--fail", "--location", "--retry", "3", "--output", archive, ARTIFACT_URL])
    if digest(archive) != CHECKSUM:
        raise SystemExit("Downloaded binary archive checksum differs from sdk-lock.json")
    archived = {}
    with zipfile.ZipFile(archive) as bundle:
        for item in bundle.infolist():
            path = Path(item.filename)
            if (not path.parts or path.parts[0] != "CPrinterSDK.xcframework" or ".." in path.parts
                    or path.is_absolute() or (item.external_attr >> 16) & 0o170000 == 0o120000):
                raise SystemExit("Unexpected binary archive entry")
            if item.is_dir():
                continue
            name = str(path.relative_to("CPrinterSDK.xcframework"))
            if name in archived:
                raise SystemExit("Duplicate binary archive entry")
            archived[name] = hashlib.sha256(bundle.read(item)).hexdigest()
    if not archived or inventory(binary) != archived:
        raise SystemExit("SwiftPM unpacked bytes differ from the separately downloaded signed-off archive")
    frameworks = plistlib.loads((binary / "Info.plist").read_bytes())["AvailableLibraries"]
    platforms = {(value["SupportedPlatform"], value.get("SupportedPlatformVariant", "")):
                 set(value["SupportedArchitectures"]) for value in frameworks}
    if platforms != {("ios", ""): {"arm64"}, ("ios", "simulator"): {"arm64", "x86_64"}}:
        raise SystemExit("Expected iOS arm64 and simulator arm64/x86_64 binary architectures")
    builds = []
    simulator_apps = {}
    simulator_arch = "arm64" if platform.machine() == "arm64" else "x86_64"
    for scheme, suffix in SCHEMES.items():
        for sdk, arch in [("iphoneos", "arm64"), ("iphonesimulator", "arm64"), ("iphonesimulator", "x86_64")]:
            derived = output / f"derived-{scheme}-{sdk}-{arch}"
            run(common + ["-scheme", scheme, "-configuration", "Release", "-sdk", sdk,
                          "-destination", "generic/platform=iOS" if sdk == "iphoneos" else "generic/platform=iOS Simulator",
                          "-derivedDataPath", derived, f"ARCHS={arch}", "ONLY_ACTIVE_ARCH=NO", "CODE_SIGNING_ALLOWED=NO", "build"])
            app = derived / f"Build/Products/Release-{sdk}/{scheme}.app"
            framework = app / "Frameworks/CPrinterSDK.framework"
            app_info = plistlib.loads((app / "Info.plist").read_bytes())
            framework_info = plistlib.loads((framework / "Info.plist").read_bytes())
            if app_info.get("CFBundleIdentifier") != "com.netumscan.printersdk.samples." + suffix:
                raise SystemExit("Unexpected sample application identity")
            if framework_info.get("CFBundleIdentifier") != "com.netumscan.printersdk.core":
                raise SystemExit("Unexpected embedded SDK identity")
            run(["xcrun", "lipo", app / scheme, "-verify_arch", arch])
            run(["xcrun", "lipo", framework / "CPrinterSDK", "-verify_arch", arch])
            symbols = run(["xcrun", "nm", "-gU", framework / "CPrinterSDK"])
            exports = set(re.findall(r"\b(_psdk_[A-Za-z0-9_]+)\s*$", symbols, re.MULTILINE))
            if len(exports) != 49:
                raise SystemExit("Expected 49 C ABI exports in the embedded framework")
            privacy = list(app.rglob("PrivacyInfo.xcprivacy"))
            if (len(privacy) != 1 or plistlib.loads(privacy[0].read_bytes())
                    .get("NSPrivacyAccessedAPITypes", [{}])[0].get("NSPrivacyAccessedAPITypeReasons") != ["35F9.1"]):
                raise SystemExit("SDK privacy resource missing or changed")
            builds.append({"scheme": scheme, "architecture": f"{sdk}-{arch}",
                           "bundle_identifier": app_info["CFBundleIdentifier"], "sdk_c_exports": len(exports),
                           "dynamic_framework_embedded": True, "privacy_resource_embedded": True})
            if sdk == "iphonesimulator" and arch == simulator_arch:
                simulator_apps[scheme] = app
    evidence = {"success": True, "sdk_version": VERSION, "sdk_revision": REVISION,
                "repository_url": REPOSITORY, "artifact_url": ARTIFACT_URL, "binary_sha256": CHECKSUM,
                "online_only": True, "fresh_package_cache": True, "download_matches_swiftpm_artifact": True,
                "builds": builds, "hardware_verified": False, "physical_device_installed": False,
                "simulator_runtime": False}
    if not args.skip_runtime:
        devices = json.loads(run(["xcrun", "simctl", "list", "devices", "available", "--json"]))["devices"]
        candidates = [device for runtime, group in devices.items() if ".iOS-" in runtime
                      for device in group if device.get("isAvailable") and device["name"].startswith("iPhone")]
        if args.simulator_udid:
            candidates = [device for device in candidates if device["udid"] == args.simulator_udid]
        else:
            candidates.sort(key=lambda device: (device["state"] == "Booted", device["name"]), reverse=True)
        if not candidates:
            raise SystemExit("No available iPhone simulator; install an iOS runtime or use --skip-runtime")
        simulator = candidates[0]
        udid = simulator["udid"]
        if simulator["state"] != "Booted":
            run(["xcrun", "simctl", "boot", udid])
        run(["xcrun", "simctl", "bootstatus", udid, "-b"])
        result_bundle = output / "Tests.xcresult"
        run(common + ["-scheme", "PrinterQuickStart", "-configuration", "Debug", "-destination", "id=" + udid,
                      "-derivedDataPath", output / "derived-tests", "-resultBundlePath", result_bundle,
                      "-parallel-testing-enabled", "NO", f"ARCHS={simulator_arch}", "ONLY_ACTIVE_ARCH=YES",
                      "CODE_SIGN_IDENTITY=-", "test"], timeout=900)
        summary = json.loads(run(["xcrun", "xcresulttool", "get", "test-results", "summary", "--path", result_bundle]))
        if (summary.get("passedTests") != 4 or summary.get("failedTests") != 0
                or summary.get("skippedTests") != 0 or summary.get("totalTestCount") != 4):
            raise SystemExit("Expected four executed, passed hardware-free sample tests")
        launched = []
        for scheme, app in simulator_apps.items():
            run(["codesign", "--force", "--deep", "--sign", "-", app])
            run(["xcrun", "simctl", "install", udid, app])
            bundle_id = "com.netumscan.printersdk.samples." + SCHEMES[scheme]
            run(["xcrun", "simctl", "launch", "--terminate-running-process", udid, bundle_id])
            time.sleep(1)
            processes = run(["xcrun", "simctl", "spawn", udid, "launchctl", "list"])
            if bundle_id not in processes:
                raise SystemExit("Sample app exited during simulator launch: " + scheme)
            run(["xcrun", "simctl", "io", udid, "screenshot", output / (scheme + ".png")])
            launched.append(scheme)
        evidence.update(simulator_runtime=True, simulator_name=simulator["name"], tests_passed=4, tests_failed=0,
                        tests_skipped=0, simulator_apps_launched=launched)
    if resolved.read_bytes() != pinned_bytes:
        raise SystemExit("Committed dependency resolution changed during verification")
    (output / "validation.json").write_text(json.dumps(evidence, indent=2) + "\n")
    print("PASS: both iOS samples build using the exact online SDK" +
          ("; 4 tests and 2 simulator launches passed" if evidence["simulator_runtime"] else "; runtime omitted"))


if __name__ == "__main__":
    main()
