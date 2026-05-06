package com.unihub.backend.core.controller;

import com.unihub.backend.core.model.dto.RegistrationRequest;
import com.unihub.backend.core.model.dto.RegistrationResponse;
import com.unihub.backend.core.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping("/registrations")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public RegistrationResponse createRegistration(
            @Valid @RequestBody RegistrationRequest request) {
        return registrationService.createRegistration(request);
    }

    @GetMapping("/me/registrations")
    public java.util.List<com.unihub.backend.core.model.dto.RegistrationDetailResponse> getMyRegistrations(
            org.springframework.security.core.Authentication authentication) {
        return registrationService.getMyRegistrations(authentication);
    }

    @GetMapping("/admin/registrations")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public java.util.List<com.unihub.backend.core.model.dto.RegistrationDetailResponse> getAllRegistrations(
            @RequestParam java.util.Map<String, String> filters) {
        return registrationService.getAllRegistrations(filters);
    }

    @GetMapping("/registrations/{id}")
    public com.unihub.backend.core.model.dto.RegistrationDetailResponse getRegistrationById(@PathVariable UUID id) {
        return registrationService.getRegistrationById(id);
    }

    @PostMapping("/registrations/{id}/payment/mock-success")
    public com.unihub.backend.core.model.dto.RegistrationDetailResponse mockPaymentSuccess(@PathVariable UUID id) {
        return registrationService.confirmRegistration(id);
    }
}
