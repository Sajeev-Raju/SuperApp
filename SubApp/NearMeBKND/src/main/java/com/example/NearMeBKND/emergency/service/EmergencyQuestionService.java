package com.example.NearMeBKND.emergency.service;

import com.example.NearMeBKND.emergency.model.EmergencyQuestion;
import com.example.NearMeBKND.emergency.repository.EmergencyQuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class EmergencyQuestionService {
    @Autowired
    private EmergencyQuestionRepository repository;

    public int postQuestion(EmergencyQuestion question) {
        return repository.saveQuestion(question);
    }

    public List<EmergencyQuestion> getQuestionsByEmergencyId(int emergencyId) {
        return repository.getQuestionsByEmergencyId(emergencyId);
    }

    public EmergencyQuestion getQuestionById(int id) {
        return repository.getQuestionById(id);
    }

    public void deleteQuestion(int id) {
        repository.deleteQuestion(id);
    }
} 