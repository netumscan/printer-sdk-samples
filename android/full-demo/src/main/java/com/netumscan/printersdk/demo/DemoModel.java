// SPDX-License-Identifier: MIT
package com.netumscan.printersdk.demo;

import com.netumscan.printersdk.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Demo 的 SDK 控制入口：在主线程维护应用状态，直接调用公共 PrinterManager。
 * 按 connect → printFile → cancel / disconnect 阅读即可了解主要用法，后面是可选诊断与状态维护。
 * Manager 负责后台准备、请求生命周期和设备 IO；本类不另建打印队列，也不持有原生句柄。
 */
final class DemoModel implements AutoCloseable {
    // SDK 请求预算，单位毫秒。图像解码和发送共同使用 PRINT_TIMEOUT_MS。
    private static final int CONNECT_TIMEOUT_MS = 10000, PRINT_TIMEOUT_MS = 120000;
    private static final int OPERATION_TIMEOUT_MS = 30000, SCAN_MS = 10000;
    private static final int QUERY_TIMEOUT_MS = 5000, QUERY_READ_TIMEOUT_MS = 2000;
    private final PrinterManager manager;
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
    private PrinterProfile printProfile;
    private ConnectionInfo connectionInfo;
    private boolean configured;
    private long connectionVersion;
    private MediaMode selectedMedia = MediaMode.GAP;
    private DemoResult<StatusResult> statusResult;
    private DemoResult<PrinterCapabilities> capabilitiesResult;
    private DemoResult<SdkInfo> identityResult;
    private DemoResult<List<DiscoveredDevice>> discoveryResult;
    private DemoUiState.MediaRequest mediaRequest;
    // 仅保留应用请求的提交顺序和真实终态；准备、准入与 IO FIFO 全部由同一个 Manager 负责。
    private static final int MAX_PRINT_HISTORY = 32, BATCH_SIZE = 3;
    private final List<DemoUiState.PrintRequest> printRequests = new ArrayList<>();
    private long nextPrintSequence;
    private DemoUiState state;

