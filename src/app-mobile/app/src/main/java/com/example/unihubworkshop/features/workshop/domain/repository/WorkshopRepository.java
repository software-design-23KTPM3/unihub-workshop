package com.example.unihubworkshop.features.workshop.domain.repository;

import androidx.lifecycle.LiveData;

import com.example.unihubworkshop.features.workshop.domain.entity.WorkShop;
// Note: We might need a Registration domain model or just boolean for verify
import java.util.List;

public interface WorkshopRepository {
    List<WorkShop> getWorkshops();
    WorkShop getWorkshopById(String id);
    void updateRegistrationStatus(String workshopId, boolean isRegistered, String registrationId, int attendanceCount);
    
    void registerForWorkshop(String workshopId, java.util.function.Consumer<Boolean> callback);
    
    void fetchAndCacheRegistrations(String workshopId);
    LiveData<Integer> getLocalAttendanceCount(String workshopId);
    boolean verifyOfflineCheckin(String qrCode, String workshopId);
    
    WorkShop getWorkshopByIdLocal(String id);
    LiveData<WorkShop> getWorkshopLiveData(String id);
    void syncWorkshopAndRegistrations(String id);
    LiveData<List<com.example.unihubworkshop.features.workshop.data.local.entity.RegistrationEntity>> getRegistrationDetails(String workshopId);

    enum CheckinResult {
        SUCCESS,
        SUCCESS_ONLINE,
        SUCCESS_OFFLINE,
        ALREADY_CHECKED_IN,
        INVALID_TICKET,
        ERROR
    }
    CheckinResult verifyOfflineCheckinDetailed(String qrCode, String workshopId);
}
