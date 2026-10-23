# iOS Quick Start

See the current [iOS customer guide](../SDK_GUIDE.md) for SDK permissions and API lifecycle.

Open [`../PrinterSamples.xcodeproj`](../PrinterSamples.xcodeproj), select the
`PrinterQuickStart` scheme and run on iOS 15+. Xcode 16+ resolves the exact public
`XLPrinterSDK` dependency below; no local SDK checkout is needed.

## Current SDK dependency

| Field | Value |
| --- | --- |
| Repository | `https://github.com/netumscan/printer-sdk-ios.git` |
| Exact version | `0.3.0-preview.16` |
| Release commit | `c7c00f40855a998c4f408c01323f8ce6d6816ee7` |

## Running the sample

Scan explicitly, select a BLE candidate, confirm the supported built-in model and connect. Submit one generated PNG image with page dimensions in millimetres, observe the completion result, cancel or disconnect. Backgrounding releases the session; returning does
not reconnect or replay. `DATA_SENT` does not establish actual paper output.

`QuickStartModel` owns `PrinterManager` and submits a `UIImage` with page width and height in millimeters. PNG/JPEG `Data` uses `printEncodedImage`. Select the confirmed printer model explicitly; paper type selection and label positioning are separate requests.

See the [iOS guide](../README.md) for setup, licensing and complete online
verification. This sample is MIT; the downloaded SDK remains closed commercial
software under its own license. `hardware_verified=false`.

Current iOS setup and CocoaPods / SwiftPM migration: [XLPrinterSDK guide](../SDK_GUIDE.md).
