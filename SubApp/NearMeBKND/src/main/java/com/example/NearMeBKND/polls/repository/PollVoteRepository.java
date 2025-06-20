package com.example.NearMeBKND.polls.repository;

import com.example.NearMeBKND.polls.model.PollVote;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class PollVoteRepository {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    private final RowMapper<PollVote> voteRowMapper = new RowMapper<PollVote>() {
        @Override
        public PollVote mapRow(ResultSet rs, int rowNum) throws SQLException {
            PollVote vote = new PollVote();
            vote.setVoteId(rs.getInt("vote_id"));
            vote.setUserId(rs.getString("user_id"));
            vote.setQuestionId(rs.getInt("question_id"));
            vote.setOptionId(rs.getInt("option_id"));
            vote.setCreatedAt(rs.getString("created_at"));
            return vote;
        }
    };
    
    public int insertVote(PollVote vote) {
        String sql = """
            INSERT INTO poll_votes (user_id, question_id, option_id, created_at) 
            VALUES (?, ?, ?, ?)
        """;
        return jdbcTemplate.update(sql, 
            vote.getUserId(), 
            vote.getQuestionId(), 
            vote.getOptionId(), 
            vote.getCreatedAt()
        );
    }
    
    public List<PollVote> findVotesByQuestionId(int questionId) {
        String sql = "SELECT * FROM poll_votes WHERE question_id = ?";
        return jdbcTemplate.query(sql, voteRowMapper, questionId);
    }
    
    public List<PollVote> findVotesByUserIdAndQuestionId(String userId, int questionId) {
        String sql = "SELECT * FROM poll_votes WHERE user_id = ? AND question_id = ?";
        return jdbcTemplate.query(sql, voteRowMapper, userId, questionId);
    }
    
    public boolean hasUserVotedOnQuestion(String userId, int questionId) {
        String sql = "SELECT COUNT(*) FROM poll_votes WHERE user_id = ? AND question_id = ?";
        int count = jdbcTemplate.queryForObject(sql, Integer.class, userId, questionId);
        return count > 0;
    }
    
    public int getVoteCountForOption(int optionId) {
        String sql = "SELECT COUNT(*) FROM poll_votes WHERE option_id = ?";
        return jdbcTemplate.queryForObject(sql, Integer.class, optionId);
    }
    
    public void deleteVotesByUserIdAndQuestionId(String userId, int questionId) {
        String sql = "DELETE FROM poll_votes WHERE user_id = ? AND question_id = ?";
        jdbcTemplate.update(sql, userId, questionId);
    }
    
    public List<Integer> getVotedOptionIds(String userId, int questionId) {
        String sql = "SELECT option_id FROM poll_votes WHERE user_id = ? AND question_id = ?";
        return jdbcTemplate.queryForList(sql, Integer.class, userId, questionId);
    }
} 