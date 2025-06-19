package com.example.NearMeBKND.meetup.repository;

import com.example.NearMeBKND.meetup.model.Meetup;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.NearMeBKND.util.QueryLogger;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.time.format.DateTimeFormatter;

@Repository
public class MeetupRepository {
    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<Meetup> meetupRowMapper;
    private static final Logger logger = LoggerFactory.getLogger(MeetupRepository.class);

    public MeetupRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        this.meetupRowMapper = (rs, rowNum) -> {
            Meetup meetup = new Meetup();
            meetup.setId(rs.getLong("id"));
            meetup.setOrganizerName(rs.getString("organizer_name"));
            meetup.setTitle(rs.getString("title"));
            meetup.setDescription(rs.getString("description"));
            String tagsString = String.join(", ", getMeetupTags(meetup.getId()));
            meetup.setTags(tagsString);
            // Fetch and set all notes for this meetup
            String allNotesSql = "SELECT note FROM mtp_note WHERE meetup_id = ? ORDER BY id ASC";
            List<String> allNotes = jdbcTemplate.queryForList(allNotesSql, String.class, meetup.getId());
            meetup.setNotes(allNotes);
            // Fetch and set all questions for this meetup
            String allQuestionsSql = "SELECT user_id, content, created_at FROM mtp_meetup_question WHERE meetup_id = ? ORDER BY id ASC";
            List<com.example.NearMeBKND.meetup.model.MeetupQuestionDTO> allQuestions = jdbcTemplate.query(
                allQuestionsSql,
                (qrs, qrowNum) -> {
                    com.example.NearMeBKND.meetup.model.MeetupQuestionDTO q = new com.example.NearMeBKND.meetup.model.MeetupQuestionDTO();
                    q.setUserId(qrs.getString("user_id"));
                    q.setContent(qrs.getString("content"));
                    try {
                        String timestampStr = qrs.getString("created_at");
                        if (timestampStr != null) {
                            q.setCreatedAt(java.time.LocalDateTime.parse(timestampStr, dtf));
                        }
                    } catch (Exception e) {
                        logger.warn("Error parsing timestamp for question: {}", e.getMessage());
                        q.setCreatedAt(java.time.LocalDateTime.now());
                    }
                    return q;
                },
                meetup.getId()
            );
            meetup.setQuestions(allQuestions);
            meetup.setStartDate(LocalDate.parse(rs.getString("start_date")));
            meetup.setStartTime(LocalTime.parse(rs.getString("start_time")));
            meetup.setEndDate(LocalDate.parse(rs.getString("end_date")));
            meetup.setEndTime(LocalTime.parse(rs.getString("end_time")));
            meetup.setOrganizerId(rs.getString("organizer_id"));
            meetup.setEventAddress(rs.getString("event_address"));
            meetup.setImageUrl(rs.getBytes("image_url"));
            meetup.setGoogleLocationURL(rs.getString("google_location_url"));
            meetup.setCreatedAt(LocalDateTime.parse(rs.getString("created_at"), dtf));
            meetup.setIsActive(rs.getBoolean("is_active"));
            meetup.setMaxParticipants(rs.getInt("max_participants"));
            meetup.setContactInfo(rs.getString("contact_info"));
            return meetup;
        };
    }

    public List<Meetup> findAll() {
        return jdbcTemplate.query(
            "SELECT * FROM mtp_meetup WHERE is_active = true",
            meetupRowMapper
        );
    }

    public Optional<Meetup> findById(Long id) {
        List<Meetup> results = jdbcTemplate.query(
            "SELECT * FROM mtp_meetup WHERE id = ? AND is_active = true",
            meetupRowMapper,
            id
        );
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<Meetup> findByOrganizerId(String organizerId) {
        return jdbcTemplate.query(
            "SELECT * FROM mtp_meetup WHERE organizer_id = ? AND is_active = true",
            meetupRowMapper,
            organizerId
        );
    }

    public Meetup save(Meetup meetup) {
        return insert(meetup);
    }

    private Meetup insert(Meetup meetup) {
        String sql = "INSERT INTO mtp_meetup (" +
            "organizer_name, title, description, " +
            "start_date, start_time, end_date, end_time, " +
            "organizer_id, event_address, " +
            "image_url, google_location_url, created_at, " +
            "is_active, max_participants, contact_info" +
            ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql,
            meetup.getOrganizerName(),
            meetup.getTitle(),
            meetup.getDescription(),
            meetup.getStartDate(),
            meetup.getStartTime(),
            meetup.getEndDate(),
            meetup.getEndTime(),
            meetup.getOrganizerId(),
            meetup.getEventAddress(),
            meetup.getImageUrl(),
            meetup.getGoogleLocationURL(),
            meetup.getCreatedAt(),
            meetup.getIsActive(),
            meetup.getMaxParticipants(),
            meetup.getContactInfo()
        );

        // Fetch the last inserted ID (SQLite specific)
        Long id = jdbcTemplate.queryForObject("SELECT last_insert_rowid()", Long.class);
        meetup.setId(id);

        // Save tags if provided
        if (meetup.getTags() != null && !meetup.getTags().isEmpty()) {
            String[] tags = meetup.getTags().split("\\s*,\\s*");
            String tagSql = "INSERT OR IGNORE INTO mtp_meetup_tag (meetupId, tag) VALUES (?, ?)";
            for (String tag : tags) {
                jdbcTemplate.update(tagSql, id, tag.trim());
            }
        }

        return meetup;
    }

    public void saveMeetupTags(Long meetupId, Set<String> tagNames) {
        // Remove existing tags for this meetup
        deleteMeetupTags(meetupId);
        // Insert new tags
        String sql = "INSERT OR IGNORE INTO mtp_meetup_tag (meetupId, tag) VALUES (?, ?)";
        for (String tag : tagNames) {
            jdbcTemplate.update(sql, meetupId, tag);
        }
    }

    public Set<String> getMeetupTags(Long meetupId) {
        String sql = "SELECT tag FROM mtp_meetup_tag WHERE meetupId = ?";
        return new HashSet<>(jdbcTemplate.queryForList(sql, String.class, meetupId));
    }

    public void deleteMeetupTags(Long meetupId) {
        jdbcTemplate.update("DELETE FROM mtp_meetup_tag WHERE meetupId = ?", meetupId);
    }

    public void delete(Long id) {
        // First delete the tags
        deleteMeetupTags(id);
        // Then mark the meetup as inactive
        jdbcTemplate.update(
            "UPDATE mtp_meetup SET is_active = false WHERE id = ?",
            id
        );
    }

    public RowMapper<Meetup> getMeetupRowMapper() {
        return meetupRowMapper;
    }

    public int countNoteUpdates(Long meetupId) {
        String sql = "SELECT COUNT(*) FROM mtp_meetup_note_history WHERE meetup_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, meetupId);
        return count != null ? count : 0;
    }

    public void saveMeetupNote(Long meetupId, String note) {
        String sql = "INSERT INTO mtp_note (meetup_id, note) VALUES (?, ?)";
        jdbcTemplate.update(sql, meetupId, note);
    }

    public int countNotesInMtpNote(Long meetupId) {
        String sql = "SELECT COUNT(*) FROM mtp_note WHERE meetup_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, meetupId);
        return count != null ? count : 0;
    }

    public List<Meetup> findMeetupsByTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return findAll();
        }
        String inSql = String.join(",", java.util.Collections.nCopies(tags.size(), "?"));
        String sql = "SELECT DISTINCT m.* FROM mtp_meetup m " +
                "JOIN mtp_meetup_tag t ON m.id = t.meetupId " +
                "WHERE t.tag IN (" + inSql + ") AND m.is_active = true";
        return jdbcTemplate.query(sql, tags.toArray(), meetupRowMapper);
    }

    public List<Meetup> findAllPaginated(int limit, int offset) {
        String sql = "SELECT id, organizer_name, title, description, start_date, start_time, end_date, end_time, organizer_id, event_address, image_url, latitude, longitude, created_at, max_participants, contact_info, google_location_url, is_active FROM mtp_meetup WHERE is_active = true ORDER BY created_at DESC LIMIT ? OFFSET ?";
        long start = System.currentTimeMillis();
        List<Meetup> result = jdbcTemplate.query(sql, meetupRowMapper, limit, offset);
        long end = System.currentTimeMillis();
        QueryLogger.log(sql, end - start);
        return result;
    }

    public int countAllActiveMeetups() {
        String sql = "SELECT COUNT(*) FROM mtp_meetup WHERE is_active = true";
        long start = System.currentTimeMillis();
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        long end = System.currentTimeMillis();
        QueryLogger.log(sql, end - start);
        return count != null ? count : 0;
    }

    public List<Meetup> findAllWithinBoundingBox(double userLat, double userLon, double latDelta, double lonDelta) {
        String sql = "SELECT * FROM mtp_meetup WHERE is_active = true AND latitude IS NOT NULL AND longitude IS NOT NULL " +
                     "AND latitude BETWEEN ? AND ? AND longitude BETWEEN ? AND ?";
        return jdbcTemplate.query(sql, meetupRowMapper,
            userLat - latDelta, userLat + latDelta, userLon - lonDelta, userLon + lonDelta);
    }

    // Lightweight RowMapper for list view (no tags, notes, questions, or image blob)
    private static final RowMapper<Meetup> meetupListRowMapper = (rs, rowNum) -> {
        Meetup meetup = new Meetup();
        meetup.setId(rs.getLong("id"));
        meetup.setOrganizerName(rs.getString("organizer_name"));
        meetup.setTitle(rs.getString("title"));
        meetup.setDescription(rs.getString("description"));
        meetup.setStartDate(rs.getObject("start_date", java.time.LocalDate.class));
        meetup.setStartTime(rs.getObject("start_time", java.time.LocalTime.class));
        meetup.setEndDate(rs.getObject("end_date", java.time.LocalDate.class));
        meetup.setEndTime(rs.getObject("end_time", java.time.LocalTime.class));
        meetup.setOrganizerId(rs.getString("organizer_id"));
        meetup.setEventAddress(rs.getString("event_address"));
        meetup.setCreatedAt(rs.getObject("created_at", java.time.LocalDateTime.class));
        meetup.setIsActive(rs.getBoolean("is_active"));
        meetup.setMaxParticipants(rs.getObject("max_participants") != null ? rs.getInt("max_participants") : null);
        meetup.setContactInfo(rs.getString("contact_info"));
        meetup.setGoogleLocationURL(rs.getString("google_location_url"));
        meetup.setLatitude(rs.getObject("latitude") != null ? rs.getDouble("latitude") : null);
        meetup.setLongitude(rs.getObject("longitude") != null ? rs.getDouble("longitude") : null);
        // Do not set image, tags, notes, or questions
        return meetup;
    };

    public List<Meetup> findMeetupsInRadiusPaged(double userLat, double userLon, double radiusKm, int limit, int offset) {
        // Calculate bounding box
        double latDelta = radiusKm / 111.0;
        double lonDelta = radiusKm / (111.0 * Math.cos(Math.toRadians(userLat)));
        String sql = """
            SELECT id, organizer_name, title, description, start_date, start_time, end_date, end_time, organizer_id, event_address, created_at, is_active, max_participants, contact_info, google_location_url, latitude, longitude
            FROM mtp_meetup
            WHERE is_active = true
              AND latitude IS NOT NULL AND longitude IS NOT NULL
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
        long start = System.currentTimeMillis();
        List<Meetup> result = jdbcTemplate.query(sql, meetupListRowMapper,
            userLat - latDelta, userLat + latDelta,
            userLon - lonDelta, userLon + lonDelta,
            userLat, userLon, userLat, radiusKm, limit, offset
        );
        long end = System.currentTimeMillis();
        QueryLogger.log(sql, end - start);
        return result;
    }

    /**
     * Returns the count of meetups within the given radius (km) of the user's location, using bounding box and Haversine filtering in SQL.
     */
    public int countMeetupsInRadius(double userLat, double userLon, double radiusKm) {
        // Calculate bounding box
        double latDelta = radiusKm / 111.0;
        double lonDelta = radiusKm / (111.0 * Math.cos(Math.toRadians(userLat)));
        String sql = """
            SELECT COUNT(*) FROM (
                SELECT id FROM mtp_meetup
                WHERE is_active = true
                  AND latitude IS NOT NULL AND longitude IS NOT NULL
                  AND latitude BETWEEN ? AND ?
                  AND longitude BETWEEN ? AND ?
                  AND (6371 * acos(
                    cos(radians(?)) * cos(radians(latitude)) *
                    cos(radians(longitude) - radians(?)) +
                    sin(radians(?)) * sin(radians(latitude))
                  )) <= ?
            )
        """;
        long start = System.currentTimeMillis();
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class,
            userLat - latDelta, userLat + latDelta,
            userLon - lonDelta, userLon + lonDelta,
            userLat, userLon, userLat, // for distance filter
            radiusKm
        );
        long end = System.currentTimeMillis();
        QueryLogger.log(sql, end - start);
        return count != null ? count : 0;
    }
} 