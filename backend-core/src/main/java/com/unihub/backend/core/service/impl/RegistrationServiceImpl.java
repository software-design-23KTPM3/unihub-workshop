package com.unihub.backend.core.service.impl;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.unihub.backend.core.config.RabbitConfig;
import com.unihub.backend.core.model.dto.*;
import com.unihub.backend.core.model.entity.*;
import com.unihub.backend.core.model.enums.RegistrationStatus;
import com.unihub.backend.core.model.enums.NotificationStatus;
import com.unihub.backend.core.model.enums.NotificationType;
import com.unihub.backend.core.repository.*;
import com.unihub.backend.core.service.RegistrationService;
import com.unihub.backend.core.service.RedisService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import com.unihub.backend.core.exception.RegistrationConflictException;
import com.unihub.backend.core.exception.WorkshopSoldOutException;
import com.unihub.backend.core.exception.InvalidWorkshopException;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
public class RegistrationServiceImpl implements RegistrationService {

        private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RegistrationServiceImpl.class);
        private static final DateTimeFormatter EMAIL_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        private final RedisService redisService;
        private final RabbitTemplate rabbitTemplate;
        private final WorkshopRepository workshopRepository;
        private final StudentRepository studentRepository;
        private final RegistrationRepository registrationRepository;
        private final TransactionRepository transactionRepository;
        private final TransactionTemplate transactionTemplate;
        private final NotificationRepository notificationRepository;

        public RegistrationServiceImpl(
                        RedisService redisService,
                        RabbitTemplate rabbitTemplate,
                        WorkshopRepository workshopRepository,
                        StudentRepository studentRepository,
                        RegistrationRepository registrationRepository,
                        TransactionRepository transactionRepository,
                        org.springframework.transaction.PlatformTransactionManager transactionManager,
                        NotificationRepository notificationRepository) {
                this.redisService = redisService;
                this.rabbitTemplate = rabbitTemplate;
                this.workshopRepository = workshopRepository;
                this.studentRepository = studentRepository;
                this.registrationRepository = registrationRepository;
                this.transactionRepository = transactionRepository;
                this.transactionTemplate = new TransactionTemplate(transactionManager);
                this.notificationRepository = notificationRepository;
        }

        @Override
        public RegistrationResponse createRegistration(RegistrationRequest request, Authentication authentication) {
                String studentMssv = getStudentMssv(authentication);
                UUID workshopId = request.getWorkshopId();
                UUID registrationId = UUID.randomUUID();
                UUID idempotencyKey = request.getIdempotencyKey() == null ? UUID.randomUUID()
                                : request.getIdempotencyKey();

                if (request.getIdempotencyKey() != null) {
                        Registration existingRegistration = registrationRepository.findByIdempotencyKey(idempotencyKey)
                                        .orElse(null);
                        if (existingRegistration != null) {
                                return toCreateResponse(existingRegistration);
                        }
                }

                Long result = redisService.registerUserInRedis(workshopId, studentMssv);

                if (result == null || result != 1) {
                        if (result != null && result == -1) {
                                Registration existingRegistration = registrationRepository
                                                .findByStudentMssvAndWorkshopId(studentMssv, workshopId)
                                                .orElse(null);
                                if (existingRegistration != null) {
                                        return toCreateResponse(existingRegistration);
                                }
                                throw new RegistrationConflictException(
                                                "You have already registered for this workshop");
                        } else if (result != null && result == -2) {
                                throw new WorkshopSoldOutException("Workshop is sold out");
                        } else if (result != null && result == -4) {
                                throw new InvalidWorkshopException("Workshop is not active");
                        } else if (result != null && result == -5) {
                                throw new InvalidWorkshopException("Registration has not opened yet");
                        } else if (result != null && result == -6) {
                                throw new InvalidWorkshopException("Registration period has ended");
                        } else {
                                throw new InvalidWorkshopException("Workshop is not available for registration");
                        }
                }

                try {
                        Registration existingRegistration = registrationRepository
                                        .findByStudentMssvAndWorkshopId(studentMssv, workshopId)
                                        .orElse(null);
                        if (existingRegistration != null
                                        && existingRegistration.getStatus() != RegistrationStatus.FAILED) {
                                return toCreateResponse(existingRegistration);
                        }

                        Workshop workshop = workshopRepository.findById(workshopId)
                                        .orElseThrow(() -> new RuntimeException("Workshop not found"));
                        Student student = studentRepository.findById(studentMssv)
                                        .orElseThrow(() -> new RuntimeException("Student not found"));
                        RegistrationStatus initialStatus = workshop.getIsPaid() ? RegistrationStatus.PENDING
                                        : RegistrationStatus.SUCCESS;

                        startAsyncRegistrationTasks(
                                        registrationId,
                                        idempotencyKey,
                                        student.getMssv(),
                                        workshop.getId(),
                                        initialStatus);

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

        private void startAsyncRegistrationTasks(
                        UUID registrationId,
                        UUID idempotencyKey,
                        String studentMssv,
                        UUID workshopId,
                        RegistrationStatus initialStatus) {
                Thread worker = new Thread(() -> persistRegistrationAndPublishTasks(
                                registrationId,
                                idempotencyKey,
                                studentMssv,
                                workshopId,
                                initialStatus),
                                "registration-task-" + registrationId);
                worker.start();
        }

        private void persistRegistrationAndPublishTasks(
                        UUID registrationId,
                        UUID idempotencyKey,
                        String studentMssv,
                        UUID workshopId,
                        RegistrationStatus initialStatus) {
                try {
                        RegistrationTaskResult taskResult = transactionTemplate.execute(status -> {
                                Registration existingRegistration = registrationRepository
                                                .findByStudentMssvAndWorkshopId(studentMssv, workshopId)
                                                .orElse(null);
                                if (existingRegistration != null) {
                                        if (existingRegistration.getStatus() != RegistrationStatus.FAILED) {
                                                return new RegistrationTaskResult(
                                                                existingRegistration.getId(),
                                                                existingRegistration.getQrCode(),
                                                                null,
                                                                null,
                                                                false);
                                        }

                                        transactionRepository.findByRegistrationId(existingRegistration.getId())
                                                        .ifPresent(transactionRepository::delete);
                                        registrationRepository.delete(existingRegistration);
                                        registrationRepository.flush();
                                }

                                Student student = studentRepository.findById(studentMssv)
                                                .orElseThrow(() -> new RuntimeException("Student not found"));
                                Workshop workshop = workshopRepository.findById(workshopId)
                                                .orElseThrow(() -> new RuntimeException("Workshop not found"));

                                Registration savedRegistration = registrationRepository.save(Registration.builder()
                                                .id(registrationId)
                                                .student(student)
                                                .workshop(workshop)
                                                .status(initialStatus)
                                                .qrCode(generateQrCode(registrationId))
                                                .idempotencyKey(idempotencyKey)
                                                .build());

                                if (workshop.getIsPaid()) {
                                        transactionRepository.save(Transaction.builder()
                                                        .registration(savedRegistration)
                                                        .amount(workshop.getPrice())
                                                        .status(com.unihub.backend.core.model.enums.TransactionStatus.PENDING)
                                                        .idempotencyKey(idempotencyKey)
                                                        .provider("SANDBOX")
                                                        .paymentUrl("sandbox://payments/" + savedRegistration.getId())
                                                        .expiresAt(ZonedDateTime.now().plusMinutes(30))
                                                        .build());
                                }

                                return new RegistrationTaskResult(
                                                savedRegistration.getId(),
                                                savedRegistration.getQrCode(),
                                                workshop,
                                                student,
                                                true);
                        });

                        if (taskResult != null && taskResult.created()) {
                                sendNotification(taskResult.workshop(), taskResult.student(), initialStatus,
                                                taskResult.qrCode());
                                try {
                                        rabbitTemplate.convertAndSend(
                                                        RabbitConfig.REGISTRATION_EXCHANGE,
                                                        RabbitConfig.REGISTRATION_ROUTING_KEY,
                                                        taskResult.registrationId().toString());
                                } catch (Exception e) {
                                        log.error("Failed to publish registration-created event for registration {}",
                                                        taskResult.registrationId(), e);
                                }
                        }
                } catch (Exception e) {
                        log.error("Async registration persistence failed for student {} and workshop {}: {}. Rolling back Redis reservation...",
                                        studentMssv, workshopId, e.getMessage(), e);
                        redisService.rollbackRegistration(workshopId, studentMssv);
                }
        }

        private String generateQrCode(UUID registrationId) {
                return registrationId.toString();
        }

        private record RegistrationTaskResult(
                        UUID registrationId,
                        String qrCode,
                        Workshop workshop,
                        Student student,
                        boolean created) {
        }

        private RegistrationResponse toCreateResponse(Registration registration) {
                return RegistrationResponse.builder()
                                .registrationId(registration.getId())
                                .status(registration.getStatus())
                                .message(registration.getStatus() == RegistrationStatus.SUCCESS
                                                ? "Registration successful."
                                                : "Registration initiated. Please check your 'Order History' (Lịch sử đăng ký) to complete payment within 30 minutes to secure your seat.")
                                .build();
        }

        private String getStudentMssv(Authentication authentication) {
                if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
                        throw new InvalidWorkshopException("Missing student identity in JWT");
                }
                return authentication.getName();
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

                if (filters.containsKey("status")) {
                        RegistrationStatus status = RegistrationStatus.valueOf(filters.get("status"));
                        registrations = registrations.stream()
                                        .filter(r -> r.getStatus() == status)
                                        .collect(java.util.stream.Collectors.toList());
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
        public byte[] getRegistrationQrPng(UUID id, Authentication authentication) {
                Registration registration = registrationRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Registration not found"));
                ensureCanViewRegistration(registration, authentication);

                if (registration.getStatus() != RegistrationStatus.SUCCESS
                                && registration.getStatus() != RegistrationStatus.CHECKED_IN) {
                        throw new InvalidWorkshopException("QR is only available after registration is confirmed");
                }

                return generateQrPng(registration.getQrCode());
        }

        @Override
        @Transactional
        public RegistrationDetailResponse confirmRegistration(UUID id) {
                Registration registration = registrationRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Registration not found"));

                if (registration.getStatus() != RegistrationStatus.SUCCESS) {
                        registration.setStatus(RegistrationStatus.SUCCESS);
                        registration = registrationRepository.save(registration);
                        transactionRepository.findByRegistrationId(registration.getId()).ifPresent(transaction -> {
                                transaction.setStatus(com.unihub.backend.core.model.enums.TransactionStatus.SUCCESS);
                                transaction.setPaidAt(ZonedDateTime.now());
                                transactionRepository.save(transaction);
                        });

                        // Sau khi thanh toán thành công, gửi thông báo
                        sendNotification(registration.getWorkshop(), registration.getStudent(),
                                        RegistrationStatus.SUCCESS, registration.getQrCode());
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
                                .startTime(workshop.getStartTime().toLocalTime().toString())
                                .endTime(workshop.getEndTime().toLocalTime().toString())
                                .price(workshop.getPrice())
                                .isPaid(workshop.getIsPaid())
                                .capacity(workshop.getMaxSeats())
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
                                .paymentStatus(paymentStatus(registration))
                                .build();
        }

        private String paymentStatus(Registration registration) {
                if (!registration.getWorkshop().getIsPaid()) {
                        return "FREE";
                }
                if (registration.getStatus() == RegistrationStatus.SUCCESS
                                || registration.getStatus() == RegistrationStatus.CHECKED_IN) {
                        return "PAID";
                }
                return transactionRepository.findByRegistrationId(registration.getId())
                                .map(transaction -> transaction.getStatus().name())
                                .orElse("PENDING");
        }

        private void ensureCanViewRegistration(Registration registration, Authentication authentication) {
                if (authentication == null || authentication.getName() == null) {
                        throw new InvalidWorkshopException("Missing user identity in JWT");
                }
                boolean privileged = authentication.getAuthorities().stream()
                                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority())
                                                || "ROLE_ORGANIZER".equals(authority.getAuthority())
                                                || "ROLE_CHECKIN_STAFF".equals(authority.getAuthority()));
                if (!privileged && !authentication.getName().equals(registration.getStudent().getMssv())) {
                        throw new InvalidWorkshopException("You can only view your own ticket QR");
                }
        }

        private byte[] generateQrPng(String payload) {
                try {
                        QRCodeWriter qrCodeWriter = new QRCodeWriter();
                        BitMatrix bitMatrix = qrCodeWriter.encode(payload, BarcodeFormat.QR_CODE, 320, 320);
                        ByteArrayOutputStream output = new ByteArrayOutputStream();
                        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", output);
                        return output.toByteArray();
                } catch (Exception e) {
                        throw new RuntimeException("Cannot generate QR image", e);
                }
        }

        private void sendNotification(Workshop workshop, Student student, RegistrationStatus status, String qrCode) {
                boolean pendingPayment = status == RegistrationStatus.PENDING;
                if (!pendingPayment) {
                        Notification inAppNotification = Notification.builder()
                                        .student(student)
                                        .workshop(workshop)
                                        .type(NotificationType.IN_APP)
                                        .content("You have successfully registered for the workshop: " + workshop.getName()
                                                        + ". Your QR ticket is ready.")
                                        .status(NotificationStatus.PENDING)
                                        .build();
                        notificationRepository.save(inAppNotification);
                }

                NotificationData data = NotificationData.builder()
                                .title(pendingPayment
                                                ? "REGISTER WORKSHOP " + workshop.getName() + " PENDING PAYMENT"
                                                : "REGISTER WORKSHOP " + workshop.getName() + " SUCCESS")
                                .msg(pendingPayment
                                                ? "Hi " + student.getName()
                                                                + ", your seat is temporarily reserved. Please complete payment within 30 minutes."
                                                : "Congratulations " + student.getName()
                                                                + "! You have successfully registered. Your QR ticket is ready below.")
                                .to(student.getEmail())
                                .qrPayload(pendingPayment ? null : qrCode)
                                .qrImageBase64(pendingPayment ? null : qrImageBase64(qrCode))
                                .workshopTitle(workshop.getName())
                                .workshopTime(formatWorkshopTime(workshop))
                                .workshopRoom(workshop.getRoom())
                                .workshopSpeaker(workshop.getSpeaker())
                                .build();

                NotificationRequest notificationRequest = NotificationRequest.builder()
                                .type("EMAIL")
                                .data(data)
                                .build();

                try {
                        rabbitTemplate.convertAndSend(
                                        RabbitConfig.NOTIFICATION_EXCHANGE,
                                        RabbitConfig.NOTIFICATION_ROUTING_KEY,
                                        notificationRequest);

                        log.info("Notification message sent to RabbitMQ for workshop: {}", workshop.getName());
                } catch (Exception e) {
                        log.error("Failed to publish notification for workshop {} and student {}",
                                        workshop.getId(), student.getMssv(), e);
                }
        }

        private String qrImageBase64(String qrCode) {
                return Base64.getEncoder().encodeToString(generateQrPng(qrCode));
        }

        private String formatWorkshopTime(Workshop workshop) {
                return EMAIL_TIME_FORMATTER.format(workshop.getStartTime())
                                + " - "
                                + EMAIL_TIME_FORMATTER.format(workshop.getEndTime());
        }
}
