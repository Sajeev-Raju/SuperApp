package com.example.NearMeBKND.qanda.model;

import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonProperty;

public class QuestionRequest {
   private static final Logger logger = LoggerFactory.getLogger(QuestionRequest.class);

   @JsonProperty("qTitle")
   @NotBlank(message = "qTitle cannot be blank")
   private String qTitle;

   private String questionDescription;
   private java.util.List<String> tags;

   public QuestionRequest() {}

   public String getQTitle() {
       logger.debug("Getting qTitle: {}", qTitle);
       return qTitle;
   }

   public void setQTitle(String qTitle) {
       logger.debug("Setting qTitle: {}", qTitle);
       this.qTitle = qTitle;
   }

   public String getQuestionDescription() {
       return questionDescription;
   }

   public void setQuestionDescription(String questionDescription) {
       this.questionDescription = questionDescription;
   }

   public java.util.List<String> getTags() {
       return tags;
   }

   public void setTags(java.util.List<String> tags) {
       this.tags = tags;
   }

   @Override
   public String toString() {
       return "QuestionRequest{" +
               "qTitle='" + qTitle + '\'' +
               ", questionDescription='" + questionDescription + '\'' +
               ", tags=" + tags +
               '}';
   }
}
