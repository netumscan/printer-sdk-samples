// SPDX-License-Identifier: MIT
package com.netumscan.printersdk.demo;

/** App-only cancellation for image work; unrelated to printer request scheduling. */
interface ImageCancellation {
    boolean getAsBoolean();
}
