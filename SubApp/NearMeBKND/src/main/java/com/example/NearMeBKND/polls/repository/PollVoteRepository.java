package com.example.NearMeBKND.polls.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PollVoteRepository {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public boolean hasVoted(String userId, int questionId) {
        String sql = "SELECT COUNT(*) FROM poll_votes WHERE user_id = ? AND question_id = ?";
        int count = jdbcTemplate.queryForObject(sql, Integer.class, userId, questionId);
        return count > 0;
    }

    public void recordVote(String userId, int questionId, int optionId) {
        String sql = "INSERT INTO poll_votes (user_id, question_id, option_id) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, userId, questionId, optionId);
    }

    public void deleteVotes(String userId, int questionId) {
        String sql = "DELETE FROM poll_votes WHERE user_id = ? AND question_id = ?";
        jdbcTemplate.update(sql, userId, questionId);
    }

    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }
} 