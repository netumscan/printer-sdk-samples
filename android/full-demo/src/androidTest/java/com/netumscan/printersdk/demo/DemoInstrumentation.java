// SPDX-License-Identifier: MIT
package com.netumscan.printersdk.demo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.AtomicFile;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import com.netumscan.printersdk.ConnectionInfo;
import com.netumscan.printersdk.DiscoveredDevice;
import com.netumscan.printersdk.ManagerResult;
import com.netumscan.printersdk.MediaMode;
import com.netumscan.printersdk.PrinterProfile;
import com.netumscan.printersdk.RequestId;
import com.netumscan.printersdk.SdkInfo;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Real Full Demo controls/recreation with synthetic device data; no Bluetooth or printer IO. */
public final class DemoInstrumentation extends Instrumentation {
    private String regression;
    @Override public void onCreate(Bundle arguments) {
        super.onCreate(arguments); regression = arguments == null ? null : arguments.getString("regression"); start();
    }

    @Override public void onStart() {
        Bundle result = new Bundle();
        AtomicReference<MainActivity> screen = new AtomicReference<>();
        try {
            if (regression != null) {
                DemoRegressionChecks.run(this, regression);
                result.putString("stream", "PASS: Full Demo regression " + regression + "; no Bluetooth or paper output\n");
                finish(Activity.RESULT_OK, result); return;
            }
            progress("existing input and settings checks");
            DemoInputChecks.main(new String[0]);
            connectionInputChecks();
            settingsStoreChecks();
            // Source/preview checks and real Manager printing use synthetic images and a memory transport.
            progress("existing image and Manager checks");
            DemoImageChecks.run(this);
            progress("SDK AndroidImages PNG/JPEG EXIF, resource, alpha and ownership checks");
            com.netumscan.printersdk.AndroidImageAdapterChecks.run(this);
            progress("existing full-page and lifecycle checks");
            MainActivity original = launch(); screen.set(original);
            DemoOwner owner = (DemoOwner)field(original, "owner");
            DemoModel model = owner.model;
            sdkIdentityChecks(owner);
            progress("default Profile rollback and explicit initialization retry");
            DemoModelChecks.profileFailureChecks(getTargetContext());
            DemoModelChecks.centeredProfileChecks(getTargetContext());
            onMain(() -> {
                offline(model, "Initial launch");
                check(original.findViewById(R.id.connect).isEnabled(), "Explicit connect is available");
                check(original.findViewById(R.id.discover).isEnabled(), "SPP paired-device action is available");
                check(choice(original, R.id.transport) == 0, "Initial transport is SPP");
                check(text(original, R.id.dpi).isEmpty() && text(original, R.id.width).isEmpty(), "Device geometry is absent until a connection adopts a Profile");
                check(!original.findViewById(R.id.print).isEnabled() && !original.findViewById(R.id.send_raw).isEnabled(),
                    "Print and Raw require an explicit connection");
                check(!original.findViewById(R.id.cancel).isEnabled(), "No operation starts on launch");
                check(choice(original, R.id.origin_mode) == OriginPlacement.MANUAL &&
                    text(original, R.id.origin_adjustment).equals("0") && !original.findViewById(R.id.origin_mode).isShown(),
                    "Offline launch does not invent a model placement policy and calibration starts at zero");
                restorePrintDraft(original, "203", "864", OriginPlacement.MANUAL);
                set(original, R.id.label_width, "384"); set(original, R.id.height, "512");
                set(original, R.id.copies, "2"); set(original, R.id.x_offset, "8");
                set(original, R.id.origin_adjustment, "4"); set(original, R.id.trailing_blank, "16");
                select(original, R.id.form, 1);
            });
            waitForIdleSync();
            onMain(() -> {
                LabelConfig config = (LabelConfig)call(original, "readConfig");
                check(config.labelWidthDots == 384 && config.height == 512 && config.copies == 2 && config.xOffsetDots == 8 && config.form,
                    "Current XML width/height/copies/manual origin/FORM reach label configuration");
                restorePrintDraft(original, "203", "864", OriginPlacement.CENTERED); select(original, R.id.form, 2);
                select(original, R.id.transport, 1); select(original, R.id.ble_profile, 1);
            });
            waitForIdleSync();
            onMain(() -> {
                set(original, R.id.mtu, "185"); set(original, R.id.payload, "180");
                select(original, R.id.transport, 0);
            });
            waitForIdleSync();
            onMain(() -> {
                LabelConfig config = (LabelConfig)call(original, "readConfig");
                check(config.xOffsetDots == 0 && config.manualOriginDots == 0 && !config.form,
                    "An old centered draft remains a draft offline and its resolved X cannot become a manual origin");
                check(owner.images.state().prepared == null, "The source needs no preview preparation");
                seedPresentationConnection(model, true, false);
                check(((LabelConfig)call(original, "readConfig")).xOffsetDots == 244 &&
                    ((LabelConfig)call(original, "readConfig")).manualOriginDots == 0,
                    "The adopted centered Profile replaces a legacy absolute origin with SDK origin plus calibration");
                check(!original.findViewById(R.id.print).isEnabled(),
                    "Connection and valid geometry do not bypass pending media initialization");
                check(!original.findViewById(R.id.dpi).isEnabled() && !original.findViewById(R.id.width).isEnabled() &&
                    !original.findViewById(R.id.print_format).isEnabled(),
                    "Bound default Profile fields are read-only while connected");
                seedPresentationConnection(model, true, true);
                check(original.findViewById(R.id.print).isEnabled(),
                    "Initialized valid print parameters enable printing without a preview: " + printReadiness(original, owner));
                set(original, R.id.height, "640");
                check(original.findViewById(R.id.print).isEnabled() && owner.images.state().prepared == null,
                    "Changing print dimensions does not require preparing display pixels");
                set(original, R.id.height, "512");
                seedPresentationConnection(model, false, false);
                check(text(original, R.id.raw_copies).equals("2"), "Canvas copies synchronize into Raw copies");
                offline(model, "Editing geometry and protocol options");
            });

            DiscoveredDevice synthetic = candidate();
            onMain(() -> {
                // Seed only discovery presentation. The public SDK discovery suites exercise its producer.
                Field candidates = DemoModel.class.getDeclaredField("candidates"); candidates.setAccessible(true);
                candidates.set(model, Collections.singletonList(synthetic)); model.inputError(null);
                call(original, "showDevices");
                check(model.state().selectedID == null && text(original, R.id.address).isEmpty(),
                    "Showing SPP choices does not adopt an address or connect");
            });
            waitForIdleSync();
            onMain(() -> {
                AlertDialog picker = (AlertDialog)field(original, "devicePicker");
                check(picker != null && picker.isShowing(), "SPP choices appear in the real device dialog");
                ListView list = picker.getListView();
                check(list.getAdapter().getCount() == 1, "Synthetic paired device is selectable");
                list.performItemClick(list.getChildAt(0), 0, list.getAdapter().getItemId(0));
                check(synthetic.address.equals(model.state().selectedID) && synthetic.address.equals(text(original, R.id.address)),
                    "Explicit SPP dialog selection adopts the chosen device address");
                offline(model, "SPP candidate adoption");
            });

            onMain(() -> original.findViewById(R.id.logs_button).performClick());
            waitForIdleSync();
            logViewChecks(original, owner);
            onMain(() -> {
                check(original.findViewById(R.id.logs_page).getVisibility() == View.VISIBLE &&
                    original.findViewById(R.id.print_page).getVisibility() == View.GONE, "Logs navigation displays the real log page");
                original.findViewById(R.id.logs_back).performClick();
                original.findViewById(R.id.more_button).performClick();
                PopupMenu menu = (PopupMenu)field(original, "configMenu");
                check(menu != null && menu.getMenu().performIdentifierAction(R.id.raw_diagnostics, 0), "Actual diagnostics menu opens Raw page");
                check(original.findViewById(R.id.raw_page).getVisibility() == View.VISIBLE, "Raw is a separate navigable page");
                set(original, R.id.raw_hex, "1B 40 0A"); set(original, R.id.raw_copies, "3");
                check(text(original, R.id.copies).equals("3"), "Raw copies synchronize back into canvas copies");
                check(!original.findViewById(R.id.send_raw).isEnabled(), "Offline Raw input cannot send output");
                original.findViewById(R.id.logs_back).performClick();
                offline(model, "Logs and Raw navigation");
                check(original.findViewById(R.id.preview).isEnabled(), "Image preview works before connecting a printer");
                original.findViewById(R.id.preview).performClick();
            });
            await(() -> mainValue(() -> !owner.images.state().busy && field(original, "previewDialog") != null),
                "Explicit preview finishes and displays the full canvas");
            PreparedImage snapshot = mainValue(() -> owner.images.state().prepared);
            onMain(() -> {
                DemoImages.State image = owner.images.state();
                check(snapshot != null && image.preview != null && image.preview.getWidth() == 384 && image.preview.getHeight() == 512,
                    "Real preview contains configured complete image canvas");
                check(image.thumbnail != null && Math.max(image.thumbnail.getWidth(), image.thumbnail.getHeight()) <= 360,
                    "Inline thumbnail remains bounded independently from full preview");
                AlertDialog preview = (AlertDialog)field(original, "previewDialog");
                check(preview.isShowing(), "Full preview dialog is visible");
                preview.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
            });
            await(() -> mainValue(() -> owner.images.state().preview == null), "Dialog dismissal releases the full preview bitmap");
            onMain(() -> {
                check(owner.images.state().preview == null && owner.images.state().prepared == snapshot,
                    "Closing preview releases its bitmap while retaining only the cached display preparation");
                seedPresentationConnection(model, true, false);
                check(!original.findViewById(R.id.print).isEnabled(), "Cached preview does not bypass initialization gating");
                seedPresentationConnection(model, true, true);
                set(original, R.id.height, "640");
                check(!snapshot.matches((LabelConfig)call(original, "readConfig")) && original.findViewById(R.id.print).isEnabled(),
                    "A stale preview cannot disable printing with changed dimensions");
                set(original, R.id.height, "512");
                seedPresentationConnection(model, false, false);
                offline(model, "Image preparation and preview");
                original.findViewById(R.id.more_button).performClick();
                ((PopupMenu)field(original, "configMenu")).getMenu().performIdentifierAction(R.id.raw_diagnostics, 0);
                Bundle saved = new Bundle(); callActivityOnSaveInstanceState(original, saved);
                check(saved.getBundle("inputs") != null && !saved.getBundle("inputs").containsKey("raw_hex"),
                    "Explicit saved inputs exclude Raw commands");
                check(!original.findViewById(R.id.raw_hex).isSaveEnabled(), "View hierarchy also excludes Raw commands");
            });

            MainActivity current = recreate(original); screen.set(current);
            onMain(() -> {
                check(field(current, "owner") == owner && field(current, "model") == model,
                    "Actual Activity.recreate retains the exact owner and Manager model");
                check(owner.images.state().prepared == snapshot, "Actual recreation retains the exact prepared image snapshot");
                DemoUiState state = model.state(); offline(model, "Configuration recreation");
                check(state.cleanupResult == null, "Configuration recreation does not disconnect the retained Manager");
                check(state.candidates.size() == 1 && synthetic.address.equals(state.selectedID), "Paired candidates and explicit selection survive recreation");
                check(synthetic.address.equals(text(current, R.id.address)), "Chosen SPP address input is restored");
                check(text(current, R.id.dpi).equals("203") && text(current, R.id.width).equals("864") &&
                    text(current, R.id.label_width).equals("384") && text(current, R.id.height).equals("512") &&
                    text(current, R.id.copies).equals("3") && text(current, R.id.x_offset).equals("244") &&
                    text(current, R.id.origin_adjustment).equals("4") && text(current, R.id.trailing_blank).equals("16"),
                    "Full geometry form survives recreation");
                check(choice(current, R.id.origin_mode) == OriginPlacement.CENTERED && choice(current, R.id.form) == 2 &&
                    choice(current, R.id.transport) == 0 && choice(current, R.id.ble_profile) == 1 &&
                    text(current, R.id.mtu).equals("185") && text(current, R.id.payload).equals("180"),
                    "Origin/FORM and explicit BLE overrides survive recreation");
                check(current.findViewById(R.id.raw_page).getVisibility() == View.VISIBLE && text(current, R.id.raw_hex).isEmpty(),
                    "Raw navigation state is restored while Raw command bytes are discarded");
                check(text(current, R.id.raw_copies).equals("3"), "Raw copies reflect retained form copies");
                Runnable listener = (Runnable)field(current, "listener"); listener.run(); listener.run();
                check(model.state() == state, "Repeated rendering consumes no SDK state and submits no work");
                current.findViewById(R.id.logs_back).performClick();
                check(!current.findViewById(R.id.print).isEnabled(), "Retained image still needs an explicit printer connection");
            });
            pendingActivityResults(current, owner, model);
            importedActivityLifecycle(current, owner, model, screen);
            MainActivity fresh = launch(); screen.set(fresh);
            onMain(() -> {
                DemoOwner newOwner = (DemoOwner)field(fresh, "owner");
                check(newOwner != owner && newOwner.model != model, "Fresh Activity launch creates a fresh runtime owner");
                offline(newOwner.model, "Fresh runtime owner");
                check(newOwner.model.state().candidates.isEmpty() && newOwner.model.state().selectedID == null &&
                    newOwner.images.state().prepared == null && !newOwner.images.state().imported,
                    "Fresh runtime owner restores no connection, candidates, image snapshot or source");
                check(text(fresh, R.id.address).isEmpty() && text(fresh, R.id.raw_hex).isEmpty(),
                    "Fresh launch restores no device address or Raw command");
                fresh.finish();
            });
            waitForIdleSync(); screen.set(null);
            progress("focused regressions and print queue checks");
            DemoRegressionChecks.run(this);
            result.putString("stream", "PASS: Full Demo image source/import/EXIF/limits/reading leases, real Manager file/built-in printing and resized printing through memory transport without preview, same-page RGB/RGBA/dithering/multiple images/blank segments, FIFO and per-request cancellation, pending/history budgets, multi-request recreation and terminal journal catch-up, connection UUID inputs, Android AtomicFile/JsonReader schema3, SPP device picker, geometry/FORM/Raw controls, log history/details/latest scrolling, optional preview, deferred import/export results, imported-source recreation, real Home/background cleanup and foreground return without reconnect/replay, final source/worker release, Raw exclusion and fresh-owner offline state; no Bluetooth or paper output\n");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable failure) {
            MainActivity activity = screen.get();
            if (activity != null) runOnMainSync(activity::finish);
            result.putString("stream", "FAIL: Android Full Demo / " + android.util.Log.getStackTraceString(failure));
            finish(Activity.RESULT_CANCELED, result);
        }
    }

