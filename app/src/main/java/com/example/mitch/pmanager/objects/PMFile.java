package com.example.mitch.pmanager.objects;

import com.example.mitch.pmanager.database.entity.FileEntity;
import com.example.mitch.pmanager.interfaces.Writable;
import com.example.mitch.pmanager.util.Constants;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * Data class represents a saved file
 * Outdated, replaced by {@link FileEntity} and database structure
 * @noinspection ALL
 */
@Deprecated
public class PMFile implements Serializable, Writable {
    /**
     * File to write the encrypted data to
     */
    private File file;
    /**
     * File version for future-proofing
     *
     * @serial
     */
    private final Constants.Version version;
    /**
     * Password entries arraylist for the stored data
     * @serial
     */
    private final ArrayList<PasswordEntry> passwordEntries;

    /**
     * Constructor
     * @param version Version for this file
     * @param entries Password entries
     */
    public PMFile(Constants.Version version, ArrayList<PasswordEntry> entries, File file) {
        this.version = version;
        this.passwordEntries = entries;
        this.file = file;
    }

    /**
     * Setter for the output file
     * @param file File to write to
     */
    public void setFile(File file) {
        this.file = file;
    }

    /**
     * Getter for the output file
     * @return output file
     */
    public File getFile() {
        return file;
    }

    /**
     * Getter for the version
     * @return version number
     */
    public Constants.Version getVersion() {
        return version;
    }

    /**
     * Getter for the password entries
     *
     * @return password entries
     */
    public ArrayList<PasswordEntry> getPasswordEntries() {
        return passwordEntries;
    }

    private static final long serialVersionUID = 1L;
}
