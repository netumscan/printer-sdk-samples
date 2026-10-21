// SPDX-License-Identifier: MIT
package com.netumscan.printersdk.demo;

import com.netumscan.printersdk.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable presentation state. Reading or rendering it never consumes an event. */
final class DemoUiState {
    static final int MAX_PENDING_PRINTS = 8;
    enum Phase { OFFLINE, CONNECTING, READY, WORKING, DISCONNECTING, CLOSED }
    /** 应用登记的打印请求；没有结果只表示等待 SDK 终态，不推断设备队列或走纸状态。 */
    static final class PrintRequest {
        final long sequence;
        final RequestId requestId;
        final String description;
        final boolean cancelling;
        final DemoResult<Void> result;
        PrintRequest(long sequence, RequestId requestId, String description, boolean cancelling, DemoResult<Void> result) {
            this.sequence = sequence; this.requestId = requestId; this.description = description;
            this.cancelling = cancelling; this.result = result;
        }
    }
    /** 本连接的一次介质请求；终态始终与发送时的类型和请求标识绑定。 */
    static final class MediaRequest {
        final PaperType mode;
        final RequestId connectionId, requestId;
        final DemoResult<Void> result;
        MediaRequest(PaperType mode, RequestId connectionId, RequestId requestId, DemoResult<Void> result) {
            this.mode = mode; this.connectionId = connectionId; this.requestId = requestId; this.result = result;
        }
    }
    final Phase phase;
    final boolean foreground, discovering, cancelling, connected;
    /** SDK 为当前连接实际采用的配置；离线时为空，未知配置不会补造型号或参数。 */
    final ConnectionInfo connectionInfo;
    private final boolean printingAllowed;
    final String inputError, selectedID;
    final List<DiscoveredDevice> candidates;
    final DemoResult.Kind activeKind;
    final DemoResult<?> latestResult;
    final DemoResult<?> lastRequest;
    final DemoResult<Void> disconnectedResult, cleanupResult;
    final DemoResult<StatusResult> statusResult;
    final DemoResult<PrinterCapabilities> capabilitiesResult;
    final DemoResult<SdkInfo> identityResult;
    final DemoResult<List<DiscoveredDevice>> discoveryResult;
    final MediaRequest mediaRequest;
    final List<PrintRequest> printRequests;

    DemoUiState(Phase phase, boolean foreground, boolean discovering, boolean cancelling, String inputError,
            String selectedID, List<DiscoveredDevice> candidates, DemoResult.Kind activeKind, DemoResult<?> latestResult,
            DemoResult<?> lastRequest, DemoResult<StatusResult> statusResult,
            DemoResult<PrinterCapabilities> capabilitiesResult, DemoResult<SdkInfo> identityResult,
            DemoResult<List<DiscoveredDevice>> discoveryResult, DemoResult<Void> disconnectedResult, DemoResult<Void> cleanupResult,
            MediaRequest mediaRequest) {
        this(phase, foreground, discovering, cancelling, inputError, selectedID, candidates, activeKind, latestResult,
            lastRequest, statusResult, capabilitiesResult, identityResult, discoveryResult, disconnectedResult, cleanupResult,
            mediaRequest, phase == Phase.READY || phase == Phase.WORKING, phase == Phase.READY, Collections.emptyList());
    }
    DemoUiState(Phase phase, boolean foreground, boolean discovering, boolean cancelling, String inputError,
            String selectedID, List<DiscoveredDevice> candidates, DemoResult.Kind activeKind, DemoResult<?> latestResult,
            DemoResult<?> lastRequest, DemoResult<StatusResult> statusResult,
            DemoResult<PrinterCapabilities> capabilitiesResult, DemoResult<SdkInfo> identityResult,
            DemoResult<List<DiscoveredDevice>> discoveryResult, DemoResult<Void> disconnectedResult, DemoResult<Void> cleanupResult,
            MediaRequest mediaRequest, boolean connected, boolean printingAllowed, List<PrintRequest> printRequests) {
        this(phase, foreground, discovering, cancelling, inputError, selectedID, candidates, activeKind, latestResult,
            lastRequest, statusResult, capabilitiesResult, identityResult, discoveryResult, disconnectedResult, cleanupResult,
            mediaRequest, connected, printingAllowed, printRequests, null);
    }
    DemoUiState(Phase phase, boolean foreground, boolean discovering, boolean cancelling, String inputError,
            String selectedID, List<DiscoveredDevice> candidates, DemoResult.Kind activeKind, DemoResult<?> latestResult,
            DemoResult<?> lastRequest, DemoResult<StatusResult> statusResult,
            DemoResult<PrinterCapabilities> capabilitiesResult, DemoResult<SdkInfo> identityResult,
            DemoResult<List<DiscoveredDevice>> discoveryResult, DemoResult<Void> disconnectedResult, DemoResult<Void> cleanupResult,
            MediaRequest mediaRequest, boolean connected, boolean printingAllowed, List<PrintRequest> printRequests,
            ConnectionInfo connectionInfo) {
        this.phase = phase; this.foreground = foreground; this.discovering = discovering; this.cancelling = cancelling;
        this.connected = connected; this.printingAllowed = printingAllowed;
        this.connectionInfo = connectionInfo;
        this.inputError = inputError; this.selectedID = selectedID;
        this.candidates = Collections.unmodifiableList(new ArrayList<>(candidates));
        this.activeKind = activeKind; this.latestResult = latestResult; this.lastRequest = lastRequest;
        this.statusResult = statusResult; this.capabilitiesResult = capabilitiesResult; this.identityResult = identityResult;
        this.discoveryResult = discoveryResult; this.disconnectedResult = disconnectedResult; this.cleanupResult = cleanupResult;
        this.mediaRequest = mediaRequest;
        this.printRequests = Collections.unmodifiableList(new ArrayList<>(printRequests));
    }
    boolean canConnect() { return foreground && phase == Phase.OFFLINE; }
    boolean canWork() { return foreground && phase == Phase.READY; }
    int pendingPrintCount() {
        int pending = 0;
        for (PrintRequest request : printRequests) if (request.result == null) pending++;
        return pending;
    }
    boolean canSubmitPrint() { return canSubmitPrints(1); }
    boolean canSubmitPrints(int count) {
        return foreground && printingAllowed && count > 0 && count <= MAX_PENDING_PRINTS &&
            pendingPrintCount() <= MAX_PENDING_PRINTS - count;
    }
    boolean canCancel() { return foreground && (phase == Phase.CONNECTING || phase == Phase.WORKING); }
    boolean canDisconnect() { return foreground && (phase == Phase.CONNECTING || phase == Phase.READY || phase == Phase.WORKING || discovering); }
    boolean canDiscover() { return foreground && phase == Phase.OFFLINE; }
    boolean canEditConnection() { return canConnect(); }
    boolean canEditDocument() { return foreground && (phase == Phase.OFFLINE || phase == Phase.READY); }
    boolean canSelectCandidate() { return canEditConnection() && !candidates.isEmpty(); }
}
