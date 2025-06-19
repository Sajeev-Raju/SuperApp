package com.example.NearMeBKND.classified.model;

import java.util.List;
import java.util.Map;

public class ClassifiedDTO {
    private int id;
    private String title;
    private String description;
    private int price;
    private String userId;
    private String createdAt;
    private List<String> categories;
    private List<?> questions;
    private String image;

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = price; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public List<String> getCategories() { return categories; }
    public void setCategories(List<String> categories) { this.categories = categories; }
    public List<?> getQuestions() { return questions; }
    public void setQuestions(List<?> questions) { this.questions = questions; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
} 