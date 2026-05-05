package com.unihub.backend.core.repository;

import com.unihub.backend.core.model.entity.Workshop;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

public interface WorkshopRepository extends JpaRepository<Workshop, UUID> {
   @Lock(LockModeType.PESSIMISTIC_WRITE)
   @Query("SELECT w FROM Workshop w WHERE w.id = :id")
   Optional<Workshop> findByIdWithLock(@Param("id") UUID id);

   @Query("SELECT w.availableSlots FROM Workshop w WHERE w.id = :id")
   Optional<Integer> getAvailableSlots(@Param("id") UUID id);

   List<Workshop> findByOrganizerId(String organizerId);
}
