package com.example.mitch.pmanager.util;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * Constants class for holding various constants
 */
public class Constants {
    public static final Charset STRING_ENCODING = StandardCharsets.UTF_8;
    public static final String BACKUP_EXTENSION = ".bak";

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

    /**
     * Version enum for storing versions and extensions
     */
    @Deprecated
    public enum Version {
        /**
         * Used in case of no extension
         */
        //NULL(""),
        V2(".jpweds"),
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

    /**
     * Intent key string constants
     */
    public enum IntentKeys {
        FILE("file"),
        FILEDATA("filedata"),
        FILENAME("filename"),
        PASSWORD("password");

        public final String key;

        IntentKeys(String key) {
            this.key = key;
        }
    }

    public static final String CALLBACK_CODE = "op";
    public static final String CALLBACK_FILEKEY = "pwd";
    public static final String CALLBACK_FILE = "file";

    public enum CallbackCodes {
        DELETE_FILE, LOAD_FILE, EXPORT_FILE
    }

    public static int DP16;
}
