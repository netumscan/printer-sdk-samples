package com.netumscan.printersdk.demo;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.regex.*;

/** Two bounded private files. All filesystem work and sequence assignment are serialized. */
final class DemoJournal implements AutoCloseable {
    static final int FILE_LIMIT = 1024 * 1024, RECORD_LIMIT = 4096;
    static final long RETENTION_MS = 7L * 24 * 60 * 60 * 1000;
    interface Clock { long wall(); long elapsed(); }
    private static final DateTimeFormatter UTC = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);
    private static final Pattern PRIVATE = Pattern.compile("(?i)(?:content|file|https?)://|[0-9a-f]{2}(?::[0-9a-f]{2}){5}|[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}|(?:^|\\s)/(?:[^\\s/]+/)+[^\\s]*");
    private static final Pattern PRIVATE_FIELD = Pattern.compile("(?i)(?:image(?:name|path)?|file(?:name|path)|uri|path|exif|raw(?:bytes|payload)?|pixels|device(?:name|id|address)?|address|identifier|mac)\\s*[:=]");
    private static final Pattern WALL = Pattern.compile("(?:^| )utc_ms=(\\d+) "), SEQUENCE = Pattern.compile(" seq=(\\d+) ");
    private static final Pattern RECORD = Pattern.compile("^utc=[0-9TZ:.\\-]+ utc_ms=[0-9]+ elapsed_ms=[0-9]+ run=[0-9a-f]{16} connection=[0-9]+ seq=[0-9]+ message=.*$");
    private static final ExecutorService worker = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "PrinterDemo.journal"); thread.setDaemon(true); return thread;
    });
    private final File directory, currentFile, previousFile;
    private final Clock clock;
    private final String run = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    private final List<String> current = new ArrayList<>(), previous = new ArrayList<>();
    private long sequence;
    private int currentBytes;
    private volatile boolean degraded;
    DemoJournal(File directory) {
        this(directory, new Clock() {
            final long start = System.nanoTime();
            public long wall() { return System.currentTimeMillis(); }
            public long elapsed() { return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start); }
        });
    }
    DemoJournal(File directory, Clock clock) {
        this.directory = directory; this.clock = clock;
        currentFile = new File(directory, "journal-current.log"); previousFile = new File(directory, "journal-previous.log");
        worker.execute(() -> {
            try {
                Files.createDirectories(directory.toPath());
                load(previousFile, previous); load(currentFile, current); currentBytes = size(current); prune();
            } catch (IOException | RuntimeException error) { degraded = true; }
        });
    }
    boolean degraded() { return degraded; }
    private void load(File file, List<String> records) throws IOException {
        if (!file.exists()) return;
        if (file.length() > FILE_LIMIT) throw new IOException("Journal limit");
        for (String line : new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8).split("\n")) {
            Matcher stamp = WALL.matcher(line), seq = SEQUENCE.matcher(line);
            if (line.getBytes(StandardCharsets.UTF_8).length + 1 > RECORD_LIMIT || !RECORD.matcher(line).matches() || !stamp.find() || !seq.find()) continue;
            try { sequence = Math.max(sequence, Long.parseLong(seq.group(1))); }
            catch (NumberFormatException error) { continue; }
            int start = line.indexOf(" message=") + 9;
            String content = line.substring(start);
            if (PRIVATE.matcher(content).find() || PRIVATE_FIELD.matcher(content).find()) line = line.substring(0,start) + "诊断内容已隐藏";
            records.add(line + "\n");
        }
    }
    private static int size(List<String> records) {
        int size = 0; for (String record : records) size += record.getBytes(StandardCharsets.UTF_8).length; return size;
    }
    private void prune() throws IOException {
        long cutoff = clock.wall() - RETENTION_MS;
        boolean changed = previous.removeIf(line -> expired(line, cutoff)) | current.removeIf(line -> expired(line, cutoff));
        if (changed) currentBytes = size(current);
        if (changed && !degraded) { write(previousFile, previous); write(currentFile, current); }
    }
    private static boolean expired(String line, long cutoff) {
        Matcher match = WALL.matcher(line);
        try { return !match.find() || Long.parseLong(match.group(1)) < cutoff; }
        catch (NumberFormatException error) { return true; }
    }
    private static void write(File file, List<String> records) throws IOException {
        try (OutputStream output = new FileOutputStream(file)) {
            for (String record : records) output.write(record.getBytes(StandardCharsets.UTF_8));
        }
    }
    private String record(String message, long wall, long elapsed, long connection) {
        String safe = (PRIVATE.matcher(message).find() || PRIVATE_FIELD.matcher(message).find()) ? "诊断内容已隐藏" : message.replace('\r', ' ').replace('\n', ' ');
        String prefix = "utc=" + UTC.format(Instant.ofEpochMilli(wall)) + " utc_ms=" + wall + " elapsed_ms=" + elapsed +
            " run=" + run + " connection=" + connection + " seq=" + (++sequence) + " message=";
        int available = RECORD_LIMIT - prefix.getBytes(StandardCharsets.UTF_8).length - 1;
        StringBuilder limited = new StringBuilder(); int count = 0;
        for (int index = 0; index < safe.length();) {
            int cp = safe.codePointAt(index); String piece = new String(Character.toChars(cp));
            int length = piece.getBytes(StandardCharsets.UTF_8).length;
            if (count + length > available) break;
            limited.append(piece); count += length; index += Character.charCount(cp);
        }
        return prefix + limited + "\n";
    }
    Future<?> append(String message, long connection, Consumer<String> callback) {
        long wall = clock.wall(), elapsed = clock.elapsed();
        return worker.submit(() -> {
            try { prune(); } catch (IOException | RuntimeException error) { degraded = true; }
            String line = record(message, wall, elapsed, connection);
            if (currentBytes + line.getBytes(StandardCharsets.UTF_8).length > FILE_LIMIT) {
                previous.clear(); previous.addAll(current); current.clear();
                current.add(record("rotation: previous retained segment replaced; older records may be omitted", wall, elapsed, connection));
                try { if (!degraded) { write(previousFile, previous); write(currentFile, current); } }
                catch (IOException | RuntimeException error) { degraded = true; }
                line = record(message, wall, elapsed, connection); currentBytes = size(current);
            }
            current.add(line); currentBytes += line.getBytes(StandardCharsets.UTF_8).length;
            try {
                if (!degraded) try (OutputStream output = new FileOutputStream(currentFile, true)) { output.write(line.getBytes(StandardCharsets.UTF_8)); }
            } catch (IOException | RuntimeException error) { degraded = true; }
            callback.accept(line.trim());
        });
    }
    Future<String> snapshot() {
        return worker.submit(() -> {
            try { prune(); } catch (IOException | RuntimeException error) { degraded = true; }
            return "Printer Demo retained diagnostic records; bounded rotation may omit older records.\n" +
                (degraded ? "Storage unavailable; exporting retained in-memory records.\n" : "") +
                String.join("", previous) + String.join("", current);
        });
    }
    Future<?> clear() {
        return worker.submit(() -> {
            previous.clear(); current.clear(); currentBytes = 0;
            try {
                Files.deleteIfExists(previousFile.toPath()); Files.deleteIfExists(currentFile.toPath());
                degraded = false; Files.createDirectories(directory.toPath());
            } catch (IOException | RuntimeException error) { degraded = true; }
        });
    }
    public void close() { /* Process-owned serial lane also protects Activity recreation. */ }
}
