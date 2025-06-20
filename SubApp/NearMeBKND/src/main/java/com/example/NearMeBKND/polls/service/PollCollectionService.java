package com.example.NearMeBKND.polls.service;

import com.example.NearMeBKND.polls.model.CreatePollCollectionRequest;
import com.example.NearMeBKND.polls.model.PaginatedResponse;
import com.example.NearMeBKND.polls.model.PollOption;
import com.example.NearMeBKND.polls.model.PollQuestion;
import com.example.NearMeBKND.polls.repository.PollQuestionRepository;
import com.example.NearMeBKND.polls.repository.PollOptionRepository;
import com.example.NearMeBKND.polls.repository.PollCollectionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.LinkedHashMap;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

@Service
public class PollCollectionService {
    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double DEFAULT_RADIUS_KM = 10.0; // Fixed 10km radius

    @Autowired
    private PollQuestionRepository pollQuestionRepository;
    @Autowired
    private PollOptionRepository pollOptionRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PollCollectionRepository pollCollectionRepository;

    @Transactional
    public int createPollCollection(String userId, double latitude, double longitude, CreatePollCollectionRequest request) {
        // 1. Insert poll collection with createdAt and location info
        String createdAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        int collectionId = pollCollectionRepository.insertPollCollection(
            request.getCollectionTitle(), 
            createdAt,
            userId,
            latitude,
            longitude
        );

        // 2. Insert each question (max 5)
        List<CreatePollCollectionRequest.CreatePollQuestionRequest> questions = request.getQuestions();
        if (questions.size() > 5) {
            throw new IllegalArgumentException("Cannot create more than 5 questions per collection");
        }
        for (CreatePollCollectionRequest.CreatePollQuestionRequest q : questions) {
            PollQuestion question = new PollQuestion();
            question.setQuestionText(q.getQuestionText());
            question.setSelectionLimit(q.getSelectionLimit());
            question.setSelectionMode(q.getSelectionLimit() == 1 ? "SINGLE" : "MULTIPLE");
            question.setCollectionId(collectionId);
            int questionId = pollQuestionRepository.insertPollQuestion(question);
            // Insert options
            for (String optionText : q.getPollOptions()) {
                PollOption option = new PollOption();
                option.setQuestionId(questionId);
                option.setOptionText(optionText);
                pollOptionRepository.insertPollOption(option);
            }
        }
        return collectionId;
    }

