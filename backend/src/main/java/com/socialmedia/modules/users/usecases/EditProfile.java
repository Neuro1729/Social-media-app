package com.socialmedia.modules.users.usecases;

import com.socialmedia.modules.users.domain.User;
import com.socialmedia.modules.users.infrastructure.UserJpaRepository;
import com.socialmedia.modules.users.publicapi.PublicUserReader;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class EditProfile {

  private final UserJpaRepository users;
  private final PublicUserReader publicUserReader;

  public EditProfile(UserJpaRepository users, PublicUserReader publicUserReader) {
    this.users = users;
    this.publicUserReader = publicUserReader;
  }

  @Transactional
  public Map<String, Object> execute(Long userId, String fullName, String bio) {
    User user =
        users.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
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
    users.save(user);
    return GetMyProfile.toMap(
        publicUserReader
            .getPublicProfile(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found")));
  }
}
