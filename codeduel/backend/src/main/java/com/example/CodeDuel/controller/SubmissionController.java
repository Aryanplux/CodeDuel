package com.example.CodeDuel.controller;

import com.example.CodeDuel.dto.SubmissionRequest;
import com.example.CodeDuel.dto.SubmissionResponse;
import com.example.CodeDuel.model.BattleSession;
import com.example.CodeDuel.model.Problem;
import com.example.CodeDuel.repository.BattleSessionRepository;
import com.example.CodeDuel.service.Judge0Service;
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
    public ResponseEntity<SubmissionResponse> submitCode(@RequestBody SubmissionRequest request) {
        if (request == null || request.getBattleId() == null || request.getSourceCode() == null
                || request.getUserId() == null || request.getLanguage() == null) {
            return ResponseEntity.badRequest().body(
                    SubmissionResponse.builder()
                            .verdict("BAD_REQUEST")
                            .errorMessage("battleId, userId, language, and sourceCode are required.")
                            .build()
            );
        }

        // 1. Fetch Battle Session & Problem
        Optional<BattleSession> sessionOpt = battleSessionRepository.findById(request.getBattleId());
        if (sessionOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    SubmissionResponse.builder()
                            .verdict("BATTLE_NOT_FOUND")
                            .errorMessage("Battle session with ID " + request.getBattleId() + " does not exist.")
                            .build()
            );
        }

        BattleSession session = sessionOpt.get();
        Problem problem = session.getProblem();

        // 2. Parse Test Cases (Fallback to Sample Cases if Hidden Json is missing)
        List<String> inputs = parseJsonArray(problem.getHiddenInputsJson(), problem.getSampleInput());
        List<String> expectedOutputs = parseJsonArray(problem.getHiddenOutputsJson(), problem.getSampleOutput());

        int totalTests = inputs.size();
        int testsPassed = 0;
        int languageId = judge0Service.getLanguageId(request.getLanguage());

        String finalVerdict = "ACCEPTED";
        String lastStdout = "";
        String errorMessage = "";
        String execTime = "0.0";
        String memory = "0";

        // 3. Run Code Against Each Test Case in Judge0
        for (int i = 0; i < totalTests; i++) {
            String testInput = inputs.get(i);
            String expectedOut = expectedOutputs.get(i);

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
                break;
            } else if (statusId == 5) { // Time Limit Exceeded
                finalVerdict = "TIME_LIMIT_EXCEEDED";
                errorMessage = "Time Limit Exceeded on test case " + (i + 1);
                break;
            } else if (statusId == 4) { // Wrong Answer
                finalVerdict = "WRONG_ANSWER";
                errorMessage = "Wrong Answer on test case " + (i + 1);
                break;
            } else { // Runtime Error or internal error
                finalVerdict = "RUNTIME_ERROR";
                errorMessage = !stderr.isBlank() ? stderr : statusDesc;
                break;
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

        // 5. Broadcast Live Opponent Progress via WebSocket
        Map<String, Object> progressUpdate = Map.of(
                "type", "OPPONENT_SUBMISSION",
                "submitterId", request.getUserId(),
                "verdict", finalVerdict,
                "testsPassed", testsPassed,
                "totalTests", totalTests
        );
        messagingTemplate.convertAndSend(
                "/topic/battle/" + request.getBattleId(),
                (Object) progressUpdate);

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