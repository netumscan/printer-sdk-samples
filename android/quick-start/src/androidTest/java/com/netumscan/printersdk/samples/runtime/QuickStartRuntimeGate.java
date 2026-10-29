// SPDX-License-Identifier: MIT
package com.netumscan.printersdk.samples.runtime;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.TextView;
import com.netumscan.printersdk.ManagerResult;
import com.netumscan.printersdk.PrinterManager;
import com.netumscan.printersdk.PrinterListener;
import com.netumscan.printersdk.SdkInfo;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Map;
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
    private static void noPrinterWork(Object model, PrinterManager manager) throws Exception {
        for (String name : new String[]{"scanRequest", "connectRequest", "printRequest", "cleanupRequest"})
            if (field(model, name) != null) throw new AssertionError("Unexpected Quick Start request: " + name);
        if (!Boolean.FALSE.equals(field(model, "connected")) || field(model, "connectionInfo") != null)
            throw new AssertionError("Quick Start did not stay offline");
        synchronized (field(manager, "lock")) {
            if (field(manager, "session") != null || field(manager, "scan") != null ||
                    !((Map<?, ?>)field(manager, "requests")).isEmpty())
                throw new AssertionError("Quick Start created native IO or retained a request");
        }
    }
    private void permissionsUngranted() {
        String[] permissions = Build.VERSION.SDK_INT >= 31 ? new String[]{
            "android.permission.BLUETOOTH_SCAN", "android.permission.BLUETOOTH_CONNECT"} :
            new String[]{"android.permission.ACCESS_FINE_LOCATION"};
        if (Build.VERSION.SDK_INT >= 23) for (String permission : permissions)
            if (getTargetContext().checkSelfPermission(permission) != PackageManager.PERMISSION_DENIED)
                throw new AssertionError("Runtime Bluetooth/scan permission was granted");
    }
    private void onMainChecked(Checked action) {
        final Throwable[] failure = new Throwable[1];
        runOnMainSync(() -> { try { action.run(); } catch (Throwable error) { failure[0] = error; } });
        if (failure[0] != null) throw new AssertionError("Quick Start main-thread check failed", failure[0]);
    }
    @FunctionalInterface private interface Checked { void run() throws Exception; }
    @Override public void onStart() {
        Bundle result = new Bundle();
        Activity activity = null;
        try {
            if (nonce == null || !nonce.matches("[0-9a-f]{32}")) throw new AssertionError("Missing fresh nonce");
            permissionsUngranted();
            activity = startActivitySync(new Intent().setClassName(getTargetContext(),
                "com.netumscan.printersdk.samples.quickstart.MainActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            waitForIdleSync();
            if (activity == null || activity.isFinishing()) throw new AssertionError("Quick Start did not launch");
            final Activity current = activity;
            final Object[] model = new Object[1];
            final PrinterManager[] owner = new PrinterManager[1];
            onMainChecked(() -> {
                model[0] = field(current, "model");
                owner[0] = (PrinterManager)field(model[0], "manager");
                noPrinterWork(model[0], owner[0]);
                synchronized (field(owner[0], "lock")) {
                    if (((Number)field(owner[0], "nextId")).longValue() != 0)
                        throw new AssertionError("Quick Start submitted work before a user action");
                }
                if (!Boolean.TRUE.equals(field(model[0], "foreground")) ||
                        !Boolean.FALSE.equals(field(model[0], "destroyed")) ||
                        !Boolean.FALSE.equals(field(current, "permissionPending")) ||
                        !((TextView)field(current, "status")).getText().toString().startsWith("未连接；"))
                    throw new AssertionError("Quick Start is not in its initial idle state");
            });
            CountDownLatch identity = new CountDownLatch(1);
            final SdkInfo[] loaded = new SdkInfo[1];
            final int[] identityCode = new int[]{-1};
            onMainChecked(() -> {
                owner[0].setListener(new PrinterListener() {
                    @Override public void onSdkInfoCompleted(ManagerResult<SdkInfo> value) {
                        loaded[0] = value.value; identityCode[0] = value.code; identity.countDown();
                    }
                });
                owner[0].sdkInfo();
            });
            if (!identity.await(8, TimeUnit.SECONDS) || identityCode[0] != 0 || loaded[0] == null || !Integer.valueOf(3).equals(field(loaded[0], "abiVersion")) ||
                    !Integer.valueOf(3).equals(field(loaded[0], "abiRevision")) || !"0.3.0-preview.17".equals(loaded[0].distributionVersion) ||
                    !"0.3.0".equals(loaded[0].nativeVersion))
                throw new AssertionError("Quick Start Manager runtime identity differs");
            onMainChecked(() -> {
                owner[0].setListener((PrinterListener)model[0]);
                noPrinterWork(model[0], owner[0]);
            });
            runOnMainSync(current::finish); waitForIdleSync();
            // Observe the application's own close callback. Calling close from this gate
            // would hide a missing Activity cleanup path.
            final boolean[] closed = new boolean[1];
            long deadline = SystemClock.uptimeMillis() + 10000;
            while (!closed[0] && SystemClock.uptimeMillis() < deadline) {
                onMainChecked(() -> {
                    ManagerResult<?> value = (ManagerResult<?>)field(model[0], "lastCloseResult");
                    closed[0] = Boolean.TRUE.equals(field(model[0], "destroyed")) && value != null;
                    if (closed[0]) {
                        if (!current.isDestroyed() || value.outcome != ManagerResult.Outcome.SUCCESS || value.code != 0 ||
                                field(model[0], "closeRequest") != null || field(model[0], "changed") != null)
                            throw new AssertionError("Quick Start owner cleanup failed");
                        noPrinterWork(model[0], owner[0]);
                        synchronized (field(owner[0], "lock")) {
                            if (!Boolean.TRUE.equals(field(owner[0], "closed")) ||
                                    !Boolean.FALSE.equals(field(owner[0], "cleaning")))
                                throw new AssertionError("Manager did not finish permanent cleanup");
                        }
                    }
                });
                if (!closed[0]) SystemClock.sleep(20);
            }
            if (!closed[0]) throw new AssertionError("Activity destruction did not complete Manager cleanup");
            permissionsUngranted();
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
