package com.example.CodeDuel.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.LinkedHashMap;

@Service
public class Judge0Service {

    private static final Logger log = LoggerFactory.getLogger(Judge0Service.class);
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${judge0.url:http://localhost:2358}")
    private String judge0BaseUrl;

    /**
     * Map language string to Judge0 Language ID
     */
    public int getLanguageId(String language) {
        if (language == null) {
            throw new IllegalArgumentException("Language cannot be null");
        }
        String normalized = language.toLowerCase().trim()
                .replace("_", "")
                .replace("-", "")
                .replace(" ", "");
        return switch (normalized) {
            case "71", "python", "python3", "python38", "python39", "python310", "python311", "python312" -> 71;
            case "62", "java", "java17", "openjdk", "openjdk17" -> 62;
            case "54", "cpp", "c++", "cpp17", "c++17", "g++", "gplusplus" -> 54;
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
            Map<String, Object> errorResult = new java.util.HashMap<>();
            errorResult.put("status", Map.of("id", 13, "description", "Blank Source Code"));
            errorResult.put("stderr", "source_code can't be blank");
            return errorResult;
        }

        String targetUrl = judge0BaseUrl.replaceAll("/+$", "")
                + "/submissions?wait=true&base64_encoded=false";

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("source_code", sourceCode);
        requestBody.put("language_id", languageId);
        requestBody.put("cpu_time_limit", 2.0);
        requestBody.put("memory_limit", 128000);
        if (stdin != null && !stdin.isBlank()) {
            requestBody.put("stdin", stdin.trim());
        }
        if (expectedOutput != null && !expectedOutput.isBlank()) {
            requestBody.put("expected_output", expectedOutput.trim());
        }

        log.info("[Judge0Service] Sending submission to Judge0 at {}: langId={}, sourceLength={} chars",
                targetUrl, languageId, sourceCode.length());

        try {
            String requestJson = objectMapper.writeValueAsString(requestBody);
            log.debug("[Judge0Service] Judge0 request JSON fields: source_code={}, language_id={}",
                    sourceCode.length(), languageId);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.error("[Judge0Service] Judge0 returned HTTP {} with body: {}",
                        response.statusCode(), responseBody);
                return judgeError("Judge Sandbox Error", responseBody);
            }

            if (responseBody == null || responseBody.isBlank()) {
                return judgeError("Judge Sandbox Error", "Judge0 returned an empty response");
            }

            return objectMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {});
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[Judge0Service] Interrupted while communicating with Judge0", e);
            return judgeError("Internal Judge Error", "Judge0 request was interrupted");
        } catch (Exception e) {
            log.error("[Judge0Service] Error communicating with Judge0: {}", e.getMessage(), e);
            return judgeError("Internal Judge Error", e.getMessage());
        }
    }

    private Map<String, Object> judgeError(String description, String details) {
        Map<String, Object> errorResult = new java.util.HashMap<>();
        errorResult.put("status", Map.of("id", 13, "description", description));
        errorResult.put("stderr", details == null || details.isBlank() ? description : details);
        return errorResult;
    }
}