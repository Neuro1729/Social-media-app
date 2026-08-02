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
public class CommentRepository {

    private final EntityManager em;

    public CommentRepository(EntityManager em) {
        this.em = em;
    }

    public Comment save(Comment comment) {
        em.persist(comment);
        return comment;
    }

    public Optional<Comment> findById(UUID id) {
        return Optional.ofNullable(em.find(Comment.class, id));
    }

    public Optional<Comment> findActiveById(UUID id) {
        return findById(id).filter(Comment::isActive);
    }

    public Comment merge(Comment comment) {
        return em.merge(comment);
    }

    public long countActiveByPostId(UUID postId) {
        return em.createQuery(
                        """
                                SELECT COUNT(c) FROM Comment c
                                WHERE c.postId = :postId AND c.status = :status
                                """,
                        Long.class
                )
                .setParameter("postId", postId)
                .setParameter("status", Comment.Status.ACTIVE)
                .getSingleResult();
    }

    public List<PostModels.CommentRow> findActiveByPostId(UUID postId, String cursor, int size) {
        CursorDecoded decoded = decodeCursor(cursor);
        String sql = """
                SELECT c.id, c.post_id, c.author_id, li.value, pr.profile_picture_url, c.body, c.created_at
                FROM post_comments c
                JOIN profiles pr ON pr.user_id = c.author_id
                LEFT JOIN login_identifiers li
                    ON li.user_id = c.author_id AND li.type = 'USERNAME' AND li.active = TRUE
                WHERE c.post_id = :postId
                  AND c.status = 'ACTIVE'
                  AND (
                      CAST(:hasCursor AS boolean) = FALSE
                      OR c.created_at < CAST(:cursorAt AS timestamptz)
                      OR (c.created_at = CAST(:cursorAt AS timestamptz) AND c.id < CAST(:cursorId AS uuid))
                  )
                ORDER BY c.created_at DESC, c.id DESC
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
        List<PostModels.CommentRow> result = new ArrayList<>();
        for (Object row : rows) {
            Object[] cols = (Object[]) row;
            result.add(new PostModels.CommentRow(
                    toUuid(cols[0]),
                    toUuid(cols[1]),
                    toUuid(cols[2]),
                    cols[3] == null ? null : cols[3].toString(),
                    cols[4] == null ? null : cols[4].toString(),
                    cols[5].toString(),
                    toInstant(cols[6])
            ));
        }
        return result;
    }

    public Optional<PostModels.CommentRow> findActiveCommentRow(UUID commentId) {
        String sql = """
                SELECT c.id, c.post_id, c.author_id, li.value, pr.profile_picture_url, c.body, c.created_at
                FROM post_comments c
                JOIN profiles pr ON pr.user_id = c.author_id
                LEFT JOIN login_identifiers li
                    ON li.user_id = c.author_id AND li.type = 'USERNAME' AND li.active = TRUE
                WHERE c.id = :commentId AND c.status = 'ACTIVE'
                """;
        try {
            Object row = em.createNativeQuery(sql)
                    .setParameter("commentId", commentId)
                    .getSingleResult();
            Object[] cols = (Object[]) row;
            return Optional.of(new PostModels.CommentRow(
                    toUuid(cols[0]),
                    toUuid(cols[1]),
                    toUuid(cols[2]),
                    cols[3] == null ? null : cols[3].toString(),
                    cols[4] == null ? null : cols[4].toString(),
                    cols[5].toString(),
                    toInstant(cols[6])
            ));
        } catch (NoResultException ex) {
            return Optional.empty();
        }
    }

    public String encodeCursor(Instant createdAt, UUID id) {
        String raw = createdAt.toString() + "|" + id;
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
