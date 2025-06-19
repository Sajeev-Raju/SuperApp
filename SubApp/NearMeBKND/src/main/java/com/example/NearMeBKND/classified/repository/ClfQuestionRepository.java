package com.example.NearMeBKND.classified.repository;

import com.example.NearMeBKND.classified.model.ClfQuestion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ClfQuestionRepository {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public int saveQuestion(ClfQuestion question) {
        jdbcTemplate.update(
            "INSERT INTO clf_question (classified_id, content, user_id) VALUES (?, ?, ?)",
            question.getClassifiedId(), question.getContent(), question.getUserId()
        );
        Integer id = jdbcTemplate.queryForObject("SELECT last_insert_rowid()", Integer.class);
        return id != null ? id : -1;
    }

    public boolean classifiedExists(int classifiedId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM clf_classified WHERE id = ?",
            Integer.class, classifiedId
        );
        return count != null && count > 0;
    }

    public void deleteQuestion(int questionId) {
        jdbcTemplate.update("DELETE FROM clf_question WHERE id = ?", questionId);
    }

    public ClfQuestion getQuestionById(int id) {
        String sql = "SELECT id, classified_id, content, user_id, created_at FROM clf_question WHERE id = ?";
        return jdbcTemplate.query(sql, rs -> {
            if (rs.next()) {
                ClfQuestion q = new ClfQuestion();
                q.setId(rs.getInt("id"));
                q.setClassifiedId(rs.getInt("classified_id"));
                q.setContent(rs.getString("content"));
                q.setUserId(rs.getString("user_id"));
                q.setCreatedAt(rs.getString("created_at"));
                return q;
            } else {
                return null;
            }
        }, id);
    }

    public List<ClfQuestion> getQuestionsByClassifiedId(int classifiedId) {
        String sql = "SELECT id, classified_id, content, user_id, created_at FROM clf_question WHERE classified_id = ?";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            ClfQuestion q = new ClfQuestion();
            q.setId(rs.getInt("id"));
            q.setClassifiedId(rs.getInt("classified_id"));
            q.setContent(rs.getString("content"));
            q.setUserId(rs.getString("user_id"));
            q.setCreatedAt(rs.getString("created_at"));
            return q;
        }, classifiedId);
    }
} 