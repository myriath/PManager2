package com.example.mitch.pmanager.database.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.mitch.pmanager.database.dao.FileDAO;
import com.example.mitch.pmanager.database.entity.FileEntity;

@Database(entities = FileEntity.class, version = 3)
public abstract class FileDatabase extends RoomDatabase {
    private static final String DB_NAME = "files_db";
    private static FileDatabase singleton;

    public static synchronized FileDatabase singleton(Context context) {
        if (singleton != null) return singleton;
        singleton = Room.databaseBuilder(context.getApplicationContext(), FileDatabase.class, DB_NAME)
                .fallbackToDestructiveMigration()
                .build();
        return singleton;
    }

    public abstract FileDAO fileDAO();
}
