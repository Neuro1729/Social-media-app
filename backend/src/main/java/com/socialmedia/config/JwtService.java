package com.socialmedia.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtService {

  private final SecretKey key;
  private final long expirationMs;

  public JwtService(
      @Value("${app.jwt.secret}") String secret,
      @Value("${app.jwt.expiration-ms}") long expirationMs) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.expirationMs = expirationMs;
  }

  public String generateToken(Long userId, String email) {
    Date now = new Date();
    return Jwts.builder()
        .subject(String.valueOf(userId))
        .claim("email", email)
        .issuedAt(now)
        .expiration(new Date(now.getTime() + expirationMs))
        .signWith(key)
        .compact();
  }

  public Long extractUserId(String token) {
    return Long.parseLong(parseClaims(token).getSubject());
  }

  public boolean isValid(String token) {
    try {
      Claims claims = parseClaims(token);
      return claims.getExpiration().after(new Date());
    } catch (Exception ex) {
      return false;
    }
  }

  private Claims parseClaims(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }
}
