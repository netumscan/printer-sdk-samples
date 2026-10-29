# iOS Quick Start

See the shared [customer guide](../../docs/README.md) for SDK permissions and API lifecycle.

Open [`../PrinterSamples.xcodeproj`](../PrinterSamples.xcodeproj), select the
`PrinterQuickStart` scheme and run on iOS 15+. Xcode 16+ resolves the exact public
`XLPrinterSDK` dependency below; no local SDK checkout is needed.

## Current SDK dependency

| Field | Value |
| --- | --- |
| Repository | `https://github.com/netumscan/printer-sdk-ios.git` |
| Exact version | `0.3.0-preview.17` |
| Release commit | `98bf75c66370d1b85f165ca297a9b4fa299e0fde` |

## Running the sample

Scan explicitly, select a BLE candidate, confirm the supported built-in model and connect. Submit one generated PNG image with page dimensions in millimetres, observe the completion result, cancel or disconnect. Backgrounding releases the session; returning does
not reconnect or replay. `DATA_SENT` does not establish actual paper output.

`QuickStartModel` implements `PrinterDelegate` and assigns `manager.delegate = self`. The delegate is weak: the App retains the model until cleanup completes. Requests return IDs; `printerManager(_:didDiscover:scanID:)` updates the visible list during scanning and the terminal result supplies merged names and RSSI. Cancel the scan by its ID, wait for its terminal result, then connect explicitly. Printing uses PNG `Data` with `printEncodedImage` and page dimensions in millimetres. Replacing or clearing the delegate invalidates queued notifications.

See the [iOS guide](../README.md) for setup, licensing and complete online
verification. This sample is MIT; the downloaded SDK remains closed commercial
software under its own license. `hardware_verified=false`.
