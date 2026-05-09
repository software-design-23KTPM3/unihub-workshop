package com.example.unihubworkshop.features.workshop.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.unihubworkshop.features.workshop.data.local.entity.RegistrationEntity;

import java.util.List;

@Dao
public interface RegistrationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<RegistrationEntity> registrations);

    @Query("SELECT * FROM registrations WHERE (id = :qrCode OR qrCode = :qrCode) AND workshopId = :workshopId LIMIT 1")
    RegistrationEntity findByQrCode(String qrCode, String workshopId);

    @Query("UPDATE registrations SET status = 'CHECKED_IN' WHERE id = :registrationId")
    void markAsCheckedIn(String registrationId);

    @Query("SELECT COUNT(*) FROM registrations WHERE workshopId = :workshopId AND status = 'CHECKED_IN'")
    LiveData<Integer> getCheckedInCount(String workshopId);
}
