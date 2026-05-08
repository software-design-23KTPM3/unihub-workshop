package com.unihub.backend.core.service;

import com.unihub.backend.core.model.dto.RegistrationRequest;
import com.unihub.backend.core.model.dto.RegistrationResponse;
import com.unihub.backend.core.model.dto.RegistrationDetailResponse;

import java.util.UUID;

public interface RegistrationService {
    RegistrationResponse createRegistration(RegistrationRequest request, org.springframework.security.core.Authentication authentication);
    java.util.List<RegistrationDetailResponse> getMyRegistrations(org.springframework.security.core.Authentication authentication);
    java.util.List<RegistrationDetailResponse> getAllRegistrations(java.util.Map<String, String> filters);
    RegistrationDetailResponse getRegistrationById(UUID id);
    byte[] getRegistrationQrPng(UUID id, org.springframework.security.core.Authentication authentication);
    RegistrationDetailResponse confirmRegistration(UUID id);
}
