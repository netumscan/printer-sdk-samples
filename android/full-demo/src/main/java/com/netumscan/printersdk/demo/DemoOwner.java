// SPDX-License-Identifier: MIT
package com.netumscan.printersdk.demo;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import com.netumscan.printersdk.PrinterManager;
import com.netumscan.printersdk.RequestId;
import com.netumscan.printersdk.ConnectionInfo;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.IdentityHashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 跨 Activity 配置重建保留的会话资源与主线程界面状态。
 * 配置重建和受控选图临时离开会保留连接；普通后台则断开设备并取消图片准备。
 */
final class DemoOwner implements AutoCloseable {
    final DemoModel model;
    final DemoImages images;
    final SettingsStore settings;
    final DemoJournal journal;
    // 配置与导出等文件任务使用独立执行器，不占用界面线程或设备 IO 队列。
    final ExecutorService utilities;
    final ArrayDeque<String> lines = new ArrayDeque<>();
    private final Context application;
    private final Handler main;
    // 按结果对象身份去重，避免每次 render 都重复写入同一条终态日志。
    private final IdentityHashMap<DemoResult<?>, Boolean> recorded = new IdentityHashMap<>();
    private final IdentityHashMap<RequestId, Boolean> recordedPrints = new IdentityHashMap<>();
    private Runnable changed;
    private boolean closed;
    private boolean exportPickerPending, exportPickerOpen, logsCleared;
    private long exportGeneration;
    boolean utilityBusy, pickerOpen;
    private boolean pickerValid;
    Uri importUri;
    private Uri exportUri;
    String notice, exportText;
    DemoSettings pendingSettings;
    java.util.Map<String, ?> pendingLegacy;
    long connectionNumber;

