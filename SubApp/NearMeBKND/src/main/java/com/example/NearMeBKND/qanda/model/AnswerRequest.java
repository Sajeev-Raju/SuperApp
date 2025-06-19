package com.example.NearMeBKND.qanda.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AnswerRequest {
    @NotNull(message = "qId cannot be null")
    private Long qId;

    @NotBlank(message = "answerDescription cannot be blank")
    private String answerDescription;

    public Long getQId() {
        return qId;
    }

    public void setQId(Long qId) {
        this.qId = qId;
    }

    public String getAnswerDescription() {
        return answerDescription;
    }

    public void setAnswerDescription(String answerDescription) {
        this.answerDescription = answerDescription;
    }
}