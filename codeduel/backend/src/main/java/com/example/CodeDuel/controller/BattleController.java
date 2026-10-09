package com.example.CodeDuel.controller;

import com.example.CodeDuel.model.BattleSession;
import com.example.CodeDuel.repository.BattleSessionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/battles")
@CrossOrigin(origins = "*")
public class BattleController {

    private final BattleSessionRepository battleSessionRepository;

    public BattleController(BattleSessionRepository battleSessionRepository) {
        this.battleSessionRepository = battleSessionRepository;
    }

    @GetMapping("/{battleId}")
    public ResponseEntity<BattleSession> getBattleDetails(@PathVariable String battleId) {
        return battleSessionRepository.findById(battleId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}