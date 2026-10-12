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
python3 "$android_root/../tools/check-samples.py" > "$output/source-check.json"
gradle_bin="${GRADLE:-$android_root/gradlew}"
if ! "$gradle_bin" --version > "$output/gradle-bootstrap.log" 2>&1; then
    tail -n 80 "$output/gradle-bootstrap.log" >&2
    echo "Gradle Wrapper bootstrap failed; inspect gradle-bootstrap.log." >&2
    exit 2
fi
if ! grep -q '^Gradle 8\.13$' "$output/gradle-bootstrap.log"; then
    echo "Gradle 8.13 is required; the pinned Wrapper is the default. GRADLE may explicitly override its executable." >&2
    exit 2
fi
python3 - "$output/gradle-tool.json" "${GRADLE:+explicit_override}" <<'PYTHON'
import json
from pathlib import Path
import sys
Path(sys.argv[1]).write_text(json.dumps({"selection": sys.argv[2] or "pinned_wrapper",
    "gradle_version": "8.13", "wrapper_jar_sha256": "81a82aaea5abcc8ff68b3dfcb58b3c3c429378efd98e7433460610fecd7ae45f",
    "distribution_sha256": "20f1b1176237254a6fc204d8434196fa11a4cfb387567519c61556e8710aed78"}, indent=2) + "\n")
PYTHON
"$gradle_bin" -p "$android_root" \
    :quick-start:assembleDebug :quick-start:assembleDebugAndroidTest :quick-start:assembleRelease :quick-start:lintRelease :quick-start:copyResolvedSdk \
    :full-demo:assembleDebug :full-demo:assembleDebugAndroidTest :full-demo:assembleRelease :full-demo:lintRelease :full-demo:copyResolvedSdk \
    --refresh-dependencies --no-build-cache --max-workers=2 --console plain \
    > "$output/gradle.log" 2>&1 || { tail -n 100 "$output/gradle.log" >&2; exit 1; }
python3 "$android_root/scripts/verify-packaging.py" --android-root "$android_root" --output "$output"

python3 "$android_root/scripts/verify-runtime.py" --android-root "$android_root" --output "$output" --emulator-serial "$emulator_serial"
