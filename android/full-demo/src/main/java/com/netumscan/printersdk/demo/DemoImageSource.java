// SPDX-License-Identifier: MIT
package com.netumscan.printersdk.demo;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CancellationException;

/** 图片来源只管理源文件寿命和像素读取，不持有预览或打印请求。 */
final class DemoImageSource implements AutoCloseable {
    private static final class Shared {
        final File file;
        final com.netumscan.printersdk.ImageSource printing;
        int references = 1;
        Shared(File file) {
            this.file = file;
            // 缓存副本的删除权转交 SDK；Demo 只在来源仍存活时借用它生成预览。
            printing = file == null ? null : com.netumscan.printersdk.AndroidImages.takeFile(file);
        }
    }

    private final Shared shared;
    private boolean closed;

    private DemoImageSource(Shared shared) { this.shared = shared; }
    static DemoImageSource builtin() { return new DemoImageSource(new Shared(null)); }
    static DemoImageSource ownedFile(File file) {
        if (file == null) throw new IllegalArgumentException("No image file");
        return new DemoImageSource(new Shared(file));
    }

    /** 每个使用者取得独立句柄；界面换图或关闭后，已提交的读取仍能完成清理。 */
    DemoImageSource acquire() {
        synchronized (shared) {
            if (closed) throw new CancellationException("Image source released");
            DemoImageSource acquired = new DemoImageSource(shared);
            shared.references++;
            return acquired;
        }
    }

    byte[] pixels(LabelConfig config, ImageCancellation cancelled) throws IOException {
        // 取消请求可能先回调终态，解码仍在退出；读取引用保证此时源文件不被删除。
        try (DemoImageSource reading = acquire()) {
            if (cancelled.getAsBoolean()) throw new CancellationException();
            return reading.shared.file == null ? TestLabel.gray(config, cancelled) :
                ImageImport.decode(reading.shared.file, config, cancelled);
        }
    }

    /** 导入缓存直接用 SDK AndroidImages；动态校准图使用高级 loader。 */
    DemoPrintData printData(LabelConfig config, com.netumscan.printersdk.MediaMode mode) {
        if (shared.printing == null) return DemoPrintData.image(config, printSource(config), mode);
        DemoImageSource retained = acquire();
        try {
            com.netumscan.printersdk.PrintOptions.Builder builder = DemoPrintData.builder(config, mode);
            com.netumscan.printersdk.ImageRender render = com.netumscan.printersdk.ImageRender.threshold(128)
                .containBoxDots(config.labelWidthDots, config.height);
            if (config.receipt) builder.appendImage(shared.printing, render);
            else builder.addImage(shared.printing, 0, 0, render);
            return new DemoPrintData(builder.build(), java.util.Collections.emptyList(), retained::close);
        } catch (RuntimeException | Error failure) { retained.close(); throw failure; }
    }

    /** 创建 SDK 来源，提交返回后由 SDK 引用负责实际读取寿命。 */
    com.netumscan.printersdk.ImageSource printSource(LabelConfig config) {
        DemoImageSource retained = acquire();
        try {
            return com.netumscan.printersdk.ImageSource.deferred(context -> {
                context.throwIfCancelled();
                byte[] pixels = retained.pixels(config, context::isCancelled);
                context.throwIfCancelled();
                return com.netumscan.printersdk.PixelImage.copyOf(pixels,
                    config.labelWidthDots, config.height, com.netumscan.printersdk.PixelImage.GRAY8);
            }, retained::close);
        } catch (RuntimeException | Error failure) { retained.close(); throw failure; }
    }

    @Override public void close() {
        com.netumscan.printersdk.ImageSource released = null;
        synchronized (shared) {
            if (closed) return;
            closed = true;
            if (--shared.references == 0) released = shared.printing;
        }
        // SDK 引用包括已接收请求；实际读取退出前不会删除文件。
        if (released != null) released.close();
    }
}
