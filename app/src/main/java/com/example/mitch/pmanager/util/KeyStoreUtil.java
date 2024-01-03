package com.example.mitch.pmanager.util;

import static com.example.mitch.pmanager.util.Constants.Encryption.AES;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import androidx.annotation.Nullable;

import java.security.KeyStore;
import java.security.KeyStoreException;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class KeyStoreUtil {
    private static final String ANDROID_KS = "AndroidKeyStore";
    private static final KeyStore KEY_STORE;
    public static final String APPLICATION_KEY_ALIAS = "app_key";

    /**
     * Used for unit testing where AndroidKeyStore is unavailable
     */
    private static byte[] customApplicationKey;

    static {
        KeyStore tempKeyStore;
        try {
            tempKeyStore = KeyStore.getInstance(ANDROID_KS);
        } catch (KeyStoreException e) {
            // Skip throw if not testing, AndroidKeyStore doesn't exist in unit tests
            if (!isTesting()) throw new IllegalStateException(e);
            tempKeyStore = null;
        }
        KEY_STORE = tempKeyStore;
    }

    /**
     * Util class, private constructor
     */
    private KeyStoreUtil() {
    }

    /**
     * Checks if the app is running in a unit test
     *
     * @return True if this is a test
     */
    public static boolean isTesting() {
        try {
            Class.forName("com.example.mitch.pmanager.EncryptionTest");
        } catch (ClassNotFoundException e) {
            return false;
        }
        return true;
    }

    public static void setCustomApplicationKey(byte[] key) {
        customApplicationKey = key;
    }

    public static KeyStore.SecretKeyEntry getApplicationKey() throws Exception {
        if (isTesting())
            return new KeyStore.SecretKeyEntry(new SecretKeySpec(customApplicationKey, AES));
        return getEntry(APPLICATION_KEY_ALIAS, null);
    }

    public static KeyStore.SecretKeyEntry getEntry(String alias, @Nullable KeyStore.ProtectionParameter protParam) throws Exception {
        KEY_STORE.load(null);
        KeyStore.Entry entry = KEY_STORE.getEntry(alias, protParam);
        if (entry != null) return (KeyStore.SecretKeyEntry) entry;
        final KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KS);
        final KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(alias,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build();
        generator.init(spec);
        SecretKey ignored = generator.generateKey();
        return (KeyStore.SecretKeyEntry) KEY_STORE.getEntry(alias, protParam);
    }
}
