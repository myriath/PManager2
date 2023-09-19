package com.example.mitch.pmanager.objects.storage;

import com.example.mitch.pmanager.interfaces.Writable;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * PasswordBank class for new file storage schema
 * New storage allows multiple entries per domain
 */
public class PasswordBank implements Serializable, Writable {
    /**
     * Hashmap of data to store
     */
    private final ArrayList<DomainEntry> bank;

    public PasswordBank() {
        bank = new ArrayList<>();
    }

    /**
     * ArrayList constructor
     *
     * @param bank Bank to use
     */
    public PasswordBank(ArrayList<DomainEntry> bank) {
        this.bank = bank;
    }

    /**
     * Gets the DomainEntry for a given domain
     *
     * @param domain Domain to retrieve DomainEntry for
     * @return DomainEntry of given domain
     */
    public DomainEntry getEntry(String domain) {
        for (DomainEntry domainEntry : bank) {
            if (domainEntry.getDomain().equals(domain)) return domainEntry;
        }
        return null;
    }

    /**
     * Creates an empty domain with a new ArrayList
     *
     * @param domain Domain to create
     * @return DomainEntry for later use
     */
    public DomainEntry createDomain(String domain) {
        for (DomainEntry domainEntry : bank) {
            if (domainEntry.getDomain().equals(domain)) return domainEntry;
        }
        DomainEntry entry = new DomainEntry(new ArrayList<>(), domain);
        bank.add(entry);
        return entry;
    }

    /**
     * Sets the given domain to the given entries list
     *
     * @param domain  Domain to set
     * @param entries ArrayList for the domain
     */
    public void addEntries(String domain, ArrayList<UserEntry> entries) {
        createDomain(domain).getEntries().addAll(entries);
    }

    public ArrayList<DomainEntry> getEntries() {
        return bank;
    }

    private static final long serialVersionUID = 1L;
}
