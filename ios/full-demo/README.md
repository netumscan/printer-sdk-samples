# iOS Full Demo

See the shared [customer guide](../../docs/README.md) for diagnostic records,
compatibility boundaries and support.

Open [`../PrinterSamples.xcodeproj`](../PrinterSamples.xcodeproj), select the
`PrinterFullDemo` scheme and run on iOS 15+. Xcode 16+ resolves the exact public
`PrinterSDK` dependency below; no local SDK checkout is needed.

## Current SDK dependency

| Field | Value |
| --- | --- |
| Repository | `https://github.com/netumscan/printer-sdk-ios.git` |
| Exact version | `0.3.0-preview.10` |
| Release commit | `ed5aa42043d4adf32393c19a057b80510dfed294` |

## Running the sample

This app demonstrates explicit BLE discovery/connection, PNG/JPEG image printing, import/preview, page dimensions, built-in model and paper type selection, label positioning, Raw diagnostics,
operation cancellation, local configuration and bounded logs. Start by confirming
the actual device model and paper. Selecting the built-in M90 model does not certify hardware. Communication and rendering policies stay inside the SDK. No scan, connection, command or saved settings
are replayed automatically.

See the [iOS guide](../README.md) for setup, known firmware limitations, licensing
and complete online verification. This sample is MIT; the downloaded SDK remains
closed commercial software under its own license. `DATA_SENT` is transfer
completion, not physical print confirmation. `hardware_verified=false`.
