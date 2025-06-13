package com.example.idgeneration.controller;

import com.example.idgeneration.dto.ApiResponse;
import com.example.idgeneration.dto.PaymentRequest;
import com.example.idgeneration.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/id-generation/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/initiate")
    public ResponseEntity<ApiResponse<Map<String, String>>> initiatePayment(@RequestBody PaymentRequest request) {
        ApiResponse<Map<String, String>> response = paymentService.initiateQrPayment(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPaymentStatus(@RequestParam("paymentId") String paymentId) {
        ApiResponse<Map<String, Object>> response = paymentService.getPaymentStatus(paymentId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse<Void>> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String signature) {
        ApiResponse<Void> response = paymentService.handleWebhook(payload, signature);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/callback")
    public ResponseEntity<Void> handleCallback(
            @RequestParam("razorpay_payment_id") String paymentId,
            @RequestParam("razorpay_payment_link_id") String paymentLinkId,
            @RequestParam("razorpay_payment_link_reference_id") String referenceId,
            @RequestParam("razorpay_payment_link_status") String status,
            @RequestParam("razorpay_signature") String signature) {

        boolean isValid = paymentService.verifyCallbackSignature(paymentLinkId, paymentId, signature);
        if (!isValid) {
            return ResponseEntity.status(302)
                    .header("Location", "http://localhost:5173/register/error?reason=invalid_signature")
                    .build();
        }

        if (!"paid".equals(status)) {
            return ResponseEntity.status(302)
                    .header("Location", "http://localhost:5173/register/error?reason=payment_failed")
                    .build();
        }

        return ResponseEntity.status(302)
                .header("Location", "http://localhost:5173/register/success")
                .build();
    }
}