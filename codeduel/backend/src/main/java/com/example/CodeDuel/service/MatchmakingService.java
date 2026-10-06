package com.example.CodeDuel.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class MatchmakingService {

    private static final Logger log = LoggerFactory.getLogger(MatchmakingService.class);
    private static final String QUEUE_KEY = "waiting_players";
    private static final String TIME_KEY = "waiting_players_time";
    public static final String PLAYER_KEY = "waiting_players_profile";

    private final StringRedisTemplate redisTemplate;

    public MatchmakingService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Adds a user to the matchmaking queue[cite: 24, 27].
     */
    public void joinQueue(String userId, double userRating, String username) {
        try {
            redisTemplate.opsForZSet().add(QUEUE_KEY, userId, userRating);
            redisTemplate.opsForHash().put(TIME_KEY, userId, String.valueOf(System.currentTimeMillis()));
            redisTemplate.opsForHash().put(PLAYER_KEY, userId, username);
        } catch (DataAccessException exception) {
            log.error("Unable to join matchmaking queue for user {}: Redis operation failed ({})",
                    userId, exception.getMostSpecificCause().getMessage(), exception);
            throw new MatchmakingException("Redis queue unavailable", exception);
        }
    }

    /**
     * Removes a user if they cancel search[cite: 16, 27].
     */
    public void leaveQueue(String userId) {
        try {
            redisTemplate.opsForZSet().remove(QUEUE_KEY, userId);
            redisTemplate.opsForHash().delete(TIME_KEY, userId);
            redisTemplate.opsForHash().delete(PLAYER_KEY, userId);
        } catch (DataAccessException exception) {
            log.error("Unable to remove user {} from matchmaking queue: Redis operation failed ({})",
                    userId, exception.getMostSpecificCause().getMessage(), exception);
            throw new MatchmakingException("Redis queue unavailable", exception);
        }
    }
}