package com.example.registration.service;

import com.example.registration.dto.*;
import com.example.registration.model.Otp;
import com.example.registration.model.User;
import com.example.registration.model.UsernameValidationResult;
import com.example.registration.repository.OtpRepository;
import com.example.registration.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final UserRepository userRepository;
    private final OtpRepository otpRepository;
    private final EmailOtpService emailOtpService;
    private final WhatsAppOtpService whatsAppOtpService;
    private final IdGenerationApiClient idGenerationApiClient;

    @Value("${otp.validity.minutes}")
    private int otpValidityMinutes;

    @Value("${registration.base.price}")
    private double basePrice;

    @Transactional
    public ApiResponse<String> startRegistration(RegistrationRequest request) {
        // Check if user with this email or phone already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ApiResponse.error("Email already registered");
        }

        if (userRepository.findByPhone(request.getPhone()).isPresent()) {
            return ApiResponse.error("Phone already registered");
        }

        // Create a pending user
        User user = User.builder()
                .email(request.getEmail())
                .phone(request.getPhone())
                .status("PENDING")
                .build();
        userRepository.save(user);

        // Generate OTPs
        String emailOtp = generateOtp();
        String phoneOtp = generateOtp();

        // Save OTPs
        Otp otp = Otp.builder()
                .email(request.getEmail())
                .phone(request.getPhone())
                .emailOtp(emailOtp)
                .phoneOtp(phoneOtp)
                .expiresAt(LocalDateTime.now().plusMinutes(otpValidityMinutes))
                .build();
        otpRepository.save(otp);

        // Send OTPs
        emailOtpService.sendOtp(request.getEmail(), emailOtp);
        whatsAppOtpService.sendOtp(request.getPhone(), phoneOtp);

        return ApiResponse.success("OTP sent to your email and phone");
    }

    @Transactional
    public ApiResponse<String> verifyOtp(OtpVerificationRequest request) {
        // Get the latest OTP for this email and phone
        Otp otp = otpRepository.findLatestByEmailAndPhone(request.getEmail(), request.getPhone())
                .orElseThrow(() -> new RuntimeException("No OTP found for this email and phone"));

        // Check if OTP is expired
        if (LocalDateTime.now().isAfter(otp.getExpiresAt())) {
            return ApiResponse.error("OTP expired");
        }

        // Check if OTP is correct
        if (!otp.getEmailOtp().equals(request.getEmailOtp()) || !otp.getPhoneOtp().equals(request.getPhoneOtp())) {
            return ApiResponse.error("Invalid OTP");
        }

        // Mark OTP as verified
        otpRepository.markAsVerified(otp.getId());

        return ApiResponse.success("OTP verified successfully");
    }

    @Transactional
    public ApiResponse<UsernameValidationResult> validateUsername(UsernameValidationRequest request) {
        // Ensure the user has verified OTP recently
        if (!otpRepository.isRecentlyVerified(request.getEmail(), request.getPhone())) {
            return ApiResponse.error("Please verify OTP first");
        }

        String username;
        
        // If username is not provided, generate one
        if (request.getUsername() == null || request.getUsername().isEmpty()) {
            username = idGenerationApiClient.generateUsername();
        } else {
            username = request.getUsername();
        }

        // Check if username is available
        if (userRepository.existsByUsernameAndStatusActive(username)) {
            return ApiResponse.error("Username already taken");
        }

        // Check if it's a fancy username and calculate price
        UsernameValidationResult result = idGenerationApiClient.checkUsername(username);
        result.setBasePrice(basePrice);
        
        if (result.isFancy()) {
            double totalPrice = result.getBasePrice() + result.getFancyPrice();
            result.setTotalPrice(totalPrice);
        } else {
            result.setTotalPrice(result.getBasePrice());
        }

        return ApiResponse.success("Username validation successful", result);
    }

    @Transactional
    public ApiResponse<String> initiatePayment(PaymentRequest request) {
        // Ensure the user has verified OTP recently
        if (!otpRepository.isRecentlyVerified(request.getEmail(), request.getPhone())) {
            return ApiResponse.error("Please verify OTP first");
        }

        // Get the user
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Initiate payment through ID Generation Service
        String paymentUrl = idGenerationApiClient.initiatePayment(request);

        return ApiResponse.success("Payment initiated", paymentUrl);
    }

    @Transactional
    public ApiResponse<String> completeRegistration(String email, String phone, String username) {
        // Get the user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update username and activate the user
        userRepository.updateUsername(user.getId(), username);

        // Send confirmation email and WhatsApp message
        emailOtpService.sendConfirmation(email, username);
        whatsAppOtpService.sendConfirmation(phone, username);

        return ApiResponse.success("Registration completed successfully");
    }

    private String generateOtp() {
        // Generate a 6-digit OTP
        return String.format("%06d", (int) (Math.random() * 1000000));
    }
}