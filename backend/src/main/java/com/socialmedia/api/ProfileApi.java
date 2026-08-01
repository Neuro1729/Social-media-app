package com.socialmedia.api;

import com.socialmedia.logic.ProfileLogic;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/profile")
public class ProfileApi {

  private final ProfileLogic profileLogic;

  public ProfileApi(ProfileLogic profileLogic) {
    this.profileLogic = profileLogic;
  }

  @GetMapping("/me")
  public ResponseEntity<?> me(Authentication authentication) {
    try {
      return ResponseEntity.ok(profileLogic.getProfile(currentUserId(authentication)));
    } catch (IllegalArgumentException ex) {
      return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }
  }

  @PutMapping
  public ResponseEntity<?> saveProfile(
      Authentication authentication, @RequestBody Map<String, String> body) {
    try {
      return ResponseEntity.ok(
          profileLogic.createOrUpdateProfile(
              currentUserId(authentication), body.get("fullName"), body.get("bio")));
    } catch (IllegalArgumentException ex) {
      return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }
  }

  @PostMapping("/picture")
  public ResponseEntity<?> uploadPicture(
      Authentication authentication, @RequestParam("file") MultipartFile file) {
    try {
      return ResponseEntity.ok(profileLogic.uploadPicture(currentUserId(authentication), file));
    } catch (IllegalArgumentException ex) {
      return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    } catch (Exception ex) {
      return ResponseEntity.internalServerError()
          .body(Map.of("error", "Failed to upload image"));
    }
  }

  private static Long currentUserId(Authentication authentication) {
    return (Long) authentication.getPrincipal();
  }
}
