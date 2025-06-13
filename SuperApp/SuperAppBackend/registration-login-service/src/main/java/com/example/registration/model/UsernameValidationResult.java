package com.example.registration.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsernameValidationResult {
    private String username;
    private boolean available;
    private boolean isFancy;
    private String fancyType;
    private double basePrice;
    private double fancyPrice;
    private double totalPrice;
}