package com.example.CodeDuel.controller;

import com.example.CodeDuel.dto.SubmissionResponse;
import com.example.CodeDuel.dto.TestCaseResult;
import com.example.CodeDuel.entity.Submission;
import com.example.CodeDuel.model.BattleSession;
import com.example.CodeDuel.model.Problem;
import com.example.CodeDuel.repository.BattleSessionRepository;
import com.example.CodeDuel.repository.SubmissionRepository;
import com.example.CodeDuel.service.Judge0Service;
import com.example.CodeDuel.service.OpenRouterReviewService;
import com.example.CodeDuel.service.EloService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import com.example.CodeDuel.controller.AuthController;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/submissions")
@CrossOrigin(origins = "*")
public class SubmissionController {

    private static final Logger log = LoggerFactory.getLogger(SubmissionController.class);

    private final BattleSessionRepository battleSessionRepository;
    private final Judge0Service judge0Service;
    private final OpenRouterReviewService openRouterReviewService;
    private final double executionWeight;
    private final double aiWeight;
    private final SubmissionRepository submissionRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final long submissionTimeoutSeconds;
    private final EloService eloService;

    public SubmissionController(BattleSessionRepository battleSessionRepository,
                                Judge0Service judge0Service,
                                OpenRouterReviewService openRouterReviewService,
                                SubmissionRepository submissionRepository,
                                SimpMessagingTemplate messagingTemplate,
                                @org.springframework.beans.factory.annotation.Value("${codeduel.winner.execution-weight:0.7}") double executionWeight,
                                @org.springframework.beans.factory.annotation.Value("${codeduel.winner.ai-weight:0.3}") double aiWeight,
                                @org.springframework.beans.factory.annotation.Value("${codeduel.submission.timeout-seconds:75}") long submissionTimeoutSeconds,
                                EloService eloService) {
        this.battleSessionRepository = battleSessionRepository;
        this.judge0Service = judge0Service;
        this.openRouterReviewService = openRouterReviewService;
        this.submissionRepository = submissionRepository;
        this.messagingTemplate = messagingTemplate;
        this.executionWeight = executionWeight;
        this.aiWeight = aiWeight;
        this.submissionTimeoutSeconds = Math.max(10, submissionTimeoutSeconds);
        this.eloService = eloService;
    }

