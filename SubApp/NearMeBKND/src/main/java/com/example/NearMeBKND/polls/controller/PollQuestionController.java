package com.example.NearMeBKND.polls.controller;

import com.example.NearMeBKND.polls.model.PollOption;
import com.example.NearMeBKND.polls.model.PollQuestion;
import com.example.NearMeBKND.polls.service.PollQuestionService;
import com.example.NearMeBKND.polls.service.PollVoteService;
import com.example.NearMeBKND.service.UserLocationService;
import com.example.NearMeBKND.nearme.model.UserLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/polls/questions")
public class PollQuestionController {

    private static final Logger logger = LoggerFactory.getLogger(PollQuestionController.class);

    @Autowired
    private PollQuestionService pollQuestionService;

    @Autowired
    private PollVoteService pollVoteService;

    @Autowired
    private UserLocationService userLocationService;

    // POST /api/questions - handles both single and multiple questions
    @PostMapping
    public ResponseEntity<?> createQuestion(
            @RequestHeader("X-User-ID") String userId,
            @RequestBody Object payload) {
        logger.info("[POST /api/questions] userId={}, payload={}", userId, payload);
        // Validate user location (same as classified)
        UserLocation userLocation = userLocationService.getUserLocation(userId);
        if (userLocation == null) {
            return ResponseEntity.status(404)
                .body(Map.of("error", "User location not found. Please update your location first."));
        }
        // Handle single question
        if (payload instanceof Map) {
            Map<String, Object> singleQuestion = (Map<String, Object>) payload;
            String questionText = (String) singleQuestion.get("questionText");
            List<String> options = (List<String>) singleQuestion.get("pollOptions");
            int selectionLimit = singleQuestion.get("selectionLimit") != null ? (int) singleQuestion.get("selectionLimit") : 1;

            if (options.size() < 2 || options.size() > 4) {
                return ResponseEntity.badRequest().body("Poll options must be between 2 and 4.");
            }
            if (selectionLimit < 1 || selectionLimit > options.size()) {
                return ResponseEntity.badRequest().body("selectionLimit must be between 1 and the number of options.");
            }
            // No selectionMode, always UPTO logic

            try {
                int questionId = pollQuestionService.createQuestionWithPolls(userId, questionText, options, selectionLimit, "UPTO");
                logger.info("Question created: userId={}, questionId={}", userId, questionId);
                return ResponseEntity.ok(Map.of("message", "Question created", "questionId", questionId));
            } catch (Exception e) {
                logger.error("Error creating question: userId={}, error={}", userId, e.getMessage(), e);
                return ResponseEntity.status(500).body("Error creating question: " + e.getMessage());
            }
        }
        // Handle multiple questions
        else if (payload instanceof List) {
            List<Map<String, Object>> questions = (List<Map<String, Object>>) payload;
            List<Map<String, Object>> results = new ArrayList<>();
            
            for (Map<String, Object> question : questions) {
                String questionText = (String) question.get("questionText");
                List<String> options = (List<String>) question.get("pollOptions");
                int selectionLimit = question.get("selectionLimit") != null ? (int) question.get("selectionLimit") : 1;

                if (options.size() < 2 || options.size() > 4) {
                    return ResponseEntity.badRequest().body("Poll options must be between 2 and 4 for question: " + questionText);
                }
                if (selectionLimit < 1 || selectionLimit > options.size()) {
                    return ResponseEntity.badRequest().body("selectionLimit must be between 1 and the number of options for question: " + questionText);
                }
                // No selectionMode, always UPTO logic

                int questionId = pollQuestionService.createQuestionWithPolls(userId, questionText, options, selectionLimit, "UPTO");
                results.add(Map.of(
                    "questionText", questionText,
                    "questionId", questionId,
                    "message", "Question created"
                ));
            }

            return ResponseEntity.ok(results);
        }

        return ResponseEntity.badRequest().body("Invalid request format");
    }

    // POST /api/questions/{id}/vote
    @PostMapping("/{id}/vote")
    public ResponseEntity<?> voteOnPoll(
            @RequestHeader("X-User-ID") String userId,
            @PathVariable int id,
            @RequestBody Map<String, Object> payload) {
        logger.info("[POST /api/questions/{}/vote] userId={}, optionIds={}", id, userId, payload.get("optionIds"));
        List<Integer> optionIds = (List<Integer>) payload.get("optionIds");
        if (optionIds == null || optionIds.isEmpty()) {
            return ResponseEntity.badRequest().body("You must select at least one option.");
        }

        // Fetch selectionLimit for this question
        PollQuestion question = pollQuestionService.getQuestion(id, userId);
        if (question == null) {
            return ResponseEntity.status(404).body("Poll question not found or not accessible.");
        }
        int selectionLimit = question.getSelectionLimit();

        if (optionIds.size() < 1 || optionIds.size() > selectionLimit) {
            return ResponseEntity.badRequest().body("You can select up to " + selectionLimit + " options.");
        }

        boolean success = pollVoteService.vote(userId, id, optionIds);

        if (!success) {
            logger.warn("Voting failed: userId={}, questionId={}", userId, id);
            // Check if the failure is due to invalid optionIds
            return ResponseEntity.badRequest().body("Voting failed. One or more optionIds are invalid for this question, or you might be the owner, or already voted.");
        }
        logger.info("Vote recorded: userId={}, questionId={}", userId, id);
        return ResponseEntity.ok(Map.of("message", "Vote recorded"));
    }

