package com.authmodule.post;

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
public class PostRepository {

    private final EntityManager em;

    public PostRepository(EntityManager em) {
        this.em = em;
    }

    public Post save(Post post) {
        em.persist(post);
        return post;
    }

    public Optional<Post> findById(UUID id) {
        return Optional.ofNullable(em.find(Post.class, id));
    }

    public Optional<Post> findActiveById(UUID id) {
        return findById(id).filter(Post::isActive);
    }

    public Post merge(Post post) {
        return em.merge(post);
    }

    public Optional<PostModels.PostRow> findActivePostRow(UUID postId) {
        String sql = """
                SELECT p.id, p.author_id, li.value, pr.profile_picture_url, p.body, p.created_at,
                       (
                           SELECT COUNT(*)
                           FROM post_comments c
                           WHERE c.post_id = p.id AND c.status = 'ACTIVE'
                       ) AS comment_count
                FROM posts p
                JOIN profiles pr ON pr.user_id = p.author_id
                LEFT JOIN login_identifiers li
                    ON li.user_id = p.author_id AND li.type = 'USERNAME' AND li.active = TRUE
                WHERE p.id = :postId AND p.status = 'ACTIVE'
                """;
        try {
            Object row = em.createNativeQuery(sql)
                    .setParameter("postId", postId)
                    .getSingleResult();
            return Optional.of(mapPostRow((Object[]) row));
        } catch (NoResultException ex) {
            return Optional.empty();
        }
    }

    public List<PostModels.PostRow> findActivePostsByAuthor(UUID authorId, String cursor, int size) {
        CursorDecoded decoded = decodeCursor(cursor);
        String sql = """
                SELECT p.id, p.author_id, li.value, pr.profile_picture_url, p.body, p.created_at,
                       (
                           SELECT COUNT(*)
                           FROM post_comments c
                           WHERE c.post_id = p.id AND c.status = 'ACTIVE'
                       ) AS comment_count
                FROM posts p
                JOIN profiles pr ON pr.user_id = p.author_id
                LEFT JOIN login_identifiers li
                    ON li.user_id = p.author_id AND li.type = 'USERNAME' AND li.active = TRUE
                WHERE p.author_id = :authorId
                  AND p.status = 'ACTIVE'
                  AND (
                      CAST(:hasCursor AS boolean) = FALSE
                      OR p.created_at < CAST(:cursorAt AS timestamptz)
                      OR (p.created_at = CAST(:cursorAt AS timestamptz) AND p.id < CAST(:cursorId AS uuid))
                  )
                ORDER BY p.created_at DESC, p.id DESC
                LIMIT :limit
                """;
        List<?> rows = em.createNativeQuery(sql)
                .setParameter("authorId", authorId)
                .setParameter("hasCursor", decoded.hasCursor())
                .setParameter("cursorAt", decoded.createdAt() == null ? Instant.EPOCH : decoded.createdAt())
                .setParameter("cursorId", decoded.id() == null
                        ? UUID.fromString("00000000-0000-0000-0000-000000000000")
                        : decoded.id())
                .setParameter("limit", size)
                .getResultList();
        List<PostModels.PostRow> result = new ArrayList<>();
        for (Object row : rows) {
            result.add(mapPostRow((Object[]) row));
        }
        return result;
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

    public String encodeCursor(Instant createdAt, UUID id) {
        String raw = createdAt.toString() + "|" + id;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private PostModels.PostRow mapPostRow(Object[] cols) {
        return new PostModels.PostRow(
                toUuid(cols[0]),
                toUuid(cols[1]),
                cols[2] == null ? null : cols[2].toString(),
                cols[3] == null ? null : cols[3].toString(),
                cols[4].toString(),
                toInstant(cols[5]),
                ((Number) cols[6]).longValue()
        );
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
