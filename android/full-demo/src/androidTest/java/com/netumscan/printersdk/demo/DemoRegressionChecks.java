// SPDX-License-Identifier: MIT
package com.netumscan.printersdk.demo;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import com.netumscan.printersdk.BleOptions;
import com.netumscan.printersdk.ConnectionOptions;
import com.netumscan.printersdk.ConnectionInfo;
import com.netumscan.printersdk.DiscoveredDevice;
import com.netumscan.printersdk.ManagerResult;
import com.netumscan.printersdk.MediaMode;
import com.netumscan.printersdk.RequestId;
import com.netumscan.printersdk.PrinterManager;
import com.netumscan.printersdk.PrinterProfile;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Focused Full Demo regressions. Synthetic state only; no Bluetooth, permission changes or document UI. */
final class DemoRegressionChecks {
    private final Instrumentation instrumentation;

    private DemoRegressionChecks(Instrumentation instrumentation) { this.instrumentation = instrumentation; }

    static void run(Instrumentation instrumentation) throws Exception { run(instrumentation, "all"); }

    /** Individual selectors also allow the pre-fix APK to reproduce the media/export failures. */
    static void run(Instrumentation instrumentation, String selection) throws Exception {
        DemoRegressionChecks checks = new DemoRegressionChecks(instrumentation);
        switch (selection) {
        case "permissions": checks.permissionRules(); return;
        case "media": checks.invalidMediaKeepsPreviousResult(); return;
        case "export": checks.exportPreparationSurvivesRecreation(); return;
        case "profile-auto": checks.profileDraftDoesNotConfigureConnection(); return;
        case "workflow": checks.connectionFirstWorkflow(); return;
        case "draft-migration": checks.centeredDraftCannotBecomeManualOrigin(); return;
        case "profiles":
            checks.transportPresetOnly(); checks.profileDraftDoesNotConfigureConnection(); checks.profileDraftRestoration();
            checks.centeredDraftCannotBecomeManualOrigin(); checks.connectionHistoryAcrossRecreation(); return;
        case "print":
            checks.queueChecks();
            checks.printRequests(); return;
        case "all":
            checks.permissionRules();
            checks.invalidMediaKeepsPreviousResult();
            checks.exportPreparationSurvivesRecreation();
            checks.transportPresetOnly();
            checks.profileDraftDoesNotConfigureConnection();
            checks.profileDraftRestoration();
            checks.centeredDraftCannotBecomeManualOrigin();
            checks.connectionHistoryAcrossRecreation();
            checks.connectionFirstWorkflow();
            checks.queueChecks();
            checks.printRequests();
            return;
        default: throw new IllegalArgumentException("Unknown Full Demo regression selection: " + selection);
        }
    }

    private void progress(String stage) {
        Bundle status = new Bundle(); status.putString("stream", "CHECK: " + stage + "\n");
        instrumentation.sendStatus(0, status);
    }

    private void queueChecks() throws Exception {
        // Keep the test process foreground on OEM devices which freeze cached processes.
        // This offline Activity never connects; the tested Manager uses only its memory transport.
        MainActivity foreground = launch();
        try { DemoQueueChecks.run(instrumentation.getTargetContext(), this::progress); }
        finally { onMain(foreground::finish); }
    }

