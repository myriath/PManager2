package com.example.mitch.pmanager.database.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "files")
public class FileEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;
    @ColumnInfo(name = "display_name")
    private String displayName;

    public FileEntity(String displayName) {
        this.displayName = displayName;
    }

    public FileEntity(int id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public long getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
