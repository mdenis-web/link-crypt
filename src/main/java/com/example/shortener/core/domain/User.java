package com.example.shortener.core.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class User {
    private final UUID id;

    public User(UUID id, Instant createdAt) {
        this.id = Objects.requireNonNull(id);
    }

    public UUID getId() {
        return id;
    }
}
