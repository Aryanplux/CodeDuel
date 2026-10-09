package com.example.CodeDuel.service;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class Judge0Service {

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String JUDGE0_URL = "http://localhost:2358/submissions?wait=true";

    /**
     * Map language string to Judge0 Language ID
     */
    public int getLanguageId(String language) {
        return switch (language.toLowerCase().trim()) {
            case "python", "python3" -> 71; // Python (3.8.1)
            case "java" -> 62;             // Java (OpenJDK 13.0.1)
            case "cpp", "c++" -> 54;        // C++ (GCC 9.2.0)
            default -> throw new IllegalArgumentException("Unsupported language: " + language);
        };
    }

    /**
     * Sends code execution payload to Judge0 sandbox
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> executeCode(String sourceCode, int languageId, String stdin, String expectedOutput) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("source_code", sourceCode);
        requestBody.put("language_id", languageId);
        if (stdin != null && !stdin.isBlank()) {
            requestBody.put("stdin", stdin.trim());
        }
        if (expectedOutput != null && !expectedOutput.isBlank()) {
            requestBody.put("expected_output", expectedOutput.trim());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(JUDGE0_URL, entity, Map.class);
            return response.getBody() != null ? response.getBody() : Map.of();
        } catch (Exception e) {
            System.err.println("[Judge0Service] Error communicating with Judge0: " + e.getMessage());
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("status", Map.of("id", 13, "description", "Internal Judge Error"));
            errorResult.put("stderr", e.getMessage());
            return errorResult;
        }
    }
}