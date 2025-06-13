package com.example.idgeneration.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    private Long id;
    private String email;
    private String phone;
    private String username;
    private boolean isFancy;
    private String fancyType;
    private BigDecimal basePrice;
    private BigDecimal fancyPrice;
    private BigDecimal totalPrice;
    private String paymentId;
    private String paymentStatus; // PENDING, COMPLETED, FAILED
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}