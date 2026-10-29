// SPDX-License-Identifier: MIT
package com.netumscan.printersdk.samples.quickstart;

import android.graphics.Bitmap;
import com.netumscan.printersdk.ConnectionInfo;
import com.netumscan.printersdk.PrinterModel;
import com.netumscan.printersdk.DiscoveredDevice;
import com.netumscan.printersdk.ManagerResult;
import com.netumscan.printersdk.PrinterManager;
import com.netumscan.printersdk.PrinterListener;
import com.netumscan.printersdk.RequestId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Main-thread sample state. One Manager owns discovery, requests and the device IO queue. */
final class QuickStartModel implements PrinterListener {
    static final int SAMPLE_WIDTH = 384, SAMPLE_HEIGHT = 64;
    private final PrinterManager manager;
    private Runnable changed;
    private RequestId connectionRequestId;
    private boolean connectionAllowed;
    boolean foreground, destroyed, connected, disconnecting;
    RequestId scanRequest, connectRequest, printRequest, cleanupRequest, closeRequest;
    ManagerResult<List<DiscoveredDevice>> lastScanResult;
    ManagerResult<ConnectionInfo> lastConnectResult;
    ManagerResult<Void> lastPrintResult, lastCleanupResult, lastConnectionLoss, lastCloseResult;
    ConnectionInfo connectionInfo;
    List<DiscoveredDevice> candidates = Collections.emptyList();
    String selectedAddress;
    String status = "未连接；不会自动重连、续打或重放";

    QuickStartModel(PrinterManager manager, Runnable changed) {
        this.manager = java.util.Objects.requireNonNull(manager);
        this.changed = java.util.Objects.requireNonNull(changed);
        manager.setListener(this);
    }

    void start() {
        if (destroyed) return;
        foreground = true;
        notifyChanged();
    }

    void stop() {
        if (destroyed) return;
        foreground = false;
        connectionAllowed = false;
        if (connected || scanRequest != null || connectRequest != null || printRequest != null) disconnect();
        else notifyChanged();
    }

    boolean canSelect() {
        return foreground && !destroyed && !connected && !disconnecting &&
            connectRequest == null && printRequest == null;
    }

    boolean canConnect() {
        DiscoveredDevice selected = selectedDevice();
        return canSelect() && scanRequest == null && selected != null;
    }

    boolean canPrint() {
        PrinterModel model = connectionInfo == null ? null : connectionInfo.model;
        return foreground && !destroyed && connected && !disconnecting &&
            scanRequest == null && connectRequest == null && printRequest == null &&
            model == PrinterModel.LT_M90;
    }

    boolean canCancel() {
        return foreground && !destroyed && !disconnecting &&
            (scanRequest != null || connectRequest != null || printRequest != null);
    }

    boolean canDisconnect() {
        return foreground && !destroyed && !disconnecting &&
            (connected || scanRequest != null || connectRequest != null || printRequest != null);
    }

    void scan() {
        if (!canSelect() || scanRequest != null) return;
        candidates = Collections.emptyList(); selectedAddress = null; lastScanResult = null;
        status = "正在扫描 BLE 设备，实时显示候选，5 秒后结束…";
        scanRequest = manager.scanBle();
        notifyChanged();
    }

    void select(String address) {
        if (!canSelect()) return;
        for (DiscoveredDevice candidate : candidates) {
            if (!candidate.address.equals(address)) continue;
            selectedAddress = address;
            status = scanRequest != null ? "正在扫描 BLE；已选择设备，扫描结束后再手动连接" :
                "已选择设备；请确认实物为 LT-M90，再手动连接（本例明确指定该型号）";
            notifyChanged();
            return;
        }
    }

    DiscoveredDevice selectedDevice() {
        for (DiscoveredDevice candidate : candidates)
            if (candidate.address.equals(selectedAddress)) return candidate;
        return null;
    }

    void connect() {
        if (!canConnect()) return;
        DiscoveredDevice selected = selectedDevice();
        lastConnectResult = null;
        lastConnectionLoss = null;
        connectionAllowed = true;
        status = "正在连接所选设备…";
        connectRequest = manager.connectBle(selected, PrinterModel.LT_M90);
        connectionRequestId = connectRequest;
        notifyChanged();
    }

    void print() {
        if (!canPrint()) return;
        status = "正在准备并发送固定条纹（页面 48 × 8 mm，1 份，不追加标签定位）…";
        Bitmap bitmap = stripeBitmap(SAMPLE_WIDTH, SAMPLE_HEIGHT);
        try {
            // Acceptance fixes the Bitmap snapshot before returning; the App can release its copy now.
            printRequest = manager.printImage(bitmap, 48, 8);
        } finally { bitmap.recycle(); }
        notifyChanged();
    }

    void cancel() {
        if (!canCancel()) return;
        RequestId request = scanRequest != null ? scanRequest : connectRequest != null ? connectRequest : printRequest;
        manager.cancel(request);
        // Keep a cleanup barrier before allowing another connection after cancellation.
        disconnect();
    }

    void disconnect() {
        if (destroyed || disconnecting) return;
        connectionAllowed = false;
        disconnecting = true;
        connected = false;
        connectionInfo = null;
        status = "正在断开，等待已有请求终态；已发送数据无法撤回…";
        cleanupRequest = manager.disconnect();
        notifyChanged();
    }

