package com.example.CodeDuel.service;

import com.example.CodeDuel.dto.JudgeRequest;
import com.example.CodeDuel.dto.JudgeResponse;
import com.example.CodeDuel.entity.TestCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
public class Judge0Service {

    private final WebClient webClient;

    public Judge0Service(@Value("${judge0.url}") String judge0Url) {
        this.webClient = WebClient.builder()
                .baseUrl(judge0Url)
                .build();
    }

    /**
     * Executes single code input against Judge0 synchronously.
     */
    public JudgeResponse executeSingleTest(String sourceCode, int languageId, String stdin, String expectedOutput, float timeLimit) {
        JudgeRequest request = JudgeRequest.builder()
                .sourceCode(sourceCode)
                .languageId(languageId)
                .stdin(stdin)
                .expectedOutput(expectedOutput)
                .cpuTimeLimit(timeLimit)
                .build();

        return webClient.post()
                .uri("/submissions?wait=true")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(JudgeResponse.class)
                .block(); // Blocking call for synchronous batch judging
    }

    /**
     * Iterates over visible and hidden test cases for a problem and aggregates total passes[cite: 18, 28].
     */
    public ExecutionSummary evaluateAllTests(String sourceCode, int languageId, List<TestCase> testCases, float timeLimit) {
        int passedCount = 0;
        String overallVerdict = "ACCEPTED";

        for (TestCase test : testCases) {
            JudgeResponse response = executeSingleTest(sourceCode, languageId, test.getInput(), test.getExpectedOutput(), timeLimit);

            if (response == null || response.getStatus() == null
                    || response.getStatus().getId() == null) {
                return new ExecutionSummary(0, testCases.size(), "SYSTEM_ERROR");
            }

            if (response.isCompileError()) {
                return new ExecutionSummary(0, testCases.size(), "COMPILATION_ERROR");
            }

            if (response.isAccepted()) {
                passedCount++;
            } else {
                String description = response.getStatus().getDescription();
                overallVerdict = description == null
                        ? "SYSTEM_ERROR"
                        : description.toUpperCase().replace(' ', '_');
                // Stop evaluation on first failure to save resources
                break; 
            }
        }

        if (passedCount == testCases.size()) {
            overallVerdict = "ACCEPTED";
        }

        return new ExecutionSummary(passedCount, testCases.size(), overallVerdict);
    }

    public record ExecutionSummary(int passedCount, int totalCount, String verdict) {}
}