package com.unihub.backend.core.model.dto;

import java.time.ZonedDateTime;
import java.util.UUID;

public class CheckinResult {
    private String studentId;
    private UUID workshopId;
    private UUID registrationId;
    private String status;
    private String message;
    private ZonedDateTime checkedInAt;

    public CheckinResult() {
    }

    public CheckinResult(String studentId, UUID workshopId, UUID registrationId, String status, String message,
            ZonedDateTime checkedInAt) {
        this.studentId = studentId;
        this.workshopId = workshopId;
        this.registrationId = registrationId;
        this.status = status;
        this.message = message;
        this.checkedInAt = checkedInAt;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public UUID getWorkshopId() {
        return workshopId;
    }

    public void setWorkshopId(UUID workshopId) {
        this.workshopId = workshopId;
    }

    public UUID getRegistrationId() {
        return registrationId;
    }

    public void setRegistrationId(UUID registrationId) {
        this.registrationId = registrationId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ZonedDateTime getCheckedInAt() {
        return checkedInAt;
    }

    public void setCheckedInAt(ZonedDateTime checkedInAt) {
        this.checkedInAt = checkedInAt;
    }

    public static CheckinResultBuilder builder() {
        return new CheckinResultBuilder();
    }

    public static class CheckinResultBuilder {
        private String studentId;
        private UUID workshopId;
        private UUID registrationId;
        private String status;
        private String message;
        private ZonedDateTime checkedInAt;

        public CheckinResultBuilder studentId(String studentId) {
            this.studentId = studentId;
            return this;
        }

        public CheckinResultBuilder workshopId(UUID workshopId) {
            this.workshopId = workshopId;
            return this;
        }

        public CheckinResultBuilder registrationId(UUID registrationId) {
            this.registrationId = registrationId;
            return this;
        }

        public CheckinResultBuilder status(String status) {
            this.status = status;
            return this;
        }

        public CheckinResultBuilder message(String message) {
            this.message = message;
            return this;
        }

        public CheckinResultBuilder checkedInAt(ZonedDateTime checkedInAt) {
            this.checkedInAt = checkedInAt;
            return this;
        }

        public CheckinResult build() {
            return new CheckinResult(studentId, workshopId, registrationId, status, message, checkedInAt);
        }
    }
}
