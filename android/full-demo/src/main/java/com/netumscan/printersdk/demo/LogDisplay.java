package com.netumscan.printersdk.demo;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Presentation only: stored and exported journal records keep their original UTC fields. */
final class LogDisplay {
    private static final DateTimeFormatter LOCAL =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS", Locale.ROOT);
    private static final Pattern WALL = Pattern.compile("(?:^| )utc_ms=([^ ]*)(?= |$)");

    private LogDisplay() { }

    static String compact(String entry, ZoneId zone) {
        if (entry == null || zone == null) return entry;
        int message = entry.indexOf(" message=");
        if (message < 0) return entry;
        Matcher stamp = WALL.matcher(entry.substring(0, message));
        if (!stamp.find()) return entry;
        String wall = stamp.group(1);
        if (stamp.find() || !wall.matches("[0-9]+")) return entry;
        try {
            String time = LOCAL.withZone(zone).format(Instant.ofEpochMilli(Long.parseLong(wall)));
            return time + " · " + entry.substring(message + 9);
        } catch (DateTimeException | NumberFormatException error) {
            return entry;
        }
    }
}
