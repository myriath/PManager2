package com.example.mitch.pmanager.util;

/**
 * Constants class for holding various constants
 */
public class Constants {
    /**
     * Version enum for storing versions and extensions
     */
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

    public static final String BACKUP_EXTENSION = ".bak";

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
