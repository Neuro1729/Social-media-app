package com.socialmedia.modules.auth.usecases;

import com.socialmedia.modules.users.publicapi.PublicUserReader;
import com.socialmedia.modules.users.publicapi.UserAccountPort;
import com.socialmedia.modules.users.usecases.GetMyProfile;
import com.socialmedia.shared.security.JwtService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class LoginUser {

  private final UserAccountPort accounts;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final PublicUserReader users;

  public LoginUser(
      UserAccountPort accounts,
      PasswordEncoder passwordEncoder,
      JwtService jwtService,
      PublicUserReader users) {
    this.accounts = accounts;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.users = users;
  }

  public Map<String, Object> execute(String email, String password) {
    if (email == null || password == null) {
      throw new IllegalArgumentException("Email and password are required");
    }
    UserAccountPort.AuthAccount account =
        accounts
            .findByEmail(email.trim().toLowerCase())
            .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));
    if (!passwordEncoder.matches(password, account.passwordHash())) {
      throw new IllegalArgumentException("Invalid email or password");
    }
    String token = jwtService.generateToken(account.id(), account.email());
    return authResponse(token, loadProfile(account.id()));
  }

  Map<String, Object> loadProfile(Long userId) {
    return GetMyProfile.toMap(
        users
            .getPublicProfile(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found")));
  }

  static Map<String, Object> authResponse(String token, Map<String, Object> user) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("token", token);
    body.put("user", user);
    return body;
  }
}
