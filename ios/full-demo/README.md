# iOS Full Demo

Open [`../PrinterSamples.xcodeproj`](../PrinterSamples.xcodeproj), select the
`PrinterFullDemo` scheme and run on iOS 15+. Xcode 15+ resolves the exact public
`PrinterSDK` version `0.2.0-preview.2`; no local SDK checkout is needed.

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
