package com.netumscan.printersdk.demo;

import java.io.*;
import java.util.zip.CRC32;

/** Bounded PNG eXIf reader for API 26+, where the framework did not always support PNG EXIF. */
final class PngOrientation {
    private static final int METADATA_LIMIT = 65536;
    static int read(File file) throws IOException {
        int orientation = 1; boolean seen = false;
        try (RandomAccessFile input = new RandomAccessFile(file, "r")) {
            input.seek(8);
            while (input.getFilePointer() < input.length()) {
                if (input.length() - input.getFilePointer() < 12) throw new IOException("Invalid PNG chunk");
                long length = Integer.toUnsignedLong(input.readInt()); int type = input.readInt();
                long end = input.getFilePointer() + length + 4;
                if (end > input.length()) throw new IOException("Invalid PNG chunk");
                if (type == 0x65584966) { // eXIf contains TIFF, without the JPEG Exif prefix.
                    if (seen || length > METADATA_LIMIT) throw new IOException("Invalid PNG EXIF");
                    seen = true; byte[] metadata = new byte[(int)length]; input.readFully(metadata);
                    CRC32 crc = new CRC32(); crc.update(new byte[]{101,88,73,102}); crc.update(metadata);
                    if (crc.getValue() != Integer.toUnsignedLong(input.readInt())) throw new IOException("Invalid PNG EXIF checksum");
                    orientation = tiff(metadata);
                } else input.seek(end);
                if (type == 0x49454e44) {
                    if (length != 0 || end != input.length()) throw new IOException("Invalid PNG end");
                    return orientation;
                }
            }
        }
        throw new IOException("Incomplete PNG");
    }
    static int tiff(byte[] data) throws IOException {
        if (data.length < 8) throw new IOException("Invalid TIFF");
        boolean little = data[0] == 'I' && data[1] == 'I';
        if (!little && !(data[0] == 'M' && data[1] == 'M')) throw new IOException("Invalid TIFF byte order");
        if (number(data, 2, 2, little) != 42) throw new IOException("Invalid TIFF marker");
        long offset = number(data, 4, 4, little);
        if (offset < 8 || offset > data.length - 2) throw new IOException("Invalid TIFF directory");
        int count = (int)number(data, (int)offset, 2, little);
        if (count > 4096 || offset + 2 + (long)count * 12 + 4 > data.length) throw new IOException("Invalid TIFF entries");
        int orientation = 1; boolean seen = false;
        for (int index = 0; index < count; index++) {
            int entry = (int)offset + 2 + index * 12;
            if (number(data, entry, 2, little) != 274) continue;
            if (seen || number(data, entry + 2, 2, little) != 3 || number(data, entry + 4, 4, little) != 1)
                throw new IOException("Invalid TIFF orientation");
            seen = true; orientation = (int)number(data, entry + 8, 2, little);
            if (orientation < 1 || orientation > 8) throw new IOException("Invalid TIFF orientation");
        }
        return orientation;
    }
    private static long number(byte[] data, int offset, int bytes, boolean little) throws IOException {
        if (offset < 0 || offset > data.length - bytes) throw new IOException("Invalid TIFF offset");
        long value = 0;
        for (int index = 0; index < bytes; index++) value = (value << 8) | (data[offset + (little ? bytes - 1 - index : index)] & 255);
        return value;
    }
}
