package com.authmodule.social;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "blocks")
@IdClass(Block.BlockId.class)
public class Block {

    @Id
    @Column(name = "blocker_id", nullable = false)
    private UUID blockerId;

    @Id
    @Column(name = "blocked_id", nullable = false)
    private UUID blockedId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Block() {
    }

    public Block(UUID blockerId, UUID blockedId, Instant createdAt) {
        this.blockerId = blockerId;
        this.blockedId = blockedId;
        this.createdAt = createdAt;
    }

    public UUID getBlockerId() {
        return blockerId;
    }

    public UUID getBlockedId() {
        return blockedId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public static class BlockId implements Serializable {
        private UUID blockerId;
        private UUID blockedId;

        public BlockId() {
        }

        public BlockId(UUID blockerId, UUID blockedId) {
            this.blockerId = blockerId;
            this.blockedId = blockedId;
        }

        public UUID getBlockerId() {
            return blockerId;
        }

        public void setBlockerId(UUID blockerId) {
            this.blockerId = blockerId;
        }

        public UUID getBlockedId() {
            return blockedId;
        }

        public void setBlockedId(UUID blockedId) {
            this.blockedId = blockedId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof BlockId blockId)) {
                return false;
            }
            return Objects.equals(blockerId, blockId.blockerId)
                    && Objects.equals(blockedId, blockId.blockedId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(blockerId, blockedId);
        }
    }
}
