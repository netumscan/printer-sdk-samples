// SPDX-License-Identifier: MIT
package com.netumscan.printersdk.demo;

import android.app.Activity;
import android.app.AlertDialog;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.netumscan.printersdk.ConnectionInfo;
import com.netumscan.printersdk.DiscoveredDevice;
import com.netumscan.printersdk.PrinterModel;

/** Device/model display; no transport, protocol or native settings enter the UI. */
final class DemoDeviceView {
    interface ModelAccepted { void accept(PrinterModel model); }
    interface CandidateSelected { void select(String address); }
    private final Activity activity;
    private final CandidateSelected selected;
    DemoDeviceView(Activity activity, CandidateSelected selected) { this.activity = activity; this.selected = selected; }
    void render(DemoUiState state, String address) {
        render(state, address, true);
    }
    void render(DemoUiState state, String address, boolean interactionAllowed) {
        activity.findViewById(R.id.connect).setEnabled(interactionAllowed && state.canConnect());
        ConnectionInfo adopted = state.connectionInfo;
        String name = adopted == null ? null : adopted.device.name;
        if (name == null) for (DiscoveredDevice candidate : state.candidates) {
            if (candidate.address.equals(state.selectedID) && candidate.address.equalsIgnoreCase(address)) {
                name = candidate.name; break;
            }
        }
        String device = name == null ? "未命名设备" : name;
        String summary = state.phase == DemoUiState.Phase.DISCONNECTING ? "正在断开 " + device :
            state.phase == DemoUiState.Phase.CONNECTING ? "正在连接 " + device :
            state.connected ? "已连接 " + device : address.isEmpty() ? "尚未选择设备" : device + " · 待连接";
        ((TextView)activity.findViewById(R.id.device_summary)).setText(summary);
        ((TextView)activity.findViewById(R.id.profile_summary)).setText(!state.connected ? "连接时按名称推荐，或明确选择 LT-M90" :
            adopted == null || adopted.model == null ? "型号未知；请确认实物型号后选择" :
            "已采用 LT-M90；纸张设置由独立按钮发送，名称不认证硬件型号");
        String count = "已发现 " + state.candidates.size() + " 台设备";
        String discovery = state.discovering ? "正在扫描／读取设备，" + count + "；可先选择，结束后手动连接" :
            state.discoveryResult != null && state.discoveryResult.raw.outcome != com.netumscan.printersdk.ManagerResult.Outcome.SUCCESS
                ? "查找设备：" + state.discoveryResult.raw.outcome + "（" + state.discoveryResult.raw.code + "）；" + count :
            state.candidates.isEmpty() ? "尚无候选；请扫描 BLE 或读取已配对设备" : count + "；选择设备后手动连接";
        ((TextView)activity.findViewById(R.id.discovery_status)).setText(discovery);
        LinearLayout rows = activity.findViewById(R.id.discovery_devices);
        rows.removeAllViews();
        for (DiscoveredDevice candidate : state.candidates) {
            Button row = new Button(activity);
            row.setText((candidate.address.equals(state.selectedID) ? "✓ " : "") +
                (candidate.name == null || candidate.name.isEmpty() ? "未命名设备" : candidate.name) + " · " + candidate.address +
                " · " + (candidate.rssi == null ? "信号未知" : candidate.rssi + " dBm"));
            row.setEnabled(interactionAllowed && state.canSelectCandidate());
            row.setOnClickListener(view -> selected.select(candidate.address));
            rows.addView(row);
        }
    }
    AlertDialog modelEditor(ModelAccepted accepted) {
        return new AlertDialog.Builder(activity).setTitle("选择实际打印机型号")
            .setMessage("当前仅收录 LT-M90。请确认实物型号；选择不会发送纸张设置或打印。")
            .setPositiveButton("采用 LT-M90", (dialog, which) -> accepted.accept(PrinterModel.LT_M90))
            .setNegativeButton("取消", null).create();
    }
}
