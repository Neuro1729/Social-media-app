package com.authmodule.post;

import com.authmodule.social.PrivacyService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class PostPermissionService {

    private final PrivacyService privacyService;
    private final PostRepository postRepository;

    public PostPermissionService(PrivacyService privacyService, PostRepository postRepository) {
        this.privacyService = privacyService;
        this.postRepository = postRepository;
    }

    public Post requireActivePost(UUID postId) {
        return postRepository.findActiveById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
    }

    public void requireCanViewPost(UUID viewerId, Post post) {
        if (!privacyService.canViewPosts(viewerId, post.getAuthorId())) {
            throw unavailable();
        }
    }

    public void requireCanViewAuthorPosts(UUID viewerId, UUID authorId) {
        if (!privacyService.canViewPosts(viewerId, authorId)) {
            throw unavailable();
        }
    }

    public void requireCanInteractWithPost(UUID actorId, Post post) {
        if (actorId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        requireCanViewPost(actorId, post);
    }

    private static ResponseStatusException unavailable() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Post unavailable");
    }
}
