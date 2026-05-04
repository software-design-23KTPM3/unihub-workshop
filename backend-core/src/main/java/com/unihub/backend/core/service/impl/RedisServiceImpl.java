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

        if (Boolean.FALSE.equals(redisTemplate.hasKey(slotKey))) {
            int currentAvailable = workshopRepository.getAvailableSlots(workshopId)
                    .orElseThrow(() -> new RuntimeException("Workshop not found"));
            redisTemplate.opsForValue().set(slotKey, String.valueOf(currentAvailable), 1, TimeUnit.HOURS);
        }

        Long remaining = redisTemplate.opsForValue().decrement(slotKey);
        
        if (remaining != null && remaining < 0) {
            redisTemplate.opsForValue().increment(slotKey);
            return false;
        }
        return remaining != null;
    }

    @Override
    public boolean registerUserInRedis(UUID workshopId, String userId) {
        String userSetKey = "workshop_registrations:" + workshopId;
        String slotKey = WORKSHOP_SLOTS_PREFIX + workshopId;

        String script = 
            "if redis.call('SISMEMBER', KEYS[1], ARGV[1]) == 1 then " +
            "  return -1 " + // Already registered
            "end " +
            "local remaining = redis.call('DECR', KEYS[2]) " +
            "if remaining < 0 then " +
            "  redis.call('INCR', KEYS[2]) " +
            "  return -2 " + // Sold out
            "end " +
            "redis.call('SADD', KEYS[1], ARGV[1]) " +
            "return 1"; // Success

        // If slot key doesn't exist, we must initialize it
        if (Boolean.FALSE.equals(redisTemplate.hasKey(slotKey))) {
            int currentAvailable = workshopRepository.getAvailableSlots(workshopId)
                    .orElseThrow(() -> new RuntimeException("Workshop not found"));
            redisTemplate.opsForValue().set(slotKey, String.valueOf(currentAvailable), 1, TimeUnit.HOURS);
        }

        Long result = redisTemplate.execute(
            new org.springframework.data.redis.core.script.DefaultRedisScript<>(script, Long.class),
            java.util.Arrays.asList(userSetKey, slotKey),
            userId
        );

        return result != null && result == 1;
    }

    @Override
    public boolean isUserRegisteredInRedis(UUID workshopId, String userId) {
        String userSetKey = "workshop_registrations:" + workshopId;
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(userSetKey, userId));
    }

    @Override
    public void rollbackSlot(UUID workshopId) {
        String slotKey = WORKSHOP_SLOTS_PREFIX + workshopId;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(slotKey))) {
            redisTemplate.opsForValue().increment(slotKey);
        }
        // Also remove user from set if we are rolling back a specific user
    }
}