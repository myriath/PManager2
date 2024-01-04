package com.example.mitch.pmanager.util;

import java.nio.ByteBuffer;

/**
 * Utility class for converting bytes
 *
 * @author <a href="https://stackoverflow.com/a/4485196">source</a>
 */
public class ByteUtil {
    /**
     * Buffer used for conversions
     */
    private static final ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);

    /**
     * Util class, private constructor
     */
    private ByteUtil() {
    }

    /**
     * Converts a long into a byte array
     *
     * @param l long to convert
     * @return byte[] of the long data
     */
    public static byte[] longToBytes(long l) {
        buffer.putLong(0, l);
        return buffer.array();
    }

    /**
     * Converts a byte[] into a long
     *
     * @param bytes byte[] to convert
     * @return long of the data
     */
    public static long bytesToLong(byte[] bytes) {
        buffer.put(bytes, 0, bytes.length);
        buffer.flip();
        return buffer.getLong();
    }
}
