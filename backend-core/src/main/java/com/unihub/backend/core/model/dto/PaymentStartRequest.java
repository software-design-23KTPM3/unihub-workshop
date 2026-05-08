package com.unihub.backend.core.model.dto;

import java.util.UUID;

public class PaymentStartRequest {
    private UUID idempotencyKey;

    public UUID getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(UUID idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
}
