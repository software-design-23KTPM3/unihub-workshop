package com.unihub.backend.core.worker.strategy;

import com.unihub.backend.core.model.entity.Notification;
import com.unihub.backend.core.model.enums.NotificationType;

public interface NotificationStrategy {
    NotificationType getType();
    void send(Notification notification);
}
