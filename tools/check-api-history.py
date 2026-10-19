#!/usr/bin/env python3
"""Verify source-free published API history without Git, network or SDK sources.

Public Git commit/tree/blob proofs anchor mobile compiler snapshots. The C
snapshot only checks reviewed declarations and metadata, not compiled binaries
or private repository history. This checker never accepts or updates anchors.
"""
from __future__ import annotations

import argparse
import base64
import hashlib
import json
from pathlib import Path, PurePosixPath
import re
import sys

# Reviewed releases with unchanged declarations still use the original proof.
# This mapping is explicit; neither unknown versions nor contract drift inherit it.
CONTRACT_BASELINES = {"0.2.0-preview.4": "0.2.0-preview.3"}
TRUSTED_MOBILE = {
    "0.2.0-preview.3": {
        "android": {"repository": "netumscan/printer-sdk-android",
                    "revision": "7f0d25c73280b3cbe7d6ba3fd72a0ee353a58dd2", "path": "docs/api-reference.json"},
        "apple": {"repository": "netumscan/printer-sdk-ios",
                  "revision": "4fac5d03efb71ffaef770d368f879d508ddc800d", "path": "docs/api-reference.json"},
    },
}
C_HEADER_SHA256 = {
    "printer_sdk_candidate.h": "f9b50a5b97e38984600f5d64aa723af83a3e76d57532605b0747bc1609ae6e48",
    "platform_transport_candidate.h": "74d5adda62a1031ad30bb329c7cd881825f17866de00094426906b0c3930193b",
}
# Filled from the fixed, previously committed declaration manifest, not a build.
C_MANIFEST_SHA256 = "82c39055dff03ffce55230095bd6b2feba0f8e159a6c4c74878c26430b371e79"
PUBLISHED_RELEASES = [
    {"platform": "android", "sdk_version": "0.2.0-preview.2", "tag": "v0.2.0-preview.2",
     "repository": "netumscan/printer-sdk-android", "revision": "4b2510f6476dac5e6795a3e1c71c776670cd40b9",
     "artifact_sha256": "3e8c8ceb60df1c94187a95cf776d9776284a41db495b9a96dccdb17a00a85d0b",
     "api_baseline_sha256": "dbbab332f2d1280ef680de940b1ccad9848b658a2e9659f74ae45b2b9515b823",
     "api_evidence": "independently_extracted_verified_published_aar"},
    {"platform": "android", "sdk_version": "0.2.0-preview.3", "tag": "v0.2.0-preview.3",
     "repository": "netumscan/printer-sdk-android", "revision": "e256c72f1180f2481a47dcdb2feccff470470449",
     "artifact_sha256": "20c7a4abc4ef6434a4f66c2ad9651489072a8a5e20133e4c42b2610fbdcf6295",
     "api_baseline_sha256": "8ad69087e4a965f7df4aa56f84b33dc6cfcf340ed3e8395f57155dfe449390a2",
     "api_evidence": "published_compiler_snapshot_git_proof"},
    {"platform": "apple", "sdk_version": "0.2.0-preview.1", "tag": "v0.2.0-preview.1",
     "repository": "netumscan/printer-sdk-ios", "revision": "3df348c440d4aebcc6a254362dbc1023e53f9718",
     "artifact_sha256": "ee448c3ae09d277598a86853f0b80ae02e719bbe4d45fd9052c6ee2035653c28",
     "api_baseline_sha256": "62ccd5c542c02285429d8942b7eb69a68555180ddb3b9d335780e30009f2fd65",
     "api_evidence": "verified_published_swift_sources_equal_to_preview3"},
    {"platform": "apple", "sdk_version": "0.2.0-preview.2", "tag": "v0.2.0-preview.2",
     "repository": "netumscan/printer-sdk-ios", "revision": "b629ccc1aeddf0370a3845cfb3f47152df0e9a5d",
     "artifact_sha256": "c0e7afc018ef533ab5868867446897d77b6f6b0f6531c8639b1d3699287fdeab",
     "api_baseline_sha256": "62ccd5c542c02285429d8942b7eb69a68555180ddb3b9d335780e30009f2fd65",
     "api_evidence": "verified_published_swift_sources_equal_to_preview3"},
    {"platform": "apple", "sdk_version": "0.2.0-preview.3", "tag": "v0.2.0-preview.3",
     "repository": "netumscan/printer-sdk-ios", "revision": "d08676ca6ca11fccf82cc0dce56ba69d9d917557",
     "artifact_sha256": "c0e7afc018ef533ab5868867446897d77b6f6b0f6531c8639b1d3699287fdeab",
     "api_baseline_sha256": "62ccd5c542c02285429d8942b7eb69a68555180ddb3b9d335780e30009f2fd65",
     "api_evidence": "published_compiler_snapshot_git_proof"},
]
HISTORICAL_ANDROID = {"baseline_sha256": "dbbab332f2d1280ef680de940b1ccad9848b658a2e9659f74ae45b2b9515b823",
                      "artifact_sha256": "3e8c8ceb60df1c94187a95cf776d9776284a41db495b9a96dccdb17a00a85d0b",
                      "classes_sha256": "a38917a334569a217a3a3564038b50e18dfeb2af24e390522f3c0dc25a159ac9",
                      "types": 14, "members": 101}
