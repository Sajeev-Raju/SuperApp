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
public class Otp {
    private Long id;
    private String email;
    private String phone;
    private String emailOtp;
    private String phoneOtp;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private boolean verified;
}