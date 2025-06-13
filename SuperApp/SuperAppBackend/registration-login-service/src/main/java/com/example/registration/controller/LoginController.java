package com.example.registration.controller;

import com.example.registration.dto.ApiResponse;
import com.example.registration.dto.LoginOtpVerificationRequest;
import com.example.registration.dto.LoginRequest;
import com.example.registration.service.LoginService;
import com.example.registration.util.CookieUtil;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/login")
@RequiredArgsConstructor
public class LoginController {

    private final LoginService loginService;
    private final CookieUtil cookieUtil;

    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@Valid @RequestBody LoginRequest request) {
        ApiResponse<Map<String, Object>> response = loginService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyOtp(
            @Valid @RequestBody LoginOtpVerificationRequest request,
            HttpServletResponse httpResponse) {
        ApiResponse<Map<String, Object>> response = loginService.verifyLoginOtp(request);
        
        if (response.isSuccess() && response.getData() != null) {
            String sessionId = (String) response.getData().get("sessionId");
            String username = request.getUsername();
            
            // Set HTTP-only cookies
            cookieUtil.setCookie(httpResponse, "sessionId", sessionId);
            cookieUtil.setCookie(httpResponse, "username", username);
        }
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/continue-with-oldest-logout")
    public ResponseEntity<ApiResponse<Map<String, Object>>> continueWithOldestLogout(@Valid @RequestBody LoginRequest request) {
        ApiResponse<Map<String, Object>> response = loginService.continueWithOldestLogout(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Map<String, Object>>> logout(HttpServletResponse httpResponse) {
        // Clear cookies
        cookieUtil.clearCookie(httpResponse, "sessionId");
        cookieUtil.clearCookie(httpResponse, "username");
        
        Map<String, Object> response = Map.of("message", "Logged out successfully");
        return ResponseEntity.ok(ApiResponse.success("Logout successful", response));
    }
}