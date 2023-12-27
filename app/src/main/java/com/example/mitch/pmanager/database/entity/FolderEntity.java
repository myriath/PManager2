package com.example.mitch.pmanager.database.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "data")
public class FolderEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;
    @ColumnInfo(name = "last_accessed")
    private long lastAccessed;
    @ColumnInfo(name = "iv")
    private byte[] iv;
    @ColumnInfo(name = "salt")
    private byte[] salt;
    @ColumnInfo(name = "json")
    private byte[] encryptedJson;

    public FolderEntity(long lastAccessed, byte[] iv, byte[] salt, byte[] encryptedJson) {
        this.lastAccessed = lastAccessed;
        this.iv = iv;
        this.salt = salt;
        this.encryptedJson = encryptedJson;
    }

    public FolderEntity(long id, long lastAccessed, byte[] iv, byte[] salt, byte[] encryptedJson) {
        this.id = id;
        this.lastAccessed = lastAccessed;
        this.iv = iv;
        this.salt = salt;
        this.encryptedJson = encryptedJson;
    }

    public long getId() {
        return id;
    }

    public long getLastAccessed() {
        return lastAccessed;
    }

    public byte[] getIv() {
        return iv;
    }

    public byte[] getSalt() {
        return salt;
    }

    public byte[] getEncryptedJson() {
        return encryptedJson;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setLastAccessed(long lastAccessed) {
        this.lastAccessed = lastAccessed;
    }

    public void setIv(byte[] iv) {
        this.iv = iv;
    }

    public void setSalt(byte[] salt) {
        this.salt = salt;
    }

    public void setEncryptedJson(byte[] encryptedJson) {
        this.encryptedJson = encryptedJson;
    }
}
