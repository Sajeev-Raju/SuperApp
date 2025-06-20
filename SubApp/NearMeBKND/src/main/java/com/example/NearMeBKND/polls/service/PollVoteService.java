package com.example.NearMeBKND.polls.service;

import com.example.NearMeBKND.polls.model.PollVote;
import com.example.NearMeBKND.polls.model.VoteRequest;
import com.example.NearMeBKND.polls.repository.PollVoteRepository;
import com.example.NearMeBKND.polls.repository.PollQuestionRepository;
import com.example.NearMeBKND.polls.repository.PollOptionRepository;
import com.example.NearMeBKND.nearme.repository.UserLocationRepository;
import com.example.NearMeBKND.nearme.model.UserLocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Optional;

@Service
public class PollVoteService {
    
    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double DEFAULT_RADIUS_KM = 10.0; // Fixed 10km radius
    
    @Autowired
    private PollVoteRepository pollVoteRepository;
    
    @Autowired
    private PollQuestionRepository pollQuestionRepository;
    
    @Autowired
    private PollOptionRepository pollOptionRepository;
    
    @Autowired
    private UserLocationRepository userLocationRepository;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Transactional
    public Map<String, Object> voteOnQuestion(String userId, int questionId, VoteRequest voteRequest) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 1. Validate question exists
            if (!questionExists(questionId)) {
                response.put("success", false);
                response.put("errorCode", "QUESTION_NOT_FOUND");
                response.put("message", "Poll question not found. Please check the question ID and try again.");
                response.put("questionId", questionId);
                response.put("suggestion", "Verify the question ID exists in the system.");
                return response;
            }
            
