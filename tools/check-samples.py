#!/usr/bin/env python3
# SPDX-License-Identifier: MIT
"""Check mixed samples without relabeling the historical root SDK lock."""
import importlib.util
from pathlib import Path
import sys
sys.dont_write_bytecode = True
_spec = importlib.util.spec_from_file_location("android_demo_overlay", Path(__file__).with_name("check-android-demo-overlay.py"))
_gate = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(_gate)
check = _gate.check

if __name__ == "__main__":
    import json
    try:
        print(json.dumps(check(Path(__file__).resolve().parents[1]), sort_keys=True))
    except (ValueError, KeyError, OSError) as error:
        raise SystemExit(str(error)) from None
