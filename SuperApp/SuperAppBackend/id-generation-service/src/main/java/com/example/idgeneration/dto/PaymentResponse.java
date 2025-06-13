package com.example.idgeneration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private String orderId;
    private String razorpayOrderId;
    private double amount;
    private String currency;
    private String receipt;
    private String paymentId;
    private String status;
    private String username;
}