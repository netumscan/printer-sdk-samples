// SPDX-License-Identifier: MIT
package com.netumscan.printersdk.demo;

import android.graphics.*;
import java.util.concurrent.CancellationException;

final class TestLabel {
    static int rulerDots(LabelConfig config) { return config.printProfile().dotsFromMm(10); }
    static Bitmap bitmap(LabelConfig config) { return bitmap(config, () -> false); }
    static Bitmap bitmap(LabelConfig config, ImageCancellation cancelled) {
        if (cancelled.getAsBoolean()) throw new CancellationException();
        Bitmap bitmap = Bitmap.createBitmap(config.labelWidthDots, config.height, Bitmap.Config.ARGB_8888);
        try {
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(Color.WHITE);
            if (cancelled.getAsBoolean()) throw new CancellationException();
            Paint ink = new Paint(Paint.ANTI_ALIAS_FLAG);
            ink.setColor(Color.BLACK); ink.setStrokeWidth(2); ink.setStyle(Paint.Style.STROKE);
            canvas.drawRect(2, 2, config.labelWidthDots - 3, config.height - 3, ink);
            ink.setStyle(Paint.Style.FILL);
            float text = Math.max(10, Math.min(32, config.labelWidthDots / 18f));
            ink.setTextSize(text); ink.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
            canvas.drawText(config.receipt ? "ESC/POS / RECEIPT" : "NETUM / CPCL", 12, 16 + text, ink);
            ink.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL)); ink.setTextSize(text * .8f);
            canvas.drawText("中文打印 · 对齐测试", 12, 22 + text * 2, ink);
            ink.setTextSize(text * .65f);
            canvas.drawText(config.labelWidthDots + " x " + config.height + " dots", 12, 28 + text * 3, ink);
            float tenMm = rulerDots(config);
            float y = Math.min(config.height - 24, 44 + text * 4);
            for (float x = 12; x + tenMm < config.labelWidthDots - 12; x += tenMm) {
                if (cancelled.getAsBoolean()) throw new CancellationException();
                canvas.drawLine(x, y, x + tenMm, y, ink);
                canvas.drawLine(x, y - 6, x, y + 6, ink);
                canvas.drawLine(x + tenMm, y - 6, x + tenMm, y + 6, ink);
            }
            canvas.drawText("10 mm", 12, Math.min(config.height - 8, y + 18), ink);
            if (config.receipt) {
                ink.setTextSize(text * .8f);
                float rowY = y + 48;
                for (int i = 1; i <= 6 && rowY < config.height - 40; i++, rowY += 32) {
                    canvas.drawText("商品 " + i + "    1 × 12.00", 12, rowY, ink);
                }
                if (rowY < config.height - 20) canvas.drawText("合计 / TOTAL    72.00", 12, rowY, ink);
            }
            if (cancelled.getAsBoolean()) throw new CancellationException();
            return bitmap;
        } catch (RuntimeException | Error failure) { bitmap.recycle(); throw failure; }
    }
    static byte[] gray(LabelConfig config, ImageCancellation cancelled) {
        if (cancelled.getAsBoolean()) throw new CancellationException();
        Bitmap bitmap = bitmap(config, cancelled);
        try {
            byte[] pixels = new byte[config.labelWidthDots * config.height];
            int[] row = new int[config.labelWidthDots];
            for (int y = 0; y < config.height; y++) {
                if (cancelled.getAsBoolean()) throw new CancellationException();
                bitmap.getPixels(row, 0, config.labelWidthDots, 0, y, config.labelWidthDots, 1);
                for (int x = 0; x < row.length; x++) {
                    int color = row[x];
                    pixels[y * config.labelWidthDots + x] = (byte)ImageRaster.gray(color);
                }
            }
            return pixels;
        } finally { bitmap.recycle(); }
    }
}
