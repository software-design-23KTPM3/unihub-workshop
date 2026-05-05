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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RegistrationServiceImpl implements RegistrationService {

        private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RegistrationServiceImpl.class);

        private final RedisService redisService;
        private final RabbitTemplate rabbitTemplate;
        private final WorkshopRepository workshopRepository;
        private final StudentRepository studentRepository;
        private final RegistrationRepository registrationRepository;

        public RegistrationServiceImpl(
                        RedisService redisService,
                        RabbitTemplate rabbitTemplate,
                        WorkshopRepository workshopRepository,
                        StudentRepository studentRepository,
                        RegistrationRepository registrationRepository) {
                this.redisService = redisService;
                this.rabbitTemplate = rabbitTemplate;
                this.workshopRepository = workshopRepository;
                this.studentRepository = studentRepository;
                this.registrationRepository = registrationRepository;
        }

        @Override
        public RegistrationDetailResponse getRegistrationById(UUID id) {
                Registration reg = registrationRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Registration not found"));
                return mapToDetailResponse(reg);
        }

        private RegistrationDetailResponse mapToDetailResponse(Registration reg) {
                Workshop w = reg.getWorkshop();
                WorkshopResponse wResp = WorkshopResponse.builder()
                                .id(w.getId())
                                .title(w.getName())
                                .description(w.getDescription())
                                .speakerName(w.getSpeaker())
                                .room(w.getRoom())
                                .date(w.getStartTime().toLocalDate().toString())
                                .startTime(w.getStartTime().toLocalTime().toString())
                                .endTime(w.getEndTime().toLocalTime().toString())
                                .build();

                return RegistrationDetailResponse.builder()
                                .id(reg.getId())
                                .workshop(wResp)
                                .studentId(reg.getStudent().getMssv())
                                .studentName(reg.getStudent().getName())
                                .status(reg.getStatus())
                                .qrCode(reg.getQrCode())
                                .registeredAt(reg.getCreatedAt() != null ? reg.getCreatedAt().toString() : null)
                                .build();
        }

        @Override
        @Transactional
        public RegistrationResponse createRegistration(UUID idempotencyKey, RegistrationRequest request) {

                // 1. Check Redis Idempotency (Fast Path)
                if (!redisService.isUniqueRequest(idempotencyKey)) {
                        // If Redis says not unique, we should check DB if it's actually finished
                        return registrationRepository.findByIdempotencyKey(idempotencyKey)
                                        .map(reg -> RegistrationResponse.builder()
                                                        .registrationId(reg.getId())
                                                        .status(RegistrationStatus.SUCCESS)
                                                        .message("Registration already exists.")
                                                        .build())
                                        .orElse(RegistrationResponse.builder()
                                                        .status(RegistrationStatus.PENDING)
                                                        .message("Request is already being processed.")
                                                        .build());
                }

                boolean slotDeducted = false;
                try {
                        // 2. Redis Slot Check & Deduct (Throttle)
                        if (!redisService.deductWorkshopSlot(request.getWorkshopId())) {
                                redisService.removeIdempotencyKey(idempotencyKey);
                                return RegistrationResponse.builder()
                                                .status(RegistrationStatus.FAILED)
                                                .message("Workshop is sold out")
                                                .build();
                        }
                        slotDeducted = true;

                        // 3. Database Work (Source of Truth with Pessimistic Locking)
                        Workshop workshop = workshopRepository.findByIdWithLock(request.getWorkshopId())
                                        .orElseThrow(() -> new RuntimeException("Workshop not found"));

                        Student student = studentRepository.findById(request.getStudentId())
                                        .orElseThrow(() -> new RuntimeException("Student not found"));

                        // KIỂM TRA: Sinh viên đã đăng ký workshop này chưa?
                        if (registrationRepository.findByStudentMssvAndWorkshopId(student.getMssv(), workshop.getId())
                                        .isPresent()) {
                                redisService.removeIdempotencyKey(idempotencyKey);
                                return RegistrationResponse.builder()
                                                .status(RegistrationStatus.FAILED)
                                                .message("You are already registered for this workshop.")
                                                .build();
                        }

                        if (workshop.getAvailableSlots() <= 0) {
                                throw new RuntimeException("Workshop is sold out");
                        }

                        // TRỪ CHỖ TRỐNG: Quan trọng để cập nhật số lượng thực tế
                        workshop.setAvailableSlots(workshop.getAvailableSlots() - 1);
                        workshopRepository.save(workshop);

                        String qrData = "{\"studentId\":\"" + student.getMssv() + "\",\"workshopId\":\""
                                        + workshop.getId() + "\"}";

                        Registration registration = Registration.builder()
                                        .student(student)
                                        .workshop(workshop)
                                        .status(RegistrationStatus.SUCCESS)
                                        .idempotencyKey(idempotencyKey)
                                        .qrCode(qrData)
                                        .build();

                        // LƯU QUAN TRỌNG: Phải lưu vào DB và ép Flush để bắt lỗi ngay lập tức
                        registration = registrationRepository.save(registration);
                        registrationRepository.flush(); // Kích hoạt ngay lệnh INSERT/UPDATE để bắt
                                                        // DataIntegrity/OptimisticLock Exception tại đây

                        // 4. Register Transaction Synchronization for both Success and Failure
                        final Registration finalReg = registration; // Cần final để dùng trong inner class
                        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                                @Override
                                public void afterCompletion(int status) {
                                        if (status == STATUS_COMMITTED) {
                                                // Success: Send notification
                                                log.info("Transaction committed successfully. Sending notifications.");
                                                sendNotification(workshop, student, qrData);
                                        } else if (status == STATUS_ROLLED_BACK) {
                                                // Failure: Rollback Redis
                                                log.warn("Transaction rolled back. Rolling back Redis slots and idempotency key.");
                                                redisService.rollbackSlot(request.getWorkshopId());
                                                redisService.removeIdempotencyKey(idempotencyKey);
                                        }
                                }
                        });

                        log.info("Registration successful for student {} and workshop {}",
                                        request.getStudentId(), request.getWorkshopId());

                        return RegistrationResponse.builder()
                                        .registrationId(registration.getId())
                                        .status(RegistrationStatus.SUCCESS)
                                        .message("Registration successful. Notification will be sent shortly.")
                                        .build();

                } catch (Exception e) {
                        log.error("Error during registration processing: {}", e.getMessage());
                        // Rethrow to trigger transactional rollback (which triggers afterCompletion)
                        throw e;
                }
        }

        private void sendNotification(Workshop workshop, Student student, String qrData) {
                try {
                        String qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=300x300&data="
                                        + java.net.URLEncoder.encode(qrData, "UTF-8");

                        String htmlMsg = "<h2>Congratulations " + student.getName() + "!</h2>"
                                        + "<p>You have successfully registered for the workshop: <b>"
                                        + workshop.getName() + "</b></p>"
                                        + "<p><b>Time:</b> " + workshop.getStartTime() + "</p>"
                                        + "<p><b>Room:</b> " + workshop.getRoom() + "</p>"
                                        + "<p>Please use the QR code below to check-in:</p>"
                                        + "<img src='" + qrUrl + "' alt='QR Code' />";

                        NotificationData data = NotificationData.builder()
                                        .title("Registration Successful: " + workshop.getName())
                                        .msg(htmlMsg)
                                        .to(student.getEmail())
                                        .build();

                        NotificationRequest emailRequest = NotificationRequest.builder()
                                        .type("EMAIL")
                                        .data(data)
                                        .build();

                        rabbitTemplate.convertAndSend(
                                        com.unihub.backend.core.config.RabbitConfig.NOTIFICATION_EXCHANGE,
                                        com.unihub.backend.core.config.RabbitConfig.NOTIFICATION_ROUTING_KEY,
                                        emailRequest);

                        // APP Notification
                        NotificationData appData = NotificationData.builder()
                                        .title("Registration Successful: " + workshop.getName())
                                        .msg("You have successfully registered for " + workshop.getName()
                                                        + ". See you there!")
                                        .to(student.getFcmToken() != null ? student.getFcmToken() : student.getEmail())
                                        .build();

                        NotificationRequest appRequest = NotificationRequest.builder()
                                        .type("APP")
                                        .data(appData)
                                        .build();

                        rabbitTemplate.convertAndSend(
                                        com.unihub.backend.core.config.RabbitConfig.NOTIFICATION_EXCHANGE,
                                        com.unihub.backend.core.config.RabbitConfig.NOTIFICATION_ROUTING_KEY,
                                        appRequest);

                        log.info("Messages (EMAIL & APP) sent to RabbitMQ successfully after commit.");
                } catch (Exception e) {
                        log.error("Failed to construct or send notification", e);
                }
        }
}