package com.example.mitch.pmanager.objects.storage;

import java.io.Serializable;
import java.util.Arrays;

/**
 * UserEntry stores username and password as char[]
 */
public class UserEntry implements Serializable {
    /**
     * Username for the entry
     */
    private char[] username;
    /**
     * Password for the entry
     */
    private char[] password;

    /**
     * Constructor
     *
     * @param username Username for this entry
     * @param password Password for this entry
     */
    public UserEntry(char[] username, char[] password) {
        this.username = username;
        this.password = password;
    }

    public UserEntry(UserEntry toClone) {
        this.username = new char[toClone.username.length];
        this.password = new char[toClone.password.length];

        int i;
        for (i = 0; i < username.length; i++) {
            username[i] = toClone.username[i];
        }
        for (i = 0; i < password.length; i++) {
            password[i] = toClone.password[i];
        }
    }

    /**
     * Username getter
     *
     * @return username
     */
    public char[] getUsername() {
        return username;
    }

    /**
     * Password getter
     *
     * @return password
     */
    public char[] getPassword() {
        return password;
    }

    /**
     * Special set method that ensures the deletion of old username
     *
     * @param username New username
     */
    public void setUsername(char[] username) {
        Arrays.fill(this.username, (char) 0);
        this.username = username;
    }

    /**
     * Special set method that ensures the deletion of old password
     *
     * @param password New password
     */
    public void setPassword(char[] password) {
        Arrays.fill(this.password, (char) 0);
        this.password = password;
    }

    /**
     * Prepares this UserEntry for garbage collection
     * Done manually to protect the sensitive data
     */
    public void destroy() {
        Arrays.fill(username, (char) 0);
        Arrays.fill(password, (char) 0);
        username = null;
        password = null;
    }

    private static final long serialVersionUID = 1L;
}
