package com.example.unihubworkshop.features.workshop.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "registrations")
public class RegistrationEntity {
    @PrimaryKey
    @NonNull
    public String id = "";
    
    public String workshopId;
    public String studentId;
    public String studentName;
    public String qrCode;
    public String status;

    public RegistrationEntity() {}

    public RegistrationEntity(@NonNull String id, String workshopId, String studentId, String studentName, String qrCode, String status) {
        this.id = id;
        this.workshopId = workshopId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.qrCode = qrCode;
        this.status = status;
    }
}