MAX_OBJECT_BYTES = 2 * 1024 * 1024
CANDIDATE_CONTRACTS = {"0.3.0-preview.1": {
    "native_sdk_version": "0.3.0", "abi": {"version": 3, "revision": 1},
    "historical_baseline_version": "0.2.0-preview.3",
    "manifest_sha256": "a0dcb7f9f95af5ad36a0f1dc18ff656d3bdb4d8ba07ae4023d95dbd18e64c0a1",
    "c_baseline_sha256": "b7e129d676796c178787b35c7d22bb00ee2be316ee6e390bd3f7a01e8f5c2cfd",
}, "0.3.0-preview.2": {
    "native_sdk_version": "0.3.0", "abi": {"version": 3, "revision": 1},
    "historical_baseline_version": "0.2.0-preview.3",
    "manifest_sha256": "57e9edb1a65fe73c4e74dfd9dff2a1902b4e7f240b337be10fa744356a7c4694",
    "c_baseline_sha256": "74a75132f39a83974d5f961a7f8ef2d209d0473e0a7850abf485f4270f2b7bc2",
}}


class NotVerified(RuntimeError):
    """A required historical proof is unavailable, not a successful skip."""


def require(condition, message):
    if not condition:
        raise ValueError(message)


def sha256(data):
    return hashlib.sha256(data).hexdigest()


def unique_object(pairs):
    value = {}
    for key, item in pairs:
        require(key not in value, "Duplicate JSON key: " + key)
        value[key] = item
    return value


def loads(data):
    return json.loads(data, object_pairs_hook=unique_object)


def exact_keys(value, expected):
    require(isinstance(value, dict) and set(value) == set(expected), "Unexpected API history fields")


def canonical(value):
    return json.dumps(value, sort_keys=True, ensure_ascii=False, separators=(",", ":")).encode()


def ordinary_file(root, name):
    relative = PurePosixPath(name)
    require(isinstance(name, str) and not relative.is_absolute() and str(relative) == name
            and all(part not in {".", ".."} for part in relative.parts), "Invalid API history path")
    target = Path(root)
    for part in relative.parts:
        target = target / part
        require(not target.is_symlink(), "Symlinked API history input: " + name)
    if not target.is_file():
        raise NotVerified("Missing API history input: " + name)
    return target


def git_object_id(kind, data):
    return hashlib.sha1(kind.encode() + b" " + str(len(data)).encode() + b"\0" + data).hexdigest()


