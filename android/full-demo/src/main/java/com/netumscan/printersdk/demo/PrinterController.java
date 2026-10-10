package com.netumscan.printersdk.demo;

import java.util.concurrent.*;
import java.util.function.BooleanSupplier;

/** Demo lifecycle orchestration only. SDK owns the actual device IO queue. */
final class PrinterController implements AutoCloseable {
    enum Phase { OFFLINE, CONNECTING, READY, PRINTING, STOPPING, CLOSED }
    interface Listener { void changed(Phase phase, String message); void log(String message); }
    interface Factory { Session create(); }
    interface Renderer { byte[] render(BooleanSupplier cancelled); }
    interface Session extends AutoCloseable {
        void connect();
        default boolean supportsMedia() { return true; }
        default boolean supportsLabelPositioning() { return true; }
        default long submit(byte[] pixels, int width, int height, int copies) {
            throw new IllegalArgumentException("需要完整的打印配置");
        }
        default long submit(byte[] pixels, LabelConfig config) {
            return submit(pixels, config.labelWidthDots, config.height, config.copies);
        }
        default long mediaControl(int mode) { throw new UnsupportedOperationException("CPCL control unavailable"); }
        default long raw(byte[] bytes, int copies) { throw new UnsupportedOperationException("Raw unavailable"); }
        Update[] poll();
        void cancel(long id);
        void close();
    }
    static final class Update {
        final long id;
        final int state, completion, error, byteCountFlags;
        final long current, total;
        final String errorDescription;
        Update(long id, int state, int completion, int error) {
            this(id, state, completion, error, 0, 0, "错误码 " + error);
        }
        Update(long id, int state, int completion, int error, long current, long total, String errorDescription) {
            this(id, state, completion, error, current, total, errorDescription, 0);
        }
        Update(long id, int state, int completion, int error, long current, long total,
               String errorDescription, int byteCountFlags) {
            this.byteCountFlags = byteCountFlags;
            this.id = id; this.state = state; this.completion = completion; this.error = error;
            this.current = current; this.total = total; this.errorDescription = errorDescription;
        }
    }
    private final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor();
    private final ScheduledExecutorService control = Executors.newSingleThreadScheduledExecutor();
    private final Listener listener;
    private Phase phase = Phase.OFFLINE;
    private Session session;
    private long epoch, task;
    private final long taskTimeoutNanos;
    private long deadline;
    PrinterController(Listener listener) {
        this(listener, 120_000);
    }
    PrinterController(Listener listener, long taskTimeoutMs) {
        this.listener = listener;
        taskTimeoutNanos = TimeUnit.MILLISECONDS.toNanos(taskTimeoutMs);
        worker.scheduleWithFixedDelay(this::poll, 100, 100, TimeUnit.MILLISECONDS);
        control.scheduleWithFixedDelay(this::checkDeadline, 100, 100, TimeUnit.MILLISECONDS);
    }
    private void changed(Phase next, String message) { phase = next; listener.changed(next, message); }
    synchronized Phase phase() { return phase; }
    synchronized boolean supportsMedia() { return session != null && session.supportsMedia(); }
    synchronized boolean supportsLabelPositioning() { return session != null && session.supportsLabelPositioning(); }
    synchronized void connect(Factory factory) {
        if (phase != Phase.OFFLINE) throw new IllegalStateException("请先断开当前连接");
        long version = ++epoch;
        changed(Phase.CONNECTING, "正在连接…");
        worker.execute(() -> {
            Session candidate = null;
            try {
                candidate = factory.create();
                boolean abandoned;
                synchronized (this) {
                    abandoned = version != epoch;
                    if (!abandoned) session = candidate;
                }
                if (abandoned) { candidate.close(); return; }
                candidate.connect();
                synchronized (this) {
                    if (version == epoch) changed(Phase.READY, "已连接；请设置画布与纸张，并预览后打印");
                }
            } catch (RuntimeException error) { fail(version, error); }
        });
    }
    synchronized void print(Renderer renderer, int width, int height, int copies) {
        print(renderer, width, height, copies, null);
    }
    synchronized void print(Renderer renderer, LabelConfig config) {
        print(renderer, config.labelWidthDots, config.height, config.copies, config);
    }
    private synchronized void print(Renderer renderer, int width, int height, int copies, LabelConfig config) {
        if (phase != Phase.READY) throw new IllegalStateException("请先连接打印机");
        long version = epoch;
        Session selected = session;
        task = 0; deadline = System.nanoTime() + taskTimeoutNanos;
        changed(Phase.PRINTING, "正在准备打印数据…");
        worker.execute(() -> {
            try {
                byte[] pixels = renderer.render(() -> stale(version));
                synchronized (this) { if (version != epoch) return; }
                long id = config == null ? selected.submit(pixels, width, height, copies) : selected.submit(pixels, config);
                synchronized (this) {
                    if (version != epoch) return;
                    task = id; changed(Phase.PRINTING, "任务 " + id + " 已入队，正在发送…");
                    listener.log("task=" + id + " copies=" + copies);
                }
            } catch (RuntimeException error) { fail(version, error); }
        });
    }
    // 0 = locate next label; 1..3 = the explicitly selected CPCL media mode.
    synchronized void mediaControl(int mode) {
        if (phase != Phase.READY) throw new IllegalStateException("请先连接打印机");
        if (mode < 0 || mode > 3) throw new IllegalArgumentException("无效介质模式");
        long version = epoch;
        Session selected = session;
        task = 0; deadline = System.nanoTime() + taskTimeoutNanos;
        changed(Phase.PRINTING, mode == 0 ? "正在发送标签定位指令…" : "正在发送介质设置…");
        worker.execute(() -> {
            try {
                if (stale(version)) return;
                long id = selected.mediaControl(mode);
                synchronized (this) {
                    if (version != epoch) return;
                    task = id;
                    listener.log("task=" + id + " CPCL control=" + mode + "；需实物确认生效");
                }
            } catch (RuntimeException error) { fail(version, error); }
        });
    }
    private synchronized boolean stale(long version) { return epoch != version; }
    synchronized void sendRaw(byte[] bytes, int copies) {
        if (phase != Phase.READY) throw new IllegalStateException("请先连接打印机");
        RawCommand command = new RawCommand(bytes, copies);
        long version = epoch; Session selected = session;
        task = 0; deadline = System.nanoTime() + taskTimeoutNanos;
        changed(Phase.PRINTING, "正在提交 Raw 多份操作…");
        worker.execute(() -> {
            try {
                if (stale(version)) return;
                long id = selected.raw(command.copyBytes(), command.copies);
                synchronized (this) {
                    if (version != epoch) return;
                    task = id;
                    listener.log("task=" + id + " Raw copies=" + command.copies + " bytes=" + command.size());
                }
            } catch (RuntimeException error) { fail(version, error); }
        });
    }
    synchronized void cancel() {
        if (phase != Phase.PRINTING) return;
        if (task == 0) { disconnect(); return; }
        long version = epoch, id = task;
        Session selected = session;
        listener.log("取消任务 " + id + "；已发送的数据无法撤回");
        control.execute(() -> {
            if (stale(version)) return;
            try { selected.cancel(id); } catch (RuntimeException error) { fail(version, error); }
        });
    }
    synchronized void disconnect() {
        disconnect("已断开；不会自动重印");
    }
    private synchronized void disconnect(String message) {
        if (phase == Phase.CLOSED || phase == Phase.OFFLINE || phase == Phase.STOPPING) return;
        long version = ++epoch;
        Session selected = session; session = null; task = 0;
        changed(Phase.STOPPING, "正在断开…");
        control.execute(() -> {
            try { if (selected != null) selected.close(); }
            finally {
                synchronized (this) { if (version == epoch) changed(Phase.OFFLINE, message); }
            }
        });
    }
    private synchronized void checkDeadline() {
        if (phase == Phase.PRINTING && System.nanoTime() - deadline >= 0) {
            listener.log("任务超过演示应用截止时间；断开且不重放");
            disconnect("任务超时；请检查出纸后手动重连");
        }
    }
    private void poll() {
        Session selected; long version;
        synchronized (this) {
            if (session == null || (phase != Phase.READY && phase != Phase.PRINTING)) return;
            selected = session; version = epoch;
        }
        try {
            for (Update event : selected.poll()) {
                synchronized (this) {
                    if (version != epoch) return;
                    listener.log("task=" + event.id + " state=" + event.state +
                        " completion=" + event.completion + " bytesSent=" + event.current +
                        " bytesTotal=" + ((event.byteCountFlags & 2) == 0 ? "unknown" : Long.toString(event.total)) +
                        " byteCountFlags=" + event.byteCountFlags + " error=" + event.error);
                    if (event.id != task) continue;
                    if (event.state < 4) {
                        String count = ((event.byteCountFlags & 1) == 0 ? "至少 " : "") + event.current;
                        String total = (event.byteCountFlags & 2) == 0 ? "（总字节数未知）" : "/" + event.total;
                        changed(Phase.PRINTING, "任务 " + event.id + "：已发送字节 " + count + total);
                        continue;
                    }
                    if (event.state == 4 && event.completion == 1) {
                        task = 0; changed(Phase.READY, "DATA_SENT · 数据已发送，设置与出纸结果需实物确认");
                    } else {
                        String message = event.state == 6 ? "任务已取消；已发送数据无法撤回" : "任务失败：" + event.errorDescription;
                        listener.log(message); disconnect(message); return;
                    }
                }
            }
        } catch (RuntimeException error) { fail(version, error); }
    }
    private synchronized void fail(long version, RuntimeException error) {
        if (version != epoch) return;
        String message = error instanceof com.netumscan.printersdk.PrinterException ?
            "SDK 错误码 " + ((com.netumscan.printersdk.PrinterException)error).code + "；" +
                com.netumscan.printersdk.PrinterException.description(((com.netumscan.printersdk.PrinterException)error).code) :
            error instanceof SecurityException ? "蓝牙访问权限不可用；请在系统应用设置中授权后重试" :
            "操作失败；请检查设备电源、蓝牙与配置后手动重连";
        listener.log(message); disconnect(message);
    }
    public synchronized void close() {
        if (phase == Phase.CLOSED) return;
        ++epoch;
        Session selected = session; session = null;
        changed(Phase.CLOSED, "会话已关闭");
        control.execute(() -> { if (selected != null) selected.close(); });
        worker.shutdownNow(); control.shutdown();
    }
}
