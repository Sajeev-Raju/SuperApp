package com.example.NearMeBKND.polls.model;

import java.util.List;

public class VoteRequest {
    private List<Integer> optionIds;
    
    public List<Integer> getOptionIds() {
        return optionIds;
    }
    
    public void setOptionIds(List<Integer> optionIds) {
        this.optionIds = optionIds;
    }
} 