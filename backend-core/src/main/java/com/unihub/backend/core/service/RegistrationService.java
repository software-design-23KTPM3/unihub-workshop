package com.unihub.backend.core.service;

import com.unihub.backend.core.model.dto.RegistrationRequest;
import com.unihub.backend.core.model.dto.RegistrationResponse;

import java.util.UUID;

public interface RegistrationService {
    RegistrationResponse createRegistration(UUID idempotencyKey, RegistrationRequest request);
}
