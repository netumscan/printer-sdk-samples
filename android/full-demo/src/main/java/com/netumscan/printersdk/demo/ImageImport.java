package com.netumscan.printersdk.demo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import java.io.*;
import java.util.concurrent.CancellationException;
import com.netumscan.printersdk.demo.PrinterController.Cancellation;

/** Platform decoder. Its caller serializes imports and owns the temporary source file. */
final class ImageImport {
    static void copy(InputStream input, File destination, Cancellation cancelled) throws IOException {
        long count = 0;
        try (OutputStream output = new FileOutputStream(destination)) {
            byte[] block = new byte[16384]; int read;
            while ((read = input.read(block)) != -1) {
                if (cancelled.getAsBoolean()) throw new CancellationException();
                count += read;
                if (count > ImageRaster.MAX_FILE_BYTES) throw new IOException("Image limit");
                output.write(block, 0, read);
            }
        }
        if (count == 0) throw new IOException("Empty image");
    }
    static PreparedImage prepare(File file, LabelConfig config, Cancellation cancelled) throws IOException {
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
        ImageRaster.Layout fit = new ImageRaster.Layout(orientation >= 5 ? bounds.outHeight : bounds.outWidth,
            orientation >= 5 ? bounds.outWidth : bounds.outHeight, config.labelWidthDots, config.height);
        BitmapFactory.Options options = new BitmapFactory.Options(); options.inScaled = false;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        options.inSampleSize = ImageRaster.sample(bounds.outWidth, bounds.outHeight, fit, orientation);
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
        byte[] pixels = ImageRaster.render(argb, width, height, orientation, fit, config.labelWidthDots, config.height, cancelled);
        return new PreparedImage(config, pixels);
    }
    static Bitmap preview(PreparedImage image) { return preview(image, () -> false); }
    /** Bounded display-only sample of the immutable submission canvas. */
    static Bitmap thumbnail(PreparedImage image, Cancellation cancelled) {
        int sourceWidth = image.config.labelWidthDots, sourceHeight = image.config.height;
        double scale = Math.min(1.0, 360.0 / Math.max(sourceWidth, sourceHeight));
        int width = Math.max(1, (int)(sourceWidth * scale)), height = Math.max(1, (int)(sourceHeight * scale));
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
    static Bitmap preview(PreparedImage image, Cancellation cancelled) {
        int width = image.config.labelWidthDots, height = image.config.height;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        int[] row = new int[width];
        for (int y = 0; y < height; y++) {
            if (cancelled.getAsBoolean()) { bitmap.recycle(); throw new CancellationException(); }
            for (int x = 0; x < width; x++) {
                int value = image.grayAt(y * width + x); row[x] = 0xff000000 | value * 0x010101;
            }
            bitmap.setPixels(row, 0, width, 0, y, width, 1);
        }
        return bitmap;
    }
}
