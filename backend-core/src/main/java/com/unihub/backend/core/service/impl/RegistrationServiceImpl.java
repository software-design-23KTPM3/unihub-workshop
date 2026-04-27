package com.unihub.backend.core.service.impl;

import com.unihub.backend.core.config.RabbitConfig;
import com.unihub.backend.core.model.dto.*;
import com.unihub.backend.core.model.entity.*;
import com.unihub.backend.core.model.enums.RegistrationStatus;
import com.unihub.backend.core.repository.*;
import com.unihub.backend.core.service.RegistrationService;
import com.unihub.backend.core.service.RedisService; // Service mới
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.unihub.backend.core.service.AsyncDbService;

import java.util.UUID;

@Service
public class RegistrationServiceImpl implements RegistrationService {

        private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RegistrationServiceImpl.class);

        private final RedisService redisService;
        private final RabbitTemplate rabbitTemplate;
        private final WorkshopRepository workshopRepository;
        private final StudentRepository studentRepository;
        private final AsyncDbService asyncDbService;

        public RegistrationServiceImpl(
                        RedisService redisService,
                        RabbitTemplate rabbitTemplate,
                        WorkshopRepository workshopRepository,
                        StudentRepository studentRepository,
                        AsyncDbService asyncDbService) {
                this.redisService = redisService;
                this.rabbitTemplate = rabbitTemplate;
                this.workshopRepository = workshopRepository;
                this.studentRepository = studentRepository;
                this.asyncDbService = asyncDbService;
        }

        @Override
        @Transactional
        public RegistrationResponse createRegistration(UUID idempotencyKey, RegistrationRequest request) {

                if (!redisService.isUniqueRequest(idempotencyKey)) {
                        return RegistrationResponse.builder()
                                        .status(RegistrationStatus.PENDING)
                                        .message("Request is already being processed or completed.")
                                        .build();
                }

                try {
                        if (!redisService.deductWorkshopSlot(request.getWorkshopId())) {
                                redisService.removeIdempotencyKey(idempotencyKey);
                                return RegistrationResponse.builder()
                                                .status(RegistrationStatus.FAILED)
                                                .message("Workshop is sold out")
                                                .build();
                        }

                        Workshop workshop = workshopRepository.findById(request.getWorkshopId())
                                        .orElseThrow(() -> new RuntimeException("Workshop not found"));
                        Student student = studentRepository.findById(request.getStudentId())
                                        .orElseThrow(() -> new RuntimeException("Student not found"));

                        asyncDbService.saveRegistrationAsync(workshop, student, idempotencyKey);

                        log.info("Registration successful for student {} and workshop {}",
                                        request.getStudentId(), request.getWorkshopId());

                        sendNotification(workshop, student);

                        return RegistrationResponse.builder()
                                        .registrationId(idempotencyKey)
                                        .status(RegistrationStatus.PENDING)
                                        .message("Registration successful. Notification sent.")
                                        .build();

                } catch (Exception e) {
                        log.error("Error during registration process: {}", e.getMessage());

                        redisService.rollbackSlot(request.getWorkshopId());
                        redisService.removeIdempotencyKey(idempotencyKey);

                        throw new RuntimeException("Failed to process registration", e);
                }
        }

        private void sendNotification(Workshop workshop, Student student) {
                NotificationData data = NotificationData.builder()
                                .title("REGISTER WORKSHOP " + workshop.getName() + " SUCCESS")
                                .msg("Congratulations " + student.getName() + "! You have successfully registered.")
                                .to(student.getEmail())
                                .build();

                NotificationRequest notificationRequest = NotificationRequest.builder()
                                .type("EMAIL")
                                .data(data)
                                .build();

                rabbitTemplate.convertAndSend(
                                RabbitConfig.REGISTRATION_EXCHANGE,
                                RabbitConfig.NOTIFICATION_ROUTING_KEY,
                                notificationRequest);

                log.info("Message sent to RabbitMQ for workshop: {}", workshop.getName());
        }
}