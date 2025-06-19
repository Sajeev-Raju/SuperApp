package com.example.NearMeBKND.Business.model;

import lombok.Data;
import java.sql.Timestamp;

@Data
public class bsns_BusinessAnswer {
    private Integer answerId;
    private Integer questionId;
    private String userId;
    private String answerText;
    private Timestamp createdAt;
} 