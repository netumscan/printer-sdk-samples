// SPDX-License-Identifier: MIT
package com.netumscan.printersdk.demo;

/** 仅供缩略图和完整预览使用的不可变 Gray8 缓存；打印独立读取 DemoImageSource。 */
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
        return other != null && config.dpi == other.dpi && config.printerWidthDots == other.printerWidthDots &&
            config.labelWidthDots == other.labelWidthDots && config.height == other.height && config.copies == other.copies &&
            config.xOffsetDots == other.xOffsetDots && config.horizontalCalibrationDots == other.horizontalCalibrationDots &&
            config.manualOriginDots == other.manualOriginDots && config.printProfile().dotsPerMm == other.printProfile().dotsPerMm &&
            config.printProfile().labelAlignment == other.printProfile().labelAlignment && config.trailingBlankDots == other.trailingBlankDots &&
            config.form == other.form && config.receipt == other.receipt;
    }
}
