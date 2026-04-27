package com.unihub.backend.core.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "admin_id", nullable = false)
    private String adminId;

    @Column(nullable = false)
    private String action;

    @Column(name = "target_id")
    private String targetId;

    @CreationTimestamp
    private ZonedDateTime timestamp;

    @Column(name = "ip_address")
    private String ipAddress;
}
