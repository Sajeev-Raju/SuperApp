package com.example.NearMeBKND.polls.model;

import java.util.List;

public class PollQuestion {
    private int questionId;
    private String userId;
    private String questionText;
    private int selectionLimit;
    private String selectionMode;
    private double latitude;
    private double longitude;
    private String createdAt;
    private List<PollOption> options;

    // Getters and setters
    public int getQuestionId() { return questionId; }
    public void setQuestionId(int questionId) { this.questionId = questionId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
    public int getSelectionLimit() { return selectionLimit; }
    public void setSelectionLimit(int selectionLimit) { this.selectionLimit = selectionLimit; }
    public String getSelectionMode() { return selectionMode; }
    public void setSelectionMode(String selectionMode) { this.selectionMode = selectionMode; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public List<PollOption> getOptions() { return options; }
    public void setOptions(List<PollOption> options) { this.options = options; }
} 