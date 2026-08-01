package com.socialmedia.modules.users.infrastructure;

import com.socialmedia.modules.media.publicapi.MediaStorage;
import com.socialmedia.modules.users.domain.User;
import com.socialmedia.modules.users.publicapi.PublicUserReader;
import com.socialmedia.modules.users.publicapi.UserAccountPort;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class UserModuleAdapters implements PublicUserReader, UserAccountPort {

  private final UserJpaRepository users;
  private final MediaStorage mediaStorage;

  public UserModuleAdapters(UserJpaRepository users, MediaStorage mediaStorage) {
    this.users = users;
    this.mediaStorage = mediaStorage;
  }

  @Override
  public Optional<PublicProfile> getPublicProfile(Long userId) {
    return users.findById(userId).map(this::toPublic);
  }

  @Override
  public Optional<PublicProfile> getPublicProfileByUsername(String username) {
    return users.findByUsernameIgnoreCase(username).map(this::toPublic);
  }

  @Override
  public CreatedAccount register(String username, String email, String passwordHash) {
    User user = new User();
    user.setUsername(username);
    user.setEmail(email);
    user.setPasswordHash(passwordHash);
    user = users.save(user);
    return new CreatedAccount(user.getId(), user.getUsername(), user.getEmail());
  }

  @Override
  public Optional<AuthAccount> findByEmail(String email) {
    return users
        .findByEmail(email)
        .map(
            u ->
                new AuthAccount(
                    u.getId(), u.getUsername(), u.getEmail(), u.getPasswordHash()));
  }

  @Override
  public boolean existsByUsername(String username) {
    return users.existsByUsername(username);
  }

  @Override
  public boolean existsByEmail(String email) {
    return users.existsByEmail(email);
  }

  private PublicProfile toPublic(User user) {
    String pictureUrl =
        mediaStorage.getUrl(user.getProfilePictureMediaId()).orElse(null);
    return new PublicProfile(
        user.getId(),
        user.getUsername(),
        user.getEmail(),
        user.getFullName(),
        user.getBio(),
        pictureUrl,
        user.getCreatedAt());
  }
}
