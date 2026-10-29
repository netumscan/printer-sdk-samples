// SPDX-License-Identifier: MIT
package com.netumscan.printersdk.samples.quickstart;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.netumscan.printersdk.DiscoveredDevice;
import com.netumscan.printersdk.PrinterManager;
import java.util.ArrayList;
import java.util.List;

/** Customer entry point: permissions, explicit device selection, connect, print and disconnect. */
public final class MainActivity extends Activity {
    private static final int PERMISSION = 10;
    private QuickStartModel model;
    private LinearLayout deviceList;
    private TextView selection, status, printResult, cleanupResult;
    private Button scan, connect, print, cancel, disconnect;
    private Runnable afterPermission;
    private boolean permissionPending;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        // Recreation starts offline with a new Manager. No address, parameters or requests are restored.
        model = new QuickStartModel(new PrinterManager(this), this::render);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int padding = (int)(16 * getResources().getDisplayMetrics().density);
        content.setPadding(padding, padding, padding, padding);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(content);
        setContentView(scroll);
        text(content, "Printer SDK · Quick Start");
        text(content, "开启打印机和手机蓝牙：扫描 → 选择 → 连接 → 打印 → 断开。" +
            "本例连接时明确选择 LT-M90 型号；请先确认实物型号，其他型号不要使用本例。");
        text(content, "固定示例：Bitmap 条纹，页面 48 × 8 mm、1 份、不追加标签定位；连接与打印不会自动设置纸张。");
        scan = button(content, "1. 扫描 BLE 设备（5 秒）", view -> {
            if (!permissionPending && model.canSelect()) withPermission(true, model::scan);
        });
        deviceList = new LinearLayout(this);
        deviceList.setOrientation(LinearLayout.VERTICAL);
        content.addView(deviceList);
        selection = text(content, "尚未选择设备");
        connect = button(content, "2. 连接所选设备", view -> {
            if (!permissionPending && model.canConnect()) withPermission(false, model::connect);
        });
        print = button(content, "3. 打印固定条纹", view -> model.print());
        cancel = button(content, "取消当前请求并断开", view -> model.cancel());
        disconnect = button(content, "4. 断开", view -> model.disconnect());
        status = text(content, "");
        printResult = text(content, "");
        cleanupResult = text(content, "");
        render();
    }

    private void render() {
        if (model.destroyed || status == null) return;
        boolean selectable = !permissionPending && model.canSelect();
        deviceList.removeAllViews();
        selection.setText("尚未选择设备；扫描后请点击你的打印机");
        for (DiscoveredDevice device : model.candidates) {
            boolean selected = device.address.equals(model.selectedAddress);
            String label = (device.name == null ? "未命名设备" : device.name) + " · " +
                device.address.substring(Math.max(0, device.address.length() - 8)) + " · " +
                (device.rssi == null ? "信号未知" : device.rssi + " dBm");
            Button candidate = button(deviceList, (selected ? "✓ " : "") + label, view -> model.select(device.address));
            candidate.setEnabled(selectable);
            if (selected) selection.setText("已选择：" + label +
                "；连接时采用 LT-M90，请确认实物型号");
        }
        scan.setEnabled(selectable && model.scanRequest == null);
        connect.setEnabled(!permissionPending && model.canConnect());
        print.setEnabled(!permissionPending && model.canPrint());
        cancel.setEnabled(!permissionPending && model.canCancel());
        disconnect.setEnabled(!permissionPending && model.canDisconnect());
        status.setText(permissionPending ? "等待附近设备／扫描权限…" : model.status);
        printResult.setText(model.lastPrintResult == null ?
            "尚无打印结果；DATA_SENT 只代表数据已发送，未确认实际打印完成" :
            "最后一次打印结果：" + QuickStartModel.terminalText(model.lastPrintResult));
        cleanupResult.setText(model.lastCleanupResult == null ? "" :
            "最后一次断开结果：" + QuickStartModel.terminalText(model.lastCleanupResult));
    }

    @android.annotation.SuppressLint("InlinedApi")
    static String[] requiredPermissions(boolean scanning) {
        if (Build.VERSION.SDK_INT >= 31) return scanning ?
            new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT} :
            new String[]{Manifest.permission.BLUETOOTH_CONNECT};
        return scanning && Build.VERSION.SDK_INT >= 23 ?
            new String[]{Manifest.permission.ACCESS_FINE_LOCATION} : new String[0];
    }

    private void withPermission(boolean scanning, Runnable action) {
        if (Build.VERSION.SDK_INT < 23) { action.run(); return; }
        List<String> missing = new ArrayList<>();
        for (String permission : requiredPermissions(scanning))
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) missing.add(permission);
        if (missing.isEmpty()) { action.run(); return; }
        afterPermission = action;
        permissionPending = true;
        render();
        requestPermissions(missing.toArray(new String[0]), PERMISSION);
    }

    @Override public void onRequestPermissionsResult(int request, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(request, permissions, grants);
        if (request != PERMISSION || !permissionPending) return;
        Runnable action = afterPermission;
        afterPermission = null;
        permissionPending = false;
        if (!model.foreground || model.destroyed) return;
        boolean granted = grants.length > 0 && grants.length == permissions.length;
        for (int grant : grants) granted &= grant == PackageManager.PERMISSION_GRANTED;
        if (granted && action != null) action.run();
        else model.status = "需要附近设备／扫描权限；授权后请手动重试。Android 6–11 BLE 扫描还需要开启系统定位";
        render();
    }

    private TextView text(LinearLayout parent, String value) {
        TextView view = new TextView(this);
        view.setText(value);
        parent.addView(view);
        return view;
    }

    private Button button(LinearLayout parent, String label, View.OnClickListener action) {
        Button view = new Button(this);
        view.setText(label);
        view.setOnClickListener(action);
        parent.addView(view);
        return view;
    }

    @Override protected void onStart() {
        super.onStart();
        model.start();
    }

    @Override protected void onStop() {
        permissionPending = false;
        afterPermission = null;
        model.stop();
        super.onStop();
    }

    @Override protected void onDestroy() {
        model.close();
        super.onDestroy();
    }
}
