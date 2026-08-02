package com.authmodule.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class AuthModels {

    private AuthModels() {
    }

    public enum IdentifierType {
        USERNAME, EMAIL, PHONE
    }

    public record RegisterRequest(
            @Size(min = 3, max = 30) String username,
            String email,
            String phone,
            @NotBlank @Size(min = 8, max = 100) String password
    ) {
    }

    public record LoginRequest(
            @NotBlank String login,
            @NotBlank String password,
            String deviceName
    ) {
    }

    public record LoginResponse(
            String accessToken,
            UserResponse user
    ) {
    }

    public record UserResponse(
            UUID id,
            String username,
            String email,
            String phone,
            Instant createdAt
    ) {
    }

    public record SessionResponse(
            String sessionId,
            String deviceName,
            Instant createdAt,
            Instant expiresAt,
            boolean current
    ) {
    }

    public record SessionsResponse(List<SessionResponse> sessions) {
    }

    public record ForgotPasswordRequest(@NotBlank String login) {
    }

    public record ForgotPasswordResponse(String message, String resetToken) {
    }

    public record ResetPasswordRequest(
            @NotBlank String token,
            @NotBlank @Size(min = 8, max = 100) String newPassword
    ) {
    }

    public record ChangeUsernameRequest(
            @NotBlank @Size(min = 3, max = 30) String username
    ) {
    }

    public record MessageResponse(String message) {
    }

    public record ErrorResponse(String error) {
    }

    public record SessionData(
            String sessionId,
            UUID userId,
            String refreshTokenHash,
            String deviceName,
            Instant createdAt,
            Instant expiresAt
    ) {
    }
}

@Entity
@Table(name = "users")
class UserEntity {

    @Id
    private UUID id;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected UserEntity() {
    }

    UserEntity(UUID id, String passwordHash, Instant createdAt) {
        this.id = id;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
    }

    UUID getId() {
        return id;
    }

    String getPasswordHash() {
        return passwordHash;
    }

    void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}

@Entity
@Table(name = "login_identifiers")
class LoginIdentifierEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuthModels.IdentifierType type;

    @Column(nullable = false)
    private String value;

    @Column(name = "normalized_value", nullable = false)
    private String normalizedValue;

    @Column(nullable = false)
    private boolean active;

    protected LoginIdentifierEntity() {
    }

    LoginIdentifierEntity(
            UUID id,
            UUID userId,
            AuthModels.IdentifierType type,
            String value,
            String normalizedValue,
            boolean active
    ) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.value = value;
        this.normalizedValue = normalizedValue;
        this.active = active;
    }

    UUID getId() {
        return id;
    }

    UUID getUserId() {
        return userId;
    }

    AuthModels.IdentifierType getType() {
        return type;
    }

    String getValue() {
        return value;
    }

    void setValue(String value) {
        this.value = value;
    }

    String getNormalizedValue() {
        return normalizedValue;
    }

    void setNormalizedValue(String normalizedValue) {
        this.normalizedValue = normalizedValue;
    }

    boolean isActive() {
        return active;
    }

    void setActive(boolean active) {
        this.active = active;
    }
}

@Entity
@Table(name = "username_reservations")
class UsernameReservationEntity {

    @Id
    @Column(name = "normalized_username", length = 30)
    private String normalizedUsername;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    protected UsernameReservationEntity() {
    }

    UsernameReservationEntity(String normalizedUsername, UUID ownerUserId) {
        this.normalizedUsername = normalizedUsername;
        this.ownerUserId = ownerUserId;
    }

    String getNormalizedUsername() {
        return normalizedUsername;
    }

    UUID getOwnerUserId() {
        return ownerUserId;
    }
}

@Entity
@Table(name = "password_reset_tokens")
class PasswordResetTokenEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean used;

    protected PasswordResetTokenEntity() {
    }

    PasswordResetTokenEntity(UUID id, UUID userId, String tokenHash, Instant expiresAt, boolean used) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.used = used;
    }

    UUID getId() {
        return id;
    }

    UUID getUserId() {
        return userId;
    }

    String getTokenHash() {
        return tokenHash;
    }

    Instant getExpiresAt() {
        return expiresAt;
    }

    boolean isUsed() {
        return used;
    }

    void setUsed(boolean used) {
        this.used = used;
    }
}
