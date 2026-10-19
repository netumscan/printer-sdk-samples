// SPDX-License-Identifier: MIT
package com.netumscan.printersdk.samples.runtime;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import com.netumscan.printersdk.internal.BluetoothChannel;
import com.netumscan.printersdk.SdkInfo;
import com.netumscan.printersdk.Subscription;
import com.netumscan.printersdk.DeviceStateSnapshot;
import com.netumscan.printersdk.internal.IoControl;
import com.netumscan.printersdk.MobilePrinter;
import com.netumscan.printersdk.PrintOperation;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.json.JSONObject;

/** Test APK only: the online SDK runs against an in-memory channel, never printer IO. */
public final class RuntimeGate extends Instrumentation {
    private String nonce;

    @Override public void onCreate(Bundle arguments) {
        super.onCreate(arguments);
        nonce = arguments == null ? null : arguments.getString("nonce");
        start();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static boolean processIs64Bit() throws IOException {
        if (Build.VERSION.SDK_INT >= 23) return android.os.Process.is64Bit();
        // API 21/22 have no Process.is64Bit(); inspect the running executable, not device ABI preferences.
        byte[] header = new byte[5];
        try (FileInputStream executable = new FileInputStream("/proc/self/exe")) {
            int offset = 0;
            while (offset < header.length) {
                int count = executable.read(header, offset, header.length - offset);
                if (count <= 0) throw new IOException("Incomplete process ELF header");
                offset += count;
            }
        }
        if (header[0] != 0x7f || header[1] != 'E' || header[2] != 'L' || header[3] != 'F' ||
                (header[4] != 1 && header[4] != 2)) throw new IOException("Invalid process ELF header");
        return header[4] == 2;
    }

    private static Object field(Object target, String name) throws Exception {
        Field value = target.getClass().getDeclaredField(name);
        value.setAccessible(true);
        return value.get(target);
    }

    private static Object call(Object target, String name) throws Exception {
        Method value = target.getClass().getDeclaredMethod(name);
        value.setAccessible(true);
        return value.invoke(target);
    }

    private static final class Fake implements BluetoothChannel {
        private volatile boolean open;
        private final CountDownLatch firstWrite = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        private int calls, partialWrites;
        public int open(IoControl control) { open = true; return control.error(); }
        public boolean isOpen() { return open; }
        public void close() { open = false; release.countDown(); }
        public synchronized int write(byte[] data, IoControl control) {
            calls++;
            firstWrite.countDown();
            try { if (!release.await(5, TimeUnit.SECONDS)) return -2003; }
            catch (InterruptedException error) { Thread.currentThread().interrupt(); return -1005; }
            if (control.error() != 0) return -control.error();
            if (!open) return -2002;
            int count = Math.min(3, data.length);
            if (count < data.length) partialWrites++;
            bytes.write(data, 0, count);
            return count;
        }
    }

    private static PrintOperation.Snapshot finish(PrintOperation operation) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        PrintOperation.Snapshot value;
        do {
            value = operation.snapshot();
            if (value.terminal()) return value;
            Thread.sleep(5);
        } while (System.nanoTime() < deadline);
        throw new AssertionError("Native operation did not finish");
    }

