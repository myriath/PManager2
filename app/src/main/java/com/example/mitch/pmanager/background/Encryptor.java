package com.example.mitch.pmanager.background;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

/**
 * @author mitch
 * <p>
 * Basic class using AEAD and PBKDF2 to create a password-based data encryption methods
 * This should securely encrypt plaintext data using a password.
 * The returned encrypted data includes a randomly generated salt for the password, a randomly generated iv, and the ciphertext.
 */
public class Encryptor {
    /**
     * The length of the salt in bytes
     */
    public static final int SALT_LENGTH = 16;
    /**
     * Number of PBKDF2 iterations. Designed to slow down brute force attempts.
     */
    public static final int ITERATION_COUNT = 65536;
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
     * Generates a secret key given a password and salt.
     *
     * @param password Password to be given by the user.
     * @param salt     Salt to prevent dictionary attacks
     * @return SecretKey object for AES encryption
     */
    private static SecretKey generateKey(char[] password, byte[] salt) {
        SecretKeyFactory factory;
        try {
            factory = SecretKeyFactory.getInstance(PBKDF2_HMAC_SHA256);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error creating factory");
        }
        KeySpec spec = new PBEKeySpec(password, salt, ITERATION_COUNT, KEY_LENGTH);
        SecretKey tmp;
        try {
            tmp = factory.generateSecret(spec);
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException("Error creating secret");
        }
        return new SecretKeySpec(tmp.getEncoded(), "AES");
    }

    /**
     * Encrypts given data with AES using the given password and associated data.
     *
     * @param data           Byte[] of plaintext data to encrypt.
     * @param associatedData Associated data to preserve data integrity.
     * @param password       Password used to encrypt the data.
     * @return EncryptedData object containing the salt, iv, and ciphertext
     * @throws Exception Any error from the cipher process
     */
    public static EncryptedData encrypt(byte[] data, byte[] associatedData, char[] password) throws Exception {
        byte[] iv = new byte[GCM_IV_LENGTH];
        byte[] salt = new byte[SALT_LENGTH];

        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        random.nextBytes(salt);

        SecretKey key = generateKey(password, salt);

        Cipher cipher = Cipher.getInstance(AES_GCM_NOPADDING);
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv));
        cipher.updateAAD(associatedData);
        return new EncryptedData(salt, iv, cipher.doFinal(data));
    }

    /**
     * Decrypts an EncryptedData object from the encrypt function using the given associated data and password.
     *
     * @param data           Data to decrypt.
     * @param associatedData Associated data to preserve integrity.
     * @param password       Password to generate AES key with.
     * @return byte[] of plaintext data.
     * @throws Exception Any error from the cipher process.
     */
    public static byte[] decrypt(EncryptedData data, byte[] associatedData, char[] password) throws Exception {
        SecretKey key = generateKey(password, data.getSalt());
        Cipher cipher = Cipher.getInstance(AES_GCM_NOPADDING);
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH * 8, data.getIv()));
        cipher.updateAAD(associatedData);
        return cipher.doFinal(data.getCiphertext());
    }

    /**
     * Writes an EncryptedData object to the given file.
     * @param data Data to write
     * @param out File to output to
     */
    public static void writeEncrypted(EncryptedData data, File out) {
        try (FileOutputStream outputStream = new FileOutputStream(out)) {
            outputStream.write(data.getSalt());
            outputStream.write(data.getIv());
            outputStream.write(data.getCiphertext());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Fills out an EncryptedData object from a given input file
     * @param in File to read
     * @return EncryptedData object of the data in the file.
     */
    public static EncryptedData readFromFile(File in) {
        byte[] salt = new byte[SALT_LENGTH];
        byte[] iv = new byte[GCM_IV_LENGTH];
        byte[] ciphertext = new byte[(int) (in.length() - SALT_LENGTH - GCM_IV_LENGTH)];
        try (FileInputStream inputStream = new FileInputStream(in)) {
            inputStream.read(salt);
            inputStream.read(iv);
            inputStream.read(ciphertext);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new EncryptedData(salt, iv, ciphertext);
    }

    /**
     * Data class to hold the salt, iv, and ciphertext from encryption.
     */
    public static class EncryptedData {
        /**
         * Salt to add to the password for this piece of data.
         */
        private final byte[] salt;
        /**
         * IV for the AES_GCM encryption.
         */
        private final byte[] iv;
        /**
         * Encrypted ciphertext.
         */
        private final byte[] ciphertext;

        /**
         * Constructor
         *
         * @param salt       Salt for this data
         * @param iv         IV for this data
         * @param ciphertext Encrypted ciphertext
         */
        public EncryptedData(byte[] salt, byte[] iv, byte[] ciphertext) {
            this.salt = salt;
            this.iv = iv;
            this.ciphertext = ciphertext;
        }

        /**
         * Getter for the salt
         *
         * @return salt
         */
        public byte[] getSalt() {
            return salt;
        }

        /**
         * Getter for the IV
         *
         * @return IV
         */
        public byte[] getIv() {
            return iv;
        }

        /**
         * Getter for the ciphertext
         *
         * @return encrypted ciphertext
         */
        public byte[] getCiphertext() {
            return ciphertext;
        }
    }
}
