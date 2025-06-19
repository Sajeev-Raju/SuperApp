package com.example.NearMeBKND.qanda.model;

import java.util.List;

public class QuestionResponse {
    private Long id;
    private String userId;
    private String title;
    private String description;
    private String tags;
    private String createdAt;
    private List<String> tagList;
    // Add other fields as needed (e.g., answers, requestPath)

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public List<String> getTagList() { return tagList; }
    public void setTagList(List<String> tagList) { this.tagList = tagList; }
    // Add other getters/setters as needed
} 