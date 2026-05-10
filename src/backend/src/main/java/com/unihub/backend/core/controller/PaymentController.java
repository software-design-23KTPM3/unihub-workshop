package com.unihub.backend.core.controller;

import com.unihub.backend.core.model.dto.PaymentStartRequest;
import com.unihub.backend.core.model.dto.PaymentStartResponse;
import com.unihub.backend.core.model.dto.PaymentWebhookRequest;
import com.unihub.backend.core.model.dto.PaymentWebhookResponse;
import com.unihub.backend.core.service.PaymentService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/registrations/{id}/payment/start")
    @PreAuthorize("hasRole('STUDENT')")
    public PaymentStartResponse startPayment(
            @PathVariable UUID id,
            @RequestBody(required = false) PaymentStartRequest request,
            Authentication authentication) {
        return paymentService.startPayment(id, request, authentication);
    }

    @PostMapping("/payments/webhook")
    public PaymentWebhookResponse paymentWebhook(
            @RequestBody PaymentWebhookRequest request,
            @RequestHeader(name = "X-Sandbox-Signature", required = false) String signature) {
        return paymentService.handleWebhook(request, signature);
    }

    @PostMapping("/payments/sandbox-server-failure")
    public PaymentWebhookResponse sandboxServerFailure(
            @RequestBody PaymentWebhookRequest request,
            @RequestHeader(name = "X-Sandbox-Signature", required = false) String signature) {
        return paymentService.simulateGatewayServerFailure(request, signature);
    }
}
