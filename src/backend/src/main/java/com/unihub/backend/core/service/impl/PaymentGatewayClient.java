package com.unihub.backend.core.service.impl;

import com.unihub.backend.core.exception.PaymentGatewayUnavailableException;
import com.unihub.backend.core.model.dto.SandboxPaymentCreateRequest;
import com.unihub.backend.core.model.dto.SandboxPaymentCreateResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class PaymentGatewayClient {

    private final RestClient restClient;
    private final String baseUrl;
    private final CircuitBreaker paymentGatewayCircuitBreaker;

    public PaymentGatewayClient(
            RestClient restClient,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${app.payment.sandbox-base-url:http://localhost:8090}") String baseUrl) {
        this.restClient = restClient;
        this.baseUrl = baseUrl.replaceAll("/$", "");
        this.paymentGatewayCircuitBreaker = circuitBreakerRegistry.circuitBreaker("paymentGateway");
    }

    public SandboxPaymentCreateResponse createPayment(SandboxPaymentCreateRequest request) {
        try {
            return paymentGatewayCircuitBreaker.executeSupplier(() -> restClient.post()
                    .uri(baseUrl + "/sandbox/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(SandboxPaymentCreateResponse.class));
        } catch (Exception ex) {
            throw new PaymentGatewayUnavailableException(
                    "Dịch vụ thanh toán đang tạm gián đoạn. Chỗ của bạn vẫn được giữ, vui lòng thử lại sau.");
        }
    }
}
