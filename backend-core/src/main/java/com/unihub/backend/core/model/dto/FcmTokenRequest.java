package com.unihub.backend.core.model.dto;

import lombok.Data;

@Data
public class FcmTokenRequest {
    private String studentId;
    private String token;

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}
