package com.example.NearMeBKND.emergency.repository;

import com.example.NearMeBKND.emergency.model.EmergencyQuestion;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public class EmergencyQuestionRepository {
    private final JdbcTemplate jdbcTemplate;

    public EmergencyQuestionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int saveQuestion(EmergencyQuestion question) {
        jdbcTemplate.update(
            "INSERT INTO emergency_question (emergency_id, content, user_id) VALUES (?, ?, ?)",
            question.getEmergencyId(), question.getContent(), question.getUserId()
        );
        Integer id = jdbcTemplate.queryForObject("SELECT last_insert_rowid()", Integer.class);
        return id != null ? id : -1;
    }

    public List<EmergencyQuestion> getQuestionsByEmergencyId(int emergencyId) {
        return jdbcTemplate.query(
            "SELECT * FROM emergency_question WHERE emergency_id = ?",
            (rs, rowNum) -> {
                EmergencyQuestion q = new EmergencyQuestion();
                q.setId(rs.getInt("id"));
                q.setEmergencyId(rs.getInt("emergency_id"));
                q.setContent(rs.getString("content"));
                q.setUserId(rs.getString("user_id"));
                q.setCreatedAt(rs.getString("created_at"));
                return q;
            },
            emergencyId
        );
    }

    public EmergencyQuestion getQuestionById(int id) {
        List<EmergencyQuestion> list = jdbcTemplate.query(
            "SELECT * FROM emergency_question WHERE id = ?",
            (rs, rowNum) -> {
                EmergencyQuestion q = new EmergencyQuestion();
                q.setId(rs.getInt("id"));
                q.setEmergencyId(rs.getInt("emergency_id"));
                q.setContent(rs.getString("content"));
                q.setUserId(rs.getString("user_id"));
                q.setCreatedAt(rs.getString("created_at"));
                return q;
            },
            id
        );
        return list.isEmpty() ? null : list.get(0);
    }

    public void deleteQuestion(int id) {
        jdbcTemplate.update("DELETE FROM emergency_question WHERE id = ?", id);
    }
} 