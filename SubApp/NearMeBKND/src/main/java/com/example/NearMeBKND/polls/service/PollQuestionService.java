// package com.example.NearMeBKND.polls.service;

// import com.example.NearMeBKND.polls.model.PollOption;
// import com.example.NearMeBKND.polls.model.PollQuestion;
// import com.example.NearMeBKND.polls.repository.PollQuestionRepository;
// import com.example.NearMeBKND.polls.repository.PollOptionRepository;
// import com.example.NearMeBKND.util.LocationUtil;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.jdbc.core.JdbcTemplate;
// import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Transactional;

// import java.util.List;
// import java.util.Map;
// import java.util.ArrayList;
// import java.util.stream.Collectors;

// @Service
// public class PollQuestionService {
//     private static final double RADIUS_KM = 15.0; // 15km radius restriction

//     @Autowired
//     private PollQuestionRepository pollQuestionRepository;

//     @Autowired
//     private PollOptionRepository pollOptionRepository;

//     @Autowired
//     private JdbcTemplate jdbcTemplate;

//     @Autowired
//     private LocationUtil locationUtil;

//     @Transactional
//     public int createQuestionWithPolls(String userId, String questionText, List<String> options, int selectionLimit, String selectionMode) {
//         // Get user's location from user_locations table
//         String locationSql = "SELECT latitude, longitude FROM user_locations WHERE user_id = ?";
//         List<Map<String, Object>> locations = jdbcTemplate.queryForList(locationSql, userId);
//         if (locations.isEmpty()) {
//             throw new RuntimeException("User location not found. Please set your location first.");
//         }
//         Map<String, Object> location = locations.get(0);
//         double latitude = (double) location.get("latitude");
//         double longitude = (double) location.get("longitude");

//         int questionId = pollQuestionRepository.createQuestion(userId, questionText, selectionLimit, selectionMode, latitude, longitude);
//         for (String optionText : options) {
//             pollOptionRepository.createOption(questionId, optionText);
//         }
//         return questionId;
//     }

//     public PollQuestion getQuestion(int questionId, String userId) {
//         PollQuestion question = pollQuestionRepository.getQuestion(questionId);
//         if (question != null) {
//             // Get user's location
//             String locationSql = "SELECT latitude, longitude FROM user_locations WHERE user_id = ?";
//             Map<String, Object> userLocation = jdbcTemplate.queryForMap(locationSql, userId);
//             double userLat = (double) userLocation.get("latitude");
//             double userLon = (double) userLocation.get("longitude");

//             // Check if question is within radius
//             if (!locationUtil.isWithinRadius(userLat, userLon, question.getLatitude(), question.getLongitude(), RADIUS_KM)) {
//                 return null;
//             }
//         }
//         return question;
//     }

//     public List<PollOption> getResults(int questionId, String userId) {
//         // First check if user can access this question
//         PollQuestion question = getQuestion(questionId, userId);
//         if (question == null) {
//             return new ArrayList<>();
//         }
//         return pollOptionRepository.getOptionsWithVoteCount(questionId);
//     }

//     public List<Map<String, Object>> getAllQuestionsWithOptions(String userId) {
//         // Get user's location
//         String locationSql = "SELECT latitude, longitude FROM user_locations WHERE user_id = ?";
//         Map<String, Object> userLocation = jdbcTemplate.queryForMap(locationSql, userId);
//         double userLat = (double) userLocation.get("latitude");
//         double userLon = (double) userLocation.get("longitude");

//         List<PollQuestion> allQuestions = pollQuestionRepository.getAllQuestions();
        
//         // Filter questions within radius
//         List<PollQuestion> filteredQuestions = allQuestions.stream()
//             .filter(q -> locationUtil.isWithinRadius(userLat, userLon, q.getLatitude(), q.getLongitude(), RADIUS_KM))
//             .collect(Collectors.toList());

//         List<Map<String, Object>> result = new ArrayList<>();
//         for (PollQuestion question : filteredQuestions) {
//             List<PollOption> options = pollOptionRepository.getOptionsWithVoteCount(question.getQuestionId());
//             question.setOptions(options);
//             result.add(Map.of(
//                 "questionId", question.getQuestionId(),
//                 "userId", question.getUserId(),
//                 "questionText", question.getQuestionText(),
//                 "selectionLimit", question.getSelectionLimit(),
//                 "selectionMode", question.getSelectionMode(),
//                 "latitude", question.getLatitude(),
//                 "longitude", question.getLongitude(),
//                 "options", options
//             ));
//         }
        
//         return result;
//     }

//     @Transactional
//     public boolean deleteQuestion(String userId, int questionId) {
//         PollQuestion question = pollQuestionRepository.getQuestion(questionId);
//         if (question != null && question.getUserId().equals(userId)) {
//             // First delete all votes for this question
//             String deleteVotesSql = "DELETE FROM poll_votes WHERE question_id = ?";
//             jdbcTemplate.update(deleteVotesSql, questionId);
            
//             // Then delete all options for this question
//             String deleteOptionsSql = "DELETE FROM poll_options WHERE question_id = ?";
//             jdbcTemplate.update(deleteOptionsSql, questionId);
            
//             // Finally delete the question
//             pollQuestionRepository.deleteQuestion(questionId);
//             return true;
//         }
//         return false;
//     }

//     @Transactional
//     public List<Integer> createMultipleQuestions(String userId, List<Map<String, Object>> questions) {
//         List<Integer> createdQuestionIds = new ArrayList<>();
        
//         // Get user's location from user_locations table
//         String locationSql = "SELECT latitude, longitude FROM user_locations WHERE user_id = ?";
//         List<Map<String, Object>> locations = jdbcTemplate.queryForList(locationSql, userId);
//         if (locations.isEmpty()) {
//             throw new RuntimeException("User location not found. Please set your location first.");
//         }
//         Map<String, Object> location = locations.get(0);
//         double latitude = (double) location.get("latitude");
//         double longitude = (double) location.get("longitude");

//         for (Map<String, Object> question : questions) {
//             String questionText = (String) question.get("questionText");
//             @SuppressWarnings("unchecked")
//             List<String> options = (List<String>) question.get("options");
//             int selectionLimit = (int) question.get("selectionLimit");
//             String selectionMode = (String) question.get("selectionMode");

//             int questionId = pollQuestionRepository.createQuestion(userId, questionText, selectionLimit, selectionMode, latitude, longitude);
//             for (String optionText : options) {
//                 pollOptionRepository.createOption(questionId, optionText);
//             }
//             createdQuestionIds.add(questionId);
//         }
        
//         return createdQuestionIds;
//     }

//     public List<PollQuestion> getQuestionsInRadiusPaged(double userLat, double userLon, double radiusKm, int limit, int offset) {
//         return pollQuestionRepository.getQuestionsInRadiusPaged(userLat, userLon, radiusKm, limit, offset);
//     }

//     public int countQuestionsInRadius(double userLat, double userLon, double radiusKm) {
//         return pollQuestionRepository.countQuestionsInRadius(userLat, userLon, radiusKm);
//     }

//     public PollOptionRepository getPollOptionRepository() {
//         return pollOptionRepository;
//     }
// } 