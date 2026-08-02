-- Auth module schema (4 tables only)

CREATE TABLE users (
    id            UUID PRIMARY KEY,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMP NOT NULL
);

CREATE TABLE login_identifiers (
    id               UUID PRIMARY KEY,
    user_id          UUID NOT NULL REFERENCES users (id),
    type             VARCHAR(20) NOT NULL,
    value            VARCHAR(255) NOT NULL,
    normalized_value VARCHAR(255) NOT NULL,
    active           BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_login_identifiers_type
        CHECK (type IN ('USERNAME', 'EMAIL', 'PHONE')),
    CONSTRAINT uq_login_identifiers_type_normalized
        UNIQUE (type, normalized_value)
);

CREATE INDEX idx_login_identifiers_user_id
    ON login_identifiers (user_id);

CREATE TABLE username_reservations (
    normalized_username VARCHAR(30) PRIMARY KEY,
    owner_user_id       UUID NOT NULL REFERENCES users (id)
);

CREATE INDEX idx_username_reservations_owner
    ON username_reservations (owner_user_id);

CREATE TABLE password_reset_tokens (
    id         UUID PRIMARY KEY,
    user_id    UUID NOT NULL REFERENCES users (id),
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used       BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_password_reset_tokens_user_id
    ON password_reset_tokens (user_id);

CREATE INDEX idx_password_reset_tokens_token_hash
    ON password_reset_tokens (token_hash);
