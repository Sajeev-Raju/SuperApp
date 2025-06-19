package com.example.NearMeBKND.qanda.repository;

import com.example.NearMeBKND.qanda.model.Question;
import com.example.NearMeBKND.qanda.model.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class QuestionRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TagRepository tagRepository;

    @Transactional
    public int saveQuestion(Question question) {
        String sql = "INSERT INTO qna_questions (user_id, title, description, tags, latitude, longitude) VALUES (?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
            question.getUserId(),
            question.getTitle(),
            question.getDescription(),
            question.getTags(),
            question.getLatitude(),
            question.getLongitude()
        );
        Integer id = jdbcTemplate.queryForObject("SELECT last_insert_rowid()", Integer.class);
        return id != null ? id : -1;
    }

    public List<Question> findAll() {
        String sql = "SELECT * FROM qna_questions ORDER BY created_at DESC";
        List<Question> questions = jdbcTemplate.query(sql, (rs, rowNum) -> mapRowToQuestion(rs, rowNum));
        for (Question question : questions) {
            question.setTagList(tagRepository.getTagsForQuestion(question.getId().intValue()));
            loadNotes(question);
        }
        return questions;
    }

    public Question findById(Long id) {
        String sql = "SELECT * FROM qna_questions WHERE id = ?";
        Question question = jdbcTemplate.queryForObject(sql, (rs, rowNum) -> mapRowToQuestion(rs, rowNum), id);
        if (question != null) {
            question.setTagList(tagRepository.getTagsForQuestion(id.intValue()));
            loadNotes(question);
        }
        return question;
    }

    private void loadNotes(Question question) {
        String sql = "SELECT note FROM qna_note WHERE question_id = ? ORDER BY id";
        List<String> notes = jdbcTemplate.queryForList(sql, String.class, question.getId());
        question.setNotes(notes);
    }

    public void addNote(Long questionId, String note) {
        String sql = "INSERT INTO qna_note (question_id, note) VALUES (?, ?)";
        jdbcTemplate.update(sql, questionId, note);
    }

    public void deleteQuestion(int id) {
        jdbcTemplate.update("DELETE FROM qna_questions WHERE id = ?", id);
    }

    // Multi-tag search (AND logic)
    public List<Question> findByTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) return findAll();
        String inSql = String.join(",", Collections.nCopies(tags.size(), "?"));
        String sql = "SELECT q.* FROM qna_questions q " +
                "JOIN qna_question_tag qt ON q.id = qt.qId " +
                "JOIN qna_tags t ON qt.tagId = t.tagId " +
                "WHERE t.tagName IN (" + inSql + ") " +
                "GROUP BY q.id " +
                "HAVING COUNT(DISTINCT t.tagName) = ?";
        List<Object> params = new ArrayList<>(tags);
        params.add(tags.size());
        return jdbcTemplate.query(sql, params.toArray(), this::mapRowToQuestion);
    }

    // Tag association
    public void addTagsToQuestion(int qId, List<Integer> tagIds) {
        for (Integer tagId : tagIds) {
            jdbcTemplate.update(
                "INSERT OR IGNORE INTO qna_question_tag (qId, tagId) VALUES (?, ?)",
                qId, tagId
            );
        }
    }

    public List<Tag> findTagsForQuestion(int qId) {
        String sql = "SELECT t.* FROM qna_tags t JOIN qna_question_tag qt ON t.tagId = qt.tagId WHERE qt.qId = ?";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Tag tag = new Tag();
            tag.setTagId(rs.getInt("tagId"));
            tag.setTagName(rs.getString("tagName"));
            return tag;
        }, qId);
    }

    private Question mapRowToQuestion(ResultSet rs, int rowNum) throws SQLException {
        Question question = new Question();
        question.setId(rs.getLong("id"));
        question.setUserId(rs.getString("user_id"));
        question.setTitle(rs.getString("title"));
        question.setDescription(rs.getString("description"));
        question.setTags(rs.getString("tags"));
        question.setCreatedAt(rs.getString("created_at"));
        question.setLatitude(rs.getDouble("latitude"));
        question.setLongitude(rs.getDouble("longitude"));
        return question;
    }

    /**
     * Returns a page of Q&A questions within the given radius (km) of the user's location, using bounding box and Haversine filtering in SQL.
     */
    public List<Question> findQuestionsInRadiusPaged(double userLat, double userLon, double radiusKm, int limit, int offset) {
        double latDelta = radiusKm / 111.0;
        double lonDelta = radiusKm / (111.0 * Math.cos(Math.toRadians(userLat)));
        String sql = """
            SELECT *, (6371 * acos(
                cos(radians(?)) * cos(radians(latitude)) *
                cos(radians(longitude) - radians(?)) +
                sin(radians(?)) * sin(radians(latitude))
            )) AS distance
            FROM qna_questions
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
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapRowToQuestion(rs, rowNum),
            userLat, userLon, userLat,
            userLat - latDelta, userLat + latDelta,
            userLon - lonDelta, userLon + lonDelta,
            userLat, userLon, userLat,
            radiusKm,
            limit, offset
        );
    }

    /**
     * Returns the count of Q&A questions within the given radius (km) of the user's location, using bounding box and Haversine filtering in SQL.
     */
    public int countQuestionsInRadius(double userLat, double userLon, double radiusKm) {
        double latDelta = radiusKm / 111.0;
        double lonDelta = radiusKm / (111.0 * Math.cos(Math.toRadians(userLat)));
        String sql = """
            SELECT COUNT(*) FROM (
                SELECT id FROM qna_questions
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
            userLat, userLon, userLat,
            radiusKm
        );
        return count != null ? count : 0;
    }
}















