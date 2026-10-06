package com.example.CodeDuel.controller;

import com.example.CodeDuel.entity.Submission;
import com.example.CodeDuel.entity.TestCase;
import com.example.CodeDuel.entity.Problem;
import com.example.CodeDuel.repository.ProblemRepository;
import com.example.CodeDuel.repository.SubmissionRepository;
import com.example.CodeDuel.service.Judge0Service;
import com.example.CodeDuel.service.Judge0Service.ExecutionSummary;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {

    private final Judge0Service judge0Service;
    private final SimpMessagingTemplate messagingTemplate;
    private final ProblemRepository problemRepository;
    private final SubmissionRepository submissionRepository;

    public SubmissionController(
            Judge0Service judge0Service,
            SimpMessagingTemplate messagingTemplate,
            ProblemRepository problemRepository,
            SubmissionRepository submissionRepository) {
        this.judge0Service = judge0Service;
        this.messagingTemplate = messagingTemplate;
        this.problemRepository = problemRepository;
        this.submissionRepository = submissionRepository;
    }

    @PostMapping
    public ResponseEntity<Submission> submitCode(@RequestBody Submission request) {
        request.setSubmittedAt(LocalDateTime.now());
        request.setStatus("PROCESSING");

        List<TestCase> testCases = findTestCases(request.getProblemId());

        ExecutionSummary summary = judge0Service.evaluateAllTests(
                request.getSourceCode(),
                request.getLanguageId(),
                testCases,
                findTimeLimit(request.getProblemId()));

        request.setTestsPassed(summary.passedCount());
        request.setTotalTests(summary.totalCount());
        request.setStatus(summary.verdict());
        Submission savedSubmission = submissionRepository.save(request);

        Map<String, Object> wsPayload = Map.of(
                "event", "PLAYER_SUBMITTED",
                "userId", savedSubmission.getUserId(),
                "testsPassed", summary.passedCount(),
                "totalTests", summary.totalCount(),
                "status", summary.verdict());

        messagingTemplate.convertAndSend(
                "/topic/battle/" + savedSubmission.getBattleId(),
                (Object) wsPayload);

        return ResponseEntity.ok(savedSubmission);
    }

    private List<TestCase> findTestCases(Long problemId) {
        if (problemId != null) {
            Problem problem = problemRepository.findById(problemId).orElse(null);
            if (problem != null && problem.getTestCases() != null && !problem.getTestCases().isEmpty()) {
                return problem.getTestCases();
            }
        }

        return List.of(
                new TestCase(1L, "2 3\n", "5\n", false),
                new TestCase(2L, "10 20\n", "30\n", true));
    }

    private float findTimeLimit(Long problemId) {
        if (problemId != null) {
            Problem problem = problemRepository.findById(problemId).orElse(null);
            if (problem != null && problem.getTimeLimit() != null) {
                return problem.getTimeLimit();
            }
        }
        return 2.0f;
    }
}