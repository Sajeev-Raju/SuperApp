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
public class UserSession {
    private Long id;
    private String username;
    private String sessionId;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}