// package com.example.submitqanda.repository;

// public class AnswerRepository {

// }


package com.example.NearMeBKND.qanda.repository;

import com.example.NearMeBKND.qanda.model.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class AnswerRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public int saveAnswer(Answer answer) {
        jdbcTemplate.update(
            "INSERT INTO qna_answers (question_id, user_id, description) VALUES (?, ?, ?)",
            answer.getQuestionId(), answer.getUserId(), answer.getDescription()
        );
        Integer id = jdbcTemplate.queryForObject("SELECT last_insert_rowid()", Integer.class);
        return id != null ? id : -1;
    }

    public List<Answer> findByQuestionId(int questionId) {
        String sql = "SELECT * FROM qna_answers WHERE question_id = ?";
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapRowToAnswer(rs), questionId);
    }

    public void deleteAnswer(int id) {
        jdbcTemplate.update("DELETE FROM qna_answers WHERE id = ?", id);
    }

    public void acceptAnswer(int aId) {
        jdbcTemplate.update("UPDATE qna_answers SET accepted = 1 WHERE aId = ?", aId);
    }

    public void unacceptAnswersForQuestion(int qId) {
        jdbcTemplate.update("UPDATE qna_answers SET accepted = 0 WHERE qId = ?", qId);
    }

    public Answer findById(int id) {
        String sql = "SELECT * FROM qna_answers WHERE id = ?";
        List<Answer> answers = jdbcTemplate.query(sql, (rs, rowNum) -> mapRowToAnswer(rs), id);
        return answers.isEmpty() ? null : answers.get(0);
    }

    private Answer mapRowToAnswer(ResultSet rs) throws SQLException {
        Answer a = new Answer();
        a.setId(rs.getLong("id"));
        a.setQuestionId(rs.getLong("question_id"));
        a.setUserId(rs.getString("user_id"));
        a.setDescription(rs.getString("description"));
        return a;
    }
}