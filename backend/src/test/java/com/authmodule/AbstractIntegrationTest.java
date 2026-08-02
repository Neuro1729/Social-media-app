package com.authmodule;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Integration tests expect reachable Postgres + Redis.
 * Defaults match local Docker Compose published ports.
 */
public abstract class AbstractIntegrationTest {

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () ->
                "jdbc:postgresql://"
                        + env("DB_HOST", "localhost") + ":"
                        + env("DB_PORT", "5432") + "/"
                        + env("DB_NAME", "socialmedia")
                        + "?sslmode=" + env("DB_SSL_MODE", "disable"));
        registry.add("spring.datasource.username", () -> env("DB_USER", "postgres"));
        registry.add("spring.datasource.password", () -> env("DB_PASSWORD", "postgres"));
        registry.add("spring.data.redis.host", () -> env("REDIS_HOST", "localhost"));
        registry.add("spring.data.redis.port", () -> Integer.parseInt(env("REDIS_PORT", "6379")));
        registry.add("app.likes.sync-enabled", () -> "false");
        registry.add("app.frontend-origins", () -> "http://localhost:3000");
        registry.add("app.jwt.secret", () -> "test-secret-key-at-least-32-characters-long");
    }

    private static String env(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
