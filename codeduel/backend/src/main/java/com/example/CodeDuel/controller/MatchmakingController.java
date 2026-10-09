package com.example.CodeDuel.controller;

import com.example.CodeDuel.dto.QueueJoinRequest;
import com.example.CodeDuel.service.MatchmakingException;
import com.example.CodeDuel.service.MatchmakingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/matchmaking")
@CrossOrigin(origins = "*") // Allows cross-origin requests from React dev server (port 5173)
public class MatchmakingController {

    private final MatchmakingService matchmakingService;

    public MatchmakingController(MatchmakingService matchmakingService) {
        this.matchmakingService = matchmakingService;
    }

    @PostMapping("/join")
    public ResponseEntity<String> joinQueue(@RequestBody QueueJoinRequest request) {
        if (request == null || request.userId() == null || request.userId().isBlank()
                || request.rating() == null || !Double.isFinite(request.rating()) || request.rating() < 0) {
            return ResponseEntity.badRequest().body("userId and a non-negative finite rating are required");
        }

        matchmakingService.joinQueue(
            request.userId().trim(),
            request.rating(),
            request.username() == null || request.username().isBlank()
                    ? request.userId().trim() : request.username().trim()
        );
        return ResponseEntity.ok("Added to queue");
    }

    @PostMapping({"/cancel", "/leave"})
    public ResponseEntity<String> cancelQueue(@RequestBody QueueJoinRequest request) {
        if (request == null || request.userId() == null || request.userId().isBlank()) {
            return ResponseEntity.badRequest().body("userId is required");
        }

        matchmakingService.leaveQueue(request.userId().trim());
        return ResponseEntity.ok("Removed from queue");
    }

    @ExceptionHandler(MatchmakingException.class)
    public ResponseEntity<String> handleMatchmakingFailure(MatchmakingException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("Matchmaking is temporarily unavailable. Please retry.");
    }
}