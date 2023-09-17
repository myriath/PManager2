package com.example.mitch.pmanager.objects.storage;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * PasswordBank class for new file storage schema
 * New storage allows multiple entries per domain
 */
public class PasswordBank implements Serializable {
    /**
     * Hashmap of data to store
     */
    private final HashMap<String, ArrayList<UserEntry>> bank;

    /**
     * Default constructor
     * Creates a new hashmap
     */
    public PasswordBank() {
        bank = new HashMap<>();
    }

    /**
     * Hashmap constructor
     * @param bank Bank to use
     */
    public PasswordBank(HashMap<String, ArrayList<UserEntry>> bank) {
        this.bank = bank;
    }

    /**
     * Gets the arraylist of entries for the given domain
     * @param domain Domain to retrieve entries for
     * @return ArrayList of entries
     */
    public ArrayList<UserEntry> getEntries(String domain) {
        return bank.get(domain);
    }

    /**
     * Creates an empty domain with a new ArrayList
     * @param domain Domain to create
     * @return ArrayList for later use
     */
    public ArrayList<UserEntry> createDomain(String domain) {
        ArrayList<UserEntry> entries = new ArrayList<>();
        bank.put(domain, entries);
        return entries;
    }

    /**
     * Sets the given domain to the given entries list
     * @param domain Domain to set
     * @param entries ArrayList for the domain
     */
    public void addEntries(String domain, ArrayList<UserEntry> entries) {
        bank.put(domain, entries);
    }

    private static final long serialVersionUID = 1L;
}
