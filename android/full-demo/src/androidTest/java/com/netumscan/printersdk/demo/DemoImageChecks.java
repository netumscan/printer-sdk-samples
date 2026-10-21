// SPDX-License-Identifier: MIT
package com.netumscan.printersdk.demo;

import android.app.Instrumentation;
import android.graphics.Bitmap;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Looper;
import com.netumscan.printersdk.PrinterProfile;
import com.netumscan.printersdk.LTM90Profile;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.CRC32;

/** Synthetic image/lifecycle checks; SDK printing uses a memory transport without Bluetooth or paper output. */
final class DemoImageChecks {
    static void run(Instrumentation instrumentation) throws Exception {
        geometry();
        builtInRulers();
        File directory = new File(instrumentation.getTargetContext().getCacheDir(), "full-demo-image-checks");
        check(directory.isDirectory() || directory.mkdirs(), "Synthetic image directory");
        File png = new File(directory, "synthetic.png"), jpeg = new File(directory, "synthetic.jpg"), invalid = new File(directory, "invalid.png");
        LabelConfig config = new LabelConfig(203, 384, 128, 128, 1, 1);
        try {
            Bitmap bitmap = Bitmap.createBitmap(40, 60, Bitmap.Config.ARGB_8888);
            try {
                for (int y = 0; y < 60; y++) for (int x = 0; x < 40; x++) {
                    int gray = (y / 20 * 2 + x / 20 + 1) * 20;
                    bitmap.setPixel(x, y, 0xff000000 | gray * 0x010101);
                }
                try (OutputStream output = new FileOutputStream(png)) {
                    check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output), "Synthetic PNG encoded");
                }
                try (OutputStream output = new FileOutputStream(jpeg)) {
                    check(bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output), "Synthetic JPEG encoded");
                }
            } finally { bitmap.recycle(); }
            byte[] pngBytes;
            try (java.io.InputStream input = new java.io.FileInputStream(png)) {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                byte[] block = new byte[1024]; int read;
                while ((read = input.read(block)) != -1) output.write(block, 0, read);
                pngBytes = output.toByteArray();
            }
            int[][] corners = {{20,40,100,120},{40,20,120,100},{120,100,40,20},{100,120,20,40},
                {20,100,40,120},{100,20,120,40},{120,40,100,20},{40,120,20,100}};
            PreparedImage untagged = ImageImport.prepare(jpeg, config, () -> false);
            ImageRaster.Layout normalFit = new ImageRaster.Layout(40, 60, 128, 128);
            check(Math.abs(untagged.grayAt(normalFit.y * 128 + normalFit.x) - 20) <= 3 &&
                Math.abs(untagged.grayAt((normalFit.y + normalFit.height - 1) * 128 + normalFit.x + normalFit.width - 1) - 120) <= 3,
                "JPEG without EXIF uses normal orientation");
            for (int orientation = 1; orientation <= 8; orientation++) {
                ExifInterface exif = new ExifInterface(jpeg.getPath());
                exif.setAttribute(ExifInterface.TAG_ORIENTATION, Integer.toString(orientation)); exif.saveAttributes();
                PreparedImage prepared = ImageImport.prepare(jpeg, config, () -> false);
                ImageRaster.Layout fit = new ImageRaster.Layout(orientation >= 5 ? 60 : 40,
                    orientation >= 5 ? 40 : 60, 128, 128);
                int[] positions = {fit.y * 128 + fit.x, fit.y * 128 + fit.x + fit.width - 1,
                    (fit.y + fit.height - 1) * 128 + fit.x, (fit.y + fit.height - 1) * 128 + fit.x + fit.width - 1};
                Bitmap preview = ImageImport.preview(prepared);
                try {
                    for (int index = 0; index < positions.length; index++) {
                        check(Math.abs(prepared.grayAt(positions[index]) - corners[orientation - 1][index]) <= 3,
                            "JPEG EXIF orientation " + orientation);
                        check((preview.getPixel(positions[index] % 128, positions[index] / 128) & 255) == prepared.grayAt(positions[index]),
                            "JPEG preview uses the prepared Gray8 snapshot");
                    }
                } finally { preview.recycle(); }
            }
            ExifInterface undefined = new ExifInterface(jpeg.getPath());
            undefined.setAttribute(ExifInterface.TAG_ORIENTATION, "0"); undefined.saveAttributes();
            PreparedImage normal = ImageImport.prepare(jpeg, config, () -> false);
            check(Math.abs(normal.grayAt(normalFit.y * 128 + normalFit.x) - 20) <= 3,
                "JPEG EXIF UNDEFINED=0 normalizes to normal orientation");
            File oriented = new File(directory, "oriented.png");
            for (int orientation = 1; orientation <= 8; orientation++) {
                try (OutputStream output = new FileOutputStream(oriented)) { output.write(oriented(pngBytes, orientation)); }
                PreparedImage prepared = ImageImport.prepare(oriented, config, () -> false);
                ImageRaster.Layout fit = new ImageRaster.Layout(orientation >= 5 ? 60 : 40,
                    orientation >= 5 ? 40 : 60, 128, 128);
                int[] positions = {fit.y * 128 + fit.x, fit.y * 128 + fit.x + fit.width - 1,
                    (fit.y + fit.height - 1) * 128 + fit.x, (fit.y + fit.height - 1) * 128 + fit.x + fit.width - 1};
                for (int index = 0; index < positions.length; index++)
                    check(prepared.grayAt(positions[index]) == corners[orientation - 1][index], "PNG EXIF orientation " + orientation);
                Bitmap preview = ImageImport.preview(prepared);
                try {
                    for (int index = 0; index < 128 * 128; index++)
                        check((preview.getPixel(index % 128, index / 128) & 255) == prepared.grayAt(index), "Exact Gray8 preview");
                } finally { preview.recycle(); }
            }
            try (OutputStream output = new FileOutputStream(invalid)) { output.write(new byte[]{1,2,3,4,5,6,7,8}); }
            try { ImageImport.prepare(invalid, config, () -> false); throw new AssertionError("Invalid PNG accepted"); }
            catch (IOException expected) { }
            try { ImageImport.copy(new ByteArrayInputStream(pngBytes), invalid, () -> true); throw new AssertionError("Cancelled copy accepted"); }
            catch (CancellationException expected) { }
            try (RandomAccessFile huge = new RandomAccessFile(invalid, "rw")) { huge.setLength(ImageRaster.MAX_FILE_BYTES + 1); }
            try { ImageImport.prepare(invalid, config, () -> false); throw new AssertionError("Oversized file accepted"); }
            catch (IOException expected) { }
            try (OutputStream output = new FileOutputStream(invalid)) { output.write(new byte[]{1,2,3,4,5,6,7,8}); }
            owner(instrumentation, config, png, invalid);
            readingLease(config, png, directory);
            File printing = new File(directory, "memory-print.png");
            try (java.io.InputStream input = new java.io.FileInputStream(png)) {
                ImageImport.copy(input, printing, () -> false);
            }
            DemoModelChecks.sourcePrintChecks(instrumentation.getTargetContext(), printing);
            await(() -> !printing.exists(), "Real Manager source print releases its private file");
        } finally {
            File[] files = directory.listFiles();
            if (files != null) for (File file : files) file.delete();
            directory.delete();
        }
    }

    /** Inspect the rendered ticks, so a correct helper cannot hide a stale DPI drawing path. */
    private static void builtInRulers() {
        PrinterProfile[] profiles = {PrinterProfile.cpcl(203, 864, 12, PrinterProfile.LabelAlignment.LEFT), LTM90Profile.printProfile()};
        for (PrinterProfile profile : profiles) {
            LabelConfig config = new LabelConfig(profile, 400, 400, 1, 1, 0, 0, 0);
            Bitmap bitmap = TestLabel.bitmap(config);
            try {
                int step = profile == profiles[0] ? 120 : 80;
                float text = Math.max(10, Math.min(32, config.labelWidthDots / 18f));
                int probeY = Math.round(Math.min(config.height - 24, 44 + text * 4)) - 4;
                check((bitmap.getPixel(12 + step, probeY) & 255) < 32,
                    "The drawn 10 mm ruler tick uses the adopted exact density " + profile.dotsPerMm);
                check((bitmap.getPixel(12 + step - 6, probeY) & 255) > 240 &&
                    (bitmap.getPixel(12 + step + 6, probeY) & 255) > 240,
                    "The expected tick is a vertical ruler marker rather than text or the horizontal line");
            } finally { bitmap.recycle(); }
        }
    }

    private static void geometry() {
        check(OriginPlacement.resolve(864, 801, OriginPlacement.CENTERED, 0, 0) == 28, "Byte-aligned centered origin");
        try { OriginPlacement.resolve(864, 801, OriginPlacement.MANUAL, 57, 0); throw new AssertionError("Overflow origin accepted"); }
        catch (LabelConfig.ValidationError expected) { }
        try { new LabelConfig(203, 7992, 7992, 1002, 1, 1); throw new AssertionError("Canvas pixel limit accepted"); }
        catch (LabelConfig.ValidationError expected) { }
        try { ImageRaster.sourceBounds(8001, 8000); throw new AssertionError("Source pixel limit accepted"); }
        catch (IllegalArgumentException expected) { }
        ImageRaster.Layout small = new ImageRaster.Layout(40, 60, 128, 128);
        check(small.width == 40 && small.height == 60 && small.x == 44 && small.y == 34,
            "App preview uses the SDK contain contract without enlarging a small image");
        check(ImageRaster.gray(0x00ff0000) == 255 && ImageRaster.gray(0x80000000) == 127, "Alpha composites onto white");
        LabelConfig config = new LabelConfig(203, 384, 128, 128, 1, 1);
        byte[] pixels = new byte[128 * 128]; Arrays.fill(pixels, (byte)255);
        PreparedImage prepared = new PreparedImage(config, pixels); pixels[0] = 0;
        byte[] copied = prepared.copyPixels(); copied[1] = 0;
        check(prepared.grayAt(0) == 255 && prepared.grayAt(1) == 255, "Prepared canvas is immutable");
    }

    private static void owner(Instrumentation instrumentation, LabelConfig config, File png, File invalid) throws Exception {
        AtomicReference<DemoImages> reference = new AtomicReference<>();
        Runnable listener = () -> check(Looper.myLooper() == Looper.getMainLooper(), "Image listener runs on main thread");
        instrumentation.runOnMainSync(() -> {
            DemoImages images = new DemoImages(instrumentation.getTargetContext()); reference.set(images);
            images.attach(listener); images.foreground(); images.importImage(Uri.fromFile(png));
        });
        DemoImages images = reference.get();
        try {
            await(() -> !state(instrumentation, images).busy, "Image source imported without print parameters");
            DemoImages.State first = state(instrumentation, images);
            check(first.prepared == null && first.imported && first.thumbnail == null && first.error == null,
                "Import retains only the source; preview is optional");
            DemoImageSource source = imageSource(instrumentation, images);
            File privateSource = (File)field(field(source, "shared"), "file");
            check(!privateSource.equals(png) && privateSource.isFile(), "Source copied into app private directory");
            check(source.pixels(config, () -> false).length == 128 * 128 && state(instrumentation, images).prepared == null,
                "Imported source decodes for printing without preparing a preview");
            File quota = new File((File)field(images, "directory"), "source-quota.image");
            try {
                try (RandomAccessFile cache = new RandomAccessFile(quota, "rw")) {
                    cache.setLength(DemoImages.MAX_CACHE_BYTES);
                }
                instrumentation.runOnMainSync(() -> images.importImage(Uri.fromFile(png)));
                await(() -> !state(instrumentation, images).busy, "Cache quota rejection completes");
                check(imageSource(instrumentation, images) == source && privateSource.isFile() &&
                    state(instrumentation, images).error != null,
                    "App total cache quota refuses new import while preserving its in-use source");
            } finally { check(!quota.exists() || quota.delete(), "Cache quota fixture released"); }

            instrumentation.runOnMainSync(() -> { images.detach(listener); images.attach(listener); });
            check(imageSource(instrumentation, images) == source, "Configuration reattachment retains the selected source");
            instrumentation.runOnMainSync(() -> images.importImage(Uri.fromFile(invalid)));
            await(() -> !state(instrumentation, images).busy, "Invalid import completed");
            check(state(instrumentation, images).prepared == first.prepared && state(instrumentation, images).error != null && privateSource.isFile(),
                "Failure preserves the previous source without creating a preview");
            instrumentation.runOnMainSync(() -> { images.importImage(Uri.fromFile(png)); images.cancel(); });
            check(imageSource(instrumentation, images) == source, "Cancelled import preserves the previous source");
            LabelConfig previewConfig = new LabelConfig(203, 384, 256, 512, 1, 1);
            instrumentation.runOnMainSync(() -> images.preview(previewConfig));
            await(() -> !state(instrumentation, images).busy, "Full preview generated");
            DemoImages.State full = state(instrumentation, images);
            check(full.preview != null && full.preview.getWidth() == 256 && full.preview.getHeight() == 512 && full.previewRevision == 1 &&
                Math.max(full.thumbnail.getWidth(), full.thumbnail.getHeight()) <= 360,
                "Explicit preview contains complete canvas and one result revision");
            instrumentation.runOnMainSync(images::clearPreview);
            check(state(instrumentation, images).preview == null && full.preview.isRecycled(), "Full preview released on dialog dismissal");
            instrumentation.runOnMainSync(images::invalidate);
            check(state(instrumentation, images).prepared == null &&
                imageSource(instrumentation, images).pixels(config, () -> false).length == 128 * 128,
                "Clearing all display preparation leaves the source usable with different print dimensions");
            instrumentation.runOnMainSync(() -> { images.prepare(config); images.background(); });
            check(!state(instrumentation, images).busy && state(instrumentation, images).prepared == null,
                "Background cancels display work without deleting the source");
            DemoImageSource retained = imageSource(instrumentation, images).acquire();
            try {
                instrumentation.runOnMainSync(images::close);
                AtomicReference<DemoImages> replacement = new AtomicReference<>();
                instrumentation.runOnMainSync(() -> replacement.set(new DemoImages(instrumentation.getTargetContext())));
                DemoImages next = replacement.get();
                try {
                    ThreadPoolExecutor nextWorker = (ThreadPoolExecutor)field(next, "worker");
                    nextWorker.submit(() -> { }).get(10, TimeUnit.SECONDS);
                    check(privateSource.isFile() && retained.pixels(config, () -> false).length == 128 * 128,
                        "A new image owner does not delete the old owner's retained source");
                } finally { instrumentation.runOnMainSync(next::close); }
            } finally { retained.close(); }
            await(() -> !privateSource.exists(), "Replaced private source removed");
            ThreadPoolExecutor worker = (ThreadPoolExecutor)field(images, "worker");
            await(worker::isTerminated, "Image worker shuts down after cancellation cleanup");
            // Another independent owner verifies that choosing the built-in source does not prepare a preview.
            AtomicReference<DemoImages> builtin = new AtomicReference<>();
            instrumentation.runOnMainSync(() -> {
                DemoImages next = new DemoImages(instrumentation.getTargetContext()); builtin.set(next);
                next.foreground(); next.importImage(Uri.fromFile(png));
            });
            try {
                await(() -> !state(instrumentation, builtin.get()).busy, "Replacement source imported");
                instrumentation.runOnMainSync(builtin.get()::useBuiltin);
                await(() -> !state(instrumentation, builtin.get()).busy, "Built-in source selected");
                check(!state(instrumentation, builtin.get()).imported && state(instrumentation, builtin.get()).prepared == null,
                    "Built-in selection changes only the source, without producing display pixels");
            } finally { instrumentation.runOnMainSync(builtin.get()::close); }
        } finally { instrumentation.runOnMainSync(images::close); }
    }

    private static void readingLease(LabelConfig config, File png, File directory) throws Exception {
        File file = new File(directory, "reading-lease.png");
        try (java.io.InputStream input = new java.io.FileInputStream(png)) { ImageImport.copy(input, file, () -> false); }
        DemoImageSource owner = DemoImageSource.ownedFile(file), request = owner.acquire();
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1), done = new CountDownLatch(1);
        AtomicBoolean cancelled = new AtomicBoolean();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread reading = new Thread(() -> {
            try {
                request.pixels(config, () -> {
                    entered.countDown();
                    try { release.await(); } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); return true; }
                    return cancelled.get();
                });
                failure.set(new AssertionError("Cancelled source decoding succeeded"));
            } catch (CancellationException expected) { }
            catch (Throwable error) { failure.set(error); }
            finally { done.countDown(); }
        }, "DemoImageChecks.reading");
        reading.setDaemon(true); reading.start();
        try {
            check(entered.await(10, TimeUnit.SECONDS), "Source reading holds an independent lease");
            cancelled.set(true); request.close(); owner.close(); request.close(); owner.close();
            check(file.isFile(), "Request terminal and owner close cannot delete a source still being read");
            release.countDown(); check(done.await(10, TimeUnit.SECONDS), "Cancelled source reading exits");
            if (failure.get() != null) throw new AssertionError("Source reading failure", failure.get());
            await(() -> !file.exists(), "Final reading lease releases the private source");
            try { request.acquire(); throw new AssertionError("Closed source revived"); }
            catch (CancellationException expected) { }
        } finally { release.countDown(); owner.close(); request.close(); }
    }

    private static byte[] oriented(byte[] png, int orientation) throws IOException {
        byte[] metadata = {'I','I',42,0,8,0,0,0,1,0,18,1,3,0,1,0,0,0,(byte)orientation,0,0,0,0,0,0,0};
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(); bytes.write(png, 0, 33);
        DataOutputStream output = new DataOutputStream(bytes); output.writeInt(metadata.length);
        byte[] type = {101,88,73,102}; output.write(type); output.write(metadata);
        CRC32 crc = new CRC32(); crc.update(type); crc.update(metadata); output.writeInt((int)crc.getValue());
        output.write(png, 33, png.length - 33); return bytes.toByteArray();
    }
    private static DemoImages.State state(Instrumentation instrumentation, DemoImages images) {
        AtomicReference<DemoImages.State> result = new AtomicReference<>();
        instrumentation.runOnMainSync(() -> result.set(images.state())); return result.get();
    }
    private static DemoImageSource imageSource(Instrumentation instrumentation, DemoImages images) {
        AtomicReference<DemoImageSource> result = new AtomicReference<>();
        instrumentation.runOnMainSync(() -> result.set(images.source())); return result.get();
    }
    private static Object field(Object object, String name) throws Exception {
        Field field = object.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(object);
    }
    private interface Condition { boolean ready() throws Exception; }
    private static void await(Condition condition, String message) throws Exception {
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(10);
        while (!condition.ready() && System.nanoTime() < deadline) Thread.sleep(10);
        check(condition.ready(), message);
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
