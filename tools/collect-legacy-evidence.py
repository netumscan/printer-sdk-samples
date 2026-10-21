#!/usr/bin/env python3
# SPDX-License-Identifier: MIT
"""Capture verification failures and export bounded copies with known-pattern masking.

This is a diagnostic copy, not a replacement for original verification receipts.
Known-pattern masking cannot guarantee that arbitrary application text is secret-free.
No environment, global configuration, credential store or build tree is enumerated.
"""
from __future__ import annotations

import argparse
import codecs
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import re
import stat
import subprocess
import sys
import time

sys.dont_write_bytecode = True
MAX_FILE = 256 * 1024
MAX_READ = 8 * 1024 * 1024
MAX_TOTAL = 4 * 1024 * 1024
MAX_FILES = 64
MAX_SCAN = 128
ANDROID_FILES = ("validation.json", "runtime-validation.json", "instrumentation-result.json",
                 "quick-start-runtime.json", "full-demo-journal.log", "gradle.log", "adb.log",
                 "source-check.json", "gradle-bootstrap.log", "gradle-tool.json")
SECRET_KEY = re.compile(r"(?:.*(?:password|passwd|passphrase|secret|token|authorization|cookie|credential|private.?key|"
                        r"api.?key|access.?key|signingInMemoryKey).*|raw(?:_command|_data|_bytes|_content)?|payload|content|image|data|bytes|text|"
                        r"printer_name|device_name|serial|serial_number|bluetooth_address|mac_address|uuid|udid)\Z", re.I)


def now():
    return datetime.now(timezone.utc).isoformat()


def mask_text(value):
    value = re.sub(r"-----BEGIN [^-\n]*PRIVATE KEY(?: BLOCK)?-----.*?(?:-----END [^-\n]*PRIVATE KEY(?: BLOCK)?-----|\Z)",
                   "[REDACTED PRIVATE KEY]", value, flags=re.S)
    # Headers can use arbitrary authentication schemes and cookie attributes;
    # replacing a first whitespace-delimited word would leave credentials behind.
    value = re.sub(r"(?im)(\b(?:authorization|proxy-authorization|cookie|set-cookie)\s*[:=]\s*)[^\r\n]*",
                   r"\1[REDACTED HEADER]", value)
    value = re.sub(r"(?i)\bBearer\s+[A-Za-z0-9._~+/=\-]+", "Bearer [REDACTED]", value)
    value = re.sub(r"(?i)\bBasic\s+[A-Za-z0-9+/=]+", "Basic [REDACTED]", value)
    value = re.sub(r"(?i)(\b[A-Za-z0-9_.-]*(?:password|passwd|passphrase|secret|token|credential|cookie|"
                   r"private.?key|api[_-]?key|access[_-]?key|signingInMemoryKey)[A-Za-z0-9_.-]*"
                   r"[\"']?\s*[:=]\s*)(?:\"[^\"]*\"|'[^']*'|[^\s,;}]+)", r"\1[REDACTED]", value)
    value = re.sub(r"(?i)(\b(?:authorization|password|passwd|passphrase|secret|token|api[_-]?key|access[_-]?key|"
                   r"raw[_-]?(?:command|data|bytes|content)|payload|content|printer[_-]?name|device[_-]?name)"
                   r"[\"']?\s*[:=]\s*)(?:\"[^\"]*\"|'[^']*'|[^\s,;}]+)", r"\1[REDACTED]", value)
    value = re.sub(r"\b(?:gh[pousr]_[A-Za-z0-9_]+|github_pat_[A-Za-z0-9_]+|glpat-[A-Za-z0-9_-]+|"
                   r"xox[baprs]-[A-Za-z0-9-]+|AKIA[A-Z0-9]{16})\b", "[REDACTED TOKEN]", value)
    value = re.sub(r"\beyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\b", "[REDACTED JWT]", value)
    value = re.sub(r"(?i)\b(?:[0-9a-f]{2}[:-]){5}[0-9a-f]{2}\b", "[REDACTED MAC]", value)
    value = re.sub(r"(?i)\b[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\b", "[REDACTED UUID]", value)
    value = re.sub(r"(?i)(?:/Users/|/home/)[^/\s\"'<>]+", "/[REDACTED HOME]", value)
    value = re.sub(r"(?i)[A-Z]:[\\/]Users[\\/][^\\/\s\"'<>]+", "[REDACTED HOME]", value)
    value = re.sub(r"/private/var/folders/[^\s\"']+", "[REDACTED TEMP PATH]", value)
    value = re.sub(r"(?i)(https?://)[^/\s:@]+:[^/\s@]+@", r"\1[REDACTED USERINFO]@", value)
    value = re.sub(r"\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}\b", "[REDACTED EMAIL]", value)
    value = re.sub(r"\b[0-9A-Fa-f]{160,}\b", "[REDACTED RAW HEX]", value)
    # This also removes private-key body lines when an oversized log tail starts
    # after its BEGIN marker. SHA-256/SHA-1 identities remain useful diagnostics.
    value = re.sub(r"(?m)^[A-Za-z0-9+/]{48,}={0,2}$", lambda match:
                   match[0] if re.fullmatch(r"[0-9a-fA-F]{40}|[0-9a-fA-F]{64}|[0-9a-fA-F]{128}", match[0]) else "[REDACTED ENCODED CONTENT]", value)
    return value


