import 'dart:typed_data';
import 'dart:ui' as ui;

/// A complete PNG; no raw RGBA, base64, or printer protocol in Dart.
Future<Uint8List> createTestLabel() async {
  final recorder = ui.PictureRecorder();
  final canvas = ui.Canvas(recorder);
  canvas.drawColor(const ui.Color(0xffffffff), ui.BlendMode.src);
  final ink = ui.Paint()..color = const ui.Color(0xff000000);
  canvas.drawRect(const ui.Rect.fromLTWH(24, 24, 352, 5), ink);
  void text(String value, double x, double y, double size) {
    final builder =
        ui.ParagraphBuilder(
            ui.ParagraphStyle(fontSize: size, fontWeight: ui.FontWeight.w600),
          )
          ..pushStyle(ui.TextStyle(color: const ui.Color(0xff000000)))
          ..addText(value);
    final paragraph = builder.build()
      ..layout(const ui.ParagraphConstraints(width: 352));
    canvas.drawParagraph(paragraph, ui.Offset(x, y));
  }

  text('NETUM', 24, 45, 46);
  text('FLUTTER / SDK', 26, 108, 20);
  text('PRINT TEST', 26, 170, 32);
  text('01 — READY TO PRINT', 26, 218, 16);
  // Decorative bars, deliberately not presented as a scannable barcode.
  for (var x = 26; x < 370; x += 9) {
    canvas.drawRect(
      ui.Rect.fromLTWH(x.toDouble(), 276, (x % 3 + 2).toDouble(), 56),
      ink,
    );
  }
  text('NETUM PRINTER SDK', 26, 351, 15);
  final picture = recorder.endRecording();
  try {
    final image = await picture.toImage(400, 400);
    try {
      final data = await image.toByteData(format: ui.ImageByteFormat.png);
      if (data == null) throw StateError('PNG encoding failed');
      return data.buffer.asUint8List(data.offsetInBytes, data.lengthInBytes);
    } finally {
      image.dispose();
    }
  } finally {
    picture.dispose();
  }
}
