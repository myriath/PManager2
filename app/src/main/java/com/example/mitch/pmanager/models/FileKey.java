package com.example.mitch.pmanager.models;

import static com.example.mitch.pmanager.util.Encryption.AES;
import static com.example.mitch.pmanager.util.Encryption.RANDOM;

import com.example.mitch.pmanager.util.KeyStoreUtil;

import java.security.KeyStore;
import java.security.spec.KeySpec;
import java.util.Arrays;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Holds an encrypted AES key, intended to be generated from a password and salt using PBEKeySpec
 * A file key is used to generate encrypted folder keys that are stored alongside each folder in the folder database
 * The salt and check for the file key is stored in the folder database's metadata table
 * <p>
 * Each individual folder created will create a random AES key that is then encrypted by the file key.
 * This allows unique secret keys for every folder without regenerating one from the raw password and a random salt,
 * which is expensive.
 *
 * @author mitch
 */
public class FileKey {
    /**
     * Stores the encrypted file key
     */
    private final EncryptedValue keyValue;

    /**
     * Algorithm string for the secret key generation
     */
    public static final String PBKDF2_HMAC_SHA256 = "PBKDF2WithHmacSHA256";
    /**
     * Number of PBKDF2 iterations. Designed to slow down brute force attempts.
     */
    public static final int ITERATION_COUNT = 65536;
    /**
     * AES key length in bits.
     */
    public static final int KEY_LENGTH = 256;

    /**
     * Creates a new file key given an encrypted file key
     *
     * @param encryptedFileKey Encrypted value that is supposed to be the file key
     */
    public FileKey(EncryptedValue encryptedFileKey) {
        this.keyValue = encryptedFileKey;
    }

    /**
     * Generates the encrypted file key from a password and salt.
     * The password and salt are used with PBEKeySpec to generate an AES key, which is then encrypted
     * using the application AES key
     *
     * @param password Password used for key generation
     * @param salt     Salt used for key generation
     */
    public FileKey(char[] password, byte[] salt) {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_HMAC_SHA256);
            KeySpec spec = new PBEKeySpec(password, salt, ITERATION_COUNT, KEY_LENGTH);
            Arrays.fill(password, (char) 0);
            KeyStore.SecretKeyEntry entry = KeyStoreUtil.getApplicationKey();
            keyValue = new EncryptedValue(factory.generateSecret(spec).getEncoded(), null, entry.getSecretKey());
        } catch (Exception e) {
            throw new RuntimeException("Error generating key");
        }
    }

    /**
     * Generates a new random folder key and returns it encrypted
     *
     * @return Encrypted folder key that has been randomly generated with a CSPRNG
     */
    public EncryptedValue generateFolderKey() {
        try {
            KeyStore.SecretKeyEntry applicationKey = KeyStoreUtil.getApplicationKey();

            byte[] secret = new byte[KEY_LENGTH / 8];
            RANDOM.nextBytes(secret);

            byte[] folderKey = keyValue.getDecrypted(null, applicationKey.getSecretKey());
            EncryptedValue encrypted = new EncryptedValue(secret, null, new SecretKeySpec(folderKey, AES));

            Arrays.fill(folderKey, (byte) 0);
            Arrays.fill(secret, (byte) 0);

            return encrypted;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Encrypts the given data with the file key.
     * Usually used for encrypting metadata in the metadata table to check that the user entered
     * the correct password
     *
     * @param plaintext      Data to encrypt
     * @param associatedData Associated data for the encryption
     * @return Encrypted data
     */
    public EncryptedValue encrypt(byte[] plaintext, byte[] associatedData) {
        try {
            KeyStore.SecretKeyEntry applicationKey = KeyStoreUtil.getApplicationKey();

            byte[] folderKey = keyValue.getDecrypted(null, applicationKey.getSecretKey());
            EncryptedValue encrypted = new EncryptedValue(plaintext, associatedData, new SecretKeySpec(folderKey, AES));
            Arrays.fill(folderKey, (byte) 0);

            return encrypted;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Decrypts the given EncryptedValue using the file key
     *
     * @param encrypted      Ciphertext and IV to decrypt
     * @param associatedData Associated data for the encryption
     * @return Decrypted plaintext
     * @throws Exception Thrown when decryption fails (usually incorrect key)
     */
    public byte[] decrypt(EncryptedValue encrypted, byte[] associatedData) throws Exception {
        KeyStore.SecretKeyEntry applicationKey;
        try {
            applicationKey = KeyStoreUtil.getApplicationKey();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        byte[] folderKey = keyValue.getDecrypted(null, applicationKey.getSecretKey());
        byte[] plaintext = encrypted.getDecrypted(associatedData, new SecretKeySpec(folderKey, AES));
        Arrays.fill(folderKey, (byte) 0);

        return plaintext;
    }

    /**
     * Encrypts the given data with the given encrypted folder key.
     *
     * @param encryptedFolderKey Folder key that was encrypted with the file key to use for encryption
     * @param plaintext          Plaintext to encrypt
     * @param associatedData     Associated data for the encryption.
     * @return EncryptedValue with ciphertext and IV
     */
    public EncryptedValue encryptWithFolderKey(EncryptedValue encryptedFolderKey, byte[] plaintext, byte[] associatedData) {
        try {
            KeyStore.SecretKeyEntry applicationKey = KeyStoreUtil.getApplicationKey();

            byte[] folderKey = keyValue.getDecrypted(null, applicationKey.getSecretKey());
            byte[] entryKey = encryptedFolderKey.getDecrypted(null, new SecretKeySpec(folderKey, AES));
            Arrays.fill(folderKey, (byte) 0);
            EncryptedValue encrypted = new EncryptedValue(plaintext, associatedData, new SecretKeySpec(entryKey, AES));
            Arrays.fill(entryKey, (byte) 0);

            return encrypted;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Decrypts a given EncryptedValue with the given folder key
     *
     * @param encryptedFolderKey Folder key encrypted by this file key to be used for decryption
     * @param encrypted          EncryptedValue containing the ciphertext and IV
     * @param associatedData     Associated data for the integrity check
     * @return Decrypted plaintext
     * @throws Exception Thrown when decryption fails (usually incorrect key)
     */
    public byte[] decryptWithFolderKey(EncryptedValue encryptedFolderKey, EncryptedValue encrypted, byte[] associatedData) throws Exception {
        KeyStore.SecretKeyEntry applicationKey;
        try {
            applicationKey = KeyStoreUtil.getApplicationKey();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        byte[] folderKey = keyValue.getDecrypted(null, applicationKey.getSecretKey());
        byte[] entryKey = encryptedFolderKey.getDecrypted(null, new SecretKeySpec(folderKey, AES));
        Arrays.fill(folderKey, (byte) 0);
        byte[] plaintext = encrypted.getDecrypted(associatedData, new SecretKeySpec(entryKey, AES));
        Arrays.fill(entryKey, (byte) 0);

        return plaintext;
    }
}
