// SPDX-License-Identifier: MIT
package com.netumscan.printersdk.demo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.CancellationException;

/** App owns an imported cache and encoded bytes; no SDK loader or source object is exposed. */
final class DemoImageSource implements AutoCloseable {
    private static final class Shared {
        final File file;
        final byte[] encoded;
        int references = 1;
        Shared(File file) {
            this.file = file;
            if (file == null) { encoded = null; return; }
            long length = file.length();
            if (length <= 0 || length > ImageRaster.MAX_FILE_BYTES)
                throw new IllegalArgumentException("图片文件超过导入限制");
            byte[] data = new byte[(int)length];
            // ownedFile is called by the import worker, never by a print button.
            try (FileInputStream input = new FileInputStream(file)) {
                int offset = 0;
                while (offset < data.length) {
                    int count = input.read(data, offset, data.length - offset);
                    if (count < 0) throw new IOException("Incomplete image cache");
                    offset += count;
                }
                if (input.read() != -1) throw new IOException("Image cache changed");
            } catch (IOException failure) { throw new IllegalArgumentException("无法读取导入图片", failure); }
            encoded = data;
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
    DemoImageSource acquire() {
        synchronized (shared) {
            if (closed) throw new CancellationException("Image source released");
            shared.references++;
            return new DemoImageSource(shared);
        }
    }
    byte[] pixels(LabelConfig config, ImageCancellation cancelled) throws IOException {
        try (DemoImageSource reading = acquire()) {
            if (cancelled.getAsBoolean()) throw new CancellationException();
            return reading.shared.file == null ? TestLabel.gray(config, cancelled) :
                ImageImport.decode(reading.shared.file, config, cancelled);
        }
    }
    DemoPrintData printData(LabelConfig config) { return printData(config, () -> false); }
    DemoPrintData printData(LabelConfig config, ImageCancellation cancelled) {
        try (DemoImageSource reading = acquire()) {
            if (cancelled.getAsBoolean()) throw new CancellationException();
            return reading.shared.encoded == null ? DemoPrintData.bitmap(config, TestLabel.bitmap(config, cancelled)) :
                DemoPrintData.encoded(config, reading.shared.encoded);
        }
    }
    @Override public void close() {
        File released = null;
        synchronized (shared) {
            if (closed) return;
            closed = true;
            if (--shared.references == 0) released = shared.file;
        }
        // SDK acceptance copies the byte array; cache ownership remains entirely in the App.
        if (released != null) released.delete();
    }
}
