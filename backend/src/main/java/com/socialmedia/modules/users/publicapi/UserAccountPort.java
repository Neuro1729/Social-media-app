package com.socialmedia.modules.users.publicapi;

import java.util.Optional;

/** Account operations exposed to the auth module only. */
public interface UserAccountPort {

  record AuthAccount(Long id, String username, String email, String passwordHash) {}

  record CreatedAccount(Long id, String username, String email) {}

  CreatedAccount register(String username, String email, String passwordHash);

  Optional<AuthAccount> findByEmail(String email);

  boolean existsByUsername(String username);

  boolean existsByEmail(String email);
}
