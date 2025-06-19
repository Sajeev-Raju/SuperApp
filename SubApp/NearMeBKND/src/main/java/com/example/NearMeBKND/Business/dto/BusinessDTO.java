package com.example.NearMeBKND.Business.dto;

public class BusinessDTO {
    private Integer businessId;
    private String name;
    private String title;
    private String description;
    private String address;
    private String mobileNumber;
    private String timings;
    private String googlemapsURL;
    private String image;

    // Getters and setters
    public Integer getBusinessId() { return businessId; }
    public void setBusinessId(Integer businessId) { this.businessId = businessId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getMobileNumber() { return mobileNumber; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }
    public String getTimings() { return timings; }
    public void setTimings(String timings) { this.timings = timings; }
    public String getGooglemapsURL() { return googlemapsURL; }
    public void setGooglemapsURL(String googlemapsURL) { this.googlemapsURL = googlemapsURL; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
} 