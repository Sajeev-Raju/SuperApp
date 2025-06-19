package com.example.NearMeBKND.Business.service;

import com.example.NearMeBKND.Business.model.bsns_Business;
import com.example.NearMeBKND.Business.repository.bsns_BusinessRepository;
import com.example.NearMeBKND.Business.Util.GoogleMapsUtil;
import com.example.NearMeBKND.Business.model.bsns_BusinessNotification;
import com.example.NearMeBKND.Business.model.bsns_BusinessQuestion;
import com.example.NearMeBKND.Business.model.bsns_BusinessAnswer;
import com.example.NearMeBKND.Business.model.bsns_BusinessFeedback;
import com.example.NearMeBKND.Business.model.bsns_BusinessFeedbackReply;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import com.example.NearMeBKND.nearme.repository.UserLocationRepository;
import com.example.NearMeBKND.nearme.model.UserLocation;
import com.example.NearMeBKND.util.LocationUtil;
import com.example.NearMeBKND.Business.dto.BusinessDTO;

@Service
public class bsns_BusinessService {
    @Autowired
    private bsns_BusinessRepository businessRepository;

    @Value("${business.radius.km:10}")
    private double businessRadiusKm;

    @Autowired
    private UserLocationRepository userLocationRepository;

    @Autowired
    private LocationUtil locationUtil;

    public void createBusiness(bsns_Business business, String userId) throws Exception {
        // Validate userId format
        if (!Pattern.matches("[A-Za-z]{3}\\d{3}", userId)) {
            throw new IllegalArgumentException("Invalid user ID format");
        }
        // Check user exists
        if (!businessRepository.userExists(userId)) {
            throw new IllegalArgumentException("User does not exist");
        }
        // Geocode if googlemapsURL is present
        if (business.getGooglemapsURL() != null && !business.getGooglemapsURL().isBlank()) {
            Double[] coords = GoogleMapsUtil.extractLatLong(business.getGooglemapsURL());
            if (coords != null) {
                business.setLatitude(coords[0]);
                business.setLongitude(coords[1]);
            }
        }
        business.setUserId(userId);
        Integer businessId = businessRepository.save(business);
        // Save tags to business_tags table
        if (business.getTags() != null && !business.getTags().isBlank()) {
            java.util.List<String> tags = java.util.Arrays.asList(business.getTags().split(","));
            businessRepository.saveTags(businessId, tags);
        }
    }

    private Integer getLastInsertedBusinessId() {
        // This is a simple way for SQLite; for other DBs, use appropriate method
        return businessRepository.getLastInsertedBusinessId();
    }

    public bsns_Business getBusinessById(Integer businessId) {
        return businessRepository.findById(businessId);
    }

    public java.util.List<bsns_Business> getAllBusinesses(String userId, int page, int size) {
        if (!businessRepository.userExists(userId)) {
            throw new IllegalArgumentException("User does not exist");
        }
        UserLocation userLocation = userLocationRepository.findByUserId(userId)
            .orElseThrow(() -> new IllegalArgumentException("User location not found"));
        int offset = page * size;
        return businessRepository.findAllPaginatedFiltered(userId, userLocation.getLatitude(), userLocation.getLongitude(), businessRadiusKm, size, offset);
    }

    public java.util.List<bsns_Business> getBusinessesByTags(String userId, String tagsParam) {
        if (!businessRepository.userExists(userId)) {
            throw new IllegalArgumentException("User does not exist");
        }
        if (tagsParam == null || tagsParam.isBlank()) {
            return java.util.Collections.emptyList();
        }
        UserLocation userLocation = userLocationRepository.findByUserId(userId)
            .orElseThrow(() -> new IllegalArgumentException("User location not found"));
        java.util.List<String> tags = java.util.Arrays.asList(tagsParam.split(","));
        java.util.List<bsns_Business> all = businessRepository.findByTags(tags);
        java.util.List<bsns_Business> filtered = new java.util.ArrayList<>();
        for (bsns_Business b : all) {
            if (b.getUserId().equals(userId) ||
                (b.getLatitude() != null && b.getLongitude() != null &&
                 locationUtil.isWithinRadius(userLocation.getLatitude(), userLocation.getLongitude(), b.getLatitude(), b.getLongitude(), businessRadiusKm))) {
                filtered.add(b);
            }
        }
        filtered.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        return filtered;
    }

