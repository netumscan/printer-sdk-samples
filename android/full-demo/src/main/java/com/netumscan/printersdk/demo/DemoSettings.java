package com.netumscan.printersdk.demo;

import java.util.*;

/** Versioned app preferences, never a printer identity or an SDK profile catalog. */
final class DemoSettings {
    static final String[] VERSION_ONE_KEYS = {"version", "dpi", "width", "label_width", "height", "copies", "x_offset",
        "form", "print_format", "media", "transport", "ble_profile", "write_mode", "mtu", "payload", "service", "characteristic"};
    static final String[] VERSION_TWO_KEYS, KEYS;
    static {
        VERSION_TWO_KEYS = Arrays.copyOf(VERSION_ONE_KEYS, VERSION_ONE_KEYS.length + 2);
        VERSION_TWO_KEYS[VERSION_ONE_KEYS.length] = "origin_mode";
        VERSION_TWO_KEYS[VERSION_ONE_KEYS.length + 1] = "origin_adjustment";
        KEYS = Arrays.copyOf(VERSION_TWO_KEYS, VERSION_TWO_KEYS.length + 1);
        KEYS[VERSION_TWO_KEYS.length] = "trailing_blank";
    }
    final Map<String, Object> values;
    final LabelConfig label;
    DemoSettings(Map<String, Object> input) {
        int version = integer(input, "version", 1, 3);
        String[] expected = version == 1 ? VERSION_ONE_KEYS : version == 2 ? VERSION_TWO_KEYS : KEYS;
        if (input.size() != expected.length || !input.keySet().equals(new HashSet<>(Arrays.asList(expected))))
            throw new IllegalArgumentException("配置字段不完整或不受支持");
        Map<String, Object> copy = new LinkedHashMap<>(input);
        if (version == 1) {
            copy.put("origin_mode", OriginPlacement.MANUAL); copy.put("origin_adjustment", 0);
        }
        if (version < 3) copy.put("trailing_blank", 0);
        copy.put("version", 3);
        int kind = integer(copy, "print_format", 0, 1), transport = integer(copy, "transport", 0, 1);
        integer(copy, "media", 0, 2);
        int form = kind == 1 ? unused(copy, "form") : integer(copy, "form", 1, 2);
        int mode = integer(copy, "origin_mode", OriginPlacement.MANUAL, OriginPlacement.CENTERED);
        int adjustment = integer(copy, "origin_adjustment", Integer.MIN_VALUE, Integer.MAX_VALUE);
        int width = integer(copy, "width", 8, 7992), canvas = integer(copy, "label_width", 128, 7992);
        int x = integer(copy, "x_offset", 0, 7991);
        if (kind == 1 && (mode != OriginPlacement.MANUAL || adjustment != 0 || x != 0))
            throw new IllegalArgumentException("小票不使用横向定位策略或校准偏移");
        if (OriginPlacement.resolve(width, canvas, mode, x, adjustment) != x)
            throw new IllegalArgumentException("保存的横向起点与定位策略不一致");
        label = new LabelConfig(integer(copy, "dpi", 1, 1200), width, canvas,
            integer(copy, "height", 128, 65535), integer(copy, "copies", 1, 20), form, x, kind == 1,
            integer(copy, "trailing_blank", 0, 65535));
        if (transport == 0) {
            for (String key : new String[]{"ble_profile", "write_mode", "mtu", "payload", "service", "characteristic"}) unused(copy, key);
        } else {
            int profile = integer(copy, "ble_profile", 0, 1);
            integer(copy, "mtu", 23, 517); integer(copy, "payload", 1, 512);
            if (profile == 0) {
                unused(copy, "write_mode"); unused(copy, "service"); unused(copy, "characteristic");
            } else {
                integer(copy, "write_mode", 1, 2);
                for (String key : new String[]{"service", "characteristic"}) {
                    Object value = copy.get(key);
                    if (!(value instanceof String) || !((String)value).matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))
                        throw new IllegalArgumentException("配置 UUID 无效");
                }
            }
        }
        values = Collections.unmodifiableMap(copy);
    }
    private static int unused(Map<String, Object> map, String key) {
        if (map.get(key) != null) throw new IllegalArgumentException("未使用的配置字段须为 null");
        return 0;
    }
    static int integer(Map<String, Object> map, String key, int low, int high) {
        Object value = map.get(key);
        if (!(value instanceof Number) || ((Number)value).doubleValue() != ((Number)value).longValue() ||
            ((Number)value).longValue() < low || ((Number)value).longValue() > high)
            throw new IllegalArgumentException("配置数值无效：" + key);
        return ((Number)value).intValue();
    }
    static DemoSettings legacy(Map<String, ?> old) {
        if (!old.containsKey("label_width")) throw new IllegalArgumentException("旧配置缺少独立 label_width；请手动填写");
        Map<String, Object> map = new LinkedHashMap<>(); map.put("version", 1);
        for (String key : VERSION_ONE_KEYS) {
            if (key.equals("version")) continue;
            Object value = old.get(key);
            if (value instanceof String && !key.equals("service") && !key.equals("characteristic")) {
                try { value = Integer.valueOf((String)value); }
                catch (NumberFormatException error) { throw new IllegalArgumentException("旧配置数值无效"); }
            }
            map.put(key, value);
        }
        if (Objects.equals(map.get("print_format"), 1)) map.put("form", null);
        if (Objects.equals(map.get("transport"), 0)) {
            for (String key : new String[]{"ble_profile", "write_mode", "mtu", "payload", "service", "characteristic"}) map.put(key, null);
        } else if (Objects.equals(map.get("ble_profile"), 0)) {
            for (String key : new String[]{"write_mode", "service", "characteristic"}) map.put(key, null);
        }
        return new DemoSettings(map); // Unknown legacy keys, including address, are deliberately ignored.
    }
    static DemoSettings legacy(Map<String, ?> old, int selectedMedia) {
        if (selectedMedia < 0 || selectedMedia > 2) throw new IllegalArgumentException("请明确选择旧参数的介质");
        Map<String, Object> confirmed = new LinkedHashMap<>(old);
        confirmed.put("media", selectedMedia);
        return legacy(confirmed);
    }
}
