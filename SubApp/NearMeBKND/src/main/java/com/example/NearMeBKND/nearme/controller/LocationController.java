package com.example.NearMeBKND.nearme.controller;

import com.example.NearMeBKND.nearme.model.UserLocation;
import com.example.NearMeBKND.nearme.service.LocationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/location")
public class LocationController {
    private static final Logger logger = LoggerFactory.getLogger(LocationController.class);

    @Autowired
    private LocationService locationService;

    @PostMapping("/save")
    public ResponseEntity<Map<String, Object>> saveLocation(@Valid @RequestBody UserLocation location) {
        try {
            logger.info("Received request to save location: {}", location);
            UserLocation savedLocation = locationService.saveLocation(location);
            return ResponseEntity.ok(Map.of(
                "message", "Location saved successfully",
                "data", savedLocation
            ));
        } catch (Exception e) {
            logger.error("Error saving location: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "message", "Error saving location: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserLocation> getUserLocation(@PathVariable String userId) {
        try {
            logger.info("Received request to get location for userId: {}", userId);
            UserLocation location = locationService.getUserLocation(userId);
            return ResponseEntity.ok(location);
        } catch (Exception e) {
            logger.error("Error getting location for user {}: {}", userId, e.getMessage(), e);
            throw e;
        }
    }
} 