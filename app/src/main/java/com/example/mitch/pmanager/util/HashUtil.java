package com.example.mitch.pmanager.util;

import static com.example.mitch.pmanager.util.Constants.STRING_ENCODING;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for encryption and hashing
 *
 * @author mitch
 */
public class HashUtil {
    public static final int SHA_512_BYTES = 64;
    public static final MessageDigest SHA_512;
    public static final String SHA_ALG = "SHA-512";

    static {
        try {
            SHA_512 = MessageDigest.getInstance(SHA_ALG);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] SHA512(byte[] bytes) {
        return SHA_512.digest(bytes);
    }

    public static byte[] SHA512(String message) {
        return SHA512(message.getBytes(STRING_ENCODING));
    }

    public static boolean compareHashes(byte[] hash0, byte[] hash1) {
        if (hash0.length != hash1.length && hash0.length != SHA_512_BYTES) return false;
        for (int i = 0; i < SHA_512_BYTES; i++) {
            if (hash0[i] != hash1[i]) return false;
        }
        return true;
    }
}
