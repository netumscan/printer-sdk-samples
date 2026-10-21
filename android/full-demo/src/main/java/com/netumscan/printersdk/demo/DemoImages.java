// SPDX-License-Identifier: MIT
package com.netumscan.printersdk.demo;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 跨 Activity 配置重建保留的图片状态。界面状态在主线程更新，文件读写和图片处理在单独的 worker 执行。
 * 本类只准备图片，不提交 SDK 打印请求，也不访问打印机。
 */
final class DemoImages implements AutoCloseable {
    // 同进程的多个 Owner 共享会话标记，初始化时不会误删另一个活跃 Owner 的图片。
    static final long MAX_CACHE_BYTES = 128L * 1024 * 1024;
    private static final Object CACHE_IMPORT_LOCK = new Object();
    private static final String PROCESS_DIRECTORY = "session-" + UUID.randomUUID();
    /** 主线程发布的显示快照；Bitmap 由本类持有，界面只能读取，不能自行回收。 */
    static final class State {
        final boolean busy, imported;
        final String operation, error;
        final PreparedImage prepared;
        final Bitmap thumbnail, preview;
        final long revision, previewRevision;
        State(boolean busy, boolean imported, String operation, String error, PreparedImage prepared,
              Bitmap thumbnail, Bitmap preview, long revision, long previewRevision) {
            this.busy = busy; this.imported = imported; this.operation = operation; this.error = error;
            this.prepared = prepared; this.thumbnail = thumbnail; this.preview = preview;
            this.revision = revision; this.previewRevision = previewRevision;
        }
    }

