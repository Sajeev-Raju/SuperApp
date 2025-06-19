package com.example.NearMeBKND.nearme.repository;

import com.example.NearMeBKND.nearme.model.UserLocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserLocationRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<UserLocation> rowMapper = (rs, rowNum) -> {
        UserLocation location = new UserLocation();
        location.setId(rs.getLong("id"));
        location.setUserId(rs.getString("user_id"));
        location.setLatitude(rs.getDouble("latitude"));
        location.setLongitude(rs.getDouble("longitude"));
        location.setLocationName(rs.getString("location_name"));
        location.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return location;
    };

    public Optional<UserLocation> findByUserId(String userId) {
        String sql = "SELECT * FROM user_locations WHERE user_id = ?";
        try {
            UserLocation location = jdbcTemplate.queryForObject(sql, rowMapper, userId);
            return Optional.ofNullable(location);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public UserLocation save(UserLocation userLocation) {
        String sql = "INSERT INTO user_locations (user_id, latitude, longitude, location_name) VALUES (?, ?, ?, ?) " +
                     "ON CONFLICT (user_id) DO UPDATE SET latitude = ?, longitude = ?, location_name = ?";
        jdbcTemplate.update(sql, 
            userLocation.getUserId(), 
            userLocation.getLatitude(), 
            userLocation.getLongitude(),
            userLocation.getLocationName(),
            userLocation.getLatitude(), 
            userLocation.getLongitude(),
            userLocation.getLocationName()
        );
        return userLocation;
    }
} 