    DemoOwner(PrinterManager manager) { this(manager, null); }
    DemoOwner(PrinterManager manager, Context context) {
        if (context == null) {
            application = null;
            images = null; settings = null; journal = null; utilities = null; main = null;
        } else {
            application = context.getApplicationContext();
            main = new Handler(Looper.getMainLooper());
            images = new DemoImages(application);
            settings = new SettingsStore(application.getNoBackupFilesDir());
            journal = new DemoJournal(new java.io.File(application.getNoBackupFilesDir(), "demo-journal"));
            utilities = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(4), task -> {
                Thread thread = new Thread(task, "PrinterDemo.files"); thread.setDaemon(true); return thread;
            });
        }
        model = utilities == null ? new DemoModel(manager) : new DemoModel(manager, utilities);
    }
    void attach(Runnable listener) { if (!closed) changed = listener; }
    void notifyChanged() { if (!closed && changed != null) changed.run(); }
    /** 日志交给 journal 异步记录，回调切回主线程更新有长度上限的界面记录。 */
    void log(String message) {
        if (closed || journal == null) return;
        journal.append(message, connectionNumber, line -> main.post(() -> {
            if (closed) return;
            lines.addLast(line);
            while (lines.size() > 160) lines.removeFirst();
            notifyChanged();
        }));
    }
    /** 分别记录请求、查询、断开和清理等带类型的结果，不把 cleanup 当作原请求结果。 */
    void recordResults() {
        DemoUiState state = model.state();
        IdentityHashMap<DemoResult<?>, Boolean> kept = new IdentityHashMap<>();
        for (DemoResult<?> result : model.resultHistory()) {
            kept.put(result, Boolean.TRUE);
            if (recorded.put(result, Boolean.TRUE) == null && result.kind != DemoResult.Kind.PRINT) logResult(result);
        }
        recorded.keySet().retainAll(kept.keySet());
        IdentityHashMap<RequestId, Boolean> retained = new IdentityHashMap<>();
        for (DemoUiState.PrintRequest request : state.printRequests) {
            retained.put(request.requestId, Boolean.TRUE);
            if (request.result != null && recordedPrints.put(request.requestId, Boolean.TRUE) == null)
                logResult(request.result);
        }
        recordedPrints.keySet().retainAll(retained.keySet());
    }
    private void logResult(DemoResult<?> result) {
        String profile = result.raw.value instanceof ConnectionInfo
            ? " " + DemoFormatter.profileDetails((ConnectionInfo)result.raw.value) : "";
        log(result.kind + " outcome=" + result.raw.outcome + " code=" + result.raw.code +
            " completion=" + result.raw.completion + " outputMayHaveOccurred=" + result.raw.outputMayHaveOccurred + profile);
    }
    /** 文件任务只保留 Owner 与 application Context，重建后的界面消费主线程结果。 */
    private interface FileWork { Runnable run() throws Exception; }
    private boolean utility(FileWork work) {
        if (closed || utilityBusy || utilities == null) return false;
        utilityBusy = true;
        notifyChanged();
        try {
            utilities.execute(() -> {
                Runnable completion;
                try { completion = work.run(); }
                catch (Exception failure) {
                    if (failure instanceof InterruptedException) Thread.currentThread().interrupt();
                    completion = () -> notice = "文件操作失败；当前输入未改变";
                }
                Runnable result = completion;
                main.post(() -> {
                    if (closed) return;
                    result.run();
                    utilityBusy = false;
                    notifyChanged();
                });
            });
            return true;
        } catch (RejectedExecutionException failure) {
            utilityBusy = false;
            notice = "文件操作繁忙，请稍后重试";
            notifyChanged();
            return false;
        }
    }
    boolean saveSettings(DemoSettings snapshot) {
        return utility(() -> {
            settings.save(snapshot);
            return () -> notice = "参数已保存（不含设备地址、Raw 或图片）";
        });
    }
    boolean loadSettings() {
        return utility(() -> {
            DemoSettings loaded = settings.load();
            return () -> pendingSettings = loaded;
        });
    }
    boolean clearSettings() {
        return utility(() -> {
            settings.clear();
            return () -> notice = "已清除保存配置；当前输入保留";
        });
    }
    /** preferencesName 对应 Activity.getLocalClassName()，继续读取既有 getPreferences 文件。 */
    boolean loadLegacySettings(String preferencesName) {
        return utility(() -> {
            java.util.Map<String, ?> loaded = new LinkedHashMap<>(
                application.getSharedPreferences(preferencesName, Context.MODE_PRIVATE).getAll());
            return () -> pendingLegacy = loaded;
        });
    }
    /** 快照与打开意图先交付 Owner，旧 Activity 的销毁不会丢弃已发起的导出。 */
    boolean prepareLogExport() {
        if (closed || utilityBusy || exportPickerPending || exportPickerOpen || exportUri != null) return false;
        long generation = ++exportGeneration;
        String identity = DemoFormatter.identity(model.state().identityResult);
        return utility(() -> {
            String snapshot = identity + "\n硬件状态未知；以下为本地应用日志\n" + journal.snapshot().get();
            return () -> {
                if (generation != exportGeneration) return;
                exportText = snapshot;
                exportPickerPending = true;
            };
        });
    }
    /** 仅由当前前台 Activity 在空闲时消费一次，并在启动选择器之前标记为已打开。 */
    boolean takeExportPickerRequest() {
        if (closed || utilityBusy || !exportPickerPending || exportText == null) return false;
        exportPickerPending = false;
        exportPickerOpen = true;
        return true;
    }
    /** 系统结果可早于 onStart 到达，选中的目标先留在 Owner，恢复前台后再写入。 */
    void acceptLogExport(Uri uri) {
        if (closed) return;
        exportPickerOpen = false;
        exportPickerPending = false;
        exportUri = exportText == null ? null : uri;
        if (exportUri == null) exportText = null;
    }
    /** 先成功排队，再取走数据；拒绝或忙碌时保留待写结果供下一次前台通知消费。 */
    boolean writeLogExport() {
        if (closed || utilityBusy || exportUri == null || exportText == null) return false;
        Uri uri = exportUri;
        String snapshot = exportText;
        boolean accepted = utility(() -> {
            try (OutputStream output = application.getContentResolver().openOutputStream(uri, "wt")) {
                if (output == null) throw new IOException("No output stream");
                output.write(snapshot.getBytes(StandardCharsets.UTF_8));
            }
            return () -> notice = "日志已导出";
        });
        if (accepted) { exportUri = null; exportText = null; }
        return accepted;
    }
    boolean clearLogs() {
        return utility(() -> {
            journal.clear().get();
            return () -> {
                lines.clear(); logsCleared = true;
                notice = journal.degraded() ? "内存日志已清除；磁盘清除失败" : "日志已清除";
            };
        });
    }
    boolean takeLogsCleared() {
        boolean cleared = logsCleared;
        logsCleared = false;
        return cleared;
    }
    /** 只有显式发起且仍有效的选择器可交回数据；从不因此重连或打印。 */
    boolean beginImagePicker() {
        if (closed || pickerOpen || importUri != null) return false;
        pickerOpen = true; pickerValid = true;
        return true;
    }
    void acceptImageResult(Uri uri) {
        if (closed) return;
        boolean accept = pickerOpen && pickerValid;
        pickerOpen = false; pickerValid = false;
        if (accept) importUri = uri;
        if (accept && uri == null) notice = "已取消导入；保留上一有效图片，未发送";
    }
    void abandonImageFlow() {
        pickerValid = false; importUri = null;
        if (images != null) images.cancel();
        // 已打开的系统选择器仍占用其返回槽，不能误接成新流程。
    }
    /** 配置重建或受控选图只解绑界面；普通后台触发设备断开和图片取消。 */
    void stopUi(Runnable listener, boolean changingConfiguration) {
        model.detach(listener);
        if (images != null) images.detach(listener);
        // 按 listener 身份解绑，防止旧 Activity 的回调移除新 Activity 已注册的监听。
        if (changed == listener) changed = null;
        if (!changingConfiguration) {
            // Home 后不自动弹出尚未打开的保存选择器；已打开的系统选择器可正常返回结果。
            if (!exportPickerOpen && exportUri == null) {
                ++exportGeneration;
                exportPickerPending = false;
                exportText = null;
            }
            // 选择器覆盖与选择器中按 Home 都可能触发同一次 onStop；整个受控选图流程保留连接。
            if (!pickerOpen) {
                importUri = null; pickerValid = false;
                model.background();
                if (images != null) images.background();
            }
        }
    }
    /** 配置重建继续保留本对象，Activity 真正销毁时释放会话资源。 */
    void destroyUi(Runnable listener, boolean changingConfiguration) {
        model.detach(listener);
        if (images != null) images.detach(listener);
        if (changed == listener) changed = null;
        if (!changingConfiguration) close();
    }
    /** 幂等关闭各资源的入口；各执行器按自身的关闭策略完成收尾。 */
    @Override public void close() {
        if (closed) return;
        closed = true; changed = null;
        model.close();
        if (images != null) images.close();
        if (utilities != null) utilities.shutdown();
        if (journal != null) journal.close();
    }
}
