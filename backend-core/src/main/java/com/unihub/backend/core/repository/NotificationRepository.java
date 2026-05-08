package com.unihub.backend.core.repository;

import com.unihub.backend.core.model.entity.Notification;
import com.unihub.backend.core.model.enums.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByStatus(NotificationStatus status);
    List<Notification> findByStudentMssvOrderByCreatedAtDesc(String mssv);
}
