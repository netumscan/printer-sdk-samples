// SPDX-License-Identifier: MIT
package com.netumscan.printersdk.demo;

import android.graphics.Bitmap;

/** App-owned image snapshot; the SDK accepts a Bitmap or PNG/JPEG and a page in millimeters. */
final class DemoPrintData implements AutoCloseable {
    final LabelConfig config;
    final Bitmap bitmap;
    final byte[] encoded;
    private DemoPrintData(LabelConfig config, Bitmap bitmap, byte[] encoded) {
        this.config = config; this.bitmap = bitmap; this.encoded = encoded;
    }
    static DemoPrintData bitmap(LabelConfig config, Bitmap bitmap) {
        if (config == null || bitmap == null) throw new IllegalArgumentException("图片与页面不能为空");
        return new DemoPrintData(config, bitmap, null);
    }
    static DemoPrintData encoded(LabelConfig config, byte[] encoded) {
        if (config == null || encoded == null || encoded.length == 0)
            throw new IllegalArgumentException("请选择 PNG/JPEG 图片");
        return new DemoPrintData(config, null, encoded);
    }
    static DemoPrintData pixels(LabelConfig config, byte[] pixels) {
        if (config == null || pixels == null || (long)config.labelWidthDots * config.height != pixels.length)
            throw new IllegalArgumentException("图片像素尺寸与本次打印宽高不一致");
        return bitmap(config, ImageImport.preview(new PreparedImage(config, pixels)));
    }
    @Override public void close() { if (bitmap != null) bitmap.recycle(); }
}
