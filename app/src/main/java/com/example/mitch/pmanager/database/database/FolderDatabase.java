package com.example.mitch.pmanager.database.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.mitch.pmanager.database.dao.FolderDAO;
import com.example.mitch.pmanager.database.dao.MetadataDAO;
import com.example.mitch.pmanager.database.entity.FolderEntity;
import com.example.mitch.pmanager.database.entity.MetadataEntity;

import java.util.HashMap;

@Database(entities = {FolderEntity.class, MetadataEntity.class}, exportSchema = false, version = 4)
public abstract class FolderDatabase extends RoomDatabase {
    private static final HashMap<String, FolderDatabase> singleton = new HashMap<>();

    public static synchronized FolderDatabase singleton(Context context, String dbName) {
        FolderDatabase database = singleton.get(dbName);
        if (database == null) {
            database = Room.databaseBuilder(context.getApplicationContext(), FolderDatabase.class, dbName)
                    .fallbackToDestructiveMigration()
                    .build();
            singleton.put(dbName, database);
        }
        return database;
    }

    public static void destroy() {
        for (String key : singleton.keySet()) {
            FolderDatabase db = singleton.get(key);
            if (db != null && db.isOpen()) db.close();
            singleton.put(key, null);
        }
    }

    public abstract FolderDAO folderDAO();

    public abstract MetadataDAO metadataDAO();
}
