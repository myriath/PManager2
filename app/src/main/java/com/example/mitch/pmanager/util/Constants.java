package com.example.mitch.pmanager.util;

/**
 * Constants class for holding various constants
 */
public class Constants {
    public static final String BACKUP_EXTENSION = ".bak";

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
    public static final String CALLBACK_PWD = "pwd";
    public static final String CALLBACK_FILE = "file";

    public enum CallbackCodes {
        DELETE_FILE, LOAD_FILE, EXPORT_FILE
    }

    public static int DP16;
}
