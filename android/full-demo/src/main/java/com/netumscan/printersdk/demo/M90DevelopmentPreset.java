// SPDX-License-Identifier: MIT
package com.netumscan.printersdk.demo;

import com.netumscan.printersdk.PrinterModel;

/** Explicit App selection; communication details remain inside the SDK. */
final class M90DevelopmentPreset {
    private M90DevelopmentPreset() { }
    static PrinterModel model() { return PrinterModel.LT_M90; }
}
