package com.socialmedia.logic;

import com.socialmedia.data.UserData;
import com.socialmedia.model.User;
import com.socialmedia.upload.ImageUpload;
import java.io.IOException;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProfileLogic {

  private final UserData userData;
  private final ImageUpload imageUpload;

  public ProfileLogic(UserData userData, ImageUpload imageUpload) {
    this.userData = userData;
    this.imageUpload = imageUpload;
  }

  public Map<String, Object> getProfile(Long userId) {
    return AuthLogic.toPublicUser(requireUser(userId));
  }

  public Map<String, Object> createOrUpdateProfile(Long userId, String fullName, String bio) {
    User user = requireUser(userId);
    if (fullName != null) {
      user.setFullName(fullName.trim());
    }
    if (bio != null) {
      user.setBio(bio.trim());
    }
    if ((user.getFullName() == null || user.getFullName().isBlank())
        && (user.getBio() == null || user.getBio().isBlank())) {
      throw new IllegalArgumentException("Provide at least a full name or bio");
    }
    return AuthLogic.toPublicUser(userData.save(user));
  }

  public Map<String, Object> uploadPicture(Long userId, MultipartFile file) throws IOException {
    User user = requireUser(userId);
    String path = imageUpload.saveProfilePicture(userId, file);
    user.setProfilePicture(path);
    return AuthLogic.toPublicUser(userData.save(user));
  }

  private User requireUser(Long userId) {
    return userData
        .findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("User not found"));
  }
}
