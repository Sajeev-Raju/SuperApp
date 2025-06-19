package com.example.NearMeBKND.meetup.model;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.example.NearMeBKND.meetup.model.MeetupQuestion;
import com.example.NearMeBKND.meetup.model.MeetupQuestionDTO;

@Data
public class Meetup {
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
    private byte[] imageUrl;
    private Double latitude;
    private Double longitude;
    private LocalDateTime createdAt;
    private Boolean isActive;
    private Integer maxParticipants;
    private String contactInfo;
    private String googleLocationURL;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private java.util.List<String> notes;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private java.util.List<MeetupQuestionDTO> questions;

    public record UserLocation(Double latitude, Double longitude) {}
} 