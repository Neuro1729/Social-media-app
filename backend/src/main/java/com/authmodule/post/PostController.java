package com.authmodule.post;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping("/api/posts")
    public PostModels.PostResponse createPost(
            @Valid @RequestBody PostModels.CreatePostRequest request,
            Authentication authentication
    ) {
        return postService.createPost(requireUserId(authentication), request);
    }

    @GetMapping("/api/posts/{id}")
    public PostModels.PostResponse getPost(
            @PathVariable("id") UUID id,
            Authentication authentication
    ) {
        return postService.getPost(id, optionalUserId(authentication));
    }

    @GetMapping("/api/profiles/{username}/posts")
    public PostModels.CursorPageResponse<PostModels.PostResponse> profilePosts(
            @PathVariable String username,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        return postService.getProfilePosts(username, optionalUserId(authentication), cursor, size);
    }

    @DeleteMapping("/api/posts/{id}")
    public PostModels.MessageResponse deletePost(
            @PathVariable("id") UUID id,
            Authentication authentication
    ) {
        return postService.deletePost(id, requireUserId(authentication));
    }

    @PutMapping("/api/posts/{id}/like")
    public PostModels.LikeResponse like(
            @PathVariable("id") UUID id,
            Authentication authentication
    ) {
        return postService.likePost(id, requireUserId(authentication));
    }

    @DeleteMapping("/api/posts/{id}/like")
    public PostModels.LikeResponse unlike(
            @PathVariable("id") UUID id,
            Authentication authentication
    ) {
        return postService.unlikePost(id, requireUserId(authentication));
    }

    @GetMapping("/api/posts/{id}/likes")
    public PostModels.CursorPageResponse<PostModels.LikerResponse> likes(
            @PathVariable("id") UUID id,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        return postService.getLikes(id, optionalUserId(authentication), cursor, size);
    }

    @PostMapping("/api/posts/{id}/comments")
    public PostModels.CommentResponse createComment(
            @PathVariable("id") UUID id,
            @Valid @RequestBody PostModels.CreateCommentRequest request,
            Authentication authentication
    ) {
        return postService.createComment(id, requireUserId(authentication), request);
    }

    @GetMapping("/api/posts/{id}/comments")
    public PostModels.CursorPageResponse<PostModels.CommentResponse> comments(
            @PathVariable("id") UUID id,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        return postService.getComments(id, optionalUserId(authentication), cursor, size);
    }

    @DeleteMapping("/api/comments/{id}")
    public PostModels.MessageResponse deleteComment(
            @PathVariable("id") UUID id,
            Authentication authentication
    ) {
        return postService.deleteComment(id, requireUserId(authentication));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<PostModels.ErrorResponse> handleStatus(ResponseStatusException ex) {
        String message = ex.getReason() == null ? "Request failed" : ex.getReason();
        return ResponseEntity.status(ex.getStatusCode()).body(new PostModels.ErrorResponse(message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<PostModels.ErrorResponse> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(new PostModels.ErrorResponse(ex.getMessage()));
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
