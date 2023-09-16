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
        NULL(""),
        V2(".jpweds"),
        V3(".pm3");

        public final String ext;
        Version(String ext) {
            this.ext = ext;
        }
    }

    /**
     * File data intent constant
     */
    public static final String STATE_FILEDATA = "filedata";
    /**
     * File name intent constant
     */
    public static final String STATE_FILENAME = "filename";
    /**
     * Password intent constant
     */
    public static final String STATE_PASSWORD = "password";
    /**
     * File intent constant
     */
    public static final String STATE_FILE = "file";
}