    private static void restorePrintDraft(MainActivity activity, String dpi, String width, int mode) throws Exception {
        Bundle draft = new Bundle(); draft.putString("dpi", dpi); draft.putString("width", width);
        draft.putInt("origin_mode", mode);
        Method restore = MainActivity.class.getDeclaredMethod("restoreDraft", Bundle.class, boolean.class);
        restore.setAccessible(true); restore.invoke(activity, draft, false);
    }

    private void progress(String stage) {
        Bundle status = new Bundle(); status.putString("stream", "CHECK: " + stage + "\n");
        sendStatus(0, status);
    }

    private void sdkIdentityChecks(DemoOwner owner) throws Exception {
        await(() -> mainValue(() -> owner.model.state().identityResult != null), "SDK identity callback completes");
        SdkInfo info = mainValue(() -> {
            DemoResult<SdkInfo> result = owner.model.state().identityResult;
            check(result.raw.outcome == ManagerResult.Outcome.SUCCESS && result.raw.value != null,
                "Runtime SDK identity query succeeds");
            return result.raw.value;
        });
        String installed = getTargetContext().getPackageManager()
            .getPackageInfo(getTargetContext().getPackageName(), 0).versionName;
        check(installed.equals(info.distributionVersion),
            "Loaded native distribution matches the target APK version: " + info.distributionVersion + " / " + installed);
        check("0.3.0".equals(info.nativeVersion) && info.abiVersion == 3 && info.abiRevision == 2,
            "Loaded native SDK reports the expected 0.3.0 ABI 3/r2 contract");
        progress("runtime identity distribution=" + info.distributionVersion + " native=" + info.nativeVersion +
            " ABI=" + info.abiVersion + "/r" + info.abiRevision + " APK=" + installed);
    }

