package com.netumscan.printersdk.demo;

/** Private immutable Gray8 snapshot shared by preview and submission. */
final class PreparedImage {
    final LabelConfig config;
    private final byte[] pixels;
    PreparedImage(LabelConfig config, byte[] pixels) {
        if ((long)config.labelWidthDots * config.height != pixels.length) throw new IllegalArgumentException("无效画布");
        this.config = config; this.pixels = pixels.clone();
    }
    byte[] copyPixels() { return pixels.clone(); }
    int grayAt(int index) { return pixels[index] & 255; }
    boolean matches(LabelConfig other) {
        return config.description().equals(other.description()) && config.copies == other.copies;
    }
}
