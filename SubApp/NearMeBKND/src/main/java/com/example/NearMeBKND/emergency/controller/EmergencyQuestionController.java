package com.example.NearMeBKND.emergency.controller;

import com.example.NearMeBKND.emergency.model.EmergencyQuestion;
import com.example.NearMeBKND.emergency.repository.EmergencyMessageRepository;
import com.example.NearMeBKND.emergency.service.EmergencyQuestionService;
import com.example.NearMeBKND.nearme.model.UserLocation;
import com.example.NearMeBKND.service.UserLocationService;
import com.example.NearMeBKND.util.GeoUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

@RestController
@RequestMapping("/api/emergency_messages")
public class EmergencyQuestionController {
    @Autowired
    private EmergencyQuestionService questionService;
    @Autowired
    private EmergencyMessageRepository messageRepository;
    @Autowired
    private UserLocationService userLocationService;

    // Post a question (only if not owner and within 25km)
    @PostMapping("/{id}/question")
    public ResponseEntity<?> postQuestion(
        @PathVariable int id,
        @RequestBody Map<String, String> payload,
        @RequestHeader("X-User-ID") String userId
    ) {
        var message = messageRepository.findById(id);
        if (message == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Emergency message not found."));
        }
        if (userId.equals(message.getUserId())) {
            return ResponseEntity.status(403).body(Map.of("message", "You cannot ask a question on your own emergency message."));
        }
        UserLocation userLoc = userLocationService.getUserLocation(userId);
        UserLocation ownerLoc = userLocationService.getUserLocation(message.getUserId());
        double distance = GeoUtils.haversine(
            userLoc.getLatitude(), userLoc.getLongitude(),
            ownerLoc.getLatitude(), ownerLoc.getLongitude()
        );
        if (distance > 15.0) {
            return ResponseEntity.status(403).body(Map.of("message", "You must be within 15 km of the emergency message owner to ask a question."));
        }
        String content = payload.get("content");
        if (content == null || content.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Question content cannot be empty."));
        }
        EmergencyQuestion question = new EmergencyQuestion();
        question.setEmergencyId(id);
        question.setContent(content);
        question.setUserId(userId);
        int qid = questionService.postQuestion(question);
        return ResponseEntity.ok(Map.of("id", qid));
    }

    // Get emergency message with its questions (only if within 25km)
    @GetMapping("/{id}/with-questions")
    public ResponseEntity<?> getEmergencyWithQuestions(
        @PathVariable int id,
        @RequestHeader("X-User-ID") String userId
    ) {
        var message = messageRepository.findById(id);
        if (message == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Emergency message not found."));
        }
        UserLocation userLoc = userLocationService.getUserLocation(userId);
        UserLocation ownerLoc = userLocationService.getUserLocation(message.getUserId());
        double distance = GeoUtils.haversine(
            userLoc.getLatitude(), userLoc.getLongitude(),
            ownerLoc.getLatitude(), ownerLoc.getLongitude()
        );
        if (distance > 15.0) {
            return ResponseEntity.status(403).body(Map.of("message", "You are not within 15 km of the emergency message owner."));
        }
        List<EmergencyQuestion> questions = questionService.getQuestionsByEmergencyId(id);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("emergency", message);
        response.put("questions", questions);
        return ResponseEntity.ok(response);
    }

    // Delete a question (only by question owner)
    @DeleteMapping("/question/{qid}")
    public ResponseEntity<?> deleteQuestion(
        @PathVariable int qid,
        @RequestHeader("X-User-ID") String userId
    ) {
        EmergencyQuestion question = questionService.getQuestionById(qid);
        if (question == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Question not found."));
        }
        if (!userId.equals(question.getUserId())) {
            return ResponseEntity.status(403).body(Map.of("message", "You are not authorized to delete this question."));
        }
        questionService.deleteQuestion(qid);
        return ResponseEntity.ok(Map.of("message", "Question deleted successfully."));
    }
} 