    private static void connectionInputChecks() {
        check(MainActivity.parseUuid(" 0000fF00-0000-1000-8000-00805F9B34FB ").equals(
            java.util.UUID.fromString("0000FF00-0000-1000-8000-00805F9B34FB")),
            "Actual BLE UUID input accepts complete mixed-case values and edge whitespace");
        for (String invalid : new String[]{"1-1-1-1-1", "FF00", "0000FF00-0000-1000-8000-00805F9B34FG"}) {
            try { MainActivity.parseUuid(invalid); throw new AssertionError("Incomplete/non-hex BLE UUID accepted"); }
            catch (IllegalArgumentException expected) { }
        }
    }

    /** Exercise the real log controls using in-memory records; never read, append or clear the journal. */
    private void logViewChecks(MainActivity activity, DemoOwner owner) throws Exception {
        ArrayList<String> previous = mainValue(() -> new ArrayList<>(owner.lines));
        boolean detailsBefore = mainValue(() -> ((CheckBox)activity.findViewById(R.id.log_details)).isChecked());
        try {
            onMain(() -> {
                ((CheckBox)activity.findViewById(R.id.log_details)).setChecked(false);
                owner.lines.clear();
                for (int index = 0; index < 120; index++) owner.lines.add(syntheticLog(index));
                owner.notifyChanged();
            });
            await(() -> mainValue(() -> logReady(activity) && logAtBottom(activity)), "New log page initially follows the latest record");
            onMain(() -> ((ScrollView)activity.findViewById(R.id.log_scroll)).scrollTo(0, 0));
            await(() -> mainValue(() -> ((ScrollView)activity.findViewById(R.id.log_scroll)).getScrollY() == 0 &&
                activity.findViewById(R.id.log_latest).getVisibility() == View.VISIBLE), "Scrolling to history reveals the jump-to-latest control");
            onMain(() -> { owner.lines.add(syntheticLog(120)); owner.notifyChanged(); });
            await(() -> mainValue(() -> logReady(activity) && ((TextView)activity.findViewById(R.id.log)).getText().toString().contains("SYNTHETIC_LOG_120")),
                "New synthetic record is rendered while reading history");
            onMain(() -> check(((ScrollView)activity.findViewById(R.id.log_scroll)).getScrollY() <= 1 && !logAtBottom(activity),
                "Appending a record preserves the history position instead of jumping to the end"));
            onMain(() -> activity.findViewById(R.id.log_details).performClick());
            await(() -> mainValue(() -> logReady(activity) && ((TextView)activity.findViewById(R.id.log)).getText().toString().startsWith("utc_ms=")),
                "Details toggle displays the original diagnostic fields");
            onMain(() -> check(((ScrollView)activity.findViewById(R.id.log_scroll)).getScrollY() <= 1 && !logAtBottom(activity),
                "Showing details preserves the history position"));
            onMain(() -> activity.findViewById(R.id.log_details).performClick());
            await(() -> mainValue(() -> logReady(activity) && !((TextView)activity.findViewById(R.id.log)).getText().toString().contains("utc_ms=")),
                "Compact toggle removes diagnostic fields from the displayed text");
            onMain(() -> {
                check(((ScrollView)activity.findViewById(R.id.log_scroll)).getScrollY() <= 1 && !logAtBottom(activity),
                    "Returning to compact logs preserves the history position");
                check(activity.findViewById(R.id.log_latest).getVisibility() == View.VISIBLE, "Jump-to-latest remains available while reading history");
                activity.findViewById(R.id.log_latest).performClick();
            });
            await(() -> mainValue(() -> logAtBottom(activity) && activity.findViewById(R.id.log_latest).getVisibility() == View.GONE),
                "Explicit jump-to-latest reaches the end of the actual ScrollView");
            onMain(() -> offline(owner.model, "Log display operations"));
        } finally {
            onMain(() -> {
                owner.lines.clear(); owner.lines.addAll(previous);
                ((CheckBox)activity.findViewById(R.id.log_details)).setChecked(detailsBefore);
                owner.notifyChanged();
            });
        }
    }
    private static String syntheticLog(int index) {
        return "utc_ms=1700000000000 sequence=" + index + " message=SYNTHETIC_LOG_" + index + " history scrolling check";
    }
    private static boolean logReady(Activity activity) {
        ScrollView scroll = activity.findViewById(R.id.log_scroll);
        TextView log = activity.findViewById(R.id.log);
        return log.isShown() && !log.isLayoutRequested() && log.getLayout() != null && scroll.getHeight() > 0 &&
            scroll.getChildCount() == 1 && scroll.getChildAt(0).getHeight() > scroll.getHeight() * 2;
    }
    private static boolean logAtBottom(Activity activity) {
        ScrollView scroll = activity.findViewById(R.id.log_scroll);
        return scroll.getChildCount() == 1 && scroll.getScrollY() >=
            Math.max(0, scroll.getChildAt(0).getBottom() + scroll.getPaddingBottom() - scroll.getHeight()) - 1;
    }

