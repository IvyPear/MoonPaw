package com.example.moonpaw.model;

public class Song {
    private String title;
    private String category;
    private String duration;
    private int resourceId; // ID file nhạc trong res/raw

    public Song(String title, String category, String duration, int resourceId) {
        this.title = title;
        this.category = category;
        this.duration = duration;
        this.resourceId = resourceId;
    }

    public String getTitle() { return title; }
    public String getCategory() { return category; }
    public String getDuration() { return duration; }
    public int getResourceId() { return resourceId; }
}