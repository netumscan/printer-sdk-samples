// SPDX-License-Identifier: MIT
package com.netumscan.printersdk.demo;

import com.netumscan.printersdk.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Demo 的 SDK 控制入口：在主线程维护应用状态，直接调用公共 PrinterManager。
 * 按 connect → print → cancel / disconnect 阅读即可了解主要用法，后面是可选诊断与状态维护。
 * Manager 负责后台准备、请求生命周期和设备 IO；本类不另建打印队列，也不持有原生句柄。
 */
final class DemoModel implements AutoCloseable {
    private final PrinterManager manager;
    interface SampleFactory { DemoPrintData create(DemoPixelSamples.Example example, LabelConfig config, ImageCancellation cancelled); }
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService imageWorker;
    private final boolean ownsImageWorker;
    private final SampleFactory samples;
    private ImagePreparation preparation;
    private Runnable changed;
    enum RequestGroup { DEVICE, INDEPENDENT, LIFECYCLE }
    private final java.util.Map<RequestId, RequestGroup> pending = new java.util.IdentityHashMap<>();
    private void track(RequestId id, RequestGroup group) { pending.put(id, group); }
    int pendingCount(RequestGroup group) {
        int count = 0;
        for (RequestGroup value : pending.values()) if (value == group) count++;
        return count;
    }
    // 分别跟踪普通请求、设备发现和当前连接，避免迟到回调修改后续请求的状态。
    private RequestId active, discovery, connection;
    private DemoResult.Kind activeKind;
    private boolean connected, disconnecting, foreground, closed, cancelling;
    private boolean connectionBle;
    private StatusResult liveStatus;
    private String inputError, selectedID;
    private List<DiscoveredDevice> candidates = Collections.emptyList();
    private DemoResult<?> latestResult;
    private final List<DemoResult<?>> resultHistory = new ArrayList<>();
    private void record(DemoResult<?> result) {
        resultHistory.add(result);
        while (resultHistory.size() > 32) resultHistory.remove(0);
    }
    List<DemoResult<?>> resultHistory() {
        return Collections.unmodifiableList(new ArrayList<>(resultHistory));
    }
    // 业务终态、意外断连和主动清理结果独立保存，清理成功不能覆盖打印失败或可能输出的信息。
    private DemoResult<?> lastRequest;
    private DemoResult<Void> disconnectedResult, cleanupResult;
    private DemoGeometry printProfile;
    private ConnectionInfo connectionInfo;
    private boolean configured;
    private long connectionVersion;
    private PaperType selectedMedia = PaperType.GAP_LABEL;
    private DemoResult<Void> statusResult;
    private DemoResult<PrinterCapabilities> capabilitiesResult;
    private DemoResult<SdkInfo> identityResult;
    private DemoResult<List<DiscoveredDevice>> discoveryResult;
    private DemoUiState.MediaRequest mediaRequest;
    // 仅保留应用请求的提交顺序和真实终态；准备、准入与 IO FIFO 全部由同一个 Manager 负责。
    private static final int MAX_PRINT_HISTORY = 32, BATCH_SIZE = 3;
    private final List<DemoUiState.PrintRequest> printRequests = new ArrayList<>();
    private long nextPrintSequence;
    private DemoUiState state;
    private final DemoUiState.StatusTransitions statusTransitions = new DemoUiState.StatusTransitions();