    public PaginatedResponse getPaginatedPollCollections(String userId, double userLat, double userLon, int page, int size) {
        int offset = page * size;
        
        // Calculate bounding box for the fixed radius
        double latDelta = DEFAULT_RADIUS_KM / EARTH_RADIUS_KM * (180.0 / Math.PI);
        double lonDelta = DEFAULT_RADIUS_KM / (EARTH_RADIUS_KM * Math.cos(Math.toRadians(userLat))) * (180.0 / Math.PI);
        
        double minLat = userLat - latDelta;
        double maxLat = userLat + latDelta;
        double minLon = userLon - lonDelta;
        double maxLon = userLon + lonDelta;

        // Get total count of collections within radius
        String countSql = """
            SELECT COUNT(DISTINCT pc.collection_id) 
            FROM poll_collection pc
            WHERE pc.latitude BETWEEN ? AND ?
            AND pc.longitude BETWEEN ? AND ?
            AND (
                6371 * acos(
                    cos(radians(?)) * cos(radians(pc.latitude)) * 
                    cos(radians(pc.longitude) - radians(?)) + 
                    sin(radians(?)) * sin(radians(pc.latitude))
                )
            ) <= ?
        """;
        int totalElements = jdbcTemplate.queryForObject(countSql, Integer.class, 
            minLat, maxLat, minLon, maxLon, userLat, userLon, userLat, DEFAULT_RADIUS_KM);

        // Get collections within radius ordered by poll_collection.created_at
        String sql = """
            SELECT pc.collection_id, pc.collection_title, pc.created_at, pc.user_id,
                   pc.latitude, pc.longitude,
                   (
                       6371 * acos(
                           cos(radians(?)) * cos(radians(pc.latitude)) * 
                           cos(radians(pc.longitude) - radians(?)) + 
                           sin(radians(?)) * sin(radians(pc.latitude))
                       )
                   ) as distance
            FROM poll_collection pc
            WHERE pc.latitude BETWEEN ? AND ?
            AND pc.longitude BETWEEN ? AND ?
            AND (
                6371 * acos(
                    cos(radians(?)) * cos(radians(pc.latitude)) * 
                    cos(radians(pc.longitude) - radians(?)) + 
                    sin(radians(?)) * sin(radians(pc.latitude))
                )
            ) <= ?
            ORDER BY pc.created_at DESC
            LIMIT ? OFFSET ?
        """;
        List<Map<String, Object>> collections = jdbcTemplate.query(sql, 
            new Object[]{
                userLat, userLon, userLat,  // For distance calculation
                minLat, maxLat, minLon, maxLon,  // For bounding box
                userLat, userLon, userLat, DEFAULT_RADIUS_KM,  // For radius check
                size, offset  // For pagination
            }, 
            (rs, rowNum) -> {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("collectionId", rs.getInt("collection_id"));
                map.put("collectionTitle", rs.getString("collection_title"));
                map.put("createdAt", rs.getString("created_at"));
                map.put("userId", rs.getString("user_id"));
                map.put("latitude", rs.getDouble("latitude"));
                map.put("longitude", rs.getDouble("longitude"));
                map.put("distance", rs.getDouble("distance"));
                return map;
            });

        // For each collection, get all question_texts
        for (Map<String, Object> collection : collections) {
            int collectionId = (int) collection.get("collectionId");
            List<String> questions = jdbcTemplate.query(
                "SELECT question_text FROM poll_questions WHERE collection_id = ? ORDER BY question_id ASC",
                (rs, rowNum) -> rs.getString("question_text"),
                collectionId
            );
            collection.put("questions", questions);
        }

        return new PaginatedResponse(collections, page, size, totalElements);
    }

    public boolean isUserCollectionOwner(String userId, int id, boolean isQuestionId) {
        String sql;
        if (isQuestionId) {
            sql = """
                SELECT pc.user_id 
                FROM poll_collection pc
                JOIN poll_questions pq ON pc.collection_id = pq.collection_id
                WHERE pq.question_id = ?
            """;
        } else {
            sql = "SELECT user_id FROM poll_collection WHERE collection_id = ?";
        }
        
        try {
            String ownerId = jdbcTemplate.queryForObject(sql, String.class, id);
            System.out.println("[DEBUG] Comparing: header='" + userId + "', db='" + ownerId + "'");
            return userId != null && ownerId != null &&
                   userId.trim().equalsIgnoreCase(ownerId.trim());
        } catch (Exception e) {
            System.out.println("[DEBUG] Exception in isUserCollectionOwner: " + e.getMessage());
            return false;
        }
    }