def mask_json(value, depth=0):
    if depth > 64:
        return "[REDACTED EXCESSIVE DEPTH]"
    if isinstance(value, dict):
        return {mask_text(key): "[REDACTED]" if SECRET_KEY.fullmatch(key) else mask_json(item, depth + 1)
                for key, item in value.items()}
    if isinstance(value, list):
        return [mask_json(item, depth + 1) for item in value]
    return mask_text(value) if isinstance(value, str) else value


def plain_path(value):
    path = Path(value).absolute()
    if ".." in path.parts:
        raise ValueError("Traversal is not an evidence path")
    for parent in (path, *path.parents):
        if parent.is_symlink():
            raise ValueError("Symlink is not an evidence path")
    return path


def open_directory(path):
    path = plain_path(path)
    descriptor = os.open(path.anchor, os.O_RDONLY | os.O_DIRECTORY)
    try:
        for component in path.parts[1:]:
            child = os.open(component, os.O_RDONLY | os.O_DIRECTORY | os.O_NOFOLLOW, dir_fd=descriptor)
            os.close(descriptor)
            descriptor = child
        return descriptor
    except BaseException:
        os.close(descriptor)
        raise


def open_regular(path):
    """Open each path component without following links; never block on a FIFO."""
    path = plain_path(path)
    descriptor = open_directory(path.parent)
    try:
        result = os.open(path.name, os.O_RDONLY | os.O_NOFOLLOW | os.O_NONBLOCK, dir_fd=descriptor)
        if not stat.S_ISREG(os.fstat(result).st_mode):
            os.close(result)
            raise ValueError("Evidence input is not a regular file")
        return result
    finally:
        os.close(descriptor)


def read_source(path):
    descriptor = open_regular(path)
    with os.fdopen(descriptor, "rb") as source:
        before = os.fstat(source.fileno())
        if before.st_size > MAX_READ:
            return {"status": "omitted", "reason": "source_exceeds_read_budget", "source_bytes": before.st_size}
        if path.suffix == ".json" and before.st_size > MAX_FILE:
            return {"status": "omitted", "reason": "json_exceeds_file_budget", "source_bytes": before.st_size}
        digest = hashlib.sha256()
        tail = bytearray()
        count = 0
        binary = False
        decoder = codecs.getincrementaldecoder("utf-8")()
        retained_limit = MAX_FILE if path.suffix == ".json" else MAX_FILE - 1024
        while True:
            block = source.read(min(65536, MAX_READ + 1 - count))
            if not block:
                break
            count += len(block)
            if count > MAX_READ:
                return {"status": "omitted", "reason": "source_grew_past_read_budget", "source_bytes": count}
            binary |= b"\0" in block
            if not binary:
                try:
                    decoder.decode(block)
                except UnicodeDecodeError:
                    binary = True
            digest.update(block)
            tail.extend(block)
            if len(tail) > retained_limit:
                del tail[:-retained_limit]
        after = os.fstat(source.fileno())
        if (before.st_size, before.st_mtime_ns, before.st_ino) != (after.st_size, after.st_mtime_ns, after.st_ino):
            raise ValueError("Evidence source changed during collection")
        result = {"source_bytes": count, "source_sha256": digest.hexdigest(), "truncated": count > retained_limit}
        if binary:
            return dict(result, status="omitted", reason="binary_input")
        try:
            decoder.decode(b"", final=True)
        except UnicodeDecodeError:
            return dict(result, status="omitted", reason="non_utf8_input")
        if result["truncated"]:
            # Drop a partial first line, including a split UTF-8 codepoint.
            tail = tail.partition(b"\n")[2]
        try:
            text = tail.decode("utf-8")
        except UnicodeDecodeError:
            return dict(result, status="omitted", reason="non_utf8_input")
        if path.suffix == ".json":
            try:
                parsed = json.loads(text)
                text = json.dumps(mask_json(parsed), indent=2, ensure_ascii=False) + "\n"
            except (ValueError, RecursionError):
                return dict(result, status="omitted", reason="invalid_json")
        else:
            text = mask_text(text)
            if result["truncated"]:
                text = "[TRUNCATED: sanitized tail of original log]\n" + text
        content = text.encode("utf-8")
        if len(content) > MAX_FILE:
            return dict(result, status="omitted", reason="masked_copy_exceeds_file_budget")
        return dict(result, status="included", content=content)


