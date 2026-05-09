package com.example.unihubworkshop.features.workshop.data.datasource;

public class CheckinEvent {
    private String clientEventId;
    private String studentId;
    private String workshopId;
    private String registrationId;
    private String qrCode;
    private String staffId;
    private String deviceId;
    private String checkinAt;

    public CheckinEvent(
            String clientEventId,
            String studentId,
            String workshopId,
            String registrationId,
            String qrCode,
            String staffId,
            String deviceId,
            String checkinAt) {
        this.clientEventId = clientEventId;
        this.studentId = studentId;
        this.workshopId = workshopId;
        this.registrationId = registrationId;
        this.qrCode = qrCode;
        this.staffId = staffId;
        this.deviceId = deviceId;
        this.checkinAt = checkinAt;
    }

    public String getClientEventId() { return clientEventId; }
    public String getStudentId() { return studentId; }
    public String getWorkshopId() { return workshopId; }
    public String getRegistrationId() { return registrationId; }
    public String getQrCode() { return qrCode; }
    public String getStaffId() { return staffId; }
    public String getDeviceId() { return deviceId; }
    public String getCheckinAt() { return checkinAt; }
}
