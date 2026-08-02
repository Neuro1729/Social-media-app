package com.authmodule.social;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "follows")
@IdClass(Follow.FollowId.class)
public class Follow {

    public enum Status {
        PENDING,
        FOLLOWING,
        REJECTED
    }

    @Id
    @Column(name = "follower_id", nullable = false)
    private UUID followerId;

    @Id
    @Column(name = "following_id", nullable = false)
    private UUID followingId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Status status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Follow() {
    }

    public Follow(UUID followerId, UUID followingId, Status status, Instant createdAt) {
        this.followerId = followerId;
        this.followingId = followingId;
        this.status = status;
        this.createdAt = createdAt;
    }

    public UUID getFollowerId() {
        return followerId;
    }

    public UUID getFollowingId() {
        return followingId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public static class FollowId implements Serializable {
        private UUID followerId;
        private UUID followingId;

        public FollowId() {
        }

        public FollowId(UUID followerId, UUID followingId) {
            this.followerId = followerId;
            this.followingId = followingId;
        }

        public UUID getFollowerId() {
            return followerId;
        }

        public void setFollowerId(UUID followerId) {
            this.followerId = followerId;
        }

        public UUID getFollowingId() {
            return followingId;
        }

        public void setFollowingId(UUID followingId) {
            this.followingId = followingId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof FollowId followId)) {
                return false;
            }
            return Objects.equals(followerId, followId.followerId)
                    && Objects.equals(followingId, followId.followingId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(followerId, followingId);
        }
    }
}
