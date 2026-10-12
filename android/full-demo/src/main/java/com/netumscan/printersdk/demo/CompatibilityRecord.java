package com.netumscan.printersdk.demo;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/** Local metadata only. No printer instance, device identifier or payload is accepted. */
final class CompatibilityRecord {
    static final int SCHEMA_VERSION = 1;
    private final Map<String, Object> values = new LinkedHashMap<>();

    CompatibilityRecord(DemoBuildIdentity identity, String osVersion, String transport) {
        Map<String, Object> sdk = new LinkedHashMap<>(), demo = new LinkedHashMap<>(), runtime = new LinkedHashMap<>();
        sdk.put("distribution_version", identity.sdkDistributionVersion);
        sdk.put("core_version", identity.sdkCoreVersion);
        sdk.put("abi_version", identity.sdkAbiVersion); sdk.put("abi_revision", identity.sdkAbiRevision);
        sdk.put("release_commit", identity.sdkReleaseCommit); sdk.put("artifact_sha256", identity.sdkArtifactSha256);
        sdk.put("identity_source", identity.hasSdkIdentity() ? "distribution_manifest" : null);
        sdk.put("abi_source", identity.sdkAbiVersion != null && identity.sdkAbiRevision != null ? "distribution_manifest" : null);
        demo.put("source_fingerprint", identity.demoSourceFingerprint); demo.put("build_version", identity.demoBuildVersion);
        runtime.put("platform", "android");
        runtime.put("os_version", osVersion != null && osVersion.matches("[0-9]+(?:\\.[0-9]+){0,3}") ? osVersion : null);
        runtime.put("transport", "bluetooth_spp".equals(transport) || "bluetooth_ble".equals(transport) ? transport : null);
        values.put("schema_version", SCHEMA_VERSION); values.put("sdk", sdk); values.put("demo", demo); values.put("runtime", runtime);
        values.put("known_limitations", Arrays.asList("hardware_acceptance_deferred", "data_sent_is_not_printed", "no_automatic_replay"));
        values.put("hardware_verified", false);
    }

    String json() { StringBuilder output = new StringBuilder(); encode(values, output); return output.toString(); }
    String exporting(String journal) { return "compatibility_record=" + json() + "\n" + journal; }

    private static void encode(Object value, StringBuilder output) {
        if (value == null) { output.append("null"); return; }
        if (value instanceof Number || value instanceof Boolean) { output.append(value); return; }
        if (value instanceof Map<?, ?>) {
            output.append('{'); boolean first = true;
            for (Map.Entry<?, ?> item : ((Map<?, ?>)value).entrySet()) {
                if (!first) output.append(','); first = false;
                encode(item.getKey(), output); output.append(':'); encode(item.getValue(), output);
            }
            output.append('}'); return;
        }
        if (value instanceof Iterable<?>) {
            output.append('['); boolean first = true;
            for (Object item : (Iterable<?>)value) { if (!first) output.append(','); first = false; encode(item, output); }
            output.append(']'); return;
        }
        output.append('"');
        for (char character : ((String)value).toCharArray()) {
            if (character == '"' || character == '\\') output.append('\\');
            if (character < 0x20) output.append(String.format(java.util.Locale.ROOT, "\\u%04x", (int)character));
            else output.append(character);
        }
        output.append('"');
    }
}
