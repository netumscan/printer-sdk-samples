package com.netumscan.netum_printer_demo

import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine

class MainActivity : FlutterActivity() {
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        // The engine owns the manager, including across Activity recreation.
        if (!flutterEngine.plugins.has(PrinterSdkPlugin::class.java)) {
            flutterEngine.plugins.add(PrinterSdkPlugin())
        }
    }
}
