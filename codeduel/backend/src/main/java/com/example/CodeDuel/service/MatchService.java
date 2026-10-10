package com.example.CodeDuel.service;

import com.example.CodeDuel.model.BattleSession;
import com.example.CodeDuel.model.Problem;
import com.example.CodeDuel.repository.BattleSessionRepository;
import com.example.CodeDuel.repository.ProblemRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class MatchService {

    private static final String MATCH_PREFIX = "match:";
    private final StringRedisTemplate redisTemplate;
    private final ProblemRepository problemRepository;
    private final BattleSessionRepository battleSessionRepository;

    public MatchService(StringRedisTemplate redisTemplate, ProblemRepository problemRepository, BattleSessionRepository battleSessionRepository) {
        this.redisTemplate = redisTemplate;
        this.problemRepository = problemRepository;
        this.battleSessionRepository = battleSessionRepository;
    }

    @Transactional
    public Map<String, String> createMatch(String playerA, String playerB, double ratingA, double ratingB,
                                            String nameA, String nameB) {
        String matchId = UUID.randomUUID().toString();
        String key = MATCH_PREFIX + matchId;
        String startTime = Instant.now().toString();
        String endsAt = Instant.now().plusSeconds(600).toString();
        
        Problem problem = problemRepository.findRandomProblem().orElseThrow(() -> new IllegalStateException("No problems available in DB"));
        
        BattleSession session = BattleSession.builder()
                .battleId(matchId)
                .player1Id(playerA)
                .player2Id(playerB)
                .player1Name(nameA)
                .player2Name(nameB)
                .problem(problem)
                .startTime(LocalDateTime.now())
                .status("IN_PROGRESS")
                .build();
        
        battleSessionRepository.save(session);

        Map<String, String> match = new HashMap<>();
        match.put("matchId", matchId);
        match.put("player1Id", playerA);
        match.put("player2Id", playerB);
        match.put("player1Name", nameA);
        match.put("player2Name", nameB);
        match.put("player1Elo", String.valueOf(ratingA));
        match.put("player2Elo", String.valueOf(ratingB));
        match.put("problemId", String.valueOf(problem.getId()));
        match.put("startTime", startTime);
        match.put("serverTime", startTime);
        match.put("endsAt", endsAt);
        match.put("status", "STARTED");
        try {
            redisTemplate.opsForHash().putAll(key, match);
            redisTemplate.expire(key, java.time.Duration.ofHours(2));
            return match;
        } catch (DataAccessException exception) {
            throw new MatchmakingException("Unable to persist match in Redis", exception);
        }
    }

    public Map<Object, Object> getMatch(String matchId) {
        return redisTemplate.opsForHash().entries(MATCH_PREFIX + matchId);
    }
}
