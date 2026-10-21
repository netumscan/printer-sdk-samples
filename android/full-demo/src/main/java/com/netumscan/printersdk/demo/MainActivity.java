// SPDX-License-Identifier: MIT
package com.netumscan.printersdk.demo;

import android.app.*;
import android.bluetooth.BluetoothAdapter;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.text.*;
import android.view.*;
import android.widget.*;
import com.netumscan.printersdk.*;
import java.util.*;

/**
 * 完整 Demo 的界面入口：绑定控件、读取参数、处理权限和系统文件选择器。
 * SDK 操作统一交给 DemoModel，图片准备交给 DemoImages；Activity 不直接执行打印机 IO。
 */
public final class MainActivity extends Activity {
    private static final int PERMISSION = 10, IMPORT = 11, EXPORT = 12;
    private static final int[] FIELDS = {R.id.address, R.id.service, R.id.characteristic, R.id.mtu,
        R.id.payload, R.id.label_width, R.id.height, R.id.copies,
        R.id.x_offset, R.id.origin_adjustment, R.id.trailing_blank};
    private static final int[] CHOICES = {R.id.transport, R.id.ble_profile, R.id.write_mode,
        R.id.form, R.id.media, R.id.pixel_example};
    private DemoOwner owner;
    private DemoModel model;
    private final Runnable listener = this::changed;
    private boolean started, applying, syncingCopies, showingLogs, showingRaw;
    // 只有用户本次明确发起的发现/预览完成后才显示弹窗，普通重绘不会重复打开。
    private boolean requestedDiscovery, requestedPreview;
    private DemoLogView logView;
    private DemoPrintRequestsView printRequestsView;
    private DemoDeviceView deviceView;
    private DemoSections sections;
    private DemoResult<?> discoveryBefore;
    private long previewBefore;
    private Runnable afterPermission, unregisterBack;
    private AlertDialog devicePicker, previewDialog, utilityDialog;
    private ImageView previewImage;
    private PopupMenu configMenu;
    private int hintTarget;
    private PrinterProfile displayedProfile;
    private DemoPrintDraft printDraft = new DemoPrintDraft();

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        setContentView(R.layout.activity_main);
        Object retained = getLastNonConfigurationInstance();
        // 配置重建复用同一个 Owner/Manager；新进程只恢复表单，不恢复连接或旧请求。
        owner = retained instanceof DemoOwner ? (DemoOwner)retained : new DemoOwner(new PrinterManager(getApplicationContext()), this);
        model = owner.model;
        applyWindowInsets();
        initializeFields(saved, retained instanceof DemoOwner);
        logView = new DemoLogView(this, owner.lines);
        printRequestsView = new DemoPrintRequestsView(this, model);
        deviceView = new DemoDeviceView(this);
        sections = new DemoSections(this, saved, this::render);
        bindInputChanges();
        bindConnectionActions();
        bindPrintActions();
        bindDiagnostics();
        bindUtilities();
        bindNavigation();
        if (saved != null) showPage(saved.getBoolean("logs"), saved.getBoolean("raw"));
        render();
    }

    private void applyWindowInsets() {
        View root = findViewById(R.id.root);
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
            root.setOnApplyWindowInsetsListener((view, insets) -> {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout() | WindowInsets.Type.ime());
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom); return insets;
            });
        }
    }

    private void initializeFields(Bundle saved, boolean retained) {
        // 批量初始化期间抑制输入联动，避免程序赋值被当作用户编辑。
        applying = true;
        spinner(R.id.transport, "Bluetooth SPP（需配对）", "Bluetooth BLE");
        spinner(R.id.ble_profile, "M90 V1.2 令牌流控", "通用 BLE（自定义 UUID）");
        spinner(R.id.write_mode, "请选择 BLE 写入方式", "Write With Response", "Write Without Response");
        spinner(R.id.print_format, "CPCL 标签整图", "ESC/POS 小票整图（无切纸）");
        spinner(R.id.form, "请选择", "发送 FORM", "不发送 FORM");
        spinner(R.id.media, "间隙纸", "黑标纸", "连续纸");
        spinner(R.id.origin_mode, "手动填写横向起点", "自动居中（按整字节宽度）");
        spinner(R.id.pixel_example, getResources().getStringArray(R.array.pixel_examples));
        BleOptions preset = M90DevelopmentPreset.bleOptions();
        for (int id : FIELDS) edit(id).setText(id == R.id.copies ? "1" : id == R.id.x_offset ||
            id == R.id.origin_adjustment || id == R.id.trailing_blank ? "0" : id == R.id.mtu ? "517" : id == R.id.payload ? "512" : "");
        edit(R.id.service).setText(preset.service.toString()); edit(R.id.characteristic).setText(preset.writeCharacteristic.toString());
        select(R.id.write_mode, 2);
        mirrorPrintDraft();
        if (saved != null) restoreDraft(saved.getBundle("inputs"), retained);
        if (retained && model.state().selectedID != null)
            edit(R.id.address).setText(model.state().selectedID);
        applying = false;
    }

    private void bindInputChanges() {
        for (int id : FIELDS) edit(id).addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence text, int start, int count, int after) { }
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                if (applying) return;
                edit(id).setError(null);
                if (id == R.id.copies && !syncingCopies) {
                    syncingCopies = true; edit(R.id.raw_copies).setText(text); syncingCopies = false;
                }
                if (owner.images.state().busy) owner.images.cancel();
                render();
            }
            public void afterTextChanged(Editable value) { }
        });
        for (int id : CHOICES) {
            Spinner field = findViewById(id);
            field.setTag(field.getSelectedItemPosition());
            field.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                public void onItemSelected(AdapterView<?> parent, View view, int position, long rowId) {
                    if (Objects.equals(field.getTag(), position)) return;
                    field.setTag(position);
                    if (!applying) {
                        if (owner.images.state().busy) owner.images.cancel();
                        if (id == R.id.transport || id == R.id.ble_profile) { model.clearCandidates(); requestedDiscovery = false; }
                        render();
                    }
                }
                public void onNothingSelected(AdapterView<?> parent) { }
            });
        }
    }

    private void bindConnectionActions() {
        // 连接仅校验地址与传输参数，不要求先填写打印尺寸或导入图片。
        findViewById(R.id.connect).setOnClickListener(view -> withInput(() -> {
            ConnectionOptions options = connectionOptions();
            permissions(() -> { owner.notice = null; owner.connectionNumber++; model.connect(options, selectedMedia()); },
                ble() ? DemoPermissions.Operation.BLE_CONNECT : DemoPermissions.Operation.SPP_CONNECT);
        }));
        findViewById(R.id.disconnect).setOnClickListener(view -> {
            owner.abandonImageFlow(); requestedPreview = false;
            owner.images.cancel(); owner.notice = null; model.disconnect();
        });
        findViewById(R.id.discover).setOnClickListener(view -> {
            if (model.state().discovering) { model.stopDiscovery(); return; }
            permissions(() -> {
                owner.notice = null; requestedDiscovery = true;
                discoveryBefore = model.state().discoveryResult; model.discover(ble());
            }, ble() ? DemoPermissions.Operation.BLE_DISCOVERY : DemoPermissions.Operation.SPP_DISCOVERY);
        });
        findViewById(R.id.settings).setOnClickListener(view -> startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)));
        findViewById(R.id.m90_preset).setOnClickListener(view -> applyPreset());
        findViewById(R.id.apply_print_profile).setOnClickListener(view -> showProfileEditor());
        findViewById(R.id.retry_initialize).setOnClickListener(view -> model.retryInitialize());
    }

    private void bindPrintActions() {
        findViewById(R.id.preview).setOnClickListener(view -> withInput(() -> {
            if (!idle()) return;
            requestedPreview = true; previewBefore = owner.images.state().previewRevision;
            owner.notice = null; owner.images.preview(readConfig());
        }));
        findViewById(R.id.print).setOnClickListener(view -> withInput(() -> {
            // 只提交图片来源与打印参数；文件解码和任务构建在 SDK 后台准备回调中执行。
            LabelConfig config = readConfig();
            owner.notice = null;
            DemoPixelSamples.Example example = pixelExample();
            if (example == null) model.print(owner.images.source(), config);
            else model.printSample(example, config);
        }));
        findViewById(R.id.print_fifo).setOnClickListener(view -> withInput(() -> {
            owner.notice = null; model.printBatch(owner.images.source(), readConfig(), pixelExample());
        }));
        findViewById(R.id.cancel).setOnClickListener(view -> {
            requestedPreview = false;
            // 图片准备与 SDK 请求属于不同生命周期，各自走所属对象的取消入口。
            if (owner.images.state().busy) owner.images.cancel(); else model.cancel();
        });
        findViewById(R.id.import_image).setOnClickListener(view -> withInput(this::chooseImage));
        findViewById(R.id.builtin_image).setOnClickListener(view -> withInput(() -> {
            if (offlineIdle()) { owner.notice = null; owner.images.useBuiltin(); }
        }));
        findViewById(R.id.apply_media).setOnClickListener(view -> withInput(() -> {
            requireCpcl(); owner.notice = null;
            model.setMedia(selectedMedia());
        }));
        findViewById(R.id.locate_label).setOnClickListener(view -> withInput(() -> {
            requireCpcl(); owner.notice = null; model.feed();
        }));
    }

    private void bindDiagnostics() {
        findViewById(R.id.query_status).setOnClickListener(view -> { owner.notice = null; model.queryStatus(receipt()); });
        findViewById(R.id.capabilities).setOnClickListener(view -> { owner.notice = null; model.capabilities(); });
        findViewById(R.id.send_raw).setOnClickListener(view -> withInput(() -> {
            RawCommand command = RawCommand.parse(edit(R.id.raw_hex).getText().toString(), number(R.id.copies));
            owner.notice = null; model.sendRaw(command.copyBytes(), command.copies);
        }));
        bindRaw();
    }

    private void bindUtilities() {
        findViewById(R.id.more_button).setOnClickListener(view -> showMenu());
        findViewById(R.id.export).setOnClickListener(view -> exportLogs());
        findViewById(R.id.clear_log).setOnClickListener(view -> clearLogs());
        findViewById(R.id.form_help_button).setOnClickListener(view -> help(R.string.form_help_title, R.string.form_help));
        findViewById(R.id.media_help_button).setOnClickListener(view -> help(R.string.media_help_title, R.string.media_help));
    }

    private void bindNavigation() {
        findViewById(R.id.transport_details_toggle).setOnClickListener(view -> {
            View panel = findViewById(R.id.transport_details); boolean expand = panel.getVisibility() != View.VISIBLE;
            panel.setVisibility(expand ? View.VISIBLE : View.GONE);
            ((Button)view).setText(expand ? R.string.transport_details_collapse : R.string.transport_details);
        });
        findViewById(R.id.go_canvas).setOnClickListener(view -> scrollTo(R.id.canvas_section_heading));
        findViewById(R.id.print_hint).setOnClickListener(view -> { if (hintTarget != 0) scrollTo(hintTarget); });
        findViewById(R.id.logs_button).setOnClickListener(view -> showPage(true, false));
        findViewById(R.id.logs_back).setOnClickListener(view -> showPage(false, false));
        findViewById(R.id.canvas_panel).addOnLayoutChangeListener((view, left, top, right, bottom, a, b, c, d) -> updateCanvasLayout());
    }
    private EditText edit(int id) { return findViewById(id); }
    private String text(int id) { return edit(id).getText().toString().trim(); }
    private int choice(int id) { return ((Spinner)findViewById(id)).getSelectedItemPosition(); }
    private void select(int id, int position) {
        Spinner field = findViewById(id);
        if (applying) field.setTag(position);
        field.setSelection(position);
    }
    private boolean ble() { return choice(R.id.transport) == 1; }
    private boolean receipt() {
        PrinterProfile bound = model.printProfile();
        return bound == null ? printDraft.receipt() : bound.protocol == PrinterProfile.Protocol.ESC_POS;
    }
    private MediaMode selectedMedia() {
        return choice(R.id.media) == 0 ? MediaMode.GAP : choice(R.id.media) == 1 ? MediaMode.BLACK_MARK : MediaMode.CONTINUOUS;
    }
    private boolean centered() {
        PrinterProfile profile = model.printProfile();
        return profile != null && profile.protocol == PrinterProfile.Protocol.CPCL &&
            profile.labelAlignment == PrinterProfile.LabelAlignment.CENTERED;
    }
    private DemoPixelSamples.Example pixelExample() {
        int position = choice(R.id.pixel_example);
        return position == 0 ? null : DemoPixelSamples.Example.values()[position - 1];
    }
    private int number(int id) {
        try { return Integer.parseInt(text(id)); }
        catch (NumberFormatException failure) { throw new LabelConfig.ValidationError(getResources().getResourceEntryName(id), "请填写有效整数"); }
    }
    private void spinner(int id, String... entries) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, entries);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ((Spinner)findViewById(id)).setAdapter(adapter);
    }
    /** 固化本次打印参数；SDK 负责起点计算，App 不提交已解析的居中绝对起点。 */
    private LabelConfig readConfig() {
        PrinterProfile bound = model.printProfile();
        if (bound == null) bound = printDraft.previewProfile();
        boolean label = bound.protocol == PrinterProfile.Protocol.CPCL;
        boolean manual = label && !centered() &&
            (model.printProfile() != null || printDraft.originMode() == OriginPlacement.MANUAL);
        return new LabelConfig(bound, number(R.id.label_width), number(R.id.height), number(R.id.copies),
            choice(R.id.form), manual ? number(R.id.x_offset) : 0,
            label && model.printProfile() != null ? number(R.id.origin_adjustment) : 0, number(R.id.trailing_blank));
    }
    private BleOptions bleOptions() {
        int mtu = number(R.id.mtu), payload = number(R.id.payload);
        if (choice(R.id.ble_profile) == 0) return LTM90Profile.bleOptions(mtu, payload);
        if (choice(R.id.write_mode) == 0) throw new IllegalArgumentException("请选择 BLE 写入方式");
        return new BleOptions(parseUuid(text(R.id.service)), parseUuid(text(R.id.characteristic)),
            choice(R.id.write_mode) == 1, mtu, payload);
    }
    static UUID parseUuid(String input) {
        String value = input.trim();
        if (value.length() != 36 || !value.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))
            throw new IllegalArgumentException("BLE 需要完整的 36 位服务 / 写特征 UUID");
        return UUID.fromString(value);
    }
    /** SPP 与 BLE 共用连接入口，BLE 的 UUID、MTU 和写入方式由当前表单明确提供。 */
    private ConnectionOptions connectionOptions() {
        String address = text(R.id.address).toUpperCase(Locale.ROOT);
        if (!BluetoothAdapter.checkBluetoothAddress(address)) throw new LabelConfig.ValidationError("address", "请选择已配对设备或填写有效蓝牙地址");
        DiscoveredDevice selected = null;
        for (DiscoveredDevice candidate : model.state().candidates)
            if (candidate.address.equalsIgnoreCase(address) && candidate.address.equals(model.state().selectedID)) selected = candidate;
        if (ble() && choice(R.id.ble_profile) == 0 && !owner.m90BleExplicit &&
            PrinterProfiles.resolveBleOptions(selected == null ? null : selected.name, null) == null)
            throw new LabelConfig.ValidationError("service", "SDK 未匹配 BLE 通道；请在高级设置中选择通用 BLE 并填写参数，或显式应用 M90 BLE 通信预设");
        ConnectionOptions options = ble()
            ? selected == null ? ConnectionOptions.ble(address, bleOptions()) : ConnectionOptions.ble(selected, bleOptions())
            : selected == null ? ConnectionOptions.spp(address) : ConnectionOptions.spp(selected);
        return options;
    }
    /** 连接默认 Profile 只在无设备请求时显式更新，主表显示绑定值。 */
    private void showProfileEditor() {
        if (!idle() || !model.state().canWork()) return;
        PrinterProfile draft = null;
        try { draft = printDraft.editorProfile(); }
        catch (IllegalArgumentException ignored) { /* 旧草稿不完整时由用户在对话框重新填写。 */ }
        showUtilityDialog(deviceView.profileEditor(model.printProfile(), draft, profile -> {
            // 只有成功采用的配置才由 render 回填；失败不覆盖旧设备配置。
            model.configureProfile(profile, selectedMedia());
        }));
    }
    private void requireCpcl() { if (receipt()) throw new IllegalArgumentException("请先选择 CPCL；ESC/POS 不使用此介质命令"); }
    /** 同时检查前台、SDK 请求、图片准备和文件操作，避免互相覆盖正在使用的输入。 */
    private boolean idle() {
        return started && model.state().canEditDocument() && !owner.images.state().busy && !owner.utilityBusy &&
            !owner.importAfterDisconnect && !owner.pickerOpen;
    }
    private boolean offlineIdle() { return idle() && model.state().phase == DemoUiState.Phase.OFFLINE; }
    /** 接收状态变化并完成此前的显式操作；文件结果暂存在 Owner，避免重建期间丢失。 */
    private void changed() {
        if (!started || isDestroyed()) return;
        if (owner.takeLogsCleared()) logView.clear();
        owner.recordResults(); render();
        if (owner.pendingSettings != null) {
            DemoSettings settings = owner.pendingSettings; owner.pendingSettings = null;
            applySettings(settings); message("配置已加载，请重新选择设备");
        }
        if (owner.pendingLegacy != null) {
            Map<String, ?> values = owner.pendingLegacy; owner.pendingLegacy = null;
            confirmLegacy(values);
        }
        acceptImport();
        acceptExport();
        openExportPicker();
        // 必须等断开清理完成后才进入文件选择器，不能用按钮点击或本地标志推断已断开。
        if (owner.importAfterDisconnect && model.state().canConnect()) {
            owner.importAfterDisconnect = false; openImagePicker();
        }
        DemoResult<List<DiscoveredDevice>> found = model.state().discoveryResult;
        if (requestedDiscovery && found != null && found != discoveryBefore && !model.state().discovering) {
            requestedDiscovery = false;
            if (found.raw.outcome == ManagerResult.Outcome.SUCCESS && model.state().canConnect()) showDevices();
        }
        DemoImages.State image = owner.images.state();
        if (requestedPreview && !image.busy) {
            requestedPreview = false;
            if (image.preview != null && image.previewRevision > previewBefore) showPreview(image.preview);
        }
    }
    /** 按当前状态更新控件和提示；渲染本身不提交 SDK 请求，也不触发自动打印。 */
    private void render() {
        if (owner == null) return;
        DemoUiState state = model.state(); DemoImages.State image = owner.images.state();
        PrinterProfile profile = model.printProfile();
        if (profile != null && displayedProfile != profile) {
            displayedProfile = profile;
            boolean previous = applying;
            applying = true;
            try {
                adoptPrintProfile(profile);
            } finally { applying = previous; }
        }
        mirrorPrintDraft();
        boolean idle = idle(), editDevice = offlineIdle();
        boolean pending = owner.utilityBusy || owner.pickerOpen || owner.importAfterDisconnect;
        sections.render();
        ((TextView)findViewById(R.id.connection_status)).setText(getString(R.string.connection_summary,
            state.phase == DemoUiState.Phase.READY ? getString(R.string.connected) :
            state.phase == DemoUiState.Phase.OFFLINE ? getString(R.string.offline) : DemoFormatter.status(state), ble() ? "BLE" : "SPP"));
        String notice = image.busy ? image.operation : image.error != null ? image.error : owner.notice;
        if (notice == null && (state.inputError != null || state.cancelling || state.discovering ||
            state.phase == DemoUiState.Phase.CLOSED)) notice = DemoFormatter.status(state);
        ((TextView)findViewById(R.id.status)).setText(notice);
        findViewById(R.id.status).setVisibility(showingLogs || notice == null ? View.GONE : View.VISIBLE);
        ((TextView)findViewById(R.id.identity)).setText(DemoFormatter.identity(state.identityResult));
        ((TextView)findViewById(R.id.last_result)).setText(DemoFormatter.result(state.latestResult));
        ((TextView)findViewById(R.id.connection_result)).setText(DemoFormatter.connection(state));
        for (int id : FIELDS) findViewById(id).setEnabled(editDevice);
        for (int id : new int[]{R.id.label_width, R.id.height, R.id.copies, R.id.trailing_blank, R.id.raw_hex, R.id.raw_copies})
            findViewById(id).setEnabled(idle);
        for (int id : new int[]{R.id.transport, R.id.ble_profile, R.id.write_mode, R.id.m90_preset}) findViewById(id).setEnabled(editDevice);
        // 旧设置中的设备参数只保留为手动草稿；当前连接只能通过配置对话框更正。
        for (int id : new int[]{R.id.print_format, R.id.dpi, R.id.width}) findViewById(id).setEnabled(false);
        findViewById(R.id.media).setEnabled(idle);
        findViewById(R.id.apply_print_profile).setEnabled(idle && state.canWork());
        findViewById(R.id.apply_print_profile).setVisibility(state.connected &&
            (profile == null || sections.expanded(R.id.device_advanced)) ? View.VISIBLE : View.GONE);
        ((Button)findViewById(R.id.apply_print_profile)).setText(profile == null ? "补充打印配置" : "更正打印配置…");
        findViewById(R.id.retry_initialize).setEnabled(idle && state.canWork() && profile != null);
        findViewById(R.id.retry_initialize).setVisibility(state.connected && profile != null &&
            !model.configured() && state.canWork() ? View.VISIBLE : View.GONE);
        findViewById(R.id.form).setEnabled(idle && !receipt());
        findViewById(R.id.x_offset).setEnabled(idle && !receipt() && !centered() &&
            (profile != null || printDraft.originMode() == OriginPlacement.MANUAL));
        findViewById(R.id.origin_mode).setEnabled(false);
        findViewById(R.id.origin_adjustment).setEnabled(idle && !receipt() &&
            (profile != null || printDraft.originMode() == OriginPlacement.CENTERED));
        findViewById(R.id.ble_fields).setVisibility(ble() ? View.VISIBLE : View.GONE);
        findViewById(R.id.ble_custom_fields).setVisibility(ble() && choice(R.id.ble_profile) == 1 ? View.VISIBLE : View.GONE);
        findViewById(R.id.cpcl_fields).setVisibility(receipt() ? View.GONE : View.VISIBLE);
        findViewById(R.id.media_fields).setVisibility(receipt() ? View.GONE : View.VISIBLE);
        findViewById(R.id.origin_policy_fields).setVisibility(receipt() ? View.GONE : View.VISIBLE);
        findViewById(R.id.origin_adjustment_field).setVisibility(!receipt() &&
            (profile != null || printDraft.originMode() == OriginPlacement.CENTERED) ? View.VISIBLE : View.GONE);
        ((TextView)findViewById(R.id.origin_help)).setText(centered() ? R.string.origin_centered_help : R.string.x_offset_help);
        if (centered()) {
            String origin = "";
            try { origin = Integer.toString(profile.labelOriginXDots(number(R.id.label_width), number(R.id.origin_adjustment))); }
            catch (IllegalArgumentException ignored) { }
            if (!origin.equals(text(R.id.x_offset))) {
                boolean previous = applying; applying = true; edit(R.id.x_offset).setText(origin); applying = previous;
            }
        }
        // 本轮渲染只读取并校验一次打印表单，按钮、摘要和预览状态共用同一不可变参数。
        LabelConfig config;
        int invalidTarget = R.id.label_width;
        try { config = readConfig(); } catch (IllegalArgumentException invalid) {
            config = null;
            if (invalid instanceof LabelConfig.ValidationError) {
                int target = getResources().getIdentifier(((LabelConfig.ValidationError)invalid).field, "id", getPackageName());
                if (target != 0) invalidTarget = target;
            }
        }
        boolean valid = config != null;
        DemoPixelSamples.Example example = pixelExample();
        if (valid && example != null) {
            try { DemoPixelSamples.validate(example, config); }
            catch (IllegalArgumentException invalid) { valid = false; }
        }
        boolean mediaReady = profile != null && (receipt() || model.configured() && state.mediaRequest != null &&
            state.mediaRequest.mode == selectedMedia());
        boolean printReady = started && state.canSubmitPrint() && mediaReady && !image.busy && !pending;
        boolean current = valid && image.prepared != null && image.prepared.matches(config);
        findViewById(R.id.connect).setEnabled(editDevice);
        findViewById(R.id.discover).setEnabled(started && state.canDiscover() && !image.busy && !pending);
        ((Button)findViewById(R.id.discover)).setText(state.discovering ? R.string.stop_discovery : ble() ? R.string.scan_ble : R.string.paired_devices);
        findViewById(R.id.settings).setEnabled(started && !pending);
        findViewById(R.id.disconnect).setEnabled(started && (state.canDisconnect() || image.busy));
        findViewById(R.id.preview).setEnabled(idle && example == null);
        findViewById(R.id.print).setEnabled(printReady && valid);
        ((Button)findViewById(R.id.print)).setText(example == null ? R.string.print_test : R.string.print_pixel_example);
        findViewById(R.id.pixel_example).setEnabled(idle || printReady);
        findViewById(R.id.print_fifo).setEnabled(printReady && valid && state.canSubmitPrints(3));
        printRequestsView.render(state);
        findViewById(R.id.import_image).setEnabled(idle);
        ((Button)findViewById(R.id.import_image)).setText(state.phase == DemoUiState.Phase.READY ? R.string.disconnect_import_image : R.string.import_image);
        findViewById(R.id.builtin_image).setEnabled(editDevice && image.imported);
        boolean cancel = state.canCancel() || image.busy;
        findViewById(R.id.cancel).setEnabled(started && cancel);
        ((Button)findViewById(R.id.cancel)).setText(!image.busy && state.pendingPrintCount() != 0 ? R.string.cancel_earliest_print : R.string.cancel);
        findViewById(R.id.cancel).setVisibility(cancel ? View.VISIBLE : View.GONE);
        findViewById(R.id.action_bar).setVisibility((showingLogs || showingRaw) && !cancel ? View.GONE : View.VISIBLE);
        for (int id : new int[]{R.id.send_raw, R.id.query_status, R.id.capabilities}) findViewById(id).setEnabled(idle && state.canWork());
        findViewById(R.id.apply_media).setEnabled(idle && state.canWork() && profile != null && !receipt());
        findViewById(R.id.apply_media).setVisibility(receipt() ? View.GONE : View.VISIBLE);
        findViewById(R.id.locate_label).setEnabled(idle && state.canWork() && profile != null && !receipt());
        findViewById(R.id.label_motion_group).setVisibility(receipt() ? View.GONE : View.VISIBLE);
        for (int id : new int[]{R.id.export, R.id.clear_log}) findViewById(id).setEnabled(idle);
        findViewById(R.id.go_canvas).setVisibility(state.canWork() ? View.VISIBLE : View.GONE);
        deviceView.render(state, text(R.id.address));
        ((TextView)findViewById(R.id.canvas_summary)).setText(valid ?
            config.labelWidthDots + " × " + config.height + " dots · " + config.copies + " 份" +
                (config.receipt ? "" : " · X" + config.xOffsetDots) :
            state.connected && profile == null ? "请补充打印配置" :
            profile == null ? "连接后设置尺寸" : "请填写尺寸并选择 FORM");
        ((TextView)findViewById(R.id.image_summary)).setText(image.imported ? "已导入 PNG/JPEG" : receipt() ? "内置小票示例" : "内置校准图");
        ((TextView)findViewById(R.id.image_status)).setText(image.busy ? image.operation : current ? "预览已更新" : image.prepared == null ? "尚未预览" : "预览需更新");
        ImageView thumbnail = findViewById(R.id.thumbnail);
        if (example != null) ((TextView)findViewById(R.id.image_status)).setText(getString(R.string.pixel_example_selected,
            ((Spinner)findViewById(R.id.pixel_example)).getSelectedItem()));
        thumbnail.setImageBitmap(current && example == null ? image.thumbnail : null);
        thumbnail.setVisibility(!current || image.thumbnail == null || example != null ? View.GONE : View.VISIBLE);
        ((TextView)findViewById(R.id.raw_hint)).setText(state.canWork() ? "发送前请确认命令与份数" : "请先连接设备");
        String media = receipt() ? "小票无需介质初始化" : "介质设置：未发送";
        if (!receipt() && state.phase == DemoUiState.Phase.READY && state.mediaRequest != null && state.mediaRequest.result != null) {
            MediaMode selected = choice(R.id.media) == 0 ? MediaMode.GAP : choice(R.id.media) == 1 ? MediaMode.BLACK_MARK : MediaMode.CONTINUOUS;
            String name = state.mediaRequest.mode == MediaMode.GAP ? "间隙纸" : state.mediaRequest.mode == MediaMode.BLACK_MARK ? "黑标纸" : "连续纸";
            media = (selected == state.mediaRequest.mode ? "" : "当前选择尚未发送；上次：") +
                name + " · " + DemoFormatter.summary(state.mediaRequest.result);
        }
        ((TextView)findViewById(R.id.media_status)).setText(media);
        hintTarget = state.pendingPrintCount() != 0 ? R.id.print_requests_section : !state.canWork() ?
            state.connected ? R.id.media_status : R.id.discover :
            profile == null ? R.id.apply_print_profile : !valid ? invalidTarget : !mediaReady ? R.id.apply_media : R.id.print;
        ((TextView)findViewById(R.id.print_hint)).setText(image.busy ? image.operation : state.pendingPrintCount() >= 8 ? getString(R.string.print_requests_full) :
            state.pendingPrintCount() != 0 ? getString(R.string.print_requests_pending, state.pendingPrintCount()) :
            !state.canWork() ? state.connected ? "设备处理中…" : "请选择并连接设备" :
            profile == null ? "请补充打印配置" :
            !valid ? "请设置尺寸与纸张参数" : !mediaReady ? "请发送当前介质设置" : "可以打印");
        updateCanvasLayout(); updateMenu(); if (showingLogs) logView.render(started, owner.journal.degraded());
    }
    private void updateCanvasLayout() {
        View panel = findViewById(R.id.canvas_panel);
        if (panel == null || panel.getWidth() == 0) return;
        int widthDp = Math.round(panel.getWidth() / getResources().getDisplayMetrics().density);
        CanvasLayout.apply(findViewById(R.id.root), widthDp, getResources().getConfiguration().fontScale, receipt());
    }
    /** 用户选择候选后直接回填地址；选择不连接，也不自动判定设备型号兼容性。 */
    private void showDevices() {
        List<DiscoveredDevice> devices = model.state().candidates;
        if (devices.isEmpty()) { message(ble() ? "未发现 BLE 设备，请确认打印机电源、蓝牙及扫描权限" : "没有已配对设备，请先打开系统蓝牙设置完成配对"); return; }
        String[] labels = new String[devices.size()];
        for (int i = 0; i < devices.size(); i++) labels[i] = (devices.get(i).name == null ? "未命名设备" : devices.get(i).name) + "\n" + devices.get(i).address;
        devicePicker = new AlertDialog.Builder(this).setTitle("选择设备").setItems(labels, (dialog, index) -> {
            if (!started || !model.state().canConnect()) return;
            DiscoveredDevice selected = devices.get(index);
            if (model.selectCandidate(selected.address)) edit(R.id.address).setText(selected.address);
            message("已选择，请连接"); owner.log("已选择候选设备；未连接");
        }).setNegativeButton("取消", null).create();
        devicePicker.setOnDismissListener(dialog -> devicePicker = null); devicePicker.show();
    }
    /** 完整预览位图仅用于显示；关闭预览不影响图片源和打印。 */
    private void showPreview(Bitmap bitmap) {
        previewImage = new ImageView(this); previewImage.setImageBitmap(bitmap); previewImage.setAdjustViewBounds(true);
        previewImage.setContentDescription("按当前打印参数生成的完整图片预览；尚未发送");
        ScrollView scroll = new ScrollView(this); scroll.addView(previewImage);
        previewDialog = new AlertDialog.Builder(this).setTitle("图片预览").setView(scroll).setPositiveButton("关闭", null).create();
        previewDialog.setOnDismissListener(dialog -> {
            if (previewImage != null) previewImage.setImageDrawable(null);
            previewImage = null; previewDialog = null; owner.images.clearPreview();
        });
        previewDialog.show();
    }
    /** 导入只选择图片源，无需打印尺寸；已连接时先断开，导入后不会自动重连或打印。 */
    private void chooseImage() {
        if (!idle()) return;
        owner.notice = null;
        owner.importAfterDisconnect = true;
        model.disconnect();
    }
    /** 只在前台且离线时打开一次系统选择器，PNG/JPEG 实际解码与限额检查由图片模块执行。 */
    private void openImagePicker() {
        if (!started || !model.state().canConnect() || owner.pickerOpen) return;
        if (!owner.beginImagePicker()) return;
        render();
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE)
            .putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/png", "image/jpeg"});
        try { startActivityForResult(intent, IMPORT); }
        catch (RuntimeException failure) { owner.acceptImageResult(null); message("系统图片选择器不可用；请安装或启用文件应用"); }
    }
    /** 只明确指定 BLE 通信参数；打印配置始终由 SDK 在连接时匹配。 */
    private void applyPreset() {
        if (!offlineIdle()) return;
        applying = true; owner.m90BleExplicit = true;
        select(R.id.ble_profile, 0); select(R.id.write_mode, 2);
        setNumber(R.id.mtu, M90DevelopmentPreset.REQUESTED_MTU);
        setNumber(R.id.payload, M90DevelopmentPreset.MAX_WRITE_BYTES);
        applying = false;
        message("已应用 M90 BLE 通信参数");
        owner.log("显式应用 M90 BLE 通信预设；未连接或发送");
    }
    // 设备参数表单沿用可解析的 ASCII 整数，独立于面向用户的本地化说明文字。
    @android.annotation.SuppressLint("SetTextI18n")
    private void setNumber(int id, int value) {
        edit(id).setText(Integer.toString(value));
    }
    /** 按本次 SDK 操作申请权限，SPP 取消发现与 BLE 扫描都包含 SCAN。 */
    private void permissions(Runnable action, DemoPermissions.Operation operation) {
        if (!started || afterPermission != null) return;
        if (Build.VERSION.SDK_INT < 23) { action.run(); return; }
        String[] required = DemoPermissions.required(Build.VERSION.SDK_INT, operation);
        List<String> missing = new ArrayList<>();
        for (String permission : required) if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) missing.add(permission);
        if (missing.isEmpty()) { action.run(); return; }
        afterPermission = action; requestPermissions(missing.toArray(new String[0]), PERMISSION);
    }
    @Override public void onRequestPermissionsResult(int request, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(request, permissions, grants);
        if (request != PERMISSION) return;
        Runnable action = afterPermission; afterPermission = null;
        if (!started || action == null) return;
        boolean granted = grants.length != 0;
        for (int value : grants) granted &= value == PackageManager.PERMISSION_GRANTED;
        if (granted) action.run(); else message("蓝牙权限未授予；可在系统设置中授权后重试");
    }
    /** Raw 与主页面共用份数；超长编辑整次拒绝，避免截断后意外发送另一条命令。 */
    private void bindRaw() {
        EditText raw = edit(R.id.raw_hex);
        raw.setFilters(new InputFilter[]{(source, start, end, destination, from, to) -> {
            if (destination.length() - (to - from) + end - start <= RawCommand.MAX_CHARACTERS) return null;
            raw.post(() -> raw.setError("Raw 输入最多 65536 个字符；此次输入未应用"));
            return destination.length() == 0 && raw.length() != 0 ? raw.getText().toString() : destination.subSequence(from, to);
        }});
        edit(R.id.raw_copies).setText(text(R.id.copies));
        edit(R.id.raw_copies).addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence text, int start, int count, int after) { }
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                if (syncingCopies) return;
                syncingCopies = true; edit(R.id.copies).setText(text); syncingCopies = false;
            }
            public void afterTextChanged(Editable text) { }
        });
    }
    private void showMenu() {
        configMenu = new PopupMenu(this, findViewById(R.id.more_button));
        configMenu.getMenuInflater().inflate(R.menu.config_actions, configMenu.getMenu());
        configMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.raw_diagnostics) showPage(false, true);
            else if (id == R.id.sdk_diagnostics) showUtilityDialog(new AlertDialog.Builder(this).setTitle("SDK 信息")
                .setMessage(DemoFormatter.identity(model.state().identityResult) + "\n硬件状态未知；请检查实际出纸和设备指示")
                .setPositiveButton("关闭", null).create());
            else if (offlineIdle()) settingsAction(id);
            return true;
        });
        configMenu.setOnDismissListener(menu -> configMenu = null); updateMenu(); configMenu.show();
    }
    private void updateMenu() {
        if (configMenu == null) return;
        Menu menu = configMenu.getMenu();
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            item.setEnabled(item.getItemId() == R.id.raw_diagnostics || item.getItemId() == R.id.sdk_diagnostics ? started : offlineIdle());
        }
    }
    /** 只收集允许保存的打印/传输选项，配置不包含设备地址、Raw 内容、图片或文件 URI。 */
    private DemoSettings readSettings() {
        if (ble()) bleOptions();
        Map<String,Object> values = new LinkedHashMap<>(); values.put("version", 3);
        PrinterProfile draft = printDraft.previewProfile();
        values.put("dpi", draft.dpi); values.put("width", draft.maxPrintWidthDots);
        for (int id : new int[]{R.id.label_width,R.id.height,R.id.copies,R.id.trailing_blank})
            values.put(getResources().getResourceEntryName(id), number(id));
        // 保存用户草稿，而非断开后用于离线预览的临时通用配置；此处不采用或恢复设备 Profile。
        int mode = receipt() ? OriginPlacement.MANUAL : model.printProfile() == null ? printDraft.originMode()
            : centered() ? OriginPlacement.CENTERED : OriginPlacement.MANUAL;
        int calibration = receipt() ? 0 : number(R.id.origin_adjustment);
        int origin = receipt() ? 0 : mode == OriginPlacement.CENTERED
            ? OriginPlacement.resolve(draft.maxPrintWidthDots, number(R.id.label_width), mode, 0, calibration)
            : number(R.id.x_offset);
        values.put("x_offset",origin); values.put("origin_mode",mode);
        values.put("origin_adjustment",calibration); values.put("form",receipt()?null:choice(R.id.form));
        values.put("print_format",receipt()?1:0); values.put("media",choice(R.id.media)); values.put("transport",choice(R.id.transport));
        boolean generic=ble() && choice(R.id.ble_profile)==1;
        values.put("ble_profile",ble()?choice(R.id.ble_profile):null); values.put("write_mode",generic?choice(R.id.write_mode):null);
        values.put("mtu",ble()?number(R.id.mtu):null); values.put("payload",ble()?number(R.id.payload):null);
        values.put("service",generic?text(R.id.service):null); values.put("characteristic",generic?text(R.id.characteristic):null);
        return new DemoSettings(values);
    }
    private void applySettings(DemoSettings settings) {
        applying=true;
        printDraft = printDraft.settings(settings);
        owner.m90BleExplicit = ((Number)settings.values.get("transport")).intValue() == 1 &&
            ((Number)settings.values.get("ble_profile")).intValue() == 0;
        for(int id:FIELDS) {
            Object value=settings.values.get(getResources().getResourceEntryName(id)); edit(id).setText(value==null?"":value.toString());
        }
        for(int id:CHOICES) { Object value=settings.values.get(getResources().getResourceEntryName(id)); select(id,value==null?0:((Number)value).intValue()); }
        mirrorPrintDraft(); applying=false; owner.images.invalidate(); model.clearCandidates(); render();
    }
    /** 保存/加载/清除均由用户显式发起；加载结果经 Owner 交给当前仍有效的 Activity。 */
    private void settingsAction(int action) {
        withInput(() -> {
            if (action == R.id.save_config) owner.saveSettings(readSettings());
            else if (action == R.id.clear_config) owner.clearSettings();
            else if (action == R.id.load_config) owner.loadSettings();
            else if (action == R.id.load_legacy) owner.loadLegacySettings(getLocalClassName());
        });
    }
    private void confirmLegacy(Map<String, ?> values) {
        if(values.containsKey("media")) {
            withInput(() -> { applySettings(DemoSettings.legacy(values)); message("旧参数已加载，请手动保存；未连接或发送"); });
        } else showUtilityDialog(new AlertDialog.Builder(this).setTitle("旧配置未保存介质；请选择")
            .setItems(new String[]{"间隙纸","黑标纸","连续纸"},(dialog,index) -> withInput(() -> {
                applySettings(DemoSettings.legacy(values,index)); message("旧参数已加载，请手动保存；未连接或发送");
            })).setNegativeButton("取消",null).create());
    }
    /** 文件任务及其结果由保留的 Owner 管理，Activity 只消费选择器意图。 */
    private void exportLogs() {
        if (idle()) owner.prepareLogExport();
    }
    private void clearLogs() {
        if (idle()) owner.clearLogs();
    }
    private void openExportPicker() {
        if (!idle() || !owner.takeExportPickerRequest()) return;
        try {
            startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("text/plain")
                .addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE, "printer-demo-log.txt"), EXPORT);
        } catch (RuntimeException failure) {
            owner.acceptLogExport(null); message("系统保存选择器不可用");
        }
    }
    private void showPage(boolean logs,boolean raw) {
        if (showingLogs && !logs) logView.onHide();
        else if (logs && !showingLogs) logView.onShow();
        showingLogs=logs; showingRaw=raw;
        if(configMenu!=null) configMenu.dismiss();
        View focused=getCurrentFocus();
        if(focused!=null) { android.view.inputmethod.InputMethodManager keyboard=(android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
            if(keyboard!=null) keyboard.hideSoftInputFromWindow(focused.getWindowToken(),0); focused.clearFocus(); }
        findViewById(R.id.print_page).setVisibility(logs||raw?View.GONE:View.VISIBLE);
        findViewById(R.id.logs_page).setVisibility(logs?View.VISIBLE:View.GONE); findViewById(R.id.raw_page).setVisibility(raw?View.VISIBLE:View.GONE);
        findViewById(R.id.print_actions).setVisibility(logs||raw?View.GONE:View.VISIBLE); findViewById(R.id.logs_back).setVisibility(logs||raw?View.VISIBLE:View.GONE);
        findViewById(R.id.logs_button).setVisibility(logs?View.GONE:View.VISIBLE);
        ((TextView)findViewById(R.id.app_title)).setText(logs?R.string.logs_title:raw?R.string.raw_title:R.string.screen_title);
        if(unregisterBack!=null) { unregisterBack.run(); unregisterBack=null; }
        if((logs||raw) && Build.VERSION.SDK_INT>=33) unregisterBack=BackNavigation.register(this,() -> showPage(false,false));
        render();
    }
    @Override public void onBackPressed() { if(showingLogs||showingRaw) showPage(false,false); else super.onBackPressed(); }
    @android.annotation.TargetApi(33) private static final class BackNavigation {
        static Runnable register(Activity activity,Runnable back) {
            android.window.OnBackInvokedDispatcher dispatcher=activity.getOnBackInvokedDispatcher();
            android.window.OnBackInvokedCallback callback=back::run;
            dispatcher.registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,callback);
            return () -> dispatcher.unregisterOnBackInvokedCallback(callback);
        }
    }
    private void scrollTo(int id) {
        showPage(false,false);
        boolean channel = id == R.id.address || id == R.id.service || id == R.id.characteristic ||
            id == R.id.ble_profile || id == R.id.write_mode || id == R.id.mtu || id == R.id.payload;
        int destination = (id == R.id.service || id == R.id.characteristic) && choice(R.id.ble_profile) == 0 ? R.id.ble_profile : id;
        View target=findViewById(destination);
        sections.reveal(target);
        if(channel) findViewById(R.id.transport_details).setVisibility(View.VISIBLE);
        // 用 ScrollView 的实际坐标定位；不依赖当前输入框焦点或 OEM 的请求矩形行为。
        target.post(() -> {
            ScrollView scroll = findViewById(R.id.content_scroll);
            android.graphics.Rect rect = new android.graphics.Rect(); target.getDrawingRect(rect);
            scroll.offsetDescendantRectToMyCoords(target, rect);
            scroll.smoothScrollTo(0, Math.max(0, rect.top - Math.round(12 * getResources().getDisplayMetrics().density)));
        });
    }
    private void showUtilityDialog(AlertDialog dialog) {
        if(utilityDialog!=null) utilityDialog.dismiss();
        utilityDialog=dialog; dialog.setOnDismissListener(value -> utilityDialog=null); dialog.show();
    }
    private void help(int title,int body) { showUtilityDialog(new AlertDialog.Builder(this).setTitle(title).setMessage(body).setPositiveButton("关闭",null).create()); }
    private void message(String text) { owner.notice=text; render(); }
    private void withInput(Runnable action) {
        try { action.run(); }
        catch(IllegalArgumentException failure) {
            if(failure instanceof LabelConfig.ValidationError) {
                int id=getResources().getIdentifier(((LabelConfig.ValidationError)failure).field,"id",getPackageName());
                if(id!=0 && findViewById(id) instanceof EditText) { edit(id).setError(failure.getMessage()); scrollTo(id); }
            }
            message(failure.getMessage());
        }
    }
    /** 系统恢复仅保存表单选项；设备地址和 Raw 文本不进入恢复草稿。 */
    private Bundle inputs() {
        Bundle data=new Bundle();
        printDraft.save(data);
        data.putBoolean("m90_ble_explicit", owner.m90BleExplicit);
        for(int id:FIELDS) if(id!=R.id.address) data.putString(getResources().getResourceEntryName(id),edit(id).getText().toString());
        for(int id:CHOICES) data.putInt(getResources().getResourceEntryName(id),choice(id));
        return data;
    }
    /** 恢复手动草稿和通信参数；不恢复或采用打印 Profile。 */
    private void restoreDraft(Bundle data, boolean retained) {
        if (data == null) return;
        boolean previous = applying;
        applying = true;
        try {
            printDraft = printDraft.restored(data);
            restoreInputs(data);
            adoptPrintProfile(model.printProfile());
            mirrorPrintDraft();
            if (!retained) {
                owner.m90BleExplicit = data.getBoolean("m90_ble_explicit");
            }
        } finally { applying = previous; }
    }
    private void restoreInputs(Bundle data) {
        if(data==null) return;
        for(int id:FIELDS) { String key=getResources().getResourceEntryName(id); if(data.containsKey(key)) edit(id).setText(data.getString(key)); }
        for(int id:CHOICES) { String key=getResources().getResourceEntryName(id); if(data.containsKey(key)) select(id,data.getInt(key)); }
    }
    /** 先读取旧草稿的定位来源，再采用当前配置，避免旧居中结果成为手动绝对起点。 */
    private void adoptPrintProfile(PrinterProfile profile) {
        if (profile == null) return;
        if (profile.labelAlignment != PrinterProfile.LabelAlignment.CENTERED &&
            printDraft.originMode() == OriginPlacement.CENTERED) setNumber(R.id.x_offset, 0);
        printDraft = printDraft.adopted(profile);
    }
    /** 兼容 ID 仅作为草稿输出镜像，不从隐藏控件读取或接收输入事件。 */
    private void mirrorPrintDraft() {
        boolean previous = applying; applying = true;
        try {
            edit(R.id.dpi).setText(printDraft.dpiText()); edit(R.id.width).setText(printDraft.widthText());
            select(R.id.print_format, printDraft.receipt() ? 1 : 0);
            select(R.id.origin_mode, printDraft.originMode());
        } finally { applying = previous; }
    }
    @Override protected void onSaveInstanceState(Bundle saved) {
        saved.putBundle("inputs",inputs()); saved.putBoolean("logs",showingLogs); saved.putBoolean("raw",showingRaw);
        sections.save(saved);
        super.onSaveInstanceState(saved);
    }
    /** Framework 恢复控件也会触发监听；配置来源与已绑定 Profile 以 Owner 当前状态为准。 */
    @Override protected void onRestoreInstanceState(Bundle saved) {
        boolean previous = applying;
        applying = true;
        try {
            super.onRestoreInstanceState(saved);
            // Framework 可能重新填入旧 resolved X；显式草稿保留其旧策略，供适配层再次判别。
            restoreDraft(saved.getBundle("inputs"), true);
            // 同步异步选择通知的 tag 与 Spinner 尚待布局的恢复位置。
            for (int id : CHOICES) select(id, choice(id));
            displayedProfile = null;
            render();
        } finally {
            applying = previous;
        }
    }
    /** 文件选择器可能在 onStart 之前返回，先保存结果，待前台与清理条件满足后再消费。 */
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if(request==IMPORT) {
            owner.acceptImageResult(result == RESULT_OK && data != null ? data.getData() : null);
            acceptImport(); render();
        } else if(request==EXPORT) {
            owner.acceptLogExport(result==RESULT_OK && data!=null ? data.getData() : null);
            acceptExport();
        }
    }
    /** 前台且完成连接清理后，只消费一次所选 URI；导入不依赖打印参数或预览。 */
    private void acceptImport() {
        if(!started || owner.importUri==null || !model.state().canConnect()) return;
        Uri uri=owner.importUri; owner.importUri=null;
        owner.images.importImage(uri);
    }
    /** 导出也等待界面恢复空闲，先取走待写数据再排后台任务，防止重入导致重复写入。 */
    private void acceptExport() {
        if (idle()) owner.writeLogExport();
    }
    /** 重绑保留的状态与监听，继续处理显式文件操作，不恢复蓝牙连接或重放打印。 */
    @Override protected void onStart() {
        super.onStart(); started=true; model.foreground(); owner.images.foreground();
        owner.attach(listener); model.attach(listener); owner.images.attach(listener); changed();
    }
    /** 配置重建保留 Owner 并取消尚未展示的完整预览；真正离开前台时断开设备并取消图片准备。 */
    @Override protected void onStop() {
        logView.onStop(showingLogs);
        if(isChangingConfigurations() && requestedPreview) owner.images.cancel();
        started=false; afterPermission=null; requestedDiscovery=false; requestedPreview=false;
        if(devicePicker!=null) devicePicker.dismiss();
        if(previewImage!=null) previewImage.setImageDrawable(null);
        if(previewDialog!=null) previewDialog.dismiss();
        owner.images.clearPreview();
        if(configMenu!=null) configMenu.dismiss();
        if(utilityDialog!=null) utilityDialog.dismiss();
        owner.stopUi(listener,isChangingConfigurations()); super.onStop();
    }
    @Override public Object onRetainNonConfigurationInstance() { return owner; }
    /** 先解除界面对位图和回调的引用；是否最终关闭资源由 Owner 根据重建状态决定。 */
    @Override protected void onDestroy() {
        logView.close();
        if(unregisterBack!=null) unregisterBack.run();
        ((ImageView)findViewById(R.id.thumbnail)).setImageDrawable(null);
        owner.destroyUi(listener,isChangingConfigurations()); super.onDestroy();
    }
}
