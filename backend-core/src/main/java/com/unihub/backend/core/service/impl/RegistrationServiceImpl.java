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
        public RegistrationResponse createRegistration(UUID idempotencyKeyFromHeader, RegistrationRequest request) {
                // Lưu ý: idempotencyKeyFromHeader bị bỏ qua theo yêu cầu "gen dưới backend"
                // Tuy nhiên ta có thể dùng userId + workshopId làm key tự nhiên trong Redis
                
                String studentMssv = request.getStudentId();
                UUID workshopId = request.getWorkshopId();
                UUID registrationId = UUID.randomUUID(); // Backend tự gen ID

                // 1. Kiểm tra và đăng ký trên Redis (Atomic: Check duplicate + Deduct slot)
                if (!redisService.registerUserInRedis(workshopId, studentMssv)) {
                        // Nếu thất bại, có thể là do hết chỗ hoặc đã đăng ký rồi
                        if (redisService.isUserRegisteredInRedis(workshopId, studentMssv)) {
                                return RegistrationResponse.builder()
                                                .status(RegistrationStatus.FAILED)
                                                .message("You have already registered for this workshop")
                                                .build();
                        }
                        return RegistrationResponse.builder()
                                        .status(RegistrationStatus.FAILED)
                                        .message("Workshop is sold out or unavailable")
                                        .build();
                }

                try {
                        // 2. Lấy thông tin Workshop (để biết isPaid)
                        Workshop workshop = workshopRepository.findById(workshopId)
                                        .orElseThrow(() -> new RuntimeException("Workshop not found"));
                        Student student = studentRepository.findById(studentMssv)
                                        .orElseThrow(() -> new RuntimeException("Student not found"));

                        RegistrationStatus initialStatus = workshop.getIsPaid() ? RegistrationStatus.PENDING : RegistrationStatus.SUCCESS;

                        // 3. Gọi Async lưu DB
                        asyncDbService.saveRegistrationAsync(workshop, student, registrationId, initialStatus);

                        log.info("Registration initiated in Redis for student {} and workshop {}. Async DB save triggered.",
                                        studentMssv, workshopId);

                        // 4. Trả về ngay lập tức
                        return RegistrationResponse.builder()
                                        .registrationId(registrationId)
                                        .status(initialStatus)
                                        .message(initialStatus == RegistrationStatus.SUCCESS 
                                                ? "Registration successful." 
                                                : "Registration initiated. Please complete payment.")
                                        .build();

                } catch (Exception e) {
                        log.error("Error during registration process: {}", e.getMessage());
                        // Rollback Redis nếu có lỗi nghiêm trọng khi chuẩn bị dữ liệu
                        redisService.rollbackSlot(workshopId);
                        // Cần thêm logic remove user khỏi set trong Redis nếu rollback
                        throw new RuntimeException("Failed to initiate registration", e);
                }
        }

        @Override
        public java.util.List<RegistrationDetailResponse> getMyRegistrations(org.springframework.security.core.Authentication authentication) {
                String mssv = authentication.getName();
                return registrationRepository.findByStudentMssvOrderByCreatedAtDesc(mssv).stream()
                                .map(this::mapToDetailResponse)
                                .collect(java.util.stream.Collectors.toList());
        }

        @Override
        public java.util.List<RegistrationDetailResponse> getAllRegistrations(java.util.Map<String, String> filters) {
                java.util.List<Registration> registrations;
                if (filters.containsKey("workshopId")) {
                        registrations = registrationRepository.findByWorkshopId(UUID.fromString(filters.get("workshopId")));
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