            // 2. Get user location for radius validation
            Optional<UserLocation> userLocationOpt = userLocationRepository.findByUserId(userId);
            if (userLocationOpt.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "USER_LOCATION_MISSING");
                response.put("message", "Your location is not set. Please update your location to vote on local polls.");
                response.put("userId", userId);
                response.put("suggestion", "Set your location using the location update endpoint before voting.");
                return response;
            }
            
            UserLocation userLocation = userLocationOpt.get();
            double userLat = userLocation.getLatitude();
            double userLon = userLocation.getLongitude();
            
            // 3. Get question details with collection location
            Map<String, Object> questionDetails = getQuestionDetailsWithLocation(questionId);
            Object selectionLimitObj = questionDetails.get("selectionLimit");
            Object questionOwnerIdObj = questionDetails.get("collectionOwnerId");
            Object collectionLatObj = questionDetails.get("collectionLatitude");
            Object collectionLonObj = questionDetails.get("collectionLongitude");
            Object questionTextObj = questionDetails.get("questionText");
            
            if (selectionLimitObj == null || questionOwnerIdObj == null || collectionLatObj == null || collectionLonObj == null || questionTextObj == null) {
                response.put("success", false);
                response.put("errorCode", "QUESTION_DATA_INVALID");
                response.put("message", "Poll question data is incomplete or corrupted. Please contact support.");
                response.put("questionId", questionId);
                response.put("suggestion", "Check that the poll question and its collection are valid and not missing required fields.");
                return response;
            }
            
            int selectionLimit = ((Number) selectionLimitObj).intValue();
            String questionOwnerId = (String) questionOwnerIdObj;
            double collectionLat = ((Number) collectionLatObj).doubleValue();
            double collectionLon = ((Number) collectionLonObj).doubleValue();
            String questionText = (String) questionTextObj;
            
            // 4. Validate radius restriction
            double distance = calculateDistance(userLat, userLon, collectionLat, collectionLon);
            if (distance > DEFAULT_RADIUS_KM) {
                response.put("success", false);
                response.put("errorCode", "OUTSIDE_RADIUS");
                response.put("message", "This poll is outside your voting radius. You can only vote on polls within 10km of your location.");
                response.put("distance", String.format("%.2f", distance) + "km");
                response.put("maxRadius", DEFAULT_RADIUS_KM + "km");
                response.put("userLocation", Map.of("latitude", userLat, "longitude", userLon));
                response.put("pollLocation", Map.of("latitude", collectionLat, "longitude", collectionLon));
                response.put("suggestion", "Look for polls closer to your location or update your location if you've moved.");
                return response;
            }
            
            // 5. Check if user is the question owner (owners cannot vote)
            if (userId.equals(questionOwnerId)) {
                response.put("success", false);
                response.put("errorCode", "OWNER_CANNOT_VOTE");
                response.put("message", "You cannot vote on your own poll question. Poll creators are not allowed to vote on their own questions.");
                response.put("questionId", questionId);
                response.put("questionText", questionText);
                response.put("suggestion", "You can view the results of your poll but cannot vote on it.");
                return response;
            }
            
            // 6. Validate option count
            List<Integer> optionIds = voteRequest.getOptionIds();
            if (optionIds == null || optionIds.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "NO_OPTIONS_SELECTED");
                response.put("message", "Please select at least one option to vote.");
                response.put("questionId", questionId);
                response.put("questionText", questionText);
                response.put("selectionLimit", selectionLimit);
                response.put("suggestion", "Choose one or more options based on the question's selection limit.");
                return response;
            }
            
            if (optionIds.size() > selectionLimit) {
                response.put("success", false);
                response.put("errorCode", "TOO_MANY_OPTIONS");
                response.put("message", "You have selected too many options. This question allows a maximum of " + selectionLimit + " option(s).");
                response.put("selectedCount", optionIds.size());
                response.put("maxAllowed", selectionLimit);
                response.put("questionId", questionId);
                response.put("questionText", questionText);
                response.put("suggestion", "Reduce your selection to " + selectionLimit + " option(s) or fewer.");
                return response;
            }
            
            // 7. Validate all options belong to the question
            if (!allOptionsBelongToQuestion(questionId, optionIds)) {
                response.put("success", false);
                response.put("errorCode", "INVALID_OPTIONS");
                response.put("message", "One or more selected options are not valid for this question.");
                response.put("questionId", questionId);
                response.put("questionText", questionText);
                response.put("selectedOptionIds", optionIds);
                response.put("suggestion", "Please select only valid options for this specific question.");
                return response;
            }
            
            // 8. Check if user has already voted on this question
            if (pollVoteRepository.hasUserVotedOnQuestion(userId, questionId)) {
                response.put("success", false);
                response.put("errorCode", "ALREADY_VOTED");
                response.put("message", "You have already voted on this question. Each user can only vote once per question.");
                response.put("questionId", questionId);
                response.put("questionText", questionText);
                response.put("userId", userId);
                response.put("suggestion", "You can view the current results or check your previous vote.");
                return response;
            }
            
            // 9. Record votes
            String createdAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            
            for (Integer optionId : optionIds) {
                PollVote vote = new PollVote();
                vote.setUserId(userId);
                vote.setQuestionId(questionId);
                vote.setOptionId(optionId);
                vote.setCreatedAt(createdAt);
                
                pollVoteRepository.insertVote(vote);
            }
            
            response.put("success", true);
            response.put("message", "Your vote has been recorded successfully!");
            response.put("questionId", questionId);
            response.put("questionText", questionText);
            response.put("selectedOptions", optionIds);
            response.put("distance", String.format("%.2f", distance) + "km");
            response.put("votedAt", createdAt);
            response.put("selectionMode", optionIds.size() == 1 ? "SINGLE_CHOICE" : "MULTIPLE_CHOICE");
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("errorCode", "INTERNAL_ERROR");
            response.put("message", "An unexpected error occurred while processing your vote. Please try again later.");
            response.put("questionId", questionId);
            response.put("userId", userId);
            response.put("suggestion", "If the problem persists, please contact support.");
            // Log the actual error for debugging
            System.err.println("Error in voteOnQuestion: " + e.getMessage());
            e.printStackTrace();
        }
        
        return response;
    }
    
    public Map<String, Object> getQuestionResults(String userId, int questionId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 1. Validate question exists
            if (!questionExists(questionId)) {
                response.put("success", false);
                response.put("errorCode", "QUESTION_NOT_FOUND");
                response.put("message", "Poll question not found. Please check the question ID and try again.");
                response.put("questionId", questionId);
                response.put("suggestion", "Verify the question ID exists in the system.");
                return response;
            }
            
            // 2. Get question details with location
            Map<String, Object> questionDetails = getQuestionDetailsWithLocation(questionId);
            Object questionOwnerIdObj = questionDetails.get("collectionOwnerId");
            Object collectionLatObj = questionDetails.get("collectionLatitude");
            Object collectionLonObj = questionDetails.get("collectionLongitude");
            
            if (questionOwnerIdObj == null || collectionLatObj == null || collectionLonObj == null) {
                response.put("success", false);
                response.put("errorCode", "QUESTION_DATA_INVALID");
                response.put("message", "Poll question data is incomplete or corrupted. Please contact support.");
                response.put("questionId", questionId);
                response.put("suggestion", "Check that the poll question and its collection are valid and not missing required fields.");
                return response;
            }
            
            String questionOwnerId = (String) questionOwnerIdObj;
            double collectionLat = ((Number) collectionLatObj).doubleValue();
            double collectionLon = ((Number) collectionLonObj).doubleValue();
            
            // 3. Check if user is the owner
            boolean isOwner = userId.equals(questionOwnerId);
            
            // 4. If not owner, validate radius restriction
            if (!isOwner) {
                // Get user location
                Optional<UserLocation> userLocationOpt = userLocationRepository.findByUserId(userId);
                if (userLocationOpt.isEmpty()) {
                    response.put("success", false);
                    response.put("errorCode", "USER_LOCATION_MISSING");
                    response.put("message", "Your location is not set. Please update your location to view poll results.");
                    response.put("userId", userId);
                    response.put("suggestion", "Set your location using the location update endpoint before viewing results.");
                    return response;
                }
                
                UserLocation userLocation = userLocationOpt.get();
                double userLat = userLocation.getLatitude();
                double userLon = userLocation.getLongitude();
                
                // Calculate distance and check radius restriction
                double distance = calculateDistance(userLat, userLon, collectionLat, collectionLon);
                if (distance > DEFAULT_RADIUS_KM) {
                    response.put("success", false);
                    response.put("errorCode", "OUTSIDE_RADIUS");
                    response.put("message", "This poll is outside your viewing radius. You can only view results of polls within 10km of your location.");
                    response.put("distance", String.format("%.2f", distance) + "km");
                    response.put("maxRadius", DEFAULT_RADIUS_KM + "km");
                    response.put("userLocation", Map.of("latitude", userLat, "longitude", userLon));
                    response.put("pollLocation", Map.of("latitude", collectionLat, "longitude", collectionLon));
                    response.put("suggestion", "Look for polls closer to your location or update your location if you've moved.");
                    return response;
                }
            }
            
            // 5. Get all options for the question with vote counts
            String sql = """
                SELECT po.option_id, po.option_text, 
                       COALESCE(vote_counts.vote_count, 0) as vote_count
                FROM poll_options po
                LEFT JOIN (
                    SELECT option_id, COUNT(*) as vote_count 
                    FROM poll_votes 
                    WHERE question_id = ? 
                    GROUP BY option_id
                ) vote_counts ON po.option_id = vote_counts.option_id
                WHERE po.question_id = ?
                ORDER BY po.option_id
            """;
            
            List<Map<String, Object>> options = jdbcTemplate.queryForList(sql, questionId, questionId);
            
            response.put("success", true);
            response.put("questionId", questionId);
            response.put("questionText", questionDetails.get("questionText"));
            response.put("selectionLimit", questionDetails.get("selectionLimit"));
            response.put("selectionMode", questionDetails.get("selectionMode"));
            response.put("collectionLatitude", questionDetails.get("collectionLatitude"));
            response.put("collectionLongitude", questionDetails.get("collectionLongitude"));
            response.put("options", options);
            response.put("totalVotes", options.stream().mapToInt(opt -> (Integer) opt.get("vote_count")).sum());
            response.put("isOwner", isOwner);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("errorCode", "INTERNAL_ERROR");
            response.put("message", "An error occurred while retrieving poll results. Please try again later.");
            response.put("questionId", questionId);
            response.put("suggestion", "If the problem persists, please contact support.");
            System.err.println("Error in getQuestionResults: " + e.getMessage());
            e.printStackTrace();
        }
        
        return response;
    }
    
    public Map<String, Object> getUserVote(String userId, int questionId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 1. Validate question exists
            if (!questionExists(questionId)) {
                response.put("success", false);
                response.put("errorCode", "QUESTION_NOT_FOUND");
                response.put("message", "Poll question not found. Please check the question ID and try again.");
                response.put("questionId", questionId);
                response.put("suggestion", "Verify the question ID exists in the system.");
                return response;
            }
            
            // 2. Get question details with location
            Map<String, Object> questionDetails = getQuestionDetailsWithLocation(questionId);
            Object questionOwnerIdObj = questionDetails.get("collectionOwnerId");
            Object collectionLatObj = questionDetails.get("collectionLatitude");
            Object collectionLonObj = questionDetails.get("collectionLongitude");
            
            if (questionOwnerIdObj == null || collectionLatObj == null || collectionLonObj == null) {
                response.put("success", false);
                response.put("errorCode", "QUESTION_DATA_INVALID");
                response.put("message", "Poll question data is incomplete or corrupted. Please contact support.");
                response.put("questionId", questionId);
                response.put("suggestion", "Check that the poll question and its collection are valid and not missing required fields.");
                return response;
            }
            
            String questionOwnerId = (String) questionOwnerIdObj;
            double collectionLat = ((Number) collectionLatObj).doubleValue();
            double collectionLon = ((Number) collectionLonObj).doubleValue();
            
            // 3. Check if user is the owner
            boolean isOwner = userId.equals(questionOwnerId);
            
            // 4. If not owner, validate radius restriction
            if (!isOwner) {
                // Get user location
                Optional<UserLocation> userLocationOpt = userLocationRepository.findByUserId(userId);
                if (userLocationOpt.isEmpty()) {
                    response.put("success", false);
                    response.put("errorCode", "USER_LOCATION_MISSING");
                    response.put("message", "Your location is not set. Please update your location to view your vote.");
                    response.put("userId", userId);
                    response.put("suggestion", "Set your location using the location update endpoint before viewing your vote.");
                    return response;
                }
                
                UserLocation userLocation = userLocationOpt.get();
                double userLat = userLocation.getLatitude();
                double userLon = userLocation.getLongitude();
                
                // Calculate distance and check radius restriction
                double distance = calculateDistance(userLat, userLon, collectionLat, collectionLon);
                if (distance > DEFAULT_RADIUS_KM) {
                    response.put("success", false);
                    response.put("errorCode", "OUTSIDE_RADIUS");
                    response.put("message", "This poll is outside your viewing radius. You can only view your vote for polls within 10km of your location.");
                    response.put("distance", String.format("%.2f", distance) + "km");
                    response.put("maxRadius", DEFAULT_RADIUS_KM + "km");
                    response.put("userLocation", Map.of("latitude", userLat, "longitude", userLon));
                    response.put("pollLocation", Map.of("latitude", collectionLat, "longitude", collectionLon));
                    response.put("suggestion", "Look for polls closer to your location or update your location if you've moved.");
                    return response;
                }
            }
            
            // 5. Check if user has voted
            if (!pollVoteRepository.hasUserVotedOnQuestion(userId, questionId)) {
                response.put("success", false);
                response.put("errorCode", "NO_VOTE_FOUND");
                response.put("message", "You have not voted on this question yet.");
                response.put("questionId", questionId);
                response.put("userId", userId);
                response.put("suggestion", "You can vote on this question if you haven't already, or view the current results.");
                return response;
            }
            
            // 6. Get user's voted options
            List<Integer> votedOptionIds = pollVoteRepository.getVotedOptionIds(userId, questionId);
            
            response.put("success", true);
            response.put("questionId", questionId);
            response.put("userId", userId);
            response.put("votedOptionIds", votedOptionIds);
            response.put("voteCount", votedOptionIds.size());
            response.put("isOwner", isOwner);
            response.put("message", "Your vote has been found successfully.");
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("errorCode", "INTERNAL_ERROR");
            response.put("message", "An error occurred while retrieving your vote. Please try again later.");
            response.put("questionId", questionId);
            response.put("userId", userId);
            response.put("suggestion", "If the problem persists, please contact support.");
            System.err.println("Error in getUserVote: " + e.getMessage());
            e.printStackTrace();
        }
        
        return response;
    }
    
    public Map<String, Object> getAllUserVotes(String userId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 1. Get user location for radius validation
            Optional<UserLocation> userLocationOpt = userLocationRepository.findByUserId(userId);
            if (userLocationOpt.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "USER_LOCATION_MISSING");
                response.put("message", "Your location is not set. Please update your location to view your votes.");
                response.put("userId", userId);
                response.put("suggestion", "Set your location using the location update endpoint before viewing your votes.");
                return response;
            }
            
            UserLocation userLocation = userLocationOpt.get();
            double userLat = userLocation.getLatitude();
            double userLon = userLocation.getLongitude();
            
            // 2. Get all questions the user has voted on with complete details
            String sql = """
                SELECT DISTINCT 
                    pq.question_id,
                    pq.question_text,
                    pq.selection_limit,
                    pq.selection_mode,
                    pc.collection_title,
                    pc.user_id AS collection_owner_id,
                    pc.latitude AS collection_latitude,
                    pc.longitude AS collection_longitude,
                    pc.created_at AS collection_created_at,
                    (
                        6371 * acos(
                            cos(radians(?)) * cos(radians(pc.latitude)) * 
                            cos(radians(pc.longitude) - radians(?)) + 
                            sin(radians(?)) * sin(radians(pc.latitude))
                        )
                    ) as distance
                FROM poll_votes pv
                JOIN poll_questions pq ON pv.question_id = pq.question_id
                JOIN poll_collection pc ON pq.collection_id = pc.collection_id
                WHERE pv.user_id = ?
                ORDER BY pv.created_at DESC
            """;
            
            List<Map<String, Object>> votedQuestions = jdbcTemplate.queryForList(sql, 
                userLat, userLon, userLat, userId);
            
            // 3. For each question, get all options and user's votes
            List<Map<String, Object>> detailedVotes = new ArrayList<>();
            
            for (Map<String, Object> question : votedQuestions) {
                int questionId = (Integer) question.get("question_id");
                String collectionOwnerId = (String) question.get("collection_owner_id");
                double collectionLat = (Double) question.get("collection_latitude");
                double collectionLon = (Double) question.get("collection_longitude");
                double distance = (Double) question.get("distance");
                
                // Check if user is owner or within radius
                boolean isOwner = userId.equals(collectionOwnerId);
                boolean withinRadius = isOwner || distance <= DEFAULT_RADIUS_KM;
                
                if (withinRadius) {
                    // Get all options for this question
                    String optionsSql = """
                        SELECT po.option_id, po.option_text,
                               CASE WHEN pv.user_id IS NOT NULL THEN 1 ELSE 0 END as user_voted_for_this_option
                        FROM poll_options po
                        LEFT JOIN poll_votes pv ON po.option_id = pv.option_id AND pv.user_id = ? AND pv.question_id = ?
                        WHERE po.question_id = ?
                        ORDER BY po.option_id
                    """;
                    
                    List<Map<String, Object>> options = jdbcTemplate.queryForList(optionsSql, userId, questionId, questionId);
                    
                    // Get user's voted option IDs
                    List<Integer> userVotedOptionIds = pollVoteRepository.getVotedOptionIds(userId, questionId);
                    
                    // Create detailed question object
                    Map<String, Object> detailedQuestion = new HashMap<>();
                    detailedQuestion.put("questionId", questionId);
                    detailedQuestion.put("questionText", question.get("question_text"));
                    detailedQuestion.put("selectionLimit", question.get("selection_limit"));
                    detailedQuestion.put("selectionMode", question.get("selection_mode"));
                    detailedQuestion.put("collectionTitle", question.get("collection_title"));
                    detailedQuestion.put("collectionOwnerId", collectionOwnerId);
                    detailedQuestion.put("isOwner", isOwner);
                    detailedQuestion.put("distance", String.format("%.2f", distance) + "km");
                    detailedQuestion.put("collectionLatitude", collectionLat);
                    detailedQuestion.put("collectionLongitude", collectionLon);
                    detailedQuestion.put("collectionCreatedAt", question.get("collection_created_at"));
                    detailedQuestion.put("allOptions", options);
                    detailedQuestion.put("userVotedOptionIds", userVotedOptionIds);
                    detailedQuestion.put("userVoteCount", userVotedOptionIds.size());
                    
                    detailedVotes.add(detailedQuestion);
                }
            }
            
            response.put("success", true);
            response.put("userId", userId);
            response.put("userLocation", Map.of("latitude", userLat, "longitude", userLon));
            response.put("totalVotedQuestions", detailedVotes.size());
            response.put("votes", detailedVotes);
            response.put("message", "Your voting history has been retrieved successfully.");
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("errorCode", "INTERNAL_ERROR");
            response.put("message", "An error occurred while retrieving your voting history. Please try again later.");
            response.put("userId", userId);
            response.put("suggestion", "If the problem persists, please contact support.");
            System.err.println("Error in getAllUserVotes: " + e.getMessage());
            e.printStackTrace();
        }
        
        return response;
    }
    
    private boolean questionExists(int questionId) {
        String sql = "SELECT COUNT(*) FROM poll_questions WHERE question_id = ?";
        int count = jdbcTemplate.queryForObject(sql, Integer.class, questionId);
        return count > 0;
    }
    
    private Map<String, Object> getQuestionDetailsWithLocation(int questionId) {
        String sql = """
            SELECT pq.question_id, pq.question_text AS questionText, pq.selection_limit AS selectionLimit, pq.selection_mode AS selectionMode,
                   pc.user_id AS collectionOwnerId, pc.latitude AS collectionLatitude, 
                   pc.longitude AS collectionLongitude
            FROM poll_questions pq
            JOIN poll_collection pc ON pq.collection_id = pc.collection_id
            WHERE pq.question_id = ?
        """;
        
        return jdbcTemplate.queryForMap(sql, questionId);
    }
    
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLon = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                   Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                   Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return EARTH_RADIUS_KM * c;
    }
    
    private boolean allOptionsBelongToQuestion(int questionId, List<Integer> optionIds) {
        if (optionIds == null || optionIds.isEmpty()) {
            return false;
        }
        
        String sql = "SELECT COUNT(*) FROM poll_options WHERE question_id = ? AND option_id IN (" + 
                    String.join(",", optionIds.stream().map(String::valueOf).toList()) + ")";
        
        int count = jdbcTemplate.queryForObject(sql, Integer.class, questionId);
        return count == optionIds.size();
    }
} 