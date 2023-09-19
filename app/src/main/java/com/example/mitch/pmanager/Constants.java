package com.example.mitch.pmanager;

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

    public enum CallbackCodes {
        LOAD_FILE
    }

    public static final String SP_NAME = "PManager_Prefs";
    public static final String SP_DIR_IMPORT_EXPORT = "PManager_Import_Export";

    public static final String IMPORTS_SUBDIR = "PManagerImports";
    public static final String EXPORTS_SUBDIR = "PManagerExports";
}
