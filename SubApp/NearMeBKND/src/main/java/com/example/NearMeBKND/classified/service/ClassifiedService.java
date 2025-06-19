package com.example.NearMeBKND.classified.service;

import com.example.NearMeBKND.classified.model.Classified;
import com.example.NearMeBKND.classified.model.ClfQuestion;
import com.example.NearMeBKND.classified.repository.ClassifiedRepository;
import com.example.NearMeBKND.classified.repository.ClfQuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClassifiedService {

    @Autowired
    private ClassifiedRepository repository;

    @Autowired
    private ClfQuestionRepository questionRepository;

    public Classified createClassified(Classified classified) {
        int id = repository.saveClassified(classified);
        return repository.getClassifiedById(id);
    }

    public Classified getClassifiedById(int id) {
        return repository.getClassifiedById(id);
    }

    public List<String> getAllCategories() {
        return repository.getAllCategories();
    }

    public void deleteClassified(int id) {
        repository.deleteClassified(id);
    }

    public String getClassifiedUserId(int id) {
        return repository.getClassifiedUserId(id);
    }

    public void updateClassifiedNotes(int id, String newNote) {
        repository.updateClassifiedNotes(id, newNote);
    }

    public List<ClfQuestion> getQuestionsByClassifiedId(int classifiedId) {
        return questionRepository.getQuestionsByClassifiedId(classifiedId);
    }

    public List<Classified> getAllClassifiedsRaw() {
        return repository.getAllClassifiedsRaw();
    }

    public List<Classified> getClassifiedsInRadiusPaged(double userLat, double userLon, double radiusKm, int limit, int offset) {
        return repository.getClassifiedsInRadiusPaged(userLat, userLon, radiusKm, limit, offset);
    }

    public int countClassifiedsInRadius(double userLat, double userLon, double radiusKm) {
        return repository.countClassifiedsInRadius(userLat, userLon, radiusKm);
    }
}
