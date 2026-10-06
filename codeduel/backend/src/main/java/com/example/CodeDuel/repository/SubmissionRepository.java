package com.example.CodeDuel.repository;

import com.example.CodeDuel.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
}
