package com.unihub.backend.core.model.dto;

import java.time.ZonedDateTime;
import java.util.UUID;

public class CheckinEvent {
    private String studentId;
    private UUID workshopId;
    private ZonedDateTime checkinAt;

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public UUID getWorkshopId() { return workshopId; }
    public void setWorkshopId(UUID workshopId) { this.workshopId = workshopId; }
    public ZonedDateTime getCheckinAt() { return checkinAt; }
    public void setCheckinAt(ZonedDateTime checkinAt) { this.checkinAt = checkinAt; }
}
