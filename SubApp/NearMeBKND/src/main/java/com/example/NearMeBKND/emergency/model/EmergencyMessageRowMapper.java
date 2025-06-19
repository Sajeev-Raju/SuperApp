package com.example.NearMeBKND.emergency.model;

//package com.example.emergency.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class EmergencyMessageRowMapper implements RowMapper<EmergencyMessage> {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EmergencyMessageRowMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public EmergencyMessage mapRow(ResultSet rs, int rowNum) throws SQLException {
        EmergencyMessage message = new EmergencyMessage();
        message.setId(rs.getInt("id"));
        message.setTitle(rs.getString("title"));
        message.setDescription(rs.getString("description"));
        
        // Handle google_maps_location safely
        String googleMapsLocation = rs.getString("google_maps_location");
        if (googleMapsLocation != null && !googleMapsLocation.trim().isEmpty()) {
            try {
                message.setGoogleMapsLocation(googleMapsLocation);
            } catch (IllegalArgumentException e) {
                // If URL is invalid, set it to null
                message.setGoogleMapsLocation(null);
            }
        } else {
            message.setGoogleMapsLocation(null);
        }
        
        message.setUserId(rs.getString("user_id"));
        message.setCreatedAt(rs.getString("created_at"));
        message.setLatitude(rs.getObject("latitude") != null ? rs.getDouble("latitude") : null);
        message.setLongitude(rs.getObject("longitude") != null ? rs.getDouble("longitude") : null);

        // Parse JSON details
        String detailsJson = rs.getString("details");
        try {
            Map<String, String> details = objectMapper.readValue(detailsJson, Map.class);
            message.setDetails(details);
        } catch (Exception e) {
            message.setDetails(new HashMap<>());
        }

        // Fetch types from message_type_map
        List<String> types = jdbcTemplate.queryForList(
                "SELECT type FROM message_type_map WHERE message_id = ?",
                String.class,
                message.getId());
        message.setTypes(types);

        return message;
    }
}
