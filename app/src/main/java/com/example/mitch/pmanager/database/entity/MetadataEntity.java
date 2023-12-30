package com.example.mitch.pmanager.database.entity;

import static com.example.mitch.pmanager.util.Encryption.ENCRYPT_RETURN_CIPHERTEXT;
import static com.example.mitch.pmanager.util.Encryption.ENCRYPT_RETURN_IV;
import static com.example.mitch.pmanager.util.Encryption.encrypt;
import static com.example.mitch.pmanager.util.Encryption.generateSalt;
import static com.example.mitch.pmanager.util.Encryption.getMetadataPlaintext;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.example.mitch.pmanager.models.EncryptedPassword;

// TODO: Use this
@Entity(tableName = "metadata")
public class MetadataEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;
    @ColumnInfo(name = "last_updated")
    private long lastUpdated;
    @ColumnInfo(name = "folder_count")
    private long folderCount;
    @ColumnInfo(name = "associated_data")
    private byte[] ad;
    @ColumnInfo(name = "salt")
    private byte[] salt;
    @ColumnInfo(name = "iv")
    private byte[] iv;
    @ColumnInfo(name = "encrypted_check")
    private byte[] encrypted;

    public MetadataEntity(long id, long lastUpdated, long folderCount, byte[] salt, byte[] iv, byte[] encrypted) {
        this.id = id;
        this.lastUpdated = lastUpdated;
        this.folderCount = folderCount;
        this.salt = salt;
        this.iv = iv;
        this.encrypted = encrypted;
    }

    @Ignore
    public MetadataEntity(long lastUpdated, long folderCount, EncryptedPassword password) {
        this.salt = generateSalt();
        update(lastUpdated, folderCount, password);
    }

    public void update(long lastUpdated, long folderCount, EncryptedPassword password) {
        this.lastUpdated = lastUpdated;
        this.folderCount = folderCount;
        this.ad = generateSalt();

        byte[] plaintext = getMetadataPlaintext(lastUpdated, folderCount);

        byte[][] encrypted = encrypt(plaintext, this.ad, password.getKey(salt));

        this.iv = encrypted[ENCRYPT_RETURN_IV];
        this.encrypted = encrypted[ENCRYPT_RETURN_CIPHERTEXT];
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public long getFolderCount() {
        return folderCount;
    }

    public void setFolderCount(long folderCount) {
        this.folderCount = folderCount;
    }

    public byte[] getAd() {
        return ad;
    }

    public void setAd(byte[] ad) {
        this.ad = ad;
    }

    public byte[] getSalt() {
        return salt;
    }

    public void setSalt(byte[] salt) {
        this.salt = salt;
    }

    public byte[] getIv() {
        return iv;
    }

    public void setIv(byte[] iv) {
        this.iv = iv;
    }

    public byte[] getEncrypted() {
        return encrypted;
    }

    public void setEncrypted(byte[] encrypted) {
        this.encrypted = encrypted;
    }
}
