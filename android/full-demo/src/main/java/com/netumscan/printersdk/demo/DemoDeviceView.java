// SPDX-License-Identifier: MIT
package com.netumscan.printersdk.demo;

import android.app.Activity;
import android.app.AlertDialog;
import android.widget.TextView;
import com.netumscan.printersdk.ConnectionInfo;
import com.netumscan.printersdk.DiscoveredDevice;
import com.netumscan.printersdk.PrinterModel;

/** Device/model display; no transport, protocol or native settings enter the UI. */
final class DemoDeviceView {
    interface ModelAccepted { void accept(PrinterModel model); }
    private final Activity activity;
    DemoDeviceView(Activity activity) { this.activity = activity; }
    void render(DemoUiState state, String address) {
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
    }
    AlertDialog modelEditor(ModelAccepted accepted) {
        return new AlertDialog.Builder(activity).setTitle("选择实际打印机型号")
            .setMessage("当前仅收录 LT-M90。请确认实物型号；选择不会发送纸张设置或打印。")
            .setPositiveButton("采用 LT-M90", (dialog, which) -> accepted.accept(PrinterModel.LT_M90))
            .setNegativeButton("取消", null).create();
    }
}
