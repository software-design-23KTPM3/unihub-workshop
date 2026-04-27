package com.unihub.backend.core.service.impl;

import com.unihub.backend.core.model.dto.RegistrationRequest;
import com.unihub.backend.core.model.dto.RegistrationResponse;
import com.unihub.backend.core.model.enums.RegistrationStatus;
import com.unihub.backend.core.service.RegistrationService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import com.unihub.backend.core.repository.WorkshopRepository;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class RegistrationServiceImpl implements RegistrationService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RegistrationServiceImpl.class);

    private final StringRedisTemplate redisTemplate;
    private final RabbitTemplate rabbitTemplate;

    private final WorkshopRepository workshopRepository;

    public RegistrationServiceImpl(StringRedisTemplate redisTemplate, RabbitTemplate rabbitTemplate, WorkshopRepository workshopRepository) {
        this.redisTemplate = redisTemplate;
        this.rabbitTemplate = rabbitTemplate;
        this.workshopRepository = workshopRepository;
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
            
            // 1. Kiểm tra xem key này có tồn tại trong Redis chưa
            if (Boolean.FALSE.equals(redisTemplate.hasKey(slotKey))) {
                // 2. Nếu không có, gọi xuống Database để lấy số lượng chỗ trống thực tế
                int currentAvailable = workshopRepository.getAvailableSlots(request.getWorkshopId())
                    .orElseThrow(() -> new RuntimeException("Workshop not found"));
                
                // 3. Lưu vào Redis (set giá trị khởi tạo)
                redisTemplate.opsForValue().set(slotKey, String.valueOf(currentAvailable), 1, TimeUnit.HOURS);
            }

            // 4. Tiến hành trừ slot như bình thường
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
            log.info("Bypass RabbitMQ for testing");

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
