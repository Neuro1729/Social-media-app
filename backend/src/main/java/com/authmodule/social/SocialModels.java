package com.authmodule.social;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public final class SocialModels {

    private SocialModels() {
    }

    public enum RelationshipStatus {
        NONE,
        PENDING,
        FOLLOWING,
        REJECTED,
        SELF
    }

    public record UpdateProfileRequest(
            @Size(max = 160) String bio,
            String profilePictureUrl
    ) {
    }

    public record ChangePrivacyRequest(
            @NotNull Boolean isPrivate
    ) {
    }

    public record ProfileResponse(
            UUID userId,
            String username,
            String bio,
            String profilePictureUrl,
            boolean isPrivate,
            long followerCount,
            long followingCount,
            RelationshipStatus relationshipStatus,
            boolean canViewProtectedContent
    ) {
    }

    public record LimitedProfileResponse(
            UUID userId,
            String username,
            String bio,
            String profilePictureUrl,
            boolean isPrivate,
            long followerCount,
            long followingCount,
            RelationshipStatus relationshipStatus,
            boolean canViewProtectedContent
    ) {
    }

    public record FollowResponse(
            RelationshipStatus relationshipStatus,
            String message
    ) {
    }

    public record ConnectionUserResponse(
            UUID userId,
            String username,
            String profilePictureUrl,
            boolean isPrivate,
            RelationshipStatus relationshipStatus
    ) {
    }

    public record FollowRequestResponse(
            UUID userId,
            String username,
            String profilePictureUrl,
            boolean isPrivate,
            RelationshipStatus relationshipStatus
    ) {
    }

    public record BlockedUserResponse(
            UUID userId,
            String username,
            String profilePictureUrl,
            boolean isPrivate
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

    /** Internal row for cursor-paginated follow/block lists. */
    public record ConnectionRow(
            UUID userId,
            String username,
            String profilePictureUrl,
            boolean isPrivate,
            java.time.Instant createdAt
    ) {
    }
}