    @PostMapping
    public ResponseEntity<SubmissionResponse> submitCode(@RequestBody(required = false) Map<String, Object> request,
                                                        HttpSession httpSession) {
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

        String battleId = getString(request, "battleId", "battle_id", "battleID");
        String userId = getString(request, "userId", "user_id", "userID");
        String language = getString(request, "language", "lang", "language_id");
        String sourceCode = getString(request, "sourceCode", "source_code", "code", "source");
        Integer numericLanguageId = getInteger(request, "languageId", "language_id", "languageID");
        boolean sampleRun = getBoolean(request, "sampleRun", "sample_run");

        log.info("[SubmissionController] Received submission - battleId: {}, userId: {}, language: {}, sourceCode length: {} chars",
                battleId,
                userId,
                language,
                sourceCode != null ? sourceCode.length() : 0);

        if (battleId == null || battleId.trim().isEmpty()) {
            log.warn("[SubmissionController] Rejected submission: battleId is missing or blank.");
            return ResponseEntity.badRequest().body(
                    SubmissionResponse.builder()
                            .verdict("BAD_REQUEST")
                            .errorMessage("battleId cannot be blank.")
                            .build()
            );
        }

        if (userId == null || userId.trim().isEmpty()) {
            log.warn("[SubmissionController] Rejected submission: userId is missing or blank.");
            return ResponseEntity.badRequest().body(
                    SubmissionResponse.builder()
                            .verdict("BAD_REQUEST")
                            .errorMessage("userId cannot be blank.")
                            .build()
            );
        }

        if ((language == null || language.trim().isEmpty()) && numericLanguageId == null) {
            log.warn("[SubmissionController] Rejected submission: language is missing or blank.");
            return ResponseEntity.badRequest().body(
                    SubmissionResponse.builder()
                            .verdict("BAD_REQUEST")
                            .errorMessage("language cannot be blank.")
                            .build()
            );
        }

        if (sourceCode == null || sourceCode.trim().isEmpty()) {
            log.warn("[SubmissionController] Rejected submission: sourceCode is missing or blank.");
            return ResponseEntity.badRequest().body(
                    SubmissionResponse.builder()
                            .verdict("BAD_REQUEST")
                            .errorMessage("sourceCode cannot be blank.")
                            .build()
            );
        }

        String authenticatedId = (String) httpSession.getAttribute(AuthController.SESSION_USER_ID);
        if (authenticatedId == null) {
            return ResponseEntity.status(401).body(SubmissionResponse.builder()
                    .verdict("UNAUTHORIZED")
                    .errorMessage("Please log in before submitting code.")
                    .build());
        }
        if (!authenticatedId.equals(userId)) {
            return ResponseEntity.status(403).body(SubmissionResponse.builder()
                    .verdict("FORBIDDEN")
                    .errorMessage("User identity does not match your authenticated session.")
                    .build());
        }

        int languageId;
        try {
            languageId = numericLanguageId != null
                    ? judge0Service.getLanguageId(String.valueOf(numericLanguageId))
                    : judge0Service.getLanguageId(language);
        } catch (IllegalArgumentException e) {
            log.warn("[SubmissionController] Rejected submission: unsupported language '{}'", language);
            return ResponseEntity.badRequest().body(
                    SubmissionResponse.builder()
                            .verdict("BAD_REQUEST")
                            .errorMessage(e.getMessage())
                            .build()
            );
        }

        // 1. Fetch Battle Session & Problem
        Optional<BattleSession> sessionOpt = battleSessionRepository.findById(battleId);
        if (sessionOpt.isEmpty()) {
            log.warn("[SubmissionController] Battle session with ID {} does not exist.", battleId);
            return ResponseEntity.badRequest().body(
                    SubmissionResponse.builder()
                            .verdict("BATTLE_NOT_FOUND")
                            .errorMessage("Battle session with ID " + battleId + " does not exist.")
                            .build()
            );
        }

        BattleSession session = sessionOpt.get();
        if ("COMPLETED".equals(session.getStatus())) {
            return ResponseEntity.status(409).body(SubmissionResponse.builder()
                    .verdict("ROUND_FINISHED")
                    .errorMessage("This round has already finished.")
                    .locked(true)
                    .build());
        }
        if (!userId.equals(session.getPlayer1Id())
                && !userId.equals(session.getPlayer2Id())) {
            return ResponseEntity.status(403).body(SubmissionResponse.builder()
                    .verdict("FORBIDDEN")
                    .errorMessage("Only players in this battle may submit.")
                    .build());
        }
        Problem problem = session.getProblem();
        if (problem == null) {
            log.error("[SubmissionController] No problem associated with battle session {}", battleId);
            return ResponseEntity.badRequest().body(
                    SubmissionResponse.builder()
                            .verdict("PROBLEM_NOT_FOUND")
                            .errorMessage("Problem not found for battle session " + battleId)
                            .build()
            );
        }

        if (!sampleRun
                && !submissionRepository.findByBattleIdAndUserIdOrderBySubmittedAtDesc(
                        battleId, userId).isEmpty()) {
            return ResponseEntity.status(409).body(SubmissionResponse.builder()
                    .verdict("SUBMISSION_LOCKED")
                    .errorMessage("Your solution has already been submitted for this round.")
                    .locked(true)
                    .build());
        }

        // 2. Parse Test Cases (Fallback to Sample Cases if Hidden Json is missing)
        List<String> inputs = sampleRun
                ? List.of(problem.getSampleInput() == null ? "" : problem.getSampleInput())
                : parseJsonArray(problem.getHiddenInputsJson(), problem.getSampleInput());
        List<String> expectedOutputs = sampleRun
                ? List.of(problem.getSampleOutput() == null ? "" : problem.getSampleOutput())
                : parseJsonArray(problem.getHiddenOutputsJson(), problem.getSampleOutput());

        int totalTests = Math.min(inputs.size(), expectedOutputs.size());
        if (totalTests == 0) {
            return ResponseEntity.badRequest().body(
                    SubmissionResponse.builder()
                            .verdict("BAD_REQUEST")
                            .errorMessage("This problem does not contain any executable test cases.")
                            .build()
            );
        }
        int testsPassed = 0;
        List<TestCaseResult> testCases = new ArrayList<>();

        String finalVerdict = "ACCEPTED";
        String lastStdout = "";
        String errorMessage = "";
        String execTime = "0.0";
        String memory = "0";

        Submission lockedSubmission = null;
        if (!sampleRun) {
            if (session.getSubmissionDeadline() == null) {
                session.setSubmissionDeadline(LocalDateTime.now().plusSeconds(submissionTimeoutSeconds));
                battleSessionRepository.save(session);
            }
            lockedSubmission = submissionRepository.save(Submission.builder()
                    .userId(userId)
                    .battleId(battleId)
                    .problemId(problem.getId())
                    .languageId(languageId)
                    .sourceCode(sourceCode)
                    .status("SUBMITTING")
                    .testsPassed(0)
                    .totalTests(totalTests)
                    .submittedAt(java.time.LocalDateTime.now())
                    .build());

            broadcast(battleId, Map.of(
                    "type", "PLAYER_SUBMITTED",
                    "submitterId", userId,
                    "submittedCount", submissionRepository.findByBattleIdOrderBySubmittedAtAsc(battleId).size(),
                    "totalPlayers", 2,
                    "waitingDeadline", session.getSubmissionDeadline()
                            .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            ));
        }

        // 3. Run Code Against Each Test Case in Judge0
        for (int i = 0; i < totalTests; i++) {
            String testInput = inputs.get(i);
            String expectedOut = expectedOutputs.get(i);

            log.debug("[SubmissionController] Executing test case {}/{} for battleId: {}", (i + 1), totalTests, battleId);

            Map<String, Object> judgeResult = judge0Service.executeCode(
                    sourceCode,
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
            boolean passed = statusId == 3;
            if (statusId == 3) {
                testsPassed++;
            } else if (statusId == 6) { // Compilation Error
                if (finalVerdict.equals("ACCEPTED")) {
                    finalVerdict = "COMPILE_ERROR";
                    errorMessage = compileOutput;
                }
                log.info("[SubmissionController] Test {} failed with COMPILE_ERROR: {}", (i + 1), compileOutput);
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
            testCases.add(TestCaseResult.builder()
                    .caseNumber(i + 1)
                    .input(testInput)
                    .expectedOutput(expectedOut)
                    .actualOutput(lastStdout.trim())
                    .passed(passed)
                    .status(passed ? "PASSED" : statusDesc)
                    .build());
        }

        var aiReview = !sampleRun && finalVerdict.equals("ACCEPTED")
                ? openRouterReviewService.review(
                        language,
                        problem.getTitle(),
                        problem.getDescription(),
                        sourceCode)
                : null;

        if (lockedSubmission != null) {
            lockedSubmission.setStatus(finalVerdict);
            lockedSubmission.setTestsPassed(testsPassed);
            lockedSubmission.setTotalTests(totalTests);
            lockedSubmission.setExecutionTimeSeconds(parseExecutionTime(execTime));
            lockedSubmission.setAiQualityScore(aiReview == null ? null : aiReview.getScore());
            submissionRepository.save(lockedSubmission);
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
                .aiReview(aiReview)
                .testCases(testCases)
                .locked(!sampleRun)
                .build();

        log.info("[SubmissionController] Submission evaluated for userId: {} in battleId: {} -> Verdict: {}, Passed: {}/{}",
                userId, battleId, finalVerdict, testsPassed, totalTests);

        if (!sampleRun) {
            Map<String, Object> progressUpdate = new LinkedHashMap<>();
            progressUpdate.put("type", "PLAYER_SUBMISSION_RESULT");
            progressUpdate.put("submitterId", userId);
            progressUpdate.put("verdict", finalVerdict);
            progressUpdate.put("testsPassed", testsPassed);
            progressUpdate.put("totalTests", totalTests);
            maybeFinishBattle(battleId, progressUpdate);
            broadcast(battleId, progressUpdate);
        }

        return ResponseEntity.ok(response);
    }

    private void maybeFinishBattle(String battleId, Map<String, Object> progressUpdate) {
        BattleSession session = battleSessionRepository.findById(battleId).orElse(null);
        if (session == null || session.getWinnerId() != null) return;

        List<Submission> submissions = submissionRepository.findByBattleIdOrderBySubmittedAtAsc(battleId);
        Submission playerOne = latestFor(submissions, session.getPlayer1Id());
        Submission playerTwo = latestFor(submissions, session.getPlayer2Id());
        if (playerOne == null || playerTwo == null
                || "SUBMITTING".equals(playerOne.getStatus())
                || "SUBMITTING".equals(playerTwo.getStatus())) {
            return;
        }

        boolean bothDidNotSubmit = "NOT_SUBMITTED".equals(playerOne.getStatus())
                && "NOT_SUBMITTED".equals(playerTwo.getStatus());
        Submission winner = bothDidNotSubmit ? null
                : (compareSubmissions(playerOne, playerTwo) >= 0 ? playerOne : playerTwo);
        Submission loser = winner == null ? null : winner == playerOne ? playerTwo : playerOne;

        Map<String, Object> scores = new LinkedHashMap<>();
        for (Submission submission : List.of(playerOne, playerTwo)) {
            scores.put(submission.getUserId(), Map.of(
                    "testsPassed", submission.getTestsPassed() == null ? 0 : submission.getTestsPassed(),
                    "totalTests", submission.getTotalTests() == null ? 0 : submission.getTotalTests(),
                    "aiScore", submission.getAiQualityScore() == null ? 0 : submission.getAiQualityScore(),
                    "executionTime", submission.getExecutionTimeSeconds() == null
                            ? Double.MAX_VALUE : submission.getExecutionTimeSeconds(),
                    "optimalityScore", optimalityScore(submission)
            ));
        }

        session.setWinnerId(winner == null ? null : winner.getUserId());
        session.setStatus("COMPLETED");
        session.setEndTime(java.time.LocalDateTime.now());
        session.setSubmissionDeadline(null);
        battleSessionRepository.save(session);
        if (winner != null) {
            eloService.applyResult(winner.getUserId(), true,
                    eloService.getElo(loser.getUserId()));
            eloService.applyResult(loser.getUserId(), false,
                    eloService.getElo(winner.getUserId()));
        } else {
            eloService.applyResult(session.getPlayer1Id(), false, eloService.getElo(session.getPlayer2Id()));
            eloService.applyResult(session.getPlayer2Id(), false, eloService.getElo(session.getPlayer1Id()));
        }
        progressUpdate.put("type", "BATTLE_FINISHED");
        progressUpdate.put("winnerId", winner == null ? null : winner.getUserId());
        progressUpdate.put("scores", scores);
        progressUpdate.put("reasons", winner == null
                ? Map.of(session.getPlayer1Id(), "Neither player submitted",
                session.getPlayer2Id(), "Neither player submitted")
                : Map.of(winner.getUserId(), winningReason(winner, loser),
                loser.getUserId(), losingReason(winner, loser)));
    }

    private Submission latestFor(List<Submission> submissions, String userId) {
        return submissions.stream()
                .filter(submission -> userId.equals(submission.getUserId()))
                .max(Comparator.comparing(Submission::getSubmittedAt))
                .orElse(null);
    }

    private int compareSubmissions(Submission first, Submission second) {
        int tests = Integer.compare(value(first.getTestsPassed()), value(second.getTestsPassed()));
        if (tests != 0) return tests;
        int optimality = Integer.compare(optimalityScore(first), optimalityScore(second));
        if (optimality != 0) return optimality;
        int efficiency = Double.compare(executionTime(second), executionTime(first));
        if (efficiency != 0) return efficiency;
        return -first.getSubmittedAt().compareTo(second.getSubmittedAt());
    }

    private int optimalityScore(Submission submission) {
        return submission.getAiQualityScore() == null ? 0 : submission.getAiQualityScore();
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    private double executionTime(Submission submission) {
        return submission.getExecutionTimeSeconds() == null
                ? Double.MAX_VALUE : submission.getExecutionTimeSeconds();
    }

    private String winningReason(Submission winner, Submission loser) {
        if (value(winner.getTestsPassed()) != value(loser.getTestsPassed())) return "More test cases passed";
        if (optimalityScore(winner) != optimalityScore(loser)) return "Optimized";
        return "Beat the clock";
    }

    private String losingReason(Submission winner, Submission loser) {
        if (value(winner.getTestsPassed()) != value(loser.getTestsPassed())) return "Fewer test cases passed";
        if (optimalityScore(winner) != optimalityScore(loser)) return "Less optimized";
        return "Slower submission";
    }

    private void broadcast(String battleId, Map<String, Object> payload) {
        try {
            messagingTemplate.convertAndSend("/topic/battle/" + battleId, (Object) payload);
        } catch (Exception exception) {
            log.error("[SubmissionController] Failed to broadcast WebSocket update: {}", exception.getMessage());
        }
    }

    @Scheduled(fixedDelay = 1000)
    void finalizeExpiredSubmissionWindows() {
        List<BattleSession> expired = battleSessionRepository
                .findByStatus("IN_PROGRESS");
        for (BattleSession session : expired) {
            if (session.getEndTime() != null && !session.getEndTime().isAfter(LocalDateTime.now())) {
                finalizeExpiredBattle(session);
                continue;
            }
            List<Submission> submissions = submissionRepository
                    .findByBattleIdOrderBySubmittedAtAsc(session.getBattleId());
            if (submissions.isEmpty()) {
                continue;
            }
            if (session.getSubmissionDeadline() == null) {
                LocalDateTime firstSubmission = submissions.stream()
                        .map(Submission::getSubmittedAt)
                        .min(LocalDateTime::compareTo)
                        .orElse(LocalDateTime.now());
                session.setSubmissionDeadline(firstSubmission.plusSeconds(submissionTimeoutSeconds));
                battleSessionRepository.save(session);
            }
            if (session.getSubmissionDeadline().isAfter(LocalDateTime.now())) {
                continue;
            }

            Set<String> submittedPlayers = new HashSet<>();
            for (Submission submission : submissions) {
                submittedPlayers.add(submission.getUserId());
            }

            List<String> players = List.of(session.getPlayer1Id(), session.getPlayer2Id());
            for (String playerId : players) {
                if (!submittedPlayers.contains(playerId)) {
                    submissionRepository.save(Submission.builder()
                            .userId(playerId)
                            .battleId(session.getBattleId())
                            .problemId(session.getProblem().getId())
                            .status("NOT_SUBMITTED")
                            .testsPassed(0)
                            .totalTests(0)
                            .submittedAt(LocalDateTime.now())
                            .build());
                }
            }

            broadcast(session.getBattleId(), Map.of(
                    "type", "SUBMISSION_TIMEOUT",
                    "submittedCount", submittedPlayers.size(),
                    "totalPlayers", 2
            ));
            session.setSubmissionDeadline(null);
            battleSessionRepository.save(session);
            Map<String, Object> finalUpdate = new LinkedHashMap<>();
            maybeFinishBattle(session.getBattleId(), finalUpdate);
            if (!finalUpdate.isEmpty()) {
                broadcast(session.getBattleId(), finalUpdate);
            }
        }
    }

    private void finalizeExpiredBattle(BattleSession session) {
        List<Submission> submissions = submissionRepository
                .findByBattleIdOrderBySubmittedAtAsc(session.getBattleId());
        Set<String> submittedPlayers = submissions.stream()
                .map(Submission::getUserId)
                .collect(java.util.stream.Collectors.toSet());
        for (String playerId : List.of(session.getPlayer1Id(), session.getPlayer2Id())) {
            if (!submittedPlayers.contains(playerId)) {
                submissionRepository.save(Submission.builder()
                        .userId(playerId)
                        .battleId(session.getBattleId())
                        .problemId(session.getProblem().getId())
                        .status("NOT_SUBMITTED")
                        .testsPassed(0)
                        .totalTests(0)
                        .submittedAt(LocalDateTime.now())
                        .build());
            }
        }
        Map<String, Object> finalUpdate = new LinkedHashMap<>();
        maybeFinishBattle(session.getBattleId(), finalUpdate);
        if (!finalUpdate.isEmpty()) {
            broadcast(session.getBattleId(), finalUpdate);
        }
    }

    private String getString(Map<String, Object> request, String... keys) {
        for (String key : keys) {
            Object value = request.get(key);
            if (value != null) {
                String text = String.valueOf(value).trim();
                if (!text.isEmpty()) return text;
            }
        }
        return null;
    }

    private Integer getInteger(Map<String, Object> request, String... keys) {
        String text = getString(request, keys);
        if (text == null) return null;
        try {
            return Integer.valueOf(text);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private boolean getBoolean(Map<String, Object> request, String... keys) {
        String text = getString(request, keys);
        return text != null && Boolean.parseBoolean(text);
    }

    private Double parseExecutionTime(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            return null;
        }
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