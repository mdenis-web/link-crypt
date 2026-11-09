package com.example.shortener.core.port;

import com.example.shortener.core.domain.User;

import java.util.UUID;

public interface UserRepository {
    void save(User user);

    boolean exists(UUID id);
}
