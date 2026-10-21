import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';

import 'demo_controller.dart';
import 'test_label.dart';

void main() => runApp(const DemoApp());

class DemoApp extends StatelessWidget {
  const DemoApp({super.key, this.controller});
  final DemoController? controller;

  @override
  Widget build(BuildContext context) => MaterialApp(
    title: 'NETUM Printer',
    debugShowCheckedModeBanner: false,
    theme: ThemeData(
      useMaterial3: true,
      colorScheme: ColorScheme.fromSeed(
        seedColor: const Color(0xff166b50),
        primary: const Color(0xff166b50),
        surface: const Color(0xfff7f8f5),
      ),
      scaffoldBackgroundColor: const Color(0xfff7f8f5),
      inputDecorationTheme: const InputDecorationTheme(
        border: OutlineInputBorder(),
        isDense: true,
      ),
      filledButtonTheme: FilledButtonThemeData(
        style: FilledButton.styleFrom(minimumSize: const Size(48, 48)),
      ),
    ),
    home: DemoPage(controller: controller),
  );
}

class DemoPage extends StatefulWidget {
  const DemoPage({super.key, this.controller});
  final DemoController? controller;
  @override
  State<DemoPage> createState() => _DemoPageState();
}

class _DemoPageState extends State<DemoPage> with WidgetsBindingObserver {
  late final DemoController model;
  Uint8List? _label;
  String? _imageError;

  @override
  void initState() {
    super.initState();
    model = widget.controller ?? DemoController();
    WidgetsBinding.instance.addObserver(this);
    unawaited(model.initialize());
    unawaited(_prepareLabel());
  }

