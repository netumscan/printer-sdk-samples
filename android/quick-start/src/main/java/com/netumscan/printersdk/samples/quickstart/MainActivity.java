// SPDX-License-Identifier: MIT
package com.netumscan.printersdk.samples.quickstart;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Minimal explicit SPP/CPCL integration. No discovery, printing, or reconnect on launch. */
public final class MainActivity extends Activity {
    private static final int CONNECT_PERMISSION = 10;
    private QuickStartSession session;
    private TextView status;
    private EditText address, dpi, width;
    private boolean destroyed;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int padding = (int)(16 * getResources().getDisplayMetrics().density);
        content.setPadding(padding, padding, padding, padding);
        ScrollView scroll = new ScrollView(this); scroll.addView(content); setContentView(scroll);
        text(content, "Printer SDK · Android Quick Start");
        text(content, "先在 Android 蓝牙设置中配对支持 SPP / CPCL 的打印机。以下 DPI 和最大宽度是示例值，需按设备文档确认。点击打印会实际出纸。");
        address = input(content, "蓝牙地址 AA:BB:CC:DD:EE:FF", "", false);
        dpi = input(content, "DPI", "203", true);
        width = input(content, "设备最大可打印宽度（dots，8 的倍数）", "576", true);
        button(content, "授权并连接", view -> connect());
        button(content, "打印测试条纹（1 份）", view -> session.print());
        button(content, "取消当前任务", view -> session.cancel());
        button(content, "查看连接、工作和队列状态", view -> session.connectionState());
        button(content, "关闭连接", view -> session.disconnect());
        status = text(content, "未连接；SDK 0.3.0-preview.2 · native 0.3.0 / ABI 3/r1；硬件未知");
        session = new QuickStartSession(this, message -> runOnUiThread(() -> {
            if (!destroyed) status.setText(message);
        }));
    }

    private TextView text(LinearLayout content, String value) {
        TextView view = new TextView(this); view.setText(value); content.addView(view); return view;
    }

    private EditText input(LinearLayout content, String hint, String value, boolean numeric) {
        text(content, hint);
        EditText view = new EditText(this);
        view.setSingleLine(true); view.setText(value); view.setHint(hint);
        view.setInputType(numeric ? InputType.TYPE_CLASS_NUMBER : InputType.TYPE_CLASS_TEXT);
        content.addView(view); return view;
    }

    private void button(LinearLayout content, String title, View.OnClickListener action) {
        Button view = new Button(this); view.setText(title); view.setOnClickListener(action); content.addView(view);
    }

    private void connect() {
        if (Build.VERSION.SDK_INT >= 31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] { Manifest.permission.BLUETOOTH_CONNECT }, CONNECT_PERMISSION);
            return;
        }
        try {
            session.connect(address.getText().toString().trim(), Integer.parseInt(dpi.getText().toString()),
                Integer.parseInt(width.getText().toString()));
        } catch (NumberFormatException error) { status.setText("请填写有效的 DPI 和宽度整数"); }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode != CONNECT_PERMISSION) return;
        if (results.length == 1 && results[0] == PackageManager.PERMISSION_GRANTED) connect();
        else status.setText("需要蓝牙连接权限；授权后请手动重试");
    }

    @Override public void onDestroy() {
        destroyed = true;
        if (session != null) session.close();
        super.onDestroy();
    }
}
