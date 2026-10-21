import 'dart:async';

import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netum_printer_demo/printer_bridge.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  const channel = MethodChannel('com.netumscan/printer_sdk/test');
  final messenger =
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger;

  tearDown(() {
    messenger.setMockMethodCallHandler(channel, null);
  });

  test(
    'each request has its own identity and preserves the final SDK result',
    () async {
      final calls = <MethodCall>[];
      messenger.setMockMethodCallHandler(channel, (call) async {
        calls.add(call);
        return _result(call, completion: 'DATA_SENT');
      });
      final bridge = PrinterBridge(channel: channel);

      final first = bridge.start('scanBle');
      final second = bridge.start('bondedDevices');
      final results = await Future.wait([first.result, second.result]);

      expect(first.id, isNotEmpty);
      expect(second.id, isNot(first.id));
      expect(calls[0].arguments['requestId'], first.id);
      expect(calls[1].arguments['requestId'], second.id);
      expect(results[0].requestId, first.id);
      expect(results[1].requestId, second.id);
      expect(results[0].completion, 'DATA_SENT');
      expect(results[0].isSuccess, isTrue);
    },
  );

  test(
    'cancel signals the original request and waits for its real final result',
    () async {
      final pendingPrint = Completer<Map<String, Object?>>();
      final calls = <MethodCall>[];
      messenger.setMockMethodCallHandler(channel, (call) async {
        calls.add(call);
        if (call.method == 'printEncodedImage') return pendingPrint.future;
        return null;
      });
      final bridge = PrinterBridge(channel: channel);
      final request = bridge.start('printEncodedImage');
      var completed = false;
      unawaited(request.result.then((_) => completed = true));
      await Future<void>.delayed(Duration.zero);

      await bridge.cancel(request.id);

      expect(calls.last.method, 'cancel');
      expect(calls.last.arguments['requestId'], request.id);
      expect(completed, isFalse);
      pendingPrint.complete({
        'requestId': request.id,
        'outcome': 'CANCELLED',
        'code': 1005,
        'message': 'Cancelled after sending began',
        'completion': 'NONE',
        'outputMayHaveOccurred': true,
      });
      final result = await request.result;
      expect(result.outcome, 'CANCELLED');
      expect(result.outputMayHaveOccurred, isTrue);
      expect(result.requestId, request.id);
      expect(
        calls.where((call) => call.method == 'printEncodedImage'),
        hasLength(1),
      );
    },
  );

  test(
    'encoded bytes stay typed and page dimensions remain millimetres',
    () async {
      MethodCall? received;
      messenger.setMockMethodCallHandler(channel, (call) async {
        received = call;
        return _result(call);
      });
      final bridge = PrinterBridge(channel: channel);
      final bytes = Uint8List.fromList([137, 80, 78, 71, 255, 0]);

      await bridge.start('printEncodedImage', {
        'imageBytes': bytes,
        'widthMm': 48.5,
        'heightMm': 12.25,
      }).result;

      final arguments = received!.arguments as Map<Object?, Object?>;
      expect(arguments['imageBytes'], isA<Uint8List>());
      expect(arguments['imageBytes'], orderedEquals(bytes));
      expect(arguments['widthMm'], 48.5);
      expect(arguments['heightMm'], 12.25);
    },
  );

  test(
    'platform failures become a diagnostic result associated with the request',
    () async {
      messenger.setMockMethodCallHandler(channel, (call) async {
        throw PlatformException(
          code: 'BLUETOOTH_PERMISSION_DENIED',
          message: 'Permission denied',
        );
      });
      final request = PrinterBridge(channel: channel).start('scanBle');

      final result = await request.result;

      expect(result.isSuccess, isFalse);
      expect(result.requestId, request.id);
      expect(result.completion, 'NONE');
      expect(result.outputMayHaveOccurred, isFalse);
      expect(result.message, contains('Permission denied'));
    },
  );

  test('an unavailable native plugin becomes a failed result', () async {
    final request = PrinterBridge(channel: channel).start('initialize');

    final result = await request.result;

    expect(result.isSuccess, isFalse);
    expect(result.requestId, request.id);
    expect(result.completion, 'NONE');
    expect(result.outputMayHaveOccurred, isFalse);
  });

  test('a result with another request identity is rejected', () async {
    messenger.setMockMethodCallHandler(
      channel,
      (call) async => {..._result(call), 'requestId': 'another-engine-request'},
    );
    final request = PrinterBridge(channel: channel).start('scanBle');

    final result = await request.result;

    expect(result.isSuccess, isFalse);
    expect(result.requestId, request.id);
    expect(result.completion, 'NONE');
    expect(result.outputMayHaveOccurred, isFalse);
  });

  test('release closes once and prevents later native requests', () async {
    final calls = <MethodCall>[];
    messenger.setMockMethodCallHandler(channel, (call) async {
      calls.add(call);
      return _result(call);
    });
    final bridge = PrinterBridge(channel: channel);

    await bridge.release();
    await bridge.release();
    final result = await bridge.start('scanBle').result;

    expect(calls.map((call) => call.method), ['close']);
    expect(result.isSuccess, isFalse);
    expect(result.completion, 'NONE');
    expect(result.outputMayHaveOccurred, isFalse);
  });

  test(
    'DATA_SENT is described as transfer completion without claiming paper completion',
    () {
      final result = PrinterResult.fromMap({
        'requestId': 'request-1',
        'outcome': 'SUCCESS',
        'code': 0,
        'message': 'Bytes sent',
        'completion': 'DATA_SENT',
        'outputMayHaveOccurred': true,
      });

      expect(result.completion, 'DATA_SENT');
      expect(result.summary, contains('DATA_SENT'));
      expect(result.summary, isNot(contains('打印已完成')));
    },
  );

  test(
    'unsupported status preserves its SDK diagnostics and unknown values',
    () {
      final result = PrinterResult.fromMap({
        'requestId': 'status-1',
        'outcome': 'FAILURE',
        'code': 1004,
        'message': 'Status query is not supported',
        'completion': 'NONE',
        'outputMayHaveOccurred': false,
        'value': null,
      });

      expect(result.isSuccess, isFalse);
      expect(result.code, 1004);
      expect(result.message, 'Status query is not supported');
      expect(result.value, isNull);
      expect(result.completion, 'NONE');
      expect(result.outputMayHaveOccurred, isFalse);
    },
  );
}

Map<String, Object?> _result(MethodCall call, {String completion = 'NONE'}) => {
  'requestId': (call.arguments as Map<Object?, Object?>)['requestId'],
  'outcome': 'SUCCESS',
  'code': 0,
  'message': 'Accepted',
  'completion': completion,
  'outputMayHaveOccurred': completion == 'DATA_SENT',
};
