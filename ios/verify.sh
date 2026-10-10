#!/usr/bin/env bash
# SPDX-License-Identifier: MIT
set -euo pipefail
exec python3 "$(dirname "$0")/verify.py" "$@"
