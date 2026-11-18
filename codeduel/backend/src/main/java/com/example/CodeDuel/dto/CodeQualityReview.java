package com.example.CodeDuel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeQualityReview {
    private Integer score;
    private String complexity;
    private String feedback;
    private String status;
}
