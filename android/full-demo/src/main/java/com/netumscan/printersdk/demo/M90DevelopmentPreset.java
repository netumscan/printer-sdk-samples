package com.netumscan.printersdk.demo;

import com.netumscan.printersdk.BleOptions;
import com.netumscan.printersdk.M90Profile;

/** Demo-only development defaults, not an accepted SDK product profile. */
final class M90DevelopmentPreset {
    static final String ID = "m90-development";
    static final int REVISION = 1;
    static final int DPI = 203, WIDTH_DOTS = 864, REQUESTED_MTU = 517, MAX_WRITE_BYTES = 512;
    private M90DevelopmentPreset() { }
    static BleOptions bleOptions() { return M90Profile.bleOptions(REQUESTED_MTU, MAX_WRITE_BYTES); }
}
