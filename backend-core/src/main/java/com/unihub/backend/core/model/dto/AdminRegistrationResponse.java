package com.unihub.backend.core.model.dto;

import com.unihub.backend.core.model.enums.RegistrationStatus;

import java.util.UUID;

public class AdminRegistrationResponse {
    private UUID id;
    private String studentId;
    private String studentName;
    private String studentEmail;
    private UUID workshopId;
    private WorkshopSummary workshop;
    private RegistrationStatus status;
    private String paymentStatus;
    private String qrCode;
    private String registeredAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public String getStudentEmail() { return studentEmail; }
    public void setStudentEmail(String studentEmail) { this.studentEmail = studentEmail; }
    public UUID getWorkshopId() { return workshopId; }
    public void setWorkshopId(UUID workshopId) { this.workshopId = workshopId; }
    public WorkshopSummary getWorkshop() { return workshop; }
    public void setWorkshop(WorkshopSummary workshop) { this.workshop = workshop; }
    public RegistrationStatus getStatus() { return status; }
    public void setStatus(RegistrationStatus status) { this.status = status; }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public String getQrCode() { return qrCode; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }
    public String getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(String registeredAt) { this.registeredAt = registeredAt; }

    public static AdminRegistrationResponseBuilder builder() {
        return new AdminRegistrationResponseBuilder();
    }

    public static class AdminRegistrationResponseBuilder {
        private final AdminRegistrationResponse r = new AdminRegistrationResponse();

        public AdminRegistrationResponseBuilder id(UUID id) { r.id = id; return this; }
        public AdminRegistrationResponseBuilder studentId(String studentId) { r.studentId = studentId; return this; }
        public AdminRegistrationResponseBuilder studentName(String studentName) { r.studentName = studentName; return this; }
        public AdminRegistrationResponseBuilder studentEmail(String studentEmail) { r.studentEmail = studentEmail; return this; }
        public AdminRegistrationResponseBuilder workshopId(UUID workshopId) { r.workshopId = workshopId; return this; }
        public AdminRegistrationResponseBuilder workshop(WorkshopSummary workshop) { r.workshop = workshop; return this; }
        public AdminRegistrationResponseBuilder status(RegistrationStatus status) { r.status = status; return this; }
        public AdminRegistrationResponseBuilder paymentStatus(String paymentStatus) { r.paymentStatus = paymentStatus; return this; }
        public AdminRegistrationResponseBuilder qrCode(String qrCode) { r.qrCode = qrCode; return this; }
        public AdminRegistrationResponseBuilder registeredAt(String registeredAt) { r.registeredAt = registeredAt; return this; }
        public AdminRegistrationResponse build() { return r; }
    }

    public static class WorkshopSummary {
        private UUID id;
        private String title;

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public static WorkshopSummary of(UUID id, String title) {
            WorkshopSummary summary = new WorkshopSummary();
            summary.setId(id);
            summary.setTitle(title);
            return summary;
        }
    }
}
