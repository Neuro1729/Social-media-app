package com.socialmedia.modules.users.publicapi;

import java.time.Instant;
import java.util.Optional;

/** Public contract used by posts/auth. No access to User entity outside users module. */
public interface PublicUserReader {

  record PublicProfile(
      Long id,
      String username,
      String email,
      String fullName,
      String bio,
      String profilePictureUrl,
      Instant createdAt) {}

  Optional<PublicProfile> getPublicProfile(Long userId);

  Optional<PublicProfile> getPublicProfileByUsername(String username);
}
