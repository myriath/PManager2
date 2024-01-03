package com.example.mitch.pmanager.database.entity;

import static com.example.mitch.pmanager.models.FileKey.generateSalt;
import static com.example.mitch.pmanager.util.ByteUtil.longToBytes;
import static com.example.mitch.pmanager.util.HashUtil.SHA512;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.example.mitch.pmanager.models.EncryptedValue;
import com.example.mitch.pmanager.models.FileKey;

import java.util.Arrays;

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
    @ColumnInfo(name = "ciphertext")
    private byte[] ciphertext;

    public MetadataEntity(long id, long lastUpdated, long folderCount, byte[] salt, byte[] iv, byte[] ciphertext) {
        this.id = id;
        this.lastUpdated = lastUpdated;
        this.folderCount = folderCount;
        this.salt = salt;
        this.iv = iv;
        this.ciphertext = ciphertext;
    }

    @Ignore
    public MetadataEntity(long lastUpdated, long folderCount, FileKey key) {
        update(lastUpdated, folderCount, key);
    }

    public void update(long lastUpdated, long folderCount, FileKey key) {
        this.lastUpdated = lastUpdated;
        this.folderCount = folderCount;
        this.ad = generateSalt();
        this.salt = key.getSalt();

        byte[] plaintext = getMetadataPlaintext(lastUpdated, folderCount);

        EncryptedValue value = key.encrypt(plaintext, this.ad);

        this.iv = value.getIv();
        this.ciphertext = value.getCiphertext();
    }

    public static byte[] getMetadataPlaintext(long time, long folderCount) {
        byte[] preHash = new byte[Long.BYTES + Long.BYTES];
        System.arraycopy(longToBytes(time), 0, preHash, 0, Long.BYTES);
        System.arraycopy(longToBytes(folderCount), 0, preHash, 0, Long.BYTES);
        return SHA512(preHash);
    }

    public boolean check(FileKey key) {
        try {
            return Arrays.equals(
                    key.decrypt(new EncryptedValue(ciphertext, iv), ad),
                    getMetadataPlaintext(lastUpdated, folderCount)
            );
        } catch (Exception e) {
            return false;
        }
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

    public byte[] getCiphertext() {
        return ciphertext;
    }

    public void setCiphertext(byte[] ciphertext) {
        this.ciphertext = ciphertext;
    }
}
