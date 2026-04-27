package com.unihub.backend.core.service.impl;

import com.unihub.backend.core.model.dto.RegistrationRequest;
import com.unihub.backend.core.model.dto.RegistrationResponse;
import com.unihub.backend.core.model.enums.RegistrationStatus;
import com.unihub.backend.core.service.RegistrationService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class RegistrationServiceImpl implements RegistrationService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RegistrationServiceImpl.class);

    private final StringRedisTemplate redisTemplate;
    private final RabbitTemplate rabbitTemplate;

    public RegistrationServiceImpl(StringRedisTemplate redisTemplate, RabbitTemplate rabbitTemplate) {
        this.redisTemplate = redisTemplate;
        this.rabbitTemplate = rabbitTemplate;
    }

    private static final String IDEMPOTENCY_PREFIX = "idempotency:registration:";
    private static final String WORKSHOP_SLOTS_PREFIX = "workshop_slots:";
    private static final String REGISTRATION_EXCHANGE = "registration.exchange";
    private static final String REGISTRATION_ROUTING_KEY = "registration.created";

    @Override
    public RegistrationResponse createRegistration(UUID idempotencyKey, RegistrationRequest request) {
        String idempotencyPath = IDEMPOTENCY_PREFIX + idempotencyKey;
        
        Boolean isNew = redisTemplate.opsForValue().setIfAbsent(idempotencyPath, "PROCESSING", 24, TimeUnit.HOURS);
        if (Boolean.FALSE.equals(isNew)) {
            String status = redisTemplate.opsForValue().get(idempotencyPath);
            return RegistrationResponse.builder()
                    .status(RegistrationStatus.PENDING)
                    .message("Request is being processed or already completed: " + status)
                    .build();
        }

        try {
            String slotKey = WORKSHOP_SLOTS_PREFIX + request.getWorkshopId();
            Long remaining = redisTemplate.opsForValue().decrement(slotKey);

            if (remaining == null || remaining < 0) {
                redisTemplate.opsForValue().increment(slotKey);
                redisTemplate.delete(idempotencyPath);
                return RegistrationResponse.builder()
                        .status(RegistrationStatus.FAILED)
                        .message("Workshop is sold out")
                        .build();
            }

            request.setIdempotencyKey(idempotencyKey);
            rabbitTemplate.convertAndSend(REGISTRATION_EXCHANGE, REGISTRATION_ROUTING_KEY, request);

            return RegistrationResponse.builder()
                    .registrationId(idempotencyKey)
                    .status(RegistrationStatus.PENDING)
                    .message("Registration request accepted and is being processed")
                    .build();

        } catch (Exception e) {
            log.error("Error during registration process", e);
            redisTemplate.delete(idempotencyPath);
            throw new RuntimeException("Failed to process registration", e);
        }
    }
}
