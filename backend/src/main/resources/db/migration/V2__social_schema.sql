-- Social module: profiles, follows, blocks

CREATE TABLE profiles (
    user_id             UUID PRIMARY KEY REFERENCES users (id),
    bio                 VARCHAR(160),
    profile_picture_url TEXT,
    is_private          BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE follows (
    follower_id  UUID                     NOT NULL REFERENCES users (id),
    following_id UUID                     NOT NULL REFERENCES users (id),
    status       VARCHAR(10)              NOT NULL,
    created_at   TIMESTAMPTZ              NOT NULL DEFAULT NOW(),

    PRIMARY KEY (follower_id, following_id),

    CHECK (follower_id <> following_id),
    CHECK (status IN ('PENDING', 'FOLLOWING', 'REJECTED'))
);

CREATE INDEX idx_follows_following_status_created
    ON follows (following_id, status, created_at DESC);

CREATE INDEX idx_follows_follower_status_created
    ON follows (follower_id, status, created_at DESC);

CREATE TABLE blocks (
    blocker_id UUID        NOT NULL REFERENCES users (id),
    blocked_id UUID        NOT NULL REFERENCES users (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    PRIMARY KEY (blocker_id, blocked_id),

    CHECK (blocker_id <> blocked_id)
);

CREATE INDEX idx_blocks_blocked_blocker
    ON blocks (blocked_id, blocker_id);
