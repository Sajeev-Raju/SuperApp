package com.example.NearMeBKND.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import java.util.Map;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class GoogleMapsUrlProcessor {
    private static final Logger logger = LoggerFactory.getLogger(GoogleMapsUrlProcessor.class);
    private final RestTemplate restTemplate;
    private final String apiKey;
    
    // Pattern for direct coordinate URLs (e.g., @LAT,LNG,z)
    private static final Pattern COORDINATE_PATTERN = 
        Pattern.compile("@([\\-\\d.]+),([\\-\\d.]+)(?:,|z)");
    
    // Pattern for search URLs (e.g., /maps/search/LAT,LNG or /maps/search/LAT,+LNG)
    private static final Pattern SEARCH_URL_PATTERN = 
        Pattern.compile("maps/search/([\\-\\d.]+)[,\\s]*[+]?([\\-\\d.]+)");
    
    // Pattern for short URLs
    private static final Pattern SHORT_URL_PATTERN = 
        Pattern.compile("^https://maps\\.app\\.goo\\.gl/[a-zA-Z0-9]+$");

    public GoogleMapsUrlProcessor(RestTemplate restTemplate, @Value("${google.api.key}") String apiKey) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
    }

    public record Coordinates(double latitude, double longitude) {}

    public Coordinates extractCoordinatesFromUrl(String googleMapsUrl) {
        logger.debug("Extracting coordinates from URL: {}", googleMapsUrl);
        
        if (googleMapsUrl == null || googleMapsUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Google Maps URL is required");
        }

        // Try direct coordinate URL first
        var matcher = COORDINATE_PATTERN.matcher(googleMapsUrl);
        if (matcher.find()) {
            double lat = Double.parseDouble(matcher.group(1));
            double lng = Double.parseDouble(matcher.group(2));
            logger.debug("Extracted coordinates directly: {}, {}", lat, lng);
            return new Coordinates(lat, lng);
        }
        // Try search URL pattern
        matcher = SEARCH_URL_PATTERN.matcher(googleMapsUrl);
        if (matcher.find()) {
            double lat = Double.parseDouble(matcher.group(1));
            double lng = Double.parseDouble(matcher.group(2));
            logger.debug("Extracted coordinates from search URL: {}, {}", lat, lng);
            return new Coordinates(lat, lng);
        }

        // Try short URL
        if (SHORT_URL_PATTERN.matcher(googleMapsUrl).matches()) {
            try {
                String fullUrl = resolveShortUrl(googleMapsUrl);
                logger.debug("Resolved short URL to: {}", fullUrl);
                // Try to extract coordinates from resolved URL (all patterns)
                matcher = COORDINATE_PATTERN.matcher(fullUrl);
                if (matcher.find()) {
                    double lat = Double.parseDouble(matcher.group(1));
                    double lng = Double.parseDouble(matcher.group(2));
                    logger.debug("Extracted coordinates from resolved URL: {}, {}", lat, lng);
                    return new Coordinates(lat, lng);
                }
                matcher = SEARCH_URL_PATTERN.matcher(fullUrl);
                if (matcher.find()) {
                    double lat = Double.parseDouble(matcher.group(1));
                    double lng = Double.parseDouble(matcher.group(2));
                    logger.debug("Extracted coordinates from resolved search URL: {}, {}", lat, lng);
                    return new Coordinates(lat, lng);
                }
                // If no coordinates in URL, try to extract place ID
                String placeId = extractPlaceId(fullUrl);
                if (placeId != null) {
                    return getCoordinatesFromPlaceId(placeId);
                }
            } catch (Exception e) {
                logger.error("Failed to process short URL: {}", e.getMessage(), e);
                throw new IllegalArgumentException("Failed to process Google Maps URL", e);
            }
        }

        // Fallback: try geocoding
        return geocodeUrl(googleMapsUrl);
    }

    private String resolveShortUrl(String shortUrl) {
        try {
            return restTemplate.headForHeaders(shortUrl).getLocation().toString();
        } catch (Exception e) {
            logger.error("Failed to resolve short URL: {}", e.getMessage(), e);
            throw new IllegalArgumentException("Failed to resolve Google Maps short URL", e);
        }
    }

    private String extractPlaceId(String fullUrl) {
        try {
            // Try to extract place ID from URL
            String[] parts = fullUrl.split("!1s0x");
            if (parts.length >= 2) {
                return "0x" + parts[1].split("!")[0];
            }
            return null;
        } catch (Exception e) {
            logger.error("Failed to extract place ID: {}", e.getMessage(), e);
            return null;
        }
    }

    private Coordinates getCoordinatesFromPlaceId(String placeId) {
        String url = UriComponentsBuilder.fromHttpUrl("https://maps.googleapis.com/maps/api/place/details/json")
                .queryParam("place_id", placeId)
                .queryParam("key", apiKey)
                .build()
                .toString();

        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response == null || !response.containsKey("result")) {
                throw new IllegalArgumentException("Invalid response from Google Places API");
            }

            Map<String, Object> result = (Map<String, Object>) response.get("result");
            Map<String, Object> geometry = (Map<String, Object>) result.get("geometry");
            Map<String, Object> location = (Map<String, Object>) geometry.get("location");

            if (location == null || !location.containsKey("lat") || !location.containsKey("lng")) {
                throw new IllegalArgumentException("Invalid location data received from Google Places API");
            }

            double lat = ((Number) location.get("lat")).doubleValue();
            double lng = ((Number) location.get("lng")).doubleValue();
            
            logger.debug("Extracted coordinates from place ID: {}, {}", lat, lng);
            return new Coordinates(lat, lng);
        } catch (Exception e) {
            logger.error("Failed to get coordinates from place ID: {}", e.getMessage(), e);
            throw new IllegalArgumentException("Failed to get coordinates from Google Places API", e);
        }
    }

    private Coordinates geocodeUrl(String urlOrAddress) {
        String url = UriComponentsBuilder.fromHttpUrl("https://maps.googleapis.com/maps/api/geocode/json")
                .queryParam("address", urlOrAddress)
                .queryParam("key", apiKey)
                .build()
                .toString();

        try {
            logger.debug("Calling Google Geocoding API: {}", url);
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response == null || !response.containsKey("results")) {
                throw new IllegalArgumentException("No geocoding results found");
            }

            var results = (java.util.List<?>) response.get("results");
            if (results.isEmpty()) {
                throw new IllegalArgumentException("No geocoding results found");
            }

            var geometry = (java.util.Map<?, ?>) ((java.util.Map<?, ?>) results.get(0)).get("geometry");
            var location = (java.util.Map<?, ?>) geometry.get("location");
            
            if (location == null || !location.containsKey("lat") || !location.containsKey("lng")) {
                throw new IllegalArgumentException("Invalid location data in geocoding response");
            }

            double lat = ((Number) location.get("lat")).doubleValue();
            double lng = ((Number) location.get("lng")).doubleValue();
            
            logger.debug("Extracted coordinates from geocoding: {}, {}", lat, lng);
            return new Coordinates(lat, lng);
        } catch (Exception e) {
            logger.error("Failed to geocode URL/address: {}", e.getMessage(), e);
            throw new IllegalArgumentException("Failed to geocode location", e);
        }
    }
} 