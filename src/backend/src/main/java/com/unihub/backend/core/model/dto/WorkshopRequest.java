package com.unihub.backend.core.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class WorkshopRequest {
    @NotBlank
    private String title;
    private String description;
    private String speaker;
    private String speakerName;
    private String speakerTitle;
    private String topic;
    private String room;
    private String roomMapText;
    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;
    @NotNull
    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;
    @NotNull
    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;
    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime registrationStartTime;
    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime registrationEndTime;
    @NotNull
    @Min(1)
    private Integer capacity;
    private BigDecimal price = BigDecimal.ZERO;
    private Boolean isPaid = false;
    private String status;
    private List<String> tags;
    private String aiSummary;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSpeaker() { return speaker != null ? speaker : speakerName; }
    public void setSpeaker(String speaker) { this.speaker = speaker; }
    public String getSpeakerName() { return speakerName != null ? speakerName : speaker; }
    public void setSpeakerName(String speakerName) { this.speakerName = speakerName; }
    public String getSpeakerTitle() { return speakerTitle; }
    public void setSpeakerTitle(String speakerTitle) { this.speakerTitle = speakerTitle; }
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public String getRoom() { return room; }
    public void setRoom(String room) { this.room = room; }
    public String getRoomMapText() { return roomMapText; }
    public void setRoomMapText(String roomMapText) { this.roomMapText = roomMapText; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public LocalDateTime getRegistrationStartTime() { return registrationStartTime; }
    public void setRegistrationStartTime(LocalDateTime registrationStartTime) { this.registrationStartTime = registrationStartTime; }
    public LocalDateTime getRegistrationEndTime() { return registrationEndTime; }
    public void setRegistrationEndTime(LocalDateTime registrationEndTime) { this.registrationEndTime = registrationEndTime; }
    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Boolean getIsPaid() { return isPaid; }
    public void setIsPaid(Boolean isPaid) { this.isPaid = isPaid; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public String getAiSummary() { return aiSummary; }
    public void setAiSummary(String aiSummary) { this.aiSummary = aiSummary; }
}
