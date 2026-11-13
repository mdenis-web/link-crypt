package com.linkcrypt.core.port;

import java.util.UUID;

public interface NotificationService {
  void notify(UUID userId, String message);
}
