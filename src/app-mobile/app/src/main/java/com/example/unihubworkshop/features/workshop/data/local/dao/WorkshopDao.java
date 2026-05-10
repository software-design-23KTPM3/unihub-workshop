package com.example.unihubworkshop.features.workshop.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.unihubworkshop.features.workshop.data.local.entity.WorkshopEntity;

import java.util.List;

@Dao
public interface WorkshopDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<WorkshopEntity> workshops);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(WorkshopEntity workshop);

    @Query("SELECT * FROM workshops")
    List<WorkshopEntity> getAllWorkshops();

    @Query("SELECT * FROM workshops WHERE id = :id LIMIT 1")
    WorkshopEntity getWorkshopById(String id);

    @Query("SELECT * FROM workshops WHERE id = :id LIMIT 1")
    LiveData<WorkshopEntity> getWorkshopLiveDataById(String id);
}
