package com.example.registration.repository;

import com.example.registration.model.UserSession;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.ResultSet;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserSessionRepository {

    private static final Logger logger = LoggerFactory.getLogger(UserSessionRepository.class);
    private final JdbcTemplate jdbcTemplate;
    private static final DateTimeFormatter SQL_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RowMapper<UserSession> sessionRowMapper = (ResultSet rs, int rowNum) -> {
        logger.debug("Mapping row: id={}, username={}, session_id={}, created_at={}, expires_at={}", 
            rs.getLong("id"), rs.getString("username"), rs.getString("session_id"), 
            rs.getString("created_at"), rs.getString("expires_at"));
        UserSession session = new UserSession();
        session.setId(rs.getLong("id"));
        session.setUsername(rs.getString("username"));
        session.setSessionId(rs.getString("session_id"));
        try {
            String createdAtStr = rs.getString("created_at");
            String expiresAtStr = rs.getString("expires_at");
            session.setCreatedAt(createdAtStr != null 
                ? LocalDateTime.parse(createdAtStr, SQL_FORMATTER) 
                : null);
            session.setExpiresAt(expiresAtStr != null 
                ? LocalDateTime.parse(expiresAtStr, SQL_FORMATTER) 
                : null);
        } catch (Exception e) {
            logger.error("Error mapping timestamps: created_at={}, expires_at={}, error: {}", 
                rs.getString("created_at"), rs.getString("expires_at"), e.getMessage(), e);
            throw e;
        }
        return session;
    };

    public Optional<UserSession> findByUsernameAndSessionId(String username, String sessionId) {
        logger.info("Querying session for username: '{}', sessionId: '{}'", username, sessionId);
        username = username != null ? username.trim() : username;
        sessionId = sessionId != null ? sessionId.trim() : sessionId;
        try {
            String sql = "SELECT * FROM user_sessions WHERE username = ? AND session_id = ?";
            List<UserSession> sessions = jdbcTemplate.query(sql, sessionRowMapper, username, sessionId);
            if (sessions.isEmpty()) {
                logger.info("No sessions found for username: '{}', sessionId: '{}'", username, sessionId);
                return Optional.empty();
            }
            if (sessions.size() > 1) {
                logger.warn("Multiple sessions found for username: '{}', sessionId: '{}', count: {}", 
                    username, sessionId, sessions.size());
            }
            UserSession session = sessions.get(0);
            logger.info("Found session: id={}, username={}, sessionId={}, expiresAt={}", 
                session.getId(), session.getUsername(), session.getSessionId(), session.getExpiresAt());
            return Optional.of(session);
        } catch (Exception e) {
            logger.error("Error querying session for username: '{}', sessionId: '{}', error: {}", 
                username, sessionId, e.getMessage(), e);
            String debugSql = "SELECT * FROM user_sessions WHERE username = ?";
            List<UserSession> debugSessions = jdbcTemplate.query(debugSql, sessionRowMapper, username);
            logger.info("Debug: Found {} sessions for username: '{}'", debugSessions.size(), username);
            debugSessions.forEach(s -> logger.info("Session: id={}, sessionId={}, createdAt={}, expiresAt={}", 
                s.getId(), s.getSessionId(), s.getCreatedAt(), s.getExpiresAt()));
            return Optional.empty();
        }
    }

    public int refreshSession(String username, String sessionId) {
        LocalDateTime newExpiresAt = LocalDateTime.now().plusHours(48);
        String formattedExpiresAt = newExpiresAt.format(SQL_FORMATTER);
        String sql = "UPDATE user_sessions SET created_at = CURRENT_TIMESTAMP, expires_at = ? " +
                     "WHERE username = ? AND session_id = ?";
        logger.info("Refreshing session for username: '{}', session_id: '{}', expires_at: '{}'", 
            username, sessionId, formattedExpiresAt);
        int rowsAffected = jdbcTemplate.update(sql, formattedExpiresAt, username, sessionId);
        logger.info("Refreshed session for username: '{}', session_id: '{}', rows affected: {}", 
            username, sessionId, rowsAffected);
        return rowsAffected;
    }

    public UserSession save(UserSession session) {
        String formattedExpiresAt = session.getExpiresAt().format(SQL_FORMATTER);
        logger.info("Saving session for username: '{}', session_id: '{}', expires_at: '{}'", 
            session.getUsername(), session.getSessionId(), formattedExpiresAt);
        String sql = "INSERT INTO user_sessions (username, session_id, expires_at) VALUES (?, ?, ?)";
        int result = jdbcTemplate.update(sql, 
            session.getUsername(),
            session.getSessionId(),
            formattedExpiresAt
        );
        UserSession savedSession = findByUsernameAndSessionId(session.getUsername(), session.getSessionId())
                .orElseThrow(() -> new RuntimeException("Failed to save user session"));
        logger.info("Saved session: id={}, expiresAt={}", savedSession.getId(), savedSession.getExpiresAt());
        return savedSession;
    }
    public List<UserSession> findActiveSessionsByUsername(String username) {
        String sql = "SELECT * FROM user_sessions WHERE username = ? AND expires_at > ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, sessionRowMapper, username, LocalDateTime.now());
    }

    public void deleteOldestSession(String username) {
        String sql = "DELETE FROM user_sessions WHERE id = (" +
                     "SELECT id FROM user_sessions WHERE username = ? ORDER BY created_at ASC LIMIT 1)";
        jdbcTemplate.update(sql, username);
    }

    public void deleteSession(String username, String sessionId) {
        String sql = "DELETE FROM user_sessions WHERE username = ? AND session_id = ?";
        jdbcTemplate.update(sql, username, sessionId);
    }

    public void deleteExpiredSessions() {
        String sql = "DELETE FROM user_sessions WHERE expires_at < CURRENT_TIMESTAMP";
        jdbcTemplate.update(sql);
    }
}
