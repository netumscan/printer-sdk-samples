package com.netumscan.printersdk.demo;

import com.netumscan.printersdk.CpclProfile;
import com.netumscan.printersdk.EscPosProfile;

/** Verified device parameters, independent of the next image or paper settings. */
final class ConnectionConfig {
    // Demo validation ceiling, not a detected hardware capability.
    static final int MAX_PAGE_HEIGHT = 65535;
    final int dpi, printerWidthDots;
    final boolean receipt;
    ConnectionConfig(int dpi, int printerWidthDots, boolean receipt) {
        if (dpi <= 0 || dpi > (receipt ? 255 : 1200))
            throw new LabelConfig.ValidationError("dpi", receipt ? "小票 DPI 须为 1–255" : "请填写有效 DPI（1–1200）");
        if (printerWidthDots <= 0 || printerWidthDots > 7992 || printerWidthDots % 8 != 0)
            throw new LabelConfig.ValidationError("width", "打印机最大宽度须为 8–7992 dots，且为 8 的倍数");
        this.dpi = dpi; this.printerWidthDots = printerWidthDots; this.receipt = receipt;
    }
    boolean matches(LabelConfig label) {
        return label.dpi == dpi && label.printerWidthDots == printerWidthDots && label.receipt == receipt;
    }
    CpclProfile profile() { return new CpclProfile(dpi, printerWidthDots, MAX_PAGE_HEIGHT, false); }
    EscPosProfile receiptProfile() { return new EscPosProfile(dpi, printerWidthDots); }
    String description() {
        return (receipt ? "ESC/POS / " : "CPCL / ") + dpi + " DPI / 设备最大宽度=" + printerWidthDots + " dots";
    }
}
