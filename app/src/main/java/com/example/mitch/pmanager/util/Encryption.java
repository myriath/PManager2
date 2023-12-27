package com.example.mitch.pmanager.util;

import static com.example.mitch.pmanager.util.ByteCharStringUtil.bytesToChars;
import static com.example.mitch.pmanager.util.ByteCharStringUtil.charsToBytes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

// TODO: Associated data is hashed then used for encryption. The file's display name should be used I think

/**
 * Utility class for encryption and hashing
 *
 * @author mitch
 */
public class Encryption {
    /**
     * Digest for hashing with SHA 512
     */
    public static final MessageDigest SHA512_DIGEST;

    static {
        try {
            SHA512_DIGEST = MessageDigest.getInstance("SHA-512");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * The length of the salt in bytes
     */
    public static final int SALT_LENGTH = 16;
    /**
     * Number of PBKDF2 iterations. Designed to slow down brute force attempts.
     */
    public static final int ITERATION_COUNT = 65536;
    /**
     * Number of PBKDF2 iterations. Designed to slow down brute force attempts.
     */
    public static final int KEY_ITERATION_COUNT = ITERATION_COUNT / 4;
    /**
     * AES key length in bits.
     */
    public static final int KEY_LENGTH = 256;
    /**
     * IV length for GCM in bytes
     */
    public static final int GCM_IV_LENGTH = 16;
    /**
     * Tag length for GCM in bytes
     */
    public static final int GCM_TAG_LENGTH = 16;
    /**
     * Algorithm string for the secret key generation
     */
    private static final String PBKDF2_HMAC_SHA256 = "PBKDF2WithHmacSHA256";
    /**
     * Algorithm string for the AES encryption
     */
    private static final String AES_GCM_NOPADDING = "AES/GCM/NoPadding";
    /**
     * Algorithm string for final key type
     */
    private static final String AES = "AES";
    /**
     * SHA512 algorithm string
     */
    public static final String SHA_512 = "SHA-512";

    /**
     * Bundle keys for async callbacks
     */
    public static final String BUNDLE_SECRET_KEY = "secret_key";
    public static final String BUNDLE_IV = "iv";
    public static final String BUNDLE_CIPHERTEXT = "ciphertext";
    public static final String BUNDLE_PLAINTEXT = "plaintext";

    /**
     * Hashes a given string with SHA 512
     * @param s String to hash
     * @return hashed bytes
     */
    public static byte[] SHA512(String s) {
        return SHA512(s.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Hashes a given byte[] with SHA 512
     * @param in bytes to hash
     * @return hashed bytes
     */
    public static byte[] SHA512(byte[] in) {
        return SHA512_DIGEST.digest(in);
    }

    /**
     * Hashes a given char[] with SHA 512
     * @param in chars to hash
     * @return hashed chars
     */
    public static byte[] SHA512(char[] in) {
        return SHA512(charsToBytes(in));
    }

    /**
     * THIS SHOULD BE RAN ASYNCHRONOUSLY!
     * Generates a secret key from a given password and salt
     * @param pwHash hash of the password
     * @param salt SALT_LENGTH bytes for salt
     */
    public static SecretKey generateKey(byte[] pwHash, byte[] salt) {
        return generateKey(bytesToChars(pwHash), salt);
    }

    /**
     * THIS SHOULD BE RAN ASYNCHRONOUSLY!
     * Generates a secret key from a given password and salt
     * @param password password to generate key from
     * @param salt SALT_LENGTH bytes for salt
     */
    public static SecretKey generateKey(char[] password, byte[] salt) {
        SecretKeyFactory factory;
        try {
            factory = SecretKeyFactory.getInstance(PBKDF2_HMAC_SHA256);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error creating factory");
        }
        KeySpec spec = new PBEKeySpec(password, salt, KEY_ITERATION_COUNT, KEY_LENGTH);
        Arrays.fill(password, (char) 0);
        SecretKey tmp;
        try {
            tmp = factory.generateSecret(spec);
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException("Error creating secret");
        }
        return new SecretKeySpec(tmp.getEncoded(), AES);
    }

    /**
     * Encrypts a given byte[] of data with AEAD and the given key
     * @param data Data to encrypt
     * @param associatedData Associated data to authorize the encryption
     * @param key Key to encrypt with
     * @return IV and encrypted data packaged in a byte[][]
     */
    public static byte[][] encrypt(byte[] data, byte[] associatedData, SecretKey key) {
        byte[] iv = new byte[GCM_IV_LENGTH];

        try {
            Cipher cipher = Cipher.getInstance(AES_GCM_NOPADDING);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv));
            cipher.updateAAD(associatedData);

            return new byte[][] {iv, cipher.doFinal(data)};
        } catch (InvalidAlgorithmParameterException | NoSuchPaddingException |
                 NoSuchAlgorithmException | InvalidKeyException | IllegalBlockSizeException |
                 BadPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Decrypts a given ciphertext
     * @param iv IV used for encryption
     * @param ciphertext Ciphertext to decrypt
     * @param associatedData Associated data for verification
     * @param key Key used for encryption
     * @return Decrypted plaintext bytes
     * @throws Exception Thrown when decryption fails
     */
    public static byte[] decrypt(byte[] iv, byte[] ciphertext, byte[] associatedData, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_GCM_NOPADDING);
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv));
        cipher.updateAAD(associatedData);

        return cipher.doFinal(ciphertext);
    }

    public static ExportedFile importFile(InputStream in) {
        byte[] salt = new byte[SALT_LENGTH];
        byte[] iv = new byte[GCM_IV_LENGTH];
        byte[] ciphertext;
        try {
            in.read(salt);
            in.read(iv);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int i;
            while ((i = in.read(buf, 0, buf.length)) != -1) {
                baos.write(buf, 0, i);
            }
            ciphertext = baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new ExportedFile(iv, salt, ciphertext);
    }

    public static void exportFile(ExportedFile f, OutputStream out) {
        try {
            out.write(f.salt);
            out.write(f.iv);
            out.write(f.ciphertext);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static class ExportedFile {
        private final byte[] iv;
        private final byte[] salt;
        private final byte[] ciphertext;

        public ExportedFile(byte[][] encrypted, byte[] salt) {
            iv = encrypted[0];
            this.salt = salt;
            ciphertext = encrypted[1];
        }

        public ExportedFile(byte[] iv, byte[] salt, byte[] ciphertext) {
            this.iv = iv;
            this.salt = salt;
            this.ciphertext = ciphertext;
        }

        public byte[] getIv() {
            return iv;
        }

        public byte[] getSalt() {
            return salt;
        }

        public byte[] getCiphertext() {
            return ciphertext;
        }
    }
}
