package com.example.NearMeBKND.qanda.repository;

import com.example.NearMeBKND.qanda.model.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class TagRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public int saveTag(String tagName) {
        // First try to get existing tag ID
        try {
            return getTagId(tagName);
        } catch (Exception e) {
            // If tag doesn't exist, insert it
            String sql = "INSERT OR IGNORE INTO qna_tags (tagName) VALUES (?)";
            jdbcTemplate.update(sql, tagName);
            return jdbcTemplate.queryForObject("SELECT last_insert_rowid()", Integer.class);
        }
    }

    public int getTagId(String tagName) {
        String sql = "SELECT tagId FROM qna_tags WHERE tagName = ?";
        return jdbcTemplate.queryForObject(sql, Integer.class, tagName);
    }

    public void linkTagToQuestion(int questionId, int tagId) {
        String sql = "INSERT INTO qna_question_tag (qId, tagId) VALUES (?, ?)";
        jdbcTemplate.update(sql, questionId, tagId);
    }

    public List<Tag> getTagsForQuestion(int questionId) {
        String sql = "SELECT t.tagId as tagId, t.tagName as tagName FROM qna_tags t " +
                    "JOIN qna_question_tag qt ON t.tagId = qt.tagId " +
                    "WHERE qt.qId = ?";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Tag tag = new Tag();
            tag.setTagId(rs.getInt("tagId"));
            tag.setTagName(rs.getString("tagName"));
            return tag;
        }, questionId);
    }

    public List<Tag> findAll() {
        String sql = "SELECT * FROM qna_tags";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Tag tag = new Tag();
            tag.setTagId(rs.getInt("tagId"));
            tag.setTagName(rs.getString("tagName"));
            return tag;
        });
    }

    public List<Integer> findTagIdsByNames(List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) return List.of();
        String inSql = String.join(",", java.util.Collections.nCopies(tagNames.size(), "?"));
        String sql = "SELECT tagId FROM qna_tags WHERE tagName IN (" + inSql + ")";
        return jdbcTemplate.query(sql, tagNames.toArray(), (rs, rowNum) -> rs.getInt("tagId"));
    }
}