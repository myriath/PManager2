package com.example.mitch.pmanager.models;

import static com.example.mitch.pmanager.util.Constants.Encryption.AES_GCM_NOPADDING;
import static com.example.mitch.pmanager.util.Constants.Encryption.GCM_IV_LENGTH;
import static com.example.mitch.pmanager.util.Constants.Encryption.GCM_TAG_LENGTH;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Data class that stores an encrypted value and its iv
 *
 * @author mitch
 */
public class EncryptedValue implements Parcelable {
    /**
     * Ciphertext for the encrypted value
     */
    private final byte[] ciphertext;
    /**
     * IV used for encryption
     */
    private final byte[] iv;

    /**
     * Encrypts the given plaintext with the given key. Optional associated data for integrity check
     * @param plaintext Plaintext to encrypt
     * @param associatedData Optional associated data for integrity check
     * @param key Key for encryption
     */
    public EncryptedValue(byte[] plaintext, @Nullable byte[] associatedData, SecretKey key) {
        try {
            final Cipher cipher = Cipher.getInstance(AES_GCM_NOPADDING);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            if (associatedData != null) cipher.updateAAD(associatedData);
            iv = cipher.getIV();
            ciphertext = cipher.doFinal(plaintext);
            Arrays.fill(plaintext, (byte) 0);
        } catch (NoSuchPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException |
                 BadPaddingException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Generic constructor from existing data
     * @param ciphertext Ciphertext
     * @param iv IV
     */
    public EncryptedValue(byte[] ciphertext, byte[] iv) {
        this.ciphertext = ciphertext;
        this.iv = iv;
    }

    protected EncryptedValue(Parcel in) {
        ciphertext = in.readBlob();
        iv = in.readBlob();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeBlob(ciphertext);
        dest.writeBlob(iv);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<EncryptedValue> CREATOR = new Creator<EncryptedValue>() {
        @Override
        public EncryptedValue createFromParcel(Parcel in) {
            return new EncryptedValue(in);
        }

        @Override
        public EncryptedValue[] newArray(int size) {
            return new EncryptedValue[size];
        }
    };

    public byte[] getCiphertext() {
        return ciphertext;
    }

    /**
     * Gets the decrypted plaintext from the stored ciphertext and given associated data and key
     * @param associatedData Optional associated data for integrity check
     * @param key Key for decryption
     * @return Plaintext bytes
     * @throws Exception when decryption fails (usually incorrect key)
     */
    public byte[] getDecrypted(@Nullable byte[] associatedData, SecretKey key) throws Exception {
        final Cipher cipher = Cipher.getInstance(AES_GCM_NOPADDING);
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv));
        if (associatedData != null) cipher.updateAAD(associatedData);

        return cipher.doFinal(ciphertext);
    }

    public byte[] getIv() {
        return iv;
    }
}
