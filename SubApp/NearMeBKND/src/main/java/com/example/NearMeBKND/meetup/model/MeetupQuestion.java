package com.example.NearMeBKND.meetup.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MeetupQuestion {
    private Long id;
    private Long meetupId;
    private String userId;
    private String content;
    private LocalDateTime createdAt;
} 