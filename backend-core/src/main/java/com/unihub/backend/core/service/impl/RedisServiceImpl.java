package com.unihub.backend.core.service.impl;

import java.util.Arrays;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import com.unihub.backend.core.service.RedisService;

@Service
public class RedisServiceImpl implements RedisService {
    private final StringRedisTemplate redisTemplate;
    private static final String WORKSHOP_SLOTS_PREFIX = "workshop_slots:";
    private static final String WORKSHOP_META_PREFIX = "workshop_meta:";

    public RedisServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Long registerUserInRedis(UUID workshopId, String userId) {
        String userSetKey = "workshop_registrations:" + workshopId;
        String slotKey = WORKSHOP_SLOTS_PREFIX + workshopId;
        String metaKey = WORKSHOP_META_PREFIX + workshopId;

        String script = "if redis.call('EXISTS', KEYS[2]) == 0 or redis.call('EXISTS', KEYS[3]) == 0 then " +
                "  return -3 " +
                "end " +
                "if redis.call('SISMEMBER', KEYS[1], ARGV[1]) == 1 then " +
                "  return -1 " +
                "end " +
                "local status = redis.call('HGET', KEYS[3], 'status') " +
                "if status ~= 'ACTIVE' then " +
                "  return -4 " +
                "end " +
                "local registration_start = tonumber(redis.call('HGET', KEYS[3], 'registration_start_epoch')) " +
                "local registration_end = tonumber(redis.call('HGET', KEYS[3], 'registration_end_epoch')) " +
                "local now = tonumber(ARGV[2]) " +
                "if registration_start == nil or registration_end == nil then " +
                "  return -3 " +
                "end " +
                "if now < registration_start then " +
                "  return -5 " +
                "end " +
                "if now > registration_end then " +
                "  return -6 " +
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
                Arrays.asList(userSetKey, slotKey, metaKey),
                userId,
                String.valueOf(java.time.Instant.now().getEpochSecond()));
    }

    @Override
    public void initializeWorkshopSlots(UUID workshopId, int slots) {
        String slotKey = WORKSHOP_SLOTS_PREFIX + workshopId;
        redisTemplate.opsForValue().set(slotKey, String.valueOf(slots));
    }

    @Override
    public void rollbackRegistration(UUID workshopId, String userId) {
        String userSetKey = "workshop_registrations:" + workshopId;
        String slotKey = WORKSHOP_SLOTS_PREFIX + workshopId;

        String script = "local removed = redis.call('SREM', KEYS[1], ARGV[1]) " +
                "if removed == 1 and redis.call('EXISTS', KEYS[2]) == 1 then " +
                "  redis.call('INCR', KEYS[2]) " +
                "end " +
                "return removed";

        redisTemplate.execute(
                new DefaultRedisScript<>(script, Long.class),
                Arrays.asList(userSetKey, slotKey),
                userId);
    }
}
