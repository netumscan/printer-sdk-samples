// SPDX-License-Identifier: MIT
package com.netumscan.printersdk.samples.quickstart;

import com.netumscan.printersdk.ConnectionInfo;
import com.netumscan.printersdk.ConnectionOptions;
import com.netumscan.printersdk.DiscoveredDevice;
import com.netumscan.printersdk.ImageLoadContext;
import com.netumscan.printersdk.ImageSource;
import com.netumscan.printersdk.ManagerResult;
import com.netumscan.printersdk.PixelImage;
import com.netumscan.printersdk.PrintOptions;
import com.netumscan.printersdk.PrinterManager;
import com.netumscan.printersdk.PrinterProfile;
import com.netumscan.printersdk.PrinterProfiles;
import com.netumscan.printersdk.RequestId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Main-thread sample state. One Manager owns discovery, requests and the device IO queue. */
final class QuickStartModel {
    static final int SCAN_MS = 10000, CONNECT_TIMEOUT_MS = 10000, PRINT_TIMEOUT_MS = 30000;
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
            scanRequest == null && connectRequest == null && printRequest == null;
    }

    boolean canConnect() {
        DiscoveredDevice selected = selectedDevice();
        return canSelect() && selected != null && PrinterProfiles.resolveBleOptions(selected.name, null) != null;
    }

    boolean canPrint() {
        PrinterProfile profile = connectionInfo == null ? null : connectionInfo.printProfile;
        return foreground && !destroyed && connected && !disconnecting &&
            scanRequest == null && connectRequest == null && printRequest == null &&
            profile != null && profile.protocol == PrinterProfile.Protocol.CPCL &&
            profile.maxPrintWidthDots >= SAMPLE_WIDTH;
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
        if (!canSelect()) return;
        status = "正在扫描 BLE 设备，10 秒后显示结果…";
        scanRequest = manager.scanBle(null, SCAN_MS, result -> {
            if (scanRequest != result.requestId) return;
            scanRequest = null;
            lastScanResult = result;
            if (!destroyed && !disconnecting && foreground) {
                if (result.outcome == ManagerResult.Outcome.SUCCESS) {
                    candidates = result.value == null ? Collections.emptyList() :
                        Collections.unmodifiableList(new ArrayList<>(result.value));
                    // Every scan requires an explicit choice; discovery never connects a device.
                    selectedAddress = null;
                    status = candidates.isEmpty() ? "未发现设备；请检查打印机、手机蓝牙和扫描权限后重试" :
                        "请选择你的打印机，再点击连接；蓝牙名称不认证硬件型号";
                } else status = "扫描：" + terminalText(result);
            }
            notifyChanged();
        });
        notifyChanged();
    }

    void select(String address) {
        if (!canSelect()) return;
        for (DiscoveredDevice candidate : candidates) {
            if (!candidate.address.equals(address)) continue;
            selectedAddress = address;
            status = PrinterProfiles.resolveBleOptions(candidate.name, null) == null ?
                "该名称没有已知 BLE 配置，请使用 Full Demo 显式配置通信与打印参数" :
                "已选择设备；请确认它是你的 LT-M90，再手动连接";
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
        connectRequest = manager.connect(ConnectionOptions.ble(selected), CONNECT_TIMEOUT_MS, result -> {
            if (connectRequest != result.requestId) return;
            connectRequest = null;
            lastConnectResult = result;
            if (result.outcome == ManagerResult.Outcome.SUCCESS && connectionAllowed &&
                    foreground && !destroyed && !disconnecting) {
                connected = true;
                connectionInfo = result.value;
                status = canPrint() ? "已连接；采用名称推荐配置。确认型号与纸张后，手动打印固定示例" :
                    "已连接，但当前打印配置不适用于此示例；请断开并使用 Full Demo";
            } else {
                connected = false;
                connectionInfo = null;
                // A cancellation callback may precede completion of the platform open call.
                if (!destroyed) disconnect();
            }
            notifyChanged();
        }, result -> {
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
        });
        connectionRequestId = connectRequest;
        notifyChanged();
    }

    void print() {
        if (!canPrint()) return;
        status = "正在准备并发送固定 GRAY8 条纹（384 × 64 dots，1 份，FORM 关闭）…";
        // Manager retains the source before returning; release the application's reference now.
        try (ImageSource source = ImageSource.deferred(context -> PixelImage.copyOf(
                stripePixels(SAMPLE_WIDTH, SAMPLE_HEIGHT, context), SAMPLE_WIDTH, SAMPLE_HEIGHT, PixelImage.GRAY8))) {
            PrintOptions options = PrintOptions.labelDots(SAMPLE_WIDTH, SAMPLE_HEIGHT)
                .copies(1).nextLabel(false).maxPageHeightDots(SAMPLE_HEIGHT).timeoutMs(PRINT_TIMEOUT_MS)
                .addImage(source, 0, 0).build();
            printRequest = manager.print(options, result -> {
                if (printRequest != result.requestId) return;
                printRequest = null;
                // Preserve the SDK terminal separately from cancellation, cleanup and connection loss.
                lastPrintResult = result;
                if (!destroyed && !disconnecting)
                    status = connected ? "请求已结束；可手动再次打印或断开" : "未连接；请手动连接";
                notifyChanged();
            });
        }
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
        cleanupRequest = manager.disconnect(result -> {
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
        });
        notifyChanged();
    }

    void close() {
        if (destroyed) return;
        destroyed = true;
        foreground = connected = connectionAllowed = false;
        connectionInfo = null;
        changed = null; // The Manager can finish without retaining an Activity or its Views.
        closeRequest = manager.close(result -> {
            if (closeRequest != result.requestId) return;
            closeRequest = null;
            lastCloseResult = result;
            disconnecting = false;
        });
    }

    static byte[] stripePixels(int width, int height, ImageLoadContext context) {
        byte[] pixels = new byte[width * height];
        for (int y = 0; y < height; y++) {
            context.throwIfCancelled();
            for (int x = 0; x < width; x++)
                pixels[y * width + x] = (byte)(((x / 16 + y / 16) % 2 == 0) ? 0 : 255);
        }
        context.throwIfCancelled();
        return pixels;
    }

    static String terminalText(ManagerResult<?> result) {
        return result.requestId + "：" + result.outcome + " code=" + result.code +
            " completion=" + result.completion + " outputMayHaveOccurred=" + result.outputMayHaveOccurred +
            "；" + result.message + (result.completion == ManagerResult.Completion.DATA_SENT ?
                "；DATA_SENT：数据已发送，未确认实际打印完成，请检查实际出纸" : "") +
            (result.outputMayHaveOccurred ? "；可能已产生输出，不会自动重打" : "");
    }

    private void notifyChanged() { if (!destroyed && changed != null) changed.run(); }
}
