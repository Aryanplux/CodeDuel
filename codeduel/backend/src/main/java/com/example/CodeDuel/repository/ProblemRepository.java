package com.example.CodeDuel.repository;

import com.example.CodeDuel.entity.Problem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProblemRepository extends JpaRepository<Problem, Long> {

    @Override
    @EntityGraph(attributePaths = "testCases")
    Optional<Problem> findById(Long id);
}