    private JSONObject nativeGate() throws Exception {
        SdkInfo info = MobilePrinter.sdkInfo();
        require(info.abiVersion == 3 && info.abiRevision == 1 && "0.3.0".equals(info.nativeVersion)
            && "0.3.0-preview.2".equals(info.distributionVersion), "Loaded native SDK identity differs");
        Fake fake = new Fake();
        Constructor<MobilePrinter> create = MobilePrinter.class.getDeclaredConstructor(
            BluetoothChannel.class, int.class);
        create.setAccessible(true);
        // MobilePrinter loads the real JNI library. nCreate checks ABI 3 / revision 1.
        // No native build, alternate SDK class, or test hook is supplied by this application.
        try (MobilePrinter printer = create.newInstance(fake, 3)) {
            require(printer.stateSnapshot().connectionState == MobilePrinter.DISCONNECTED, "Unexpected initial state");
            printer.connect(1000);
            byte[] first = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9};
            byte[] following = new byte[]{10, 11, 12, 13, 14};
            try (PrintOperation a = printer.sendRaw(first, 1, 10000)) {
                require(fake.firstWrite.await(5, TimeUnit.SECONDS), "Native write was not called");
                CountDownLatch callback = new CountDownLatch(1);
                java.util.concurrent.atomic.AtomicReference<PrintOperation.Result> copied = new java.util.concurrent.atomic.AtomicReference<>();
                Subscription subscription = a.onComplete(Runnable::run, value -> {
                    copied.set(value); callback.countDown();
                });
                try (PrintOperation b = printer.sendRaw(following, 1, 10000)) {
                    fake.release.countDown();
                    PrintOperation.Snapshot left = finish(a), right = finish(b);
                    require(left.state == 4 && right.state == 4 && left.completion == 1 && right.completion == 1,
                        "Native tasks did not reach DATA_SENT");
                    require(left.bytesSent == 9 && right.bytesSent == 5, "Incorrect native byte counts");
                    require(callback.await(5, TimeUnit.SECONDS) && copied.get().snapshot.completion == 1
                        && copied.get().diagnostic.errorCode == 0, "Owned terminal callback differs");
                } finally { subscription.close(); }
            }
            require(Arrays.equals(fake.bytes.toByteArray(), new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14}),
                "Raw tasks interleaved or partial writes lost bytes");
            require(fake.calls == 5 && fake.partialWrites == 3, "Partial-write loop did not execute");
        } finally { fake.release.countDown(); }
        return new JSONObject().put("jni_loaded", true).put("abi_handshake_passed", true)
            .put("abi_expected_version", info.abiVersion).put("abi_minimum_revision", info.abiRevision)
            .put("native_version", info.nativeVersion).put("distribution_version", info.distributionVersion)
            .put("completion_callback_verified", true)
            .put("partial_write_calls", fake.partialWrites).put("write_calls", fake.calls)
            .put("bytes_sent", fake.bytes.size()).put("raw_order_verified", true)
            .put("transport", "in_memory_fake");
    }

    @Override public void onStart() {
        Bundle result = new Bundle();
        Activity activity = null;
        try {
            require(nonce != null && nonce.matches("[0-9a-f]{32}"), "Missing fresh verification nonce");
            JSONObject nativeResult = nativeGate();
            Intent intent = new Intent().setClassName(getTargetContext(), "com.netumscan.printersdk.demo.MainActivity")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity = startActivitySync(intent);
            waitForIdleSync();
            require(activity != null && !activity.isFinishing(), "Full Demo did not start");
            final Activity current = activity;
            final Throwable[] failure = new Throwable[1];
            final JSONObject[] compatibility = new JSONObject[1];
            runOnMainSync(() -> {
                try {
                    require(Boolean.FALSE.equals(field(current, "scanning")), "Full Demo started discovery");
                    Object controller = field(current, "controller");
                    require(controller != null, "Full Demo controller was not created");
                    synchronized (controller) {
                        require(field(controller, "session") == null, "Full Demo created a session on launch");
                        require("OFFLINE".equals(String.valueOf(call(controller, "phase"))), "Full Demo is not offline");
                    }
                    compatibility[0] = new JSONObject((String)call(call(current, "compatibilityRecord"), "json"));
                } catch (Throwable error) { failure[0] = error; }
            });
            if (failure[0] != null) throw new AssertionError("Full Demo launch check failed", failure[0]);
            Object journal = field(activity, "journal");
            String snapshot = (String)((Future<?>)call(journal, "snapshot")).get(10, TimeUnit.SECONDS);
            require(!snapshot.contains("Storage unavailable"), "Journal storage is degraded");
            require(snapshot.contains("message=compatibility_record="),
                "Fresh launch compatibility record was not written to the real journal");
            boolean permissionsDenied = true;
            if (Build.VERSION.SDK_INT >= 31) {
                for (String permission : new String[]{"android.permission.BLUETOOTH_CONNECT", "android.permission.BLUETOOTH_SCAN"})
                    permissionsDenied &= getTargetContext().checkSelfPermission(permission) == PackageManager.PERMISSION_DENIED;
            }
            require(permissionsDenied, "Runtime gate requires Bluetooth permissions to remain ungranted");
            JSONObject report = new JSONObject().put("schema_version", 1).put("success", true).put("nonce", nonce)
                .put("hardware_verified", false).put("native", nativeResult)
                .put("full_demo_offline", true).put("bluetooth_runtime_permissions_ungranted", permissionsDenied)
                .put("api_level", Build.VERSION.SDK_INT).put("os_version", Build.VERSION.RELEASE)
                .put("process_abi", processIs64Bit() ? Build.SUPPORTED_64_BIT_ABIS[0] : Build.SUPPORTED_32_BIT_ABIS[0])
                .put("compatibility_record", compatibility[0]).put("journal", snapshot);
            try (FileOutputStream output = getTargetContext().openFileOutput("runtime-gate.json", 0)) {
                output.write((report.toString(2) + "\n").getBytes(StandardCharsets.UTF_8));
            }
            result.putString("stream", "Printer SDK online runtime gate passed; nonce=" + nonce + "\n");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable error) {
            result.putString("stream", "Printer SDK runtime gate failed:\n" + android.util.Log.getStackTraceString(error));
            finish(Activity.RESULT_CANCELED, result);
        } finally {
            if (activity != null) {
                final Activity current = activity;
                runOnMainSync(current::finish);
            }
        }
    }
}
