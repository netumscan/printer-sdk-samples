// SPDX-License-Identifier: MIT
package com.netumscan.printersdk.demo;

import android.util.AtomicFile;
import android.util.JsonReader;
import android.util.JsonToken;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Explicit-only local storage. Call on the image/utility worker, never during startup. */
final class SettingsStore {
    private final AtomicFile file;
    SettingsStore(File directory) { file = new AtomicFile(new File(directory, "demo-settings.json")); }
    void save(DemoSettings settings) throws IOException {
        JSONObject object = new JSONObject();
        try { for (String key : DemoSettings.KEYS) object.put(key, settings.values.get(key) == null ? JSONObject.NULL : settings.values.get(key)); }
        catch (JSONException error) { throw new IOException("Configuration encoding"); }
        FileOutputStream output = null;
        try {
            output = file.startWrite(); output.write(object.toString().getBytes(StandardCharsets.UTF_8)); file.finishWrite(output);
        } catch (IOException error) { if (output != null) file.failWrite(output); throw error; }
    }
    DemoSettings load() throws IOException {
        try (InputStream input = file.openRead()) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream(); byte[] block = new byte[4096]; int count;
            while ((count = input.read(block)) != -1) {
                if (bytes.size() + count > 65536) throw new IOException("Configuration limit"); bytes.write(block, 0, count);
            }
            Map<String, Object> values = new LinkedHashMap<>();
            try (JsonReader reader = new JsonReader(new StringReader(new String(bytes.toByteArray(), StandardCharsets.UTF_8)))) {
                reader.setLenient(false); reader.beginObject();
                while (reader.hasNext()) {
                    String key = reader.nextName();
                    if (values.containsKey(key)) throw new IOException("Duplicate configuration key");
                    Object value;
                    if (reader.peek() == JsonToken.NULL) { reader.nextNull(); value = null; }
                    else if (reader.peek() == JsonToken.STRING) value = reader.nextString();
                    else if (reader.peek() == JsonToken.NUMBER) {
                        String number = reader.nextString();
                        if (!number.matches("-?(0|[1-9][0-9]*)")) throw new IOException("Configuration integer");
                        try { value = Long.valueOf(number); } catch (NumberFormatException error) { throw new IOException("Configuration range"); }
                    } else throw new IOException("Configuration type");
                    values.put(key, value);
                }
                reader.endObject(); if (reader.peek() != JsonToken.END_DOCUMENT) throw new IOException("Trailing configuration data");
            }
            return new DemoSettings(values);
        } catch (RuntimeException error) { throw new IOException("Invalid configuration"); }
    }
    void clear() throws IOException {
        file.delete();
        File base = file.getBaseFile();
        if (base.exists() || new File(base.getPath() + ".bak").exists() || new File(base.getPath() + ".new").exists())
            throw new IOException("Configuration clear failed");
    }
}
