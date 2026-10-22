// SPDX-License-Identifier: MIT
package com.netumscan.printersdk.demo;

import android.graphics.Bitmap;
import android.graphics.Color;
import java.util.concurrent.CancellationException;

/** App composes the page before passing one Bitmap to the ordinary SDK entry point. */
final class DemoPixelSamples {
    enum Example { RGB, RGBA, DITHERING, MULTI_IMAGE }
    static final int GRAY8 = 1, RGB24 = 2, RGBA32 = 3;
    private DemoPixelSamples() { }
    static void validate(Example example, LabelConfig config) {
        if (example == null || config == null) throw new IllegalArgumentException("请选择像素示例并填写页面尺寸");
        if (config.labelWidthDots < 32 || config.height < 64)
            throw new IllegalArgumentException("像素示例至少需要 32 × 64 dots 页面");
    }
    static DemoPrintData create(Example example, LabelConfig config) {
        return create(example, config, () -> false);
    }
    static DemoPrintData create(Example example, LabelConfig config, ImageCancellation cancelled) {
        validate(example, config);
        if (cancelled.getAsBoolean()) throw new CancellationException();
        Bitmap page = Bitmap.createBitmap(config.labelWidthDots, config.height, Bitmap.Config.ARGB_8888);
        try {
            int[] row = new int[config.labelWidthDots];
            int tiles = example == Example.DITHERING ? 4 : example == Example.MULTI_IMAGE ? 3 : 1;
            int gap = tiles == 1 ? 0 : 8;
            int tileHeight = Math.max(1, (config.height - gap * (tiles - 1)) / tiles);
            for (int y = 0; y < config.height; y++) {
                if (cancelled.getAsBoolean()) throw new CancellationException();
                int tile = y / (tileHeight + gap), tileY = y % (tileHeight + gap);
                for (int x = 0; x < row.length; x++) {
                    if (tile >= tiles || tileY >= tileHeight) { row[x] = Color.WHITE; continue; }
                    int level = x * 255 / Math.max(1, row.length - 1);
                    if (example == Example.DITHERING) {
                        int threshold = tile == 0 ? 128 : bayer(x, tileY, tile);
                        int gray = level < threshold ? 0 : 255;
                        row[x] = Color.rgb(gray, gray, gray);
                    } else if (example == Example.RGBA) {
                        // Match the previous white Canvas composition, including straight alpha.
                        int alpha = x < row.length / 3 ? 0 : x < row.length * 2 / 3 ? 128 : 255;
                        int gray = 255 - alpha;
                        row[x] = Color.rgb(gray, gray, gray);
                    } else row[x] = Color.rgb(level, tileY * 255 / Math.max(1, tileHeight - 1), 255 - level);
                }
                page.setPixels(row, 0, row.length, 0, y, row.length, 1);
            }
            if (cancelled.getAsBoolean()) throw new CancellationException();
            return DemoPrintData.bitmap(config, page);
        } catch (RuntimeException | Error failure) { page.recycle(); throw failure; }
    }
    // App-only ordered patterns. The SDK's own binarization remains fixed at threshold 128.
    private static int bayer(int x, int y, int variant) {
        int side = variant == 1 ? 2 : variant == 2 ? 4 : 8;
        int rank = 0;
        for (int bit = 0; (1 << bit) < side; bit++) {
            int xb = (x >> bit) & 1, yb = (y >> bit) & 1;
            rank = rank * 4 + ((xb ^ yb) * 2 + yb);
        }
        return (rank * 256 + 128) / (side * side);
    }
    static byte[] pixels(int format, int width, int height, ImageCancellation cancellation) {
        int channels = format == GRAY8 ? 1 : format == RGB24 ? 3 : 4;
        byte[] pixels = new byte[width * height * channels];
        for (int y = 0; y < height; y++) {
            if (cancellation.getAsBoolean()) throw new CancellationException();
            for (int x = 0; x < width; x++) {
                int offset = (y * width + x) * channels;
                int level = x * 255 / Math.max(1, width - 1);
                if (channels == 1) pixels[offset] = (byte)level;
                else if (channels == 3) {
                    pixels[offset] = (byte)level; pixels[offset + 1] = (byte)(y * 255 / Math.max(1, height - 1));
                    pixels[offset + 2] = (byte)(255 - level);
                } else pixels[offset + 3] = (byte)(x * 3 / width == 0 ? 0 : x * 3 / width == 1 ? 128 : 255);
            }
        }
        return pixels;
    }
}
