// SPDX-License-Identifier: MIT
package com.netumscan.printersdk.demo;

import java.util.Arrays;

/** Bounded, immutable diagnostic input; never persisted or included in logs. */
final class RawCommand {
    static final int MAX_CHARACTERS = 65536, MAX_BYTES = 4096;
    final int copies;
    private final byte[] bytes;
    RawCommand(byte[] bytes, int copies) {
        if (bytes == null || bytes.length == 0 || bytes.length > MAX_BYTES)
            throw new IllegalArgumentException("请输入 1–4096 个空白分隔的十六进制字节");
        if (copies < 1 || copies > 20) throw new IllegalArgumentException("Raw 每次限 1–20 份");
        this.bytes = bytes.clone(); this.copies = copies;
    }
    int size() { return bytes.length; }
    byte[] copyBytes() { return bytes.clone(); }
    static RawCommand parse(String input, int copies) {
        if (input == null || input.length() > MAX_CHARACTERS)
            throw new IllegalArgumentException("Raw 输入最多 65536 个字符");
        byte[] bytes = new byte[MAX_BYTES]; int count = 0, index = 0;
        while (index < input.length()) {
            if (space(input.charAt(index))) { ++index; continue; }
            int start = index;
            while (index < input.length() && !space(input.charAt(index))) ++index;
            if (index - start != 2) throw new IllegalArgumentException("Raw 字节必须是两位十六进制，如 1B 40");
            int high = hex(input.charAt(start)), low = hex(input.charAt(start + 1));
            if (high < 0 || low < 0) throw new IllegalArgumentException("Raw 字节必须是两位十六进制，如 1B 40");
            if (count == MAX_BYTES) throw new IllegalArgumentException("Raw 每次最多 4096 个字节");
            bytes[count++] = (byte)((high << 4) | low);
        }
        return new RawCommand(Arrays.copyOf(bytes, count), copies);
    }
    private static boolean space(char value) {
        // Unicode White_Space, matching Swift Character.isWhitespace rather than Java's wider controls.
        return value >= '\t' && value <= '\r' || value == ' ' || value == '\u0085' || value == '\u00a0' ||
            value == '\u1680' || value >= '\u2000' && value <= '\u200a' || value == '\u2028' ||
            value == '\u2029' || value == '\u202f' || value == '\u205f' || value == '\u3000';
    }
    private static int hex(char value) {
        if (value >= '0' && value <= '9') return value - '0';
        if (value >= 'a' && value <= 'f') return value - 'a' + 10;
        if (value >= 'A' && value <= 'F') return value - 'A' + 10;
        return -1;
    }
}
