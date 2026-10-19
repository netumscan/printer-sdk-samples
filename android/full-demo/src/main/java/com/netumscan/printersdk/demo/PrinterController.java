package com.netumscan.printersdk.demo;

import com.netumscan.printersdk.DeviceStateSnapshot;
import com.netumscan.printersdk.MobilePrinter;
import com.netumscan.printersdk.Diagnostic;
import com.netumscan.printersdk.ResourceSnapshot;
import com.netumscan.printersdk.SdkInfo;
import java.util.concurrent.*;

/** Demo lifecycle orchestration only. SDK owns the actual device IO queue. */
final class PrinterController implements AutoCloseable {
    enum Phase { OFFLINE, CONNECTING, READY, PRINTING, STOPPING, CLOSED }
    interface Listener {
        void changed(Phase phase, String message);
        void log(String message);
        default void deviceStateChanged(DeviceStateSnapshot snapshot) { }
        default void deviceDetailsChanged(ResourceSnapshot resources, Diagnostic diagnostic) { }
    }
    interface Factory { Session create(); }
    interface Owner { Future<Void> close(); }
    interface Cancellation { boolean getAsBoolean(); }
    interface Renderer { byte[] render(Cancellation cancelled); }
    interface Observation {
        void state(DeviceStateSnapshot snapshot, ResourceSnapshot resources, Diagnostic diagnostic);
        void completed(Update update);
    }
    interface Session extends AutoCloseable {
        void connect();
        default DeviceStateSnapshot stateSnapshot() { return null; }
        default ResourceSnapshot resourceSnapshot() { return null; }
        default Diagnostic diagnostic() { return null; }
        default boolean supportsMedia() { return true; }
        default boolean supportsLabelPositioning() { return true; }
        default long submit(byte[] pixels, int width, int height, int copies) {
            throw new IllegalArgumentException("需要完整的打印配置");
        }
        default long submit(byte[] pixels, LabelConfig config) {
            return submit(pixels, config.labelWidthDots, config.height, config.copies);
        }
        default long mediaControl(int mode, LabelConfig config) { throw new UnsupportedOperationException("CPCL control unavailable"); }
        default long raw(byte[] bytes, int copies) { throw new UnsupportedOperationException("Raw unavailable"); }
        default void observe(Executor dispatcher, Observation observer) { }
        void cancel(long id);
        void close();
    }
    static final class Update {
        final long id;
        final int state, completion, error, byteCountFlags;
        final long current, total;
        final String errorDescription;
        final Diagnostic diagnostic;
        Update(long id, int state, int completion, int error) {
            this(id, state, completion, error, 0, 0, "错误码 " + error);
        }
        Update(long id, int state, int completion, int error, long current, long total, String errorDescription) {
            this(id, state, completion, error, current, total, errorDescription, 0);
        }
        Update(long id, int state, int completion, int error, long current, long total,
               String errorDescription, int byteCountFlags) {
            this(id, state, completion, error, current, total, errorDescription, byteCountFlags, null);
        }
        Update(long id, int state, int completion, int error, long current, long total,
               String errorDescription, int byteCountFlags, Diagnostic diagnostic) {
            this.byteCountFlags = byteCountFlags;
            this.id = id; this.state = state; this.completion = completion; this.error = error;
            this.current = current; this.total = total; this.errorDescription = errorDescription;
            this.diagnostic = diagnostic;
        }
    }
    private final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor();
    private final ScheduledExecutorService control = Executors.newSingleThreadScheduledExecutor();
    private final Listener listener;
    private final Owner owner;
    private Phase phase = Phase.OFFLINE;
    private Session session;
    private DeviceStateSnapshot deviceState;
    private boolean connectedStateObserved;
    private long minimumStateGeneration = -1;
    private ResourceSnapshot resources;
    private Diagnostic diagnostic;
    private Future<?> connecting;
    private long epoch, task;
    private final long taskTimeoutNanos;
    private long deadline;
    private ScheduledFuture<?> deadlineCheck;
    private Update earlyTerminal;
    PrinterController(Listener listener) {
        this(listener, 120_000, () -> null);
    }
    PrinterController(Listener listener, long taskTimeoutMs) {
        this(listener, taskTimeoutMs, () -> null);
    }
    PrinterController(Listener listener, Owner owner) {
        this(listener, 120_000, owner);
    }
    private PrinterController(Listener listener, long taskTimeoutMs, Owner owner) {
        this.listener = listener; this.owner = owner;
        taskTimeoutNanos = TimeUnit.MILLISECONDS.toNanos(taskTimeoutMs);

    }
    private void changed(Phase next, String message) { phase = next; listener.changed(next, message); }
    synchronized Phase phase() { return phase; }
    synchronized DeviceStateSnapshot deviceState() { return deviceState; }
    synchronized ResourceSnapshot resourceSnapshot() { return resources; }
    synchronized Diagnostic diagnostic() { return diagnostic; }
    static String resourceStatusText(ResourceSnapshot snapshot) {
        return snapshot == null ? "资源占用未知" : "载荷 " + snapshot.pendingPayloadBytes + "/" +
            snapshot.maxPendingPayloadBytes + " 字节 · 操作 " + snapshot.outstandingOperations + "/" + snapshot.maxPendingOperations;
    }
    static String diagnosticText(Diagnostic value) {
        if (value == null || value.source == Diagnostic.NONE) return "暂无诊断";
        String message = value.errorCode == 0 ? "主动断开" : "SDK 错误码 " + value.errorCode + "；" + value.message;
        return message + " · 连接代次 " + value.connectionGeneration +
            (value.hasNativeCode ? " · " + value.domain + "=" + value.nativeCode : "");
    }
    static String sdkInfoText(SdkInfo info) {
        return "SDK " + info.nativeVersion + " · " + info.distributionVersion + " · ABI " + info.abiVersion + "/r" + info.abiRevision +
            "\n构建标识：" + (info.buildId == null ? "未知" : info.buildId);
    }
    static String deviceStatusText(DeviceStateSnapshot snapshot, Phase phase) {
        if (snapshot == null) {
            String connection = phase == Phase.CONNECTING ? "正在连接" : phase == Phase.STOPPING ? "正在断开" :
                phase == Phase.CLOSED ? "会话已关闭" : phase == Phase.OFFLINE ? "未连接" : "连接状态未知";
            return connection + " · 工作状态未知 · 等待数量未知 · 硬件未知";
        }
        String connection;
        switch (snapshot.connectionState) {
        case MobilePrinter.DISCONNECTED: connection = "未连接"; break;
        case MobilePrinter.CONNECTING: connection = "正在连接"; break;
        case MobilePrinter.CONNECTED: connection = "已连接"; break;
        case MobilePrinter.DISCONNECTING: connection = "正在断开"; break;
        default: connection = "连接状态未知";
        }
        String work;
        switch (snapshot.workState) {
        case DeviceStateSnapshot.WORK_IDLE: work = "无待处理任务"; break;
        case DeviceStateSnapshot.WORK_QUEUED: work = "等待执行"; break;
        case DeviceStateSnapshot.WORK_PREPARING: work = "正在准备"; break;
        case DeviceStateSnapshot.WORK_SENDING: work = "正在发送"; break;
        default: work = "工作状态未知";
        }
        return connection + " · " + work + " · 等待 " + snapshot.queuedCount + " 项 · 硬件未知";
    }
    synchronized boolean supportsMedia() { return session != null && session.supportsMedia(); }
    synchronized boolean supportsLabelPositioning() { return session != null && session.supportsLabelPositioning(); }
    synchronized void connect(Factory factory) {
        if (phase != Phase.OFFLINE) throw new IllegalStateException("请先断开当前连接");
        long version = ++epoch; connectedStateObserved = false; minimumStateGeneration = -1;
        changed(Phase.CONNECTING, "正在连接…");
        connecting = worker.submit(() -> {
            Session candidate = null;
            try {
                candidate = factory.create();
                boolean abandoned;
                synchronized (this) {
                    abandoned = version != epoch;
                    if (!abandoned) session = candidate;
                }
                if (abandoned) { candidate.close(); return; }
                Session observed = candidate;
                candidate.observe(control, new Observation() {
                    public void state(DeviceStateSnapshot snapshot, ResourceSnapshot resources, Diagnostic diagnostic) {
                        receiveState(version, observed, snapshot, resources, diagnostic);
                    }
                    public void completed(Update update) { receiveUpdate(version, update); }
                });
                candidate.connect();
                DeviceStateSnapshot current = null;
                try { current = candidate.stateSnapshot(); } catch (RuntimeException unavailable) { /* Observation does not change the connect outcome. */ }
                synchronized (this) {
                    if (version == epoch) {
                        if (current != null) minimumStateGeneration = Math.max(minimumStateGeneration,current.connectionGeneration);
                        changed(Phase.READY, "已连接；请设置画布与纸张，并预览后打印");
                        if (current != null) receiveState(version,candidate,current,resources,diagnostic);
                    }
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
        task = 0; earlyTerminal = null; deadline = System.nanoTime() + taskTimeoutNanos;
        if (deadlineCheck != null) deadlineCheck.cancel(false);
        deadlineCheck = control.schedule(this::checkDeadline, taskTimeoutNanos, TimeUnit.NANOSECONDS);
        changed(Phase.PRINTING, "正在准备打印数据…");
        worker.execute(() -> {
            try {
                byte[] pixels = renderer.render(() -> stale(version));
                synchronized (this) { if (version != epoch) return; }
                long id = config == null ? selected.submit(pixels, width, height, copies) : selected.submit(pixels, config);
                synchronized (this) {
                    if (version != epoch) return;
                    task = id; changed(Phase.PRINTING, "任务 " + id + " 已入队，正在发送…");
                    deliverEarlyTerminal(version, id);
                    listener.log("task=" + id + " copies=" + copies);
                }
            } catch (RuntimeException error) { fail(version, error); }
        });
    }
    // 0 = locate next label; 1..3 = the explicitly selected CPCL media mode.
    synchronized void mediaControl(int mode, LabelConfig config) {
        if (phase != Phase.READY) throw new IllegalStateException("请先连接打印机");
        if (mode < 0 || mode > 3) throw new IllegalArgumentException("无效介质模式");
        if (config == null || config.receipt)
            throw new IllegalArgumentException("介质设置和标签定位需要本次标签打印参数");
        long version = epoch;
        Session selected = session;
        task = 0; earlyTerminal = null; deadline = System.nanoTime() + taskTimeoutNanos;
        if (deadlineCheck != null) deadlineCheck.cancel(false);
        deadlineCheck = control.schedule(this::checkDeadline, taskTimeoutNanos, TimeUnit.NANOSECONDS);
        changed(Phase.PRINTING, mode == 0 ? "正在发送标签定位指令…" : "正在发送介质设置…");
        worker.execute(() -> {
            try {
                if (stale(version)) return;
                long id = selected.mediaControl(mode, config);
                synchronized (this) {
                    if (version != epoch) return;
                    task = id;
                    deliverEarlyTerminal(version, id);
                    listener.log("task=" + id + " CPCL control=" + mode + "；需实物确认生效");
                }
            } catch (RuntimeException error) { fail(version, error); }
        });
    }
    private synchronized boolean stale(long version) { return epoch != version; }
    private synchronized void receiveState(long version, Session selected, DeviceStateSnapshot snapshot,
                                           ResourceSnapshot usage, Diagnostic latest) {
        if (version != epoch || selected != session) return;
        if (snapshot != null && snapshot.connectionGeneration < minimumStateGeneration) return;
        if (snapshot != null) minimumStateGeneration = Math.max(minimumStateGeneration,snapshot.connectionGeneration);
        deviceState = snapshot; resources = usage; diagnostic = latest;
        listener.deviceStateChanged(snapshot); listener.deviceDetailsChanged(usage, latest);
        if (snapshot != null && snapshot.connectionState == MobilePrinter.CONNECTED) connectedStateObserved = true;
        if (snapshot != null && snapshot.connectionState == MobilePrinter.DISCONNECTED && phase == Phase.READY &&
            (connectedStateObserved || (latest != null && latest.connectionGeneration == snapshot.connectionGeneration &&
                latest.source == Diagnostic.TRANSPORT && latest.errorCode != 0))) {
            listener.log(diagnosticText(latest)); disconnect("连接已丢失；请检查电源和蓝牙后手动重连");
        }
    }
    synchronized void sendRaw(byte[] bytes, int copies) {
        if (phase != Phase.READY) throw new IllegalStateException("请先连接打印机");
        RawCommand command = new RawCommand(bytes, copies);
        long version = epoch; Session selected = session;
        task = 0; earlyTerminal = null; deadline = System.nanoTime() + taskTimeoutNanos;
        if (deadlineCheck != null) deadlineCheck.cancel(false);
        deadlineCheck = control.schedule(this::checkDeadline, taskTimeoutNanos, TimeUnit.NANOSECONDS);
        changed(Phase.PRINTING, "正在提交 Raw 多份操作…");
        worker.execute(() -> {
            try {
                if (stale(version)) return;
                long id = selected.raw(command.copyBytes(), command.copies);
                synchronized (this) {
                    if (version != epoch) return;
                    task = id;
                    deliverEarlyTerminal(version, id);
                    listener.log("task=" + id + " Raw copies=" + command.copies + " bytes=" + command.size());
                }
            } catch (RuntimeException error) { fail(version, error); }
        });
    }
    synchronized void cancel() {
        if (phase != Phase.PRINTING) return;
        if (task == 0) {
            // Submission may already have been accepted before its ID reaches this controller.
            String message = "已请求取消并断开；可能已发送数据，请检查出纸后再重试";
            listener.log(message); disconnect(message); return;
        }
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
        Session selected = session; session = null; task = 0; earlyTerminal = null; deviceState = null; resources = null;
        if (deadlineCheck != null) { deadlineCheck.cancel(false); deadlineCheck = null; }
        listener.deviceStateChanged(null);
        listener.deviceDetailsChanged(null, diagnostic);
        Future<?> connection = connecting;
        changed(Phase.STOPPING, "正在断开…");
        control.execute(() -> {
            String completed = message;
            try { if (selected != null) selected.close(); }
            catch (RuntimeException error) { completed = failureMessage(error); listener.log(completed); }
            finally {
                try { awaitAttempt(connection); }
                finally {
                    synchronized (this) {
                        if (version == epoch) { connecting = null; changed(Phase.OFFLINE, completed); }
                    }
                }
            }
        });
    }
    private synchronized void checkDeadline() {
        if (phase == Phase.PRINTING && System.nanoTime() - deadline >= 0) {
            listener.log("任务超过演示应用截止时间；断开且不重放");
            disconnect("任务超时；请检查出纸后手动重连");
        }
    }
    private synchronized void receiveUpdate(long version, Update event) {
        if (version != epoch) return;
        if (task == 0 && phase == Phase.PRINTING) { earlyTerminal = event; return; }
        if (event.id != task) return;
        listener.log("task=" + event.id + " state=" + event.state + " completion=" + event.completion +
            " bytesSent=" + event.current + " bytesTotal=" + (((event.byteCountFlags & 2) == 0) ? "unknown" : event.total) + " error=" + event.error);
        if (deadlineCheck != null) { deadlineCheck.cancel(false); deadlineCheck = null; }
        if (event.state == 4 && event.completion == 1) {
            task = 0; changed(Phase.READY, "DATA_SENT · 数据已发送，设置与出纸结果需实物确认");
        } else {
            if (event.diagnostic != null) { diagnostic = event.diagnostic; listener.log(diagnosticText(diagnostic)); }
            String message = event.state == 6 ? "任务已取消；已发送数据无法撤回" : "任务失败：" + event.errorDescription;
            listener.log(message); disconnect(message);
        }
    }
    private void deliverEarlyTerminal(long version, long id) {
        Update selected = earlyTerminal; earlyTerminal = null;
        if (selected != null && selected.id == id) receiveUpdate(version, selected);
    }
    private synchronized void fail(long version, RuntimeException error) {
        if (version != epoch) return;
        if (error instanceof com.netumscan.printersdk.PrinterException &&
            ((com.netumscan.printersdk.PrinterException)error).diagnostic != null)
            diagnostic = ((com.netumscan.printersdk.PrinterException)error).diagnostic;
        String message = failureMessage(error);
        listener.log(message); disconnect(message);
    }
    private static String failureMessage(RuntimeException error) {
        return error instanceof com.netumscan.printersdk.PrinterException ?
            ((com.netumscan.printersdk.PrinterException)error).diagnostic != null ?
                diagnosticText(((com.netumscan.printersdk.PrinterException)error).diagnostic) :
                "SDK 错误码 " + ((com.netumscan.printersdk.PrinterException)error).code + "；" +
                    com.netumscan.printersdk.PrinterException.description(((com.netumscan.printersdk.PrinterException)error).code) :
            error instanceof SecurityException ? "蓝牙访问权限不可用；请在系统应用设置中授权后重试" :
            "操作失败；请检查设备电源、蓝牙与配置后手动重连";
    }
    static void awaitConnection(Future<Void> completion) { await(completion, false); }
    static void awaitCleanup(Future<?> completion) { await(completion, true); }
    private static void awaitAttempt(Future<?> completion) {
        if (completion == null) return;
        try { awaitCleanup(completion); }
        catch (CancellationException ignored) { /* Only never-started attempts are cancelled on teardown. */ }
    }
    private static void await(Future<?> completion, boolean cleanup) {
        boolean interrupted = false;
        try {
            for (;;) {
                try { completion.get(); return; }
                catch (InterruptedException error) {
                    interrupted = true;
                    if (!cleanup) throw new com.netumscan.printersdk.PrinterException(1005);
                } catch (ExecutionException error) {
                    Throwable cause = error.getCause();
                    if (cause instanceof RuntimeException) throw (RuntimeException)cause;
                    if (cause instanceof Error) throw (Error)cause;
                    throw new com.netumscan.printersdk.PrinterException(1000);
                }
            }
        } finally { if (interrupted) Thread.currentThread().interrupt(); }
    }
    public synchronized void close() {
        if (phase == Phase.CLOSED) return;
        ++epoch;
        if (deadlineCheck != null) { deadlineCheck.cancel(false); deadlineCheck = null; }
        earlyTerminal = null;
        Session selected = session; session = null; deviceState = null; resources = null;
        listener.deviceStateChanged(null);
        listener.deviceDetailsChanged(null, diagnostic);
        Future<?> connection = connecting; connecting = null;
        changed(Phase.CLOSED, "会话已关闭");
        Future<Void> closing = null;
        try { closing = owner.close(); }
        catch (RuntimeException error) { listener.log(failureMessage(error)); }
        Future<Void> cleanup = closing;
        control.execute(() -> {
            try { if (selected != null) selected.close(); }
            catch (RuntimeException error) { listener.log(failureMessage(error)); }
            finally {
                try { awaitAttempt(connection); }
                catch (RuntimeException error) { listener.log(failureMessage(error)); }
                finally {
                    try { if (cleanup != null) awaitCleanup(cleanup); }
                    catch (RuntimeException error) { listener.log(failureMessage(error)); }
                }
            }
        });
        // shutdownNow removes queued Future tasks without completing them. Cancel those
        // never-started tasks so cleanup cannot wait forever; active attempts finish their own cleanup.
        for (Runnable pending : worker.shutdownNow()) if (pending instanceof Future<?>) ((Future<?>)pending).cancel(false);
        control.shutdown();
    }
}
