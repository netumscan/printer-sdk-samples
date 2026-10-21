// SPDX-License-Identifier: MIT
package com.netumscan.printersdk.demo;

import android.os.Bundle;
import com.netumscan.printersdk.PrinterProfile;

/** 旧表单的设备参数草稿；只能用于离线预览和配置对话框，不能配置 Manager。 */
final class DemoPrintDraft {
    final PrinterProfile.Protocol protocol;
    final Integer dpi, maxPrintWidthDots;
    final PrinterProfile.LabelAlignment alignment;

    DemoPrintDraft() { this(PrinterProfile.Protocol.CPCL, null, null, PrinterProfile.LabelAlignment.LEFT); }

    private DemoPrintDraft(PrinterProfile.Protocol protocol, Integer dpi, Integer width,
                           PrinterProfile.LabelAlignment alignment) {
        this.protocol = protocol; this.dpi = dpi; maxPrintWidthDots = width; this.alignment = alignment;
    }

    DemoPrintDraft adopted(PrinterProfile profile) {
        return new DemoPrintDraft(profile.protocol, profile.dpi, profile.maxPrintWidthDots, profile.labelAlignment);
    }

    DemoPrintDraft restored(Bundle values) {
        if (values == null) return this;
        return new DemoPrintDraft(values.containsKey("print_format") && values.getInt("print_format") == 1
            ? PrinterProfile.Protocol.ESC_POS : values.containsKey("print_format") ? PrinterProfile.Protocol.CPCL : protocol,
            values.containsKey("dpi") ? integer(values.getString("dpi")) : dpi,
            values.containsKey("width") ? integer(values.getString("width")) : maxPrintWidthDots,
            values.containsKey("origin_mode") ? values.getInt("origin_mode") == OriginPlacement.CENTERED
                ? PrinterProfile.LabelAlignment.CENTERED : PrinterProfile.LabelAlignment.LEFT : alignment);
    }

    DemoPrintDraft settings(DemoSettings settings) { return adopted(settings.label.printProfile()); }

    void save(Bundle values) {
        values.putString("dpi", dpiText()); values.putString("width", widthText());
        values.putInt("print_format", receipt() ? 1 : 0); values.putInt("origin_mode", originMode());
    }

    boolean receipt() { return protocol == PrinterProfile.Protocol.ESC_POS; }
    int originMode() { return alignment == PrinterProfile.LabelAlignment.CENTERED ? OriginPlacement.CENTERED : OriginPlacement.MANUAL; }
    String dpiText() { return dpi == null ? "" : dpi.toString(); }
    String widthText() { return maxPrintWidthDots == null ? "" : maxPrintWidthDots.toString(); }

    /** 未连接时不采用旧定位策略；旧 resolved X 不能成为新设备的绝对起点。 */
    PrinterProfile previewProfile() {
        int density = require(dpi, "dpi"), maximum = require(maxPrintWidthDots, "width");
        return receipt() ? PrinterProfile.escPos(density, maximum) : PrinterProfile.cpcl(density, maximum);
    }

    PrinterProfile editorProfile() {
        PrinterProfile profile = previewProfile();
        return receipt() ? profile : PrinterProfile.cpcl(profile.dpi, profile.maxPrintWidthDots,
            profile.dotsPerMm, alignment);
    }

    private static Integer integer(String value) {
        try { return Integer.valueOf(value == null ? "" : value.trim()); }
        catch (NumberFormatException invalid) { return null; }
    }
    private static int require(Integer value, String field) {
        if (value == null) throw new LabelConfig.ValidationError(field, "请先连接设备并确认打印配置，或加载有效草稿用于预览");
        return value;
    }
}
