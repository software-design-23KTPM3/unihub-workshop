package com.unihub.backend.core.service.impl;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.unihub.backend.core.config.RabbitConfig;
import com.unihub.backend.core.exception.PaymentException;
import com.unihub.backend.core.model.dto.*;
import com.unihub.backend.core.model.entity.Registration;
import com.unihub.backend.core.model.entity.Transaction;
import com.unihub.backend.core.model.enums.RegistrationStatus;
import com.unihub.backend.core.model.enums.TransactionStatus;
import com.unihub.backend.core.repository.RegistrationRepository;
import com.unihub.backend.core.repository.TransactionRepository;
import com.unihub.backend.core.service.PaymentService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PaymentServiceImpl.class);
    private static final DateTimeFormatter EMAIL_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final RegistrationRepository registrationRepository;
    private final TransactionRepository transactionRepository;
    private final PaymentGatewayClient paymentGatewayClient;
    private final RabbitTemplate rabbitTemplate;
    private final String frontendBaseUrl;
    private final String webhookUrl;
    private final String webhookSecret;

    public PaymentServiceImpl(
            RegistrationRepository registrationRepository,
            TransactionRepository transactionRepository,
            PaymentGatewayClient paymentGatewayClient,
            RabbitTemplate rabbitTemplate,
            @Value("${app.frontend-base-url:http://localhost:3000}") String frontendBaseUrl,
            @Value("${app.payment.webhook-url:http://localhost:8081/api/payments/webhook}") String webhookUrl,
            @Value("${app.payment.webhook-secret:dev-payment-secret}") String webhookSecret) {
        this.registrationRepository = registrationRepository;
        this.transactionRepository = transactionRepository;
        this.paymentGatewayClient = paymentGatewayClient;
        this.rabbitTemplate = rabbitTemplate;
        this.frontendBaseUrl = frontendBaseUrl.replaceAll("/$", "");
        this.webhookUrl = webhookUrl;
        this.webhookSecret = webhookSecret;
    }

    @Override
    @Transactional
    public PaymentStartResponse startPayment(UUID registrationId, PaymentStartRequest request, Authentication authentication) {
        Registration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new PaymentException("Registration not found"));

        ensureOwner(registration, authentication);

        if (!Boolean.TRUE.equals(registration.getWorkshop().getIsPaid())) {
            throw new PaymentException("This workshop is free and does not require payment");
        }
        if (registration.getStatus() == RegistrationStatus.SUCCESS || registration.getStatus() == RegistrationStatus.CHECKED_IN) {
            Transaction transaction = getTransaction(registrationId);
            return toStartResponse(transaction, "Thanh toán đã hoàn tất.");
        }
        if (registration.getStatus() != RegistrationStatus.PENDING) {
            throw new PaymentException("Registration is not payable in current status: " + registration.getStatus());
        }

        Transaction transaction = getTransaction(registrationId);

        if (transaction.getStatus() == TransactionStatus.SUCCESS) {
            registration.setStatus(RegistrationStatus.SUCCESS);
            registrationRepository.save(registration);
            return toStartResponse(transaction, "Thanh toán đã hoàn tất.");
        }
        if (transaction.getStatus() == TransactionStatus.PENDING
                && transaction.getPaymentUrl() != null && transaction.getPgTransactionId() != null) {
            return toStartResponse(transaction, "Tiếp tục phiên thanh toán hiện tại.");
        }

        UUID idempotencyKey = request != null && request.getIdempotencyKey() != null
                ? request.getIdempotencyKey()
                : transaction.getIdempotencyKey();

        if (transaction.getStatus() == TransactionStatus.FAILED) {
            transaction.setStatus(TransactionStatus.PENDING);
            transaction.setIdempotencyKey(idempotencyKey);
            transaction.setPgTransactionId(null);
            transaction.setPaymentUrl(null);
            transaction.setFailureReason(null);
        }

        SandboxPaymentCreateRequest sandboxRequest = new SandboxPaymentCreateRequest();
        sandboxRequest.setTransactionId(transaction.getId());
        sandboxRequest.setRegistrationId(registration.getId());
        sandboxRequest.setAmount(transaction.getAmount());
        sandboxRequest.setCurrency("VND");
        sandboxRequest.setIdempotencyKey(idempotencyKey);
        sandboxRequest.setReturnUrl(frontendBaseUrl + "/student/tickets/" + registration.getId());
        sandboxRequest.setWebhookUrl(webhookUrl);

        SandboxPaymentCreateResponse sandboxResponse = paymentGatewayClient.createPayment(sandboxRequest);
        if (sandboxResponse == null || sandboxResponse.getPaymentId() == null || sandboxResponse.getPaymentUrl() == null) {
            throw new PaymentException("Dịch vụ thanh toán chưa tạo được phiên thanh toán hợp lệ.");
        }

        transaction.setProvider(sandboxResponse.getProvider() == null ? "SANDBOX" : sandboxResponse.getProvider());
        transaction.setPgTransactionId(sandboxResponse.getPaymentId());
        transaction.setPaymentUrl(sandboxResponse.getPaymentUrl());
        transaction.setFailureReason(null);
        transaction = transactionRepository.save(transaction);

        return toStartResponse(transaction, "Phiên thanh toán đã sẵn sàng.");
    }

    @Override
    @Transactional
    public PaymentWebhookResponse handleWebhook(PaymentWebhookRequest request, String signature) {
        validateWebhook(request, signature);

        Transaction transaction = transactionRepository.findById(request.getTransactionId())
                .or(() -> transactionRepository.findByPgTransactionId(request.getGatewayPaymentId()))
                .orElseThrow(() -> new PaymentException("Transaction not found"));

        Registration registration = transaction.getRegistration();
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("transactionId", String.valueOf(request.getTransactionId()));
        raw.put("gatewayPaymentId", request.getGatewayPaymentId());
        raw.put("status", request.getStatus());
        raw.put("amount", request.getAmount());
        raw.put("paidAt", request.getPaidAt());
        raw.put("failureReason", request.getFailureReason());
        raw.put("raw", request.getRaw());
        transaction.setRawCallback(raw);

        if ("SUCCESS".equalsIgnoreCase(request.getStatus())) {
            if (transaction.getStatus() != TransactionStatus.SUCCESS) {
                transaction.setStatus(TransactionStatus.SUCCESS);
                transaction.setPaidAt(request.getPaidAt() == null ? ZonedDateTime.now() : request.getPaidAt());
                transaction.setFailureReason(null);
                registration.setStatus(RegistrationStatus.SUCCESS);
                registrationRepository.save(registration);
                sendPaymentSuccessNotification(registration);
            }
        } else if ("FAILED".equalsIgnoreCase(request.getStatus())) {
            if (transaction.getStatus() != TransactionStatus.SUCCESS) {
                transaction.setStatus(TransactionStatus.FAILED);
                transaction.setFailureReason(request.getFailureReason() == null ? "Thanh toán chưa thành công" : request.getFailureReason());
            }
        } else {
            throw new PaymentException("Unsupported payment status: " + request.getStatus());
        }

        transactionRepository.save(transaction);
        return new PaymentWebhookResponse("OK", "Webhook processed");
    }

    private void sendPaymentSuccessNotification(Registration registration) {
        NotificationData data = NotificationData.builder()
                .title("PAYMENT WORKSHOP " + registration.getWorkshop().getName() + " SUCCESS")
                .msg("Congratulations " + registration.getStudent().getName()
                        + "! Your payment is successful and your workshop ticket is valid. QR code: "
                        + registration.getQrCode())
                .to(registration.getStudent().getEmail())
                .qrPayload(registration.getQrCode())
                .qrImageBase64(qrImageBase64(registration.getQrCode()))
                .workshopTitle(registration.getWorkshop().getName())
                .workshopTime(formatWorkshopTime(registration))
                .workshopRoom(registration.getWorkshop().getRoom())
                .workshopSpeaker(registration.getWorkshop().getSpeaker())
                .build();

        NotificationRequest notificationRequest = NotificationRequest.builder()
                .type("EMAIL")
                .data(data)
                .build();

        try {
            rabbitTemplate.convertAndSend(
                    RabbitConfig.NOTIFICATION_EXCHANGE,
                    RabbitConfig.NOTIFICATION_ROUTING_KEY,
                    notificationRequest);
        } catch (Exception e) {
            log.error("Failed to publish payment success notification for registration {}",
                    registration.getId(), e);
        }
    }

    private Transaction getTransaction(UUID registrationId) {
        return transactionRepository.findByRegistrationId(registrationId)
                .orElseThrow(() -> new PaymentException("Payment transaction not found. Please refresh and try again."));
    }

    private String qrImageBase64(String qrCode) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(qrCode, BarcodeFormat.QR_CODE, 320, 320);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", output);
            return Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (Exception e) {
            throw new PaymentException("Cannot generate payment QR image");
        }
    }

    private String formatWorkshopTime(Registration registration) {
        return EMAIL_TIME_FORMATTER.format(registration.getWorkshop().getStartTime())
                + " - "
                + EMAIL_TIME_FORMATTER.format(registration.getWorkshop().getEndTime());
    }

    private void ensureOwner(Registration registration, Authentication authentication) {
        if (authentication == null || authentication.getName() == null
                || !authentication.getName().equals(registration.getStudent().getMssv())) {
            throw new PaymentException("You can only pay for your own registration");
        }
    }

    private void validateWebhook(PaymentWebhookRequest request, String signature) {
        if (request == null || request.getTransactionId() == null || request.getGatewayPaymentId() == null
                || request.getStatus() == null) {
            throw new PaymentException("Invalid payment webhook payload");
        }
        String expected = sign(canonicalPayload(request));
        if (signature == null || !MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8))) {
            throw new PaymentException("Invalid payment webhook signature");
        }
    }

    private String canonicalPayload(PaymentWebhookRequest request) {
        return request.getTransactionId() + "|" + request.getGatewayPaymentId() + "|" + request.getStatus();
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
            throw new PaymentException("Cannot verify payment signature");
        }
    }

    private PaymentStartResponse toStartResponse(Transaction transaction, String message) {
        return PaymentStartResponse.from(
                new PaymentStartResponse.TransactionView(
                        transaction.getRegistration().getId(),
                        transaction.getId(),
                        transaction.getStatus().name(),
                        transaction.getAmount(),
                        transaction.getProvider(),
                        transaction.getPgTransactionId(),
                        transaction.getPaymentUrl()),
                message);
    }
}
