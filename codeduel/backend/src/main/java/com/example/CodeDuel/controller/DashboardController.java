package com.example.CodeDuel.controller;

import com.example.CodeDuel.entity.UserAccount;
import com.example.CodeDuel.model.BattleSession;
import com.example.CodeDuel.repository.BattleSessionRepository;
import com.example.CodeDuel.repository.UserAccountRepository;
import com.example.CodeDuel.service.EloService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {
    private final UserAccountRepository users;
    private final BattleSessionRepository battles;

    public DashboardController(UserAccountRepository users, BattleSessionRepository battles) {
        this.users = users;
        this.battles = battles;
    }

    @GetMapping
    public ResponseEntity<?> dashboard(HttpSession session) {
        String userId = (String) session.getAttribute(AuthController.SESSION_USER_ID);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));

        UserAccount user = users.findById(userId).orElse(null);
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Session expired"));

        int matches = user.getMatchesPlayed();
        double winRate = matches == 0 ? 0 : (user.getWins() * 100.0) / matches;
        List<Map<String, Object>> history = battles.findRecentCompletedByPlayer(userId).stream()
                .limit(20)
                .map(battle -> historyItem(battle, user))
                .toList();

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", user.getId());
        profile.put("username", user.getUsername());
        profile.put("elo", user.getElo());
        profile.put("rank", EloService.getRank(user.getElo()));
        profile.put("matchesPlayed", matches);
        profile.put("wins", user.getWins());
        profile.put("losses", user.getLosses());
        profile.put("winRate", Math.round(winRate * 10.0) / 10.0);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("profile", profile);
        response.put("recentMatches", history);
        return ResponseEntity.ok(response);
    }

    private Map<String, Object> historyItem(BattleSession battle, UserAccount user) {
        boolean playerOne = user.getId().equals(battle.getPlayer1Id());
        String opponentName = playerOne ? battle.getPlayer2Name() : battle.getPlayer1Name();
        String opponentId = playerOne ? battle.getPlayer2Id() : battle.getPlayer1Id();
        String result = battle.getWinnerId() == null ? "DRAW"
                : battle.getWinnerId().equals(user.getId()) ? "WIN" : "LOSS";

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("battleId", battle.getBattleId());
        item.put("opponent", opponentName == null ? opponentId : opponentName);
        item.put("result", result);
        item.put("problem", battle.getProblem() == null ? "Competitive programming duel" : battle.getProblem().getTitle());
        item.put("date", battle.getEndTime() == null ? battle.getStartTime() : battle.getEndTime());
        return item;
    }
}
