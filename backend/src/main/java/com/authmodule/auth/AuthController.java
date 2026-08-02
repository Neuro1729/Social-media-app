package com.authmodule.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/api/auth/register")
    public AuthModels.LoginResponse register(
            @Valid @RequestBody AuthModels.RegisterRequest request,
            HttpServletResponse response
    ) {
        return authService.register(request, response);
    }

    @PostMapping("/api/auth/login")
    public AuthModels.LoginResponse login(
            @Valid @RequestBody AuthModels.LoginRequest request,
            HttpServletResponse response
    ) {
        return authService.login(request, response);
    }

    @PostMapping("/api/auth/refresh")
    public AuthModels.LoginResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        return authService.refresh(request, response);
    }

    @PostMapping("/api/auth/logout")
    public AuthModels.MessageResponse logout(
            Authentication authentication,
            HttpServletResponse response
    ) {
        AuthPrincipal principal = principal(authentication);
        authService.logout(principal.userId(), principal.sessionId(), response);
        return new AuthModels.MessageResponse("Logged out");
    }

    @PostMapping("/api/auth/forgot-password")
    public AuthModels.ForgotPasswordResponse forgotPassword(
            @Valid @RequestBody AuthModels.ForgotPasswordRequest request
    ) {
        return authService.forgotPassword(request);
    }

    @PostMapping("/api/auth/reset-password")
    public AuthModels.MessageResponse resetPassword(
            @Valid @RequestBody AuthModels.ResetPasswordRequest request
    ) {
        authService.resetPassword(request);
        return new AuthModels.MessageResponse("Password updated");
    }

    @GetMapping("/api/account/me")
    public AuthModels.UserResponse me(Authentication authentication) {
        return authService.getCurrentUser(principal(authentication).userId());
    }

    @GetMapping("/api/account/sessions")
    public AuthModels.SessionsResponse sessions(Authentication authentication) {
        AuthPrincipal principal = principal(authentication);
        return authService.listSessions(principal.userId(), principal.sessionId());
    }

    @DeleteMapping("/api/account/sessions/{sessionId}")
    public AuthModels.MessageResponse deleteSession(
            @PathVariable String sessionId,
            Authentication authentication
    ) {
        AuthPrincipal principal = principal(authentication);
        authService.logoutSession(principal.userId(), sessionId, principal.sessionId());
        return new AuthModels.MessageResponse("Session revoked");
    }

    @DeleteMapping("/api/account/sessions")
    public AuthModels.MessageResponse deleteAllSessions(
            Authentication authentication,
            HttpServletResponse response
    ) {
        AuthPrincipal principal = principal(authentication);
        authService.logoutAll(principal.userId(), response);
        return new AuthModels.MessageResponse("All sessions revoked");
    }

    @PutMapping("/api/account/username")
    public AuthModels.UserResponse changeUsername(
            @Valid @RequestBody AuthModels.ChangeUsernameRequest request,
            Authentication authentication
    ) {
        return authService.changeUsername(principal(authentication).userId(), request);
    }

    @DeleteMapping("/api/account/username")
    public AuthModels.UserResponse removeUsername(Authentication authentication) {
        return authService.removeUsername(principal(authentication).userId());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<AuthModels.ErrorResponse> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(new AuthModels.ErrorResponse(ex.getMessage()));
    }

    private AuthPrincipal principal(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthPrincipal principal)) {
            throw new IllegalArgumentException("Unauthorized");
        }
        return principal;
    }
}
