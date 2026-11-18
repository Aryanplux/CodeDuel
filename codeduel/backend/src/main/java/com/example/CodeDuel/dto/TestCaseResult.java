package com.example.CodeDuel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestCaseResult {
    private int caseNumber;
    private String input;
    private String expectedOutput;
    private String actualOutput;
    private boolean passed;
    private String status;
}
