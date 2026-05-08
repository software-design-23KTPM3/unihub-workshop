package com.unihub.payment.model;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

public class PaymentWebhookPayload {
    private UUID transactionId;
    private String gatewayPaymentId;
    private String status;
    private BigDecimal amount;
    private ZonedDateTime paidAt;
    private String failureReason;
    private Map<String, Object> raw;

    public UUID getTransactionId() { return transactionId; }
    public void setTransactionId(UUID transactionId) { this.transactionId = transactionId; }
    public String getGatewayPaymentId() { return gatewayPaymentId; }
    public void setGatewayPaymentId(String gatewayPaymentId) { this.gatewayPaymentId = gatewayPaymentId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public ZonedDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(ZonedDateTime paidAt) { this.paidAt = paidAt; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public Map<String, Object> getRaw() { return raw; }
    public void setRaw(Map<String, Object> raw) { this.raw = raw; }
}
