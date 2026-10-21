// SPDX-License-Identifier: MIT
package com.netumscan.printersdk.demo;


/** 本次打印的不可变参数，宽高以 dots 表示；不读取屏幕尺寸或预览 View。 */
final class LabelConfig {
    // Demo validation ceiling, not a detected hardware capability.
    static final int MAX_PAGE_HEIGHT = 65535;
    static final class ValidationError extends IllegalArgumentException {
        private static final long serialVersionUID = 1L;
        final String field;
        ValidationError(String field, String message) { super(message); this.field = field; }
    }
    final int dpi, printerWidthDots, labelWidthDots, height, copies, xOffsetDots, trailingBlankDots;
    final boolean form, receipt;
    final int manualOriginDots, horizontalCalibrationDots;
    private final DemoGeometry adoptedProfile;
    LabelConfig(int dpi, int printerWidthDots, int labelWidthDots, int height, int copies, int formChoice) {
        this(dpi, printerWidthDots, labelWidthDots, height, copies, formChoice, 0);
    }
    LabelConfig(int dpi, int printerWidthDots, int labelWidthDots, int height, int copies, int formChoice, int xOffsetDots) {
        this(dpi, printerWidthDots, labelWidthDots, height, copies, formChoice, xOffsetDots, false);
    }
    LabelConfig(int dpi, int printerWidthDots, int labelWidthDots, int height, int copies, int formChoice,
                int xOffsetDots, boolean receipt) {
        this(dpi, printerWidthDots, labelWidthDots, height, copies, formChoice, xOffsetDots, receipt, 0);
    }
    LabelConfig(int dpi, int printerWidthDots, int labelWidthDots, int height, int copies, int formChoice,
                int xOffsetDots, boolean receipt, int trailingBlankDots) {
        this(legacyProfile(dpi, printerWidthDots, receipt),
            labelWidthDots, height, copies, formChoice, xOffsetDots, 0, trailingBlankDots);
    }
    /** 已采用的 Profile 只供 SDK 预览和校验；提交时不覆盖 Manager 的配置。 */
    LabelConfig(DemoGeometry profile, int labelWidthDots, int height, int copies, int formChoice,
                int manualOriginDots, int horizontalCalibrationDots, int trailingBlankDots) {
        int dpi = profile.dpi, printerWidthDots = profile.maxPrintWidthDots;
        boolean receipt = profile.protocol == DemoGeometry.Protocol.ESC_POS;
        if (manualOriginDots != 0 && horizontalCalibrationDots != 0)
            throw new ValidationError("origin_adjustment", "手动横向起点和校准偏移不能同时非零");
        if (profile.labelAlignment == DemoGeometry.LabelAlignment.CENTERED && manualOriginDots != 0)
            throw new ValidationError("x_offset", "当前 Profile 固定居中，只能填写校准偏移");
        int xOffsetDots;
        try { xOffsetDots = receipt ? 0 : manualOriginDots == 0
            ? profile.labelOriginXDots(labelWidthDots, horizontalCalibrationDots)
            : profile.labelOriginXDots(labelWidthDots, manualOriginDots); }
        catch (IllegalArgumentException invalid) {
            throw new ValidationError(manualOriginDots == 0 ? "origin_adjustment" : "x_offset", invalid.getMessage());
        }
        if (receipt && (manualOriginDots != 0 || horizontalCalibrationDots != 0))
            throw new ValidationError("x_offset", "小票不使用标签起点或校准偏移");
        this.adoptedProfile = profile; this.manualOriginDots = manualOriginDots;
        this.horizontalCalibrationDots = horizontalCalibrationDots;
        if (dpi <= 0 || dpi > 1200) throw new ValidationError("dpi", "请填写有效 DPI（1–1200）");
        if (printerWidthDots <= 0 || printerWidthDots > 7992 || printerWidthDots % 8 != 0)
            throw new ValidationError("width", "打印机最大宽度须为 8–7992 dots，且为 8 的倍数");
        if (labelWidthDots < 128 || labelWidthDots > printerWidthDots)
            throw new ValidationError("label_width", "本次标签可打印宽度须至少 128 dots，且不能超过打印机最大宽度");
        if (xOffsetDots < 0 || xOffsetDots >= printerWidthDots ||
            (labelWidthDots + 7) / 8 * 8 > printerWidthDots - xOffsetDots)
            throw new ValidationError("x_offset", "横向起点加标签宽度（按整字节补齐）不能超过设备最大宽度");
        this.xOffsetDots = xOffsetDots;
        if (height < 128 || height > 65535 || (long)labelWidthDots * height > 8_000_000)
            throw new ValidationError("height", "打印高度须为 128–65535 dots，总像素不得超过 800 万");
        if (copies < 1 || copies > 20) throw new ValidationError("copies", "演示每次限 1–20 份");
        if (trailingBlankDots < 0 || trailingBlankDots > 65535)
            throw new ValidationError("trailing_blank", "结束留白须为 0–65535 dots");
        if (!receipt && (long)height + trailingBlankDots > MAX_PAGE_HEIGHT)
            throw new ValidationError("trailing_blank", "标签高度与结束留白合计不能超过 65535 dots");
        if (receipt && (dpi > 255 || xOffsetDots != 0))
            throw new ValidationError("dpi", "小票 DPI 须为 1–255，横向起点为 0");
        if (!receipt && (formChoice < 1 || formChoice > 2)) throw new ValidationError("form", "请选择是否发送 FORM");
        this.dpi = dpi; this.printerWidthDots = printerWidthDots; this.labelWidthDots = labelWidthDots;
        this.height = height; this.copies = copies; this.trailingBlankDots = trailingBlankDots;
        form = !receipt && formChoice == 1; this.receipt = receipt;
    }
    private static DemoGeometry legacyProfile(int dpi, int width, boolean receipt) {
        try { return receipt ? DemoGeometry.escPos(dpi, width) : DemoGeometry.cpcl(dpi, width); }
        catch (IllegalArgumentException invalid) {
            throw new ValidationError(dpi <= 0 || dpi > (receipt ? 255 : 1200) ? "dpi" : "width", invalid.getMessage());
        }
    }
    DemoGeometry printProfile() {
        return adoptedProfile;
    }
    double widthMm() { return labelWidthDots / adoptedProfile.dotsPerMm; }
    double heightMm() { return height / adoptedProfile.dotsPerMm; }
    long receiptBudget() { return ((long)height + trailingBlankDots) * copies; }
    String description() {
        return (receipt ? "ESC/POS 小票 / " : "CPCL 标签 / ") + dpi + " DPI / 设备最大宽度=" + printerWidthDots + " dots / 标签=" +
            labelWidthDots + "×" + height + " dots / 横向起点=" + xOffsetDots + " dots / FORM=" + form +
            " / 结束留白=" + trailingBlankDots + " dots";
    }
}
