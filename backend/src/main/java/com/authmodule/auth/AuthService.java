package com.authmodule.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository users;
    private final RedisSessionRepository sessions;
    private final JwtService jwtService;
    private final UsernameValidator validator;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    private final String cookieName;
    private final boolean cookieSecure;
    private final String cookieSameSite;
    private final long refreshTokenDays;
    private final long passwordResetHours;

    public AuthService(
            UserRepository users,
            RedisSessionRepository sessions,
            JwtService jwtService,
            UsernameValidator validator,
            PasswordEncoder passwordEncoder,
            @Value("${app.refresh-token.cookie-name}") String cookieName,
            @Value("${app.refresh-token.cookie-secure}") boolean cookieSecure,
            @Value("${app.refresh-token.cookie-same-site}") String cookieSameSite,
            @Value("${app.refresh-token.days}") long refreshTokenDays,
            @Value("${app.password-reset.token-hours}") long passwordResetHours
    ) {
        this.users = users;
        this.sessions = sessions;
        this.jwtService = jwtService;
        this.validator = validator;
        this.passwordEncoder = passwordEncoder;
        this.cookieName = cookieName;
        this.cookieSecure = cookieSecure;
        this.cookieSameSite = cookieSameSite;
        this.refreshTokenDays = refreshTokenDays;
        this.passwordResetHours = passwordResetHours;
    }

    @Transactional
    public AuthModels.LoginResponse register(
            AuthModels.RegisterRequest request,
            HttpServletResponse response
    ) {
        boolean hasEmail = request.email() != null && !request.email().isBlank();
        boolean hasPhone = request.phone() != null && !request.phone().isBlank();
        boolean hasUsername = request.username() != null && !request.username().isBlank();

        if (!hasEmail && !hasPhone) {
            throw new IllegalArgumentException("Email or phone is required");
        }

        if (hasUsername) {
            validator.validateUsername(request.username());
            String normalized = validator.normalizeUsername(request.username());
            var reservation = users.findUsernameReservation(normalized);
            if (reservation.isPresent()) {
                throw new IllegalArgumentException("Username is permanently reserved");
            }
            if (users.identifierExists(AuthModels.IdentifierType.USERNAME, normalized)) {
                throw new IllegalArgumentException("Username is already taken");
            }
        }
        if (hasEmail) {
            validator.validateEmail(request.email());
            String normalized = validator.normalizeEmail(request.email());
            if (users.identifierExists(AuthModels.IdentifierType.EMAIL, normalized)) {
                throw new IllegalArgumentException("Email is already registered");
            }
        }
        if (hasPhone) {
            validator.validatePhone(request.phone());
            String normalized = validator.normalizePhone(request.phone());
            if (users.identifierExists(AuthModels.IdentifierType.PHONE, normalized)) {
                throw new IllegalArgumentException("Phone is already registered");
            }
        }

        UserEntity user = users.createUser(passwordEncoder.encode(request.password()));

        if (hasUsername) {
            String normalized = validator.normalizeUsername(request.username());
            users.saveIdentifier(new LoginIdentifierEntity(
                    UUID.randomUUID(),
                    user.getId(),
                    AuthModels.IdentifierType.USERNAME,
                    request.username().trim(),
                    normalized,
                    true
            ));
            users.reserveUsername(normalized, user.getId());
        }
        if (hasEmail) {
            users.saveIdentifier(new LoginIdentifierEntity(
                    UUID.randomUUID(),
                    user.getId(),
                    AuthModels.IdentifierType.EMAIL,
                    request.email().trim(),
                    validator.normalizeEmail(request.email()),
                    true
            ));
        }
        if (hasPhone) {
            users.saveIdentifier(new LoginIdentifierEntity(
                    UUID.randomUUID(),
                    user.getId(),
                    AuthModels.IdentifierType.PHONE,
                    request.phone().trim(),
                    validator.normalizePhone(request.phone()),
                    true
            ));
        }

        return issueTokens(user, "Registration", response);
    }

    public AuthModels.LoginResponse login(
            AuthModels.LoginRequest request,
            HttpServletResponse response
    ) {
        AuthModels.IdentifierType type = validator.detectLoginType(request.login());
        String normalized = validator.normalizeLogin(request.login(), type);
        UserEntity user = users.findUserByLogin(type, normalized)
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        return issueTokens(user, request.deviceName(), response);
    }

    public AuthModels.LoginResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = readCookie(request);
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh token missing");
        }
        String hash = hashToken(refreshToken);
        String sessionId = sessions.findSessionIdByRefreshHash(hash)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));
        AuthModels.SessionData session = sessions.findSession(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session expired"));

        String newRefresh = generateSecureToken();
        AuthModels.SessionData rotated = sessions.rotateRefreshToken(sessionId, hashToken(newRefresh));
        writeRefreshCookie(response, newRefresh);

        UserEntity user = users.findUserById(rotated.userId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        String accessToken = jwtService.generateAccessToken(user.getId(), rotated.sessionId());
        return new AuthModels.LoginResponse(accessToken, toUserResponse(user));
    }

    public void logout(UUID userId, String sessionId, HttpServletResponse response) {
        sessions.findSession(sessionId).ifPresent(session -> {
            if (!session.userId().equals(userId)) {
                throw new IllegalArgumentException("Forbidden");
            }
            sessions.deleteSession(sessionId);
        });
        clearRefreshCookie(response);
    }

    public void logoutAll(UUID userId, HttpServletResponse response) {
        sessions.deleteAllSessions(userId);
        clearRefreshCookie(response);
    }

    public void logoutSession(UUID userId, String targetSessionId, String currentSessionId) {
        AuthModels.SessionData session = sessions.findSession(targetSessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));
        if (!session.userId().equals(userId)) {
            throw new IllegalArgumentException("Forbidden");
        }
        sessions.deleteSession(targetSessionId);
    }

    public AuthModels.UserResponse getCurrentUser(UUID userId) {
        UserEntity user = users.findUserById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return toUserResponse(user);
    }

    public AuthModels.SessionsResponse listSessions(UUID userId, String currentSessionId) {
        List<AuthModels.SessionResponse> list = sessions.getUserSessions(userId).stream()
                .map(s -> new AuthModels.SessionResponse(
                        s.sessionId(),
                        s.deviceName(),
                        s.createdAt(),
                        s.expiresAt(),
                        s.sessionId().equals(currentSessionId)
                ))
                .toList();
        return new AuthModels.SessionsResponse(list);
    }

    @Transactional
    public AuthModels.UserResponse changeUsername(UUID userId, AuthModels.ChangeUsernameRequest request) {
        validator.validateUsername(request.username());
        String normalized = validator.normalizeUsername(request.username());

        var reservation = users.findUsernameReservation(normalized);
        if (reservation.isPresent() && !reservation.get().getOwnerUserId().equals(userId)) {
            throw new IllegalArgumentException("Username is permanently reserved");
        }
        if (users.identifierExists(AuthModels.IdentifierType.USERNAME, normalized)) {
            var existing = users.findUserByLogin(AuthModels.IdentifierType.USERNAME, normalized);
            if (existing.isEmpty() || !existing.get().getId().equals(userId)) {
                throw new IllegalArgumentException("Username is already taken");
            }
        }

        var current = users.findIdentifier(userId, AuthModels.IdentifierType.USERNAME);
        if (current.isPresent()) {
            LoginIdentifierEntity identifier = current.get();
            // previous active username stays reserved forever (so others cannot take it)
            if (identifier.isActive()) {
                users.reserveUsername(identifier.getNormalizedValue(), userId);
            }
            identifier.setValue(request.username().trim());
            identifier.setNormalizedValue(normalized);
            identifier.setActive(true);
            users.updateIdentifier(identifier);
        } else {
            users.saveIdentifier(new LoginIdentifierEntity(
                    UUID.randomUUID(),
                    userId,
                    AuthModels.IdentifierType.USERNAME,
                    request.username().trim(),
                    normalized,
                    true
            ));
        }
        users.reserveUsername(normalized, userId);
        return getCurrentUser(userId);
    }

    @Transactional
    public AuthModels.UserResponse removeUsername(UUID userId) {
        LoginIdentifierEntity identifier = users.findIdentifier(userId, AuthModels.IdentifierType.USERNAME)
                .orElseThrow(() -> new IllegalArgumentException("No username set"));
        if (!identifier.isActive()) {
            throw new IllegalArgumentException("No username set");
        }
        // Keep reservation so others cannot claim it; owner can reclaim via changeUsername.
        // Delete the identifier row (do not store a long "removed:..." tombstone — exceeds varchar(30)).
        users.reserveUsername(identifier.getNormalizedValue(), userId);
        users.deleteIdentifier(identifier);
        return getCurrentUser(userId);
    }

    @Transactional
    public AuthModels.ForgotPasswordResponse forgotPassword(AuthModels.ForgotPasswordRequest request) {
        AuthModels.IdentifierType type = validator.detectLoginType(request.login());
        String normalized = validator.normalizeLogin(request.login(), type);
        UserEntity user = users.findUserByLogin(type, normalized)
                .orElse(null);
        // Always return success-looking message to avoid account enumeration
        if (user == null) {
            return new AuthModels.ForgotPasswordResponse(
                    "If an account exists, a reset token was issued.",
                    null
            );
        }
        String token = generateSecureToken();
        users.createPasswordResetToken(
                user.getId(),
                hashToken(token),
                Instant.now().plus(Duration.ofHours(passwordResetHours))
        );
        // No email provider in this module — return token for local/demo use
        return new AuthModels.ForgotPasswordResponse(
                "If an account exists, a reset token was issued.",
                token
        );
    }

    @Transactional
    public void resetPassword(AuthModels.ResetPasswordRequest request) {
        String hash = hashToken(request.token());
        PasswordResetTokenEntity token = users.findPasswordResetTokenByHash(hash)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset token"));
        if (token.isUsed() || token.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Invalid or expired reset token");
        }
        UserEntity user = users.findUserById(token.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        users.updatePassword(user, passwordEncoder.encode(request.newPassword()));
        token.setUsed(true);
        sessions.deleteAllSessions(user.getId());
    }

    private AuthModels.LoginResponse issueTokens(
            UserEntity user,
            String deviceName,
            HttpServletResponse response
    ) {
        String refreshToken = generateSecureToken();
        AuthModels.SessionData session = sessions.createSession(
                user.getId(),
                hashToken(refreshToken),
                deviceName
        );
        writeRefreshCookie(response, refreshToken);
        String accessToken = jwtService.generateAccessToken(user.getId(), session.sessionId());
        return new AuthModels.LoginResponse(accessToken, toUserResponse(user));
    }

    private AuthModels.UserResponse toUserResponse(UserEntity user) {
        String username = null;
        String email = null;
        String phone = null;
        for (LoginIdentifierEntity identifier : users.findIdentifiersByUserId(user.getId())) {
            if (!identifier.isActive()) {
                continue;
            }
            switch (identifier.getType()) {
                case USERNAME -> username = identifier.getValue();
                case EMAIL -> email = identifier.getValue();
                case PHONE -> phone = identifier.getValue();
            }
        }
        return new AuthModels.UserResponse(user.getId(), username, email, phone, user.getCreatedAt());
    }

    private void writeRefreshCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from(cookieName, refreshToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/api/auth")
                .maxAge(Duration.ofDays(refreshTokenDays))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/api/auth")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String readCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
