-- Posts module: posts, likes, comments, interaction events

CREATE TABLE posts (
    id         UUID PRIMARY KEY,
    author_id  UUID                     NOT NULL REFERENCES users (id),
    body       VARCHAR(2000)            NOT NULL,
    status     VARCHAR(20)              NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ              NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ              NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,

    CHECK (status IN ('ACTIVE', 'DELETED')),
    CHECK (char_length(body) > 0)
);

CREATE INDEX idx_posts_author_status_created
    ON posts (author_id, status, created_at DESC, id DESC);

CREATE INDEX idx_posts_status_created
    ON posts (status, created_at DESC, id DESC);

CREATE TABLE post_likes (
    post_id    UUID        NOT NULL REFERENCES posts (id),
    user_id    UUID        NOT NULL REFERENCES users (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    PRIMARY KEY (post_id, user_id)
);

CREATE INDEX idx_post_likes_user_created
    ON post_likes (user_id, created_at DESC);

CREATE INDEX idx_post_likes_post_created
    ON post_likes (post_id, created_at DESC);

CREATE TABLE post_comments (
    id         UUID PRIMARY KEY,
    post_id    UUID                     NOT NULL REFERENCES posts (id),
    author_id  UUID                     NOT NULL REFERENCES users (id),
    body       VARCHAR(1000)            NOT NULL,
    status     VARCHAR(20)              NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ              NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ              NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,

    CHECK (status IN ('ACTIVE', 'DELETED')),
    CHECK (char_length(body) > 0)
);

CREATE INDEX idx_post_comments_post_status_created
    ON post_comments (post_id, status, created_at DESC, id DESC);

CREATE INDEX idx_post_comments_author_created
    ON post_comments (author_id, created_at DESC);

CREATE TABLE interaction_events (
    id              UUID PRIMARY KEY,
    actor_id        UUID                     NOT NULL REFERENCES users (id),
    event_type      VARCHAR(40)              NOT NULL,
    target_type     VARCHAR(40)              NOT NULL,
    target_id       UUID                     NOT NULL,
    target_owner_id UUID                     REFERENCES users (id),
    metadata_json   TEXT,
    created_at      TIMESTAMPTZ              NOT NULL DEFAULT NOW(),

    CHECK (event_type IN (
        'POST_LIKED',
        'POST_UNLIKED',
        'COMMENT_CREATED',
        'COMMENT_DELETED',
        'POST_VIEWED',
        'PROFILE_VIEWED',
        'FOLLOWED_USER'
    )),
    CHECK (target_type IN ('POST', 'COMMENT', 'USER', 'PROFILE'))
);

CREATE INDEX idx_interaction_events_actor_created
    ON interaction_events (actor_id, created_at DESC);

CREATE INDEX idx_interaction_events_target
    ON interaction_events (target_type, target_id, created_at DESC);

CREATE INDEX idx_interaction_events_type_created
    ON interaction_events (event_type, created_at DESC);

CREATE INDEX idx_interaction_events_owner_created
    ON interaction_events (target_owner_id, created_at DESC);
