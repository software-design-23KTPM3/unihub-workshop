package com.example.unihubworkshop.features.workshop.data.datasource;

public class RegistrationRequestDto {
    private String workshopId;
    private String studentId;
    private String idempotencyKey;

    public RegistrationRequestDto(String workshopId, String studentId) {
        this.workshopId = workshopId;
        this.studentId = studentId;
        this.idempotencyKey = java.util.UUID.randomUUID().toString();
    }

    public String getWorkshopId() { return workshopId; }
    public String getStudentId() { return studentId; }
    public String getIdempotencyKey() { return idempotencyKey; }
}
