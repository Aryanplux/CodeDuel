package com.example.CodeDuel.controller;

import com.example.CodeDuel.model.BattleSession;
import com.example.CodeDuel.repository.BattleSessionRepository;
import com.example.CodeDuel.service.EloService;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import com.example.CodeDuel.controller.AuthController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/battles")
@CrossOrigin(origins = "*")
public class BattleController {

    private final BattleSessionRepository battleSessionRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final EloService eloService;

    public BattleController(BattleSessionRepository battleSessionRepository,
                            SimpMessagingTemplate messagingTemplate,
                            EloService eloService) {
        this.battleSessionRepository = battleSessionRepository;
        this.messagingTemplate = messagingTemplate;
        this.eloService = eloService;
    }

    @GetMapping("/{battleId}")
    public ResponseEntity<BattleSession> getBattleDetails(@PathVariable String battleId) {
        return battleSessionRepository.findById(battleId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{battleId}/leave")
    public ResponseEntity<Map<String, String>> leaveBattle(
            @PathVariable String battleId,
            @RequestBody(required = false) Map<String, String> request,
            HttpSession httpSession) {
        String userId = request == null ? null : request.get("userId");
        String authenticatedId = (String) httpSession.getAttribute(AuthController.SESSION_USER_ID);
        if (authenticatedId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        if (!authenticatedId.equals(userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "User identity mismatch"));
        }
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "userId is required"));
        }

        BattleSession session = battleSessionRepository.findById(battleId).orElse(null);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }
        if (!userId.equals(session.getPlayer1Id()) && !userId.equals(session.getPlayer2Id())) {
            return ResponseEntity.status(403).body(Map.of("error", "User is not a player in this battle"));
        }
        if (!"IN_PROGRESS".equals(session.getStatus())) {
            return ResponseEntity.ok(Map.of("status", session.getStatus()));
        }

        String winnerId = userId.equals(session.getPlayer1Id())
                ? session.getPlayer2Id() : session.getPlayer1Id();
        session.setWinnerId(winnerId);
        session.setStatus("COMPLETED");
        session.setEndTime(LocalDateTime.now());
        session.setSubmissionDeadline(null);
        battleSessionRepository.save(session);
        eloService.applyResult(winnerId, true, eloService.getElo(userId));
        eloService.applyResult(userId, false, eloService.getElo(winnerId));

        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", "BATTLE_FINISHED");
        event.put("winnerId", winnerId);
        event.put("reasons", Map.of(
                winnerId, "Opponent left the battle",
                userId, "You left the battle"
        ));
        messagingTemplate.convertAndSend("/topic/battle/" + battleId, (Object) event);
        return ResponseEntity.ok(Map.of("status", "COMPLETED", "winnerId", winnerId));
    }
}