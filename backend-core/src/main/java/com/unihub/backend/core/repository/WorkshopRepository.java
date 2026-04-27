package com.unihub.backend.core.repository;

import com.unihub.backend.core.model.entity.Workshop;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface WorkshopRepository extends JpaRepository<Workshop, UUID> {
}
