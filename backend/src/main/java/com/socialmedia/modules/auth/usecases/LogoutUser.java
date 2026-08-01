package com.socialmedia.modules.auth.usecases;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class LogoutUser {

  public Map<String, String> execute() {
    return Map.of("message", "Logged out successfully");
  }
}