    public bsns_BusinessNotification createNotification(Integer businessId, String userId, String message) {
        bsns_Business business = businessRepository.findById(businessId);
        if (business == null) {
            throw new IllegalArgumentException("Business not found");
        }
        if (!business.getUserId().equals(userId)) {
            throw new SecurityException("User is not the owner of this business");
        }
        bsns_BusinessNotification notification = new bsns_BusinessNotification();
        notification.setBusinessId(businessId);
        notification.setUserId(userId);
        notification.setMessage(message);
        businessRepository.saveNotification(notification);
        // Fetch the notification with timestamp
        return businessRepository.findLatestNotification(businessId);
    }

    public bsns_BusinessNotification getLatestNotification(Integer businessId) {
        return businessRepository.findLatestNotification(businessId);
    }

    public java.util.List<bsns_Business> getMyBusinesses(String userId) {
        if (!businessRepository.userExists(userId)) {
            throw new IllegalArgumentException("User does not exist");
        }
        UserLocation userLocation = userLocationRepository.findByUserId(userId)
            .orElseThrow(() -> new IllegalArgumentException("User location not found"));
        java.util.List<bsns_Business> all = businessRepository.findByUserId(userId);
        java.util.List<bsns_Business> filtered = new java.util.ArrayList<>();
        for (bsns_Business b : all) {
            // Always include own businesses
            filtered.add(b);
        }
        filtered.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        return filtered;
    }

    public bsns_Business getBusinessByIdWithAuth(String userId, Integer businessId) {
        if (!businessRepository.userExists(userId)) {
            throw new IllegalArgumentException("User does not exist");
        }
        UserLocation userLocation = userLocationRepository.findByUserId(userId)
            .orElseThrow(() -> new IllegalArgumentException("User location not found"));
        bsns_Business business = businessRepository.findById(businessId);
        if (business == null) {
            return null;
        }
        if (business.getUserId().equals(userId)) {
            return business;
        }
        if (business.getLatitude() == null || business.getLongitude() == null ||
            !locationUtil.isWithinRadius(userLocation.getLatitude(), userLocation.getLongitude(), business.getLatitude(), business.getLongitude(), businessRadiusKm)) {
            return null;
        }
        return business;
    }

    public java.util.List<bsns_Business> getBusinessesByIds(String userId, java.util.List<Integer> ids) {
        if (!businessRepository.userExists(userId)) {
            throw new IllegalArgumentException("User does not exist");
        }
        UserLocation userLocation = userLocationRepository.findByUserId(userId)
            .orElseThrow(() -> new IllegalArgumentException("User location not found"));
        java.util.List<bsns_Business> all = businessRepository.findByIds(ids);
        java.util.List<bsns_Business> filtered = new java.util.ArrayList<>();
        for (bsns_Business b : all) {
            if (b.getUserId().equals(userId) ||
                (b.getLatitude() != null && b.getLongitude() != null &&
                 locationUtil.isWithinRadius(userLocation.getLatitude(), userLocation.getLongitude(), b.getLatitude(), b.getLongitude(), businessRadiusKm))) {
                filtered.add(b);
            }
        }
        return filtered;
    }

    public boolean softDeleteBusiness(String userId, Integer businessId) {
        if (!businessRepository.userExists(userId)) {
            throw new IllegalArgumentException("User does not exist");
        }
        bsns_Business business = businessRepository.findById(businessId);
        if (business == null) {
            throw new IllegalArgumentException("Business not found");
        }
        if (!business.getUserId().equals(userId)) {
            return false;
        }
        int rows = businessRepository.softDeleteById(businessId, userId);
        return rows > 0;
    }

    public boolean updateBusinessImage(String userId, Integer businessId, org.springframework.web.multipart.MultipartFile imageFile) throws Exception {
        if (!businessRepository.userExists(userId)) {
            throw new IllegalArgumentException("User does not exist");
        }
        bsns_Business business = businessRepository.findById(businessId);
        if (business == null) {
            throw new IllegalArgumentException("Business not found or is inactive");
        }
        if (!business.getUserId().equals(userId)) {
            return false;
        }
        if (imageFile == null || imageFile.isEmpty()) {
            throw new IllegalArgumentException("No image file provided");
        }
        int rows = businessRepository.updateImage(businessId, userId, imageFile.getBytes());
        return rows > 0;
    }

    public boolean updateBusinessFields(String userId, Integer businessId, java.util.Map<String, Object> fields) {
        if (!businessRepository.userExists(userId)) {
            throw new IllegalArgumentException("User does not exist");
        }
        bsns_Business business = businessRepository.findById(businessId);
        if (business == null) {
            throw new IllegalArgumentException("Business not found or is inactive");
        }
        if (!business.getUserId().equals(userId)) {
            return false;
        }
        // If googlemapsURL is being updated, also update longitude and latitude
        if (fields.containsKey("googlemapsURL")) {
            String url = (String) fields.get("googlemapsURL");
            Double[] coords = GoogleMapsUtil.extractLatLong(url);
            if (coords != null) {
                fields.put("latitude", coords[0]);
                fields.put("longitude", coords[1]);
            }
        }
        int rows = businessRepository.updateBusinessFields(businessId, userId, fields);
        return rows > 0;
    }

