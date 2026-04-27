package com.unihub.backend.core.model.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class RegistrationRequest {
    @NotNull
    private UUID workshopId;
    @NotNull
    private String studentId;
    private UUID idempotencyKey;

    public UUID getWorkshopId() { return workshopId; }
    public void setWorkshopId(UUID workshopId) { this.workshopId = workshopId; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public UUID getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(UUID idempotencyKey) { this.idempotencyKey = idempotencyKey; }
}
