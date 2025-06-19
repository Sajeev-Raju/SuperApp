package com.example.NearMeBKND.classified.repository;

import com.example.NearMeBKND.classified.model.Classified;
import com.example.NearMeBKND.classified.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Repository
public class ClassifiedRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public int saveClassified(Classified classified) {
        // Debug: Before insert
        System.out.println("[DEBUG] Attempting to insert classified: " + classified.getTitle());

        // Use a single connection for both insert and last_insert_rowid
        return jdbcTemplate.execute((org.springframework.jdbc.core.ConnectionCallback<Integer>) connection -> {
            int id = -1;
            try (
                PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO clf_classified (title, description, price, image_url, categories, user_id, latitude, longitude, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)"
                )
            ) {
                ps.setString(1, classified.getTitle());
                ps.setString(2, classified.getDescription());
                ps.setInt(3, classified.getPrice());
                ps.setBytes(4, classified.getImageUrl());
                ps.setString(5, classified.getCategories() != null ? String.join(",", classified.getCategories()) : null);
                ps.setString(6, classified.getUserId());
                if (classified.getLatitude() != null) {
                    ps.setDouble(7, classified.getLatitude());
                } else {
                    ps.setNull(7, java.sql.Types.DOUBLE);
                }
                if (classified.getLongitude() != null) {
                    ps.setDouble(8, classified.getLongitude());
                } else {
                    ps.setNull(8, java.sql.Types.DOUBLE);
                }

                int rows = ps.executeUpdate();
                if (rows == 0) {
                    System.err.println("[ERROR] Insert failed, no rows affected.");
                    return -1;
                }
            }

            // Now get the last inserted id
            try (PreparedStatement ps2 = connection.prepareStatement("SELECT last_insert_rowid()")) {
                try (ResultSet rs = ps2.executeQuery()) {
                    if (rs.next()) {
                        id = rs.getInt(1);
                    }
                }
            }

            if (id == -1) {
                System.err.println("[WARN] Insert succeeded but could not retrieve last inserted ID.");
            }

            // Save details if present
            if (classified.getDetails() != null) {
                for (Map.Entry<String, String> detail : classified.getDetails().entrySet()) {
                    try (PreparedStatement ps = connection.prepareStatement(
                            "INSERT INTO clf_classified_detail (classified_id, key, value) VALUES (?, ?, ?)")) {
                        ps.setInt(1, id);
                        ps.setString(2, detail.getKey());
                        ps.setString(3, detail.getValue());
                        ps.executeUpdate();
                    }
                }
            }

            // Save categories if present (these can use jdbcTemplate as before)
            if (classified.getCategories() != null) {
                for (String category : classified.getCategories()) {
                    int catId = saveCategory(category);
                    mapCategory(id, catId);
                }
            }

            return id;
        });
    }

    public int saveCategory(String category) {
        jdbcTemplate.update("INSERT OR IGNORE INTO clf_category (name) VALUES (?)", category);
        return jdbcTemplate.queryForObject("SELECT id FROM clf_category WHERE name = ?", Integer.class, category);
    }

    public void mapCategory(int classifiedId, int categoryId) {
        jdbcTemplate.update("INSERT OR IGNORE INTO clf_classified_category_map (classified_id, category_id) VALUES (?, ?)", classifiedId, categoryId);
    }

    public Classified getClassifiedById(int id) {
        Classified classified = jdbcTemplate.queryForObject(
            "SELECT * FROM clf_classified WHERE id = ?",
            (rs, rowNum) -> mapClassified(rs),
            id
        );
        if (classified != null) {
            loadDetails(classified);
            loadCategories(classified);
            loadNotes(classified);
        }
        return classified;
    }

    public List<String> getAllCategories() {
        return jdbcTemplate.queryForList("SELECT name FROM clf_category", String.class);
    }

    public void deleteClassified(int id) {
        jdbcTemplate.update("DELETE FROM clf_classified_detail WHERE classified_id = ?", id);
        jdbcTemplate.update("DELETE FROM clf_classified_category_map WHERE classified_id = ?", id);
        jdbcTemplate.update("DELETE FROM clf_classified WHERE id = ?", id);
    }

    public String getClassifiedUserId(int id) {
        return jdbcTemplate.queryForObject(
            "SELECT user_id FROM clf_classified WHERE id = ?",
            String.class,
            id
        );
    }

    private Classified mapClassified(ResultSet rs) throws SQLException {
        Classified classified = new Classified();
        classified.setId(rs.getInt("id"));
        classified.setTitle(rs.getString("title"));
        classified.setDescription(rs.getString("description"));
        classified.setPrice(rs.getInt("price"));
        classified.setImageUrl(rs.getBytes("image_url"));
        classified.setImageUrlString("/api/classified/" + classified.getId() + "/image");
        classified.setUserId(rs.getString("user_id"));
        classified.setCreatedAt(rs.getString("created_at"));
        
        // Handle null latitude and longitude
        double latitude = rs.getDouble("latitude");
        if (!rs.wasNull()) {
            classified.setLatitude(latitude);
        }
        
        double longitude = rs.getDouble("longitude");
        if (!rs.wasNull()) {
            classified.setLongitude(longitude);
        }
        
        // Load categories from the classified table first
        String categoriesStr = rs.getString("categories");
        if (categoriesStr != null && !categoriesStr.isEmpty()) {
            classified.setCategories(Arrays.asList(categoriesStr.split(",")));
        }
        
        return classified;
    }

    private void loadDetails(Classified classified) {
        List<Map<String, Object>> details = jdbcTemplate.queryForList(
            "SELECT key, value FROM clf_classified_detail WHERE classified_id = ?",
            classified.getId()
        );
        Map<String, String> detailsMap = new HashMap<>();
        for (Map<String, Object> detail : details) {
            detailsMap.put(
                (String) detail.get("key"),
                (String) detail.get("value")
            );
        }
        classified.setDetails(detailsMap);
    }

    private void loadNotes(Classified classified) {
        List<String> notes = jdbcTemplate.queryForList(
            "SELECT note FROM clf_note WHERE classified_id = ? ORDER BY id",
            String.class,
            classified.getId()
        );
        classified.setNotes(notes);
    }

    private void loadCategories(Classified classified) {
        // If categories are already loaded from the classified table, don't load them again
        if (classified.getCategories() != null && !classified.getCategories().isEmpty()) {
            return;
        }
        
        List<String> categories = jdbcTemplate.queryForList(
            "SELECT c.name FROM clf_category c " +
            "JOIN clf_classified_category_map ccm ON c.id = ccm.category_id " +
            "WHERE ccm.classified_id = ?",
            String.class,
            classified.getId()
        );
        classified.setCategories(categories);
    }

    public void updateClassifiedNotes(int id, String newNote) {
        jdbcTemplate.update(
            "INSERT INTO clf_note (classified_id, note) VALUES (?, ?)",
            id,
            newNote
        );
    }

    public List<Classified> getAllClassifiedsRaw() {
        List<Classified> classifieds = jdbcTemplate.query(
            "SELECT * FROM clf_classified",
            (rs, rowNum) -> mapClassified(rs)
        );
        for (Classified classified : classifieds) {
            loadDetails(classified);
            loadCategories(classified);
            loadNotes(classified);
        }
        return classifieds;
    }

    public List<Classified> getClassifiedsInRadiusPaged(double userLat, double userLon, double radiusKm, int limit, int offset) {
        double latDegree = radiusKm / 111.0;
        double lonDegree = radiusKm / (111.0 * Math.cos(Math.toRadians(userLat)));
        double minLat = userLat - latDegree;
        double maxLat = userLat + latDegree;
        double minLon = userLon - lonDegree;
        double maxLon = userLon + lonDegree;

        String sql = """
            SELECT *, (6371 * acos(
                cos(radians(?)) * cos(radians(latitude)) *
                cos(radians(longitude) - radians(?)) +
                sin(radians(?)) * sin(radians(latitude))
            )) AS distance
            FROM clf_classified
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
        return jdbcTemplate.query(sql, new Object[]{
            userLat, userLon, userLat,
            minLat, maxLat, minLon, maxLon,
            userLat, userLon, userLat, radiusKm, limit, offset
        }, (rs, rowNum) -> mapClassified(rs));
    }

    public int countClassifiedsInRadius(double userLat, double userLon, double radiusKm) {
        String sql = "SELECT COUNT(*) FROM (" +
                "SELECT * FROM clf_classified " +
                "WHERE latitude IS NOT NULL AND longitude IS NOT NULL " +
                "AND (6371 * acos(" +
                "cos(radians(?)) * cos(radians(latitude)) * " +
                "cos(radians(longitude) - radians(?)) + " +
                "sin(radians(?)) * sin(radians(latitude))" +
                ")) <= ?" +
                ")";
        return jdbcTemplate.queryForObject(sql, new Object[]{userLat, userLon, userLat, radiusKm}, Integer.class);
    }
}
