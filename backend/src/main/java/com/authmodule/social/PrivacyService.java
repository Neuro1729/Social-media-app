package com.authmodule.social;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PrivacyService {

    private final SocialRepository socialRepository;

    public PrivacyService(SocialRepository socialRepository) {
        this.socialRepository = socialRepository;
    }

    public boolean isBlockedEitherDirection(UUID viewerId, UUID ownerId) {
        if (viewerId == null || ownerId == null || viewerId.equals(ownerId)) {
            return false;
        }
        return socialRepository.isBlockedEitherDirection(viewerId, ownerId);
    }

    public boolean canViewProfile(UUID viewerId, UUID ownerId) {
        if (ownerId == null) {
            return false;
        }
        if (viewerId != null && viewerId.equals(ownerId)) {
            return true;
        }
        return !isBlockedEitherDirection(viewerId, ownerId);
    }

    public boolean canViewFullProfile(UUID viewerId, UUID ownerId) {
        if (!canViewProfile(viewerId, ownerId)) {
            return false;
        }
        if (viewerId != null && viewerId.equals(ownerId)) {
            return true;
        }
        Profile profile = socialRepository.findProfileByUserId(ownerId).orElse(null);
        if (profile == null) {
            return false;
        }
        if (!profile.isPrivate()) {
            return true;
        }
        if (viewerId == null) {
            return false;
        }
        return socialRepository.findRelationship(viewerId, ownerId)
                .map(rel -> rel.getStatus() == Follow.Status.FOLLOWING)
                .orElse(false);
    }

    public boolean canViewPosts(UUID viewerId, UUID ownerId) {
        return canViewFullProfile(viewerId, ownerId);
    }

    public boolean canViewFollowers(UUID viewerId, UUID ownerId) {
        return canViewFullProfile(viewerId, ownerId);
    }

    public boolean canViewFollowing(UUID viewerId, UUID ownerId) {
        return canViewFullProfile(viewerId, ownerId);
    }

    public boolean canFollow(UUID viewerId, UUID ownerId) {
        if (viewerId == null || ownerId == null || viewerId.equals(ownerId)) {
            return false;
        }
        return !isBlockedEitherDirection(viewerId, ownerId);
    }
}
