package com.unihub.backend.core.repository;

import com.unihub.backend.core.model.entity.CheckinEventRecord;
import com.unihub.backend.core.model.enums.CheckinEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CheckinEventRecordRepository extends JpaRepository<CheckinEventRecord, UUID> {
    Optional<CheckinEventRecord> findByClientEventId(UUID clientEventId);
    boolean existsByRegistrationIdAndStatus(UUID registrationId, CheckinEventStatus status);
    List<CheckinEventRecord> findByWorkshopId(UUID workshopId);
}
