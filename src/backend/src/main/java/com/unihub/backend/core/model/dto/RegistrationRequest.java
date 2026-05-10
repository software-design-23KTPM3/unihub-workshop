package com.unihub.backend.core.model.dto;

import java.util.UUID;
import jakarta.validation.constraints.NotNull;

public class RegistrationRequest {
    @NotNull
    private UUID workshopId;
    
    private UUID idempotencyKey;

    public UUID getWorkshopId() { return workshopId; }
    public void setWorkshopId(UUID workshopId) { this.workshopId = workshopId; }
    public UUID getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(UUID idempotencyKey) { this.idempotencyKey = idempotencyKey; }
}
