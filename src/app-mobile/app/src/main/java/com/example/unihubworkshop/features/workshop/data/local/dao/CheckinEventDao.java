package com.example.unihubworkshop.features.workshop.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.unihubworkshop.features.workshop.data.local.entity.CheckinEventEntity;

import java.util.List;

@Dao
public interface CheckinEventDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(CheckinEventEntity event);

    @Query("SELECT * FROM pending_checkins ORDER BY createdAt ASC")
    List<CheckinEventEntity> getPendingEvents();

    @Query("DELETE FROM pending_checkins WHERE clientEventId IN (:clientEventIds)")
    void deleteSynced(List<String> clientEventIds);
}
