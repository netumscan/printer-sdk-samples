package com.netumscan.printersdk.demo;

import java.util.Collections;
import java.util.Map;

/** Optional, generated distribution identity. A local source build must not guess a release. */
final class DemoBuildIdentity {
    static final String RESOURCE = "printer-sdk-samples-build.json";
    final String sdkDistributionVersion, sdkReleaseCommit, sdkArtifactSha256, sdkCoreVersion;
    final String demoSourceFingerprint, demoBuildVersion;
    final Long sdkAbiVersion, sdkAbiRevision;

    DemoBuildIdentity(Map<String, ?> identity) {
        sdkDistributionVersion = version(identity.get("sdk_distribution_version"));
        sdkReleaseCommit = digest(identity.get("sdk_release_commit"), 40);
        sdkArtifactSha256 = digest(identity.get("sdk_artifact_sha256"), 64);
        sdkCoreVersion = version(identity.get("sdk_core_version"));
        sdkAbiVersion = unsigned(identity.get("sdk_abi_version"));
        sdkAbiRevision = unsigned(identity.get("sdk_abi_revision"));
        demoSourceFingerprint = digest(identity.get("demo_source_fingerprint"), 64);
        demoBuildVersion = token(identity.get("demo_build_version"));
    }

    static DemoBuildIdentity unknown() { return new DemoBuildIdentity(Collections.emptyMap()); }
    static boolean supportsSchema(Object value) { return Long.valueOf(1).equals(unsigned(value)); }
    static String token(Object value) {
        if (!(value instanceof String)) return null;
        String text = (String)value;
        return text.matches("[A-Za-z0-9][A-Za-z0-9._+\\-]{0,95}") &&
            !text.matches("(?i)[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}") ? text : null;
    }
    private static String version(Object value) {
        String text = token(value);
        return text != null && text.matches("[0-9]+\\.[0-9]+\\.[0-9]+(?:-[A-Za-z0-9][A-Za-z0-9.\\-]*)?(?:\\+[A-Za-z0-9][A-Za-z0-9.\\-]*)?") ? text : null;
    }
    private static String digest(Object value, int count) {
        return value instanceof String && ((String)value).matches("[0-9a-f]{" + count + "}") ? (String)value : null;
    }
    private static Long unsigned(Object value) {
        if (!(value instanceof Number)) return null;
        Number number = (Number)value;
        long integer = number.longValue();
        return integer >= 0 && integer <= 0xffffffffL && number.doubleValue() == integer ? integer : null;
    }
    boolean hasSdkIdentity() {
        return sdkDistributionVersion != null || sdkReleaseCommit != null || sdkArtifactSha256 != null ||
            sdkCoreVersion != null || sdkAbiVersion != null || sdkAbiRevision != null;
    }
}
