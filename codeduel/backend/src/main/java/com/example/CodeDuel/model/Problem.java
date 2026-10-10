package com.example.CodeDuel.model;

import java.util.ArrayList;
import java.util.List;

import com.example.CodeDuel.entity.TestCase;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "problems")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Problem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String difficulty; // "EASY", "MEDIUM", "HARD"

    @Column(columnDefinition = "TEXT")
    private String description; // Markdown formatted description

    @Column(columnDefinition = "TEXT")
    private String starterCodePython;

    @Column(columnDefinition = "TEXT")
    private String starterCodeJava;

    @Column(columnDefinition = "TEXT")
    private String starterCodeCpp;

    @Column(columnDefinition = "TEXT")
    private String sampleInput;

    @Column(columnDefinition = "TEXT")
    private String sampleOutput;

    // Hidden test cases stored as JSON arrays for Judge0 execution
    @Column(columnDefinition = "TEXT")
    private String hiddenInputsJson;

    @Column(columnDefinition = "TEXT")
    private String hiddenOutputsJson;

    // Execution limits per the documentation spec
    @Builder.Default
    private Float timeLimit = 2.0f; // seconds

    @Builder.Default
    private Integer memoryLimit = 256000; // KB (256 MB)

    // Bidirectional relationship with TestCase entity
    @OneToMany(mappedBy = "problem", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    @Builder.Default
    private List<TestCase> testCases = new ArrayList<>();
}