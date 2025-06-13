package com.example.registration.service;

import com.example.registration.dto.ApiResponse;
import com.example.registration.dto.SessionValidationRequest;
import com.example.registration.model.UserSession;
import com.example.registration.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SessionService {

    private static final Logger logger = LoggerFactory.getLogger(SessionService.class);
    private final UserSessionRepository userSessionRepository;

    @Transactional
    public ApiResponse<Map<String, Object>> validateSession(SessionValidationRequest request) {
        try {
            Optional<UserSession> sessionOpt = userSessionRepository.findByUsernameAndSessionId(
                request.getUsername(), 
                request.getSessionId()
            );

            Map<String, Object> response = new HashMap<>();

            if (sessionOpt.isEmpty()) {
                response.put("valid", false);
                response.put("message", "Session not found");
                return ApiResponse.success("Session status retrieved", response);
            }

            UserSession session = sessionOpt.get();

            if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
                response.put("valid", false);
                response.put("message", "Session expired. Please login again.");
                return ApiResponse.success("Session status retrieved", response);
            }

            // Refresh session
            userSessionRepository.refreshSession(request.getUsername(), request.getSessionId());
            
            response.put("valid", true);
            response.put("message", "Session refreshed successfully");
            return ApiResponse.success("Session status retrieved", response);

        } catch (Exception e) {
            logger.error("Error validating session: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to validate session: " + e.getMessage());
        }
    }
}