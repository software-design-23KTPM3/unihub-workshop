package com.unihub.backend.core.model.dto;

import com.unihub.backend.core.model.enums.SummaryStatus;
import com.unihub.backend.core.model.enums.WorkshopStatus;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

public class WorkshopResponse {
    private UUID id;
    private String name;
    private String speaker;
    private String room;
    private Integer maxSeats;
    private Integer availableSlots;
    private ZonedDateTime startTime;
    private ZonedDateTime endTime;
    private Boolean isPaid;
    private BigDecimal price;
    private WorkshopStatus status;
    private String summaryText;
    private SummaryStatus summaryStatus;
    private String pdfUrl;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSpeaker() { return speaker; }
    public void setSpeaker(String speaker) { this.speaker = speaker; }
    public String getRoom() { return room; }
    public void setRoom(String room) { this.room = room; }
    public Integer getMaxSeats() { return maxSeats; }
    public void setMaxSeats(Integer maxSeats) { this.maxSeats = maxSeats; }
    public Integer getAvailableSlots() { return availableSlots; }
    public void setAvailableSlots(Integer availableSlots) { this.availableSlots = availableSlots; }
    public ZonedDateTime getStartTime() { return startTime; }
    public void setStartTime(ZonedDateTime startTime) { this.startTime = startTime; }
    public ZonedDateTime getEndTime() { return endTime; }
    public void setEndTime(ZonedDateTime endTime) { this.endTime = endTime; }
    public Boolean getIsPaid() { return isPaid; }
    public void setIsPaid(Boolean isPaid) { this.isPaid = isPaid; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public WorkshopStatus getStatus() { return status; }
    public void setStatus(WorkshopStatus status) { this.status = status; }
    public String getSummaryText() { return summaryText; }
    public void setSummaryText(String summaryText) { this.summaryText = summaryText; }
    public SummaryStatus getSummaryStatus() { return summaryStatus; }
    public void setSummaryStatus(SummaryStatus summaryStatus) { this.summaryStatus = summaryStatus; }
    public String getPdfUrl() { return pdfUrl; }
    public void setPdfUrl(String pdfUrl) { this.pdfUrl = pdfUrl; }

    public static WorkshopResponseBuilder builder() { return new WorkshopResponseBuilder(); }

    public static class WorkshopResponseBuilder {
        private WorkshopResponse r = new WorkshopResponse();
        public WorkshopResponseBuilder id(UUID id) { r.id = id; return this; }
        public WorkshopResponseBuilder name(String name) { r.name = name; return this; }
        public WorkshopResponseBuilder speaker(String speaker) { r.speaker = speaker; return this; }
        public WorkshopResponseBuilder room(String room) { r.room = room; return this; }
        public WorkshopResponseBuilder maxSeats(Integer maxSeats) { r.maxSeats = maxSeats; return this; }
        public WorkshopResponseBuilder availableSlots(Integer availableSlots) { r.availableSlots = availableSlots; return this; }
        public WorkshopResponseBuilder startTime(ZonedDateTime startTime) { r.startTime = startTime; return this; }
        public WorkshopResponseBuilder endTime(ZonedDateTime endTime) { r.endTime = endTime; return this; }
        public WorkshopResponseBuilder isPaid(Boolean isPaid) { r.isPaid = isPaid; return this; }
        public WorkshopResponseBuilder price(BigDecimal price) { r.price = price; return this; }
        public WorkshopResponseBuilder status(WorkshopStatus status) { r.status = status; return this; }
        public WorkshopResponseBuilder summaryText(String summaryText) { r.summaryText = summaryText; return this; }
        public WorkshopResponseBuilder summaryStatus(SummaryStatus summaryStatus) { r.summaryStatus = summaryStatus; return this; }
        public WorkshopResponseBuilder pdfUrl(String pdfUrl) { r.pdfUrl = pdfUrl; return this; }
        public WorkshopResponse build() { return r; }
    }
}
