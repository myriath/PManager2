package com.example.mitch.pmanager.models;

import static com.example.mitch.pmanager.util.Constants.STRING_ENCODING;
import static com.example.mitch.pmanager.util.Encryption.AES;
import static com.example.mitch.pmanager.util.Encryption.AES_GCM_NOPADDING;
import static com.example.mitch.pmanager.util.Encryption.GCM_IV_LENGTH;
import static com.example.mitch.pmanager.util.StringsUtil.bytesToChars;
import static com.example.mitch.pmanager.util.StringsUtil.charsToBytes;

import android.os.Parcel;
import android.os.Parcelable;

import com.example.mitch.pmanager.util.KeyStoreUtil;

import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptedPassword implements Parcelable {
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
    public static final String KEY_ALIAS = "app_key";

    private final byte[] encrypted;
    private final byte[] iv;

    // TODO: Delete this, maybe encryption too
    public EncryptedPassword(char[] password) {
        this(charsToBytes(password));
        Arrays.fill(password, (char) 0);
    }

    public EncryptedPassword(byte[] password) {
        try {
            final KeyStore.SecretKeyEntry entry = KeyStoreUtil.getEntry(KEY_ALIAS, null);
            final Cipher cipher = Cipher.getInstance(AES_GCM_NOPADDING);
            cipher.init(Cipher.ENCRYPT_MODE, entry.getSecretKey());
            iv = cipher.getIV();
            encrypted = cipher.doFinal(password);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            Arrays.fill(password, (byte) 0);
        }
    }

    protected EncryptedPassword(Parcel in) {
        encrypted = in.readBlob();
        iv = in.readBlob();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeBlob(encrypted);
        dest.writeBlob(iv);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<EncryptedPassword> CREATOR = new Creator<EncryptedPassword>() {
        @Override
        public EncryptedPassword createFromParcel(Parcel in) {
            return new EncryptedPassword(in);
        }

        @Override
        public EncryptedPassword[] newArray(int size) {
            return new EncryptedPassword[size];
        }
    };

    public SecretKeySpec getKey(byte[] salt) {
        SecretKeyFactory factory;
        try {
            factory = SecretKeyFactory.getInstance(PBKDF2_HMAC_SHA256);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error creating factory");
        }

        byte[] decrypted;
        try {
            final KeyStore.SecretKeyEntry entry = KeyStoreUtil.getEntry(KEY_ALIAS, null);
            final Cipher cipher = Cipher.getInstance(AES_GCM_NOPADDING);
            cipher.init(Cipher.DECRYPT_MODE, entry.getSecretKey(), new GCMParameterSpec(GCM_IV_LENGTH * 8, iv));
            decrypted = cipher.doFinal(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting with app key");
        }
        char[] password = bytesToChars(decrypted);
        Arrays.fill(decrypted, (byte) 0);
        KeySpec spec = new PBEKeySpec(password, salt, ITERATION_COUNT, KEY_LENGTH);
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
     * Gets the original unencrypted password as a string.
     * ONLY USE FOR UPGRADING V2 FILES!
     *
     * @return String of the unencrypted password.
     */
    @Deprecated
    public String getPassword() {
        try {
            final KeyStore.SecretKeyEntry entry = KeyStoreUtil.getEntry(KEY_ALIAS, null);
            final Cipher cipher = Cipher.getInstance(AES_GCM_NOPADDING);
            cipher.init(Cipher.DECRYPT_MODE, entry.getSecretKey(), new GCMParameterSpec(GCM_IV_LENGTH * 8, iv));
            return new String(cipher.doFinal(encrypted), STRING_ENCODING);
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting password");
        }
    }
}
