package com.linkcrypt.infra;

import com.linkcrypt.core.domain.User;
import com.linkcrypt.core.port.UserRepository;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryUserRepository implements UserRepository {
  private final ConcurrentHashMap<UUID, User> store = new ConcurrentHashMap<>();

  @Override
  public void save(User user) {
    store.put(user.getId(), user);
  }

  @Override
  public boolean exists(UUID id) {
    return store.containsKey(id);
  }
}
