package com.example.registration.controller;

import com.example.registration.dto.*;
import com.example.registration.model.UsernameValidationResult;
import com.example.registration.service.RegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/register")
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService registrationService;

    @PostMapping("/start")
    public ResponseEntity<ApiResponse<String>> startRegistration(@Valid @RequestBody RegistrationRequest request) {
        ApiResponse<String> response = registrationService.startRegistration(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<String>> verifyOtp(@Valid @RequestBody OtpVerificationRequest request) {
        ApiResponse<String> response = registrationService.verifyOtp(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/validate-username")
    public ResponseEntity<ApiResponse<UsernameValidationResult>> validateUsername(@Valid @RequestBody UsernameValidationRequest request) {
        ApiResponse<UsernameValidationResult> response = registrationService.validateUsername(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/initiate-payment")
    public ResponseEntity<ApiResponse<String>> initiatePayment(@Valid @RequestBody PaymentRequest request) {
        ApiResponse<String> response = registrationService.initiatePayment(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/complete")
    public ResponseEntity<ApiResponse<String>> completeRegistration(
            @RequestParam String email,
            @RequestParam String phone,
            @RequestParam String username) {
        ApiResponse<String> response = registrationService.completeRegistration(email, phone, username);
        return ResponseEntity.ok(response);
    }
}