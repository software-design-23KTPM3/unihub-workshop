package com.unihub.backend.core.model.dto;

public class PaymentWebhookResponse {
    private String status;
    private String message;

    public PaymentWebhookResponse() {
    }

    public PaymentWebhookResponse(String status, String message) {
        this.status = status;
        this.message = message;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
