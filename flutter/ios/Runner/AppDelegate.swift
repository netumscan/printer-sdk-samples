import Flutter
import UIKit

@main
@objc class AppDelegate: FlutterAppDelegate, FlutterImplicitEngineDelegate {
  override func application(
    _ application: UIApplication,
    didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
  ) -> Bool {
    return super.application(application, didFinishLaunchingWithOptions: launchOptions)
  }

  nonisolated func didInitializeImplicitFlutterEngine(_ engineBridge: FlutterImplicitEngineBridge) {
    MainActor.assumeIsolated {
      GeneratedPluginRegistrant.register(with: engineBridge.pluginRegistry)
      if let registrar = engineBridge.pluginRegistry.registrar(forPlugin: "PrinterSdkBridge") {
        PrinterSdkBridge.register(with: registrar)
      }
    }
  }
}
