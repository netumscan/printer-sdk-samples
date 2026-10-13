# iOS Full Demo

See the shared [customer guide](../../docs/README.md) for diagnostic records,
compatibility boundaries and support.

Open [`../PrinterSamples.xcodeproj`](../PrinterSamples.xcodeproj), select the
`PrinterFullDemo` scheme and run on iOS 15+. Xcode 15+ resolves the exact public
`PrinterSDK` dependency below; no local SDK checkout is needed.

## Current SDK dependency

| Field | Value |
| --- | --- |
| Repository | `https://github.com/netumscan/printer-sdk-ios.git` |
| Exact version | `0.2.0-preview.4` |
| Release commit | `195458b93a8c7c5465c53dcd563398f7ad18f5b0` |

## Running the sample

This app demonstrates explicit BLE discovery/connection, CPCL and ESC-POS image
jobs, image import/preview, canvas configuration, media actions, Raw diagnostics,
operation cancellation, local configuration and bounded logs. Start by confirming
device/firmware settings; fields are initially empty. An optional M90 development
preset does not certify the device. No scan, connection, command or saved settings
are replayed automatically.

See the [iOS guide](../README.md) for setup, known firmware limitations, licensing
and complete online verification. This sample is MIT; the downloaded SDK remains
closed commercial software under its own license. `DATA_SENT` is transfer
completion, not physical print confirmation. `hardware_verified=false`.
