package com.example.registration.repository;

import com.example.registration.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<User> userRowMapper = (ResultSet rs, int rowNum) -> {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setEmail(rs.getString("email"));
        user.setPhone(rs.getString("phone"));
        user.setUsername(rs.getString("username"));
        user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        user.setStatus(rs.getString("status"));
        return user;
    };

    public Optional<User> findByEmail(String email) {
        try {
            User user = jdbcTemplate.queryForObject(
                    "SELECT * FROM users WHERE email = ?",
                    userRowMapper,
                    email
            );
            return Optional.ofNullable(user);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Optional<User> findByPhone(String phone) {
        try {
            User user = jdbcTemplate.queryForObject(
                    "SELECT * FROM users WHERE phone = ?",
                    userRowMapper,
                    phone
            );
            return Optional.ofNullable(user);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Optional<User> findByUsername(String username) {
        try {
            User user = jdbcTemplate.queryForObject(
                    "SELECT * FROM users WHERE username = ?",
                    userRowMapper,
                    username
            );
            return Optional.ofNullable(user);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public User save(User user) {
        if (user.getId() == null) {
            // Insert new user
            jdbcTemplate.update(
                    "INSERT INTO users (email, phone, status) VALUES (?, ?, ?)",
                    user.getEmail(),
                    user.getPhone(),
                    user.getStatus()
            );
            
            // Get the newly created user
            return findByEmail(user.getEmail()).orElseThrow(() -> new RuntimeException("Failed to create user"));
        } else {
            // Update existing user
            jdbcTemplate.update(
                    "UPDATE users SET email = ?, phone = ?, username = ?, status = ? WHERE id = ?",
                    user.getEmail(),
                    user.getPhone(),
                    user.getUsername(),
                    user.getStatus(),
                    user.getId()
            );
            return user;
        }
    }

    public void updateUsername(Long userId, String username) {
        jdbcTemplate.update(
                "UPDATE users SET username = ?, status = 'ACTIVE' WHERE id = ?",
                username,
                userId
        );
    }

    public boolean existsByUsernameAndStatusActive(String username) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE username = ? AND status = 'ACTIVE'",
                Integer.class,
                username
        );
        return count != null && count > 0;
    }
}