// SPDX-License-Identifier: MIT
package com.netumscan.printersdk.demo;

import com.netumscan.printersdk.PrinterProfile;

/** 旧配置的定位字段适配；起点计算始终委托 SDK。 */
final class OriginPlacement {
    static final int MANUAL = 0, CENTERED = 1;
    private OriginPlacement() { }
    static int resolve(int deviceWidth, int canvasWidth, int mode, int manualOrigin, int adjustment) {
        if (mode != MANUAL && mode != CENTERED)
            throw new LabelConfig.ValidationError("origin_mode", "请选择有效横向定位方式");
        if (mode == MANUAL && manualOrigin != 0 && adjustment != 0)
            throw new LabelConfig.ValidationError("origin_adjustment", "手动横向起点和校准偏移不能同时非零");
        try {
            PrinterProfile profile = PrinterProfile.cpcl(203, deviceWidth, 203 / 25.4,
                mode == CENTERED ? PrinterProfile.LabelAlignment.CENTERED : PrinterProfile.LabelAlignment.LEFT);
            return profile.labelOriginXDots(canvasWidth, mode == CENTERED || manualOrigin == 0 ? adjustment : manualOrigin);
        } catch (IllegalArgumentException invalid) {
            throw new LabelConfig.ValidationError(mode == CENTERED || manualOrigin == 0 ? "origin_adjustment" : "x_offset", invalid.getMessage());
        }
    }
}
