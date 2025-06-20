package com.example.NearMeBKND.polls.model;

import java.util.List;

public class CreatePollCollectionRequest {
    private String collectionTitle;
    private List<CreatePollQuestionRequest> questions;

    public String getCollectionTitle() { return collectionTitle; }
    public void setCollectionTitle(String collectionTitle) { this.collectionTitle = collectionTitle; }
    public List<CreatePollQuestionRequest> getQuestions() { return questions; }
    public void setQuestions(List<CreatePollQuestionRequest> questions) { this.questions = questions; }

    public static class CreatePollQuestionRequest {
        private String questionText;
        private List<String> pollOptions;
        private int selectionLimit;

        public String getQuestionText() { return questionText; }
        public void setQuestionText(String questionText) { this.questionText = questionText; }
        public List<String> getPollOptions() { return pollOptions; }
        public void setPollOptions(List<String> pollOptions) { this.pollOptions = pollOptions; }
        public int getSelectionLimit() { return selectionLimit; }
        public void setSelectionLimit(int selectionLimit) { this.selectionLimit = selectionLimit; }
    }
} 