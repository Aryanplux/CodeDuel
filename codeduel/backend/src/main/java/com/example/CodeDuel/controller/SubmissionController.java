package com.example.CodeDuel.controller;

import com.example.CodeDuel.dto.SubmissionRequest;
import com.example.CodeDuel.dto.SubmissionResponse;
import com.example.CodeDuel.model.BattleSession;
import com.example.CodeDuel.model.Problem;
import com.example.CodeDuel.repository.BattleSessionRepository;
import com.example.CodeDuel.service.Judge0Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/submissions")
@CrossOrigin(origins = "*")
public class SubmissionController {

    private static final Logger log = LoggerFactory.getLogger(SubmissionController.class);

    private final BattleSessionRepository battleSessionRepository;
    private final Judge0Service judge0Service;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SubmissionController(BattleSessionRepository battleSessionRepository,
                                Judge0Service judge0Service,
                                SimpMessagingTemplate messagingTemplate) {
        this.battleSessionRepository = battleSessionRepository;
        this.judge0Service = judge0Service;
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping
    public ResponseEntity<SubmissionResponse> submitCode(@RequestBody(required = false) SubmissionRequest request) {
        // Detailed logging and input validation guard checks
        if (request == null) {
            log.warn("[SubmissionController] Rejected submission: request body is null.");
            return ResponseEntity.badRequest().body(
                    SubmissionResponse.builder()
                            .verdict("BAD_REQUEST")
                            .errorMessage("Request body cannot be null.")
                            .build()
            );
        }

        log.info("[SubmissionController] Received submission - battleId: {}, userId: {}, language: {}, sourceCode length: {} chars",
                request.getBattleId(),
                request.getUserId(),
                request.getLanguage(),
                request.getSourceCode() != null ? request.getSourceCode().length() : 0);

        if (request.getBattleId() == null || request.getBattleId().trim().isEmpty()) {
            log.warn("[SubmissionController] Rejected submission: battleId is missing or blank.");
            return ResponseEntity.badRequest().body(
                    SubmissionResponse.builder()
                            .verdict("BAD_REQUEST")
                            .errorMessage("battleId cannot be blank.")
                            .build()
            );
        }

        if (request.getUserId() == null || request.getUserId().trim().isEmpty()) {
            log.warn("[SubmissionController] Rejected submission: userId is missing or blank.");
            return ResponseEntity.badRequest().body(
                    SubmissionResponse.builder()
                            .verdict("BAD_REQUEST")
                            .errorMessage("userId cannot be blank.")
                            .build()
            );
        }

        if (request.getLanguage() == null || request.getLanguage().trim().isEmpty()) {
            log.warn("[SubmissionController] Rejected submission: language is missing or blank.");
            return ResponseEntity.badRequest().body(
                    SubmissionResponse.builder()
                            .verdict("BAD_REQUEST")
                            .errorMessage("language cannot be blank.")
                            .build()
            );
        }

        if (request.getSourceCode() == null || request.getSourceCode().trim().isEmpty()) {
            log.warn("[SubmissionController] Rejected submission: sourceCode is missing or blank.");
            return ResponseEntity.badRequest().body(
                    SubmissionResponse.builder()
                            .verdict("BAD_REQUEST")
                            .errorMessage("sourceCode cannot be blank.")
                            .build()
            );
        }

        int languageId;
        try {
            languageId = judge0Service.getLanguageId(request.getLanguage());
        } catch (IllegalArgumentException e) {
            log.warn("[SubmissionController] Rejected submission: unsupported language '{}'", request.getLanguage());
            return ResponseEntity.badRequest().body(
                    SubmissionResponse.builder()
                            .verdict("BAD_REQUEST")
                            .errorMessage(e.getMessage())
                            .build()
            );
        }

        // 1. Fetch Battle Session & Problem
        Optional<BattleSession> sessionOpt = battleSessionRepository.findById(request.getBattleId());
        if (sessionOpt.isEmpty()) {
            log.warn("[SubmissionController] Battle session with ID {} does not exist.", request.getBattleId());
            return ResponseEntity.badRequest().body(
                    SubmissionResponse.builder()
                            .verdict("BATTLE_NOT_FOUND")
                            .errorMessage("Battle session with ID " + request.getBattleId() + " does not exist.")
                            .build()
            );
        }

        BattleSession session = sessionOpt.get();
        Problem problem = session.getProblem();
        if (problem == null) {
            log.error("[SubmissionController] No problem associated with battle session {}", request.getBattleId());
            return ResponseEntity.badRequest().body(
                    SubmissionResponse.builder()
                            .verdict("PROBLEM_NOT_FOUND")
                            .errorMessage("Problem not found for battle session " + request.getBattleId())
                            .build()
            );
        }

        // 2. Parse Test Cases (Fallback to Sample Cases if Hidden Json is missing)
        List<String> inputs = parseJsonArray(problem.getHiddenInputsJson(), problem.getSampleInput());
        List<String> expectedOutputs = parseJsonArray(problem.getHiddenOutputsJson(), problem.getSampleOutput());

        int totalTests = inputs.size();
        int testsPassed = 0;

        String finalVerdict = "ACCEPTED";
        String lastStdout = "";
        String errorMessage = "";
        String execTime = "0.0";
        String memory = "0";

        // 3. Run Code Against Each Test Case in Judge0
        for (int i = 0; i < totalTests; i++) {
            String testInput = inputs.get(i);
            String expectedOut = expectedOutputs.get(i);

            log.debug("[SubmissionController] Executing test case {}/{} for battleId: {}", (i + 1), totalTests, request.getBattleId());

            Map<String, Object> judgeResult = judge0Service.executeCode(
                    request.getSourceCode(),
                    languageId,
                    testInput,
                    expectedOut
            );

            Map<?, ?> status = judgeResult.get("status") instanceof Map<?, ?> statusValue
                    ? statusValue
                    : Map.of();
            int statusId = status.get("id") instanceof Number id ? id.intValue() : 13;
            String statusDesc = status.get("description") instanceof String description
                    ? description
                    : "Unknown Error";

            lastStdout = judgeResult.get("stdout") != null ? (String) judgeResult.get("stdout") : "";
            String stderr = judgeResult.get("stderr") != null ? (String) judgeResult.get("stderr") : "";
            String compileOutput = judgeResult.get("compile_output") != null ? (String) judgeResult.get("compile_output") : "";

            execTime = judgeResult.get("time") != null ? String.valueOf(judgeResult.get("time")) : execTime;
            memory = judgeResult.get("memory") != null ? String.valueOf(judgeResult.get("memory")) : memory;

            // Status ID 3 = Accepted in Judge0
            if (statusId == 3) {
                testsPassed++;
            } else if (statusId == 6) { // Compilation Error
                finalVerdict = "COMPILE_ERROR";
                errorMessage = compileOutput;
                log.info("[SubmissionController] Test {} failed with COMPILE_ERROR: {}", (i + 1), compileOutput);
                break;
            } else if (statusId == 5) { // Time Limit Exceeded
                if (finalVerdict.equals("ACCEPTED")) {
                    finalVerdict = "TIME_LIMIT_EXCEEDED";
                    errorMessage = "Time Limit Exceeded on test case " + (i + 1);
                }
            } else if (statusId == 4) { // Wrong Answer
                if (finalVerdict.equals("ACCEPTED")) {
                    finalVerdict = "WRONG_ANSWER";
                    errorMessage = "Wrong Answer on test case " + (i + 1);
                }
            } else { // Runtime Error or internal error
                if (finalVerdict.equals("ACCEPTED")) {
                    finalVerdict = "RUNTIME_ERROR";
                    errorMessage = !stderr.isBlank() ? stderr : statusDesc;
                }
            }
        }

        // 4. Construct Submission Result
        SubmissionResponse response = SubmissionResponse.builder()
                .verdict(finalVerdict)
                .testsPassed(testsPassed)
                .totalTests(totalTests)
                .output(lastStdout.trim())
                .executionTime(execTime + "s")
                .memoryUsed(memory + " KB")
                .errorMessage(errorMessage)
                .build();

        log.info("[SubmissionController] Submission evaluated for userId: {} in battleId: {} -> Verdict: {}, Passed: {}/{}",
                request.getUserId(), request.getBattleId(), finalVerdict, testsPassed, totalTests);

        // 5. Broadcast Live Opponent Progress via WebSocket
        Map<String, Object> progressUpdate = Map.of(
                "type", "OPPONENT_SUBMISSION",
                "submitterId", request.getUserId(),
                "verdict", finalVerdict,
                "testsPassed", testsPassed,
                "totalTests", totalTests
        );
        try {
            messagingTemplate.convertAndSend(
                    "/topic/battle/" + request.getBattleId(),
                    (Object) progressUpdate);
        } catch (Exception ex) {
            log.error("[SubmissionController] Failed to broadcast WebSocket update: {}", ex.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    private List<String> parseJsonArray(String json, String fallback) {
        if (json != null && !json.isBlank()) {
            try {
                return objectMapper.readValue(json, new TypeReference<List<String>>() {});
            } catch (Exception ignored) {}
        }
        return List.of(fallback != null ? fallback : "");
    }
}