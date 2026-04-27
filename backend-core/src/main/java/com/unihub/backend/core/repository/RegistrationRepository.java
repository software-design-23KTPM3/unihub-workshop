package com.unihub.backend.core.repository;

import com.unihub.backend.core.model.entity.Registration;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface RegistrationRepository extends JpaRepository<Registration, UUID> {
    Optional<Registration> findByIdempotencyKey(UUID idempotencyKey);
}
