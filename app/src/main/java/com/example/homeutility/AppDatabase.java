package com.example.homeutility;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import android.content.Context;
import android.util.Log;

@Database(entities = {UtilityRecord.class}, version = 1, exportSchema = false)
@TypeConverters({})
public abstract class AppDatabase extends RoomDatabase {

    public abstract UtilityDao utilityDao();

    private static volatile AppDatabase INSTANCE;
    private static final String DATABASE_NAME = "utility_database.db";

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    try {
                        INSTANCE = Room.databaseBuilder(
                                        context.getApplicationContext(),
                                        AppDatabase.class,
                                        DATABASE_NAME)
                                .fallbackToDestructiveMigration()
                                .build();

                        Log.d("AppDatabase", "База данных создана: " + DATABASE_NAME);
                    } catch (Exception e) {
                        Log.e("AppDatabase", "Ошибка создания базы данных: " + e.getMessage(), e);
                        throw e;
                    }
                }
            }
        }
        return INSTANCE;
    }

    public static void closeDatabase() {
        if (INSTANCE != null && INSTANCE.isOpen()) {
            INSTANCE.close();
            INSTANCE = null;
            Log.d("AppDatabase", "База данных закрыта");
        }
    }

    public boolean isOpen() {
        return INSTANCE != null && INSTANCE.getOpenHelper().getWritableDatabase().isOpen();
    }
}