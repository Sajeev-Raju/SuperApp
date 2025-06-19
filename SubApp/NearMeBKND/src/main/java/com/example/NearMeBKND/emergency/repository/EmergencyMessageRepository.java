package com.example.NearMeBKND.emergency.repository;

// aaaaaaaaaaaaaaaaa

//package com.example.emergencymessage.repository;

import com.example.NearMeBKND.emergency.model.EmergencyMessage;
import com.example.NearMeBKND.emergency.model.EmergencyMessageRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

@Repository
public class EmergencyMessageRepository {

    private final JdbcTemplate jdbcTemplate;

    public EmergencyMessageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public EmergencyMessage save(EmergencyMessage message) {
        return jdbcTemplate.execute((ConnectionCallback<EmergencyMessage>) connection -> {
            // Insert the message
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO emergency_message (title, description, types, details, google_maps_location, user_id, latitude, longitude, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)"
            )) {
                ps.setString(1, message.getTitle());
                ps.setString(2, message.getDescription());
                ps.setString(3, String.join(",", message.getTypes()));
                ps.setString(4, message.getDetailsAsJson());
                if (message.getGoogleMapsLocation() != null) {
                    ps.setString(5, message.getGoogleMapsLocation());
                } else {
                    ps.setNull(5, java.sql.Types.VARCHAR);
                }
                ps.setString(6, message.getUserId());
                if (message.getLatitude() != null) {
                    ps.setDouble(7, message.getLatitude());
                } else {
                    ps.setNull(7, java.sql.Types.DOUBLE);
                }
                if (message.getLongitude() != null) {
                    ps.setDouble(8, message.getLongitude());
                } else {
                    ps.setNull(8, java.sql.Types.DOUBLE);
                }
                ps.executeUpdate();
            }

            // Get the last inserted id
            int messageId = -1;
            try (PreparedStatement ps2 = connection.prepareStatement("SELECT last_insert_rowid()")) {
                try (ResultSet rs = ps2.executeQuery()) {
                    if (rs.next()) {
                        messageId = rs.getInt(1);
                    }
                }
            }

            message.setId(messageId);

            // Insert types into message_type_map only
            for (String type : message.getTypes()) {
                try (PreparedStatement psMap = connection.prepareStatement("INSERT INTO message_type_map (message_id, type) VALUES (?, ?)")) {
                    psMap.setInt(1, messageId);
                    psMap.setString(2, type.trim());
                    psMap.executeUpdate();
                }
            }

            // Fetch the saved message to get the created_at timestamp
            EmergencyMessage result = null;
            try (PreparedStatement ps3 = connection.prepareStatement("SELECT * FROM emergency_message WHERE id = ?")) {
                ps3.setInt(1, messageId);
                try (ResultSet rs = ps3.executeQuery()) {
                    if (rs.next()) {
                        // Use your RowMapper logic
                        EmergencyMessageRowMapper rowMapper = new EmergencyMessageRowMapper(jdbcTemplate);
                        result = rowMapper.mapRow(rs, 0);
                    }
                }
            }
            return result;
        });
    }

    public List<EmergencyMessage> findAll() {
        List<EmergencyMessage> messages = jdbcTemplate.query("SELECT * FROM emergency_message", new EmergencyMessageRowMapper(jdbcTemplate));
        for (EmergencyMessage message : messages) {
            loadNotes(message);
        }
        return messages;
    }

    public List<EmergencyMessage> findByTypes(List<String> types) {
        if (types.isEmpty()) return findAll();

        String inClause = String.join(",", Collections.nCopies(types.size(), "?"));
        String sql = """
            SELECT DISTINCT em.* FROM emergency_message em
            JOIN message_type_map map ON em.id = map.message_id
            WHERE map.type IN (%s)
        """.formatted(inClause);

        return jdbcTemplate.query(sql, types.toArray(), new EmergencyMessageRowMapper(jdbcTemplate));
    }

    public List<EmergencyMessage> findByDetails(Map<String, String> filters) {
        String baseQuery = "SELECT * FROM emergency_message";

        if (filters.isEmpty()) {
            return jdbcTemplate.query(baseQuery, new EmergencyMessageRowMapper(jdbcTemplate));
        }

        StringBuilder whereClause = new StringBuilder(" WHERE ");
        List<Object> params = new ArrayList<>();

        int i = 0;
        for (Map.Entry<String, String> entry : filters.entrySet()) {
            if (i++ > 0) whereClause.append(" AND ");
            whereClause.append("json_extract(details, '$.").append(entry.getKey()).append("') = ?");
            params.add(entry.getValue());
        }

        String fullQuery = baseQuery + whereClause;
        return jdbcTemplate.query(fullQuery, params.toArray(), new EmergencyMessageRowMapper(jdbcTemplate));
    }

    public List<EmergencyMessage> findByTypesAndDetails(List<String> types, Map<String, String> filters) {
        if (types.isEmpty()) return findByDetails(filters);

        StringBuilder sql = new StringBuilder("""
            SELECT DISTINCT em.* FROM emergency_message em
            JOIN message_type_map map ON em.id = map.message_id
            WHERE map.type IN (%s)
        """.formatted(String.join(",", Collections.nCopies(types.size(), "?"))));

        List<Object> params = new ArrayList<>(types);

        for (Map.Entry<String, String> entry : filters.entrySet()) {
            sql.append(" AND json_extract(em.details, '$.").append(entry.getKey()).append("') = ?");
            params.add(entry.getValue());
        }

        return jdbcTemplate.query(sql.toString(), params.toArray(), new EmergencyMessageRowMapper(jdbcTemplate));
    }

    public EmergencyMessage findById(int id) {
        List<EmergencyMessage> messages = jdbcTemplate.query(
            "SELECT * FROM emergency_message WHERE id = ?",
            new EmergencyMessageRowMapper(jdbcTemplate), id);
        if (!messages.isEmpty()) {
            loadNotes(messages.get(0));
            return messages.get(0);
        }
        return null;
    }

    private void loadNotes(EmergencyMessage message) {
        List<String> notes = jdbcTemplate.queryForList(
            "SELECT note FROM emergency_note WHERE emergency_id = ? ORDER BY id",
            String.class,
            message.getId()
        );
        message.setNotes(notes);
    }

    public void addNote(int emergencyId, String note) {
        jdbcTemplate.update(
            "INSERT INTO emergency_note (emergency_id, note) VALUES (?, ?)",
            emergencyId,
            note
        );
    }

    public void deleteById(int id) {
        jdbcTemplate.update("DELETE FROM emergency_note WHERE emergency_id = ?", id);
        jdbcTemplate.update("DELETE FROM emergency_message WHERE id = ?", id);
    }

    public List<EmergencyMessage> getEmergencyMessagesInRadiusPaged(double userLat, double userLon, double radiusKm, int size, int offset) {
        // Calculate bounding box
        double latDelta = radiusKm / 111.0; // 1 deg latitude ~ 111 km
        double lonDelta = radiusKm / (111.0 * Math.cos(Math.toRadians(userLat)));
        double minLat = userLat - latDelta;
        double maxLat = userLat + latDelta;
        double minLon = userLon - lonDelta;
        double maxLon = userLon + lonDelta;

        String sql = """
            SELECT *, (6371 * acos(
                cos(radians(?)) * cos(radians(latitude)) *
                cos(radians(longitude) - radians(?)) +
                sin(radians(?)) * sin(radians(latitude))
            )) AS distance
            FROM emergency_message
            WHERE latitude IS NOT NULL AND longitude IS NOT NULL
              AND latitude BETWEEN ? AND ?
              AND longitude BETWEEN ? AND ?
              AND (6371 * acos(
                cos(radians(?)) * cos(radians(latitude)) *
                cos(radians(longitude) - radians(?)) +
                sin(radians(?)) * sin(radians(latitude))
              )) <= ?
            ORDER BY created_at DESC
            LIMIT ? OFFSET ?
        """;
        Object[] params = new Object[] {
            userLat, userLon, userLat, minLat, maxLat, minLon, maxLon,
            userLat, userLon, userLat, radiusKm, size, offset
        };
        List<EmergencyMessage> messages = jdbcTemplate.query(sql, params, new EmergencyMessageRowMapper(jdbcTemplate));
        for (EmergencyMessage message : messages) {
            loadNotes(message);
        }
        return messages;
    }

    public int countEmergencyMessagesInRadius(double userLat, double userLon, double radiusKm) {
        String sql = """
            SELECT COUNT(*) FROM (
                SELECT * FROM emergency_message 
                WHERE latitude IS NOT NULL AND longitude IS NOT NULL 
                AND (6371 * acos(
                    cos(radians(?)) * cos(radians(latitude)) *
                    cos(radians(longitude) - radians(?)) +
                    sin(radians(?)) * sin(radians(latitude))
                )) <= ?
            )
        """;
        Object[] params = new Object[] { userLat, userLon, userLat, radiusKm };
        Integer count = jdbcTemplate.queryForObject(sql, params, Integer.class);
        return count != null ? count : 0;
    }
}
