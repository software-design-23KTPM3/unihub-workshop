package com.unihubworkshop.worker.entity;

import com.unihubworkshop.worker.enums.SummaryStatus;
import com.unihubworkshop.worker.enums.WorkshopStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "workshops")
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
    private String tags;

    @Column(name = "organizer_id")
    private String organizerId;

    @Column(name = "max_seats", nullable = false)
    private Integer maxSeats;

    @Column(name = "available_slots", nullable = false)
    private Integer availableSlots;

    @Column(name = "start_time", nullable = false)
    private ZonedDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private ZonedDateTime endTime;

    @Column(name = "is_paid")
    private Boolean isPaid;

    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    private WorkshopStatus status;

    @Column(name = "summary_text")
    private String summaryText;

    @Enumerated(EnumType.STRING)
    @Column(name = "summary_status")
    private SummaryStatus summaryStatus;

    @Column(name = "pdf_url")
    private String pdfUrl;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    public Workshop() {
    }

    public Workshop(UUID id, String name, String description, String speaker, String speakerTitle, String topic,
            String room, String roomMapText, String tags, String organizerId, Integer maxSeats, Integer availableSlots,
            ZonedDateTime startTime, ZonedDateTime endTime, Boolean isPaid, BigDecimal price, WorkshopStatus status,
            SummaryStatus summaryStatus) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.speaker = speaker;
        this.speakerTitle = speakerTitle;
        this.topic = topic;
        this.room = room;
        this.roomMapText = roomMapText;
        this.tags = tags;
        this.organizerId = organizerId;
        this.maxSeats = maxSeats;
        this.availableSlots = availableSlots;
        this.startTime = startTime;
        this.endTime = endTime;
        this.isPaid = isPaid;
        this.price = price;
        this.status = status;
        this.summaryStatus = summaryStatus;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSpeaker() {
        return speaker;
    }

    public void setSpeaker(String speaker) {
        this.speaker = speaker;
    }

    public String getSpeakerTitle() {
        return speakerTitle;
    }

    public void setSpeakerTitle(String speakerTitle) {
        this.speakerTitle = speakerTitle;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public String getRoomMapText() {
        return roomMapText;
    }

    public void setRoomMapText(String roomMapText) {
        this.roomMapText = roomMapText;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getOrganizerId() {
        return organizerId;
    }

    public void setOrganizerId(String organizerId) {
        this.organizerId = organizerId;
    }

    public Integer getMaxSeats() {
        return maxSeats;
    }

    public void setMaxSeats(Integer maxSeats) {
        this.maxSeats = maxSeats;
    }

    public Integer getAvailableSlots() {
        return availableSlots;
    }

    public void setAvailableSlots(Integer availableSlots) {
        this.availableSlots = availableSlots;
    }

    public ZonedDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(ZonedDateTime startTime) {
        this.startTime = startTime;
    }

    public ZonedDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(ZonedDateTime endTime) {
        this.endTime = endTime;
    }

    public Boolean getIsPaid() {
        return isPaid;
    }

    public void setIsPaid(Boolean isPaid) {
        this.isPaid = isPaid;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public WorkshopStatus getStatus() {
        return status;
    }

    public void setStatus(WorkshopStatus status) {
        this.status = status;
    }

    public String getSummaryText() {
        return summaryText;
    }

    public void setSummaryText(String summaryText) {
        this.summaryText = summaryText;
    }

    public SummaryStatus getSummaryStatus() {
        return summaryStatus;
    }

    public void setSummaryStatus(SummaryStatus summaryStatus) {
        this.summaryStatus = summaryStatus;
    }

    public String getPdfUrl() {
        return pdfUrl;
    }

    public void setPdfUrl(String pdfUrl) {
        this.pdfUrl = pdfUrl;
    }

    public static WorkshopBuilder builder() {
        return new WorkshopBuilder();
    }

    public static class WorkshopBuilder {
        private String name;
        private String description;
        private String speaker;
        private String speakerTitle;
        private String topic;
        private String room;
        private String roomMapText;
        private String tags;
        private String organizerId;
        private Integer maxSeats;
        private Integer availableSlots;
        private ZonedDateTime startTime;
        private ZonedDateTime endTime;
        private Boolean isPaid;
        private BigDecimal price;
        private WorkshopStatus status;
        private SummaryStatus summaryStatus;

        public WorkshopBuilder name(String name) {
            this.name = name;
            return this;
        }

        public WorkshopBuilder description(String description) {
            this.description = description;
            return this;
        }

        public WorkshopBuilder speaker(String speaker) {
            this.speaker = speaker;
            return this;
        }

        public WorkshopBuilder speakerTitle(String speakerTitle) {
            this.speakerTitle = speakerTitle;
            return this;
        }

        public WorkshopBuilder topic(String topic) {
            this.topic = topic;
            return this;
        }

        public WorkshopBuilder room(String room) {
            this.room = room;
            return this;
        }

        public WorkshopBuilder roomMapText(String roomMapText) {
            this.roomMapText = roomMapText;
            return this;
        }

        public WorkshopBuilder tags(String tags) {
            this.tags = tags;
            return this;
        }

        public WorkshopBuilder organizerId(String organizerId) {
            this.organizerId = organizerId;
            return this;
        }

        public WorkshopBuilder maxSeats(Integer maxSeats) {
            this.maxSeats = maxSeats;
            return this;
        }

        public WorkshopBuilder availableSlots(Integer availableSlots) {
            this.availableSlots = availableSlots;
            return this;
        }

        public WorkshopBuilder startTime(ZonedDateTime startTime) {
            this.startTime = startTime;
            return this;
        }

        public WorkshopBuilder endTime(ZonedDateTime endTime) {
            this.endTime = endTime;
            return this;
        }

        public WorkshopBuilder isPaid(Boolean isPaid) {
            this.isPaid = isPaid;
            return this;
        }

        public WorkshopBuilder price(BigDecimal price) {
            this.price = price;
            return this;
        }

        public WorkshopBuilder status(WorkshopStatus status) {
            this.status = status;
            return this;
        }

        public WorkshopBuilder summaryStatus(SummaryStatus summaryStatus) {
            this.summaryStatus = summaryStatus;
            return this;
        }

        public Workshop build() {
            return new Workshop(null, name, description, speaker, speakerTitle, topic, room, roomMapText, tags,
                    organizerId, maxSeats, availableSlots, startTime, endTime, isPaid, price, status, summaryStatus);
        }
    }
}
