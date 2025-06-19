package com.example.NearMeBKND.classified.service;

import com.example.NearMeBKND.classified.model.ClfQuestion;
import com.example.NearMeBKND.classified.repository.ClfQuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClfQuestionService {
    @Autowired
    private ClfQuestionRepository repository;

    public int postQuestion(ClfQuestion question) {
        if (!repository.classifiedExists(question.getClassifiedId())) {
            throw new IllegalArgumentException("Classified not found");
        }
        return repository.saveQuestion(question);
    }

    public void deleteQuestion(int questionId) {
        repository.deleteQuestion(questionId);
    }

    public ClfQuestion getQuestionById(int id) {
        return repository.getQuestionById(id);
    }

    public List<ClfQuestion> getQuestionsByClassifiedId(int classifiedId) {
        return repository.getQuestionsByClassifiedId(classifiedId);
    }
} 