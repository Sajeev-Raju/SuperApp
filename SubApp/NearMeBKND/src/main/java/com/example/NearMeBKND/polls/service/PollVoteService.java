package com.example.NearMeBKND.polls.service;

import com.example.NearMeBKND.polls.repository.PollVoteRepository;
import com.example.NearMeBKND.polls.repository.PollQuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.LinkedHashMap;

@Service
public class PollVoteService {
    @Autowired
    private PollVoteRepository pollVoteRepository;

    @Autowired
    private PollQuestionRepository pollQuestionRepository;

    @Transactional
    public boolean vote(String userId, int questionId, List<Integer> optionIds) {
        // Check if user is the owner
        if (pollQuestionRepository.isOwner(userId, questionId)) {
            return false;
        }

        // Check if user has already voted
        if (pollVoteRepository.hasVoted(userId, questionId)) {
            return false;
        }

        // Validate that all optionIds belong to the questionId
        for (Integer optionId : optionIds) {
            String sql = "SELECT COUNT(*) FROM poll_options WHERE option_id = ? AND question_id = ?";
            int count = pollVoteRepository.getJdbcTemplate().queryForObject(sql, Integer.class, optionId, questionId);
            if (count == 0) {
                return false; // Invalid optionId for this question
            }
        }

        // Record votes
        for (Integer optionId : optionIds) {
            pollVoteRepository.recordVote(userId, questionId, optionId);
        }

        return true;
    }

    @Transactional
    public boolean updateVote(String userId, int questionId, List<Integer> newOptionIds) {
        if (!pollVoteRepository.hasVoted(userId, questionId)) {
            return false; // User hasn't voted before
        }
        // Validate that all newOptionIds belong to the questionId
        for (Integer optionId : newOptionIds) {
            String sql = "SELECT COUNT(*) FROM poll_options WHERE option_id = ? AND question_id = ?";
            int count = pollVoteRepository.getJdbcTemplate().queryForObject(sql, Integer.class, optionId, questionId);
            if (count == 0) {
                return false; // Invalid optionId for this question
            }
        }
        pollVoteRepository.deleteVotes(userId, questionId);
        for (Integer optionId : newOptionIds) {
            pollVoteRepository.recordVote(userId, questionId, optionId);
        }
        return true;
    }

    public List<Map<String, Object>> getVotedPollsByUser(String userId) {
        // Query to get all votes by user, joining questions and options
        String sql = "SELECT q.question_id, q.question_text, o.option_text " +
                "FROM poll_votes v " +
                "JOIN poll_questions q ON v.question_id = q.question_id " +
                "JOIN poll_options o ON v.option_id = o.option_id " +
                "WHERE v.user_id = ? " +
                "ORDER BY v.question_id";
        List<Map<String, Object>> rows = pollVoteRepository.getJdbcTemplate().queryForList(sql, userId);
        // Group by question
        Map<Integer, Map<String, Object>> result = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Integer qid = (Integer) row.get("question_id");
            String qtext = (String) row.get("question_text");
            String otext = (String) row.get("option_text");
            if (!result.containsKey(qid)) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("questionId", qid);
                entry.put("questionText", qtext);
                entry.put("votedOptions", new ArrayList<String>());
                result.put(qid, entry);
            }
            ((List<String>) result.get(qid).get("votedOptions")).add(otext);
        }
        return new ArrayList<>(result.values());
    }

    public String getVoteOwner(int questionId, String userId) {
        // Check if the user has voted on this question
        boolean hasVoted = pollVoteRepository.hasVoted(userId, questionId);
        if (hasVoted) {
            return userId;
        } else {
            return null;
        }
    }
} 