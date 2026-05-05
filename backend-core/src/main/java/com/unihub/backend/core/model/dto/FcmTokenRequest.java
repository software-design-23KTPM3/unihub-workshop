package com.unihub.backend.core.model.dto;

import lombok.Data;

@Data
public class FcmTokenRequest {
    private String studentId;
    private String token;
}
