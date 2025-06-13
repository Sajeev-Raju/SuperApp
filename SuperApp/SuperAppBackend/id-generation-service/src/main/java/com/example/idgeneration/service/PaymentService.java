package com.example.idgeneration.service;

import com.example.idgeneration.dto.ApiResponse;
import com.example.idgeneration.dto.PaymentRequest;
import com.example.idgeneration.model.Transaction;
import com.example.idgeneration.repository.TransactionRepository;
import com.razorpay.PaymentLink;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final TransactionRepository transactionRepository;
    private final IdGenerationService idGenerationService;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    @Value("${razorpay.currency}")
    private String currency;

    @Value("${razorpay.webhook.secret}")
    private String webhookSecret;

    @Transactional
public ApiResponse<Map<String, String>> initiateQrPayment(PaymentRequest request) {
    try {
        RazorpayClient razorpayClient = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

        int amountInPaise = (int) (request.getTotalPrice() * 100);

        // Prepare Payment Link request
        JSONObject paymentRequest = new JSONObject();
        paymentRequest.put("amount", amountInPaise);
        paymentRequest.put("currency", currency);
        paymentRequest.put("description", "Payment for username: " + request.getUsername());
        paymentRequest.put("reference_id", "txn_" + System.currentTimeMillis());
        paymentRequest.put("callback_url", "http://localhost:8081/api/id-generation/payment/callback");
        paymentRequest.put("callback_method", "get");

        // Customer details
        JSONObject customer = new JSONObject();
        customer.put("name", request.getUsername());
        customer.put("email", request.getEmail());
        customer.put("contact", request.getPhone());
        paymentRequest.put("customer", customer);

        // Notification settings
        JSONObject notify = new JSONObject();
        notify.put("sms", true);
        notify.put("email", true);
        paymentRequest.put("notify", notify);

        // Payment settings
        paymentRequest.put("reminder_enable", true);
        paymentRequest.put("accept_partial", false);

        // Add notes to track payment link ID in webhook
        JSONObject notes = new JSONObject();
        notes.put("username", request.getUsername());
        notes.put("email", request.getEmail());
        paymentRequest.put("notes", notes);

        // Create payment link
        PaymentLink paymentLink = razorpayClient.paymentLink.create(paymentRequest);

        // Save transaction
        Transaction transaction = Transaction.builder()
                .email(request.getEmail())
                .phone(request.getPhone())
                .username(request.getUsername())
                .isFancy(request.isFancy())
                .fancyType(request.getFancyType())
                .basePrice(BigDecimal.valueOf(request.getBasePrice()))
                .fancyPrice(BigDecimal.valueOf(request.getFancyPrice()))
                .totalPrice(BigDecimal.valueOf(request.getTotalPrice()))
                .paymentId(paymentLink.get("id"))
                .paymentStatus("PENDING")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        transactionRepository.save(transaction);

        // Return payment link details
        Map<String, String> response = new HashMap<>();
        response.put("paymentLink", paymentLink.get("short_url"));
        response.put("paymentLinkId", paymentLink.get("id"));
        response.put("qrCodeUrl", paymentLink.get("short_url") + ".qr"); // QR code URL

        return ApiResponse.success("Payment Link generated successfully", response);
    } catch (RazorpayException e) {
        System.err.println("Razorpay Error: " + e.getMessage());
        return ApiResponse.error("Failed to generate payment link: " + e.getMessage());
    }
    }

    @Transactional(readOnly = true)
    public ApiResponse<Map<String, Object>> getPaymentStatus(String paymentId) {
        try {
            Transaction transaction = transactionRepository.findByPaymentId(paymentId)
                    .orElseThrow(() -> new RuntimeException("Transaction not found"));

            Map<String, Object> result = new HashMap<>();
            result.put("orderId", paymentId);
            result.put("status", transaction.getPaymentStatus());
            result.put("username", transaction.getUsername());
            result.put("amount", transaction.getTotalPrice());
            result.put("email", transaction.getEmail());
            result.put("phone", transaction.getPhone());

            return ApiResponse.success("Payment status retrieved", result);
        } catch (Exception e) {
            return ApiResponse.error("Failed to get payment status: " + e.getMessage());
        }
    }

    @Transactional
    public ApiResponse<Void> handleWebhook(String payload, String signature) {
        try {
            boolean isValid = Utils.verifyWebhookSignature(payload, signature, webhookSecret);
            if (!isValid) {
                return ApiResponse.error("Invalid webhook signature");
            }

            JSONObject webhookData = new JSONObject(payload);
            String event = webhookData.getString("event");
            System.out.println("Webhook Event: " + event);
            System.out.println("Webhook Payload: " + webhookData.toString());

            if ("payment.captured".equals(event)) {
                JSONObject payment = webhookData.getJSONObject("payload").getJSONObject("payment").getJSONObject("entity");
                JSONObject notes = payment.optJSONObject("notes");
                String paymentLinkId = notes != null ? notes.optString("payment_link_id", "") : payment.optString("payment_link_id", "");
                System.out.println("Payment Link ID: " + paymentLinkId);

                if (paymentLinkId != null && !paymentLinkId.isEmpty()) {
                    Transaction transaction = transactionRepository.findByPaymentId(paymentLinkId)
                            .orElseThrow(() -> new RuntimeException("Transaction not found"));

                    transaction.setPaymentStatus("COMPLETED");
                    transaction.setUpdatedAt(LocalDateTime.now());
                    transactionRepository.save(transaction);

                    idGenerationService.markUsernameAsAssigned(transaction.getUsername());
                } else {
                    System.out.println("Payment Link ID not found in webhook payload");
                }
            }

            return ApiResponse.success("Webhook processed successfully", null);
        } catch (Exception e) {
            return ApiResponse.error("Failed to process webhook: " + e.getMessage());
        }
    }

    public boolean verifyCallbackSignature(String paymentLinkId, String paymentId, String signature) {
        try {
            String payload = paymentLinkId + "|" + paymentId;
            String computedSignature = hmacSha256(payload, webhookSecret);
            System.out.println("Computed Signature: " + computedSignature);
            System.out.println("Expected Signature: " + signature);
            System.out.println("Webhook Secret: " + webhookSecret);
            return computedSignature.equals(signature);
        } catch (Exception e) {
            throw new RuntimeException("Failed to verify callback signature: " + e.getMessage());
        }
    }

    private String hmacSha256(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}