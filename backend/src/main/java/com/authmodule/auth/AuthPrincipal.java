package com.authmodule.auth;

import java.util.UUID;

public record AuthPrincipal(UUID userId, String sessionId) {
}
