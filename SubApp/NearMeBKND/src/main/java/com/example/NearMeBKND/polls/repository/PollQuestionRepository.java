package com.example.NearMeBKND.polls.repository;

import com.example.NearMeBKND.polls.model.PollQuestion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;

@Repository
public class PollQuestionRepository {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public int insertPollQuestion(PollQuestion question) {
        String sql = "INSERT INTO poll_questions (question_text, selection_limit, selection_mode, collection_id) VALUES (?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, question.getQuestionText());
            ps.setInt(2, question.getSelectionLimit());
            ps.setString(3, question.getSelectionMode());
            ps.setInt(4, question.getCollectionId());
            return ps;
        }, keyHolder);
        return keyHolder.getKey().intValue();
    }

    public void deleteByCollectionId(int collectionId) {
        String sql = "DELETE FROM poll_questions WHERE collection_id = ?";
        jdbcTemplate.update(sql, collectionId);
    }
}