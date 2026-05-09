package com.unihub.backend.core.repository;

import com.unihub.backend.core.model.entity.Notification;
import com.unihub.backend.core.model.entity.Student;
import com.unihub.backend.core.model.enums.NotificationStatus;
import com.unihub.backend.core.model.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByStatus(NotificationStatus status);
    List<Notification> findByStudentMssvOrderByCreatedAtDesc(String mssv);
    List<Notification> findByStudentAndTypeAndStatusOrderByCreatedAtDesc(Student student, NotificationType type, NotificationStatus status);
}
