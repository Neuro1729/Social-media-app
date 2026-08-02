package com.authmodule.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public class RedisSessionRepository {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final long refreshTokenDays;

    public RedisSessionRepository(
            StringRedisTemplate redis,
            ObjectMapper objectMapper,
            @Value("${app.refresh-token.days}") long refreshTokenDays
    ) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.refreshTokenDays = refreshTokenDays;
    }

    public AuthModels.SessionData createSession(
            UUID userId,
            String refreshTokenHash,
            String deviceName
    ) {
        String sessionId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        Instant expiresAt = now.plus(Duration.ofDays(refreshTokenDays));
        AuthModels.SessionData session = new AuthModels.SessionData(
                sessionId,
                userId,
                refreshTokenHash,
                deviceName == null || deviceName.isBlank() ? "Unknown device" : deviceName,
                now,
                expiresAt
        );
        Duration ttl = Duration.between(now, expiresAt);
        redis.opsForValue().set(sessionKey(sessionId), toJson(session), ttl);
        redis.opsForSet().add(userSessionsKey(userId), sessionId);
        redis.expire(userSessionsKey(userId), ttl);
        redis.opsForValue().set(refreshKey(refreshTokenHash), sessionId, ttl);
        return session;
    }

    public Optional<AuthModels.SessionData> findSession(String sessionId) {
        String json = redis.opsForValue().get(sessionKey(sessionId));
        if (json == null) {
            return Optional.empty();
        }
        return Optional.of(fromJson(json));
    }

    public Optional<String> findSessionIdByRefreshHash(String refreshTokenHash) {
        return Optional.ofNullable(redis.opsForValue().get(refreshKey(refreshTokenHash)));
    }

    public AuthModels.SessionData rotateRefreshToken(String sessionId, String newRefreshTokenHash) {
        AuthModels.SessionData existing = findSession(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));
        redis.delete(refreshKey(existing.refreshTokenHash()));
        Instant now = Instant.now();
        Instant expiresAt = now.plus(Duration.ofDays(refreshTokenDays));
        AuthModels.SessionData updated = new AuthModels.SessionData(
                existing.sessionId(),
                existing.userId(),
                newRefreshTokenHash,
                existing.deviceName(),
                existing.createdAt(),
                expiresAt
        );
        Duration ttl = Duration.between(now, expiresAt);
        redis.opsForValue().set(sessionKey(sessionId), toJson(updated), ttl);
        redis.expire(userSessionsKey(existing.userId()), ttl);
        redis.opsForValue().set(refreshKey(newRefreshTokenHash), sessionId, ttl);
        return updated;
    }

    public void deleteSession(String sessionId) {
        findSession(sessionId).ifPresent(session -> {
            redis.delete(sessionKey(sessionId));
            redis.delete(refreshKey(session.refreshTokenHash()));
            redis.opsForSet().remove(userSessionsKey(session.userId()), sessionId);
        });
    }

    public void deleteAllSessions(UUID userId) {
        Set<String> sessionIds = redis.opsForSet().members(userSessionsKey(userId));
        if (sessionIds != null) {
            for (String sessionId : sessionIds) {
                deleteSession(sessionId);
            }
        }
        redis.delete(userSessionsKey(userId));
    }

    public List<AuthModels.SessionData> getUserSessions(UUID userId) {
        Set<String> sessionIds = redis.opsForSet().members(userSessionsKey(userId));
        List<AuthModels.SessionData> sessions = new ArrayList<>();
        if (sessionIds == null) {
            return sessions;
        }
        for (String sessionId : sessionIds) {
            findSession(sessionId).ifPresent(sessions::add);
        }
        sessions.sort((a, b) -> b.createdAt().compareTo(a.createdAt()));
        return sessions;
    }

    private static String sessionKey(String sessionId) {
        return "session:" + sessionId;
    }

    private static String userSessionsKey(UUID userId) {
        return "user_sessions:" + userId;
    }

    private static String refreshKey(String refreshTokenHash) {
        return "refresh:" + refreshTokenHash;
    }

    private String toJson(AuthModels.SessionData session) {
        try {
            return objectMapper.writeValueAsString(session);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize session", e);
        }
    }

    private AuthModels.SessionData fromJson(String json) {
        try {
            return objectMapper.readValue(json, AuthModels.SessionData.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize session", e);
        }
    }
}
