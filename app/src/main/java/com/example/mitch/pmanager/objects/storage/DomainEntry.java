package com.example.mitch.pmanager.objects.storage;

import java.io.Serializable;
import java.util.ArrayList;

public class DomainEntry implements Serializable {
    private final ArrayList<UserEntry> entries;
    private final ArrayList<Boolean> shown = new ArrayList<>();
    private final String domain;

    public DomainEntry(ArrayList<UserEntry> entries, String domain) {
        this.entries = entries;
        this.domain = domain;
        for (int i = 0; i < entries.size(); i++) {
            shown.add(false);
        }
    }

    public void remove(int position) {
        shown.remove(position);
        entries.remove(position);
    }

    public void add(UserEntry entry) {
        shown.add(false);
        entries.add(entry);
    }

    public void replace(int position, UserEntry entry) {
        shown.set(position, false);
        entries.set(position, entry);
    }

    public boolean getShown(int i) {
        return shown.get(i);
    }

    public void setShown(int pos, boolean newShown) {
        shown.set(pos, newShown);
    }

    public ArrayList<UserEntry> getEntries() {
        return entries;
    }

    public String getDomain() {
        return domain;
    }

    private static final long serialVersionUID = 1L;
}
