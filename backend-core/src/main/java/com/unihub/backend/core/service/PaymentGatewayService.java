package com.unihub.backend.core.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PaymentGatewayService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PaymentGatewayService.class);

    @CircuitBreaker(name = "paymentGateway", fallbackMethod = "fallbackProcessPayment")
    public String processPayment(UUID transactionId, double amount) {
        log.info("Processing payment via external gateway for transaction {}: {}", transactionId, amount);
        
        // Mock external call
        if (Math.random() > 0.8) {
            throw new RuntimeException("External Payment Gateway Timeout");
        }
        
        return "PG_TXN_" + UUID.randomUUID().toString().substring(0, 8);
    }

    public String fallbackProcessPayment(UUID transactionId, double amount, Throwable t) {
        log.warn("Fallback: Payment gateway is down for transaction {}. Reason: {}", transactionId, t.getMessage());
        return "PENDING_RETRY";
    }
}
