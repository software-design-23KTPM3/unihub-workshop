package com.unihub.backend.core.service;

import com.unihub.backend.core.model.dto.WorkshopRequest;
import com.unihub.backend.core.model.dto.WorkshopResponse;

import java.util.List;
import java.util.UUID;

public interface WorkshopService {
    List<WorkshopResponse> getAllWorkshops();
    WorkshopResponse getWorkshopById(UUID id);
    WorkshopResponse createWorkshop(WorkshopRequest request);
    WorkshopResponse updateWorkshop(UUID id, WorkshopRequest request);
    void cancelWorkshop(UUID id);
    void uploadPdf(UUID id, org.springframework.web.multipart.MultipartFile file);
}
