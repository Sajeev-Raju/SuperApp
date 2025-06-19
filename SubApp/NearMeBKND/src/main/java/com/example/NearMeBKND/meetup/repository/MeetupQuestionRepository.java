package com.example.NearMeBKND.meetup.repository;

import com.example.NearMeBKND.meetup.model.MeetupQuestion;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class MeetupQuestionRepository {
    private final JdbcTemplate jdbcTemplate;
    private static final Logger logger = LoggerFactory.getLogger(MeetupQuestionRepository.class);

    public MeetupQuestionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<MeetupQuestion> rowMapper = (rs, rowNum) -> mapRow(rs);

    private MeetupQuestion mapRow(ResultSet rs) throws SQLException {
        MeetupQuestion q = new MeetupQuestion();
        q.setId(rs.getLong("id"));
        q.setMeetupId(rs.getLong("meetup_id"));
        q.setUserId(rs.getString("user_id"));
        q.setContent(rs.getString("content"));
        // Handle timestamp parsing safely
        try {
            String timestampStr = rs.getString("created_at");
            if (timestampStr != null) {
                q.setCreatedAt(LocalDateTime.parse(timestampStr.replace(" ", "T")));
            }
        } catch (Exception e) {
            logger.warn("Error parsing timestamp for question: {}", e.getMessage());
            q.setCreatedAt(LocalDateTime.now());
        }
        return q;
    }

    public int saveQuestion(MeetupQuestion question) {
        String sql = "INSERT INTO mtp_meetup_question (meetup_id, user_id, content, created_at) VALUES (?, ?, ?, ?)";
        return jdbcTemplate.update(sql, question.getMeetupId(), question.getUserId(), question.getContent(), question.getCreatedAt());
    }

    public List<MeetupQuestion> findByMeetupId(Long meetupId) {
        String sql = "SELECT * FROM mtp_meetup_question WHERE meetup_id = ? ORDER BY created_at ASC";
        return jdbcTemplate.query(sql, rowMapper, meetupId);
    }
} 