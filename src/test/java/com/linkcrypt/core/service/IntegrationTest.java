package com.linkcrypt.core.service;

import com.linkcrypt.config.AppConfig;
import com.linkcrypt.core.domain.ShortLink;
import com.linkcrypt.core.exception.LimitExceededException;
import com.linkcrypt.core.port.UserRepository;
import com.linkcrypt.infra.*;
import com.linkcrypt.support.*;
import com.linkcrypt.infra.InMemoryLinkRepository;
import com.linkcrypt.infra.InMemoryUserRepository;
import com.linkcrypt.support.CollectingLogger;
import com.linkcrypt.support.CollectingNotificationService;
import com.linkcrypt.support.FakeClock;
import org.junit.jupiter.api.*;

import java.time.*;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class IntegrationTest {
    @Test
    void fullFlow_limitExhaustionNotifiesOwner_andLogs() {
        FakeClock clock = new FakeClock(Instant.parse("2025-01-01T00:00:00Z"));
        UserRepository userRepo = new InMemoryUserRepository();
        InMemoryLinkRepository linkRepo = new InMemoryLinkRepository();
        CollectingNotificationService notifier = new CollectingNotificationService();
        CollectingLogger logger = new CollectingLogger();
        AppConfig cfg = new AppConfig(
            Duration.ofHours(24), 5, 100, Duration.ofSeconds(60),
            "http://s/", 7, "logs/test.log");
        UserService userService = new UserService(userRepo, logger);
        LinkService linkService = new LinkService(
            linkRepo, userRepo, notifier, clock, cfg, new ShortCodeGenerator(7), logger);

        UUID owner = userService.register().getId();
        UUID other = userService.register().getId();

        ShortLink link = linkService.create(owner, "https://baeldung.com", 2);
        linkService.open(link.getCode(), other);
        linkService.open(link.getCode(), other);

        assertThrows(LimitExceededException.class, () -> linkService.open(link.getCode(), other));
        assertFalse(notifier.forUser(owner).isEmpty());
        assertTrue(logger.containsMessage("link created: code=" + link.getCode()));
        assertTrue(logger.containsMessage("open failed (limit)"));
        assertTrue(logger.containsLevel("WARN"));
    }

    @Test
    void fullFlow_expirationNotifiesOwner_andLogs() {
        FakeClock clock = new FakeClock(Instant.parse("2025-01-01T00:00:00Z"));
        UserRepository userRepo = new InMemoryUserRepository();
        InMemoryLinkRepository linkRepo = new InMemoryLinkRepository();
        CollectingNotificationService notifier = new CollectingNotificationService();
        CollectingLogger logger = new CollectingLogger();
        AppConfig cfg = new AppConfig(
            Duration.ofSeconds(30), 5, 100, Duration.ofSeconds(5),
            "http://s/", 7, "logs/test.log");
        UserService userService = new UserService(userRepo, logger);
        LinkService linkService = new LinkService(
            linkRepo, userRepo, notifier, clock, cfg, new ShortCodeGenerator(7), logger);
        UUID owner = userService.register().getId();
        ShortLink link = linkService.create(owner, "https://x.com", 5);

        clock.advanceSeconds(60);
        LinkExpirationScheduler scheduler = new LinkExpirationScheduler(
            linkRepo, notifier, clock, Duration.ofSeconds(5), logger);
        scheduler.cleanup();

        assertTrue(linkRepo.findByCode(link.getCode()).isEmpty());
        assertFalse(notifier.forUser(owner).isEmpty());
        assertTrue(logger.containsMessage("expired link removed: code=" + link.getCode()));
    }
}
