package com.example.mitch.pmanager.util;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import androidx.annotation.Nullable;

import java.security.KeyStore;
import java.security.KeyStoreException;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class KeyStoreUtil {
    private static final String ANDROID_KS = "AndroidKeyStore";
    private static final KeyStore KEY_STORE;
    public static final String APPLICATION_KEY_ALIAS = "app_key";

    static {
        try {
            KEY_STORE = KeyStore.getInstance(ANDROID_KS);
        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Util class, private constructor
     */
    private KeyStoreUtil() {
    }

    public static KeyStore.SecretKeyEntry getApplicationKey() throws Exception {
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
