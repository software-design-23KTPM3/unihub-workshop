package com.unihub.payment.model;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public class CreatePaymentRequest {
    @NotNull
    private UUID transactionId;
    @NotNull
    private UUID registrationId;
    @NotNull
    private BigDecimal amount;
    private String currency = "VND";
    @NotNull
    private UUID idempotencyKey;
    @NotNull
    private String returnUrl;
    @NotNull
    private String webhookUrl;
    private boolean simulateGatewayFailure;

    public UUID getTransactionId() { return transactionId; }
    public void setTransactionId(UUID transactionId) { this.transactionId = transactionId; }
    public UUID getRegistrationId() { return registrationId; }
    public void setRegistrationId(UUID registrationId) { this.registrationId = registrationId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public UUID getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(UUID idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getReturnUrl() { return returnUrl; }
    public void setReturnUrl(String returnUrl) { this.returnUrl = returnUrl; }
    public String getWebhookUrl() { return webhookUrl; }
    public void setWebhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; }
    public boolean isSimulateGatewayFailure() { return simulateGatewayFailure; }
    public void setSimulateGatewayFailure(boolean simulateGatewayFailure) { this.simulateGatewayFailure = simulateGatewayFailure; }
}
