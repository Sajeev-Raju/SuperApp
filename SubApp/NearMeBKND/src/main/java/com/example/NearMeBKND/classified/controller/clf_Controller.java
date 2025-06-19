package com.example.NearMeBKND.classified.controller;

import com.example.NearMeBKND.classified.model.Classified;
import com.example.NearMeBKND.classified.model.ClassifiedDTO;
import com.example.NearMeBKND.classified.service.ClassifiedService;
import com.example.NearMeBKND.classified.util.UserContext;
import com.example.NearMeBKND.service.UserLocationService;
import com.example.NearMeBKND.util.GeoUtils;
import com.example.NearMeBKND.nearme.model.UserLocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;

@RestController
@RequestMapping("/api/classified")
public class clf_Controller {

    @Autowired
    private ClassifiedService service;

    @Autowired
    private UserLocationService userLocationService;

    @Autowired
    private ObjectMapper objectMapper;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> addClassified(
        @RequestPart("classified") String classifiedJson,
        @RequestPart("image") MultipartFile imageFile,
        @RequestHeader("X-User-ID") String userId
    ) {
        try {
            // Parse the classified JSON
            Classified classified = objectMapper.readValue(classifiedJson, Classified.class);

            // Validate user location
            UserLocation userLocation = userLocationService.getUserLocation(userId);
            if (userLocation == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "User location not found. Please update your location first."));
            }

            // Set the user's location to the classified
            classified.setLatitude(userLocation.getLatitude());
            classified.setLongitude(userLocation.getLongitude());
            classified.setUserId(userId);
            classified.setImageUrl(imageFile.getBytes());

            Classified savedClassified = service.createClassified(classified);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Classified saved successfully!");
            response.put("created_at", savedClassified.getCreatedAt());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to save classified: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> getClassifiedImage(@PathVariable int id) {
        Classified classified = service.getClassifiedById(id);
        if (classified == null || classified.getImageUrl() == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
            .contentType(MediaType.IMAGE_JPEG)
            .body(classified.getImageUrl());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getClassifiedById(@PathVariable int id, @RequestHeader("X-User-ID") String currentUserId) {
        Classified classified = service.getClassifiedById(id);
        if (classified == null) {
            return ResponseEntity.notFound().build();
        }
        // Radius check (15 km)
        UserLocation currentUserLoc = userLocationService.getUserLocation(currentUserId);
        UserLocation ownerLoc = userLocationService.getUserLocation(classified.getUserId());
        double distance = GeoUtils.haversine(
            currentUserLoc.getLatitude(), currentUserLoc.getLongitude(),
            ownerLoc.getLatitude(), ownerLoc.getLongitude()
        );
        if (distance > 15.0) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "You are not within 15 km of the classified poster."));
        }
        // Build DTO
        ClassifiedDTO dto = new ClassifiedDTO();
        dto.setId(classified.getId());
        dto.setTitle(classified.getTitle());
        dto.setDescription(classified.getDescription());
        dto.setPrice(classified.getPrice());
        dto.setUserId(classified.getUserId());
        dto.setCreatedAt(classified.getCreatedAt());
        dto.setCategories(classified.getCategories());
        dto.setQuestions(service.getQuestionsByClassifiedId(id));
        dto.setImage("/api/classified/" + classified.getId() + "/image");
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> getAllCategories() {
        return ResponseEntity.ok(service.getAllCategories());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteClassified(@PathVariable int id) {
        String currentUserId = UserContext.getCurrentUserId();
        String classifiedUserId = service.getClassifiedUserId(id);
        
        if (classifiedUserId == null) {
            return ResponseEntity.notFound().build();
        }
        
        if (!currentUserId.equals(classifiedUserId)) {
            return ResponseEntity.status(403).body("You are not authorized to delete this classified");
        }
        
        service.deleteClassified(id);
        return ResponseEntity.ok("Classified deleted successfully!");
    }

    @PostMapping("/{id}/notes")
    public ResponseEntity<?> addNoteToClassified(
        @PathVariable int id,
        @RequestBody Map<String, String> request
    ) {
        String currentUserId = UserContext.getCurrentUserId();
        String classifiedUserId = service.getClassifiedUserId(id);
        
        if (classifiedUserId == null) {
            return ResponseEntity.notFound().build();
        }
        
        if (!currentUserId.equals(classifiedUserId)) {
            return ResponseEntity.status(403).body("You are not authorized to add notes to this classified");
        }

        String note = request.get("note");
        if (note == null || note.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Note cannot be empty");
        }

        service.updateClassifiedNotes(id, note);
        return ResponseEntity.ok(Map.of("message", "Note added successfully"));
    }

    @GetMapping("/")
    public ResponseEntity<?> getClassifiedsInRadius(
            @RequestHeader("X-User-ID") String userId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "30") int size) {
        // Check if user exists in user_locations
        UserLocation userLocation = userLocationService.getUserLocation(userId);
        if (userLocation == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "User location not found. Please update your location first."));
        }
        double radiusKm = 15.0;
        int offset = page * size;
        int total = service.countClassifiedsInRadius(userLocation.getLatitude(), userLocation.getLongitude(), radiusKm);
        int totalPages = (int) Math.ceil((double) total / size);
        List<Classified> classifieds = service.getClassifiedsInRadiusPaged(userLocation.getLatitude(), userLocation.getLongitude(), radiusKm, size, offset);
        List<Map<String, Object>> pageContent = new ArrayList<>();
        for (Classified classified : classifieds) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", classified.getId());
            map.put("title", classified.getTitle());
            map.put("description", classified.getDescription());
            map.put("price", classified.getPrice());
            map.put("userId", classified.getUserId());
            map.put("createdAt", classified.getCreatedAt());
            map.put("image", "/api/classified/" + classified.getId() + "/image");
            pageContent.add(map);
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("data", pageContent);
        response.put("currentPage", page);
        response.put("size", size);
        response.put("count", pageContent.size());
        response.put("totalPages", totalPages);
        return ResponseEntity.ok(response);
    }
}
