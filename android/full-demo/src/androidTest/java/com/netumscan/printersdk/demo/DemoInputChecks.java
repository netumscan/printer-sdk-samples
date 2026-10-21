// SPDX-License-Identifier: MIT
package com.netumscan.printersdk.demo;

import com.netumscan.printersdk.*;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.concurrent.CancellationException;

/** Actual Demo helpers, usable on the host without JNI or Bluetooth. */
public final class DemoInputChecks {
    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    private static CancellationContext context() throws Exception {
        Constructor<CancellationContext> constructor = CancellationContext.class.getDeclaredConstructor(long.class);
        constructor.setAccessible(true);
        return constructor.newInstance(Long.MAX_VALUE);
    }
    public static void main(String[] args) throws Exception {
        check(Arrays.equals(DemoPermissions.required(31, DemoPermissions.Operation.SPP_CONNECT),
            new String[]{"android.permission.BLUETOOTH_CONNECT", "android.permission.BLUETOOTH_SCAN"}),
            "SPP connection requests SCAN for SDK cancelDiscovery as well as CONNECT");
        check(Arrays.equals(DemoPermissions.required(36, DemoPermissions.Operation.BLE_CONNECT),
            new String[]{"android.permission.BLUETOOTH_CONNECT"}), "BLE connection alone does not request scanning");
        check(Arrays.equals(DemoPermissions.required(30, DemoPermissions.Operation.BLE_DISCOVERY),
            new String[]{"android.permission.ACCESS_FINE_LOCATION"}) &&
            DemoPermissions.required(22, DemoPermissions.Operation.BLE_DISCOVERY).length == 0,
            "Legacy BLE scan runtime permissions preserve API boundaries");
        check(Arrays.equals(RawCommand.parse("1b 40\n0A", 1).copyBytes(), new byte[]{27, 64, 10}), "Raw hex maps to the entered bytes");
        for (String invalid : new String[]{"", "1", "GG", "1B40", "0x1B", "１B"}) {
            try { RawCommand.parse(invalid, 1); throw new AssertionError("Invalid Raw accepted"); }
            catch (IllegalArgumentException expected) { }
        }
        check(Arrays.equals(RawCommand.parse(" 1b\t40\n0A\r0b\f0C\u000b0d\u00a00e\u20030f\u008510 ", 1).copyBytes(),
            new byte[]{27, 64, 10, 11, 12, 13, 14, 15, 16}), "Production Raw parsing accepts Unicode White_Space separators");
        check(RawCommand.parse("1B", 1).copies == 1 && RawCommand.parse("1B", 20).copies == 20, "Explicit copy count boundaries");
        for (int invalid : new int[]{0, -1, 21}) {
            try { RawCommand.parse("1B", invalid); throw new AssertionError("Invalid copy count accepted"); }
            catch (IllegalArgumentException expected) { }
        }
        char[] maximum = new char[RawCommand.MAX_BYTES * 3];
        for (int i = 0; i < maximum.length; i++) maximum[i] = i % 3 == 2 ? ' ' : 'A';
        check(RawCommand.parse(new String(maximum), 1).size() == RawCommand.MAX_BYTES, "4096 Raw bytes accepted");
        try { RawCommand.parse(new String(maximum) + "AA", 1); throw new AssertionError("Oversized Raw bytes accepted"); }
        catch (IllegalArgumentException expected) { }
        char[] boundedText = new char[RawCommand.MAX_CHARACTERS];
        Arrays.fill(boundedText, ' '); boundedText[0] = '1'; boundedText[1] = 'B';
        check(RawCommand.parse(new String(boundedText), 1).size() == 1, "Raw text accepts the production character limit");
        try { RawCommand.parse(new String(boundedText) + " ", 1); throw new AssertionError("Oversized Raw text accepted"); }
        catch (IllegalArgumentException expected) { }
        LabelConfig config = new LabelConfig(203, 384, 129, 128, 2, 1, 31, false, 9);
        byte[] canvas = new byte[config.labelWidthDots * config.height];
        PreparedImage snapshot = new PreparedImage(config, canvas);
        canvas[0] = (byte)255;
        byte[] copy = snapshot.copyPixels(); copy[1] = (byte)255;
        check(snapshot.grayAt(0) == 0 && snapshot.grayAt(1) == 0, "Preview snapshot owns its immutable pixels");
        try (DemoPrintData data = DemoPrintData.pixels(config, snapshot.copyPixels())) {
            check(data.options != null, "Custom label canvas PrintOptions");
        }
        PrinterProfile centeredProfile = LTM90Profile.printProfile();
        LabelConfig exactDensity = new LabelConfig(PrinterProfile.cpcl(203, 864, 12,
            PrinterProfile.LabelAlignment.LEFT), 400, 400, 1, 1, 0, 0, 0);
        check(TestLabel.rulerDots(exactDensity) == 120,
            "The built-in 10 mm ruler uses the adopted 12 dots/mm rather than nominal 203 DPI");
        LabelConfig centered = new LabelConfig(centeredProfile, 400, 400, 1, 1, 0, 0, 0);
        check(TestLabel.rulerDots(centered) == 80, "The LT-M90 10 mm ruler is exactly 80 dots");
        check(centered.xOffsetDots == 232 && centered.manualOriginDots == 0 && centered.printProfile() == centeredProfile,
            "Demo previews SDK positioning without turning X232 into an explicit origin or replacing its Profile");
        check(new LabelConfig(centeredProfile, 401, 400, 1, 1, 0, 0, 0).xOffsetDots == 228 &&
            new LabelConfig(centeredProfile, 400, 400, 1, 1, 0, -3, 0).xOffsetDots == 229 &&
            new LabelConfig(centeredProfile, 400, 400, 1, 1, 0, 3, 0).xOffsetDots == 235,
            "SDK padding and signed calibration are reflected in Demo preview");
        for (int[] origins : new int[][]{{17, 0}, {0, -233}, {0, 233}}) {
            try { new LabelConfig(centeredProfile, 400, 400, 1, 1, origins[0], origins[1], 0);
                throw new AssertionError("Invalid centered configuration accepted"); }
            catch (LabelConfig.ValidationError expected) { }
        }
        try { new LabelConfig(PrinterProfile.cpcl(203, 864), 400, 400, 1, 1, 17, 1, 0);
            throw new AssertionError("Manual origin mixed with calibration"); }
        catch (LabelConfig.ValidationError expected) { }
        LabelConfig receipt = new LabelConfig(180, 384, 128, 128, 3, 0, 0, true, 11);
        try (DemoPrintData data = DemoPrintData.pixels(receipt, new byte[receipt.labelWidthDots * receipt.height])) {
            check(receipt.receiptBudget() == 417 && data.options != null, "Receipt budget includes every copy and trailing blank");
        }
        try { DemoPrintData.pixels(config, new byte[1]); throw new AssertionError("Invalid canvas accepted"); }
        catch (IllegalArgumentException expected) { }
        Constructor<?> constructor = ManagerResult.class.getDeclaredConstructor(RequestId.class,
            ManagerResult.Outcome.class, int.class, String.class, ManagerResult.Completion.class, boolean.class, Object.class);
        constructor.setAccessible(true);
        ManagerResult<?> result = (ManagerResult<?>)constructor.newInstance(null, ManagerResult.Outcome.FAILURE,
            2003, "write failed", ManagerResult.Completion.NONE, true, null);
        DemoResult<?> typed = new DemoResult<>(DemoResult.Kind.PRINT, result);
        check(typed.raw == result && typed.raw.code == 2003 && typed.raw.outputMayHaveOccurred &&
            typed.raw.completion == ManagerResult.Completion.NONE, "Structured result keeps the original SDK fields");
        check(DemoFormatter.result(new DemoResult<>(DemoResult.Kind.PRINT, result)).contains("2003") && DemoFormatter.result(new DemoResult<>(DemoResult.Kind.PRINT, result)).contains("可能已产生输出"),
            "Failures preserve error and possible output");
        result = (ManagerResult<?>)constructor.newInstance(null, ManagerResult.Outcome.SUCCESS,
            0, "", ManagerResult.Completion.DATA_SENT, true, null);
        check(DemoFormatter.result(new DemoResult<>(DemoResult.Kind.PRINT, result)).contains("不代表打印完成"), "DATA_SENT does not claim paper completion");
        Constructor<ConnectionInfo> connectionConstructor = ConnectionInfo.class.getDeclaredConstructor(
            String.class, String.class, PrinterProfile.class, ConnectionInfo.ConfigurationSource.class);
        connectionConstructor.setAccessible(true);
        ConnectionInfo adopted = connectionConstructor.newInstance("AA:BB:CC:DD:EE:01", "private-printer-name",
            LTM90Profile.printProfile(), ConnectionInfo.ConfigurationSource.NAME_RECOMMENDATION);
        result = (ManagerResult<?>)constructor.newInstance(null, ManagerResult.Outcome.SUCCESS,
            0, "", ManagerResult.Completion.NONE, false, adopted);
        String details = DemoFormatter.profileDetails(adopted);
        check(details.contains("source=NAME_RECOMMENDATION") && details.contains("model=LT-M90") &&
            details.contains("protocol=CPCL") && details.contains("dpi=203") && details.contains("maxWidthDots=864") &&
            details.contains("dotsPerMm=8.0") && details.contains("labelAlignment=CENTERED"),
            "Connection diagnostics report the actual adopted Profile and source");
        check(!details.contains(adopted.address) && !details.contains(adopted.deviceName) &&
            DemoFormatter.result(new DemoResult<>(DemoResult.Kind.CONNECT, result)).contains(details),
            "Displayed connection configuration and journal details exclude private device identity");
        ConnectionInfo unknown = connectionConstructor.newInstance("AA:BB:CC:DD:EE:01", null,
            null, ConnectionInfo.ConfigurationSource.NONE);
        check(DemoFormatter.profileDetails(unknown).equals("source=NONE profile=UNKNOWN model=UNKNOWN"),
            "Unknown connection Profile is not presented as a known model");
        java.util.List<DiscoveredDevice> source = new java.util.ArrayList<>();
        DemoUiState state = new DemoUiState(DemoUiState.Phase.OFFLINE, true, false, false, null, null,
            source, null, null, null, null, null, null, null, null, null, null);
        source.add(null);
        check(state.candidates.isEmpty() && state.selectedID == null, "UI state defensively copies candidates without selecting implicitly");
        try { state.candidates.add(null); throw new AssertionError("Mutable UI candidates"); }
        catch (UnsupportedOperationException expected) { }
        check(state.canEditDocument(), "Offline foreground can prepare a document without a printer");
        System.out.println("PASS: Android Manager Demo canvas/Raw inputs and typed results (no JNI)");
    }
}
