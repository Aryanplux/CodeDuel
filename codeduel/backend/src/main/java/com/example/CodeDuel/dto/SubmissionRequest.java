package com.example.CodeDuel.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionRequest {
    private String battleId;
    private String userId;
    private String language; // "python", "java", "cpp"
    private String sourceCode;
}
