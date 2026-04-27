package com.unihub.backend.core.model.dto;

import com.unihub.backend.core.model.enums.RegistrationStatus;
import java.util.UUID;

public class RegistrationResponse {
    private UUID registrationId;
    private RegistrationStatus status;
    private String message;

    public UUID getRegistrationId() { return registrationId; }
    public void setRegistrationId(UUID registrationId) { this.registrationId = registrationId; }
    public RegistrationStatus getStatus() { return status; }
    public void setStatus(RegistrationStatus status) { this.status = status; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public static RegistrationResponseBuilder builder() { return new RegistrationResponseBuilder(); }
    public static class RegistrationResponseBuilder {
        private RegistrationResponse r = new RegistrationResponse();
        public RegistrationResponseBuilder registrationId(UUID id) { r.registrationId = id; return this; }
        public RegistrationResponseBuilder status(RegistrationStatus status) { r.status = status; return this; }
        public RegistrationResponseBuilder message(String message) { r.message = message; return this; }
        public RegistrationResponse build() { return r; }
    }
}
