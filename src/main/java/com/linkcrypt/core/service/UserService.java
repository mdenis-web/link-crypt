package com.linkcrypt.core.service;

import com.linkcrypt.core.domain.User;
import com.linkcrypt.core.port.AppLogger;
import com.linkcrypt.core.port.UserRepository;

import java.util.UUID;

public class UserService {
    private final UserRepository repository;
    private final AppLogger logger;

    public UserService(UserRepository repository, AppLogger logger) {
        this.repository = repository;
        this.logger = logger;
    }

    public User register() {
        User user = new User(UUID.randomUUID());
        repository.save(user);
        logger.info("user registered: id=" + user.getId());
        return user;
    }

    public boolean exists(UUID id) {
        return repository.exists(id);
    }
}
