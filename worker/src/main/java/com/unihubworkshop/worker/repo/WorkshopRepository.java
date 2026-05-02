package com.unihubworkshop.worker.repo;

import com.unihubworkshop.worker.entity.Workshop;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface WorkshopRepository extends JpaRepository<Workshop, UUID> {
}