    @Transactional
    public void deletePollQuestion(int questionId) {
        System.out.println("[DEBUG] Starting deletePollQuestion for questionId: " + questionId);
        
        try {
            // First, get the collection_id for this question
            String getCollectionSql = "SELECT collection_id FROM poll_questions WHERE question_id = ?";
            Integer collectionId = jdbcTemplate.queryForObject(getCollectionSql, Integer.class, questionId);
            
            if (collectionId != null) {
                System.out.println("[DEBUG] Found collectionId: " + collectionId + " for questionId: " + questionId);
                
                // 1. Delete all votes for this question first
                try {
                    String deleteVotesSql = "DELETE FROM poll_votes WHERE question_id = ?";
                    int deletedVotes = jdbcTemplate.update(deleteVotesSql, questionId);
                    System.out.println("[DEBUG] Deleted " + deletedVotes + " votes for questionId: " + questionId);
                } catch (Exception e) {
                    System.err.println("[ERROR] Failed to delete votes for questionId " + questionId + ": " + e.getMessage());
                    throw e;
                }
                
                // 2. Delete all options for this question
                try {
                    String deleteOptionsSql = "DELETE FROM poll_options WHERE question_id = ?";
                    int deletedOptions = jdbcTemplate.update(deleteOptionsSql, questionId);
                    System.out.println("[DEBUG] Deleted " + deletedOptions + " options for questionId: " + questionId);
                } catch (Exception e) {
                    System.err.println("[ERROR] Failed to delete options for questionId " + questionId + ": " + e.getMessage());
                    throw e;
                }
                
                // 3. Delete the question
                try {
                    String deleteQuestionSql = "DELETE FROM poll_questions WHERE question_id = ?";
                    int deletedQuestions = jdbcTemplate.update(deleteQuestionSql, questionId);
                    System.out.println("[DEBUG] Deleted " + deletedQuestions + " questions for questionId: " + questionId);
                } catch (Exception e) {
                    System.err.println("[ERROR] Failed to delete question " + questionId + ": " + e.getMessage());
                    throw e;
                }
                
                // 4. Check if this was the last question in the collection
                try {
                    String countQuestionsSql = "SELECT COUNT(*) FROM poll_questions WHERE collection_id = ?";
                    int remainingQuestions = jdbcTemplate.queryForObject(countQuestionsSql, Integer.class, collectionId);
                    System.out.println("[DEBUG] Remaining questions in collection " + collectionId + ": " + remainingQuestions);
                    
                    // 5. If no questions left, delete the collection
                    if (remainingQuestions == 0) {
                        String deleteCollectionSql = "DELETE FROM poll_collection WHERE collection_id = ?";
                        int deletedCollections = jdbcTemplate.update(deleteCollectionSql, collectionId);
                        System.out.println("[DEBUG] Deleted " + deletedCollections + " collections for collectionId: " + collectionId);
                    }
                } catch (Exception e) {
                    System.err.println("[ERROR] Failed to check/delete collection " + collectionId + ": " + e.getMessage());
                    throw e;
                }
            } else {
                System.out.println("[DEBUG] No collection found for questionId: " + questionId);
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Exception in deletePollQuestion for questionId " + questionId + ": " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Transactional
    public void deletePollCollection(int collectionId) {
        System.out.println("[DEBUG] Starting deletePollCollection for collectionId: " + collectionId);
        
        try {
            // Delete in the correct order to respect foreign key constraints:
            // 1. Delete all votes for all questions in this collection
            try {
                String deleteVotesSql = """
                    DELETE FROM poll_votes 
                    WHERE question_id IN (
                        SELECT question_id FROM poll_questions WHERE collection_id = ?
                    )
                """;
                int deletedVotes = jdbcTemplate.update(deleteVotesSql, collectionId);
                System.out.println("[DEBUG] Deleted " + deletedVotes + " votes for collectionId: " + collectionId);
            } catch (Exception e) {
                System.err.println("[ERROR] Failed to delete votes for collectionId " + collectionId + ": " + e.getMessage());
                throw e;
            }
            
            // 2. Delete all poll options for all questions in this collection
            try {
                String deleteOptionsSql = """
                    DELETE FROM poll_options 
                    WHERE question_id IN (
                        SELECT question_id FROM poll_questions WHERE collection_id = ?
                    )
                """;
                int deletedOptions = jdbcTemplate.update(deleteOptionsSql, collectionId);
                System.out.println("[DEBUG] Deleted " + deletedOptions + " options for collectionId: " + collectionId);
            } catch (Exception e) {
                System.err.println("[ERROR] Failed to delete options for collectionId " + collectionId + ": " + e.getMessage());
                throw e;
            }
            
            // 3. Delete all questions in this collection
            try {
                String deleteQuestionsSql = "DELETE FROM poll_questions WHERE collection_id = ?";
                int deletedQuestions = jdbcTemplate.update(deleteQuestionsSql, collectionId);
                System.out.println("[DEBUG] Deleted " + deletedQuestions + " questions for collectionId: " + collectionId);
            } catch (Exception e) {
                System.err.println("[ERROR] Failed to delete questions for collectionId " + collectionId + ": " + e.getMessage());
                throw e;
            }
            
            // 4. Finally delete the collection
            try {
                String deleteCollectionSql = "DELETE FROM poll_collection WHERE collection_id = ?";
                int deletedCollections = jdbcTemplate.update(deleteCollectionSql, collectionId);
                System.out.println("[DEBUG] Deleted " + deletedCollections + " collections for collectionId: " + collectionId);
            } catch (Exception e) {
                System.err.println("[ERROR] Failed to delete collection " + collectionId + ": " + e.getMessage());
                throw e;
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Exception in deletePollCollection for collectionId " + collectionId + ": " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public Map<String, Object> getPollCollectionDetails(String userId, int collectionId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            System.out.println("[DEBUG] getPollCollectionDetails called | userId='" + userId + "' | collectionId=" + collectionId);
            
            // 1. Get user location for radius validation
            String userLocationSql = "SELECT latitude, longitude FROM user_locations WHERE user_id = ?";
            Map<String, Object> userLocation;
            try {
                userLocation = jdbcTemplate.queryForMap(userLocationSql, userId);
            } catch (Exception e) {
                System.out.println("[DEBUG] User location not found for userId: " + userId);
                response.put("success", false);
                response.put("errorCode", "USER_LOCATION_MISSING");
                response.put("message", "Your location is not set. Please update your location to view poll collections.");
                response.put("userId", userId);
                response.put("suggestion", "Set your location using the location update endpoint before viewing collections.");
                return response;
            }
            
            double userLat = (Double) userLocation.get("latitude");
            double userLon = (Double) userLocation.get("longitude");
            System.out.println("[DEBUG] User location found | lat=" + userLat + " | lon=" + userLon);
            
            // 2. Get collection details with distance calculation
            String collectionSql = """
                SELECT collection_id, collection_title, user_id AS collection_owner_id, 
                       latitude, longitude, created_at,
                       (
                           6371 * acos(
                               cos(radians(?)) * cos(radians(latitude)) * 
                               cos(radians(longitude) - radians(?)) + 
                               sin(radians(?)) * sin(radians(latitude))
                           )
                       ) as distance
                FROM poll_collection 
                WHERE collection_id = ?
            """;
            
            Map<String, Object> collectionDetails;
            try {
                collectionDetails = jdbcTemplate.queryForMap(collectionSql, userLat, userLon, userLat, collectionId);
            } catch (Exception e) {
                System.out.println("[DEBUG] Collection not found for collectionId: " + collectionId);
                response.put("success", false);
                response.put("errorCode", "COLLECTION_NOT_FOUND");
                response.put("message", "Poll collection not found. Please check the collection ID and try again.");
                response.put("collectionId", collectionId);
                response.put("suggestion", "Verify the collection ID exists in the system.");
                return response;
            }
            
            String collectionOwnerId = (String) collectionDetails.get("collection_owner_id");
            double collectionLat = (Double) collectionDetails.get("latitude");
            double collectionLon = (Double) collectionDetails.get("longitude");
            double distance = (Double) collectionDetails.get("distance");
            
            System.out.println("[DEBUG] Collection found | ownerId='" + collectionOwnerId + "' | distance=" + distance + "km");
            
            // 3. Check if user is owner or within radius
            boolean isOwner = userId.equals(collectionOwnerId);
            boolean withinRadius = isOwner || distance <= DEFAULT_RADIUS_KM;
            
            System.out.println("[DEBUG] Access check | isOwner=" + isOwner + " | withinRadius=" + withinRadius);
            
            if (!withinRadius) {
                response.put("success", false);
                response.put("errorCode", "OUTSIDE_RADIUS");
                response.put("message", "This poll collection is outside your viewing radius. You can only view collections within 10km of your location.");
                response.put("distance", String.format("%.2f", distance) + "km");
                response.put("maxRadius", DEFAULT_RADIUS_KM + "km");
                response.put("userLocation", Map.of("latitude", userLat, "longitude", userLon));
                response.put("collectionLocation", Map.of("latitude", collectionLat, "longitude", collectionLon));
                response.put("suggestion", "Look for collections closer to your location or update your location if you've moved.");
                return response;
            }
            
            // 4. Get all questions for this collection
            String questionsSql = """
                SELECT question_id, question_text, selection_limit, selection_mode
                FROM poll_questions 
                WHERE collection_id = ?
                ORDER BY question_id
            """;
            
            List<Map<String, Object>> questions = jdbcTemplate.queryForList(questionsSql, collectionId);
            System.out.println("[DEBUG] Found " + questions.size() + " questions for collection " + collectionId);
            
            // 5. For each question, get all options with vote counts
            List<Map<String, Object>> detailedQuestions = new ArrayList<>();
            
            for (Map<String, Object> question : questions) {
                int questionId = (Integer) question.get("question_id");
                
                // Get options with vote counts
                String optionsSql = """
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
                
                List<Map<String, Object>> options = jdbcTemplate.queryForList(optionsSql, questionId, questionId);
                
                // Calculate total votes for this question
                int totalQuestionVotes = options.stream()
                    .mapToInt(opt -> (Integer) opt.get("vote_count"))
                    .sum();
                
                // Create detailed question object
                Map<String, Object> detailedQuestion = new HashMap<>();
                detailedQuestion.put("questionId", questionId);
                detailedQuestion.put("questionText", question.get("question_text"));
                detailedQuestion.put("selectionLimit", question.get("selection_limit"));
                detailedQuestion.put("selectionMode", question.get("selection_mode"));
                detailedQuestion.put("options", options);
                detailedQuestion.put("totalVotes", totalQuestionVotes);
                
                detailedQuestions.add(detailedQuestion);
                
                System.out.println("[DEBUG] Question " + questionId + " | options=" + options.size() + " | totalVotes=" + totalQuestionVotes);
            }
            
            // 6. Calculate total votes across all questions
            int totalCollectionVotes = detailedQuestions.stream()
                .mapToInt(q -> (Integer) q.get("totalVotes"))
                .sum();
            
            // 7. Build final response
            Map<String, Object> collectionInfo = new HashMap<>();
            collectionInfo.put("collectionId", collectionId);
            collectionInfo.put("collectionTitle", collectionDetails.get("collection_title"));
            collectionInfo.put("collectionOwnerId", collectionOwnerId);
            collectionInfo.put("isOwner", isOwner);
            collectionInfo.put("distance", String.format("%.2f", distance) + "km");
            collectionInfo.put("latitude", collectionLat);
            collectionInfo.put("longitude", collectionLon);
            collectionInfo.put("createdAt", collectionDetails.get("created_at"));
            collectionInfo.put("questions", detailedQuestions);
            collectionInfo.put("totalQuestions", detailedQuestions.size());
            collectionInfo.put("totalVotes", totalCollectionVotes);
            
            response.put("success", true);
            response.put("collection", collectionInfo);
            response.put("userLocation", Map.of("latitude", userLat, "longitude", userLon));
            response.put("message", "Poll collection details retrieved successfully.");
            
            System.out.println("[DEBUG] Successfully retrieved collection details | totalQuestions=" + detailedQuestions.size() + " | totalVotes=" + totalCollectionVotes);
            
        } catch (Exception e) {
            System.err.println("[ERROR] Exception in getPollCollectionDetails: " + e.getMessage());
            e.printStackTrace();
            
            response.put("success", false);
            response.put("errorCode", "INTERNAL_ERROR");
            response.put("message", "An error occurred while retrieving poll collection details. Please try again later.");
            response.put("collectionId", collectionId);
            response.put("userId", userId);
            response.put("suggestion", "If the problem persists, please contact support.");
        }
        
        return response;
    }
} 