    /** Actual disclosures and profile recovery, with one real Manager and a memory-only channel. */
    private void connectionFirstWorkflow() throws Exception {
        progress("connection-first workflow and progressive controls");
        MainActivity original = launch();
        AtomicReference<MainActivity> screen = new AtomicReference<>(original);
        DemoOwner owner = mainValue(() -> (DemoOwner)field(original, "owner"));
        DemoModel model = owner.model;
        DemoQueueChecks.Memory memory = new DemoQueueChecks.Memory();
        PrinterManager manager = DemoQueueChecks.manager(instrumentation.getTargetContext(), memory);
        try {
            PrinterManager initial = mainValue(() -> (PrinterManager)field(model, "manager"));
            CountDownLatch initialClosed = new CountDownLatch(1);
            initial.close(result -> initialClosed.countDown());
            check(initialClosed.await(10, TimeUnit.SECONDS), "Offline Manager closes before workflow injection");
            onMain(() -> {
                setField(model, "manager", manager);
                for (int panel : new int[]{R.id.device_advanced, R.id.print_advanced, R.id.examples_panel, R.id.diagnostics_panel})
                    check(original.findViewById(panel).getVisibility() == View.GONE,
                        "Optional controls start collapsed: " + original.getResources().getResourceEntryName(panel));
                check(!original.findViewById(R.id.dpi).isShown() && !original.findViewById(R.id.width).isShown(),
                    "Normal workflow does not ask the user to repeat device geometry");
                original.findViewById(R.id.device_advanced_toggle).performClick();
                check(!original.findViewById(R.id.dpi).isShown() && !original.findViewById(R.id.width).isShown() &&
                    !original.findViewById(R.id.apply_print_profile).isShown(),
                    "Device disclosure offers transport controls and no pre-connection print Profile editor");
                original.findViewById(R.id.device_advanced_toggle).performClick();
                set(original, R.id.label_width, "256"); set(original, R.id.height, "128");
                // A legacy draft may contain a previously resolved absolute X; it is never a calibration.
                set(original, R.id.x_offset, "232"); set(original, R.id.origin_adjustment, "0");
                select(original, R.id.form, 2);
            });
            onMain(() -> select(original, R.id.transport, 1));
            instrumentation.waitForIdleSync();
            chooseCandidate(original, model, "Synthetic unknown BLE printer");
            onMain(() -> {
                try {
                    call(original, "connectionOptions");
                    throw new AssertionError("Unknown BLE device silently inherited the M90 channel");
                } catch (java.lang.reflect.InvocationTargetException expected) {
                    check(expected.getCause() instanceof LabelConfig.ValidationError &&
                        "service".equals(((LabelConfig.ValidationError)expected.getCause()).field),
                        "Unknown BLE device requests an explicit channel instead of guessing M90 UUIDs");
                }
                original.findViewById(R.id.connect).performClick();
                check(original.findViewById(R.id.device_advanced).getVisibility() == View.VISIBLE &&
                    ((EditText)original.findViewById(R.id.service)).getError() != null && memory.opens.get() == 0,
                    "Actual validation reveals advanced settings without opening a channel or requesting permissions");
                original.findViewById(R.id.device_advanced_toggle).performClick();
                select(original, R.id.transport, 0);
            });
            instrumentation.waitForIdleSync();
            chooseCandidate(original, model, "LT-M90_AB12");
            onMain(() -> model.connect((ConnectionOptions)call(original, "connectionOptions")));
            await(() -> mainValue(() -> model.state().canWork() && model.configured()),
                "Known-name Manager connection completes media initialization");
            onMain(() -> {
                assertNameConnection(model);
                String summary = text(original, R.id.profile_summary);
                check(summary.contains("LT-M90") && summary.contains("CPCL") && summary.contains("203") &&
                    summary.contains("864") && summary.contains("8.0 dots/mm") && summary.contains("固定居中") && summary.contains("名称"),
                    "Readonly summary displays the actual adopted Profile and name source: " + summary);
                check(!original.findViewById(R.id.apply_print_profile).isShown() &&
                    !original.findViewById(R.id.dpi).isEnabled() && !original.findViewById(R.id.width).isEnabled(),
                    "Known device keeps its adopted geometry readonly and optional override collapsed");
                check(choice(original, R.id.origin_mode) == OriginPlacement.CENTERED,
                    "The adopted Profile fixes centered label placement");
                set(original, R.id.label_width, "400"); set(original, R.id.height, "400");
                LabelConfig centered = (LabelConfig)call(original, "readConfig");
                check(centered.xOffsetDots == 232 && text(original, R.id.x_offset).equals("232") &&
                    centered.printerWidthDots == 864 && centered.manualOriginDots == 0 &&
                    centered.horizontalCalibrationDots == 0 && !original.findViewById(R.id.x_offset).isEnabled(),
                    "A 50 mm label centers at X232 without replacing the automatic 864-dot Profile");
                set(original, R.id.label_width, "401");
                check(((LabelConfig)call(original, "readConfig")).xOffsetDots == 228 &&
                    text(original, R.id.x_offset).equals("228"),
                    "Changing label width recomputes centering using the padded 408-dot raster width");
                set(original, R.id.label_width, "400");
                original.findViewById(R.id.device_advanced_toggle).performClick();
                check(original.findViewById(R.id.apply_print_profile).isShown(),
                    "Advanced device controls retain an explicit override action");
                original.findViewById(R.id.print_advanced_toggle).performClick();
                check(!original.findViewById(R.id.origin_mode).isShown() &&
                    !original.findViewById(R.id.origin_mode).isEnabled(), "LT-M90 exposes no per-print placement override");
                set(original, R.id.origin_adjustment, "-3");
                check(((LabelConfig)call(original, "readConfig")).xOffsetDots == 229,
                    "Optional calibration is applied to the SDK centered origin");
                original.findViewById(R.id.diagnostics_toggle).performClick();
            });
            int opens = memory.opens.get(), sent = memory.size();
            MainActivity current = recreate(original); screen.set(current);
            onMain(() -> {
                check(field(current, "owner") == owner && field(current, "model") == model,
                    "Workflow recreation keeps the same callback owner and Manager");
                check(current.findViewById(R.id.device_advanced).getVisibility() == View.VISIBLE &&
                    current.findViewById(R.id.print_advanced).getVisibility() == View.VISIBLE &&
                    current.findViewById(R.id.diagnostics_panel).getVisibility() == View.VISIBLE &&
                    current.findViewById(R.id.examples_panel).getVisibility() == View.GONE,
                    "Real recreation preserves each disclosure independently");
                call(current, "render"); call(current, "render");
                check(choice(current, R.id.origin_mode) == OriginPlacement.CENTERED &&
                    ((LabelConfig)call(current, "readConfig")).xOffsetDots == 229 &&
                    ((LabelConfig)call(current, "readConfig")).horizontalCalibrationDots == -3 &&
                    !current.findViewById(R.id.origin_mode).isEnabled(),
                    "Recreation preserves calibration and the adopted Profile continues to fix centered placement");
                check(memory.opens.get() == opens && memory.size() == sent && model.state().printRequests.isEmpty(),
                    "Disclosure restoration and rendering never reconnect, initialize again or print");
                current.findViewById(R.id.device_advanced_toggle).performClick();
                model.disconnect();
            });
            await(() -> mainValue(() -> model.state().phase == DemoUiState.Phase.OFFLINE),
                "Known connection completes its cleanup before choosing another device");
            DemoSettings calibrated = mainValue(() -> (DemoSettings)call(current, "readSettings"));
            check(((Number)calibrated.values.get("origin_adjustment")).intValue() == -3 &&
                ((Number)calibrated.values.get("origin_mode")).intValue() == OriginPlacement.CENTERED &&
                ((Number)calibrated.values.get("x_offset")).intValue() == 229,
                "Disconnected settings preserve the signed calibration and its old resolved-origin draft");
            onMain(() -> check(model.printProfile() == null &&
                ((LabelConfig)call(current, "readConfig")).manualOriginDots == 0,
                "An offline centered draft cannot become an explicit manual origin or adopt a Profile"));
            java.io.File settingsDirectory = new java.io.File(instrumentation.getTargetContext().getCacheDir(),
                "centered-settings-regression-" + System.nanoTime());
            check(settingsDirectory.mkdir(), "Create isolated settings storage for the real save/load round trip");
            SettingsStore settingsStore = new SettingsStore(settingsDirectory);
            DemoSettings loaded;
            try { settingsStore.save(calibrated); loaded = settingsStore.load(); }
            finally { settingsStore.clear(); check(settingsDirectory.delete(), "Remove isolated settings test storage"); }
            onMain(() -> {
                Method apply = MainActivity.class.getDeclaredMethod("applySettings", DemoSettings.class);
                apply.setAccessible(true); apply.invoke(current, loaded);
                check(model.printProfile() == null && !model.state().connected && memory.opens.get() == opens &&
                    text(current, R.id.origin_adjustment).equals("-3") &&
                    ((LabelConfig)call(current, "readConfig")).manualOriginDots == 0,
                    "Loading saved calibration remains a local draft and does not configure or reconnect");
            });
            chooseCandidate(current, model, "LT-M90_AB12");
            onMain(() -> model.connect((ConnectionOptions)call(current, "connectionOptions")));
            await(() -> mainValue(() -> model.state().canWork() && model.configured()),
                "Explicit reconnect re-adopts LT-M90 after calibration save/load");
            onMain(() -> {
                LabelConfig restored = (LabelConfig)call(current, "readConfig");
                check(restored.horizontalCalibrationDots == -3 && restored.xOffsetDots == 229 &&
                    restored.manualOriginDots == 0 && !current.findViewById(R.id.origin_mode).isEnabled() &&
                    !current.findViewById(R.id.x_offset).isEnabled(),
                    "Reconnect retains -3 calibration and SDK X229 without exposing a manual placement override");
                model.disconnect();
            });
            await(() -> mainValue(() -> model.state().phase == DemoUiState.Phase.OFFLINE),
                "Calibration round-trip connection completes cleanup");
            chooseCandidate(current, model, "Synthetic unknown printer");
            int beforeUnknown = memory.size();
            onMain(() -> model.connect((ConnectionOptions)call(current, "connectionOptions")));
            await(() -> mainValue(() -> model.state().connected && model.state().canWork()),
                "Unknown printer can connect without inventing a print Profile");
            onMain(() -> {
                check(model.state().connectionInfo != null && model.state().connectionInfo.printProfile == null &&
                    model.state().connectionInfo.configurationSource == ConnectionInfo.ConfigurationSource.NONE,
                    "Unknown connection result keeps its absent Profile and source");
                check(memory.size() == beforeUnknown && !model.configured() &&
                    !current.findViewById(R.id.print).isEnabled() && !current.findViewById(R.id.print_fifo).isEnabled(),
                    "Unknown connection sends no media or print request until configuration is supplied");
                check(current.findViewById(R.id.apply_print_profile).isShown() &&
                    text(current, R.id.apply_print_profile).contains("补充") &&
                    text(current, R.id.profile_summary).contains("补充"),
                    "Missing Profile exposes its recovery action without opening advanced controls");
                current.findViewById(R.id.apply_print_profile).performClick();
            });
            // AlertDialog delivers onShow on the next main-loop turn before the real user can click.
            instrumentation.waitForIdleSync();
            onMain(() -> {
                AlertDialog dialog = (AlertDialog)field(current, "utilityDialog");
                check(dialog != null && dialog.isShowing(), "Missing Profile opens the actual configuration dialog");
                java.util.List<EditText> inputs = new java.util.ArrayList<>();
                collectViews(dialog.getWindow().getDecorView(), EditText.class, inputs);
                java.util.List<Spinner> protocols = new java.util.ArrayList<>();
                collectViews(dialog.getWindow().getDecorView(), Spinner.class, protocols);
                check(inputs.size() == 3 && protocols.size() == 2,
                    "Profile recovery includes optional precise density and the label placement rule");
                protocols.get(0).setSelection(0); inputs.get(0).setText("203"); inputs.get(1).setText("864");
                inputs.get(2).setText("8"); protocols.get(1).setSelection(0);
                memory.blocked = true;
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
            });
            await(() -> mainValue(() -> model.state().connectionInfo != null &&
                model.state().connectionInfo.configurationSource == ConnectionInfo.ConfigurationSource.EXPLICIT &&
                model.state().mediaRequest != null && model.state().mediaRequest.result == null),
                "Explicit Profile succeeds before its independent media request completes");
            onMain(() -> check(!current.findViewById(R.id.print).isEnabled() && !model.configured(),
                "Profile success alone does not bypass pending CPCL media initialization"));
            memory.blocked = false;
            await(() -> mainValue(() -> model.state().canWork() && model.configured()),
                "Explicit configuration and media initialization restore normal printing");
            onMain(() -> {
                set(current, R.id.origin_adjustment, "0"); set(current, R.id.x_offset, "17");
                check(current.findViewById(R.id.print).isEnabled(),
                    "Configured unknown device can now print the existing image without a preview");
                LabelConfig manual = (LabelConfig)call(current, "readConfig");
                check(current.findViewById(R.id.x_offset).isEnabled() && manual.xOffsetDots == 17 &&
                    manual.manualOriginDots == 17 && manual.printProfile().labelAlignment == PrinterProfile.LabelAlignment.LEFT,
                    "A connected generic LEFT Profile retains the existing advanced manual origin");
            });
            DemoUiState.MediaRequest previousMedia = mainValue(() -> model.state().mediaRequest);
            int beforeChoice = memory.size();
            onMain(() -> select(current, R.id.media, 1));
            instrumentation.waitForIdleSync();
            onMain(() -> {
                check(memory.size() == beforeChoice && model.state().mediaRequest == previousMedia,
                    "Changing the medium selection neither sends IO nor relabels the previous terminal");
                check(!current.findViewById(R.id.print).isEnabled() &&
                    current.findViewById(R.id.apply_media).isEnabled(),
                    "Changed medium requires its explicit apply action before printing");
                memory.blocked = true; current.findViewById(R.id.apply_media).performClick();
            });
            await(() -> mainValue(() -> model.state().mediaRequest != null &&
                model.state().mediaRequest != previousMedia && model.state().mediaRequest.mode == MediaMode.BLACK_MARK),
                "Apply submits a fresh media request for the current selection");
            memory.blocked = false;
            await(() -> mainValue(() -> model.state().canWork() && model.configured()),
                "Explicitly applied medium receives its real completion");
            onMain(() -> {
                check(current.findViewById(R.id.print).isEnabled() && model.state().mediaRequest.result.raw.code == 0,
                    "Only successful matching media completion restores the print action");
                check(memory.opens.get() == opens + 2 && model.state().printRequests.isEmpty(),
                    "Workflow performs only three intentional connections and never automatically prints");
            });
            onMain(model::disconnect);
            await(() -> mainValue(() -> model.state().phase == DemoUiState.Phase.OFFLINE),
                "Workflow releases its connection before loading explicit saved BLE options");
            onMain(() -> {
                select(current, R.id.transport, 1);
                call(current, "applyPreset");
                DemoSettings saved = (DemoSettings)call(current, "readSettings");
                java.util.Map<String, Object> fromJson = new java.util.LinkedHashMap<>(saved.values);
                for (String key : fromJson.keySet()) {
                    Object value = fromJson.get(key);
                    if (value instanceof Number) fromJson.put(key, ((Number)value).longValue());
                }
                owner.m90BleExplicit = false;
                Method apply = MainActivity.class.getDeclaredMethod("applySettings", DemoSettings.class);
                apply.setAccessible(true); apply.invoke(current, new DemoSettings(fromJson));
                check(owner.m90BleExplicit,
                    "JsonReader Long-valued saved M90 BLE options retain their explicit transport choice");
                check(memory.opens.get() == opens + 2,
                    "Loading saved explicit BLE options remains a local action without reconnecting");
            });
        } finally {
            memory.blocked = false;
            onMain(() -> { if (!screen.get().isFinishing()) screen.get().finish(); });
            CountDownLatch closed = new CountDownLatch(1); manager.close(result -> closed.countDown());
            check(closed.await(10, TimeUnit.SECONDS), "Workflow test releases its native memory channel");
        }
    }

