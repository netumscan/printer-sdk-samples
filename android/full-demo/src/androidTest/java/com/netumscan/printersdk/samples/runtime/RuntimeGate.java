// SPDX-License-Identifier: MIT
package com.netumscan.printersdk.samples.runtime;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import com.netumscan.printersdk.internal.BluetoothChannel;
import com.netumscan.printersdk.*;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import java.lang.reflect.Proxy;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.netumscan.printersdk.internal.IoControl;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.JSONObject;

/**
 * Current Manager sample smoke; test APK only, never printer IO.
 * This live template checks offline startup, runtime identity and cleanup. It
 * does not produce the compatibility journal required by the historical online
 * release verifier. Published .2 exports use their complete frozen gate/verifier;
 * current development exports remain disabled until a new release contract exists.
 */
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

    private static ExecutorService pool() {
        return Executors.newCachedThreadPool(task -> { Thread thread = new Thread(task); thread.setDaemon(true); return thread; });
    }
    private PrinterManager memoryManager(Fake fake) throws Exception {
        Class<?> factoryType=Class.forName("com.netumscan.printersdk.PrinterManager$Factory");
        Object factory=Proxy.newProxyInstance(factoryType.getClassLoader(),new Class<?>[]{factoryType},(proxy,method,args)->{
            Constructor<?> create=Class.forName("com.netumscan.printersdk.MobilePrinter").getDeclaredConstructor(BluetoothChannel.class,int.class);
            create.setAccessible(true);return create.newInstance(fake,3);
        });
        Constructor<PrinterManager> create=PrinterManager.class.getDeclaredConstructor(Context.class,factoryType,Executor.class,ExecutorService.class,ExecutorService.class);
        create.setAccessible(true);Handler main=new Handler(Looper.getMainLooper());
        return create.newInstance(getTargetContext().getApplicationContext(),factory,(Executor) action->main.post(action),pool(),pool());
    }
    private static final class Reply<T> implements PrinterManager.Callback<T> {
        final CountDownLatch done=new CountDownLatch(1);ManagerResult<T> result;
        final AtomicInteger count=new AtomicInteger();boolean mainThread;
        public void onComplete(ManagerResult<T> value){result=value;mainThread=Looper.myLooper()==Looper.getMainLooper();count.incrementAndGet();done.countDown();}
        ManagerResult<T> await()throws Exception{require(done.await(8,TimeUnit.SECONDS),"Manager callback missing");require(count.get()==1&&mainThread,"Expected exactly one main-thread callback");return result;}
    }
    private static int nativeAbi(SdkInfo info,String name)throws Exception {
        Field value=SdkInfo.class.getDeclaredField(name);value.setAccessible(true);return value.getInt(info);
    }
    private JSONObject nativeGate() throws Exception {
        Fake fake=new Fake();PrinterManager manager=memoryManager(fake);SdkInfo info;
        try {
            Reply<SdkInfo> identity=new Reply<>();manager.sdkInfo(identity);info=identity.await().value;
            require(nativeAbi(info,"abiVersion")==3&&nativeAbi(info,"abiRevision")>=2&&"0.3.0".equals(info.nativeVersion)&&"0.3.0-preview.10".equals(info.distributionVersion),"Loaded SDK identity differs");
            Reply<ConnectionInfo> connection=new Reply<>();manager.connectSpp(new DiscoveredDevice("AA:BB:CC:DD:EE:01","LT-M90_AB12"),connection,null);require(connection.await().code==0,"Connect failed");
            Reply<Void> first=new Reply<>(),second=new Reply<>();manager.sendRaw(new byte[]{1,2,3,4,5,6,7,8,9},first);
            require(fake.firstWrite.await(5,TimeUnit.SECONDS),"First write missing");manager.sendRaw(new byte[]{10,11,12,13,14},second);fake.release.countDown();
            require(first.await().completion==ManagerResult.Completion.DATA_SENT&&second.await().completion==ManagerResult.Completion.DATA_SENT,"Native result is not DATA_SENT");
            require(Arrays.equals(fake.bytes.toByteArray(),new byte[]{1,2,3,4,5,6,7,8,9,10,11,12,13,14})&&fake.calls==5&&fake.partialWrites==3,"Raw FIFO or partial writes differ");
        } finally {fake.release.countDown();Reply<Void> close=new Reply<>();manager.close(close);require(close.await().code==0,"Cleanup failed");}
        return new JSONObject().put("jni_loaded",true).put("abi_handshake_passed",true)
            .put("abi_expected_version",nativeAbi(info,"abiVersion")).put("abi_minimum_revision",nativeAbi(info,"abiRevision"))
            .put("native_version",info.nativeVersion).put("distribution_version",info.distributionVersion)
            .put("completion_callback_verified",true).put("partial_write_calls",fake.partialWrites).put("write_calls",fake.calls)
            .put("bytes_sent",fake.bytes.size()).put("raw_order_verified",true).put("transport","in_memory_fake")
            .put("main_thread_callback_verified",true).put("exactly_once_callback_verified",true);
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
            final PrinterManager[] owner = new PrinterManager[1];
            runOnMainSync(() -> {
                try {
                    Object model = field(current, "model");
                    owner[0] = (PrinterManager)field(model, "manager");
                    require(owner[0] != null, "Demo Manager was not created");
                    require(field(model, "active") == null && field(model, "discovery") == null &&
                            field(model, "connection") == null && Boolean.FALSE.equals(field(model, "connected")),
                            "Demo started printer work on launch");
                    synchronized (field(owner[0], "lock")) {
                        require(field(owner[0], "session") == null, "Demo created a native session on launch");
                    }
                    Method state = model.getClass().getDeclaredMethod("state");
                    state.setAccessible(true);
                    require("OFFLINE".equals(String.valueOf(field(state.invoke(model), "phase"))), "Demo is not offline");
                } catch (Throwable error) { failure[0] = error; }
            });
            if (failure[0] != null) throw new AssertionError("Full Demo launch check failed", failure[0]);
            Reply<SdkInfo> identity = new Reply<>(); owner[0].sdkInfo(identity);
            SdkInfo loaded = identity.await().value;
            require(loaded != null && nativeAbi(loaded,"abiVersion") == 3 && nativeAbi(loaded,"abiRevision") >= 2 &&
                    "0.3.0".equals(loaded.nativeVersion) && "0.3.0-preview.10".equals(loaded.distributionVersion),
                    "Demo Manager runtime identity differs");
            runOnMainSync(current::finish); waitForIdleSync();
            Reply<Void> cleanup = new Reply<>(); owner[0].close(cleanup);
            require(cleanup.await().code == 0, "Demo Manager cleanup failed");
            synchronized (field(owner[0], "lock")) {
                require(field(owner[0], "session") == null && Boolean.TRUE.equals(field(owner[0], "closed")),
                        "Demo Manager remained active after destruction");
            }
            boolean permissionsDenied = true;
            if (Build.VERSION.SDK_INT >= 31) {
                for (String permission : new String[]{"android.permission.BLUETOOTH_CONNECT", "android.permission.BLUETOOTH_SCAN"})
                    permissionsDenied &= getTargetContext().checkSelfPermission(permission) == PackageManager.PERMISSION_DENIED;
            }
            require(permissionsDenied, "Runtime gate requires Bluetooth permissions to remain ungranted");
            JSONObject report = new JSONObject().put("schema_version", 1).put("success", true).put("nonce", nonce)
                .put("hardware_verified", false).put("native", nativeResult)
                .put("full_demo_offline", true).put("bluetooth_runtime_permissions_ungranted", permissionsDenied)
                .put("manager_cleanup_verified", true)
                .put("api_level", Build.VERSION.SDK_INT).put("os_version", Build.VERSION.RELEASE)
                .put("process_abi", processIs64Bit() ? Build.SUPPORTED_64_BIT_ABIS[0] : Build.SUPPORTED_32_BIT_ABIS[0])
                .put("validation_scope", "current_manager_offline_lifecycle_smoke");
            try (FileOutputStream output = getTargetContext().openFileOutput("runtime-gate.json", 0)) {
                output.write((report.toString(2) + "\n").getBytes(StandardCharsets.UTF_8));
            }
            result.putString("stream", "Printer SDK Manager offline smoke passed; nonce=" + nonce + "\n");
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