    DemoModel(PrinterManager manager) { this(manager, imageWorker(), DemoPixelSamples::create, true); }
    DemoModel(PrinterManager manager, ExecutorService worker) { this(manager, worker, DemoPixelSamples::create, false); }
    DemoModel(PrinterManager manager, SampleFactory samples) { this(manager, imageWorker(), samples, true); }
    private static ExecutorService imageWorker() {
        return new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1), task -> {
            Thread thread = new Thread(task, "PrinterDemo.compose"); thread.setDaemon(true); return thread;
        });
    }
    private DemoModel(PrinterManager manager, ExecutorService worker, SampleFactory samples, boolean ownsWorker) {
        this.manager = manager; imageWorker = worker; this.samples = samples; ownsImageWorker = ownsWorker;
        publish();
        manager.setStatusListener(status -> {
            if (connected && foreground && !disconnecting && !closed) { liveStatus = status; publish(); }
        });
        RequestId identity = manager.sdkInfo(result -> {
            pending.remove(result.requestId);
            identityResult = new DemoResult<>(DemoResult.Kind.IDENTITY, result); record(identityResult);
            if (closed) return;
            publish();
        });
        track(identity, RequestGroup.INDEPENDENT);
    }
    DemoUiState state() { return state; }
    long statusRecoverySequence() { return statusTransitions.recoverySequence(); }
    long statusSampleSequence() { return statusTransitions.sampleSequence(); }
    String statusRecoveryNotice() { return statusTransitions.recoveryNotice(); }
    void attach(Runnable listener) { if (!closed) { changed = listener; listener.run(); } }
    // 旧 Activity 的生命周期回调只能解绑自己的监听，不能移除重建后新界面的监听。
    void detach(Runnable listener) { if (changed == listener) changed = null; }
    void inputError(String message) { if (!closed) { inputError = message; publish(); } }
    /** 连接绑定默认打印配置，后续打印不重复传入 DPI 和最大宽度。 */
    void connect(DiscoveredDevice target, boolean ble, PrinterModel model, PaperType media) {
        if (!state.canConnect()) return;
        connectionBle = ble;
        selectedMedia = media;
        mediaRequest = null; configured = false; printProfile = null; connectionInfo = null;
        final long version = ++connectionVersion;
        begin(DemoResult.Kind.CONNECT);
        PrinterManager.Callback<ConnectionInfo> completion = result -> {
            pending.remove(result.requestId);
            DemoResult<ConnectionInfo> completed = new DemoResult<>(DemoResult.Kind.CONNECT, result);
            record(completed); lastRequest = completed; latestResult = completed;
            if (closed || version != connectionVersion || active != result.requestId) { publish(); return; }
            active = null; cancelling = false;
            if (!disconnecting) {
                connected = result.outcome == ManagerResult.Outcome.SUCCESS;
                if (!connected) { connection = null; disconnect(); }
                else {
                    connectionInfo = result.value;
                    printProfile = geometry(connectionInfo);
                    configured = printProfile != null;
                }
            }
            publish();
        };
        PrinterManager.DisconnectedListener disconnected = result -> {
            // SDK 已停止 IO、清理旧连接并安排请求终态，无需再调用一次 disconnect。
            if (closed || version != connectionVersion || disconnecting) return;
            ++connectionVersion; cancelPreparation();
            connection = null; connected = false; mediaRequest = null;
            configured = false; printProfile = null; connectionInfo = null;
            disconnectedResult = new DemoResult<>(DemoResult.Kind.DISCONNECTED, result); record(disconnectedResult);
            publish();
        };
        active = ble ? model == null ? manager.connectBle(target, completion, disconnected) :
            manager.connectBle(target, model, completion, disconnected) : model == null ?
            manager.connectSpp(target, completion, disconnected) : manager.connectSpp(target, model, completion, disconnected);
        connection = active;
        track(active, RequestGroup.LIFECYCLE);
        publish();
    }
    DemoGeometry printProfile() { return printProfile; }
    boolean configured() { return configured; }
    /** 型号选择只更新连接配置，不隐式发送纸张设置。 */
    void configureModel(PrinterModel model) {
        if (!state.canWork()) return;
        final DemoGeometry previous = printProfile;
        final ConnectionInfo previousInfo = connectionInfo;
        final boolean previousConfigured = configured;
        final long version = connectionVersion;
        configured = false;
        begin(DemoResult.Kind.PROFILE);
        active = manager.setPrinterModel(model, result -> {
            DemoResult<ConnectionInfo> completed = new DemoResult<>(DemoResult.Kind.PROFILE, result); record(completed);
            if (!takeResult(result)) { publish(); return; }
            lastRequest = completed; latestResult = lastRequest;
            if (version == connectionVersion && connected && !disconnecting) {
                if (result.outcome == ManagerResult.Outcome.SUCCESS) {
                    connectionInfo = result.value;
                    printProfile = geometry(connectionInfo); mediaRequest = null;
                    configured = printProfile != null;
                } else {
                    printProfile = previous; connectionInfo = previousInfo; configured = previousConfigured;
                }
            }
            publish();
        });
        track(active, RequestGroup.DEVICE);
        publish();
    }
    private static DemoGeometry geometry(ConnectionInfo info) {
        return info != null && info.model == PrinterModel.LT_M90 ? DemoGeometry.ltM90() : null;
    }
    /** App-owned preparation uses one bounded worker; only the Manager owns printer IO scheduling. */
    void print(DemoImageSource source, LabelConfig config) { prepareImages(source, config, null, 1); }
    void printSample(DemoPixelSamples.Example example, LabelConfig config) {
        if (example == null) throw new IllegalArgumentException("请选择像素示例");
        prepareImages(null, config, example, 1);
    }
    /** Construct, snapshot through the ordinary SDK API, and release each page before making the next. */
    void printBatch(DemoImageSource source, LabelConfig config, DemoPixelSamples.Example example) {
        prepareImages(source, config, example, BATCH_SIZE);
    }
    private void prepareImages(DemoImageSource source, LabelConfig config, DemoPixelSamples.Example example, int count) {
        if (preparation != null || !state.canSubmitPrints(count)) return;
        validatePrintConfig(config);
        if (example != null) DemoPixelSamples.validate(example, config);
        else if (source == null) throw new IllegalArgumentException("请选择图片来源");
        ImagePreparation task = new ImagePreparation(source == null ? null : source.acquire(), config, example, count);
        preparation = task; begin(DemoResult.Kind.PRINT); publish();
        try { imageWorker.execute(task); }
        catch (RejectedExecutionException failure) {
            task.releaseSource(); preparation = null; inputError = "图片准备繁忙；未提交打印"; publish();
        }
    }
    private final class ImagePreparation implements Runnable {
        private final DemoImageSource source;
        private final LabelConfig config;
        private final DemoPixelSamples.Example example;
        private final int count;
        private final long version = connectionVersion;
        private volatile boolean cancelled;
        ImagePreparation(DemoImageSource source, LabelConfig config, DemoPixelSamples.Example example, int count) {
            this.source = source; this.config = config; this.example = example; this.count = count;
        }
        private boolean cancelled() { return cancelled || Thread.currentThread().isInterrupted(); }
        void releaseSource() { if (source != null) source.close(); }
        @Override public void run() {
            boolean failed = false;
            try {
                for (int index = 0; index < count && !cancelled(); index++) {
                    DemoPrintData data = example == null ? source.printData(config, this::cancelled) :
                        samples.create(example, config, this::cancelled);
                    CountDownLatch consumed = new CountDownLatch(1);
                    final int page = index;
                    // Ownership crosses to main only after post succeeds. The worker waits for
                    // that one page to be snapshotted/released, never for printer IO or its result.
                    if (!main.post(() -> {
                        try (DemoPrintData held = data) {
                            if (preparation != this || cancelled || closed || !foreground || disconnecting ||
                                    !connected || version != connectionVersion) return;
                            String description = example == null ? "当前图片" : "像素示例：" + example;
                            submitPrint(held, description + (count == 1 ? "" : " · 批量 " + (page + 1) + "/" + count), true);
                        } catch (RuntimeException | OutOfMemoryError failure) {
                            cancelled = true; inputError = "图片提交失败；未接纳的图片不再提交"; publish();
                        } finally { consumed.countDown(); }
                    })) { data.close(); throw new CancellationException(); }
                    boolean interrupted = false;
                    for (;;) try { consumed.await(); break; }
                    catch (InterruptedException interruption) { interrupted = true; cancelled = true; }
                    if (interrupted) Thread.currentThread().interrupt();
                }
            } catch (CancellationException ignored) { /* An unsubmitted page has no SDK request or terminal to invent. */ }
            catch (RuntimeException | OutOfMemoryError failure) { failed = true; }
            finally {
                releaseSource();
                final boolean reportFailure = failed;
                main.post(() -> {
                    if (preparation != this) return;
                    preparation = null; if (active == null) cancelling = false;
                    if (reportFailure && !cancelled && !closed && foreground)
                        inputError = "图片准备失败；未接纳的图片不再提交";
                    publish();
                });
            }
        }
    }
    private void cancelPreparation() { if (preparation != null) preparation.cancelled = true; }
    /** 同一个像素入口供 App 生成的灰度图及无 Android Bitmap 的真实回归检查使用。 */
    void printPixels(LabelConfig config, byte[] pixels) {
        if (!state.canSubmitPrint()) return;
        validatePrintConfig(config);
        try (DemoPrintData print = DemoPrintData.pixels(config, pixels)) {
            submitPrint(print, "像素图", true);
        }
    }
    private void submitPrint(DemoPrintData print, String description, boolean notify) {
        begin(DemoResult.Kind.PRINT);
        RequestId requestId = print.encoded == null
            ? manager.printImage(print.bitmap, print.config.widthMm(), print.config.heightMm(), this::completedPrint)
            : manager.printEncodedImage(print.encoded, print.config.widthMm(), print.config.heightMm(), this::completedPrint);
        track(requestId, RequestGroup.DEVICE);
        trimPrintHistory();
        printRequests.add(new DemoUiState.PrintRequest(++nextPrintSequence, requestId, description, false, null));
        if (notify) publish();
    }
    private static void validatePrintConfig(LabelConfig config) {
        if (config == null) throw new IllegalArgumentException("请先填写打印参数");
    }
    private void trimPrintHistory() {
        while (printRequests.size() >= MAX_PRINT_HISTORY) {
            int terminal = -1;
            for (int i = 0; i < printRequests.size(); ++i)
                if (printRequests.get(i).result != null) { terminal = i; break; }
            if (terminal < 0) return;
            printRequests.remove(terminal);
        }
    }
    private void completedPrint(ManagerResult<Void> result) {
        pending.remove(result.requestId);
        DemoResult<Void> completed = new DemoResult<>(DemoResult.Kind.PRINT, result); record(completed);
        if (closed) return;
        for (int i = 0; i < printRequests.size(); ++i) {
            DemoUiState.PrintRequest request = printRequests.get(i);
            if (request.requestId != result.requestId || request.result != null) continue;
            lastRequest = completed; latestResult = completed;
            printRequests.set(i, new DemoUiState.PrintRequest(request.sequence, request.requestId,
                request.description, false, completed));
            publish();
            return;
        }
    }
    /** 取消只表达停止意图，仍等待真实终态；已经发送到打印机的数据无法撤回。 */
    void cancel() {
        if (!state.canCancel()) return;
        if (preparation != null) {
            cancelPreparation(); cancelling = true;
            inputError = "图片准备已取消；未接纳的图片不会提交";
            publish();
        }
        if (active != null) {
            manager.cancel(active); cancelling = true; inputError = null;
            publish();
        } else {
            for (DemoUiState.PrintRequest request : printRequests) if (request.result == null) {
                cancel(request.requestId);
                return;
            }
        }
    }
    /** 按请求取消只设置意图；发送期取消可能使连接失效，所有请求仍各自等待真实终态。 */
    void cancel(RequestId requestId) {
        if (closed || !foreground || disconnecting || requestId == null) return;
        for (int i = 0; i < printRequests.size(); ++i) {
            DemoUiState.PrintRequest request = printRequests.get(i);
            if (request.requestId != requestId || request.result != null || request.cancelling) continue;
            manager.cancel(requestId);
            printRequests.set(i, new DemoUiState.PrintRequest(request.sequence, request.requestId,
                request.description, true, null));
            inputError = null;
            publish();
            return;
        }
    }
    /** 断开期间保持清理屏障，完成后才允许换机或重新连接，不自动重放旧任务。 */
    void disconnect() {
        if (closed || disconnecting) return;
        cancelPreparation();
        disconnecting = true; ++connectionVersion; connection = null; inputError = null; mediaRequest = null;
        configured = false; printProfile = null; connectionInfo = null;
        publish();
        RequestId cleanup = manager.disconnect(result -> {
            pending.remove(result.requestId);
            DemoResult<Void> completed = new DemoResult<>(DemoResult.Kind.DISCONNECT, result); record(completed);
            if (closed) return;
            active = null; discovery = null; connected = false; disconnecting = false; cancelling = false;
            cleanupResult = completed;
            // 单独记录清理结果，保留原打印/取消终态及 outputMayHaveOccurred。
            publish();
        });
        track(cleanup, RequestGroup.LIFECYCLE);
    }

    /** Raw 也通过同一个 Manager 提交，不能直接写蓝牙或绕过设备队列。 */
    void sendRaw(byte[] bytes, int copies) {
        if (!state.canWork()) return;
        if (copies != 1) throw new IllegalArgumentException("Raw 每次仅发送一次；命令内部份数由调用者确认");
        if (bytes == null || bytes.length == 0 || bytes.length > 4096)
            throw new IllegalArgumentException("Raw 需要 1–4096 个字节");
        // 任意 Raw 可能改变设备介质模式，不能继续展示先前介质请求为当前状态。
        mediaRequest = null; configured = printProfile != null;
        begin(DemoResult.Kind.RAW);
        active = manager.sendRaw(bytes, result -> completed(DemoResult.Kind.RAW, result));
        track(active, RequestGroup.DEVICE);
        publish();
    }
    /** 介质设置使用连接已采用的 Profile，无需重复提供设备参数。 */
    void setMedia(PaperType mode) {
        if (!state.canWork()) return;
        if (mode == null) throw new IllegalArgumentException("请选择介质类型");
        begin(DemoResult.Kind.MEDIA);
        selectedMedia = mode;
        active = manager.setPaperType(mode, result -> completed(DemoResult.Kind.MEDIA, result));
        track(active, RequestGroup.DEVICE);
        // 所有输入校验完成、请求已提交后才替换快照，失败输入不能重标旧成功结果。
        mediaRequest = new DemoUiState.MediaRequest(mode, connection, active, null);
        publish();
    }
    /** 显式定位下一标签可能走纸；普通打印不会自动追加定位。 */
    void feed() {
        if (!state.canWork()) return;
        begin(DemoResult.Kind.FEED);
        active = manager.locateNextLabel( result -> completed(DemoResult.Kind.FEED, result));
        track(active, RequestGroup.DEVICE);
        publish();
    }
    /** 查询协议独立于图片几何；不支持或未知的结果原样保留，不能解释成硬件正常。 */
    void getStatus() {
        if (!state.canWork()) return;
        begin(DemoResult.Kind.STATUS);
        active = manager.getStatus(result -> {
            DemoResult<Void> completed = new DemoResult<>(DemoResult.Kind.STATUS, result); record(completed);
            if (!takeResult(result)) { publish(); return; }
            statusResult = completed; latestResult = statusResult;
            publish();
        });
        track(active, RequestGroup.DEVICE);
        publish();
    }
    /** 保存 SDK 返回的能力快照；实现能力不等于目标打印机已通过硬件验证。 */
    void capabilities() {
        if (!state.canWork()) return;
        begin(DemoResult.Kind.CAPABILITIES);
        active = manager.capabilities(result -> {
            DemoResult<PrinterCapabilities> completed = new DemoResult<>(DemoResult.Kind.CAPABILITIES, result); record(completed);
            if (!takeResult(result)) { publish(); return; }
            capabilitiesResult = completed; latestResult = capabilitiesResult;
            publish();
        });
        track(active, RequestGroup.DEVICE);
        publish();
    }
    // 控制命令不打印图片；固定合法高度即可，无需读取本次图片尺寸。
    /** SPP 只读取系统已配对列表；BLE 执行限时扫描，发现候选不会自动建立连接。 */
    void discover(boolean ble) {
        if (!state.canDiscover() || discovery != null) return;
        inputError = null;
        DemoResult.Kind kind = ble ? DemoResult.Kind.SCAN : DemoResult.Kind.BONDED;
        PrinterManager.Callback<List<DiscoveredDevice>> completion = result -> {
            pending.remove(result.requestId);
            DemoResult<List<DiscoveredDevice>> completed = new DemoResult<>(kind, result); record(completed);
            if (closed || discovery != result.requestId) { publish(); return; }
            discovery = null;
            discoveryResult = completed; latestResult = discoveryResult;
            if (result.outcome == ManagerResult.Outcome.SUCCESS && foreground && !disconnecting) {
                candidates = result.value == null ? Collections.emptyList() : new ArrayList<>(result.value);
                if (selectedID != null && !containsCandidate(selectedID)) selectedID = null;
            }
            publish();
        };
        discovery = ble ? manager.scanBle(completion) : manager.bondedDevices(completion);
        track(discovery, RequestGroup.INDEPENDENT);
        publish();
    }
    void stopDiscovery() { if (discovery != null) manager.cancel(discovery); }
    /** 切换传输或加载配置时清除候选与选择，不因此扫描、连接或发送。 */
    void clearCandidates() {
        if (!state.canEditConnection()) return;
        if (discovery != null) { manager.cancel(discovery); discovery = null; }
        selectedID = null; candidates = Collections.emptyList(); discoveryResult = null;
        publish();
    }
    /** 仅记录用户明确选中的候选，地址回填和后续连接由界面显式操作。 */
    boolean selectCandidate(String id) {
        if (!state.canSelectCandidate() || !containsCandidate(id)) return false;
        selectedID = id; inputError = null; publish(); return true;
    }
    private boolean containsCandidate(String id) {
        for (DiscoveredDevice device : candidates) if (device.address.equals(id)) return true;
        return false;
    }
    void foreground() { if (!closed) { foreground = true; publish(); } }
    // 真正进入后台时主动断开；返回前台只恢复交互许可，不恢复连接或打印。
    void background() { foreground = false; publish(); disconnect(); }
    /** 永久结束本 Model，先停止向界面回调，再交给 Manager 异步释放设备资源。 */
    @Override public void close() {
        manager.setStatusListener(null);
        if (closed) return;
        closed = true; foreground = false; changed = null; mediaRequest = null;
        cancelPreparation(); if (ownsImageWorker) imageWorker.shutdown();
        connected = false; configured = false; printProfile = null; connectionInfo = null;
        RequestId cleanup = manager.close(result -> {
            pending.remove(result.requestId);
            record(new DemoResult<>(DemoResult.Kind.CLOSE, result));
        });
        track(cleanup, RequestGroup.LIFECYCLE);
        publish();
    }
    /** 仅接收当前请求的终态；关闭后的回调和旧请求的结果不再改变界面状态。 */
    private boolean takeResult(ManagerResult<?> result) {
        pending.remove(result.requestId);
        if (closed || active != result.requestId) return false;
        active = null; cancelling = false;
        return true;
    }
    private void completed(DemoResult.Kind kind, ManagerResult<Void> result) {
        DemoResult<Void> completed = new DemoResult<>(kind, result); record(completed);
        if (!takeResult(result)) { publish(); return; }
        lastRequest = completed; latestResult = completed;
        if (kind == DemoResult.Kind.MEDIA && mediaRequest != null &&
                mediaRequest.requestId == result.requestId && mediaRequest.connectionId == connection) {
            mediaRequest = new DemoUiState.MediaRequest(mediaRequest.mode, connection, result.requestId, completed);
            configured = connected && printProfile != null;
        }
        publish();
    }
    /** 生成只读展示快照；界面状态不等于打印机的实际硬件状态。 */
    private void publish() {
        boolean observesStatus = connected && foreground && !disconnecting && !closed;
        if (!observesStatus) liveStatus = null;
        statusTransitions.update(observesStatus, liveStatus);
        boolean pendingPrint = false, cancellingPrint = false;
        for (DemoUiState.PrintRequest request : printRequests) if (request.result == null) {
            pendingPrint = true; cancellingPrint |= request.cancelling;
        }
        DemoUiState.Phase phase = closed ? DemoUiState.Phase.CLOSED : disconnecting ? DemoUiState.Phase.DISCONNECTING :
            activeKind == DemoResult.Kind.CONNECT && active != null ? DemoUiState.Phase.CONNECTING : active != null ? DemoUiState.Phase.WORKING :
            pendingPrint || (preparation != null && connected) ? DemoUiState.Phase.WORKING : connected ? DemoUiState.Phase.READY : DemoUiState.Phase.OFFLINE;
        state = new DemoUiState(phase, foreground, discovery != null, cancelling || cancellingPrint, inputError, selectedID, candidates,
            activeKind, latestResult, lastRequest, statusResult, capabilitiesResult, identityResult, discoveryResult, disconnectedResult, cleanupResult,
            mediaRequest, connected, connected && configured && !closed && !disconnecting && active == null, printRequests,
            connectionInfo, preparation != null, liveStatus);
        if (changed != null) changed.run();
    }
    private void begin(DemoResult.Kind kind) { inputError = null; cancelling = false; activeKind = kind; }

}
