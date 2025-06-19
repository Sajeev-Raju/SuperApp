package com.example.NearMeBKND.meetup.model;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class MeetupDTO {
    private Long id;
    private String organizerName;
    private String title;
    private String tags;
    private String description;
    private LocalDate startDate;
    private LocalTime startTime;
    private LocalDate endDate;
    private LocalTime endTime;
    private String organizerId;
    private String eventAddress;
    private String imageUrl;
    private LocalDateTime createdAt;
    private Boolean isActive;
    private Integer maxParticipants;
    private String contactInfo;
    private String googleLocationURL;
    private List<String> notes;
    private List<MeetupQuestionDTO> questions;
} 