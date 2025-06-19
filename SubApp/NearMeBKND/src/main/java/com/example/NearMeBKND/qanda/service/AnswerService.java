package com.example.NearMeBKND.qanda.service;

import com.example.NearMeBKND.qanda.model.Answer;
import com.example.NearMeBKND.qanda.repository.AnswerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AnswerService {

    @Autowired
    private AnswerRepository answerRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public int addAnswer(Answer answer) {
        return answerRepository.saveAnswer(answer);
    }

    public List<Answer> getAnswersForQuestion(int qId) {
        return answerRepository.findByQuestionId(qId);
    }

    public void deleteAnswer(int aId) {
        answerRepository.deleteAnswer(aId);
    }

    public void acceptAnswer(int aId) {
        answerRepository.acceptAnswer(aId);
    }

    public Answer getAnswerById(int id) {
        return answerRepository.findById(id);
    }
}