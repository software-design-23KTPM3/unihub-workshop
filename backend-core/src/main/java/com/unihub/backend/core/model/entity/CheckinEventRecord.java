package com.unihub.backend.core.model.entity;

import com.unihub.backend.core.model.enums.CheckinEventStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "checkin_events",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_checkin_events_client_event", columnNames = "client_event_id"),
                @UniqueConstraint(name = "uq_checkin_events_registration_accepted", columnNames = {"registration_id", "status"})
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckinEventRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registration_id")
    private Registration registration;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workshop_id", nullable = false)
    private Workshop workshop;

    @Column(name = "qr_code", nullable = false)
    private String qrCode;

    @Column(name = "staff_id", nullable = false)
    private String staffId;

    @Column(name = "device_id")
    private String deviceId;

    @Column(name = "scanned_at", nullable = false)
    private ZonedDateTime scannedAt;

    @CreationTimestamp
    @Column(name = "synced_at", updatable = false)
    private ZonedDateTime syncedAt;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "checkin_event_status")
    private CheckinEventStatus status;

    @Column(name = "conflict_reason")
    private String conflictReason;

    @Column(name = "client_event_id")
    private UUID clientEventId;
}
