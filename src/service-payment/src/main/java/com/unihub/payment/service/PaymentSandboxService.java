package com.unihub.payment.service;

import com.unihub.payment.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(PaymentSandboxService.class);

    private final Map<String, SandboxPayment> paymentsById = new ConcurrentHashMap<>();
    private final Map<UUID, String> paymentIdByIdempotencyKey = new ConcurrentHashMap<>();
    private final RestClient restClient = RestClient.create();
    private final String publicBaseUrl;
    private final String webhookSecret;
    private final String failureCallbackUrls;
    private volatile GatewayMode mode = GatewayMode.NORMAL;

    public PaymentSandboxService(
            @Value("${app.payment.public-base-url:http://localhost/sandbox}") String publicBaseUrl,
            @Value("${app.payment.webhook-secret:dev-payment-secret}") String webhookSecret,
            @Value("${app.payment.failure-callback-urls:}") String failureCallbackUrls) {
        this.publicBaseUrl = publicBaseUrl.replaceAll("/$", "");
        this.webhookSecret = webhookSecret;
        this.failureCallbackUrls = failureCallbackUrls;
    }

    public CreatePaymentResponse createPayment(CreatePaymentRequest request) {
        if (request.isSimulateGatewayFailure()) {
            throw new IllegalStateException("Sandbox gateway failed after client submitted payment");
        }

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

    public SandboxPayment failOnServer(String paymentId) {
        SandboxPayment payment = getPayment(paymentId);
        if (payment.getStatus() == PaymentStatus.PENDING) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setCompletedAt(ZonedDateTime.now());
            sendServerFailureCallback(payment);
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

    private void sendServerFailureCallback(SandboxPayment payment) {
        PaymentWebhookPayload payload = new PaymentWebhookPayload();
        payload.setTransactionId(payment.getTransactionId());
        payload.setGatewayPaymentId(payment.getPaymentId());
        payload.setStatus("SERVER_FAILED");
        payload.setAmount(payment.getAmount());
        payload.setFailureReason("Payment server failed after client submitted payment");
        payload.setRaw(Map.of(
                "provider", "SANDBOX",
                "currency", payment.getCurrency(),
                "registrationId", payment.getRegistrationId().toString()));

        String signature = sign(canonicalPayload(payload));
        for (String callbackUrl : failureCallbackUrls(payment)) {
            try {
                restClient.post()
                        .uri(callbackUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Sandbox-Signature", signature)
                        .body(payload)
                        .retrieve()
                        .toBodilessEntity();
            } catch (Exception e) {
                log.warn("Sandbox server-failure callback failed: {}", callbackUrl, e);
            }
        }
    }

    private java.util.List<String> failureCallbackUrls(SandboxPayment payment) {
        if (failureCallbackUrls != null && !failureCallbackUrls.isBlank()) {
            return java.util.Arrays.stream(failureCallbackUrls.split(","))
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .toList();
        }
        return java.util.List.of(payment.getWebhookUrl().replace("/webhook", "/sandbox-server-failure"));
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
