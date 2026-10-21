package com.netumscan.printersdk;

import android.app.Instrumentation;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.media.ExifInterface;
import com.netumscan.printersdk.internal.BluetoothChannel;
import com.netumscan.printersdk.internal.IoControl;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.CRC32;

/** Real Android PNG/JPEG decoder, EXIF and source-ownership regression checks. */
public final class AndroidImageAdapterChecks {
    private AndroidImageAdapterChecks() { }
    public static void run(Instrumentation instrumentation) throws Exception {
        Context application = instrumentation.getTargetContext();
        File directory = new File(application.getCacheDir(), "sdk-image-adapter-checks");
        check(directory.isDirectory() || directory.mkdirs(), "SDK image test directory");
        File png = new File(directory, "source.png"), jpeg = new File(directory, "source.jpg");
        try {
            Bitmap bitmap = Bitmap.createBitmap(40, 60, Bitmap.Config.ARGB_8888);
            try {
                for (int y = 0; y < 60; y++) for (int x = 0; x < 40; x++) {
                    int gray = (y / 20 * 2 + x / 20 + 1) * 20;
                    bitmap.setPixel(x, y, 0xff000000 | gray * 0x010101);
                }
                write(bitmap, Bitmap.CompressFormat.PNG, png);
                write(bitmap, Bitmap.CompressFormat.JPEG, jpeg);
            } finally { bitmap.recycle(); }
            byte[] original = read(png);
            int[][] corners = {{20,40,100,120},{40,20,120,100},{120,100,40,20},{100,120,20,40},
                {20,100,40,120},{100,20,120,40},{120,40,100,20},{40,120,20,100}};
            for (int orientation = 1; orientation <= 8; orientation++) {
                File rotatedPng = new File(directory, "png-" + orientation + ".png");
                try (OutputStream output = new FileOutputStream(rotatedPng)) { output.write(oriented(original, orientation)); }
                assertCorners(loadOwned(rotatedPng), corners[orientation - 1], 0, "SDK PNG EXIF " + orientation);
                File rotatedJpeg = new File(directory, "jpeg-" + orientation + ".jpg");
                try (OutputStream output = new FileOutputStream(rotatedJpeg)) { output.write(read(jpeg)); }
                ExifInterface exif = new ExifInterface(rotatedJpeg.getPath());
                exif.setAttribute(ExifInterface.TAG_ORIENTATION, Integer.toString(orientation)); exif.saveAttributes();
                assertCorners(loadOwned(rotatedJpeg), corners[orientation - 1], 3, "SDK JPEG EXIF " + orientation);
            }
            resource(application, png, corners[0]);
            alpha(directory);
            boundedSampling(directory);
            File invalid = new File(directory, "invalid.png");
            try (OutputStream output = new FileOutputStream(invalid)) { output.write(new byte[16]); }
            try { loadOwned(invalid); throw new AssertionError("SDK invalid image accepted"); }
            catch (IllegalArgumentException expected) {
                check(expected.getCause() instanceof IOException &&
                    expected.getMessage().equals("Image source is unreadable or invalid"), "Fixed local image failure without a path");
                awaitDeleted(invalid);
            }
            File large = new File(directory, "too-large.png");
            try (RandomAccessFile output = new RandomAccessFile(large, "rw")) { output.setLength(32L * 1024 * 1024 + 1); }
            try { loadOwned(large); throw new AssertionError("SDK oversized encoded file accepted"); }
            catch (PrinterException expected) { check(expected.code == 1008, "SDK encoded file resource limit"); awaitDeleted(large); }
            managerInvalidFile(instrumentation, directory);
        } finally {
            File[] files = directory.listFiles();
            if (files != null) for (File file : files) if (file.exists() && !file.delete()) throw new IOException("Image fixture cleanup failed");
            if (directory.exists() && !directory.delete()) throw new IOException("Image fixture directory cleanup failed");
        }
    }
    private static PreparedPrint.Pixels loadOwned(File file) throws Exception {
        try (ImageSource source = AndroidImages.takeFile(file)) {
            PreparedPrint.Pixels result = prepare(PrintOptions.labelDots(128, 128).addImage(source, 0, 0).build());
            return result;
        } finally { awaitDeleted(file); }
    }
    private static PreparedPrint.Pixels prepare(PrintOptions options) throws Exception {
        try (PrintOptions.Retained retained = options.retainSources()) {
            PreparedPrint result = retained.prepare(PrinterProfile.cpcl(203, 384),
                new CancellationContext(System.nanoTime() + TimeUnit.SECONDS.toNanos(30)), new PreparationBudget());
            return result.elements.get(0).pixels;
        }
    }
    private static void assertCorners(PreparedPrint.Pixels value, int[] expected, int tolerance, String message) {
        int[] positions = {0, value.width - 1, (value.height - 1) * value.width, value.height * value.width - 1};
        for (int i = 0; i < 4; i++)
            check(Math.abs((value.bytes[positions[i]] & 255) - expected[i]) <= tolerance, message);
    }
    @SuppressWarnings("deprecation")
    private static void resource(Context application, File png, int[] corners) throws Exception {
        Resources original = application.getResources();
        Resources resource = new Resources(original.getAssets(), original.getDisplayMetrics(), original.getConfiguration()) {
            @Override public InputStream openRawResource(int id) {
                try { return new FileInputStream(png); }
                catch (IOException error) { throw new Resources.NotFoundException("Fixture PNG missing"); }
            }
        };
        Context context = new ContextWrapper(application) {
            @Override public Context getApplicationContext() { return this; }
            @Override public Resources getResources() { return resource; }
        };
        try (ImageSource source = AndroidImages.fromResource(context, 1)) {
            assertCorners(prepare(PrintOptions.labelDots(128, 128).addImage(source, 0, 0).build()), corners, 0,
                "SDK resource adapter decodes without density scaling");
        }
        check(png.exists(), "Resource adapter does not own original resource fixture");
    }
    private static void alpha(File directory) throws Exception {
        File file = new File(directory, "alpha.png");
        Bitmap bitmap = Bitmap.createBitmap(3, 1, Bitmap.Config.ARGB_8888);
        try {
            bitmap.setPixel(0, 0, 0x00000000); bitmap.setPixel(1, 0, 0x80000000); bitmap.setPixel(2, 0, 0xff000000);
            write(bitmap, Bitmap.CompressFormat.PNG, file);
        } finally { bitmap.recycle(); }
        PreparedPrint.Pixels image = loadOwned(file);
        check((image.bytes[0] & 255) == 255 && (image.bytes[1] & 255) == 127 && (image.bytes[2] & 255) == 0,
            "SDK PNG alpha composites against white");
    }
    private static void boundedSampling(File directory) throws Exception {
        File file = new File(directory, "sampled.png");
        Bitmap bitmap = Bitmap.createBitmap(512, 1024, Bitmap.Config.ARGB_8888);
        try { bitmap.eraseColor(0xff000000); write(bitmap, Bitmap.CompressFormat.PNG, file); }
        finally { bitmap.recycle(); }
        try (ImageSource source = AndroidImages.takeFile(file)) {
            PreparedPrint.Pixels image = prepare(PrintOptions.labelDots(64, 64)
                .addImage(source, 0, 0, ImageRender.threshold(128).containBoxDots(64, 64)).build());
            check(image.width == 64 && image.height == 64 && (image.bytes[0] & 255) == 255 &&
                (image.bytes[32] & 255) == 0, "SDK sampled decode retains centered aspect without enlargement");
        } finally { awaitDeleted(file); }
    }
    private static void write(Bitmap bitmap, Bitmap.CompressFormat format, File file) throws IOException {
        try (OutputStream output = new FileOutputStream(file)) {
            check(bitmap.compress(format, 100, output), "Image fixture compression");
        }
    }
    private static void managerInvalidFile(Instrumentation instrumentation, File directory) throws Exception {
        AtomicInteger writes = new AtomicInteger(), completions = new AtomicInteger();
        BluetoothChannel channel = new BluetoothChannel() {
            private volatile boolean open;
            @Override public int open(IoControl control) { open = true; return 0; }
            @Override public int write(byte[] bytes, IoControl control) { writes.addAndGet(bytes.length); return bytes.length; }
            @Override public boolean isOpen() { return open; }
            @Override public void close() { open = false; }
        };
        PrinterManager.Factory factory = (application, address, options) -> {
            try {
                java.lang.reflect.Constructor<MobilePrinter> constructor = MobilePrinter.class.getDeclaredConstructor(BluetoothChannel.class, int.class);
                constructor.setAccessible(true); return constructor.newInstance(channel, 2);
            } catch (ReflectiveOperationException invalid) { throw new AssertionError(invalid); }
        };
        ExecutorService workers = pool(), controls = pool();
        PrinterManager manager = new PrinterManager(instrumentation.getTargetContext(), factory,
            ObserverService.mainExecutor(), workers, controls);
        try {
            CountDownLatch connected = new CountDownLatch(1);
            AtomicReference<ManagerResult<ConnectionInfo>> connection = new AtomicReference<>();
            manager.connect(ConnectionOptions.spp("AA:BB:CC:DD:EE:01"), 5_000,
                result -> { connection.set(result); connected.countDown(); }, null);
            check(connected.await(10, TimeUnit.SECONDS) && connection.get().code == 0, "SDK invalid-image test connection");
            CountDownLatch configured = new CountDownLatch(1);
            AtomicReference<ManagerResult<ConnectionInfo>> configuration = new AtomicReference<>();
            manager.configurePrintProfile(PrinterProfile.cpcl(203, 384), result -> {
                configuration.set(result); configured.countDown();
            });
            check(configured.await(10, TimeUnit.SECONDS) && configuration.get().code == 0,
                "SDK invalid-image test configures the connected printer");
            File invalid = new File(directory, "manager-invalid.png");
            try (OutputStream output = new FileOutputStream(invalid)) { output.write(new byte[16]); }
            CountDownLatch completed = new CountDownLatch(1);
            AtomicReference<ManagerResult<Void>> terminal = new AtomicReference<>();
            try (ImageSource source = AndroidImages.takeFile(invalid)) {
                manager.print(PrintOptions.labelDots(8, 1).addImage(source, 0, 0).build(), result -> {
                    completions.incrementAndGet(); terminal.set(result); completed.countDown();
                });
            }
            check(completed.await(10, TimeUnit.SECONDS), "SDK invalid-image terminal");
            ManagerResult<Void> result = terminal.get();
            check(result.outcome == ManagerResult.Outcome.FAILURE && result.code == 1001 &&
                result.completion == ManagerResult.Completion.NONE && !result.outputMayHaveOccurred && writes.get() == 0,
                "SDK built-in local decode failure is INVALID_ARGUMENT before printer output");
            awaitDeleted(invalid);
            instrumentation.waitForIdleSync();
            check(completions.get() == 1, "SDK invalid-image callback delivered once");
            File empty = new File(directory, "directory-is-not-an-image");
            check(empty.mkdir(), "Empty-directory input fixture");
            try {
                CountDownLatch rejected = new CountDownLatch(1);
                AtomicReference<ManagerResult<Void>> directoryResult = new AtomicReference<>();
                try (ImageSource source = AndroidImages.takeFile(empty)) {
                    manager.print(PrintOptions.labelDots(8, 1).addImage(source, 0, 0).build(), value -> {
                        directoryResult.set(value); rejected.countDown();
                    });
                }
                check(rejected.await(10, TimeUnit.SECONDS), "SDK directory-input terminal");
                ManagerResult<Void> invalidDirectory = directoryResult.get();
                check(invalidDirectory.code == 1001 && invalidDirectory.completion == ManagerResult.Completion.NONE &&
                    !invalidDirectory.outputMayHaveOccurred && writes.get() == 0, "Directory input fails before any printer output");
                // A normal close without decoding must also preserve the directory.
                AndroidImages.takeFile(empty).close();
                CountDownLatch released = new CountDownLatch(1);
                ImageSource.deferred(context -> PixelImage.copyOf(new byte[1], 1, 1, PixelImage.GRAY8), released::countDown).close();
                check(released.await(5, TimeUnit.SECONDS) && empty.isDirectory(),
                    "Owned-file cleanup never deletes a directory, including close without decoding");
            } finally { check(empty.delete(), "Empty-directory fixture cleanup"); }
        } finally {
            CountDownLatch closed = new CountDownLatch(1);
            manager.close(result -> closed.countDown());
            check(closed.await(10, TimeUnit.SECONDS), "SDK image-test manager cleanup");
            workers.shutdown(); controls.shutdown();
        }
    }
    private static ExecutorService pool() {
        return Executors.newCachedThreadPool(action -> {
            Thread thread = new Thread(action, "ImageAdapterTest"); thread.setDaemon(true); return thread;
        });
    }
    private static byte[] read(File file) throws IOException {
        try (InputStream input = new FileInputStream(file); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] block = new byte[4096]; int count;
            while ((count = input.read(block)) != -1) output.write(block, 0, count);
            return output.toByteArray();
        }
    }
    private static byte[] oriented(byte[] png, int orientation) throws IOException {
        byte[] tiff = {'I','I',42,0,8,0,0,0,1,0,18,1,3,0,1,0,0,0,(byte)orientation,0,0,0,0,0,0,0};
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            output.write(png, 0, png.length - 12);
            output.writeInt(tiff.length); byte[] type = {'e','X','I','f'};
            output.write(type); output.write(tiff);
            CRC32 crc = new CRC32(); crc.update(type); crc.update(tiff); output.writeInt((int)crc.getValue());
            output.write(png, png.length - 12, 12);
        }
        return bytes.toByteArray();
    }
    private static void awaitDeleted(File file) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (file.exists() && System.nanoTime() < deadline) Thread.sleep(5);
        check(!file.exists(), "SDK owned file released after actual reading: " + file.getName());
    }
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
