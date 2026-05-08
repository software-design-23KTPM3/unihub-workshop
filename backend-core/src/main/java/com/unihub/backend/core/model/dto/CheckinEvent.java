package com.unihub.backend.core.model.dto;

import java.time.ZonedDateTime;
import java.util.UUID;

public class CheckinEvent {
    private UUID clientEventId;
    private String studentId;
    private UUID workshopId;
    private UUID registrationId;
    private String qrCode;
    private String staffId;
    private String deviceId;
    private ZonedDateTime checkinAt;

    public UUID getClientEventId() { return clientEventId; }
    public void setClientEventId(UUID clientEventId) { this.clientEventId = clientEventId; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public UUID getWorkshopId() { return workshopId; }
    public void setWorkshopId(UUID workshopId) { this.workshopId = workshopId; }
    public UUID getRegistrationId() { return registrationId; }
    public void setRegistrationId(UUID registrationId) { this.registrationId = registrationId; }
    public String getQrCode() { return qrCode; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }
    public String getStaffId() { return staffId; }
    public void setStaffId(String staffId) { this.staffId = staffId; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public ZonedDateTime getCheckinAt() { return checkinAt; }
    public void setCheckinAt(ZonedDateTime checkinAt) { this.checkinAt = checkinAt; }
    public ZonedDateTime getScannedAt() { return checkinAt; }
    public void setScannedAt(ZonedDateTime scannedAt) { this.checkinAt = scannedAt; }
}
