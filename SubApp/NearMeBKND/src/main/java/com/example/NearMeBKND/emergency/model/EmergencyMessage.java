package com.example.NearMeBKND.emergency.model;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

public class EmergencyMessage {
    private int id;
    private String title;
    private String description;
    private List<String> types;
    private Map<String, String> details;
    private String googleMapsLocation;
    private String userId;
    private String createdAt;
    private Double latitude;
    private Double longitude;
    private List<String> notes;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<String> getTypes() { return types; }
    public void setTypes(List<String> types) { this.types = types; }

    public Map<String, String> getDetails() { return details; }
    public void setDetails(Map<String, String> details) { this.details = details; }

    public String getGoogleMapsLocation() { return googleMapsLocation; }
    public void setGoogleMapsLocation(String googleMapsLocation) {
        // If the value is empty or null, set it to null
        if (googleMapsLocation == null || googleMapsLocation.trim().isEmpty()) {
            this.googleMapsLocation = null;
            return;
        }
        
        // Validate Google Maps URL
        String trimmedUrl = googleMapsLocation.trim();
        if (!isValidGoogleMapsUrl(trimmedUrl)) {
            throw new IllegalArgumentException("Invalid Google Maps URL format");
        }
        
        this.googleMapsLocation = trimmedUrl;
    }

    private boolean isValidGoogleMapsUrl(String url) {
        // Check if URL is null or empty
        if (url == null || url.trim().isEmpty()) {
            return true; // Allow null/empty values
        }

        // More lenient Google Maps URL validation
        String trimmedUrl = url.trim().toLowerCase();
        return trimmedUrl.matches("^https?://(www\\.)?(maps\\.google\\.[a-z]{2,3}|goo\\.gl/maps|maps\\.app\\.goo\\.gl|maps\\.googleapis\\.com)/.*$") ||
               trimmedUrl.matches("^https?://(www\\.)?google\\.[a-z]{2,3}/maps/.*$") ||
               trimmedUrl.matches("^https?://(www\\.)?maps\\.google\\.[a-z]{2,3}/.*$");
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public List<String> getNotes() { return notes; }
    public void setNotes(List<String> notes) { this.notes = notes; }

    public String getDetailsAsJson() {
        try {
            return objectMapper.writeValueAsString(details);
        } catch (Exception e) {
            return "{}";
        }
    }
} 



















// package com.example.emergencymessage.model;

// import java.util.List;
// import java.util.Map;

// public class EmergencyMessage {
//     private int id;
//     private String title;
//     private String description;
//     private List<String> types;
//     private Map<String, String> details;
//     private String googleMapsLocation;

//     // Getters and Setters
//     public int getId() { return id; }
//     public void setId(int id) { this.id = id; }

//     public String getTitle() { return title; }
//     public void setTitle(String title) { this.title = title; }

//     public String getDescription() { return description; }
//     public void setDescription(String description) { this.description = description; }

//     public List<String> getTypes() { return types; }
//     public void setTypes(List<String> types) { this.types = types; }

//     public Map<String, String> getDetails() { return details; }
//     public void setDetails(Map<String, String> details) { this.details = details; }

//     public String getGoogleMapsLocation() { return googleMapsLocation; }
//     public void setGoogleMapsLocation(String googleMapsLocation) { this.googleMapsLocation = googleMapsLocation; }
// }
