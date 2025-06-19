package com.example.NearMeBKND.Business.model;

import lombok.Data;
import java.sql.Timestamp;

@Data
public class bsns_BusinessQuestion {
    private Integer questionId;
    private Integer businessId;
    private String userId;
    private String questionText;
    private Timestamp createdAt;
} 