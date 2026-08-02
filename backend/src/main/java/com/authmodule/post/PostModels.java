package com.authmodule.post;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class PostModels {

    private PostModels() {
    }

    public record CreatePostRequest(
            @NotBlank @Size(max = 2000) String body
    ) {
    }

    public record CreateCommentRequest(
            @NotBlank @Size(max = 1000) String body
    ) {
    }

    public record PostResponse(
            UUID id,
            UUID authorId,
            String authorUsername,
            String authorProfilePictureUrl,
            String body,
            Instant createdAt,
            long likeCount,
            long commentCount,
            boolean likedByViewer,
            boolean canDelete
    ) {
    }

    public record CommentResponse(
            UUID id,
            UUID postId,
            UUID authorId,
            String authorUsername,
            String authorProfilePictureUrl,
            String body,
            Instant createdAt,
            boolean canDelete
    ) {
    }

    public record LikeResponse(
            UUID postId,
            long likeCount,
            boolean likedByViewer
    ) {
    }

    public record LikerResponse(
            UUID userId,
            String username,
            String profilePictureUrl
    ) {
    }

    public record CursorPageResponse<T>(
            List<T> items,
            String nextCursor,
            boolean hasMore
    ) {
    }

    public record MessageResponse(String message) {
    }

    public record ErrorResponse(String error) {
    }

    public record PostRow(
            UUID id,
            UUID authorId,
            String authorUsername,
            String authorProfilePictureUrl,
            String body,
            Instant createdAt,
            long commentCount
    ) {
    }

    public record CommentRow(
            UUID id,
            UUID postId,
            UUID authorId,
            String authorUsername,
            String authorProfilePictureUrl,
            String body,
            Instant createdAt
    ) {
    }

    public record LikerRow(
            UUID userId,
            String username,
            String profilePictureUrl,
            Instant likedAt
    ) {
    }
}
