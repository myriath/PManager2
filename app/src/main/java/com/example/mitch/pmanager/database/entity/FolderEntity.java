package com.example.mitch.pmanager.database.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "folders")
public class FolderEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;
    @ColumnInfo(name = "last_accessed")
    private long lastAccessed;
    @ColumnInfo(name = "label")
    private byte[] label;
    @ColumnInfo(name = "key")
    private byte[] encryptedKey;
    @ColumnInfo(name = "iv")
    private byte[] iv;
    @ColumnInfo(name = "json")
    private byte[] encryptedJson;

    public FolderEntity(long id, long lastAccessed, byte[] label, byte[] encryptedKey, byte[] iv, byte[] encryptedJson) {
        this.id = id;
        this.lastAccessed = lastAccessed;
        this.label = label;
        this.encryptedKey = encryptedKey;
        this.iv = iv;
        this.encryptedJson = encryptedJson;
    }

    @Ignore
    public FolderEntity(long lastAccessed, byte[] encryptedKey) {
        this.lastAccessed = lastAccessed;
        this.encryptedKey = encryptedKey;
    }

    public long getId() {
        return id;
    }

    public long getLastAccessed() {
        return lastAccessed;
    }

    public byte[] getLabel() {
        return label;
    }

    public byte[] getEncryptedKey() {
        return encryptedKey;
    }

    public byte[] getIv() {
        return iv;
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

    public void setLabel(byte[] label) {
        this.label = label;
    }

    public void setEncryptedKey(byte[] encryptedKey) {
        this.encryptedKey = encryptedKey;
    }

    public void setIv(byte[] iv) {
        this.iv = iv;
    }

    public void setEncryptedJson(byte[] encryptedJson) {
        this.encryptedJson = encryptedJson;
    }
}
