package com.unihub.backend.core.service;

import java.util.UUID;

public interface RedisService {
    Long registerUserInRedis(UUID workshopId, String userId);

    void initializeWorkshopSlots(UUID workshopId, int slots);

    void rollbackRegistration(UUID workshopId, String userId);
}