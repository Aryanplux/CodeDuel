package com.example.CodeDuel.service;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class MatchService {

    private static final String MATCH_PREFIX = "match:";
    private final StringRedisTemplate redisTemplate;

    public MatchService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Map<String, String> createMatch(String playerA, String playerB, double ratingA, double ratingB,
                                            String nameA, String nameB) {
        String matchId = UUID.randomUUID().toString();
        String key = MATCH_PREFIX + matchId;
        String startTime = Instant.now().toString();
        String endsAt = Instant.now().plusSeconds(600).toString();
        Map<String, String> match = new HashMap<>();
        match.put("matchId", matchId);
        match.put("player1Id", playerA);
        match.put("player2Id", playerB);
        match.put("player1Name", nameA);
        match.put("player2Name", nameB);
        match.put("player1Elo", String.valueOf(ratingA));
        match.put("player2Elo", String.valueOf(ratingB));
        match.put("problemId", "1");
        match.put("startTime", startTime);
        match.put("serverTime", startTime);
        match.put("endsAt", endsAt);
        match.put("status", "STARTED");
        try {
            redisTemplate.opsForHash().putAll(key, match);
            redisTemplate.expire(key, java.time.Duration.ofHours(2));
            return match;
        } catch (DataAccessException exception) {
            throw new MatchmakingException("Unable to persist match", exception);
        }
    }

    public Map<Object, Object> getMatch(String matchId) {
        return redisTemplate.opsForHash().entries(MATCH_PREFIX + matchId);
    }
}
