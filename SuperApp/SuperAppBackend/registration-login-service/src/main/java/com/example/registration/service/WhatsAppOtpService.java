package com.example.registration.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class WhatsAppOtpService {

    private static final Logger logger = LoggerFactory.getLogger(WhatsAppOtpService.class);

    @Value("${twilio.account.sid}")
    private String accountSid;

    @Value("${twilio.auth.token}")
    private String authToken;

    @Value("${twilio.whatsapp.number}")
    private String twilioWhatsAppNumber;

    @PostConstruct
    private void init() {
        try {
            Twilio.init(accountSid, authToken);
            logger.info("Twilio initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize Twilio: {}", e.getMessage(), e);
        }
    }

    public void sendOtp(String phoneNumber, String otp) {
        try {
            Message message = Message.creator(
                    new PhoneNumber("whatsapp:" + phoneNumber),
                    new PhoneNumber("whatsapp:" + twilioWhatsAppNumber),
                    "Your OTP for registration is: " + otp + ". This OTP will expire in 5 minutes."
            ).create();
            
            logger.info("WhatsApp OTP sent successfully to {}, Message SID: {}", phoneNumber, message.getSid());
        } catch (Exception e) {
            logger.error("Failed to send WhatsApp OTP to {}: {}", phoneNumber, e.getMessage(), e);
            throw new RuntimeException("Failed to send WhatsApp OTP: " + e.getMessage());
        }
    }

    public void sendConfirmation(String phoneNumber, String username) {
        try {
            Message message = Message.creator(
                    new PhoneNumber("whatsapp:" + phoneNumber),
                    new PhoneNumber("whatsapp:" + twilioWhatsAppNumber),
                    "Congratulations! Your registration is complete. Your username is: " + username + 
                    ". You can now log in using this username and OTP verification."
            ).create();
            
            logger.info("WhatsApp confirmation sent successfully to {}, Message SID: {}", phoneNumber, message.getSid());
        } catch (Exception e) {
            logger.error("Failed to send WhatsApp confirmation to {}: {}", phoneNumber, e.getMessage(), e);
            throw new RuntimeException("Failed to send WhatsApp confirmation: " + e.getMessage());
        }
    }

    public void sendLoginOtp(String phoneNumber, String otp) {
        try {
            Message message = Message.creator(
                    new PhoneNumber("whatsapp:" + phoneNumber),
                    new PhoneNumber("whatsapp:" + twilioWhatsAppNumber),
                    "Your OTP for login is: " + otp + ". This OTP will expire in 5 minutes."
            ).create();
            
            logger.info("WhatsApp login OTP sent successfully to {}, Message SID: {}", phoneNumber, message.getSid());
        } catch (Exception e) {
            logger.error("Failed to send WhatsApp login OTP to {}: {}", phoneNumber, e.getMessage(), e);
            throw new RuntimeException("Failed to send WhatsApp login OTP: " + e.getMessage());
        }
    }
}