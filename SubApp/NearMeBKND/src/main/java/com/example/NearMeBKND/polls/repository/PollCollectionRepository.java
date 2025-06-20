package com.example.NearMeBKND.polls.repository;

import com.example.NearMeBKND.polls.model.PollCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;

@Repository
public class PollCollectionRepository {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public int insertPollCollection(String collectionTitle, String createdAt, String userId, double latitude, double longitude) {
        String sql = "INSERT INTO poll_collection (collection_title, created_at, user_id, latitude, longitude) VALUES (?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, collectionTitle);
            ps.setString(2, createdAt);
            ps.setString(3, userId);
            ps.setDouble(4, latitude);
            ps.setDouble(5, longitude);
            return ps;
        }, keyHolder);
        return keyHolder.getKey().intValue();
    }

    public void deleteByCollectionId(int collectionId) {
        String sql = "DELETE FROM poll_collection WHERE collection_id = ?";
        jdbcTemplate.update(sql, collectionId);
    }
} 