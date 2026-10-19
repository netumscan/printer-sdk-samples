// SPDX-License-Identifier: MIT
package com.netumscan.printersdk.samples.quickstart;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.netumscan.printersdk.*;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/** Main-thread session owner. Only image preparation/submission runs on worker; SDK owns IO ordering. */
final class QuickStartSession implements AutoCloseable {
    interface Listener { void changed(String message); }
    private final Context context;
    private final Listener listener;
    private final PrinterManager manager = new PrinterManager();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor();
    private final AtomicBoolean closing = new AtomicBoolean();
    private MobilePrinter printer;
    private PrintOperation operation;
    private CpclProfile profile;
    private Subscription connection, state, completion;
    private boolean ready, submitting, releasing, cancelPending;
    private AtomicBoolean preparationCancellation;
    private long epoch, connectionGeneration;

    QuickStartSession(Context context, Listener listener) {
        requireMain(); this.context = context.getApplicationContext(); this.listener = listener;
        SdkInfo info = MobilePrinter.sdkInfo();
        if (info.abiVersion != 3 || info.abiRevision != 1 || !"0.3.0".equals(info.nativeVersion)
                || !"0.3.0-preview.2".equals(info.distributionVersion)) throw new PrinterException(1006);
    }
    void connect(String address, int dpi, int widthDots) {
        requireMain();
        try {
            if (closing.get() || printer != null || releasing) throw new IllegalStateException("请先关闭当前连接");
            profile = new CpclProfile(dpi, widthDots, 65535, false);
            MobilePrinter created = manager.spp(context, address);
            printer = created; ready = false; connectionGeneration = 0;
            long version = ++epoch;
            state = created.observeState(value -> {
                if (!current(version, created) || value.connectionGeneration < connectionGeneration) return;
                connectionGeneration = value.connectionGeneration;
                if (value.connectionState != MobilePrinter.CONNECTED) ready = false;
                show(stateMessage(value));
            });
            connection = manager.connect(created, 10000, failure -> {
                if (!current(version, created)) return;
                if (failure != null) { fail(failure); return; }
                try {
                    DeviceStateSnapshot value = created.stateSnapshot();
                    ready = value.connectionState == MobilePrinter.CONNECTED;
                    connectionGeneration = value.connectionGeneration;
                    if (!ready) throw new PrinterException(2002);
                    show("已连接；" + stateMessage(value));
                } catch (RuntimeException error) { fail(error); }
            });
            show("正在连接 SPP / CPCL…");
        } catch (RuntimeException error) { fail(error); }
    }
    void print() {
        requireMain();
        if (!ready || submitting || operation != null || closing.get()) {
            show("请先连接并等待当前任务结束"); return;
        }
        final MobilePrinter target = printer;
        final CpclProfile selected = profile;
        final long version = epoch;
        final AtomicBoolean cancelled = new AtomicBoolean();
        preparationCancellation = cancelled;
        submitting = true; cancelPending = false;
        show("正在准备测试图…");
        worker.execute(() -> {
            try {
                if (closing.get()) return;
                PrintOperation next = prepareAndSubmit(cancelled, () -> {
                    int width = Math.min(selected.widthDots, 384), height = 64;
                    byte[] pixels = testImage(width, height);
                    checkCancellation(cancelled);
                    try (PrinterImage image = PrinterImage.gray8(pixels, width, height)) {
                        return PrintJob.label(width, height, selected.dpi).addImage(image, 0, 0).build();
                    }
                }, job -> target.submit(job, selected, 30000));
                // Cancellation may race the final check/admission. Interrupt immediately,
                // without waiting for the main dispatcher to publish the caller handle.
                boolean posted = false;
                try {
                    if (cancelled.get()) next.cancel();
                    posted = main.post(() -> publish(next, version, target));
                } finally {
                    if (!posted) { next.close(); manager.close(); worker.shutdown(); closing.set(true); }
                }
            } catch (CancellationException cancelledPreparation) {
                main.post(() -> {
                    if (current(version, target)) { submitting = false; preparationCancellation = null; show("已取消准备；未提交打印任务"); }
                });
            } catch (RuntimeException error) {
                main.post(() -> { if (current(version, target)) { submitting = false; fail(error); } });
            }
        });
    }
    private void publish(PrintOperation next, long version, MobilePrinter target) {
        if (!current(version, target)) { try { next.cancel(); } finally { next.close(); } return; }
        submitting = false; preparationCancellation = null; operation = next;
        try {
            completion = next.onComplete(result -> {
                if (!current(version, target) || operation != next) return;
                releaseOperation(); show(terminalMessage(result.snapshot));
            });
            if (cancelPending) next.cancel();
            show("任务已入队；通过状态订阅和完成回调接收结果");
        } catch (RuntimeException error) { fail(error); }
    }
    void cancel() {
        requireMain(); cancelPending = true;
        if (preparationCancellation != null) preparationCancellation.set(true);
        try { if (operation != null) operation.cancel(); show("已请求取消；已发送的数据无法撤回"); }
        catch (RuntimeException error) { fail(error); }
    }
    void connectionState() {
        requireMain();
        try { show(printer == null ? "尚未连接" : stateMessage(printer.stateSnapshot())); }
        catch (RuntimeException error) { fail(error); }
    }
    void disconnect() {
        requireMain();
        if (preparationCancellation != null) preparationCancellation.set(true);
        preparationCancellation = null;
        ++epoch; ready = false; submitting = false; cancelPending = false;
        if (connection != null) { connection.close(); connection = null; }
        if (state != null) { state.close(); state = null; }
        releaseOperation();
        MobilePrinter previous = printer; printer = null;
        if (previous != null) {
            releasing = true;
            manager.release(previous, failure -> {
                releasing = false;
                show(failure == null ? "已关闭；不会自动重连或重放" : "关闭失败：" + failure.getMessage());
            });
        }
    }
    private void releaseOperation() {
        if (completion != null) { completion.close(); completion = null; }
        if (operation != null) { operation.close(); operation = null; }
    }
    private boolean current(long version, MobilePrinter target) {
        return !closing.get() && epoch == version && printer == target;
    }
    private void fail(Throwable failure) {
        disconnect();
        show(failure instanceof PrinterException ? "SDK error=" + ((PrinterException)failure).code + "；" + failure.getMessage()
            : failure instanceof SecurityException ? "蓝牙权限不可用；请授权后手动重试" : failure.getMessage());
    }
    private void show(String message) { if (!closing.get()) listener.changed(message); }
    interface JobPreparation { PrintJob prepare(); }
    interface JobSubmission { PrintOperation submit(PrintJob job); }
    static PrintOperation prepareAndSubmit(AtomicBoolean cancelled, JobPreparation preparation, JobSubmission submission) {
        checkCancellation(cancelled);
        try (PrintJob job = preparation.prepare()) {
            checkCancellation(cancelled);
            return submission.submit(job);
        }
    }
    private static void checkCancellation(AtomicBoolean cancelled) {
        if (cancelled.get()) throw new CancellationException("Preparation cancelled");
    }
    static byte[] testImage(int width, int height) {
        long count = (long)width * height;
        if (width <= 0 || height <= 0 || count > Integer.MAX_VALUE) throw new IllegalArgumentException("Invalid image dimensions");
        byte[] pixels = new byte[(int)count]; Arrays.fill(pixels, (byte)255);
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++)
            if ((x / 16 + y / 16) % 2 == 0) pixels[y * width + x] = 0;
        return pixels;
    }
    static String terminalMessage(PrintOperation.Snapshot value) {
        if (value.state == 4 && value.error == 0 && value.completion == 1)
            return "DATA_SENT：" + value.bytesSent + " bytes；请检查实际出纸";
        return "任务结束：state=" + value.state + " error=" + value.error + "；已发送=" + value.bytesSent + " bytes";
    }
    private static String stateMessage(DeviceStateSnapshot value) {
        String[] connections = {"未连接", "连接中", "已连接", "断开中"};
        String[] work = {"空闲", "排队", "准备", "发送"};
        return "连接=" + connections[value.connectionState] + "；工作=" + work[value.workState]
            + "；等待队列=" + value.queuedCount + "；硬件=未知";
    }
    @Override public void close() {
        requireMain(); if (closing.get()) return;
        disconnect(); closing.set(true); manager.close(); worker.shutdown();
    }
    private static void requireMain() {
        if (Looper.myLooper() != Looper.getMainLooper()) throw new IllegalStateException("Call on the main thread");
    }
}
