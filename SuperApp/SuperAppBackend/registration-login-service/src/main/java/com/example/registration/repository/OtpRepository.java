package com.example.registration.repository;

import com.example.registration.model.Otp;
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
public class OtpRepository {

    private static final Logger logger = LoggerFactory.getLogger(OtpRepository.class);
    private final JdbcTemplate jdbcTemplate;
    private static final DateTimeFormatter SQL_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RowMapper<Otp> otpRowMapper = (ResultSet rs, int rowNum) -> {
        Otp otp = new Otp();
        otp.setId(rs.getLong("id"));
        otp.setEmail(rs.getString("email"));
        otp.setPhone(rs.getString("phone"));
        otp.setEmailOtp(rs.getString("email_otp"));
        otp.setPhoneOtp(rs.getString("phone_otp"));
        otp.setCreatedAt(LocalDateTime.parse(rs.getString("created_at"), SQL_FORMATTER));
        otp.setExpiresAt(LocalDateTime.parse(rs.getString("expires_at"), SQL_FORMATTER));
        otp.setVerified(rs.getInt("verified") == 1);
        return otp;
    };

    public Optional<Otp> findLatestByEmailAndPhone(String email, String phone) {
        try {
            String sql = "SELECT * FROM otps WHERE email = ? AND phone = ? ORDER BY created_at DESC LIMIT 1";
            logger.debug("Executing SQL: {} with params: [{}, {}]", sql, email, phone);
            
            Otp otp = jdbcTemplate.queryForObject(sql, otpRowMapper, email, phone);
            return Optional.ofNullable(otp);
        } catch (Exception e) {
            logger.debug("No OTP found for email: {} and phone: {}", email, phone);
            return Optional.empty();
        }
    }

    public Otp save(Otp otp) {
        try {
            String sql = "INSERT INTO otps (email, phone, email_otp, phone_otp, expires_at) VALUES (?, ?, ?, ?, datetime(?))";
            logger.debug("Executing SQL: {} with params: [{}, {}, {}, {}, {}]", 
                sql, otp.getEmail(), otp.getPhone(), otp.getEmailOtp(), otp.getPhoneOtp(), 
                otp.getExpiresAt().format(SQL_FORMATTER));

            jdbcTemplate.update(sql,
                otp.getEmail(),
                otp.getPhone(),
                otp.getEmailOtp(),
                otp.getPhoneOtp(),
                otp.getExpiresAt().format(SQL_FORMATTER)
            );

            return findLatestByEmailAndPhone(otp.getEmail(), otp.getPhone())
                    .orElseThrow(() -> {
                        logger.error("Failed to retrieve saved OTP for email: {} and phone: {}", 
                            otp.getEmail(), otp.getPhone());
                        return new RuntimeException("Failed to save OTP");
                    });
        } catch (Exception e) {
            logger.error("Error saving OTP: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save OTP: " + e.getMessage());
        }
    }

    public void markAsVerified(Long otpId) {
        try {
            String sql = "UPDATE otps SET verified = 1 WHERE id = ?";
            logger.debug("Executing SQL: {} with param: [{}]", sql, otpId);
            
            jdbcTemplate.update(sql, otpId);
            logger.info("OTP marked as verified: {}", otpId);
        } catch (Exception e) {
            logger.error("Error marking OTP as verified: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to mark OTP as verified: " + e.getMessage());
        }
    }

    public boolean isRecentlyVerified(String email, String phone) {
        try {
            String sql = "SELECT COUNT(*) FROM otps WHERE email = ? AND phone = ? AND verified = 1 AND datetime(created_at) > datetime('now', '-1 hour')";
            logger.debug("Executing SQL: {} with params: [{}, {}]", sql, email, phone);
            
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, email, phone);
            return count != null && count > 0;
        } catch (Exception e) {
            logger.error("Error checking recent verification: {}", e.getMessage(), e);
            return false;
        }
    }
}
