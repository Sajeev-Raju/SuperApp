package com.example.registration.repository;

import com.example.registration.model.LoginSession;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LoginSessionRepository {

    private static final Logger logger = LoggerFactory.getLogger(LoginSessionRepository.class);
    private final JdbcTemplate jdbcTemplate;
    private static final DateTimeFormatter SQL_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RowMapper<LoginSession> sessionRowMapper = (ResultSet rs, int rowNum) -> {
        LoginSession session = new LoginSession();
        session.setId(rs.getLong("id"));
        session.setUsername(rs.getString("username"));
        session.setPhone(rs.getString("phone"));
        session.setOtp(rs.getString("otp"));
        session.setCreatedAt(LocalDateTime.parse(rs.getString("created_at"), SQL_FORMATTER));
        session.setExpiresAt(LocalDateTime.parse(rs.getString("expires_at"), SQL_FORMATTER));
        session.setVerified(rs.getInt("verified") == 1);
        return session;
    };

    public Optional<LoginSession> findLatestByUsername(String username) {
        try {
            String sql = "SELECT * FROM login_sessions WHERE username = ? ORDER BY created_at DESC LIMIT 1";
            logger.debug("Finding latest session for username: {}", username);
            
            LoginSession session = jdbcTemplate.queryForObject(sql, sessionRowMapper, username);
            return Optional.ofNullable(session);
        } catch (Exception e) {
            logger.debug("No session found for username: {}", username);
            return Optional.empty();
        }
    }

    public LoginSession save(LoginSession session) {
        try {
            logger.debug("Saving login session: {}", session);
            
            String sql = "INSERT INTO login_sessions (username, phone, otp, expires_at) VALUES (?, ?, ?, datetime(?))";
            
            int result = jdbcTemplate.update(sql,
                session.getUsername(),
                session.getPhone(),
                session.getOtp(),
                session.getExpiresAt().format(SQL_FORMATTER)
            );
            
            if (result == 0) {
                logger.error("Failed to insert login session");
                throw new RuntimeException("Failed to insert login session");
            }

            return findLatestByUsername(session.getUsername())
                    .orElseThrow(() -> {
                        logger.error("Failed to retrieve saved login session for username: {}", session.getUsername());
                        return new RuntimeException("Failed to save login session");
                    });
        } catch (Exception e) {
            logger.error("Error saving login session: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save login session: " + e.getMessage());
        }
    }

    public void markAsVerified(Long sessionId) {
        try {
            String sql = "UPDATE login_sessions SET verified = 1 WHERE id = ?";
            logger.debug("Marking session {} as verified", sessionId);
            
            int result = jdbcTemplate.update(sql, sessionId);
            
            if (result == 0) {
                logger.error("No session found with ID: {}", sessionId);
                throw new RuntimeException("Failed to mark session as verified");
            }
            
            logger.info("Session marked as verified: {}", sessionId);
        } catch (Exception e) {
            logger.error("Error marking session as verified: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to mark session as verified: " + e.getMessage());
        }
    }
}
