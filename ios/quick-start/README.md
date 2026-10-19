# iOS Quick Start

See the shared [customer guide](../../docs/README.md) for SDK permissions and API lifecycle.

Open [`../PrinterSamples.xcodeproj`](../PrinterSamples.xcodeproj), select the
`PrinterQuickStart` scheme and run on iOS 15+. Xcode 16+ resolves the exact public
`PrinterSDK` dependency below; no local SDK checkout is needed.

## Current SDK dependency

| Field | Value |
| --- | --- |
| Repository | `https://github.com/netumscan/printer-sdk-ios.git` |
| Exact version | `0.3.0-preview.2` |
| Release commit | `599ab4f496cbdfdb53f2f75bcd072f3a79240e5a` |

## Running the sample

Scan explicitly, select a BLE candidate, confirm the M90 V1.2 / CPCL development
settings and connect. Submit one generated grayscale image, observe operation
status, cancel or disconnect. Backgrounding releases the session; returning does
not reconnect or replay. `DATA_SENT` does not establish actual paper output.

See the [iOS guide](../README.md) for setup, licensing and complete online
verification. This sample is MIT; the downloaded SDK remains closed commercial
software under its own license. `hardware_verified=false`.