    private void settingsStoreChecks() throws Exception {
        File directory = new File(getTargetContext().getCacheDir(), "full-demo-settings-checks");
        check(directory.isDirectory() || directory.mkdirs(), "Independent settings test directory");
        File file = new File(directory, "demo-settings.json");
        SettingsStore store = new SettingsStore(directory);
        DemoSettings expected = new DemoSettings(DemoUtilityChecks.settings());
        try {
            store.save(expected);
            DemoSettings loaded = store.load();
            check(loaded.label.description().equals(expected.label.description()) && loaded.label.copies == expected.label.copies,
                "Actual Android AtomicFile/JsonReader preserve schema3 canvas and protocol settings");
            check(DemoSettings.integer(loaded.values, "version", 3, 3) == 3 && loaded.values.keySet().equals(expected.values.keySet()),
                "Real JSON reload contains exactly the current schema keys");
            for (Map.Entry<String, Object> entry : expected.values.entrySet()) {
                Object actual = loaded.values.get(entry.getKey()), value = entry.getValue();
                check(value instanceof Number ? actual instanceof Number && ((Number)value).longValue() == ((Number)actual).longValue() :
                    java.util.Objects.equals(value, actual), "Settings value round trip: " + entry.getKey());
            }
            Map<String, Object> left = DemoUtilityChecks.settings(); left.put("origin_mode", OriginPlacement.MANUAL);
            left.put("x_offset", 0); left.put("origin_adjustment", 17);
            store.save(new DemoSettings(left));
            DemoSettings calibrated = store.load();
            check(calibrated.label.xOffsetDots == 17 && calibrated.label.manualOriginDots == 0 &&
                calibrated.label.horizontalCalibrationDots == 17 && ((Number)calibrated.values.get("x_offset")).intValue() == 0,
                "Real JSON save/load keeps positive LEFT calibration separate from the absolute-origin field");
            store.save(expected);
            AtomicFile atomic = new AtomicFile(file);
            FileOutputStream interrupted = atomic.startWrite();
            interrupted.write("{partial".getBytes(StandardCharsets.UTF_8)); atomic.failWrite(interrupted);
            check(store.load().label.description().equals(expected.label.description()), "Interrupted AtomicFile write keeps the previous valid configuration");
            for (String malformed : new String[]{"{\"version\":3,\"version\":3}", "{\"version\":3.5}", "{} trailing"}) {
                try (OutputStream output = new FileOutputStream(file)) { output.write(malformed.getBytes(StandardCharsets.UTF_8)); }
                try { store.load(); throw new AssertionError("Malformed Android JSON accepted"); }
                catch (IOException expectedFailure) { }
            }
            store.save(expected); store.clear();
            check(!file.exists() && !new File(file.getPath() + ".bak").exists() && !new File(file.getPath() + ".new").exists(),
                "Explicit settings clear removes AtomicFile segments");
            try { store.load(); throw new AssertionError("Cleared configuration loaded"); }
            catch (IOException expectedFailure) { }
        } finally {
            store.clear();
            check(directory.delete(), "Settings test directory removed without touching application configuration");
        }
    }

