// SPDX-License-Identifier: MIT
package com.netumscan.netum_printer_demo

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.pm.PackageManager
import android.os.Build
import com.netumscan.printersdk.ConnectionInfo
import com.netumscan.printersdk.DiscoveredDevice
import com.netumscan.printersdk.ManagerResult
import com.netumscan.printersdk.PaperType
import com.netumscan.printersdk.PrinterManager
import com.netumscan.printersdk.PrinterModel
import com.netumscan.printersdk.RequestId
import com.netumscan.printersdk.SdkInfo
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.PluginRegistry

/** Sample adapter only. Discovery, preparation and printer IO remain in PrinterManager. */
class PrinterSdkPlugin : FlutterPlugin, MethodChannel.MethodCallHandler, ActivityAware,
    PluginRegistry.RequestPermissionsResultListener {
    private class Pending(val id: String, val result: MethodChannel.Result, val generation: Long) {
        var nativeId: RequestId? = null
    }

    private class PermissionRequest(val pending: Pending, val action: () -> Unit)

    private var channel: MethodChannel? = null
    private var manager: PrinterManager? = null
    private var activityBinding: ActivityPluginBinding? = null
    private var generation = 0L
    private val pending = mutableMapOf<String, Pending>()
    private var permissionRequest: PermissionRequest? = null

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        generation++
        manager = PrinterManager(binding.applicationContext)
        channel = MethodChannel(binding.binaryMessenger, "com.netumscan/printer_sdk").also {
            it.setMethodCallHandler(this)
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        // Invalidate delivery before SDK cancellation can schedule any terminal callbacks.
        channel?.setMethodCallHandler(null)
        channel = null
        generation++
        activityBinding?.removeRequestPermissionsResultListener(this)
        activityBinding = null
        permissionRequest = null
        val owner = manager
        manager = null
        val unfinished = pending.values.toList()
        pending.clear()
        unfinished.forEach { owner?.cancel(it.nativeId) }
        owner?.close { /* The detached engine no longer has a reply destination. */ }
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        val owner = manager
        if (owner == null || channel == null) {
            result.error("sdk_unavailable", "Flutter engine is detached", null)
            return
        }
        if (call.method !in METHODS) {
            result.notImplemented()
            return
        }
        val arguments = call.arguments as? Map<*, *>
        if (arguments == null) {
            result.error("invalid_argument", "A request argument map is required", null)
            return
        }
        val id = arguments["requestId"] as? String
        if (id.isNullOrBlank() || id.length > 128) {
            result.error("invalid_argument", "A nonempty requestId (at most 128 characters) is required", null)
            return
        }
        if (call.method == "cancel") {
            val target = pending[id]
            if (target?.nativeId != null) owner.cancel(target.nativeId)
            else if (target != null) finish(target, terminal(id, "CANCELLED", 1005,
                "Cancelled before SDK submission", "NONE", false, null))
            // Cancelling is a signal. The original method retains its one terminal result.
            result.success(null)
            return
        }
        if (pending.containsKey(id)) {
            result.error("duplicate_request", "requestId is already pending", mapOf("requestId" to id))
            return
        }
        val request = Pending(id, result, generation)
        pending[id] = request
        try {
            when (call.method) {
                "sdkInfo" -> submit<SdkInfo>(request, { owner.sdkInfo(it) }) {
                    mapOf("distributionVersion" to it.distributionVersion)
                }
                "scanBle" -> withPermissions(request, true) {
                    submit<List<DiscoveredDevice>>(request, { owner.scanBle(it) }) { devices ->
                        devices.map(::deviceValue)
                    }
                }
                "bondedDevices" -> withPermissions(request, false) {
                    submit<List<DiscoveredDevice>>(request, { owner.bondedDevices(it) }) { devices ->
                        devices.map(::deviceValue)
                    }
                }
                "connect" -> {
                    val device = arguments["device"] as? Map<*, *>
                        ?: throw IllegalArgumentException("device is required")
                    val address = device["id"] as? String
                    require(address != null && BluetoothAdapter.checkBluetoothAddress(address)) {
                        "device.id must be a Bluetooth address"
                    }
                    val name = device["name"]
                    require(name == null || name is String) { "device.name must be a string or null" }
                    val discovered = DiscoveredDevice(address, name as? String)
                    val transport = arguments["transport"]
                    require(transport == "BLE" || transport == "SPP") { "transport must be BLE or SPP" }
                    val modelValue = arguments["model"]
                    require(modelValue == null || modelValue == "LT_M90") { "Unsupported printer model" }
                    val model = if (modelValue == null) null else PrinterModel.LT_M90
                    val eventChannel = channel
                    val eventGeneration = generation
                    val disconnected = PrinterManager.DisconnectedListener { response ->
                        if (eventChannel != null && channel === eventChannel && generation == eventGeneration) {
                            eventChannel.invokeMethod("disconnected", envelope(id, response, null))
                        }
                    }
                    withPermissions(request, false) {
                        submit<ConnectionInfo>(request, { callback ->
                            if (transport == "BLE") {
                                if (model == null) owner.connectBle(discovered, callback, disconnected)
                                else owner.connectBle(discovered, model, callback, disconnected)
                            } else {
                                if (model == null) owner.connectSpp(discovered, callback, disconnected)
                                else owner.connectSpp(discovered, model, callback, disconnected)
                            }
                        }) { info -> mapOf("device" to deviceValue(info.device), "model" to info.model?.name) }
                    }
                }
                "setPaperType" -> {
                    val value = arguments["paperType"] as? String
                        ?: throw IllegalArgumentException("paperType is required")
                    val paper = PaperType.valueOf(value)
                    submit<Void>(request, { owner.setPaperType(paper, it) }) { null }
                }
                "printEncodedImage" -> {
                    val bytes = arguments["imageBytes"] as? ByteArray
                        ?: throw IllegalArgumentException("imageBytes must be PNG/JPEG Uint8List bytes")
                    require(bytes.isNotEmpty()) { "imageBytes cannot be empty" }
                    val width = dimension(arguments["widthMm"], "widthMm")
                    val height = dimension(arguments["heightMm"], "heightMm")
                    submit<Void>(request, { owner.printEncodedImage(bytes, width, height, it) }) { null }
                }
                "locateNextLabel" -> submit<Void>(request, { owner.locateNextLabel(it) }) { null }
                "disconnect" -> {
                    cancelPermissionWait("Cancelled by disconnect")
                    submit<Void>(request, { owner.disconnect(it) }) { null }
                }
                "close" -> {
                    cancelPermissionWait("Cancelled by close")
                    submit<Void>(request, { owner.close(it) }) { null }
                }
            }
        } catch (error: IllegalArgumentException) {
            finish(request, terminal(id, "FAILURE", 1001, error.message ?: "Invalid argument", "NONE", false, null))
        } catch (_: SecurityException) {
            failChannel(request, "permission_denied", "Bluetooth permission is unavailable; grant it and retry manually")
        } catch (_: RuntimeException) {
            finish(request, terminal(id, "FAILURE", 1000, "Native bridge failed to submit the request", "NONE", false, null))
        }
    }

    private fun <T> submit(request: Pending, start: (PrinterManager.Callback<T>) -> RequestId,
                           value: (T) -> Any?) {
        if (!isPending(request)) return
        request.nativeId = start(PrinterManager.Callback { response ->
            finish(request, envelope(request.id, response, response.value?.let(value)))
        })
    }

    private fun withPermissions(request: Pending, scanning: Boolean, action: () -> Unit) {
        val activity = activityBinding?.activity
        if (activity == null) {
            failChannel(request, "activity_unavailable", "An attached Android Activity is required")
            return
        }
        val permissions = requiredPermissions(scanning)
        val missing = permissions.filter { activity.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isEmpty()) { action(); return }
        if (permissionRequest != null) {
            failChannel(request, "permission_request_pending", "A Bluetooth permission request is already pending")
            return
        }
        permissionRequest = PermissionRequest(request, action)
        try {
            activity.requestPermissions(missing.toTypedArray(), PERMISSION_CODE)
        } catch (_: RuntimeException) {
            permissionRequest = null
            failChannel(request, "permission_denied", "Unable to request Bluetooth permissions")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
                                             grantResults: IntArray): Boolean {
        if (requestCode != PERMISSION_CODE) return false
        val waiting = permissionRequest ?: return false
        permissionRequest = null
        if (!isPending(waiting.pending)) return true
        if (permissions.isEmpty() || permissions.size != grantResults.size ||
            grantResults.any { it != PackageManager.PERMISSION_GRANTED }) {
            failChannel(waiting.pending, "permission_denied",
                "Bluetooth permission was denied. Grant permission and retry manually; Android 6–11 BLE scanning also requires system location.")
        } else {
            try { waiting.action() }
            catch (_: SecurityException) { failChannel(waiting.pending, "permission_denied", "Bluetooth permission is unavailable") }
            catch (_: RuntimeException) { failChannel(waiting.pending, "native_error", "Native bridge failed to submit the request") }
        }
        return true
    }

    private fun cancelPermissionWait(message: String) {
        permissionRequest?.pending?.let { request ->
            finish(request, terminal(request.id, "CANCELLED", 1005, message, "NONE", false, null))
        }
        // Keep the ticket until the OS permission dialog returns; never open overlapping dialogs.
    }

    private fun isPending(request: Pending) = channel != null && generation == request.generation &&
        pending[request.id] === request

    private fun finish(request: Pending, value: Map<String, Any?>) {
        if (!isPending(request)) return
        pending.remove(request.id)
        request.result.success(value)
    }

    private fun failChannel(request: Pending, code: String, message: String) {
        if (!isPending(request)) return
        pending.remove(request.id)
        request.result.error(code, message, mapOf("requestId" to request.id))
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activityBinding = binding
        binding.addRequestPermissionsResultListener(this)
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) = onAttachedToActivity(binding)
    override fun onDetachedFromActivityForConfigChanges() = detachActivity()
    override fun onDetachedFromActivity() = detachActivity()

    private fun detachActivity() {
        activityBinding?.removeRequestPermissionsResultListener(this)
        activityBinding = null
        permissionRequest?.pending?.let {
            failChannel(it, "activity_unavailable", "Activity detached while requesting Bluetooth permissions")
        }
        permissionRequest = null
    }

    private companion object {
        const val PERMISSION_CODE = 4972
        val METHODS = setOf("sdkInfo", "scanBle", "bondedDevices", "connect", "setPaperType",
            "printEncodedImage", "locateNextLabel", "disconnect", "close", "cancel")

        fun requiredPermissions(scanning: Boolean): List<String> = when {
            Build.VERSION.SDK_INT >= 31 -> if (scanning)
                listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
                else listOf(Manifest.permission.BLUETOOTH_CONNECT)
            scanning && Build.VERSION.SDK_INT >= 23 -> listOf(Manifest.permission.ACCESS_FINE_LOCATION)
            else -> emptyList()
        }

        fun dimension(value: Any?, name: String): Double {
            val number = (value as? Number)?.toDouble()
            require(number != null && number.isFinite() && number > 0) { "$name must be positive and finite" }
            return number
        }

        fun deviceValue(device: DiscoveredDevice): Map<String, Any?> = mapOf(
            "id" to device.address, "address" to device.address, "name" to device.name, "rssi" to device.rssi)

        fun envelope(id: String, response: ManagerResult<*>, value: Any?): Map<String, Any?> = terminal(
            id, response.outcome.name, response.code, response.message, response.completion.name,
            response.outputMayHaveOccurred, value)

        fun terminal(id: String, outcome: String, code: Int, message: String, completion: String,
                     output: Boolean, value: Any?): Map<String, Any?> = mapOf(
            "requestId" to id, "outcome" to outcome, "code" to code, "message" to message,
            "completion" to completion, "outputMayHaveOccurred" to output, "value" to value)
    }
}
