package com.example.idgeneration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateUsernameRequest {
    // Optional: Can be empty if we just want a completely random username
    private String prefix; // e.g., "ABC"
    private String suffix; // e.g., "123"
}