package com.unihub.backend.core.service;

import java.util.UUID;

public interface RedisService {
    boolean isUniqueRequest(UUID key);
    void removeIdempotencyKey(UUID key);
    boolean deductWorkshopSlot(UUID workshopId);
    void rollbackSlot(UUID workshopId);
}