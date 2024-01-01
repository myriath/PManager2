package com.example.mitch.pmanager.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.mitch.pmanager.database.entity.FolderEntity;
import com.example.mitch.pmanager.database.entity.MetadataEntity;

import java.util.List;

@Dao
public interface MetadataDAO {
    @Query("SELECT * FROM metadata")
    List<MetadataEntity> getMeta();

    @Insert
    long insert(MetadataEntity entity);

    @Update
    void update(MetadataEntity entity);

    @Delete
    void delete(MetadataEntity entity);
}
