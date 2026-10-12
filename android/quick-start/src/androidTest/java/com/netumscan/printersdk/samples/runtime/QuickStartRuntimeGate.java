// SPDX-License-Identifier: MIT
package com.netumscan.printersdk.samples.runtime;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.json.JSONObject;

/** Launch the real Quick Start without user actions or Bluetooth authorization. */
public final class QuickStartRuntimeGate extends Instrumentation {
    private String nonce;
    @Override public void onCreate(Bundle arguments) {
        super.onCreate(arguments);
        nonce = arguments == null ? null : arguments.getString("nonce");
        start();
    }
    private static Object field(Object target, String name) throws Exception {
        Field value = target.getClass().getDeclaredField(name);
        value.setAccessible(true);
        return value.get(target);
    }
    @Override public void onStart() {
        Bundle result = new Bundle();
        Activity activity = null;
        try {
            if (nonce == null || !nonce.matches("[0-9a-f]{32}")) throw new AssertionError("Missing fresh nonce");
            activity = startActivitySync(new Intent().setClassName(getTargetContext(),
                "com.netumscan.printersdk.samples.quickstart.MainActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            waitForIdleSync();
            if (activity == null || activity.isFinishing()) throw new AssertionError("Quick Start did not launch");
            Object session = field(activity, "session");
            ScheduledExecutorService worker = (ScheduledExecutorService)field(session, "worker");
            worker.submit(() -> {
                try {
                    if (field(session, "printer") != null || field(session, "operation") != null)
                        throw new AssertionError("Quick Start created printer IO on launch");
                } catch (Exception error) { throw new AssertionError(error); }
                return null;
            }).get(10, TimeUnit.SECONDS);
            final Activity current = activity;
            final String[] text = new String[1];
            runOnMainSync(() -> {
                try { text[0] = ((TextView)field(current, "status")).getText().toString(); }
                catch (Exception error) { throw new AssertionError(error); }
            });
            if (text[0] == null || !text[0].startsWith("未连接；SDK ")) throw new AssertionError("Quick Start is not offline");
            if (Build.VERSION.SDK_INT >= 31 && getTargetContext().checkSelfPermission("android.permission.BLUETOOTH_CONNECT")
                    != PackageManager.PERMISSION_DENIED) throw new AssertionError("Bluetooth permission was granted");
            JSONObject report = new JSONObject().put("schema_version", 1).put("success", true).put("nonce", nonce)
                .put("offline_launch", true).put("printer_created", false).put("operation_created", false)
                .put("bluetooth_runtime_permissions_ungranted", true).put("hardware_verified", false);
            try (FileOutputStream output = getTargetContext().openFileOutput("quick-runtime-gate.json", 0)) {
                output.write((report.toString(2) + "\n").getBytes(StandardCharsets.UTF_8));
            }
            result.putString("stream", "Quick Start offline runtime passed; nonce=" + nonce + "\n");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable error) {
            result.putString("stream", "Quick Start runtime failed: " + error.getClass().getSimpleName() + ": " + error.getMessage() + "\n");
            finish(Activity.RESULT_CANCELED, result);
        } finally {
            if (activity != null) { final Activity current = activity; runOnMainSync(current::finish); }
        }
    }
}
