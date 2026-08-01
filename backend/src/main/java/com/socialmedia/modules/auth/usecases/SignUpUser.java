package com.socialmedia.modules.auth.usecases;

import com.socialmedia.modules.users.publicapi.PublicUserReader;
import com.socialmedia.modules.users.publicapi.UserAccountPort;
import com.socialmedia.shared.security.JwtService;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class SignUpUser {

  private final UserAccountPort accounts;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final LoginUser loginUser;

  public SignUpUser(
      UserAccountPort accounts,
      PasswordEncoder passwordEncoder,
      JwtService jwtService,
      LoginUser loginUser) {
    this.accounts = accounts;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.loginUser = loginUser;
  }

  public Map<String, Object> execute(String username, String email, String password) {
    if (username == null || username.isBlank()) {
      throw new IllegalArgumentException("Username is required");
    }
    if (email == null || email.isBlank()) {
      throw new IllegalArgumentException("Email is required");
    }
    if (password == null || password.length() < 6) {
      throw new IllegalArgumentException("Password must be at least 6 characters");
    }
    String cleanUsername = username.trim();
    String cleanEmail = email.trim().toLowerCase();
    if (accounts.existsByUsername(cleanUsername)) {
      throw new IllegalArgumentException("Username already taken");
    }
    if (accounts.existsByEmail(cleanEmail)) {
      throw new IllegalArgumentException("Email already registered");
    }

    UserAccountPort.CreatedAccount created =
        accounts.register(cleanUsername, cleanEmail, passwordEncoder.encode(password));
    String token = jwtService.generateToken(created.id(), created.email());
    return LoginUser.authResponse(token, loginUser.loadProfile(created.id()));
  }
}
