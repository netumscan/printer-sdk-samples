// SPDX-License-Identifier: MIT
package com.netumscan.printersdk.demo;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.netumscan.printersdk.*;
import com.netumscan.printersdk.internal.BluetoothChannel;
import com.netumscan.printersdk.internal.IoControl;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Actual Model + public Manager + JNI, with a test-only memory transport. No UI mirror. */
public final class DemoModelChecks {
    private interface Condition { boolean get(); }
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final ConnectionOptions TARGET = DemoQueueChecks.namedTarget("AA:BB:CC:DD:EE:01");
    private static final class Memory implements BluetoothChannel {
        volatile boolean open, blocked;
        final AtomicInteger opens = new AtomicInteger();
        final AtomicInteger writes = new AtomicInteger();
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        public int open(IoControl control) { opens.incrementAndGet(); open = true; return 0; }
        public int write(byte[] data, IoControl control) {
            writes.incrementAndGet();
            while (blocked && open && control.error() == 0) Thread.yield();
            if (control.error() != 0) return -control.error();
            if (!open) return -2002;
            synchronized (bytes) { bytes.write(data, 0, data.length); }
            return data.length;
        }
        public boolean isOpen() { return open; }
        public void close() { open = false; }
        String output() { synchronized (bytes) { return new String(bytes.toByteArray(), StandardCharsets.ISO_8859_1); } }
        int size() { synchronized (bytes) { return bytes.size(); } }
    }
    private static ExecutorService pool() {
        return Executors.newCachedThreadPool(task -> { Thread thread = new Thread(task); thread.setDaemon(true); return thread; });
    }
    private static PrinterManager manager(Memory memory, CountDownLatch factoryEntered, CountDownLatch releaseFactory) throws Exception {
        return manager(memory, factoryEntered, releaseFactory, pool());
    }
    private static PrinterManager manager(Memory memory, CountDownLatch factoryEntered, CountDownLatch releaseFactory,
            ExecutorService preparationWorkers) throws Exception {
        Context context = (Context)Class.forName("android.content.Context").getDeclaredConstructor().newInstance();
        return manager(context, memory, factoryEntered, releaseFactory, preparationWorkers);
    }
    private static PrinterManager manager(Context context, Memory memory, CountDownLatch factoryEntered,
            CountDownLatch releaseFactory, ExecutorService preparationWorkers) throws Exception {
        Class<?> factoryType = Class.forName("com.netumscan.printersdk.PrinterManager$Factory");
        Object factory = Proxy.newProxyInstance(factoryType.getClassLoader(), new Class<?>[]{factoryType}, (proxy, method, args) -> {
            if (factoryEntered != null) { factoryEntered.countDown(); check(releaseFactory.await(5, TimeUnit.SECONDS), "Release test factory"); }
            Constructor<?> constructor = Class.forName("com.netumscan.printersdk.MobilePrinter")
                .getDeclaredConstructor(BluetoothChannel.class, int.class);
            constructor.setAccessible(true);
            return constructor.newInstance(memory, 2);
        });
        Constructor<PrinterManager> constructor = PrinterManager.class.getDeclaredConstructor(Context.class, factoryType,
            Executor.class, ExecutorService.class, ExecutorService.class);
        constructor.setAccessible(true);
        return constructor.newInstance(context, factory, (Executor)action -> MAIN.post(action), preparationWorkers, pool());
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static int count(String text, String token) {
        int count = 0, index = 0;
        while ((index = text.indexOf(token, index)) >= 0) { count++; index += token.length(); }
        return count;
    }
    private static void main(Runnable action) throws Exception {
        CountDownLatch done = new CountDownLatch(1); AtomicReference<Throwable> failure = new AtomicReference<>();
        MAIN.post(() -> { try { action.run(); } catch (Throwable error) { failure.set(error); } finally { done.countDown(); } });
        check(done.await(5, TimeUnit.SECONDS), "Main callback deadline");
        if (failure.get() != null) throw new AssertionError(failure.get());
    }
    private static void until(Condition condition) throws Exception {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        do {
            AtomicBoolean done = new AtomicBoolean(); main(() -> done.set(condition.get()));
            if (done.get()) return;
            Thread.sleep(2);
        } while (System.nanoTime() < end);
        throw new AssertionError("Model completion deadline");
    }
    private static void close(DemoModel model, PrinterManager manager) throws Exception {
        if (model != null) main(model::close);
        CountDownLatch done = new CountDownLatch(1); manager.close(result -> done.countDown());
        check(done.await(5, TimeUnit.SECONDS), "Native cleanup");
    }
    /** Host checks inject immutable pixels through the same new PrintOptions path. */
    private static void printPixels(DemoModel model, LabelConfig config, byte[] immutablePixels) {
        model.printPixels(config, immutablePixels);
    }
    private static void printPixels(DemoModel model, LabelConfig config) {
        printPixels(model, config, new byte[config.labelWidthDots * config.height]);
    }
    private static void configure(DemoModel model, LabelConfig config) throws Exception {
        main(() -> {
            PrinterProfile current = model.printProfile();
            PrinterProfile target = config.printProfile();
            if (current == null || current.protocol != target.protocol || current.dpi != target.dpi ||
                    current.maxPrintWidthDots != target.maxPrintWidthDots || current.dotsPerMm != target.dotsPerMm ||
                    current.labelAlignment != target.labelAlignment) model.configureProfile(target, MediaMode.GAP);
            else if (!model.configured()) model.retryInitialize();
        });
        until(() -> model.state().canWork());
    }
    /** Full App request path: no resolved absolute origin is submitted for a centered Profile. */
    public static void centeredProfileChecks(Context context) throws Exception {
        Memory memory = new Memory(); PrinterManager manager = manager(context, memory, null, null, pool());
        AtomicReference<DemoModel> reference = new AtomicReference<>();
        main(() -> { DemoModel model = new DemoModel(manager); reference.set(model); model.foreground(); model.connect(TARGET); });
        DemoModel model = reference.get();
        try {
            until(() -> model.state().canWork() && model.configured());
            PrinterProfile profile = LTM90Profile.printProfile();
            check(profile.dotsPerMm == 8 && profile.dotsFromMm(50) == 400,
                "LT-M90 precise density maps a 50 mm label to 400 dots");
            for (int index = 0; index < 4; index++) {
                int width = index == 1 ? 401 : 400, calibration = index == 2 ? 4 : index == 3 ? -4 : 0;
                LabelConfig config = new LabelConfig(profile, width, 400, 1, 1, 0, calibration, 0);
                check(config.printProfile() == profile && config.manualOriginDots == 0,
                    "Demo keeps the adopted Profile snapshot and sends only optional calibration");
                int before = memory.size();
                main(() -> printPixels(model, config)); until(() -> model.state().pendingPrintCount() == 0);
                int origin = index == 0 ? 232 : index == 1 ? 228 : index == 2 ? 236 : 228;
                int packedBytes = width == 401 ? 51 : 50;
                check(memory.output().substring(before).contains("CG " + packedBytes + " 128 " + origin + " 0 "),
                    "SDK centering, byte padding and signed calibration reach native CPCL output");
                main(() -> check(model.state().lastRequest.raw.code == 0 &&
                    model.state().lastRequest.raw.completion == ManagerResult.Completion.DATA_SENT,
                    "Centered Demo request preserves the real terminal result"));
            }
            int before = memory.size();
            try { new LabelConfig(profile, 400, 400, 1, 1, 17, 0, 0); throw new AssertionError("Fixed center overridden"); }
            catch (LabelConfig.ValidationError expected) { }
            check(memory.size() == before, "Invalid manual origin never submits IO for a centered Profile");
            LabelConfig manual = new LabelConfig(203, 864, 400, 400, 1, 1, 17);
            configure(model, manual); before = memory.size();
            main(() -> printPixels(model, manual)); until(() -> model.state().pendingPrintCount() == 0);
            check(memory.output().substring(before).contains("CG 50 128 17 0 "),
                "A manually configured LEFT Profile preserves the advanced explicit origin feature");
        } finally { close(model, manager); }
    }
    public static void main(String[] args) throws Exception {
        centeredProfileChecks((Context)Class.forName("android.content.Context").getDeclaredConstructor().newInstance());
        Memory memory = new Memory(); PrinterManager manager = manager(memory, null, null);
        AtomicReference<DemoModel> reference = new AtomicReference<>();
        PrinterManager firstManager = manager;
        AtomicReference<DemoOwner> retainedOwner = new AtomicReference<>();
        main(() -> {
            DemoOwner owner = new DemoOwner(firstManager);
            retainedOwner.set(owner); reference.set(owner.model);
        });
        DemoModel model = reference.get();
        DemoOwner owner = retainedOwner.get();
        LabelConfig plainLabel = new LabelConfig(203, 256, 256, 128, 1, 2);
        LabelConfig plainReceipt = new LabelConfig(180, 384, 384, 128, 1, 0, 0, true);
        try {
            main(() -> {
                model.foreground();
                try (DemoImageSource source = DemoImageSource.builtin()) { model.print(source, plainLabel); }
                model.printFile(new File("printer-demo-offline-source.png"), plainLabel);
                check(model.state().canConnect() && model.state().lastRequest == null, "Offline image printing does no preparation or SDK work");
                model.connect(TARGET);
            });
            until(() -> model.state().canWork());
            configure(model, plainLabel);
            main(() -> printPixels(model, plainLabel)); until(() -> model.state().canWork());
            main(() -> check(model.state().lastRequest.kind == DemoResult.Kind.PRINT && model.state().lastRequest.raw.completion == ManagerResult.Completion.DATA_SENT && model.state().lastRequest.raw.code == 0, "Original CPCL completion retained"));
            configure(model, plainReceipt); main(() -> printPixels(model, plainReceipt)); until(() -> model.state().canWork());
            check(memory.output().contains("PAGE-WIDTH 256\r\n") && memory.output().contains("\u001b@\u001dP\u0000\u00b4"), "Connection configuration changes reach the real encoder");
            int before = memory.size();
            LabelConfig label = new LabelConfig(300, 384, 129, 128, 2, 1, 31, false, 9);
            byte[] labelPixels = new byte[label.labelWidthDots * label.height];
            configure(model, label); main(() -> printPixels(model, label, labelPixels)); until(() -> model.state().canWork());
            main(() -> check(model.state().lastRequest.raw.code == 0 && model.state().lastRequest.raw.completion == ManagerResult.Completion.DATA_SENT,
                "Custom pixels retain the original print completion"));
            String custom = memory.output().substring(before);
            check(count(custom, "! 0 200 200 137 1\r\nPAGE-WIDTH 384\r\n") == 2 &&
                count(custom, "CG 17 128 31 0 ") == 2 && count(custom, "FORM\r\nPRINT\r\n") == 2,
                "Custom canvas, packed width, origin, FORM, trailing blank and copies reach CPCL");
            before = memory.size();
            LabelConfig resized = new LabelConfig(300, 384, 256, 160, 1, 2);
            configure(model, resized); main(() -> printPixels(model, resized)); until(() -> model.state().canWork());
            String resizedBytes = memory.output().substring(before);
            check(resizedBytes.contains("! 0 200 200 160 1\r\nPAGE-WIDTH 384\r\n") &&
                resizedBytes.contains("CG 32 128 0 0 "), "Changed print dimensions reach the encoder");
            before = memory.size();
            LabelConfig receipt = new LabelConfig(180, 384, 128, 128, 3, 0, 0, true, 11);
            configure(model, receipt); main(() -> printPixels(model, receipt)); until(() -> model.state().canWork());
            String receiptBytes = memory.output().substring(before);
            main(() -> check(model.state().lastRequest.raw.code == 0, "Receipt budget admits all copies"));
            check(count(receiptBytes, "\u001b@\u001dP\u0000\u00b4") == 3 && count(receiptBytes, "\u001bJ\u000b") == 3,
                "Receipt copies each include the configured trailing blank without cutting");
            before = memory.size();
            main(() -> model.sendRaw(new byte[]{90}, 3)); until(() -> model.state().canWork());
            check(memory.output().substring(before).equals("ZZZ"), "Raw sends the explicitly requested copies");
            before = memory.size();
            configure(model, label);
            before = memory.size(); int previousWrites = memory.writes.get();
            memory.blocked = true;
            main(() -> printPixels(model, label, labelPixels));
            until(() -> memory.writes.get() > previousWrites);
            main(() -> { model.cancel(); model.disconnect(); }); until(() -> model.state().canConnect());
            memory.blocked = false;
            main(() -> check(model.state().lastRequest.raw.code == 1005 && model.state().lastRequest.kind == DemoResult.Kind.PRINT &&
                model.state().cleanupResult.raw.code == 0, "Custom sending cancellation retains its typed result beside cleanup"));
            check(memory.size() == before && memory.opens.get() == 1, "Sending cancellation does not reconnect or produce a late write");
            main(() -> model.connect(TARGET)); until(() -> model.state().canWork());
            main(() -> model.sendRaw(new byte[]{81}, 1)); until(() -> model.state().canWork());
            check(memory.output().substring(before).endsWith("Q") && memory.opens.get() == 2,
                "Cancelled custom print is not replayed after an explicit reconnect");
            main(() -> model.sendRaw(new byte[]{27, 64}, 1)); until(() -> model.state().canWork());
            main(() -> model.queryStatus(false)); until(() -> model.state().canWork());
            main(() -> check(model.state().statusResult.raw.outcome == ManagerResult.Outcome.FAILURE && model.state().statusResult.raw.value == null, "Unsupported status remains failure/unknown"));
            main(model::capabilities); until(() -> model.state().canWork());
            main(() -> check(model.state().capabilitiesResult.raw.code == 0 && model.state().capabilitiesResult.raw.value != null, "Original capability values retained"));
            AtomicInteger oldNotifications = new AtomicInteger(), newNotifications = new AtomicInteger();
            Runnable oldUi = oldNotifications::incrementAndGet, newUi = newNotifications::incrementAndGet;
            configure(model, label);
            memory.blocked = true;
            main(() -> {
                model.attach(oldUi); printPixels(model, label, labelPixels);
                owner.stopUi(oldUi, true);
                check(model.state().phase == DemoUiState.Phase.WORKING, "Rotation keeps the active request");
                owner.destroyUi(oldUi, true);
                model.attach(newUi);
                owner.destroyUi(oldUi, true); // A late old Activity cannot detach its replacement.
            });
            memory.blocked = false; until(() -> model.state().canWork());
            main(() -> check(model.state().lastRequest.raw.code == 0 && newNotifications.get() > 1,
                "Rotated UI receives the original terminal without replay"));
            check(memory.opens.get() == 2, "Rotation does not reopen transport");
            configure(model, label);
            memory.blocked = true;
            main(() -> { printPixels(model, label, labelPixels); owner.stopUi(newUi, false); });
            main(model::foreground); until(() -> model.state().canConnect());
            main(() -> check(model.state().lastRequest.raw.code == 1005 && model.state().cleanupResult.raw.code == 0, "Cleanup does not overwrite the true print cancellation"));
            check(memory.opens.get() == 2, "Foreground return does not reconnect or replay");
            memory.blocked = false;
            main(() -> model.connect(TARGET)); until(() -> model.state().canWork());
        } finally { close(model, manager); }

        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        manager = manager(new Memory(), entered, release);
        PrinterManager pendingManager = manager;
        main(() -> reference.set(new DemoModel(pendingManager)));
        DemoModel pending = reference.get();
        try {
            main(() -> {
                check(pending.state().phase == DemoUiState.Phase.OFFLINE && pending.state().lastRequest == null &&
                    pending.state().candidates.isEmpty() && pending.state().selectedID == null, "Fresh process owner has no restored SDK work");
                pending.foreground(); pending.connect(TARGET);
            });
            check(entered.await(5, TimeUnit.SECONDS), "Connect preparation entered");
            main(pending::cancel);
            until(() -> pending.state().lastRequest != null && pending.state().lastRequest.raw.code == 1005);
            main(() -> check(!pending.state().canConnect(), "Cancelled connect waits for actual cleanup before retry"));
            release.countDown(); until(() -> pending.state().canConnect());
            AtomicInteger notifications = new AtomicInteger();
            main(() -> { pending.attach(notifications::incrementAndGet); pending.close(); });
            main(() -> { pending.foreground(); pending.connect(TARGET); pending.inputError("late"); });
            check(notifications.get() == 1 && pending.state().phase == DemoUiState.Phase.CLOSED, "Closed Model detaches UI and rejects late actions");
        } finally { release.countDown(); close(pending, manager); }
        cancelledPreparationDoesNotReplay();
        cancelledFilePreparationDoesNotReplay();
        mediaRequestBelongsToItsConnection();
        profileFailureChecks((Context)Class.forName("android.content.Context").getDeclaredConstructor().newInstance());
        System.out.println("PASS: Android DemoModel public Manager callbacks, cancellation and cleanup (real JNI)");
    }
    static void profileFailureChecks(Context context) throws Exception {
        unknownProfileChecks(context);
        ExecutorService workers = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task); thread.setDaemon(true); return thread;
        });
        Memory memory = new Memory(); PrinterManager manager = manager(context, memory, null, null, workers);
        AtomicReference<DemoModel> reference = new AtomicReference<>();
        main(() -> { reference.set(new DemoModel(manager)); reference.get().foreground(); reference.get().connect(TARGET); });
        DemoModel model = reference.get();
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        try {
            until(() -> model.state().canWork() && model.configured());
            PrinterProfile previous = model.printProfile();
            ConnectionInfo previousInfo = model.state().connectionInfo;
            memory.blocked = true;
            int writes = memory.writes.get();
            CountDownLatch direct = new CountDownLatch(1);
            manager.sendRaw(new byte[]{85}, 1, 5000, result -> direct.countDown());
            until(() -> memory.writes.get() > writes);
            main(() -> model.configureProfile(PrinterProfile.cpcl(300, 384), MediaMode.GAP));
            until(() -> model.state().canWork());
            main(() -> check(model.state().lastRequest.kind == DemoResult.Kind.PROFILE &&
                model.state().lastRequest.raw.outcome == ManagerResult.Outcome.FAILURE &&
                model.printProfile() == previous && model.state().connectionInfo == previousInfo && model.configured(),
                "Rejected profile update preserves the adopted SDK configuration and previous initialized state"));
            memory.blocked = false;
            check(direct.await(5, TimeUnit.SECONDS), "Competing public Manager request completes");
            workers.execute(() -> {
                entered.countDown();
                try { release.await(); } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
            });
            check(entered.await(5, TimeUnit.SECONDS), "Initialization preparation gate entered");
            main(() -> model.configureProfile(PrinterProfile.cpcl(300, 384), MediaMode.GAP));
            until(() -> model.state().activeKind == DemoResult.Kind.MEDIA && model.state().canCancel());
            main(model::cancel);
            until(() -> model.state().canWork());
            main(() -> check(model.printProfile().dpi == 300 && model.printProfile().maxPrintWidthDots == 384 &&
                model.state().connectionInfo != previousInfo &&
                model.state().connectionInfo.printProfile == model.printProfile() &&
                model.state().connectionInfo.configurationSource == ConnectionInfo.ConfigurationSource.EXPLICIT &&
                !model.configured() && model.state().lastRequest.raw.code == 1005,
                "New Profile survives failed initialization and printing remains disabled"));
            release.countDown(); workers.submit(() -> { }).get(5, TimeUnit.SECONDS);
            main(model::retryInitialize);
            until(() -> model.state().canWork() && model.configured());
            check(memory.opens.get() == 1 && memory.output().contains("GAP-SENSE"),
                "Initialization can be retried on the same connection without automatic reconnect");
        } finally { memory.blocked = false; release.countDown(); close(model, manager); }
    }
    /** 未识别型号可以连接；必须由调用者明确补齐 Profile 后才能初始化和打印。 */
    private static void unknownProfileChecks(Context context) throws Exception {
        Memory memory = new Memory(); PrinterManager manager = manager(context, memory, null, null, pool());
        AtomicReference<DemoModel> reference = new AtomicReference<>();
        main(() -> {
            DemoModel model = new DemoModel(manager); reference.set(model); model.foreground();
            model.connect(ConnectionOptions.spp("AA:BB:CC:DD:EE:09"), MediaMode.GAP);
        });
        DemoModel model = reference.get();
        try {
            until(() -> model.state().canWork());
            AtomicReference<DemoUiState> unknown = new AtomicReference<>();
            main(() -> {
                unknown.set(model.state());
                check(model.state().connected && model.state().connectionInfo != null &&
                    model.state().connectionInfo.configurationSource == ConnectionInfo.ConfigurationSource.NONE &&
                    model.state().connectionInfo.printProfile == null && model.printProfile() == null &&
                    model.state().mediaRequest == null && !model.configured() && !model.state().canSubmitPrint(),
                    "Unknown printer remains connected with real NONE metadata and requires explicit configuration");
                printPixels(model, new LabelConfig(203, 256, 128, 128, 1, 2));
                check(model.state().printRequests.isEmpty(), "Unknown Profile never starts image preparation or a print request");
            });
            check(memory.size() == 0 && memory.writes.get() == 0,
                "Connecting an unknown printer sends no guessed media or print commands");
            memory.blocked = true;
            main(() -> model.configureProfile(PrinterProfile.cpcl(203, 256), MediaMode.GAP));
            until(() -> memory.writes.get() > 0);
            main(() -> check(model.state().connectionInfo.configurationSource == ConnectionInfo.ConfigurationSource.EXPLICIT &&
                model.state().connectionInfo.printProfile == model.printProfile() && model.state().mediaRequest != null &&
                model.state().mediaRequest.result == null && !model.state().canSubmitPrint(),
                "Explicit CPCL Profile is adopted before its separate media request completes"));
            memory.blocked = false;
            until(() -> model.state().canWork() && model.state().canSubmitPrint());
            main(() -> check(model.configured() && model.state().mediaRequest.result.raw.completion == ManagerResult.Completion.DATA_SENT &&
                unknown.get().connectionInfo.printProfile == null && unknown.get().mediaRequest == null,
                "Media DATA_SENT enables printing while earlier unknown snapshots remain immutable"));
            check(memory.output().contains("GAP-SENSE\r\n"), "Explicit CPCL configuration initializes the selected media");
            int beforeReceipt = memory.size();
            main(() -> model.configureProfile(PrinterProfile.escPos(203, 256), MediaMode.CONTINUOUS));
            until(() -> model.state().canWork() && model.state().canSubmitPrint());
            main(() -> check(model.state().connectionInfo.printProfile.protocol == PrinterProfile.Protocol.ESC_POS &&
                model.state().connectionInfo.configurationSource == ConnectionInfo.ConfigurationSource.EXPLICIT &&
                model.state().mediaRequest == null && model.configured(),
                "Explicit ESC/POS defaults become ready without inventing CPCL media initialization"));
            check(memory.size() == beforeReceipt, "ESC/POS configuration performs no media IO");
            main(() -> {
                model.disconnect();
                check(model.state().connectionInfo == null && model.printProfile() == null && !model.configured(),
                    "Disconnect immediately removes current adopted configuration");
            });
            until(() -> model.state().canConnect());
            main(() -> model.connect(TARGET));
            until(() -> model.state().canWork() && model.state().canSubmitPrint());
            main(() -> {
                check(model.state().connectionInfo != null && model.state().connectionInfo.printProfile == model.printProfile(),
                    "Explicit reconnect publishes the new connection configuration");
                model.close();
                check(model.state().connectionInfo == null && !model.state().connected && !model.configured(),
                    "Permanent close removes all current connection configuration");
            });
        } finally { memory.blocked = false; close(model, manager); }
    }
    private static void mediaRequestBelongsToItsConnection() throws Exception {
        Memory memory = new Memory(); PrinterManager manager = manager(memory, null, null);
        AtomicReference<DemoModel> reference = new AtomicReference<>();
        main(() -> { reference.set(new DemoModel(manager)); reference.get().foreground(); reference.get().connect(TARGET); });
        DemoModel model = reference.get();
        try {
            until(() -> model.state().canWork());
            main(() -> model.setMedia(MediaMode.GAP)); until(() -> model.state().canWork());
            AtomicReference<DemoUiState.MediaRequest> gap = new AtomicReference<>();
            main(() -> {
                gap.set(model.state().mediaRequest);
                check(gap.get().mode == MediaMode.GAP && gap.get().result.raw.outcome == ManagerResult.Outcome.SUCCESS &&
                    gap.get().result.raw.requestId == gap.get().requestId, "Media success retains the sent mode and actual request");
            });
            check(memory.output().contains("GAP-SENSE\r\n"), "Media request reaches the real CPCL encoder");
            int beforeInvalid = memory.size();
            main(() -> {
                try { model.setMedia(null); throw new AssertionError("Invalid media accepted"); }
                catch (IllegalArgumentException expected) { }
                model.inputError("Invalid media settings");
                check(model.state().mediaRequest == gap.get() && model.state().mediaRequest.mode == MediaMode.GAP,
                    "Rejected BLACK_MARK parameters cannot relabel previous GAP success");
                try { model.sendRaw(new byte[0], 1); throw new AssertionError("Empty Raw accepted"); }
                catch (IllegalArgumentException expected) { }
                check(model.state().mediaRequest == gap.get(), "Rejected Raw leaves the original media request unchanged");
            });
            check(memory.size() == beforeInvalid, "Rejected inputs do no printer IO");
            main(() -> printPixels(model, new LabelConfig(203, 256, 128, 128, 1, 2))); until(() -> model.state().canWork());
            main(() -> model.queryStatus(false)); until(() -> model.state().canWork());
            main(model::capabilities); until(() -> model.state().canWork());
            main(() -> model.feed()); until(() -> model.state().canWork());
            main(() -> check(model.state().mediaRequest == gap.get(), "Printing, status, capabilities and feed preserve the original media request"));
            memory.blocked = true;
            int writesBefore = memory.writes.get();
            main(() -> model.setMedia(MediaMode.BLACK_MARK));
            until(() -> memory.writes.get() > writesBefore);
            AtomicReference<DemoUiState.MediaRequest> black = new AtomicReference<>();
            main(() -> {
                black.set(model.state().mediaRequest);
                check(black.get().mode == MediaMode.BLACK_MARK && black.get().result == null &&
                    black.get().connectionId == gap.get().connectionId && black.get().requestId != gap.get().requestId,
                    "New media submission replaces old success with its own pending request");
            });
            memory.blocked = false; until(() -> model.state().canWork());
            main(() -> check(model.state().mediaRequest.mode == MediaMode.BLACK_MARK &&
                model.state().mediaRequest.result.raw.outcome == ManagerResult.Outcome.SUCCESS && black.get().result == null &&
                gap.get().result.raw.outcome == ManagerResult.Outcome.SUCCESS,
                "Media completion is bound to BLACK_MARK and old immutable snapshots retain their original results"));
            main(() -> { model.sendRaw(new byte[]{82}, 1); check(model.state().mediaRequest == null, "Accepted Raw immediately invalidates previous media"); });
            until(() -> model.state().canWork());
            main(() -> model.setMedia(MediaMode.GAP)); until(() -> model.state().canWork());
            memory.blocked = true;
            int writesBeforeDisconnect = memory.writes.get();
            AtomicReference<RequestId> interrupted = new AtomicReference<>();
            main(() -> { model.setMedia(MediaMode.CONTINUOUS); interrupted.set(model.state().mediaRequest.requestId); });
            until(() -> memory.writes.get() > writesBeforeDisconnect);
            main(() -> { model.cancel(); model.disconnect(); check(model.state().mediaRequest == null, "Disconnect immediately invalidates pending media"); });
            until(() -> model.state().canConnect()); memory.blocked = false;
            main(() -> {
                check(model.state().mediaRequest == null && model.state().lastRequest.kind == DemoResult.Kind.MEDIA &&
                    model.state().lastRequest.raw.requestId == interrupted.get() &&
                    model.state().lastRequest.raw.outcome != ManagerResult.Outcome.SUCCESS,
                    "Actual interrupted media terminal remains in history without restoring connection media");
                model.connect(TARGET);
            });
            until(() -> model.state().canWork());
            main(() -> {
                check(model.state().mediaRequest != null && model.state().mediaRequest.connectionId != gap.get().connectionId,
                    "Explicit reconnect performs a fresh initialization instead of restoring old media evidence");
                model.setMedia(MediaMode.GAP);
            });
            until(() -> model.state().canWork());
            main(() -> {
                check(model.state().mediaRequest.connectionId != gap.get().connectionId, "Reconnected media is bound to the new connection");
                model.background();
                check(model.state().mediaRequest == null && model.state().connectionInfo == null,
                    "Background cleanup immediately invalidates media and adopted configuration");
                model.foreground();
            });
            until(() -> model.state().canConnect());
            main(() -> { check(model.state().mediaRequest == null, "Foreground does not restore media or reconnect"); model.connect(TARGET); });
            until(() -> model.state().canWork());
            main(() -> model.setMedia(MediaMode.GAP)); until(() -> model.state().canWork());
            memory.close();
            main(() -> model.feed());
            until(() -> model.state().disconnectedResult != null);
            main(() -> check(model.state().mediaRequest == null && model.state().connectionInfo == null && model.state().canConnect(),
                "Unexpected transport loss invalidates current connection media and adopted configuration"));
        } finally { memory.blocked = false; close(model, manager); }
    }
    private static void cancelledPreparationDoesNotReplay() throws Exception {
        ExecutorService workers = Executors.newSingleThreadExecutor(task -> { Thread thread = new Thread(task); thread.setDaemon(true); return thread; });
        Memory memory = new Memory(); PrinterManager manager = manager(memory, null, null, workers);
        AtomicReference<DemoModel> reference = new AtomicReference<>();
        main(() -> { reference.set(new DemoModel(manager)); reference.get().foreground(); reference.get().connect(TARGET); });
        DemoModel model = reference.get();
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        File sourceFile = File.createTempFile("printer-demo-cancelled-source-", ".png");
        DemoImageSource source = DemoImageSource.ownedFile(sourceFile);
        try {
            until(() -> model.state().canWork());
            int initializedBytes = memory.size(), initializedWrites = memory.writes.get();
            workers.execute(() -> {
                entered.countDown();
                try { release.await(); } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
            });
            check(entered.await(5, TimeUnit.SECONDS), "Background preparation gate entered");
            LabelConfig config = new LabelConfig(203, 256, 128, 128, 2, 2);
            main(() -> model.print(source, config));
            source.close();
            check(sourceFile.exists(), "Queued request retains its image after the selected source is released");
            main(model::cancel);
            until(() -> model.state().canWork());
            main(() -> check(model.state().lastRequest.raw.code == 1005 && model.state().cleanupResult == null,
                "Preparation cancellation is independent of cleanup"));
            until(() -> !sourceFile.exists());
            check(memory.size() == initializedBytes && memory.writes.get() == initializedWrites,
                "Cancellation releases the source before preparation or printer IO starts");
            release.countDown(); workers.submit(() -> { }).get(5, TimeUnit.SECONDS);
            main(() -> model.sendRaw(new byte[]{82}, 1)); until(() -> model.state().canWork());
            check(memory.output().substring(initializedBytes).equals("R") && memory.opens.get() == 1, "Cancelled preparation never enters printer IO or replays");
        } finally { source.close(); release.countDown(); close(model, manager); }
    }

    private static void cancelledFilePreparationDoesNotReplay() throws Exception {
        ExecutorService workers = Executors.newSingleThreadExecutor(task -> { Thread thread = new Thread(task); thread.setDaemon(true); return thread; });
        Memory memory = new Memory(); PrinterManager manager = manager(memory, null, null, workers);
        AtomicReference<DemoModel> reference = new AtomicReference<>();
        main(() -> { reference.set(new DemoModel(manager)); reference.get().foreground(); reference.get().connect(TARGET); });
        DemoModel model = reference.get();
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        File sourceFile = File.createTempFile("printer-demo-borrowed-source-", ".png");
        try {
            until(() -> model.state().canWork());
            int initializedBytes = memory.size(), initializedWrites = memory.writes.get();
            LabelConfig config = new LabelConfig(203, 256, 128, 128, 1, 2);
            main(() -> {
                try { model.printFile(null, config); throw new AssertionError("Null file accepted"); }
                catch (IllegalArgumentException expected) { }
            });
            workers.execute(() -> {
                entered.countDown();
                try { release.await(); } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
            });
            check(entered.await(5, TimeUnit.SECONDS), "Direct-file preparation gate entered");
            main(() -> model.printFile(sourceFile, config));
            main(model::cancel); until(() -> model.state().canWork());
            main(() -> check(model.state().lastRequest.raw.code == 1005 && model.state().cleanupResult == null,
                "Queued direct-file cancellation retains the real terminal result"));
            check(sourceFile.exists() && memory.size() == initializedBytes && memory.writes.get() == initializedWrites,
                "Direct-file cancellation keeps the caller's file and sends no printer data");
            release.countDown(); workers.submit(() -> { }).get(5, TimeUnit.SECONDS);
            main(() -> model.sendRaw(new byte[]{70}, 1)); until(() -> model.state().canWork());
            check(memory.output().substring(initializedBytes).equals("F") && memory.opens.get() == 1,
                "Cancelled direct-file preparation never replays or reconnects");
        } finally {
            release.countDown();
            try {
                close(model, manager);
                check(sourceFile.exists(), "Closing the Model never deletes the caller's cancelled file");
            } finally {
                if (sourceFile.exists()) check(sourceFile.delete(), "Caller removes its direct-file cancellation fixture");
            }
        }
    }

    /** Android 图片解码使用真实框架，打印仍注入内存传输，不连接蓝牙或输出纸张。 */
    static void sourcePrintChecks(Context context, File png) throws Exception {
        DemoImageSource source = DemoImageSource.ownedFile(png);
        File directFile = File.createTempFile("caller-owned-print-", ".png", png.getParentFile());
        File invalidFile = File.createTempFile("caller-owned-invalid-", ".png", png.getParentFile());
        Memory memory = new Memory();
        PrinterManager manager = null;
        AtomicReference<DemoModel> reference = new AtomicReference<>();
        try {
            try (FileInputStream input = new FileInputStream(png)) { ImageImport.copy(input, directFile, () -> false); }
            try (FileOutputStream output = new FileOutputStream(invalidFile)) { output.write(new byte[]{1,2,3,4,5,6,7,8}); }
            PrinterManager created = manager(context, memory, null, null, pool());
            manager = created;
            main(() -> { reference.set(new DemoModel(created)); reference.get().foreground(); reference.get().connect(TARGET); });
            DemoModel model = reference.get();
            until(() -> model.state().canWork());
            LabelConfig first = new LabelConfig(300, 384, 129, 128, 2, 1, 31, false, 9);
            configure(model, first); main(() -> model.printFile(directFile, first)); until(() -> model.state().canWork());
            main(() -> check(model.state().lastRequest.raw.code == 0 &&
                model.state().lastRequest.raw.completion == ManagerResult.Completion.DATA_SENT,
                "Ordinary file prints through printFile without preparing or opening a preview"));
            check(directFile.exists(), "Successful direct-file printing preserves the caller's source");
            String firstBytes = memory.output();
            check(count(firstBytes, "! 0 200 200 137 1\r\nPAGE-WIDTH 384\r\n") == 2 &&
                count(firstBytes, "CG 17 128 31 0 ") == 2 && count(firstBytes, "FORM\r\nPRINT\r\n") == 2,
                "Direct file printing preserves geometry, copies, origin, FORM and blank space");
            int before = memory.size();
            LabelConfig resized = new LabelConfig(300, 384, 256, 160, 1, 2);
            configure(model, resized); main(() -> model.printFile(directFile, resized)); until(() -> model.state().canWork());
            main(() -> check(model.state().lastRequest.raw.code == 0 &&
                model.state().lastRequest.raw.completion == ManagerResult.Completion.DATA_SENT,
                "Same file prints with changed parameters without generating a preview"));
            check(directFile.exists(), "Printing with changed dimensions preserves the caller's source");
            String resizedBytes = memory.output().substring(before);
            check(resizedBytes.contains("! 0 200 200 160 1\r\nPAGE-WIDTH 384\r\n") &&
                resizedBytes.contains("CG 32 128 0 0 ") && !resizedBytes.contains("FORM\r\n"),
                "Direct file printing uses the new width, height and FORM choice");
            before = memory.size();
            configure(model, first); main(() -> model.print(source, first)); until(() -> model.state().canWork());
            main(() -> check(model.state().lastRequest.raw.code == 0 &&
                model.state().lastRequest.raw.completion == ManagerResult.Completion.DATA_SENT,
                "Managed image source retains its independent print entry without a preview"));
            String managedBytes = memory.output().substring(before);
            check(count(managedBytes, "CG 17 128 31 0 ") == 2 && count(managedBytes, "FORM\r\nPRINT\r\n") == 2,
                "Managed private source still preserves requested geometry and copies");
            before = memory.size();
            try (DemoImageSource builtin = DemoImageSource.builtin()) {
                main(() -> model.print(builtin, first)); until(() -> model.state().canWork());
                main(() -> check(model.state().lastRequest.raw.code == 0,
                    "Built-in calibration image uses the same print entry without a preview"));
            }
            String builtinBytes = memory.output().substring(before);
            check(count(builtinBytes, "CG 17 128 31 0 ") == 2 && count(builtinBytes, "FORM\r\nPRINT\r\n") == 2,
                "Built-in source preserves the same requested geometry and copies as file printing");
            int bytesBeforeFailure = memory.size(), writesBeforeFailure = memory.writes.get();
            main(() -> model.printFile(invalidFile, first)); until(() -> model.state().canWork());
            main(() -> check(model.state().lastRequest.raw.outcome == ManagerResult.Outcome.FAILURE,
                "Invalid ordinary image reports an asynchronous print failure"));
            check(memory.size() == bytesBeforeFailure && memory.writes.get() == writesBeforeFailure && invalidFile.exists(),
                "Invalid image preparation sends no printer data and preserves the caller's file");
            check(memory.opens.get() == 1, "Direct image printing reuses the current connection");
        } finally {
            source.close();
            try {
                if (manager != null) close(reference.get(), manager);
                check(directFile.exists(), "Closing the Model preserves the caller's successfully printed source");
                check(invalidFile.exists(), "Closing the Model preserves the caller's failed image source");
            } finally {
                if (directFile.exists()) check(directFile.delete(), "Caller removes its direct-file print fixture");
                if (invalidFile.exists()) check(invalidFile.delete(), "Caller removes its failed direct-file fixture");
            }
        }
        until(() -> !png.exists());
    }
}
