package com.example.CodeDuel.repository;

import com.example.CodeDuel.model.BattleSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BattleSessionRepository extends JpaRepository<BattleSession, String> {
}