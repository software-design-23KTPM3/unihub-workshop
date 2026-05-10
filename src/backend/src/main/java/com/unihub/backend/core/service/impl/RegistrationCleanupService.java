package com.unihub.backend.core.service.impl;

import com.unihub.backend.core.model.entity.Registration;
import com.unihub.backend.core.model.enums.RegistrationStatus;
import com.unihub.backend.core.repository.RegistrationRepository;
import com.unihub.backend.core.service.RedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;

@Service
public class RegistrationCleanupService {

    private static final Logger log = LoggerFactory.getLogger(RegistrationCleanupService.class);
    private final RegistrationRepository registrationRepository;
    private final RedisService redisService;

    public RegistrationCleanupService(RegistrationRepository registrationRepository, RedisService redisService) {
        this.registrationRepository = registrationRepository;
        this.redisService = redisService;
    }

    /**
     * Chạy mỗi phút để dọn dẹp các đơn hàng PENDING quá 30 phút.
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void cleanupExpiredRegistrations() {
        ZonedDateTime cutoffTime = ZonedDateTime.now().minusMinutes(30);
        List<Registration> expiredRegistrations = registrationRepository
                .findByStatusAndCreatedAtBefore(RegistrationStatus.PENDING, cutoffTime);

        if (!expiredRegistrations.isEmpty()) {
            log.info("Found {} expired registrations to cleanup", expiredRegistrations.size());

            for (Registration registration : expiredRegistrations) {
                try {
                    // 1. Cập nhật trạng thái trong DB thành CANCELLED
                    registration.setStatus(RegistrationStatus.FAILED);
                    registrationRepository.save(registration);

                    // 2. Hoàn trả slot và xóa khỏi danh sách trên Redis
                    redisService.rollbackRegistration(
                            registration.getWorkshop().getId(),
                            registration.getStudent().getMssv());

                    log.info("Successfully cancelled expired registration {} for student {}",
                            registration.getId(), registration.getStudent().getMssv());
                } catch (Exception e) {
                    log.error("Failed to cleanup registration {}: {}", registration.getId(), e.getMessage());
                }
            }
        }
    }
}
