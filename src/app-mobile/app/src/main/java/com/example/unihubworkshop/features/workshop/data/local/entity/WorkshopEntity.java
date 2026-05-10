package com.example.unihubworkshop.features.workshop.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "workshops")
public class WorkshopEntity {
    @PrimaryKey
    @NonNull
    public String id = "";
    
    public String title;
    public String speaker;
    public int price;
    public String date;
    public String time;
    public String room;
    public String description;
    public String roomMapText;
    public boolean isOpen;
    public int registeredCount;
    public int capacity;

    public WorkshopEntity() {}
}
