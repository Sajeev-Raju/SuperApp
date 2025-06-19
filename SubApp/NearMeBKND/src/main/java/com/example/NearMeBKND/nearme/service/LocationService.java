package com.example.NearMeBKND.nearme.service;

import com.example.NearMeBKND.nearme.model.UserLocation;
import com.example.NearMeBKND.nearme.repository.UserLocationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LocationService {
    private static final Logger logger = LoggerFactory.getLogger(LocationService.class);

    @Autowired
    private UserLocationRepository userLocationRepository;

    @Transactional
    public UserLocation saveLocation(UserLocation location) {
        try {
            logger.info("Saving location for userId: {}", location.getUserId());
            
            // Validate required fields
            if (location.getUserId() == null || location.getUserId().trim().isEmpty()) {
                throw new IllegalArgumentException("User ID is required");
            }
            if (location.getLatitude() == null) {
                throw new IllegalArgumentException("Latitude is required");
            }
            if (location.getLongitude() == null) {
                throw new IllegalArgumentException("Longitude is required");
            }
            
            // Check if location already exists for user
            userLocationRepository.findByUserId(location.getUserId())
                .ifPresent(existingLocation -> {
                    logger.info("Updating existing location for userId: {}", location.getUserId());
                    location.setId(existingLocation.getId());
                });
            
            return userLocationRepository.save(location);
        } catch (Exception e) {
            logger.error("Error saving location: {}", e.getMessage(), e);
            throw e;
        }
    }

    public UserLocation getUserLocation(String userId) {
        try {
            logger.info("Getting user location for userId: {}", userId);
            return userLocationRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Location not found for user: " + userId));
        } catch (Exception e) {
            logger.error("Error getting location for user {}: {}", userId, e.getMessage(), e);
            throw e;
        }
    }
} 