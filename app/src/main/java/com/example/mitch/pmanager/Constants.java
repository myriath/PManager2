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
        FILEDATA("filedata"),
        FILENAME("filename"),
        PASSWORD("password");

        public final String key;
        IntentKeys(String key) {
            this.key = key;
        }
    }
}
