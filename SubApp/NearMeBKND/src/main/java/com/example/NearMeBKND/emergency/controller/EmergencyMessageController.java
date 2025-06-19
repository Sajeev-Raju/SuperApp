package com.example.NearMeBKND.emergency.controller;

import com.example.NearMeBKND.emergency.model.EmergencyMessage;
import com.example.NearMeBKND.emergency.repository.EmergencyMessageRepository;
import com.example.NearMeBKND.emergency.repository.EmergencyQuestionRepository;
import com.example.NearMeBKND.service.UserLocationService;
import com.example.NearMeBKND.util.GeoUtils;
import com.example.NearMeBKND.nearme.model.UserLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/emergency_messages")
public class EmergencyMessageController {

    private final EmergencyMessageRepository repository;
    @Autowired
    private UserLocationService userLocationService;
    @Autowired
    private EmergencyQuestionRepository emergencyQuestionRepository;
    private static final Logger logger = LoggerFactory.getLogger(EmergencyMessageController.class);

    public EmergencyMessageController(EmergencyMessageRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createMessage(@RequestBody EmergencyMessage message, @RequestHeader("X-User-ID") String userId) {
        try {
            logger.info("Received emergency message request: {}", message);
            message.setUserId(userId);
            UserLocation userLocation = userLocationService.getUserLocation(userId);
            if (userLocation != null) {
                message.setLatitude(userLocation.getLatitude());
                message.setLongitude(userLocation.getLongitude());
            }
            EmergencyMessage savedMessage = repository.save(message);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Emergency message saved successfully.");
            response.put("created_at", savedMessage.getCreatedAt());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid request data: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            logger.error("Failed to save emergency message", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to save emergency message: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/")
    public ResponseEntity<?> getAllMessages(
            @RequestHeader("X-User-ID") String currentUserId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        try {
            UserLocation currentUserLoc = userLocationService.getUserLocation(currentUserId);
            double userLat = currentUserLoc.getLatitude();
            double userLon = currentUserLoc.getLongitude();
            double radiusKm = 15.0;
            int offset = page * size;

            int total = repository.countEmergencyMessagesInRadius(userLat, userLon, radiusKm);
            List<EmergencyMessage> messages = repository.getEmergencyMessagesInRadiusPaged(userLat, userLon, radiusKm, size, offset);

            // Optionally map to DTO for lightweight response (here, returning as is)
            Map<String, Object> response = new HashMap<>();
            response.put("data", messages);
            response.put("currentPage", page);
            response.put("size", size);
            response.put("count", messages.size());
            response.put("totalPages", (int) Math.ceil((double) total / size));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to fetch emergency messages", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch emergency messages: " + e.getMessage()));
        }
    }

    @GetMapping("/filter")
    public ResponseEntity<List<EmergencyMessage>> getMessagesByTypes(
        @RequestParam("types") String types,
        @RequestHeader("X-User-ID") String currentUserId
    ) {
        List<String> typeList = Arrays.asList(types.split(","));
        List<EmergencyMessage> allMessages = repository.findByTypes(typeList);
        UserLocation currentUserLoc = userLocationService.getUserLocation(currentUserId);
        // Filter messages by 15 km radius
        List<EmergencyMessage> filtered = allMessages.stream().filter(msg -> {
            UserLocation ownerLoc = userLocationService.getUserLocation(msg.getUserId());
            double distance = GeoUtils.haversine(
                currentUserLoc.getLatitude(), currentUserLoc.getLongitude(),
                ownerLoc.getLatitude(), ownerLoc.getLongitude()
            );
            return distance <= 15.0;
        }).toList();
        return ResponseEntity.ok(filtered);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMessage(@PathVariable int id, @RequestAttribute("userId") String userId) {
        logger.info("User '{}' is attempting to delete emergency message '{}'.", userId, id);
        EmergencyMessage message = repository.findById(id);
        if (message == null) {
            logger.warn("User '{}' tried to delete non-existent emergency message '{}'.", userId, id);
            return ResponseEntity.status(404).body(Map.of("message", "Emergency message not found."));
        }
        if (!userId.equals(message.getUserId())) {
            logger.warn("User '{}' is not the owner of emergency message '{}'. Deletion forbidden.", userId, id);
            return ResponseEntity.status(403).body(Map.of("message", "You are not authorized to delete this emergency message."));
        }
        repository.deleteById(id);
        logger.info("User '{}' deleted their emergency message '{}'.", userId, id);
        return ResponseEntity.ok(Map.of("message", "Emergency message deleted successfully."));
    }

    @PostMapping("/{id}/notes")
    public ResponseEntity<Map<String, Object>> addNote(
            @PathVariable int id,
            @RequestBody Map<String, String> request,
            @RequestAttribute("userId") String userId) {
        try {
            EmergencyMessage message = repository.findById(id);
            if (message == null) {
                return ResponseEntity.status(404)
                    .body(Map.of("error", "Emergency message not found"));
            }

            if (!message.getUserId().equals(userId)) {
                return ResponseEntity.status(403)
                    .body(Map.of("error", "Not authorized to add notes to this message"));
            }

            String note = request.get("note");
            if (note == null || note.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Note cannot be empty"));
            }

            repository.addNote(id, note);
            return ResponseEntity.ok(Map.of(
                "message", "Note added successfully",
                "emergencyId", id
            ));
        } catch (Exception e) {
            logger.error("Error adding note to emergency message", e);
            return ResponseEntity.status(500)
                .body(Map.of("error", "Failed to add note: " + e.getMessage()));
        }
    }

    @GetMapping("/{emergency_message_id}")
    public ResponseEntity<?> getEmergencyMessageById(
            @PathVariable("emergency_message_id") int emergencyMessageId,
            @RequestHeader("X-User-ID") String userId
    ) {
        try {
            // 1. Validate user location
            UserLocation userLocation;
            try {
                userLocation = userLocationService.getUserLocation(userId);
            } catch (Exception e) {
                logger.warn("User location not found for userId: {}", userId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User location not found. Please set your location first."));
            }

            // 2. Fetch emergency message
            EmergencyMessage message = repository.findById(emergencyMessageId);
            if (message == null) {
                logger.warn("Emergency message not found for id: {}", emergencyMessageId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Emergency message not found."));
            }
            if (message.getLatitude() == null || message.getLongitude() == null) {
                logger.warn("Emergency message does not have valid location data. id: {}", emergencyMessageId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Emergency message does not have valid location data."));
            }

            // 3. Calculate distance
            double distance = GeoUtils.haversine(
                    userLocation.getLatitude(), userLocation.getLongitude(),
                    message.getLatitude(), message.getLongitude()
            );
            double radiusKm = 15.0; // Default radius restriction (can be changed if per-message radius is added)
            if (distance > radiusKm) {
                logger.info("User {} is not within {} km of emergency message {} (distance: {} km)", userId, radiusKm, emergencyMessageId, distance);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You are not within the allowed radius to view this emergency message."));
            }

            // 4. Prepare response: full details, notes, types, questions
            Map<String, Object> response = new HashMap<>();
            response.put("id", message.getId());
            response.put("title", message.getTitle());
            response.put("description", message.getDescription());
            response.put("types", message.getTypes());
            response.put("details", message.getDetails());
            response.put("googleMapsLocation", message.getGoogleMapsLocation());
            response.put("userId", message.getUserId());
            response.put("createdAt", message.getCreatedAt());
            response.put("latitude", message.getLatitude());
            response.put("longitude", message.getLongitude());
            response.put("notes", message.getNotes());
            // Fetch related questions
            response.put("questions", emergencyQuestionRepository.getQuestionsByEmergencyId(message.getId()));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to fetch emergency message by id", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch emergency message: " + e.getMessage()));
        }
    }
}
