package com.example.NearMeBKND.polls.repository;

import com.example.NearMeBKND.polls.model.PollQuestion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class PollQuestionRepository {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<PollQuestion> rowMapper = new RowMapper<PollQuestion>() {
        @Override
        public PollQuestion mapRow(ResultSet rs, int rowNum) throws SQLException {
            PollQuestion question = new PollQuestion();
            question.setQuestionId(rs.getInt("question_id"));
            question.setUserId(rs.getString("user_id"));
            question.setQuestionText(rs.getString("question_text"));
            question.setSelectionLimit(rs.getInt("selection_limit"));
            question.setSelectionMode(rs.getString("selection_mode"));
            question.setLatitude(rs.getDouble("latitude"));
            question.setLongitude(rs.getDouble("longitude"));
            question.setCreatedAt(rs.getString("created_at"));
            return question;
        }
    };

    public int createQuestion(String userId, String questionText, int selectionLimit, String selectionMode, double latitude, double longitude) {
        String sql = "INSERT INTO poll_questions (user_id, question_text, selection_limit, selection_mode, latitude, longitude) " +
                     "VALUES (?, ?, ?, ?, ?, ?) RETURNING question_id";
        return jdbcTemplate.queryForObject(sql, Integer.class, userId, questionText, selectionLimit, selectionMode, latitude, longitude);
    }

    public PollQuestion getQuestion(int questionId) {
        String sql = "SELECT * FROM poll_questions WHERE question_id = ?";
        List<PollQuestion> questions = jdbcTemplate.query(sql, rowMapper, questionId);
        return questions.isEmpty() ? null : questions.get(0);
    }

    public List<PollQuestion> getAllQuestions() {
        String sql = "SELECT * FROM poll_questions ORDER BY question_id DESC";
        return jdbcTemplate.query(sql, rowMapper);
    }

    public boolean isOwner(String userId, int questionId) {
        String sql = "SELECT COUNT(*) FROM poll_questions WHERE question_id = ? AND user_id = ?";
        int count = jdbcTemplate.queryForObject(sql, Integer.class, questionId, userId);
        return count > 0;
    }

    public void deleteQuestion(int questionId) {
        String sql = "DELETE FROM poll_questions WHERE question_id = ?";
        jdbcTemplate.update(sql, questionId);
    }

    /**
     * Returns a page of poll questions within the given radius (km) of the user's location, using bounding box and Haversine filtering in SQL.
     */
    public List<PollQuestion> getQuestionsInRadiusPaged(double userLat, double userLon, double radiusKm, int limit, int offset) {
        // Calculate bounding box
        double latDelta = radiusKm / 111.0;
        double lonDelta = radiusKm / (111.0 * Math.cos(Math.toRadians(userLat)));
        String sql = """
            SELECT *, (6371 * acos(
                cos(radians(?)) * cos(radians(latitude)) *
                cos(radians(longitude) - radians(?)) +
                sin(radians(?)) * sin(radians(latitude))
            )) AS distance
            FROM poll_questions
            WHERE latitude IS NOT NULL AND longitude IS NOT NULL
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
        return jdbcTemplate.query(sql, rowMapper,
            userLat, userLon, userLat, // for distance calc
            userLat - latDelta, userLat + latDelta,
            userLon - lonDelta, userLon + lonDelta,
            userLat, userLon, userLat, // for distance filter
            radiusKm,
            limit, offset
        );
    }

    /**
     * Returns the count of poll questions within the given radius (km) of the user's location, using bounding box and Haversine filtering in SQL.
     */
    public int countQuestionsInRadius(double userLat, double userLon, double radiusKm) {
        // Calculate bounding box
        double latDelta = radiusKm / 111.0;
        double lonDelta = radiusKm / (111.0 * Math.cos(Math.toRadians(userLat)));
        String sql = """
            SELECT COUNT(*) FROM (
                SELECT question_id FROM poll_questions
                WHERE latitude IS NOT NULL AND longitude IS NOT NULL
                  AND latitude BETWEEN ? AND ?
                  AND longitude BETWEEN ? AND ?
                  AND (6371 * acos(
                    cos(radians(?)) * cos(radians(latitude)) *
                    cos(radians(longitude) - radians(?)) +
                    sin(radians(?)) * sin(radians(latitude))
                  )) <= ?
            )
        """;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class,
            userLat - latDelta, userLat + latDelta,
            userLon - lonDelta, userLon + lonDelta,
            userLat, userLon, userLat, // for distance filter
            radiusKm
        );
        return count != null ? count : 0;
    }
} 