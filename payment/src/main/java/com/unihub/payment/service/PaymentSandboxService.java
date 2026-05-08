package com.unihub.payment.service;

import com.unihub.payment.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class PaymentSandboxService {

    private final Map<String, SandboxPayment> paymentsById = new ConcurrentHashMap<>();
    private final Map<UUID, String> paymentIdByIdempotencyKey = new ConcurrentHashMap<>();
    private final RestClient restClient = RestClient.create();
    private final String publicBaseUrl;
    private final String webhookSecret;
    private volatile GatewayMode mode = GatewayMode.NORMAL;

    public PaymentSandboxService(
            @Value("${app.payment.public-base-url:http://localhost/sandbox}") String publicBaseUrl,
            @Value("${app.payment.webhook-secret:dev-payment-secret}") String webhookSecret) {
        this.publicBaseUrl = publicBaseUrl.replaceAll("/$", "");
        this.webhookSecret = webhookSecret;
    }

    public CreatePaymentResponse createPayment(CreatePaymentRequest request) {
        applyModeBehavior();

        String existingPaymentId = paymentIdByIdempotencyKey.get(request.getIdempotencyKey());
        if (existingPaymentId != null) {
            return toResponse(paymentsById.get(existingPaymentId));
        }

        SandboxPayment payment = new SandboxPayment();
        payment.setPaymentId("sandbox_" + UUID.randomUUID());
        payment.setTransactionId(request.getTransactionId());
        payment.setRegistrationId(request.getRegistrationId());
        payment.setIdempotencyKey(request.getIdempotencyKey());
        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency() == null ? "VND" : request.getCurrency());
        payment.setReturnUrl(request.getReturnUrl());
        payment.setWebhookUrl(request.getWebhookUrl());

        paymentsById.put(payment.getPaymentId(), payment);
        paymentIdByIdempotencyKey.put(payment.getIdempotencyKey(), payment.getPaymentId());
        return toResponse(payment);
    }

    public SandboxPayment getPayment(String paymentId) {
        SandboxPayment payment = paymentsById.get(paymentId);
        if (payment == null) {
            throw new IllegalArgumentException("Payment session not found");
        }
        return payment;
    }

    public SandboxPayment complete(String paymentId, PaymentStatus status) {
        SandboxPayment payment = getPayment(paymentId);
        if (payment.getStatus() == PaymentStatus.PENDING) {
            payment.setStatus(status);
            payment.setCompletedAt(ZonedDateTime.now());
            sendWebhook(payment);
        }
        return payment;
    }

    public GatewayMode getMode() {
        return mode;
    }

    public GatewayMode setMode(GatewayMode mode) {
        this.mode = mode == null ? GatewayMode.NORMAL : mode;
        return this.mode;
    }

    private void applyModeBehavior() {
        if (mode == GatewayMode.ALWAYS_FAIL) {
            throw new IllegalStateException("Sandbox gateway is forced to fail");
        }
        if (mode == GatewayMode.FLAKY && ThreadLocalRandom.current().nextBoolean()) {
            throw new IllegalStateException("Sandbox gateway failed randomly");
        }
        if (mode == GatewayMode.SLOW) {
            try {
                Thread.sleep(7000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Sandbox gateway delay interrupted");
            }
        }
    }

    private void sendWebhook(SandboxPayment payment) {
        PaymentWebhookPayload payload = new PaymentWebhookPayload();
        payload.setTransactionId(payment.getTransactionId());
        payload.setGatewayPaymentId(payment.getPaymentId());
        payload.setStatus(payment.getStatus().name());
        payload.setAmount(payment.getAmount());
        payload.setPaidAt(payment.getStatus() == PaymentStatus.SUCCESS ? payment.getCompletedAt() : null);
        payload.setFailureReason(payment.getStatus() == PaymentStatus.FAILED ? "Thanh toán bị từ chối" : null);
        payload.setRaw(Map.of(
                "provider", "SANDBOX",
                "currency", payment.getCurrency(),
                "registrationId", payment.getRegistrationId().toString()));

        restClient.post()
                .uri(payment.getWebhookUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Sandbox-Signature", sign(canonicalPayload(payload)))
                .body(payload)
                .retrieve()
                .toBodilessEntity();
    }

    private CreatePaymentResponse toResponse(SandboxPayment payment) {
        CreatePaymentResponse response = new CreatePaymentResponse();
        response.setPaymentId(payment.getPaymentId());
        response.setPaymentUrl(publicBaseUrl + "/checkout/" + payment.getPaymentId());
        response.setStatus(payment.getStatus().name());
        response.setProvider("SANDBOX");
        return response;
    }

    private String canonicalPayload(PaymentWebhookPayload payload) {
        return payload.getTransactionId() + "|" + payload.getGatewayPaymentId() + "|" + payload.getStatus();
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot sign sandbox webhook", e);
        }
    }
}
