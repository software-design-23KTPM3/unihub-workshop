package com.unihub.backend.core.worker.strategy;

import com.unihub.backend.core.model.entity.Notification;
import com.unihub.backend.core.model.enums.NotificationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
public class EmailNotificationStrategy implements NotificationStrategy {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EmailNotificationStrategy.class);
    @Override
    public NotificationType getType() {
        return NotificationType.EMAIL;
    }

    @Override
    public void send(Notification notification) {
        log.info("Sending EMAIL to student {}: {}", notification.getStudentId(), notification.getContent());
        // Mock SMTP call
    }
}
