# Android Full Demo

This is the current Android application from the SDK development repository. It
uses the exact Maven Central dependency `com.netumscan:printer-sdk-android:0.3.0-preview.8`.
[sdk-lock.json](sdk-lock.json) records its AAR SHA-256, release commit and ABI.
The root lock and the other three applications retain their historical SDK pins.

1. Search for a printer, select it and connect.
2. The SDK matches a Profile by the device name. Configure an unknown device
   after connection, then initialize its paper mode.
3. Choose the default image or import a PNG/JPEG, enter label dimensions and print.

`PrinterManager` owns one device. The owner receives a Callback for each
`RequestId`; the Activity observes owner state. Profile configuration belongs to
the current connection. `PrintOptions` carries images, dimensions, copies and
optional calibration. LT-M90 uses a centered Profile with 8 dots/mm and 864 dots;
50 × 50 mm is 400 × 400 dots with the default X origin at 232. Calibration is
signed dots; it does not change the Profile.

The page also demonstrates FIFO requests, per-request cancellation, RGB/RGBA,
dithering, multiple images, blank segments, receipt mode, Raw commands,
configuration import/export and bounded image previews. Image selection and
printing keep the device selection locked. Activity recreation retains the
owner; background cleanup cancels work and disconnects. Returning to the page
does not reconnect or replay printing. A new process restores inputs only.
`DATA_SENT` confirms transfer, not physical printing.

Build and verify from the repository root:

```sh
bash android/verify.sh --emulator-serial emulator-5554 --output /tmp/printer-samples-android
```

The gate builds Debug and R8 Release APKs, runs lint, checks each application's
own exact AAR and native payload, then executes the historical Quick Start
offline gate and this application's complete instrumentation on the selected
emulator. Bluetooth permissions stay ungranted; tests use memory transport and
do not print paper. Historical iOS validation keeps its original gate. The
replaced Android Full Demo's historical journal gate is preserved in the old
release snapshot, not claimed as a test of this application.

CI diagnostic collection recognizes the schema-2 mixed report, verifies both
pins and runtime evidence hashes, and reports success only after the complete
current suite passes. It does not require the retired Full Demo journal. The
original masking, filesystem and size limits remain in the preserved legacy
collector; iOS and schema-1 evidence keep their original validation rules.

Sources under `src/main` and `src/androidTest` are exported byte-for-byte from
the current Demo. `samples-manifest.json` records their separate fingerprint.
The generated diagnostic asset ties this snapshot to its independent SDK lock.
Legacy API reference pages in the root describe the historical dependency;
see the current [Android SDK documentation](https://github.com/netumscan/printer-sdk-android)
for this release. SDK implementation remains proprietary; these Demo sources are
MIT. SDK licenses and notices remain embedded in the APK.
