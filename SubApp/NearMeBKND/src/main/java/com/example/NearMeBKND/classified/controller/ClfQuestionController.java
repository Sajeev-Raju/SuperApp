package com.example.NearMeBKND.classified.controller; 

import com.example.NearMeBKND.classified.model.ClfQuestion;
import com.example.NearMeBKND.classified.service.ClfQuestionService;
import com.example.NearMeBKND.classified.service.ClassifiedService;
import com.example.NearMeBKND.nearme.model.UserLocation;
import com.example.NearMeBKND.service.UserLocationService;
import com.example.NearMeBKND.util.GeoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/classified")
public class ClfQuestionController {
    private static final Logger logger = LoggerFactory.getLogger(ClfQuestionController.class);
    @Autowired
    private ClfQuestionService service;
    @Autowired
    private UserLocationService userLocationService;
    @Autowired
    private ClassifiedService classifiedService;

    @PostMapping("/question/{id}")
    public ResponseEntity<?> postQuestion(
        @PathVariable("id") int classifiedId,
        @RequestBody Map<String, String> payload,
        @RequestHeader("X-User-ID") String userId
    ) {
        try {
            String content = payload.get("content");
            logger.info("User '{}' is attempting to post a question to classified '{}'.", userId, classifiedId);
            if (content == null || content.trim().isEmpty()) {
                logger.warn("User '{}' submitted an empty question for classified '{}'.", userId, classifiedId);
                return ResponseEntity.badRequest().body(Map.of("message", "Question content cannot be empty. Please provide your question."));
            }

            // 1. Get the classified owner's user ID
            String ownerId = classifiedService.getClassifiedUserId(classifiedId);

            // 2. Prevent owner from asking a question on their own classified
            if (userId.equals(ownerId)) {
                logger.warn("User '{}' is the owner of classified '{}'. Cannot ask question to own classified.", userId, classifiedId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "You cannot ask a question on your own classified."));
            }

            // Check user location
            UserLocation userLocation;
            try {
                userLocation = userLocationService.getUserLocation(userId);
            } catch (RuntimeException e) {
                logger.warn("User '{}' not found in user_locations table. Cannot post question to classified '{}'.", userId, classifiedId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Your location is not set. Please update your location before asking a question."));
            }

            // Get classified owner location
            UserLocation ownerLocation;
            try {
                ownerLocation = userLocationService.getUserLocation(ownerId);
            } catch (RuntimeException e) {
                logger.error("Classified '{}' owner location not found.", classifiedId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "The classified owner has not set their location."));
            }

            double distance = GeoUtils.haversine(
                userLocation.getLatitude(), userLocation.getLongitude(),
                ownerLocation.getLatitude(), ownerLocation.getLongitude()
            );
            if (distance > 15.0) {
                logger.info("User '{}' is {:.2f} km away from classified owner (classified '{}'). Access denied.", userId, distance, classifiedId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "You must be within 15 km of the classified owner to ask a question. Your current distance is " + String.format("%.2f", distance) + " km."));
            }

            ClfQuestion question = new ClfQuestion();
            question.setClassifiedId(classifiedId);
            question.setContent(content);
            question.setUserId(userId);
            int id = service.postQuestion(question);
            logger.info("User '{}' successfully posted question '{}' to classified '{}'.", userId, id, classifiedId);
            return ResponseEntity.ok(Map.of(
                "id", id,
                "message", "Question posted successfully"
            ));
        } catch (Exception e) {
            logger.error("Error posting question to classified", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Failed to post question: " + e.getMessage()));
        }
    }

    @DeleteMapping("/question/{id}")
    public ResponseEntity<?> deleteQuestion(@PathVariable int id, @RequestHeader("X-User-ID") String userId) {
        // Fetch the question
        ClfQuestion question = service.getQuestionById(id);
        if (question == null) {
            logger.warn("User '{}' tried to delete non-existent question '{}'.", userId, id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Question not found."));
        }
        if (!userId.equals(question.getUserId())) {
            logger.warn("User '{}' is not the owner of question '{}'. Deletion forbidden.", userId, id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "You are not authorized to delete this question."));
        }
        service.deleteQuestion(id);
        logger.info("User '{}' deleted their question '{}'.", userId, id);
        return ResponseEntity.ok(Map.of("message", "Question deleted successfully."));
    }
} 