package com.example.unihubworkshop.features.workshop.domain.entity;

import java.util.List;

public class WorkShop {
    private String id;
    private String title;
    private String author;
    private Integer price;
    private String date;
    private String time;
    private String address;
    private String description;
    private String roomNumber;
    private boolean isLive;
    private List<String> learningPoints;
    private int attendanceCount;
    private int maxAttendance;
    private String roomMapText;
    private boolean isRegistered;
    private String registrationId;

    public WorkShop(String id, String title, String author, Integer price, String date, String time, String address, String description, String roomNumber, boolean isLive, List<String> learningPoints, int attendanceCount, int maxAttendance, String roomMapText, boolean isRegistered, String registrationId) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.price = price;
        this.date = date;
        this.time = time;
        this.address = address;
        this.description = description;
        this.roomNumber = roomNumber;
        this.isLive = isLive;
        this.learningPoints = learningPoints;
        this.attendanceCount = attendanceCount;
        this.maxAttendance = maxAttendance;
        this.roomMapText = roomMapText;
        this.isRegistered = isRegistered;
        this.registrationId = registrationId;
    }

    // Getters and Setters
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public Integer getPrice() { return price; }
    public String getDate() { return date; }
    public String getTime() { return time; }
    public String getAddress() { return address; }
    public String getDescription() { return description; }
    public String getRoomNumber() { return roomNumber; }
    public boolean isLive() { return isLive; }
    public List<String> getLearningPoints() { return learningPoints; }
    public int getAttendanceCount() { return attendanceCount; }
    public void setAttendanceCount(int attendanceCount) { this.attendanceCount = attendanceCount; }
    public int getMaxAttendance() { return maxAttendance; }
    public String getRoomMapText() { return roomMapText; }
    public boolean isRegistered() { return isRegistered; }
    public void setRegistered(boolean registered) { isRegistered = registered; }
    public String getRegistrationId() { return registrationId; }
    public void setRegistrationId(String registrationId) { this.registrationId = registrationId; }

    public boolean isFree() {
        return price == null || price == 0;
    }

    public int getAttendancePercentage() {
        if (maxAttendance == 0) return 0;
        return (attendanceCount * 100) / maxAttendance;
    }
}

