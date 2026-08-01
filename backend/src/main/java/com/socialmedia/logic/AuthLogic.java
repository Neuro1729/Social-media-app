package com.socialmedia.logic;

import com.socialmedia.config.JwtService;
import com.socialmedia.data.UserData;
import com.socialmedia.model.User;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthLogic {

  private final UserData userData;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;

  public AuthLogic(UserData userData, PasswordEncoder passwordEncoder, JwtService jwtService) {
    this.userData = userData;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
  }

  public Map<String, Object> signup(String username, String email, String password) {
    if (username == null || username.isBlank()) {
      throw new IllegalArgumentException("Username is required");
    }
    if (email == null || email.isBlank()) {
      throw new IllegalArgumentException("Email is required");
    }
    if (password == null || password.length() < 6) {
      throw new IllegalArgumentException("Password must be at least 6 characters");
    }
    if (userData.existsByUsername(username.trim())) {
      throw new IllegalArgumentException("Username already taken");
    }
    if (userData.existsByEmail(email.trim().toLowerCase())) {
      throw new IllegalArgumentException("Email already registered");
    }

    User user = new User();
    user.setUsername(username.trim());
    user.setEmail(email.trim().toLowerCase());
    user.setPassword(passwordEncoder.encode(password));
    user = userData.save(user);

    String token = jwtService.generateToken(user.getId(), user.getEmail());
    return authResponse(token, user);
  }

  public Map<String, Object> login(String email, String password) {
    if (email == null || password == null) {
      throw new IllegalArgumentException("Email and password are required");
    }
    User user =
        userData
            .findByEmail(email.trim().toLowerCase())
            .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));
    if (!passwordEncoder.matches(password, user.getPassword())) {
      throw new IllegalArgumentException("Invalid email or password");
    }
    String token = jwtService.generateToken(user.getId(), user.getEmail());
    return authResponse(token, user);
  }

  public Map<String, String> logout() {
    return Map.of("message", "Logged out successfully");
  }

  private Map<String, Object> authResponse(String token, User user) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("token", token);
    body.put("user", toPublicUser(user));
    return body;
  }

  static Map<String, Object> toPublicUser(User user) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("id", user.getId());
    map.put("username", user.getUsername());
    map.put("email", user.getEmail());
    map.put("fullName", user.getFullName());
    map.put("bio", user.getBio());
    map.put("profilePicture", user.getProfilePicture());
    map.put("createdAt", user.getCreatedAt());
    return map;
  }
}
