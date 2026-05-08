package com.unihub.backend.core.service;

import com.unihub.backend.core.model.dto.PaymentStartRequest;
import com.unihub.backend.core.model.dto.PaymentStartResponse;
import com.unihub.backend.core.model.dto.PaymentWebhookRequest;
import com.unihub.backend.core.model.dto.PaymentWebhookResponse;
import org.springframework.security.core.Authentication;

import java.util.UUID;

public interface PaymentService {
    PaymentStartResponse startPayment(UUID registrationId, PaymentStartRequest request, Authentication authentication);
    PaymentWebhookResponse handleWebhook(PaymentWebhookRequest request, String signature);
}
