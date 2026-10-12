#!/usr/bin/env bash
# SPDX-License-Identifier: MIT
set -euo pipefail

android_root="$(cd "$(dirname "$0")" && pwd)"
output="${android_root}/build/verification"
emulator_serial=""
while [[ $# -gt 0 ]]; do
    case "$1" in
        --output) output="$2"; shift 2 ;;
        --emulator-serial) emulator_serial="$2"; shift 2 ;;
        *) echo "Usage: bash android/verify.sh --emulator-serial emulator-N [--output DIRECTORY]" >&2; exit 2 ;;
    esac
done
if [[ ! "$emulator_serial" =~ ^emulator-[0-9]+$ ]]; then
    echo "Select an Android emulator with --emulator-serial emulator-N; physical devices are refused." >&2
    exit 2
fi
mkdir -p "$output"
output="$(cd "$output" && pwd)"
gradle_bin="${GRADLE:-gradle}"
if ! "$gradle_bin" --version | grep -q '^Gradle 8\.13$'; then
    echo "Install Gradle 8.13 or set GRADLE to its executable." >&2
    exit 2
fi
"$gradle_bin" -p "$android_root" \
    :quick-start:assembleDebug :quick-start:assembleDebugAndroidTest :quick-start:assembleRelease :quick-start:lintRelease :quick-start:copyResolvedSdk \
    :full-demo:assembleDebug :full-demo:assembleDebugAndroidTest :full-demo:assembleRelease :full-demo:lintRelease :full-demo:copyResolvedSdk \
    --refresh-dependencies --no-build-cache --max-workers=2 --console plain \
    > "$output/gradle.log" 2>&1 || { tail -n 100 "$output/gradle.log" >&2; exit 1; }
python3 "$android_root/scripts/verify-packaging.py" --android-root "$android_root" --output "$output"

python3 "$android_root/scripts/verify-runtime.py" --android-root "$android_root" --output "$output" --emulator-serial "$emulator_serial"
