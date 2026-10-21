#!/usr/bin/env bash
# SPDX-License-Identifier: MIT
set -euo pipefail
android_root="$(cd "$(dirname "$0")" && pwd)"
exec python3 "$android_root/scripts/verify-mixed.py" --android-root "$android_root" "$@"
