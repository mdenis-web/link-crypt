package com.linkcrypt.support;

import com.linkcrypt.core.port.NotificationService;
import java.util.*;
import java.util.stream.Collectors;

public class CollectingNotificationService implements NotificationService {
  private final List<Notif> notifications = new ArrayList<>();

  public record Notif(UUID userId, String message) {}

  @Override
  public synchronized void notify(UUID userId, String message) {
    notifications.add(new Notif(userId, message));
  }

  public synchronized List<Notif> all() {
    return new ArrayList<>(notifications);
  }

  public synchronized List<Notif> forUser(UUID id) {
    return notifications.stream()
        .filter(notif -> notif.userId().equals(id))
        .collect(Collectors.toList());
  }

  public synchronized void clear() {
    notifications.clear();
  }
}
