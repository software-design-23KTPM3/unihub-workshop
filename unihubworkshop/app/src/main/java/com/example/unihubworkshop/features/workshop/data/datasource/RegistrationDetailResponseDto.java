package com.example.unihubworkshop.features.workshop.data.datasource;

public class RegistrationDetailResponseDto {
    private String id;
    private WorkshopResponseDto workshop;
    private String studentId;
    private String studentName;
    private String studentEmail;
    private String status;
    private String qrCode;
    private String registeredAt;
    private String paymentStatus;

    public String getId() { return id; }
    public WorkshopResponseDto getWorkshop() { return workshop; }
    public String getStudentId() { return studentId; }
    public String getStudentName() { return studentName; }
    public String getStudentEmail() { return studentEmail; }
    public String getStatus() { return status; }
    public String getQrCode() { return qrCode; }
    public String getRegisteredAt() { return registeredAt; }
    public String getPaymentStatus() { return paymentStatus; }
}
