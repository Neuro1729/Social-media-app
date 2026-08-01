package com.socialmedia.api;

import com.socialmedia.logic.AuthLogic;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthApi {

  private final AuthLogic authLogic;

  public AuthApi(AuthLogic authLogic) {
    this.authLogic = authLogic;
  }

  @PostMapping("/signup")
  public ResponseEntity<?> signup(@RequestBody Map<String, String> body) {
    try {
      return ResponseEntity.status(HttpStatus.CREATED)
          .body(
              authLogic.signup(
                  body.get("username"), body.get("email"), body.get("password")));
    } catch (IllegalArgumentException ex) {
      return badRequest(ex.getMessage());
    }
  }

  @PostMapping("/login")
  public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
    try {
      return ResponseEntity.ok(authLogic.login(body.get("email"), body.get("password")));
    } catch (IllegalArgumentException ex) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", ex.getMessage()));
    }
  }

  @PostMapping("/logout")
  public ResponseEntity<?> logout() {
    return ResponseEntity.ok(authLogic.logout());
  }

  private static ResponseEntity<Map<String, String>> badRequest(String message) {
    return ResponseEntity.badRequest().body(Map.of("error", message));
  }
}
