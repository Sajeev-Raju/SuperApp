package com.example.NearMeBKND.polls.controller;

import com.example.NearMeBKND.polls.model.CreatePollCollectionRequest;
import com.example.NearMeBKND.polls.model.PaginatedResponse;
import com.example.NearMeBKND.polls.model.VoteRequest;
import com.example.NearMeBKND.polls.service.PollCollectionService;
import com.example.NearMeBKND.polls.service.PollVoteService;
import com.example.NearMeBKND.nearme.repository.UserLocationRepository;
import com.example.NearMeBKND.nearme.model.UserLocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/polls")
public class PollQuestionController {
    private static final int DEFAULT_PAGE_SIZE = 30;

    @Autowired
    private PollCollectionService pollCollectionService;
    @Autowired
    private PollVoteService pollVoteService;
    @Autowired
    private UserLocationRepository userLocationRepository;

    private ResponseEntity<?> validateUserAndGetLocation(String userId) {
        Optional<UserLocation> userLocationOpt = userLocationRepository.findByUserId(userId);
        if (userLocationOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(
                java.util.Map.of(
                    "error", "Bad Request",
                    "message", "User location not found. Please set your location first."
                )
            );
        }
        return null;
    }

    @PostMapping("/collection")
    public ResponseEntity<?> createPollCollection(
            @RequestHeader("X-User-ID") String userId,
            @RequestBody CreatePollCollectionRequest request) {
        // Validate user
        ResponseEntity<?> validationResponse = validateUserAndGetLocation(userId);
        if (validationResponse != null) {
            return validationResponse;
        }

        UserLocation userLocation = userLocationRepository.findByUserId(userId).get();
        double latitude = userLocation.getLatitude();
        double longitude = userLocation.getLongitude();
        int collectionId = pollCollectionService.createPollCollection(userId, latitude, longitude, request);
        return ResponseEntity.ok().body(
                java.util.Map.of(
                        "message", "Poll collection created successfully",
                        "collectionId", collectionId
                )
        );
    }

    @GetMapping("/collection")
    public ResponseEntity<?> getPollCollections(
            @RequestHeader("X-User-ID") String userId,
            @RequestParam(defaultValue = "0") int page) {
        // Validate user
        ResponseEntity<?> validationResponse = validateUserAndGetLocation(userId);
        if (validationResponse != null) {
            return validationResponse;
        }

        UserLocation userLocation = userLocationRepository.findByUserId(userId).get();
        return ResponseEntity.ok(pollCollectionService.getPaginatedPollCollections(
            userId,
            userLocation.getLatitude(),
            userLocation.getLongitude(),
            page,
            DEFAULT_PAGE_SIZE
        ));
    }

    @PostMapping("/question/{questionId}/vote")
    public ResponseEntity<?> voteOnQuestion(
            @RequestHeader("X-User-ID") String userId,
            @PathVariable int questionId,
            @RequestBody VoteRequest voteRequest) {
        // Validate user
        ResponseEntity<?> validationResponse = validateUserAndGetLocation(userId);
        if (validationResponse != null) {
            return validationResponse;
        }

        java.util.Map<String, Object> result = pollVoteService.voteOnQuestion(userId, questionId, voteRequest);
        
        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok().body(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    @GetMapping("/question/{questionId}/results")
    public ResponseEntity<?> getQuestionResults(
            @RequestHeader("X-User-ID") String userId,
            @PathVariable int questionId) {
        // Validate user
        ResponseEntity<?> validationResponse = validateUserAndGetLocation(userId);
        if (validationResponse != null) {
            return validationResponse;
        }

        java.util.Map<String, Object> result = pollVoteService.getQuestionResults(userId, questionId);
        
        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok().body(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    @GetMapping("/my-votes")
    public ResponseEntity<?> getAllUserVotes(
            @RequestHeader("X-User-ID") String userId) {
        // Validate user
        ResponseEntity<?> validationResponse = validateUserAndGetLocation(userId);
        if (validationResponse != null) {
            return validationResponse;
        }

        java.util.Map<String, Object> result = pollVoteService.getAllUserVotes(userId);
        
        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok().body(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    @DeleteMapping("/question/{questionId}")
    public ResponseEntity<?> deletePollQuestion(
            @RequestHeader("X-User-ID") String userId,
            @PathVariable int questionId) {
        // Validate user
        ResponseEntity<?> validationResponse = validateUserAndGetLocation(userId);
        if (validationResponse != null) {
            return validationResponse;
        }

        try {
            // Check if user is the owner of the question's collection
            boolean isOwner = pollCollectionService.isUserCollectionOwner(userId, questionId, true);
            System.out.println("[DEBUG] DELETE /question/" + questionId + " | userId='" + userId + "' | isOwner=" + isOwner);
            if (!isOwner) {
                return ResponseEntity.status(403).body(
                    java.util.Map.of(
                        "error", "Forbidden",
                        "message", "You are not authorized to delete this question. Only the collection owner can delete questions."
                    )
                );
            }

            // Delete the question
            pollCollectionService.deletePollQuestion(questionId);
            return ResponseEntity.ok().body(
                java.util.Map.of(
                    "message", "Question deleted successfully"
                )
            );
        } catch (Exception e) {
            return ResponseEntity.status(404).body(
                java.util.Map.of(
                    "error", "Not Found",
                    "message", "Question not found or could not be deleted"
                )
            );
        }
    }

    @DeleteMapping("/collection/{collectionId}")
    public ResponseEntity<?> deletePollCollection(
            @RequestHeader("X-User-ID") String userId,
            @PathVariable int collectionId) {
        // Validate user
        ResponseEntity<?> validationResponse = validateUserAndGetLocation(userId);
        if (validationResponse != null) {
            return validationResponse;
        }

        try {
            // Check if user is the owner of the collection
            boolean isOwner = pollCollectionService.isUserCollectionOwner(userId, collectionId, false);
            System.out.println("[DEBUG] DELETE /collection/" + collectionId + " | userId='" + userId + "' | isOwner=" + isOwner);
            if (!isOwner) {
                return ResponseEntity.status(403).body(
                    java.util.Map.of(
                        "error", "Forbidden",
                        "message", "You are not authorized to delete this collection. Only the collection owner can delete it."
                    )
                );
            }

            // Delete the collection (this will cascade delete all questions)
            pollCollectionService.deletePollCollection(collectionId);
            return ResponseEntity.ok().body(
                java.util.Map.of(
                    "message", "Collection and all its questions deleted successfully"
                )
            );
        } catch (Exception e) {
            return ResponseEntity.status(404).body(
                java.util.Map.of(
                    "error", "Not Found",
                    "message", "Collection not found or could not be deleted"
                )
            );
        }
    }

    @GetMapping("/collection/{collectionId}")
    public ResponseEntity<?> getPollCollectionDetails(
            @RequestHeader("X-User-ID") String userId,
            @PathVariable int collectionId) {
        // Validate user
        ResponseEntity<?> validationResponse = validateUserAndGetLocation(userId);
        if (validationResponse != null) {
            return validationResponse;
        }

        java.util.Map<String, Object> result = pollCollectionService.getPollCollectionDetails(userId, collectionId);
        
        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok().body(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }
} 