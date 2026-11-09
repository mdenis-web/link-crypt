package com.example.shortener.core.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class User {
    private final UUID id;
    private final Instant createdAt;

    public User(UUID id, Instant createdAt) {
        this.id = Objects.requireNonNull(id);
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public UUID getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
