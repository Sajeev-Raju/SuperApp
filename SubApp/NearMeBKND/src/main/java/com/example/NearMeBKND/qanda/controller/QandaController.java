package com.example.NearMeBKND.qanda.controller;
//abc
import com.example.NearMeBKND.qanda.model.Question;
import com.example.NearMeBKND.qanda.model.Answer;
import com.example.NearMeBKND.qanda.model.Tag;
import com.example.NearMeBKND.qanda.model.ApiResponse;
import com.example.NearMeBKND.qanda.service.QuestionService;
import com.example.NearMeBKND.qanda.service.AnswerService;
import com.example.NearMeBKND.qanda.service.TagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import com.example.NearMeBKND.qanda.model.QuestionRequest;
import com.example.NearMeBKND.qanda.model.QuestionResponse;
import com.example.NearMeBKND.service.UserLocationService;
import com.example.NearMeBKND.util.GeoUtils;
import com.example.NearMeBKND.nearme.model.UserLocation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.*;

@RestController
@RequestMapping("/api/qanda")
public class QandaController {

    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 100;
    private static final double LOCATION_RADIUS_KM = 15.0;

    @Autowired
    private QuestionService questionService;
    @Autowired
    private AnswerService answerService;
    @Autowired
    private TagService tagService;
    @Autowired
    private UserLocationService userLocationService;

