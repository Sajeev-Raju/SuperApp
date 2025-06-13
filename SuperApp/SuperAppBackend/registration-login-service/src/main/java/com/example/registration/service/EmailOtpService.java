package com.example.registration.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailOtpService {

    private static final Logger logger = LoggerFactory.getLogger(EmailOtpService.class);
    private final JavaMailSender mailSender;

    public void sendOtp(String email, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("Your OTP for Registration");
            message.setText("Your OTP for registration is: " + otp + 
                          "\nThis OTP will expire in 5 minutes.");

            mailSender.send(message);
            logger.info("OTP email sent successfully to: {}", email);
        } catch (Exception e) {
            logger.error("Failed to send OTP email to {}: {}", email, e.getMessage(), e);
            throw new RuntimeException("Failed to send OTP email: " + e.getMessage());
        }
    }

    public void sendConfirmation(String email, String username) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("Registration Successful");
            message.setText("Congratulations! Your registration is complete.\n" +
                          "Your username is: " + username + "\n" +
                          "You can now log in using this username and OTP verification.");

            mailSender.send(message);
            logger.info("Confirmation email sent successfully to: {}", email);
        } catch (Exception e) {
            logger.error("Failed to send confirmation email to {}: {}", email, e.getMessage(), e);
            throw new RuntimeException("Failed to send confirmation email: " + e.getMessage());
        }
    }
}