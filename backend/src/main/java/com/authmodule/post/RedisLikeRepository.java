package com.authmodule.post;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Redis-first like store.
 *
 * <pre>
 * like:set:{postId}      SET of userIds (hot like membership)
 * like:hydrated:{postId} marker that SET was loaded from PostgreSQL
 * like:dirty             SET of postIds awaiting PostgreSQL sync
 * </pre>
 */
@Repository
public class RedisLikeRepository {

    private static final Logger log = LoggerFactory.getLogger(RedisLikeRepository.class);

    private static final String LIKE_SCRIPT = """
            if redis.call('EXISTS', KEYS[3]) == 0 then
              return -1
            end
            local changed = redis.call('SADD', KEYS[1], ARGV[1])
            if changed == 1 then
              redis.call('SADD', KEYS[2], ARGV[2])
            end
            return changed
            """;

    private static final String UNLIKE_SCRIPT = """
            if redis.call('EXISTS', KEYS[3]) == 0 then
              return -1
            end
            local changed = redis.call('SREM', KEYS[1], ARGV[1])
            if changed == 1 then
              redis.call('SADD', KEYS[2], ARGV[2])
            end
            return changed
            """;

    private final StringRedisTemplate redis;
    private final DefaultRedisScript<Long> likeScript;
    private final DefaultRedisScript<Long> unlikeScript;

    public RedisLikeRepository(StringRedisTemplate redis) {
        this.redis = redis;
        this.likeScript = new DefaultRedisScript<>(LIKE_SCRIPT, Long.class);
        this.unlikeScript = new DefaultRedisScript<>(UNLIKE_SCRIPT, Long.class);
    }

    public boolean like(UUID postId, UUID userId, Supplier<Set<UUID>> hydrateFromDb) {
        ensureHydrated(postId, hydrateFromDb);
        Long result = executeScript(likeScript, postId, userId);
        if (result != null && result == -1L) {
            ensureHydrated(postId, hydrateFromDb);
            result = executeScript(likeScript, postId, userId);
        }
        return result != null && result == 1L;
    }

    public boolean unlike(UUID postId, UUID userId, Supplier<Set<UUID>> hydrateFromDb) {
        ensureHydrated(postId, hydrateFromDb);
        Long result = executeScript(unlikeScript, postId, userId);
        if (result != null && result == -1L) {
            ensureHydrated(postId, hydrateFromDb);
            result = executeScript(unlikeScript, postId, userId);
        }
        return result != null && result == 1L;
    }

    public boolean isLikedBy(UUID postId, UUID userId, Supplier<Set<UUID>> hydrateFromDb) {
        ensureHydrated(postId, hydrateFromDb);
        return Boolean.TRUE.equals(withRedis(() -> redis.opsForSet().isMember(setKey(postId), userId.toString())));
    }

    public long count(UUID postId, Supplier<Set<UUID>> hydrateFromDb) {
        ensureHydrated(postId, hydrateFromDb);
        Long size = withRedis(() -> redis.opsForSet().size(setKey(postId)));
        return size == null ? 0L : size;
    }

    public Set<UUID> members(UUID postId) {
        Set<String> raw = withRedis(() -> redis.opsForSet().members(setKey(postId)));
        if (raw == null || raw.isEmpty()) {
            return Set.of();
        }
        Set<UUID> ids = new HashSet<>();
        for (String value : raw) {
            ids.add(UUID.fromString(value));
        }
        return ids;
    }

    public boolean isHydrated(UUID postId) {
        return Boolean.TRUE.equals(withRedis(() -> redis.hasKey(hydratedKey(postId))));
    }

    public void ensureHydrated(UUID postId, Supplier<Set<UUID>> hydrateFromDb) {
        if (isHydrated(postId)) {
            return;
        }
        synchronized (("like-hydrate-" + postId).intern()) {
            if (isHydrated(postId)) {
                return;
            }
            Set<UUID> fromDb = hydrateFromDb.get();
            withRedis(() -> {
                String set = setKey(postId);
                redis.delete(set);
                if (fromDb != null && !fromDb.isEmpty()) {
                    String[] values = fromDb.stream().map(UUID::toString).toArray(String[]::new);
                    redis.opsForSet().add(set, values);
                }
                redis.opsForValue().set(hydratedKey(postId), "1");
                return null;
            });
        }
    }

    public List<UUID> peekDirtyPosts(int batchSize) {
        Set<String> raw = withRedis(() -> redis.opsForSet().members(dirtyKey()));
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = new ArrayList<>();
        for (String value : raw) {
            ids.add(UUID.fromString(value));
            if (ids.size() >= batchSize) {
                break;
            }
        }
        return ids;
    }

    public void clearDirty(UUID postId) {
        withRedis(() -> redis.opsForSet().remove(dirtyKey(), postId.toString()));
    }

    public void markDirty(UUID postId) {
        withRedis(() -> redis.opsForSet().add(dirtyKey(), postId.toString()));
    }

    public void clearLikeCache(UUID postId) {
        withRedis(() -> {
            redis.delete(List.of(setKey(postId), hydratedKey(postId)));
            return null;
        });
    }

    private Long executeScript(DefaultRedisScript<Long> script, UUID postId, UUID userId) {
        return withRedis(() -> redis.execute(
                script,
                List.of(setKey(postId), dirtyKey(), hydratedKey(postId)),
                userId.toString(),
                postId.toString()
        ));
    }

    private <T> T withRedis(Supplier<T> action) {
        try {
            return action.get();
        } catch (DataAccessException | IllegalStateException ex) {
            log.error("Redis like operation failed", ex);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Likes temporarily unavailable");
        }
    }

    private static String setKey(UUID postId) {
        return "like:set:" + postId;
    }

    private static String hydratedKey(UUID postId) {
        return "like:hydrated:" + postId;
    }

    private static String dirtyKey() {
        return "like:dirty";
    }
}