    private static <T extends View> void collectViews(View view, Class<T> type, java.util.List<T> result) {
        if (type.isInstance(view)) result.add(type.cast(view));
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup)view;
            for (int child = 0; child < group.getChildCount(); child++) collectViews(group.getChildAt(child), type, result);
        }
    }

    /** All request terminals survive an observer gap, including connect/profile followed by media. */
    private void connectionHistoryAcrossRecreation() throws Exception {
        progress("connection/profile history across Activity recreation");
        MainActivity original = launch();
        AtomicReference<MainActivity> screen = new AtomicReference<>(original);
        DemoOwner owner = mainValue(() -> (DemoOwner)field(original, "owner"));
        DemoModel model = owner.model;
        DemoQueueChecks.Memory memory = new DemoQueueChecks.Memory();
        PrinterManager manager = DemoQueueChecks.manager(instrumentation.getTargetContext(), memory);
        try {
            PrinterManager initial = mainValue(() -> (PrinterManager)field(model, "manager"));
            CountDownLatch initialClosed = new CountDownLatch(1);
            initial.close(result -> initialClosed.countDown());
            check(initialClosed.await(10, TimeUnit.SECONDS), "Offline Manager closes before history test injection");
            onMain(() -> {
                setField(model, "manager", manager);
                owner.stopUi((Runnable)field(original, "listener"), true);
                memory.blocked = true;
                model.connect(DemoQueueChecks.namedTarget("AA:BB:CC:DD:EE:02"));
            });
            await(() -> memory.writes.get() > 0, "Connect completed while initial media command waits");
            MainActivity current = recreate(original); screen.set(current);
            onMain(() -> {
                check(field(current, "owner") == owner, "Recreation retains the exact callback owner");
                owner.stopUi((Runnable)field(current, "listener"), true);
            });
            memory.blocked = false;
            await(() -> mainValue(() -> model.state().canWork() && model.configured()), "Initial media succeeds without an Activity observer");
            onMain(() -> model.configureProfile(PrinterProfile.cpcl(300, 384), MediaMode.BLACK_MARK));
            await(() -> mainValue(() -> model.state().canWork() && model.configured() && model.printProfile().dpi == 300),
                "Profile and new media both complete while detached");
            onMain(owner::recordResults);
            await(() -> mainValue(() -> logCount(owner, "CONNECT outcome=SUCCESS") == 1 &&
                logCount(owner, "PROFILE outcome=SUCCESS") == 1 && logCount(owner, "MEDIA outcome=SUCCESS") == 2),
                "Owner catches up every connect/profile/media terminal rather than just latestResult");
            onMain(() -> { owner.recordResults(); owner.recordResults(); });
            onMain(() -> check(logCount(owner, "CONNECT outcome=SUCCESS") == 1 &&
                logCount(owner, "PROFILE outcome=SUCCESS") == 1 && logCount(owner, "MEDIA outcome=SUCCESS") == 2,
                "Repeated observation never repeats terminal journal entries"));
        } finally {
            memory.blocked = false;
            onMain(() -> { if (!screen.get().isFinishing()) screen.get().finish(); });
            CountDownLatch closed = new CountDownLatch(1); manager.close(result -> closed.countDown());
            check(closed.await(10, TimeUnit.SECONDS), "History test releases its native memory device");
        }
    }
    private static int logCount(DemoOwner owner, String token) {
        int count = 0;
        for (String line : owner.lines) if (line.contains(token)) count++;
        return count;
    }

    /** A retained Profile may change after the old Activity saved its centered draft. */
    private void centeredDraftCannotBecomeManualOrigin() throws Exception {
        progress("saved CENTERED draft with a newer retained LEFT Profile");
        MainActivity original = launch();
        AtomicReference<MainActivity> screen = new AtomicReference<>(original);
        DemoOwner owner = mainValue(() -> (DemoOwner)field(original, "owner"));
        DemoModel model = owner.model;
        DemoQueueChecks.Memory memory = new DemoQueueChecks.Memory();
        PrinterManager manager = DemoQueueChecks.manager(instrumentation.getTargetContext(), memory);
        try {
            PrinterManager initial = mainValue(() -> (PrinterManager)field(model, "manager"));
            CountDownLatch closed = new CountDownLatch(1); initial.close(result -> closed.countDown());
            check(closed.await(10, TimeUnit.SECONDS), "Draft migration closes the initial offline Manager");
            onMain(() -> {
                setField(model, "manager", manager);
                set(original, R.id.label_width, "400"); set(original, R.id.height, "400");
                select(original, R.id.form, 1);
            });
            chooseCandidate(original, model, "LT-M90_AB12");
            onMain(() -> model.connect((ConnectionOptions)call(original, "connectionOptions")));
            await(() -> mainValue(() -> model.state().canWork() && model.configured()), "Centered draft connection initializes");
            Bundle centered = mainValue(() -> {
                LabelConfig config = (LabelConfig)call(original, "readConfig");
                check(config.xOffsetDots == 232 && config.horizontalCalibrationDots == 0,
                    "Saved fixture is a real centered X232 draft");
                return saveViewState(original);
            });
            onMain(() -> model.configureProfile(PrinterProfile.cpcl(203, 864, 8, PrinterProfile.LabelAlignment.LEFT), MediaMode.GAP));
            await(() -> mainValue(() -> model.state().canWork() && model.configured() &&
                model.printProfile().labelAlignment == PrinterProfile.LabelAlignment.LEFT),
                "Retained owner adopts and initializes its newer LEFT Profile");
            int bytes = memory.size(), opens = memory.opens.get();
            MainActivity current = recreate(original); screen.set(current);
            onMain(() -> {
                check(field(current, "owner") == owner, "Migration uses the retained owner with the newer Profile");
                restoreDraft(current, centered.getBundle("inputs"), true);
                call(current, "render");
                assertZeroManualOrigin(current, "Explicit inputs restore");
                restoreViewState(current, centered);
            });
            instrumentation.waitForIdleSync();
            onMain(() -> {
                assertZeroManualOrigin(current, "Framework hierarchy restore");
                check(memory.size() == bytes && memory.opens.get() == opens && model.state().printRequests.isEmpty(),
                    "Restoring stale centered inputs does not configure, initialize, connect or print");
            });
        } finally {
            memory.blocked = false;
            onMain(() -> screen.get().finish());
            CountDownLatch closed = new CountDownLatch(1); manager.close(result -> closed.countDown());
            check(closed.await(10, TimeUnit.SECONDS), "Draft migration releases its memory device");
        }
    }

    private static void assertZeroManualOrigin(MainActivity activity, String stage) throws Exception {
        LabelConfig config = (LabelConfig)call(activity, "readConfig");
        check(config.printProfile().labelAlignment == PrinterProfile.LabelAlignment.LEFT && config.manualOriginDots == 0 &&
            config.xOffsetDots == 0 && text(activity, R.id.x_offset).equals("0"),
            stage + " must clear old resolved X232 rather than promote it to an absolute manual origin");
    }

    /** Actual same-page controls, native memory transport and configuration recreation. */
    private void printRequests() throws Exception {
        progress("same-page print controls");
        MainActivity original = launch();
        AtomicReference<MainActivity> screen = new AtomicReference<>(original);
        DemoOwner owner = mainValue(() -> (DemoOwner)field(original, "owner"));
        DemoModel model = owner.model;
        DemoQueueChecks.Memory memory = new DemoQueueChecks.Memory();
        PrinterManager manager = DemoQueueChecks.manager(instrumentation.getTargetContext(), memory);
        try {
            PrinterManager initial = mainValue(() -> (PrinterManager)field(model, "manager"));
            CountDownLatch initialClosed = new CountDownLatch(1);
            initial.close(result -> initialClosed.countDown());
            check(initialClosed.await(10, TimeUnit.SECONDS), "Initial offline UI Manager closes before test injection");
            onMain(() -> original.findViewById(R.id.examples_toggle).performClick());
            instrumentation.waitForIdleSync();
            onMain(() -> {
                // Test-only replacement: production still constructs one public Manager in DemoOwner.
                setField(model, "manager", manager);
                restorePrintDraft(original, "203", "256");
                set(original, R.id.label_width, "256"); set(original, R.id.height, "128");
                select(original, R.id.form, 2); select(original, R.id.pixel_example, 1);
                model.connect(DemoQueueChecks.namedTarget("AA:BB:CC:DD:EE:02"));
            });
            await(() -> mainValue(() -> model.state().canWork()), "Same-page test connection succeeds through native memory transport");
            instrumentation.waitForIdleSync();
            DemoImageSource source = mainValue(() -> owner.images.source());
            onMain(() -> {
                check(!original.findViewById(R.id.preview).isEnabled(), "Pixel examples do not preview the unrelated imported image");
                check(original.findViewById(R.id.print_fifo).isEnabled(), "FIFO control is available after explicit connection");
                memory.blocked = true; original.findViewById(R.id.print_fifo).performClick();
                check(model.state().pendingPrintCount() == 3, "One FIFO click registers exactly three separate requests");
                check(original.findViewById(R.id.print).isEnabled() && original.findViewById(R.id.pixel_example).isEnabled(),
                    "More prints and pixel examples remain available while requests wait");
                check(!original.findViewById(R.id.width).isEnabled() && !original.findViewById(R.id.send_raw).isEnabled(),
                    "Pending prints preserve configuration and exclusive control boundaries");
            });
            await(() -> memory.writes.get() > 0, "First native write reaches the deterministic gate");
            RequestId middle = mainValue(() -> model.state().printRequests.get(1).requestId);
            onMain(() -> {
                Button cancel = requestCancel(original, middle);
                check(cancel != null && cancel.isEnabled(), "Each waiting row exposes its own RequestId cancellation");
                cancel.performClick();
                check(model.state().printRequests.get(1).cancelling, "Selected row retains cancellation intent until real completion");
            });
            await(() -> mainValue(() -> model.state().printRequests.get(1).result != null), "Middle request cancellation receives a terminal result");
            progress("multi-request Activity recreation");
            MainActivity current = recreate(original); screen.set(current);
            onMain(() -> {
                check(field(current, "owner") == owner && field(current, "model") == model, "Recreation retains the same multi-request owner and Manager");
                check(choice(current, R.id.pixel_example) == 1 && model.state().printRequests.size() == 3 && model.state().pendingPrintCount() == 2,
                    "Recreation restores selected example and all request records without resubmission");
                check(requestCancel(current, middle) == null && memory.opens.get() == 1, "Cancelled row stays terminal and recreation never reconnects");
            });
            memory.blocked = false;
            await(() -> mainValue(() -> model.state().pendingPrintCount() == 0), "Remaining FIFO requests reach their own real completion");
            onMain(() -> {
                check(model.state().printRequests.get(0).result.raw.code == 0 && model.state().printRequests.get(1).result.raw.code == 1005 &&
                    model.state().printRequests.get(2).result.raw.code == 0, "Only the requested middle entry is cancelled");
                for (DemoUiState.PrintRequest request : model.state().printRequests)
                    check(request.result.raw.requestId == request.requestId, "Each visible row retains its own result identity");
            });
            progress("same-page pixel options");
            for (int option = 2; option <= 4; option++) {
                final int selected = option;
                onMain(() -> select(current, R.id.pixel_example, selected)); instrumentation.waitForIdleSync();
                onMain(() -> {
                    check(current.findViewById(R.id.print).isEnabled(), "Every pixel option is printable on the original page");
                    current.findViewById(R.id.print).performClick();
                });
                await(() -> mainValue(() -> model.state().pendingPrintCount() == 0), "Pixel option completes through the public print path");
            }
            onMain(() -> select(current, R.id.pixel_example, 0)); instrumentation.waitForIdleSync();
            onMain(() -> check(owner.images.source() == source && current.findViewById(R.id.preview).isEnabled(),
                "Returning to current image preserves the original source and optional preview"));
            progress("detached terminal journal catch-up");
            await(() -> mainValue(() -> printLogCount(owner) == 6), "All visible per-request results reach the journal");
            Runnable listener = mainValue(() -> (Runnable)field(current, "listener"));
            onMain(() -> {
                owner.stopUi(listener, true);
                model.printBatch(null, new LabelConfig(203, 256, 256, 128, 1, 2), DemoPixelSamples.Example.RGB);
            });
            await(() -> mainValue(() -> model.state().pendingPrintCount() == 0), "Several requests can finish while UI listeners are detached");
            onMain(() -> {
                check(printLogCount(owner) == 6, "Detached UI does not consume terminal journal events");
                owner.attach(listener); model.attach(listener); owner.images.attach(listener);
            });
            await(() -> mainValue(() -> printLogCount(owner) == 9), "Reattachment journals every missed terminal, not just the latest result");
            onMain(() -> {
                owner.recordResults(); owner.recordResults();
                check(((java.util.Map<?, ?>)field(owner, "recordedPrints")).size() == 9,
                    "Repeated rendering keeps a bounded identity record without duplicate print logs");
            });
            progress("picker cleanup barrier and explicit reconnect requirement");
            IntentFilter imageFilter = new IntentFilter(Intent.ACTION_OPEN_DOCUMENT);
            imageFilter.addCategory(Intent.CATEGORY_OPENABLE); imageFilter.addDataType("image/*");
            Instrumentation.ActivityMonitor imagePicker = instrumentation.addMonitor(imageFilter,
                new Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null), true);
            int opensBeforePicker = memory.opens.get();
            try {
                onMain(() -> call(current, "chooseImage"));
                await(() -> imagePicker.getHits() == 1, "Explicit image import opens the picker after disconnect callback");
                onMain(() -> check(model.state().phase == DemoUiState.Phase.OFFLINE && !memory.open &&
                    model.state().cleanupResult.raw.code == 0 && memory.opens.get() == opensBeforePicker &&
                    !current.findViewById(R.id.print).isEnabled() && owner.images.source() == source,
                    "Picker cancellation retains the image and remains disconnected without replay or automatic reconnect"));
            } finally { instrumentation.removeMonitor(imagePicker); }
        } finally {
            memory.blocked = false;
            onMain(() -> { MainActivity current = screen.get(); if (!current.isFinishing()) current.finish(); });
            CountDownLatch closed = new CountDownLatch(1); manager.close(result -> closed.countDown());
            check(closed.await(10, TimeUnit.SECONDS), "Same-page request test closes native resources");
        }
    }

    private static int printLogCount(DemoOwner owner) {
        int count = 0;
        for (String line : owner.lines) if (line.contains("PRINT outcome=")) count++;
        return count;
    }

    private static Button requestCancel(Activity activity, RequestId id) {
        LinearLayout rows = activity.findViewById(R.id.print_requests);
        for (int row = 0; row < rows.getChildCount(); row++) {
            LinearLayout entry = (LinearLayout)rows.getChildAt(row);
            for (int column = 0; column < entry.getChildCount(); column++)
                if (entry.getChildAt(column) instanceof Button && entry.getChildAt(column).getTag() == id)
                    return (Button)entry.getChildAt(column);
        }
        return null;
    }

    private void permissionRules() throws Exception {
        expectPermissions(22, "SPP_CONNECT"); expectPermissions(22, "BLE_DISCOVERY");
        for (int api : new int[]{23, 30}) {
            expectPermissions(api, "SPP_CONNECT"); expectPermissions(api, "BLE_CONNECT");
            expectPermissions(api, "SPP_DISCOVERY");
            expectPermissions(api, "BLE_DISCOVERY", Manifest.permission.ACCESS_FINE_LOCATION);
        }
        for (int api : new int[]{31, 35, 36}) {
            expectPermissions(api, "SPP_CONNECT", Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN);
            expectPermissions(api, "BLE_CONNECT", Manifest.permission.BLUETOOTH_CONNECT);
            expectPermissions(api, "SPP_DISCOVERY", Manifest.permission.BLUETOOTH_CONNECT);
            expectPermissions(api, "BLE_DISCOVERY", Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN);
        }
    }

    private static void expectPermissions(int api, String operation, String... expected) throws Exception {
        Class<?> rules = Class.forName("com.netumscan.printersdk.demo.DemoPermissions");
        Class<?> operations = Class.forName("com.netumscan.printersdk.demo.DemoPermissions$Operation");
        Object selected = null;
        for (Object value : operations.getEnumConstants()) if (value.toString().equals(operation)) selected = value;
        check(selected != null, "Permission rule exposes " + operation);
        Method required = rules.getDeclaredMethod("required", int.class, operations); required.setAccessible(true);
        String[] actual = (String[])required.invoke(null, api, selected);
        check(Arrays.equals(actual, expected), "API " + api + " " + operation + " permissions: " + Arrays.toString(actual));
    }

    private void invalidMediaKeepsPreviousResult() throws Exception {
        MainActivity activity = launch();
        DemoOwner owner = mainValue(() -> (DemoOwner)field(activity, "owner"));
        DemoModel model = owner.model;
        try {
            onMain(() -> {
                restorePrintDraft(activity, "203", "864");
                select(activity, R.id.media, 0); select(activity, R.id.form, 1);
                seedSuccessfulGap(owner);
                model.inputError(null);
                check(text(activity, R.id.media_status).contains("间隙纸") && text(activity, R.id.media_status).contains("SUCCESS"),
                    "Synthetic previous gap request is shown as successful");
                select(activity, R.id.media, 1);
                // The compatibility mirror is deliberately not an input or SDK configuration source.
                set(activity, R.id.dpi, "");
                check(activity.findViewById(R.id.apply_media).isEnabled(), "Media action can report its own input validation");
                try { model.setMedia(null); throw new AssertionError("Null medium accepted"); }
                catch (IllegalArgumentException expected) { model.inputError(expected.getMessage()); }
                String status = text(activity, R.id.media_status);
                check(status.contains("间隙纸") && status.contains("SUCCESS") && status.contains("尚未发送"),
                    "Invalid black-mark input keeps the previous gap success and marks the current choice unsent: " + status);
                check(!activity.findViewById(R.id.dpi).isShown() && model.printProfile().dpi == 203,
                    "Hidden legacy DPI drafts cannot edit the connection Profile or invalidate prior media evidence");
                check(field(model, "active") == null && field(model, "connection") == null,
                    "Invalid media input submits no Manager request or Bluetooth connection");
            });
        } finally {
            onMain(() -> {
                setField(model, "connected", false); setField(model, "configured", false);
                setField(model, "printProfile", null); activity.finish();
            });
            instrumentation.waitForIdleSync();
        }
    }

    private static void seedSuccessfulGap(DemoOwner owner) throws Exception {
        Constructor<RequestId> id = RequestId.class.getDeclaredConstructor(Object.class, long.class); id.setAccessible(true);
        RequestId connection = id.newInstance(new Object(), 1L), request = id.newInstance(new Object(), 2L);
        Constructor<?> result = ManagerResult.class.getDeclaredConstructor(RequestId.class, ManagerResult.Outcome.class,
            int.class, String.class, ManagerResult.Completion.class, boolean.class, Object.class);
        result.setAccessible(true);
        @SuppressWarnings("unchecked") ManagerResult<Void> success = (ManagerResult<Void>)result.newInstance(request,
            ManagerResult.Outcome.SUCCESS, 0, "Synthetic previous gap request", ManagerResult.Completion.DATA_SENT, true, null);
        DemoResult<Void> previous = new DemoResult<>(DemoResult.Kind.MEDIA, success);
        setField(owner.model, "connected", true); setField(owner.model, "configured", true);
        setField(owner.model, "printProfile", PrinterProfile.cpcl(203, 864));
        setField(owner.model, "lastRequest", previous);
        try {
            // The same behavioral check can target the baseline APK before state ownership was fixed.
            Class<?> type = Class.forName("com.netumscan.printersdk.demo.DemoUiState$MediaRequest");
            Constructor<?> media = type.getDeclaredConstructor(MediaMode.class, RequestId.class, RequestId.class, DemoResult.class);
            media.setAccessible(true);
            setField(owner.model, "mediaRequest", media.newInstance(MediaMode.GAP, connection, request, previous));
        } catch (ClassNotFoundException baseline) {
            setField(owner, "mediaConnection", field(owner, "connectionNumber"));
            setField(owner, "mediaMode", 0); setField(owner, "mediaResult", previous);
        }
    }

    private void exportPreparationSurvivesRecreation() throws Exception {
        MainActivity original = launch();
        AtomicReference<MainActivity> screen = new AtomicReference<>(original);
        DemoOwner owner = mainValue(() -> (DemoOwner)field(original, "owner"));
        ExecutorService journal = (ExecutorService)field(owner.journal, "worker");
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        IntentFilter filter = new IntentFilter(Intent.ACTION_CREATE_DOCUMENT);
        filter.addCategory(Intent.CATEGORY_OPENABLE); filter.addDataType("text/plain");
        Instrumentation.ActivityMonitor picker = instrumentation.addMonitor(filter,
            new Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null), true);
        try {
            journal.execute(() -> {
                entered.countDown();
                try { if (!release.await(30, TimeUnit.SECONDS)) throw new AssertionError("Journal blocker timed out"); }
                catch (InterruptedException failure) { Thread.currentThread().interrupt(); throw new AssertionError(failure); }
            });
            check(entered.await(10, TimeUnit.SECONDS), "Journal lane is deterministically blocked before the export snapshot");
            onMain(() -> {
                original.findViewById(R.id.logs_button).performClick();
                check(original.findViewById(R.id.export).isEnabled(), "Actual log export control is available");
                original.findViewById(R.id.export).performClick();
                check(owner.utilityBusy && picker.getHits() == 0,
                    "Export waits for its snapshot before opening the system destination picker");
            });
            MainActivity current = recreate(original); screen.set(current);
            onMain(() -> check(field(current, "owner") == owner && owner.utilityBusy,
                "Real Activity recreation retains the in-flight export owner"));
            release.countDown();
            await(() -> picker.getHits() > 0, "Recreated Activity opens the requested export destination after the snapshot completes");
            instrumentation.waitForIdleSync();
            onMain(() -> {
                Runnable listener = (Runnable)field(current, "listener"); listener.run(); listener.run();
                check(picker.getHits() == 1, "The retained export intent is consumed exactly once");
                check(field(owner, "exportText") == null && field(owner, "exportUri") == null,
                    "Synthetic picker cancellation clears the snapshot and target URI");
                check(owner.model.state().phase == DemoUiState.Phase.OFFLINE && field(owner.model, "connection") == null,
                    "Export recreation performs no Bluetooth connection or printer IO");
            });
        } finally {
            release.countDown(); instrumentation.removeMonitor(picker);
            onMain(() -> screen.get().finish()); instrumentation.waitForIdleSync();
        }
    }

    /** Pre-connection presets configure transport only, without a print Profile acceptance path. */
    private void transportPresetOnly() throws Exception {
        progress("transport preset is independent from print Profile");
        MainActivity activity = launch();
        DemoOwner owner = mainValue(() -> (DemoOwner)field(activity, "owner"));
        try {
            onMain(() -> {
                activity.findViewById(R.id.device_advanced_toggle).performClick();
                activity.findViewById(R.id.transport_details_toggle).performClick();
                select(activity, R.id.transport, 1);
                restorePrintDraft(activity, "300", "576");
                set(activity, R.id.label_width, "384"); set(activity, R.id.height, "777");
                select(activity, R.id.form, 2);
            });
            instrumentation.waitForIdleSync();
            chooseCandidate(activity, owner.model, "LT-M90_aB12");
            onMain(() -> {
                String address = text(activity, R.id.address);
                call(activity, "applyPreset");
                check(owner.m90BleExplicit && text(activity, R.id.mtu).equals("517") &&
                    text(activity, R.id.payload).equals("512"), "M90 preset explicitly supplies BLE transport parameters");
                check(text(activity, R.id.dpi).equals("300") && text(activity, R.id.width).equals("576") &&
                    text(activity, R.id.label_width).equals("384") && text(activity, R.id.height).equals("777") &&
                    text(activity, R.id.address).equals(address) && choice(activity, R.id.form) == 2,
                    "Transport preset preserves print drafts, media choices and the selected device");
                check(!activity.findViewById(R.id.dpi).isShown() && !activity.findViewById(R.id.width).isShown() &&
                    !activity.findViewById(R.id.print_format).isShown(), "No pre-connection print Profile controls are displayed");
                set(activity, R.id.dpi, "1"); set(activity, R.id.width, "8");
                select(activity, R.id.print_format, 1); select(activity, R.id.origin_mode, OriginPlacement.CENTERED);
                LabelConfig local = (LabelConfig)call(activity, "readConfig");
                DemoSettings saved = (DemoSettings)call(activity, "readSettings");
                check(local.dpi == 300 && local.printerWidthDots == 576 && !local.receipt &&
                    saved.values.get("dpi").equals(300) && saved.values.get("width").equals(576) &&
                    saved.values.get("print_format").equals(0) && saved.values.get("origin_mode").equals(OriginPlacement.MANUAL),
                    "Hidden compatibility mirrors cannot become a second configuration authority for preview or saving");
                check(!activity.findViewById(R.id.dpi).isSaveEnabled() && !activity.findViewById(R.id.width).isSaveEnabled() &&
                    !activity.findViewById(R.id.print_format).isSaveEnabled(),
                    "Framework view restoration cannot revive stale mirrored device geometry");
                select(activity, R.id.ble_profile, 1);
            });
            instrumentation.waitForIdleSync();
            onMain(() -> {
                select(activity, R.id.write_mode, 1);
                set(activity, R.id.service, "000018f0-0000-1000-8000-00805f9b34fb");
                set(activity, R.id.characteristic, "00002af1-0000-1000-8000-00805f9b34fb");
                set(activity, R.id.mtu, "185"); set(activity, R.id.payload, "100");
                ConnectionOptions options = (ConnectionOptions)call(activity, "connectionOptions");
                BleOptions ble = options.ble;
                check(ble.service.equals(UUID.fromString("000018f0-0000-1000-8000-00805f9b34fb")) &&
                    ble.writeCharacteristic.equals(UUID.fromString("00002af1-0000-1000-8000-00805f9b34fb")) &&
                    ble.withResponse && ble.requestedMtu == 185 && ble.maxWriteBytes == 100 && ble.creditFlowControl == null,
                    "Generic BLE UUID, write mode and packet parameters remain explicitly configurable");
                check(owner.model.state().phase == DemoUiState.Phase.OFFLINE && field(owner.model, "active") == null &&
                    field(owner.model, "connection") == null, "Local preset and transport editing submit no connection or printer IO");
            });
            chooseCandidate(activity, owner.model, "Synthetic unknown BLE printer");
            onMain(() -> {
                ConnectionOptions unknown = (ConnectionOptions)call(activity, "connectionOptions");
                check(unknown.ble.withResponse && unknown.ble.requestedMtu == 185 &&
                    "Synthetic unknown BLE printer".equals(unknown.deviceName),
                    "Explicit generic BLE parameters permit unknown-name transport without requiring an automatic recommendation");
            });
        } finally { onMain(activity::finish); instrumentation.waitForIdleSync(); }
    }

    /** Legacy draft geometry cannot override automatic matching or leak to an unknown next device. */
    private void profileDraftDoesNotConfigureConnection() throws Exception {
        progress("two Profile paths, draft migration and post-connection correction");
        MainActivity original = launch();
        AtomicReference<MainActivity> screen = new AtomicReference<>(original);
        DemoOwner owner = mainValue(() -> (DemoOwner)field(original, "owner"));
        DemoModel model = owner.model;
        DemoQueueChecks.Memory memory = new DemoQueueChecks.Memory();
        PrinterManager manager = DemoQueueChecks.manager(instrumentation.getTargetContext(), memory);
        AtomicReference<Bundle> beforeConnection = new AtomicReference<>();
        try {
            PrinterManager initial = mainValue(() -> (PrinterManager)field(model, "manager"));
            CountDownLatch initialClosed = new CountDownLatch(1); initial.close(result -> initialClosed.countDown());
            check(initialClosed.await(10, TimeUnit.SECONDS), "Offline Manager closes before two-path injection");
            onMain(() -> {
                setField(model, "manager", manager);
                Bundle draft = new Bundle(); draft.putString("dpi", "300"); draft.putString("width", "576");
                draft.putInt("print_format", 1); draft.putBoolean("profile_explicit", true);
                restoreDraft(original, draft, false);
                set(original, R.id.label_width, "256"); set(original, R.id.height, "128"); select(original, R.id.form, 2);
                beforeConnection.set(saveViewState(original));
            });
            chooseCandidate(original, model, "LT-M90_AB12");
            onMain(() -> model.connect((ConnectionOptions)call(original, "connectionOptions")));
            await(() -> mainValue(() -> model.state().canWork() && model.configured()), "Known-name connection initializes media");
            onMain(() -> {
                assertNameConnection(model);
                check(model.printProfile().dpi == 203 && model.printProfile().maxPrintWidthDots == 864 &&
                    choice(original, R.id.print_format) == 0, "SDK name matching ignores saved manual geometry and protocol");
                restoreViewState(original, beforeConnection.get());
            });
            instrumentation.waitForIdleSync();
            onMain(() -> check(text(original, R.id.dpi).equals("203") && text(original, R.id.width).equals("864") &&
                choice(original, R.id.print_format) == 0, "Current connection wins over stale framework view restoration"));
            MainActivity current = recreate(original); screen.set(current);
            ConnectionInfo recommended = mainValue(() -> model.state().connectionInfo);
            DemoUiState.MediaRequest oldMedia = mainValue(() -> model.state().mediaRequest);
            onMain(() -> {
                check(field(current, "owner") == owner && model.state().connectionInfo == recommended,
                    "Real Activity recreation preserves the adopted connection configuration");
                current.findViewById(R.id.device_advanced_toggle).performClick();
                current.findViewById(R.id.apply_print_profile).performClick();
            });
            instrumentation.waitForIdleSync();
            onMain(() -> acceptProfileDialog(current, 0, "300", "576", "", PrinterProfile.LabelAlignment.LEFT, memory));
            await(() -> mainValue(() -> model.state().mediaRequest != null && model.state().mediaRequest != oldMedia &&
                model.state().mediaRequest.result == null), "Corrected known Profile invalidates prior media evidence");
            onMain(() -> check(!model.configured() && !current.findViewById(R.id.print).isEnabled() &&
                model.printProfile().dpi == 300 && model.printProfile().dotsPerMm == 300 / 25.4 &&
                model.printProfile().labelAlignment == PrinterProfile.LabelAlignment.LEFT &&
                model.state().connectionInfo.configurationSource == ConnectionInfo.ConfigurationSource.EXPLICIT &&
                recommended.configurationSource == ConnectionInfo.ConfigurationSource.NAME_RECOMMENDATION && recommended.printProfile.dpi == 203,
                "Post-connection correction waits for independent media initialization and preserves old immutable snapshots"));
            memory.blocked = false;
            await(() -> mainValue(() -> model.state().canWork() && model.configured()), "Corrected Profile media completes");
            onMain(model::disconnect);
            await(() -> mainValue(() -> model.state().canConnect()), "Known-device cleanup completes");
            int beforeUnknown = memory.size();
            onMain(() -> {
                set(current, R.id.address, "AA:BB:CC:DD:EE:99");
                model.connect((ConnectionOptions)call(current, "connectionOptions"));
            });
            await(() -> mainValue(() -> model.state().canWork()), "Unknown manual address connects");
            onMain(() -> {
                check(model.state().connectionInfo.printProfile == null &&
                    model.state().connectionInfo.configurationSource == ConnectionInfo.ConfigurationSource.NONE &&
                    !model.configured() && memory.size() == beforeUnknown,
                    "Unknown connection never inherits the previous manual draft or sends media automatically");
                current.findViewById(R.id.apply_print_profile).performClick();
            });
            instrumentation.waitForIdleSync();
            onMain(() -> {
                AlertDialog dialog = (AlertDialog)field(current, "utilityDialog");
                java.util.List<EditText> inputs = new java.util.ArrayList<>();
                collectViews(dialog.getWindow().getDecorView(), EditText.class, inputs);
                check(inputs.size() == 3 && inputs.get(0).getText().toString().equals("300") &&
                    inputs.get(1).getText().toString().equals("576"), "Only explicit post-connection editing offers the saved draft");
                acceptProfileDialog(current, 0, "300", "576", "", PrinterProfile.LabelAlignment.LEFT, memory);
            });
            await(() -> mainValue(() -> model.state().mediaRequest != null && model.state().mediaRequest.result == null),
                "Unknown-device explicit configuration requests fresh media initialization");
            memory.blocked = false;
            await(() -> mainValue(() -> model.state().canWork() && model.configured()), "Unknown-device manual configuration becomes ready");
            onMain(() -> check(memory.opens.get() == 2 && model.state().printRequests.isEmpty() &&
                model.state().connectionInfo.configurationSource == ConnectionInfo.ConfigurationSource.EXPLICIT,
                "Only two deliberate connections occur; configuring or restoring never prints automatically"));
        } finally {
            memory.blocked = false;
            onMain(() -> { if (!screen.get().isFinishing()) screen.get().finish(); });
            CountDownLatch closed = new CountDownLatch(1); manager.close(result -> closed.countDown());
            check(closed.await(10, TimeUnit.SECONDS), "Two-path test releases its native memory device");
        }
    }
    private static void acceptProfileDialog(MainActivity activity, int protocol, String dpi, String width,
            String dotsPerMm, PrinterProfile.LabelAlignment alignment, DemoQueueChecks.Memory memory) throws Exception {
        AlertDialog dialog = (AlertDialog)field(activity, "utilityDialog");
        check(dialog != null && dialog.isShowing(), "Post-connection Profile dialog is visible");
        java.util.List<EditText> inputs = new java.util.ArrayList<>();
        java.util.List<Spinner> protocols = new java.util.ArrayList<>();
        collectViews(dialog.getWindow().getDecorView(), EditText.class, inputs);
        collectViews(dialog.getWindow().getDecorView(), Spinner.class, protocols);
        check(inputs.size() == 3 && protocols.size() == 2,
            "Configuration dialog includes precise density and the Profile placement rule");
        protocols.get(0).setSelection(protocol); inputs.get(0).setText(dpi); inputs.get(1).setText(width);
        inputs.get(2).setText(dotsPerMm);
        protocols.get(1).setSelection(alignment == PrinterProfile.LabelAlignment.CENTERED ? 1 : 0);
        memory.blocked = true; dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
    }
    private static void assertNameConnection(DemoModel model) {
        ConnectionInfo connection = null;
        for (DemoResult<?> result : model.resultHistory())
            if (result.kind == DemoResult.Kind.CONNECT && result.raw.outcome == ManagerResult.Outcome.SUCCESS)
                connection = (ConnectionInfo)result.raw.value;
        check(connection != null && connection.configurationSource == ConnectionInfo.ConfigurationSource.NAME_RECOMMENDATION &&
            connection.printProfile != null && "LT-M90".equals(connection.printProfile.modelId) &&
            connection.printProfile.dpi == 203 && connection.printProfile.maxPrintWidthDots == 864,
            "Actual connection result retains LT-M90 identity and name recommendation source");
    }

    /** Restoring either historical priority marker only restores an unadopted manual draft. */
    private void profileDraftRestoration() throws Exception {
        progress("legacy Profile drafts never reconnect or configure automatically");
        for (int marker = 0; marker < 3; marker++) {
            MainActivity original = launch();
            AtomicReference<MainActivity> screen = new AtomicReference<>(original);
            DemoOwner owner = mainValue(() -> (DemoOwner)field(original, "owner"));
            final int value = marker;
            try {
                onMain(() -> {
                    Bundle draft = new Bundle(); draft.putString("dpi", "300"); draft.putString("width", "576");
                    if (value < 2) draft.putBoolean("profile_explicit", value == 1);
                    restoreDraft(original, draft, false);
                    check(text(original, R.id.dpi).equals("300") && text(original, R.id.width).equals("576"),
                        "Old manual geometry remains available as a draft");
                });
                MainActivity current = recreate(original); screen.set(current);
                onMain(() -> {
                    check(field(current, "owner") == owner && text(current, R.id.dpi).equals("300") &&
                        text(current, R.id.width).equals("576"), "Draft survives real Activity recreation");
                    set(current, R.id.address, "AA:BB:CC:DD:EE:99");
                    ConnectionOptions options = (ConnectionOptions)call(current, "connectionOptions");
                    check(options.deviceName == null && owner.model.state().connectionInfo == null &&
                        owner.model.state().phase == DemoUiState.Phase.OFFLINE && owner.model.state().pendingPrintCount() == 0,
                        "Legacy priority metadata cannot configure or connect an unknown address");
                    Bundle saved = saveViewState(current).getBundle("inputs");
                    check(!saved.containsKey("profile_explicit"), "New drafts remove the obsolete pre-connection priority marker");
                });
                Bundle inputs = mainValue(() -> saveViewState(current).getBundle("inputs"));
                onMain(current::finish); instrumentation.waitForIdleSync();
                MainActivity fresh = launch(); screen.set(fresh);
                onMain(() -> {
                    restoreDraft(fresh, inputs, false); call(fresh, "render");
                    DemoOwner restoredOwner = (DemoOwner)field(fresh, "owner");
                    check(restoredOwner != owner && restoredOwner.model.state().phase == DemoUiState.Phase.OFFLINE &&
                        restoredOwner.model.printProfile() == null && restoredOwner.model.state().pendingPrintCount() == 0 &&
                        text(fresh, R.id.dpi).equals("300") && text(fresh, R.id.width).equals("576") &&
                        text(fresh, R.id.address).isEmpty(),
                        "A fresh owner restores only the typed draft, with no adopted Profile, device identity or request replay");
                });
            } finally { onMain(() -> screen.get().finish()); instrumentation.waitForIdleSync(); }
        }
    }
    private static void restoreDraft(MainActivity activity, Bundle draft, boolean retained) throws Exception {
        Method restore = MainActivity.class.getDeclaredMethod("restoreDraft", Bundle.class, boolean.class);
        restore.setAccessible(true); restore.invoke(activity, draft, retained);
    }
    private static void restorePrintDraft(MainActivity activity, String dpi, String width) throws Exception {
        Bundle draft = new Bundle(); draft.putString("dpi", dpi); draft.putString("width", width);
        restoreDraft(activity, draft, false);
    }
    private static Bundle saveViewState(MainActivity activity) throws Exception {
        Bundle saved = new Bundle();
        Method save = MainActivity.class.getDeclaredMethod("onSaveInstanceState", Bundle.class);
        save.setAccessible(true); save.invoke(activity, saved); return saved;
    }
    private static void restoreViewState(MainActivity activity, Bundle saved) throws Exception {
        Method restore = Activity.class.getDeclaredMethod("onRestoreInstanceState", Bundle.class);
        restore.setAccessible(true); restore.invoke(activity, saved);
    }
    private void chooseCandidate(MainActivity activity, DemoModel model, String name) throws Exception {
        DiscoveredDevice candidate = candidate(name);
        onMain(() -> {
            setField(model, "candidates", Collections.singletonList(candidate)); model.inputError(null);
            call(activity, "showDevices");
        });
        instrumentation.waitForIdleSync();
        onMain(() -> {
            AlertDialog dialog = (AlertDialog)field(activity, "devicePicker");
            check(dialog != null && dialog.isShowing(), "Synthetic candidate uses the actual selection dialog");
            ListView list = dialog.getListView(); list.performItemClick(list.getChildAt(0), 0, list.getAdapter().getItemId(0));
            check(candidate.address.equals(model.state().selectedID) && candidate.address.equals(text(activity, R.id.address)),
                "Explicit selection retains the original candidate name and address");
        });
    }

    private static DiscoveredDevice candidate(String name) throws Exception {
        Class<?> type = Class.forName("com.netumscan.printersdk.BluetoothDiscovery$Device");
        Constructor<?> source = type.getDeclaredConstructor(String.class, String.class, java.util.List.class, Integer.class);
        source.setAccessible(true);
        Constructor<DiscoveredDevice> result = DiscoveredDevice.class.getDeclaredConstructor(type); result.setAccessible(true);
        return result.newInstance(source.newInstance("AA:BB:CC:DD:EE:02", name, Collections.emptyList(), null));
    }

    private MainActivity launch() {
        MainActivity activity = (MainActivity)instrumentation.startActivitySync(new Intent(instrumentation.getTargetContext(), MainActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        instrumentation.runOnMainSync(() -> activity.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
        instrumentation.waitForIdleSync(); return activity;
    }

    private MainActivity recreate(MainActivity original) throws Exception {
        CountDownLatch resumed = new CountDownLatch(1);
        AtomicReference<MainActivity> replacement = new AtomicReference<>();
        Application application = original.getApplication();
        Application.ActivityLifecycleCallbacks observer = new Application.ActivityLifecycleCallbacks() {
            public void onActivityCreated(Activity activity, Bundle saved) {
                if (activity instanceof MainActivity && activity != original) replacement.set((MainActivity)activity);
            }
            public void onActivityResumed(Activity activity) { if (activity == replacement.get()) resumed.countDown(); }
            public void onActivityStarted(Activity activity) { }
            public void onActivityPaused(Activity activity) { }
            public void onActivityStopped(Activity activity) { }
            public void onActivitySaveInstanceState(Activity activity, Bundle saved) { }
            public void onActivityDestroyed(Activity activity) { }
        };
        onMain(() -> { application.registerActivityLifecycleCallbacks(observer); original.recreate(); });
        try { check(resumed.await(10, TimeUnit.SECONDS), "Replacement Activity resumed after real recreate"); }
        finally { onMain(() -> application.unregisterActivityLifecycleCallbacks(observer)); }
        instrumentation.waitForIdleSync();
        MainActivity current = replacement.get();
        check(current != null && current != original, "Recreation created a distinct Activity"); return current;
    }

    private static Object field(Object object, String name) throws Exception {
        Field field = object.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(object);
    }
    private static void setField(Object object, String name, Object value) throws Exception {
        Field field = object.getClass().getDeclaredField(name); field.setAccessible(true); field.set(object, value);
    }
    private static Object call(Object object, String name) throws Exception {
        Method method = object.getClass().getDeclaredMethod(name); method.setAccessible(true); return method.invoke(object);
    }
    private static void set(Activity activity, int id, String value) { ((EditText)activity.findViewById(id)).setText(value); }
    private static String text(Activity activity, int id) { return ((TextView)activity.findViewById(id)).getText().toString(); }
    private static int choice(Activity activity, int id) { return ((Spinner)activity.findViewById(id)).getSelectedItemPosition(); }
    private static void select(Activity activity, int id, int value) { ((Spinner)activity.findViewById(id)).setSelection(value); }
    private interface Work { void run() throws Exception; }
    private void onMain(Work work) {
        AtomicReference<Throwable> error = new AtomicReference<>();
        instrumentation.runOnMainSync(() -> { try { work.run(); } catch (Throwable failure) { error.set(failure); } });
        if (error.get() != null) throw new AssertionError("Main-thread Full Demo regression", error.get());
    }
    private <T> T mainValue(Callable<T> work) {
        AtomicReference<T> value = new AtomicReference<>(); onMain(() -> value.set(work.call())); return value.get();
    }
    private interface Condition { boolean ready() throws Exception; }
    private static void await(Condition condition, String message) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (!condition.ready() && System.nanoTime() < deadline) Thread.sleep(10);
        check(condition.ready(), message);
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
