package com.example.mitch.pmanager.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.mitch.pmanager.database.entity.FileEntity;
import com.example.mitch.pmanager.database.entity.FolderEntity;

import java.util.List;

@Dao
public interface FolderDAO {
    @Query("SELECT * FROM data")
    List<FolderEntity> getFolders();
    @Insert
    long insert(FolderEntity folder);
    @Update
    void update(FolderEntity folder);
    @Delete
    void delete(FolderEntity folder);
}