  Future<void> _prepareLabel() async {
    try {
      final bytes = await createTestLabel();
      if (mounted) setState(() => _label = bytes);
    } catch (error) {
      if (mounted) setState(() => _imageError = '$error');
    }
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.paused ||
        state == AppLifecycleState.detached) {
      unawaited(model.onBackground());
    } else if (state == AppLifecycleState.resumed) {
      model.onForeground();
    }
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    if (widget.controller == null) model.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => ListenableBuilder(
    listenable: model,
    builder: (context, _) => Scaffold(
      appBar: AppBar(
        title: const Text(
          'NETUM',
          style: TextStyle(fontWeight: FontWeight.w800, letterSpacing: 2),
        ),
        actions: [
          Padding(
            padding: const EdgeInsets.only(right: 20),
            child: Row(
              children: [
                Icon(
                  Icons.circle,
                  size: 8,
                  color: model.connected
                      ? const Color(0xff166b50)
                      : Colors.grey,
                ),
                const SizedBox(width: 8),
                Text(model.connected ? '已连接' : '未连接'),
              ],
            ),
          ),
        ],
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.fromLTRB(24, 12, 24, 36),
          child: Center(
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 760),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const Text(
                    '打印工作台',
                    style: TextStyle(fontSize: 30, fontWeight: FontWeight.w700),
                  ),
                  const SizedBox(height: 6),
                  Text(
                    'Flutter Quick Start · SDK ${model.sdkVersion ?? '读取中'}',
                    style: TextStyle(color: Colors.grey.shade700),
                  ),
                  const SizedBox(height: 28),
                  _heading('01', '选择打印机'),
                  const SizedBox(height: 16),
                  if (!kIsWeb &&
                      defaultTargetPlatform == TargetPlatform.android) ...[
                    SegmentedButton<String>(
                      segments: const [
                        ButtonSegment(value: 'BLE', label: Text('BLE')),
                        ButtonSegment(value: 'SPP', label: Text('经典蓝牙 SPP')),
                      ],
                      selected: {model.transport},
                      onSelectionChanged: model.busy || model.connected
                          ? null
                          : (values) => model.selectTransport(values.single),
                    ),
                    const SizedBox(height: 16),
                  ],
                  DropdownButtonFormField<String>(
                    initialValue: model.model ?? 'AUTO',
                    isExpanded: true,
                    decoration: const InputDecoration(labelText: '打印机型号'),
                    items: const [
                      DropdownMenuItem(value: 'AUTO', child: Text('按设备名称推荐')),
                      DropdownMenuItem(
                        value: 'LT_M90',
                        child: Text('LT-M90（已确认实物型号）'),
                      ),
                    ],
                    onChanged: model.busy || model.connected
                        ? null
                        : (value) => setState(
                            () => model.model = value == 'AUTO' ? null : value,
                          ),
                  ),
                  const SizedBox(height: 12),
                  Text(
                    '改名设备请确认实物后选择型号。未知型号不会自动采用 LT-M90。',
                    style: TextStyle(fontSize: 12, color: Colors.grey.shade700),
                  ),
                  const SizedBox(height: 16),
                  OutlinedButton.icon(
                    onPressed: model.busy || model.connected
                        ? null
                        : model.discover,
                    icon: const Icon(Icons.bluetooth_searching),
                    label: Text(
                      model.transport == 'SPP' ? '读取已配对设备' : '扫描附近 BLE 设备',
                    ),
                  ),
                  if (model.devices.isEmpty)
                    Padding(
                      padding: const EdgeInsets.symmetric(vertical: 18),
                      child: Text(
                        model.busy && !model.connected
                            ? '正在处理，请稍候…'
                            : '打开打印机，扫描后点选设备。',
                        style: const TextStyle(color: Colors.grey),
                      ),
                    ),
                  for (final device in model.devices)
                    ListTile(
                      contentPadding: EdgeInsets.zero,
                      selected: model.selectedDevice?.id == device.id,
                      leading: Icon(
                        model.selectedDevice?.id == device.id
                            ? Icons.radio_button_checked
                            : Icons.radio_button_off,
                      ),
                      title: Text(device.name ?? '未命名设备'),
                      subtitle: Text(
                        device.id,
                        style: const TextStyle(fontSize: 11),
                      ),
                      trailing: device.rssi == null
                          ? null
                          : Text('${device.rssi} dBm'),
                      onTap: model.busy || model.connected
                          ? null
                          : () => model.selectDevice(device),
                    ),
                  if (model.connected)
                    OutlinedButton.icon(
                      onPressed: model.disconnect,
                      icon: const Icon(Icons.link_off),
                      label: const Text('断开连接'),
                    )
                  else
                    FilledButton.icon(
                      onPressed: model.busy || model.selectedDevice == null
                          ? null
                          : model.connect,
                      icon: const Icon(Icons.link),
                      label: const Text('连接所选打印机'),
                    ),
                  const SizedBox(height: 28),
                  const Divider(),
                  const SizedBox(height: 20),
                  _heading('02', '准备标签'),
                  const SizedBox(height: 20),
                  Container(
                    color: const Color(0xffe8ece5),
                    padding: const EdgeInsets.all(28),
                    child: Center(
                      child: AnimatedSwitcher(
                        duration: const Duration(milliseconds: 220),
                        child: _label == null
                            ? SizedBox(
                                height: 210,
                                child: Center(
                                  child: Text(_imageError ?? '生成测试图…'),
                                ),
                              )
                            : Image.memory(
                                _label!,
                                width: 230,
                                height: 230,
                                gaplessPlayback: true,
                                semanticLabel: 'NETUM 方形测试标签预览',
                              ),
                      ),
                    ),
                  ),
                  const SizedBox(height: 16),
                  Row(
                    children: [
                      Expanded(
                        child: TextFormField(
                          initialValue: '50',
                          decoration: const InputDecoration(
                            labelText: '页面宽度',
                            suffixText: 'mm',
                          ),
                          keyboardType: const TextInputType.numberWithOptions(
                            decimal: true,
                          ),
                          enabled: !model.busy,
                          onChanged: (value) => model.widthMm =
                              double.tryParse(value) ?? double.nan,
                        ),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: TextFormField(
                          initialValue: '50',
                          decoration: const InputDecoration(
                            labelText: '页面高度',
                            suffixText: 'mm',
                          ),
                          keyboardType: const TextInputType.numberWithOptions(
                            decimal: true,
                          ),
                          enabled: !model.busy,
                          onChanged: (value) => model.heightMm =
                              double.tryParse(value) ?? double.nan,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),
                  DropdownButtonFormField<String>(
                    initialValue: model.paperType,
                    decoration: const InputDecoration(labelText: '实际装纸类型'),
                    items: const [
                      DropdownMenuItem(
                        value: 'GAP_LABEL',
                        child: Text('间隙标签纸'),
                      ),
                      DropdownMenuItem(value: 'BLACK_MARK', child: Text('黑标纸')),
                      DropdownMenuItem(value: 'CONTINUOUS', child: Text('连续纸')),
                    ],
                    onChanged: model.busy
                        ? null
                        : (value) => setState(() => model.paperType = value!),
                  ),
                  const SizedBox(height: 8),
                  Wrap(
                    spacing: 8,
                    children: [
                      TextButton(
                        onPressed: model.canPrint ? model.setPaperType : null,
                        child: const Text('设置纸型'),
                      ),
                      TextButton(
                        onPressed: model.canPrint
                            ? model.locateNextLabel
                            : null,
                        child: const Text('定位下一张'),
                      ),
                    ],
                  ),
                  const Text(
                    '设置纸型会发送独立指令；打印不会自动设纸型或定位。',
                    style: TextStyle(fontSize: 12, color: Colors.grey),
                  ),
                  const SizedBox(height: 26),
                  _heading('03', '发送打印'),
                  const SizedBox(height: 16),
                  FilledButton.icon(
                    onPressed: model.canPrint && _label != null
                        ? () => model.printTestImage(_label!)
                        : null,
                    icon: const Icon(Icons.print_outlined),
                    label: const Text('打印测试标签'),
                  ),
                  const SizedBox(height: 8),
                  if (model.busy) ...[
                    const LinearProgressIndicator(minHeight: 2),
                    TextButton.icon(
                      onPressed: model.cancel,
                      icon: const Icon(Icons.stop_circle_outlined),
                      label: const Text('取消当前请求'),
                    ),
                  ],
                  const SizedBox(height: 16),
                  AnimatedSize(
                    duration: const Duration(milliseconds: 180),
                    alignment: Alignment.topLeft,
                    child: Align(
                      alignment: Alignment.centerLeft,
                      child: Text(
                        model.lastResult?.summary ?? '连接后可打印。发送完成不等于纸面打印完成。',
                        style: const TextStyle(fontSize: 14, height: 1.5),
                      ),
                    ),
                  ),
                  if (model.cleanupResult != null)
                    Padding(
                      padding: const EdgeInsets.only(top: 8),
                      child: Text(
                        '断开：${model.cleanupResult!.summary}',
                        style: const TextStyle(
                          fontSize: 12,
                          color: Colors.grey,
                        ),
                      ),
                    ),
                  const SizedBox(height: 24),
                  const Divider(),
                  ExpansionTile(
                    tilePadding: EdgeInsets.zero,
                    title: const Text('请求日志'),
                    subtitle: const Text(
                      '保留原始结果与输出证据',
                      style: TextStyle(fontSize: 12),
                    ),
                    children: [
                      for (final log in model.logs)
                        Padding(
                          padding: const EdgeInsets.symmetric(vertical: 10),
                          child: Align(
                            alignment: Alignment.centerLeft,
                            child: SelectableText(
                              log,
                              style: const TextStyle(
                                fontFamily: 'monospace',
                                fontSize: 11,
                                height: 1.5,
                              ),
                            ),
                          ),
                        ),
                    ],
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    ),
  );

  Widget _heading(String number, String title) => Row(
    children: [
      Text(
        number,
        style: const TextStyle(
          color: Color(0xff166b50),
          fontSize: 12,
          fontWeight: FontWeight.w700,
        ),
      ),
      const SizedBox(width: 12),
      Text(
        title,
        style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w600),
      ),
    ],
  );
}
