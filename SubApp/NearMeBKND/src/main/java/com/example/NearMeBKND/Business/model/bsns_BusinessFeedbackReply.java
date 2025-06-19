package com.example.NearMeBKND.Business.model;

import lombok.Data;
import java.sql.Timestamp;

@Data
public class bsns_BusinessFeedbackReply {
    private Integer replyId;
    private Integer feedbackId;
    private String userId;
    private String replyText;
    private Timestamp createdAt;
} 