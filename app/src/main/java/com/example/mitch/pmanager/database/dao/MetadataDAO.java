package com.example.mitch.pmanager.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.mitch.pmanager.database.entity.FolderEntity;
import com.example.mitch.pmanager.database.entity.MetadataEntity;

@Dao
public interface MetadataDAO {
    @Query("SELECT * FROM metadata WHERE id = :id")
    MetadataEntity getMeta(long id);

    @Insert
    long insert(FolderEntity folder);

    @Update
    void update(FolderEntity folder);

    @Delete
    void delete(FolderEntity folder);
}
