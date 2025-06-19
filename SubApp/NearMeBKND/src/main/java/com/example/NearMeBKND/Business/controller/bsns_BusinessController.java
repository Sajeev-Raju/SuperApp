package com.example.NearMeBKND.Business.controller;

import com.example.NearMeBKND.Business.model.bsns_Business;
import com.example.NearMeBKND.Business.model.bsns_BusinessNotification;
import com.example.NearMeBKND.Business.service.bsns_BusinessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.HashMap;
import java.util.Map;
import com.example.NearMeBKND.util.QueryLogger;

@RestController
@RequestMapping("/api/business")
public class bsns_BusinessController {
    private static final Logger logger = LoggerFactory.getLogger(bsns_BusinessController.class);

    @Autowired
    private bsns_BusinessService businessService;

    @PostMapping(value = "/", consumes = {"multipart/form-data"})
    public ResponseEntity<?> createBusiness(
            @RequestHeader("X-User-ID") String userId,
            @RequestParam("name") String name,
            @RequestParam("title") String title,
            @RequestParam("tags") String tags,
            @RequestParam("description") String description,
            @RequestParam("address") String address,
            @RequestParam("mobileNumber") String mobileNumber,
            @RequestParam("timings") String timings,
            @RequestParam("googlemapsURL") String googlemapsURL,
            @RequestParam(value = "image", required = false) MultipartFile image) {
        logger.debug("Received createBusiness request: userId={}, name={}, title={}, tags={}, description={}, address={}, mobileNumber={}, timings={}, googlemapsURL={}",
                userId, name, title, tags, description, address, mobileNumber, timings, googlemapsURL);
        if (googlemapsURL == null || googlemapsURL.isBlank()) {
            logger.warn("Bad request: googlemapsURL is required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("googlemapsURL is required");
        }
        if (image != null) {
            logger.debug("Image received: name={}, size={}, contentType={}", image.getOriginalFilename(), image.getSize(), image.getContentType());
        } else {
            logger.debug("No image received");
        }
        try {
            bsns_Business business = new bsns_Business();
            business.setName(name);
            business.setTitle(title);
            business.setTags(tags);
            business.setDescription(description);
            business.setAddress(address);
            business.setMobileNumber(mobileNumber);
            business.setTimings(timings);
            business.setGooglemapsURL(googlemapsURL);
            if (image != null && !image.isEmpty()) {
                business.setImage(image.getBytes());
                logger.debug("Image bytes set on business object ({} bytes)", image.getSize());
            }
            business.setUserId(userId);
            logger.debug("Calling businessService.createBusiness");
            businessService.createBusiness(business, userId);
            logger.debug("Business created successfully");
            return ResponseEntity.ok().body("Business created successfully");
        } catch (IllegalArgumentException e) {
            logger.warn("Bad request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            logger.error("Internal server error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/notify/{businessId}")
    public ResponseEntity<?> createNotification(
            @RequestHeader("X-User-ID") String userId,
            @PathVariable("businessId") Integer businessId,
            @RequestBody java.util.Map<String, String> body) {
        try {
            String message = body.get("notification");
            if (message == null || message.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(java.util.Map.of(
                        "success", false,
                        "message", "Notification message is required",
                        "data", null));
            }
            bsns_BusinessNotification notification = businessService.createNotification(businessId, userId, message);
            return ResponseEntity.ok(java.util.Map.of(
                    "success", true,
                    "message", "Notification created successfully",
                    "data", java.util.Map.of(
                        "businessId", notification.getBusinessId(),
                        "userId", notification.getUserId(),
                        "message", notification.getMessage(),
                        "createdAt", notification.getCreatedAt()
                    )
            ));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(java.util.Map.of(
                    "success", false,
                    "message", "User is not the owner of this business",
                    "data", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(java.util.Map.of(
                    "success", false,
                    "message", e.getMessage(),
                    "data", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(java.util.Map.of(
                    "success", false,
                    "message", "Error: " + e.getMessage(),
                    "data", null));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getBusinessById(@RequestHeader("X-User-ID") String userId, @PathVariable("id") Integer id) {
        try {
            bsns_Business business = businessService.getBusinessByIdWithAuth(userId, id);
            if (business == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(buildResponse(
                    false,
                    "Business not found",
                    null
                ));
            }
            return ResponseEntity.ok(java.util.Map.of(
                "success", true,
                "message", "Operation successful",
                "data", java.util.List.of(businessService.businessToFullMap(business))
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(buildResponse(
                false,
                e.getMessage(),
                null
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(buildResponse(
                false,
                "Error: " + e.getMessage(),
                null
            ));
        }
    }

    @GetMapping("/")
    public ResponseEntity<?> getAllBusinesses(
            @RequestHeader("X-User-ID") String userId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "30") int size) {
        long requestStart = System.currentTimeMillis();
        QueryLogger.clear();
        try {
            java.util.List<bsns_Business> businesses = businessService.getAllBusinessesInRadius(userId, page, size);
            java.util.List<com.example.NearMeBKND.Business.dto.BusinessDTO> dtos = new java.util.ArrayList<>();
            for (bsns_Business b : businesses) {
                dtos.add(businessService.toBusinessDTO(b));
            }
            int totalCount = businessService.getFilteredBusinessCount(userId);
            int totalPages = (int) Math.ceil((double) totalCount / size);
            if (dtos == null) dtos = java.util.Collections.emptyList();
            long requestEnd = System.currentTimeMillis();
            return ResponseEntity.ok(java.util.Map.of(
                "success", true,
                "message", "Operation successful",
                "page", page,
                "size", size,
                "count", dtos.size(),
                "totalPages", totalPages,
                "data", dtos,
                "queries", QueryLogger.getQueries(),
                "totalRequestTimeMs", requestEnd - requestStart
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(java.util.Map.of(
                "success", false,
                "message", e.getMessage(),
                "data", null
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(java.util.Map.of(
                "success", false,
                "message", "Error: " + e.getMessage(),
                "data", null
            ));
        } finally {
            QueryLogger.clear();
        }
    }

    @GetMapping("/tags={tags}")
    public ResponseEntity<?> getBusinessesByTags(@RequestHeader("X-User-ID") String userId, @PathVariable("tags") String tags) {
        try {
            java.util.List<bsns_Business> businesses = businessService.getBusinessesByTags(userId, tags);
            java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
            for (bsns_Business b : businesses) {
                result.add(businessService.businessToFullMap(b));
            }
            return ResponseEntity.ok(java.util.Map.of(
                "success", true,
                "message", "Operation successful",
                "data", result
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(java.util.Map.of(
                "success", false,
                "message", e.getMessage(),
                "data", null
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(java.util.Map.of(
                "success", false,
                "message", "Error: " + e.getMessage(),
                "data", null
            ));
        }
    }

    @GetMapping("/image/{businessId}")
    public ResponseEntity<byte[]> getBusinessImage(@PathVariable("businessId") Integer businessId) {
        bsns_Business business = businessService.getBusinessById(businessId);
        if (business == null || business.getImage() == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().header("Content-Type", "image/jpeg").body(business.getImage());
    }

    @GetMapping("/mine")
    public ResponseEntity<?> getMyBusinesses(@RequestHeader("X-User-ID") String userId) {
        try {
            java.util.List<bsns_Business> businesses = businessService.getMyBusinesses(userId);
            java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
            for (bsns_Business b : businesses) {
                result.add(businessService.businessToFullMap(b));
            }
            return ResponseEntity.ok(java.util.Map.of(
                "success", true,
                "message", "Operation successful",
                "data", result
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(java.util.Map.of(
                "success", false,
                "message", e.getMessage(),
                "data", null
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(java.util.Map.of(
                "success", false,
                "message", "Error: " + e.getMessage(),
                "data", null
            ));
        }
    }

    @GetMapping("/ids={businessIds}")
    public ResponseEntity<?> getBusinessesByIds(@RequestHeader("X-User-ID") String userId, @PathVariable("businessIds") String businessIds) {
        try {
            if (businessIds == null || businessIds.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(java.util.Map.of(
                    "success", false,
                    "message", "No business IDs provided",
                    "data", null
                ));
            }
            java.util.List<Integer> ids = new java.util.ArrayList<>();
            for (String idStr : businessIds.split(",")) {
                try {
                    ids.add(Integer.parseInt(idStr.trim()));
                } catch (NumberFormatException ignored) {}
            }
            java.util.List<bsns_Business> businesses = businessService.getBusinessesByIds(userId, ids);
            java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
            for (bsns_Business b : businesses) {
                result.add(businessService.businessToFullMap(b));
            }
            return ResponseEntity.ok(java.util.Map.of(
                "success", true,
                "message", "Operation successful",
                "data", result
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(java.util.Map.of(
                "success", false,
                "message", e.getMessage(),
                "data", null
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(java.util.Map.of(
                "success", false,
                "message", "Error: " + e.getMessage(),
                "data", null
            ));
        }
    }

    @DeleteMapping("/{businessId}")
    public ResponseEntity<?> softDeleteBusiness(@RequestHeader("X-User-ID") String userId, @PathVariable("businessId") Integer businessId) {
        try {
            boolean success = businessService.softDeleteBusiness(userId, businessId);
            if (success) {
                return ResponseEntity.ok(buildResponse(true, "Business marked as inactive successfully", null));
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(buildResponse(false, "User is not the owner of this business", null));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(buildResponse(false, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(buildResponse(false, "Error: " + e.getMessage(), null));
        }
    }

    @PutMapping(value = "/{businessId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateBusinessImage(
            @RequestHeader("X-User-ID") String userId,
            @PathVariable("businessId") Integer businessId,
            @RequestPart("image") MultipartFile image) {
        try {
            boolean success = businessService.updateBusinessImage(userId, businessId, image);
            if (success) {
                return ResponseEntity.ok(buildResponse(
                    true,
                    "Business image updated successfully",
                    null
                ));
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(buildResponse(
                    false,
                    "User is not the owner of this business",
                    null
                ));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(buildResponse(
                false,
                e.getMessage(),
                null
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(buildResponse(
                false,
                "Error processing image: " + e.getMessage(),
                null
            ));
        }
    }

    @PutMapping(value = "/{businessId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateBusinessFields(
            @RequestHeader("X-User-ID") String userId,
            @PathVariable("businessId") Integer businessId,
            @RequestBody java.util.Map<String, Object> fields) {
        try {
            boolean success = businessService.updateBusinessFields(userId, businessId, fields);
            if (success) {
                return ResponseEntity.ok(buildResponse(
                    true,
                    "Business updated successfully",
                    null
                ));
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(buildResponse(
                    false,
                    "User is not the owner of this business",
                    null
                ));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(buildResponse(
                false,
                e.getMessage(),
                null
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(buildResponse(
                false,
                "Error processing update: " + e.getMessage(),
                null
            ));
        }
    }

    @PostMapping("/question")
    public ResponseEntity<?> postBusinessQuestion(
            @RequestHeader("X-User-ID") String userId,
            @RequestBody java.util.Map<String, Object> body) {
        try {
            Integer businessId = (body.get("businessId") instanceof Number)
                ? ((Number) body.get("businessId")).intValue()
                : Integer.parseInt(body.get("businessId").toString());
            String questionText = (String) body.get("questionText");
            businessService.postBusinessQuestion(userId, businessId, questionText);
            return ResponseEntity.ok(buildResponse(
                true,
                "Question posted successfully",
                null
            ));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(buildResponse(
                false,
                e.getMessage(),
                null
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(buildResponse(
                false,
                e.getMessage(),
                null
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(buildResponse(
                false,
                "Error: " + e.getMessage(),
                null
            ));
        }
    }

    @DeleteMapping("/question/{questionId}")
    public ResponseEntity<?> deleteBusinessQuestion(
            @RequestHeader("X-User-ID") String userId,
            @PathVariable("questionId") Integer questionId) {
        try {
            boolean success = businessService.deleteBusinessQuestion(userId, questionId);
            if (success) {
                return ResponseEntity.ok(buildResponse(
                    true,
                    "Question deleted successfully",
                    null
                ));
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(buildResponse(
                    false,
                    "Not authorized to delete this question",
                    null
                ));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(buildResponse(
                false,
                e.getMessage(),
                null
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(buildResponse(
                false,
                "Error: " + e.getMessage(),
                null
            ));
        }
    }

    @PostMapping("/answer")
    public ResponseEntity<?> postBusinessAnswer(
            @RequestHeader("X-User-ID") String userId,
            @RequestBody java.util.Map<String, Object> body) {
        try {
            Integer questionId = (body.get("questionId") instanceof Number)
                ? ((Number) body.get("questionId")).intValue()
                : Integer.parseInt(body.get("questionId").toString());
            String answerText = (String) body.get("answerText");
            businessService.postBusinessAnswer(userId, questionId, answerText);
            return ResponseEntity.ok(buildResponse(
                true,
                "Answer posted successfully",
                null
            ));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(buildResponse(
                false,
                e.getMessage(),
                null
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(buildResponse(
                false,
                e.getMessage(),
                null
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(buildResponse(
                false,
                "Error: " + e.getMessage(),
                null
            ));
        }
    }

    @GetMapping("/questions/mine")
    public ResponseEntity<?> getMyQuestionsWithAnswers(@RequestHeader("X-User-ID") String userId) {
        try {
            java.util.List<java.util.Map<String, Object>> questions = businessService.getMyQuestionsWithAnswers(userId);
            return ResponseEntity.ok(questions);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(buildResponse(
                false,
                e.getMessage(),
                null
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(buildResponse(
                false,
                "Error: " + e.getMessage(),
                null
            ));
        }
    }

    @PostMapping("/{businessId}/feedback")
    public ResponseEntity<?> postBusinessFeedback(
            @RequestHeader("X-User-ID") String userId,
            @PathVariable("businessId") Integer businessId,
            @RequestBody java.util.Map<String, Object> body) {
        try {
            String feedbackText = (String) body.get("feedbackText");
            businessService.postBusinessFeedback(userId, businessId, feedbackText);
            return ResponseEntity.ok(buildResponse(
                true,
                "Feedback posted successfully",
                null
            ));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(buildResponse(
                false,
                e.getMessage(),
                null
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(buildResponse(
                false,
                e.getMessage(),
                null
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(buildResponse(
                false,
                "Error: " + e.getMessage(),
                null
            ));
        }
    }

    @PostMapping("/feedback/{feedbackId}/reply")
    public ResponseEntity<?> postBusinessFeedbackReply(
            @RequestHeader("X-User-ID") String userId,
            @PathVariable("feedbackId") Integer feedbackId,
            @RequestBody java.util.Map<String, Object> body) {
        try {
            String replyText = (String) body.get("replyText");
            businessService.postBusinessFeedbackReply(userId, feedbackId, replyText);
            return ResponseEntity.ok(buildResponse(
                true,
                "Reply posted successfully",
                null
            ));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(buildResponse(
                false,
                e.getMessage(),
                null
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(buildResponse(
                false,
                e.getMessage(),
                null
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(buildResponse(
                false,
                "Error: " + e.getMessage(),
                null
            ));
        }
    }

    @GetMapping("/{businessId}/feedback")
    public ResponseEntity<?> getBusinessFeedbacks(@RequestHeader("X-User-ID") String userId, @PathVariable("businessId") Integer businessId) {
        try {
            java.util.Map<String, Object> data = businessService.getBusinessFeedbacks(businessId, userId);
            return ResponseEntity.ok(buildResponse(
                true,
                "Operation successful",
                data
            ));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(buildResponse(
                false,
                e.getMessage(),
                null
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(buildResponse(
                false,
                e.getMessage(),
                null
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(buildResponse(
                false,
                "Error: " + e.getMessage(),
                null
            ));
        }
    }

    @DeleteMapping("/feedback/{feedbackId}")
    public ResponseEntity<?> deleteBusinessFeedback(
            @RequestHeader("X-User-ID") String userId,
            @PathVariable("feedbackId") Integer feedbackId) {
        try {
            boolean success = businessService.deleteBusinessFeedback(userId, feedbackId);
            if (success) {
                return ResponseEntity.ok(buildResponse(
                    true,
                    "Feedback deleted successfully",
                    null
                ));
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(buildResponse(
                    false,
                    "You are not authorized to delete this feedback.",
                    null
                ));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(buildResponse(
                false,
                e.getMessage(),
                null
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(buildResponse(
                false,
                "Error: " + e.getMessage(),
                null
            ));
        }
    }

    // Helper method to build response maps that allow null values
    private Map<String, Object> buildResponse(boolean success, String message, Object data) {
        Map<String, Object> map = new HashMap<>();
        map.put("success", success);
        map.put("message", message);
        map.put("data", data);
        return map;
    }
}