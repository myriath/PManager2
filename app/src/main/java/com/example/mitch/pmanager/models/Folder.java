package com.example.mitch.pmanager.models;

import com.example.mitch.pmanager.objects.storage.DomainEntry;
import com.example.mitch.pmanager.objects.storage.UserEntry;

import java.util.ArrayList;

public class Folder {
    private String label;
    private ArrayList<Entry> entries = new ArrayList<>();

    public Folder(DomainEntry domain) {
        label = domain.getDomain();
        for (UserEntry entry : domain.getEntries()) {
            entries.add(new Entry(entry.getUsername(), entry.getPassword()));
        }
    }

    public Folder(String label) {
        this.label = label;
    }

    public Folder(String label, ArrayList<Entry> entries) {
        this.label = label;
        this.entries = entries;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public ArrayList<Entry> getEntries() {
        if (entries == null) entries = new ArrayList<>();
        return entries;
    }

    public void setEntries(ArrayList<Entry> entries) {
        this.entries = entries;
    }
}
