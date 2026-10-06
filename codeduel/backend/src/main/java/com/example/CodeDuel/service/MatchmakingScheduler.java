package com.example.CodeDuel.service;

import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@EnableScheduling
public class MatchmakingScheduler {

    private static final Logger log = LoggerFactory.getLogger(MatchmakingScheduler.class);
    private static final String QUEUE_KEY = "waiting_players";
    private static final String TIME_KEY = "waiting_players_time";

    private final StringRedisTemplate redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final MatchService matchService;

    public MatchmakingScheduler(StringRedisTemplate redisTemplate, SimpMessagingTemplate messagingTemplate,
                                MatchService matchService) {
        this.redisTemplate = redisTemplate;
        this.messagingTemplate = messagingTemplate;
        this.matchService = matchService;
    }

    @Scheduled(fixedDelay = 1000) // Runs every 1 second[cite: 27]
    public void matchPlayers() {
        try {
            matchPlayersFromQueue();
        } catch (DataAccessException exception) {
            log.error("Matchmaking poll failed: Redis operation failed ({})",
                    exception.getMostSpecificCause().getMessage(), exception);
        }
    }

    private void matchPlayersFromQueue() {
        // Fetch all players ordered by Elo rating from Redis ZSET[cite: 27]
        Set<TypedTuple<String>> range = redisTemplate.opsForZSet().rangeWithScores(QUEUE_KEY, 0, -1);
        if (range == null || range.size() < 2) return;

        List<TypedTuple<String>> players = new ArrayList<>(range);
        long now = System.currentTimeMillis();

        for (int i = 0; i < players.size(); i++) {
            TypedTuple<String> playerA = players.get(i);
            String userA = playerA.getValue();
            double ratingA = playerA.getScore();

            Long joinTimeA = getJoinTimestamp(userA);
            if (joinTimeA == null) continue;

            long waitSecondsA = (now - joinTimeA) / 1000;
            double windowA = 100 * ((waitSecondsA / 10) + 1); // Expanding window[cite: 27]

            for (int j = i + 1; j < players.size(); j++) {
                TypedTuple<String> playerB = players.get(j);
                String userB = playerB.getValue();
                double ratingB = playerB.getScore();

                double ratingDiff = Math.abs(ratingA - ratingB);

                // Check if players fall within acceptable window[cite: 27]
                if (userA != null && userB != null && !userA.equals(userB) && ratingDiff <= windowA) {
                    // Remove both from the queue before publishing the match.
                    redisTemplate.opsForZSet().remove(QUEUE_KEY, userA, userB);
                    redisTemplate.opsForHash().delete(TIME_KEY, userA, userB);
                    Object nameAValue = redisTemplate.opsForHash().get(MatchmakingService.PLAYER_KEY, userA);
                    Object nameBValue = redisTemplate.opsForHash().get(MatchmakingService.PLAYER_KEY, userB);
                    redisTemplate.opsForHash().delete(MatchmakingService.PLAYER_KEY, userA, userB);

                    Map<String, String> match = matchService.createMatch(userA, userB, ratingA, ratingB,
                            nameAValue == null ? userA : nameAValue.toString(),
                            nameBValue == null ? userB : nameBValue.toString());
                    log.info("[Matchmaking] Paired {} and {} for {}", userA, userB, match.get("matchId"));
                    messagingTemplate.convertAndSend("/topic/match/" + userA, notification(match, userA));
                    messagingTemplate.convertAndSend("/topic/match/" + userB, notification(match, userB));

                    return; // Break iteration to prevent double matching
                }
            }
        }
    }

    private Map<String, String> notification(Map<String, String> match, String playerId) {
        boolean firstPlayer = playerId.equals(match.get("player1Id"));
        String opponentName = firstPlayer ? match.get("player2Name") : match.get("player1Name");
        String opponentElo = firstPlayer ? match.get("player2Elo") : match.get("player1Elo");
        return Map.of("type", "MATCH_FOUND", "matchId", match.get("matchId"),
                "opponentName", opponentName, "opponentElo", opponentElo,
                "problemId", match.get("problemId"));
    }

    private Long getJoinTimestamp(String userId) {
        Object val = redisTemplate.opsForHash().get(TIME_KEY, userId);
        return val != null ? Long.parseLong(val.toString()) : null;
    }
}