    // Helper method to check location restrictions
    private ResponseEntity<?> checkLocationRestriction(String currentUserId, String targetUserId) {
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "X-User-ID header is required."));
        }

        UserLocation currentUserLoc = userLocationService.getUserLocation(currentUserId);
        UserLocation targetUserLoc = userLocationService.getUserLocation(targetUserId);
        
        double distance = GeoUtils.haversine(
            currentUserLoc.getLatitude(), currentUserLoc.getLongitude(),
            targetUserLoc.getLatitude(), targetUserLoc.getLongitude()
        );

        if (distance > LOCATION_RADIUS_KM) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "You are not within " + LOCATION_RADIUS_KM + " km of the target user."));
        }

        return null; // No restriction violation
    }

    // Create a question with tags
    @PostMapping("/questions")
    public ResponseEntity<?> addQuestion(@RequestHeader("X-User-ID") String userId, @RequestBody QuestionRequest payload) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "X-User-ID header is required."));
        }

        // Validate required fields
        Map<String, List<String>> validationErrors = new HashMap<>();
        if (payload.getQTitle() == null || payload.getQTitle().trim().isEmpty()) {
            validationErrors.put("qTitle", List.of("Question title is required"));
        }
        if (payload.getQuestionDescription() == null || payload.getQuestionDescription().trim().isEmpty()) {
            validationErrors.put("questionDescription", List.of("Question description is required"));
        }
        
        if (!validationErrors.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.validationError(validationErrors));
        }

        // Get user's location
        UserLocation userLocation = userLocationService.getUserLocation(userId);
        if (userLocation == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "User location not found. Please update your location first."));
        }

        Question question = new Question();
        question.setTitle(payload.getQTitle());
        question.setDescription(payload.getQuestionDescription());
        question.setUserId(userId);
        question.setLatitude(userLocation.getLatitude());
        question.setLongitude(userLocation.getLongitude());

        List<String> tagList = payload.getTags() != null ? payload.getTags() : List.of();
        question.setTags(String.join(",", tagList));
        
        if (!tagList.isEmpty()) {
            List<Tag> tagObjs = tagList.stream()
                .map(tagName -> {
                    Tag tag = new Tag();
                    tag.setTagName(tagName.trim());
                    return tag;
                })
                .toList();
            question.setTagList(tagObjs);
        }
        
        try {
            int qId = questionService.addQuestion(question, tagList);
            Map<String, Object> resp = new HashMap<>();
            resp.put("qId", qId);
            return ResponseEntity.ok(ApiResponse.success(resp));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to create question: " + e.getMessage()));
        }
    }

    // Get all questions or by tags
    @GetMapping("/questions/")
    public ResponseEntity<?> getQuestions(
            @RequestHeader("X-User-ID") String userId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int size) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "X-User-ID header is required."));
        }
        UserLocation userLocation = userLocationService.getUserLocation(userId);
        if (userLocation == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "User location not found. Please update your location first."));
        }
        double radiusKm = 15.0;
        int offset = page * size;
        int total = questionService.countQuestionsInRadius(userLocation.getLatitude(), userLocation.getLongitude(), radiusKm);
        int totalPages = (int) Math.ceil((double) total / size);
        List<Question> questions = questionService.findQuestionsInRadiusPaged(userLocation.getLatitude(), userLocation.getLongitude(), radiusKm, size, offset);
        List<Map<String, Object>> pageContent = new ArrayList<>();
        for (Question q : questions) {
            pageContent.add(Map.of(
                "id", q.getId(),
                "userId", q.getUserId(),
                "title", q.getTitle(),
                "description", q.getDescription(),
                "tags", q.getTags(),
                "createdAt", q.getCreatedAt(),
                "latitude", q.getLatitude(),
                "longitude", q.getLongitude()
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

    // Get question by id
    @GetMapping("/questions/{id}")
    public ResponseEntity<?> getQuestionById(@RequestHeader("X-User-ID") String userId, @PathVariable Long id) {
        Question question = questionService.getQuestionById(id).orElse(null);
        if (question == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Question not found with id: " + id));
        }

        // Check location restriction
        ResponseEntity<?> locationCheck = checkLocationRestriction(userId, question.getUserId());
        if (locationCheck != null) {
            return locationCheck;
        }

        Map<String, Object> resp = new java.util.LinkedHashMap<>();
        resp.put("id", question.getId());
        resp.put("userId", question.getUserId());
        resp.put("title", question.getTitle());
        resp.put("description", question.getDescription());
        resp.put("tags", question.getTags());
        resp.put("createdAt", question.getCreatedAt());
        resp.put("notes", question.getNotes());
        resp.put("answers", question.getAnswers());
        return ResponseEntity.ok(resp);
    }

    // Delete question
    @DeleteMapping("/questions/{id}")
    public ResponseEntity<?> deleteQuestion(@RequestHeader("X-User-ID") String userId, @PathVariable Long id) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "X-User-ID header is required."));
        }
        Question question = questionService.getQuestionById(id).orElse(null);
        if (question == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Question not found with id: " + id));
        }
        if (!userId.equals(question.getUserId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "You can only delete your own questions"));
        }
        try {
            questionService.deleteQuestion(id.intValue());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Failed to delete question: " + e.getMessage()));
        }
    }

    // Add answer to question
    @PostMapping("/questions/{id}/answers")
    public ResponseEntity<?> addAnswer(
            @RequestHeader("X-User-ID") String userId,
            @PathVariable("id") Long questionId,
            @RequestBody Map<String, String> payload) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "X-User-ID header is required."));
        }

        Question question = questionService.getQuestionById(questionId).orElse(null);
        if (question == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Question not found with id: " + questionId));
        }

        // Check if user is trying to answer their own question
        if (userId.equals(question.getUserId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of(
                    "status", "error",
                    "message", "You cannot answer your own question. This restriction helps maintain the community's collaborative nature."
                ));
        }

        // Check location restriction
        ResponseEntity<?> locationCheck = checkLocationRestriction(userId, question.getUserId());
        if (locationCheck != null) {
            return locationCheck;
        }

        String description = payload.get("description");
        if (description == null || description.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", "Answer description cannot be empty"));
        }

        Answer answer = new Answer();
        answer.setQuestionId(questionId);
        answer.setDescription(description);
        answer.setUserId(userId);

        try {
            int aId = answerService.addAnswer(answer);
            Map<String, Object> resp = new HashMap<>();
            resp.put("aId", aId);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Failed to add answer: " + e.getMessage()));
        }
    }

    // Get answers for a question
    @GetMapping("/questions/{id}/answers")
    public ResponseEntity<?> getAnswersForQuestion(@RequestHeader("X-User-ID") String userId, @PathVariable Long id) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "X-User-ID header is required."));
        }

        Question question = questionService.getQuestionById(id).orElse(null);
        if (question == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Question not found with id: " + id));
        }

        // Check location restriction
        ResponseEntity<?> locationCheck = checkLocationRestriction(userId, question.getUserId());
        if (locationCheck != null) {
            return locationCheck;
        }

        List<Answer> answers = answerService.getAnswersForQuestion(id.intValue());
        Map<String, Object> resp = new HashMap<>();
        resp.put("question", question);
        resp.put("answers", answers);
        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    // Delete answer
    @DeleteMapping("/answers/{id}")
    public ResponseEntity<?> deleteAnswer(@RequestHeader("X-User-ID") String userId, @PathVariable int id) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "X-User-ID header is required."));
        }

        Answer answer = answerService.getAnswerById(id);
        if (answer == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Answer not found with id: " + id));
        }

        // Only the answer owner can delete
        boolean isAnswerOwner = userId.equals(answer.getUserId());
        if (!isAnswerOwner) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "You can only delete your own answers"));
        }

        try {
            answerService.deleteAnswer(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Failed to delete answer: " + e.getMessage()));
        }
    }

    // Test endpoint to create a sample question
    // @PostMapping("/test/create-question")
    // public ResponseEntity<?> createTestQuestion(HttpServletRequest request) {
    //     String currentUserId = getCurrentUserId(request);
    //     if (currentUserId == null) {
    //         return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
    //             .body(Map.of("message", "X-User-ID header is required."));
    //     }

    //     QuestionRequest payload = new QuestionRequest();
    //     payload.setQTitle("Test Question");
    //     payload.setQuestionDescription("This is a test question for testing the answer functionality");
    //     payload.setTags(List.of("test", "qa"));

    //     Question question = new Question();
    //     question.setTitle(payload.getQTitle());
    //     question.setDescription(payload.getQuestionDescription());
    //     question.setUserId(currentUserId);
    //     question.setTags(String.join(",", payload.getTags()));

    //     try {
    //         int qId = questionService.addQuestion(question, payload.getTags());
    //         Map<String, Object> resp = new HashMap<>();
    //         resp.put("qId", qId);
    //         resp.put("requestPath", request.getRequestURI());
    //         return ResponseEntity.ok(ApiResponse.success(resp));
    //     } catch (Exception e) {
    //         return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
    //             .body(ApiResponse.error("Failed to create test question: " + e.getMessage()));
    //     }
    // }

    // Get all tags
    @GetMapping("/tags")
    public ResponseEntity<?> getAllTags(@RequestHeader("X-User-ID") String userId) {
        List<Tag> tags = tagService.getAllTags();
        return ResponseEntity.ok(tags);
    }

    @PostMapping("/questions/{id}/notes")
    public ResponseEntity<ApiResponse> addNote(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            @RequestHeader("X-User-ID") String userId) {
        try {
            Question question = questionService.getQuestionById(id).orElse(null);
            if (question == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(false, "Question not found"));
            }

            if (!question.getUserId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse(false, "Not authorized to add notes to this question"));
            }

            String note = request.get("note");
            if (note == null || note.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Note cannot be empty"));
            }

            questionService.addNote(id, note);
            return ResponseEntity.ok(new ApiResponse(true, "Note added successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Error adding note: " + e.getMessage()));
        }
    }
}