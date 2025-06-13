package com.example.registration.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginSession {
    private Long id;
    private String username;
    private String phone;
    private String otp;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private boolean verified;
}