package com.linkcrypt.core.port;

import com.linkcrypt.core.domain.User;

import java.util.UUID;

public interface UserRepository {
    void save(User user);

    boolean exists(UUID id);
}
