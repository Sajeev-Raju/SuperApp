package com.example.NearMeBKND.meetup.service;

import com.example.NearMeBKND.meetup.model.Meetup;
import com.example.NearMeBKND.meetup.model.MeetupQuestion;
import com.example.NearMeBKND.meetup.repository.MeetupQuestionRepository;
import com.example.NearMeBKND.meetup.repository.MeetupRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MeetupQuestionService {
    private final MeetupQuestionRepository questionRepository;
    private final MeetupRepository meetupRepository;
    private final MeetupService meetupService;

    public MeetupQuestionService(MeetupQuestionRepository questionRepository, MeetupRepository meetupRepository, MeetupService meetupService) {
        this.questionRepository = questionRepository;
        this.meetupRepository = meetupRepository;
        this.meetupService = meetupService;
    }

    public void postQuestion(Long meetupId, String userId, String content) {
        Meetup meetup = meetupRepository.findById(meetupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Meetup not found"));
        if (userId.equals(meetup.getOrganizerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot ask questions on your own meetup.");
        }
        Meetup.UserLocation userLoc = meetupService.getUserLocation(userId);
        if (!meetupService.isWithinAccessRadius(userLoc.latitude(), userLoc.longitude(), meetup.getLatitude(), meetup.getLongitude())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not within the allowed radius to ask questions.");
        }
        MeetupQuestion question = new MeetupQuestion();
        question.setMeetupId(meetupId);
        question.setUserId(userId);
        question.setContent(content);
        question.setCreatedAt(LocalDateTime.now());
        questionRepository.saveQuestion(question);
    }

    public List<MeetupQuestion> getQuestions(Long meetupId, String userId) {
        Meetup meetup = meetupRepository.findById(meetupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Meetup not found"));
        if (userId.equals(meetup.getOrganizerId())) {
            return questionRepository.findByMeetupId(meetupId);
        }
        Meetup.UserLocation userLoc = meetupService.getUserLocation(userId);
        if (!meetupService.isWithinAccessRadius(userLoc.latitude(), userLoc.longitude(), meetup.getLatitude(), meetup.getLongitude())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not within the allowed radius to view questions.");
        }
        return questionRepository.findByMeetupId(meetupId);
    }
} 