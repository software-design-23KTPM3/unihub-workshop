package com.unihub.backend.core.repository;

import com.unihub.backend.core.model.entity.SyncLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface SyncLogRepository extends JpaRepository<SyncLog, UUID> {
}
