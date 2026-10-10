package com.netumscan.printersdk.demo;

/** Application layout policy. The SDK still receives only the resolved CPCL origin. */
final class OriginPlacement {
    static final int MANUAL = 0, CENTERED = 1;
    private OriginPlacement() { }
    static int resolve(int deviceWidth, int canvasWidth, int mode, int manualOrigin, int adjustment) {
        if (deviceWidth < 1) throw new LabelConfig.ValidationError("width", "请填写有效打印机最大宽度");
        if (canvasWidth < 1) throw new LabelConfig.ValidationError("label_width", "请填写有效画布宽度");
        long packed = ((long)canvasWidth + 7) / 8 * 8;
        if (packed > deviceWidth)
            throw new LabelConfig.ValidationError("label_width", "画布宽度（按整字节补齐）不能超过设备最大宽度");
        if (mode != MANUAL && mode != CENTERED)
            throw new LabelConfig.ValidationError("origin_mode", "请选择有效横向定位方式");
        long origin = mode == CENTERED ? (deviceWidth - packed) / 2 + adjustment : manualOrigin;
        if (origin < 0 || origin + packed > deviceWidth)
            throw new LabelConfig.ValidationError(mode == CENTERED ? "origin_adjustment" : "x_offset",
                "横向起点须非负，且加画布宽度（按整字节补齐）不能超过设备最大宽度");
        return (int)origin;
    }
}
