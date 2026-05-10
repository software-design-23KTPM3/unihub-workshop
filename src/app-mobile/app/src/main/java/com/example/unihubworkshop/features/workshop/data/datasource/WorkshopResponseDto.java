package com.example.unihubworkshop.features.workshop.data.datasource;

import java.math.BigDecimal;
import java.util.List;

public class WorkshopResponseDto {
    private String id;
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

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getSpeaker() { return speaker; }
    public String getSpeakerName() { return speakerName; }
    public String getSpeakerTitle() { return speakerTitle; }
    public String getTopic() { return topic; }
    public String getRoom() { return room; }
    public String getRoomMapText() { return roomMapText; }
    public String getDate() { return date; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public Integer getCapacity() { return capacity; }
    public Integer getRegisteredCount() { return registeredCount; }
    public BigDecimal getPrice() { return price; }
    public Boolean getIsPaid() { return isPaid; }
    public String getStatus() { return status; }
    public List<String> getTags() { return tags; }
    public String getAiSummary() { return aiSummary; }
}
