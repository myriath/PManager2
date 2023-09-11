package com.example.mitch.pmanager.objects;

import java.io.Serializable;

/**
 * Data class for a stored password entry
 */
public class PasswordEntry implements Serializable {

    /**
     * Stored domain
     * @serial
     */
    public char[] domain;
    /**
     * Stored username
     * @serial
     */
    public char[] username;
    /**
     * Stored password
     * @serial
     */
    public char[] password;
    /**
     * Index in the list
     */
    public int index;

    /**
     * Constructor
     * @param domain Domain to store
     * @param username Username to store
     * @param password Password to store
     * @param i Index for this entry
     */
    public PasswordEntry(char[] domain, char[] username, char[] password, int i) {
        this.domain = domain;
        this.username = username;
        this.password = password;
        index = i;
    }

    /**
     * UID for proper Serializable implementation
     */
    private static final long serialVersionUID = 1L;
}
