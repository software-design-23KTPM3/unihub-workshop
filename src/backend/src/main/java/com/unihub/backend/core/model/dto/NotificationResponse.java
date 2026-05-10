package com.unihub.backend.core.model.dto;

import com.unihub.backend.core.model.entity.Notification;

public class NotificationResponse {
    private String id;
    private String type;
    private String content;
    private String status;
    private String createdAt;

    public static NotificationResponse from(Notification notification) {
        NotificationResponse response = new NotificationResponse();
        response.id = notification.getId().toString();
        response.type = notification.getType().name();
        response.content = notification.getContent();
        response.status = notification.getStatus().name();
        response.createdAt = notification.getCreatedAt() == null ? null : notification.getCreatedAt().toString();
        return response;
    }

    public String getId() { return id; }
    public String getType() { return type; }
    public String getContent() { return content; }
    public String getStatus() { return status; }
    public String getCreatedAt() { return createdAt; }
}