    public void postBusinessQuestion(String userId, Integer businessId, String questionText) {
        if (!businessRepository.userExists(userId)) {
            throw new IllegalArgumentException("User does not exist");
        }
        bsns_Business business = businessRepository.findById(businessId);
        if (business == null) {
            throw new IllegalArgumentException("Business not found");
        }
        if (businessRepository.isBusinessOwner(businessId, userId)) {
            throw new SecurityException("You cannot post a question to your own business");
        }
        // Radius restriction: user can only post if business is within their radius
        UserLocation userLocation = userLocationRepository.findByUserId(userId)
            .orElseThrow(() -> new IllegalArgumentException("User location not found"));
        if (business.getLatitude() == null || business.getLongitude() == null ||
            !locationUtil.isWithinRadius(userLocation.getLatitude(), userLocation.getLongitude(), business.getLatitude(), business.getLongitude(), businessRadiusKm)) {
            throw new SecurityException("You can only post a question to businesses within your radius");
        }
        if (questionText == null || questionText.isBlank()) {
            throw new IllegalArgumentException("Question text is required");
        }
        bsns_BusinessQuestion question = new bsns_BusinessQuestion();
        question.setBusinessId(businessId);
        question.setUserId(userId);
        question.setQuestionText(questionText);
        businessRepository.saveBusinessQuestion(question);
    }

    public boolean deleteBusinessQuestion(String userId, Integer questionId) {
        if (!businessRepository.userExists(userId)) {
            throw new IllegalArgumentException("User does not exist");
        }
        bsns_BusinessQuestion question = businessRepository.findBusinessQuestionById(questionId);
        if (question == null) {
            throw new IllegalArgumentException("Question not found");
        }
        // Allow if user is the question owner
        if (userId.equals(question.getUserId())) {
            return businessRepository.deleteBusinessQuestionById(questionId) > 0;
        }
        // Allow if user is the business owner
        bsns_Business business = businessRepository.findById(question.getBusinessId());
        if (business != null && userId.equals(business.getUserId())) {
            return businessRepository.deleteBusinessQuestionById(questionId) > 0;
        }
        // Not authorized
        return false;
    }

    public void postBusinessAnswer(String userId, Integer questionId, String answerText) {
        if (!businessRepository.userExists(userId)) {
            throw new IllegalArgumentException("User does not exist");
        }
        bsns_BusinessQuestion question = businessRepository.findBusinessQuestionById(questionId);
        if (question == null) {
            throw new IllegalArgumentException("Question not found");
        }
        bsns_Business business = businessRepository.findById(question.getBusinessId());
        if (business == null || !userId.equals(business.getUserId())) {
            throw new SecurityException("You are not the owner of the business for this question.");
        }
        if (businessRepository.isQuestionAnswered(questionId)) {
            throw new IllegalArgumentException("This question has already been answered.");
        }
        if (answerText == null || answerText.isBlank()) {
            throw new IllegalArgumentException("Answer text is required");
        }
        bsns_BusinessAnswer answer = new bsns_BusinessAnswer();
        answer.setQuestionId(questionId);
        answer.setUserId(userId);
        answer.setAnswerText(answerText);
        businessRepository.saveBusinessAnswer(answer);
    }

    public java.util.List<java.util.Map<String, Object>> getMyQuestionsWithAnswers(String userId) {
        if (!businessRepository.userExists(userId)) {
            throw new IllegalArgumentException("User does not exist");
        }
        return businessRepository.findQuestionsByUserWithAnswers(userId);
    }

    public void postBusinessFeedback(String userId, Integer businessId, String feedbackText) {
        if (!businessRepository.userExists(userId)) {
            throw new IllegalArgumentException("User does not exist");
        }
        bsns_Business business = businessRepository.findById(businessId);
        if (business == null) {
            throw new IllegalArgumentException("Business not found");
        }
        if (businessRepository.isBusinessOwner(businessId, userId)) {
            throw new SecurityException("You cannot post feedback to your own business");
        }
        // Radius restriction: user can only post if business is within their radius
        UserLocation userLocation = userLocationRepository.findByUserId(userId)
            .orElseThrow(() -> new IllegalArgumentException("User location not found"));
        if (business.getLatitude() == null || business.getLongitude() == null ||
            !locationUtil.isWithinRadius(userLocation.getLatitude(), userLocation.getLongitude(), business.getLatitude(), business.getLongitude(), businessRadiusKm)) {
            throw new SecurityException("You can only post feedback to businesses within your radius");
        }
        if (feedbackText == null || feedbackText.isBlank()) {
            throw new IllegalArgumentException("Feedback text is required");
        }
        bsns_BusinessFeedback feedback = new bsns_BusinessFeedback();
        feedback.setBusinessId(businessId);
        feedback.setUserId(userId);
        feedback.setFeedbackText(feedbackText);
        businessRepository.saveBusinessFeedback(feedback);
    }

