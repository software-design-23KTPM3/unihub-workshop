package com.unihub.backend.core.repository;

import com.unihub.backend.core.model.entity.Workshop;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkshopRepository extends JpaRepository<Workshop, UUID> {
   @Query("SELECT w.availableSlots FROM Workshop w WHERE w.id = :id")
   Optional<Integer> getAvailableSlots(@Param("id") UUID id);

   List<Workshop> findByOrganizerId(String organizerId);
}
