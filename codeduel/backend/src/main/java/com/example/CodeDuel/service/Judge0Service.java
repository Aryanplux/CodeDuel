package com.example.CodeDuel.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class Judge0Service {

    private static final Logger log = LoggerFactory.getLogger(Judge0Service.class);
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${judge0.url:http://localhost:2358}")
    private String judge0BaseUrl;

    /**
     * Map language string to Judge0 Language ID
     */
    public int getLanguageId(String language) {
        if (language == null) {
            throw new IllegalArgumentException("Language cannot be null");
        }
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
        if (sourceCode == null || sourceCode.trim().isEmpty()) {
            log.warn("[Judge0Service] Aborting execution: source_code is null or blank.");
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("status", Map.of("id", 13, "description", "Blank Source Code"));
            errorResult.put("stderr", "source_code can't be blank");
            return errorResult;
        }

        String targetUrl = judge0BaseUrl.replaceAll("/+$", "") + "/submissions?wait=true";

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

        log.info("[Judge0Service] Sending submission to Judge0 at {}: langId={}, sourceLength={} chars",
                targetUrl, languageId, sourceCode.length());

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(targetUrl, entity, Map.class);
            return response.getBody() != null ? response.getBody() : Map.of();
        } catch (HttpStatusCodeException e) {
            String responseBody = e.getResponseBodyAsString();
            log.error("[Judge0Service] Judge0 returned HTTP {} with body: {}", e.getStatusCode(), responseBody);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("status", Map.of("id", 13, "description", "Judge Sandbox Error"));
            errorResult.put("stderr", responseBody.isBlank() ? e.getMessage() : responseBody);
            return errorResult;
        } catch (Exception e) {
            log.error("[Judge0Service] Error communicating with Judge0: {}", e.getMessage(), e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("status", Map.of("id", 13, "description", "Internal Judge Error"));
            errorResult.put("stderr", e.getMessage());
            return errorResult;
        }
    }
}