    public void postBusinessFeedbackReply(String userId, Integer feedbackId, String replyText) {
        if (!businessRepository.userExists(userId)) {
            throw new IllegalArgumentException("User does not exist.");
        }
        java.util.Map<String, Object> feedbackInfo = businessRepository.findFeedbackWithBusinessId(feedbackId);
        if (feedbackInfo == null) {
            throw new IllegalArgumentException("Feedback not found.");
        }
        String ownerId = (String) feedbackInfo.get("ownerId");
        if (!userId.equals(ownerId)) {
            throw new SecurityException("You are not authorized to reply to this feedback.");
        }
        if (businessRepository.isFeedbackReplyExists(feedbackId)) {
            throw new IllegalArgumentException("A reply already exists for this feedback.");
        }
        if (replyText == null || replyText.isBlank()) {
            throw new IllegalArgumentException("Reply text is required.");
        }
        bsns_BusinessFeedbackReply reply = new bsns_BusinessFeedbackReply();
        reply.setFeedbackId(feedbackId);
        reply.setUserId(userId);
        reply.setReplyText(replyText);
        businessRepository.saveBusinessFeedbackReply(reply);
    }

    public java.util.Map<String, Object> businessToFullMap(bsns_Business business) {
        if (business == null) return null;
        java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("businessId", business.getBusinessId());
        map.put("userId", business.getUserId() == null ? "" : business.getUserId());
        map.put("name", business.getName() == null ? "" : business.getName());
        map.put("title", business.getTitle() == null ? "" : business.getTitle());
        map.put("tags", business.getTags() == null ? "" : business.getTags());
        map.put("description", business.getDescription() == null ? "" : business.getDescription());
        map.put("googlemapsURL", business.getGooglemapsURL() == null ? "" : business.getGooglemapsURL());
        map.put("address", business.getAddress() == null ? "" : business.getAddress());
        map.put("mobileNumber", business.getMobileNumber() == null ? "" : business.getMobileNumber());
        map.put("timings", business.getTimings() == null ? "" : business.getTimings());
        map.put("createdAt", business.getCreatedAt() == null ? "" : business.getCreatedAt());
        map.put("image", "http://localhost:8080/api/business/image/" + business.getBusinessId());
        // Notification removed for performance
        map.put("active", business.getActive() == null ? false : business.getActive());
        return map;
    }

    public java.util.Map<String, Object> getBusinessFeedbacks(Integer businessId, String userId) {
        if (!businessRepository.userExists(userId)) {
            throw new IllegalArgumentException("User does not exist");
        }
        bsns_Business business = businessRepository.findById(businessId);
        if (business == null) {
            throw new IllegalArgumentException("Business not found");
        }
        boolean isOwner = business.getUserId().equals(userId);
        java.util.List<java.util.Map<String, Object>> feedbacks;
        if (isOwner) {
            feedbacks = businessRepository.findFeedbacksWithRepliesByBusinessId(businessId);
        } else {
            feedbacks = businessRepository.findFeedbacksWithRepliesByBusinessIdAndUserId(businessId, userId);
            if (feedbacks.isEmpty()) {
                throw new SecurityException("You are not authorized to view these feedbacks.");
            }
        }
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("businessId", business.getBusinessId());
        result.put("name", business.getName());
        result.put("userId", business.getUserId());
        result.put("title", business.getTitle());
        result.put("description", business.getDescription());
        result.put("feedbacks", feedbacks);
        return result;
    }

    public boolean deleteBusinessFeedback(String userId, Integer feedbackId) {
        if (!businessRepository.userExists(userId)) {
            throw new IllegalArgumentException("User does not exist.");
        }
        java.util.Map<String, Object> feedbackInfo = businessRepository.findFeedbackWithBusinessIdAndUserId(feedbackId);
        if (feedbackInfo == null) {
            throw new IllegalArgumentException("Feedback not found.");
        }
        String feedbackUserId = (String) feedbackInfo.get("feedbackUserId");
        String ownerId = (String) feedbackInfo.get("ownerId");
        if (!userId.equals(feedbackUserId) && !userId.equals(ownerId)) {
            return false;
        }
        // Explicitly delete reply first
        businessRepository.deleteBusinessFeedbackReplyByFeedbackId(feedbackId);
        int rows = businessRepository.deleteBusinessFeedbackById(feedbackId);
        return rows > 0;
    }

