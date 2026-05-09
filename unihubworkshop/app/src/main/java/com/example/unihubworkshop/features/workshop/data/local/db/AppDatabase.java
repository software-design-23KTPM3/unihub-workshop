package com.example.unihubworkshop.features.workshop.data.local.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.unihubworkshop.features.workshop.data.local.dao.CheckinEventDao;
import com.example.unihubworkshop.features.workshop.data.local.dao.RegistrationDao;
import com.example.unihubworkshop.features.workshop.data.local.dao.WorkshopDao;
import com.example.unihubworkshop.features.workshop.data.local.entity.CheckinEventEntity;
import com.example.unihubworkshop.features.workshop.data.local.entity.RegistrationEntity;
import com.example.unihubworkshop.features.workshop.data.local.entity.WorkshopEntity;

@Database(entities = {RegistrationEntity.class, CheckinEventEntity.class, WorkshopEntity.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract RegistrationDao registrationDao();
    public abstract CheckinEventDao checkinEventDao();
    public abstract WorkshopDao workshopDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "unihub_room.db")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
