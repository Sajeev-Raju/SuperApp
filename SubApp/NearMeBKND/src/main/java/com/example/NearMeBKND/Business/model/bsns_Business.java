package com.example.NearMeBKND.Business.model;

import lombok.Data;
import java.sql.Timestamp;

@Data
public class bsns_Business {
    private Integer businessId;
    private String userId;
    private String name;
    private String title;
    private String tags;
    private String description;
    private byte[] image;
    private String googlemapsURL;
    private Double longitude;
    private Double latitude;
    private String address;
    private String mobileNumber;
    private String timings;
    private Timestamp createdAt;
    private Boolean active;

    public Integer getBusinessId() {
        return businessId;
    }
    public java.sql.Timestamp getCreatedAt() {
        return createdAt;
    }
    public Boolean getActive() {
        return active;
    }
} 