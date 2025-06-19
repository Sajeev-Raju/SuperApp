package com.example.NearMeBKND.service;

import com.example.NearMeBKND.nearme.model.UserLocation;
import com.example.NearMeBKND.nearme.repository.UserLocationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserLocationService {
    private static final Logger logger = LoggerFactory.getLogger(UserLocationService.class);

    @Autowired
    private UserLocationRepository userLocationRepository;

    public UserLocation saveLocation(String userId, Double latitude, Double longitude, String locationName) {
        logger.info("Saving location for userId: {}", userId);
        UserLocation loc = new UserLocation();
        loc.setUserId(userId);
        loc.setLatitude(latitude);
        loc.setLongitude(longitude);
        loc.setLocationName(locationName);
        return userLocationRepository.save(loc);
    }

    public UserLocation getUserLocation(String userId) {
        logger.info("Getting user location for userId: {}", userId);
        return userLocationRepository.findByUserId(userId)
            .orElseThrow(() -> new RuntimeException("Location not found for user: " + userId));
    }
} 