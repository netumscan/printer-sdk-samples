import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netum_printer_demo/main.dart';

void main() {
  const channel = MethodChannel('com.netumscan/printer_sdk');

  testWidgets('quick start remains usable on a narrow phone', (tester) async {
    tester.view.physicalSize = const Size(320, 568);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);
    final messenger = tester.binding.defaultBinaryMessenger;
    messenger.setMockMethodCallHandler(
      channel,
      (call) async => {
        'requestId': (call.arguments as Map<Object?, Object?>)['requestId'],
        'outcome': 'SUCCESS',
        'code': 0,
        'message': 'Ready',
        'completion': 'NONE',
        'outputMayHaveOccurred': false,
        'value': <String, Object?>{
          'platform': 'android',
          'transports': ['BLE', 'SPP'],
          'distributionVersion': '0.3.0-preview.10',
          'nativeVersion': '0.3.0',
        },
      },
    );
    addTearDown(() => messenger.setMockMethodCallHandler(channel, null));

    await tester.pumpWidget(const DemoApp());
    await tester.pumpAndSettle();

    expect(find.byType(Scaffold), findsOneWidget);
    expect(tester.takeException(), isNull);
    final scrollable = find.byType(Scrollable).first;
    await tester.drag(scrollable, const Offset(0, -450));
    await tester.pumpAndSettle();
    expect(tester.takeException(), isNull);
    await tester.pumpWidget(const SizedBox.shrink());
    await tester.pumpAndSettle();
  });
}
