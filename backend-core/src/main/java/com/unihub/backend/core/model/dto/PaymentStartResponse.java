package com.unihub.backend.core.model.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class PaymentStartResponse {
    private UUID registrationId;
    private UUID transactionId;
    private String status;
    private BigDecimal amount;
    private String provider;
    private String gatewayPaymentId;
    private String paymentUrl;
    private String message;

    public UUID getRegistrationId() { return registrationId; }
    public void setRegistrationId(UUID registrationId) { this.registrationId = registrationId; }
    public UUID getTransactionId() { return transactionId; }
    public void setTransactionId(UUID transactionId) { this.transactionId = transactionId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getGatewayPaymentId() { return gatewayPaymentId; }
    public void setGatewayPaymentId(String gatewayPaymentId) { this.gatewayPaymentId = gatewayPaymentId; }
    public String getPaymentUrl() { return paymentUrl; }
    public void setPaymentUrl(String paymentUrl) { this.paymentUrl = paymentUrl; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public static PaymentStartResponse from(TransactionView transaction, String message) {
        PaymentStartResponse response = new PaymentStartResponse();
        response.setRegistrationId(transaction.registrationId());
        response.setTransactionId(transaction.transactionId());
        response.setStatus(transaction.status());
        response.setAmount(transaction.amount());
        response.setProvider(transaction.provider());
        response.setGatewayPaymentId(transaction.gatewayPaymentId());
        response.setPaymentUrl(transaction.paymentUrl());
        response.setMessage(message);
        return response;
    }

    public record TransactionView(
            UUID registrationId,
            UUID transactionId,
            String status,
            BigDecimal amount,
            String provider,
            String gatewayPaymentId,
            String paymentUrl) {
    }
}
