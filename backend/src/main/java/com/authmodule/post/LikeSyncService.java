package com.authmodule.post;

import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
public class LikeSyncService {

    private static final Logger log = LoggerFactory.getLogger(LikeSyncService.class);

    private final RedisLikeRepository redisLikeRepository;
    private final PostLikeRepository postLikeRepository;
    private final EntityManager em;
    private final TransactionTemplate transactionTemplate;
    private final int batchSize;

    public LikeSyncService(
            RedisLikeRepository redisLikeRepository,
            PostLikeRepository postLikeRepository,
            EntityManager em,
            TransactionTemplate transactionTemplate,
            @Value("${app.likes.sync-batch-size:50}") int batchSize
    ) {
        this.redisLikeRepository = redisLikeRepository;
        this.postLikeRepository = postLikeRepository;
        this.em = em;
        this.transactionTemplate = transactionTemplate;
        this.batchSize = Math.max(1, batchSize);
    }

    public int syncDirtyLikes() {
        int synced = 0;
        for (UUID postId : redisLikeRepository.peekDirtyPosts(batchSize)) {
            try {
                Boolean ok = transactionTemplate.execute(status -> reconcilePost(postId));
                if (Boolean.TRUE.equals(ok)) {
                    synced++;
                }
            } catch (RuntimeException ex) {
                log.warn("Failed to sync likes for post {}; will retry", postId, ex);
            }
        }
        return synced;
    }

    private boolean reconcilePost(UUID postId) {
        if (!redisLikeRepository.isHydrated(postId)) {
            redisLikeRepository.clearDirty(postId);
            return false;
        }
        Set<UUID> snapshot = new HashSet<>(redisLikeRepository.members(postId));
        Instant now = Instant.now();
        for (UUID userId : snapshot) {
            postLikeRepository.upsertLike(postId, userId, now);
        }
        postLikeRepository.deleteLikesNotIn(postId, snapshot);
        em.flush();

        redisLikeRepository.clearDirty(postId);
        Set<UUID> after = redisLikeRepository.members(postId);
        if (!after.equals(snapshot)) {
            redisLikeRepository.markDirty(postId);
        }
        return true;
    }
}
