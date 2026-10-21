import 'package:flutter/services.dart';

/// Application tokens stay in Dart; SDK request handles stay in the engine.
class PrinterRequest {
  const PrinterRequest(this.id, this.result);
  final String id;
  final Future<PrinterResult> result;
}

class PrinterResult {
  const PrinterResult({
    required this.requestId,
    required this.outcome,
    required this.code,
    required this.message,
    required this.completion,
    required this.outputMayHaveOccurred,
    this.value,
  });

  factory PrinterResult.fromMap(Map<Object?, Object?> map) => PrinterResult(
    requestId: map['requestId'] as String,
    outcome: map['outcome'] as String,
    code: map['code'] as int,
    message: map['message'] as String,
    completion: map['completion'] as String,
    outputMayHaveOccurred: map['outputMayHaveOccurred'] as bool,
    value: map['value'],
  );

  final String requestId;
  final String outcome;
  final int code;
  final String message;
  final String completion;
  final bool outputMayHaveOccurred;
  final Object? value;
  bool get isSuccess => outcome == 'SUCCESS' && code == 0;
  String get summary {
    if (completion == 'DATA_SENT' && isSuccess) {
      return 'DATA_SENT · 数据已发送，尚未确认纸面打印完成';
    }
    if (completion == 'DEVICE_CONFIRMED' && isSuccess) {
      return 'DEVICE_CONFIRMED · 设备已确认完成';
    }
    if (outcome == 'CANCELLED') {
      return outputMayHaveOccurred ? '已取消，可能已有部分输出' : '已取消';
    }
    if (!isSuccess) {
      return '$message（$code）${outputMayHaveOccurred ? '；可能已有部分输出' : ''}';
    }
    return message.isEmpty ? '操作成功' : message;
  }
}

class PrinterBridge {
  PrinterBridge({
    MethodChannel channel = const MethodChannel('com.netumscan/printer_sdk'),
  }) : _channel = channel {
    _channel.setMethodCallHandler((call) async {
      if (call.method == 'disconnected' && !_released) {
        onDisconnected?.call(
          PrinterResult.fromMap(call.arguments as Map<Object?, Object?>),
        );
      }
    });
  }
  final MethodChannel _channel;
  int _sequence = 0;
  bool _released = false;
  void Function(PrinterResult)? onDisconnected;

  PrinterRequest start(
    String method, [
    Map<String, Object?> arguments = const {},
  ]) {
    final id = 'dart-${++_sequence}';
    return PrinterRequest(id, _invoke(method, id, arguments));
  }

  Future<PrinterResult> _invoke(
    String method,
    String id,
    Map<String, Object?> arguments,
  ) async {
    try {
      if (_released) throw StateError('PrinterBridge is closed');
      final map = await _channel.invokeMapMethod<Object?, Object?>(method, {
        ...arguments,
        'requestId': id,
      });
      if (map == null || map['requestId'] != id) {
        throw const FormatException('Invalid request identity');
      }
      return PrinterResult.fromMap(map);
    } catch (error) {
      // -1 is an adapter failure, never a fabricated SDK error code.
      final message = switch (error) {
        MissingPluginException() => '请在 Android 或 iOS 上运行此 demo',
        PlatformException() => '${error.code}: ${error.message ?? ''}',
        _ => '$error',
      };
      return PrinterResult(
        requestId: id,
        outcome: 'FAILURE',
        code: -1,
        message: message,
        completion: 'NONE',
        outputMayHaveOccurred: false,
      );
    }
  }

  /// This acknowledges the signal only. Await the original request's result.
  Future<void> cancel(String requestId) =>
      _channel.invokeMethod<void>('cancel', {'requestId': requestId});

  Future<void> release() async {
    if (_released) return;
    onDisconnected = null;
    _channel.setMethodCallHandler(null);
    final closing = start('close');
    _released = true;
    await closing.result;
  }
}
