package com.netumscan.printersdk.demo;

import java.util.Arrays;
import java.util.concurrent.CancellationException;
import com.netumscan.printersdk.demo.PrinterController.Cancellation;

/** Demo-only geometry and pixel conversion; no Android or SDK IO. */
final class ImageRaster {
    static final long MAX_FILE_BYTES = 32L * 1024 * 1024;
    static final long MAX_SOURCE_PIXELS = 64_000_000;
    static final int MAX_SOURCE_EDGE = 32768, MAX_OUTPUT_PIXELS = 8_000_000;
    static void sourceBounds(int width, int height) {
        if (width <= 0 || height <= 0 || width > MAX_SOURCE_EDGE || height > MAX_SOURCE_EDGE ||
            (long)width * height > MAX_SOURCE_PIXELS) throw new IllegalArgumentException("图片尺寸超过导入限制");
    }
    static final class Layout {
        final int width, height, x, y;
        Layout(int sourceWidth, int sourceHeight, int canvasWidth, int canvasHeight) {
            sourceBounds(sourceWidth, sourceHeight);
            if (canvasWidth <= 0 || canvasHeight <= 0 || (long)canvasWidth * canvasHeight > MAX_OUTPUT_PIXELS)
                throw new IllegalArgumentException("画布超过 800 万像素");
            if ((long)sourceWidth * canvasHeight >= (long)sourceHeight * canvasWidth) {
                width = canvasWidth; height = Math.max(1, (int)((long)sourceHeight * canvasWidth / sourceWidth));
            } else {
                height = canvasHeight; width = Math.max(1, (int)((long)sourceWidth * canvasHeight / sourceHeight));
            }
            x = (canvasWidth - width) / 2; y = (canvasHeight - height) / 2;
        }
    }
    static int sample(int width, int height, Layout fit, int orientation) {
        int targetWidth = orientation >= 5 ? fit.height : fit.width;
        int targetHeight = orientation >= 5 ? fit.width : fit.height;
        int sample = 1;
        while ((width + sample - 1L) / sample > targetWidth ||
               (height + sample - 1L) / sample > targetHeight ||
               ((width + sample - 1L) / sample) * ((height + sample - 1L) / sample) > MAX_OUTPUT_PIXELS) sample *= 2;
        return sample;
    }
    static int gray(int argb) {
        int a = argb >>> 24;
        int r = (((argb >>> 16) & 255) * a + 255 * (255 - a) + 127) / 255;
        int g = (((argb >>> 8) & 255) * a + 255 * (255 - a) + 127) / 255;
        int b = ((argb & 255) * a + 255 * (255 - a) + 127) / 255;
        return (299 * r + 587 * g + 114 * b + 500) / 1000;
    }
    static byte[] render(int[] argb, int decodedWidth, int decodedHeight, int orientation,
                         Layout fit, int canvasWidth, int canvasHeight, Cancellation cancelled) {
        if (orientation < 1 || orientation > 8 || (long)decodedWidth * decodedHeight != argb.length)
            throw new IllegalArgumentException("无效图片像素");
        int orientedWidth = orientation >= 5 ? decodedHeight : decodedWidth;
        int orientedHeight = orientation >= 5 ? decodedWidth : decodedHeight;
        if (canvasWidth <= 0 || canvasHeight <= 0 || (long)canvasWidth * canvasHeight > MAX_OUTPUT_PIXELS)
            throw new IllegalArgumentException("画布超过 800 万像素");
        byte[] pixels = new byte[canvasWidth * canvasHeight]; Arrays.fill(pixels, (byte)255);
        for (int y = 0; y < fit.height; y++) {
            if (cancelled.getAsBoolean()) throw new CancellationException();
            int oy = (int)((long)y * orientedHeight / fit.height);
            for (int x = 0; x < fit.width; x++) {
                int ox = (int)((long)x * orientedWidth / fit.width), sx, sy;
                switch (orientation) {
                case 2: sx = decodedWidth - 1 - ox; sy = oy; break;
                case 3: sx = decodedWidth - 1 - ox; sy = decodedHeight - 1 - oy; break;
                case 4: sx = ox; sy = decodedHeight - 1 - oy; break;
                case 5: sx = oy; sy = ox; break;
                case 6: sx = oy; sy = decodedHeight - 1 - ox; break;
                case 7: sx = decodedWidth - 1 - oy; sy = decodedHeight - 1 - ox; break;
                case 8: sx = decodedWidth - 1 - oy; sy = ox; break;
                default: sx = ox; sy = oy;
                }
                pixels[(y + fit.y) * canvasWidth + x + fit.x] = (byte)gray(argb[sy * decodedWidth + sx]);
            }
        }
        return pixels;
    }
}
