package com.socialmedia.api;

import com.socialmedia.logic.PostLogic;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/posts")
public class PostApi {

  private final PostLogic postLogic;

  public PostApi(PostLogic postLogic) {
    this.postLogic = postLogic;
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<?> create(
      Authentication authentication,
      @RequestParam(value = "caption", required = false) String caption,
      @RequestPart(value = "image", required = false) MultipartFile image) {
    try {
      return ResponseEntity.status(HttpStatus.CREATED)
          .body(postLogic.create(currentUserId(authentication), caption, image));
    } catch (IllegalArgumentException ex) {
      return badRequest(ex.getMessage());
    } catch (Exception ex) {
      return ResponseEntity.internalServerError()
          .body(Map.of("error", "Failed to create post"));
    }
  }

  @GetMapping("/me")
  public ResponseEntity<?> myPosts(Authentication authentication) {
    try {
      return ResponseEntity.ok(postLogic.myPosts(currentUserId(authentication)));
    } catch (IllegalArgumentException ex) {
      return badRequest(ex.getMessage());
    }
  }

  @GetMapping("/user/{username}")
  public ResponseEntity<?> postsByUser(
      Authentication authentication, @PathVariable String username) {
    try {
      return ResponseEntity.ok(
          postLogic.postsByUsername(username, currentUserId(authentication)));
    } catch (IllegalArgumentException ex) {
      return badRequest(ex.getMessage());
    }
  }

  @PutMapping("/{postId}")
  public ResponseEntity<?> editCaption(
      Authentication authentication,
      @PathVariable Long postId,
      @RequestBody Map<String, String> body) {
    try {
      return ResponseEntity.ok(
          postLogic.editCaption(currentUserId(authentication), postId, body.get("caption")));
    } catch (IllegalArgumentException ex) {
      return badRequest(ex.getMessage());
    }
  }

  @DeleteMapping("/{postId}")
  public ResponseEntity<?> delete(Authentication authentication, @PathVariable Long postId) {
    try {
      return ResponseEntity.ok(postLogic.delete(currentUserId(authentication), postId));
    } catch (IllegalArgumentException ex) {
      return badRequest(ex.getMessage());
    }
  }

  private static Long currentUserId(Authentication authentication) {
    return (Long) authentication.getPrincipal();
  }

  private static ResponseEntity<Map<String, String>> badRequest(String message) {
    return ResponseEntity.badRequest().body(Map.of("error", message));
  }
}
