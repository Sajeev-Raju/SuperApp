package com.example.NearMeBKND.polls.repository;

import com.example.NearMeBKND.polls.model.PollOption;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PollOptionRepository {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<PollOption> rowMapper = (rs, rowNum) -> {
        PollOption option = new PollOption();
        option.setOptionId(rs.getInt("option_id"));
        option.setQuestionId(rs.getInt("question_id"));
        option.setOptionText(rs.getString("option_text"));
        option.setVoteCount(rs.getInt("vote_count"));
        return option;
    };

    public void createOption(int questionId, String optionText) {
        String sql = "INSERT INTO poll_options (question_id, option_text) VALUES (?, ?)";
        jdbcTemplate.update(sql, questionId, optionText);
    }

    public List<PollOption> getOptionsWithVoteCount(int questionId) {
        String sql = "SELECT o.*, COUNT(v.vote_id) as vote_count " +
                     "FROM poll_options o " +
                     "LEFT JOIN poll_votes v ON o.option_id = v.option_id " +
                     "WHERE o.question_id = ? " +
                     "GROUP BY o.option_id";
        return jdbcTemplate.query(sql, rowMapper, questionId);
    }
} 