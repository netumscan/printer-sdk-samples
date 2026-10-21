# Android Quick Start

This is the customer entry point: scan BLE devices, select your printer, connect,
print one fixed image, inspect the result and disconnect. It uses the exact Maven
Central dependency `com.netumscan:printer-sdk-android:0.3.0-preview.8`.
[sdk-lock.json](sdk-lock.json) records the AAR SHA-256, release commit and ABI.
The [Full Demo](../full-demo/README.md) supports daily SDK development and testing,
including logs and saved print settings.

Read [QuickStartModel.java](src/main/java/com/netumscan/printersdk/samples/quickstart/QuickStartModel.java)
for the public `PrinterManager` flow, then
[MainActivity.java](src/main/java/com/netumscan/printersdk/samples/quickstart/MainActivity.java)
for controls, permissions and lifecycle handling.

1. Scan and explicitly select your LT-M90 BLE printer. Selection never connects.
   An unknown name has no guessed communication or print settings; use Full Demo
   to configure that device. A recognized name does not authenticate the model.
2. Connect. The SDK resolves connection options from the original discovered name
   and adopts the connection's print Profile.
3. Explicitly print the fixed 384 × 64 dot GRAY8 pattern, one copy, FORM disabled.
   Confirm that your printer and paper suit the example before sending it.
4. Inspect the original request outcome, code, message, completion and
   `outputMayHaveOccurred`, then disconnect. `DATA_SENT` confirms transfer only;
   inspect actual paper output to confirm physical printing.

Cancellation keeps its real request terminal separate from cleanup. Backgrounding
cancels work and disconnects; Activity destruction closes the Manager. Returning
to the page never reconnects, retries or replays a print. Each new Activity or
process starts offline. There are no logs, saved parameters, image import,
preview, Raw commands or advanced device controls in this application.
Android 12+ needs nearby-device permissions; Android 6–11 BLE scanning needs
location permission and system location enabled. Permission refusal leaves the
application offline until another explicit user action.

Build and verify both Android consumers from the repository root:

```sh
bash android/verify.sh --emulator-serial emulator-5554 --output /tmp/printer-samples-android
```

The gate builds Debug and R8 Release APKs, runs lint, checks each application's
exact AAR, JNI/native payload and licenses, then runs Full Demo instrumentation
and this application's offline lifecycle gate on the selected emulator. Quick
Start checks a fresh idle model with no printer session or request, loads the
actual JNI identity (`0.3.0-preview.8`, native `0.3.0`, ABI 3/r2), and verifies
that Activity destruction completes Manager cleanup. Bluetooth permissions stay
ungranted. The offline gate does not verify real BLE compatibility or paper
output; `hardware_verified` stays false.

`src/main` is exported byte-for-byte from the current SDK repository sample.
`samples-manifest.json` binds its fingerprint separately from generated build and
instrumentation files. The Full Demo and historical iOS applications retain
their own exact SDK locks. Legacy API reference pages describe their historical
dependency; use the current
[Android SDK documentation](https://github.com/netumscan/printer-sdk-android)
for this release. Demo sources are MIT; SDK licenses and notices remain embedded
in the APK.
