package com.example.NearMeBKND.classified.model;

public class ClfQuestion {
    private int id;
    private int classifiedId;
    private String content;
    private String userId;
    private String createdAt;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getClassifiedId() { return classifiedId; }
    public void setClassifiedId(int classifiedId) { this.classifiedId = classifiedId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
} 