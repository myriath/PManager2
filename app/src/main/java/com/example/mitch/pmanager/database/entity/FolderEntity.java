package com.example.mitch.pmanager.database.entity;

import static com.example.mitch.pmanager.util.ByteUtil.longToBytes;
import static com.example.mitch.pmanager.util.HashUtil.SHA_512_BYTES;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.example.mitch.pmanager.models.EncryptedValue;

@Entity(tableName = "folders")
public class FolderEntity implements Parcelable {
    @PrimaryKey(autoGenerate = true)
    private long id;
    @ColumnInfo(name = "last_accessed")
    private long lastAccessed;
    @ColumnInfo(name = "label")
    private byte[] label;
    @ColumnInfo(name = "key_iv")
    private byte[] keyIv;
    @ColumnInfo(name = "key")
    private byte[] keyCiphertext;
    @ColumnInfo(name = "iv")
    private byte[] iv;
    @ColumnInfo(name = "json")
    private byte[] encryptedJson;

    public FolderEntity(long id, long lastAccessed, byte[] label, byte[] keyIv, byte[] keyCiphertext, byte[] iv, byte[] encryptedJson) {
        this.id = id;
        this.lastAccessed = lastAccessed;
        this.label = label;
        this.keyIv = keyIv;
        this.keyCiphertext = keyCiphertext;
        this.iv = iv;
        this.encryptedJson = encryptedJson;
    }

    @Ignore
    public FolderEntity(long lastAccessed, EncryptedValue key) {
        this.lastAccessed = lastAccessed;
        this.keyIv = key.getIv();
        this.keyCiphertext = key.getCiphertext();
    }

    @Ignore
    protected FolderEntity(Parcel in) {
        id = in.readLong();
        lastAccessed = in.readLong();
        label = in.readBlob();
        keyIv = in.readBlob();
        keyCiphertext = in.readBlob();
        iv = in.readBlob();
        encryptedJson = in.readBlob();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeLong(lastAccessed);
        dest.writeBlob(label);
        dest.writeBlob(keyIv);
        dest.writeBlob(keyCiphertext);
        dest.writeBlob(iv);
        dest.writeBlob(encryptedJson);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<FolderEntity> CREATOR = new Creator<FolderEntity>() {
        @Override
        public FolderEntity createFromParcel(Parcel in) {
            return new FolderEntity(in);
        }

        @Override
        public FolderEntity[] newArray(int size) {
            return new FolderEntity[size];
        }
    };

    public byte[] getAssociatedData() {
        byte[] associatedData = new byte[Long.BYTES + Long.BYTES + SHA_512_BYTES];
        System.arraycopy(longToBytes(id), 0, associatedData, 0, Long.BYTES);
        System.arraycopy(longToBytes(lastAccessed), 0, associatedData, Long.BYTES, Long.BYTES);
        System.arraycopy(label, 0, associatedData, Long.BYTES + Long.BYTES, SHA_512_BYTES);
        return associatedData;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getLastAccessed() {
        return lastAccessed;
    }

    public void setLastAccessed(long lastAccessed) {
        this.lastAccessed = lastAccessed;
    }

    public byte[] getLabel() {
        return label;
    }

    public void setLabel(byte[] label) {
        this.label = label;
    }

    public byte[] getKeyIv() {
        return keyIv;
    }

    public void setKeyIv(byte[] keyIv) {
        this.keyIv = keyIv;
    }

    public byte[] getKeyCiphertext() {
        return keyCiphertext;
    }

    public void setKeyCiphertext(byte[] keyCiphertext) {
        this.keyCiphertext = keyCiphertext;
    }

    public byte[] getIv() {
        return iv;
    }

    public void setIv(byte[] iv) {
        this.iv = iv;
    }

    public byte[] getEncryptedJson() {
        return encryptedJson;
    }

    public void setEncryptedJson(byte[] encryptedJson) {
        this.encryptedJson = encryptedJson;
    }

    public EncryptedValue getKey() {
        return new EncryptedValue(keyCiphertext, keyIv);
    }

    public void setKey(EncryptedValue key) {
        this.keyIv = key.getIv();
        this.keyCiphertext = key.getCiphertext();
    }
}
