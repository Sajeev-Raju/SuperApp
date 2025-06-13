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
public class User {
    private Long id;
    private String email;
    private String phone;
    private String username;
    private LocalDateTime createdAt;
    private String status; // PENDING, ACTIVE, SUSPENDED
}