package com.authmodule.post;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public class PostLikeRepository {

    private final EntityManager em;

    public PostLikeRepository(EntityManager em) {
        this.em = em;
    }

    public Set<UUID> findUserIdsByPostId(UUID postId) {
        List<UUID> ids = em.createQuery(
                        """
                                SELECT pl.userId FROM PostLike pl
                                WHERE pl.postId = :postId
                                """,
                        UUID.class
                )
                .setParameter("postId", postId)
                .getResultList();
        return new HashSet<>(ids);
    }

    public long countByPostId(UUID postId) {
        return em.createQuery(
                        """
                                SELECT COUNT(pl) FROM PostLike pl
                                WHERE pl.postId = :postId
                                """,
                        Long.class
                )
                .setParameter("postId", postId)
                .getSingleResult();
    }

    public boolean exists(UUID postId, UUID userId) {
        Long count = em.createQuery(
                        """
                                SELECT COUNT(pl) FROM PostLike pl
                                WHERE pl.postId = :postId AND pl.userId = :userId
                                """,
                        Long.class
                )
                .setParameter("postId", postId)
                .setParameter("userId", userId)
                .getSingleResult();
        return count > 0;
    }

    public void upsertLike(UUID postId, UUID userId, Instant createdAt) {
        em.createNativeQuery(
                        """
                                INSERT INTO post_likes (post_id, user_id, created_at)
                                VALUES (:postId, :userId, :createdAt)
                                ON CONFLICT (post_id, user_id) DO NOTHING
                                """
                )
                .setParameter("postId", postId)
                .setParameter("userId", userId)
                .setParameter("createdAt", createdAt)
                .executeUpdate();
    }

    public void deleteLike(UUID postId, UUID userId) {
        em.createQuery(
                        """
                                DELETE FROM PostLike pl
                                WHERE pl.postId = :postId AND pl.userId = :userId
                                """
                )
                .setParameter("postId", postId)
                .setParameter("userId", userId)
                .executeUpdate();
    }

    public void deleteLikesNotIn(UUID postId, Set<UUID> keepUserIds) {
        if (keepUserIds == null || keepUserIds.isEmpty()) {
            em.createQuery("DELETE FROM PostLike pl WHERE pl.postId = :postId")
                    .setParameter("postId", postId)
                    .executeUpdate();
            return;
        }
        em.createQuery(
                        """
                                DELETE FROM PostLike pl
                                WHERE pl.postId = :postId AND pl.userId NOT IN :keepUserIds
                                """
                )
                .setParameter("postId", postId)
                .setParameter("keepUserIds", keepUserIds)
                .executeUpdate();
    }

    public List<PostModels.LikerRow> findLikers(UUID postId, String cursor, int size) {
        CursorDecoded decoded = decodeCursor(cursor);
        String sql = """
                SELECT pl.user_id, li.value, pr.profile_picture_url, pl.created_at
                FROM post_likes pl
                JOIN profiles pr ON pr.user_id = pl.user_id
                LEFT JOIN login_identifiers li
                    ON li.user_id = pl.user_id AND li.type = 'USERNAME' AND li.active = TRUE
                WHERE pl.post_id = :postId
                  AND (
                      CAST(:hasCursor AS boolean) = FALSE
                      OR pl.created_at < CAST(:cursorAt AS timestamptz)
                      OR (pl.created_at = CAST(:cursorAt AS timestamptz) AND pl.user_id < CAST(:cursorId AS uuid))
                  )
                ORDER BY pl.created_at DESC, pl.user_id DESC
                LIMIT :limit
                """;
        List<?> rows = em.createNativeQuery(sql)
                .setParameter("postId", postId)
                .setParameter("hasCursor", decoded.hasCursor())
                .setParameter("cursorAt", decoded.createdAt() == null ? Instant.EPOCH : decoded.createdAt())
                .setParameter("cursorId", decoded.id() == null
                        ? UUID.fromString("00000000-0000-0000-0000-000000000000")
                        : decoded.id())
                .setParameter("limit", size)
                .getResultList();
        List<PostModels.LikerRow> result = new ArrayList<>();
        for (Object row : rows) {
            Object[] cols = (Object[]) row;
            result.add(new PostModels.LikerRow(
                    toUuid(cols[0]),
                    cols[1] == null ? null : cols[1].toString(),
                    cols[2] == null ? null : cols[2].toString(),
                    toInstant(cols[3])
            ));
        }
        return result;
    }

    public String encodeCursor(Instant createdAt, UUID userId) {
        String raw = createdAt.toString() + "|" + userId;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
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

    private record CursorDecoded(boolean hasCursor, Instant createdAt, UUID id) {
    }
}
