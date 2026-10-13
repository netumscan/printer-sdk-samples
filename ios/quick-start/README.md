# iOS Quick Start

See the shared [customer guide](../../docs/README.md) for SDK permissions and API lifecycle.

Open [`../PrinterSamples.xcodeproj`](../PrinterSamples.xcodeproj), select the
`PrinterQuickStart` scheme and run on iOS 15+. Xcode 15+ resolves the exact public
`PrinterSDK` dependency below; no local SDK checkout is needed.

## Current SDK dependency

| Field | Value |
| --- | --- |
| Repository | `https://github.com/netumscan/printer-sdk-ios.git` |
| Exact version | `0.2.0-preview.4` |
| Release commit | `195458b93a8c7c5465c53dcd563398f7ad18f5b0` |

## Running the sample

Scan explicitly, select a BLE candidate, confirm the M90 V1.2 / CPCL development
settings and connect. Submit one generated grayscale image, observe operation
status, cancel or disconnect. Backgrounding releases the session; returning does
not reconnect or replay. `DATA_SENT` does not establish actual paper output.

See the [iOS guide](../README.md) for setup, licensing and complete online
verification. This sample is MIT; the downloaded SDK remains closed commercial
software under its own license. `hardware_verified=false`.
