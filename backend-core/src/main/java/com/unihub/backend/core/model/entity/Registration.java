package com.unihub.backend.core.model.entity;

import com.unihub.backend.core.model.enums.RegistrationStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "registrations")
public class Registration {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workshop_id")
    private Workshop workshop;

    @Enumerated(EnumType.STRING)
    private RegistrationStatus status;

    @Column(name = "qr_code", unique = true)
    private String qrCode;

    @Column(name = "idempotency_key", unique = true, nullable = false)
    private UUID idempotencyKey;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    @Column(name = "checked_in_at")
    private ZonedDateTime checkedInAt;

    public Registration() {}
    public Registration(UUID id, Student student, Workshop workshop, RegistrationStatus status, UUID idempotencyKey) {
        this.id = id;
        this.student = student;
        this.workshop = workshop;
        this.status = status;
        this.idempotencyKey = idempotencyKey;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public Workshop getWorkshop() { return workshop; }
    public void setWorkshop(Workshop workshop) { this.workshop = workshop; }
    public RegistrationStatus getStatus() { return status; }
    public void setStatus(RegistrationStatus status) { this.status = status; }
    public UUID getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(UUID idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public ZonedDateTime getCheckedInAt() { return checkedInAt; }
    public void setCheckedInAt(ZonedDateTime checkedInAt) { this.checkedInAt = checkedInAt; }

    public static RegistrationBuilder builder() { return new RegistrationBuilder(); }
    public static class RegistrationBuilder {
        private Student student;
        private Workshop workshop;
        private RegistrationStatus status;
        private UUID idempotencyKey;
        public RegistrationBuilder student(Student student) { this.student = student; return this; }
        public RegistrationBuilder workshop(Workshop workshop) { this.workshop = workshop; return this; }
        public RegistrationBuilder status(RegistrationStatus status) { this.status = status; return this; }
        public RegistrationBuilder idempotencyKey(UUID idempotencyKey) { this.idempotencyKey = idempotencyKey; return this; }
        public Registration build() { return new Registration(null, student, workshop, status, idempotencyKey); }
    }
}
