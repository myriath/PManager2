package com.example.mitch.pmanager.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.mitch.pmanager.database.entity.FileEntity;

import java.util.List;

@Dao
public interface FileDAO {
    @Query("SELECT * FROM files")
    List<FileEntity> getFiles();

    @Insert
    long insert(FileEntity file);

    @Update
    void update(FileEntity file);

    @Delete
    void delete(FileEntity file);
}
