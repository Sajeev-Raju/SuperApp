package com.example.NearMeBKND.meetup.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MeetupQuestionDTO {
    private String userId;
    private String content;
    private LocalDateTime createdAt;
} 