# CI verification evidence

Every Android and iOS CI job collects a separate diagnostic artifact, including
when verification fails. Open the **Samples** workflow run, select
`android-software-verification` or `ios-software-verification`, and download it
before the 14-day retention period expires.

Start with `evidence-manifest.json`. Its `verification_status` is `passed`,
`failed`, or `unavailable`; `unavailable` means that the verification command did
not produce usable status evidence. Successful collection does not mean that
verification passed. The verification step keeps its original nonzero exit code,
so collection and upload cannot turn a failed job into a successful job.

For an early Wrapper download, checksum, or toolchain failure, inspect
`command/command.log` and the Android `gradle-bootstrap.log`, `gradle-tool.json`,
and `source-check.json` copies when available. For build or runtime failures,
inspect the available `verification/` logs and JSON reports. The command log also
captures top-level iOS exceptions that occur before `validation.json` is written.
If emulator or runner setup fails before verification starts, the manifest records
missing command/report files rather than claiming a completed SDK check.

The collector reads only these diagnostic paths:

| Platform | Allowed inputs |
| --- | --- |
| Both | Separate command directory: `command.json`, `command.log` |
| Android | `validation.json`, `runtime-validation.json`, `instrumentation-result.json`, `quick-start-runtime.json`, `full-demo-journal.log`, `gradle.log`, `adb.log`, `source-check.json`, `gradle-bootstrap.log`, `gradle-tool.json` |
| iOS | `validation.json` and numbered `logs/NN.log` / `logs/NNN.log` files |

APK/AAR files, frameworks, package caches, source-package checkouts, Xcode result
bundles, screenshots, credentials, and arbitrary build directories are not
collected. Symlinks and nonregular files are rejected. Each included copy is at
most 256 KiB, at most 64 files are copied, and the whole artifact is limited to
4 MiB. Logs may retain only a marked tail; JSON reports are omitted rather than
truncated into invalid JSON. A source file larger than 8 MiB is skipped without
reading its contents. Directory enumeration also has a fixed limit. Missing,
omitted, and truncated inputs are listed in the manifest.

Copies mask known patterns for credentials, Authorization/Bearer values, user
home paths, email addresses, Bluetooth MAC addresses, UUIDs, and raw payload or
content fields. SDK versions, public commits, checksum values, runtime nonces,
and numeric diagnostics are retained. The collector does not inspect environment
variables, global user configuration, keychains, or credential stores. Pattern
masking cannot identify every possible secret in arbitrary application text;
review an artifact before sharing it outside the intended support context.

Original local reports and logs are never rewritten. A manifest entry records
`source_sha256` for the original bytes when the bounded read completes and
`archive_sha256` for the copied, potentially masked bytes. These hashes can
differ. Diagnostic copies are not replacements for the original runtime nonce
checks, signed-off artifact hashes, or private publication/synchronization
receipts. Hardware acceptance remains deferred (`hardware_verified=false`).

To collect an already completed local verification without rerunning it:

```sh
python3 tools/collect-evidence.py collect --platform android \
  --source /absolute/path/to/verification \
  --command-output /absolute/path/to/verification-command \
  --output /absolute/path/to/new-masked-evidence
```

Use `--platform ios` for iOS. The output must be a new directory separate from
both originals. If a local run did not use the command capture helper, its command
status is honestly `unavailable` even when some verification reports exist.

To capture a new run, keep the command capture directory separate from the
verifier's own output directory:

```sh
python3 tools/collect-evidence.py run --output /absolute/path/to/new-command \
  -- bash android/verify.sh --emulator-serial emulator-5554 \
  --output /absolute/path/to/new-verification
```

Use the corresponding [Android](android/README.md) or [iOS](ios/README.md)
instructions for prerequisites. Only explicitly selected emulators/simulators
are used; these checks do not install on physical phones or accept printer
hardware.

## Flutter verification

The separate **Flutter** workflow pins Flutter 3.41.9 to upstream commit
`00b0c91f06209d9e4a41f71b7a512d6eb3b9c694`. Its `flutter-android` and
`flutter-ios` jobs use the public [Flutter Quick Start](flutter/README.md), its
committed Dart dependency lock, and exact SDK 0.3.0-preview.16 pins.

The unified sample synchronization requires both workflows on the same candidate
commit before updating main, then verifies both workflows again on that exact
main commit. Native and Flutter results remain separate in the sync receipt.

Both jobs run static analysis and the 18 Dart/widget tests. Android builds a
debug APK, validates the AAR SHA-256 from `flutter/sdk-lock.json`, and compares
the packaged SDK libraries and notices with that exact AAR. iOS builds without
production signing and runs the 5 RunnerTests on an available simulator,
including per-request final results and an independently started engine detach.
No job connects to a printer or publishes an APK/IPA.

Open the Flutter workflow's individual step logs for these checks. The two
`*-software-verification` masked artifacts described above belong to the native
Samples workflow; they are not evidence for the Flutter jobs. A passing source
inventory check alone does not establish a successful Flutter build or runtime.

Local checks:

```sh
python3 tools/check-samples.py
cd flutter
flutter pub get --enforce-lockfile
flutter analyze --no-pub
flutter test --no-pub
flutter build apk --debug --no-pub
```

For iOS simulator architecture notes and the unsigned Xcode command, see the
[Flutter README](flutter/README.md). Native SDK build/runtime evidence remains
scoped to the corresponding native workflow and version. Hardware acceptance
remains deferred (`hardware_verified=false`).
