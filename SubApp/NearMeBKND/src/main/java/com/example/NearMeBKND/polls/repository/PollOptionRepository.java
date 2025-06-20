package com.example.NearMeBKND.polls.repository;

import com.example.NearMeBKND.polls.model.PollOption;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;

@Repository
public class PollOptionRepository {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public int insertPollOption(PollOption option) {
        String sql = "INSERT INTO poll_options (question_id, option_text) VALUES (?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, option.getQuestionId());
            ps.setString(2, option.getOptionText());
            return ps;
        }, keyHolder);
        return keyHolder.getKey().intValue();
    }

    public void deleteByQuestionId(int questionId) {
        String sql = "DELETE FROM poll_options WHERE question_id = ?";
        jdbcTemplate.update(sql, questionId);
    }
} 