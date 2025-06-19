package com.example.NearMeBKND.Business.model;

import lombok.Data;
import java.sql.Timestamp;

@Data
public class bsns_BusinessNotification {
    private Integer notificationId;
    private Integer businessId;
    private String userId;
    private String message;
    private Timestamp createdAt;
} 