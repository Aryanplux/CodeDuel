package com.example.CodeDuel.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "battle_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BattleSession {

    @Id
    private String battleId; // e.g. "battle_12345"

    private String player1Id;
    private String player2Id;

    @ManyToOne
    @JoinColumn(name = "problem_id")
    private Problem problem;

    private LocalDateTime startTime;
    private String status; // "IN_PROGRESS", "COMPLETED"
}