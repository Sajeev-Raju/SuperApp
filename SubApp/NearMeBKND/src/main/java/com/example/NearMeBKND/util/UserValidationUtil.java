package com.example.NearMeBKND.util;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class UserValidationUtil {
    private final JdbcTemplate jdbcTemplate;

    public UserValidationUtil(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean isUserExists(String userId) {
        String sql = "SELECT COUNT(*) FROM user_locations WHERE user_id = ?";
        int count = jdbcTemplate.queryForObject(sql, Integer.class, userId);
        return count > 0;
    }
} 