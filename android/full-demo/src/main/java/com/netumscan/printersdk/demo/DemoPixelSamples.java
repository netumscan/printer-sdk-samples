// SPDX-License-Identifier: MIT
package com.netumscan.printersdk.demo;

import com.netumscan.printersdk.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;

/** 同一打印页演示公共 ImageSource、逐图 ImageRender 和完整 PrintOptions。 */
final class DemoPixelSamples {
    enum Example { RGB, RGBA, DITHERING, MULTI_IMAGE }
    private static final int MAX_WIDTH = 256, MAX_HEIGHT = 128, RECEIPT_GAP = 16;
    private DemoPixelSamples() { }

    static void validate(Example example, LabelConfig config) {
        if (example == null || config == null) throw new IllegalArgumentException("请选择像素示例并填写打印参数");
        if (config.labelWidthDots < 32) throw new LabelConfig.ValidationError("label_width", "像素示例宽度须至少 32 dots");
        if (config.height < 64) throw new LabelConfig.ValidationError("height", "像素示例高度须至少 64 dots");
    }
    static DemoPrintData create(Example example, LabelConfig config) { return create(example, config, null); }
    static DemoPrintData create(Example example, LabelConfig config, MediaMode mode) {
        validate(example, config);
        int width = Math.min(config.labelWidthDots, MAX_WIDTH) / 8 * 8;
        List<ImageSource> sources = new ArrayList<>();
        List<ImageSource> tiles = new ArrayList<>();
        List<ImageRender> renders = new ArrayList<>();
        List<Integer> heights = new ArrayList<>();
        try {
            int gap = config.receipt ? RECEIPT_GAP : example == Example.DITHERING ? 4 : 8;
            if (example == Example.DITHERING) {
                int height = Math.min(MAX_HEIGHT, (config.height - 3 * gap) / 4);
                ImageSource source = source(PixelImage.GRAY8, width, height); sources.add(source);
                ImageRender[] algorithms = {ImageRender.threshold(128), ImageRender.floydSteinberg(),
                    ImageRender.bayer4x4(), ImageRender.bayer8x8()};
                for (ImageRender render : algorithms) { tiles.add(source); renders.add(render); heights.add(height); }
            } else if (example == Example.MULTI_IMAGE) {
                int height = Math.min(MAX_HEIGHT, (config.height - 2 * gap) / 3);
                int stripHeight = config.receipt ? height : Math.min(32, config.height - 2 * height - 2 * gap);
                ImageSource rgb = source(PixelImage.RGB24, width, height); sources.add(rgb);
                ImageSource gray = source(PixelImage.GRAY8, width, stripHeight); sources.add(gray);
                tiles.add(rgb); tiles.add(rgb); tiles.add(gray);
                heights.add(height); heights.add(height); heights.add(stripHeight);
                for (int i = 0; i < 3; i++) renders.add(ImageRender.threshold(128));
            } else {
                int height = Math.min(MAX_HEIGHT, config.receipt ? (config.height - gap) / 2 : config.height);
                ImageSource source = source(example == Example.RGB ? PixelImage.RGB24 : PixelImage.RGBA32, width, height);
                sources.add(source); tiles.add(source); heights.add(height);
                renders.add(example == Example.RGB ? ImageRender.threshold(128) : ImageRender.bayer4x4());
                if (config.receipt) { tiles.add(source); heights.add(height); renders.add(renders.get(0)); }
            }
            PrintOptions.Builder builder = DemoPrintData.builder(config, mode);
            int cursor = 0;
            for (int i = 0; i < tiles.size(); i++) {
                if (i != 0) { if (config.receipt) builder.appendBlankDots(gap); cursor += gap; }
                if (config.receipt) builder.appendImage(tiles.get(i), renders.get(i));
                else builder.addImage(tiles.get(i), 0, cursor, renders.get(i));
                cursor += heights.get(i);
            }
            if (config.receipt && cursor < config.height) builder.appendBlankDots(config.height - cursor);
            return new DemoPrintData(builder.build(), sources);
        } catch (RuntimeException | Error failure) { for (ImageSource source : sources) source.close(); throw failure; }
    }
    private static ImageSource source(int format, int width, int height) {
        return ImageSource.deferred(context -> {
            context.throwIfCancelled();
            byte[] pixels = pixels(format, width, height, context::isCancelled);
            context.throwIfCancelled();
            return PixelImage.copyOf(pixels, width, height, format);
        });
    }
    static byte[] pixels(int format, int width, int height, ImageCancellation cancellation) {
        int channels = format == PixelImage.GRAY8 ? 1 : format == PixelImage.RGB24 ? 3 : 4;
        byte[] pixels = new byte[width * height * channels];
        for (int y = 0; y < height; y++) {
            if (cancellation.getAsBoolean()) throw new CancellationException();
            for (int x = 0; x < width; x++) {
                int offset = (y * width + x) * channels;
                int level = x * 255 / Math.max(1, width - 1);
                if (channels == 1) pixels[offset] = (byte)level;
                else if (channels == 3) {
                    pixels[offset] = (byte)level;
                    pixels[offset + 1] = (byte)(y * 255 / Math.max(1, height - 1));
                    pixels[offset + 2] = (byte)(255 - level);
                } else {
                    int band = x * 3 / width;
                    pixels[offset + 3] = (byte)(band == 0 ? 0 : band == 1 ? 128 : 255);
                }
            }
        }
        return pixels;
    }
}
