// SPDX-License-Identifier: MIT
package com.netumscan.printersdk.demo;


import java.util.Objects;

/** App-only geometry for preview and old settings migration; never configures the SDK. */
final class DemoGeometry {
    public enum Protocol { CPCL, ESC_POS }
    public enum LabelAlignment { LEFT, CENTERED }
    public final Protocol protocol;
    public final int dpi, maxPrintWidthDots;
    /** Exact physical dot density; dpi remains the nominal protocol resolution. */
    public final double dotsPerMm;
    public final LabelAlignment labelAlignment;
    public final String modelId;

    private DemoGeometry(Protocol protocol, int dpi, int width, double density,
                           LabelAlignment alignment, String modelId) {
        Objects.requireNonNull(protocol);
        Objects.requireNonNull(alignment);
        if (dpi <= 0 || (protocol == Protocol.ESC_POS && dpi > 255) ||
                width <= 0 || width > 7992 || width % 8 != 0 ||
                Double.isNaN(density) || Double.isInfinite(density) || density <= 0) {
            throw new IllegalArgumentException("Invalid printer profile");
        }
        this.protocol = protocol; this.dpi = dpi; maxPrintWidthDots = width;
        dotsPerMm = density; labelAlignment = alignment;
        this.modelId = modelId;
    }
    public static DemoGeometry cpcl(int dpi, int maxPrintWidthDots) {
        return cpcl(dpi, maxPrintWidthDots, dpi / 25.4, LabelAlignment.LEFT);
    }
    public static DemoGeometry cpcl(int dpi, int maxPrintWidthDots, double dotsPerMm,
                                              LabelAlignment labelAlignment) {
        return new DemoGeometry(Protocol.CPCL, dpi, maxPrintWidthDots, dotsPerMm, labelAlignment, null);
    }
    public static DemoGeometry escPos(int dpi, int maxPrintWidthDots) {
        return new DemoGeometry(Protocol.ESC_POS, dpi, maxPrintWidthDots, dpi / 25.4, LabelAlignment.LEFT, null);
    }
    static DemoGeometry model(Protocol protocol, int dpi, int width, double density,
                                LabelAlignment alignment, String modelId) {
        return new DemoGeometry(protocol, dpi, width, density, alignment, Objects.requireNonNull(modelId));
    }
    static DemoGeometry ltM90() { return model(Protocol.CPCL, 203, 864, 8, LabelAlignment.CENTERED, "LT-M90"); }
    /** Rounds positive millimeters to dots without adding raster byte padding. */
    public int dotsFromMm(double millimeters) {
        return dots(millimeters, true, dotsPerMm);
    }
    static int dots(double value, boolean millimeters, double density) {
        double converted = millimeters ? value * density : value;
        if (Double.isNaN(converted) || Double.isInfinite(converted) || converted > Integer.MAX_VALUE)
            throw new IllegalArgumentException("Dimension overflow");
        long result = Math.round(converted);
        if (result <= 0 || result > Integer.MAX_VALUE)
            throw new IllegalArgumentException("Dimension rounds to zero or overflows");
        return (int)result;
    }
    /** Resolves a label's physical X, including raster byte padding and signed calibration. */
    public int labelOriginXDots(int widthDots, int calibrationDots) {
        if (protocol != Protocol.CPCL || widthDots <= 0)
            throw new IllegalArgumentException("Positive CPCL label width required");
        long packedWidth = ((long)widthDots + 7) / 8 * 8;
        long origin = (labelAlignment == LabelAlignment.CENTERED ?
            ((long)maxPrintWidthDots - packedWidth) / 2 : 0) + calibrationDots;
        if (packedWidth > maxPrintWidthDots || origin < 0 || origin + packedWidth > maxPrintWidthDots)
            throw new IllegalArgumentException("Label origin exceeds printable width");
        return (int)origin;
    }
}
