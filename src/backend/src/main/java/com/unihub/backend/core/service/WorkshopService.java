package com.unihub.backend.core.service;

import com.unihub.backend.core.model.dto.WorkshopRequest;
import com.unihub.backend.core.model.dto.WorkshopResponse;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface WorkshopService {
    List<WorkshopResponse> getAllWorkshops(Authentication authentication, Map<String, String> filters);

    WorkshopResponse getWorkshopById(UUID id, Authentication authentication);

    WorkshopResponse createWorkshop(WorkshopRequest request, org.springframework.web.multipart.MultipartFile file,
            Authentication authentication);

    WorkshopResponse updateWorkshop(UUID id, WorkshopRequest request,
            org.springframework.web.multipart.MultipartFile file, Authentication authentication);

    WorkshopResponse cancelWorkshop(UUID id, Authentication authentication);

    void uploadPdf(UUID id, org.springframework.web.multipart.MultipartFile file);
}
