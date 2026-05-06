package com.unihub.backend.core.controller;

import com.unihub.backend.core.model.dto.AdminRegistrationResponse;
import com.unihub.backend.core.service.RegistrationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/registrations")
public class AdminRegistrationController {

    private final RegistrationService registrationService;

    public AdminRegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public List<AdminRegistrationResponse> getRegistrations(
            Authentication authentication,
            @RequestParam Map<String, String> filters) {
        return registrationService.getAdminRegistrations(authentication, filters);
    }
}
