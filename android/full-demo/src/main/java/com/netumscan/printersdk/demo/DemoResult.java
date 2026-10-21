// SPDX-License-Identifier: MIT
package com.netumscan.printersdk.demo;

import com.netumscan.printersdk.ManagerResult;

/** App metadata plus the unchanged, typed SDK completion. Never infer state from formatted text. */
final class DemoResult<T> {
    enum Kind { CONNECT, PROFILE, PRINT, RAW, MEDIA, FEED, STATUS, CAPABILITIES, IDENTITY, SCAN, BONDED, DISCONNECTED, DISCONNECT, CLOSE }
    final Kind kind;
    final ManagerResult<T> raw;
    DemoResult(Kind kind, ManagerResult<T> raw) { this.kind = kind; this.raw = raw; }
}
