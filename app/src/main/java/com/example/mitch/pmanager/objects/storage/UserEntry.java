package com.example.mitch.pmanager.objects.storage;

import java.io.Serializable;

/**
 * UserEntry stores username and password as char[]
 */
public class UserEntry implements Serializable {
    /**
     * Username for the entry
     */
    private final char[] username;
    /**
     * Password for the entry
     */
    private final char[] password;

    /**
     * Constructor
     * @param username Username for this entry
     * @param password Password for this entry
     */
    public UserEntry(char[] username, char[] password) {
        this.username = username;
        this.password = password;
    }

    /**
     * Username getter
     * @return username
     */
    public char[] getUsername() {
        return username;
    }

    /**
     * Password getter
     * @return password
     */
    public char[] getPassword() {
        return password;
    }

    private static final long serialVersionUID = 1L;
}
