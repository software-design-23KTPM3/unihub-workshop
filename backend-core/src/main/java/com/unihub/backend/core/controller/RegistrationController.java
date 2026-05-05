package com.unihub.backend.core.controller;

import com.unihub.backend.core.model.dto.RegistrationDetailResponse;
import com.unihub.backend.core.model.dto.RegistrationRequest;
import com.unihub.backend.core.model.dto.RegistrationResponse;
import com.unihub.backend.core.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/registrations")
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public RegistrationResponse createRegistration(
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @Valid @RequestBody RegistrationRequest request) {
        return registrationService.createRegistration(idempotencyKey, request);
    }

    @GetMapping("/{id}")
    public RegistrationDetailResponse getRegistrationById(@PathVariable UUID id) {
        return registrationService.getRegistrationById(id);
    }
}