def decode_objects(values):
    require(isinstance(values, dict) and len(values) <= 32, "Invalid Git proof object inventory")
    if not values:
        raise NotVerified("Missing Git proof objects")
    result = {}
    for oid, value in values.items():
        require(isinstance(oid, str) and re.fullmatch(r"[0-9a-f]{40}", oid), "Invalid Git proof object ID")
        exact_keys(value, {"type", "base64"})
        require(value["type"] in {"commit", "tree", "blob"} and isinstance(value["base64"], str),
                "Invalid Git proof object type or data")
        require(len(value["base64"]) <= (MAX_OBJECT_BYTES + 2) // 3 * 4, "Git proof object exceeds limit")
        data = base64.b64decode(value["base64"], validate=True)
        require(base64.b64encode(data).decode() == value["base64"], "Noncanonical Git proof encoding")
        require(git_object_id(value["type"], data) == oid, "Git proof object hash differs: " + oid)
        result[oid] = (value["type"], data)
    return result


def tree_entries(data):
    result = {}
    offset = 0
    while offset < len(data):
        space = data.find(b" ", offset)
        zero = data.find(b"\0", space + 1)
        require(space > offset and zero > space + 1 and zero + 21 <= len(data), "Malformed Git tree object")
        mode, name = data[offset:space].decode("ascii"), data[space + 1:zero].decode("utf-8")
        require(mode in {"40000", "100644", "100755", "120000", "160000"}
                and name not in {".", ".."} and "/" not in name and name not in result,
                "Invalid or duplicate Git tree entry")
        result[name] = (mode, data[zero + 1:zero + 21].hex())
        offset = zero + 21
    return result


def proof_blob(objects, pin, used):
    revision = pin["revision"]
    if revision not in objects:
        raise NotVerified("Missing fixed public Git commit object")
    require(objects[revision][0] == "commit", "Fixed public Git object is not a commit")
    used.add(revision)
    commit = objects[revision][1]
    first, separator, _ = commit.partition(b"\n")
    require(separator and re.fullmatch(rb"tree [0-9a-f]{40}", first), "Invalid Git commit root tree")
    oid = first[5:].decode()
    parts = PurePosixPath(pin["path"]).parts
    for index, name in enumerate(parts):
        if oid not in objects:
            raise NotVerified("Missing Git proof tree object")
        require(objects[oid][0] == "tree", "Git proof path object is not a tree")
        used.add(oid)
        entries = tree_entries(objects[oid][1])
        require(name in entries, "Published API path is absent from Git tree")
        mode, oid = entries[name]
        expected_mode = "100644" if index == len(parts) - 1 else "40000"
        require(mode == expected_mode, "Published API reference must be an ordinary blob path")
    if oid not in objects:
        raise NotVerified("Missing published API reference blob")
    require(objects[oid][0] == "blob", "Published API reference object is not a blob")
    used.add(oid)
    return objects[oid][1], oid


def baseline_records(platform, text):
    require(isinstance(text, str) and text.endswith("\n"), "Invalid historical compiler API text")
    records = {"type": [], "symbol": [], "relationship": [], "enum": []}
    for line in text.splitlines():
        if line.startswith("#"):
            continue
        kind, separator, raw = line.partition(" ")
        allowed = {"type"} if platform == "android" else {"symbol", "relationship", "enum"}
        require(separator and kind in allowed, "Invalid historical API record")
        value = loads(raw)
        require(isinstance(value, dict), "Invalid historical API record value")
        records[kind].append(value)
    require(records["type"] if platform == "android" else records["symbol"], "Empty historical API snapshot")
    return records


def verify_proofs(proofs):
    exact_keys(proofs, {"schema", "versions", "objects"})
    require(type(proofs["schema"]) is int and proofs["schema"] == 1, "Invalid Git proof schema")
    require(isinstance(proofs["versions"], list), "Invalid API history versions")
    objects = decode_objects(proofs["objects"])
    used, snapshots, seen = set(), {}, set()
    for version in proofs["versions"]:
        exact_keys(version, {"sdk_version", "proofs"})
        name = version["sdk_version"]
        require(isinstance(name, str) and name in TRUSTED_MOBILE and name not in seen,
                "Unreviewed or duplicate API history version")
        seen.add(name)
        require(version["proofs"] == TRUSTED_MOBILE[name], "Public API history pins differ from reviewed commits")
        snapshots[name] = {}
        for platform, pin in TRUSTED_MOBILE[name].items():
            raw, blob = proof_blob(objects, pin, used)
            bundle = loads(raw)
            exact_keys(bundle, {"schema", "sdk_version", "native_sdk_version", "abi", "platforms"})
            require(type(bundle["schema"]) is int and bundle["schema"] == 1
                    and bundle["sdk_version"] == name and bundle["native_sdk_version"] == "0.2.0"
                    and bundle["abi"] == {"version": 2, "revision": 10}, "Historical API reference identity differs")
            exact_keys(bundle["platforms"], {"android", "apple"})
            record = bundle["platforms"][platform]
            exact_keys(record, {"baseline", "baseline_sha256", "annotations"})
            baseline_records(platform, record["baseline"])
            digest = sha256(record["baseline"].encode())
            require(digest == record["baseline_sha256"], "Published API baseline checksum differs")
            snapshots[name][platform] = {"baseline": record["baseline"], "baseline_sha256": digest,
                                         "blob": blob, "reference_sha256": sha256(raw), **pin}
    if seen != set(TRUSTED_MOBILE):
        raise NotVerified("Missing reviewed API history version")
    require(used == set(objects), "Unused Git proof objects are forbidden")
    return snapshots


def verify_c_snapshot(snapshot):
    exact_keys(snapshot, {"manifest", "declarations"})
    manifest = snapshot["manifest"]
    exact_keys(manifest, {"baseline", "header_token_sha256", "minimum_prefix_bytes", "exports"})
    require(sha256(canonical(manifest)) == C_MANIFEST_SHA256, "Reviewed C declaration manifest differs")
    require(manifest["baseline"] == "ABI-2-r10" and manifest["header_token_sha256"] == C_HEADER_SHA256,
            "C ABI snapshot identity differs")
    exact_keys(snapshot["declarations"], set(C_HEADER_SHA256))
    exports, structs = [], []
    for name, expected in C_HEADER_SHA256.items():
        declaration = snapshot["declarations"][name]
        require(isinstance(declaration, str) and sha256(declaration.encode()) == expected,
                "C declaration token checksum differs: " + name)
        tokens = declaration.split("\n")
        exports.extend(tokens[index + 3] for index in range(len(tokens) - 4)
                       if tokens[index] == "PSDK_API" and tokens[index + 2] == "PSDK_CALL"
                       and tokens[index + 3].startswith("psdk_") and tokens[index + 4] == "(")
        structs.extend(tokens[index + 2] for index in range(len(tokens) - 3)
                       if tokens[index:index + 2] == ["typedef", "struct"] and tokens[index + 3] == "{")
    require(len(exports) == 49 and len(set(exports)) == 49 and sorted(exports) == manifest["exports"],
            "C declaration snapshot export inventory differs")
    require(len(structs) == 17 and len(set(structs)) == 17
            and sorted(structs) == sorted(manifest["minimum_prefix_bytes"]), "C structure prefix inventory differs")
    return {"c_declaration_snapshot_verified": True, "c_exports": len(exports), "c_structure_prefixes": len(structs),
            "c_binary_verified": False, "private_git_history_verified": False}


def verify_release_history(releases, android):
    require(releases == PUBLISHED_RELEASES, "Published release identities differ from reviewed history")
    exact_keys(android, {"baseline", *HISTORICAL_ANDROID})
    require({key: value for key, value in android.items() if key != "baseline"} == HISTORICAL_ANDROID,
            "Historical Android extraction provenance differs")
    require(isinstance(android["baseline"], str)
            and sha256(android["baseline"].encode()) == HISTORICAL_ANDROID["baseline_sha256"],
            "Historical Android API snapshot differs from verified published AAR")
    records = baseline_records("android", android["baseline"])["type"]
    require(len(records) == android["types"] and sum(len(item["members"]) for item in records) == android["members"],
            "Historical Android API inventory differs")
    return {"published_release_identities_verified": True, "published_release_count": len(releases),
            "historical_android_api_snapshot_verified": True,
            "historical_artifacts_reinspected_this_check": False, "remote_release_refs_verified_this_check": False}


def contract_baseline(version, snapshots):
    require(isinstance(version, str), "Current SDK version has no reviewed historical anchor")
    baseline = CONTRACT_BASELINES.get(version, version)
    require(baseline in snapshots, "Current SDK version has no reviewed historical anchor")
    return baseline


def verify_candidate_bundle(history, reference):
    """Check current frozen declarations separately from the original releases.

    No candidate is inserted into public Git proofs or published_releases. This
    is declaration review, not an artifact, target execution or upload proof.
    """
    exact_keys(history, {"schema", "sdk_version", "native_sdk_version", "abi", "hardware_verified",
                         "mobile_proofs", "c_abi", "published_releases", "historical_android",
                         "contract_status", "published_contract_verified", "historical_baseline_version",
                         "candidate_contract"})
    version = history["sdk_version"]
    policy = CANDIDATE_CONTRACTS[version]
    require(type(history["schema"]) is int and history["schema"] == 2
            and history["contract_status"] == "candidate" and history["published_contract_verified"] is False
            and history["hardware_verified"] is False
            and history["native_sdk_version"] == policy["native_sdk_version"] and history["abi"] == policy["abi"],
            "Invalid candidate API history identity")
    snapshots = verify_proofs(history["mobile_proofs"])
    baseline = contract_baseline(policy["historical_baseline_version"], snapshots)
    require(history["historical_baseline_version"] == baseline, "Candidate historical baseline differs")
    old_c = verify_c_snapshot(history["c_abi"])
    release_result = verify_release_history(history["published_releases"], history["historical_android"])
    payload = history["candidate_contract"]
    exact_keys(payload, {"manifest", "c_abi"})
    manifest = payload["manifest"]
    require(sha256(canonical(manifest)) == policy["manifest_sha256"], "Candidate contract manifest differs")
    require(manifest["sdk_version"] == version and manifest["contract_status"] == "candidate"
            and manifest["native_sdk_version"] == history["native_sdk_version"] and manifest["abi"] == history["abi"],
            "Candidate frozen identity differs")
    c_snapshot = payload["c_abi"]
    exact_keys(c_snapshot, {"manifest", "declarations"})
    c_manifest = c_snapshot["manifest"]
    require(sha256(canonical(c_manifest)) == policy["c_baseline_sha256"], "Candidate C baseline differs")
    exact_keys(c_snapshot["declarations"], {"printer_sdk.h"})
    declaration = c_snapshot["declarations"]["printer_sdk.h"]
    require(isinstance(declaration, str) and sha256(declaration.encode()) == c_manifest["header_token_sha256"],
            "Candidate C declaration token checksum differs")
    tokens = declaration.split("\n")
    exports = [tokens[i + 3] for i in range(len(tokens) - 4)
               if tokens[i] == "PSDK_API" and tokens[i + 2] == "PSDK_CALL"
               and tokens[i + 3].startswith("psdk_") and tokens[i + 4] == "("]
    structs = [tokens[i + 2] for i in range(len(tokens) - 3)
               if tokens[i:i + 2] == ["typedef", "struct"] and tokens[i + 3] == "{"]
    inits = [tokens[i + 2] + "_init" for i in range(len(tokens) - 3)
             if tokens[i:i + 2] == ["PSDK_INLINE_INIT", "("] and tokens[i + 2].startswith("psdk_")
             and tokens[i + 3] == ")"]
    require(len(exports) == len(set(exports)) == 25 and sorted(exports) == c_manifest["exports"]
            and len(structs) == len(set(structs)) == 25
            and len(inits) == len(set(inits)) == 25 and sorted(inits) == c_manifest["inline_initializers"],
            "Candidate C inventory differs")
    require(isinstance(reference, dict) and reference.get("sdk_version") == version
            and reference.get("contract_status") == "candidate"
            and reference.get("native_sdk_version") == history["native_sdk_version"] and reference.get("abi") == history["abi"],
            "Candidate API reference identity differs")
    exact_keys(reference.get("platforms"), {"android", "apple"})
    for platform, entry in manifest["mobile"].items():
        record = reference["platforms"][platform]
        require(isinstance(record, dict) and isinstance(record.get("baseline"), str)
                and sha256(record["baseline"].encode()) == record.get("baseline_sha256") == entry["baseline_sha256"],
                "Candidate mobile API baseline differs: " + platform)
        annotations = (json.dumps(record.get("annotations"), sort_keys=True, ensure_ascii=False, indent=2) + "\n").encode()
        require(sha256(annotations) == entry["annotations_sha256"], "Candidate API annotations differ: " + platform)
    return {"public_mobile_api_history_verified": True, "sdk_version": version, "contract_status": "candidate",
            "published_contract_verified": False, "candidate_contract_verified": True,
            "contract_baseline_version": version, "historical_baseline_version": baseline,
            "hardware_verified": False, "historical_versions": sorted(snapshots),
            "c_declaration_snapshot_verified": True, "c_exports": len(exports), "c_structures": len(structs),
            "c_binary_verified": False, "private_git_history_verified": False,
            "historical_c_exports": old_c["c_exports"], **release_result}


def verify_bundle(history, reference):
    if isinstance(history, dict) and isinstance(history.get("sdk_version"), str) and history["sdk_version"] in CANDIDATE_CONTRACTS:
        return verify_candidate_bundle(history, reference)
    fields = {"schema", "sdk_version", "native_sdk_version", "abi", "hardware_verified",
              "mobile_proofs", "c_abi", "published_releases", "historical_android"}
    version = history.get("sdk_version") if isinstance(history, dict) else None
    if isinstance(version, str) and version in CONTRACT_BASELINES:
        fields.add("contract_baseline_version")
    exact_keys(history, fields)
    require(type(history["schema"]) is int and history["schema"] == 1
            and history["hardware_verified"] is False and history["native_sdk_version"] == "0.2.0"
            and history["abi"] == {"version": 2, "revision": 10}, "Invalid public API history identity")
    snapshots = verify_proofs(history["mobile_proofs"])
    baseline = contract_baseline(version, snapshots)
    if version in CONTRACT_BASELINES:
        require(history["contract_baseline_version"] == baseline, "Reviewed contract baseline identity differs")
    require(isinstance(reference, dict) and reference.get("sdk_version") == version
            and reference.get("native_sdk_version") == history["native_sdk_version"]
            and reference.get("abi") == history["abi"], "Current API reference identity differs from history")
    exact_keys(reference.get("platforms"), {"android", "apple"})
    for platform, snapshot in snapshots[baseline].items():
        record = reference["platforms"][platform]
        require(isinstance(record, dict) and record.get("baseline") == snapshot["baseline"]
                and record.get("baseline_sha256") == snapshot["baseline_sha256"],
                "Current API baseline differs from published history: " + platform)
    c_result = verify_c_snapshot(history["c_abi"])
    release_result = verify_release_history(history["published_releases"], history["historical_android"])
    return {"public_mobile_api_history_verified": True, "sdk_version": version,
            "contract_baseline_version": baseline, "hardware_verified": False,
            "historical_versions": sorted(snapshots), "mobile_anchors": {
                name: {platform: {key: value for key, value in record.items() if key != "baseline"}
                       for platform, record in entries.items()} for name, entries in snapshots.items()},
            **c_result, **release_result}


def check(root):
    history = loads(ordinary_file(root, "docs/api-history.json").read_bytes())
    reference = loads(ordinary_file(root, "docs/api-reference.json").read_bytes())
    return verify_bundle(history, reference)


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1])
    args = parser.parse_args(argv)
    try:
        print(json.dumps(check(args.root), ensure_ascii=False, sort_keys=True))
        return 0
    except NotVerified as error:
        print(json.dumps({"status": "not_verified", "reason": str(error), "hardware_verified": False}, sort_keys=True))
        return 2
    except (ValueError, OSError, KeyError, TypeError, UnicodeError) as error:
        print(json.dumps({"status": "failed", "reason": str(error), "hardware_verified": False}, sort_keys=True))
        return 1


if __name__ == "__main__":
    sys.exit(main())