    private void pendingActivityResults(MainActivity activity, DemoOwner owner, DemoModel model) throws Exception {
        File directory = new File(getTargetContext().getCacheDir(), "full-demo-result-checks");
        check(directory.isDirectory() || directory.mkdirs(), "Independent Activity-result test directory");
        File source = new File(directory, "synthetic.png"), export = new File(directory, "synthetic-log.txt");
        Bitmap bitmap = Bitmap.createBitmap(24, 16, Bitmap.Config.ARGB_8888);
        try {
            bitmap.eraseColor(0xff000000);
            try (OutputStream output = new FileOutputStream(source)) {
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output), "Synthetic Activity-result PNG encoded");
            }
        } finally { bitmap.recycle(); }
        PreparedImage previous = mainValue(() -> owner.images.state().prepared);
        Uri sourceUri = Uri.fromFile(source), exportUri = Uri.fromFile(export);
        String exportedText = "Synthetic Full Demo export\n第二行\n";
        try {
            onMain(() -> {
                check(((Integer)field(activity, "IMPORT")) == 11 && ((Integer)field(activity, "EXPORT")) == 12,
                    "Activity-result regression targets the actual import/export request codes");
                check(owner.beginImagePicker(), "Owner registers a picker flow to invalidate");
                owner.abandonImageFlow();
                activity.onActivityResult(11, Activity.RESULT_OK, new Intent().setData(sourceUri));
                check(owner.importUri == null && !owner.pickerOpen && !owner.images.state().busy,
                    "A late invalidated picker result cannot import into a later device flow");
                check(owner.beginImagePicker(), "Owner registers the explicit synthetic picker flow");
                set(activity, R.id.label_width, ""); set(activity, R.id.height, "");
                setField(activity, "started", false);
                activity.onActivityResult(11, Activity.RESULT_OK, new Intent().setData(sourceUri));
                check(sourceUri.equals(owner.importUri) && !owner.pickerOpen &&
                    !owner.images.state().busy && owner.images.state().prepared == previous,
                    "Result-before-onStart retains the import URI without requiring print parameters or decoding early");
                // Simulate only the model's cleanup gate; no SDK disconnect or Bluetooth call is made.
                setField(model, "disconnecting", true);
                callActivityOnStart(activity);
                check(sourceUri.equals(owner.importUri) && !owner.images.state().busy,
                    "onStart preserves an import while previous connection cleanup is pending");
                setField(model, "disconnecting", false); model.inputError(null);
            });
            await(() -> mainValue(() -> !owner.images.state().busy && owner.images.state().imported && owner.images.state().prepared != previous),
                "Deferred import resumes after foreground cleanup becomes ready");
            onMain(() -> {
                check(owner.importUri == null && owner.images.state().error == null && owner.images.state().prepared == null &&
                    owner.images.state().thumbnail == null && owner.images.state().preview == null,
                    "Deferred import works with empty dimensions and retains a private source without preparing display pixels");
                offline(model, "Deferred image import");
                owner.exportText = exportedText; setField(activity, "started", false);
                activity.onActivityResult(12, Activity.RESULT_OK, new Intent().setData(exportUri));
                check(exportUri.equals(field(owner, "exportUri")) && exportedText.equals(owner.exportText) && !export.exists(),
                    "Result-before-onStart retains the export URI and snapshot without writing early");
                owner.utilityBusy = true; callActivityOnStart(activity);
                check(exportUri.equals(field(owner, "exportUri")) && exportedText.equals(owner.exportText) && !export.exists(),
                    "onStart preserves an export while the utility cleanup gate is busy");
                owner.utilityBusy = false; owner.notifyChanged();
            });
            await(() -> mainValue(() -> !owner.utilityBusy && field(owner, "exportUri") == null && owner.exportText == null) && export.isFile(),
                "Deferred export resumes after the utility gate becomes idle");
            check(new String(read(export), StandardCharsets.UTF_8).equals(exportedText), "Deferred export writes the exact retained UTF-8 snapshot");
            onMain(() -> offline(model, "Deferred local log export"));
        } finally {
            onMain(() -> { setField(activity, "started", true); setField(model, "disconnecting", false); });
            source.delete(); export.delete(); directory.delete();
        }
    }

    private static byte[] read(File file) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (InputStream input = new FileInputStream(file)) {
            byte[] block = new byte[1024]; int count;
            while ((count = input.read(block)) != -1) bytes.write(block, 0, count);
        }
        return bytes.toByteArray();
    }

    /** Observe real framework stop/resume/destroy callbacks around an imported source; no printer is connected. */
    private void importedActivityLifecycle(MainActivity original, DemoOwner owner, DemoModel model,
            AtomicReference<MainActivity> screen) throws Exception {
        DemoImageSource importedSource = mainValue(owner.images::source);
        File privateSource = (File)field(field(importedSource, "shared"), "file");
        check(privateSource != null && privateSource.isFile(), "Deferred import owns a private source before recreation");
        MainActivity activity = recreate(original); screen.set(activity);
        onMain(() -> {
            check(field(activity, "owner") == owner && field(activity, "model") == model &&
                owner.images.source() == importedSource && owner.images.state().imported && privateSource.isFile(),
                "Real Activity recreation retains the exact imported source, owner, Model and private file");
            check(model.state().cleanupResult == null, "Imported-source recreation performs no Manager disconnect");
            set(activity, R.id.label_width, "128"); set(activity, R.id.height, "128");
            activity.findViewById(R.id.preview).performClick();
        });
        await(() -> mainValue(() -> !owner.images.state().busy && field(activity, "previewDialog") != null),
            "Imported image preview is displayed before leaving the foreground");
        DemoImageSource sourceBeforeStop = mainValue(owner.images::source);
        PreparedImage preparedBeforeStop = mainValue(() -> owner.images.state().prepared);
        Bitmap preview = mainValue(() -> owner.images.state().preview);
        check(preview != null, "Background regression starts with a full preview bitmap");
        ThreadPoolExecutor images = (ThreadPoolExecutor)field(owner.images, "worker");
        ExecutorService journal = (ExecutorService)field(owner.journal, "worker");
        CountDownLatch workerEntered = new CountDownLatch(1), releaseWorker = new CountDownLatch(1);
        CountDownLatch stopped = new CountDownLatch(1), resumed = new CountDownLatch(1), destroyed = new CountDownLatch(1);
        Application application = activity.getApplication();
        Application.ActivityLifecycleCallbacks observer = new Application.ActivityLifecycleCallbacks() {
            public void onActivityCreated(Activity value, Bundle state) { }
            public void onActivityStarted(Activity value) { }
            public void onActivityResumed(Activity value) { if (value == activity) resumed.countDown(); }
            public void onActivityPaused(Activity value) { }
            public void onActivityStopped(Activity value) { if (value == activity) stopped.countDown(); }
            public void onActivitySaveInstanceState(Activity value, Bundle state) { }
            public void onActivityDestroyed(Activity value) { if (value == activity) destroyed.countDown(); }
        };
        onMain(() -> application.registerActivityLifecycleCallbacks(observer));
        try {
            // Hold only the test's image worker so a real onStop must cancel a queued preparation deterministically.
            images.execute(() -> {
                workerEntered.countDown();
                try { releaseWorker.await(); }
                catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
            });
            check(workerEntered.await(10, TimeUnit.SECONDS), "Image worker gate entered");
            onMain(() -> {
                owner.images.prepare((LabelConfig)call(activity, "readConfig"));
                check(owner.images.state().busy && images.getQueue().size() == 1,
                    "A display preparation is pending when the real Activity leaves the foreground");
                activity.startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            });
            check(stopped.await(10, TimeUnit.SECONDS), "Home delivers the real Activity.onStop callback");
            await(() -> mainValue(() -> !model.state().foreground && model.state().cleanupResult != null &&
                model.state().phase == DemoUiState.Phase.OFFLINE), "Real background Manager cleanup completes");
            DemoResult<Void> cleanup = mainValue(() -> model.state().cleanupResult);
            onMain(() -> {
                check(!(Boolean)field(activity, "started") && !activity.isChangingConfigurations(),
                    "Home stopped the Activity outside a configuration change");
                check(cleanup.raw.outcome == ManagerResult.Outcome.SUCCESS && cleanup.raw.code == 0 &&
                    !cleanup.raw.outputMayHaveOccurred, "Offline Manager background cleanup succeeds without output");
                DemoImages.State image = owner.images.state();
                check(!image.busy && image.preview == null && preview.isRecycled() &&
                    field(activity, "previewDialog") == null && images.getQueue().isEmpty(),
                    "Real onStop cancels queued image preparation and releases the full preview");
                check(image.prepared == preparedBeforeStop && image.imported && owner.images.source() == sourceBeforeStop &&
                    privateSource.isFile(), "Background cleanup preserves the valid imported source and display preparation");
                offline(model, "Real Home background transition");
            });
            releaseWorker.countDown();
            images.submit(() -> { }).get(10, TimeUnit.SECONDS);
            onMain(() -> getTargetContext().startActivity(new Intent(getTargetContext(), MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP)));
            check(resumed.await(10, TimeUnit.SECONDS), "The existing Activity resumes after returning from Home");
            waitForIdleSync();
            onMain(() -> {
                check((Boolean)field(activity, "started") && field(activity, "owner") == owner &&
                    field(activity, "model") == model && model.state().foreground,
                    "Foreground return reuses the same Activity, owner and Model");
                check(model.state().cleanupResult == cleanup && owner.images.source() == sourceBeforeStop &&
                    owner.images.state().imported && !owner.images.state().busy && owner.images.state().preview == null &&
                    privateSource.isFile(), "Foreground return retains the source without replaying preparation or cleanup");
                offline(model, "Real foreground return");
                check(!activity.findViewById(R.id.print).isEnabled() && !activity.findViewById(R.id.send_raw).isEnabled(),
                    "Foreground return requires an explicit connection before Print or Raw");
                activity.finish();
            });
            check(destroyed.await(10, TimeUnit.SECONDS), "Explicit finish delivers the real Activity.onDestroy callback");
            await(() -> mainValue(() -> model.state().phase == DemoUiState.Phase.CLOSED &&
                (Boolean)field(sourceBeforeStop, "closed")) && !privateSource.exists() && images.isTerminated() &&
                owner.utilities.isTerminated() && journal.isTerminated(),
                "Final Activity destruction releases the private source and all Demo owner workers");
            screen.set(null);
        } finally {
            releaseWorker.countDown();
            onMain(() -> {
                application.unregisterActivityLifecycleCallbacks(observer);
                if (!activity.isFinishing()) activity.finish();
            });
        }
    }

    private MainActivity launch() {
        MainActivity activity = (MainActivity)startActivitySync(new Intent(getTargetContext(), MainActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        runOnMainSync(() -> activity.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
        waitForIdleSync(); return activity;
    }
    private MainActivity recreate(MainActivity original) throws Exception {
        CountDownLatch created = new CountDownLatch(1);
        AtomicReference<MainActivity> replacement = new AtomicReference<>();
        Application application = original.getApplication();
        Application.ActivityLifecycleCallbacks observer = new Application.ActivityLifecycleCallbacks() {
            public void onActivityCreated(Activity value, Bundle state) {
                if (value instanceof MainActivity && value != original) { replacement.set((MainActivity)value); created.countDown(); }
            }
            public void onActivityStarted(Activity value) { }
            public void onActivityResumed(Activity value) { }
            public void onActivityPaused(Activity value) { }
            public void onActivityStopped(Activity value) { }
            public void onActivitySaveInstanceState(Activity value, Bundle state) { }
            public void onActivityDestroyed(Activity value) { }
        };
        onMain(() -> { application.registerActivityLifecycleCallbacks(observer); original.recreate(); });
        try { check(created.await(10, TimeUnit.SECONDS), "Replacement Activity lifecycle observed"); }
        finally { onMain(() -> application.unregisterActivityLifecycleCallbacks(observer)); }
        MainActivity current = replacement.get();
        check(current != null && current != original, "Recreation creates a new Activity");
        waitForIdleSync();
        await(() -> mainValue(() -> (Boolean)field(current, "started")), "Replacement Activity is started");
        return current;
    }
    private static DiscoveredDevice candidate() throws Exception {
        Class<?> type = Class.forName("com.netumscan.printersdk.BluetoothDiscovery$Device");
        Constructor<?> source = type.getDeclaredConstructor(String.class, String.class, java.util.List.class, Integer.class);
        source.setAccessible(true);
        Constructor<DiscoveredDevice> result = DiscoveredDevice.class.getDeclaredConstructor(type); result.setAccessible(true);
        return result.newInstance(source.newInstance("AA:BB:CC:DD:EE:01", "Synthetic SPP printer", Collections.emptyList(), null));
    }
    /** Presentation-only fixture: real connection/configuration behavior is checked through memory JNI above. */
    private static void seedPresentationConnection(DemoModel model, boolean connected, boolean initialized) throws Exception {
        PrinterProfile profile = connected ? com.netumscan.printersdk.LTM90Profile.printProfile() : null;
        Constructor<ConnectionInfo> info = ConnectionInfo.class.getDeclaredConstructor(
            String.class, String.class, PrinterProfile.class, ConnectionInfo.ConfigurationSource.class);
        info.setAccessible(true);
        Constructor<RequestId> ids = RequestId.class.getDeclaredConstructor(Object.class, long.class);
        ids.setAccessible(true);
        Object scope = new Object();
        RequestId connection = connected ? ids.newInstance(scope, 1L) : null;
        DemoUiState.MediaRequest media = null;
        if (connected && initialized) {
            RequestId request = ids.newInstance(scope, 2L);
            Constructor<?> result = ManagerResult.class.getDeclaredConstructor(RequestId.class,
                ManagerResult.Outcome.class, int.class, String.class, ManagerResult.Completion.class, boolean.class, Object.class);
            result.setAccessible(true);
            @SuppressWarnings("unchecked") ManagerResult<Void> success = (ManagerResult<Void>)result.newInstance(request,
                ManagerResult.Outcome.SUCCESS, 0, "Synthetic completed gap initialization",
                ManagerResult.Completion.DATA_SENT, true, null);
            media = new DemoUiState.MediaRequest(MediaMode.GAP, connection, request,
                new DemoResult<>(DemoResult.Kind.MEDIA, success));
        }
        setField(model, "connected", connected);
        setField(model, "connection", connection);
        setField(model, "printProfile", profile);
        setField(model, "connectionInfo", connected ? info.newInstance("AA:BB:CC:DD:EE:02",
            "Synthetic presentation printer", profile, ConnectionInfo.ConfigurationSource.EXPLICIT) : null);
        setField(model, "mediaRequest", media);
        setField(model, "configured", connected && initialized);
        model.inputError(null);
    }

    /** Keep failures actionable when framework widgets or owner lifecycle differ on a device. */
    private static String printReadiness(MainActivity activity, DemoOwner owner) throws Exception {
        String config;
        try { config = ((LabelConfig)call(activity, "readConfig")).description(); }
        catch (java.lang.reflect.InvocationTargetException invalid) { config = String.valueOf(invalid.getCause()); }
        DemoUiState state = owner.model.state();
        return "phase=" + state.phase + " foreground=" + state.foreground + " configured=" + owner.model.configured() +
            " canSubmit=" + state.canSubmitPrint() + " printingAllowed=" + field(state, "printingAllowed") +
            " started=" + field(activity, "started") + " form=" + choice(activity, R.id.form) +
            " receipt=" + call(activity, "receipt") + " media=" + (state.mediaRequest == null ? null : state.mediaRequest.mode) +
            " mediaResult=" + (state.mediaRequest == null || state.mediaRequest.result == null ? null :
                state.mediaRequest.result.raw.code + "/" + state.mediaRequest.result.raw.completion) +
            " choice=" + choice(activity, R.id.media) + " pixel=" + choice(activity, R.id.pixel_example) +
            " picker=" + owner.pickerOpen + " import=" + owner.importAfterDisconnect + " utility=" + owner.utilityBusy +
            " imageBusy=" + owner.images.state().busy + " hint=" + ((android.widget.TextView)activity.findViewById(R.id.print_hint)).getText() + " config=" + config;
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
    private static String text(Activity activity, int id) { return ((EditText)activity.findViewById(id)).getText().toString(); }
    private static int choice(Activity activity, int id) { return ((Spinner)activity.findViewById(id)).getSelectedItemPosition(); }
    private static void select(Activity activity, int id, int value) { ((Spinner)activity.findViewById(id)).setSelection(value); }
    private static void offline(DemoModel model, String action) throws Exception {
        DemoUiState state = model.state();
        check(state.phase == DemoUiState.Phase.OFFLINE && !state.discovering && state.lastRequest == null &&
            field(model, "active") == null && field(model, "discovery") == null && field(model, "connection") == null,
            action + " performs no Bluetooth request or printer IO");
    }
    private interface Work { void run() throws Exception; }
    private void onMain(Work work) {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        runOnMainSync(() -> { try { work.run(); } catch (Throwable error) { failure.set(error); } });
        if (failure.get() != null) throw new AssertionError("Main-thread Full Demo check", failure.get());
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
