package com.example.mitch.pmanager.database.entity;

import static com.example.mitch.pmanager.util.Constants.Extensions.DB;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.io.File;

@Entity(tableName = "files")
public class FileEntity implements Parcelable {
    @PrimaryKey(autoGenerate = true)
    private long id;
    @ColumnInfo(name = "display_name")
    private String displayName;
    @ColumnInfo(name = "duplicate_num")
    private long duplicateNumber;
    @Ignore
    private long size;
    @Ignore
    private MetadataEntity metadata;
    @Ignore
    private boolean corrupt = false;

    @Ignore
    public FileEntity(String displayName) {
        this.displayName = displayName;
    }

    public FileEntity(long id, String displayName, long duplicateNumber) {
        this.id = id;
        this.displayName = displayName;
        this.duplicateNumber = duplicateNumber;
    }

    @Ignore
    protected FileEntity(Parcel in) {
        id = in.readLong();
        displayName = in.readString();
        duplicateNumber = in.readLong();
        size = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(displayName);
        dest.writeLong(duplicateNumber);
        dest.writeLong(size);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<FileEntity> CREATOR = new Creator<FileEntity>() {
        @Override
        public FileEntity createFromParcel(Parcel in) {
            return new FileEntity(in);
        }

        @Override
        public FileEntity[] newArray(int size) {
            return new FileEntity[size];
        }
    };

    public void setSize(long size) {
        this.size = size;
    }

    public long getSize() {
        return size;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public long getDuplicateNumber() {
        return duplicateNumber;
    }

    public void setDuplicateNumber(long duplicateNumber) {
        this.duplicateNumber = duplicateNumber;
    }

    public String getFilename() {
        return id + DB;
    }

    public File getFile(Context context) {
        return context.getDatabasePath(getFilename());
    }

    public MetadataEntity getMetadata() {
        return metadata;
    }

    public void setMetadata(MetadataEntity metadata) {
        this.metadata = metadata;
    }

    public boolean isCorrupt() {
        return corrupt;
    }

    public void setCorrupt(boolean corrupt) {
        this.corrupt = corrupt;
    }
}
