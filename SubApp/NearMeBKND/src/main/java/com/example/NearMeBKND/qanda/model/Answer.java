package com.example.NearMeBKND.qanda.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class Answer {
    private Long id;
    
    @NotNull(message = "questionId cannot be null")
    @JsonIgnore
    private Long questionId;
    
    @NotNull(message = "userId cannot be null")
    private String userId;
    
    @NotBlank(message = "description cannot be blank")
    private String description;
    
    @JsonIgnore
    private String requestPath;
    
    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRequestPath() {
        return requestPath;
    }

    public void setRequestPath(String requestPath) {
        this.requestPath = requestPath;
    }
}







