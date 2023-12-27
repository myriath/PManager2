package com.example.mitch.pmanager.database.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.mitch.pmanager.database.dao.FileDAO;
import com.example.mitch.pmanager.database.dao.FolderDAO;
import com.example.mitch.pmanager.database.entity.FileEntity;

@Database(entities = FileEntity.class, version = 3)
public abstract class FolderDatabase extends RoomDatabase {
    private static FolderDatabase singleton;

    public static synchronized FolderDatabase singleton(Context context, String dbName) {
        if (singleton != null) return singleton;
        singleton = Room.databaseBuilder(context.getApplicationContext(), FolderDatabase.class, dbName)
                .fallbackToDestructiveMigration()
                .build();
        return singleton;
    }

    public abstract FolderDAO folderDAO();
}
