package com.example.idgeneration.repository;

import com.example.idgeneration.model.Username;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UsernameRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Username> usernameRowMapper = (ResultSet rs, int rowNum) -> {
        Username username = new Username();
        username.setId(rs.getLong("id"));
        username.setUsername(rs.getString("username"));
        username.setAssigned(rs.getBoolean("is_assigned"));
        username.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return username;
    };

    public Optional<Username> findByUsername(String username) {
        try {
            Username result = jdbcTemplate.queryForObject(
                    "SELECT * FROM usernames WHERE username = ?",
                    usernameRowMapper,
                    username
            );
            return Optional.ofNullable(result);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Username save(Username username) {
        jdbcTemplate.update(
                "INSERT INTO usernames (username, is_assigned) VALUES (?, ?)",
                username.getUsername(),
                username.isAssigned()
        );
        
        return findByUsername(username.getUsername())
                .orElseThrow(() -> new RuntimeException("Failed to save username"));
    }

    public void markAsAssigned(String username) {
        jdbcTemplate.update(
                "UPDATE usernames SET is_assigned = 1 WHERE username = ?",
                username
        );
    }

    public boolean existsByUsername(String username) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM usernames WHERE username = ?",
                Integer.class,
                username
        );
        return count != null && count > 0;
    }
}