    public bsns_BusinessQuestion createQuestion(String userId, Integer businessId, String questionText) {
        if (!businessRepository.userExists(userId)) {
            throw new IllegalArgumentException("User does not exist");
        }
        UserLocation userLocation = userLocationRepository.findByUserId(userId)
            .orElseThrow(() -> new IllegalArgumentException("User location not found"));
        bsns_Business business = businessRepository.findById(businessId);
        if (business == null) {
            throw new IllegalArgumentException("Business not found");
        }
        if (!business.getUserId().equals(userId) &&
            (business.getLatitude() == null || business.getLongitude() == null ||
             !locationUtil.isWithinRadius(userLocation.getLatitude(), userLocation.getLongitude(), business.getLatitude(), business.getLongitude(), businessRadiusKm))) {
            throw new IllegalArgumentException("Business not accessible");
        }
        if (questionText == null || questionText.isBlank()) {
            throw new IllegalArgumentException("Question text is required");
        }
        bsns_BusinessQuestion question = new bsns_BusinessQuestion();
        question.setBusinessId(businessId);
        question.setUserId(userId);
        question.setQuestionText(questionText);
        question.setCreatedAt(new java.sql.Timestamp(System.currentTimeMillis()));
        businessRepository.saveBusinessQuestion(question);
        return question;
    }

    public bsns_BusinessFeedback createFeedback(String userId, Integer businessId, String feedbackText, Integer rating) {
        if (!businessRepository.userExists(userId)) {
            throw new IllegalArgumentException("User does not exist");
        }
        UserLocation userLocation = userLocationRepository.findByUserId(userId)
            .orElseThrow(() -> new IllegalArgumentException("User location not found"));
        bsns_Business business = businessRepository.findById(businessId);
        if (business == null) {
            throw new IllegalArgumentException("Business not found");
        }
        if (!business.getUserId().equals(userId) &&
            (business.getLatitude() == null || business.getLongitude() == null ||
             !locationUtil.isWithinRadius(userLocation.getLatitude(), userLocation.getLongitude(), business.getLatitude(), business.getLongitude(), businessRadiusKm))) {
            throw new IllegalArgumentException("Business not accessible");
        }
        if (feedbackText == null || feedbackText.isBlank()) {
            throw new IllegalArgumentException("Feedback text is required");
        }
        bsns_BusinessFeedback feedback = new bsns_BusinessFeedback();
        feedback.setBusinessId(businessId);
        feedback.setUserId(userId);
        feedback.setFeedbackText(feedbackText);
        feedback.setCreatedAt(new java.sql.Timestamp(System.currentTimeMillis()));
        businessRepository.saveBusinessFeedback(feedback);
        return feedback;
    }

    public int getTotalActiveBusinessCount() {
        return businessRepository.countAllActiveBusinesses();
    }

    public BusinessDTO toBusinessDTO(bsns_Business business) {
        if (business == null) return null;
        BusinessDTO dto = new BusinessDTO();
        dto.setBusinessId(business.getBusinessId());
        dto.setName(business.getName());
        dto.setTitle(business.getTitle());
        dto.setDescription(business.getDescription());
        dto.setAddress(business.getAddress());
        dto.setMobileNumber(business.getMobileNumber());
        dto.setTimings(business.getTimings());
        dto.setGooglemapsURL(business.getGooglemapsURL());
        dto.setImage("http://localhost:8080/api/business/image/" + business.getBusinessId());
        return dto;
    }

    public int getFilteredBusinessCount(String userId) {
        UserLocation userLocation = userLocationRepository.findByUserId(userId)
            .orElseThrow(() -> new IllegalArgumentException("User location not found"));
        return businessRepository.countAllFiltered(userId, userLocation.getLatitude(), userLocation.getLongitude(), businessRadiusKm);
    }

    public java.util.List<bsns_Business> getAllBusinessesInRadius(String userId, int page, int size) {
        UserLocation userLocation = userLocationRepository.findByUserId(userId)
            .orElseThrow(() -> new IllegalArgumentException("User location not found"));
        int offset = page * size;
        return businessRepository.findAllWithinRadiusPaged(userId, userLocation.getLatitude(), userLocation.getLongitude(), businessRadiusKm, size, offset);
    }
}