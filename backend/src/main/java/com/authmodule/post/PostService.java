package com.authmodule.post;

import com.authmodule.auth.UsernameValidator;
import com.authmodule.social.ProfileProvisioningRepository;
import com.authmodule.social.SocialRepository;
import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;
    private final RedisLikeRepository redisLikeRepository;
    private final PostPermissionService permissionService;
    private final UsernameValidator usernameValidator;
    private final SocialRepository socialRepository;
    private final ProfileProvisioningRepository profileProvisioning;
    private final EntityManager em;

    public PostService(
            PostRepository postRepository,
            CommentRepository commentRepository,
            PostLikeRepository postLikeRepository,
            RedisLikeRepository redisLikeRepository,
            PostPermissionService permissionService,
            UsernameValidator usernameValidator,
            SocialRepository socialRepository,
            ProfileProvisioningRepository profileProvisioning,
            EntityManager em
    ) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.postLikeRepository = postLikeRepository;
        this.redisLikeRepository = redisLikeRepository;
        this.permissionService = permissionService;
        this.usernameValidator = usernameValidator;
        this.socialRepository = socialRepository;
        this.profileProvisioning = profileProvisioning;
        this.em = em;
    }

    @Transactional
    public PostModels.PostResponse createPost(UUID authorId, PostModels.CreatePostRequest request) {
        ensureProfile(authorId);
        String body = normalizeText(request.body(), 2000, "Post");
        Instant now = Instant.now();
        Post post = new Post(UUID.randomUUID(), authorId, body, now);
        postRepository.save(post);
        em.flush();
        redisLikeRepository.ensureHydrated(post.getId(), Set::of);
        PostModels.PostRow row = postRepository.findActivePostRow(post.getId())
                .orElseThrow(() -> notFound("Post not found"));
        return toPostResponse(row, authorId, true);
    }

    @Transactional(readOnly = true)
    public PostModels.PostResponse getPost(UUID postId, UUID viewerId) {
        PostModels.PostRow row = postRepository.findActivePostRow(postId)
                .orElseThrow(() -> notFound("Post not found"));
        permissionService.requireCanViewAuthorPosts(viewerId, row.authorId());
        return toPostResponse(row, viewerId, viewerId != null && viewerId.equals(row.authorId()));
    }

    @Transactional(readOnly = true)
    public PostModels.CursorPageResponse<PostModels.PostResponse> getProfilePosts(
            String username,
            UUID viewerId,
            String cursor,
            int size
    ) {
        UUID authorId = resolveAuthor(username);
        permissionService.requireCanViewAuthorPosts(viewerId, authorId);
        int pageSize = normalizeSize(size);
        List<PostModels.PostRow> rows = postRepository.findActivePostsByAuthor(authorId, cursor, pageSize + 1);
        boolean hasMore = rows.size() > pageSize;
        List<PostModels.PostRow> page = hasMore ? rows.subList(0, pageSize) : rows;
        List<PostModels.PostResponse> items = new ArrayList<>();
        for (PostModels.PostRow row : page) {
            items.add(toPostResponse(row, viewerId, viewerId != null && viewerId.equals(row.authorId())));
        }
        String nextCursor = null;
        if (hasMore && !page.isEmpty()) {
            PostModels.PostRow last = page.get(page.size() - 1);
            nextCursor = postRepository.encodeCursor(last.createdAt(), last.id());
        }
        return new PostModels.CursorPageResponse<>(items, nextCursor, hasMore);
    }

    @Transactional
    public PostModels.MessageResponse deletePost(UUID postId, UUID actorId) {
        Post post = permissionService.requireActivePost(postId);
        if (!post.getAuthorId().equals(actorId)) {
            throw forbidden("Not allowed to delete this post");
        }
        post.softDelete(Instant.now());
        postRepository.merge(post);
        redisLikeRepository.clearLikeCache(postId);
        return new PostModels.MessageResponse("Post deleted");
    }

    @Transactional
    public PostModels.LikeResponse likePost(UUID postId, UUID actorId) {
        Post post = permissionService.requireActivePost(postId);
        permissionService.requireCanInteractWithPost(actorId, post);
        boolean added = redisLikeRepository.like(
                postId,
                actorId,
                () -> postLikeRepository.findUserIdsByPostId(postId)
        );
        if (added) {
            recordEvent(actorId, "POST_LIKED", "POST", postId, post.getAuthorId(), null);
        }
        return likeState(postId, actorId);
    }

    @Transactional
    public PostModels.LikeResponse unlikePost(UUID postId, UUID actorId) {
        Post post = permissionService.requireActivePost(postId);
        permissionService.requireCanInteractWithPost(actorId, post);
        boolean removed = redisLikeRepository.unlike(
                postId,
                actorId,
                () -> postLikeRepository.findUserIdsByPostId(postId)
        );
        if (removed) {
            recordEvent(actorId, "POST_UNLIKED", "POST", postId, post.getAuthorId(), null);
        }
        return likeState(postId, actorId);
    }

    @Transactional(readOnly = true)
    public PostModels.CursorPageResponse<PostModels.LikerResponse> getLikes(
            UUID postId,
            UUID viewerId,
            String cursor,
            int size
    ) {
        Post post = permissionService.requireActivePost(postId);
        permissionService.requireCanViewPost(viewerId, post);
        // Prefer Redis membership when hydrated; list comes from durable PG for stable cursors.
        int pageSize = normalizeSize(size);
        List<PostModels.LikerRow> rows = postLikeRepository.findLikers(postId, cursor, pageSize + 1);
        boolean hasMore = rows.size() > pageSize;
        List<PostModels.LikerRow> page = hasMore ? rows.subList(0, pageSize) : rows;
        List<PostModels.LikerResponse> items = page.stream()
                .map(r -> new PostModels.LikerResponse(r.userId(), r.username(), r.profilePictureUrl()))
                .toList();
        String nextCursor = null;
        if (hasMore && !page.isEmpty()) {
            PostModels.LikerRow last = page.get(page.size() - 1);
            nextCursor = postLikeRepository.encodeCursor(last.likedAt(), last.userId());
        }
        return new PostModels.CursorPageResponse<>(items, nextCursor, hasMore);
    }

    @Transactional
    public PostModels.CommentResponse createComment(UUID postId, UUID actorId, PostModels.CreateCommentRequest request) {
        ensureProfile(actorId);
        Post post = permissionService.requireActivePost(postId);
        permissionService.requireCanInteractWithPost(actorId, post);
        String body = normalizeText(request.body(), 1000, "Comment");
        Instant now = Instant.now();
        Comment comment = new Comment(UUID.randomUUID(), postId, actorId, body, now);
        commentRepository.save(comment);
        em.flush();
        recordEvent(actorId, "COMMENT_CREATED", "COMMENT", comment.getId(), post.getAuthorId(),
                "{\"postId\":\"" + postId + "\"}");
        return commentRepository.findActiveCommentRow(comment.getId())
                .map(row -> toCommentResponse(row, actorId, post.getAuthorId()))
                .orElseGet(() -> new PostModels.CommentResponse(
                        comment.getId(),
                        postId,
                        actorId,
                        null,
                        null,
                        body,
                        now,
                        true
                ));
    }

    @Transactional(readOnly = true)
    public PostModels.CursorPageResponse<PostModels.CommentResponse> getComments(
            UUID postId,
            UUID viewerId,
            String cursor,
            int size
    ) {
        Post post = permissionService.requireActivePost(postId);
        permissionService.requireCanViewPost(viewerId, post);
        int pageSize = normalizeSize(size);
        List<PostModels.CommentRow> rows = commentRepository.findActiveByPostId(postId, cursor, pageSize + 1);
        boolean hasMore = rows.size() > pageSize;
        List<PostModels.CommentRow> page = hasMore ? rows.subList(0, pageSize) : rows;
        List<PostModels.CommentResponse> items = new ArrayList<>();
        for (PostModels.CommentRow row : page) {
            items.add(toCommentResponse(row, viewerId, post.getAuthorId()));
        }
        String nextCursor = null;
        if (hasMore && !page.isEmpty()) {
            PostModels.CommentRow last = page.get(page.size() - 1);
            nextCursor = commentRepository.encodeCursor(last.createdAt(), last.id());
        }
        return new PostModels.CursorPageResponse<>(items, nextCursor, hasMore);
    }

    @Transactional
    public PostModels.MessageResponse deleteComment(UUID commentId, UUID actorId) {
        Comment comment = commentRepository.findActiveById(commentId)
                .orElseThrow(() -> notFound("Comment not found"));
        Post post = postRepository.findById(comment.getPostId())
                .orElseThrow(() -> notFound("Post not found"));
        boolean isCommentAuthor = comment.getAuthorId().equals(actorId);
        boolean isPostOwner = post.getAuthorId().equals(actorId);
        if (!isCommentAuthor && !isPostOwner) {
            throw forbidden("Not allowed to delete this comment");
        }
        if (post.isActive()) {
            permissionService.requireCanInteractWithPost(actorId, post);
        }
        comment.softDelete(Instant.now());
        commentRepository.merge(comment);
        recordEvent(actorId, "COMMENT_DELETED", "COMMENT", commentId, post.getAuthorId(),
                "{\"postId\":\"" + post.getId() + "\"}");
        return new PostModels.MessageResponse("Comment deleted");
    }

    private PostModels.LikeResponse likeState(UUID postId, UUID actorId) {
        long count = redisLikeRepository.count(postId, () -> postLikeRepository.findUserIdsByPostId(postId));
        boolean liked = redisLikeRepository.isLikedBy(
                postId,
                actorId,
                () -> postLikeRepository.findUserIdsByPostId(postId)
        );
        return new PostModels.LikeResponse(postId, count, liked);
    }

    private PostModels.PostResponse toPostResponse(PostModels.PostRow row, UUID viewerId, boolean canDelete) {
        long likeCount = redisLikeRepository.count(row.id(), () -> postLikeRepository.findUserIdsByPostId(row.id()));
        boolean liked = viewerId != null && redisLikeRepository.isLikedBy(
                row.id(),
                viewerId,
                () -> postLikeRepository.findUserIdsByPostId(row.id())
        );
        return new PostModels.PostResponse(
                row.id(),
                row.authorId(),
                row.authorUsername(),
                row.authorProfilePictureUrl(),
                row.body(),
                row.createdAt(),
                likeCount,
                row.commentCount(),
                liked,
                canDelete
        );
    }

    private PostModels.CommentResponse toCommentResponse(
            PostModels.CommentRow row,
            UUID viewerId,
            UUID postAuthorId
    ) {
        boolean canDelete = viewerId != null
                && (viewerId.equals(row.authorId()) || viewerId.equals(postAuthorId));
        return new PostModels.CommentResponse(
                row.id(),
                row.postId(),
                row.authorId(),
                row.authorUsername(),
                row.authorProfilePictureUrl(),
                row.body(),
                row.createdAt(),
                canDelete
        );
    }

    private void recordEvent(
            UUID actorId,
            String eventType,
            String targetType,
            UUID targetId,
            UUID targetOwnerId,
            String metadataJson
    ) {
        em.createNativeQuery(
                        """
                                INSERT INTO interaction_events
                                    (id, actor_id, event_type, target_type, target_id, target_owner_id, metadata_json, created_at)
                                VALUES
                                    (:id, :actorId, :eventType, :targetType, :targetId, :targetOwnerId, :metadataJson, :createdAt)
                                """
                )
                .setParameter("id", UUID.randomUUID())
                .setParameter("actorId", actorId)
                .setParameter("eventType", eventType)
                .setParameter("targetType", targetType)
                .setParameter("targetId", targetId)
                .setParameter("targetOwnerId", targetOwnerId)
                .setParameter("metadataJson", metadataJson)
                .setParameter("createdAt", Instant.now())
                .executeUpdate();
    }

    private UUID resolveAuthor(String username) {
        String normalized = usernameValidator.normalizeUsername(username.trim());
        return postRepository.findUserIdByNormalizedUsername(normalized)
                .orElseThrow(() -> notFound("User not found"));
    }

    private void ensureProfile(UUID userId) {
        profileProvisioning.createIfMissing(userId);
    }

    private static String normalizeText(String raw, int max, String label) {
        if (raw == null || raw.isBlank()) {
            throw badRequest(label + " cannot be blank");
        }
        String trimmed = raw.trim();
        if (trimmed.length() > max) {
            throw badRequest(label + " is too long");
        }
        return trimmed;
    }

    private static int normalizeSize(int size) {
        if (size <= 0) {
            return 20;
        }
        return Math.min(size, 50);
    }

    private static ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private static ResponseStatusException forbidden(String message) {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, message);
    }
}
