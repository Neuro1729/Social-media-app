package com.socialmedia.modules.users.infrastructure;

import com.socialmedia.modules.users.domain.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<User, Long> {
  Optional<User> findByEmail(String email);

  Optional<User> findByUsernameIgnoreCase(String username);

  boolean existsByUsername(String username);

  boolean existsByEmail(String email);
}
