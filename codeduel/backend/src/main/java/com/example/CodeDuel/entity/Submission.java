package com.example.CodeDuel.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "submissions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userId;
    private String battleId;
    private Long problemId;

    private Integer languageId;

    @Column(columnDefinition = "TEXT")
    private String sourceCode;

    private String status; // QUEUED, ACCEPTED, WRONG_ANSWER, COMPILATION_ERROR[cite: 22]
    private Integer testsPassed;
    private Integer totalTests;

    private LocalDateTime submittedAt;
}