package com.unihub.backend.core.service.impl;

import com.unihub.backend.core.config.RabbitConfig;
import com.unihub.backend.core.model.dto.*;
import com.unihub.backend.core.model.entity.*;
import com.unihub.backend.core.model.enums.RegistrationStatus;
import com.unihub.backend.core.repository.*;
import com.unihub.backend.core.service.RegistrationService;
import com.unihub.backend.core.service.RedisService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.unihub.backend.core.service.AsyncDbService;
import com.unihub.backend.core.exception.RegistrationConflictException;
import com.unihub.backend.core.exception.WorkshopSoldOutException;
import com.unihub.backend.core.exception.InvalidWorkshopException;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class RegistrationServiceImpl implements RegistrationService {

        private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RegistrationServiceImpl.class);

        private final RedisService redisService;
        private final RabbitTemplate rabbitTemplate;
        private final WorkshopRepository workshopRepository;
        private final StudentRepository studentRepository;
        private final RegistrationRepository registrationRepository;
        private final AsyncDbService asyncDbService;

        public RegistrationServiceImpl(
                        RedisService redisService,
                        RabbitTemplate rabbitTemplate,
                        WorkshopRepository workshopRepository,
                        StudentRepository studentRepository,
                        RegistrationRepository registrationRepository,
                        AsyncDbService asyncDbService) {
                this.redisService = redisService;
                this.rabbitTemplate = rabbitTemplate;
                this.workshopRepository = workshopRepository;
                this.studentRepository = studentRepository;
                this.registrationRepository = registrationRepository;
                this.asyncDbService = asyncDbService;
        }

        @Override
        @Transactional
        public RegistrationResponse createRegistration(RegistrationRequest request) {
                String studentMssv = request.getStudentId();
                UUID workshopId = request.getWorkshopId();
                UUID registrationId = UUID.randomUUID();

                Long result = redisService.registerUserInRedis(workshopId, studentMssv);

                if (result == null || result != 1) {
                        if (result != null && result == -1) {
                                throw new RegistrationConflictException(
                                                "You have already registered for this workshop");
                        } else if (result != null && result == -2) {
                                throw new WorkshopSoldOutException("Workshop is sold out");
                        } else {
                                throw new InvalidWorkshopException("Workshop is not available for registration");
                        }
                }

                try {
                        CompletableFuture<Workshop> workshopFuture = CompletableFuture
                                        .supplyAsync(() -> workshopRepository.findById(workshopId)
                                                        .orElseThrow(() -> new RuntimeException("Workshop not found")));
                        CompletableFuture<Student> studentFuture = CompletableFuture
                                        .supplyAsync(() -> studentRepository.findById(studentMssv)
                                                        .orElseThrow(() -> new RuntimeException("Student not found")));
                        CompletableFuture.allOf(workshopFuture, studentFuture).join();
                        Workshop workshop = workshopFuture.get();
                        Student student = studentFuture.get();
                        RegistrationStatus initialStatus = workshop.getIsPaid() ? RegistrationStatus.PENDING
                                        : RegistrationStatus.SUCCESS;
                        asyncDbService.saveRegistrationAsync(workshop, student, registrationId, initialStatus);
                        return RegistrationResponse.builder()
                                        .registrationId(registrationId)
                                        .status(initialStatus)
                                        .message(initialStatus == RegistrationStatus.SUCCESS
                                                        ? "Registration successful."
                                                        : "Registration initiated. Please check your 'Order History' (Lịch sử đăng ký) to complete payment within 30 minutes to secure your seat.")
                                        .build();
                } catch (Exception e) {
                        log.error("Error during registration processing for student {} and workshop {}: {}. Rolling back Redis reservation...",
                                        studentMssv, workshopId, e.getMessage());

                        redisService.rollbackRegistration(workshopId, studentMssv);

                        throw new RuntimeException("Failed to process registration data. Please try again later.", e);
                }
        }

        @Override
        public java.util.List<RegistrationDetailResponse> getMyRegistrations(
                        org.springframework.security.core.Authentication authentication) {
                String mssv = authentication.getName();
                return registrationRepository.findByStudentMssvOrderByCreatedAtDesc(mssv).stream()
                                .map(this::mapToDetailResponse)
                                .collect(java.util.stream.Collectors.toList());
        }

        @Override
        public java.util.List<RegistrationDetailResponse> getAllRegistrations(java.util.Map<String, String> filters) {
                java.util.List<Registration> registrations;
                if (filters.containsKey("workshopId")) {
                        registrations = registrationRepository
                                        .findByWorkshopId(UUID.fromString(filters.get("workshopId")));
                } else {
                        registrations = registrationRepository.findAll();
                }

                return registrations.stream()
                                .map(this::mapToDetailResponse)
                                .collect(java.util.stream.Collectors.toList());
        }

        @Override
        public RegistrationDetailResponse getRegistrationById(UUID id) {
                return registrationRepository.findById(id)
                                .map(this::mapToDetailResponse)
                                .orElseThrow(() -> new RuntimeException("Registration not found"));
        }

        @Override
        @Transactional
        public RegistrationDetailResponse confirmRegistration(UUID id) {
                Registration registration = registrationRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Registration not found"));

                if (registration.getStatus() != RegistrationStatus.SUCCESS) {
                        registration.setStatus(RegistrationStatus.SUCCESS);
                        registration = registrationRepository.save(registration);

                        // Sau khi thanh toán thành công, gửi thông báo
                        sendNotification(registration.getWorkshop(), registration.getStudent());
                }

                return mapToDetailResponse(registration);
        }

        private RegistrationDetailResponse mapToDetailResponse(Registration registration) {
                Workshop workshop = registration.getWorkshop();
                Student student = registration.getStudent();

                WorkshopResponse workshopResponse = WorkshopResponse.builder()
                                .id(workshop.getId())
                                .title(workshop.getName())
                                .speakerName(workshop.getSpeaker())
                                .topic(workshop.getTopic())
                                .room(workshop.getRoom())
                                .date(workshop.getStartTime().toLocalDate().toString())
                                .status(workshop.getStatus().toString())
                                .build();

                return RegistrationDetailResponse.builder()
                                .id(registration.getId())
                                .workshop(workshopResponse)
                                .studentId(student.getMssv())
                                .studentName(student.getName())
                                .studentEmail(student.getEmail())
                                .status(registration.getStatus())
                                .qrCode(registration.getQrCode())
                                .registeredAt(registration.getCreatedAt().toString())
                                .paymentStatus(workshop.getIsPaid() ? "PENDING" : "FREE")
                                .build();
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
                                RabbitConfig.NOTIFICATION_EXCHANGE,
                                RabbitConfig.NOTIFICATION_ROUTING_KEY,
                                notificationRequest);

                log.info("Message sent to RabbitMQ for workshop: {}", workshop.getName());
        }
}