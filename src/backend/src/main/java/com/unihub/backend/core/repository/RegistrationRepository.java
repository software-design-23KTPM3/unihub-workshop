package com.unihub.backend.core.repository;

import com.unihub.backend.core.model.entity.Registration;
import com.unihub.backend.core.model.enums.RegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface RegistrationRepository extends JpaRepository<Registration, UUID> {
    Optional<Registration> findByIdempotencyKey(UUID idempotencyKey);
    Optional<Registration> findByStudentMssvAndWorkshopId(String mssv, UUID workshopId);
    Optional<Registration> findByQrCode(String qrCode);
    java.util.List<Registration> findByStudentMssvOrderByCreatedAtDesc(String mssv);
    java.util.List<Registration> findByWorkshopId(UUID workshopId);
    boolean existsByStudentMssvAndWorkshopId(String mssv, UUID workshopId);
    java.util.List<Registration> findByStatusAndCreatedAtBefore(com.unihub.backend.core.model.enums.RegistrationStatus status, java.time.ZonedDateTime dateTime);
    long countByWorkshopIdAndStatusIn(UUID workshopId, Collection<RegistrationStatus> statuses);
}
