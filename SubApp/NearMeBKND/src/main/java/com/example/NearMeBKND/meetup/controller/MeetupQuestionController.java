package com.example.NearMeBKND.meetup.controller;

import com.example.NearMeBKND.meetup.model.MeetupQuestion;
import com.example.NearMeBKND.meetup.service.MeetupQuestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/meetups/{meetupId}/questions")
public class MeetupQuestionController {
    private final MeetupQuestionService questionService;

    public MeetupQuestionController(MeetupQuestionService questionService) {
        this.questionService = questionService;
    }

    @PostMapping
    public ResponseEntity<?> postQuestion(
            @PathVariable Long meetupId,
            @RequestHeader("X-User-ID") String userId,
            @RequestBody Map<String, String> body) {
        String content = body.get("content");
        questionService.postQuestion(meetupId, userId, content);
        return ResponseEntity.ok(Map.of("message", "Question posted successfully"));
    }

    @GetMapping
    public ResponseEntity<List<MeetupQuestion>> getQuestions(
            @PathVariable Long meetupId,
            @RequestHeader("X-User-ID") String userId) {
        return ResponseEntity.ok(questionService.getQuestions(meetupId, userId));
    }
} 