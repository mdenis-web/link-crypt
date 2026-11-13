package com.linkcrypt.core.domain;

import java.util.Objects;
import java.util.UUID;

public final class User {
  private final UUID id;

  public User(UUID id) {
    this.id = Objects.requireNonNull(id);
  }

  public UUID getId() {
    return id;
  }
}
