package com.linkcrypt.infra;

import com.linkcrypt.core.domain.ShortLink;
import com.linkcrypt.core.port.LinkRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryLinkRepository implements LinkRepository {
  private final ConcurrentHashMap<String, ShortLink> store = new ConcurrentHashMap<>();

  @Override
  public void save(ShortLink link) {
    store.put(link.getCode(), link);
  }

  @Override
  public Optional<ShortLink> findByCode(String c) {
    return Optional.ofNullable(store.get(c));
  }

  @Override
  public void delete(String code) {
    store.remove(code);
  }

  @Override
  public List<ShortLink> findAll() {
    return List.copyOf(store.values());
  }

  @Override
  public List<ShortLink> findByOwner(UUID ownerId) {
    return store.values().stream()
        .filter(l -> l.getOwnerId().equals(ownerId))
        .collect(Collectors.toList());
  }

  @Override
  public boolean existsByCode(String code) {
    return store.containsKey(code);
  }
}
