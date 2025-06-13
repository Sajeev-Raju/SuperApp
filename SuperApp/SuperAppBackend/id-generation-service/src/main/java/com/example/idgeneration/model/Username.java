package com.example.idgeneration.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Username {
    private Long id;
    private String username;
    private boolean isAssigned;
    private LocalDateTime createdAt;
}