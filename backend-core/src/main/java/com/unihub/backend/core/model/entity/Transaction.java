package com.unihub.backend.core.model.entity;

import com.unihub.backend.core.model.enums.TransactionStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registration_id")
    private Registration registration;
    @Column(nullable = false)
    private BigDecimal amount;
    @Enumerated(EnumType.STRING)
    private TransactionStatus status;
    @Column(name = "idempotency_key", unique = true, nullable = false)
    private UUID idempotencyKey;
    @Column(name = "pg_transaction_id")
    private String pgTransactionId;
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt;
    @UpdateTimestamp
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    public Transaction() {}
    public Transaction(Registration registration, BigDecimal amount, TransactionStatus status, UUID idempotencyKey) {
        this.registration = registration;
        this.amount = amount;
        this.status = status;
        this.idempotencyKey = idempotencyKey;
    }

    public UUID getId() { return id; }
    public Registration getRegistration() { return registration; }
    public void setRegistration(Registration registration) { this.registration = registration; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }
    public UUID getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(UUID idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getPgTransactionId() { return pgTransactionId; }
    public void setPgTransactionId(String pgTransactionId) { this.pgTransactionId = pgTransactionId; }

    public static TransactionBuilder builder() { return new TransactionBuilder(); }
    public static class TransactionBuilder {
        private Registration registration;
        private BigDecimal amount;
        private TransactionStatus status;
        private UUID idempotencyKey;
        public TransactionBuilder registration(Registration registration) { this.registration = registration; return this; }
        public TransactionBuilder amount(BigDecimal amount) { this.amount = amount; return this; }
        public TransactionBuilder status(TransactionStatus status) { this.status = status; return this; }
        public TransactionBuilder idempotencyKey(UUID idempotencyKey) { this.idempotencyKey = idempotencyKey; return this; }
        public Transaction build() { return new Transaction(registration, amount, status, idempotencyKey); }
    }
}
