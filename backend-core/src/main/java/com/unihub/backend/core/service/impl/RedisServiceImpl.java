package com.unihub.backend.core.service.impl;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.unihub.backend.core.repository.WorkshopRepository;
import com.unihub.backend.core.service.RedisService;

@Service
public class RedisServiceImpl implements RedisService {
    private final StringRedisTemplate redisTemplate;
    private final WorkshopRepository workshopRepository;

    private static final String IDEMPOTENCY_PREFIX = "idempotency:registration:";
    private static final String WORKSHOP_SLOTS_PREFIX = "workshop_slots:";

    public RedisServiceImpl(StringRedisTemplate redisTemplate, WorkshopRepository workshopRepository) {
        this.redisTemplate = redisTemplate;
        this.workshopRepository = workshopRepository;
    }

    @Override
    public boolean isUniqueRequest(UUID key) {
        String path = IDEMPOTENCY_PREFIX + key;
        return Boolean.TRUE.equals(
            redisTemplate.opsForValue().setIfAbsent(path, "PROCESSING", 24, TimeUnit.HOURS)
        );
    }

    @Override
    public void removeIdempotencyKey(UUID key) {
        redisTemplate.delete(IDEMPOTENCY_PREFIX + key);
    }

    @Override
    public boolean deductWorkshopSlot(UUID workshopId) {
        String slotKey = WORKSHOP_SLOTS_PREFIX + workshopId;

        // 1. Khởi tạo nếu chưa có trong Redis
        if (Boolean.FALSE.equals(redisTemplate.hasKey(slotKey))) {
            int currentAvailable = workshopRepository.getAvailableSlots(workshopId)
                    .orElseThrow(() -> new RuntimeException("Workshop not found"));
            redisTemplate.opsForValue().set(slotKey, String.valueOf(currentAvailable), 1, TimeUnit.HOURS);
        }

        // 2. Fast-fail check: Kiểm tra nhanh trước khi trừ
        String val = redisTemplate.opsForValue().get(slotKey);
        if (val != null && Integer.parseInt(val) <= 0) {
            return false;
        }

        // 3. Atomic decrement: Trừ số lượng một cách nguyên tử
        Long remaining = redisTemplate.opsForValue().decrement(slotKey);
        
        if (remaining != null && remaining < 0) {
            // Nếu vô tình trừ xuống âm (do race condition lúc khởi tạo), rollback lại về 0
            redisTemplate.opsForValue().increment(slotKey);
            return false;
        }
        return remaining != null;
    }

    @Override
    public void rollbackSlot(UUID workshopId) {
        String slotKey = WORKSHOP_SLOTS_PREFIX + workshopId;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(slotKey))) {
            redisTemplate.opsForValue().increment(slotKey);
        }
    }
}