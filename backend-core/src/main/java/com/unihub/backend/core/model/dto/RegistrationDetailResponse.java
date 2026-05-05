package com.unihub.backend.core.model.dto;

import com.unihub.backend.core.model.enums.RegistrationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationDetailResponse {
    private UUID id;
    private WorkshopResponse workshop;
    private String studentId;
    private String studentName;
    private RegistrationStatus status;
    private String qrCode;
    private String registeredAt;
}
