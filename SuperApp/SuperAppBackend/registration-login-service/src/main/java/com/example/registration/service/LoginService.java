package com.example.registration.service;

import com.example.registration.dto.ApiResponse;
import com.example.registration.dto.LoginOtpVerificationRequest;
import com.example.registration.dto.LoginRequest;
import com.example.registration.model.LoginSession;
import com.example.registration.model.User;
import com.example.registration.model.UserSession;
import com.example.registration.repository.LoginSessionRepository;
import com.example.registration.repository.UserRepository;
import com.example.registration.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LoginService {

    private static final Logger logger = LoggerFactory.getLogger(LoginService.class);
    private static final int MAX_ACTIVE_SESSIONS = 4;
    
    private final UserRepository userRepository;
    private final LoginSessionRepository loginSessionRepository;
    private final UserSessionRepository userSessionRepository;
    private final WhatsAppOtpService whatsAppOtpService;

    @Value("${otp.validity.minutes}")
    private int otpValidityMinutes;

    @Transactional
    public ApiResponse<Map<String, Object>> login(LoginRequest request) {
        try {
            logger.debug("Processing login request for username: {}", request.getUsername());
            
            // Check if user exists and is active
            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!"ACTIVE".equals(user.getStatus())) {
                logger.warn("Login attempt for inactive user: {}", request.getUsername());
                return ApiResponse.error("User account is not active");
            }

            // Check active sessions
            List<UserSession> activeSessions = userSessionRepository.findActiveSessionsByUsername(request.getUsername());
            if (activeSessions.size() >= MAX_ACTIVE_SESSIONS) {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Already logged in on 4 devices");
                response.put("options", List.of("logout_oldest_and_continue", "cancel_login"));
                return ApiResponse.success("Max sessions reached", response);
            }

            // Generate OTP
            String otp = generateOtp();
            logger.debug("Generated OTP for user: {}", request.getUsername());

            // Create and save login session
            LoginSession session = LoginSession.builder()
                    .username(user.getUsername())
                    .phone(user.getPhone())
                    .otp(otp)
                    .expiresAt(LocalDateTime.now().plusMinutes(otpValidityMinutes))
                    .verified(false)
                    .build();

            loginSessionRepository.save(session);
            logger.debug("Login session saved for user: {}", request.getUsername());

            // Send OTP via WhatsApp
            whatsAppOtpService.sendLoginOtp(user.getPhone(), otp);
            logger.info("Login OTP sent successfully to user: {}", request.getUsername());
            Map<String, Object> data = new HashMap<>();
            data.put("message", "OTP sent successfully");
            return ApiResponse.success("OTP sent to your WhatsApp", data);
            
        } catch (Exception e) {
            logger.error("Error during login process: {}", e.getMessage(), e);
            throw new RuntimeException("Login failed: " + e.getMessage());
        }
    }

    @Transactional
    public ApiResponse<Map<String, Object>> verifyLoginOtp(LoginOtpVerificationRequest request) {
        try {
            logger.debug("Verifying login OTP for username: {}", request.getUsername());
            
            // Get user
            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Get latest login session
            LoginSession session = loginSessionRepository.findLatestByUsername(request.getUsername())
                    .orElseThrow(() -> new RuntimeException("No login session found"));

            // Validate OTP
            if (LocalDateTime.now().isAfter(session.getExpiresAt())) {
                logger.warn("Expired OTP attempt for user: {}", request.getUsername());
                return ApiResponse.error("OTP expired");
            }

            if (!session.getOtp().equals(request.getOtp())) {
                logger.warn("Invalid OTP attempt for user: {}", request.getUsername());
                return ApiResponse.error("Invalid OTP");
            }

            // Mark session as verified
            loginSessionRepository.markAsVerified(session.getId());
            logger.debug("Login session verified for user: {}", request.getUsername());

            // Create user session
            String sessionId = request.getUsername() + "_" + System.currentTimeMillis();
            UserSession userSession = UserSession.builder()
                    .username(request.getUsername())
                    .sessionId(sessionId)
                    .expiresAt(LocalDateTime.now().plusHours(48))
                    .build();
            userSessionRepository.save(userSession);

            logger.info("Login successful for user: {}", request.getUsername());
            Map<String, Object> data = new HashMap<>();
            data.put("sessionId", sessionId);
            return ApiResponse.success("Login successful", data);
            
        } catch (Exception e) {
            logger.error("Error during OTP verification: {}", e.getMessage(), e);
            throw new RuntimeException("OTP verification failed: " + e.getMessage());
        }
    }

    @Transactional
    public ApiResponse<Map<String, Object>> continueWithOldestLogout(LoginRequest request) {
        try {
            // Delete oldest session
            userSessionRepository.deleteOldestSession(request.getUsername());

            // Proceed with normal login flow
            return login(request);
        } catch (Exception e) {
            logger.error("Error during oldest session logout: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to logout oldest session: " + e.getMessage());
        }
    }

    @Transactional
    public ApiResponse<Map<String, Object>> logout(String username, String sessionId) {
      try {
            userSessionRepository.deleteSession(username, sessionId);
        
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Logged out successfully");
            return ApiResponse.success("Logout successful", response);
        } catch (Exception e) {
             logger.error("Error during logout: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to logout: " + e.getMessage());
        }
    }

    private String generateOtp() {
        return String.format("%06d", (int) (Math.random() * 1000000));
    }
}