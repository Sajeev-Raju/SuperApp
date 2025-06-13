package com.example.idgeneration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuggestedIdsResponse {
    private List<String> fancyIds;
    private List<String> randomIds;
    private int attempt;
}
