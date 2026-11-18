package com.example.CodeDuel.repository;

import com.example.CodeDuel.model.BattleSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface BattleSessionRepository extends JpaRepository<BattleSession, String> {
    java.util.List<BattleSession> findByStatus(String status);

    java.util.List<BattleSession> findByStatusAndSubmissionDeadlineBefore(
            String status, java.time.LocalDateTime deadline);

    @Query("""
            select b from BattleSession b
            where b.status = 'COMPLETED'
              and (b.player1Id = :userId or b.player2Id = :userId)
            order by b.endTime desc
            """)
    java.util.List<BattleSession> findRecentCompletedByPlayer(@Param("userId") String userId);
}