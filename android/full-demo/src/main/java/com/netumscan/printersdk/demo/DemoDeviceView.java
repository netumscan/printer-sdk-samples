// SPDX-License-Identifier: MIT
package com.netumscan.printersdk.demo;

import android.app.Activity;
import android.app.AlertDialog;
import android.text.InputType;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import com.netumscan.printersdk.ConnectionInfo;
import com.netumscan.printersdk.DiscoveredDevice;
import com.netumscan.printersdk.PrinterProfile;

/** 设备配置的展示与编辑；不拥有 Manager，也不发送打印机命令。 */
final class DemoDeviceView {
    interface ProfileAccepted { void accept(PrinterProfile profile); }
    private final Activity activity;

    DemoDeviceView(Activity activity) { this.activity = activity; }

    void render(DemoUiState state, String address) {
        ConnectionInfo adopted = state.connectionInfo;
        String name = adopted == null ? null : adopted.deviceName;
        if (name == null) for (DiscoveredDevice candidate : state.candidates) {
            if (candidate.address.equals(state.selectedID) && candidate.address.equalsIgnoreCase(address)) {
                name = candidate.name; break;
            }
        }
        String device = name == null ? "未命名设备" : name;
        String summary;
        if (state.phase == DemoUiState.Phase.DISCONNECTING) summary = "正在断开 " + device;
        else if (state.phase == DemoUiState.Phase.CONNECTING) summary = "正在连接 " + device;
        else if (state.connected) summary = "已连接 " + device;
        else if (address.isEmpty()) summary = "尚未选择设备";
        else summary = name == null ? "已选择地址" : device + " · 待连接";
        ((TextView)activity.findViewById(R.id.device_summary)).setText(summary);

        PrinterProfile profile = adopted == null ? null : adopted.printProfile;
        String details;
        if (!state.connected) details = "连接后自动识别";
        else if (profile == null) details = "未识别，请补充打印配置";
        else details = (profile.modelId == null ? "自定义" : profile.modelId) + " · " +
            (profile.protocol == PrinterProfile.Protocol.CPCL ? "CPCL" : "ESC/POS") + " · " + profile.dpi +
            " DPI\n" + profile.dotsPerMm + " dots/mm · 最大 " + profile.maxPrintWidthDots + " dots · " +
            (profile.labelAlignment == PrinterProfile.LabelAlignment.CENTERED ? "固定居中" : "左侧起点") + "\n" +
            (adopted.configurationSource == ConnectionInfo.ConfigurationSource.NAME_RECOMMENDATION
                ? "名称匹配" : "手动配置");
        ((TextView)activity.findViewById(R.id.profile_summary)).setText(details);
    }

    /** App 确认输入后交给 Model 配置；对话框本身不执行 SDK 操作。 */
    AlertDialog profileEditor(PrinterProfile current, PrinterProfile draft, ProfileAccepted accepted) {
        PrinterProfile initial = current == null ? draft : current;
        LinearLayout fields = new LinearLayout(activity); fields.setOrientation(LinearLayout.VERTICAL);
        int padding = Math.round(20 * activity.getResources().getDisplayMetrics().density);
        fields.setPadding(padding, 0, padding, 0);
        Spinner protocol = new Spinner(activity);
        protocol.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item,
            new String[]{"CPCL", "ESC/POS"}));
        protocol.setSelection(initial != null && initial.protocol == PrinterProfile.Protocol.ESC_POS ? 1 : 0);
        fields.addView(protocol);
        EditText dpi = numberField(fields, "分辨率（DPI）", initial == null ? "" : Integer.toString(initial.dpi));
        EditText width = numberField(fields, "设备最大打印宽度（dots）",
            initial == null ? "" : Integer.toString(initial.maxPrintWidthDots));
        EditText preciseDensity = numberField(fields, "点密度（dots/mm，留空按 DPI）",
            initial == null ? "" : Double.toString(initial.dotsPerMm));
        preciseDensity.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        Spinner alignment = new Spinner(activity);
        alignment.setContentDescription("标签定位规则");
        alignment.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item,
            new String[]{"左侧起点", "固定居中"}));
        alignment.setSelection(initial != null && initial.labelAlignment == PrinterProfile.LabelAlignment.CENTERED ? 1 : 0);
        fields.addView(alignment);
        protocol.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                preciseDensity.setEnabled(position == 0); alignment.setEnabled(position == 0);
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });
        AlertDialog dialog = new AlertDialog.Builder(activity)
            .setTitle(current == null ? "补充打印配置" : "更正打印配置")
            .setView(fields).setPositiveButton("应用并初始化", null).setNegativeButton("取消", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            try {
                int density = Integer.parseInt(dpi.getText().toString().trim());
                int maximum = Integer.parseInt(width.getText().toString().trim());
                String suppliedDensity = preciseDensity.getText().toString().trim();
                double dotsPerMm = suppliedDensity.isEmpty() ? density / 25.4 : Double.parseDouble(suppliedDensity);
                accepted.accept(protocol.getSelectedItemPosition() == 0
                    ? PrinterProfile.cpcl(density, maximum, dotsPerMm, alignment.getSelectedItemPosition() == 1
                        ? PrinterProfile.LabelAlignment.CENTERED : PrinterProfile.LabelAlignment.LEFT)
                    : PrinterProfile.escPos(density, maximum));
                dialog.dismiss();
            } catch (IllegalArgumentException invalid) { width.setError("请填写有效 DPI、点密度和设备最大宽度（8 的倍数）"); }
        }));
        return dialog;
    }

    private EditText numberField(LinearLayout fields, String label, String value) {
        EditText input = new EditText(activity); input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint(label); input.setContentDescription(label); input.setText(value); fields.addView(input);
        return input;
    }
}
