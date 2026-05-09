package com.unihub.backend.core.model.dto;

import java.util.UUID;

public class PaymentStartRequest {
    private UUID idempotencyKey;
    private boolean simulateGatewayFailure;

    public UUID getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(UUID idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public boolean isSimulateGatewayFailure() {
        return simulateGatewayFailure;
    }

    public void setSimulateGatewayFailure(boolean simulateGatewayFailure) {
        this.simulateGatewayFailure = simulateGatewayFailure;
    }
}
