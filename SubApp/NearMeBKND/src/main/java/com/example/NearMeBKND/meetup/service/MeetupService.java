package com.example.NearMeBKND.meetup.service;

import com.example.NearMeBKND.meetup.model.Meetup;
import com.example.NearMeBKND.meetup.repository.MeetupRepository;
import com.example.NearMeBKND.util.GeoUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.HttpURLConnection;
import java.net.URL;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.Arrays;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.NearMeBKND.util.GoogleMapsUrlProcessor;
import com.example.NearMeBKND.meetup.model.MeetupDTO;

@Service
public class MeetupService {
    private static final Logger logger = LoggerFactory.getLogger(MeetupService.class);
    private final MeetupRepository meetupRepository;
    private final JdbcTemplate jdbcTemplate;
    private static final double ACCESS_RADIUS_KM = 15.0;

    @Value("${google.api.key}")
    private String googleApiKey;
    @Value("${google.geocoding.api.url}")
    private String geocodingApiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private GoogleMapsUrlProcessor googleMapsUrlProcessor;

    public MeetupService(MeetupRepository meetupRepository, 
                        JdbcTemplate jdbcTemplate) {
        this.meetupRepository = meetupRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean validateUser(String userId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM user_locations WHERE user_id = ?",
            Integer.class,
            userId
        );
        return count != null && count > 0;
    }

    public Meetup.UserLocation getUserLocation(String userId) {
        return jdbcTemplate.queryForObject(
            "SELECT latitude, longitude FROM user_locations WHERE user_id = ?",
            (rs, rowNum) -> new Meetup.UserLocation(
                rs.getDouble("latitude"),
                rs.getDouble("longitude")
            ),
            userId
        );
    }

    public boolean isWithinAccessRadius(double userLat, double userLon, double eventLat, double eventLon) {
        double distance = GeoUtils.haversine(userLat, userLon, eventLat, eventLon);
        return distance <= ACCESS_RADIUS_KM;
    }

    private String getDistanceMessage(double userLat, double userLon, double eventLat, double eventLon) {
        double distance = GeoUtils.haversine(userLat, userLon, eventLat, eventLon);
        return String.format("You are %.2f km away from this meetup. The maximum allowed distance is %.2f km.", 
            distance, ACCESS_RADIUS_KM);
    }

    public List<Meetup> getAllMeetups(String userId) {
        logger.debug("getAllMeetups called by userId={}", userId);
        try {
        if (!validateUser(userId)) {
            throw new IllegalArgumentException("Invalid user ID");
        }
        Meetup.UserLocation userLocation = getUserLocation(userId);
        List<Meetup> allMeetups = meetupRepository.findAll();
        return allMeetups.stream()
            .filter(meetup -> 
                meetup.getOrganizerId().equals(userId) || 
                isWithinAccessRadius(
                    userLocation.latitude(), 
                    userLocation.longitude(),
                    meetup.getLatitude(),
                    meetup.getLongitude()
                )
            )
            .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error in getAllMeetups: {}", e.getMessage(), e);
            throw e;
        }
    }

    public List<Meetup> getMeetupsByOrganizer(String organizerId, String userId) {
        logger.debug("getMeetupsByOrganizer called for organizerId={}, userId={}", organizerId, userId);
        try {
        if (!validateUser(userId)) {
            throw new IllegalArgumentException("Invalid user ID");
        }
        Meetup.UserLocation userLocation = getUserLocation(userId);
        List<Meetup> organizerMeetups = meetupRepository.findByOrganizerId(organizerId);
        if (organizerId.equals(userId)) {
            return organizerMeetups;
    }
        return organizerMeetups.stream()
            .filter(meetup -> isWithinAccessRadius(
                userLocation.latitude(),
                userLocation.longitude(),
                meetup.getLatitude(),
                meetup.getLongitude()
            ))
            .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error in getMeetupsByOrganizer: {}", e.getMessage(), e);
            throw e;
        }
    }

