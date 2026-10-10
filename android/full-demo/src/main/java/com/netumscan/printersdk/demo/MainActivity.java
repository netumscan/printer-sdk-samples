package com.netumscan.printersdk.demo;

import android.Manifest;
import android.app.*;
import android.bluetooth.BluetoothAdapter;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import com.netumscan.printersdk.*;
import java.io.*;
import android.text.*;
import java.util.concurrent.atomic.AtomicLong;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public final class MainActivity extends Activity {
    private static final int PERMISSIONS = 10, EXPORT = 11, IMPORT = 12;
    private static final String EDIT_DRAFT = "demo-edit-draft";
    private static final String SCANNING_MESSAGE = "正在扫描 BLE，最多 10 秒；可随时停止";
    private final Handler main = new Handler(Looper.getMainLooper());
    private static final ExecutorService images = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "PrinterDemo.images"); thread.setDaemon(true); return thread;
    });
    private final ArrayDeque<String> lines = new ArrayDeque<>();
    private final Map<Integer, Integer> selectedChoices = new HashMap<>();
    private final int[] fieldIds = {R.id.address, R.id.service, R.id.characteristic, R.id.mtu,
        R.id.payload, R.id.dpi, R.id.width, R.id.label_width, R.id.x_offset, R.id.origin_adjustment, R.id.height, R.id.copies, R.id.trailing_blank};
    private final int[] draftFieldIds = {R.id.service, R.id.characteristic, R.id.mtu, R.id.payload,
        R.id.dpi, R.id.width, R.id.label_width, R.id.x_offset, R.id.origin_adjustment, R.id.height, R.id.copies, R.id.trailing_blank};
    private final int[] draftChoiceIds = {R.id.transport, R.id.ble_profile, R.id.form,
        R.id.print_format, R.id.media, R.id.write_mode, R.id.origin_mode};
    private PrinterController controller;
    private DemoDiscovery discovery;
    interface DiscoveryFactory { DemoDiscovery create(); }
    private DiscoveryFactory discoveryFactory = () -> DemoDiscovery.create(this);
    private long discoveryGeneration;
    private AlertDialog devicePicker;
    private ConnectionConfig connectedConfig;
    interface ConnectionFactory {
        PrinterController.Session create(String address, boolean useBle, BleOptions options, ConnectionConfig config);
    }
    private ConnectionFactory sessionFactory = (address, useBle, options, config) -> new SdkSession(config.receipt ?
        (useBle ? MobilePrinter.ble(getApplicationContext(), address, options, config.receiptProfile()) :
                  MobilePrinter.spp(getApplicationContext(), address, config.receiptProfile())) :
        (useBle ? MobilePrinter.ble(getApplicationContext(), address, options, config.profile()) :
                  MobilePrinter.spp(getApplicationContext(), address, config.profile())), config);
    private Runnable permissionAction;
    private int permissionRequest = PERMISSIONS, nextPermissionRequest = PERMISSIONS;
    private String exportText;
    private boolean scanning, previewing, utilityBusy, applyingFields, diskWarningShown, showingLogs;
    private boolean showingRaw, syncingCopies;
    private boolean pendingImportAfterDisconnect, imagePickerOpen, followingLogs = true, renderingLogs;
    private long logRenderGeneration, logInteractionGeneration;
    private final ArrayList<String> renderedLogEntries = new ArrayList<>();
    private int[] renderedLogStarts = new int[0];
    private LogAnchor pendingLogAnchor;
    private ViewTreeObserver logPositionObserver;
    private ViewTreeObserver.OnPreDrawListener logPositionListener;
    private static final class LogAnchor {
        final String entry;
        final int offset;
        LogAnchor(String entry, int offset) { this.entry = entry; this.offset = offset; }
    }
    private int hintTarget;
    private Runnable unregisterLogBack;
    private PopupMenu configMenu;
    private final AtomicLong preparationGeneration = new AtomicLong();
    private volatile boolean started;
    private File sourceFile, sourceDirectory;
    private PreparedImage prepared;
    private PreparedImage thumbnailSnapshot;
    private long thumbnailGeneration = -1;
    private Bitmap thumbnailBitmap;
    private String imageOperation = "", selectedDeviceName;
    private boolean hadPreparedImage;
    private int pendingMedia = -1, sentMedia = -1;
    private DemoJournal journal;
    private SettingsStore settings;
    private static DemoJournal processJournal;
    private static final AtomicLong connectionCounter = new AtomicLong();
    private static synchronized DemoJournal journalForProcess(File directory) {
        if (processJournal == null) processJournal = new DemoJournal(directory);
        return processJournal;
    }
    private boolean offlineIdle() { return controller != null && controller.phase() == PrinterController.Phase.OFFLINE && idle(); }
    private boolean idle() {
        PrinterController.Phase phase = controller.phase();
        return !scanning && !previewing && !utilityBusy && !pendingImportAfterDisconnect && !imagePickerOpen &&
            (phase == PrinterController.Phase.OFFLINE || phase == PrinterController.Phase.READY);
    }
    private void invalidatePrepared() {
        if (applyingFields) return;
        preparationGeneration.incrementAndGet(); prepared = null; previewing = false;
        imageOperation = ""; clearThumbnail();
        if (controller != null) updateButtons(controller.phase());
    }
    private String text(int id) { return ((EditText)findViewById(id)).getText().toString().trim(); }
    private int choice(int id) { return ((Spinner)findViewById(id)).getSelectedItemPosition(); }
    private boolean receipt() { return choice(R.id.print_format) == 1; }
    private boolean ble() { return choice(R.id.transport) == 1; }
    private boolean centeredOrigin() { return !receipt() && choice(R.id.origin_mode) == OriginPlacement.CENTERED; }
    private void spinner(int id, String... labels) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ((Spinner)findViewById(id)).setAdapter(adapter);
    }
    private void watchChoice(int id, Runnable changed) {
        selectedChoices.put(id, choice(id));
        ((Spinner)findViewById(id)).setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onNothingSelected(AdapterView<?> parent) { }
            public void onItemSelected(AdapterView<?> parent, View view, int position, long rowId) {
                int current = choice(id);
                if (Objects.equals(selectedChoices.get(id), current)) return;
                selectedChoices.put(id, current); changed.run();
            }
        });
    }
    private void captureChoices() {
        for (int id : draftChoiceIds)
            selectedChoices.put(id, choice(id));
    }
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved); setContentView(R.layout.activity_main);
        View root = findViewById(R.id.root);
        disableSavedState(root);
        status("");
        findViewById(R.id.canvas_panel).addOnLayoutChangeListener((view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
            updateCanvasLayout());
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
            root.setOnApplyWindowInsetsListener((view, insets) -> {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() |
                    WindowInsets.Type.displayCutout() | WindowInsets.Type.ime());
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom); return insets;
            });
        }
        spinner(R.id.print_format, "CPCL 标签整图", "ESC/POS 小票整图（无切纸）");
        spinner(R.id.transport, "Bluetooth SPP（优先联调）", "Bluetooth BLE");
        spinner(R.id.media, "间隙纸", "黑标纸", "连续纸");
        spinner(R.id.origin_mode, "手动填写横向起点", "自动居中（按整字节宽度）");
        spinner(R.id.form, "请选择", "发送 FORM", "不发送 FORM");
        spinner(R.id.write_mode, "请选择 BLE 写入方式", "Write With Response", "Write Without Response");
        spinner(R.id.ble_profile, "M90 V1.2 令牌流控", "通用 BLE（无应用层流控）");
        for (int id : fieldIds) ((EditText)findViewById(id)).setText(id == R.id.copies ? "1" :
            id == R.id.x_offset || id == R.id.origin_adjustment || id == R.id.trailing_blank ? "0" : id == R.id.mtu ? "517" : id == R.id.payload ? "512" : "");
        BleOptions initialPreset = M90DevelopmentPreset.bleOptions();
        ((EditText)findViewById(R.id.service)).setText(initialPreset.service.toString());
        ((EditText)findViewById(R.id.characteristic)).setText(initialPreset.writeCharacteristic.toString());
        ((Spinner)findViewById(R.id.write_mode)).setSelection(2);
        // A new launch never loads saved configuration or a device. Only page drafts are restored below.
        sourceDirectory = new File(getNoBackupFilesDir(), "demo-images");
        settings = new SettingsStore(getNoBackupFilesDir());
        journal = journalForProcess(new File(getNoBackupFilesDir(), "demo-journal"));
        images.execute(() -> {
            if (!sourceDirectory.isDirectory() && !sourceDirectory.mkdirs()) return;
            File[] abandoned = sourceDirectory.listFiles();
            if (abandoned != null) for (File file : abandoned) if (file.isFile() && file.getName().startsWith("source-")) file.delete();
        });
        watchChoice(R.id.transport, () -> { transportChanged(); invalidatePrepared(); });
        watchChoice(R.id.ble_profile, () -> {
            if (applyingFields) return;
            if (choice(R.id.ble_profile) == 0) {
                BleOptions preset = M90DevelopmentPreset.bleOptions();
                ((EditText)findViewById(R.id.service)).setText(preset.service.toString());
                ((EditText)findViewById(R.id.characteristic)).setText(preset.writeCharacteristic.toString());
                ((Spinner)findViewById(R.id.write_mode)).setSelection(2);
            }
            invalidatePrepared();
            if (controller != null) updateButtons(controller.phase());
        });
        controller = new PrinterController(new PrinterController.Listener() {
            public void changed(PrinterController.Phase phase, String message) {
                main.post(() -> {
                    if (isDestroyed()) return;
                    if (phase == PrinterController.Phase.READY && message.startsWith("DATA_SENT") && pendingMedia > 0)
                        sentMedia = pendingMedia;
                    if (phase != PrinterController.Phase.PRINTING) pendingMedia = -1;
                    if (phase == PrinterController.Phase.OFFLINE || phase == PrinterController.Phase.CLOSED) sentMedia = -1;
                    status(message.equals("已连接；请设置画布与纸张，并预览后打印") ? "连接成功" : message);
                    log(message); updateButtons(controller.phase());
                    if (pendingImportAfterDisconnect && controller.phase() == PrinterController.Phase.OFFLINE) {
                        pendingImportAfterDisconnect = false;
                        if (started && !isFinishing()) openImagePicker();
                        updateButtons(controller.phase());
                    }
                });
            }
            public void log(String message) { main.post(() -> { if (!isDestroyed()) MainActivity.this.log(message); }); }
        });
        findViewById(R.id.discover).setOnClickListener(v -> {
            if (!started) return;
            if (scanning) stopDiscovery(true);
            else if (offlineIdle()) permissions(this::discover, ble());
        });
        findViewById(R.id.settings).setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)));
        findViewById(R.id.connect).setOnClickListener(v -> connect());
        findViewById(R.id.m90_preset).setOnClickListener(v -> applyM90Preset());
        findViewById(R.id.disconnect).setOnClickListener(v -> {
            pendingImportAfterDisconnect = false;
            if (previewing) { preparationGeneration.incrementAndGet(); previewing = false; imageOperation = ""; }
            controller.disconnect(); updateButtons(controller.phase());
        });
        findViewById(R.id.cancel).setOnClickListener(v -> {
            if (previewing) { preparationGeneration.incrementAndGet(); previewing = false; imageOperation = ""; updateButtons(controller.phase()); status("图片准备已取消；未提交"); }
            else controller.cancel();
        });
        findViewById(R.id.preview).setOnClickListener(v -> preview());
        findViewById(R.id.print).setOnClickListener(v -> print());
        findViewById(R.id.send_raw).setOnClickListener(v -> sendRaw());
        EditText rawHex = findViewById(R.id.raw_hex);
        String rawLimitError = "Raw 输入最多 65536 个字符；此次输入未应用";
        boolean[] rawEditRejected = {false};
        rawHex.setFilters(new InputFilter[]{(source, start, end, destination, from, to) -> {
            rawEditRejected[0] = false;
            if (destination.length() - (to - from) + (end - start) <= RawCommand.MAX_CHARACTERS) return null;
            rawEditRejected[0] = true;
            rawHex.setError(rawLimitError);
            // TextView.setText filters against an empty destination; preserve the existing command too.
            if (destination.length() == 0 && rawHex.length() != 0) return rawHex.getText().toString();
            return destination.subSequence(from, to); // Reject the entire edit; never silently truncate a command.
        }});
        rawHex.addTextChangedListener(new TextWatcher() {
            private String previous = "";
            public void beforeTextChanged(CharSequence value, int start, int count, int after) { previous = value.toString(); }
            public void onTextChanged(CharSequence value, int start, int before, int count) {
                // TextView resets its error flag after filtering, even for an unchanged replacement.
                if (rawEditRejected[0]) { rawEditRejected[0] = false; rawHex.setError(rawLimitError); }
                else if (!previous.contentEquals(value)) rawHex.setError(null);
            }
            public void afterTextChanged(Editable value) { }
        });
        ((EditText)findViewById(R.id.raw_copies)).addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence value, int start, int count, int after) { }
            public void onTextChanged(CharSequence value, int start, int before, int count) {
                if (syncingCopies) return;
                ((EditText)findViewById(R.id.raw_copies)).setError(null);
                ((EditText)findViewById(R.id.copies)).setText(value.toString());
            }
            public void afterTextChanged(Editable value) { }
        });
        findViewById(R.id.go_canvas).setOnClickListener(v -> scrollToControl(R.id.canvas_section_heading, false));
        findViewById(R.id.print_hint).setOnClickListener(v -> {
            if (started && hintTarget != 0 && (idle() || (scanning && hintTarget == R.id.discover)))
                scrollToControl(hintTarget, true);
        });
        findViewById(R.id.form_help_button).setOnClickListener(v -> showHelp(R.string.form_help_title, R.string.form_help));
        findViewById(R.id.media_help_button).setOnClickListener(v -> showHelp(R.string.media_help_title, R.string.media_help));
        findViewById(R.id.apply_media).setOnClickListener(v -> {
            try { pendingMedia = choice(R.id.media) + 1; controller.mediaControl(pendingMedia); }
            catch (RuntimeException error) { pendingMedia = -1; error(error); }
        });
        findViewById(R.id.locate_label).setOnClickListener(v -> {
            try { pendingMedia = 0; controller.mediaControl(0); }
            catch (RuntimeException error) { error(error); }
        });
        findViewById(R.id.export).setOnClickListener(v -> export());
        findViewById(R.id.clear_log).setOnClickListener(v -> clearLogs());
        findViewById(R.id.import_image).setOnClickListener(v -> chooseImage());
        findViewById(R.id.builtin_image).setOnClickListener(v -> {
            if (!offlineIdle() || sourceFile == null) return;
            File old = sourceFile; sourceFile = null; invalidatePrepared();
            if (old != null) images.execute(() -> old.delete());
            status("已选择内置测试图；请更新预览");
        });
        findViewById(R.id.more_button).setOnClickListener(view -> showConfigMenu());
        bindPanel(R.id.transport_details_toggle, R.id.transport_details, "高级连接参数");
        findViewById(R.id.logs_button).setOnClickListener(view -> showLogs(true));
        findViewById(R.id.logs_back).setOnClickListener(view -> { if (showingRaw) showRaw(false); else showLogs(false); });
        ((CheckBox)findViewById(R.id.log_details)).setOnCheckedChangeListener((button, checked) -> renderLogs());
        // Programmatic tail/anchor positioning is immediate; gesture scrolling remains unchanged.
        ((ScrollView)findViewById(R.id.log_scroll)).setSmoothScrollingEnabled(false);
        findViewById(R.id.log_latest).setOnClickListener(view -> {
            ++logInteractionGeneration; pendingLogAnchor = null; followingLogs = true;
            scrollLogs(true, 0);
            updateLogLatest();
        });
        findViewById(R.id.log_scroll).setOnTouchListener((view, event) -> {
            // Observe interaction only; the stock ScrollView keeps click, scrolling and accessibility handling.
            ++logInteractionGeneration; pendingLogAnchor = null;
            return false;
        });
        findViewById(R.id.log_scroll).setOnScrollChangeListener((view, x, y, oldX, oldY) -> {
            if (!renderingLogs) followingLogs = logsAtBottom();
            updateLogLatest();
        });
        for (int id : new int[]{R.id.dpi, R.id.width, R.id.label_width, R.id.height, R.id.copies, R.id.x_offset, R.id.trailing_blank,
                R.id.origin_adjustment, R.id.mtu, R.id.payload, R.id.service, R.id.characteristic})
            ((EditText)findViewById(id)).addTextChangedListener(new TextWatcher() {
                public void beforeTextChanged(CharSequence value, int start, int count, int after) { }
                public void onTextChanged(CharSequence value, int start, int before, int count) {
                    if (applyingFields) return;
                    ((EditText)findViewById(id)).setError(null); invalidatePrepared();
                }
                public void afterTextChanged(Editable value) { }
            });
        for (int id : new int[]{R.id.form, R.id.print_format, R.id.media, R.id.write_mode, R.id.origin_mode})
            watchChoice(id, () -> {
                if (applyingFields) return;
                if (id == R.id.form) findViewById(R.id.form_error).setVisibility(View.GONE);
                if (id == R.id.write_mode) findViewById(R.id.write_mode_error).setVisibility(View.GONE);
                invalidatePrepared();
            });
        ((EditText)findViewById(R.id.address)).addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence value, int start, int count, int after) { }
            public void onTextChanged(CharSequence value, int start, int before, int count) {
                selectedDeviceName = null; ((EditText)findViewById(R.id.address)).setError(null);
                if (applyingFields) return;
                if (controller != null) updateButtons(controller.phase());
            }
            public void afterTextChanged(Editable value) { }
        });
        boolean restoredDraft = restoreEditDraft(saved);
        log("SDK ABI 2/r10 / Android API " + Build.VERSION.SDK_INT + " / CPCL + ESC/POS");
        transportChanged(); updateButtons(PrinterController.Phase.OFFLINE);
        if (restoredDraft) {
            status("已保留填写参数；请重新选择设备并更新预览");
            log("页面重建，已恢复编辑参数；未连接或发送");
        }
    }
    @Override protected void onSaveInstanceState(Bundle saved) {
        // Keep incomplete input too, without saving identity, images, sessions or tasks.
        Bundle draft = new Bundle(); draft.putInt("version", 3);
        for (int id : draftFieldIds)
            draft.putString(getResources().getResourceEntryName(id), ((EditText)findViewById(id)).getText().toString());
        for (int id : draftChoiceIds)
            draft.putInt(getResources().getResourceEntryName(id), choice(id));
        saved.putBundle(EDIT_DRAFT, draft);
        super.onSaveInstanceState(saved);
    }
    private boolean restoreEditDraft(Bundle saved) {
        Bundle draft = saved == null ? null : saved.getBundle(EDIT_DRAFT);
        if (draft == null || draft.getInt("version") < 1 || draft.getInt("version") > 3) return false;
        applyingFields = true;
        try {
            for (int id : draftFieldIds) {
                String value = draft.getString(getResources().getResourceEntryName(id));
                if (value != null) ((EditText)findViewById(id)).setText(value);
            }
            for (int id : draftChoiceIds) {
                Spinner selector = findViewById(id);
                int value = draft.getInt(getResources().getResourceEntryName(id), -1);
                if (value >= 0 && value < selector.getCount()) selector.setSelection(value);
            }
            captureChoices();
        } finally { applyingFields = false; }
        return true;
    }
    private static void disableSavedState(View view) {
        view.setSaveEnabled(false); view.setSaveFromParentEnabled(false);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup)view;
            for (int index = 0; index < group.getChildCount(); index++) disableSavedState(group.getChildAt(index));
        }
    }
    private void transportChanged() {
        findViewById(R.id.ble_fields).setVisibility(ble() ? View.VISIBLE : View.GONE);
        findViewById(R.id.ble_custom_fields).setVisibility(ble() && choice(R.id.ble_profile) == 1 ? View.VISIBLE : View.GONE);
        updateDiscoveryButton();
    }
    private void bindPanel(int toggleId, int panelId, String label) {
        Button toggle = findViewById(toggleId);
        toggle.setOnClickListener(view -> setPanel(toggleId, panelId, label,
            findViewById(panelId).getVisibility() != View.VISIBLE));
        setPanel(toggleId, panelId, label, false);
    }
    private void setPanel(int toggleId, int panelId, String label, boolean expanded) {
        findViewById(panelId).setVisibility(expanded ? View.VISIBLE : View.GONE);
        Button toggle = findViewById(toggleId);
        toggle.setText((expanded ? "收起" : "展开") + label);
        toggle.setContentDescription(label + (expanded ? "，已展开" : "，已收起"));
    }
    private void showConfigMenu() {
        if (!started) return;
        dismissConfigMenu();
        PopupMenu menu = new PopupMenu(this, findViewById(R.id.more_button));
        menu.getMenuInflater().inflate(R.menu.config_actions, menu.getMenu());
        menu.setOnMenuItemClickListener(item -> {
            if (!started || !item.isEnabled()) return true;
            if (item.getItemId() == R.id.raw_diagnostics) { showRaw(true); return true; }
            if (!offlineIdle()) return true;
            int action;
            if (item.getItemId() == R.id.save_config) action = 0;
            else if (item.getItemId() == R.id.load_config) action = 1;
            else if (item.getItemId() == R.id.clear_config) action = 2;
            else if (item.getItemId() == R.id.load_legacy) action = 3;
            else return false;
            settingsAction(action); return true;
        });
        menu.setOnDismissListener(closed -> { if (configMenu == closed) configMenu = null; });
        configMenu = menu; updateConfigMenu(); menu.show();
    }
    private void updateConfigMenu() {
        if (configMenu == null) return;
        boolean enabled = started && offlineIdle();
        Menu menu = configMenu.getMenu();
        for (int index = 0; index < menu.size(); index++) {
            MenuItem item = menu.getItem(index);
            item.setEnabled(item.getItemId() == R.id.raw_diagnostics ? started : enabled);
        }
    }
    private void dismissConfigMenu() {
        PopupMenu menu = configMenu; configMenu = null;
        if (menu != null) menu.dismiss();
    }
    private void showLogs(boolean visible) {
        showPages(visible, false);
    }
    private void showRaw(boolean visible) { showPages(false, visible); }
    private void showPages(boolean logs, boolean raw) {
        if (showingLogs == logs && showingRaw == raw) return;
        dismissConfigMenu();
        showingLogs = logs; showingRaw = raw;
        View focused = getCurrentFocus();
        if (focused != null) {
            android.view.inputmethod.InputMethodManager keyboard =
                (android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
            if (keyboard != null) keyboard.hideSoftInputFromWindow(focused.getWindowToken(), 0);
            focused.clearFocus();
        }
        findViewById(R.id.print_page).setVisibility(logs || raw ? View.GONE : View.VISIBLE);
        findViewById(R.id.logs_page).setVisibility(logs ? View.VISIBLE : View.GONE);
        findViewById(R.id.raw_page).setVisibility(raw ? View.VISIBLE : View.GONE);
        findViewById(R.id.print_actions).setVisibility(logs || raw ? View.GONE : View.VISIBLE);
        findViewById(R.id.logs_button).setVisibility(logs ? View.GONE : View.VISIBLE);
        findViewById(R.id.logs_back).setVisibility(logs || raw ? View.VISIBLE : View.GONE);
        ((TextView)findViewById(R.id.app_title)).setText(logs ? R.string.logs_title : raw ? R.string.raw_title : R.string.screen_title);
        findViewById(R.id.status).setVisibility(logs || ((TextView)findViewById(R.id.status)).getText().length() == 0 ? View.GONE : View.VISIBLE);
        if (logs) renderLogs(); else cancelLogPositioning();
        if (unregisterLogBack != null) { unregisterLogBack.run(); unregisterLogBack = null; }
        if ((logs || raw) && Build.VERSION.SDK_INT >= 33)
            unregisterLogBack = LogBackNavigation.register(this, () -> showPages(false, false));
        updateButtons(controller.phase());
        findViewById(logs || raw ? R.id.logs_back : R.id.logs_button).sendAccessibilityEvent(
            android.view.accessibility.AccessibilityEvent.TYPE_VIEW_FOCUSED);
    }
    @Override public void onBackPressed() {
        if (showingLogs || showingRaw) showPages(false, false);
        else super.onBackPressed();
    }
    // Isolate the API 33 callback so older Android versions use Activity's Back path.
    @android.annotation.TargetApi(33)
    private static final class LogBackNavigation {
        static Runnable register(Activity activity, Runnable returnToPrint) {
            android.window.OnBackInvokedDispatcher dispatcher = activity.getOnBackInvokedDispatcher();
            android.window.OnBackInvokedCallback callback = returnToPrint::run;
            dispatcher.registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, callback);
            return () -> dispatcher.unregisterOnBackInvokedCallback(callback);
        }
    }
    private void clearThumbnail() {
        ImageView image = findViewById(R.id.thumbnail);
        image.setImageDrawable(null); image.setVisibility(View.GONE);
        if (thumbnailBitmap != null) { thumbnailBitmap.recycle(); thumbnailBitmap = null; }
        thumbnailSnapshot = null;
        thumbnailGeneration = -1;
    }
    private void updateThumbnail(PreparedImage snapshot) {
        long version = preparationGeneration.get();
        if (thumbnailSnapshot == snapshot && (thumbnailBitmap != null || thumbnailGeneration == version)) return;
        thumbnailSnapshot = snapshot; thumbnailGeneration = version;
        images.execute(() -> {
            try {
                Bitmap bitmap = ImageImport.thumbnail(snapshot, () -> preparationGeneration.get() != version);
                main.post(() -> {
                    if (isDestroyed() || !started || preparationGeneration.get() != version || prepared != snapshot) {
                        bitmap.recycle();
                        if (thumbnailSnapshot == snapshot && thumbnailGeneration == version) thumbnailSnapshot = null;
                        return;
                    }
                    ImageView image = findViewById(R.id.thumbnail); image.setImageDrawable(null);
                    if (thumbnailBitmap != null) thumbnailBitmap.recycle();
                    thumbnailBitmap = bitmap; image.setImageBitmap(bitmap); image.setVisibility(View.VISIBLE);
                    image.setContentDescription("当前图片预览，画布 " + snapshot.config.labelWidthDots + " × " + snapshot.config.height + " 点");
                });
            } catch (RuntimeException failure) {
                main.post(() -> {
                    if (thumbnailSnapshot == snapshot && thumbnailGeneration == version) thumbnailSnapshot = null;
                });
            }
        });
    }
    private void applyM90Preset() {
        if (!started || !offlineIdle()) return;
        applyingFields = true;
        try {
            ((Spinner)findViewById(R.id.print_format)).setSelection(0);
            ((EditText)findViewById(R.id.dpi)).setText(String.format(Locale.ROOT, "%d", M90DevelopmentPreset.DPI));
            ((EditText)findViewById(R.id.width)).setText(String.format(Locale.ROOT, "%d", M90DevelopmentPreset.WIDTH_DOTS));
            if (ble()) {
                BleOptions preset = M90DevelopmentPreset.bleOptions();
                ((Spinner)findViewById(R.id.ble_profile)).setSelection(0);
                ((Spinner)findViewById(R.id.write_mode)).setSelection(2);
                ((EditText)findViewById(R.id.mtu)).setText(String.format(Locale.ROOT, "%d", preset.requestedMtu));
                ((EditText)findViewById(R.id.payload)).setText(String.format(Locale.ROOT, "%d", preset.maxWriteBytes));
                ((EditText)findViewById(R.id.service)).setText(preset.service.toString());
                ((EditText)findViewById(R.id.characteristic)).setText(preset.writeCharacteristic.toString());
            }
            ((Spinner)findViewById(R.id.origin_mode)).setSelection(OriginPlacement.CENTERED);
            ((EditText)findViewById(R.id.origin_adjustment)).setText("0");
            ((EditText)findViewById(R.id.address)).setText("");
            connectedConfig = null; selectedDeviceName = null; clearFieldErrors(); captureChoices();
        } finally { applyingFields = false; }
        invalidatePrepared();
        status("M90 开发预设 r" + M90DevelopmentPreset.REVISION + " 已应用；画布与纸张参数保留，请重新选择设备");
        log("开发预设=" + M90DevelopmentPreset.ID + " revision=" + M90DevelopmentPreset.REVISION +
            " / CPCL / 203 DPI / 864 dots；未连接或发送");
    }
    private void updateOriginPresentation() {
        boolean automatic = centeredOrigin();
        findViewById(R.id.origin_policy_fields).setVisibility(receipt() ? View.GONE : View.VISIBLE);
        findViewById(R.id.origin_adjustment_field).setVisibility(automatic ? View.VISIBLE : View.GONE);
        ((TextView)findViewById(R.id.origin_help)).setText(automatic ? R.string.origin_centered_help : R.string.x_offset_help);
        if (!automatic) return;
        String origin = "";
        try {
            origin = Integer.toString(OriginPlacement.resolve(number(R.id.width, "打印机最大宽度"),
                number(R.id.label_width, "画布宽度"), OriginPlacement.CENTERED, 0,
                number(R.id.origin_adjustment, "居中校准偏移")));
        } catch (LabelConfig.ValidationError invalid) { /* Incomplete input stays visible without changing validation state. */ }
        EditText field = findViewById(R.id.x_offset);
        if (!field.getText().toString().equals(origin)) {
            boolean previous = applyingFields; applyingFields = true;
            try { field.setText(origin); } finally { applyingFields = previous; }
        }
    }
    private void updatePresentation(PrinterController.Phase phase) {
        updateOriginPresentation();
        String connection;
        switch (phase) {
            case CONNECTING: connection = "正在连接"; break;
            case READY: connection = "已连接 · 空闲"; break;
            case PRINTING: connection = "已连接 · 任务进行中"; break;
            case STOPPING: connection = "正在断开"; break;
            case CLOSED: connection = "会话已关闭"; break;
            default: connection = "未连接";
        }
        ((TextView)findViewById(R.id.connection_status)).setText(connection + " · " + (ble() ? "BLE" : "SPP"));
        boolean selected = BluetoothAdapter.checkBluetoothAddress(text(R.id.address).toUpperCase(Locale.ROOT));
        ((TextView)findViewById(R.id.device_summary)).setText(selected ?
            selectedDeviceName == null ? "已填写设备地址" : "已选择：" + selectedDeviceName : "尚未选择设备");
        String form = choice(R.id.form) == 1 ? "发送" : choice(R.id.form) == 2 ? "不发送" : "未选择";
        ((TextView)findViewById(R.id.canvas_summary)).setText((receipt() ? "小票" : "标签") + " · " +
            fieldSummary(R.id.label_width) + " × " + fieldSummary(R.id.height) + " dots · " + fieldSummary(R.id.dpi) + " DPI\n" +
            fieldSummary(R.id.copies) + " 份 · 留白=" + fieldSummary(R.id.trailing_blank) + " dots" +
            (receipt() ? "" : " · X=" + fieldSummary(R.id.x_offset) + " · FORM=" + form));
        EditText rawCopies = findViewById(R.id.raw_copies);
        String copiesText = ((EditText)findViewById(R.id.copies)).getText().toString();
        if (!copiesText.contentEquals(rawCopies.getText())) {
            syncingCopies = true;
            try { rawCopies.setText(copiesText); } finally { syncingCopies = false; }
        }
        findViewById(R.id.cpcl_fields).setVisibility(receipt() ? View.GONE : View.VISIBLE);
        findViewById(R.id.media_fields).setVisibility(receipt() ? View.GONE : View.VISIBLE);
        findViewById(R.id.apply_media).setVisibility(receipt() ? View.GONE : View.VISIBLE);
        findViewById(R.id.locate_label).setVisibility(receipt() ? View.GONE : View.VISIBLE);
        findViewById(R.id.media_status).setVisibility(receipt() ? View.GONE : View.VISIBLE);
        findViewById(R.id.media_apply_group).setVisibility(receipt() ? View.GONE : View.VISIBLE);
        findViewById(R.id.label_motion_group).setVisibility(receipt() ? View.GONE : View.VISIBLE);
        updateCanvasLayout();
        transportChanged();
        String source = sourceFile == null ? receipt() ? "内置小票示例" : "内置校准图" : "已导入 PNG/JPEG";
        ((Button)findViewById(R.id.builtin_image)).setText(sourceFile == null ? getString(R.string.builtin_image_current, source) :
            receipt() ? "切回内置小票示例" : "切回内置校准图");
        ((Button)findViewById(R.id.import_image)).setText(phase == PrinterController.Phase.READY ? R.string.disconnect_import_image : R.string.import_image);
        String dimensions = prepared == null ? "等待准备" : prepared.config.labelWidthDots + " × " + prepared.config.height + " dots";
        ((TextView)findViewById(R.id.image_summary)).setText("来源：" + source + "\n画布：" + dimensions);
        boolean valid = currentImage() && (phase != PrinterController.Phase.READY ||
            connectedConfig != null && connectedConfig.matches(prepared.config));
        String imageState = previewing ? imageOperation : valid ?
            phase == PrinterController.Phase.READY ? "图片已准备，可打印" :
            phase == PrinterController.Phase.OFFLINE ? "图片已准备，待连接" : "图片已准备，等待当前操作结束" :
            hadPreparedImage ? "需要重新预览" : "尚未准备图片";
        ((TextView)findViewById(R.id.image_status)).setText(imageState);
        if (valid && !previewing) updateThumbnail(prepared);
        updateActionHint(phase, valid, selected);
        ((TextView)findViewById(R.id.media_status)).setText(pendingMedia > 0 ? R.string.media_sending :
            sentMedia == choice(R.id.media) + 1 ? R.string.media_sent : R.string.media_not_sent);
    }
    private void updateCanvasLayout() {
        View panel = findViewById(R.id.canvas_panel);
        int width = Math.round(panel.getWidth() / getResources().getDisplayMetrics().density);
        CanvasLayout.apply(findViewById(R.id.root), width, getResources().getConfiguration().fontScale, receipt());
        findViewById(R.id.copies).setNextFocusForwardId(receipt() ? R.id.trailing_blank :
            centeredOrigin() ? R.id.origin_mode : R.id.x_offset);
        findViewById(R.id.x_offset).setNextFocusForwardId(R.id.origin_mode);
        findViewById(R.id.origin_mode).setNextFocusForwardId(centeredOrigin() ? R.id.origin_adjustment : R.id.trailing_blank);
        findViewById(R.id.origin_adjustment).setNextFocusForwardId(R.id.trailing_blank);
        findViewById(R.id.trailing_blank).setNextFocusForwardId(receipt() ? R.id.import_image : R.id.media);
    }
    private void updateActionHint(PrinterController.Phase phase, boolean valid, boolean selected) {
        hintTarget = 0;
        String hint;
        if (pendingImportAfterDisconnect) hint = "正在断开以导入图片 · 请稍候";
        else if (imagePickerOpen) hint = "正在选择图片 · 取消后保留原图";
        else if (previewing) hint = imageOperation + " · 完成前不能发送";
        else if (scanning) { hint = "正在扫描设备 · 点击定位停止按钮"; hintTarget = R.id.discover; }
        else if (utilityBusy) hint = "正在处理本机数据 · 请稍候";
        else if (phase == PrinterController.Phase.CONNECTING || phase == PrinterController.Phase.STOPPING) hint = "连接状态切换中 · 请稍候";
        else if (phase == PrinterController.Phase.PRINTING) hint = "任务进行中 · 已发送的数据无法撤回";
        else if (phase == PrinterController.Phase.CLOSED) hint = "会话已关闭";
        else {
            try {
                if (phase == PrinterController.Phase.OFFLINE) {
                    if (!selected) { hint = "请选择设备 · 点击定位"; hintTarget = R.id.discover; }
                    else {
                        readConnectionConfig(); if (ble()) bleOptions();
                        hint = "设备参数已填写 · 点击定位连接按钮"; hintTarget = R.id.connect;
                    }
                } else {
                    LabelConfig label = readConfig();
                    if (connectedConfig == null || !connectedConfig.matches(label)) {
                        hint = "设备参数已变化 · 请断开后重新连接"; hintTarget = R.id.disconnect;
                    } else if (!valid) {
                        hint = hadPreparedImage ? "参数已修改 · 点击定位，更新预览" : "请准备图片 · 点击定位预览按钮";
                        hintTarget = R.id.preview;
                    } else { hint = "图片已准备 · 点击定位打印按钮"; hintTarget = R.id.print; }
                }
            } catch (LabelConfig.ValidationError error) {
                hint = error.getMessage() + " · 点击定位"; hintTarget = fieldId(error.field);
            }
        }
        TextView target = findViewById(R.id.print_hint); target.setText(hint);
        boolean navigable = hintTarget != 0 && (idle() || (scanning && hintTarget == R.id.discover));
        target.setClickable(navigable); target.setFocusable(navigable);
    }
    private void showHelp(int title, int message) {
        if (!started) return;
        new AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton("知道了", null).show();
    }
    private String fieldSummary(int id) {
        String value = text(id);
        return value.isEmpty() ? "未填写" : value;
    }
    private void status(String message) {
        TextView view = findViewById(R.id.status); view.setText(message);
        view.setVisibility(showingLogs || message.isEmpty() ? View.GONE : View.VISIBLE);
    }
    private void log(String message) {
        journal.append(message, connectionCounter.get(), entry -> main.post(() -> {
            if (isDestroyed()) return;
            lines.addLast(entry); while (lines.size() > 160) lines.removeFirst();
            renderLogs();
            if (journal.degraded() && !diskWarningShown) {
                diskWarningShown = true; Toast.makeText(this, "日志磁盘不可用；本次使用有界内存记录", Toast.LENGTH_LONG).show();
            }
        }));
    }
    private void renderLogs() {
        ScrollView scroll = findViewById(R.id.log_scroll);
        TextView text = findViewById(R.id.log);
        boolean follow = followingLogs;
        LogAnchor anchor = follow ? null : captureLogAnchor();
        int previousY = scroll.getScrollY();
        long generation = ++logRenderGeneration, interaction = logInteractionGeneration;
        cancelLogPositioning();
        pendingLogAnchor = anchor;
        boolean details = ((CheckBox)findViewById(R.id.log_details)).isChecked();
        ArrayList<String> display = new ArrayList<>();
        java.time.ZoneId zone = java.time.ZoneId.systemDefault();
        for (String entry : lines) display.add(details ? entry : LogDisplay.compact(entry, zone));
        renderedLogEntries.clear(); renderedLogEntries.addAll(lines);
        renderedLogStarts = new int[display.size()];
        int start = 0;
        for (int index = 0; index < display.size(); index++) {
            renderedLogStarts[index] = start; start += display.get(index).length() + 1;
        }
        text.setText(String.join("\n", display));
        updateLogTimeZone();
        updateLogLatest();
        // Hidden pages keep their anchor until shown; visible pages position after the new text is measured.
        if (!started || !text.isShown()) return;
        logPositionObserver = text.getViewTreeObserver();
        logPositionListener = () -> {
            cancelLogPositioning();
            if (!isDestroyed() && generation == logRenderGeneration) {
                pendingLogAnchor = null;
                if (interaction == logInteractionGeneration) {
                    scrollLogs(follow, anchor == null ? previousY : logAnchorY(anchor));
                    followingLogs = follow || logsAtBottom();
                }
                updateLogLatest();
            }
            return true;
        };
        logPositionObserver.addOnPreDrawListener(logPositionListener);
    }
    private void cancelLogPositioning() {
        if (logPositionObserver != null && logPositionObserver.isAlive() && logPositionListener != null)
            logPositionObserver.removeOnPreDrawListener(logPositionListener);
        logPositionObserver = null; logPositionListener = null;
    }
    private LogAnchor captureLogAnchor() {
        if (pendingLogAnchor != null) return pendingLogAnchor;
        TextView text = findViewById(R.id.log);
        Layout layout = text.getLayout();
        if (layout == null || renderedLogEntries.isEmpty()) return pendingLogAnchor;
        ScrollView scroll = findViewById(R.id.log_scroll);
        int top = Math.max(0, scroll.getScrollY() - text.getTop() - text.getTotalPaddingTop());
        int line = layout.getLineForVertical(top), offset = layout.getLineStart(line);
        int index = Arrays.binarySearch(renderedLogStarts, offset);
        if (index < 0) index = -index - 2;
        index = Math.max(0, Math.min(index, renderedLogEntries.size() - 1));
        int recordLine = layout.getLineForOffset(renderedLogStarts[index]);
        return new LogAnchor(renderedLogEntries.get(index), Math.max(0, top - layout.getLineTop(recordLine)));
    }
    private int logAnchorY(LogAnchor anchor) {
        TextView text = findViewById(R.id.log);
        Layout layout = text.getLayout();
        int index = renderedLogEntries.indexOf(anchor.entry);
        // An evicted anchor falls back to the oldest retained record.
        if (layout == null || index < 0) return 0;
        int top = layout.getLineTop(layout.getLineForOffset(renderedLogStarts[index]));
        int end = index + 1 < renderedLogStarts.length ?
            layout.getLineTop(layout.getLineForOffset(renderedLogStarts[index + 1])) : layout.getHeight();
        return text.getTop() + text.getTotalPaddingTop() + top + Math.min(anchor.offset, Math.max(0, end - top - 1));
    }
    private void scrollLogs(boolean tail, int y) {
        ScrollView scroll = findViewById(R.id.log_scroll);
        renderingLogs = true;
        try {
            int bottom = scroll.getChildCount() == 0 ? 0 : scroll.getChildAt(0).getBottom() + scroll.getPaddingBottom();
            // Scroll only: fullScroll also changes focus and can pull a selectable TextView back to its start.
            scroll.scrollTo(0, tail ? Math.max(0, bottom - scroll.getHeight()) : y);
        }
        finally { renderingLogs = false; }
    }
    private boolean logsAtBottom() {
        ScrollView scroll = findViewById(R.id.log_scroll);
        return scroll.getChildCount() == 0 || scroll.getChildAt(0).getHeight() <=
            scroll.getHeight() + scroll.getScrollY() + Math.round(24 * getResources().getDisplayMetrics().density);
    }
    private void updateLogLatest() {
        findViewById(R.id.log_latest).setVisibility(!followingLogs && !lines.isEmpty() ? View.VISIBLE : View.GONE);
    }
    private void updateLogTimeZone() {
        java.time.ZoneId zone = java.time.ZoneId.systemDefault();
        String offset = java.time.ZonedDateTime.now(zone).getOffset().getId();
        ((TextView)findViewById(R.id.log_time_zone)).setText(getString(R.string.log_time_zone, zone.getId() + "（UTC" +
            (offset.equals("Z") ? "+00:00" : offset) + "）"));
    }
    private void error(RuntimeException error) {
        String message = error instanceof PrinterException ? "SDK 错误码 " + ((PrinterException)error).code + "；" +
            PrinterException.description(((PrinterException)error).code) :
            error instanceof SecurityException ? "蓝牙或文件访问权限不可用；请在系统应用设置中检查授权" :
            error instanceof IllegalArgumentException ? error.getMessage() : "操作失败；请检查输入后重试";
        status(message == null ? "操作失败" : message); log("错误：" + message);
        if (error instanceof LabelConfig.ValidationError) {
            showFieldError(((LabelConfig.ValidationError)error).field, message); return;
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
    private int fieldId(String name) {
        switch (name) {
            case "dpi": return R.id.dpi;
            case "width": return R.id.width;
            case "label_width": return R.id.label_width;
            case "height": return R.id.height;
            case "copies": return R.id.copies;
            case "trailing_blank": return R.id.trailing_blank;
            case "x_offset": return R.id.x_offset;
            case "origin_adjustment": return R.id.origin_adjustment;
            case "origin_mode": return R.id.origin_mode;
            case "address": return R.id.address;
            case "service": return R.id.service;
            case "characteristic": return R.id.characteristic;
            case "mtu": return R.id.mtu;
            case "payload": return R.id.payload;
            case "form": return R.id.form;
            case "write_mode": return R.id.write_mode;
            default: return 0;
        }
    }
    private void locateField(String name) { int id = fieldId(name); if (id != 0) scrollToControl(id, true); }
    private void scrollToControl(int id, boolean focus) {
        showLogs(false);
        boolean deviceField = id == R.id.address || id == R.id.service || id == R.id.characteristic ||
            id == R.id.mtu || id == R.id.payload || id == R.id.write_mode;
        if (deviceField)
            setPanel(R.id.transport_details_toggle, R.id.transport_details, "高级连接参数", true);
        View field = findViewById(id);
        main.post(() -> {
            if (isDestroyed() || !started) return;
            if (focus && (field instanceof EditText || field instanceof Spinner)) field.requestFocus();
            field.requestRectangleOnScreen(new Rect(0, 0, field.getWidth(), field.getHeight()), true);
        });
    }
    private void showFieldError(String name, String message) {
        int id = fieldId(name); if (id == 0) return;
        View field = findViewById(id);
        if (field instanceof EditText) ((EditText)field).setError(message);
        else {
            TextView detail = findViewById(id == R.id.form ? R.id.form_error : R.id.write_mode_error);
            detail.setText(message); detail.setVisibility(View.VISIBLE);
            field.setFocusableInTouchMode(true);
        }
        locateField(name);
    }
    private void clearFieldErrors() {
        for (int id : fieldIds) ((EditText)findViewById(id)).setError(null);
        findViewById(R.id.form_error).setVisibility(View.GONE);
        findViewById(R.id.write_mode_error).setVisibility(View.GONE);
    }
    private int number(int id, String name) {
        try { return Integer.parseInt(text(id)); }
        catch (NumberFormatException error) {
            throw new LabelConfig.ValidationError(getResources().getResourceEntryName(id), "请填写" + name + "（整数）");
        }
    }
    private LabelConfig config() {
        clearFieldErrors();
        return readConfig();
    }
    private LabelConfig readConfig() {
        int dpi = number(R.id.dpi, "DPI"), width = number(R.id.width, "打印机最大宽度");
        int canvas = number(R.id.label_width, "本次标签可打印宽度");
        int x = receipt() ? 0 : OriginPlacement.resolve(width, canvas, choice(R.id.origin_mode),
            centeredOrigin() ? 0 : number(R.id.x_offset, "横向起点"),
            centeredOrigin() ? number(R.id.origin_adjustment, "居中校准偏移") : 0);
        return new LabelConfig(dpi, width, canvas, number(R.id.height, "标签高度"),
            number(R.id.copies, "份数"), choice(R.id.form), x, receipt(), number(R.id.trailing_blank, "结束留白"));
    }
    private ConnectionConfig connectionConfig() {
        clearFieldErrors();
        return readConnectionConfig();
    }
    private ConnectionConfig readConnectionConfig() {
        return new ConnectionConfig(number(R.id.dpi, "DPI"), number(R.id.width, "打印机最大宽度"), receipt());
    }
    private boolean currentImage() {
        if (prepared == null) return false;
        try { return prepared.matches(readConfig()); }
        catch (IllegalArgumentException invalid) { return false; }
    }
    private UUID uuid(int id, String name) {
        String value = text(id);
        if (!value.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))
            throw new LabelConfig.ValidationError(getResources().getResourceEntryName(id), "请填写完整的" + name);
        return UUID.fromString(value);
    }
    private BleOptions bleOptions() {
        int mtu = number(R.id.mtu, "MTU"), payload = number(R.id.payload, "每包有效字节数");
        if (mtu < 23 || mtu > 517) throw new LabelConfig.ValidationError("mtu", "MTU 须为 23–517");
        if (payload < 1 || payload > 512) throw new LabelConfig.ValidationError("payload", "每包有效字节数须为 1–512");
        if (choice(R.id.ble_profile) == 0) return M90Profile.bleOptions(mtu, payload);
        if (choice(R.id.write_mode) == 0) throw new LabelConfig.ValidationError("write_mode", "请选择 BLE 写入方式");
        return new BleOptions(uuid(R.id.service, "服务 UUID"), uuid(R.id.characteristic, "写入特征 UUID"),
            choice(R.id.write_mode) == 1, mtu, payload);
    }
    private void connect() {
        if (!started || !offlineIdle()) return;
        try {
            connectionConfig();
            String address = text(R.id.address).toUpperCase(Locale.ROOT);
            if (!BluetoothAdapter.checkBluetoothAddress(address)) throw new LabelConfig.ValidationError("address", "请选择设备或填写有效蓝牙地址");
            if (ble()) bleOptions();
            permissions(() -> {
                if (!started || !offlineIdle()) return;
                try {
                    ConnectionConfig connection = connectionConfig();
                    String selected = text(R.id.address).toUpperCase(Locale.ROOT);
                    if (!BluetoothAdapter.checkBluetoothAddress(selected))
                        throw new LabelConfig.ValidationError("address", "请选择设备或填写有效蓝牙地址");
                    boolean useBle = ble(); BleOptions options = useBle ? bleOptions() : null;
                    connectedConfig = connection; pendingMedia = -1; sentMedia = -1; connectionCounter.incrementAndGet();
                    log((useBle ? "BLE / " : "SPP / ") + connection.description());
                    if (options != null) log("BLE creditFlow=" + (options.creditFlowControl != null) +
                        " / requestedMtu=" + options.requestedMtu + " / payloadCap=" + options.maxWriteBytes);
                    controller.connect(() -> sessionFactory.create(selected, useBle, options, connection));
                } catch (RuntimeException failure) { error(failure); }
            }, false);
        } catch (RuntimeException failure) { error(failure); }
    }
    private void print() {
        if (!started || !idle() || controller.phase() != PrinterController.Phase.READY) return;
        try {
            LabelConfig label = config(); PreparedImage snapshot = prepared;
            if (connectedConfig == null || !connectedConfig.matches(label) || snapshot == null || !snapshot.matches(label))
                throw new IllegalArgumentException("请先更新当前图片预览；设备参数变化时需重新连接");
            controller.print(cancelled -> {
                if (cancelled.getAsBoolean()) throw new CancellationException();
                return snapshot.copyPixels();
            }, label);
        } catch (RuntimeException failure) { error(failure); }
    }
    private void preview() {
        try { if (idle()) prepare(config(), true, null); }
        catch (RuntimeException failure) { error(failure); }
    }
    private void sendRaw() {
        if (!started || !idle() || controller.phase() != PrinterController.Phase.READY) return;
        EditText raw = findViewById(R.id.raw_hex), copies = findViewById(R.id.raw_copies);
        try {
            int count;
            try { count = Integer.parseInt(text(R.id.copies)); }
            catch (NumberFormatException failure) { copies.setError("Raw 份数须为 1–20 的整数"); return; }
            if (count < 1 || count > 20) { copies.setError("Raw 每次限 1–20 份"); return; }
            RawCommand command = RawCommand.parse(raw.getText().toString(), count);
            raw.setError(null); copies.setError(null);
            controller.sendRaw(command.copyBytes(), command.copies);
        } catch (IllegalArgumentException failure) {
            raw.setError(failure.getMessage()); status(failure.getMessage());
        } catch (RuntimeException failure) { error(failure); }
    }
    private void showPreview(PreparedImage snapshot) {
        long version = preparationGeneration.get(); previewing = true; imageOperation = "正在生成预览…"; updateButtons(controller.phase());
        images.execute(() -> {
            try {
                Bitmap bitmap = ImageImport.preview(snapshot, () -> preparationGeneration.get() != version);
                main.post(() -> {
                    if (isDestroyed() || !started || preparationGeneration.get() != version || prepared != snapshot) { bitmap.recycle(); return; }
                    previewing = false; imageOperation = ""; updateButtons(controller.phase());
                    if (showingLogs || showingRaw) { bitmap.recycle(); return; }
                    ImageView image = new ImageView(this); image.setImageBitmap(bitmap); image.setAdjustViewBounds(true);
                    image.setContentDescription("当前图片预览，与发送内容一致");
                    ScrollView scroll = new ScrollView(this); scroll.addView(image);
                    AlertDialog dialog = new AlertDialog.Builder(this).setTitle("当前画布预览（未发送）")
                        .setView(scroll).setPositiveButton("关闭", null).create();
                    dialog.setOnDismissListener(d -> { image.setImageDrawable(null); bitmap.recycle(); }); dialog.show();
                });
            } catch (RuntimeException failure) {
                main.post(() -> {
                    if (!isDestroyed() && preparationGeneration.get() == version) {
                        previewing = false; imageOperation = ""; updateButtons(controller.phase());
                        status("预览显示失败；请关闭其他应用后重试");
                    }
                });
            }
        });
    }
    private void prepare(LabelConfig label, boolean show, Runnable after) {
        captureChoices();
        long version = preparationGeneration.incrementAndGet(); File source = sourceFile;
        PreparedImage cached = prepared; previewing = true; imageOperation = "正在准备图片…"; updateButtons(controller.phase());
        status(imageOperation);
        images.execute(() -> {
            try {
                PreparedImage snapshot = cached != null && cached.matches(label) ? cached :
                    source == null ? new PreparedImage(label, TestLabel.gray(label, () -> preparationGeneration.get() != version)) :
                    ImageImport.prepare(source, label, () -> preparationGeneration.get() != version);
                main.post(() -> {
                    if (isDestroyed() || !started || preparationGeneration.get() != version) return;
                    if (prepared != snapshot) clearThumbnail();
                    prepared = snapshot; hadPreparedImage = true; previewing = false; imageOperation = ""; updateButtons(controller.phase());
                    status("当前图片已准备；预览与发送内容一致，尚未发送"); log("图片画布：" + label.description());
                    if (show && !showingLogs && !showingRaw) showPreview(snapshot); if (after != null) after.run();
                });
            } catch (IOException | RuntimeException failure) {
                main.post(() -> {
                    if (isDestroyed() || preparationGeneration.get() != version) return;
                    previewing = false; imageOperation = ""; updateButtons(controller.phase()); status("图片准备失败或已取消；未提交，请检查画布参数后重试"); log("图片准备失败或已取消；未提交");
                });
            }
        });
    }
    private void chooseImage() {
        if (!started || !idle()) return;
        try { config(); } catch (RuntimeException failure) { error(failure); return; }
        if (controller.phase() == PrinterController.Phase.READY) {
            pendingImportAfterDisconnect = true;
            controller.disconnect(); updateButtons(controller.phase());
        } else if (offlineIdle()) openImagePicker();
    }
    private void openImagePicker() {
        if (!started || !offlineIdle()) return;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE)
            .putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/png", "image/jpeg"});
        imagePickerOpen = true; updateButtons(controller.phase());
        try { startActivityForResult(intent, IMPORT); }
        catch (RuntimeException failure) { imagePickerOpen = false; updateButtons(controller.phase()); error(failure); }
    }
    private void importImage(android.net.Uri uri) {
        if (!offlineIdle()) return;
        final LabelConfig label;
        try { label = config(); } catch (RuntimeException failure) { error(failure); return; }
        captureChoices();
        long version = preparationGeneration.incrementAndGet(); previewing = true; imageOperation = "正在导入图片…"; updateButtons(controller.phase());
        status(imageOperation);
        images.execute(() -> {
            File candidate = null;
            try {
                candidate = File.createTempFile("source-", ".image", sourceDirectory);
                try (InputStream input = getContentResolver().openInputStream(uri)) {
                    if (input == null) throw new IOException("No image");
                    ImageImport.copy(input, candidate, () -> preparationGeneration.get() != version);
                }
                PreparedImage snapshot = ImageImport.prepare(candidate, label, () -> preparationGeneration.get() != version);
                File accepted = candidate; candidate = null;
                main.post(() -> {
                    if (isDestroyed() || !started || preparationGeneration.get() != version) { images.execute(() -> accepted.delete()); return; }
                    File old = sourceFile; sourceFile = accepted; clearThumbnail();
                    prepared = snapshot; hadPreparedImage = true; previewing = false; imageOperation = "";
                    if (old != null) images.execute(() -> old.delete()); updateButtons(controller.phase());
                    status("PNG/JPEG 已导入；等比居中白底，尚未发送"); log("导入图片画布=" + label.labelWidthDots + "×" + label.height);
                    if (!showingLogs && !showingRaw) showPreview(snapshot);
                });
            } catch (IOException | RuntimeException failure) {
                main.post(() -> {
                    if (isDestroyed() || preparationGeneration.get() != version) return;
                    previewing = false; imageOperation = ""; updateButtons(controller.phase()); status("图片导入失败或取消；保留上一有效图片，请确认文件为有效 PNG/JPEG 且未超限"); log("图片导入失败或取消；未提交");
                });
            } finally { if (candidate != null) candidate.delete(); }
        });
    }
    private void updateButtons(PrinterController.Phase phase) {
        boolean offline = phase == PrinterController.Phase.OFFLINE;
        boolean editDevice = offline && idle();
        for (int id : fieldIds) findViewById(id).setEnabled(editDevice);
        for (int id : new int[]{R.id.label_width, R.id.height, R.id.copies, R.id.trailing_blank, R.id.raw_hex, R.id.raw_copies})
            findViewById(id).setEnabled(idle());
        findViewById(R.id.send_raw).setEnabled(phase == PrinterController.Phase.READY && idle());
        ((TextView)findViewById(R.id.raw_hint)).setText(phase == PrinterController.Phase.READY && idle() ?
            "已连接；发送前请确认命令与份数，无需画布或 FORM" : phase == PrinterController.Phase.OFFLINE ?
            "请返回打印页，选择设备并手动连接" : "当前操作进行中；完成后才能发送 Raw");
        for (int id : new int[]{R.id.transport, R.id.print_format, R.id.write_mode, R.id.ble_profile, R.id.connect, R.id.discover})
            findViewById(id).setEnabled(editDevice);
        findViewById(R.id.discover).setEnabled(offline && (scanning || editDevice));
        updateDiscoveryButton();
        for (int id : new int[]{R.id.service, R.id.characteristic, R.id.write_mode})
            findViewById(id).setEnabled(editDevice && choice(R.id.ble_profile) != 0);
        findViewById(R.id.settings).setEnabled(!pendingImportAfterDisconnect && !imagePickerOpen);
        findViewById(R.id.disconnect).setEnabled(phase == PrinterController.Phase.CONNECTING ||
            phase == PrinterController.Phase.READY || phase == PrinterController.Phase.PRINTING || previewing);
        findViewById(R.id.print).setEnabled(phase == PrinterController.Phase.READY);
        findViewById(R.id.media).setEnabled(editDevice || (phase == PrinterController.Phase.READY && controller.supportsMedia() && idle()));
        findViewById(R.id.apply_media).setEnabled(phase == PrinterController.Phase.READY && controller.supportsMedia() && idle());
        findViewById(R.id.locate_label).setEnabled(phase == PrinterController.Phase.READY && controller.supportsLabelPositioning() && idle());
        findViewById(R.id.form).setEnabled(idle() && !receipt());
        findViewById(R.id.x_offset).setEnabled(idle() && !receipt() && !centeredOrigin());
        findViewById(R.id.origin_mode).setEnabled(idle() && !receipt());
        findViewById(R.id.origin_adjustment).setEnabled(idle() && centeredOrigin());
        findViewById(R.id.m90_preset).setEnabled(editDevice);
        boolean canCancel = phase == PrinterController.Phase.PRINTING || previewing;
        findViewById(R.id.action_bar).setVisibility((showingLogs || showingRaw) && !canCancel ? View.GONE : View.VISIBLE);
        findViewById(R.id.cancel).setEnabled(canCancel);
        findViewById(R.id.cancel).setVisibility(canCancel ? View.VISIBLE : View.GONE);
        findViewById(R.id.preview).setEnabled(idle());
        findViewById(R.id.go_canvas).setVisibility(phase == PrinterController.Phase.READY ? View.VISIBLE : View.GONE);
        findViewById(R.id.go_canvas).setEnabled(idle());
        findViewById(R.id.import_image).setEnabled(idle());
        findViewById(R.id.builtin_image).setEnabled(offlineIdle() && sourceFile != null);
        updateConfigMenu();
        findViewById(R.id.export).setEnabled(idle()); findViewById(R.id.clear_log).setEnabled(idle());
        findViewById(R.id.print).setEnabled(phase == PrinterController.Phase.READY && idle() && currentImage() &&
            connectedConfig != null && connectedConfig.matches(prepared.config));
        updatePresentation(phase);
    }
    private void permissions(Runnable action, boolean scan) {
        if (!started || isFinishing() || isDestroyed() || permissionAction != null) return;
        ArrayList<String> missing = new ArrayList<>();
        String[] required = Build.VERSION.SDK_INT >= 31 ? new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN} :
            scan ? new String[]{Manifest.permission.ACCESS_FINE_LOCATION} : new String[0];
        for (String permission : required) if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) missing.add(permission);
        if (missing.isEmpty()) { action.run(); return; }
        permissionAction = action; permissionRequest = nextPermissionRequest;
        nextPermissionRequest = nextPermissionRequest == 65535 ? PERMISSIONS : nextPermissionRequest + 1;
        requestPermissions(missing.toArray(new String[0]), permissionRequest);
    }
    @Override public void onRequestPermissionsResult(int request, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(request, permissions, grants);
        if (request != permissionRequest) return;
        Runnable action = permissionAction; permissionAction = null;
        if (action == null || !started || isFinishing() || isDestroyed()) return;
        boolean granted = grants.length != 0;
        for (int grant : grants) granted &= grant == PackageManager.PERMISSION_GRANTED;
        if (granted) action.run();
        else error(new IllegalArgumentException("蓝牙权限未授予；可在系统应用设置中授权后重试"));
    }
    private void discover() {
        if (!started || !offlineIdle()) return;
        stopDiscovery(false);
        long generation = discoveryGeneration;
        try {
            DemoDiscovery source = discoveryFactory.create(); discovery = source;
            if (!ble()) { showDevices(source.bondedDevices(), generation); return; }
            scanning = true; updateButtons(controller.phase()); status(SCANNING_MESSAGE);
            source.scanBle((devices, code) -> {
                if (!activeDiscovery(generation) || discovery != source || !scanning) return;
                scanning = false;
                updateButtons(controller.phase());
                if (code == 1005) { scanStopped(); return; }
                if (code != 0) { error(new PrinterException(code)); return; }
                status("扫描完成，发现 " + devices.size() + " 台设备"); showDevices(devices, generation);
            });
        } catch (RuntimeException failure) { scanning = false; updateButtons(controller.phase()); error(failure); }
    }
    private boolean activeDiscovery(long generation) {
        return generation == discoveryGeneration && started && !isDestroyed() && !isFinishing() &&
            controller.phase() == PrinterController.Phase.OFFLINE;
    }
    private void updateDiscoveryButton() {
        ((Button)findViewById(R.id.discover)).setText(scanning ? R.string.stop_discovery :
            ble() ? R.string.scan_ble : R.string.paired_devices);
    }
    private void scanStopped() { status("扫描已停止；未连接"); log("扫描已停止；未连接"); }
    private void stopDiscovery(boolean explicit) {
        ++discoveryGeneration;
        boolean wasScanning = scanning; scanning = false;
        if (devicePicker != null) { devicePicker.dismiss(); devicePicker = null; }
        DemoDiscovery stopped = discovery; discovery = null;
        if (stopped != null) stopped.stop();
        if (controller != null) updateButtons(controller.phase());
        if (wasScanning && (explicit || (!started &&
            SCANNING_MESSAGE.contentEquals(((TextView)findViewById(R.id.status)).getText())))) scanStopped();
    }
    private void showDevices(List<DemoDiscovery.Device> devices, long generation) {
        if (!activeDiscovery(generation)) return;
        boolean selectionPresent = false;
        String selectedAddress = text(R.id.address);
        for (DemoDiscovery.Device device : devices)
            if (device.address.equalsIgnoreCase(selectedAddress)) selectionPresent = true;
        if (!selectionPresent && !selectedAddress.isEmpty()) {
            ((EditText)findViewById(R.id.address)).setText(""); selectedDeviceName = null;
            updateButtons(controller.phase());
        }
        if (devices.isEmpty()) {
            status("未找到设备，请检查蓝牙、打印机电源与配对状态后重新选择");
            log("未找到设备；Android 11 及以下 BLE 扫描还需开启定位服务"); return;
        }
        String[] labels = new String[devices.size()];
        for (int i = 0; i < devices.size(); i++) {
            DemoDiscovery.Device device = devices.get(i);
            labels[i] = (device.name == null ? "未命名设备" : device.name) + "\n" + device.address;
        }
        AlertDialog picker = new AlertDialog.Builder(this).setTitle("选择设备（不自动判定型号）")
            .setItems(labels, (dialog, index) -> {
                if (!activeDiscovery(generation) || scanning || devicePicker != dialog) return;
                DemoDiscovery.Device selected = devices.get(index);
                ((EditText)findViewById(R.id.address)).setText(selected.address);
                selectedDeviceName = selected.name == null ? "未命名设备" : selected.name;
                updateButtons(controller.phase());
                status("已选择设备，请确认打印参数");
                log("已选择设备；地址不写入导出日志");

            }).setNegativeButton("取消", null).create();
        devicePicker = picker;
        picker.setOnDismissListener(dialog -> { if (devicePicker == picker) devicePicker = null; });
        picker.show();
    }
    private void export() {
        if (!idle()) return;
        utilityBusy = true; updateButtons(controller.phase());
        images.execute(() -> {
            try {
                String snapshot = journal.snapshot().get();
                main.post(() -> {
                    if (isDestroyed()) return;
                    if (!started) { exportText = null; utilityFinished("已离开页面；未打开日志保存选择器"); return; }
                    utilityBusy = false; updateButtons(controller.phase()); exportText = snapshot;
                    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("text/plain")
                        .addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE, "printer-demo-log.txt");
                    startActivityForResult(intent, EXPORT);
                });
            } catch (InterruptedException | ExecutionException failure) { main.post(() -> utilityFinished("日志快照失败")); }
        });
    }
    private void clearLogs() {
        if (!idle()) return;
        utilityBusy = true; updateButtons(controller.phase());
        images.execute(() -> {
            try { journal.clear().get(); main.post(() -> {
                if (isDestroyed()) return;
                ++logInteractionGeneration; pendingLogAnchor = null; followingLogs = true;
                lines.clear(); renderLogs(); updateLogLatest();
                utilityFinished(journal.degraded() ? "内存日志已清除；磁盘清除失败" : "日志已清除");
            }); } catch (InterruptedException | ExecutionException failure) { main.post(() -> utilityFinished("日志清除失败")); }
        });
    }
    private void utilityFinished(String message) {
        if (isDestroyed()) return;
        utilityBusy = false; updateButtons(controller.phase()); status(message);
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == IMPORT) {
            imagePickerOpen = false;
            if (result == RESULT_OK && data != null && data.getData() != null) importImage(data.getData());
            else { status("已取消导入；保留上一有效图片，未发送"); updateButtons(controller.phase()); }
            return;
        }
        if (request != EXPORT || result != RESULT_OK || data == null || data.getData() == null || exportText == null) { exportText = null; return; }
        String snapshot = exportText; exportText = null; android.net.Uri destination = data.getData();
        images.execute(() -> {
            try (OutputStream output = getContentResolver().openOutputStream(destination, "wt")) {
                if (output == null) throw new IOException("No output stream"); output.write(snapshot.getBytes(StandardCharsets.UTF_8));
                main.post(() -> { if (!isDestroyed()) log("日志已导出"); });
            } catch (IOException | SecurityException failure) { main.post(() -> { if (!isDestroyed()) utilityFinished("日志导出失败"); }); }
        });
    }
    private DemoSettings readSettings() {
        LabelConfig label = config(); if (ble()) bleOptions();
        Map<String, Object> values = new LinkedHashMap<>(); values.put("version", 3);
        for (int id : new int[]{R.id.dpi, R.id.width, R.id.label_width, R.id.height, R.id.copies, R.id.trailing_blank})
            values.put(getResources().getResourceEntryName(id), number(id, getResources().getResourceEntryName(id)));
        values.put("x_offset", label.xOffsetDots);
        values.put("origin_mode", receipt() ? OriginPlacement.MANUAL : choice(R.id.origin_mode));
        values.put("origin_adjustment", centeredOrigin() ? number(R.id.origin_adjustment, "居中校准偏移") : 0);
        values.put("form", receipt() ? null : choice(R.id.form)); values.put("print_format", choice(R.id.print_format));
        values.put("media", choice(R.id.media)); values.put("transport", choice(R.id.transport));
        boolean generic = ble() && choice(R.id.ble_profile) == 1;
        values.put("ble_profile", ble() ? choice(R.id.ble_profile) : null);
        values.put("write_mode", generic ? choice(R.id.write_mode) : null);
        values.put("mtu", ble() ? number(R.id.mtu, "MTU") : null); values.put("payload", ble() ? number(R.id.payload, "payload") : null);
        values.put("service", generic ? text(R.id.service) : null); values.put("characteristic", generic ? text(R.id.characteristic) : null);
        return new DemoSettings(values);
    }
    private void applySettings(DemoSettings loaded) {
        applyingFields = true;
        try {
            for (int id : fieldIds) {
                Object value = loaded.values.get(getResources().getResourceEntryName(id));
                ((EditText)findViewById(id)).setText(value == null ? "" : value.toString());
            }
            for (int id : new int[]{R.id.transport, R.id.form, R.id.write_mode, R.id.ble_profile, R.id.print_format, R.id.media, R.id.origin_mode}) {
                Object value = loaded.values.get(getResources().getResourceEntryName(id));
                ((Spinner)findViewById(id)).setSelection(value == null ? 0 : ((Number)value).intValue());
            }
            connectedConfig = null; selectedDeviceName = null; prepared = null; preparationGeneration.incrementAndGet();
            clearThumbnail(); clearFieldErrors();
            captureChoices();
        } finally { applyingFields = false; }
        transportChanged();
    }
    private void settingsAction(int action) {
        if (!offlineIdle()) return;
        if (action == 3) { loadLegacy(); return; }
        final DemoSettings snapshot;
        try { snapshot = action == 0 ? readSettings() : null; }
        catch (RuntimeException failure) { error(failure); return; }
        utilityBusy = true; updateButtons(controller.phase());
        images.execute(() -> {
            try {
                DemoSettings loaded = null;
                if (action == 0) settings.save(snapshot);
                else if (action == 1) loaded = settings.load();
                else if (action == 2) settings.clear();
                DemoSettings accepted = loaded;
                main.post(() -> {
                    if (isDestroyed()) return;
                    if (!started) { utilityFinished("参数操作已停止；当前参数未改变"); return; }
                    if (accepted != null) applySettings(accepted);
                    utilityFinished(action == 0 ? "参数已手动保存（不含设备地址）" : action == 2 ? "已清除保存的参数" :
                        "参数已加载；请选择设备并更新预览，未连接或发送");
                    log(action == 0 ? "参数已保存" : action == 2 ? "保存参数已清除" : "参数已手动加载；无设备 IO");
                });
            } catch (IOException | RuntimeException failure) {
                main.post(() -> utilityFinished("参数操作失败；未改变当前参数"));
            }
        });
    }
    private void loadLegacy() {
        utilityBusy = true; updateButtons(controller.phase());
        images.execute(() -> {
            Map<String, ?> old = new LinkedHashMap<>(getPreferences(MODE_PRIVATE).getAll());
            main.post(() -> {
                if (isDestroyed()) return;
                if (!started) { utilityFinished("旧参数加载已停止；当前参数未改变"); return; }
                if (!old.containsKey("label_width")) {
                    utilityFinished("旧参数缺少独立 label_width；请手动填写后保存，当前参数未改变"); return;
                }
                if (old.containsKey("media")) { loadLegacyConfirmed(old, null); return; }
                new AlertDialog.Builder(this).setTitle("旧参数未保存介质；请选择本次介质")
                    .setSingleChoiceItems(new String[]{"间隙纸", "黑标纸", "连续纸"}, -1, (dialog, selected) -> {
                        dialog.dismiss(); loadLegacyConfirmed(old, selected);
                    }).setNegativeButton("取消", (dialog, which) -> utilityFinished("未加载旧参数；当前参数未改变"))
                    .setOnCancelListener(dialog -> utilityFinished("未加载旧参数；当前参数未改变")).show();
            });
        });
    }
    private void loadLegacyConfirmed(Map<String, ?> old, Integer media) {
        images.execute(() -> {
            try {
                DemoSettings accepted = media == null ? DemoSettings.legacy(old) : DemoSettings.legacy(old, media);
                main.post(() -> {
                    if (isDestroyed()) return;
                    if (!started) { utilityFinished("旧参数加载已停止；当前参数未改变"); return; }
                    applySettings(accepted); utilityFinished("旧参数已加载；请更新预览并手动保存，未连接或发送");
                    log("旧参数已显式加载；无设备 IO");
                });
            } catch (RuntimeException failure) {
                main.post(() -> utilityFinished("旧参数缺少必要字段或数值无效；当前参数未改变，请手动确认后保存"));
            }
        });
    }
    @Override protected void onStart() {
        super.onStart(); started = true;
        if (controller != null) updateButtons(controller.phase());
        if (showingLogs) renderLogs();
    }
    @Override protected void onStop() {
        dismissConfigMenu();
        cancelLogPositioning();
        pendingImportAfterDisconnect = false;
        started = false; preparationGeneration.incrementAndGet(); previewing = false; imageOperation = ""; permissionAction = null;
        stopDiscovery(false);
        controller.disconnect(); // Foreground-only demo; leaving never resumes a print.
        super.onStop();
    }
    @Override protected void onDestroy() {
        started = false; permissionAction = null;
        stopDiscovery(false);
        dismissConfigMenu();
        cancelLogPositioning();
        pendingImportAfterDisconnect = false; imagePickerOpen = false;
        if (unregisterLogBack != null) { unregisterLogBack.run(); unregisterLogBack = null; }
        controller.close(); preparationGeneration.incrementAndGet();
        clearThumbnail();
        File source = sourceFile; if (source != null) images.execute(() -> source.delete());
        super.onDestroy();
    }
}
