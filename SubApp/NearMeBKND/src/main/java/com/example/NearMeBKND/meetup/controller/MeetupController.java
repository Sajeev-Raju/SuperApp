package com.example.NearMeBKND.meetup.controller;

import com.example.NearMeBKND.meetup.model.Meetup;
import com.example.NearMeBKND.meetup.model.MeetupDTO;
import com.example.NearMeBKND.meetup.model.MeetupNoteUpdate;
import com.example.NearMeBKND.meetup.service.MeetupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.NearMeBKND.util.GeoUtils;
import com.example.NearMeBKND.service.UserLocationService;
import com.example.NearMeBKND.util.QueryLogger;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;

@RestController
@RequestMapping("/api/meetups")
public class MeetupController {
    private static final Logger logger = LoggerFactory.getLogger(MeetupController.class);
    private final MeetupService meetupService;
    private final ObjectMapper objectMapper;
    private final UserLocationService userLocationService;

    @Autowired
    public MeetupController(MeetupService meetupService, ObjectMapper objectMapper, UserLocationService userLocationService) {
        this.meetupService = meetupService;
        this.objectMapper = objectMapper;
        this.userLocationService = userLocationService;
    }

    @GetMapping("/")
    public ResponseEntity<?> getAllMeetups(
            @RequestHeader("X-User-ID") String userId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "30") int size) {
        logger.debug("GET /api/meetups called by userId={}, page={}, size={}", userId, page, size);
        long requestStart = System.currentTimeMillis();
        QueryLogger.clear();
        try {
            int offset = page * size;
            logger.debug("Calculated offset: {} (page * size)", offset);
            int total = meetupService.countAllWithinRadius(userId);
            logger.debug("Total meetups within radius: {}", total);
            int totalPages = (int) Math.ceil((double) total / size);
            logger.debug("Total pages: {}", totalPages);
            java.util.List<MeetupDTO> pageContent = meetupService.findAllPaginated(size, offset, userId)
                .stream()
                .map(dto -> {
                    dto.setNotes(null);
                    dto.setQuestions(null);
                    return dto;
                })
                .collect(java.util.stream.Collectors.toList());
            logger.debug("Fetched {} meetups for this page.", pageContent.size());
            long requestEnd = System.currentTimeMillis();
            java.util.Map<String, Object> response = new java.util.LinkedHashMap<>();
            response.put("success", true);
            response.put("message", "Operation successful");
            response.put("page", page);
            response.put("size", size);
            response.put("count", pageContent.size());
            response.put("totalPages", totalPages);
            response.put("data", pageContent);
            response.put("queries", QueryLogger.getQueries());
            response.put("totalRequestTimeMs", requestEnd - requestStart);
            logger.debug("Returning response: {}", response);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(java.util.Map.of(
                "success", false,
                "message", e.getMessage(),
                "data", null
            ));
        } catch (Exception e) {
            logger.error("Error in getAllMeetups: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(java.util.Map.of(
                "success", false,
                "message", "Error: " + e.getMessage(),
                "data", null
            ));
        } finally {
            QueryLogger.clear();
        }
    }

    @GetMapping("/MyEvents")
    public ResponseEntity<List<MeetupDTO>> getMyEvents(
            @RequestHeader("X-User-ID") String userId) {
        logger.debug("GET /api/meetups/MyEvents called by userId={}", userId);
        try {
            List<MeetupDTO> dtos = meetupService.getMeetupsByOrganizer(userId, userId).stream()
                .map(this::toMeetupDTO)
                .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            logger.error("Error in getMyEvents: {}", e.getMessage(), e);
            throw e;
        }
    }

    @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createMeetupWithImage(
            @RequestPart("meetup") String meetupJson,
            @RequestPart("image") MultipartFile imageFile,
            @RequestHeader("X-User-ID") String userId) {
        try {
            Meetup meetup = objectMapper.readValue(meetupJson, Meetup.class);
            meetup.setOrganizerId(userId);
            meetup.setImageUrl(imageFile.getBytes());
            Meetup created = meetupService.createMeetup(meetup);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMeetup(
            @PathVariable Long id,
            @RequestHeader("X-User-ID") String userId) {
        logger.debug("DELETE /api/meetups/{} called by userId={}", id, userId);
        try {
        meetupService.deleteMeetup(id, userId);
        return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error in deleteMeetup: {}", e.getMessage(), e);
            throw e;
        }
    }

    @PatchMapping("/{id}/note")
    public ResponseEntity<Meetup> updateMeetupNote(
            @PathVariable Long id,
            @RequestBody MeetupNoteUpdate noteUpdate,
            @RequestHeader("X-User-ID") String userId) {
        logger.debug("PATCH /api/meetups/{}/note called by userId={}, noteUpdate={}", id, userId, noteUpdate);
        try {
            return ResponseEntity.ok(meetupService.updateMeetupNote(id, noteUpdate.getNote(), userId));
        } catch (Exception e) {
            logger.error("Error in updateMeetupNote: {}", e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchMeetupsByTagsAndRadius(
            @RequestHeader("X-User-ID") String userId,
            @RequestParam(value = "tags", required = false) String tagsParam) {
        List<String> tags = tagsParam == null || tagsParam.isBlank()
                ? List.of()
                : Arrays.asList(tagsParam.split(","));
        List<MeetupDTO> dtos = meetupService.searchMeetupsByTagsAndRadius(userId, tags).stream()
            .map(this::toMeetupDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> getMeetupImage(@PathVariable Long id) {
        byte[] image = meetupService.getImageById(id);
        if (image == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(image);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getMeetupById(
            @PathVariable Long id,
            @RequestHeader("X-User-ID") String userId) {
        // Fetch the meetup (with tags, notes, questions, etc.)
        var meetupOpt = meetupService.findByIdWithDetails(id);
        if (meetupOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Meetup not found"));
        }
        var meetup = meetupOpt.get();
        // Fetch user locations
        var userLocation = userLocationService.getUserLocation(userId);
        var organizerLocation = userLocationService.getUserLocation(meetup.getOrganizerId());
        if (userLocation == null || organizerLocation == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "User location not found for either requester or organizer"));
        }
        // Calculate distance
        double distance = GeoUtils.haversine(
            userLocation.getLatitude(), userLocation.getLongitude(),
            organizerLocation.getLatitude(), organizerLocation.getLongitude()
        );
        if (distance > 15.0) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "You are not within 15 km of the meetup organizer."));
        }
        // Build DTO
        MeetupDTO dto = meetupService.toMeetupDTO(meetup);
        return ResponseEntity.ok(dto);
    }

    private MeetupDTO toMeetupDTO(Meetup meetup) {
        MeetupDTO dto = new MeetupDTO();
        dto.setId(meetup.getId());
        dto.setOrganizerName(meetup.getOrganizerName());
        dto.setTitle(meetup.getTitle());
        dto.setTags(meetup.getTags());
        dto.setDescription(meetup.getDescription());
        dto.setStartDate(meetup.getStartDate());
        dto.setStartTime(meetup.getStartTime());
        dto.setEndDate(meetup.getEndDate());
        dto.setEndTime(meetup.getEndTime());
        dto.setOrganizerId(meetup.getOrganizerId());
        dto.setEventAddress(meetup.getEventAddress());
        dto.setImageUrl("/api/meetups/" + meetup.getId() + "/image");
        dto.setCreatedAt(meetup.getCreatedAt());
        dto.setIsActive(meetup.getIsActive());
        dto.setMaxParticipants(meetup.getMaxParticipants());
        dto.setContactInfo(meetup.getContactInfo());
        dto.setGoogleLocationURL(meetup.getGoogleLocationURL());
        dto.setNotes(meetup.getNotes());
        dto.setQuestions(meetup.getQuestions());
        return dto;
    }
} 