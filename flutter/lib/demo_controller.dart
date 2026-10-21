import 'package:flutter/foundation.dart';
import 'printer_bridge.dart';

class PrinterDevice {
  PrinterDevice.fromMap(Map<Object?, Object?> value)
    : id = value['id'] as String,
      name = value['name'] as String?,
      rssi = value['rssi'] as int?;
  final String id;
  final String? name;
  final int? rssi;
  Map<String, Object?> toMap() => {'id': id, 'name': name};
}

/// App state only: the native PrinterManager schedules all printer IO.
class DemoController extends ChangeNotifier {
  DemoController({PrinterBridge? bridge})
    : _bridge = bridge ?? PrinterBridge() {
    _bridge.onDisconnected = (result) {
      _sessionEpoch++;
      connected = false;
      adoptedModel = null;
      _record('连接中断', result);
    };
  }
  final PrinterBridge _bridge;
  PrinterRequest? _active;
  bool _cleaning = false;
  bool _disposed = false;
  bool _foreground = true;
  int _sessionEpoch = 0;
  bool connected = false;
  List<PrinterDevice> devices = [];
  PrinterDevice? selectedDevice;
  String? model;
  String? adoptedModel;
  String transport = 'BLE';
  String paperType = 'GAP_LABEL';
  double widthMm = 50;
  double heightMm = 50;
  String? sdkVersion;
  PrinterResult? lastResult;
  PrinterResult? cleanupResult;
  final List<String> logs = [];
  bool get busy => _active != null || _cleaning;
  bool get canPrint =>
      connected && adoptedModel != null && !busy && _foreground;
  void _notify() {
    if (!_disposed) notifyListeners();
  }

  void _record(String label, PrinterResult result, {bool cleanup = false}) {
    if (_disposed) return;
    if (cleanup) {
      cleanupResult = result;
    } else {
      lastResult = result;
    }
    logs.insert(
      0,
      '$label · ${result.requestId}\n${result.outcome} / ${result.code} / ${result.completion}\n${result.message}\noutputMayHaveOccurred=${result.outputMayHaveOccurred}',
    );
    if (logs.length > 30) logs.removeLast();
    _notify();
  }

  Future<PrinterResult?> _run(
    String method,
    String label, [
    Map<String, Object?> args = const {},
  ]) async {
    if (_disposed || busy || !_foreground) return null;
    final request = _bridge.start(method, args);
    _active = request;
    _notify();
    final result = await request.result;
    if (_active == request) _active = null;
    _record(label, result);
    return result;
  }

  Future<void> initialize() async {
    final result = await _run('sdkInfo', 'SDK 信息');
    if (result?.isSuccess == true && !_disposed) {
      sdkVersion =
          (result!.value as Map<Object?, Object?>)['distributionVersion']
              as String?;
      _notify();
    }
  }

  Future<void> discover() async {
    if (connected || busy) return;
    devices = [];
    selectedDevice = null;
    final result = await _run(
      transport == 'SPP' ? 'bondedDevices' : 'scanBle',
      transport == 'SPP' ? '已配对设备' : 'BLE 扫描',
    );
    if (result?.isSuccess == true && !_disposed) {
      devices = (result!.value as List<Object?>)
          .map((item) => PrinterDevice.fromMap(item as Map<Object?, Object?>))
          .toList();
      _notify();
    }
  }

  void selectDevice(PrinterDevice device) {
    if (connected || busy) return;
    selectedDevice = device;
    _notify();
  }

  void selectTransport(String value) {
    if (connected || busy) return;
    transport = value;
    devices = [];
    selectedDevice = null;
    _notify();
  }

  Future<void> connect() async {
    final device = selectedDevice;
    if (device == null || connected) return;
    final epoch = _sessionEpoch;
    final result = await _run('connect', '连接', {
      'device': device.toMap(),
      'transport': transport,
      'model': model,
    });
    if (result?.isSuccess == true &&
        !_disposed &&
        _foreground &&
        !_cleaning &&
        epoch == _sessionEpoch) {
      connected = true;
      adoptedModel =
          (result!.value as Map<Object?, Object?>)['model'] as String?;
      _notify();
    }
  }

  Future<void> printTestImage(Uint8List bytes) async {
    if (!canPrint) return;
    if (!widthMm.isFinite ||
        !heightMm.isFinite ||
        widthMm <= 0 ||
        heightMm <= 0) {
      _record(
        '页面尺寸',
        const PrinterResult(
          requestId: 'validation',
          outcome: 'FAILURE',
          code: -1,
          message: '页面宽高必须是有限的正数（mm）',
          completion: 'NONE',
          outputMayHaveOccurred: false,
        ),
      );
      return;
    }
    await _run('printEncodedImage', '测试图打印', {
      'imageBytes': bytes,
      'widthMm': widthMm,
      'heightMm': heightMm,
    });
  }

  Future<void> setPaperType() async {
    if (connected) await _run('setPaperType', '设置纸型', {'paperType': paperType});
  }

  Future<void> locateNextLabel() async {
    if (connected) await _run('locateNextLabel', '定位下一张');
  }

  Future<void> cancel() async {
    final request = _active;
    if (request == null) return;
    try {
      await _bridge.cancel(request.id);
    } catch (error) {
      if (!_disposed) {
        logs.insert(0, '取消信号未送达：$error');
        _notify();
      }
    }
  }

  Future<void> disconnect() async {
    if (_disposed || _cleaning) return;
    _sessionEpoch++;
    _cleaning = true;
    connected = false;
    adoptedModel = null;
    _notify();
    // Cleanup is a control operation, allowed while a print is pending.
    final result = await _bridge.start('disconnect').result;
    _cleaning = false;
    _record('断开', result, cleanup: true);
  }

  Future<void> onBackground() async {
    _sessionEpoch++;
    _foreground = false;
    await cancel();
    await disconnect();
  }

  void onForeground() {
    _foreground = true;
    _notify();
  }

  @override
  void dispose() {
    if (_disposed) return;
    _disposed = true;
    _bridge.onDisconnected = null;
    // Engine detach also closes the manager if Dart has already stopped.
    _bridge.release();
    super.dispose();
  }
}