    void close() {
        if (destroyed) return;
        destroyed = true;
        foreground = connected = connectionAllowed = false;
        connectionInfo = null;
        changed = null; // The Manager can finish without retaining an Activity or its Views.
        closeRequest = manager.close();
    }

    static Bitmap stripeBitmap(int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        int[] row = new int[width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++)
                row[x] = ((x / 16 + y / 16) % 2 == 0) ? 0xff000000 : 0xffffffff;
            bitmap.setPixels(row, 0, width, 0, y, width, 1);
        }
        return bitmap;
    }

    static String terminalText(ManagerResult<?> result) {
        return result.requestId + "：" + result.outcome + " code=" + result.code +
            " completion=" + result.completion + " outputMayHaveOccurred=" + result.outputMayHaveOccurred +
            "；" + result.message + (result.completion == ManagerResult.Completion.DATA_SENT ?
                "；DATA_SENT：数据已发送，未确认实际打印完成，请检查实际出纸" : "") +
            (result.outputMayHaveOccurred ? "；可能已产生输出，不会自动重打" : "");
    }

    @Override public void onScanCompleted(ManagerResult<List<DiscoveredDevice>> result) {
        if (scanRequest != result.requestId) return;
        scanRequest = null;
        lastScanResult = result;
        if (!destroyed && !disconnecting && foreground) {
            if (result.outcome == ManagerResult.Outcome.SUCCESS) {
                candidates = result.value == null ? Collections.emptyList() :
                    Collections.unmodifiableList(new ArrayList<>(result.value));
                // Preserve an explicit choice from this scan only while it is in the final snapshot.
                if (selectedDevice() == null) selectedAddress = null;
                status = candidates.isEmpty() ? "未发现设备；请检查打印机、手机蓝牙和扫描权限后重试" :
                    selectedAddress == null ? "请选择你的打印机，再点击连接；蓝牙名称不认证硬件型号" :
                    "扫描结束，已保留所选设备；请确认实物为 LT-M90，再手动连接";
            } else status = "扫描：" + terminalText(result);
        }
        notifyChanged();
    }

    @Override public void onConnectionCompleted(ManagerResult<ConnectionInfo> result) {
        if (connectRequest != result.requestId) return;
        connectRequest = null;
        lastConnectResult = result;
        if (result.outcome == ManagerResult.Outcome.SUCCESS && connectionAllowed &&
                foreground && !destroyed && !disconnecting) {
            connected = true;
            connectionInfo = result.value;
            status = canPrint() ? "已连接；采用明确选择的 LT-M90。确认纸张后，手动打印固定示例" :
                "已连接，但当前打印配置不适用于此示例；请断开并使用 Full Demo";
        } else {
            connected = false;
            connectionInfo = null;
            // A cancellation callback may precede completion of the platform open call.
            if (!destroyed) disconnect();
        }
        notifyChanged();
    }

    @Override public void onDisconnected(ManagerResult<Void> result) {
        if (connectionRequestId != result.requestId) return;
        lastConnectionLoss = result;
        connected = false;
        connectionInfo = null;
        connectionAllowed = false;
        if (!destroyed) {
            status = "连接中断：" + terminalText(result) + "；请手动连接，不会重放已有请求";
            disconnect();
        }
        notifyChanged();
    }

    @Override public void onPrintCompleted(ManagerResult<Void> result) {
        if (printRequest != result.requestId) return;
        printRequest = null;
        lastPrintResult = result;
        if (!destroyed && !disconnecting)
            status = connected ? "请求已结束；可手动再次打印或断开" : "未连接；请手动连接";
        notifyChanged();
    }

    @Override public void onDisconnectCompleted(ManagerResult<Void> result) {
        if (cleanupRequest != result.requestId) return;
        cleanupRequest = null;
        lastCleanupResult = result;
        disconnecting = false;
        connected = false;
        connectionInfo = null;
        if (!destroyed) {
            String cleanup = result.outcome == ManagerResult.Outcome.SUCCESS ?
                "已断开；请手动连接，不会自动重打" : "断开清理：" + terminalText(result);
            status = lastConnectionLoss != null ? "连接中断：" + terminalText(lastConnectionLoss) + "；" + cleanup :
                lastConnectResult != null && lastConnectResult.outcome != ManagerResult.Outcome.SUCCESS ?
                "连接：" + terminalText(lastConnectResult) + "；" + cleanup : cleanup;
        }
        notifyChanged();
    }

    @Override public void onCloseCompleted(ManagerResult<Void> result) {
        if (closeRequest != result.requestId) return;
        closeRequest = null;
        lastCloseResult = result;
        disconnecting = false;
    }

    @Override public void onDeviceDiscovered(RequestId id, DiscoveredDevice device) {
        if (scanRequest != id || destroyed || disconnecting || !foreground) return;
        ArrayList<DiscoveredDevice> discovered = new ArrayList<>(candidates);
        int existing = -1;
        for (int index = 0; index < discovered.size(); index++)
            if (discovered.get(index).address.equals(device.address)) { existing = index; break; }
        if (existing < 0) discovered.add(device); else discovered.set(existing, device);
        candidates = Collections.unmodifiableList(discovered);
        notifyChanged();
    }

    private void notifyChanged() { if (!destroyed && changed != null) changed.run(); }
}
