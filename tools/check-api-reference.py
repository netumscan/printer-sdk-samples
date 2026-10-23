#!/usr/bin/env python3
"""Validate and render the source-free, reviewed Java and Swift API reference.

Uses only Python's standard library. This verifies the published documentation
against compiler baselines, not a replacement for compiling the SDK API gate.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import re
import sys

APPROVED_BASELINES = {
    "android": "8ad69087e4a965f7df4aa56f84b33dc6cfcf340ed3e8395f57155dfe449390a2",
    "apple": "62ccd5c542c02285429d8942b7eb69a68555180ddb3b9d335780e30009f2fd65",
}
DEVELOPMENT_BASELINES = {"0.3.0-preview.16-dev": {
    "android": "49d9629dfc0027c4800486a48808472b39ff9c75645bea7a35f4c0f4a3a49d45",
    "apple": "197498b66174e391abd4b8d5704ea4e09762b775f3fb6ff55d16b92653125307",
}, "0.3.0-preview.15-dev": {
    "android": "49d9629dfc0027c4800486a48808472b39ff9c75645bea7a35f4c0f4a3a49d45",
    "apple": "197498b66174e391abd4b8d5704ea4e09762b775f3fb6ff55d16b92653125307",
}, "0.3.0-preview.13-dev": {"android": "e336f28038f2e4c0e4f867a39e78397695dc97a6570699997969eece32747059", "apple": "f19617f1c780b9a76fe9587efc60211c08f4ccae52660b1b10349d04913d53eb"}, "0.3.0-preview.12-dev": {
    "android": "cbcd1ea6c2716542e31228f51c9c85be2a40c043fa576a9d190c5ef436372b0e",
    "apple": "653acceda0c2df9f04f4b6c2018a5aee8cfc364e879e04cee136d8ce41414a86",
}, "0.3.0-preview.10-dev": {
    "android": "b42d80dff97b0dd790219ae23107f9946bca030b301aef865ef93694c093e980",
    "apple": "8aaed8263549d0d9ea0abc7c14918fdf38492531e97e975143c2d1c583533377",
}, "0.3.0-preview.9-dev": {
    "android": "4dcc42d4069e03b42c9922027e4d44811d07cf9362d1dc7a62c17ecc09350ded",
    "apple": "94621ef91e247a4176bab1f21cad43053d71aa18accf9503ff7e35e7685a924b",
}, "0.3.0-preview.8-dev": {
    "android": "4dcc42d4069e03b42c9922027e4d44811d07cf9362d1dc7a62c17ecc09350ded",
    "apple": "8e3d0c3c52682ea9285c363cd9fa7dc574f4321fe17ef8460b523dc7f1778d97",
}, "0.3.0-preview.7-dev": {
    "android": "9ea320e5c233e8cf52a9bdeccc6ff26aa192ad322fbb370c2e70575d0cdc067d",
    "apple": "8e3d0c3c52682ea9285c363cd9fa7dc574f4321fe17ef8460b523dc7f1778d97",
}, "0.3.0-preview.6-dev": {
    "android": "1a80d9008c0eb7453d4c2452d2d1491b5ae00ef641c77db42f4e26b467b91856",
    "apple": "45385d4c7226165d39e476bd1ecd9b9dc3174608e32c14a8949d7273fc841b65",
}, "0.3.0-preview.5-dev": {
    "android": "cdee503bff25d6c1ee0b9d3f0fa488293570cf83124a382ca53e7bbc14ed686c",
    "apple": "affaba2490e4db54029a34867b9774917b0cb07e928276742609533e9ccd9169",
}, "0.3.0-preview.4-dev": {
    "android": "cc7d3634f25deeca19aeef6a8775eab17f40ede6837967113b386bcc33b9288e",
    "apple": "e2f8e9b815eda327ea9e573fc014a5964d73b4d834394091ced9968c1765b19b",
}, "0.3.0-preview.3-dev": {
    "android": "07ff86b2327a0fc2ba155928f4847d8047eb1b61826bcdb6b7e223e563442fe6",
    "apple": "7e2a51b45f07800a08a180161ad6bfff71b19bc2fd46dc70d0d124c74a44416c",
}, "0.3.0-preview.2-dev": {
    "android": "07ff86b2327a0fc2ba155928f4847d8047eb1b61826bcdb6b7e223e563442fe6",
    "apple": "7e2a51b45f07800a08a180161ad6bfff71b19bc2fd46dc70d0d124c74a44416c",
}, "0.3.0-preview.1-dev": {
    "android": "07ff86b2327a0fc2ba155928f4847d8047eb1b61826bcdb6b7e223e563442fe6",
    "apple": "f695701df639bd0fb39cb1481ab5cce2f2f4631729ea1fd8697274b7b46687ea",
}, "0.2.0-preview.5-dev": {
    "android": "de4bcf3b76c140e65320136962a6eba609e450d7e604e11b71b31d5eeb0a8525",
    "apple": "24046a0889ac83a9d96bac1220867005edbd2ce50684f972f30c691d8429fe42",
}}
DEVELOPMENT_IDENTITIES = {
    "0.3.0-preview.16-dev": {"native_sdk_version": "0.3.0", "abi": {"version": 3, "revision": 3}},
    "0.3.0-preview.15-dev": {"native_sdk_version": "0.3.0", "abi": {"version": 3, "revision": 3}},
    "0.3.0-preview.13-dev": {"native_sdk_version": "0.3.0", "abi": {"version": 3, "revision": 3}},
    "0.3.0-preview.12-dev": {"native_sdk_version": "0.3.0", "abi": {"version": 3, "revision": 3}},
    "0.3.0-preview.10-dev": {"native_sdk_version": "0.3.0", "abi": {"version": 3, "revision": 2}},
    "0.3.0-preview.9-dev": {"native_sdk_version": "0.3.0", "abi": {"version": 3, "revision": 2}},
    "0.3.0-preview.8-dev": {"native_sdk_version": "0.3.0", "abi": {"version": 3, "revision": 2}},
    "0.3.0-preview.7-dev": {"native_sdk_version": "0.3.0", "abi": {"version": 3, "revision": 2}},
    "0.3.0-preview.6-dev": {"native_sdk_version": "0.3.0", "abi": {"version": 3, "revision": 2}},
    "0.3.0-preview.5-dev": {"native_sdk_version": "0.3.0", "abi": {"version": 3, "revision": 2}},
    "0.3.0-preview.4-dev": {"native_sdk_version": "0.3.0", "abi": {"version": 3, "revision": 2}},
    "0.3.0-preview.3-dev": {"native_sdk_version": "0.3.0", "abi": {"version": 3, "revision": 2}},
    "0.3.0-preview.2-dev": {"native_sdk_version": "0.3.0", "abi": {"version": 3, "revision": 1}},
    "0.2.0-preview.5-dev": {"native_sdk_version": "0.2.0", "abi": {"version": 2, "revision": 13}},
    "0.3.0-preview.1-dev": {"native_sdk_version": "0.3.0", "abi": {"version": 3, "revision": 1}},
}
CANDIDATE_BASELINES = {"0.3.0-preview.16": {
    "android": "49d9629dfc0027c4800486a48808472b39ff9c75645bea7a35f4c0f4a3a49d45",
    "apple": "197498b66174e391abd4b8d5704ea4e09762b775f3fb6ff55d16b92653125307",
}, "0.3.0-preview.15": {
    "android": "49d9629dfc0027c4800486a48808472b39ff9c75645bea7a35f4c0f4a3a49d45",
    "apple": "197498b66174e391abd4b8d5704ea4e09762b775f3fb6ff55d16b92653125307",
}, "0.3.0-preview.14": {
    "android": "49d9629dfc0027c4800486a48808472b39ff9c75645bea7a35f4c0f4a3a49d45",
    "apple": "647f51820f25fe6c57f1be73d047c69d13e9611b39cccbf56f8a3c5427aa7d3f",
}, "0.3.0-preview.11": {
    "android": "b42d80dff97b0dd790219ae23107f9946bca030b301aef865ef93694c093e980",
    "apple": "8aaed8263549d0d9ea0abc7c14918fdf38492531e97e975143c2d1c583533377",
}, "0.3.0-preview.10": {
    "android": "b42d80dff97b0dd790219ae23107f9946bca030b301aef865ef93694c093e980",
    "apple": "8aaed8263549d0d9ea0abc7c14918fdf38492531e97e975143c2d1c583533377",
}, "0.3.0-preview.1": {
    "android": "07ff86b2327a0fc2ba155928f4847d8047eb1b61826bcdb6b7e223e563442fe6",
    "apple": "f695701df639bd0fb39cb1481ab5cce2f2f4631729ea1fd8697274b7b46687ea",
}, "0.3.0-preview.2": {
    "android": "07ff86b2327a0fc2ba155928f4847d8047eb1b61826bcdb6b7e223e563442fe6",
    "apple": "7e2a51b45f07800a08a180161ad6bfff71b19bc2fd46dc70d0d124c74a44416c",
}, "0.3.0-preview.4": {
    "android": "cc7d3634f25deeca19aeef6a8775eab17f40ede6837967113b386bcc33b9288e",
    "apple": "e2f8e9b815eda327ea9e573fc014a5964d73b4d834394091ced9968c1765b19b",
}, "0.3.0-preview.5": {
    "android": "cdee503bff25d6c1ee0b9d3f0fa488293570cf83124a382ca53e7bbc14ed686c",
    "apple": "affaba2490e4db54029a34867b9774917b0cb07e928276742609533e9ccd9169",
}, "0.3.0-preview.8": {
    "android": "4dcc42d4069e03b42c9922027e4d44811d07cf9362d1dc7a62c17ecc09350ded",
    "apple": "8e3d0c3c52682ea9285c363cd9fa7dc574f4321fe17ef8460b523dc7f1778d97",
}}
CANDIDATE_IDENTITIES = {"0.3.0-preview.16": {
    "native_sdk_version": "0.3.0", "abi": {"version": 3, "revision": 3},
}, "0.3.0-preview.15": {
    "native_sdk_version": "0.3.0", "abi": {"version": 3, "revision": 3},
}, "0.3.0-preview.14": {
    "native_sdk_version": "0.3.0", "abi": {"version": 3, "revision": 3},
}, "0.3.0-preview.11": {
    "native_sdk_version": "0.3.0", "abi": {"version": 3, "revision": 2},
}, "0.3.0-preview.10": {
    "native_sdk_version": "0.3.0", "abi": {"version": 3, "revision": 2},
}, "0.3.0-preview.1": {
    "native_sdk_version": "0.3.0", "abi": {"version": 3, "revision": 1},
}, "0.3.0-preview.2": {
    "native_sdk_version": "0.3.0", "abi": {"version": 3, "revision": 1},
}, "0.3.0-preview.4": {
    "native_sdk_version": "0.3.0", "abi": {"version": 3, "revision": 2},
}, "0.3.0-preview.5": {
    "native_sdk_version": "0.3.0", "abi": {"version": 3, "revision": 2},
}, "0.3.0-preview.8": {
    "native_sdk_version": "0.3.0", "abi": {"version": 3, "revision": 2},
}}
CANDIDATE_ANNOTATIONS = {"0.3.0-preview.16": {
    "android": "8269c96693e867167d04294acf1740d8d98d8bd176d4a09f2ade8ba27d10547f",
    "apple": "cf3e03d7ed938305a2d70ee605f5973fe0809dc41942d13304e89432f9419b36",
}, "0.3.0-preview.15": {
    "android": "8269c96693e867167d04294acf1740d8d98d8bd176d4a09f2ade8ba27d10547f",
    "apple": "cf3e03d7ed938305a2d70ee605f5973fe0809dc41942d13304e89432f9419b36",
}, "0.3.0-preview.14": {
    "android": "8269c96693e867167d04294acf1740d8d98d8bd176d4a09f2ade8ba27d10547f",
    "apple": "f2bb9a4ca4cf195040e3860c38c33dc78f36db5195dab6c201d485a0b337f305",
}, "0.3.0-preview.11": {
    "android": "676a3763c5ce5349e8e55fbd18341b2a96017430cc6f1c2fc40eb44dbb39f023",
    "apple": "3b128f944f44123b7e31ca8e4ca0c7e25596ceb08328bd7bf7d715e4793569dc",
}, "0.3.0-preview.10": {
    "android": "676a3763c5ce5349e8e55fbd18341b2a96017430cc6f1c2fc40eb44dbb39f023",
    "apple": "3721dc3381043f5bb8df4e7d735b3d37f4171ac8ef333fa9a69385e0ebfdb5fd",
}, "0.3.0-preview.1": {
    "android": "6dad2100c2fc107c5dc00151547eaa62a8e78fb766ba5ec05d6264c8d6ee7082",
    "apple": "491fcf6ab3288fd1ecf4f2c002608c7c7d16ebf5baf8c1903212b2766ee7e41c",
}, "0.3.0-preview.2": {
    "android": "52b10f1869e4be989fa4c6c42b7f13f7c825e271c5063695e7b83b5f35d30d1d",
    "apple": "afd03452fa8d2aec34328dc559062b0dad501d2c9d47aba5a70848b06e2ff556",
}, "0.3.0-preview.4": {
    "android": "c36ee7f2d200a5fae92d7787db97b28481059ee824a66b6865ff9b85bf8713a4",
    "apple": "9ceb576aaad1abb327bde8afb83e87d849d7f7d3a5da44e76bcc95325875a649",
}, "0.3.0-preview.5": {
    "android": "036c79d5d0a7caefbffbbaff9084b0b95577bbdd28087e9f5c47f1606a67b495",
    "apple": "3c55f436c9b3cf8acaed0fa7f68e72b1b4d46e24ea643c05f5e78694b8f28e9e",
}, "0.3.0-preview.8": {
    "android": "439542ed752af4916008ea3a4e2f263e122f1060d5c8048463d54290175a019d",
    "apple": "2735b42e9b40f46d821e7d95a33dbd3c68c874e0c503604bbc07208bf5b24776",
}}
DOCUMENTATION_CORRECTIONS = {
    "0.3.0-preview.4": "cf2b4e4f6a0f9c3f414ac3e1a3ba7cc1386e0596860c0a958f3e3fd7f8927a03",
    "0.3.0-preview.8": "76c8044bec95795202609f2a1ab3fbddbdbf5855e8ad41351b89c35fd3170aba",
}
PUBLISHED_VERSIONS = {"0.2.0-preview." + str(n) for n in (1, 2, 3, 4)}
DOCUMENTS = {"android": "docs/android-java-api.md", "apple": "docs/ios-swift-api.md"}


def sha256(data):
    return hashlib.sha256(data).hexdigest()


def unique_object(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError("Duplicate JSON key: " + key)
        result[key] = value
    return result


def loads(text):
    return json.loads(text, object_pairs_hook=unique_object)


def exact_keys(value, required, optional=()):
    if not isinstance(value, dict) or not set(required) <= set(value) or set(value) - set(required) - set(optional):
        raise ValueError("Unexpected or missing API reference fields")


def parse_baseline(platform, text):
    if not isinstance(text, str) or not text.endswith("\n"):
        raise ValueError("API baseline must preserve the reviewed UTF-8 text")
    records = {"type": [], "symbol": [], "relationship": [], "enum": []}
    for line in text.splitlines():
        if line.startswith("#"):
            continue
        kind, separator, raw = line.partition(" ")
        allowed = {"type"} if platform == "android" else {"symbol", "relationship", "enum"}
        if not separator or kind not in allowed:
            raise ValueError("Unexpected baseline record kind")
        value = loads(raw)
        if not isinstance(value, dict):
            raise ValueError("API baseline record must be an object")
        records[kind].append(value)
    entries = {}
    if platform == "android":
        for record in records["type"]:
            exact_keys(record, {"name", "declaration", "members", "enumConstants"}, {"annotations"})
            if not isinstance(record["name"], str) or not record["name"] or not isinstance(record["declaration"], str):
                raise ValueError("Invalid Java API type")
            if not isinstance(record["members"], list) or not isinstance(record["enumConstants"], list):
                raise ValueError("Invalid Java member or enum inventory")
            if "annotations" in record:
                validate_java_annotations(record)
            key = "android:type:" + record["name"]
            if key in entries:
                raise ValueError("Duplicate Java type")
            entries[key] = record
            for member in record["members"]:
                if not isinstance(member, str) or " descriptor: " not in member:
                    raise ValueError("Missing exact JVM descriptor")
                key = member_id(record["name"], member)
                if key in entries:
                    raise ValueError("Duplicate Java member")
                entries[key] = member
    else:
        for record in records["symbol"]:
            exact_keys(record, {"id", "kind", "access", "path", "declaration", "references"},
                       {"availability", "swiftGenerics", "swiftExtension", "spi"})
            if (not isinstance(record["id"], str) or not record["id"] or
                    not isinstance(record["path"], list) or not record["path"] or
                    any(not isinstance(part, str) or not part for part in record["path"]) or
                    not isinstance(record["declaration"], str)):
                raise ValueError("Invalid Swift symbol")
            key = "apple:symbol:" + record["id"]
            if key in entries:
                raise ValueError("Duplicate Swift symbol")
            entries[key] = record
        seen = set()
        for record in records["relationship"]:
            if not {"source", "target", "kind"} <= set(record):
                raise ValueError("Incomplete Swift relationship")
            raw = canonical(record)
            if raw in seen:
                raise ValueError("Duplicate Swift relationship")
            seen.add(raw)
        seen = set()
        for record in records["enum"]:
            exact_keys(record, {"path", "cases"})
            path = tuple(record["path"])
            if path in seen:
                raise ValueError("Duplicate Swift enum")
            seen.add(path)
            cases = set()
            for case in record["cases"]:
                exact_keys(case, {"case"}, {"rawValue"})
                if case["case"] in cases:
                    raise ValueError("Duplicate Swift enum case")
                cases.add(case["case"])
                if "rawValue" in case:
                    exact_keys(case["rawValue"], {"kind", "value", "negative"})
    if not entries:
        raise ValueError("API baseline is empty")
    return records, entries


def validate_java_annotations(record):
    metadata = record["annotations"]
    exact_keys(metadata, {"declaration", "members"})
    allowed = {"androidx.annotation." + name for name in
               ("AnyThread", "MainThread", "WorkerThread", "NonNull", "Nullable")} | {"java.lang.FunctionalInterface"}

    def markers(value):
        if (not isinstance(value, list) or any(not isinstance(name, str) or name not in allowed for name in value)
                or value != sorted(set(value))):
            raise ValueError("Invalid Java contract annotations")

    markers(metadata["declaration"])
    if not isinstance(metadata["members"], dict) or not set(metadata["members"]) <= set(record["members"]):
        raise ValueError("Java annotations reference a non-public member")
    for entry in metadata["members"].values():
        exact_keys(entry, {"declaration", "parameters"})
        markers(entry["declaration"])
        if not isinstance(entry["parameters"], dict):
            raise ValueError("Invalid Java parameter annotations")
        for index, annotations in entry["parameters"].items():
            if not re.fullmatch(r"0|[1-9][0-9]*", index):
                raise ValueError("Invalid Java parameter annotation index")
            markers(annotations)


def member_id(owner, member):
    return "android:member:" + owner + ":" + sha256(member.encode())[:16]


def canonical(value):
    return json.dumps(value, sort_keys=True, ensure_ascii=False, separators=(",", ":"))


def validate_annotations(platform, baseline_sha256, entries, annotations):
    exact_keys(annotations, {"schema", "platform", "baseline_sha256", "entries"})
    if annotations["schema"] != 1 or annotations["platform"] != platform:
        raise ValueError("Wrong API annotation schema or platform")
    if annotations["baseline_sha256"] != baseline_sha256:
        raise ValueError("Stale API annotations: baseline SHA-256 differs")
    if not isinstance(annotations["entries"], dict) or set(annotations["entries"]) != set(entries):
        missing = sorted(set(entries) - set(annotations.get("entries", {})))
        extra = sorted(set(annotations.get("entries", {})) - set(entries))
        raise ValueError("API annotation coverage differs; missing=" + repr(missing) + "; extra=" + repr(extra))
    for key, annotation in annotations["entries"].items():
        exact_keys(annotation, {"summary", "details"})
        for name in ("summary", "details"):
            text = annotation[name]
            if (not isinstance(text, str) or not text.strip() or
                    not re.search(r"[\u4e00-\u9fff]", text) or
                    re.search(r"\b(?:TODO|TBD)\b|待补充", text, re.I) or
                    re.fullmatch(r"\s*(?:TODO|TBD|待补充|占位(?:说明)?|测试占位)[.!。\s]*", text, re.I)):
                raise ValueError("Empty or placeholder API explanation: " + key)
            if re.search(r"[/](?:Users|home)[/]|[/]private[/]var[/]|[/]var[/]folders[/]|"
                         r"[A-Za-z]:[\\/](?:Users|Documents and Settings)[\\/]|"
                         r"-----BEGIN (?:[A-Z0-9 ]+ )?PRIVATE KEY|@[A-Z][A-Z0-9_]*@", text):
                raise ValueError("Private path, private key marker or unresolved placeholder in API explanation: " + key)


def validate_documentation_corrections(bundle):
    corrections = bundle.get("documentation_corrections")
    if "documentation_corrections" not in bundle:
        return
    exact_keys(corrections, {"schema", "sdk_version", "platforms"})
    version = bundle["sdk_version"]
    expected = DOCUMENTATION_CORRECTIONS.get(version)
    if (expected is None or type(corrections["schema"]) is not int or corrections["schema"] != 1 or
            corrections["sdk_version"] != version):
        raise ValueError("Invalid API documentation correction identity")
    raw = (json.dumps(corrections, sort_keys=True, ensure_ascii=False, indent=2) + "\n").encode()
    if sha256(raw) != expected:
        raise ValueError("API documentation corrections differ from reviewed SHA-256")
    exact_keys(corrections["platforms"], {"android", "apple"})
    if not any(corrections["platforms"].values()):
        raise ValueError("API documentation correction layer is empty")
    for platform, entries in corrections["platforms"].items():
        original = bundle["platforms"][platform]
        if not isinstance(entries, dict) or not set(entries) <= set(original["annotations"]["entries"]):
            raise ValueError("API documentation correction ID differs from compiler baseline: " + platform)
        validate_annotations(platform, original["baseline_sha256"], entries, {
            "schema": 1, "platform": platform, "baseline_sha256": original["baseline_sha256"], "entries": entries})


def validate_bundle(bundle):
    exact_keys(bundle, {"schema", "sdk_version", "native_sdk_version", "abi", "platforms"},
               {"contract_status", "documentation_corrections"})
    if bundle["schema"] != 1 or not isinstance(bundle["sdk_version"], str) or not re.fullmatch(
            r"\d+\.\d+\.\d+(?:[-+][A-Za-z0-9.-]+)?", bundle["sdk_version"]):
        raise ValueError("Invalid API reference schema or SDK version")
    version = bundle["sdk_version"]
    unpublished = version in DEVELOPMENT_BASELINES or version in CANDIDATE_BASELINES
    status = "candidate" if version in CANDIDATE_BASELINES else "development"
    if unpublished and bundle.get("contract_status") != status:
        raise ValueError("Unpublished API reference must identify its " + status + " contract")
    if not unpublished and "contract_status" in bundle:
        raise ValueError("Published API reference cannot select a development contract")
    if not unpublished and version not in PUBLISHED_VERSIONS:
        raise ValueError("Unsupported SDK contract version: " + version)
    identity = CANDIDATE_IDENTITIES.get(version, DEVELOPMENT_IDENTITIES.get(version, {
        "native_sdk_version": "0.2.0", "abi": {"version": 2, "revision": 10}}))
    if bundle["native_sdk_version"] != identity["native_sdk_version"] or bundle["abi"] != identity["abi"]:
        raise ValueError("API reference native SDK or C ABI differs")
    exact_keys(bundle["platforms"], {"android", "apple"})
    parsed = {}
    for platform, expected in CANDIDATE_BASELINES.get(version, DEVELOPMENT_BASELINES.get(version, APPROVED_BASELINES)).items():
        value = bundle["platforms"][platform]
        exact_keys(value, {"baseline", "baseline_sha256", "annotations"})
        if not isinstance(value["baseline"], str):
            raise ValueError("Invalid API baseline text")
        digest = sha256(value["baseline"].encode())
        if digest != value["baseline_sha256"] or digest != expected:
            raise ValueError("API baseline differs from reviewed SHA-256: " + platform)
        records, entries = parse_baseline(platform, value["baseline"])
        validate_annotations(platform, digest, entries, value["annotations"])
        if version in CANDIDATE_ANNOTATIONS:
            annotation_bytes = (json.dumps(value["annotations"], sort_keys=True, ensure_ascii=False, indent=2) + "\n").encode()
            if sha256(annotation_bytes) != CANDIDATE_ANNOTATIONS[version][platform]:
                raise ValueError("Candidate API annotations differ from reviewed SHA-256: " + platform)
        parsed[platform] = records
    validate_documentation_corrections(bundle)
    return parsed


def effective_annotations(bundle, platform):
    # Keep the sealed annotations intact for independent history verification.
    return {**bundle["platforms"][platform]["annotations"]["entries"],
            **bundle.get("documentation_corrections", {}).get("platforms", {}).get(platform, {})}


def anchor(key):
    return "api-" + sha256(key.encode())[:20]


def entry(lines, key, title, signature, annotation, language):
    lines += [f'<a id="{anchor(key)}"></a>', "", "### " + title, "", annotation["summary"], "",
              "```" + language, signature, "```", "", annotation["details"].strip(), "",
              "文档 ID：`" + key + "`", ""]


def table_cell(text):
    return str(text).replace("|", "\\|").replace("\n", " ")


def header(bundle, platform, count):
    title = "Android Java" if platform == "android" else "iOS Swift"
    status = bundle.get("contract_status")
    identity = "发布候选版本" if status == "candidate" else "本地开发版本" if status == "development" else "适用分发版本"
    return ["# " + title + " API 参考", "",
            identity + " **" + bundle["sdk_version"] + "**；原生 SDK **" + bundle["native_sdk_version"] +
            "**；C ABI **" + str(bundle["abi"]["version"]) + "/r" + str(bundle["abi"]["revision"]) + "**。", "",
            "本页逐项覆盖审定的编译器公共 API 基线，共 **" + str(count) + "** 个文档条目。",
            "声明、JVM descriptor、常量、枚举和编译器关系均来自基线，说明按稳定文档 ID 维护。",
            ("机器可读的冻结基线、原始说明和独立文案勘误见 [api-reference.json](api-reference.json)；"
             "本页已应用 `documentation_corrections`，保持原始契约快照。运行" if "documentation_corrections" in bundle else
             "机器可读的完整基线与说明见 [api-reference.json](api-reference.json)；运行"),
            "`python3 tools/check-api-reference.py` 可检查覆盖范围、审定 SHA-256 和本页字节一致性。", "",
            "基线 SHA-256：`" + bundle["platforms"][platform]["baseline_sha256"] + "`", "",
            "运行约束与错误处理见 [API 与生命周期](api-lifecycle.md) 和 [错误码与排障](errors-troubleshooting.md)。",
            ("本页是未发布的发布候选契约，不代表发布制品或公开历史证明。" if status == "candidate" else
             "本页是未发布的开发契约，不代表发布制品或公开历史证明。" if status == "development" else
             "本页覆盖发布的公共接口；编译器合成接口也明确标注。") +
            "硬件验收仍暂缓，`hardware_verified=false`。", ""]


def render_android(bundle, records):
    annotations = effective_annotations(bundle, "android")
    types = records["type"]
    lines = header(bundle, "android", len(types) + sum(len(t["members"]) for t in types))
    lines += ["## 类型导航", ""]
    for record in types:
        key = "android:type:" + record["name"]
        lines += ["- [" + record["name"].removeprefix("com.netumscan.printersdk.").replace("$", ".") + "](#" + anchor(key) + ")"]
    lines += ["", "Java 签名保留编译器记录的二进制嵌套名 `$`；源码导入和类型访问使用 `.`。",
              "方法参数名不在 `javap` 基线中，参数顺序与类型以声明和 descriptor 为准。", ""]
    for record in types:
        key = "android:type:" + record["name"]
        lines += [f'<a id="{anchor(key)}"></a>', "", "## " + record["name"].removeprefix("com.netumscan.printersdk.").replace("$", "."), "",
                  annotations[key]["summary"], "", "```java", record["declaration"], "```", "",
                  annotations[key]["details"].strip(), "", "文档 ID：`" + key + "`", ""]
        metadata = record.get("annotations", {"declaration": [], "members": {}})
        if metadata["declaration"]:
            lines += ["编译器注解：" + "、".join("`@" + name + "`" for name in metadata["declaration"]) + "。", ""]
        if record["enumConstants"]:
            lines += ["枚举声明顺序（`ordinal()` 从 0 起；不作为持久化协议值）：", "", "| ordinal | 枚举项 |", "| --- | --- |"]
            lines += ["| " + str(index) + " | `" + name + "` |" for index, name in enumerate(record["enumConstants"])]
            lines.append("")
        for index, member in enumerate(record["members"], 1):
            declaration, descriptor = member.split(" descriptor: ", 1)
            title = declaration.rsplit(";", 1)[0]
            entry(lines, member_id(record["name"], member), title, declaration,
                  annotations[member_id(record["name"], member)], "java")
            lines += ["JVM descriptor：`" + descriptor + "`", ""]
            markers = metadata["members"].get(member)
            if markers:
                if markers["declaration"]:
                    lines += ["编译器注解：" + "、".join("`@" + name + "`" for name in markers["declaration"]) + "。", ""]
                for parameter, names in sorted(markers["parameters"].items(), key=lambda item: int(item[0])):
                    lines += ["参数 " + parameter + "（从 0 起）注解：" +
                              "、".join("`@" + name + "`" for name in names) + "。", ""]
            if record["enumConstants"] and re.search(r"\b(?:values\(\)|valueOf\(java.lang.String\))", declaration):
                lines += ["这是 Java 编译器为枚举生成的公共方法。", ""]
    return ("\n".join(lines).rstrip() + "\n").encode()


def render_apple(bundle, records):
    annotations = effective_annotations(bundle, "apple")
    symbols = records["symbol"]
    groups = {}
    for symbol in symbols:
        groups.setdefault(symbol["path"][0], []).append(symbol)
    lines = header(bundle, "apple", len(symbols))
    lines += ["## 类型导航", ""]
    for name in sorted(groups):
        lines += ["- [" + name + "](#" + anchor("apple:group:" + name) + ")"]
    module = "XLPrinterSDK" if bundle["sdk_version"] in {
        "0.3.0-preview.15-dev", "0.3.0-preview.15", "0.3.0-preview.16-dev", "0.3.0-preview.16"} else "PrinterSDK"
    lines += ["", "公共模块为 `" + module + "`，C 桥接模块为 `CPrinterSDK`。",
              "本基线使用 iOS 15 arm64；符号附带的 Foundation 可用性不表示其他平台已完成分发或验收。",
              "未单列 availability 的符号以 SDK 的最低 iOS 版本 15 为接入前提。", ""]
    for name in sorted(groups):
        lines += [f'<a id="{anchor("apple:group:" + name)}"></a>', "", "## " + name, ""]
        for symbol in sorted(groups[name], key=lambda value: (value["path"], value["id"])):
            key = "apple:symbol:" + symbol["id"]
            entry(lines, key, ".".join(symbol["path"]), symbol["declaration"], annotations[key], "swift")
            lines += ["符号种类：`" + symbol["kind"] + "`；访问级别：`" + symbol["access"] + "`。", ""]
            if "::SYNTHESIZED::" in symbol["id"]:
                lines += ["这是编译器为当前类型带入的协议扩展／合成公共成员。", ""]
            for field, label in [("availability", "编译器 availability"), ("swiftGenerics", "泛型约束"),
                                 ("swiftExtension", "扩展来源"), ("spi", "SPI 元数据")]:
                if field in symbol:
                    lines += [label + "（保留基线原值）：", "", "```json", canonical(symbol[field]), "```", ""]
            if symbol["references"]:
                lines += ["声明引用的编译器标识：", "", "| 名称 | precise identifier |", "| --- | --- |"]
                lines += ["| " + table_cell(name) + " | `" + table_cell(identifier) + "` |"
                          for name, identifier in symbol["references"]]
                lines.append("")
    lines += ["## 枚举声明顺序与 raw 值", "", "顺序来自编译器 AST；raw 值保留符号、类型和值，不用展示顺序代替 raw 值。", ""]
    for record in records["enum"]:
        lines += ["### " + ".".join(record["path"]), "", "| 声明顺序（从 0 起） | case | raw 值 | 编译器原值 |", "| --- | --- | --- | --- |"]
        for index, case in enumerate(record["cases"]):
            raw = case.get("rawValue")
            value = ("-" if raw["negative"] else "") + raw["value"] if raw else "无 raw 值"
            lines += ["| " + str(index) + " | `" + case["case"] + "` | `" + table_cell(value) + "` | `" +
                      table_cell(canonical(raw)) + "` |"]
        lines.append("")
    lines += ["## 编译器公共关系", "", "完整保留 **" + str(len(records["relationship"])) + "** 条继承、协议遵循、成员及合成关系；",
              "source 和 target 使用精确编译器标识，外部 Swift/Foundation 类型也保持原值。", "",
              "| kind | source | target | 其他编译器字段 |", "| --- | --- | --- | --- |"]
    for relationship in records["relationship"]:
        other = {key: value for key, value in relationship.items() if key not in {"kind", "source", "target"}}
        lines += ["| `" + table_cell(relationship["kind"]) + "` | `" + table_cell(relationship["source"]) +
                  "` | `" + table_cell(relationship["target"]) + "` | `" + table_cell(canonical(other)) + "` |"]
    return ("\n".join(lines).rstrip() + "\n").encode()


def render_documents(bundle):
    parsed = validate_bundle(bundle)
    return {DOCUMENTS["android"]: render_android(bundle, parsed["android"]),
            DOCUMENTS["apple"]: render_apple(bundle, parsed["apple"])}


def ordinary_file(root, relative):
    path = Path(root)
    for part in Path(relative).parts:
        path = path / part
        if path.is_symlink():
            raise ValueError("Symlinked API reference input: " + relative)
    if not path.is_file():
        raise ValueError("Missing API reference input: " + relative)
    return path


def check(root):
    root = Path(root)
    bundle = loads(ordinary_file(root, "docs/api-reference.json").read_text(encoding="utf-8"))
    documents = render_documents(bundle)
    for name, expected in documents.items():
        if ordinary_file(root, name).read_bytes() != expected:
            raise ValueError("API reference document drift: " + name)
    lock = root / "sdk-lock.json"
    version = root / "VERSION"
    if (lock.exists() or lock.is_symlink()) and loads(ordinary_file(root, "sdk-lock.json").read_text())["version"] != bundle["sdk_version"]:
        raise ValueError("API reference SDK version differs from sdk-lock.json")
    if (version.exists() or version.is_symlink()) and ordinary_file(root, "VERSION").read_text().strip() != bundle["sdk_version"]:
        raise ValueError("API reference SDK version differs from VERSION")
    package = root / "Package.swift"
    if package.exists() or package.is_symlink():
        urls = re.findall(r'url:\s*"(https://github\.com/netumscan/printer-sdk-ios/releases/download/[^"\s]+)"',
                          ordinary_file(root, "Package.swift").read_text())
        prefix = "https://github.com/netumscan/printer-sdk-ios/releases/download/v" + bundle["sdk_version"] + "/"
        expected = {prefix + name for name in ("XLPrinterSDK.xcframework.zip", "PrinterSDK.xcframework.zip")}
        if len(urls) != 1 or urls[0] not in expected:
            raise ValueError("API reference SDK version differs from Package.swift binary Release URL")
    parsed = validate_bundle(bundle)
    return {"api_reference_verified": True, "sdk_version": bundle["sdk_version"],
            "contract_status": bundle.get("contract_status", "historical"),
            "baseline_sha256": dict(CANDIDATE_BASELINES.get(bundle["sdk_version"],
                DEVELOPMENT_BASELINES.get(bundle["sdk_version"], APPROVED_BASELINES))),
            "android_types": len(parsed["android"]["type"]),
            "android_members": sum(len(item["members"]) for item in parsed["android"]["type"]),
            "swift_symbols": len(parsed["apple"]["symbol"]),
            "swift_relationships": len(parsed["apple"]["relationship"]),
            "swift_enums": len(parsed["apple"]["enum"]),
            "documents_sha256": {name: sha256(value) for name, value in documents.items()}}


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1])
    args = parser.parse_args(argv)
    try:
        print(json.dumps(check(args.root), sort_keys=True, ensure_ascii=False))
    except (ValueError, KeyError, OSError, TypeError) as error:
        parser.exit(1, str(error) + "\n")
    return 0


if __name__ == "__main__":
    sys.exit(main())
