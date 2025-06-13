package com.example.registration.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionValidationRequest {
    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Session ID is required")
    private String sessionId;
}