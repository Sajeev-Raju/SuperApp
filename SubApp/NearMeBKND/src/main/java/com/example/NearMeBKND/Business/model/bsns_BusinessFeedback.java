package com.example.NearMeBKND.Business.model;

import lombok.Data;
import java.sql.Timestamp;

@Data
public class bsns_BusinessFeedback {
    private Integer feedbackId;
    private Integer businessId;
    private String userId;
    private String feedbackText;
    private Timestamp createdAt;
} 