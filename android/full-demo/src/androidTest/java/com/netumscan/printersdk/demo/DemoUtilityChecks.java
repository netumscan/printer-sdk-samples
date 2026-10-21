// SPDX-License-Identifier: MIT
package com.netumscan.printersdk.demo;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/** Current Full Demo's configuration, Raw and journal helpers; no printer or JNI. */
public final class DemoUtilityChecks {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    private static void reject(Runnable action) {
        try { action.run(); throw new AssertionError("Invalid input accepted"); }
        catch (IllegalArgumentException expected) { }
    }
    static Map<String, Object> settings() {
        Map<String, Object> values = new LinkedHashMap<>();
        for (String key : DemoSettings.KEYS) values.put(key, null);
        values.put("version", 3); values.put("dpi", 203); values.put("width", 864);
        values.put("label_width", 800); values.put("height", 800); values.put("copies", 2);
        values.put("x_offset", 32); values.put("form", 1); values.put("print_format", 0);
        values.put("media", 0); values.put("transport", 0);
        values.put("origin_mode", OriginPlacement.CENTERED); values.put("origin_adjustment", 0);
        values.put("trailing_blank", 24);
        return values;
    }
    private static void settingsChecks() {
        Map<String, Object> source = settings();
        DemoSettings current = new DemoSettings(source);
        source.put("height", 1200);
        check(current.label.height == 800 && current.label.trailingBlankDots == 24,
            "Saved geometry owns its validated input snapshot");
        try { current.values.put("height", 1000); throw new AssertionError("Mutable settings"); }
        catch (UnsupportedOperationException expected) { }
        for (int version = 1; version <= 3; version++) {
            Map<String, Object> old = settings(); old.put("version", version);
            if (version < 3) old.remove("trailing_blank");
            if (version == 1) { old.remove("origin_mode"); old.remove("origin_adjustment"); }
            DemoSettings upgraded = new DemoSettings(old);
            check(upgraded.values.get("version").equals(3) && upgraded.label.xOffsetDots == 32 &&
                upgraded.label.trailingBlankDots == (version < 3 ? 0 : 24), "Schemas 1, 2 and 3 preserve geometry");
            check(new DemoSettings(upgraded.values).values.equals(upgraded.values), "Current schema round trip");
        }
        for (String key : new String[]{"address", "raw", "image_uri"}) {
            Map<String, Object> extra = settings(); extra.put(key, "private input");
            reject(() -> new DemoSettings(extra));
        }
        for (Object bad : new Object[]{null, "203", 203.5, Double.NaN, Long.MAX_VALUE}) {
            Map<String, Object> invalid = settings(); invalid.put("dpi", bad);
            reject(() -> new DemoSettings(invalid));
        }
        Map<String, Object> calibrated = settings(); calibrated.put("label_width", 400);
        calibrated.put("x_offset", 229); calibrated.put("origin_adjustment", -3);
        DemoSettings calibrationDraft = new DemoSettings(calibrated);
        check(new DemoSettings(calibrationDraft.values).values.get("origin_adjustment").equals(-3) &&
            calibrationDraft.values.get("origin_mode").equals(OriginPlacement.CENTERED) &&
            calibrationDraft.label.xOffsetDots == 229 && calibrationDraft.label.manualOriginDots == 0 &&
            calibrationDraft.label.horizontalCalibrationDots == -3,
            "An offline settings snapshot preserves negative calibration independently of any adopted Profile");
        Map<String, Object> mixed = settings(); mixed.put("origin_mode", OriginPlacement.MANUAL);
        mixed.put("x_offset", 17); mixed.put("origin_adjustment", 1);
        reject(() -> new DemoSettings(mixed));
        for (int invalid : new int[]{-1, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            Map<String, Object> left = settings(); left.put("origin_mode", OriginPlacement.MANUAL);
            left.put("x_offset", 0); left.put("origin_adjustment", invalid);
            reject(() -> new DemoSettings(left));
        }
        Map<String, Object> left = settings(); left.put("origin_mode", OriginPlacement.MANUAL);
        left.put("x_offset", 0); left.put("origin_adjustment", 17);
        DemoSettings leftCalibrated = new DemoSettings(left);
        check(leftCalibrated.label.xOffsetDots == 17 && leftCalibrated.label.manualOriginDots == 0 &&
            leftCalibrated.label.horizontalCalibrationDots == 17 && leftCalibrated.values.get("x_offset").equals(0) &&
            new DemoSettings(leftCalibrated.values).values.equals(leftCalibrated.values),
            "LEFT calibration validates exactly like printing and round-trips without becoming an absolute origin");
        DemoPrintDraft centeredDraft = new DemoPrintDraft().settings(calibrationDraft);
        check(centeredDraft.dpi == 203 && centeredDraft.maxPrintWidthDots == 864 &&
            centeredDraft.originMode() == OriginPlacement.CENTERED && !centeredDraft.receipt(),
            "Typed migration drafts retain geometry and policy without depending on hidden Activity controls");
        Map<String, Object> receipt = settings(); receipt.put("print_format", 1); receipt.put("form", null);
        receipt.put("origin_mode", OriginPlacement.MANUAL); receipt.put("x_offset", 0);
        check(new DemoSettings(receipt).label.receipt, "Receipt settings keep unused FORM null");
        Map<String, Object> legacy = settings(); legacy.put("address", "private identifier");
        legacy.put("raw", "private command"); legacy.put("image_uri", "private image");
        check(!DemoSettings.legacy(legacy).values.containsKey("address") &&
            !DemoSettings.legacy(legacy).values.containsKey("raw") &&
            !DemoSettings.legacy(legacy).values.containsKey("image_uri"), "Legacy migration discards device, Raw and image input");
        legacy.remove("media"); reject(() -> DemoSettings.legacy(legacy));
        check(DemoSettings.legacy(legacy, 2).values.get("media").equals(2), "Legacy media is explicitly selected");
        legacy.remove("label_width"); reject(() -> DemoSettings.legacy(legacy, 2));
    }
    private static void rawChecks() {
        RawCommand command = RawCommand.parse("1b\u00a040\n0A", 2);
        check(command.copies == 2 && command.size() == 3 &&
            Arrays.equals(command.copyBytes(), new byte[]{27, 64, 10}), "Diagnostic Raw accepts bounded hex bytes and copies");
        byte[] bytes = command.copyBytes(); bytes[0] = 0;
        check(command.copyBytes()[0] == 27, "Raw snapshot remains immutable");
        for (String bad : new String[]{"", "1B40", "0x1B", "1G", "１B"}) reject(() -> RawCommand.parse(bad, 1));
        reject(() -> RawCommand.parse("1B", 0)); reject(() -> RawCommand.parse("1B", 21));
        String maximum = repeat("00 ", RawCommand.MAX_BYTES);
        check(RawCommand.parse(maximum, 20).size() == RawCommand.MAX_BYTES, "4096 Raw bytes accepted");
        reject(() -> RawCommand.parse(maximum + "00", 1));
        reject(() -> RawCommand.parse(repeat(" ", RawCommand.MAX_CHARACTERS + 1), 1));
    }
    private static String repeat(String value, int count) {
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < count; index++) result.append(value);
        return result.toString();
    }
    private static final class TestClock implements DemoJournal.Clock {
        long now = 1_800_000_000_000L;
        public long wall() { return now; }
        public long elapsed() { return 1200; }
    }
    private static void journalChecks() throws Exception {
        File directory = File.createTempFile("printer-full-demo-journal-", "");
        if (!directory.delete() || !directory.mkdir()) throw new IOException("Test directory unavailable");
        TestClock clock = new TestClock();
        try {
            try (DemoJournal journal = new DemoJournal(directory, clock)) {
                journal.append("Connected", 2, null).get(5, TimeUnit.SECONDS);
                for (String secret : new String[]{"device_id=private", "raw_payload=1B40", "content://private/photo",
                    "AA:BB:CC:DD:EE:FF", "/storage/emulated/0/private.png"})
                    journal.append(secret, 2, null).get(5, TimeUnit.SECONDS);
                String retained = journal.snapshot().get(5, TimeUnit.SECONDS);
                check(retained.contains("connection=2 seq=1 message=Connected") && retained.contains("诊断内容已隐藏"),
                    "Export keeps stable UTC, session and sequence fields");
                check(!retained.contains("private") && !retained.contains("1B40") && !retained.contains("AA:BB"),
                    "Private content never reaches the journal snapshot");
                String first = retained.substring(retained.indexOf("utc="), retained.indexOf("\n", retained.indexOf("utc=")));
                String compact = LogDisplay.compact(first, TimeZone.getTimeZone("GMT+08:00"));
                check(compact.endsWith(" · Connected") && !compact.contains("utc_ms="), "Local display preserves exported UTC record");
                check(LogDisplay.compact("invalid", TimeZone.getTimeZone("UTC")).equals("invalid"), "Malformed display input is unchanged");
                String large = repeat("中文", 2500);
                for (int index = 0; index < 530; index++) journal.append(large, 2, null).get(5, TimeUnit.SECONDS);
                retained = journal.snapshot().get(5, TimeUnit.SECONDS);
                check(retained.contains("rotation:") && retained.getBytes(StandardCharsets.UTF_8).length <= DemoJournal.FILE_LIMIT * 2 + 200,
                    "Rotation retains at most two bounded segments");
                for (String line : retained.split("\n")) if (line.startsWith("utc="))
                    check(line.getBytes(StandardCharsets.UTF_8).length + 1 <= DemoJournal.RECORD_LIMIT && !line.contains("\ufffd"),
                        "UTF-8 records are truncated on character boundaries");
                clock.now += DemoJournal.RETENTION_MS + 1;
                journal.append("fresh", 3, null).get(5, TimeUnit.SECONDS);
                retained = journal.snapshot().get(5, TimeUnit.SECONDS);
                check(retained.contains("fresh") && !retained.contains("中文"), "Seven-day retention expires old records");
            }
            DemoJournal closed = new DemoJournal(directory, clock);
            Future<?> pending = closed.append("accepted before close", 3, null);
            closed.close(); closed.close(); pending.get(5, TimeUnit.SECONDS);
            try { closed.snapshot(); throw new AssertionError("Closed journal accepted work"); }
            catch (RejectedExecutionException expected) { }
            try (DemoJournal reopened = new DemoJournal(directory, clock)) {
                check(reopened.snapshot().get(5, TimeUnit.SECONDS).contains("accepted before close"), "Close flushes accepted records for the next owner");
                reopened.clear().get(5, TimeUnit.SECONDS);
                check(!reopened.snapshot().get(5, TimeUnit.SECONDS).contains(" message="), "Explicit clear removes retained records");
            }
        } finally {
            File[] files = directory.listFiles();
            if (files != null) for (File file : files) if (!file.delete()) throw new IOException("Test cleanup failed");
            if (!directory.delete()) throw new IOException("Test cleanup failed");
        }
    }
    public static void main(String[] arguments) throws Exception {
        settingsChecks(); rawChecks(); journalChecks();
        check(M90DevelopmentPreset.bleOptions().requestedMtu == 517 &&
            M90DevelopmentPreset.bleOptions().maxWriteBytes == 512, "M90 preset supplies only Demo BLE transport choices");
        System.out.println("PASS: Full Demo schema 1/2/3, bounded Raw, private journal rotation/retention and owner close");
    }
}