    private final Context application;
    private final File root, directory;
    private final Handler main = new Handler(Looper.getMainLooper());
    // 新任务、取消和关闭都会换代；worker 与主线程接收结果时均据此排除过期任务。
    private final AtomicLong generation = new AtomicLong();
    private final ThreadPoolExecutor worker = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
        new ArrayBlockingQueue<>(1), task -> {
            Thread thread = new Thread(task, "PrinterDemo.images"); thread.setDaemon(true); return thread;
        });
    private Runnable changed;
    private DemoImageSource source = DemoImageSource.builtin();
    private PreparedImage prepared;
    private Bitmap thumbnail, preview;
    private boolean foreground, closed, busy, imported;
    private boolean initialized; // 仅由图片 worker 访问。
    private String operation = "", error;
    private long revision, previewRevision;
    private State state;

    /** 在主线程创建，并由 DemoOwner 跨 Activity 配置重建保留。 */
    DemoImages(Context context) {
        application = context.getApplicationContext();
        root = new File(application.getNoBackupFilesDir(), "full-demo-images");
        directory = new File(root, PROCESS_DIRECTORY);
        publish();
        worker.execute(this::initializeDirectory);
    }
    State state() { return state; }
    /** 仅供主线程借用；打印请求自行 acquire，来源替换不影响已提交的读取。 */
    DemoImageSource source() { return source; }
    void attach(Runnable listener) { if (!closed) { changed = listener; listener.run(); } }
    void detach(Runnable listener) { if (changed == listener) changed = null; }
    void foreground() { if (!closed) foreground = true; }
    void background() { foreground = false; cancel(); }

    void importImage(Uri uri) {
        if (uri == null) throw new IllegalArgumentException("请选择 PNG/JPEG 图片");
        start(uri, null, false, false);
    }
    void prepare(LabelConfig config) { start(null, config, false, false); }
    void preview(LabelConfig config) { start(null, config, false, true); }
    void useBuiltin() { start(null, null, true, false); }

    private void start(Uri uri, LabelConfig config, boolean builtin, boolean fullPreview) {
        if (closed || !foreground) return;
        if (uri == null && !builtin && config == null) throw new IllegalArgumentException("请填写打印参数");
        long version = generation.incrementAndGet();
        discardQueued();
        // 排队任务也持有独立来源引用；取消清空队列时须显式释放，不能只清除 Runnable。
        ImageTask task = new ImageTask(version, uri, config, builtin, fullPreview, source.acquire(), prepared, imported);
        busy = true; error = null;
        operation = uri != null ? "正在导入图片…" : fullPreview ? "正在生成预览…" : "正在准备图片…";
        worker.execute(task);
        publish();
    }

    private final class ImageTask implements Runnable, AutoCloseable {
        private final long version;
        private final Uri uri;
        private final LabelConfig config;
        private final boolean builtin, fullPreview, previousImported;
        private final DemoImageSource previousSource;
        private final PreparedImage previousPrepared;
        ImageTask(long version, Uri uri, LabelConfig config, boolean builtin, boolean fullPreview,
                  DemoImageSource previousSource, PreparedImage previousPrepared, boolean previousImported) {
            this.version = version; this.uri = uri; this.config = config; this.builtin = builtin;
            this.fullPreview = fullPreview; this.previousSource = previousSource;
            this.previousPrepared = previousPrepared; this.previousImported = previousImported;
        }
        @Override public void run() {
            try { process(version, uri, config, builtin, fullPreview, previousSource, previousPrepared, previousImported); }
            finally { close(); }
        }
        @Override public void close() { previousSource.close(); }
    }

    private void process(long version, Uri uri, LabelConfig config, boolean builtin, boolean fullPreview,
                         DemoImageSource previousSource, PreparedImage previousPrepared, boolean previousImported) {
        File candidate = null;
        DemoImageSource selected = null;
        Bitmap nextThumbnail = null, nextPreview = null;
        ImageCancellation cancelled = () -> generation.get() != version || Thread.currentThread().isInterrupted();
        try {
            initializeDirectory();
            if (cancelled.getAsBoolean()) throw new CancellationException();
            if (uri != null) {
                // 将 URI 保存为独立来源；导入不读取打印参数，也不自动生成预览。
                if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("Image directory unavailable");
                synchronized (CACHE_IMPORT_LOCK) {
                    // 包含已交给 SDK、尚未实际删除的旧来源；只拒绝新导入，不删除在用文件。
                    long available = MAX_CACHE_BYTES - cacheBytes(directory);
                    if (available <= 0) throw new IOException("Image cache limit");
                    candidate = File.createTempFile("source-", ".image", directory);
                    try (InputStream input = application.getContentResolver().openInputStream(uri)) {
                        if (input == null) throw new IOException("No image");
                        ImageImport.copy(input, candidate, cancelled, Math.min(ImageRaster.MAX_FILE_BYTES, available));
                    }
                }
                ImageImport.validate(candidate, cancelled);
                selected = DemoImageSource.ownedFile(candidate);
                candidate = null;
            } else {
                selected = builtin ? DemoImageSource.builtin() : previousSource.acquire();
            }
            // 只有显式预览/准备显示图时才创建 PreparedImage；打印始终从来源单独读取。
            PreparedImage result = null;
            if (uri == null && !builtin) {
                result = previousPrepared != null && previousPrepared.matches(config) ? previousPrepared :
                    new PreparedImage(config, selected.pixels(config, cancelled));
                nextThumbnail = ImageImport.thumbnail(result, cancelled);
                if (fullPreview) nextPreview = ImageImport.preview(result, cancelled);
            }
            if (cancelled.getAsBoolean()) throw new CancellationException();
            final DemoImageSource accepted = selected;
            final PreparedImage acceptedImage = result;
            final boolean acceptedImported = uri != null || (!builtin && previousImported);
            final Bitmap acceptedThumbnail = nextThumbnail, acceptedPreview = nextPreview;
            if (!main.post(() -> accept(version, acceptedImage, accepted, acceptedImported,
                acceptedThumbnail, acceptedPreview, fullPreview))) throw new CancellationException();
            // 资源交由主线程接收或丢弃，避免 finally 提前释放已发布的结果。
            selected = null; nextThumbnail = null; nextPreview = null;
        } catch (IOException | IllegalArgumentException | CancellationException | SecurityException failure) {
            main.post(() -> failed(version));
        } catch (OutOfMemoryError failure) {
            main.post(() -> failed(version));
        } finally {
            if (candidate != null) candidate.delete();
            if (selected != null) selected.close();
            recycle(nextThumbnail); recycle(nextPreview);
        }
    }

    private void accept(long version, PreparedImage result, DemoImageSource nextSource, boolean nextImported,
                        Bitmap nextThumbnail, Bitmap nextPreview, boolean fullPreview) {
        if (closed || !foreground || generation.get() != version) {
            // 迟到结果持有自己的来源引用，只释放本次所有权，不影响正在打印的读取。
            nextSource.close();
            recycle(nextThumbnail); recycle(nextPreview); return;
        }
        DemoImageSource oldSource = source;
        Bitmap oldThumbnail = thumbnail, oldPreview = preview;
        source = nextSource; imported = nextImported;
        prepared = result; thumbnail = nextThumbnail; preview = nextPreview;
        busy = false; operation = ""; error = null; revision++;
        if (fullPreview) previewRevision++;
        publish();
        recycle(oldThumbnail); recycle(oldPreview);
        oldSource.close();
    }
    /** 只更新当前任务的错误提示，保留上一张有效图片及其源文件。 */
    private void failed(long version) {
        if (closed || generation.get() != version) return;
        busy = false; operation = "";
        error = "图片准备失败；保留上一有效图片。请使用有效 PNG/JPEG（单文件 32 MiB、缓存合计 128 MiB、6400 万像素以内），画布不超过 800 万像素";
        publish();
    }
    /** 使进行中和排队的图片任务失效；保留有效图片，本类不会因此提交打印。 */
    void cancel() {
        if (closed) return;
        generation.incrementAndGet(); discardQueued();
        boolean wasBusy = busy;
        busy = false; operation = "";
        if (wasBusy) error = "图片准备已取消；保留上一有效图片，未提交打印";
        publish();
    }
    /** 打印参数改变时只清理显示图；来源仍可直接供下一次打印读取。 */
    void invalidate() {
        cancel();
        Bitmap oldThumbnail = thumbnail, oldPreview = preview;
        prepared = null; thumbnail = null; preview = null; error = null;
        publish();
        recycle(oldThumbnail); recycle(oldPreview);
    }
    /** 调用前须先清空对话框的 ImageView，再释放本类持有的完整预览。 */
    void clearPreview() {
        Bitmap old = preview; preview = null; publish(); recycle(old);
    }
    private void publish() {
        state = new State(busy, imported, operation, error, prepared, thumbnail, preview, revision, previewRevision);
        if (changed != null) changed.run();
    }
    private void discardQueued() {
        Runnable task;
        while ((task = worker.getQueue().poll()) != null)
            if (task instanceof ImageTask) ((ImageTask)task).close();
    }
    /** 在串行 worker 上初始化一次，并清理上次会话遗留的导入临时文件。 */
    private void initializeDirectory() {
        if (initialized) return;
        initialized = true;
        if (!directory.isDirectory() && !directory.mkdirs()) return;
        File[] abandoned = root.listFiles();
        if (abandoned != null) for (File file : abandoned) {
            if (file.isFile() && sourceFile(file)) file.delete();
            else if (file.isDirectory() && file.getName().startsWith("session-") && !file.getName().equals(PROCESS_DIRECTORY)) {
                File[] sources = file.listFiles();
                if (sources != null) for (File stale : sources) if (stale.isFile() && sourceFile(stale)) stale.delete();
                file.delete();
            }
        }
    }
    private static long cacheBytes(File directory) throws IOException {
        File[] files = directory.listFiles();
        if (files == null) throw new IOException("Image cache unavailable");
        long size = 0;
        for (File file : files) if (file.isFile() && sourceFile(file)) {
            size += file.length();
            if (size >= MAX_CACHE_BYTES) return MAX_CACHE_BYTES;
        }
        return size;
    }
    private static boolean sourceFile(File file) {
        return file.getName().startsWith("source-") && file.getName().endsWith(".image");
    }
    private static void recycle(Bitmap bitmap) { if (bitmap != null && !bitmap.isRecycled()) bitmap.recycle(); }
    /** 幂等关闭；释放本 Owner 的来源引用，打印读取结束后才会删除私有文件。 */
    @Override public void close() {
        if (closed) return;
        generation.incrementAndGet(); closed = true; foreground = false; changed = null;
        discardQueued();
        source.close();
        imported = false;
        prepared = null; busy = false; operation = "";
        recycle(thumbnail); recycle(preview); thumbnail = null; preview = null;
        worker.shutdown();
        publish();
    }
}
