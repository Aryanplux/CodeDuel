package com.example.CodeDuel.controller;

import com.example.CodeDuel.service.MatchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/battles")
public class BattleController {

    private final MatchService matchService;

    public BattleController(MatchService matchService) {
        this.matchService = matchService;
    }

    @GetMapping("/{matchId}")
    public ResponseEntity<Map<Object, Object>> getBattle(@PathVariable String matchId) {
        Map<Object, Object> match = matchService.getMatch(matchId);
        return match.isEmpty() ? ResponseEntity.notFound().build() : ResponseEntity.ok(match);
    }
}
