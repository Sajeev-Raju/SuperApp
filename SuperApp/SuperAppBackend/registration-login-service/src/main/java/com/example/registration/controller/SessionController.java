package com.example.registration.controller;

import com.example.registration.dto.ApiResponse;
import com.example.registration.dto.SessionValidationRequest;
import com.example.registration.service.SessionService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/session")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateSession(
            HttpServletRequest request) {
        
        Cookie[] cookies = request.getCookies();
        String username = null;
        String sessionId = null;

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("username".equals(cookie.getName())) {
                    username = cookie.getValue();
                } else if ("sessionId".equals(cookie.getName())) {
                    sessionId = cookie.getValue();
                }
            }
        }

        if (username == null || sessionId == null) {
            Map<String, Object> response = Map.of(
                "valid", false,
                "message", "Session not found"
            );
            return ResponseEntity.ok(ApiResponse.success("Session status retrieved", response));
        }

        SessionValidationRequest validationRequest = new SessionValidationRequest();
        validationRequest.setUsername(username);
        validationRequest.setSessionId(sessionId);

        ApiResponse<Map<String, Object>> response = sessionService.validateSession(validationRequest);
        return ResponseEntity.ok(response);
    }
}