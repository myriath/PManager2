package com.example.mitch.pmanager.util;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * Constants class for holding various constants
 */
public class Constants {
    public static final Charset STRING_ENCODING = StandardCharsets.UTF_8;

    public interface Encryption {
        /**
         * Random used for generating salts
         */
        SecureRandom RANDOM = new SecureRandom();
        /**
         * The length of the salt in bytes
         */
        int SALT_LENGTH = 16;
        /**
         * IV length for GCM in bytes
         */
        int GCM_IV_LENGTH = 12;
        /**
         * Old IV length in bytes
         */
        int GCM_IV_LENGTH_OLD = 16;
        /**
         * Tag length for GCM in bytes
         */
        int GCM_TAG_LENGTH = 16;
        /**
         * Algorithm string for the AES encryption
         */
        String AES_GCM_NOPADDING = "AES/GCM/NoPadding";
        /**
         * Algorithm string for final key type
         */
        String AES = "AES";
    }

    public static final int BUFFER_SIZE = 2048;

    /**
     * Version enum for storing versions and extensions
     */
    @Deprecated
    public enum Version {
        V3(".pm3");

        public final String ext;

        Version(String ext) {
            this.ext = ext;
        }
    }

    public interface Extensions {
        String V2 = ".jpweds";
        String V3 = ".pm3";
        String V4 = ".pm4";
        String DB = ".db";
        String WAL = "-wal";
        String SHM = "-shm";
    }

    public interface IntentKeys {
        String FILE = "file";
        String FILEDATA = "filedata";
        String KEY = "key";
    }

    public enum CallbackCodes {
        LOAD_FILE, EXPORT_FILE
    }

    public interface BundleKeys {
        String BUNDLE_OP = "op";
        String BUNDLE_KEY = "key";
        String BUNDLE_FILE = "file";
        String BUNDLE_FOLDER = "folder";
    }

    public static int DP16;
}
