package com.unihub.backend.core.model.dto;

import com.unihub.backend.core.model.enums.RegistrationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationDetailResponse {
    private UUID id;
    private WorkshopResponse workshop;
    private String studentId;
    private String studentName;
    private RegistrationStatus status;
    private String qrCode;
    private String registeredAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public WorkshopResponse getWorkshop() { return workshop; }
    public void setWorkshop(WorkshopResponse workshop) { this.workshop = workshop; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public RegistrationStatus getStatus() { return status; }
    public void setStatus(RegistrationStatus status) { this.status = status; }
    public String getQrCode() { return qrCode; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }
    public String getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(String registeredAt) { this.registeredAt = registeredAt; }

    public static RegistrationDetailResponseBuilder builder() {
        return new RegistrationDetailResponseBuilder();
    }

    public static class RegistrationDetailResponseBuilder {
        private final RegistrationDetailResponse r = new RegistrationDetailResponse();

        public RegistrationDetailResponseBuilder id(UUID id) { r.id = id; return this; }
        public RegistrationDetailResponseBuilder workshop(WorkshopResponse workshop) { r.workshop = workshop; return this; }
        public RegistrationDetailResponseBuilder studentId(String studentId) { r.studentId = studentId; return this; }
        public RegistrationDetailResponseBuilder studentName(String studentName) { r.studentName = studentName; return this; }
        public RegistrationDetailResponseBuilder status(RegistrationStatus status) { r.status = status; return this; }
        public RegistrationDetailResponseBuilder qrCode(String qrCode) { r.qrCode = qrCode; return this; }
        public RegistrationDetailResponseBuilder registeredAt(String registeredAt) { r.registeredAt = registeredAt; return this; }
        public RegistrationDetailResponse build() { return r; }
    }
}
