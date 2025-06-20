package com.example.NearMeBKND.polls.model;

public class PollCollection {
    private int collectionId;
    private String collectionTitle;
    private String createdAt;
    private String userId;
    private double latitude;
    private double longitude;

    public int getCollectionId() { return collectionId; }
    public void setCollectionId(int collectionId) { this.collectionId = collectionId; }
    public String getCollectionTitle() { return collectionTitle; }
    public void setCollectionTitle(String collectionTitle) { this.collectionTitle = collectionTitle; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
} 