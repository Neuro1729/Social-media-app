package com.socialmedia.modules.auth.api;

import com.socialmedia.modules.auth.usecases.LoginUser;
import com.socialmedia.modules.auth.usecases.LogoutUser;
import com.socialmedia.modules.auth.usecases.SignUpUser;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final SignUpUser signUpUser;
  private final LoginUser loginUser;
  private final LogoutUser logoutUser;

  public AuthController(SignUpUser signUpUser, LoginUser loginUser, LogoutUser logoutUser) {
    this.signUpUser = signUpUser;
    this.loginUser = loginUser;
    this.logoutUser = logoutUser;
  }

  @PostMapping("/signup")
  public ResponseEntity<?> signup(@RequestBody Map<String, String> body) {
    try {
      return ResponseEntity.status(HttpStatus.CREATED)
          .body(signUpUser.execute(body.get("username"), body.get("email"), body.get("password")));
    } catch (IllegalArgumentException ex) {
      return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }
  }

  @PostMapping("/login")
  public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
    try {
      return ResponseEntity.ok(loginUser.execute(body.get("email"), body.get("password")));
    } catch (IllegalArgumentException ex) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", ex.getMessage()));
    }
  }

  @PostMapping("/logout")
  public ResponseEntity<?> logout() {
    return ResponseEntity.ok(logoutUser.execute());
  }
}
