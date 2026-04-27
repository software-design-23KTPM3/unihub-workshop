package com.unihub.backend.core.controller;

import com.unihub.backend.core.model.dto.WorkshopRequest;
import com.unihub.backend.core.model.dto.WorkshopResponse;
import com.unihub.backend.core.service.WorkshopService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class WorkshopController {

    private final WorkshopService workshopService;

    public WorkshopController(WorkshopService workshopService) {
        this.workshopService = workshopService;
    }

    @GetMapping("/v1/workshops")
    public List<WorkshopResponse> getAllWorkshops() {
        return workshopService.getAllWorkshops();
    }

    @GetMapping("/v1/workshops/{id}")
    public WorkshopResponse getWorkshopById(@PathVariable UUID id) {
        return workshopService.getWorkshopById(id);
    }

    @PostMapping("/admin/workshops")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public WorkshopResponse createWorkshop(@Valid @RequestBody WorkshopRequest request) {
        return workshopService.createWorkshop(request);
    }

    @PatchMapping("/admin/workshops/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public WorkshopResponse updateWorkshop(@PathVariable UUID id, @Valid @RequestBody WorkshopRequest request) {
        return workshopService.updateWorkshop(id, request);
    }

    @PatchMapping("/admin/workshops/{id}/cancel")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public void cancelWorkshop(@PathVariable UUID id) {
        workshopService.cancelWorkshop(id);
    }

    @PostMapping("/admin/workshops/{id}/pdf")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public void uploadPdf(@PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        workshopService.uploadPdf(id, file);
    }
}
