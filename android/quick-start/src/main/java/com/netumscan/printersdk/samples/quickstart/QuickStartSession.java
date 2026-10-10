// SPDX-License-Identifier: MIT
package com.netumscan.printersdk.samples.quickstart;

import android.content.Context;
import com.netumscan.printersdk.CpclProfile;
import com.netumscan.printersdk.MobilePrinter;
import com.netumscan.printersdk.PrintOperation;
import com.netumscan.printersdk.PrinterException;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Application worker owns every SDK call; the SDK owns the device IO queue. */
final class QuickStartSession implements AutoCloseable {
    interface Listener { void changed(String message); }
    private final Context context;
    private final Listener listener;
    private final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor();
    private final AtomicBoolean closing = new AtomicBoolean();
    // These fields are accessed only by worker.
    private MobilePrinter printer;
    private PrintOperation operation;
    private int width;
    private long lastBytes = -1;
    private int lastState = -1;

    QuickStartSession(Context context, Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
        worker.scheduleWithFixedDelay(this::poll, 100, 100, TimeUnit.MILLISECONDS);
    }

    void connect(String address, int dpi, int widthDots) {
        execute(() -> {
            if (printer != null) throw new IllegalStateException("请先关闭当前连接");
            if (!address.matches("(?i)[0-9a-f]{2}(:[0-9a-f]{2}){5}"))
                throw new IllegalArgumentException("请填写已配对设备的蓝牙地址，例如 AA:BB:CC:DD:EE:FF");
            CpclProfile profile = new CpclProfile(dpi, widthDots, 65535, false);
            listener.changed("正在连接 SPP / CPCL…");
            // Pair in Android Settings first. A sample profile is not a hardware guarantee.
            printer = MobilePrinter.spp(context, address, profile);
            width = widthDots;
            printer.connect(10000);
            listener.changed("已连接；请确认纸张和参数后点击打印");
        });
    }

    void print() {
        execute(() -> {
            requireConnected();
            if (operation != null) throw new IllegalStateException("请等待当前任务结束");
            int height = 160;
            byte[] pixels = new byte[Math.multiplyExact(width, height)];
            Arrays.fill(pixels, (byte)255);
            // Small Gray8 bars; the app renders pixels and the SDK encodes CPCL.
            for (int y = 24; y < 136; y++) {
                for (int x = 16; x < width - 16; x++) {
                    if ((x / 12) % 2 == 0) pixels[y * width + x] = 0;
                }
            }
            operation = printer.submitLabelGray8(pixels, width, height, 1, 0, 0, 0, false, 30000);
            lastBytes = -1; lastState = -1;
            listener.changed("任务已入队；正在发送…");
        });
    }

    void connectionState() {
        execute(() -> {
            requireConnected();
            listener.changed("连接缓存状态=" + printer.connectionState()
                + "（2=CONNECTED；此值不代表纸张、就绪或打印完成）");
        });
    }

    void disconnect() {
        execute(() -> {
            release();
            listener.changed("已关闭；不会自动重连或重放打印任务");
        });
    }

    private void requireConnected() {
        if (printer == null) throw new IllegalStateException("请先连接打印机");
    }

    private void execute(Runnable action) {
        if (closing.get()) return;
        worker.execute(() -> {
            if (closing.get()) return;
            try { action.run(); }
            catch (IllegalArgumentException | IllegalStateException error) { listener.changed(error.getMessage()); }
            catch (RuntimeException error) { failed(error); }
        });
    }

    private void poll() {
        if (closing.get() || operation == null) return;
        try {
            PrintOperation.Snapshot snapshot = operation.snapshot();
            if (snapshot.terminal()) {
                operation.close(); operation = null;
                if (snapshot.state == 4 && snapshot.completion == 1) {
                    listener.changed("DATA_SENT：数据已发送；实际出纸和打印效果需在打印机上确认");
                } else {
                    release();
                    listener.changed("任务结束：state=" + snapshot.state + " error=" + snapshot.error
                        + "；已发送数据无法撤回，请检查出纸后手动重连");
                }
            } else if (snapshot.bytesSent != lastBytes || snapshot.state != lastState) {
                lastBytes = snapshot.bytesSent; lastState = snapshot.state;
                listener.changed("state=" + snapshot.state + "，已发送=" + snapshot.bytesSent
                    + " bytes，总计=" + (snapshot.bytesTotalKnown() ? Long.toString(snapshot.bytesTotal) : "未知"));
            }
        } catch (RuntimeException error) { failed(error); }
    }

    private void failed(RuntimeException error) {
        release();
        listener.changed(error instanceof PrinterException ? "SDK error=" + ((PrinterException)error).code
            + "；" + error.getMessage() : error instanceof SecurityException
            ? "蓝牙权限不可用；请在系统设置授权后手动重连" : error.getMessage());
    }

    private void release() {
        // Close the device before releasing the operation result. No automatic replay.
        try { if (printer != null) printer.close(); }
        finally {
            printer = null;
            if (operation != null) { operation.close(); operation = null; }
        }
    }

    @Override public void close() {
        if (!closing.compareAndSet(false, true)) return;
        worker.execute(this::release);
        worker.shutdown(); // Pending device cleanup stays on the worker, never the UI thread.
    }
}
