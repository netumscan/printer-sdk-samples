# Sample and SDK licensing

NetumScan's sample application source code, original sample resources, sample
project files, and sample documentation are available under the [MIT License](LICENSE).
You may modify and reuse these samples in your applications under those terms.

In the development repository, this separate grant covers NetumScan-owned files
under `samples/`, `platform/android/demo/`, and `platform/ios/Demo/`. In the public
[`netumscan/printer-sdk-samples`](https://github.com/netumscan/printer-sdk-samples)
repository, it covers the exported Quick Start and Full Demo applications,
their original resources and documentation, and the original sample build and
CI configuration. These files retain the same MIT grant when exported.

The MIT grant does not cover SDK implementation files, SDK binaries, SDK headers,
SDK wrappers, SDK distribution manifests, or SDK documentation copied from a
distribution. References to SDK APIs and remote package coordinates in a sample
do not relicense the referenced SDK. The samples consume the SDK as a separately
licensed dependency; this repository does not redistribute the SDK artifacts.

Android SDK `com.netumscan:printer-sdk-android:0.2.0-preview.2` and the `PrinterSDK`
and `CPrinterSDK` products from iOS SDK `0.2.0-preview.2` remain proprietary under
the Netum Printer SDK License, Version 1.0. Their license and notices are provided
by the respective [Android](https://github.com/netumscan/printer-sdk-android/blob/v0.2.0-preview.2/LICENSE)
and [iOS](https://github.com/netumscan/printer-sdk-ios/blob/v0.2.0-preview.2/LICENSE) distributions.
This sample license does not modify any SDK license or any already published SDK
package, tag, checksum, or release asset.

Third-party files, generated dependency code, and build tools retain their own
licenses and notices. They are excluded from the NetumScan MIT grant unless
their own license states otherwise. In particular, the Gradle Wrapper is a
Gradle build tool governed by its own Apache-2.0 terms, and Android and Apple
platform SDKs are governed by their respective terms. Do not interpret a root
sample `LICENSE` as replacing those terms.

Printer hardware compatibility and completion guarantees follow the SDK
documentation. The examples do not turn `DATA_SENT` into a claim of physical
print completion, and this preview retains `hardware_verified=false`.
