package com.example.CodeDuel.repository;

import com.example.CodeDuel.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    java.util.List<Submission> findByBattleIdAndStatusOrderBySubmittedAtAsc(
            String battleId, String status);

    java.util.List<Submission> findByBattleIdOrderBySubmittedAtAsc(String battleId);

    java.util.List<Submission> findByBattleIdAndUserIdOrderBySubmittedAtDesc(
            String battleId, String userId);
}