    private GoogleMapsUrlProcessor.Coordinates extractCoordinates(String googleMapsUrl) {
        logger.debug("Extracting coordinates from URL: {}", googleMapsUrl);
        try {
            return googleMapsUrlProcessor.extractCoordinatesFromUrl(googleMapsUrl);
        } catch (Exception e) {
            logger.error("Failed to extract coordinates: {}", e.getMessage(), e);
            throw new IllegalArgumentException("Failed to extract coordinates from Google Maps URL: " + e.getMessage());
        }
    }

    @Transactional
    public Meetup createMeetup(Meetup meetup) {
        logger.debug("createMeetup called for meetup: {}", meetup);
        try {
            // Validate user exists
        if (!validateUser(meetup.getOrganizerId())) {
            throw new IllegalArgumentException("Invalid organizer ID");
        }
            // Extract coordinates from Google Maps URL
            if (meetup.getGoogleLocationURL() == null || meetup.getGoogleLocationURL().isEmpty()) {
                throw new IllegalArgumentException("Google Maps URL is required");
            }
            // Save the original googleLocationURL (already set in the meetup object)
            GoogleMapsUrlProcessor.Coordinates coords = extractCoordinates(meetup.getGoogleLocationURL());
            meetup.setLatitude(coords.latitude());
            meetup.setLongitude(coords.longitude());
            // Validate all other fields
        validateMeetup(meetup);
            // Set default values
        meetup.setCreatedAt(LocalDateTime.now());
        meetup.setIsActive(true);
            // Save meetup and return
        return meetupRepository.save(meetup);
        } catch (Exception e) {
            logger.error("Error in createMeetup: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public void deleteMeetup(Long id, String userId) {
        logger.debug("deleteMeetup called for id={}, userId={}", id, userId);
        try {
        if (!validateUser(userId)) {
            throw new IllegalArgumentException("Invalid user ID");
        }
        Meetup meetup = meetupRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Meetup not found"));
        if (!meetup.getOrganizerId().equals(userId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You are not authorized to delete this meetup"
            );
        }
        meetupRepository.delete(id);
        } catch (Exception e) {
            logger.error("Error in deleteMeetup: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void validateMeetup(Meetup meetup) {
        if (meetup.getTitle() == null || meetup.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Title is required");
        }
        if (meetup.getDescription() == null || meetup.getDescription().trim().isEmpty()) {
            throw new IllegalArgumentException("Description is required");
        }
        if (meetup.getStartDate() == null) {
            throw new IllegalArgumentException("Start date is required");
        }
        if (meetup.getStartTime() == null) {
            throw new IllegalArgumentException("Start time is required");
        }
        if (meetup.getEndDate() == null) {
            throw new IllegalArgumentException("End date is required");
        }
        if (meetup.getEndTime() == null) {
            throw new IllegalArgumentException("End time is required");
        }
        LocalDateTime startDateTime = meetup.getStartDate().atTime(meetup.getStartTime());
        LocalDateTime endDateTime = meetup.getEndDate().atTime(meetup.getEndTime());
        if (startDateTime.isAfter(endDateTime)) {
            throw new IllegalArgumentException("Start date/time must be before end date/time");
        }
        if (startDateTime.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Start date/time must be in the future");
        }
        if (meetup.getOrganizerName() == null || meetup.getOrganizerName().trim().isEmpty()) {
            throw new IllegalArgumentException("Organizer name is required");
        }
        if (meetup.getEventAddress() == null || meetup.getEventAddress().trim().isEmpty()) {
            throw new IllegalArgumentException("Event address is required");
        }
        if (meetup.getLatitude() == null || meetup.getLongitude() == null) {
            throw new IllegalArgumentException("Latitude and longitude are required");
        }
        if (meetup.getMaxParticipants() != null && meetup.getMaxParticipants() < 1) {
            throw new IllegalArgumentException("Maximum participants must be at least 1");
        }
        if (meetup.getContactInfo() != null && meetup.getContactInfo().trim().isEmpty()) {
            throw new IllegalArgumentException("Contact info cannot be empty if provided");
        }
    }

    @Transactional
    public Meetup updateMeetupNote(Long id, String note, String userId) {
        logger.debug("updateMeetupNote called for id={}, userId={}, note={} ", id, userId, note);
        try {
            if (!validateUser(userId)) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid user ID: User not found in the system"
                );
            }
            // Validate note
            if (note != null && note.length() > 1000) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Note cannot exceed 1000 characters"
                );
            }
            Meetup meetup = meetupRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Meetup not found with ID: " + id
                ));
            if (!meetup.getOrganizerId().equals(userId)) {
                throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not authorized to update notes for this meetup. Only the organizer can add/edit notes."
                );
            }
            // Enforce max 3 note updates using mtp_note
            int noteUpdates = meetupRepository.countNotesInMtpNote(id);
            if (noteUpdates >= 3) {
                throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You cannot update the note more than three times for this meetup."
                );
            }
            // Insert a record into mtp_note only
            meetupRepository.saveMeetupNote(id, note);
            return meetupRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to retrieve updated meetup"
                ));
        } catch (Exception e) {
            logger.error("Error in updateMeetupNote: {}", e.getMessage(), e);
            throw e;
        }
    }

    public List<Meetup> searchMeetupsByTagsAndRadius(String userId, List<String> tags) {
        logger.debug("searchMeetupsByTagsAndRadius called for userId={}, tags={}", userId, tags);
        if (!validateUser(userId)) {
            throw new IllegalArgumentException("Invalid user ID");
        }
        Meetup.UserLocation userLocation = getUserLocation(userId);
        // Fetch meetups that have any of the tags
        List<Meetup> taggedMeetups = meetupRepository.findMeetupsByTags(tags);
        // Filter by radius
        return taggedMeetups.stream()
            .filter(meetup ->
                meetup.getOrganizerId().equals(userId) ||
                isWithinAccessRadius(
                    userLocation.latitude(),
                    userLocation.longitude(),
                    meetup.getLatitude(),
                    meetup.getLongitude()
                )
            )
            .collect(Collectors.toList());
    }

    public byte[] getImageById(Long id) {
        return meetupRepository.findById(id)
            .map(Meetup::getImageUrl)
            .orElse(null);
    }

    public List<MeetupDTO> findAllPaginated(int limit, int offset, String userId) {
        if (!validateUser(userId)) {
            throw new IllegalArgumentException("Invalid user ID");
        }
        Meetup.UserLocation userLocation = getUserLocation(userId);
        List<Meetup> meetups = meetupRepository.findMeetupsInRadiusPaged(
            userLocation.latitude(), userLocation.longitude(), ACCESS_RADIUS_KM, limit, offset
        );
        // Map to DTOs without fetching notes/questions for each meetup
        return meetups.stream().map(m -> {
            MeetupDTO dto = new MeetupDTO();
            dto.setId(m.getId());
            dto.setOrganizerName(m.getOrganizerName());
            dto.setTitle(m.getTitle());
            dto.setTags(m.getTags());
            dto.setDescription(m.getDescription());
            dto.setStartDate(m.getStartDate());
            dto.setStartTime(m.getStartTime());
            dto.setEndDate(m.getEndDate());
            dto.setEndTime(m.getEndTime());
            dto.setOrganizerId(m.getOrganizerId());
            dto.setEventAddress(m.getEventAddress());
            dto.setImageUrl("/api/meetups/" + m.getId() + "/image");
            dto.setCreatedAt(m.getCreatedAt());
            dto.setIsActive(m.getIsActive());
            dto.setMaxParticipants(m.getMaxParticipants());
            dto.setContactInfo(m.getContactInfo());
            dto.setGoogleLocationURL(m.getGoogleLocationURL());
            dto.setNotes(null);
            dto.setQuestions(null);
            return dto;
        }).collect(Collectors.toList());
    }

    public int countAllActiveMeetups() {
        return meetupRepository.countAllActiveMeetups();
    }

    public int countAllWithinRadius(String userId) {
        if (!validateUser(userId)) {
            throw new IllegalArgumentException("Invalid user ID");
        }
        Meetup.UserLocation userLocation = getUserLocation(userId);
        return meetupRepository.countMeetupsInRadius(
            userLocation.latitude(), userLocation.longitude(), ACCESS_RADIUS_KM
        );
    }

    // Make this method public so it can be used in streams
    public MeetupDTO toMeetupDTO(Meetup meetup) {
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

    public java.util.Optional<Meetup> findByIdWithDetails(Long id) {
        return meetupRepository.findById(id);
    }
} 