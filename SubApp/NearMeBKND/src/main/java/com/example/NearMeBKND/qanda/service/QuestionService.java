// package com.example.submitqanda.service;

// public class QuestionService {

// }


package com.example.NearMeBKND.qanda.service;

import com.example.NearMeBKND.qanda.model.Question;
import com.example.NearMeBKND.qanda.model.Tag;
// import com.example.submitqanda.model.Answer;
import com.example.NearMeBKND.qanda.repository.QuestionRepository;
import com.example.NearMeBKND.qanda.repository.TagRepository;
import com.example.NearMeBKND.qanda.repository.AnswerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class QuestionService {

    @Autowired
    private QuestionRepository questionRepository;
    @Autowired
    private TagRepository tagRepository;
    @Autowired
    private AnswerRepository answerRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public int addQuestion(Question question, List<String> tagNames) {
        // Save the question first
        int questionId = questionRepository.saveQuestion(question);
        
        if (questionId > 0 && tagNames != null && !tagNames.isEmpty()) {
            // Save each tag and get its ID
            List<Integer> tagIds = new ArrayList<>();
            for (String tagName : tagNames) {
                int tagId = tagRepository.saveTag(tagName);
                tagIds.add(tagId);
            }
            
            // Create associations between question and tags
            questionRepository.addTagsToQuestion(questionId, tagIds);
            
            // Update the question's tags field
            question.setTags(tagNames.stream().collect(Collectors.joining(",")));
        }
        
        return questionId;
    }

    public Map<String, Object> getAllQuestions(int page, int size) {
        List<Question> allQuestions = questionRepository.findAll();
        int totalQuestions = allQuestions.size();
        int totalPages = (int) Math.ceil((double) totalQuestions / size);
        
        int start = page * size;
        int end = Math.min(start + size, totalQuestions);
        
        List<Question> paginatedQuestions = allQuestions.subList(start, end);
        
        for (Question q : paginatedQuestions) {
            List<Tag> questionTags = questionRepository.findTagsForQuestion(q.getId().intValue());
            q.setTags(questionTags.stream().map(Tag::getTagName).collect(Collectors.joining(",")));
            q.setTagList(questionTags);
            q.setAnswers(answerRepository.findByQuestionId(q.getId().intValue()));
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("questions", paginatedQuestions);
        response.put("currentPage", page);
        response.put("totalPages", totalPages);
        response.put("totalQuestions", totalQuestions);
        response.put("pageSize", size);
        
        return response;
    }

    public Map<String, Object> getQuestionsByTags(List<String> tags, int page, int size) {
        List<Question> allQuestions = questionRepository.findByTags(tags);
        int totalQuestions = allQuestions.size();
        int totalPages = (int) Math.ceil((double) totalQuestions / size);
        
        int start = page * size;
        int end = Math.min(start + size, totalQuestions);
        
        List<Question> paginatedQuestions = allQuestions.subList(start, end);
        
        for (Question q : paginatedQuestions) {
            List<Tag> questionTags = questionRepository.findTagsForQuestion(q.getId().intValue());
            q.setTags(questionTags.stream().map(Tag::getTagName).collect(Collectors.joining(",")));
            q.setTagList(questionTags);
            q.setAnswers(answerRepository.findByQuestionId(q.getId().intValue()));
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("questions", paginatedQuestions);
        response.put("currentPage", page);
        response.put("totalPages", totalPages);
        response.put("totalQuestions", totalQuestions);
        response.put("pageSize", size);
        
        return response;
    }

    public Optional<Question> getQuestionById(Long id) {
        Question question = questionRepository.findById(id);
        if (question != null) {
            List<Tag> questionTags = questionRepository.findTagsForQuestion(id.intValue());
            question.setTags(questionTags.stream().map(Tag::getTagName).collect(Collectors.joining(",")));
            question.setTagList(questionTags);
            question.setAnswers(answerRepository.findByQuestionId(id.intValue()));
        }
        return Optional.ofNullable(question);
    }

    public void deleteQuestion(int qId) {
        questionRepository.deleteQuestion(qId);
    }

    public void updateQuestion(Question question) {
        String sql = "UPDATE qna_questions SET notes = ? WHERE id = ?";
        jdbcTemplate.update(sql, question.getNotes(), question.getId());
    }

    public void addNote(Long questionId, String note) {
        questionRepository.addNote(questionId, note);
    }

    public List<Question> findQuestionsInRadiusPaged(double userLat, double userLon, double radiusKm, int limit, int offset) {
        return questionRepository.findQuestionsInRadiusPaged(userLat, userLon, radiusKm, limit, offset);
    }

    public int countQuestionsInRadius(double userLat, double userLon, double radiusKm) {
        return questionRepository.countQuestionsInRadius(userLat, userLon, radiusKm);
    }
}