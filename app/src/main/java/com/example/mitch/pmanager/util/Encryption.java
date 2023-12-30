package com.example.mitch.pmanager.util;

import static com.example.mitch.pmanager.util.ByteUtil.longToBytes;
import static com.example.mitch.pmanager.util.Constants.STRING_ENCODING;

import androidx.annotation.Nullable;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

// TODO: Associated data is hashed then used for encryption. The file's display name should be used I think

/**
 * Utility class for encryption and hashing
 *
 * @author mitch
 */
public class Encryption {
    /**
     * Random used for generating salts
     */
    public static final SecureRandom RANDOM = new SecureRandom();
    /**
     * The length of the salt in bytes
     */
    public static final int SALT_LENGTH = 16;
    /**
     * IV length for GCM in bytes
     */
    public static final int GCM_IV_LENGTH = 16;
    /**
     * Tag length for GCM in bytes
     */
    public static final int GCM_TAG_LENGTH = 16;
    /**
     * Algorithm string for the AES encryption
     */
    public static final String AES_GCM_NOPADDING = "AES/GCM/NoPadding";
    /**
     * Algorithm string for final key type
     */
    public static final String AES = "AES";

    public static final int SHA_512_BYTES = 64;
    public static final MessageDigest SHA_512;
    public static final String SHA_ALG = "SHA-512";

    public static final int ENCRYPT_RETURN_IV = 0;
    public static final int ENCRYPT_RETURN_CIPHERTEXT = 1;

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

    public static byte[] getAssociatedData(long id, long time, byte[] hash) {
        if (hash.length != SHA_512_BYTES) throw new IllegalStateException("Hash incorrect length!");
        byte[] associatedData = new byte[Long.BYTES + Long.BYTES + SHA_512_BYTES];
        System.arraycopy(longToBytes(id), 0, associatedData, 0, Long.BYTES);
        System.arraycopy(longToBytes(time), 0, associatedData, Long.BYTES, Long.BYTES);
        System.arraycopy(hash, 0, associatedData, Long.BYTES + Long.BYTES, SHA_512_BYTES);
        return associatedData;
    }

    public static byte[] getMetadataPlaintext(long time, long folderCount) {
        byte[] preHash = new byte[Long.BYTES + Long.BYTES];
        System.arraycopy(longToBytes(time), 0, preHash, 0, Long.BYTES);
        System.arraycopy(longToBytes(folderCount), 0, preHash, 0, Long.BYTES);
        return SHA512(preHash);
    }

    public static byte[] generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        RANDOM.nextBytes(salt);
        return salt;
    }

    public static boolean compareHashes(byte[] hash0, byte[] hash1) {
        if (hash0.length != hash1.length && hash0.length != SHA_512_BYTES) return false;
        for (int i = 0; i < SHA_512_BYTES; i++) {
            if (hash0[i] != hash1[i]) return false;
        }
        return true;
    }

    /**
     * Encrypts a given byte[] of data with AEAD and the given key
     *
     * @param data           Data to encrypt
     * @param associatedData Associated data to authorize the encryption
     * @param key            Key to encrypt with
     * @return IV and encrypted data packaged in a byte[][]
     */
    public static byte[][] encrypt(byte[] data, @Nullable byte[] associatedData, SecretKey key) {
        try {
            Cipher cipher = Cipher.getInstance(AES_GCM_NOPADDING);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            if (associatedData != null) cipher.updateAAD(associatedData);

            return new byte[][]{cipher.getIV(), cipher.doFinal(data)};
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException |
                 IllegalBlockSizeException | BadPaddingException e) {
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

    public static final String TAR_DB = "DB";
    public static final String TAR_WAL = "WAL";
    public static final String TAR_SHM = "SHM";

    public static void exportFile() {

    }

    public static void exportTar(ExportedFile db, ExportedFile wal, ExportedFile shm, OutputStream out) {
        try (
                BufferedOutputStream bos = new BufferedOutputStream(out);
                GZIPOutputStream gzip = new GZIPOutputStream(bos);
                TarArchiveOutputStream tar = new TarArchiveOutputStream(gzip)
        ) {
            writeTar(tar, TAR_DB, db);
            writeTar(tar, TAR_WAL, wal);
            writeTar(tar, TAR_SHM, shm);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeTar(TarArchiveOutputStream tar, String name, ExportedFile data) throws IOException {
        TarArchiveEntry entry = new TarArchiveEntry(name);
        entry.setSize(data.iv.length + data.salt.length + data.ciphertext.length);

        tar.putArchiveEntry(entry);
        tar.write(data.iv);
        tar.write(data.salt);
        tar.write(data.ciphertext);
        tar.closeArchiveEntry();
    }

    public static ExportedFile[] importTar(InputStream in) {
        ExportedFile[] files = new ExportedFile[3];
        try (
                BufferedInputStream bis = new BufferedInputStream(in);
                GZIPInputStream gunzip = new GZIPInputStream(bis);
                TarArchiveInputStream tar = new TarArchiveInputStream(gunzip)
        ) {
            ArchiveEntry entry;
            while ((entry = tar.getNextEntry()) != null) {
                byte[] iv = new byte[GCM_IV_LENGTH];
                byte[] salt = new byte[SALT_LENGTH];
                byte[] ciphertext = new byte[(int) (entry.getSize() - GCM_IV_LENGTH - SALT_LENGTH)];
                tar.read(iv);
                tar.read(salt);
                tar.read(ciphertext);
                ExportedFile file = new ExportedFile(iv, salt, ciphertext);
                switch (entry.getName()) {
                    case TAR_DB: {
                        files[0] = file;
                        break;
                    }
                    case TAR_WAL: {
                        files[1] = file;
                        break;
                    }
                    case TAR_SHM: {
                        files[2] = file;
                        break;
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return files;
    }

    public static class ExportedFile {
        private final byte[] iv;
        private final byte[] salt;
        private final byte[] ciphertext;

        public ExportedFile(byte[][] encrypted, byte[] salt) {
            iv = encrypted[ENCRYPT_RETURN_IV];
            this.salt = salt;
            ciphertext = encrypted[ENCRYPT_RETURN_CIPHERTEXT];
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
