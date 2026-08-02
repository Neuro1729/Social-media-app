package com.authmodule.social;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class SocialRepository {

    private final EntityManager em;

    public SocialRepository(EntityManager em) {
        this.em = em;
    }

    public Profile createProfile(UUID userId) {
        Profile profile = Profile.empty(userId);
        em.persist(profile);
        return profile;
    }

    public Optional<Profile> findProfileByUserId(UUID userId) {
        return Optional.ofNullable(em.find(Profile.class, userId));
    }

    public Optional<Profile> findProfileByUsername(String normalizedUsername) {
        return findUserIdByNormalizedUsername(normalizedUsername)
                .flatMap(this::findProfileByUserId);
    }

    public Profile updateProfile(Profile profile) {
        return em.merge(profile);
    }

    public Profile updatePrivacy(Profile profile, boolean isPrivate) {
        profile.setPrivate(isPrivate);
        return em.merge(profile);
    }

    public Optional<Follow> findRelationship(UUID followerId, UUID followingId) {
        return Optional.ofNullable(em.find(Follow.class, new Follow.FollowId(followerId, followingId)));
    }

    public Follow saveRelationship(Follow follow) {
        em.persist(follow);
        return follow;
    }

    public Follow updateRelationshipStatus(Follow follow, Follow.Status status) {
        follow.setStatus(status);
        return em.merge(follow);
    }

    public void deleteRelationship(UUID followerId, UUID followingId) {
        Follow follow = em.find(Follow.class, new Follow.FollowId(followerId, followingId));
        if (follow != null) {
            em.remove(follow);
        }
    }

    public void deleteRelationshipsBothDirections(UUID userA, UUID userB) {
        deleteRelationship(userA, userB);
        deleteRelationship(userB, userA);
    }

    public long countFollowers(UUID userId) {
        return em.createQuery(
                        """
                                SELECT COUNT(f) FROM Follow f
                                WHERE f.followingId = :userId AND f.status = :status
                                """,
                        Long.class
                )
                .setParameter("userId", userId)
                .setParameter("status", Follow.Status.FOLLOWING)
                .getSingleResult();
    }

    public long countFollowing(UUID userId) {
        return em.createQuery(
                        """
                                SELECT COUNT(f) FROM Follow f
                                WHERE f.followerId = :userId AND f.status = :status
                                """,
                        Long.class
                )
                .setParameter("userId", userId)
                .setParameter("status", Follow.Status.FOLLOWING)
                .getSingleResult();
    }

    public void convertIncomingPendingToFollowing(UUID userId) {
        em.createQuery(
                        """
                                UPDATE Follow f
                                SET f.status = :following
                                WHERE f.followingId = :userId AND f.status = :pending
                                """
                )
                .setParameter("following", Follow.Status.FOLLOWING)
                .setParameter("userId", userId)
                .setParameter("pending", Follow.Status.PENDING)
                .executeUpdate();
    }

    public List<SocialModels.ConnectionRow> findFollowers(UUID ownerId, String cursor, int size) {
        CursorDecoded decoded = decodeCursor(cursor);
        String sql = """
                SELECT f.follower_id, li.value, p.profile_picture_url, p.is_private, f.created_at
                FROM follows f
                JOIN profiles p ON p.user_id = f.follower_id
                LEFT JOIN login_identifiers li
                    ON li.user_id = f.follower_id
                   AND li.type = 'USERNAME'
                   AND li.active = TRUE
                WHERE f.following_id = :ownerId
                  AND f.status = 'FOLLOWING'
                  AND (
                       CAST(:hasCursor AS boolean) = FALSE
                       OR f.created_at < CAST(:cursorAt AS timestamptz)
                       OR (f.created_at = CAST(:cursorAt AS timestamptz) AND f.follower_id < CAST(:cursorId AS uuid))
                  )
                ORDER BY f.created_at DESC, f.follower_id DESC
                LIMIT :limit
                """;
        return mapConnectionRows(nativeList(sql, ownerId, decoded, size));
    }

    public List<SocialModels.ConnectionRow> findFollowing(UUID ownerId, String cursor, int size) {
        CursorDecoded decoded = decodeCursor(cursor);
        String sql = """
                SELECT f.following_id, li.value, p.profile_picture_url, p.is_private, f.created_at
                FROM follows f
                JOIN profiles p ON p.user_id = f.following_id
                LEFT JOIN login_identifiers li
                    ON li.user_id = f.following_id
                   AND li.type = 'USERNAME'
                   AND li.active = TRUE
                WHERE f.follower_id = :ownerId
                  AND f.status = 'FOLLOWING'
                  AND (
                       CAST(:hasCursor AS boolean) = FALSE
                       OR f.created_at < CAST(:cursorAt AS timestamptz)
                       OR (f.created_at = CAST(:cursorAt AS timestamptz) AND f.following_id < CAST(:cursorId AS uuid))
                  )
                ORDER BY f.created_at DESC, f.following_id DESC
                LIMIT :limit
                """;
        return mapConnectionRows(nativeList(sql, ownerId, decoded, size));
    }

    public List<SocialModels.ConnectionRow> findPendingRequests(UUID ownerId, String cursor, int size) {
        CursorDecoded decoded = decodeCursor(cursor);
        String sql = """
                SELECT f.follower_id, li.value, p.profile_picture_url, p.is_private, f.created_at
                FROM follows f
                JOIN profiles p ON p.user_id = f.follower_id
                LEFT JOIN login_identifiers li
                    ON li.user_id = f.follower_id
                   AND li.type = 'USERNAME'
                   AND li.active = TRUE
                WHERE f.following_id = :ownerId
                  AND f.status = 'PENDING'
                  AND (
                       CAST(:hasCursor AS boolean) = FALSE
                       OR f.created_at < CAST(:cursorAt AS timestamptz)
                       OR (f.created_at = CAST(:cursorAt AS timestamptz) AND f.follower_id < CAST(:cursorId AS uuid))
                  )
                ORDER BY f.created_at DESC, f.follower_id DESC
                LIMIT :limit
                """;
        return mapConnectionRows(nativeList(sql, ownerId, decoded, size));
    }

    public Block createBlock(UUID blockerId, UUID blockedId) {
        Block existing = em.find(Block.class, new Block.BlockId(blockerId, blockedId));
        if (existing != null) {
            return existing;
        }
        Block block = new Block(blockerId, blockedId, Instant.now());
        em.persist(block);
        return block;
    }

    public void deleteBlock(UUID blockerId, UUID blockedId) {
        Block block = em.find(Block.class, new Block.BlockId(blockerId, blockedId));
        if (block != null) {
            em.remove(block);
        }
    }

    public List<SocialModels.ConnectionRow> findBlockedUsers(UUID blockerId, String cursor, int size) {
        CursorDecoded decoded = decodeCursor(cursor);
        String sql = """
                SELECT b.blocked_id, li.value, p.profile_picture_url, p.is_private, b.created_at
                FROM blocks b
                JOIN profiles p ON p.user_id = b.blocked_id
                LEFT JOIN login_identifiers li
                    ON li.user_id = b.blocked_id
                   AND li.type = 'USERNAME'
                   AND li.active = TRUE
                WHERE b.blocker_id = :ownerId
                  AND (
                       CAST(:hasCursor AS boolean) = FALSE
                       OR b.created_at < CAST(:cursorAt AS timestamptz)
                       OR (b.created_at = CAST(:cursorAt AS timestamptz) AND b.blocked_id < CAST(:cursorId AS uuid))
                  )
                ORDER BY b.created_at DESC, b.blocked_id DESC
                LIMIT :limit
                """;
        return mapConnectionRows(nativeList(sql, blockerId, decoded, size));
    }

    public boolean isBlockedEitherDirection(UUID userA, UUID userB) {
        if (userA == null || userB == null) {
            return false;
        }
        Long count = em.createQuery(
                        """
                                SELECT COUNT(b) FROM Block b
                                WHERE (b.blockerId = :a AND b.blockedId = :b)
                                   OR (b.blockerId = :b AND b.blockedId = :a)
                                """,
                        Long.class
                )
                .setParameter("a", userA)
                .setParameter("b", userB)
                .getSingleResult();
        return count != null && count > 0;
    }

    public Optional<UUID> findUserIdByNormalizedUsername(String normalizedUsername) {
        try {
            Object result = em.createNativeQuery(
                            """
                                    SELECT user_id
                                    FROM login_identifiers
                                    WHERE type = 'USERNAME'
                                      AND normalized_value = :normalized
                                      AND active = TRUE
                                    """
                    )
                    .setParameter("normalized", normalizedUsername)
                    .getSingleResult();
            return Optional.of(toUuid(result));
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public Optional<String> findActiveUsername(UUID userId) {
        try {
            Object result = em.createNativeQuery(
                            """
                                    SELECT value
                                    FROM login_identifiers
                                    WHERE user_id = :userId
                                      AND type = 'USERNAME'
                                      AND active = TRUE
                                    """
                    )
                    .setParameter("userId", userId)
                    .getSingleResult();
            return Optional.ofNullable(result).map(Object::toString);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public boolean userExists(UUID userId) {
        Long count = ((Number) em.createNativeQuery(
                        "SELECT COUNT(*) FROM users WHERE id = :id"
                )
                .setParameter("id", userId)
                .getSingleResult()).longValue();
        return count > 0;
    }

    public String encodeCursor(Instant createdAt, UUID userId) {
        String raw = createdAt.toString() + "|" + userId;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private List<?> nativeList(String sql, UUID ownerId, CursorDecoded decoded, int size) {
        return em.createNativeQuery(sql)
                .setParameter("ownerId", ownerId)
                .setParameter("hasCursor", decoded.hasCursor())
                .setParameter("cursorAt", decoded.createdAt() == null ? Instant.EPOCH : decoded.createdAt())
                .setParameter("cursorId", decoded.userId() == null
                        ? UUID.fromString("00000000-0000-0000-0000-000000000000")
                        : decoded.userId())
                .setParameter("limit", size)
                .getResultList();
    }

    private List<SocialModels.ConnectionRow> mapConnectionRows(List<?> rows) {
        List<SocialModels.ConnectionRow> result = new ArrayList<>();
        for (Object row : rows) {
            Object[] cols = (Object[]) row;
            result.add(new SocialModels.ConnectionRow(
                    toUuid(cols[0]),
                    cols[1] == null ? null : cols[1].toString(),
                    cols[2] == null ? null : cols[2].toString(),
                    Boolean.TRUE.equals(cols[3]) || (cols[3] instanceof Boolean b && b),
                    toInstant(cols[4])
            ));
        }
        return result;
    }

    private CursorDecoded decodeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return new CursorDecoded(false, null, null);
        }
        try {
            String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] parts = raw.split("\\|", 2);
            return new CursorDecoded(true, Instant.parse(parts[0]), UUID.fromString(parts[1]));
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Invalid cursor");
        }
    }

    private static UUID toUuid(Object value) {
        if (value instanceof UUID uuid) {
            return uuid;
        }
        return UUID.fromString(value.toString());
    }

    private static Instant toInstant(Object value) {
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toInstant();
        }
        if (value instanceof java.time.OffsetDateTime odt) {
            return odt.toInstant();
        }
        return Instant.parse(value.toString());
    }

    private record CursorDecoded(boolean hasCursor, Instant createdAt, UUID userId) {
    }
}
