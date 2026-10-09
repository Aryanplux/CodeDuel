package com.example.CodeDuel.model;

import java.util.List;

import com.example.CodeDuel.entity.TestCase;

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

    // Hidden test cases sent to Judge0 (JSON format e.g. ["2 7 11 15\n9", "3 2 4\n6"])
    @Column(columnDefinition = "TEXT")
    private String hiddenInputsJson;

    @Column(columnDefinition = "TEXT")
    private String hiddenOutputsJson;

    public Object getTimeLimit() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getTimeLimit'");
    }

    public List<TestCase> getTestCases() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getTestCases'");
    }
}