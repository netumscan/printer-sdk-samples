// SPDX-License-Identifier: MIT
package com.netumscan.printersdk.demo;

import com.netumscan.printersdk.*;
import java.util.ArrayList;
import java.util.List;

/** App 构建完整 PrintOptions；只持提交前的来源引用，不管理设备或另建打印队列。 */
final class DemoPrintData implements AutoCloseable {
    final PrintOptions options;
    private final List<ImageSource> sources;
    private final Runnable released;

    DemoPrintData(PrintOptions options, List<ImageSource> sources) { this(options, sources, () -> { }); }
    DemoPrintData(PrintOptions options, List<ImageSource> sources, Runnable released) {
        this.options = options; this.sources = new ArrayList<>(sources); this.released = released;
    }
    static PrintOptions.Builder builder(LabelConfig config) { return builder(config, null); }
    static PrintOptions.Builder builder(LabelConfig config, MediaMode mode) {
        PrintOptions.Builder builder = config.receipt
            ? PrintOptions.receiptDots(config.labelWidthDots, config.receiptBudget())
                .receiptFinish(config.trailingBlankDots, PrintOptions.CUT_NONE, PrintOptions.EACH_COPY)
            : PrintOptions.labelDots(config.labelWidthDots, config.height + config.trailingBlankDots)
                .originDots(config.manualOriginDots, 0)
                .horizontalCalibrationDots(config.horizontalCalibrationDots).nextLabel(config.form);
        if (!config.receipt && mode != null) builder.mediaRequirement(new MediaRequirement(
            MediaSettings.tracking(mode), MediaRequirement.ALLOW_LAST_SENT));
        return builder.copies(config.copies).timeoutMs(120000);
    }
    static DemoPrintData image(LabelConfig config, ImageSource source) { return image(config, source, null); }
    static DemoPrintData image(LabelConfig config, ImageSource source, MediaMode mode) {
        List<ImageSource> sources = new ArrayList<>(); sources.add(source);
        try {
            PrintOptions.Builder builder = builder(config, mode);
            if (config.receipt) builder.appendImage(source); else builder.addImage(source, 0, 0);
            return new DemoPrintData(builder.build(), sources);
        } catch (RuntimeException | Error failure) { source.close(); throw failure; }
    }
    static DemoPrintData pixels(LabelConfig config, byte[] immutablePixels) { return pixels(config, immutablePixels, null); }
    static DemoPrintData pixels(LabelConfig config, byte[] immutablePixels, MediaMode mode) {
        if (config == null || immutablePixels == null ||
                (long)config.labelWidthDots * config.height != immutablePixels.length)
            throw new IllegalArgumentException("图片像素尺寸与本次打印宽高不一致");
        return image(config, ImageSource.pixels(PixelImage.copyOf(immutablePixels,
            config.labelWidthDots, config.height, PixelImage.GRAY8)), mode);
    }
    @Override public void close() {
        try { for (ImageSource source : sources) source.close(); }
        finally { released.run(); }
    }
}
