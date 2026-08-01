package com.socialmedia.modules.users.api;

import com.socialmedia.modules.users.usecases.EditProfile;
import com.socialmedia.modules.users.usecases.GetMyProfile;
import com.socialmedia.modules.users.usecases.UploadProfilePicture;
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
public class ProfileController {

  private final GetMyProfile getMyProfile;
  private final EditProfile editProfile;
  private final UploadProfilePicture uploadProfilePicture;

  public ProfileController(
      GetMyProfile getMyProfile,
      EditProfile editProfile,
      UploadProfilePicture uploadProfilePicture) {
    this.getMyProfile = getMyProfile;
    this.editProfile = editProfile;
    this.uploadProfilePicture = uploadProfilePicture;
  }

  @GetMapping("/me")
  public ResponseEntity<?> me(Authentication authentication) {
    try {
      return ResponseEntity.ok(getMyProfile.execute(currentUserId(authentication)));
    } catch (IllegalArgumentException ex) {
      return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }
  }

  @PutMapping
  public ResponseEntity<?> saveProfile(
      Authentication authentication, @RequestBody Map<String, String> body) {
    try {
      return ResponseEntity.ok(
          editProfile.execute(
              currentUserId(authentication), body.get("fullName"), body.get("bio")));
    } catch (IllegalArgumentException ex) {
      return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }
  }

  @PostMapping("/picture")
  public ResponseEntity<?> uploadPicture(
      Authentication authentication, @RequestParam("file") MultipartFile file) {
    try {
      return ResponseEntity.ok(
          uploadProfilePicture.execute(currentUserId(authentication), file));
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
