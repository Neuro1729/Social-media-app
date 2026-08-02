package com.authmodule.auth;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class UsernameValidator {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,30}$");
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9]{8,15}$");

    public void validateUsername(String username) {
        if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
            throw new IllegalArgumentException(
                    "Username must be 3-30 characters and contain only letters, digits, or _");
        }
    }

    public String normalizeUsername(String username) {
        return username == null ? null : username.toLowerCase(Locale.ROOT);
    }

    public String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    public String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        String trimmed = phone.trim();
        boolean plus = trimmed.startsWith("+");
        String digits = trimmed.replaceAll("[^0-9]", "");
        return plus ? "+" + digits : digits;
    }

    public void validateEmail(String email) {
        String normalized = normalizeEmail(email);
        if (normalized == null || !EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Invalid email address");
        }
    }

    public void validatePhone(String phone) {
        String normalized = normalizePhone(phone);
        if (normalized == null || !PHONE_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Invalid phone number");
        }
    }

    public AuthModels.IdentifierType detectLoginType(String login) {
        if (login == null || login.isBlank()) {
            throw new IllegalArgumentException("Login is required");
        }
        String trimmed = login.trim();
        if (trimmed.contains("@")) {
            return AuthModels.IdentifierType.EMAIL;
        }
        String phoneCandidate = normalizePhone(trimmed);
        if (PHONE_PATTERN.matcher(phoneCandidate).matches() && trimmed.matches(".*\\d.*")) {
            // Prefer phone when it looks like a phone number (digits / +)
            if (trimmed.startsWith("+") || trimmed.chars().allMatch(c -> Character.isDigit(c) || c == ' ' || c == '-' || c == '(' || c == ')')) {
                return AuthModels.IdentifierType.PHONE;
            }
        }
        return AuthModels.IdentifierType.USERNAME;
    }

    public String normalizeLogin(String login, AuthModels.IdentifierType type) {
        return switch (type) {
            case USERNAME -> normalizeUsername(login.trim());
            case EMAIL -> normalizeEmail(login);
            case PHONE -> normalizePhone(login);
        };
    }
}