def new_directory(value):
    path = plain_path(value)
    path.mkdir(parents=True, exist_ok=False)
    return path


def run_command(output, command):
    if not command:
        raise ValueError("A verification command is required after --")
    output = new_directory(output)
    metadata = {"schema_version": 1, "status": "running", "started_at": now(), "exit_code": None}
    status_path = output / "command.json"
    status_path.write_text(json.dumps(metadata, indent=2) + "\n")
    started = time.monotonic()
    tail = bytearray()
    total = 0
    code = 1
    try:
        child = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        with child.stdout:
            while True:
                block = child.stdout.read(65536)
                if not block:
                    break
                total += len(block)
                tail.extend(block)
                if len(tail) > MAX_FILE:
                    del tail[:-MAX_FILE]
        code = child.wait()
    except OSError:
        tail.extend(b"Verification launcher could not start; check the installed command/toolchain.\n")
        code = 127
    finally:
        if total > MAX_FILE:
            tail = tail.partition(b"\n")[2]
        (output / "command.log").write_bytes(tail)
        metadata.update(status="passed" if code == 0 else "failed", exit_code=code, finished_at=now(),
                        duration_seconds=round(time.monotonic() - started, 3),
                        captured_bytes=len(tail), original_output_bytes=total, truncated=total > MAX_FILE)
        status_path.write_text(json.dumps(metadata, indent=2) + "\n")
    # Raw output is intentionally kept out of the public job console. The always
    # collector provides a separately masked copy even after early failures.
    print("Verification command " + metadata["status"] + "; inspect the masked CI evidence artifact.")
    return code


