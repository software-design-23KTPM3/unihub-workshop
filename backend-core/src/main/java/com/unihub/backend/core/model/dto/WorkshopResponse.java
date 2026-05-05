package com.unihub.backend.core.model.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class WorkshopResponse {
    private UUID id;
    private String title;
    private String description;
    private String speaker;
    private String speakerName;
    private String speakerTitle;
    private String topic;
    private String room;
    private String roomMapText;
    private String date;
    private String startTime;
    private String endTime;
    private Integer capacity;
    private Integer registeredCount;
    private BigDecimal price;
    private Boolean isPaid;
    private String status;
    private List<String> tags;
    private String aiSummary;
    private String organizerId;
    private Boolean isRegistered;
    private String registrationId;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSpeaker() { return speaker; }
    public void setSpeaker(String speaker) { this.speaker = speaker; }
    public String getSpeakerName() { return speakerName; }
    public void setSpeakerName(String speakerName) { this.speakerName = speakerName; }
    public String getSpeakerTitle() { return speakerTitle; }
    public void setSpeakerTitle(String speakerTitle) { this.speakerTitle = speakerTitle; }
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public String getRoom() { return room; }
    public void setRoom(String room) { this.room = room; }
    public String getRoomMapText() { return roomMapText; }
    public void setRoomMapText(String roomMapText) { this.roomMapText = roomMapText; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
    public Integer getRegisteredCount() { return registeredCount; }
    public void setRegisteredCount(Integer registeredCount) { this.registeredCount = registeredCount; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Boolean getIsPaid() { return isPaid; }
    public void setIsPaid(Boolean paid) { isPaid = paid; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public String getAiSummary() { return aiSummary; }
    public void setAiSummary(String aiSummary) { this.aiSummary = aiSummary; }
    public String getOrganizerId() { return organizerId; }
    public void setOrganizerId(String organizerId) { this.organizerId = organizerId; }
    public Boolean getIsRegistered() { return isRegistered; }
    public void setIsRegistered(Boolean registered) { isRegistered = registered; }
    public String getRegistrationId() { return registrationId; }
    public void setRegistrationId(String registrationId) { this.registrationId = registrationId; }

    public static WorkshopResponseBuilder builder() { return new WorkshopResponseBuilder(); }

    public static class WorkshopResponseBuilder {
        private final WorkshopResponse r = new WorkshopResponse();
        public WorkshopResponseBuilder id(UUID id) { r.id = id; return this; }
        public WorkshopResponseBuilder title(String title) { r.title = title; return this; }
        public WorkshopResponseBuilder description(String description) { r.description = description; return this; }
        public WorkshopResponseBuilder speaker(String speaker) { r.speaker = speaker; return this; }
        public WorkshopResponseBuilder speakerName(String speakerName) { r.speakerName = speakerName; return this; }
        public WorkshopResponseBuilder speakerTitle(String speakerTitle) { r.speakerTitle = speakerTitle; return this; }
        public WorkshopResponseBuilder topic(String topic) { r.topic = topic; return this; }
        public WorkshopResponseBuilder room(String room) { r.room = room; return this; }
        public WorkshopResponseBuilder roomMapText(String roomMapText) { r.roomMapText = roomMapText; return this; }
        public WorkshopResponseBuilder date(String date) { r.date = date; return this; }
        public WorkshopResponseBuilder startTime(String startTime) { r.startTime = startTime; return this; }
        public WorkshopResponseBuilder endTime(String endTime) { r.endTime = endTime; return this; }
        public WorkshopResponseBuilder capacity(Integer capacity) { r.capacity = capacity; return this; }
        public WorkshopResponseBuilder registeredCount(Integer registeredCount) { r.registeredCount = registeredCount; return this; }
        public WorkshopResponseBuilder price(BigDecimal price) { r.price = price; return this; }
        public WorkshopResponseBuilder isPaid(Boolean isPaid) { r.isPaid = isPaid; return this; }
        public WorkshopResponseBuilder status(String status) { r.status = status; return this; }
        public WorkshopResponseBuilder tags(List<String> tags) { r.tags = tags; return this; }
        public WorkshopResponseBuilder aiSummary(String aiSummary) { r.aiSummary = aiSummary; return this; }
        public WorkshopResponseBuilder organizerId(String organizerId) { r.organizerId = organizerId; return this; }
        public WorkshopResponseBuilder isRegistered(Boolean isRegistered) { r.isRegistered = isRegistered; return this; }
        public WorkshopResponseBuilder registrationId(String registrationId) { r.registrationId = registrationId; return this; }
        public WorkshopResponse build() { return r; }
    }
}
