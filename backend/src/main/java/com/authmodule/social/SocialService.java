package com.authmodule.social;

import com.authmodule.auth.UsernameValidator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class SocialService {

    private final SocialRepository socialRepository;
    private final ProfileProvisioningRepository profileProvisioning;
    private final PrivacyService privacyService;
    private final UsernameValidator usernameValidator;

    public SocialService(
            SocialRepository socialRepository,
            ProfileProvisioningRepository profileProvisioning,
            PrivacyService privacyService,
            UsernameValidator usernameValidator
    ) {
        this.socialRepository = socialRepository;
        this.profileProvisioning = profileProvisioning;
        this.privacyService = privacyService;
        this.usernameValidator = usernameValidator;
    }

    @Transactional
    public SocialModels.ProfileResponse getProfileByUsername(String username, UUID viewerId) {
        String normalized = usernameValidator.normalizeUsername(username.trim());
        UUID ownerId = socialRepository.findUserIdByNormalizedUsername(normalized)
                .orElseThrow(() -> notFound("User not found"));
        return buildProfileResponse(ownerId, viewerId);
    }

    @Transactional
    public SocialModels.ProfileResponse searchProfileByUsername(String username, UUID viewerId) {
        return getProfileByUsername(username, viewerId);
    }

    @Transactional
    public SocialModels.ProfileResponse updateProfile(UUID userId, SocialModels.UpdateProfileRequest request) {
        Profile profile = getOrCreateProfile(userId);
        if (request.bio() != null) {
            profile.setBio(request.bio().isBlank() ? null : request.bio().trim());
        }
        if (request.profilePictureUrl() != null) {
            String url = request.profilePictureUrl().isBlank() ? null : request.profilePictureUrl().trim();
            validateHttpUrl(url);
            profile.setProfilePictureUrl(url);
        }
        socialRepository.updateProfile(profile);
        return buildProfileResponse(userId, userId);
    }

    @Transactional
    public SocialModels.ProfileResponse changePrivacy(UUID userId, SocialModels.ChangePrivacyRequest request) {
        Profile profile = getOrCreateProfile(userId);
        boolean makingPublic = profile.isPrivate() && !request.isPrivate();
        socialRepository.updatePrivacy(profile, request.isPrivate());
        if (makingPublic) {
            socialRepository.convertIncomingPendingToFollowing(userId);
        }
        return buildProfileResponse(userId, userId);
    }

    @Transactional
    public SocialModels.FollowResponse followUser(UUID followerId, UUID targetUserId) {
        if (followerId.equals(targetUserId)) {
            throw badRequest("Cannot follow yourself");
        }
        if (!socialRepository.userExists(targetUserId)) {
            throw notFound("User not found");
        }
        if (!privacyService.canFollow(followerId, targetUserId)) {
            throw unavailable();
        }
        getOrCreateProfile(followerId);
        Profile target = getOrCreateProfile(targetUserId);

        var existing = socialRepository.findRelationship(followerId, targetUserId);
        if (existing.isPresent()) {
            Follow rel = existing.get();
            if (rel.getStatus() == Follow.Status.FOLLOWING) {
                throw conflict("Already following");
            }
            if (rel.getStatus() == Follow.Status.PENDING) {
                throw conflict("Follow request already pending");
            }
            Follow.Status next = target.isPrivate() ? Follow.Status.PENDING : Follow.Status.FOLLOWING;
            rel.setCreatedAt(Instant.now());
            socialRepository.updateRelationshipStatus(rel, next);
            return new SocialModels.FollowResponse(toApiStatus(next), messageFor(next));
        }

        Follow.Status status = target.isPrivate() ? Follow.Status.PENDING : Follow.Status.FOLLOWING;
        socialRepository.saveRelationship(new Follow(followerId, targetUserId, status, Instant.now()));
        return new SocialModels.FollowResponse(toApiStatus(status), messageFor(status));
    }

    @Transactional
    public SocialModels.MessageResponse unfollowUser(UUID followerId, UUID targetUserId) {
        socialRepository.deleteRelationship(followerId, targetUserId);
        return new SocialModels.MessageResponse("Unfollowed");
    }

    @Transactional
    public SocialModels.MessageResponse approveFollowRequest(UUID ownerId, UUID requesterId) {
        Follow rel = socialRepository.findRelationship(requesterId, ownerId)
                .orElseThrow(() -> notFound("Follow request not found"));
        if (rel.getStatus() != Follow.Status.PENDING) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to approve this request");
        }
        socialRepository.updateRelationshipStatus(rel, Follow.Status.FOLLOWING);
        return new SocialModels.MessageResponse("Follow request approved");
    }

    @Transactional
    public SocialModels.MessageResponse rejectFollowRequest(UUID ownerId, UUID requesterId) {
        Follow rel = socialRepository.findRelationship(requesterId, ownerId)
                .orElseThrow(() -> notFound("Follow request not found"));
        if (rel.getStatus() != Follow.Status.PENDING) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to reject this request");
        }
        socialRepository.updateRelationshipStatus(rel, Follow.Status.REJECTED);
        return new SocialModels.MessageResponse("Follow request rejected");
    }

    @Transactional
    public SocialModels.MessageResponse removeFollower(UUID ownerId, UUID followerId) {
        socialRepository.deleteRelationship(followerId, ownerId);
        return new SocialModels.MessageResponse("Follower removed");
    }

    public SocialModels.CursorPageResponse<SocialModels.ConnectionUserResponse> getFollowers(
            String username,
            UUID viewerId,
            String cursor,
            int size
    ) {
        UUID ownerId = resolveOwner(username);
        if (privacyService.isBlockedEitherDirection(viewerId, ownerId)) {
            throw unavailable();
        }
        if (!privacyService.canViewFollowers(viewerId, ownerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to view followers");
        }
        int pageSize = normalizeSize(size);
        List<SocialModels.ConnectionRow> rows =
                socialRepository.findFollowers(ownerId, cursor, pageSize + 1);
        return toConnectionPage(rows, pageSize, viewerId);
    }

    public SocialModels.CursorPageResponse<SocialModels.ConnectionUserResponse> getFollowing(
            String username,
            UUID viewerId,
            String cursor,
            int size
    ) {
        UUID ownerId = resolveOwner(username);
        if (privacyService.isBlockedEitherDirection(viewerId, ownerId)) {
            throw unavailable();
        }
        if (!privacyService.canViewFollowing(viewerId, ownerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to view following");
        }
        int pageSize = normalizeSize(size);
        List<SocialModels.ConnectionRow> rows =
                socialRepository.findFollowing(ownerId, cursor, pageSize + 1);
        return toConnectionPage(rows, pageSize, viewerId);
    }

    public SocialModels.CursorPageResponse<SocialModels.FollowRequestResponse> getPendingRequests(
            UUID ownerId,
            String cursor,
            int size
    ) {
        int pageSize = normalizeSize(size);
        List<SocialModels.ConnectionRow> rows =
                socialRepository.findPendingRequests(ownerId, cursor, pageSize + 1);
        boolean hasMore = rows.size() > pageSize;
        List<SocialModels.ConnectionRow> page = hasMore ? rows.subList(0, pageSize) : rows;
        List<SocialModels.FollowRequestResponse> items = new ArrayList<>();
        for (SocialModels.ConnectionRow row : page) {
            items.add(new SocialModels.FollowRequestResponse(
                    row.userId(),
                    row.username(),
                    row.profilePictureUrl(),
                    row.isPrivate(),
                    SocialModels.RelationshipStatus.PENDING
            ));
        }
        String nextCursor = null;
        if (hasMore && !page.isEmpty()) {
            SocialModels.ConnectionRow last = page.get(page.size() - 1);
            nextCursor = socialRepository.encodeCursor(last.createdAt(), last.userId());
        }
        return new SocialModels.CursorPageResponse<>(items, nextCursor, hasMore);
    }

    @Transactional
    public SocialModels.MessageResponse blockUser(UUID blockerId, UUID blockedId) {
        if (blockerId.equals(blockedId)) {
            throw badRequest("Cannot block yourself");
        }
        if (!socialRepository.userExists(blockedId)) {
            throw notFound("User not found");
        }
        getOrCreateProfile(blockerId);
        getOrCreateProfile(blockedId);
        socialRepository.deleteRelationshipsBothDirections(blockerId, blockedId);
        socialRepository.createBlock(blockerId, blockedId);
        return new SocialModels.MessageResponse("User blocked");
    }

    @Transactional
    public SocialModels.MessageResponse unblockUser(UUID blockerId, UUID blockedId) {
        socialRepository.deleteBlock(blockerId, blockedId);
        return new SocialModels.MessageResponse("User unblocked");
    }

    public SocialModels.CursorPageResponse<SocialModels.BlockedUserResponse> getBlockedUsers(
            UUID blockerId,
            String cursor,
            int size
    ) {
        int pageSize = normalizeSize(size);
        List<SocialModels.ConnectionRow> rows =
                socialRepository.findBlockedUsers(blockerId, cursor, pageSize + 1);
        boolean hasMore = rows.size() > pageSize;
        List<SocialModels.ConnectionRow> page = hasMore ? rows.subList(0, pageSize) : rows;
        List<SocialModels.BlockedUserResponse> items = page.stream()
                .map(r -> new SocialModels.BlockedUserResponse(
                        r.userId(),
                        r.username(),
                        r.profilePictureUrl(),
                        r.isPrivate()
                ))
                .toList();
        String nextCursor = null;
        if (hasMore && !page.isEmpty()) {
            SocialModels.ConnectionRow last = page.get(page.size() - 1);
            nextCursor = socialRepository.encodeCursor(last.createdAt(), last.userId());
        }
        return new SocialModels.CursorPageResponse<>(items, nextCursor, hasMore);
    }

    private SocialModels.ProfileResponse buildProfileResponse(UUID ownerId, UUID viewerId) {
        if (!privacyService.canViewProfile(viewerId, ownerId)) {
            throw unavailable();
        }
        Profile profile = getOrCreateProfile(ownerId);
        String username = socialRepository.findActiveUsername(ownerId).orElse(null);
        SocialModels.RelationshipStatus relationship = resolveRelationship(viewerId, ownerId);
        boolean canViewProtected = privacyService.canViewFullProfile(viewerId, ownerId);
        return new SocialModels.ProfileResponse(
                ownerId,
                username,
                profile.getBio(),
                profile.getProfilePictureUrl(),
                profile.isPrivate(),
                socialRepository.countFollowers(ownerId),
                socialRepository.countFollowing(ownerId),
                relationship,
                canViewProtected
        );
    }

    private SocialModels.RelationshipStatus resolveRelationship(UUID viewerId, UUID ownerId) {
        if (viewerId != null && viewerId.equals(ownerId)) {
            return SocialModels.RelationshipStatus.SELF;
        }
        if (viewerId == null) {
            return SocialModels.RelationshipStatus.NONE;
        }
        return socialRepository.findRelationship(viewerId, ownerId)
                .map(rel -> toApiStatus(rel.getStatus()))
                .orElse(SocialModels.RelationshipStatus.NONE);
    }

    private SocialModels.CursorPageResponse<SocialModels.ConnectionUserResponse> toConnectionPage(
            List<SocialModels.ConnectionRow> rows,
            int pageSize,
            UUID viewerId
    ) {
        boolean hasMore = rows.size() > pageSize;
        List<SocialModels.ConnectionRow> page = hasMore ? rows.subList(0, pageSize) : rows;
        List<SocialModels.ConnectionUserResponse> items = new ArrayList<>();
        for (SocialModels.ConnectionRow row : page) {
            items.add(new SocialModels.ConnectionUserResponse(
                    row.userId(),
                    row.username(),
                    row.profilePictureUrl(),
                    row.isPrivate(),
                    resolveRelationship(viewerId, row.userId())
            ));
        }
        String nextCursor = null;
        if (hasMore && !page.isEmpty()) {
            SocialModels.ConnectionRow last = page.get(page.size() - 1);
            nextCursor = socialRepository.encodeCursor(last.createdAt(), last.userId());
        }
        return new SocialModels.CursorPageResponse<>(items, nextCursor, hasMore);
    }

    private Profile getOrCreateProfile(UUID userId) {
        return profileProvisioning.createIfMissing(userId);
    }

    private UUID resolveOwner(String username) {
        String normalized = usernameValidator.normalizeUsername(username.trim());
        return socialRepository.findUserIdByNormalizedUsername(normalized)
                .orElseThrow(() -> notFound("User not found"));
    }

    private static int normalizeSize(int size) {
        if (size <= 0) {
            return 20;
        }
        return Math.min(size, 50);
    }

    private static void validateHttpUrl(String url) {
        if (url == null) {
            return;
        }
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();
            if (scheme == null
                    || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))
                    || uri.getHost() == null) {
                throw badRequest("profilePictureUrl must be a valid HTTP or HTTPS URL");
            }
        } catch (IllegalArgumentException ex) {
            throw badRequest("profilePictureUrl must be a valid HTTP or HTTPS URL");
        }
    }

    private static SocialModels.RelationshipStatus toApiStatus(Follow.Status status) {
        return switch (status) {
            case PENDING -> SocialModels.RelationshipStatus.PENDING;
            case FOLLOWING -> SocialModels.RelationshipStatus.FOLLOWING;
            case REJECTED -> SocialModels.RelationshipStatus.REJECTED;
        };
    }

    private static String messageFor(Follow.Status status) {
        return status == Follow.Status.PENDING ? "Follow request sent" : "Now following";
    }

    private static ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }

    private static ResponseStatusException unavailable() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile unavailable");
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private static ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}
