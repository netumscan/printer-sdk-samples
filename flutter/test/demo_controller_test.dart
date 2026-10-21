import 'dart:async';

import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netum_printer_demo/demo_controller.dart';
import 'package:netum_printer_demo/printer_bridge.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  const channel = MethodChannel('com.netumscan/printer_sdk/controller_test');
  final messenger =
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger;
  late List<MethodCall> calls;
  late PrinterBridge bridge;
  late DemoController controller;
  Completer<Map<String, Object?>>? pendingPrint;
  Completer<Map<String, Object?>>? pendingConnect;
  MethodCall? printCall;
  MethodCall? connectCall;

  setUp(() {
    calls = [];
    pendingPrint = null;
    pendingConnect = null;
    printCall = null;
    connectCall = null;
    bridge = PrinterBridge(channel: channel);
    controller = DemoController(bridge: bridge);
    messenger.setMockMethodCallHandler(channel, (call) async {
      calls.add(call);
      switch (call.method) {
        case 'scanBle':
        case 'bondedDevices':
          return _result(
            call,
            value: [
              {'id': 'AA:BB:CC:DD:EE:FF', 'name': 'LT-M90_TEST', 'rssi': -45},
            ],
          );
        case 'sdkInfo':
          return _result(
            call,
            value: {
              'platform': 'android',
              'transports': ['BLE', 'SPP'],
              'distributionVersion': '0.3.0-preview.10',
            },
          );
        case 'connect':
          connectCall = call;
          return pendingConnect?.future ??
              _result(call, value: {'model': 'LT_M90'});
        case 'printEncodedImage':
          printCall = call;
          return pendingPrint?.future ?? _result(call, completion: 'DATA_SENT');
        default:
          return _result(call);
      }
    });
  });

  tearDown(() {
    controller.dispose();
    messenger.setMockMethodCallHandler(channel, null);
  });

  Future<void> connect() async {
    await controller.initialize();
    await controller.discover();
    controller.selectDevice(controller.devices.single);
    await controller.connect();
    expect(controller.connected, isTrue);
  }

  test(
    'connection and image printing do not implicitly change paper or position it',
    () async {
      await connect();

      await controller.printTestImage(Uint8List.fromList([137, 80, 78, 71]));

      expect(controller.lastResult!.completion, 'DATA_SENT');
      expect(calls.where((call) => call.method == 'setPaperType'), isEmpty);
      expect(calls.where((call) => call.method == 'locateNextLabel'), isEmpty);
      expect(
        calls.where((call) => call.method == 'printEncodedImage'),
        hasLength(1),
      );
      expect(
        calls.where((call) => call.method.startsWith('connect')),
        hasLength(1),
      );
    },
  );

  test(
    'cancel keeps the request busy until the original final callback arrives',
    () async {
      await connect();
      pendingPrint = Completer<Map<String, Object?>>();
      final printing = controller.printTestImage(
        Uint8List.fromList([137, 80, 78, 71]),
      );
      await Future<void>.delayed(Duration.zero);
      expect(controller.busy, isTrue);

      await controller.cancel();

      expect(controller.busy, isTrue);
      expect(calls.last.method, 'cancel');
      expect(
        calls.last.arguments['requestId'],
        printCall!.arguments['requestId'],
      );
      pendingPrint!.complete({
        ..._result(printCall!),
        'outcome': 'CANCELLED',
        'code': 1005,
        'message': 'Cancelled',
        'outputMayHaveOccurred': true,
      });
      await printing;
      expect(controller.busy, isFalse);
      expect(controller.lastResult!.outcome, 'CANCELLED');
      expect(controller.lastResult!.outputMayHaveOccurred, isTrue);
      expect(
        calls.where((call) => call.method == 'printEncodedImage'),
        hasLength(1),
      );
    },
  );

  test(
    'an unexpected disconnect clears connection without reconnecting or replaying',
    () async {
      await connect();
      await controller.printTestImage(Uint8List.fromList([137, 80, 78, 71]));
      final before = calls.length;

      bridge.onDisconnected!(
        PrinterResult.fromMap({
          'requestId': 'connection-1',
          'outcome': 'FAILURE',
          'code': 1003,
          'message': 'Connection lost',
          'completion': 'NONE',
          'outputMayHaveOccurred': true,
        }),
      );
      await Future<void>.delayed(Duration.zero);

      expect(controller.connected, isFalse);
      expect(calls.length, before);
      expect(
        calls.where((call) => call.method.startsWith('connect')),
        hasLength(1),
      );
      expect(
        calls.where((call) => call.method == 'printEncodedImage'),
        hasLength(1),
      );
    },
  );

  test(
    'backgrounding disconnects and needs a later explicit connection',
    () async {
      await connect();

      await controller.onBackground();
      await Future<void>.delayed(Duration.zero);

      expect(controller.connected, isFalse);
      expect(calls.where((call) => call.method == 'disconnect'), hasLength(1));
      expect(
        calls.where((call) => call.method.startsWith('connect')),
        hasLength(1),
      );
      expect(
        calls.where((call) => call.method == 'printEncodedImage'),
        isEmpty,
      );
    },
  );

  test(
    'background cleanup keeps the interrupted print result separate',
    () async {
      await connect();
      pendingPrint = Completer<Map<String, Object?>>();
      final printing = controller.printTestImage(
        Uint8List.fromList([137, 80, 78, 71]),
      );
      await Future<void>.delayed(Duration.zero);

      await controller.onBackground();

      expect(controller.connected, isFalse);
      expect(controller.busy, isTrue);
      expect(calls[calls.length - 2].method, 'cancel');
      expect(calls.last.method, 'disconnect');
      expect(controller.cleanupResult!.isSuccess, isTrue);
      pendingPrint!.complete({
        ..._result(printCall!),
        'outcome': 'FAILURE',
        'code': 1003,
        'message': 'Connection closed during send',
        'outputMayHaveOccurred': true,
      });
      await printing;
      controller.onForeground();
      await Future<void>.delayed(Duration.zero);

      expect(controller.busy, isFalse);
      expect(controller.lastResult!.code, 1003);
      expect(controller.lastResult!.outputMayHaveOccurred, isTrue);
      expect(controller.cleanupResult!.isSuccess, isTrue);
      expect(
        calls.where((call) => call.method.startsWith('connect')),
        hasLength(1),
      );
      expect(
        calls.where((call) => call.method == 'printEncodedImage'),
        hasLength(1),
      );
    },
  );
  test(
    'a late connection result cannot restore a connection after background cleanup',
    () async {
      await controller.initialize();
      await controller.discover();
      controller.selectDevice(controller.devices.single);
      pendingConnect = Completer<Map<String, Object?>>();
      final connecting = controller.connect();
      await Future<void>.delayed(Duration.zero);

      await controller.onBackground();
      controller.onForeground();
      pendingConnect!.complete(
        _result(connectCall!, value: {'model': 'LT_M90'}),
      );
      await connecting;

      expect(controller.connected, isFalse);
      expect(controller.adoptedModel, isNull);
      expect(calls.where((call) => call.method == 'connect'), hasLength(1));
      expect(calls.where((call) => call.method == 'disconnect'), hasLength(1));
      expect(controller.cleanupResult!.isSuccess, isTrue);
    },
  );

  test('invalid page dimensions fail without sending image bytes', () async {
    await connect();
    for (final dimensions in [
      (double.nan, 50.0),
      (double.infinity, 50.0),
      (0.0, 50.0),
      (50.0, -1.0),
    ]) {
      controller.widthMm = dimensions.$1;
      controller.heightMm = dimensions.$2;

      await controller.printTestImage(Uint8List.fromList([137, 80, 78, 71]));

      expect(controller.lastResult!.isSuccess, isFalse);
      expect(controller.lastResult!.code, -1);
      expect(controller.lastResult!.completion, 'NONE');
      expect(controller.lastResult!.outputMayHaveOccurred, isFalse);
    }
    expect(calls.where((call) => call.method == 'printEncodedImage'), isEmpty);
  });

  test(
    'a pending final result after disposal does not notify disposed UI',
    () async {
      var notifications = 0;
      controller.addListener(() => notifications++);
      await connect();
      pendingPrint = Completer<Map<String, Object?>>();
      final printing = controller.printTestImage(
        Uint8List.fromList([137, 80, 78, 71]),
      );
      await Future<void>.delayed(Duration.zero);
      final beforeDisposal = notifications;

      controller.dispose();
      pendingPrint!.complete({
        ..._result(printCall!),
        'outcome': 'CANCELLED',
        'code': 1005,
        'message': 'Closed',
        'outputMayHaveOccurred': true,
      });
      await printing;
      await Future<void>.delayed(Duration.zero);

      expect(notifications, beforeDisposal);
      expect(calls.where((call) => call.method == 'close'), hasLength(1));
    },
  );
}

Map<String, Object?> _result(
  MethodCall call, {
  String completion = 'NONE',
  Object? value,
}) => {
  'requestId': (call.arguments as Map<Object?, Object?>)['requestId'],
  'outcome': 'SUCCESS',
  'code': 0,
  'message': 'Accepted',
  'completion': completion,
  'outputMayHaveOccurred': completion == 'DATA_SENT',
  'value': value,
};
