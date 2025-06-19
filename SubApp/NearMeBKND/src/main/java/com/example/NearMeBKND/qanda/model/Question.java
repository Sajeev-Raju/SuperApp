package com.example.NearMeBKND.qanda.model;

import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class Question {
    private Long id;
    
    @NotNull(message = "userId cannot be null")
    private String userId;
    
    @NotBlank(message = "title cannot be blank")
    private String title;
    
    @NotBlank(message = "description cannot be blank")
    private String description;
    
    private String tags;  // Store tags as comma-separated values
    private List<String> notes;  // Changed from String to List<String>
    private String createdAt;
    @JsonIgnore
    private List<Tag> tagList;  // Keep this for backward compatibility
    @JsonIgnore
    private List<String> simpleTagList;  // New field for simplified tag list
    private List<Answer> answers;
    @JsonIgnore
    private String requestPath;
    private Double latitude;
    private Double longitude;
    // getters and setters
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public List<String> getNotes() {
        return notes;
    }

    public void setNotes(List<String> notes) {
        this.notes = notes;
    }

    public String getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
    public List<Tag> getTagList() {
        return tagList;
    }
    public void setTagList(List<Tag> tagList) {
        this.tagList = tagList;
    }
    public List<Answer> getAnswers() {
        return answers;
    }
    public void setAnswers(List<Answer> answers) {
        this.answers = answers;
    }
    public String getRequestPath() {
        return requestPath;
    }
    public void setRequestPath(String requestPath) {
        this.requestPath = requestPath;
    }
    public Double getLatitude() {
        return latitude;
    }
    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }
    public Double getLongitude() {
        return longitude;
    }
    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public List<String> getSimpleTagList() {
        if (tagList != null) {
            return tagList.stream()
                .map(Tag::getTagName)
                .toList();
        }
        return null;
    }

    public void setSimpleTagList(List<String> simpleTagList) {
        this.simpleTagList = simpleTagList;
    }
}

// // this is the test message for COPY 