def collect(platform, source, command_output, output):
    source, command_output, output = map(plain_path, (source, command_output, output))
    for original in (source, command_output):
        if original == output or original in output.parents or output in original.parents:
            raise ValueError("Archive must be separate from original evidence directories")
    output = new_directory(output)
    candidates = [("command/command.json", command_output / "command.json"),
                  ("command/command.log", command_output / "command.log")]
    if platform == "android":
        candidates += [("verification/" + name, source / name) for name in ANDROID_FILES]
    else:
        candidates.append(("verification/validation.json", source / "validation.json"))
        logs = source / "logs"
        try:
            descriptor = open_directory(logs)
        except FileNotFoundError:
            descriptor = None
        except (OSError, ValueError):
            descriptor = None
            candidates.append(("verification/logs/[unsafe-input]", None))
        if descriptor is not None:
            count = 0
            try:
                with os.scandir(descriptor) as items:
                    for entry in items:
                        count += 1
                        if count > MAX_SCAN:
                            break
                        if re.fullmatch(r"[0-9]{2,3}\.log", entry.name):
                            candidates.append(("verification/logs/" + entry.name, logs / entry.name))
            finally:
                os.close(descriptor)
            if count > MAX_SCAN:
                candidates.append(("verification/logs/[scan-limit]", None))
        if len(candidates) == 3:
            candidates.append(("verification/logs/[unavailable]", None))
    candidates = sorted(candidates)
    entries = []
    used = 0
    for name, path in candidates:
        if path is None:
            result = {"status": "missing" if name.endswith("[unavailable]") else "omitted",
                      "reason": "logs_unavailable" if name.endswith("[unavailable]") else "unsafe_logs_directory_or_scan_limit"}
        elif len(entries) >= MAX_FILES:
            result = {"status": "omitted", "reason": "file_count_budget"}
        else:
            try:
                result = read_source(path)
            except FileNotFoundError:
                result = {"status": "missing", "reason": "source_unavailable"}
            except (OSError, ValueError):
                result = {"status": "omitted", "reason": "unsafe_or_changed_input"}
        content = result.pop("content", None)
        if content is not None:
            if used + len(content) > MAX_TOTAL - 128 * 1024:
                result.update(status="omitted", reason="archive_total_budget")
            else:
                target = output / name
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes(content)
                result.update(archive_sha256=hashlib.sha256(content).hexdigest(), archive_bytes=len(content))
                used += len(content)
        entries.append(dict(path=name, **result))
    # State derives only from small, included copies. Missing/partial evidence
    # never becomes success simply because collection itself succeeded.
    def included_json(name):
        item = next((item for item in entries if item["path"] == name), {})
        if item.get("status") != "included":
            return {}
        value = json.loads((output / name).read_text())
        return value if isinstance(value, dict) else {}

    command = included_json("command/command.json")
    validation = included_json("verification/validation.json")
    verification = "unavailable"
    if command.get("status") == "failed" or type(command.get("exit_code")) is int and command["exit_code"] != 0:
        verification = "failed"
    elif command.get("status") == "passed" and type(command.get("exit_code")) is int and command["exit_code"] == 0:
        valid = (validation.get("emulator_runtime_verified") is True and validation.get("runtime_compatibility_verified") is True
                 if platform == "android" else validation.get("success") is True and validation.get("simulator_runtime") is True
                 and validation.get("runtime_compatibility_verified") is True)
        verification = "passed" if valid else "failed"
    manifest = {"schema_version": 1, "platform": platform, "created_at": now(),
                "collection_status": "partial" if any(item["status"] != "included" or item.get("truncated") for item in entries) else "complete",
                "verification_status": verification, "verification_exit_code": command.get("exit_code"),
                "hardware_verified": False, "original_evidence_modified": False,
                "masking_policy": "Known credential, user-path, device-identity and raw-content formats only; review before sharing arbitrary text.",
                "limits": {"max_file_bytes": MAX_FILE, "max_source_read_bytes": MAX_READ,
                           "max_total_bytes": MAX_TOTAL, "max_files": MAX_FILES, "max_directory_scan": MAX_SCAN},
                "included_bytes": used, "files": entries}
    manifest_bytes = (json.dumps(manifest, indent=2, sort_keys=True) + "\n").encode()
    if len(manifest_bytes) + used > MAX_TOTAL:
        raise ValueError("Manifest exceeds archive byte budget")
    (output / "evidence-manifest.json").write_bytes(manifest_bytes)
    print(json.dumps({"collection_status": manifest["collection_status"], "verification_status": verification,
                      "files": len(entries), "included_bytes": used}, sort_keys=True))
    return manifest


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    modes = parser.add_subparsers(dest="mode", required=True)
    run = modes.add_parser("run", help="Capture verifier output without echoing raw contents")
    run.add_argument("--output", required=True, type=Path)
    run.add_argument("command", nargs=argparse.REMAINDER)
    bundle = modes.add_parser("collect", help="Copy only allowlisted, bounded and masked diagnostics")
    bundle.add_argument("--platform", required=True, choices=("android", "ios"))
    bundle.add_argument("--source", required=True, type=Path)
    bundle.add_argument("--command-output", required=True, type=Path)
    bundle.add_argument("--output", required=True, type=Path)
    args = parser.parse_args(argv)
    try:
        if args.mode == "run":
            command = args.command[1:] if args.command[:1] == ["--"] else args.command
            return run_command(args.output, command)
        collect(args.platform, args.source, args.command_output, args.output)
        return 0
    except (OSError, ValueError) as error:
        print("Evidence operation could not complete: " + type(error).__name__, file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
