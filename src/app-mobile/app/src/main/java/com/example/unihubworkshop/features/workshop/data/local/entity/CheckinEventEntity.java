package com.example.unihubworkshop.features.workshop.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "pending_checkins")
public class CheckinEventEntity {
    @PrimaryKey
    @NonNull
    public String clientEventId = "";
    
    public String studentId;
    public String workshopId;
    public String registrationId;
    public String qrCode;
    public String staffId;
    public String deviceId;
    public String checkinAt;
    public long createdAt;

    public CheckinEventEntity() {}

    public CheckinEventEntity(@NonNull String clientEventId, String studentId, String workshopId, String registrationId, String qrCode, String staffId, String deviceId, String checkinAt, long createdAt) {
        this.clientEventId = clientEventId;
        this.studentId = studentId;
        this.workshopId = workshopId;
        this.registrationId = registrationId;
        this.qrCode = qrCode;
        this.staffId = staffId;
        this.deviceId = deviceId;
        this.checkinAt = checkinAt;
        this.createdAt = createdAt;
    }
}
