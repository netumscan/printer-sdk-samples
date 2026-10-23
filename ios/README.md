# iOS samples

See the current [iOS customer guide](SDK_GUIDE.md) for SDK permissions, lifecycle,
capabilities, errors, runtime identity and validation records and support boundaries.

Open `PrinterSamples.xcodeproj` with Xcode 16 or newer (Swift 6+), then select a scheme:

| Scheme | Source | Purpose |
| --- | --- | --- |
| `PrinterQuickStart` | `quick-start/` | Minimal explicit BLE scan, selection, connection, image submission, cancellation and operation status |
| `PrinterFullDemo` | `full-demo/` | PNG/JPEG import and preview, page dimensions, model/paper selection, label positioning, Raw, local configuration and bounded logs |

Both apps require iOS 15+ and use the exact public dependency below.
Quick Start and its tests use Swift 6.0 language mode; Full Demo retains
Swift 5 language mode with the same SDK. The shared APP owner supports Swift 6 strict concurrency without a preconcurrency import.
The committed `Package.resolved` uses the same version and release commit;
root `ios-sdk-lock.json` also records the binary checksum. No SDK implementation or
local package path is included here.

## Current SDK dependency

| Field | Value |
| --- | --- |
| Repository | `https://github.com/netumscan/printer-sdk-ios.git` |
| Exact version | `0.3.0-preview.15` |
| Release commit | `346df124ddf9a6e07e59c38b83e2d91b25a9d66a` |

## App configuration

The sample apps use independent Bundle IDs:

- `com.netumscan.printersdk.samples.quickstart`
- `com.netumscan.printersdk.samples.fulldemo`

For a physical device, select your own signing team in Xcode. No signing team,
provisioning profile, private device identity or developer credentials are supplied.
Simulator builds and the automated verification need no developer account.

## Quick Start

1. Run `PrinterQuickStart`. Tap the scan button, allow Bluetooth access and wait
   for the finite scan to finish. Select the intended device explicitly.
2. Confirm that the firmware supports the sample's M90 V1.2 BLE credit flow and
   the built-in LT-M90 model, then connect. The SDK manages communication and print geometry.
3. Submit one generated PNG test image with page width and height in millimetres. Observe the completion result. Cancel explicitly or disconnect when finished.

Selecting a model is a local configuration choice, not device certification. A discovered advertisement is only a BLE candidate.
Blocking SDK calls run on worker queues; foreground UI actions choose when to
connect and print. Moving to the background stops scanning and releases the
session. Returning does not reconnect, replay settings or resubmit work.

## Full Demo

Choose a device and confirm its supported built-in model and actual paper type. Connect, import PNG/JPEG, set page width/height and print manually. Preview is optional. SDK image preparation shrinks proportionally and centres on white, without upscaling or cropping; printing does not implicitly FORM or cut.

`更多` provides explicit configuration save/load/clear and Raw diagnostics. The
log screen has a bounded local journal and explicit export/clear. No saved
configuration or raw payload is loaded or sent automatically. Raw commands,
media changes, label positioning and image jobs share the SDK device queue.
Only use Raw commands established for the selected firmware.

`DATA_SENT` confirms byte transfer, not paper output or applied settings. Cancel
cannot recall bytes already accepted by the printer. Check the physical result
before reconnecting or manually submitting a new job. Some firmware may retain
cancelled data; neither app sends guessed buffer-clearing commands or retries
the stopped job. M90 ESC/POS and independent label positioning remain unverified.

## Online build and simulator verification

With Xcode 16 or newer and an installed iOS simulator runtime, from the repository root:

```sh
bash ios/verify.sh --output "$PWD/build/ios"
```

Use a new output directory for each run. The verifier downloads the public
Swift package and binary into fresh caches, checks the exact commit and ZIP
checksum, compares unpacked bytes, and builds both apps for device arm64 and
simulator arm64/x86_64. Each build checks the dynamic framework, its Bundle ID,
28 C exports and privacy resource. 18 tests then run on an available iPhone
simulator: current public values, invalid input, Quick Start request ownership, cancellation and offline cleanup. Both apps are launched afterward. No test scans BLE or contacts a
printer. Reports, screenshots and the XCTest result bundle stay in `build/`.

Optionally choose `--simulator-udid <simulator-id>`. If no simulator runtime is
installed, `--skip-runtime` builds all six configurations and records that tests
and launches were omitted. CI uses the complete simulator verification.

The sample source is MIT licensed. The separately downloaded **PrinterSDK is
closed commercial software** under its own license; MIT does not apply to the SDK
Swift wrapper, binary, headers or resources. See the root license boundary notice.
`hardware_verified=false`: these checks establish integration, not physical
Bluetooth, firmware or paper acceptance.

Current iOS setup and CocoaPods / SwiftPM migration: [XLPrinterSDK guide](SDK_GUIDE.md).
