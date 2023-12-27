package com.example.mitch.pmanager.util;

import java.security.KeyStore;
import java.security.KeyStoreException;

public class KeyStoreUtil {
    public static final String ANDROID_KS = "AndroidKeyStore";
    private static final KeyStore ks;

    static {
        try {
            ks = KeyStore.getInstance(ANDROID_KS);
        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Util class, private constructor
     */
    private KeyStoreUtil() {}


}
