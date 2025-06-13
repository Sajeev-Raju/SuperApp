package com.example.registration.service;

import com.example.registration.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SessionCleanupService {

    private static final Logger logger = LoggerFactory.getLogger(SessionCleanupService.class);
    private final UserSessionRepository userSessionRepository;

    @Scheduled(fixedRate = 24 * 60 * 60 * 1000) // Run every 24 hours
    public void cleanupExpiredSessions() {
        logger.info("Starting cleanup of expired sessions");
        userSessionRepository.deleteExpiredSessions();
        logger.info("Completed cleanup of expired sessions");
    }
}