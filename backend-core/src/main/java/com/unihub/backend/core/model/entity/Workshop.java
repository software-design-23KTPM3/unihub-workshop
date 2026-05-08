package com.unihub.backend.core.model.entity;

import com.unihub.backend.core.model.enums.SummaryStatus;
import com.unihub.backend.core.model.enums.WorkshopStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "workshops")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Workshop {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String description;
    private String speaker;
    @Column(name = "speaker_title")
    private String speakerTitle;
    private String topic;
    private String room;
    @Column(name = "room_map_text")
    private String roomMapText;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private List<String> tags;

    @Column(name = "organizer_id")
    private String organizerId;

    @Column(name = "max_seats", nullable = false)
    private Integer maxSeats;

    @Column(name = "available_slots", nullable = false)
    private Integer availableSlots;

    @Column(name = "registration_start_time", nullable = false)
    private ZonedDateTime registrationStartTime;

    @Column(name = "registration_end_time", nullable = false)
    private ZonedDateTime registrationEndTime;

    @Column(name = "start_time", nullable = false)
    private ZonedDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private ZonedDateTime endTime;

    @Column(name = "is_paid")
    private Boolean isPaid;

    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(columnDefinition = "workshop_status")
    private WorkshopStatus status;

    @Column(name = "summary_text")
    private String summaryText;

    @Enumerated(EnumType.STRING)
    @Column(name = "summary_status", columnDefinition = "summary_status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private SummaryStatus summaryStatus;

    @Column(name = "pdf_url")
    private String pdfUrl;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;
}
