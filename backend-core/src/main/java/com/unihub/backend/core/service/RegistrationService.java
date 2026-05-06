package com.unihub.backend.core.service;

import com.unihub.backend.core.model.dto.RegistrationDetailResponse;
import com.unihub.backend.core.model.dto.RegistrationRequest;
import com.unihub.backend.core.model.dto.RegistrationResponse;
import com.unihub.backend.core.model.dto.AdminRegistrationResponse;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface RegistrationService {
    RegistrationResponse createRegistration(UUID idempotencyKey, RegistrationRequest request);
    RegistrationDetailResponse getRegistrationById(UUID id);
    List<AdminRegistrationResponse> getAdminRegistrations(Authentication authentication, Map<String, String> filters);
}
