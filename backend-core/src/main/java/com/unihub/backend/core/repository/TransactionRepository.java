package com.unihub.backend.core.repository;

import com.unihub.backend.core.model.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    Optional<Transaction> findByIdempotencyKey(UUID idempotencyKey);
    Optional<Transaction> findByRegistrationId(UUID registrationId);
    Optional<Transaction> findByPgTransactionId(String pgTransactionId);
}
