// SPDX-License-Identifier: MIT
package com.netumscan.printersdk.demo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import com.netumscan.printersdk.internal.ImageTransforms;
import java.io.*;
import java.util.concurrent.CancellationException;

/** 平台图片检查与解码；来源、打印和预览分别调用，不持有界面状态。 */
final class ImageImport {
    private static final class Metadata {
        final int width, height, orientation;
        Metadata(int width, int height, int orientation) {
            this.width = width; this.height = height; this.orientation = orientation;
        }
    }
    static void copy(InputStream input, File destination, ImageCancellation cancelled) throws IOException {
        copy(input, destination, cancelled, ImageRaster.MAX_FILE_BYTES);
    }
    static void copy(InputStream input, File destination, ImageCancellation cancelled, long limit) throws IOException {
        if (limit <= 0 || limit > ImageRaster.MAX_FILE_BYTES) throw new IOException("Image cache limit");
        long count = 0;
        try (OutputStream output = new FileOutputStream(destination)) {
            byte[] block = new byte[16384]; int read;
            while ((read = input.read(block)) != -1) {
                if (cancelled.getAsBoolean()) throw new CancellationException();
                count += read;
                if (count > limit) throw new IOException("Image cache/file limit");
                output.write(block, 0, read);
            }
        }
        if (count == 0) throw new IOException("Empty image");
    }
    static PreparedImage prepare(File file, LabelConfig config, ImageCancellation cancelled) throws IOException {
        return new PreparedImage(config, decode(file, config, cancelled));
    }
    /** 导入只检查源文件，不依赖打印宽高，也不生成任何预览。 */
    static void validate(File file, ImageCancellation cancelled) throws IOException {
        metadata(file, cancelled);
    }
    private static Metadata metadata(File file, ImageCancellation cancelled) throws IOException {
        if (cancelled.getAsBoolean()) throw new CancellationException();
        if (file.length() <= 0 || file.length() > ImageRaster.MAX_FILE_BYTES) throw new IOException("Image limit");
        try (RandomAccessFile input = new RandomAccessFile(file, "r")) {
            byte[] signature = new byte[8]; input.readFully(signature);
            boolean png = java.util.Arrays.equals(signature, new byte[]{(byte)137,80,78,71,13,10,26,10});
            boolean jpeg = (signature[0] & 255) == 255 && (signature[1] & 255) == 216;
            if (!png && !jpeg) throw new IOException("Unsupported signature");
            if (jpeg) { input.seek(file.length() - 2); if (input.readUnsignedShort() != 0xffd9) throw new IOException("Incomplete JPEG"); }
            else {
                input.seek(file.length() - 12); byte[] end = new byte[12]; input.readFully(end);
                if (!java.util.Arrays.equals(end, new byte[]{0,0,0,0,73,69,78,68,(byte)174,66,96,(byte)130})) throw new IOException("Incomplete PNG");
            }
        }
        BitmapFactory.Options bounds = new BitmapFactory.Options(); bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getPath(), bounds);
        if (!"image/png".equals(bounds.outMimeType) && !"image/jpeg".equals(bounds.outMimeType))
            throw new IOException("Unsupported image");
        ImageRaster.sourceBounds(bounds.outWidth, bounds.outHeight);
        int orientation;
        if ("image/png".equals(bounds.outMimeType)) orientation = PngOrientation.read(file);
        else {
            orientation = new ExifInterface(file.getPath()).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
            // The platform supplies UNDEFINED for JPEGs without an orientation tag.
            if (orientation == ExifInterface.ORIENTATION_UNDEFINED) orientation = ExifInterface.ORIENTATION_NORMAL;
        }
        if (orientation < 1 || orientation > 8) throw new IOException("Image orientation");
        if (cancelled.getAsBoolean()) throw new CancellationException();
        return new Metadata(bounds.outWidth, bounds.outHeight, orientation);
    }
    /** 打印直接从源文件解码为 Gray8；不需要 PreparedImage 或预览 Bitmap。 */
    static byte[] decode(File file, LabelConfig config, ImageCancellation cancelled) throws IOException {
        Metadata image = metadata(file, cancelled);
        int orientation = image.orientation;
        ImageRaster.Layout fit = new ImageRaster.Layout(orientation >= 5 ? image.height : image.width,
            orientation >= 5 ? image.width : image.height, config.labelWidthDots, config.height);
        BitmapFactory.Options options = new BitmapFactory.Options(); options.inScaled = false;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        options.inSampleSize = ImageRaster.sample(image.width, image.height, fit, orientation);
        if (cancelled.getAsBoolean()) throw new CancellationException();
        Bitmap decoded = BitmapFactory.decodeFile(file.getPath(), options);
        if (decoded == null) throw new IOException("Invalid image");
        int width = decoded.getWidth(), height = decoded.getHeight();
        int[] argb;
        try {
            if ((long)width * height > ImageRaster.MAX_OUTPUT_PIXELS || decoded.getAllocationByteCount() > 32L * 1024 * 1024)
                throw new IOException("Decoded image limit");
            if (cancelled.getAsBoolean()) throw new CancellationException();
            argb = new int[width * height];
            decoded.getPixels(argb, 0, width, 0, 0, width, height);
        } finally { decoded.recycle(); }
        return ImageRaster.render(argb, width, height, orientation, fit, config.labelWidthDots, config.height, cancelled);
    }
    static Bitmap preview(PreparedImage image) { return preview(image, () -> false); }
    /** 从显示快照生成有尺寸上限的缩略图，与打印提交无关。 */
    static Bitmap thumbnail(PreparedImage image, ImageCancellation cancelled) {
        ImageTransforms.Layout fit = ImageTransforms.contain(image.config.labelWidthDots, image.config.height, 360, 360);
        return preview(image, fit.width, fit.height, cancelled);
    }
    static Bitmap preview(PreparedImage image, ImageCancellation cancelled) {
        return preview(image, image.config.labelWidthDots, image.config.height, cancelled);
    }
    private static Bitmap preview(PreparedImage image, int width, int height, ImageCancellation cancelled) {
        int sourceWidth = image.config.labelWidthDots, sourceHeight = image.config.height;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        int[] row = new int[width];
        for (int y = 0; y < height; y++) {
            if (cancelled.getAsBoolean()) { bitmap.recycle(); throw new CancellationException(); }
            int sourceY = (int)((long)y * sourceHeight / height);
            for (int x = 0; x < width; x++) {
                int sourceX = (int)((long)x * sourceWidth / width);
                int value = image.grayAt(sourceY * sourceWidth + sourceX);
                row[x] = 0xff000000 | value * 0x010101;
            }
            bitmap.setPixels(row, 0, width, 0, y, width, 1);
        }
        return bitmap;
    }
}
