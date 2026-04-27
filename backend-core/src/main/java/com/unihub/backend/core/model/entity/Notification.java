package com.unihub.backend.core.model.entity;

import com.unihub.backend.core.model.enums.NotificationStatus;
import com.unihub.backend.core.model.enums.NotificationType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "student_id")
    private String studentId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;
    @Column(nullable = false)
    private String content;
    @Enumerated(EnumType.STRING)
    private NotificationStatus status;
    @Column(name = "error_message")
    private String errorMessage;
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt;
    @Column(name = "sent_at")
    private ZonedDateTime sentAt;

    public Notification() {}
    public Notification(String studentId, NotificationType type, String content, NotificationStatus status) {
        this.studentId = studentId;
        this.type = type;
        this.content = content;
        this.status = status;
    }

    public UUID getId() { return id; }
    public String getStudentId() { return studentId; }
    public String getContent() { return content; }
    public NotificationType getType() { return type; }
    public NotificationStatus getStatus() { return status; }
    public void setStatus(NotificationStatus status) { this.status = status; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public void setSentAt(ZonedDateTime sentAt) { this.sentAt = sentAt; }

    public static NotificationBuilder builder() { return new NotificationBuilder(); }
    public static class NotificationBuilder {
        private String studentId;
        private NotificationType type;
        private String content;
        private NotificationStatus status;
        public NotificationBuilder studentId(String studentId) { this.studentId = studentId; return this; }
        public NotificationBuilder type(NotificationType type) { this.type = type; return this; }
        public NotificationBuilder content(String content) { this.content = content; return this; }
        public NotificationBuilder status(NotificationStatus status) { this.status = status; return this; }
        public Notification build() { return new Notification(studentId, type, content, status); }
    }
}
