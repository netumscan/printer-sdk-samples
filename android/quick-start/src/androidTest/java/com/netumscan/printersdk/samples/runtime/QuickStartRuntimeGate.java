// SPDX-License-Identifier: MIT
package com.netumscan.printersdk.samples.runtime;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;
import com.netumscan.printersdk.PrinterManager;
import com.netumscan.printersdk.SdkInfo;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.json.JSONObject;

/** Current direct-Manager startup/cleanup smoke; historical exports use frozen gates. */
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
            final Activity current = activity;
            final String[] text = new String[1];
            final PrinterManager[] owner = new PrinterManager[1];
            final Throwable[] failure = new Throwable[1];
            runOnMainSync(() -> {
                try {
                    Object model = field(current, "model");
                    owner[0] = (PrinterManager)field(model, "manager");
                    if (owner[0] == null || field(model, "scanRequest") != null ||
                            field(model, "connectRequest") != null || field(model, "printRequest") != null ||
                            !Boolean.FALSE.equals(field(model, "connected")))
                        throw new AssertionError("Quick Start created printer work on launch");
                    synchronized (field(owner[0], "lock")) {
                        if (field(owner[0], "session") != null) throw new AssertionError("Quick Start created native IO on launch");
                    }
                    text[0] = ((TextView)field(current, "status")).getText().toString();
                }
                catch (Throwable error) { failure[0] = error; }
            });
            if (failure[0] != null) throw new AssertionError("Quick Start launch check failed", failure[0]);
            if (text[0] == null || !text[0].startsWith("未连接；")) throw new AssertionError("Quick Start is not offline");
            CountDownLatch identity = new CountDownLatch(1);
            final SdkInfo[] loaded = new SdkInfo[1];
            owner[0].sdkInfo(value -> { loaded[0] = value.value; identity.countDown(); });
            if (!identity.await(8, TimeUnit.SECONDS) || loaded[0] == null || !Integer.valueOf(3).equals(field(loaded[0], "abiVersion")) ||
                    !Integer.valueOf(2).equals(field(loaded[0], "abiRevision")) || !"0.3.0-preview.11".equals(loaded[0].distributionVersion) ||
                    !"0.3.0".equals(loaded[0].nativeVersion))
                throw new AssertionError("Quick Start Manager runtime identity differs");
            runOnMainSync(current::finish); waitForIdleSync();
            CountDownLatch closed = new CountDownLatch(1);
            final int[] closeCode = new int[]{-1};
            owner[0].close(value -> { closeCode[0] = value.code; closed.countDown(); });
            if (!closed.await(8, TimeUnit.SECONDS) || closeCode[0] != 0)
                throw new AssertionError("Quick Start Manager cleanup failed");
            if (Build.VERSION.SDK_INT >= 31 && getTargetContext().checkSelfPermission("android.permission.BLUETOOTH_CONNECT")
                    != PackageManager.PERMISSION_DENIED) throw new AssertionError("Bluetooth permission was granted");
            JSONObject report = new JSONObject().put("schema_version", 1).put("success", true).put("nonce", nonce)
                .put("offline_launch", true).put("printer_created", false).put("operation_created", false)
                .put("bluetooth_runtime_permissions_ungranted", true).put("hardware_verified", false)
                .put("manager_cleanup_verified", true).put("validation_scope", "current_manager_offline_lifecycle_smoke")
                .put("sdk", new JSONObject().put("distribution_version", loaded[0].distributionVersion)
                    .put("native_version", loaded[0].nativeVersion).put("abi_version", field(loaded[0], "abiVersion"))
                    .put("abi_revision", field(loaded[0], "abiRevision")));
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