    // GET /api/questions/{id}/results
    @GetMapping("/{id}/results")
    public ResponseEntity<?> getResults(
            @RequestHeader("X-User-ID") String userId,
            @PathVariable int id) {
        logger.info("[GET /api/questions/{}/results] userId={}", id, userId);
        List<PollOption> results = pollQuestionService.getResults(id, userId);
        return ResponseEntity.ok(results);
    }

    // GET /api/questions
    @GetMapping("/")
    public ResponseEntity<?> getAllQuestions(
            @RequestHeader("X-User-ID") String userId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "30") int size) {
        logger.info("[GET /api/questions] userId={}, page={}, size={}", userId, page, size);
        UserLocation userLocation = userLocationService.getUserLocation(userId);
        if (userLocation == null) {
            return ResponseEntity.status(404)
                .body(Map.of("error", "User location not found. Please update your location first."));
        }
        double radiusKm = 15.0;
        int offset = page * size;
        int total = pollQuestionService.countQuestionsInRadius(userLocation.getLatitude(), userLocation.getLongitude(), radiusKm);
        int totalPages = (int) Math.ceil((double) total / size);
        List<PollQuestion> questions = pollQuestionService.getQuestionsInRadiusPaged(userLocation.getLatitude(), userLocation.getLongitude(), radiusKm, size, offset);
        List<Map<String, Object>> pageContent = new ArrayList<>();
        for (PollQuestion question : questions) {
            pageContent.add(Map.of(
                "questionId", question.getQuestionId(),
                "userId", question.getUserId(),
                "questionText", question.getQuestionText(),
                "selectionLimit", question.getSelectionLimit(),
                "selectionMode", question.getSelectionMode(),
                "latitude", question.getLatitude(),
                "longitude", question.getLongitude(),
                "createdAt", question.getCreatedAt()
            ));
        }
        Map<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("data", pageContent);
        response.put("currentPage", page);
        response.put("size", size);
        response.put("count", pageContent.size());
        response.put("totalPages", totalPages);
        return ResponseEntity.ok(response);
    }

    // DELETE /api/questions/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteQuestion(
            @RequestHeader("X-User-ID") String userId,
            @PathVariable int id) {
        logger.info("[DELETE /api/questions/{}] userId={}", id, userId);
        boolean deleted = pollQuestionService.deleteQuestion(userId, id);
        
        if (!deleted) {
            logger.warn("Delete failed: userId={}, questionId={}", userId, id);
            return ResponseEntity.badRequest().body("Cannot delete question. You must be the owner.");
        }
        logger.info("Question deleted: userId={}, questionId={}", userId, id);
        return ResponseEntity.ok(Map.of("message", "Question deleted successfully"));
    }

    // GET /api/questions/voted - get all polls the user has voted on
    @GetMapping("/voted")
    public ResponseEntity<?> getVotedPolls(@RequestHeader("X-User-ID") String userId) {
        // Fetch all votes by this user
        List<Map<String, Object>> votedPolls = pollVoteService.getVotedPollsByUser(userId);
        return ResponseEntity.ok(votedPolls);
    }

    @PutMapping("/{id}/vote")
    public ResponseEntity<?> updateVoteOnPoll(
            @RequestHeader("X-User-ID") String userId,
            @PathVariable int id,
            @RequestBody Map<String, Object> payload) {
        List<Integer> optionIds = (List<Integer>) payload.get("optionIds");
        if (optionIds == null || optionIds.isEmpty()) {
            return ResponseEntity.badRequest().body("You must select at least one option.");
        }
        // Enhanced validation
        String voteOwner = pollVoteService.getVoteOwner(id, userId);
        if (voteOwner == null) {
            return ResponseEntity.status(400).body("You have not voted on this question yet. Please cast a vote before attempting to update it.");
        }
        if (!voteOwner.equals(userId)) {
            return ResponseEntity.status(403).body("You are not authorized to update another user's vote.");
        }
        boolean success = pollVoteService.updateVote(userId, id, optionIds);
        if (!success) {
            return ResponseEntity.badRequest().body("Vote update failed. You might not have voted before, or selected invalid options.");
        }
        return ResponseEntity.ok(Map.of("message", "Vote updated"));
    }

    @GetMapping("/{question_id}")
    public ResponseEntity<?> getPollQuestionById(
            @PathVariable int question_id,
            @RequestHeader("X-User-ID") String userId) {
        // Check if user exists in user_locations
        com.example.NearMeBKND.nearme.model.UserLocation userLocation;
        try {
            userLocation = userLocationService.getUserLocation(userId);
        } catch (Exception e) {
            return ResponseEntity.status(403).body(Map.of("error", "User ID not found in user_locations. Please update your location first."));
        }
        // Fetch poll question
        PollQuestion question = pollQuestionService.getQuestion(question_id, userId);
        if (question == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Poll question not found or not accessible within your radius."));
        }
        // Fetch poll options
        List<com.example.NearMeBKND.polls.model.PollOption> options = pollQuestionService.getPollOptionRepository().getOptionsWithVoteCount(question_id);
        // Fetch poll votes (per option)
        // For simplicity, vote counts are already included in options
        // Build response
        Map<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("questionId", question.getQuestionId());
        response.put("userId", question.getUserId());
        response.put("questionText", question.getQuestionText());
        response.put("selectionLimit", question.getSelectionLimit());
        response.put("selectionMode", question.getSelectionMode());
        response.put("latitude", question.getLatitude());
        response.put("longitude", question.getLongitude());
        response.put("createdAt", question.getCreatedAt());
        response.put("options", options);
        return ResponseEntity.ok(response);
    }
} 