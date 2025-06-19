package com.example.NearMeBKND.Business.repository;

import com.example.NearMeBKND.Business.model.bsns_Business;
import com.example.NearMeBKND.Business.model.bsns_BusinessTag;
import com.example.NearMeBKND.Business.model.bsns_BusinessNotification;
import com.example.NearMeBKND.Business.model.bsns_BusinessQuestion;
import com.example.NearMeBKND.Business.model.bsns_BusinessAnswer;
import com.example.NearMeBKND.Business.model.bsns_BusinessFeedback;
import com.example.NearMeBKND.Business.model.bsns_BusinessFeedbackReply;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.util.List;
import com.example.NearMeBKND.util.QueryLogger;

@Repository
public class bsns_BusinessRepository {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public Integer save(bsns_Business business) {
        String sql = "INSERT INTO business (user_id, name, title, tags, description, image, googlemapsURL, longitude, latitude, address, mobile_number, timings, active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[] {"business_id"});
            ps.setString(1, business.getUserId());
            ps.setString(2, business.getName());
            ps.setString(3, business.getTitle());
            ps.setString(4, business.getTags());
            ps.setString(5, business.getDescription());
            ps.setBytes(6, business.getImage());
            ps.setString(7, business.getGooglemapsURL());
            ps.setObject(8, business.getLongitude());
            ps.setObject(9, business.getLatitude());
            ps.setString(10, business.getAddress());
            ps.setString(11, business.getMobileNumber());
            ps.setString(12, business.getTimings());
            ps.setObject(13, business.getActive() != null ? business.getActive() : true);
            return ps;
        }, keyHolder);
        return keyHolder.getKey().intValue();
    }

    public boolean userExists(String userId) {
        String sql = "SELECT COUNT(*) FROM user_locations WHERE user_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId);
        return count != null && count > 0;
    }

    public bsns_Business findById(Integer businessId) {
        String sql = "SELECT * FROM business WHERE business_id = ? AND active = 1";
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> mapRowToBusiness(rs), businessId);
    }

    public java.util.List<bsns_Business> findAll() {
        String sql = "SELECT * FROM business WHERE active = 1 ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapRowToBusiness(rs));
    }

    public void saveTags(Integer businessId, java.util.List<String> tags) {
        String sql = "INSERT INTO business_tags (business_id, tag) VALUES (?, ?)";
        for (String tag : tags) {
            jdbcTemplate.update(sql, businessId, tag.trim());
        }
    }

    public Integer getLastInsertedBusinessId() {
        String sql = "SELECT last_insert_rowid()";
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }

    public java.util.List<bsns_Business> findByTags(java.util.List<String> tags) {
        if (tags == null || tags.isEmpty()) return java.util.Collections.emptyList();
        String inSql = String.join(",", java.util.Collections.nCopies(tags.size(), "?"));
        String sql = "SELECT DISTINCT b.* FROM business b JOIN business_tags t ON b.business_id = t.business_id WHERE t.tag IN (" + inSql + ") AND b.active = 1 ORDER BY b.created_at DESC";
        return jdbcTemplate.query(sql, tags.toArray(), (rs, rowNum) -> mapRowToBusiness(rs));
    }

    public void saveNotification(bsns_BusinessNotification notification) {
        String sql = "INSERT INTO business_notification (business_id, user_id, message) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, notification.getBusinessId(), notification.getUserId(), notification.getMessage());
    }

    public bsns_BusinessNotification findLatestNotification(Integer businessId) {
        String sql = "SELECT * FROM business_notification WHERE business_id = ? ORDER BY created_at DESC LIMIT 1";
        java.util.List<bsns_BusinessNotification> list = jdbcTemplate.query(sql, new Object[]{businessId}, (rs, rowNum) -> {
            bsns_BusinessNotification n = new bsns_BusinessNotification();
            n.setNotificationId(rs.getInt("notification_id"));
            n.setBusinessId(rs.getInt("business_id"));
            n.setUserId(rs.getString("user_id"));
            n.setMessage(rs.getString("message"));
            n.setCreatedAt(rs.getTimestamp("created_at"));
            return n;
        });
        return list.isEmpty() ? null : list.get(0);
    }

    public java.util.List<bsns_Business> findByUserId(String userId) {
        String sql = "SELECT * FROM business WHERE user_id = ? AND active = 1 ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, new Object[]{userId}, (rs, rowNum) -> mapRowToBusiness(rs));
    }

    public java.util.List<bsns_Business> findByIds(java.util.List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return java.util.Collections.emptyList();
        String inSql = String.join(",", java.util.Collections.nCopies(ids.size(), "?"));
        String sql = "SELECT * FROM business WHERE business_id IN (" + inSql + ") AND active = 1 ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, ids.toArray(), (rs, rowNum) -> mapRowToBusiness(rs));
    }

    public int softDeleteById(Integer businessId, String userId) {
        String sql = "UPDATE business SET active = 0 WHERE business_id = ? AND user_id = ?";
        return jdbcTemplate.update(sql, businessId, userId);
    }

    public int updateImage(Integer businessId, String userId, byte[] image) {
        String sql = "UPDATE business SET image = ? WHERE business_id = ? AND user_id = ?";
        return jdbcTemplate.update(sql, image, businessId, userId);
    }

    public int updateBusinessFields(Integer businessId, String userId, java.util.Map<String, Object> fields) {
        if (fields == null || fields.isEmpty()) return 0;
        StringBuilder sql = new StringBuilder("UPDATE business SET ");
        java.util.List<Object> params = new java.util.ArrayList<>();
        for (String key : fields.keySet()) {
            sql.append(key).append(" = ?, ");
            params.add(fields.get(key));
        }
        sql.setLength(sql.length() - 2); // Remove last comma
        sql.append(" WHERE business_id = ? AND user_id = ?");
        params.add(businessId);
        params.add(userId);
        return jdbcTemplate.update(sql.toString(), params.toArray());
    }

    public void saveBusinessQuestion(bsns_BusinessQuestion question) {
        String sql = "INSERT INTO business_questions (business_id, user_id, question_text) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, question.getBusinessId(), question.getUserId(), question.getQuestionText());
    }

    public boolean isBusinessOwner(Integer businessId, String userId) {
        String sql = "SELECT COUNT(*) FROM business WHERE business_id = ? AND user_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, businessId, userId);
        return count != null && count > 0;
    }

    public bsns_BusinessQuestion findBusinessQuestionById(Integer questionId) {
        String sql = "SELECT * FROM business_questions WHERE question_id = ?";
        java.util.List<bsns_BusinessQuestion> list = jdbcTemplate.query(sql, new Object[]{questionId}, (rs, rowNum) -> {
            bsns_BusinessQuestion q = new bsns_BusinessQuestion();
            q.setQuestionId(rs.getInt("question_id"));
            q.setBusinessId(rs.getInt("business_id"));
            q.setUserId(rs.getString("user_id"));
            q.setQuestionText(rs.getString("question_text"));
            q.setCreatedAt(rs.getTimestamp("created_at"));
            return q;
        });
        return list.isEmpty() ? null : list.get(0);
    }

    public int deleteBusinessAnswerByQuestionId(Integer questionId) {
        String sql = "DELETE FROM business_answers WHERE question_id = ?";
        return jdbcTemplate.update(sql, questionId);
    }

    public int deleteBusinessQuestionById(Integer questionId) {
        // First, delete the answer (if any) for this question
        deleteBusinessAnswerByQuestionId(questionId);
        String sql = "DELETE FROM business_questions WHERE question_id = ?";
        return jdbcTemplate.update(sql, questionId);
    }

    public void saveBusinessAnswer(bsns_BusinessAnswer answer) {
        String sql = "INSERT INTO business_answers (question_id, user_id, answer_text) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, answer.getQuestionId(), answer.getUserId(), answer.getAnswerText());
    }

    public boolean isQuestionAnswered(Integer questionId) {
        String sql = "SELECT COUNT(*) FROM business_answers WHERE question_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, questionId);
        return count != null && count > 0;
    }

    public java.util.List<java.util.Map<String, Object>> findQuestionsByUserWithAnswers(String userId) {
        String sql = "SELECT q.question_id, q.business_id, b.name AS business_name, q.question_text, q.created_at AS question_created_at, " +
                "a.answer_id, a.answer_text, a.created_at AS answer_created_at " +
                "FROM business_questions q " +
                "JOIN business b ON q.business_id = b.business_id " +
                "LEFT JOIN business_answers a ON q.question_id = a.question_id " +
                "WHERE q.user_id = ? ORDER BY q.created_at DESC";
        return jdbcTemplate.query(sql, new Object[]{userId}, (rs, rowNum) -> {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("businessId", rs.getInt("business_id"));
            map.put("businessName", rs.getString("business_name"));
            map.put("questionId", rs.getInt("question_id"));
            map.put("questionText", rs.getString("question_text"));
            map.put("questionCreatedAt", rs.getTimestamp("question_created_at"));
            Integer answerId = rs.getObject("answer_id") != null ? rs.getInt("answer_id") : null;
            if (answerId != null) {
                java.util.Map<String, Object> answer = new java.util.HashMap<>();
                answer.put("answerId", answerId);
                answer.put("answerText", rs.getString("answer_text"));
                answer.put("createdAt", rs.getTimestamp("answer_created_at"));
                map.put("answer", answer);
            } else {
                map.put("answer", null);
            }
            return map;
        });
    }

    public void saveBusinessFeedback(bsns_BusinessFeedback feedback) {
        String sql = "INSERT INTO business_feedback (business_id, user_id, feedback_text) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, feedback.getBusinessId(), feedback.getUserId(), feedback.getFeedbackText());
    }

    public void saveBusinessFeedbackReply(bsns_BusinessFeedbackReply reply) {
        String sql = "INSERT INTO business_feedback_reply (feedback_id, user_id, reply_text) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, reply.getFeedbackId(), reply.getUserId(), reply.getReplyText());
    }

    public boolean isFeedbackReplyExists(Integer feedbackId) {
        String sql = "SELECT COUNT(*) FROM business_feedback_reply WHERE feedback_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, feedbackId);
        return count != null && count > 0;
    }

    public java.util.Map<String, Object> findFeedbackWithBusinessId(Integer feedbackId) {
        String sql = "SELECT f.feedback_id, f.business_id, b.user_id AS owner_id FROM business_feedback f JOIN business b ON f.business_id = b.business_id WHERE f.feedback_id = ?";
        java.util.List<java.util.Map<String, Object>> list = jdbcTemplate.query(sql, new Object[]{feedbackId}, (rs, rowNum) -> {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("feedbackId", rs.getInt("feedback_id"));
            map.put("businessId", rs.getInt("business_id"));
            map.put("ownerId", rs.getString("owner_id"));
            return map;
        });
        return list.isEmpty() ? null : list.get(0);
    }

    public java.util.List<java.util.Map<String, Object>> findQuestionsWithAnswersByBusinessId(Integer businessId) {
        String sql = "SELECT q.question_id, q.user_id, q.question_text, q.created_at, " +
                "a.answer_id, a.answer_text, a.created_at AS answer_created_at " +
                "FROM business_questions q " +
                "LEFT JOIN business_answers a ON q.question_id = a.question_id " +
                "WHERE q.business_id = ? ORDER BY q.created_at DESC";
        return jdbcTemplate.query(sql, new Object[]{businessId}, (rs, rowNum) -> {
            java.util.Map<String, Object> q = new java.util.HashMap<>();
            q.put("questionId", rs.getInt("question_id"));
            q.put("userId", rs.getString("user_id"));
            q.put("questionText", rs.getString("question_text"));
            q.put("createdAt", rs.getTimestamp("created_at"));
            Integer answerId = rs.getObject("answer_id") != null ? rs.getInt("answer_id") : null;
            if (answerId != null) {
                java.util.Map<String, Object> answer = new java.util.HashMap<>();
                answer.put("answerId", answerId);
                answer.put("answerText", rs.getString("answer_text"));
                answer.put("createdAt", rs.getTimestamp("answer_created_at"));
                q.put("answer", answer);
            } else {
                q.put("answer", null);
            }
            return q;
        });
    }

    public java.util.List<java.util.Map<String, Object>> findFeedbacksWithRepliesByBusinessId(Integer businessId) {
        String sql = "SELECT f.feedback_id, f.user_id, f.feedback_text, f.created_at, " +
                "r.reply_id, r.reply_text, r.created_at AS reply_created_at " +
                "FROM business_feedback f " +
                "LEFT JOIN business_feedback_reply r ON f.feedback_id = r.feedback_id " +
                "WHERE f.business_id = ? ORDER BY f.created_at DESC";
        return jdbcTemplate.query(sql, new Object[]{businessId}, (rs, rowNum) -> {
            java.util.Map<String, Object> fb = new java.util.HashMap<>();
            fb.put("feedbackId", rs.getInt("feedback_id"));
            fb.put("userId", rs.getString("user_id"));
            fb.put("feedbackText", rs.getString("feedback_text"));
            fb.put("createdAt", rs.getTimestamp("created_at"));
            Integer replyId = rs.getObject("reply_id") != null ? rs.getInt("reply_id") : null;
            if (replyId != null) {
                java.util.Map<String, Object> reply = new java.util.HashMap<>();
                reply.put("replyId", replyId);
                reply.put("replyText", rs.getString("reply_text"));
                reply.put("createdAt", rs.getTimestamp("reply_created_at"));
                fb.put("reply", reply);
            } else {
                fb.put("reply", null);
            }
            return fb;
        });
    }

    public java.util.List<java.util.Map<String, Object>> findFeedbacksWithRepliesByBusinessIdAndUserId(Integer businessId, String userId) {
        String sql = "SELECT f.feedback_id, f.user_id, f.feedback_text, f.created_at, " +
                "r.reply_id, r.reply_text, r.created_at AS reply_created_at " +
                "FROM business_feedback f " +
                "LEFT JOIN business_feedback_reply r ON f.feedback_id = r.feedback_id " +
                "WHERE f.business_id = ? AND f.user_id = ? ORDER BY f.created_at DESC";
        return jdbcTemplate.query(sql, new Object[]{businessId, userId}, (rs, rowNum) -> {
            java.util.Map<String, Object> fb = new java.util.HashMap<>();
            fb.put("feedbackId", rs.getInt("feedback_id"));
            fb.put("userId", rs.getString("user_id"));
            fb.put("feedbackText", rs.getString("feedback_text"));
            fb.put("createdAt", rs.getTimestamp("created_at"));
            Integer replyId = rs.getObject("reply_id") != null ? rs.getInt("reply_id") : null;
            if (replyId != null) {
                java.util.Map<String, Object> reply = new java.util.HashMap<>();
                reply.put("replyId", replyId);
                reply.put("replyText", rs.getString("reply_text"));
                reply.put("createdAt", rs.getTimestamp("reply_created_at"));
                fb.put("reply", reply);
            } else {
                fb.put("reply", null);
            }
            return fb;
        });
    }

    public java.util.Map<String, Object> findFeedbackWithBusinessIdAndUserId(Integer feedbackId) {
        String sql = "SELECT f.feedback_id, f.business_id, f.user_id AS feedback_user_id, b.user_id AS owner_id FROM business_feedback f JOIN business b ON f.business_id = b.business_id WHERE f.feedback_id = ?";
        java.util.List<java.util.Map<String, Object>> list = jdbcTemplate.query(sql, new Object[]{feedbackId}, (rs, rowNum) -> {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("feedbackId", rs.getInt("feedback_id"));
            map.put("businessId", rs.getInt("business_id"));
            map.put("feedbackUserId", rs.getString("feedback_user_id"));
            map.put("ownerId", rs.getString("owner_id"));
            return map;
        });
        return list.isEmpty() ? null : list.get(0);
    }

    public int deleteBusinessFeedbackById(Integer feedbackId) {
        String sql = "DELETE FROM business_feedback WHERE feedback_id = ?";
        return jdbcTemplate.update(sql, feedbackId);
    }

    public int deleteBusinessFeedbackReplyByFeedbackId(Integer feedbackId) {
        String sql = "DELETE FROM business_feedback_reply WHERE feedback_id = ?";
        return jdbcTemplate.update(sql, feedbackId);
    }

    public java.util.List<bsns_Business> findAllPaginated(int limit, int offset) {
        String sql = "SELECT * FROM business WHERE active = 1 ORDER BY created_at DESC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, new Object[]{limit, offset}, (rs, rowNum) -> mapRowToBusiness(rs));
    }

    public java.util.List<bsns_Business> findAllPaginatedFiltered(String userId, double userLat, double userLon, double radiusKm, int limit, int offset) {
        double latDegree = radiusKm / 111.0;
        double lonDegree = radiusKm / (111.0 * Math.cos(Math.toRadians(userLat)));
        double minLat = userLat - latDegree;
        double maxLat = userLat + latDegree;
        double minLon = userLon - lonDegree;
        double maxLon = userLon + lonDegree;

        String sql = """
            SELECT * FROM business
            WHERE active = 1
              AND (
                user_id = ?
                OR (
                  latitude BETWEEN ? AND ?
                  AND longitude BETWEEN ? AND ?
                  AND latitude IS NOT NULL AND longitude IS NOT NULL
                  AND (6371 * 2 * 
                    ASIN(
                      SQRT(
                        POWER(SIN(RADIANS(latitude - ?) / 2), 2) +
                        COS(RADIANS(?)) * COS(RADIANS(latitude)) *
                        POWER(SIN(RADIANS(longitude - ?) / 2), 2)
                      )
                    )
                  ) <= ?
                )
              )
            ORDER BY created_at DESC
            LIMIT ? OFFSET ?
        """;
        long start = System.currentTimeMillis();
        java.util.List<bsns_Business> result = jdbcTemplate.query(sql, new Object[]{
            userId, minLat, maxLat, minLon, maxLon,
            userLat, userLat, userLon, radiusKm, limit, offset
        }, (rs, rowNum) -> mapRowToBusiness(rs));
        long end = System.currentTimeMillis();
        QueryLogger.log(sql, end - start);
        return result;
    }
 
    public int countAllActiveBusinesses() {
        String sql = "SELECT COUNT(*) FROM business WHERE active = 1";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count != null ? count : 0;
    }

    public int countAllFiltered(String userId, double userLat, double userLon, double radiusKm) {
        double latDegree = radiusKm / 111.0;
        double lonDegree = radiusKm / (111.0 * Math.cos(Math.toRadians(userLat)));
        double minLat = userLat - latDegree;
        double maxLat = userLat + latDegree;
        double minLon = userLon - lonDegree;
        double maxLon = userLon + lonDegree;
        String sql = """
            SELECT COUNT(*) FROM business
            WHERE active = 1
              AND (
                user_id = ?
                OR (
                  latitude BETWEEN ? AND ?
                  AND longitude BETWEEN ? AND ?
                  AND latitude IS NOT NULL AND longitude IS NOT NULL
                  AND (6371 * 2 * 
                    ASIN(
                      SQRT(
                        POWER(SIN(RADIANS(latitude - ?) / 2), 2) +
                        COS(RADIANS(?)) * COS(RADIANS(latitude)) *
                        POWER(SIN(RADIANS(longitude - ?) / 2), 2)
                      )
                    )
                  ) <= ?
                )
              )
        """;
        long start = System.currentTimeMillis();
        Integer count = jdbcTemplate.queryForObject(sql, new Object[]{
            userId, minLat, maxLat, minLon, maxLon,
            userLat, userLat, userLon, radiusKm
        }, Integer.class);
        long end = System.currentTimeMillis();
        QueryLogger.log(sql, end - start);
        return count != null ? count : 0;
    }

    public List<bsns_Business> findAllWithinRadiusPaged(String userId, double userLat, double userLon, double radiusKm, int limit, int offset) {
        double latDegree = radiusKm / 111.0;
        double lonDegree = radiusKm / (111.0 * Math.cos(Math.toRadians(userLat)));
        double minLat = userLat - latDegree;
        double maxLat = userLat + latDegree;
        double minLon = userLon - lonDegree;
        double maxLon = userLon + lonDegree;
        String sql = """
            SELECT business_id, user_id, name, title, description, address, mobile_number, timings, googlemapsURL, latitude, longitude, created_at, active
            FROM business
            WHERE active = 1
              AND latitude IS NOT NULL AND longitude IS NOT NULL
              AND latitude BETWEEN ? AND ?
              AND longitude BETWEEN ? AND ?
              AND (6371 * acos(
                cos(radians(?)) * cos(radians(latitude)) *
                cos(radians(longitude) - radians(?)) +
                sin(radians(?)) * sin(radians(latitude))
              )) <= ?
            ORDER BY created_at DESC
            LIMIT ? OFFSET ?
        """;
        long start = System.currentTimeMillis();
        List<bsns_Business> result = jdbcTemplate.query(sql, new Object[]{
            minLat, maxLat, minLon, maxLon,
            userLat, userLon, userLat, radiusKm, limit, offset
        }, (rs, rowNum) -> mapRowToBusiness(rs));
        long end = System.currentTimeMillis();
        QueryLogger.log(sql, end - start);
        return result;
    }

    private bsns_Business mapRowToBusiness(ResultSet rs) throws SQLException {
        bsns_Business b = new bsns_Business();
        b.setBusinessId(rs.getInt("business_id"));
        b.setUserId(rs.getString("user_id"));
        b.setName(rs.getString("name"));
        b.setTitle(rs.getString("title"));
        b.setDescription(rs.getString("description"));
        b.setAddress(rs.getString("address"));
        b.setMobileNumber(rs.getString("mobile_number"));
        b.setTimings(rs.getString("timings"));
        b.setGooglemapsURL(rs.getString("googlemapsURL"));
        b.setLatitude(rs.getDouble("latitude"));
        b.setLongitude(rs.getDouble("longitude"));
        b.setCreatedAt(rs.getTimestamp("created_at"));
        b.setActive(rs.getBoolean("active"));
        return b;
    }
} 