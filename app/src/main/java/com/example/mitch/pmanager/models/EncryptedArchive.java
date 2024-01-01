package com.example.mitch.pmanager.models;

/**
 * Represents an export archive, used with tar to compress and store exported files
 *
 * @author mitch
 */
public class EncryptedArchive {
    /**
     * Encrypted database file
     */
    private final EncryptedValue db;
    /**
     * Encrypted wal file
     */
    private final EncryptedValue wal;
    /**
     * Encrypted shm file
     */
    private final EncryptedValue shm;
    /**
     * Salt used for encrypting
     */
    private final byte[] salt;

    public EncryptedArchive(EncryptedValue db, EncryptedValue wal, EncryptedValue shm, byte[] salt) {
        this.db = db;
        this.wal = wal;
        this.shm = shm;
        this.salt = salt;
    }

    public EncryptedValue getDb() {
        return db;
    }

    public EncryptedValue getWal() {
        return wal;
    }

    public EncryptedValue getShm() {
        return shm;
    }

    public byte[] getSalt() {
        return salt;
    }
}
