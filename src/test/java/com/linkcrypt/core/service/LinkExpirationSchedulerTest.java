package com.linkcrypt.core.service;

import static org.junit.jupiter.api.Assertions.*;

import com.linkcrypt.config.AppConfig;
import com.linkcrypt.core.port.UserRepository;
import com.linkcrypt.infra.*;
import com.linkcrypt.infra.InMemoryLinkRepository;
import com.linkcrypt.infra.InMemoryUserRepository;
import com.linkcrypt.support.*;
import com.linkcrypt.support.CollectingLogger;
import com.linkcrypt.support.CollectingNotificationService;
import com.linkcrypt.support.FakeClock;
import java.time.*;
import java.util.UUID;
import org.junit.jupiter.api.*;

class LinkExpirationSchedulerTest {
  private FakeClock clock;
  private LinkService linkService;
  private InMemoryLinkRepository linkRepo;
  private CollectingNotificationService notifier;
  private CollectingLogger logger;
  private UUID user;
  private LinkExpirationScheduler scheduler;

  @BeforeEach
  void setUp() {
    clock = new FakeClock(Instant.parse("2025-01-01T00:00:00Z"));
    UserRepository userRepo = new InMemoryUserRepository();
    linkRepo = new InMemoryLinkRepository();
    notifier = new CollectingNotificationService();
    logger = new CollectingLogger();
    AppConfig cfg =
        new AppConfig(
            Duration.ofSeconds(60),
            5,
            100,
            Duration.ofSeconds(10),
            "http://s/",
            7,
            "logs/test.log");
    UserService userService = new UserService(userRepo, logger);
    linkService =
        new LinkService(
            linkRepo, userRepo, notifier, clock, cfg, new ShortCodeGenerator(7), logger);
    user = userService.register().getId();
    scheduler =
        new LinkExpirationScheduler(linkRepo, notifier, clock, Duration.ofSeconds(10), logger);
  }

  @Test
  void deletesExpiredLinksAndNotifies() {
    var link = linkService.create(user, "https://example.com", 5);
    clock.advanceSeconds(120);
    scheduler.cleanup();
    assertTrue(linkRepo.findByCode(link.getCode()).isEmpty());
    assertFalse(notifier.forUser(user).isEmpty());
    assertTrue(logger.containsMessage("expired link removed: code=" + link.getCode()));
  }

  @Test
  void keepsActiveLinksUntouched() {
    var link = linkService.create(user, "https://example.com", 5);
    clock.advanceSeconds(30);
    scheduler.cleanup();
    assertTrue(linkRepo.findByCode(link.getCode()).isPresent());
    assertTrue(notifier.all().isEmpty());
  }
}
