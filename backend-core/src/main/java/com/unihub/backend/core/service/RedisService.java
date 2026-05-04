package com.unihub.backend.core.service;

import java.util.UUID;

public interface RedisService {
    boolean isUniqueRequest(UUID key);
    void removeIdempotencyKey(UUID key);
    boolean deductWorkshopSlot(UUID workshopId);
    void rollbackSlot(UUID workshopId);
    
    // New methods for the revised flow
    boolean registerUserInRedis(UUID workshopId, String userId);
    boolean isUserRegisteredInRedis(UUID workshopId, String userId);
}