    DemoModel(PrinterManager manager) {
        this.manager = manager;
        publish();
        RequestId identity = manager.sdkInfo(result -> {
            pending.remove(result.requestId);
            identityResult = new DemoResult<>(DemoResult.Kind.IDENTITY, result); record(identityResult);
            if (closed) return;
            publish();
        });
        track(identity, RequestGroup.INDEPENDENT);
    }
    DemoUiState state() { return state; }
    void attach(Runnable listener) { if (!closed) { changed = listener; listener.run(); } }
    // 旧 Activity 的生命周期回调只能解绑自己的监听，不能移除重建后新界面的监听。
    void detach(Runnable listener) { if (changed == listener) changed = null; }
    void inputError(String message) { if (!closed) { inputError = message; publish(); } }
    /** 连接绑定默认打印配置，后续打印不重复传入 DPI 和最大宽度。 */
    void connect(ConnectionOptions target) { connect(target, MediaMode.GAP); }
    void connect(ConnectionOptions target, MediaMode media) {
        if (!state.canConnect()) return;
        selectedMedia = media;
        mediaRequest = null; configured = false; printProfile = null; connectionInfo = null;
        final long version = ++connectionVersion;
        begin(DemoResult.Kind.CONNECT);
        active = manager.connect(target, CONNECT_TIMEOUT_MS, result -> {
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
                    printProfile = connectionInfo.printProfile;
                    initialize();
                }
            }
            publish();
        }, result -> {
            // SDK 已停止 IO、清理旧连接并安排请求终态，无需再调用一次 disconnect。
            if (closed || version != connectionVersion || disconnecting) return;
            ++connectionVersion;
            connection = null; connected = false; mediaRequest = null;
            configured = false; printProfile = null; connectionInfo = null;
            disconnectedResult = new DemoResult<>(DemoResult.Kind.DISCONNECTED, result); record(disconnectedResult);
            publish();
        });
        connection = active;
        track(active, RequestGroup.LIFECYCLE);
        publish();
    }
    PrinterProfile printProfile() { return printProfile; }
    boolean configured() { return configured; }
    /** 本地配置失败恢复旧状态；成功后仅使用新配置并允许独立重试初始化。 */
    void configureProfile(PrinterProfile profile, MediaMode media) {
        if (!state.canWork()) return;
        final PrinterProfile previous = printProfile;
        final ConnectionInfo previousInfo = connectionInfo;
        final boolean previousConfigured = configured;
        final long version = connectionVersion;
        configured = false;
        begin(DemoResult.Kind.PROFILE);
        active = manager.configurePrintProfile(profile, result -> {
            DemoResult<ConnectionInfo> completed = new DemoResult<>(DemoResult.Kind.PROFILE, result); record(completed);
            if (!takeResult(result)) { publish(); return; }
            lastRequest = completed; latestResult = lastRequest;
            if (version == connectionVersion && connected && !disconnecting) {
                if (result.outcome == ManagerResult.Outcome.SUCCESS) {
                    connectionInfo = result.value;
                    printProfile = connectionInfo.printProfile; selectedMedia = media; mediaRequest = null;
                    initialize();
                } else {
                    printProfile = previous; connectionInfo = previousInfo; configured = previousConfigured;
                }
            }
            publish();
        });
        track(active, RequestGroup.DEVICE);
        publish();
    }
    void retryInitialize() { if (state.canWork()) initialize(); }
    private void initialize() {
        if (!connected || disconnecting || printProfile == null) { configured = false; return; }
        if (printProfile.protocol == PrinterProfile.Protocol.ESC_POS) { configured = true; return; }
        configured = false;
        begin(DemoResult.Kind.MEDIA);
        active = manager.setMedia(MediaSettings.tracking(selectedMedia), OPERATION_TIMEOUT_MS,
            result -> completed(DemoResult.Kind.MEDIA, result));
        track(active, RequestGroup.DEVICE);
        mediaRequest = new DemoUiState.MediaRequest(selectedMedia, connection, active, null);
    }
    /** 普通路径文件由 App 保持只读有效；SDK 后台加载，不要求先预览。 */
    void printFile(File file, LabelConfig config) {
        if (!state.canSubmitPrint()) return;
        if (file == null) throw new IllegalArgumentException("请选择图片文件");
        validatePrintConfig(config);
        ImageSource source = ImageSource.deferred(context -> {
            context.throwIfCancelled();
            byte[] pixels = ImageImport.decode(file, config, context::isCancelled);
            context.throwIfCancelled();
            return PixelImage.copyOf(pixels, config.labelWidthDots, config.height, PixelImage.GRAY8);
        });
        try (DemoPrintData print = DemoPrintData.image(config, source, selectedMedia)) {
            submitPrint(print.options, "图片文件", true);
        }
    }
    /** App 来源负责预览；提交使用公共 SDK 来源，最后读取退出后释放文件 lease。 */
    void print(DemoImageSource source, LabelConfig config) {
        if (!state.canSubmitPrint()) return;
        if (source == null) throw new IllegalArgumentException("请选择图片来源");
        validatePrintConfig(config);
        try (DemoPrintData print = source.printData(config, selectedMedia)) {
            submitPrint(print.options, "当前图片", true);
        }
    }
    void printSample(DemoPixelSamples.Example example, LabelConfig config) {
        if (!state.canSubmitPrint()) return;
        validatePrintConfig(config);
        try (DemoPrintData print = DemoPixelSamples.create(example, config, selectedMedia)) {
            submitPrint(print.options, "像素示例：" + example, true);
        }
    }
    /** 先构建整批，再连续调用 SDK；App 不创建发送队列。 */
    void printBatch(DemoImageSource source, LabelConfig config, DemoPixelSamples.Example example) {
        if (!state.canSubmitPrints(BATCH_SIZE)) return;
        validatePrintConfig(config);
        if (example == null && source == null) throw new IllegalArgumentException("请选择图片来源");
        DemoPrintData[] batch = new DemoPrintData[BATCH_SIZE];
        int submitted = 0;
        try {
            for (int i = 0; i < BATCH_SIZE; i++) batch[i] = example == null
                ? source.printData(config, selectedMedia) : DemoPixelSamples.create(example, config, selectedMedia);
            for (int i = 0; i < BATCH_SIZE; i++) {
                submitPrint(batch[i].options, (example == null ? "当前图片" : "像素示例：" + example)
                    + " · 批量 " + (i + 1) + "/" + BATCH_SIZE, false);
                submitted++;
            }
        } finally {
            for (DemoPrintData print : batch) if (print != null) print.close();
            if (submitted > 0) publish();
        }
    }
    /** 同一个像素入口供 App 生成的灰度图及无 Android Bitmap 的真实回归检查使用。 */
    void printPixels(LabelConfig config, byte[] pixels) {
        if (!state.canSubmitPrint()) return;
        validatePrintConfig(config);
        try (DemoPrintData print = DemoPrintData.pixels(config, pixels, selectedMedia)) {
            submitPrint(print.options, "像素图", true);
        }
    }
    private void submitPrint(PrintOptions options, String description, boolean notify) {
        begin(DemoResult.Kind.PRINT);
        RequestId requestId = manager.print(options, this::completedPrint);
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
        if (copies < 1 || copies > 20) throw new IllegalArgumentException("每次限 1–20 份");
        if (bytes == null || bytes.length == 0 || bytes.length > 4096)
            throw new IllegalArgumentException("Raw 需要 1–4096 个字节");
        // 任意 Raw 可能改变设备介质模式，不能继续展示先前介质请求为当前状态。
        mediaRequest = null; configured = false;
        begin(DemoResult.Kind.RAW);
        active = manager.sendRaw(bytes, copies, OPERATION_TIMEOUT_MS, result -> completed(DemoResult.Kind.RAW, result));
        track(active, RequestGroup.DEVICE);
        publish();
    }
    /** 介质设置使用连接已采用的 Profile，无需重复提供设备参数。 */
    void setMedia(MediaMode mode) {
        if (!state.canWork()) return;
        if (mode == null) throw new IllegalArgumentException("请选择介质类型");
        MediaSettings media = MediaSettings.tracking(mode);
        begin(DemoResult.Kind.MEDIA);
        configured = false; selectedMedia = mode;
        active = manager.setMedia(media, OPERATION_TIMEOUT_MS, result -> completed(DemoResult.Kind.MEDIA, result));
        track(active, RequestGroup.DEVICE);
        // 所有输入校验完成、请求已提交后才替换快照，失败输入不能重标旧成功结果。
        mediaRequest = new DemoUiState.MediaRequest(mode, connection, active, null);
        publish();
    }
    /** 显式定位下一标签可能走纸，与打印作业内的 FORM 选项是不同操作。 */
    void feed() {
        if (!state.canWork()) return;
        begin(DemoResult.Kind.FEED);
        active = manager.locateNextLabel(OPERATION_TIMEOUT_MS, result -> completed(DemoResult.Kind.FEED, result));
        track(active, RequestGroup.DEVICE);
        publish();
    }
    /** 查询协议独立于图片几何；不支持或未知的结果原样保留，不能解释成硬件正常。 */
    void queryStatus(boolean receipt) {
        if (!state.canWork()) return;
        begin(DemoResult.Kind.STATUS);
        int protocol = receipt ? StatusQueryConfig.ESC_POS : StatusQueryConfig.CPCL;
        active = manager.queryStatus(new StatusQueryConfig(protocol, QUERY_READ_TIMEOUT_MS), QUERY_TIMEOUT_MS, result -> {
            DemoResult<StatusResult> completed = new DemoResult<>(DemoResult.Kind.STATUS, result); record(completed);
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
        discovery = ble ? manager.scanBle(null, SCAN_MS, completion) : manager.bondedDevices(completion);
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
    void background() { foreground = false; disconnect(); }
    /** 永久结束本 Model，先停止向界面回调，再交给 Manager 异步释放设备资源。 */
    @Override public void close() {
        if (closed) return;
        closed = true; foreground = false; changed = null; mediaRequest = null;
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
            configured = connected && result.outcome == ManagerResult.Outcome.SUCCESS &&
                result.completion != ManagerResult.Completion.NONE;
        }
        publish();
    }
    /** 生成只读展示快照；界面状态不等于打印机的实际硬件状态。 */
    private void publish() {
        boolean pendingPrint = false, cancellingPrint = false;
        for (DemoUiState.PrintRequest request : printRequests) if (request.result == null) {
            pendingPrint = true; cancellingPrint |= request.cancelling;
        }
        DemoUiState.Phase phase = closed ? DemoUiState.Phase.CLOSED : disconnecting ? DemoUiState.Phase.DISCONNECTING :
            activeKind == DemoResult.Kind.CONNECT && active != null ? DemoUiState.Phase.CONNECTING : active != null ? DemoUiState.Phase.WORKING :
            pendingPrint ? DemoUiState.Phase.WORKING : connected ? DemoUiState.Phase.READY : DemoUiState.Phase.OFFLINE;
        state = new DemoUiState(phase, foreground, discovery != null, cancelling || cancellingPrint, inputError, selectedID, candidates,
            activeKind, latestResult, lastRequest, statusResult, capabilitiesResult, identityResult, discoveryResult, disconnectedResult, cleanupResult,
            mediaRequest, connected, connected && configured && !closed && !disconnecting && active == null, printRequests,
            connectionInfo);
        if (changed != null) changed.run();
    }
    private void begin(DemoResult.Kind kind) { inputError = null; cancelling = false; activeKind = kind; }

}
