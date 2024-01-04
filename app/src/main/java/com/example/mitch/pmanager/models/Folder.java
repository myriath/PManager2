package com.example.mitch.pmanager.models;

import static com.example.mitch.pmanager.models.Entry.Types.BASIC;

import android.os.Parcel;
import android.os.Parcelable;

import com.example.mitch.pmanager.database.entity.FolderEntity;
import com.example.mitch.pmanager.objects.storage.DomainEntry;
import com.example.mitch.pmanager.objects.storage.UserEntry;
import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Folder data object.
 * Holds a list of entries and has a label
 *
 * @author mitch
 */
public class Folder implements Parcelable {
    /**
     * Entity reference for updating the database
     */
    private FolderEntity entity;
    /**
     * Label for the folder
     */
    @Expose
    private String label;
    /**
     * List of entries in this folder
     */
    @Expose
    private final ArrayList<Entry> entries;

    /**
     * Constructs a folder from a v3 domain structure
     *
     * @param domain Domain for constructing
     */
    public Folder(DomainEntry domain) {
        entries = new ArrayList<>();
        label = domain.getDomain();
        for (UserEntry entry : domain.getEntries()) {
            entries.add(new Entry(entry.getUsername(), entry.getPassword()));
        }
    }

    /**
     * Constructs a folder with an empty entries list
     *
     * @param label Label for the folder
     */
    public Folder(String label) {
        this(label, new ArrayList<>());
    }

    /**
     * Constructs a folder with a given entries list
     *
     * @param label   Label for the folder
     * @param entries Entries stored within
     */
    public Folder(String label, ArrayList<Entry> entries) {
        this.label = label;
        this.entries = entries;
    }

    protected Folder(Parcel in) {
        label = in.readString();
        entries = in.createTypedArrayList(Entry.CREATOR);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(label);
        dest.writeTypedList(entries);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Folder> CREATOR = new Creator<Folder>() {
        @Override
        public Folder createFromParcel(Parcel in) {
            return new Folder(in);
        }

        @Override
        public Folder[] newArray(int size) {
            return new Folder[size];
        }
    };

    public FolderEntity getEntity() {
        return entity;
    }

    public void setEntity(FolderEntity entity) {
        this.entity = entity;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public ArrayList<Entry> getEntries() {
        return entries;
    }

    public void setEntries(ArrayList<Entry> newEntries) {
        for (Entry entry : entries) {
            switch (entry.getType()) {
                case BASIC: {
                    Arrays.fill(entry.getLabel(), (char) 0);
                    Arrays.fill(entry.getSecret(), (char) 0);
                    break;
                }
            }
        }
        entries.clear();
        entries.addAll(newEntries);
    }
}
