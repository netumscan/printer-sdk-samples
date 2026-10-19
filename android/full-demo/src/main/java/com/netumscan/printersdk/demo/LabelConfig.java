package com.netumscan.printersdk.demo;

import com.netumscan.printersdk.CpclProfile;

/** Diagnostic label geometry is independent of the configured device width limit. */
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
            throw new ValidationError("height", "画布高度须为 128–65535 dots，总像素不得超过 800 万");
        if (copies < 1 || copies > 20) throw new ValidationError("copies", "联调每次限 1–20 份");
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
    CpclProfile profile() { return new CpclProfile(dpi, printerWidthDots, height + trailingBlankDots, form, xOffsetDots); }
    long receiptBudget() { return ((long)height + trailingBlankDots) * copies; }
    com.netumscan.printersdk.EscPosProfile receiptProfile() {
        return new com.netumscan.printersdk.EscPosProfile(dpi, printerWidthDots);
    }
    String description() {
        return (receipt ? "ESC/POS 小票 / " : "CPCL 标签 / ") + dpi + " DPI / 设备最大宽度=" + printerWidthDots + " dots / 标签=" +
            labelWidthDots + "×" + height + " dots / 横向起点=" + xOffsetDots + " dots / FORM=" + form +
            " / 结束留白=" + trailingBlankDots + " dots";
    }
}
