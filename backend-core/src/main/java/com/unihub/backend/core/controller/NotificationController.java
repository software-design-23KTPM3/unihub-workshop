package com.unihub.backend.core.controller;

import com.unihub.backend.core.model.dto.NotificationResponse;
import com.unihub.backend.core.model.entity.Notification;
import com.unihub.backend.core.model.enums.NotificationStatus;
import com.unihub.backend.core.model.enums.NotificationType;
import com.unihub.backend.core.repository.NotificationRepository;
import com.unihub.backend.core.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final StudentRepository studentRepository;

    @GetMapping("/unread")
    public ResponseEntity<List<NotificationResponse>> getUnreadNotifications(@AuthenticationPrincipal Jwt jwt) {
        String studentMssv = jwt.getClaimAsString("studentId");
        if (studentMssv == null || studentMssv.isBlank()) {
            studentMssv = jwt.getClaimAsString("preferred_username");
        }
        
        String finalStudentMssv = studentMssv;
        return studentRepository.findById(finalStudentMssv)
                .map(student -> {
                    List<Notification> notifications = notificationRepository
                            .findByStudentAndTypeAndStatusOrderByCreatedAtDesc(
                                    student, NotificationType.IN_APP, NotificationStatus.PENDING);
                    return ResponseEntity.ok(notifications.stream()
                            .map(NotificationResponse::from)
                            .toList());
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        String studentMssv = jwt.getClaimAsString("studentId");
        if (studentMssv == null || studentMssv.isBlank()) {
            studentMssv = jwt.getClaimAsString("preferred_username");
        }
        
        String finalStudentMssv = studentMssv;
        return notificationRepository.findById(id)
                .map(notification -> {
                    if (notification.getStudent().getMssv().equals(finalStudentMssv)) {
                        notification.setStatus(NotificationStatus.SENT);
                        notificationRepository.save(notification);
                        return ResponseEntity.ok().<Void>build();
                    }
                    return ResponseEntity.status(403).<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
