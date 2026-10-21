// SPDX-License-Identifier: MIT
package com.netumscan.printersdk.demo;

import com.netumscan.printersdk.internal.PngExif;
import java.io.File;
import java.io.IOException;

/** Package-local adapter to the shared bounded PNG eXIf reader. */
final class PngOrientation {
    static int read(File file) throws IOException { return PngExif.read(file); }
    static int tiff(byte[] data) throws IOException { return PngExif.tiff(data); }
}
