package com.example.CodeDuel.entity;

import com.example.CodeDuel.model.Problem;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "test_cases")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String input;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String expectedOutput;

    @Builder.Default
    private Boolean isHidden = false;

    // Convenience constructor for quick testing / mocking
    public TestCase(Long id, String input, String expectedOutput, Boolean isHidden) {
        this.id = id;
        this.input = input;
        this.expectedOutput = expectedOutput;
        this.isHidden = isHidden;
    }
}