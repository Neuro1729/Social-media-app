package com.authmodule.social;

import com.authmodule.auth.AuthPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/social")
public class SocialController {

    private final SocialService socialService;

    public SocialController(SocialService socialService) {
        this.socialService = socialService;
    }

    @GetMapping("/search/{username}")
    public SocialModels.ProfileResponse search(
            @PathVariable String username,
            Authentication authentication
    ) {
        return socialService.searchProfileByUsername(username, optionalUserId(authentication));
    }

    @GetMapping("/profiles/{username}")
    public SocialModels.ProfileResponse getProfile(
            @PathVariable String username,
            Authentication authentication
    ) {
        return socialService.getProfileByUsername(username, optionalUserId(authentication));
    }

    @PutMapping("/profile")
    public SocialModels.ProfileResponse updateProfile(
            @Valid @RequestBody SocialModels.UpdateProfileRequest request,
            Authentication authentication
    ) {
        return socialService.updateProfile(requireUserId(authentication), request);
    }

    @PutMapping("/profile/privacy")
    public SocialModels.ProfileResponse changePrivacy(
            @Valid @RequestBody SocialModels.ChangePrivacyRequest request,
            Authentication authentication
    ) {
        return socialService.changePrivacy(requireUserId(authentication), request);
    }

    @PostMapping("/users/{userId}/follow")
    public SocialModels.FollowResponse follow(
            @PathVariable UUID userId,
            Authentication authentication
    ) {
        return socialService.followUser(requireUserId(authentication), userId);
    }

    @DeleteMapping("/users/{userId}/follow")
    public SocialModels.MessageResponse unfollow(
            @PathVariable UUID userId,
            Authentication authentication
    ) {
        return socialService.unfollowUser(requireUserId(authentication), userId);
    }

    @GetMapping("/follow-requests")
    public SocialModels.CursorPageResponse<SocialModels.FollowRequestResponse> followRequests(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        return socialService.getPendingRequests(requireUserId(authentication), cursor, size);
    }

    @PostMapping("/follow-requests/{userId}/approve")
    public SocialModels.MessageResponse approve(
            @PathVariable UUID userId,
            Authentication authentication
    ) {
        return socialService.approveFollowRequest(requireUserId(authentication), userId);
    }

    @PostMapping("/follow-requests/{userId}/reject")
    public SocialModels.MessageResponse reject(
            @PathVariable UUID userId,
            Authentication authentication
    ) {
        return socialService.rejectFollowRequest(requireUserId(authentication), userId);
    }

    @GetMapping("/profiles/{username}/followers")
    public SocialModels.CursorPageResponse<SocialModels.ConnectionUserResponse> followers(
            @PathVariable String username,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        return socialService.getFollowers(username, optionalUserId(authentication), cursor, size);
    }

    @GetMapping("/profiles/{username}/following")
    public SocialModels.CursorPageResponse<SocialModels.ConnectionUserResponse> following(
            @PathVariable String username,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        return socialService.getFollowing(username, optionalUserId(authentication), cursor, size);
    }

    @DeleteMapping("/followers/{userId}")
    public SocialModels.MessageResponse removeFollower(
            @PathVariable UUID userId,
            Authentication authentication
    ) {
        return socialService.removeFollower(requireUserId(authentication), userId);
    }

    @PostMapping("/users/{userId}/block")
    public SocialModels.MessageResponse block(
            @PathVariable UUID userId,
            Authentication authentication
    ) {
        return socialService.blockUser(requireUserId(authentication), userId);
    }

    @DeleteMapping("/users/{userId}/block")
    public SocialModels.MessageResponse unblock(
            @PathVariable UUID userId,
            Authentication authentication
    ) {
        return socialService.unblockUser(requireUserId(authentication), userId);
    }

    @GetMapping("/blocked-users")
    public SocialModels.CursorPageResponse<SocialModels.BlockedUserResponse> blockedUsers(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        return socialService.getBlockedUsers(requireUserId(authentication), cursor, size);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<SocialModels.ErrorResponse> handleStatus(ResponseStatusException ex) {
        String message = ex.getReason() == null ? "Request failed" : ex.getReason();
        return ResponseEntity.status(ex.getStatusCode()).body(new SocialModels.ErrorResponse(message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<SocialModels.ErrorResponse> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(new SocialModels.ErrorResponse(ex.getMessage()));
    }

    private UUID optionalUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthPrincipal principal)) {
            return null;
        }
        return principal.userId();
    }

    private UUID requireUserId(Authentication authentication) {
        UUID userId = optionalUserId(authentication);
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return userId;
    }
}
