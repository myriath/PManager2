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

    public DomainEntry(DomainEntry toClone) {
        this.entries = new ArrayList<>();
        this.domain = toClone.getDomain();
        clone(toClone);
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
        entries.get(position).destroy();
        entries.set(position, entry);
    }

    public boolean getShown(int i) {
        return shown.get(i);
    }

    public void destroy() {
        for (int i = 0; i < getSize(); i++) {
            entries.get(0).destroy();
            entries.remove(0);
            shown.remove(0);
        }
    }

    public void clone(DomainEntry toClone) {
        for (UserEntry entry : toClone.getEntries()) {
            add(new UserEntry(entry));
        }
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

    public int getSize() {
        return entries.size();
    }

    private static final long serialVersionUID = 1L;

    public UserEntry getEntry(int i) {
        return entries.get(i);
    }
}
