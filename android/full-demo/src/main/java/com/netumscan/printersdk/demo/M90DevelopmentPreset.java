// SPDX-License-Identifier: MIT
package com.netumscan.printersdk.demo;

import com.netumscan.printersdk.BleOptions;
import com.netumscan.printersdk.LTM90Profile;

/** Demo-only BLE transport preset; never supplies a print Profile. */
final class M90DevelopmentPreset {
    static final String ID = "m90-development";
    static final int REVISION = 1;
    static final int REQUESTED_MTU = 517, MAX_WRITE_BYTES = 512;
    private M90DevelopmentPreset() { }
    static BleOptions bleOptions() { return LTM90Profile.bleOptions(REQUESTED_MTU, MAX_WRITE_BYTES); }
}
