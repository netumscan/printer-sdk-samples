// SPDX-License-Identifier: MIT
package com.netumscan.printersdk.demo;

import com.netumscan.printersdk.internal.ImageTransforms;
import java.util.Arrays;
import java.util.concurrent.CancellationException;

/** App image limits and cancellation around the SDK's shared pure image math. */
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
            ImageTransforms.Layout fit = ImageTransforms.contain(sourceWidth, sourceHeight, canvasWidth, canvasHeight);
            width = fit.width; height = fit.height; x = fit.x; y = fit.y;
        }
    }
    static int sample(int width, int height, Layout fit, int orientation) {
        int targetWidth = orientation >= 5 ? fit.height : fit.width;
        int targetHeight = orientation >= 5 ? fit.width : fit.height;
        return ImageTransforms.sampleSize(width, height, targetWidth, targetHeight);
    }
    static int gray(int argb) {
        return ImageTransforms.grayArgb(argb);
    }
    static byte[] render(int[] argb, int decodedWidth, int decodedHeight, int orientation,
                         Layout fit, int canvasWidth, int canvasHeight, ImageCancellation cancelled) {
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
                int ox = (int)((long)x * orientedWidth / fit.width);
                int source = ImageTransforms.sourceIndex(ox, oy, decodedWidth, decodedHeight, orientation);
                pixels[(y + fit.y) * canvasWidth + x + fit.x] = (byte)gray(argb[source]);
            }
        }
        return pixels;
    }
}
