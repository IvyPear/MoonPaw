package com.example.moonpaw.model;

public class Song {
    private String title;
    private String category;
    private String duration;

    private int resourceId;   // Dùng cho nhạc có sẵn (R.raw.xxx)
    private String filePath;  // Dùng cho nhạc tải thêm (đường dẫn file)
    private boolean isCustom; // Đánh dấu: True = Nhạc tải thêm

    // Constructor 1: Dành cho nhạc có sẵn
    public Song(String title, String category, String duration, int resourceId) {
        this.title = title;
        this.category = category;
        this.duration = duration;
        this.resourceId = resourceId;
        this.isCustom = false;
    }

    // Constructor 2: Dành cho nhạc tải từ bên ngoài
    public Song(String title, String filePath) {
        this.title = title;
        this.category = "Nhạc của tôi";
        this.duration = "∞";
        this.filePath = filePath;
        this.isCustom = true; // Đánh dấu là nhạc custom
        this.resourceId = 0;
    }

    public String getTitle() { return title; }
    public String getCategory() { return category; }
    public String getDuration() { return duration; }
    public int getResourceId() { return resourceId; }
    public String getFilePath() { return filePath; }
    public boolean isCustom() { return isCustom; }
}