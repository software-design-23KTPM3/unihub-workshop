package com.unihub.backend.core.service.impl;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import com.unihub.backend.core.service.RedisService;

@Service
public class RedisServiceImpl implements RedisService {
    private final StringRedisTemplate redisTemplate;
    private static final String WORKSHOP_SLOTS_PREFIX = "workshop_slots:";

    public RedisServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Long registerUserInRedis(UUID workshopId, String userId) {
        String userSetKey = "workshop_registrations:" + workshopId;
        String slotKey = WORKSHOP_SLOTS_PREFIX + workshopId;

        String script = "if redis.call('EXISTS', KEYS[2]) == 0 then " +
                "  return -3 " +
                "end " +
                "if redis.call('SISMEMBER', KEYS[1], ARGV[1]) == 1 then " +
                "  return -1 " +
                "end " +
                "local remaining = redis.call('DECR', KEYS[2]) " +
                "if remaining < 0 then " +
                "  redis.call('INCR', KEYS[2]) " +
                "  return -2 " +
                "end " +
                "redis.call('SADD', KEYS[1], ARGV[1]) " +
                "return 1";

        return redisTemplate.execute(
                new DefaultRedisScript<>(script, Long.class),
                Arrays.asList(userSetKey, slotKey),
                userId);
    }

    @Override
    public void initializeWorkshopSlots(UUID workshopId, int slots) {
        String slotKey = WORKSHOP_SLOTS_PREFIX + workshopId;
        redisTemplate.opsForValue().set(slotKey, String.valueOf(slots), 24, TimeUnit.HOURS);
    }

    @Override
    public void rollbackRegistration(UUID workshopId, String userId) {
        String userSetKey = "workshop_registrations:" + workshopId;
        String slotKey = WORKSHOP_SLOTS_PREFIX + workshopId;
        
        redisTemplate.opsForSet().remove(userSetKey, userId);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(slotKey))) {
            redisTemplate.opsForValue().increment(slotKey);
        }
    }
}