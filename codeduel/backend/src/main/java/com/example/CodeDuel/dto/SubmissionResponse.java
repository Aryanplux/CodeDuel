package com.example.CodeDuel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmissionResponse {
    private String verdict; // "ACCEPTED", "WRONG_ANSWER", "COMPILE_ERROR", "TIME_LIMIT_EXCEEDED", "RUNTIME_ERROR"
    private int testsPassed;
    private int totalTests;
    private String output;
    private String executionTime;
    private String memoryUsed;
    private String errorMessage;
}
