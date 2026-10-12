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
            exact_keys(record, {"name", "declaration", "members", "enumConstants"})
            if not isinstance(record["name"], str) or not record["name"] or not isinstance(record["declaration"], str):
                raise ValueError("Invalid Java API type")
            if not isinstance(record["members"], list) or not isinstance(record["enumConstants"], list):
                raise ValueError("Invalid Java member or enum inventory")
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


def validate_bundle(bundle):
    exact_keys(bundle, {"schema", "sdk_version", "native_sdk_version", "abi", "platforms"})
    if bundle["schema"] != 1 or not isinstance(bundle["sdk_version"], str) or not re.fullmatch(
            r"\d+\.\d+\.\d+(?:[-+][A-Za-z0-9.-]+)?", bundle["sdk_version"]):
        raise ValueError("Invalid API reference schema or SDK version")
    if bundle["native_sdk_version"] != "0.2.0" or bundle["abi"] != {"version": 2, "revision": 10}:
        raise ValueError("API reference native SDK or C ABI differs")
    exact_keys(bundle["platforms"], {"android", "apple"})
    parsed = {}
    for platform, expected in APPROVED_BASELINES.items():
        value = bundle["platforms"][platform]
        exact_keys(value, {"baseline", "baseline_sha256", "annotations"})
        if not isinstance(value["baseline"], str):
            raise ValueError("Invalid API baseline text")
        digest = sha256(value["baseline"].encode())
        if digest != value["baseline_sha256"] or digest != expected:
            raise ValueError("API baseline differs from reviewed SHA-256: " + platform)
        records, entries = parse_baseline(platform, value["baseline"])
        validate_annotations(platform, digest, entries, value["annotations"])
        parsed[platform] = records
    return parsed


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
    return ["# " + title + " API 参考", "",
            "适用分发版本 **" + bundle["sdk_version"] + "**；原生 SDK **0.2.0**；C ABI **2/r10**。", "",
            "本页逐项覆盖审定的编译器公共 API 基线，共 **" + str(count) + "** 个文档条目。",
            "声明、JVM descriptor、常量、枚举和编译器关系均来自基线，说明按稳定文档 ID 维护。",
            "机器可读的完整基线与说明见 [api-reference.json](api-reference.json)；运行",
            "`python3 tools/check-api-reference.py` 可检查覆盖范围、审定 SHA-256 和本页字节一致性。", "",
            "基线 SHA-256：`" + bundle["platforms"][platform]["baseline_sha256"] + "`", "",
            "运行约束与错误处理见 [API 与生命周期](api-lifecycle.md) 和 [错误码与排障](errors-troubleshooting.md)。",
            "本页覆盖发布的公共接口；编译器合成接口也明确标注。硬件验收仍暂缓，`hardware_verified=false`。", ""]


def render_android(bundle, records):
    annotations = bundle["platforms"]["android"]["annotations"]["entries"]
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
            if record["enumConstants"] and re.search(r"\b(?:values\(\)|valueOf\(java.lang.String\))", declaration):
                lines += ["这是 Java 编译器为枚举生成的公共方法。", ""]
    return ("\n".join(lines).rstrip() + "\n").encode()


def render_apple(bundle, records):
    annotations = bundle["platforms"]["apple"]["annotations"]["entries"]
    symbols = records["symbol"]
    groups = {}
    for symbol in symbols:
        groups.setdefault(symbol["path"][0], []).append(symbol)
    lines = header(bundle, "apple", len(symbols))
    lines += ["## 类型导航", ""]
    for name in sorted(groups):
        lines += ["- [" + name + "](#" + anchor("apple:group:" + name) + ")"]
    lines += ["", "公共模块为 `PrinterSDK`，C 桥接模块为 `CPrinterSDK`。",
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
        expected = "https://github.com/netumscan/printer-sdk-ios/releases/download/v" + bundle["sdk_version"] + "/PrinterSDK.xcframework.zip"
        if urls != [expected]:
            raise ValueError("API reference SDK version differs from Package.swift binary Release URL")
    parsed = validate_bundle(bundle)
    return {"api_reference_verified": True, "sdk_version": bundle["sdk_version"],
            "baseline_sha256": dict(APPROVED_BASELINES),
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
