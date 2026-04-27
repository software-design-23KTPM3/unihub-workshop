package com.unihub.backend.core.worker;

import com.unihub.backend.core.model.entity.Notification;
import com.unihub.backend.core.model.enums.NotificationStatus;
import com.unihub.backend.core.model.enums.NotificationType;
import com.unihub.backend.core.repository.NotificationRepository;
import com.unihub.backend.core.worker.strategy.NotificationStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class NotificationWorker {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NotificationWorker.class);

    private final NotificationRepository notificationRepository;
    private final Map<NotificationType, NotificationStrategy> strategies;

    public NotificationWorker(NotificationRepository notificationRepository, List<NotificationStrategy> strategyList) {
        this.notificationRepository = notificationRepository;
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(NotificationStrategy::getType, s -> s));
    }

    @RabbitListener(queues = "notification.queue")
    public void handleRegistrationNotification(com.unihub.backend.core.model.dto.RegistrationRequest request) {
        log.info("Processing notification for registration: {}", request.getIdempotencyKey());

        try {
            Notification notification = Notification.builder()
                    .studentId(request.getStudentId())
                    .type(NotificationType.EMAIL)
                    .content("Registration success! QR: QR_" + request.getStudentId())
                    .status(NotificationStatus.PENDING)
                    .workshopId(request.getWorkshopId())
                    .build();

            NotificationStrategy strategy = strategies.get(notification.getType());
            if (strategy != null) {
                strategy.send(notification);
                notification.setStatus(NotificationStatus.SENT);
                notification.setSentAt(ZonedDateTime.now());
            } else {
                notification.setStatus(NotificationStatus.FAILED);
                notification.setErrorMessage("No strategy found for " + notification.getType());
            }

            notificationRepository.save(notification);

        } catch (Exception e) {
            log.error("Fatal error in NotificationWorker: {}", e.getMessage());
        }
    }
}
