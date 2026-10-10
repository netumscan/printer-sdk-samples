# iOS samples

Open `PrinterSamples.xcodeproj` with Xcode 15 or newer (Swift 5.9+), then select a scheme:

| Scheme | Source | Purpose |
| --- | --- | --- |
| `PrinterQuickStart` | `quick-start/` | Minimal explicit BLE scan, selection, connection, image submission, cancellation and operation status |
| `PrinterFullDemo` | `full-demo/` | CPCL / ESC-POS, image import and preview, canvas settings, media controls, Raw diagnostics, local configuration and bounded logs |

Both apps require iOS 15+ and resolve `PrinterSDK` from
`https://github.com/netumscan/printer-sdk-ios.git` at **exactly `0.2.0-preview.2`**.
The committed `Package.resolved` pins commit
`b629ccc1aeddf0370a3845cfb3f47152df0e9a5d`; root `sdk-lock.json` also records the
binary checksum. No SDK implementation or local package path is included here.

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
   CPCL settings. Adjust DPI and maximum device width, then connect.
3. Submit one generated grayscale test image. Observe the operation state and
   bytes sent. Cancel explicitly or disconnect when finished.

The displayed `203 DPI / 864 dots` values are a development preset, not device
detection or certification. A discovered advertisement is only a BLE candidate.
Blocking SDK calls run on worker queues; foreground UI actions choose when to
connect and print. Moving to the background stops scanning and releases the
session. Returning does not reconnect, replay settings or resubmit work.

## Full Demo

The Full Demo starts with empty device and canvas fields. Choose a device and
explicitly enter compatible settings, or apply the M90 development preset after
confirming the target hardware. Connect, prepare an image and submit it manually.
The image and preview use the same immutable grayscale snapshot.

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
49 C exports and privacy resource. Four tests then run on an available iPhone
simulator: ABI handshake/catalog, Swift configuration validation, Quick Start
invalid configuration, and partial writes/Raw ordering through an in-memory
transport. Both apps are launched afterward. No test scans BLE or contacts a
printer. Reports, screenshots and the XCTest result bundle stay in `build/`.

Optionally choose `--simulator-udid <simulator-id>`. If no simulator runtime is
installed, `--skip-runtime` builds all six configurations and records that tests
and launches were omitted. CI uses the complete simulator verification.

The sample source is MIT licensed. The separately downloaded **PrinterSDK is
closed commercial software** under its own license; MIT does not apply to the SDK
Swift wrapper, binary, headers or resources. See the root license boundary notice.
`hardware_verified=false`: these checks establish integration, not physical
Bluetooth, firmware or paper acceptance.
